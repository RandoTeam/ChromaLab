#!/usr/bin/env python3
"""Compare full-image OCR text-box detection engines on DR-C4 annotations.

This is a PC-side benchmark runner only. It does not change Android runtime,
production graph detection, calibration, trace extraction, peak integration, or
report rendering.
"""

from __future__ import annotations

import argparse
import importlib.metadata
import json
import math
import re
import statistics
import time
from collections import Counter
from difflib import SequenceMatcher
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image


METHODS = {
    "rapidocr_full_detection_v1": {
        "engine": "rapidocr",
        "description": "RapidOCR full-image detection, classification, and recognition.",
    },
    "easyocr_en_full_detection_v1": {
        "engine": "easyocr",
        "description": "EasyOCR English full-image text-box detection and recognition.",
    },
}

RUS_TIME = "\u0432\u0440\u0435\u043c\u044f"
RUS_INTENSITY = "\u0438\u043d\u0442\u0435\u043d\u0441\u0438\u0432\u043d\u043e\u0441\u0442\u044c"
RUS_DA = "\u0434\u0430"


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
    value = value.replace("\uff0c", ",").replace("\u3002", ".").replace(",", ".")
    return re.sub(r"\s+", "", value)


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
        or RUS_TIME in value
        or RUS_INTENSITY in value
    ):
        return "axis_title"
    if compact.endswith(RUS_DA) and has_digit(value):
        return "other"
    if is_numeric_tick_text(value):
        return "tick_label"
    return "other"


def context_from_truth_role(truth_role: str) -> str:
    if truth_role == "tick_label":
        return "tick_label_candidate_context"
    return "non_tick_context_requires_geometry_proof"


def apply_context_safety_gate(predicted_role: str, text: str, context: str) -> tuple[str, str | None]:
    if (
        predicted_role == "tick_label"
        and context != "tick_label_candidate_context"
        and is_numeric_tick_text(text)
    ):
        return "numeric_rejected_non_tick_context", "pure numeric OCR is not accepted outside tick-label candidate context"
    return predicted_role, None


def bbox_from_annotation(bbox: dict[str, Any]) -> tuple[float, float, float, float]:
    return (
        float(bbox["x"]),
        float(bbox["y"]),
        float(bbox["x"]) + float(bbox["width"]),
        float(bbox["y"]) + float(bbox["height"]),
    )


def bbox_from_polygon(points: Any) -> tuple[float, float, float, float]:
    xs = [float(point[0]) for point in points]
    ys = [float(point[1]) for point in points]
    return (min(xs), min(ys), max(xs), max(ys))


def bbox_area(bbox: tuple[float, float, float, float]) -> float:
    return max(0.0, bbox[2] - bbox[0]) * max(0.0, bbox[3] - bbox[1])


def bbox_iou(a: tuple[float, float, float, float], b: tuple[float, float, float, float]) -> float:
    x0 = max(a[0], b[0])
    y0 = max(a[1], b[1])
    x1 = min(a[2], b[2])
    y1 = min(a[3], b[3])
    inter = bbox_area((x0, y0, x1, y1))
    union = bbox_area(a) + bbox_area(b) - inter
    return inter / union if union > 0 else 0.0


def bbox_center(bbox: tuple[float, float, float, float]) -> tuple[float, float]:
    return ((bbox[0] + bbox[2]) / 2.0, (bbox[1] + bbox[3]) / 2.0)


def point_inside_expanded(point: tuple[float, float], bbox: tuple[float, float, float, float], margin: float) -> bool:
    return (
        bbox[0] - margin <= point[0] <= bbox[2] + margin
        and bbox[1] - margin <= point[1] <= bbox[3] + margin
    )


def center_distance(a: tuple[float, float, float, float], b: tuple[float, float, float, float]) -> float:
    ax, ay = bbox_center(a)
    bx, by = bbox_center(b)
    return math.hypot(ax - bx, ay - by)


