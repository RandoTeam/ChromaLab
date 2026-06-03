#!/usr/bin/env python3
"""Run preprocessing variants inside axis-owned OCR crop plans.

This is a PC-side benchmark runner only. It reuses the DR-D5 crop planner and
compares OCR preprocessing variants inside graph-owned crop regions. It does
not change Android runtime or production analysis.
"""

from __future__ import annotations

import argparse
import importlib.metadata
import importlib.util
import json
import statistics
import time
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image, ImageFilter, ImageOps


VARIANTS = {
    "rapidocr_rgb_x2_p0_v1": {
        "engine": "rapidocr",
        "scale": 2,
        "pad": 0,
        "mode": "rgb",
        "description": "RapidOCR baseline axis-owned crop x2.",
    },
    "rapidocr_rgb_x3_p2_v1": {
        "engine": "rapidocr",
        "scale": 3,
        "pad": 2,
        "mode": "rgb",
        "description": "RapidOCR padded RGB crop x3.",
    },
    "rapidocr_autocontrast_x3_p4_v1": {
        "engine": "rapidocr",
        "scale": 3,
        "pad": 4,
        "mode": "autocontrast",
        "description": "RapidOCR autocontrast grayscale crop x3.",
    },
    "rapidocr_sharp_autocontrast_x3_p4_v1": {
        "engine": "rapidocr",
        "scale": 3,
        "pad": 4,
        "mode": "sharp_autocontrast",
        "description": "RapidOCR sharpened autocontrast crop x3.",
    },
    "rapidocr_binary65_x3_p4_v1": {
        "engine": "rapidocr",
        "scale": 3,
        "pad": 4,
        "mode": "binary65",
        "description": "RapidOCR percentile binary crop x3.",
    },
    "rapidocr_binary75_x4_p6_v1": {
        "engine": "rapidocr",
        "scale": 4,
        "pad": 6,
        "mode": "binary75",
        "description": "RapidOCR aggressive binary crop x4.",
    },
    "easyocr_rgb_x2_p0_v1": {
        "engine": "easyocr",
        "scale": 2,
        "pad": 0,
        "mode": "rgb",
        "description": "EasyOCR baseline axis-owned crop x2.",
    },
    "easyocr_autocontrast_x3_p4_v1": {
        "engine": "easyocr",
        "scale": 3,
        "pad": 4,
        "mode": "autocontrast",
        "description": "EasyOCR autocontrast grayscale crop x3.",
    },
}


