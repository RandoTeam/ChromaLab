#!/usr/bin/env python3
"""Benchmark optional OCR engines against DR-1R axis-label crop sweep artifacts."""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import threading
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from pathlib import Path
from typing import Any


NUMERIC_RE = re.compile(r"[-+]?(?:\d+(?:[\.,]\d*)?|[\.,]\d+)(?:[eE][-+]?\d+)?")
DEFAULT_ENGINES = (
    "rapidocr_python",
    "tesseract_cli_psm7_numeric",
    "tesseract_cli_psm6_text",
    "paddleocr_cli",
)


@dataclass(frozen=True)
class CropVariant:
    fixture: str
    graph_index: int
    band_id: str
    variant_id: str
    path: Path
    report_path: Path
    width: int
    height: int
    text_like_component_count: int
    warnings: tuple[str, ...]


@dataclass(frozen=True)
class EngineStatus:
    name: str
    available: bool
    reason: str


class OcrEngine:
    name: str

    def status(self) -> EngineStatus:
        raise NotImplementedError

    def run(self, image_path: Path, timeout_seconds: float) -> dict[str, Any]:
        raise NotImplementedError


class TesseractCliEngine(OcrEngine):
    def __init__(self, name: str, psm: int, numeric_only: bool) -> None:
        self.name = name
        self.psm = psm
        self.numeric_only = numeric_only
        self.executable = shutil.which("tesseract")

    def status(self) -> EngineStatus:
        if not self.executable:
            return EngineStatus(self.name, False, "tesseract executable was not found on PATH.")
        return EngineStatus(self.name, True, self.executable)

    def run(self, image_path: Path, timeout_seconds: float) -> dict[str, Any]:
        command = [
            self.executable or "tesseract",
            str(image_path),
            "stdout",
            "-l",
            "eng",
            "--psm",
            str(self.psm),
        ]
        if self.numeric_only:
            command += ["-c", "tessedit_char_whitelist=0123456789.,-+eE"]
        completed = subprocess.run(
            command,
            check=False,
            capture_output=True,
            text=True,
            timeout=timeout_seconds,
        )
        text = completed.stdout.strip()
        return {
            "status": "OK" if completed.returncode == 0 else "ERROR",
            "text": text,
            "raw": completed.stdout,
            "stderr": completed.stderr.strip(),
            "returnCode": completed.returncode,
        }


class PaddleOcrCliEngine(OcrEngine):
    name = "paddleocr_cli"

    def __init__(self) -> None:
        self.executable = shutil.which("paddleocr")

    def status(self) -> EngineStatus:
        if not self.executable:
            return EngineStatus(self.name, False, "paddleocr CLI was not found on PATH.")
        return EngineStatus(self.name, True, self.executable)

    def run(self, image_path: Path, timeout_seconds: float) -> dict[str, Any]:
        command = [
            self.executable or "paddleocr",
            "ocr",
            "-i",
            str(image_path),
            "--use_doc_orientation_classify",
            "False",
            "--use_doc_unwarping",
            "False",
            "--use_textline_orientation",
            "False",
        ]
        completed = subprocess.run(
            command,
            check=False,
            capture_output=True,
            text=True,
            timeout=timeout_seconds,
        )
        text = extract_text_from_paddle_cli_output(completed.stdout)
        return {
            "status": "OK" if completed.returncode == 0 else "ERROR",
            "text": "\n".join(text).strip(),
            "raw": completed.stdout,
            "stderr": completed.stderr.strip(),
            "returnCode": completed.returncode,
        }