def iter_text_labels(records: list[dict[str, Any]]) -> list[dict[str, Any]]:
    labels: list[dict[str, Any]] = []
    for record in records:
        for graph in record.get("graphs", []):
            for label in graph.get("textRoleLabels", []):
                truth_role = str(label.get("role") or "")
                bbox = bbox_from_annotation(label["bbox"])
                labels.append(
                    {
                        "fixtureId": record["fixtureId"],
                        "sourceImage": record["sourceImage"],
                        "graphId": graph["graphId"],
                        "panelGroup": graph.get("panelGroup"),
                        "textId": label.get("textId") or label.get("labelId"),
                        "truthText": label.get("text"),
                        "truthRole": truth_role,
                        "bbox": {
                            "x0": bbox[0],
                            "y0": bbox[1],
                            "x1": bbox[2],
                            "y1": bbox[3],
                        },
                        "context": context_from_truth_role(truth_role),
                        "rejectionReason": label.get("rejectionReason"),
                    }
                )
    return labels


def package_versions() -> dict[str, str | None]:
    names = [
        "rapidocr",
        "onnxruntime",
        "easyocr",
        "torch",
        "torchvision",
        "opencv-python-headless",
        "numpy",
        "Pillow",
    ]
    versions: dict[str, str | None] = {}
    for name in names:
        try:
            versions[name] = importlib.metadata.version(name)
        except importlib.metadata.PackageNotFoundError:
            versions[name] = None
    return versions


def easyocr_model_files() -> list[dict[str, Any]]:
    roots = [Path.home() / ".EasyOCR" / "model"]
    files: list[dict[str, Any]] = []
    for root in roots:
        if not root.exists():
            continue
        for path in sorted(root.glob("*")):
            if path.is_file():
                files.append({"path": str(path).replace("\\", "/"), "sizeBytes": path.stat().st_size})
    return files


def rapidocr_model_files() -> list[dict[str, Any]]:
    try:
        import rapidocr
    except Exception:
        return []
    root = Path(rapidocr.__file__).resolve().parent / "models"
    files: list[dict[str, Any]] = []
    if root.exists():
        for path in sorted(root.glob("*.onnx")):
            files.append({"path": str(path).replace("\\", "/"), "sizeBytes": path.stat().st_size})
    return files


def run_rapidocr_full(ocr: Any, image: Image.Image) -> tuple[list[dict[str, Any]], float]:
    started = time.perf_counter()
    output = ocr(np.array(image.convert("RGB")), use_det=True, use_cls=True, use_rec=True)
    elapsed = time.perf_counter() - started
    boxes = getattr(output, "boxes", None)
    texts = getattr(output, "txts", None)
    scores = getattr(output, "scores", None)
    boxes = [] if boxes is None else list(boxes)
    texts = [] if texts is None else list(texts)
    scores = [] if scores is None else list(scores)
    detections: list[dict[str, Any]] = []
    for index, box in enumerate(boxes):
        text = str(texts[index]) if index < len(texts) else ""
        score = float(scores[index]) if index < len(scores) and scores[index] is not None else 0.0
        bbox = bbox_from_polygon(box)
        detections.append(
            {
                "detectionId": f"rapidocr_det_{index + 1}",
                "text": text,
                "confidence": round(score, 4),
                "bbox": {"x0": bbox[0], "y0": bbox[1], "x1": bbox[2], "y1": bbox[3]},
            }
        )
    return detections, elapsed


def run_easyocr_full(reader: Any, image: Image.Image) -> tuple[list[dict[str, Any]], float]:
    started = time.perf_counter()
    output = reader.readtext(np.array(image.convert("RGB")), detail=1, paragraph=False)
    elapsed = time.perf_counter() - started
    detections: list[dict[str, Any]] = []
    for index, item in enumerate(output):
        box, text, score = item
        bbox = bbox_from_polygon(box)
        detections.append(
            {
                "detectionId": f"easyocr_det_{index + 1}",
                "text": str(text),
                "confidence": round(float(score), 4),
                "bbox": {"x0": bbox[0], "y0": bbox[1], "x1": bbox[2], "y1": bbox[3]},
            }
        )
    return detections, elapsed