def load_drd5() -> Any:
    path = Path(__file__).with_name("run_drd5_axis_owned_ocr_crop_planner.py")
    spec = importlib.util.spec_from_file_location("drd5_axis_owned_ocr_crop_planner", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Unable to load DR-D5 helpers from {path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def read_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def package_versions() -> dict[str, str | None]:
    names = ["rapidocr", "onnxruntime", "easyocr", "torch", "torchvision", "opencv-python-headless", "numpy", "Pillow"]
    versions: dict[str, str | None] = {}
    for name in names:
        try:
            versions[name] = importlib.metadata.version(name)
        except importlib.metadata.PackageNotFoundError:
            versions[name] = None
    return versions


def create_engines() -> dict[str, Any]:
    engines: dict[str, Any] = {}
    if any(variant["engine"] == "rapidocr" for variant in VARIANTS.values()):
        from rapidocr import RapidOCR

        engines["rapidocr"] = RapidOCR()
    if any(variant["engine"] == "easyocr" for variant in VARIANTS.values()):
        import easyocr

        engines["easyocr"] = easyocr.Reader(["en"], gpu=False, verbose=False)
    return engines


def expand_crop_box(
    drd5: Any,
    crop: dict[str, Any],
    pad: int,
    image_width: int,
    image_height: int,
) -> tuple[float, float, float, float]:
    box = crop["bboxTuple"]
    expanded = (box[0] - pad, box[1] - pad, box[2] + pad, box[3] + pad)
    clamped = drd5.clamp_bbox(expanded, image_width, image_height)
    if clamped is None:
        return box
    return clamped


def preprocess_crop(crop_image: Image.Image, mode: str) -> Image.Image:
    if mode == "rgb":
        return crop_image.convert("RGB")
    if mode == "autocontrast":
        return ImageOps.autocontrast(crop_image.convert("L")).convert("RGB")
    if mode == "sharp_autocontrast":
        sharpened = crop_image.filter(ImageFilter.SHARPEN).filter(ImageFilter.SHARPEN)
        return ImageOps.autocontrast(sharpened.convert("L")).convert("RGB")
    if mode.startswith("binary"):
        percentile = int(mode.replace("binary", ""))
        gray = ImageOps.autocontrast(crop_image.convert("L"))
        arr = np.array(gray)
        cutoff = max(80, min(225, int(np.percentile(arr, percentile))))
        binary = (arr > cutoff).astype(np.uint8) * 255
        return Image.fromarray(binary, mode="L").convert("RGB")
    raise ValueError(f"Unknown preprocessing mode: {mode}")


def rapidocr_detections(drd5: Any, engine: Any, image: Image.Image) -> list[dict[str, Any]]:
    output = engine(np.array(image.convert("RGB")), use_det=True, use_cls=True, use_rec=True)
    boxes = getattr(output, "boxes", None)
    texts = getattr(output, "txts", None)
    scores = getattr(output, "scores", None)
    boxes = [] if boxes is None else list(boxes)
    texts = [] if texts is None else list(texts)
    scores = [] if scores is None else list(scores)
    detections = []
    for index, box in enumerate(boxes):
        detections.append(
            {
                "localDetectionId": f"rapidocr_det_{index + 1}",
                "text": str(texts[index]) if index < len(texts) else "",
                "confidence": round(float(scores[index]), 4) if index < len(scores) and scores[index] is not None else 0.0,
                "localBboxTuple": drd5.bbox_from_polygon(box),
            }
        )
    return detections


def easyocr_detections(drd5: Any, engine: Any, image: Image.Image) -> list[dict[str, Any]]:
    output = engine.readtext(np.array(image.convert("RGB")), detail=1, paragraph=False)
    detections = []
    for index, item in enumerate(output):
        box, text, score = item
        detections.append(
            {
                "localDetectionId": f"easyocr_det_{index + 1}",
                "text": str(text),
                "confidence": round(float(score), 4),
                "localBboxTuple": drd5.bbox_from_polygon(box),
            }
        )
    return detections


def run_variant_on_crop(
    drd5: Any,
    variant_id: str,
    engine: Any,
    image: Image.Image,
    crop: dict[str, Any],
) -> tuple[list[dict[str, Any]], float]:
    variant = VARIANTS[variant_id]
    image_width, image_height = image.size
    source_box = expand_crop_box(drd5, crop, int(variant["pad"]), image_width, image_height)
    scale = int(variant["scale"])
    crop_image = image.crop(tuple(int(round(value)) for value in source_box)).convert("RGB")
    if scale > 1:
        crop_image = crop_image.resize((crop_image.width * scale, crop_image.height * scale), Image.Resampling.LANCZOS)
    crop_image = preprocess_crop(crop_image, str(variant["mode"]))

    started = time.perf_counter()
    if variant["engine"] == "rapidocr":
        local_detections = rapidocr_detections(drd5, engine, crop_image)
    else:
        local_detections = easyocr_detections(drd5, engine, crop_image)
    elapsed = time.perf_counter() - started

    detections: list[dict[str, Any]] = []
    for index, detection in enumerate(local_detections):
        local_box = detection["localBboxTuple"]
        original_box = (
            source_box[0] + local_box[0] / scale,
            source_box[1] + local_box[1] / scale,
            source_box[0] + local_box[2] / scale,
            source_box[1] + local_box[3] / scale,
        )
        detections.append(
            {
                "detectionId": f"{crop['cropId']}_{variant_id}_{index + 1}",
                "methodId": variant_id,
                "fixtureId": crop["fixtureId"],
                "graphId": crop["graphId"],
                "cropId": crop["cropId"],
                "cropType": crop["cropType"],
                "context": crop["context"],
                "ocrText": detection["text"],
                "confidence": detection["confidence"],
                "bboxTuple": original_box,
                "bbox": drd5.bbox_to_json(original_box),
            }
        )
    return detections, elapsed


def unique_owned_labels(drd5: Any, crops: list[dict[str, Any]], truth_labels: list[dict[str, Any]]) -> dict[str, dict[str, Any]]:
    return drd5.unique_owned_labels(crops, truth_labels)


def best_rows_by_truth(drd5: Any, rows: list[dict[str, Any]]) -> dict[str, dict[str, Any]]:
    return drd5.best_rows_by_truth(rows)


def summarize_variant(drd5: Any, variant_id: str, rows: list[dict[str, Any]], crops: list[dict[str, Any]], truth_labels: list[dict[str, Any]]) -> dict[str, Any]:
    selected = [row for row in rows if row["methodId"] == variant_id]
    owned = unique_owned_labels(drd5, crops, truth_labels)
    best = best_rows_by_truth(drd5, selected)
    matched = list(best.values())
    tick_owned = [label for label in owned.values() if label["truthRole"] == "tick_label"]
    tick_matched = [row for row in matched if row["truthRole"] == "tick_label"]
    safe_pass = [row for row in matched if row["safeRoleScore"] == "PASS"]
    safe_false_tick = [row for row in matched if row["safeFalseTickLabel"]]
    unmatched_numeric = [row for row in selected if row.get("unmatchedNumericRejected")]
    elapsed_rows = [row for row in selected if row.get("cropElapsedSummary")]
    return {
        "variantId": variant_id,
        "engine": VARIANTS[variant_id]["engine"],
        "description": VARIANTS[variant_id]["description"],
        "preprocessing": {
            "scale": VARIANTS[variant_id]["scale"],
            "pad": VARIANTS[variant_id]["pad"],
            "mode": VARIANTS[variant_id]["mode"],
        },
        "ownedTruthLabelCount": len(owned),
        "matchedOwnedTruthCount": len(best),
        "ownedBoxRecall": round(len(best) / len(owned), 4) if owned else 0.0,
        "ownedTickLabelCount": len(tick_owned),
        "matchedTickLabelCount": len(tick_matched),
        "tickLabelRecall": round(len(tick_matched) / len(tick_owned), 4) if tick_owned else 0.0,
        "safeRoleAccuracyMatched": round(len(safe_pass) / len(matched), 4) if matched else 0.0,
        "safeFalseTickLabelCount": len(safe_false_tick),
        "unmatchedNumericRejectedCount": len(unmatched_numeric),
        "meanTextSimilarityMatched": round(statistics.mean(row["textSimilarity"] for row in matched), 4) if matched else 0.0,
        "meanCropElapsedSec": round(statistics.mean(row["elapsedSec"] for row in elapsed_rows), 4) if elapsed_rows else 0.0,
    }


def summarize_fixture(
    drd5: Any,
    variant_id: str,
    fixture_id: str,
    rows: list[dict[str, Any]],
    crops: list[dict[str, Any]],
    truth_labels: list[dict[str, Any]],
) -> dict[str, Any]:
    fixture_crops = [crop for crop in crops if crop["fixtureId"] == fixture_id]
    fixture_truth = [label for label in truth_labels if label["fixtureId"] == fixture_id]
    selected = [row for row in rows if row["methodId"] == variant_id and row["fixtureId"] == fixture_id]
    owned = unique_owned_labels(drd5, fixture_crops, fixture_truth)
    best = best_rows_by_truth(drd5, selected)
    matched = list(best.values())
    tick_owned = [label for label in owned.values() if label["truthRole"] == "tick_label"]
    tick_matched = [row for row in matched if row["truthRole"] == "tick_label"]
    return {
        "fixtureId": fixture_id,
        "variantId": variant_id,
        "engine": VARIANTS[variant_id]["engine"],
        "ownedTruthLabelCount": len(owned),
        "matchedOwnedTruthCount": len(best),
        "ownedBoxRecall": round(len(best) / len(owned), 4) if owned else 0.0,
        "ownedTickLabelCount": len(tick_owned),
        "matchedTickLabelCount": len(tick_matched),
        "tickLabelRecall": round(len(tick_matched) / len(tick_owned), 4) if tick_owned else 0.0,
        "safeRoleAccuracyMatched": round(
            sum(1 for row in matched if row["safeRoleScore"] == "PASS") / len(matched),
            4,
        )
        if matched
        else 0.0,
        "safeFalseTickLabelCount": sum(1 for row in matched if row["safeFalseTickLabel"]),
        "unmatchedNumericRejectedCount": sum(1 for row in selected if row.get("unmatchedNumericRejected")),
    }


def best_variant(summaries: list[dict[str, Any]]) -> dict[str, Any]:
    return max(
        summaries,
        key=lambda item: (
            -item["safeFalseTickLabelCount"],
            item["tickLabelRecall"],
            item["safeRoleAccuracyMatched"],
            item["ownedBoxRecall"],
            -item["meanCropElapsedSec"],
        ),
    )


def build_markdown(summary: dict[str, Any]) -> str:
    lines = [
        "# DR-D6 Axis-Owned OCR Crop Preprocessing Grid",
        "",
        f"Verdict: `{summary['overallVerdict']}`",
        f"Fixtures: `{summary['fixtureCount']}`",
        f"Crop plans: `{summary['cropPlanCount']}`",
        f"Best variant: `{summary['bestVariant']['variantId']}`",
        "",
        "## Variant Summary",
        "",
        "| Variant | Engine | Mode | Tick recall | Safe role accuracy | False ticks | Rejected unmatched numeric | Mean crop time |",
        "| --- | --- | --- | ---: | ---: | ---: | ---: | ---: |",
    ]
    for item in summary["variantSummaries"]:
        mode = item["preprocessing"]["mode"]
        lines.append(
            "| `{variantId}` | `{engine}` | `{mode}` | {tickLabelRecall:.4f} | {safeRoleAccuracyMatched:.4f} | "
            "{safeFalseTickLabelCount} | {unmatchedNumericRejectedCount} | {meanCropElapsedSec:.4f}s |".format(
                mode=mode,
                **item,
            )
        )
    lines.extend(
        [
            "",
            "## Best Fixture Results",
            "",
            "| Fixture | Variant | Tick recall | Safe role accuracy | False ticks |",
            "| --- | --- | ---: | ---: | ---: |",
        ]
    )
    best_id = summary["bestVariant"]["variantId"]
    for item in summary["fixtureSummaries"]:
        if item["variantId"] == best_id:
            lines.append(
                "| `{fixtureId}` | `{variantId}` | {tickLabelRecall:.4f} | {safeRoleAccuracyMatched:.4f} | {safeFalseTickLabelCount} |".format(
                    **item
                )
            )
    lines.extend(
        [
            "",
            "## Interpretation",
            "",
            "- Preprocessing can improve individual crop behavior, but the current grid is not acceptance-ready.",
            "- Zero false tick labels remains mandatory; variants with safety failures are rejected.",
            "- `bench_05_tic_plus_ions` remains the hard OCR case because small stacked-panel labels are incomplete.",
            "- The next work should move into DR-E axis-scale/calibration using only safe owned label evidence and explicit missing-label reasons.",
        ]
    )
    return "\n".join(lines) + "\n"


def build_summary(args: argparse.Namespace) -> dict[str, Any]:
    drd5 = load_drd5()
    layout_records = read_json(args.drc3_manual)["records"]
    truth_records = read_json(args.drc4_manual)["records"]
    truth_by_fixture = drd5.records_by_fixture(truth_records)
    layout_records = [record for record in layout_records if record["fixtureId"] in truth_by_fixture]
    if args.max_fixtures:
        layout_records = layout_records[: args.max_fixtures]
        allowed = {record["fixtureId"] for record in layout_records}
        truth_records = [record for record in truth_records if record["fixtureId"] in allowed]

    truth_labels = drd5.iter_truth_labels(truth_records)
    crops = drd5.build_crop_plans(layout_records)
    images = {
        record["fixtureId"]: Image.open(args.repo_root / record["sourceImage"]).convert("RGB")
        for record in layout_records
    }
    truth_by_crop = {crop["cropId"]: drd5.truth_labels_owned_by_crop(crop, truth_labels) for crop in crops}
    engines = create_engines()

    rows: list[dict[str, Any]] = []
    variant_errors: list[dict[str, Any]] = []
    for variant_id, variant in VARIANTS.items():
        engine = engines[variant["engine"]]
        for crop in crops:
            try:
                detections, elapsed = run_variant_on_crop(drd5, variant_id, engine, images[crop["fixtureId"]], crop)
                rows.extend(drd5.match_crop_detections(variant_id, crop, detections, truth_by_crop[crop["cropId"]]))
                rows.append(
                    {
                        "methodId": variant_id,
                        "fixtureId": crop["fixtureId"],
                        "graphId": crop["graphId"],
                        "cropId": crop["cropId"],
                        "cropType": crop["cropType"],
                        "cropElapsedSummary": True,
                        "elapsedSec": round(elapsed, 4),
                    }
                )
            except Exception as exc:  # pragma: no cover - captured in report for per-crop failures.
                variant_errors.append({"variantId": variant_id, "cropId": crop["cropId"], "error": str(exc)})

    variant_summaries = [summarize_variant(drd5, variant_id, rows, crops, truth_labels) for variant_id in VARIANTS]
    best = best_variant(variant_summaries)
    fixture_summaries = [
        summarize_fixture(drd5, variant_id, record["fixtureId"], rows, crops, truth_labels)
        for variant_id in VARIANTS
        for record in layout_records
    ]
    d5_baseline = read_json(args.drd5_summary) if args.drd5_summary.exists() else None
    d5_best_tick_recall = 0.0
    if d5_baseline:
        d5_best_tick_recall = max(item["tickLabelRecall"] for item in d5_baseline.get("methodSummaries", []))

    if best["safeFalseTickLabelCount"] == 0 and best["tickLabelRecall"] >= 0.9 and best["safeRoleAccuracyMatched"] >= 0.9:
        verdict = "AXIS_OWNED_OCR_PREPROCESSING_ACCEPTANCE_CANDIDATE"
    elif best["safeFalseTickLabelCount"] == 0 and best["tickLabelRecall"] > d5_best_tick_recall:
        verdict = "AXIS_OWNED_OCR_PREPROCESSING_IMPROVES_RECALL_NOT_ACCEPTANCE_READY"
    elif best["safeFalseTickLabelCount"] == 0:
        verdict = "AXIS_OWNED_OCR_PREPROCESSING_SAFE_NO_RECALL_GAIN"
    else:
        verdict = "AXIS_OWNED_OCR_PREPROCESSING_SAFETY_FAILURE"

    return {
        "schemaVersion": "chromalab.benchmark.drd6_axis_owned_ocr_preprocessing_grid.v1",
        "overallVerdict": verdict,
        "productionImpact": "NONE_RESEARCH_ONLY",
        "layoutTruthSource": str(args.drc3_manual).replace("\\", "/"),
        "textRoleTruthSource": str(args.drc4_manual).replace("\\", "/"),
        "drd5BaselineSummary": str(args.drd5_summary).replace("\\", "/") if args.drd5_summary.exists() else None,
        "fixtureCount": len(layout_records),
        "cropPlanCount": len(crops),
        "packageVersions": package_versions(),
        "variants": VARIANTS,
        "variantErrors": variant_errors,
        "variantSummaries": variant_summaries,
        "bestVariant": best,
        "drd5BestTickRecall": round(d5_best_tick_recall, 4),
        "fixtureSummaries": fixture_summaries,
        "detectionScores": [
            row
            for row in rows
            if not row.get("cropElapsedSummary")
        ],
        "nextRequiredCapabilities": [
            "axis_scale_candidate_builder_from_safe_owned_ocr",
            "missing_label_reason_export",
            "calibration_fit_from_incomplete_but_safe_label_sets",
            "automatic_label_band_generation_without_annotation_geometry",
        ],
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo-root", type=Path, default=Path("."))
    parser.add_argument(
        "--drc3-manual",
        type=Path,
        default=Path("benchmark/annotations/drc3_initial_graph_layout_annotations/manual-p0-annotations.json"),
    )
    parser.add_argument(
        "--drc4-manual",
        type=Path,
        default=Path("benchmark/annotations/drc4_tick_text_role_annotations/manual-p0-tick-text-annotations.json"),
    )
    parser.add_argument(
        "--drd5-summary",
        type=Path,
        default=Path("benchmark/reports/drd5_axis_owned_ocr_crop_planner/summary.json"),
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("benchmark/reports/drd6_axis_owned_ocr_preprocessing_grid"),
    )
    parser.add_argument("--max-fixtures", type=int, default=None)
    args = parser.parse_args()
    summary = build_summary(args)
    write_json(args.output / "summary.json", summary)
    (args.output / "summary.md").write_text(build_markdown(summary), encoding="utf-8", newline="\n")
    print(f"Built DR-D6 axis-owned OCR preprocessing grid: {summary['overallVerdict']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