class RapidOcrPythonEngine(OcrEngine):
    name = "rapidocr_python"

    def __init__(self) -> None:
        self._local = threading.local()

    def status(self) -> EngineStatus:
        try:
            import rapidocr  # noqa: F401
            import onnxruntime  # noqa: F401
        except Exception as exc:  # pragma: no cover - environment-specific
            return EngineStatus(self.name, False, f"rapidocr/onnxruntime import failed: {exc}")
        return EngineStatus(self.name, True, "rapidocr and onnxruntime Python packages are importable.")

    def run(self, image_path: Path, timeout_seconds: float) -> dict[str, Any]:
        _ = timeout_seconds
        engine = getattr(self._local, "engine", None)
        if engine is None:
            from rapidocr import RapidOCR

            engine = RapidOCR()
            self._local.engine = engine
        result = engine(str(image_path))
        items = flatten_ocr_items(result)
        text = "\n".join(item["text"] for item in items if item.get("text")).strip()
        return {
            "status": "OK",
            "text": text,
            "items": items,
            "rawType": type(result).__name__,
        }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run OCR backend benchmark on axis-label crop sweep artifacts.")
    parser.add_argument("--input-root", default="build/dr1r-axis-label-crop-sweep")
    parser.add_argument("--output-root", default="build/dr1s-ocr-backend-benchmark")
    parser.add_argument("--engines", default=",".join(DEFAULT_ENGINES))
    parser.add_argument("--workers", type=int, default=max(1, min((os.cpu_count() or 4) - 2, 10)))
    parser.add_argument("--timeout-seconds", type=float, default=12.0)
    parser.add_argument("--variant", action="append", default=[], help="Variant id to include. Repeatable.")
    parser.add_argument("--fixture", action="append", default=[], help="Fixture id to include. Repeatable.")
    parser.add_argument("--limit", type=int, default=0, help="Limit variants after filtering.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    repo_root = Path.cwd()
    input_root = resolve_path(repo_root, args.input_root)
    output_root = resolve_path(repo_root, args.output_root)
    output_root.mkdir(parents=True, exist_ok=True)

    variants = load_crop_variants(input_root)
    if args.variant:
        allowed_variants = set(args.variant)
        variants = [variant for variant in variants if variant.variant_id in allowed_variants]
    if args.fixture:
        allowed_fixtures = set(args.fixture)
        variants = [variant for variant in variants if variant.fixture in allowed_fixtures]
    if args.limit > 0:
        variants = variants[: args.limit]

    requested_engine_names = [name.strip() for name in args.engines.split(",") if name.strip()]
    engines = [create_engine(name) for name in requested_engine_names]
    backend_statuses = [engine.status() for engine in engines]
    available_engines = [engine for engine, status in zip(engines, backend_statuses) if status.available]

    start = time.perf_counter()
    results: list[dict[str, Any]] = []
    if available_engines and variants:
        jobs = [(engine, variant) for engine in available_engines for variant in variants]
        max_workers = max(1, min(args.workers, len(jobs)))
        with ThreadPoolExecutor(max_workers=max_workers) as executor:
            futures = {
                executor.submit(run_one, engine, variant, args.timeout_seconds): (engine, variant)
                for engine, variant in jobs
            }
            for future in as_completed(futures):
                results.append(future.result())

    elapsed_ms = int((time.perf_counter() - start) * 1000)
    results.sort(key=lambda row: (row["fixture"], row["graphIndex"], row["bandId"], row["variantId"], row["engine"]))

    write_csv(output_root / "dr1s_ocr_backend_results.csv", results)
    summary = build_summary(input_root, output_root, args, variants, backend_statuses, results, elapsed_ms)
    (output_root / "dr1s_ocr_backend_summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
    (output_root / "dr1s_ocr_backend_summary.md").write_text(to_markdown(summary, results), encoding="utf-8")

    print(f"Input variants: {len(variants)}")
    print(f"Available engines: {', '.join(engine.name for engine in available_engines) or 'none'}")
    print(f"Results: {len(results)}")
    print(f"Summary: {output_root / 'dr1s_ocr_backend_summary.json'}")
    return 0


def resolve_path(repo_root: Path, value: str) -> Path:
    path = Path(value)
    if path.is_absolute():
        return path
    return (repo_root / path).resolve()


def create_engine(name: str) -> OcrEngine:
    if name == "rapidocr_python":
        return RapidOcrPythonEngine()
    if name == "tesseract_cli_psm7_numeric":
        return TesseractCliEngine(name, psm=7, numeric_only=True)
    if name == "tesseract_cli_psm6_text":
        return TesseractCliEngine(name, psm=6, numeric_only=False)
    if name == "paddleocr_cli":
        return PaddleOcrCliEngine()
    raise ValueError(f"Unknown OCR engine: {name}")


def load_crop_variants(input_root: Path) -> list[CropVariant]:
    reports = sorted(input_root.rglob("axis_label_crop_sweep_graph_*.json"))
    variants: list[CropVariant] = []
    for report_path in reports:
        report = json.loads(report_path.read_text(encoding="utf-8"))
        fixture = report_path.parents[2].name if len(report_path.parents) >= 3 else "unknown"
        graph_index = int(report.get("graphIndex", 0))
        for item in report.get("variants", []):
            path = Path(item.get("path", ""))
            variants.append(
                CropVariant(
                    fixture=fixture,
                    graph_index=graph_index,
                    band_id=str(item.get("bandId", "")),
                    variant_id=str(item.get("variantId", "")),
                    path=path,
                    report_path=report_path,
                    width=int(item.get("width", 0)),
                    height=int(item.get("height", 0)),
                    text_like_component_count=int(item.get("textLikeComponentCount", 0)),
                    warnings=tuple(str(warning) for warning in item.get("warnings", [])),
                ),
            )
    return variants


def run_one(engine: OcrEngine, variant: CropVariant, timeout_seconds: float) -> dict[str, Any]:
    started = time.perf_counter()
    try:
        if not variant.path.exists():
            payload = {
                "status": "ERROR",
                "text": "",
                "error": "crop file does not exist",
            }
        else:
            payload = engine.run(variant.path, timeout_seconds)
    except subprocess.TimeoutExpired:
        payload = {
            "status": "TIMEOUT",
            "text": "",
            "error": f"timeout after {timeout_seconds}s",
        }
    except Exception as exc:  # pragma: no cover - environment-specific
        payload = {
            "status": "ERROR",
            "text": "",
            "error": str(exc),
        }
    duration_ms = int((time.perf_counter() - started) * 1000)
    text = str(payload.get("text", "")).strip()
    numeric_tokens = NUMERIC_RE.findall(text)
    return {
        "fixture": variant.fixture,
        "graphIndex": variant.graph_index,
        "bandId": variant.band_id,
        "variantId": variant.variant_id,
        "engine": engine.name,
        "status": payload.get("status", "UNKNOWN"),
        "durationMs": duration_ms,
        "text": text,
        "numericTokens": " ".join(numeric_tokens),
        "numericTokenCount": len(numeric_tokens),
        "cropPath": str(variant.path),
        "cropSha256": sha256_file(variant.path) if variant.path.exists() else "",
        "width": variant.width,
        "height": variant.height,
        "textLikeComponentCount": variant.text_like_component_count,
        "warnings": ";".join(variant.warnings),
        "error": payload.get("error", ""),
        "stderr": payload.get("stderr", ""),
        "returnCode": payload.get("returnCode", ""),
    }


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def extract_text_from_paddle_cli_output(stdout: str) -> list[str]:
    texts: list[str] = []
    for match in re.finditer(r"\('([^']+)',\s*[0-9.]+\)", stdout):
        texts.append(match.group(1))
    if texts:
        return texts
    return [line.strip() for line in stdout.splitlines() if line.strip()]


def flatten_ocr_items(value: Any) -> list[dict[str, Any]]:
    items: list[dict[str, Any]] = []
    collect_ocr_items(value, items)
    return items


def collect_ocr_items(value: Any, items: list[dict[str, Any]]) -> None:
    if value is None:
        return
    for attr in ("txts", "texts"):
        attr_value = getattr(value, attr, None)
        if attr_value:
            for text in attr_value:
                items.append({"text": str(text), "confidence": None})
            return
    if isinstance(value, dict):
        text = value.get("text") or value.get("transcription")
        if text:
            items.append({"text": str(text), "confidence": value.get("score") or value.get("confidence")})
        for child in value.values():
            collect_ocr_items(child, items)
        return
    if isinstance(value, tuple) and len(value) >= 2 and isinstance(value[0], str):
        items.append({"text": value[0], "confidence": value[1] if isinstance(value[1], (int, float)) else None})
        return
    if isinstance(value, (list, tuple)):
        for child in value:
            collect_ocr_items(child, items)


def write_csv(path: Path, rows: list[dict[str, Any]]) -> None:
    fieldnames = [
        "fixture",
        "graphIndex",
        "bandId",
        "variantId",
        "engine",
        "status",
        "durationMs",
        "text",
        "numericTokens",
        "numericTokenCount",
        "cropPath",
        "cropSha256",
        "width",
        "height",
        "textLikeComponentCount",
        "warnings",
        "error",
        "stderr",
        "returnCode",
    ]
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames)
        writer.writeheader()
        for row in rows:
            writer.writerow({name: row.get(name, "") for name in fieldnames})


def build_summary(
    input_root: Path,
    output_root: Path,
    args: argparse.Namespace,
    variants: list[CropVariant],
    backend_statuses: list[EngineStatus],
    results: list[dict[str, Any]],
    elapsed_ms: int,
) -> dict[str, Any]:
    fixture_summary: dict[str, dict[str, Any]] = {}
    for variant in variants:
        entry = fixture_summary.setdefault(
            variant.fixture,
            {
                "variantCount": 0,
                "graphCount": 0,
                "graphs": set(),
                "bands": set(),
                "ocrTextHits": 0,
                "ocrNumericHits": 0,
                "enginesWithNumericHits": set(),
            },
        )
        entry["variantCount"] += 1
        entry["graphs"].add(variant.graph_index)
        entry["bands"].add(variant.band_id)
    for row in results:
        entry = fixture_summary.setdefault(
            row["fixture"],
            {
                "variantCount": 0,
                "graphCount": 0,
                "graphs": set(),
                "bands": set(),
                "ocrTextHits": 0,
                "ocrNumericHits": 0,
                "enginesWithNumericHits": set(),
            },
        )
        if row.get("text"):
            entry["ocrTextHits"] += 1
        if int(row.get("numericTokenCount") or 0) > 0:
            entry["ocrNumericHits"] += 1
            entry["enginesWithNumericHits"].add(row.get("engine", ""))

    normalized_fixture_summary = []
    for fixture, entry in sorted(fixture_summary.items()):
        normalized_fixture_summary.append(
            {
                "fixture": fixture,
                "variantCount": entry["variantCount"],
                "graphCount": len(entry["graphs"]),
                "bands": sorted(entry["bands"]),
                "ocrTextHits": entry["ocrTextHits"],
                "ocrNumericHits": entry["ocrNumericHits"],
                "enginesWithNumericHits": sorted(entry["enginesWithNumericHits"]),
            },
        )

    return {
        "schemaVersion": "chromalab-dr1s-ocr-backend-benchmark-1.0",
        "inputRoot": str(input_root),
        "outputRoot": str(output_root),
        "workersRequested": args.workers,
        "timeoutSeconds": args.timeout_seconds,
        "cpuCount": os.cpu_count(),
        "elapsedMs": elapsed_ms,
        "variantCount": len(variants),
        "resultCount": len(results),
        "textHitCount": sum(1 for row in results if row.get("text")),
        "numericHitCount": sum(1 for row in results if int(row.get("numericTokenCount") or 0) > 0),
        "backendStatuses": [status.__dict__ for status in backend_statuses],
        "fixtureSummary": normalized_fixture_summary,
        "outputs": {
            "csv": str(output_root / "dr1s_ocr_backend_results.csv"),
            "json": str(output_root / "dr1s_ocr_backend_summary.json"),
            "markdown": str(output_root / "dr1s_ocr_backend_summary.md"),
        },
    }


def to_markdown(summary: dict[str, Any], results: list[dict[str, Any]]) -> str:
    lines = [
        "# DR-1S OCR Backend Benchmark Summary",
        "",
        f"- Input root: `{summary['inputRoot']}`",
        f"- Variants: {summary['variantCount']}",
        f"- Results: {summary['resultCount']}",
        f"- Workers requested: {summary['workersRequested']}",
        f"- Elapsed: {summary['elapsedMs']} ms",
        f"- Text hits: {summary['textHitCount']}",
        f"- Numeric hits: {summary['numericHitCount']}",
        "",
        "## Backend Status",
        "",
        "| Backend | Available | Reason |",
        "| --- | --- | --- |",
    ]
    for status in summary["backendStatuses"]:
        lines.append(f"| {status['name']} | {str(status['available']).lower()} | {status['reason']} |")
    lines += [
        "",
        "## Fixture Summary",
        "",
        "| Fixture | Graphs | Variants | Text hits | Numeric hits | Engines with numeric hits |",
        "| --- | ---: | ---: | ---: | ---: | --- |",
    ]
    for fixture in summary["fixtureSummary"]:
        engines = ", ".join(fixture["enginesWithNumericHits"]) or "none"
        lines.append(
            f"| {fixture['fixture']} | {fixture['graphCount']} | {fixture['variantCount']} | "
            f"{fixture['ocrTextHits']} | {fixture['ocrNumericHits']} | {engines} |",
        )
    lines += [
        "",
        "## Top Numeric Reads",
        "",
        "| Fixture | Graph | Band | Variant | Engine | Text | Numeric tokens |",
        "| --- | ---: | --- | --- | --- | --- | --- |",
    ]
    numeric_rows = [row for row in results if int(row.get("numericTokenCount") or 0) > 0][:25]
    if not numeric_rows:
        lines.append("| none |  |  |  |  |  |  |")
    for row in numeric_rows:
        text = str(row.get("text", "")).replace("\n", " / ").replace("|", "/")
        lines.append(
            f"| {row['fixture']} | {row['graphIndex']} | {row['bandId']} | {row['variantId']} | "
            f"{row['engine']} | {text[:120]} | {row['numericTokens']} |",
        )
    return "\n".join(lines) + "\n"


if __name__ == "__main__":
    sys.exit(main())