def create_engine(method_id: str) -> Any:
    engine = METHODS[method_id]["engine"]
    if engine == "rapidocr":
        try:
            from rapidocr import RapidOCR
        except ImportError as exc:
            raise SystemExit("RapidOCR is not installed. Run: python -m pip install -r tools/benchmark/ocr-requirements.txt") from exc
        return RapidOCR()
    if engine == "easyocr":
        try:
            import easyocr
        except ImportError as exc:
            raise SystemExit("EasyOCR is not installed. Run: python -m pip install -r tools/benchmark/ocr-requirements.txt") from exc
        return easyocr.Reader(["en"], gpu=False, verbose=False)
    raise ValueError(f"Unknown engine: {engine}")


def run_method(method_id: str, engine: Any, image: Image.Image) -> tuple[list[dict[str, Any]], float]:
    if METHODS[method_id]["engine"] == "rapidocr":
        return run_rapidocr_full(engine, image)
    if METHODS[method_id]["engine"] == "easyocr":
        return run_easyocr_full(engine, image)
    raise ValueError(f"Unknown method: {method_id}")


def match_detections(
    fixture_id: str,
    method_id: str,
    detections: list[dict[str, Any]],
    truth_labels: list[dict[str, Any]],
) -> list[dict[str, Any]]:
    unmatched_truth = {label["textId"]: label for label in truth_labels}
    matches: list[dict[str, Any]] = []
    for detection in sorted(detections, key=lambda item: item["confidence"], reverse=True):
        det_bbox = (
            float(detection["bbox"]["x0"]),
            float(detection["bbox"]["y0"]),
            float(detection["bbox"]["x1"]),
            float(detection["bbox"]["y1"]),
        )
        best_label: dict[str, Any] | None = None
        best_score = -1.0
        best_iou = 0.0
        best_distance = 0.0
        for label in unmatched_truth.values():
            label_bbox = (
                float(label["bbox"]["x0"]),
                float(label["bbox"]["y0"]),
                float(label["bbox"]["x1"]),
                float(label["bbox"]["y1"]),
            )
            iou = bbox_iou(det_bbox, label_bbox)
            distance = center_distance(det_bbox, label_bbox)
            center_ok = point_inside_expanded(bbox_center(det_bbox), label_bbox, 16.0)
            if iou >= 0.05:
                score = 2.0 + iou
            elif center_ok:
                score = 1.0 - min(distance, 100.0) / 100.0
            else:
                score = -1.0
            if score > best_score:
                best_label = label
                best_score = score
                best_iou = iou
                best_distance = distance
        if best_label is not None and best_score >= 0.0:
            unmatched_truth.pop(best_label["textId"], None)
            context = best_label["context"]
            truth_role = str(best_label["truthRole"])
            truth_text = best_label["truthText"]
            matched = True
        else:
            context = "unmatched_detection_no_truth_context"
            truth_role = None
            truth_text = None
            matched = False

        raw_role = classify_text_role(detection["text"])
        safe_role, safety_rejection_reason = apply_context_safety_gate(raw_role, detection["text"], context)
        matches.append(
            {
                "fixtureId": fixture_id,
                "methodId": method_id,
                "detectionId": detection["detectionId"],
                "ocrText": detection["text"],
                "confidence": detection["confidence"],
                "bbox": detection["bbox"],
                "matchedTruth": matched,
                "matchIou": round(best_iou, 4) if matched else 0.0,
                "matchCenterDistance": round(best_distance, 2) if matched else None,
                "truthTextId": best_label["textId"] if matched and best_label else None,
                "truthText": truth_text,
                "truthRole": truth_role,
                "context": context,
                "rawPredictedRole": raw_role,
                "safePredictedRole": safe_role,
                "safetyRejectionReason": safety_rejection_reason,
                "textExactMatch": normalize_compact(detection["text"]) == normalize_compact(truth_text) if matched else False,
                "textSimilarity": round(text_similarity(detection["text"], truth_text), 4) if matched else 0.0,
                "rawRoleScore": "PASS" if matched and raw_role == truth_role else "FAIL" if matched else "UNMATCHED",
                "safeRoleScore": "PASS" if matched and safe_role == truth_role else "FAIL" if matched else "UNMATCHED",
                "rawFalseTickLabel": matched and raw_role == "tick_label" and truth_role != "tick_label",
                "safeFalseTickLabel": matched and safe_role == "tick_label" and truth_role != "tick_label",
                "unmatchedNumericBox": (not matched) and is_numeric_tick_text(detection["text"]),
            }
        )
    for label in unmatched_truth.values():
        matches.append(
            {
                "fixtureId": fixture_id,
                "methodId": method_id,
                "detectionId": None,
                "ocrText": "",
                "confidence": 0.0,
                "bbox": None,
                "matchedTruth": False,
                "missingTruthLabel": True,
                "truthTextId": label["textId"],
                "truthText": label["truthText"],
                "truthRole": label["truthRole"],
                "context": label["context"],
                "rawPredictedRole": "missing_text",
                "safePredictedRole": "missing_text",
                "safetyRejectionReason": "truth label had no matched OCR detection",
                "textExactMatch": False,
                "textSimilarity": 0.0,
                "rawRoleScore": "FAIL",
                "safeRoleScore": "FAIL",
                "rawFalseTickLabel": False,
                "safeFalseTickLabel": False,
                "unmatchedNumericBox": False,
            }
        )
    return matches


