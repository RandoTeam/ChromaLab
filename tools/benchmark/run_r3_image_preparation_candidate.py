#!/usr/bin/env python3
"""Build R3 Stage 1 image-preparation candidate records.

This is a PC-side shadow benchmark only. It does not change Android runtime,
validators, graph selection, plotArea selection, report gates,
chromatographic math, model policy, or CalculationEngine.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import shutil
import time
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image, ImageFilter, ImageOps


ANDROID_METADATA_DIR = Path("composeApp/src/androidMain/assets/validation")
EXAMPLE_ROOT = Path("benchmark/examples/r3_image_preparation_candidate")
REPORT_ROOT = Path("benchmark/reports/r3_image_preparation_candidate")
PREVIEW_ROOT = REPORT_ROOT / "previews"


EXPECTED_LAYOUTS = {
    "white_tiger_ion71": "SINGLE_TRACE_SINGLE_AXIS",
    "bench_01_mz71_screenshot_page": "MULTI_PANEL_SEPARATE_AXES",
    "bench_02_mz92_belyi_tigr": "SINGLE_TRACE_SINGLE_AXIS",
    "bench_03_small_tic_export": "LOW_RES_EXPORT_GRAPH",
    "bench_04_stacked_xic_resolution": "MULTI_PANEL_SEPARATE_AXES",
    "bench_05_tic_plus_ions": "TIC_PLUS_ION_PANELS",
    "bench_06_photo_two_graphs_page": "TWO_GRAPH_PAGE",
    "bench_07_rotated_page_photo": "ROTATED_PAGE_GRAPH",
}


def read_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def write_text(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write(text)


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def sha256_image(image: Image.Image) -> str:
    digest = hashlib.sha256()
    digest.update(image.convert("RGB").tobytes())
    digest.update(str(image.size).encode("ascii"))
    return digest.hexdigest()


def load_metadata() -> list[dict[str, Any]]:
    records = []
    for path in sorted(ANDROID_METADATA_DIR.glob("*.metadata.json")):
        data = read_json(path)
        data["_metadataPath"] = str(path).replace("\\", "/")
        records.append(data)
    return records


def clamp_ratio(value: float, low: float, high: float) -> float:
    if high <= low:
        return 0.0
    return max(0.0, min(1.0, (value - low) / (high - low)))


def otsu_threshold(gray: np.ndarray) -> int:
    hist = np.bincount(gray.flatten(), minlength=256).astype(np.float64)
    total = gray.size
    if total == 0:
        return 128
    sum_total = float(np.dot(np.arange(256), hist))
    sum_background = 0.0
    weight_background = 0.0
    best_variance = -1.0
    best_threshold = 128
    for threshold in range(256):
        weight_background += hist[threshold]
        if weight_background == 0:
            continue
        weight_foreground = total - weight_background
        if weight_foreground == 0:
            break
        sum_background += threshold * hist[threshold]
        mean_background = sum_background / weight_background
        mean_foreground = (sum_total - sum_background) / weight_foreground
        variance = weight_background * weight_foreground * (mean_background - mean_foreground) ** 2
        if variance > best_variance:
            best_variance = variance
            best_threshold = threshold
    return int(best_threshold)


def image_metrics(image: Image.Image) -> dict[str, float | int]:
    gray = np.array(image.convert("L"), dtype=np.uint8)
    height, width = gray.shape
    p05, p10, p50, p90, p95 = np.percentile(gray, [5, 10, 50, 90, 95])
    dark = gray < max(35, p10)
    vertical_edges = np.abs(np.diff(gray.astype(np.int16), axis=1)) > 28
    horizontal_edges = np.abs(np.diff(gray.astype(np.int16), axis=0)) > 28
    edge_density = (vertical_edges.mean() + horizontal_edges.mean()) / 2.0
    return {
        "width": int(width),
        "height": int(height),
        "megapixels": round((width * height) / 1_000_000.0, 4),
        "aspectRatio": round(width / max(1, height), 4),
        "luminanceMean": round(float(gray.mean()), 4),
        "luminanceStd": round(float(gray.std()), 4),
        "p05": round(float(p05), 4),
        "p10": round(float(p10), 4),
        "p50": round(float(p50), 4),
        "p90": round(float(p90), 4),
        "p95": round(float(p95), 4),
        "contrastP90P10": round(float(p90 - p10), 4),
        "darkPixelFraction": round(float(dark.mean()), 6),
        "edgeDensity": round(float(edge_density), 6),
    }


def variant_images(source: Image.Image) -> dict[str, Image.Image]:
    rgb = source.convert("RGB")
    gray = source.convert("L")
    autocontrast = ImageOps.autocontrast(gray)
    sharpened = autocontrast.filter(ImageFilter.SHARPEN).filter(ImageFilter.SHARPEN)
    threshold = otsu_threshold(np.array(autocontrast, dtype=np.uint8))
    binary = (np.array(autocontrast, dtype=np.uint8) > threshold).astype(np.uint8) * 255
    return {
        "source_rgb": rgb,
        "grayscale": gray.convert("RGB"),
        "autocontrast": autocontrast.convert("RGB"),
        "sharpened_autocontrast": sharpened.convert("RGB"),
        "otsu_binary": Image.fromarray(binary, mode="L").convert("RGB"),
    }


def score_variant(metrics: dict[str, float | int], variant_id: str) -> float:
    contrast = clamp_ratio(float(metrics["contrastP90P10"]), 25.0, 170.0)
    edges = clamp_ratio(float(metrics["edgeDensity"]), 0.015, 0.18)
    dark_fraction = float(metrics["darkPixelFraction"])
    dark_score = 1.0 - min(1.0, abs(dark_fraction - 0.12) / 0.18)
    penalty = 0.0
    if variant_id == "otsu_binary":
        penalty += 0.12
    if float(metrics["p95"]) < 120:
        penalty += 0.15
    return round(max(0.0, (contrast * 0.45) + (edges * 0.35) + (dark_score * 0.20) - penalty), 6)


def status_for(metrics: dict[str, float | int], selected_score: float) -> tuple[str, list[str]]:
    warnings: list[str] = []
    if float(metrics["megapixels"]) < 0.35:
        warnings.append("LOW_RESOLUTION_INPUT")
    if float(metrics["contrastP90P10"]) < 30:
        warnings.append("LOW_CONTRAST_INPUT")
    if float(metrics["edgeDensity"]) < 0.015:
        warnings.append("LOW_EDGE_DENSITY")
    if selected_score < 0.32:
        warnings.append("WEAK_PREPROCESSING_VARIANT_SCORE")
    if warnings:
        return "REVIEW", warnings
    return "PASS", warnings


def preview_image(image: Image.Image, fixture_id: str, variant_id: str) -> str:
    preview = image.copy()
    preview.thumbnail((480, 360), Image.Resampling.LANCZOS)
    out = PREVIEW_ROOT / f"{fixture_id}_{variant_id}.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    preview.save(out)
    return str(out).replace("\\", "/")


def build_record(metadata: dict[str, Any]) -> tuple[dict[str, Any], dict[str, Any]]:
    fixture_id = metadata["fixtureId"]
    source_image = Path(str(metadata["assetImagePath"]))
    if not source_image.exists():
        source_image = Path("composeApp/src/androidMain/assets") / str(metadata["assetImagePath"])
    started = time.perf_counter()
    with Image.open(source_image) as opened:
        exif_transposed = ImageOps.exif_transpose(opened)
        normalized = exif_transposed.convert("RGB")
    normalized_hash = sha256_image(normalized)
    variants = variant_images(normalized)
    variant_rows = []
    for variant_id, image in variants.items():
        metrics = image_metrics(image)
        score = score_variant(metrics, variant_id)
        variant_rows.append(
            {
                "variantId": variant_id,
                "score": score,
                "metrics": metrics,
            }
        )
    variant_rows.sort(key=lambda row: (-float(row["score"]), str(row["variantId"])))
    selected = variant_rows[0]
    selected_variant_id = str(selected["variantId"])
    selected_preview = preview_image(variants[selected_variant_id], fixture_id, selected_variant_id)
    source_preview = preview_image(normalized, fixture_id, "normalized_source")
    source_metrics = image_metrics(normalized)
    stage_status, warnings = status_for(source_metrics, float(selected["score"]))
    elapsed_ms = round((time.perf_counter() - started) * 1000.0, 3)
    expected_count = int(metadata.get("expectedGraphCount") or 0)
    record_id = f"stage123_{fixture_id}_pc_prototype_r3_image_preparation_candidate_v1"
    record = {
        "schemaVersion": "chromalab.benchmark.stage123_parity_record.v1",
        "recordId": record_id,
        "fixtureId": fixture_id,
        "mode": "pc_prototype",
        "sourceId": "r3_image_preparation_candidate_v1",
        "sourceKind": "PC_STAGE1_IMAGE_PREP_CANDIDATE",
        "productionImpact": "NONE_SHADOW_ONLY",
        "runtimeReadiness": "PC_STAGE1_SHADOW_CANDIDATE_NOT_RUNTIME_READY",
        "expectedGraphCount": expected_count,
        "detectedGraphCount": 0,
        "graphCountScore": "NOT_SCOREABLE",
        "expectedLayoutClass": EXPECTED_LAYOUTS.get(fixture_id, "UNKNOWN_REVIEW"),
        "predictedLayoutClass": "UNKNOWN_REVIEW",
        "layoutClassScore": "NOT_SCOREABLE",
        "imagePreparation": {
            "status": stage_status,
            "available": True,
            "summary": "R3 PC candidate decoded image, applied EXIF transpose, generated preprocessing variants, selected a ranked variant, and wrote preview artifacts.",
            "metrics": {
                "sourceWidth": int(source_metrics["width"]),
                "sourceHeight": int(source_metrics["height"]),
                "sourceMegapixels": float(source_metrics["megapixels"]),
                "sourceContrastP90P10": float(source_metrics["contrastP90P10"]),
                "sourceEdgeDensity": float(source_metrics["edgeDensity"]),
                "selectedVariantScore": float(selected["score"]),
                "variantCount": len(variant_rows),
                "elapsedMs": elapsed_ms,
            },
        },
        "graphDiscovery": {
            "status": "NOT_SCORED",
            "available": False,
            "summary": "R3 is Stage 1 only; graph discovery remains the next replacement layer.",
        },
        "plotAreaLayout": {
            "status": "NOT_SCORED",
            "available": False,
            "summary": "R3 is Stage 1 only; plotArea/layout remains the next replacement layer.",
        },
        "failureClass": "",
        "firstFailingStage": "",
        "evidence": {
            "sourceImage": str(source_image).replace("\\", "/"),
            "recordSource": str(REPORT_ROOT / "summary.json").replace("\\", "/"),
            "artifactPaths": [source_preview, selected_preview],
        },
        "promotionDecision": "STAGE1_CANDIDATE_REQUIRES_STAGE2_PARITY",
        "notes": [
            "Shadow-only PC Stage 1 candidate.",
            "No graphPanel, plotArea, calibration, trace, peak, or report-gate authority.",
            *warnings,
        ],
    }
    detail = {
        "fixtureId": fixture_id,
        "sourceImage": str(source_image).replace("\\", "/"),
        "metadataPath": metadata.get("_metadataPath"),
        "sourceSha256": sha256_file(source_image),
        "normalizedSha256": normalized_hash,
        "expectedGraphCount": expected_count,
        "expectedLayoutClass": record["expectedLayoutClass"],
        "status": stage_status,
        "warnings": warnings,
        "selectedVariantId": selected_variant_id,
        "selectedVariantScore": selected["score"],
        "sourcePreview": source_preview,
        "selectedPreview": selected_preview,
        "sourceMetrics": source_metrics,
        "variantScores": variant_rows,
        "elapsedMs": elapsed_ms,
        "recordPath": str(EXAMPLE_ROOT / record_id / "stage123-parity-record.json").replace("\\", "/"),
    }
    return record, detail


def build_summary(details: list[dict[str, Any]]) -> dict[str, Any]:
    status_counts: dict[str, int] = {}
    selected_counts: dict[str, int] = {}
    for detail in details:
        status_counts[detail["status"]] = status_counts.get(detail["status"], 0) + 1
        selected = detail["selectedVariantId"]
        selected_counts[selected] = selected_counts.get(selected, 0) + 1
    return {
        "schemaVersion": "chromalab.benchmark.r3_image_preparation_candidate_summary.v1",
        "productionImpact": "NONE_SHADOW_ONLY",
        "overallVerdict": "R3_STAGE1_IMAGE_PREP_CANDIDATE_READY_FOR_RUST_PARITY",
        "recordCount": len(details),
        "fixtureCount": len({detail["fixtureId"] for detail in details}),
        "statusCounts": status_counts,
        "selectedVariantCounts": selected_counts,
        "nextRequiredWork": [
            "Port the Stage 1 image-preparation candidate into a Rust parity bridge or equivalent Rust-owned primitive layer.",
            "Compare Rust Stage 1 output against the R3 PC candidate before production promotion.",
            "Only after Stage 1 parity is stable, add Stage 2 graph discovery candidate that consumes selected Stage 1 variants.",
            "Compare graph count and layout on the same fixtures before production promotion.",
            "Keep Android runtime unchanged until parity gates pass.",
        ],
        "records": details,
    }


def write_markdown(summary: dict[str, Any]) -> None:
    lines = [
        "# R3 Image Preparation Candidate",
        "",
        f"Verdict: `{summary['overallVerdict']}`",
        "",
        "Production impact: `NONE_SHADOW_ONLY`",
        "",
        f"Records: `{summary['recordCount']}`",
        f"Fixtures: `{summary['fixtureCount']}`",
        "",
        "R3 does not change Android runtime behavior, validators, report gates, graph-count expectations, chromatographic math, model policy, or CalculationEngine.",
        "",
        "Contact sheet: `benchmark/reports/r3_image_preparation_candidate/contact_sheet.png`",
        "",
        "## Fixture Results",
        "",
        "| Fixture | Status | Selected variant | Score | Size | Contrast | Edge density | Warnings |",
        "|---|---|---|---:|---|---:|---:|---|",
    ]
    for detail in summary["records"]:
        metrics = detail["sourceMetrics"]
        warnings = ", ".join(detail["warnings"]) if detail["warnings"] else "NONE"
        size = f"{metrics['width']}x{metrics['height']}"
        lines.append(
            "| `{fixtureId}` | {status} | `{selectedVariantId}` | {selectedVariantScore} | {size} | {contrast} | {edge} | {warnings} |".format(
                fixtureId=detail["fixtureId"],
                status=detail["status"],
                selectedVariantId=detail["selectedVariantId"],
                selectedVariantScore=detail["selectedVariantScore"],
                size=size,
                contrast=metrics["contrastP90P10"],
                edge=metrics["edgeDensity"],
                warnings=warnings,
            )
        )
    lines.extend(
        [
            "",
            "## Next Required Work",
            "",
        ]
    )
    for item in summary["nextRequiredWork"]:
        lines.append(f"- {item}")
    lines.append("")
    write_text(REPORT_ROOT / "summary.md", "\n".join(lines))


def create_contact_sheet(details: list[dict[str, Any]]) -> str:
    tiles: list[tuple[str, Image.Image]] = []
    for detail in details:
        for key, label in [("sourcePreview", "source"), ("selectedPreview", "selected")]:
            path = Path(detail[key])
            image = Image.open(path).convert("RGB")
            canvas = Image.new("RGB", (260, 210), "white")
            image.thumbnail((250, 170), Image.Resampling.LANCZOS)
            canvas.paste(image, ((260 - image.width) // 2, 5))
            tiles.append((f"{detail['fixtureId']}\n{label}", canvas))

    columns = 4
    tile_w, tile_h = 260, 235
    rows = math.ceil(len(tiles) / columns)
    sheet = Image.new("RGB", (columns * tile_w, rows * tile_h), "white")
    for index, (_label, tile) in enumerate(tiles):
        x = (index % columns) * tile_w
        y = (index // columns) * tile_h
        sheet.paste(tile, (x, y))
    out = REPORT_ROOT / "contact_sheet.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    return str(out).replace("\\", "/")


def write_examples(records: list[dict[str, Any]]) -> None:
    EXAMPLE_ROOT.mkdir(parents=True, exist_ok=True)
    for record in records:
        write_json(EXAMPLE_ROOT / record["recordId"] / "stage123-parity-record.json", record)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--clean", action="store_true", help="Remove previous R3 examples/report before writing.")
    args = parser.parse_args(argv)

    if args.clean:
        for path in [EXAMPLE_ROOT, REPORT_ROOT]:
            if path.exists():
                shutil.rmtree(path)

    metadata = load_metadata()
    records = []
    details = []
    for item in metadata:
        record, detail = build_record(item)
        records.append(record)
        details.append(detail)

    write_examples(records)
    summary = build_summary(details)
    summary["contactSheet"] = create_contact_sheet(details)
    write_json(REPORT_ROOT / "summary.json", summary)
    write_markdown(summary)
    print(
        "R3 image-preparation candidate wrote "
        f"{len(records)} records to {EXAMPLE_ROOT.as_posix()} and report to {REPORT_ROOT.as_posix()}."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
