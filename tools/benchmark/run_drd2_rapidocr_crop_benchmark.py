#!/usr/bin/env python3
"""Run RapidOCR crop OCR against DR-C4 P0 text-role annotations.

This is a PC-side benchmark runner only. DR-C4 boxes are used as benchmark crop
targets, not as runtime graph coordinates.
"""

from __future__ import annotations

import argparse
import importlib.metadata
import json
import re
import statistics
import time
from collections import Counter
from difflib import SequenceMatcher
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image, ImageOps


METHODS = {
    "rapidocr_rec_padded_x3_v1": {
        "scale": 3,
        "pad": 6,
        "threshold": False,
        "description": "RapidOCR recognition-only on padded x3 crop.",
    },
    "rapidocr_rec_binary_x4_v1": {
        "scale": 4,
        "pad": 8,
        "threshold": True,
        "description": "RapidOCR recognition-only on padded x4 autocontrast binary crop.",
    },
}


def read_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def normalize_text(text: str | None) -> str:
    return re.sub(r"\s+", " ", str(text or "").strip().lower())


def normalize_compact(text: str | None) -> str:
    value = normalize_text(text)
    value = value.replace("，", ",").replace("。", ".")
    value = value.replace(",", ".")
    value = re.sub(r"\s+", "", value)
    return value


def text_similarity(a: str | None, b: str | None) -> float:
    return SequenceMatcher(None, normalize_compact(a), normalize_compact(b)).ratio()


def has_digit(text: str | None) -> bool:
    return bool(re.search(r"\d", str(text or "")))


def is_numeric_tick_text(text: str | None) -> bool:
    return bool(re.fullmatch(r"\s*[+-]?\d+(?:[.,]\d+)?\s*", str(text or "")))


def classify_text_role(text: str | None) -> str:
    value = normalize_text(text)
    compact = value.replace(" ", "")
    if not value:
        return "missing_text"
    if value.startswith("tic:") or value == "tic":
        return "chart_title"
    if value.startswith("ion "):
        return "ion_or_mz_metadata"
    if value.startswith("xic("):
        return "legend"
    if (
        "abundance" in value
        or "time" in value
        or "время" in value
        or "интенсивность" in value
    ):
        return "axis_title"
    if compact.endswith("да") and has_digit(value):
        return "other"
    if is_numeric_tick_text(value):
        return "tick_label"
    return "other"


def iter_text_labels(records: list[dict[str, Any]]) -> list[dict[str, Any]]:
    labels: list[dict[str, Any]] = []
    for record in records:
        for graph in record.get("graphs", []):
            for label in graph.get("textRoleLabels", []):
                labels.append(
                    {
                        "fixtureId": record["fixtureId"],
                        "sourceImage": record["sourceImage"],
                        "graphId": graph["graphId"],
                        "panelGroup": graph.get("panelGroup"),
                        "textId": label.get("textId") or label.get("labelId"),
                        "truthText": label.get("text"),
                        "truthRole": label.get("role"),
                        "bbox": label.get("bbox"),
                        "rejectedAsTickLabel": bool(label.get("rejectedAsTickLabel")),
                        "rejectionReason": label.get("rejectionReason"),
                    }
                )
    return labels


def crop_label(image: Image.Image, bbox: dict[str, Any], *, pad: int, scale: int, threshold: bool) -> Image.Image:
    x0 = max(0, int(round(float(bbox["x"]))) - pad)
    y0 = max(0, int(round(float(bbox["y"]))) - pad)
    x1 = min(image.width, int(round(float(bbox["x"] + bbox["width"]))) + pad)
    y1 = min(image.height, int(round(float(bbox["y"] + bbox["height"]))) + pad)
    crop = image.crop((x0, y0, x1, y1)).convert("RGB")
    if scale > 1:
        crop = crop.resize((crop.width * scale, crop.height * scale), Image.Resampling.LANCZOS)
    if threshold:
        gray = ImageOps.autocontrast(crop.convert("L"))
        arr = np.array(gray)
        cutoff = max(80, min(210, int(np.percentile(arr, 65))))
        binary = (arr > cutoff).astype(np.uint8) * 255
        crop = Image.fromarray(binary, mode="L").convert("RGB")
    return crop