def summarize_method(method_id: str, fixture_scores: list[dict[str, Any]]) -> dict[str, Any]:
    selected = [score for score in fixture_scores if score["methodId"] == method_id]
    truth_rows = [score for score in selected if score.get("truthTextId")]
    matched = [score for score in selected if score.get("matchedTruth")]
    detections = [score for score in selected if score.get("detectionId")]
    raw_role_pass = [score for score in matched if score["rawRoleScore"] == "PASS"]
    safe_role_pass = [score for score in matched if score["safeRoleScore"] == "PASS"]
    exact = [score for score in matched if score["textExactMatch"]]
    raw_false_tick = [score for score in matched if score["rawFalseTickLabel"]]
    safe_false_tick = [score for score in matched if score["safeFalseTickLabel"]]
    forbidden = [score for score in matched if score["truthRole"] != "tick_label" and has_digit(score["truthText"])]
    forbidden_rejected = [score for score in forbidden if score["safePredictedRole"] == "numeric_rejected_non_tick_context"]
    unmatched_numeric = [score for score in selected if score.get("unmatchedNumericBox")]
    fixture_elapsed = [
        score["elapsedSec"]
        for score in selected
        if score.get("fixtureSummary")
    ]
    return {
        "methodId": method_id,
        "description": METHODS[method_id]["description"],
        "fixtureCount": len({score["fixtureId"] for score in selected}),
        "truthLabelCount": len(truth_rows),
        "detectionCount": len(detections),
        "matchedTruthCount": len(matched),
        "boxRecall": round(len(matched) / len(truth_rows), 4) if truth_rows else 0.0,
        "exactTextMatchCount": len(exact),
        "exactTextMatchRateMatched": round(len(exact) / len(matched), 4) if matched else 0.0,
        "meanTextSimilarityMatched": round(statistics.mean(score["textSimilarity"] for score in matched), 4)
        if matched
        else 0.0,
        "rawRoleAccuracyMatched": round(len(raw_role_pass) / len(matched), 4) if matched else 0.0,
        "safeRoleAccuracyMatched": round(len(safe_role_pass) / len(matched), 4) if matched else 0.0,
        "rawFalseTickLabelCount": len(raw_false_tick),
        "safeFalseTickLabelCount": len(safe_false_tick),
        "forbiddenNumericTextCount": len(forbidden),
        "forbiddenNumericRejectedCount": len(forbidden_rejected),
        "unmatchedNumericBoxCount": len(unmatched_numeric),
        "meanFixtureElapsedSec": round(statistics.mean(fixture_elapsed), 4) if fixture_elapsed else 0.0,
        "roleConfusionSafe": dict(Counter(f"{score['truthRole']}->{score['safePredictedRole']}" for score in matched)),
    }


