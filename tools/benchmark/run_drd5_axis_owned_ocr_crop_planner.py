#!/usr/bin/env python3
"""Prototype axis-owned OCR crop planning from graph geometry.

This is a PC-side benchmark runner only. DR-C3 graph geometry is used as the
prototype crop source, and DR-C4 text labels are used as benchmark truth. The
script does not change Android runtime or production analysis.
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
from PIL import Image, ImageDraw


METHODS = {
    "rapidocr_axis_owned_crops_x2_v1": {
        "engine": "rapidocr",
        "scale": 2,
        "description": "RapidOCR full detection inside graph-derived axis-owned crops.",
    },
    "easyocr_en_axis_owned_crops_x2_v1": {
        "engine": "easyocr",
        "scale": 2,
        "description": "EasyOCR English full detection inside graph-derived axis-owned crops.",
    },
}

CROP_COLORS = {
    "x_tick_label_band": "#1f77b4",
    "y_tick_label_band": "#2ca02c",
    "graph_header_band": "#ff7f0e",
    "right_context_band": "#9467bd",
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


def apply_axis_owned_safety_gate(
    predicted_role: str,
    text: str,
    context: str,
    *,
    matched_truth: bool,
) -> tuple[str, str | None]:
    if predicted_role != "tick_label" or not is_numeric_tick_text(text):
        return predicted_role, None
    if not matched_truth:
        return "numeric_rejected_without_owned_truth_geometry", "numeric OCR box has no matched geometry-owned truth label"
    if context != "tick_label_candidate_context":
        return "numeric_rejected_non_tick_context", "pure numeric OCR is not accepted outside tick-label candidate context"
    return predicted_role, None


def bbox_from_xywh(box: dict[str, Any]) -> tuple[float, float, float, float]:
    return (
        float(box["x"]),
        float(box["y"]),
        float(box["x"]) + float(box["width"]),
        float(box["y"]) + float(box["height"]),
    )


def bbox_to_json(box: tuple[float, float, float, float]) -> dict[str, float]:
    return {"x0": round(box[0], 2), "y0": round(box[1], 2), "x1": round(box[2], 2), "y1": round(box[3], 2)}


def clamp_bbox(box: tuple[float, float, float, float], width: int, height: int) -> tuple[float, float, float, float] | None:
    x0 = max(0.0, min(float(width), box[0]))
    y0 = max(0.0, min(float(height), box[1]))
    x1 = max(0.0, min(float(width), box[2]))
    y1 = max(0.0, min(float(height), box[3]))
    if x1 - x0 < 8 or y1 - y0 < 8:
        return None
    return (x0, y0, x1, y1)


def bbox_from_polygon(points: Any) -> tuple[float, float, float, float]:
    xs = [float(point[0]) for point in points]
    ys = [float(point[1]) for point in points]
    return (min(xs), min(ys), max(xs), max(ys))


def bbox_area(box: tuple[float, float, float, float]) -> float:
    return max(0.0, box[2] - box[0]) * max(0.0, box[3] - box[1])


def bbox_iou(a: tuple[float, float, float, float], b: tuple[float, float, float, float]) -> float:
    x0 = max(a[0], b[0])
    y0 = max(a[1], b[1])
    x1 = min(a[2], b[2])
    y1 = min(a[3], b[3])
    inter = bbox_area((x0, y0, x1, y1))
    union = bbox_area(a) + bbox_area(b) - inter
    return inter / union if union > 0 else 0.0


def bbox_center(box: tuple[float, float, float, float]) -> tuple[float, float]:
    return ((box[0] + box[2]) / 2.0, (box[1] + box[3]) / 2.0)


def center_distance(a: tuple[float, float, float, float], b: tuple[float, float, float, float]) -> float:
    ax, ay = bbox_center(a)
    bx, by = bbox_center(b)
    return math.hypot(ax - bx, ay - by)


def point_inside_expanded(point: tuple[float, float], box: tuple[float, float, float, float], margin: float) -> bool:
    return box[0] - margin <= point[0] <= box[2] + margin and box[1] - margin <= point[1] <= box[3] + margin


def iter_truth_labels(records: list[dict[str, Any]]) -> list[dict[str, Any]]:
    labels: list[dict[str, Any]] = []
    for record in records:
        for graph in record.get("graphs", []):
            for label in graph.get("textRoleLabels", []):
                bbox = bbox_from_xywh(label["bbox"])
                truth_role = str(label.get("role") or "")
                label_key = f"{record['fixtureId']}::{graph['graphId']}::{label.get('textId') or label.get('labelId')}"
                labels.append(
                    {
                        "labelKey": label_key,
                        "fixtureId": record["fixtureId"],
                        "sourceImage": record["sourceImage"],
                        "graphId": graph["graphId"],
                        "textId": label.get("textId") or label.get("labelId"),
                        "truthText": label.get("text"),
                        "truthRole": truth_role,
                        "bboxTuple": bbox,
                        "bbox": bbox_to_json(bbox),
                    }
                )
    return labels


def records_by_fixture(records: list[dict[str, Any]]) -> dict[str, dict[str, Any]]:
    return {record["fixtureId"]: record for record in records}


def add_crop(
    crops: list[dict[str, Any]],
    *,
    fixture_id: str,
    graph_id: str,
    crop_type: str,
    bbox: tuple[float, float, float, float],
    image_width: int,
    image_height: int,
    source: str,
) -> None:
    clamped = clamp_bbox(bbox, image_width, image_height)
    if clamped is None:
        return
    context = "tick_label_candidate_context" if crop_type in {"x_tick_label_band", "y_tick_label_band"} else "non_tick_context_requires_geometry_proof"
    crops.append(
        {
            "cropId": f"{fixture_id}_{graph_id}_{crop_type}_{len(crops) + 1}",
            "fixtureId": fixture_id,
            "graphId": graph_id,
            "cropType": crop_type,
            "context": context,
            "bboxTuple": clamped,
            "bbox": bbox_to_json(clamped),
            "source": source,
        }
    )


def build_crop_plans(layout_records: list[dict[str, Any]]) -> list[dict[str, Any]]:
    crops: list[dict[str, Any]] = []
    for record in layout_records:
        fixture_id = record["fixtureId"]
        image_width = int(record["image"]["width"])
        image_height = int(record["image"]["height"])
        for graph in record.get("graphs", []):
            graph_id = graph["graphId"]
            graph_panel = bbox_from_xywh(graph["graphPanel"])
            plot_area = bbox_from_xywh(graph["plotArea"])
            label_bands = graph.get("labelBands") or {}

            if "xTickLabels" in label_bands:
                x_band = bbox_from_xywh(label_bands["xTickLabels"])
                x_source = "annotated_label_band"
            else:
                bottom_space = max(16.0, graph_panel[3] - plot_area[3])
                x_band = (
                    plot_area[0] - 8.0,
                    max(graph_panel[1], plot_area[3] - 8.0),
                    plot_area[2] + 8.0,
                    min(graph_panel[3], plot_area[3] + max(36.0, bottom_space + 8.0)),
                )
                x_source = "inferred_from_plot_bottom_and_graph_panel"
            add_crop(
                crops,
                fixture_id=fixture_id,
                graph_id=graph_id,
                crop_type="x_tick_label_band",
                bbox=x_band,
                image_width=image_width,
                image_height=image_height,
                source=x_source,
            )

            if "yTickLabels" in label_bands:
                y_band = bbox_from_xywh(label_bands["yTickLabels"])
                y_source = "annotated_label_band"
            else:
                y_band = (
                    graph_panel[0],
                    max(graph_panel[1], plot_area[1] - 4.0),
                    min(plot_area[0] + 8.0, graph_panel[2]),
                    min(graph_panel[3], plot_area[3] + 4.0),
                )
                y_source = "inferred_from_plot_left_and_graph_panel"
            add_crop(
                crops,
                fixture_id=fixture_id,
                graph_id=graph_id,
                crop_type="y_tick_label_band",
                bbox=y_band,
                image_width=image_width,
                image_height=image_height,
                source=y_source,
            )

            header_height = plot_area[1] - graph_panel[1]
            if header_height >= 12:
                add_crop(
                    crops,
                    fixture_id=fixture_id,
                    graph_id=graph_id,
                    crop_type="graph_header_band",
                    bbox=(graph_panel[0], graph_panel[1], graph_panel[2], min(graph_panel[3], plot_area[1] + 8.0)),
                    image_width=image_width,
                    image_height=image_height,
                    source="inferred_from_graph_panel_above_plot_area",
                )

            right_width = graph_panel[2] - plot_area[2]
            if right_width >= 18:
                add_crop(
                    crops,
                    fixture_id=fixture_id,
                    graph_id=graph_id,
                    crop_type="right_context_band",
                    bbox=(max(graph_panel[0], plot_area[2] - 4.0), plot_area[1], graph_panel[2], plot_area[3]),
                    image_width=image_width,
                    image_height=image_height,
                    source="inferred_from_plot_right_margin",
                )
    return crops


def truth_labels_owned_by_crop(crop: dict[str, Any], truth_labels: list[dict[str, Any]]) -> list[dict[str, Any]]:
    box = crop["bboxTuple"]
    owned: list[dict[str, Any]] = []
    for label in truth_labels:
        if label["fixtureId"] != crop["fixtureId"] or label["graphId"] != crop["graphId"]:
            continue
        if point_inside_expanded(bbox_center(label["bboxTuple"]), box, 6.0):
            owned.append(label)
    return owned


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


def detect_rapidocr(engine: Any, crop_image: Image.Image) -> list[dict[str, Any]]:
    output = engine(np.array(crop_image.convert("RGB")), use_det=True, use_cls=True, use_rec=True)
    boxes = getattr(output, "boxes", None)
    texts = getattr(output, "txts", None)
    scores = getattr(output, "scores", None)
    boxes = [] if boxes is None else list(boxes)
    texts = [] if texts is None else list(texts)
    scores = [] if scores is None else list(scores)
    detections: list[dict[str, Any]] = []
    for index, box in enumerate(boxes):
        detections.append(
            {
                "localDetectionId": f"rapidocr_det_{index + 1}",
                "text": str(texts[index]) if index < len(texts) else "",
                "confidence": round(float(scores[index]), 4) if index < len(scores) and scores[index] is not None else 0.0,
                "localBboxTuple": bbox_from_polygon(box),
            }
        )
    return detections


def detect_easyocr(engine: Any, crop_image: Image.Image) -> list[dict[str, Any]]:
    output = engine.readtext(np.array(crop_image.convert("RGB")), detail=1, paragraph=False)
    detections: list[dict[str, Any]] = []
    for index, item in enumerate(output):
        box, text, score = item
        detections.append(
            {
                "localDetectionId": f"easyocr_det_{index + 1}",
                "text": str(text),
                "confidence": round(float(score), 4),
                "localBboxTuple": bbox_from_polygon(box),
            }
        )
    return detections


def run_engine_on_crop(method_id: str, engine: Any, image: Image.Image, crop: dict[str, Any]) -> tuple[list[dict[str, Any]], float]:
    crop_box = crop["bboxTuple"]
    scale = int(METHODS[method_id]["scale"])
    crop_image = image.crop(tuple(int(round(value)) for value in crop_box)).convert("RGB")
    if scale > 1:
        crop_image = crop_image.resize((crop_image.width * scale, crop_image.height * scale), Image.Resampling.LANCZOS)
    started = time.perf_counter()
    if METHODS[method_id]["engine"] == "rapidocr":
        local_detections = detect_rapidocr(engine, crop_image)
    else:
        local_detections = detect_easyocr(engine, crop_image)
    elapsed = time.perf_counter() - started
    detections: list[dict[str, Any]] = []
    for index, detection in enumerate(local_detections):
        local_box = detection["localBboxTuple"]
        original_box = (
            crop_box[0] + local_box[0] / scale,
            crop_box[1] + local_box[1] / scale,
            crop_box[0] + local_box[2] / scale,
            crop_box[1] + local_box[3] / scale,
        )
        detections.append(
            {
                "detectionId": f"{crop['cropId']}_{method_id}_{index + 1}",
                "methodId": method_id,
                "fixtureId": crop["fixtureId"],
                "graphId": crop["graphId"],
                "cropId": crop["cropId"],
                "cropType": crop["cropType"],
                "context": crop["context"],
                "ocrText": detection["text"],
                "confidence": detection["confidence"],
                "bboxTuple": original_box,
                "bbox": bbox_to_json(original_box),
            }
        )
    return detections, elapsed


def match_crop_detections(
    method_id: str,
    crop: dict[str, Any],
    detections: list[dict[str, Any]],
    owned_labels: list[dict[str, Any]],
) -> list[dict[str, Any]]:
    unmatched = {label["labelKey"]: label for label in owned_labels}
    rows: list[dict[str, Any]] = []
    for detection in sorted(detections, key=lambda item: item["confidence"], reverse=True):
        det_box = detection["bboxTuple"]
        raw_role = classify_text_role(detection["ocrText"])
        best_label: dict[str, Any] | None = None
        best_score = -1.0
        best_iou = 0.0
        best_distance = 0.0
        for label in unmatched.values():
            label_box = label["bboxTuple"]
            iou = bbox_iou(det_box, label_box)
            distance = center_distance(det_box, label_box)
            center_ok = point_inside_expanded(bbox_center(det_box), label_box, 36.0)
            if iou >= 0.04:
                score = 2.0 + iou
            elif center_ok:
                score = 1.0 - min(distance, 120.0) / 120.0
            else:
                score = -1.0
            if score >= 0.0:
                role_bias = 0.0
                if raw_role == label["truthRole"]:
                    role_bias += 0.45
                if raw_role == "tick_label" and label["truthRole"] == "tick_label":
                    role_bias += 0.45
                if raw_role == "tick_label" and label["truthRole"] != "tick_label":
                    role_bias -= 0.45
                score += role_bias + 0.25 * text_similarity(detection["ocrText"], label["truthText"])
            if score > best_score:
                best_label = label
                best_score = score
                best_iou = iou
                best_distance = distance
        matched = best_label is not None and best_score >= 0.0
        if matched and best_label is not None:
            unmatched.pop(best_label["labelKey"], None)
            truth_role = best_label["truthRole"]
            truth_text = best_label["truthText"]
            label_key = best_label["labelKey"]
        else:
            truth_role = None
            truth_text = None
            label_key = None
        safe_role, safety_rejection = apply_axis_owned_safety_gate(
            raw_role,
            detection["ocrText"],
            detection["context"],
            matched_truth=matched,
        )
        rows.append(
            {
                "methodId": method_id,
                "fixtureId": crop["fixtureId"],
                "graphId": crop["graphId"],
                "cropId": crop["cropId"],
                "cropType": crop["cropType"],
                "context": crop["context"],
                "detectionId": detection["detectionId"],
                "ocrText": detection["ocrText"],
                "confidence": detection["confidence"],
                "bbox": detection["bbox"],
                "matchedTruth": matched,
                "truthLabelKey": label_key,
                "truthText": truth_text,
                "truthRole": truth_role,
                "matchIou": round(best_iou, 4) if matched else 0.0,
                "matchCenterDistance": round(best_distance, 2) if matched else None,
                "rawPredictedRole": raw_role,
                "safePredictedRole": safe_role,
                "safetyRejectionReason": safety_rejection,
                "textExactMatch": normalize_compact(detection["ocrText"]) == normalize_compact(truth_text) if matched else False,
                "textSimilarity": round(text_similarity(detection["ocrText"], truth_text), 4) if matched else 0.0,
                "rawRoleScore": "PASS" if matched and raw_role == truth_role else "FAIL" if matched else "UNMATCHED",
                "safeRoleScore": "PASS" if matched and safe_role == truth_role else "FAIL" if matched else "UNMATCHED",
                "safeFalseTickLabel": matched and safe_role == "tick_label" and truth_role != "tick_label",
                "unmatchedNumericRejected": (not matched) and is_numeric_tick_text(detection["ocrText"]),
            }
        )
    for label in unmatched.values():
        rows.append(
            {
                "methodId": method_id,
                "fixtureId": crop["fixtureId"],
                "graphId": crop["graphId"],
                "cropId": crop["cropId"],
                "cropType": crop["cropType"],
                "context": crop["context"],
                "detectionId": None,
                "ocrText": "",
                "confidence": 0.0,
                "bbox": None,
                "matchedTruth": False,
                "missingOwnedTruthLabel": True,
                "truthLabelKey": label["labelKey"],
                "truthText": label["truthText"],
                "truthRole": label["truthRole"],
                "rawPredictedRole": "missing_text",
                "safePredictedRole": "missing_text",
                "safetyRejectionReason": "owned truth label had no matched OCR detection",
                "textExactMatch": False,
                "textSimilarity": 0.0,
                "rawRoleScore": "FAIL",
                "safeRoleScore": "FAIL",
                "safeFalseTickLabel": False,
                "unmatchedNumericRejected": False,
            }
        )
    return rows


def render_crop_plan_overlays(repo_root: Path, records: list[dict[str, Any]], crops: list[dict[str, Any]], output: Path) -> list[dict[str, Any]]:
    output.mkdir(parents=True, exist_ok=True)
    crops_by_fixture: dict[str, list[dict[str, Any]]] = {}
    for crop in crops:
        crops_by_fixture.setdefault(crop["fixtureId"], []).append(crop)
    overlays: list[dict[str, Any]] = []
    for record in records:
        fixture_id = record["fixtureId"]
        image = Image.open(repo_root / record["sourceImage"]).convert("RGB")
        draw = ImageDraw.Draw(image)
        for crop in crops_by_fixture.get(fixture_id, []):
            box = crop["bboxTuple"]
            color = CROP_COLORS.get(crop["cropType"], "#ffffff")
            draw.rectangle(box, outline=color, width=3)
            draw.text((box[0] + 3, box[1] + 3), crop["cropType"], fill=color)
        path = output / f"{fixture_id}_axis_owned_crop_plan_overlay.png"
        image.save(path)
        overlays.append({"fixtureId": fixture_id, "path": str(path).replace("\\", "/")})
    return overlays


def package_versions() -> dict[str, str | None]:
    names = ["rapidocr", "onnxruntime", "easyocr", "torch", "torchvision", "opencv-python-headless", "numpy", "Pillow"]
    versions: dict[str, str | None] = {}
    for name in names:
        try:
            versions[name] = importlib.metadata.version(name)
        except importlib.metadata.PackageNotFoundError:
            versions[name] = None
    return versions


def unique_owned_labels(crops: list[dict[str, Any]], truth_labels: list[dict[str, Any]]) -> dict[str, dict[str, Any]]:
    owned: dict[str, dict[str, Any]] = {}
    for crop in crops:
        for label in truth_labels_owned_by_crop(crop, truth_labels):
            owned[label["labelKey"]] = label
    return owned


def best_rows_by_truth(rows: list[dict[str, Any]]) -> dict[str, dict[str, Any]]:
    best: dict[str, dict[str, Any]] = {}
    for row in rows:
        label_key = row.get("truthLabelKey")
        if not row.get("matchedTruth") or not label_key:
            continue
        current = best.get(label_key)
        if current is None or (row["confidence"], row["textSimilarity"]) > (current["confidence"], current["textSimilarity"]):
            best[label_key] = row
    return best


def summarize_method(method_id: str, rows: list[dict[str, Any]], crops: list[dict[str, Any]], truth_labels: list[dict[str, Any]]) -> dict[str, Any]:
    method_rows = [row for row in rows if row["methodId"] == method_id]
    owned = unique_owned_labels(crops, truth_labels)
    best = best_rows_by_truth(method_rows)
    matched_rows = list(best.values())
    tick_owned = [label for label in owned.values() if label["truthRole"] == "tick_label"]
    tick_matched = [row for row in matched_rows if row["truthRole"] == "tick_label"]
    safe_pass = [row for row in matched_rows if row["safeRoleScore"] == "PASS"]
    safe_false_tick = [row for row in matched_rows if row["safeFalseTickLabel"]]
    unmatched_numeric = [row for row in method_rows if row.get("unmatchedNumericRejected")]
    elapsed_rows = [row for row in method_rows if row.get("cropElapsedSummary")]
    return {
        "methodId": method_id,
        "description": METHODS[method_id]["description"],
        "cropPlanCount": len(crops),
        "ownedTruthLabelCount": len(owned),
        "matchedOwnedTruthCount": len(best),
        "ownedBoxRecall": round(len(best) / len(owned), 4) if owned else 0.0,
        "ownedTickLabelCount": len(tick_owned),
        "matchedTickLabelCount": len(tick_matched),
        "tickLabelRecall": round(len(tick_matched) / len(tick_owned), 4) if tick_owned else 0.0,
        "safeRoleAccuracyMatched": round(len(safe_pass) / len(matched_rows), 4) if matched_rows else 0.0,
        "safeFalseTickLabelCount": len(safe_false_tick),
        "unmatchedNumericRejectedCount": len(unmatched_numeric),
        "meanTextSimilarityMatched": round(statistics.mean(row["textSimilarity"] for row in matched_rows), 4) if matched_rows else 0.0,
        "meanCropElapsedSec": round(statistics.mean(row["elapsedSec"] for row in elapsed_rows), 4) if elapsed_rows else 0.0,
        "roleConfusionSafe": dict(Counter(f"{row['truthRole']}->{row['safePredictedRole']}" for row in matched_rows)),
    }


def summarize_fixture(method_id: str, fixture_id: str, rows: list[dict[str, Any]], crops: list[dict[str, Any]], truth_labels: list[dict[str, Any]]) -> dict[str, Any]:
    fixture_crops = [crop for crop in crops if crop["fixtureId"] == fixture_id]
    fixture_truth = [label for label in truth_labels if label["fixtureId"] == fixture_id]
    selected = [row for row in rows if row["methodId"] == method_id and row["fixtureId"] == fixture_id]
    owned = unique_owned_labels(fixture_crops, fixture_truth)
    best = best_rows_by_truth(selected)
    matched_rows = list(best.values())
    tick_owned = [label for label in owned.values() if label["truthRole"] == "tick_label"]
    tick_matched = [row for row in matched_rows if row["truthRole"] == "tick_label"]
    elapsed_rows = [row for row in selected if row.get("cropElapsedSummary")]
    return {
        "fixtureId": fixture_id,
        "methodId": method_id,
        "cropPlanCount": len(fixture_crops),
        "ownedTruthLabelCount": len(owned),
        "matchedOwnedTruthCount": len(best),
        "ownedBoxRecall": round(len(best) / len(owned), 4) if owned else 0.0,
        "ownedTickLabelCount": len(tick_owned),
        "matchedTickLabelCount": len(tick_matched),
        "tickLabelRecall": round(len(tick_matched) / len(tick_owned), 4) if tick_owned else 0.0,
        "safeRoleAccuracyMatched": round(
            sum(1 for row in matched_rows if row["safeRoleScore"] == "PASS") / len(matched_rows),
            4,
        )
        if matched_rows
        else 0.0,
        "safeFalseTickLabelCount": sum(1 for row in matched_rows if row["safeFalseTickLabel"]),
        "unmatchedNumericRejectedCount": sum(1 for row in selected if row.get("unmatchedNumericRejected")),
        "meanCropElapsedSec": round(statistics.mean(row["elapsedSec"] for row in elapsed_rows), 4) if elapsed_rows else 0.0,
    }


def build_markdown(summary: dict[str, Any]) -> str:
    lines = [
        "# DR-D5 Axis-Owned OCR Crop Planner Prototype",
        "",
        f"Verdict: `{summary['overallVerdict']}`",
        f"Fixtures: `{summary['fixtureCount']}`",
        f"Crop plans: `{summary['cropPlanCount']}`",
        f"Owned truth labels: `{summary['ownedTruthLabelCount']}`",
        "",
        "## Method Summary",
        "",
        "| Method | Owned recall | Tick recall | Safe role accuracy | Safe false ticks | Rejected unmatched numeric | Mean crop time |",
        "| --- | ---: | ---: | ---: | ---: | ---: | ---: |",
    ]
    for item in summary["methodSummaries"]:
        lines.append(
            "| `{methodId}` | {ownedBoxRecall:.4f} | {tickLabelRecall:.4f} | {safeRoleAccuracyMatched:.4f} | "
            "{safeFalseTickLabelCount} | {unmatchedNumericRejectedCount} | {meanCropElapsedSec:.4f}s |".format(**item)
        )
    lines.extend(
        [
            "",
            "## Fixture Summary",
            "",
            "| Fixture | Method | Crops | Owned recall | Tick recall | Safe role accuracy | Rejected unmatched numeric |",
            "| --- | --- | ---: | ---: | ---: | ---: | ---: |",
        ]
    )
    for item in summary["fixtureSummaries"]:
        lines.append(
            "| `{fixtureId}` | `{methodId}` | {cropPlanCount} | {ownedBoxRecall:.4f} | {tickLabelRecall:.4f} | "
            "{safeRoleAccuracyMatched:.4f} | {unmatchedNumericRejectedCount} |".format(**item)
        )
    lines.extend(
        [
            "",
            "## Crop Plan Overlays",
            "",
        ]
    )
    for overlay in summary["cropPlanOverlays"]:
        lines.append(f"- `{overlay['fixtureId']}`: `{overlay['path']}`")
    lines.extend(
        [
            "",
            "## Interpretation",
            "",
            "- Axis-owned crops reduce OCR scope and preserve deterministic ownership rules.",
            "- This prototype still uses DR-C3 annotation geometry, so it is not Android runtime integration.",
            "- Numeric OCR boxes without matched geometry-owned labels are rejected before calibration.",
            "- The next implementation work must replace annotation geometry with automatic graph/axis label-band crop generation.",
        ]
    )
    return "\n".join(lines) + "\n"


def build_summary(args: argparse.Namespace) -> dict[str, Any]:
    layout_records = read_json(args.drc3_manual)["records"]
    truth_records = read_json(args.drc4_manual)["records"]
    truth_by_fixture = records_by_fixture(truth_records)
    layout_records = [record for record in layout_records if record["fixtureId"] in truth_by_fixture]
    if args.max_fixtures:
        layout_records = layout_records[: args.max_fixtures]
        allowed = {record["fixtureId"] for record in layout_records}
        truth_records = [record for record in truth_records if record["fixtureId"] in allowed]

    truth_labels = iter_truth_labels(truth_records)
    crops = build_crop_plans(layout_records)
    overlays = render_crop_plan_overlays(args.repo_root, layout_records, crops, args.output / "overlays")
    images = {
        record["fixtureId"]: Image.open(args.repo_root / record["sourceImage"]).convert("RGB")
        for record in layout_records
    }
    truth_by_crop = {crop["cropId"]: truth_labels_owned_by_crop(crop, truth_labels) for crop in crops}

    rows: list[dict[str, Any]] = []
    method_errors: list[dict[str, Any]] = []
    for method_id in METHODS:
        try:
            engine = create_engine(method_id)
        except Exception as exc:  # pragma: no cover - captured in report for setup failures.
            method_errors.append({"methodId": method_id, "error": str(exc)})
            continue
        for crop in crops:
            try:
                detections, elapsed = run_engine_on_crop(method_id, engine, images[crop["fixtureId"]], crop)
                rows.extend(match_crop_detections(method_id, crop, detections, truth_by_crop[crop["cropId"]]))
                rows.append(
                    {
                        "methodId": method_id,
                        "fixtureId": crop["fixtureId"],
                        "graphId": crop["graphId"],
                        "cropId": crop["cropId"],
                        "cropType": crop["cropType"],
                        "cropElapsedSummary": True,
                        "elapsedSec": round(elapsed, 4),
                    }
                )
            except Exception as exc:  # pragma: no cover - captured in report for per-crop failures.
                method_errors.append({"methodId": method_id, "cropId": crop["cropId"], "error": str(exc)})

    method_summaries = [summarize_method(method_id, rows, crops, truth_labels) for method_id in METHODS if any(row["methodId"] == method_id for row in rows)]
    fixture_summaries: list[dict[str, Any]] = []
    for method_id in METHODS:
        if not any(row["methodId"] == method_id for row in rows):
            continue
        for record in layout_records:
            fixture_summaries.append(summarize_fixture(method_id, record["fixtureId"], rows, crops, truth_labels))

    owned = unique_owned_labels(crops, truth_labels)
    best = max(
        method_summaries,
        key=lambda item: (
            -item["safeFalseTickLabelCount"],
            item["tickLabelRecall"],
            item["safeRoleAccuracyMatched"],
            item["ownedBoxRecall"],
        ),
        default=None,
    )
    if method_errors and not method_summaries:
        verdict = "AXIS_OWNED_OCR_CROP_PLANNER_BLOCKED_BY_ENGINE_SETUP"
    elif best and best["safeFalseTickLabelCount"] == 0 and best["tickLabelRecall"] >= 0.9 and best["safeRoleAccuracyMatched"] >= 0.9:
        verdict = "AXIS_OWNED_OCR_CROP_PLANNER_ACCEPTANCE_CANDIDATE"
    elif best and best["safeFalseTickLabelCount"] == 0:
        verdict = "AXIS_OWNED_OCR_CROP_PLANNER_IMPROVES_SCOPE_NOT_ACCEPTANCE_READY"
    else:
        verdict = "AXIS_OWNED_OCR_CROP_PLANNER_SAFETY_FAILURE"

    return {
        "schemaVersion": "chromalab.benchmark.drd5_axis_owned_ocr_crop_planner.v1",
        "overallVerdict": verdict,
        "productionImpact": "NONE_RESEARCH_ONLY",
        "layoutTruthSource": str(args.drc3_manual).replace("\\", "/"),
        "textRoleTruthSource": str(args.drc4_manual).replace("\\", "/"),
        "fixtureCount": len(layout_records),
        "cropPlanCount": len(crops),
        "ownedTruthLabelCount": len(owned),
        "methods": METHODS,
        "packageVersions": package_versions(),
        "methodErrors": method_errors,
        "cropPlans": [
            {key: value for key, value in crop.items() if key != "bboxTuple"}
            for crop in crops
        ],
        "cropPlanOverlays": overlays,
        "methodSummaries": method_summaries,
        "fixtureSummaries": fixture_summaries,
        "detectionScores": [
            row
            for row in rows
            if not row.get("cropElapsedSummary")
        ],
        "nextRequiredCapabilities": [
            "automatic_graph_panel_and_plot_area_geometry_for_crop_plans",
            "automatic_axis_label_band_detection_without_annotation_geometry",
            "crop_level_OCR_preprocessing_grid",
            "calibration_anchor_pairing_from_owned_numeric_label_boxes",
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
        "--output",
        type=Path,
        default=Path("benchmark/reports/drd5_axis_owned_ocr_crop_planner"),
    )
    parser.add_argument("--max-fixtures", type=int, default=None)
    args = parser.parse_args()
    summary = build_summary(args)
    write_json(args.output / "summary.json", summary)
    (args.output / "summary.md").write_text(build_markdown(summary), encoding="utf-8", newline="\n")
    print(f"Built DR-D5 axis-owned OCR crop planner: {summary['overallVerdict']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