def rapidocr_text(output: Any) -> tuple[str, float, float]:
    txts = tuple(str(text) for text in getattr(output, "txts", ()) if str(text).strip())
    scores = tuple(float(score) for score in getattr(output, "scores", ()) if score is not None)
    text = " ".join(txts).strip()
    confidence = statistics.mean(scores) if scores else 0.0
    elapsed = float(getattr(output, "elapse", 0.0) or 0.0)
    return text, confidence, elapsed


def run_ocr_for_method(ocr: Any, image: Image.Image, label: dict[str, Any], method_id: str) -> dict[str, Any]:
    method = METHODS[method_id]
    crop = crop_label(
        image,
        label["bbox"],
        pad=int(method["pad"]),
        scale=int(method["scale"]),
        threshold=bool(method["threshold"]),
    )
    started = time.perf_counter()
    output = ocr(np.array(crop), use_det=False, use_cls=True, use_rec=True)
    wall_time = time.perf_counter() - started
    text, confidence, engine_elapsed = rapidocr_text(output)
    predicted_role = classify_text_role(text)
    truth_text = label["truthText"]
    truth_role = str(label["truthRole"])
    return {
        "methodId": method_id,
        "fixtureId": label["fixtureId"],
        "graphId": label["graphId"],
        "textId": label["textId"],
        "truthText": truth_text,
        "ocrText": text,
        "truthRole": truth_role,
        "predictedRole": predicted_role,
        "textExactMatch": normalize_compact(text) == normalize_compact(truth_text),
        "textSimilarity": round(text_similarity(text, truth_text), 4),
        "roleScore": "PASS" if predicted_role == truth_role else "FAIL",
        "falseTickLabel": predicted_role == "tick_label" and truth_role != "tick_label",
        "forbiddenNumericText": truth_role != "tick_label" and has_digit(truth_text),
        "forbiddenNumericRejected": not (predicted_role == "tick_label")
        if truth_role != "tick_label" and has_digit(truth_text)
        else None,
        "confidence": round(confidence, 4),
        "engineElapsedSec": round(engine_elapsed, 4),
        "wallElapsedSec": round(wall_time, 4),
        "crop": {
            "pad": method["pad"],
            "scale": method["scale"],
            "threshold": method["threshold"],
            "sourceBbox": label["bbox"],
        },
    }


def package_versions() -> dict[str, str | None]:
    names = ["rapidocr", "onnxruntime", "opencv-python", "numpy", "Pillow"]
    versions: dict[str, str | None] = {}
    for name in names:
        try:
            versions[name] = importlib.metadata.version(name)
        except importlib.metadata.PackageNotFoundError:
            versions[name] = None
    return versions


def rapidocr_model_files() -> list[dict[str, Any]]:
    try:
        import rapidocr
    except Exception:
        return []
    root = Path(rapidocr.__file__).resolve().parent / "models"
    if not root.exists():
        return []
    files = []
    for path in sorted(root.glob("*.onnx")):
        files.append(
            {
                "path": str(path).replace("\\", "/"),
                "sizeBytes": path.stat().st_size,
            }
        )
    return files


def summarize_method(method_id: str, scores: list[dict[str, Any]]) -> dict[str, Any]:
    selected = [score for score in scores if score["methodId"] == method_id]
    total = len(selected)
    non_empty = [score for score in selected if score["ocrText"]]
    exact = [score for score in selected if score["textExactMatch"]]
    role_pass = [score for score in selected if score["roleScore"] == "PASS"]
    false_tick = [score for score in selected if score["falseTickLabel"]]
    forbidden = [score for score in selected if score["forbiddenNumericText"]]
    forbidden_rejected = [score for score in forbidden if score["forbiddenNumericRejected"]]
    return {
        "methodId": method_id,
        "description": METHODS[method_id]["description"],
        "labelCount": total,
        "nonEmptyOcrCount": len(non_empty),
        "nonEmptyOcrRate": round(len(non_empty) / total, 4) if total else 0.0,
        "exactTextMatchCount": len(exact),
        "exactTextMatchRate": round(len(exact) / total, 4) if total else 0.0,
        "meanTextSimilarity": round(statistics.mean(score["textSimilarity"] for score in selected), 4)
        if selected
        else 0.0,
        "rolePassCount": len(role_pass),
        "roleAccuracy": round(len(role_pass) / total, 4) if total else 0.0,
        "falseTickLabelCount": len(false_tick),
        "forbiddenNumericTextCount": len(forbidden),
        "forbiddenNumericRejectedCount": len(forbidden_rejected),
        "forbiddenNumericRejectedRate": round(len(forbidden_rejected) / len(forbidden), 4)
        if forbidden
        else 1.0,
        "meanWallElapsedSec": round(statistics.mean(score["wallElapsedSec"] for score in selected), 4)
        if selected
        else 0.0,
        "roleConfusion": dict(
            Counter(f"{score['truthRole']}->{score['predictedRole']}" for score in selected)
        ),
    }