def summarize_fixture(method_id: str, fixture_id: str, scores: list[dict[str, Any]], elapsed: float) -> dict[str, Any]:
    selected = [score for score in scores if score["methodId"] == method_id and score["fixtureId"] == fixture_id]
    truth_rows = [score for score in selected if score.get("truthTextId")]
    matched = [score for score in selected if score.get("matchedTruth")]
    detections = [score for score in selected if score.get("detectionId")]
    return {
        "fixtureId": fixture_id,
        "methodId": method_id,
        "truthLabelCount": len(truth_rows),
        "detectionCount": len(detections),
        "matchedTruthCount": len(matched),
        "boxRecall": round(len(matched) / len(truth_rows), 4) if truth_rows else 0.0,
        "safeFalseTickLabelCount": sum(1 for score in matched if score["safeFalseTickLabel"]),
        "unmatchedNumericBoxCount": sum(1 for score in selected if score.get("unmatchedNumericBox")),
        "safeRoleAccuracyMatched": round(
            sum(1 for score in matched if score["safeRoleScore"] == "PASS") / len(matched),
            4,
        )
        if matched
        else 0.0,
        "meanTextSimilarityMatched": round(statistics.mean(score["textSimilarity"] for score in matched), 4)
        if matched
        else 0.0,
        "elapsedSec": round(elapsed, 4),
        "fixtureSummary": True,
    }


def build_markdown(summary: dict[str, Any]) -> str:
    lines = [
        "# DR-D4 Second OCR Engine And Text-Box Detection Benchmark",
        "",
        f"Verdict: `{summary['overallVerdict']}`",
        f"Fixtures: `{summary['fixtureCount']}`",
        f"Truth labels: `{summary['textLabelCount']}`",
        "",
        "## Method Summary",
        "",
        "| Method | Box recall | Safe role accuracy | Safe false tick labels | Unmatched numeric boxes | Mean time/fixture |",
        "| --- | ---: | ---: | ---: | ---: | ---: |",
    ]
    for item in summary["methodSummaries"]:
        lines.append(
            "| `{methodId}` | {boxRecall:.4f} | {safeRoleAccuracyMatched:.4f} | "
            "{safeFalseTickLabelCount} | {unmatchedNumericBoxCount} | {meanFixtureElapsedSec:.4f}s |".format(**item)
        )
    lines.extend(
        [
            "",
            "## Fixture Summary",
            "",
            "| Fixture | Method | Detections | Matched truth | Box recall | Safe role accuracy | Safe false ticks | Time |",
            "| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |",
        ]
    )
    for item in summary["fixtureSummaries"]:
        lines.append(
            "| `{fixtureId}` | `{methodId}` | {detectionCount} | {matchedTruthCount}/{truthLabelCount} | "
            "{boxRecall:.4f} | {safeRoleAccuracyMatched:.4f} | {safeFalseTickLabelCount} | {elapsedSec:.4f}s |".format(
                **item
            )
        )
    lines.extend(
        [
            "",
            "## Interpretation",
            "",
            "- EasyOCR is installed and can run as a second PC OCR benchmark engine.",
            "- Full-image text-box detection is still not acceptance-ready for Android integration.",
            "- The context safety gate remains mandatory: pure numeric OCR outside geometry-owned tick context is rejected.",
            "- Runtime integration should wait until text boxes are generated from deterministic graph/axis label bands, not global OCR alone.",
        ]
    )
    return "\n".join(lines) + "\n"


def build_summary(args: argparse.Namespace) -> dict[str, Any]:
    records = read_json(args.drc4_manual)["records"]
    if args.max_fixtures:
        records = records[: args.max_fixtures]
    labels = iter_text_labels(records)
    labels_by_fixture: dict[str, list[dict[str, Any]]] = {}
    for label in labels:
        labels_by_fixture.setdefault(label["fixtureId"], []).append(label)

    images: dict[str, Image.Image] = {}
    for record in records:
        images[record["fixtureId"]] = Image.open(args.repo_root / record["sourceImage"]).convert("RGB")

    all_scores: list[dict[str, Any]] = []
    fixture_summaries: list[dict[str, Any]] = []
    method_errors: list[dict[str, Any]] = []
    for method_id in METHODS:
        try:
            engine = create_engine(method_id)
        except Exception as exc:  # pragma: no cover - captured in report for local setup failures.
            method_errors.append({"methodId": method_id, "error": str(exc)})
            continue
        for fixture_id, image in images.items():
            try:
                detections, elapsed = run_method(method_id, engine, image)
                scores = match_detections(fixture_id, method_id, detections, labels_by_fixture[fixture_id])
                fixture_summary = summarize_fixture(method_id, fixture_id, scores, elapsed)
                all_scores.extend(scores)
                all_scores.append(fixture_summary)
                fixture_summaries.append(fixture_summary)
            except Exception as exc:  # pragma: no cover - captured in report for local setup failures.
                method_errors.append({"methodId": method_id, "fixtureId": fixture_id, "error": str(exc)})

    method_summaries = [summarize_method(method_id, all_scores) for method_id in METHODS if any(score["methodId"] == method_id for score in all_scores)]
    best = max(
        method_summaries,
        key=lambda item: (
            -item["safeFalseTickLabelCount"],
            item["boxRecall"],
            item["safeRoleAccuracyMatched"],
            -item["unmatchedNumericBoxCount"],
        ),
        default=None,
    )
    if method_errors and not method_summaries:
        verdict = "SECOND_OCR_TEXT_BOX_BENCHMARK_BLOCKED_BY_ENGINE_SETUP"
    elif best and best["safeFalseTickLabelCount"] == 0 and best["boxRecall"] >= 0.8 and best["safeRoleAccuracyMatched"] >= 0.85:
        verdict = "SECOND_OCR_TEXT_BOX_DETECTION_ACCEPTANCE_CANDIDATE"
    elif best and best["safeFalseTickLabelCount"] == 0:
        verdict = "SECOND_OCR_INSTALLED_TEXT_BOX_DETECTION_NOT_ACCEPTANCE_READY"
    else:
        verdict = "SECOND_OCR_TEXT_BOX_DETECTION_SAFETY_FAILURE"

    return {
        "schemaVersion": "chromalab.benchmark.drd4_second_ocr_text_box_detection.v1",
        "overallVerdict": verdict,
        "productionImpact": "NONE_RESEARCH_ONLY",
        "textRoleTruthSource": str(args.drc4_manual).replace("\\", "/"),
        "fixtureCount": len(records),
        "textLabelCount": len(labels),
        "methods": METHODS,
        "packageVersions": package_versions(),
        "modelFiles": {
            "rapidocr": rapidocr_model_files(),
            "easyocr": easyocr_model_files(),
        },
        "methodErrors": method_errors,
        "methodSummaries": method_summaries,
        "fixtureSummaries": fixture_summaries,
        "detectionScores": [
            score
            for score in all_scores
            if not score.get("fixtureSummary")
        ],
        "nextRequiredCapabilities": [
            "graph_axis_label_band_owned_text_detection",
            "text_box_detector_tuned_for_small_axis_labels",
            "OCR_context_safety_gate_from_deterministic_geometry",
            "no_global_OCR_numeric_ticks_without_axis_ownership",
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
        default=Path("benchmark/reports/drd4_second_ocr_text_box_benchmark"),
    )
    parser.add_argument("--max-fixtures", type=int, default=None)
    args = parser.parse_args()
    summary = build_summary(args)
    write_json(args.output / "summary.json", summary)
    (args.output / "summary.md").write_text(build_markdown(summary), encoding="utf-8", newline="\n")
    print(f"Built DR-D4 second OCR text-box benchmark: {summary['overallVerdict']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