def summarize_fixture_features(method_id: str, scores: list[dict[str, Any]]) -> list[dict[str, Any]]:
    by_fixture: dict[str, list[dict[str, Any]]] = {}
    for score in scores:
        if score["methodId"] == method_id:
            by_fixture.setdefault(score["fixtureId"], []).append(score)

    out = []
    for fixture_id, fixture_scores in by_fixture.items():
        truth_has_tic = any(
            score["truthRole"] == "chart_title" and "tic" in normalize_text(score["truthText"])
            for score in fixture_scores
        )
        predicted_has_tic = any(
            score["predictedRole"] == "chart_title" and "tic" in normalize_text(score["ocrText"])
            for score in fixture_scores
        )
        truth_has_ion = any(score["truthRole"] == "ion_or_mz_metadata" for score in fixture_scores)
        predicted_has_ion = any(score["predictedRole"] == "ion_or_mz_metadata" for score in fixture_scores)
        truth_has_legend = any(score["truthRole"] == "legend" for score in fixture_scores)
        predicted_has_legend = any(score["predictedRole"] == "legend" for score in fixture_scores)
        false_tick = sum(1 for score in fixture_scores if score["falseTickLabel"])
        out.append(
            {
                "methodId": method_id,
                "fixtureId": fixture_id,
                "ticTitleFeatureScore": "PASS" if truth_has_tic == predicted_has_tic else "FAIL",
                "ionOrMzFeatureScore": "PASS" if truth_has_ion == predicted_has_ion else "FAIL",
                "legendFeatureScore": "PASS" if truth_has_legend == predicted_has_legend else "FAIL",
                "forbiddenNumericSafetyScore": "PASS" if false_tick == 0 else "FAIL",
                "predicted": {
                    "hasTicTitle": predicted_has_tic,
                    "hasIonOrMzMetadata": predicted_has_ion,
                    "hasLegend": predicted_has_legend,
                    "falseTickLabelCount": false_tick,
                },
                "truth": {
                    "hasTicTitle": truth_has_tic,
                    "hasIonOrMzMetadata": truth_has_ion,
                    "hasLegend": truth_has_legend,
                },
            }
        )
    return out


def build_markdown(summary: dict[str, Any]) -> str:
    lines = [
        "# DR-D2 RapidOCR Crop OCR Benchmark",
        "",
        f"Verdict: `{summary['overallVerdict']}`",
        f"Text labels: `{summary['textLabelCount']}`",
        "",
        "## Installed OCR Stack",
        "",
        "| Package | Version |",
        "| --- | --- |",
    ]
    for name, version in summary["packageVersions"].items():
        lines.append(f"| `{name}` | `{version}` |")
    lines.extend(
        [
            "",
            "## Method Summary",
            "",
            "| Method | Non-empty OCR | Exact text | Mean similarity | Role accuracy | False tick labels | Forbidden numeric rejected | Mean time/label |",
            "| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |",
        ]
    )
    for method in summary["methodSummaries"]:
        lines.append(
            "| `{methodId}` | {nonEmptyOcrCount}/{labelCount} | {exactTextMatchCount}/{labelCount} | "
            "{meanTextSimilarity:.4f} | {rolePassCount}/{labelCount} | {falseTickLabelCount} | "
            "{forbiddenNumericRejectedCount}/{forbiddenNumericTextCount} | {meanWallElapsedSec:.4f}s |".format(
                **method
            )
        )
    lines.extend(
        [
            "",
            "## Fixture Feature Scores",
            "",
            "| Method | Fixture | TIC title | Ion/mz metadata | Legend | Forbidden numeric safety |",
            "| --- | --- | --- | --- | --- | --- |",
        ]
    )
    for score in summary["fixtureFeatureScores"]:
        lines.append(
            "| `{methodId}` | `{fixtureId}` | {ticTitleFeatureScore} | {ionOrMzFeatureScore} | "
            "{legendFeatureScore} | {forbiddenNumericSafetyScore} |".format(**score)
        )
    lines.extend(
        [
            "",
            "## Interpretation",
            "",
            "- RapidOCR is now installed and model loading/download has been verified.",
            "- Recognition-only crop OCR is fast enough for PC benchmarking, but text accuracy is not yet sufficient for runtime acceptance.",
            "- The benchmark exposes exact text, role, and forbidden-numeric safety scores without changing production code.",
            "- Next work should improve crop generation/preprocessing and compare another OCR engine or OCR variant before Android parity.",
        ]
    )
    return "\n".join(lines) + "\n"


def build_summary(args: argparse.Namespace) -> dict[str, Any]:
    try:
        from rapidocr import RapidOCR
    except ImportError as exc:
        raise SystemExit("RapidOCR is not installed. Run: python -m pip install -r tools/benchmark/ocr-requirements.txt") from exc

    records = read_json(args.drc4_manual)["records"]
    labels = iter_text_labels(records)
    images: dict[str, Image.Image] = {}
    for label in labels:
        source = str(label["sourceImage"])
        if source not in images:
            images[source] = Image.open(args.repo_root / source).convert("RGB")

    ocr = RapidOCR()
    scores = []
    for method_id in METHODS:
        for label in labels:
            scores.append(run_ocr_for_method(ocr, images[str(label["sourceImage"])], label, method_id))

    method_summaries = [summarize_method(method_id, scores) for method_id in METHODS]
    fixture_scores = [
        score
        for method_id in METHODS
        for score in summarize_fixture_features(method_id, scores)
    ]
    best = max(method_summaries, key=lambda method: (method["roleAccuracy"], method["meanTextSimilarity"]))
    verdict = (
        "RAPIDOCR_INSTALLED_CROP_OCR_WORKS_BUT_NOT_ACCEPTANCE_READY"
        if best["nonEmptyOcrCount"] > 0
        else "RAPIDOCR_INSTALLED_BUT_NO_TEXT_RECOGNIZED"
    )
    return {
        "schemaVersion": "chromalab.benchmark.drd2_rapidocr_crop_ocr_benchmark.v1",
        "overallVerdict": verdict,
        "productionImpact": "NONE_RESEARCH_ONLY",
        "textRoleTruthSource": str(args.drc4_manual).replace("\\", "/"),
        "textLabelCount": len(labels),
        "packageVersions": package_versions(),
        "rapidocrModelFiles": rapidocr_model_files(),
        "methodSummaries": method_summaries,
        "fixtureFeatureScores": fixture_scores,
        "labelScores": scores,
        "bestMethodId": best["methodId"],
        "nextRequiredCapabilities": [
            "crop_preprocessing_grid_search",
            "OCR_text_box_detection_benchmark",
            "compare_RapidOCR_with_PaddleOCR_or_Tesseract",
            "panel_title_and_axis_band_crop_generation_without_truth_boxes",
        ],
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo-root", type=Path, default=Path("."))
    parser.add_argument(
        "--drc4-manual",
        type=Path,
        default=Path("benchmark/annotations/drc4_tick_text_role_annotations/manual-p0-tick-text-annotations.json"),
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("benchmark/reports/drd2_rapidocr_crop_benchmark"),
    )
    args = parser.parse_args()
    summary = build_summary(args)
    write_json(args.output / "summary.json", summary)
    (args.output / "summary.md").write_text(build_markdown(summary), encoding="utf-8")
    print(f"Built DR-D2 RapidOCR crop benchmark: {summary['overallVerdict']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
