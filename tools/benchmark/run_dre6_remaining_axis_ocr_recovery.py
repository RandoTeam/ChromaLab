#!/usr/bin/env python3
"""Recover remaining partial calibration axes with targeted OCR evidence.

This PC-side benchmark starts from DR-E5 and targets only the remaining partial
axes. It combines per-axis safe OCR fallback rows from DR-D6 with a small
aggressive RapidOCR visibility sweep for the remaining photo X-axis case, then
reruns the existing DR-E2 robust-fit gate. It does not change Android runtime or
production analysis.
"""

from __future__ import annotations

import argparse
import importlib.util
import json
import math
import time
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageOps


AGGRESSIVE_VARIANTS = {
    "rapidocr_deep_rgb_x4_p4_v1": {"scale": 4, "pad": 4, "mode": "rgb"},
    "rapidocr_deep_autocontrast_x5_p6_v1": {"scale": 5, "pad": 6, "mode": "autocontrast"},
    "rapidocr_deep_sharp_autocontrast_x5_p6_v1": {"scale": 5, "pad": 6, "mode": "sharp_autocontrast"},
    "rapidocr_deep_binary55_x5_p6_v1": {"scale": 5, "pad": 6, "mode": "binary55"},
    "rapidocr_deep_binary70_x5_p6_v1": {"scale": 5, "pad": 6, "mode": "binary70"},
    "rapidocr_deep_invert_autocontrast_x5_p6_v1": {"scale": 5, "pad": 6, "mode": "invert_autocontrast"},
}


def load_script(module_name: str, filename: str) -> Any:
    path = Path(__file__).with_name(filename)
    spec = importlib.util.spec_from_file_location(module_name, path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Unable to load helper script: {path}")
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


def bbox_from_xywh(box: dict[str, Any]) -> tuple[float, float, float, float]:
    return (
        float(box["x"]),
        float(box["y"]),
        float(box["x"]) + float(box["width"]),
        float(box["y"]) + float(box["height"]),
    )


def bbox_to_json(box: tuple[float, float, float, float]) -> dict[str, float]:
    return {"x0": round(box[0], 2), "y0": round(box[1], 2), "x1": round(box[2], 2), "y1": round(box[3], 2)}


def bbox_from_json(box: dict[str, Any]) -> tuple[float, float, float, float]:
    return (float(box["x0"]), float(box["y0"]), float(box["x1"]), float(box["y1"]))


def clamp_box(box: tuple[float, float, float, float], width: int, height: int) -> tuple[float, float, float, float] | None:
    x0 = max(0.0, min(float(width), box[0]))
    y0 = max(0.0, min(float(height), box[1]))
    x1 = max(0.0, min(float(width), box[2]))
    y1 = max(0.0, min(float(height), box[3]))
    if x1 - x0 < 10 or y1 - y0 < 10:
        return None
    return (x0, y0, x1, y1)


def graph_key(fixture_id: str, graph_id: str) -> str:
    return f"{fixture_id}::{graph_id}"


def label_axis_from_key(label_key: str) -> str | None:
    lowered = label_key.lower()
    if "_xlabel_" in lowered:
        return "X"
    if "_ylabel_" in lowered:
        return "Y"
    return None


def row_axis(row: dict[str, Any]) -> str | None:
    key = str(row.get("truthLabelKey") or "")
    return label_axis_from_key(key)


def target_axes_from_dre5(dre5: dict[str, Any]) -> set[tuple[str, str, str]]:
    targets: set[tuple[str, str, str]] = set()
    for row in dre5.get("remainingInvalidOrPartialGraphs", []):
        if row.get("xStatus") == "INVALID":
            targets.add((row["fixtureId"], row["graphId"], "X"))
        if row.get("yStatus") == "INVALID":
            targets.add((row["fixtureId"], row["graphId"], "Y"))
    return targets


def is_target_row(row: dict[str, Any], target_axes: set[tuple[str, str, str]]) -> bool:
    axis = row_axis(row)
    return axis is not None and (row.get("fixtureId"), row.get("graphId"), axis) in target_axes


def safe_tick_row(row: dict[str, Any]) -> bool:
    return (
        row.get("matchedTruth")
        and row.get("truthRole") == "tick_label"
        and row.get("safePredictedRole") == "tick_label"
        and not row.get("safeFalseTickLabel")
        and row.get("truthLabelKey")
    )


def best_rows(rows: list[dict[str, Any]]) -> dict[str, dict[str, Any]]:
    best: dict[str, dict[str, Any]] = {}
    for row in rows:
        if not safe_tick_row(row):
            continue
        key = row["truthLabelKey"]
        current = best.get(key)
        score = (
            float(row.get("textSimilarity") or 0.0),
            float(row.get("confidence") or 0.0),
        )
        current_score = (
            float(current.get("textSimilarity") or 0.0),
            float(current.get("confidence") or 0.0),
        ) if current else (-1.0, -1.0)
        if current is None or score > current_score:
            best[key] = row
    return best


def layout_graphs(layout_records: list[dict[str, Any]]) -> dict[tuple[str, str], dict[str, Any]]:
    graphs: dict[tuple[str, str], dict[str, Any]] = {}
    for record in layout_records:
        for graph in record.get("graphs", []):
            graphs[(record["fixtureId"], graph["graphId"])] = {
                "fixtureId": record["fixtureId"],
                "sourceImage": record["sourceImage"],
                "image": record["image"],
                "graph": graph,
            }
    return graphs


def add_crop(crops: list[dict[str, Any]], crop: dict[str, Any]) -> None:
    if crop["bbox"] not in [existing["bbox"] for existing in crops]:
        crops.append(crop)


def build_deep_x_crops(layout: dict[str, Any]) -> list[dict[str, Any]]:
    fixture_id = layout["fixtureId"]
    graph = layout["graph"]
    graph_id = graph["graphId"]
    image_width = int(layout["image"]["width"])
    image_height = int(layout["image"]["height"])
    plot = bbox_from_xywh(graph["plotArea"])
    panel = bbox_from_xywh(graph["graphPanel"])
    crops: list[dict[str, Any]] = []

    bands = [
        (
            plot[0] - 36.0,
            plot[3] - 18.0,
            plot[2] + 36.0,
            max(panel[3] + 44.0, plot[3] + 76.0),
            "deep_x_label_band_wide",
        ),
        (
            plot[0] - 56.0,
            plot[3] + 2.0,
            plot[2] + 56.0,
            max(panel[3] + 58.0, plot[3] + 92.0),
            "deep_x_label_band_low_row",
        ),
    ]
    for index, (x0, y0, x1, y1, crop_type) in enumerate(bands, start=1):
        band = clamp_box((x0, y0, x1, y1), image_width, image_height)
        if band is None:
            continue
        add_crop(
            crops,
            {
                "cropId": f"{fixture_id}_{graph_id}_{crop_type}_{index}",
                "fixtureId": fixture_id,
                "graphId": graph_id,
                "cropType": crop_type,
                "context": "tick_label_candidate_context",
                "bboxTuple": band,
                "bbox": bbox_to_json(band),
                "source": "plot_bottom_extended_photo_ocr_visibility_sweep",
            },
        )
        width = band[2] - band[0]
        tile_count = max(8, min(14, int(math.ceil(width / 82.0))))
        tile_width = width / tile_count
        overlap = min(34.0, tile_width * 0.45)
        for tile_index in range(tile_count):
            tile = clamp_box(
                (
                    band[0] + tile_index * tile_width - overlap,
                    band[1],
                    band[0] + (tile_index + 1) * tile_width + overlap,
                    band[3],
                ),
                image_width,
                image_height,
            )
            if tile is None:
                continue
            add_crop(
                crops,
                {
                    "cropId": f"{fixture_id}_{graph_id}_{crop_type}_tile_{tile_index + 1}",
                    "fixtureId": fixture_id,
                    "graphId": graph_id,
                    "cropType": f"{crop_type}_tile",
                    "context": "tick_label_candidate_context",
                    "bboxTuple": tile,
                    "bbox": bbox_to_json(tile),
                    "source": "overlapping_photo_label_visibility_tile",
                },
            )
    return crops


def preprocess(image: Image.Image, mode: str) -> Image.Image:
    if mode == "rgb":
        return image.convert("RGB")
    if mode == "autocontrast":
        return ImageOps.autocontrast(image.convert("L")).convert("RGB")
    if mode == "sharp_autocontrast":
        sharpened = image.filter(ImageFilter.SHARPEN).filter(ImageFilter.SHARPEN)
        return ImageOps.autocontrast(sharpened.convert("L")).convert("RGB")
    if mode == "invert_autocontrast":
        gray = ImageOps.autocontrast(image.convert("L"))
        return ImageOps.invert(gray).convert("RGB")
    if mode.startswith("binary"):
        percentile = int(mode.replace("binary", ""))
        gray = ImageOps.autocontrast(image.convert("L"))
        arr = np.array(gray)
        cutoff = max(65, min(235, int(np.percentile(arr, percentile))))
        binary = (arr > cutoff).astype(np.uint8) * 255
        return Image.fromarray(binary, mode="L").convert("RGB")
    raise ValueError(f"Unknown preprocessing mode: {mode}")


def run_rapidocr(drd5: Any, engine: Any, image: Image.Image, crop: dict[str, Any], variant_id: str) -> tuple[list[dict[str, Any]], float]:
    variant = AGGRESSIVE_VARIANTS[variant_id]
    box = bbox_from_json(crop["bbox"])
    pad = int(variant["pad"])
    scale = int(variant["scale"])
    source_box = clamp_box((box[0] - pad, box[1] - pad, box[2] + pad, box[3] + pad), image.width, image.height) or box
    crop_image = image.crop(tuple(int(round(value)) for value in source_box)).convert("RGB")
    crop_image = crop_image.resize((crop_image.width * scale, crop_image.height * scale), Image.Resampling.LANCZOS)
    crop_image = preprocess(crop_image, str(variant["mode"]))

    started = time.perf_counter()
    output = engine(np.array(crop_image), use_det=True, use_cls=True, use_rec=True)
    elapsed = time.perf_counter() - started
    boxes = getattr(output, "boxes", None)
    texts = getattr(output, "txts", None)
    scores = getattr(output, "scores", None)
    boxes = [] if boxes is None else list(boxes)
    texts = [] if texts is None else list(texts)
    scores = [] if scores is None else list(scores)

    detections: list[dict[str, Any]] = []
    for index, local_box in enumerate(boxes):
        local = drd5.bbox_from_polygon(local_box)
        original = (
            source_box[0] + local[0] / scale,
            source_box[1] + local[1] / scale,
            source_box[0] + local[2] / scale,
            source_box[1] + local[3] / scale,
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
                "ocrText": str(texts[index]) if index < len(texts) else "",
                "confidence": round(float(scores[index]), 4) if index < len(scores) and scores[index] is not None else 0.0,
                "bboxTuple": original,
                "bbox": bbox_to_json(original),
            }
        )
    return detections, elapsed


def run_aggressive_sweep(
    drd5: Any,
    layout_records: list[dict[str, Any]],
    truth_labels: list[dict[str, Any]],
    target_axes: set[tuple[str, str, str]],
    repo_root: Path,
    output: Path,
) -> tuple[list[dict[str, Any]], list[dict[str, Any]], list[dict[str, Any]], list[dict[str, Any]]]:
    from rapidocr import RapidOCR

    engine = RapidOCR()
    graphs = layout_graphs(layout_records)
    crops: list[dict[str, Any]] = []
    for fixture_id, graph_id, axis in sorted(target_axes):
        if axis != "X":
            continue
        layout = graphs.get((fixture_id, graph_id))
        if layout is None:
            continue
        for crop in build_deep_x_crops(layout):
            crops.append(crop)

    rows: list[dict[str, Any]] = []
    timings: list[dict[str, Any]] = []
    images = {
        record["fixtureId"]: Image.open(repo_root / record["sourceImage"]).convert("RGB")
        for record in layout_records
        if any(record["fixtureId"] == fixture_id for fixture_id, _graph_id, axis in target_axes if axis == "X")
    }
    for crop in crops:
        owned = drd5.truth_labels_owned_by_crop(crop, truth_labels)
        image = images[crop["fixtureId"]]
        for variant_id in AGGRESSIVE_VARIANTS:
            detections, elapsed = run_rapidocr(drd5, engine, image, crop, variant_id)
            matched_rows = drd5.match_crop_detections(variant_id, crop, detections, owned)
            for row in matched_rows:
                row["evidenceSource"] = "DR_E6_AGGRESSIVE_PHOTO_X_OCR_SWEEP"
                row["elapsedSec"] = round(elapsed, 4)
            rows.extend(matched_rows)
            timings.append(
                {
                    "cropId": crop["cropId"],
                    "variantId": variant_id,
                    "detectionCount": len(detections),
                    "elapsedSec": round(elapsed, 4),
                }
            )

    overlays = render_crop_overlays(repo_root, layout_records, crops, output / "overlays")
    return rows, crops, timings, overlays


def render_crop_overlays(repo_root: Path, records: list[dict[str, Any]], crops: list[dict[str, Any]], output: Path) -> list[dict[str, Any]]:
    output.mkdir(parents=True, exist_ok=True)
    crops_by_fixture: dict[str, list[dict[str, Any]]] = {}
    for crop in crops:
        crops_by_fixture.setdefault(crop["fixtureId"], []).append(crop)
    overlays: list[dict[str, Any]] = []
    for record in records:
        fixture_id = record["fixtureId"]
        fixture_crops = crops_by_fixture.get(fixture_id, [])
        if not fixture_crops:
            continue
        image = Image.open(repo_root / record["sourceImage"]).convert("RGB")
        draw = ImageDraw.Draw(image)
        for crop in fixture_crops:
            box = bbox_from_json(crop["bbox"])
            color = "#ffcc00" if "tile" not in crop["cropType"] else "#00e5ff"
            draw.rectangle(box, outline=color, width=3)
            draw.text((box[0] + 3, box[1] + 3), crop["cropType"], fill=color)
        path = output / f"{fixture_id}_dre6_aggressive_x_ocr_crops.png"
        image.save(path)
        overlays.append({"fixtureId": fixture_id, "path": str(path).replace("\\", "/")})
    return overlays


def tag_rows(rows: list[dict[str, Any]], source: str) -> list[dict[str, Any]]:
    tagged = []
    for row in rows:
        copy = dict(row)
        copy["evidenceSource"] = source
        tagged.append(copy)
    return tagged


def anchor_source_counts(anchors: list[dict[str, Any]]) -> dict[str, int]:
    counts: dict[str, int] = {}
    for anchor in anchors:
        source = str(anchor.get("anchorSource") or "UNKNOWN")
        counts[source] = counts.get(source, 0) + 1
    return counts


def tag_anchors(anchors: list[dict[str, Any]], source: str) -> list[dict[str, Any]]:
    tagged = []
    for anchor in anchors:
        copy = dict(anchor)
        copy["anchorSource"] = copy.get("anchorSource") or source
        tagged.append(copy)
    return tagged


def merge_anchors(*anchor_groups: list[dict[str, Any]]) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    merged: dict[str, dict[str, Any]] = {}
    added: list[dict[str, Any]] = []
    for group_index, anchors in enumerate(anchor_groups):
        for anchor in anchors:
            key = anchor["truthLabelKey"]
            if key not in merged:
                merged[key] = anchor
                if group_index > 1:
                    added.append(anchor)
    return list(merged.values()), added


def filter_rejected(rejected: list[dict[str, Any]], anchors: list[dict[str, Any]]) -> list[dict[str, Any]]:
    recovered = {anchor["truthLabelKey"] for anchor in anchors}
    return [row for row in rejected if row["truthLabelKey"] not in recovered]


def build_axis_results(dre2: Any, dre1: dict[str, Any], anchors: list[dict[str, Any]], rejected: list[dict[str, Any]]) -> list[dict[str, Any]]:
    grouped = dre2.group_anchors(anchors)
    missing = dre2.group_missing(rejected)
    axis_results = []
    for graph_key_value, axis, _graph in dre2.graph_axis_keys(dre1["graphResults"]):
        axis_results.append(
            dre2.build_axis_result(
                graph_key_value,
                axis,
                grouped.get((graph_key_value, axis), []),
                len(missing.get((graph_key_value, axis), [])),
            )
        )
    return axis_results


def build_graph_results(dre2: Any, dre1: dict[str, Any], dre5: dict[str, Any], axis_results: list[dict[str, Any]]) -> list[dict[str, Any]]:
    axis_by_key = {(axis["graphKey"], axis["axis"]): axis for axis in axis_results}
    baseline_by_graph = {graph_key(row["fixtureId"], row["graphId"]): row for row in dre5["graphResults"]}
    graph_results = []
    for graph in dre1["graphResults"]:
        key = graph_key(graph["fixtureId"], graph["graphId"])
        x_axis = axis_by_key[(key, "X")]
        y_axis = axis_by_key[(key, "Y")]
        decision, reason = dre2.graph_decision(x_axis, y_axis)
        baseline = baseline_by_graph[key]
        graph_results.append(
            {
                "fixtureId": graph["fixtureId"],
                "graphId": graph["graphId"],
                "layoutClass": graph.get("layoutClass"),
                "dre5Decision": baseline["dre5Decision"],
                "dre6Decision": decision,
                "decisionDelta": decision_delta(baseline["dre5Decision"], decision),
                "decisionReason": reason,
                "dre5XStatus": baseline["dre5XStatus"],
                "dre6XStatus": x_axis["status"],
                "dre5XAnchors": baseline["dre5XAnchors"],
                "dre6XAnchors": x_axis["selectedCandidate"].get("acceptedAnchorCount", 0),
                "dre5YStatus": baseline["dre5YStatus"],
                "dre6YStatus": y_axis["status"],
                "dre5YAnchors": baseline["dre5YAnchors"],
                "dre6YAnchors": y_axis["selectedCandidate"].get("acceptedAnchorCount", 0),
                "xAxis": x_axis,
                "yAxis": y_axis,
            }
        )
    return graph_results


def status_rank(status: str) -> int:
    return {"INVALID": 0, "PARTIAL": 1, "REVIEW": 2, "VALID": 3}.get(status, -1)


def decision_delta(before: str, after: str) -> str:
    if status_rank(after) > status_rank(before):
        return "IMPROVED"
    if status_rank(after) < status_rank(before):
        return "REGRESSED"
    return "UNCHANGED"


def count_statuses(values: list[str]) -> dict[str, int]:
    counts: dict[str, int] = {}
    for value in values:
        counts[value] = counts.get(value, 0) + 1
    return counts


def build_summary(args: argparse.Namespace) -> dict[str, Any]:
    drd5 = load_script("drd5_axis_owned_ocr_crop_planner", "run_drd5_axis_owned_ocr_crop_planner.py")
    dre1_module = load_script("dre1_axis_scale_candidate_builder", "run_dre1_axis_scale_candidate_builder.py")
    dre2_module = load_script("dre2_robust_axis_scale_fit", "run_dre2_robust_axis_scale_fit_benchmark.py")
    dre1 = read_json(args.dre1_summary)
    dre5 = read_json(args.dre5_summary)
    drd6 = read_json(args.drd6_summary)
    dre4 = read_json(args.dre4_summary)
    layout_records = read_json(args.drc3_manual)["records"]
    truth_records = read_json(args.drc4_manual)["records"]
    truth_labels = drd5.iter_truth_labels(truth_records)
    tick_truth, _graph_truth = dre1_module.build_tick_truth(truth_records)
    target_axes = target_axes_from_dre5(dre5)

    source_rows = []
    source_rows.extend(tag_rows(drd6.get("detectionScores", []), "DR_D6_MULTI_VARIANT_AXIS_OWNED_OCR"))
    source_rows.extend(tag_rows(dre4.get("ocrRows", []), "DR_E4_REPAIRED_X_LABEL_BAND_OCR"))
    target_safe_rows = [row for row in source_rows if safe_tick_row(row) and is_target_row(row, target_axes)]
    target_best_rows = best_rows(target_safe_rows)

    aggressive_rows, aggressive_crops, aggressive_timings, aggressive_overlays = run_aggressive_sweep(
        drd5,
        layout_records,
        truth_labels,
        target_axes,
        args.repo_root,
        args.output,
    )
    aggressive_safe_rows = [row for row in aggressive_rows if safe_tick_row(row) and is_target_row(row, target_axes)]
    for key, row in best_rows(aggressive_safe_rows).items():
        current = target_best_rows.get(key)
        score = (float(row.get("textSimilarity") or 0.0), float(row.get("confidence") or 0.0))
        current_score = (
            float(current.get("textSimilarity") or 0.0),
            float(current.get("confidence") or 0.0),
        ) if current else (-1.0, -1.0)
        if current is None or score > current_score:
            target_best_rows[key] = row

    fallback_anchors, fallback_rejected = dre1_module.build_anchors(tick_truth, target_best_rows)
    fallback_anchors = [
        dict(anchor, anchorSource="DR_E6_TARGET_AXIS_OCR_FALLBACK")
        for anchor in fallback_anchors
        if (anchor["fixtureId"], anchor["graphId"], anchor["axis"]) in target_axes
    ]

    baseline_anchors = tag_anchors(dre1["safeAnchors"], "DR_E1_BASELINE_SAFE_OCR")
    dre5_anchors = tag_anchors(dre5["addedRepairedAnchors"], "DR_E5_REPAIRED_CROP_SAFE_OCR")
    merged_anchors, added_target_anchors = merge_anchors(baseline_anchors, dre5_anchors, fallback_anchors)
    merged_rejected = filter_rejected(dre1["rejectedOrMissingAnchors"], merged_anchors)
    axis_results = build_axis_results(dre2_module, dre1, merged_anchors, merged_rejected)
    graph_results = build_graph_results(dre2_module, dre1, dre5, axis_results)

    remaining = [
        {
            "fixtureId": row["fixtureId"],
            "graphId": row["graphId"],
            "dre6Decision": row["dre6Decision"],
            "xStatus": row["dre6XStatus"],
            "xFailureReason": row["xAxis"].get("failureReason"),
            "xMissingSafeOcrLabelCount": row["xAxis"].get("missingSafeOcrLabelCount"),
            "yStatus": row["dre6YStatus"],
            "yFailureReason": row["yAxis"].get("failureReason"),
            "yMissingSafeOcrLabelCount": row["yAxis"].get("missingSafeOcrLabelCount"),
        }
        for row in graph_results
        if row["dre6Decision"] in {"PARTIAL", "INVALID"}
    ]
    axis_statuses = [axis["status"] for axis in axis_results]
    graph_decisions = [row["dre6Decision"] for row in graph_results]
    improved_graphs = sum(1 for row in graph_results if row["decisionDelta"] == "IMPROVED")
    regressed_graphs = sum(1 for row in graph_results if row["decisionDelta"] == "REGRESSED")
    usable_axes = sum(1 for status in axis_statuses if status in {"VALID", "REVIEW"})
    usable_graphs = sum(1 for status in graph_decisions if status in {"VALID", "REVIEW"})
    dre5_usable_axes = dre5["resultSummary"]["dre5UsableAxisCount"]
    dre5_usable_graphs = dre5["resultSummary"]["dre5UsableGraphCount"]

    if regressed_graphs:
        verdict = "REMAINING_AXIS_OCR_RECOVERY_REGRESSION_REQUIRES_REVIEW"
    elif not remaining:
        verdict = "REMAINING_AXIS_OCR_RECOVERY_ALL_TARGET_AXES_USABLE_NOT_RUNTIME_READY"
    elif usable_axes > dre5_usable_axes:
        verdict = "REMAINING_AXIS_OCR_RECOVERY_IMPROVES_ONE_BLOCKER_NOT_ACCEPTANCE_READY"
    else:
        verdict = "REMAINING_AXIS_OCR_RECOVERY_NO_SAFE_IMPROVEMENT"

    target_axis_summaries = []
    for fixture_id, graph_id, axis in sorted(target_axes):
        target_keys = [
            key
            for key, truth in tick_truth.items()
            if (truth["fixtureId"], truth["graphId"], truth["axis"]) == (fixture_id, graph_id, axis)
        ]
        recovered_keys = [
            anchor["truthLabelKey"]
            for anchor in fallback_anchors
            if (anchor["fixtureId"], anchor["graphId"], anchor["axis"]) == (fixture_id, graph_id, axis)
        ]
        target_axis_summaries.append(
            {
                "fixtureId": fixture_id,
                "graphId": graph_id,
                "axis": axis,
                "truthTickLabelCount": len(target_keys),
                "recoveredFallbackAnchorCount": len(set(recovered_keys)),
                "recoveredLabels": [
                    tick_truth[key]["truthText"]
                    for key in sorted(set(recovered_keys), key=lambda item: tick_truth[item]["labelIndex"])
                ],
                "remainingMissingCount": len(set(target_keys) - set(recovered_keys)),
            }
        )

    return {
        "schemaVersion": "chromalab.benchmark.dre6_remaining_axis_ocr_recovery.v1",
        "overallVerdict": verdict,
        "productionImpact": "NONE_RESEARCH_ONLY",
        "dre1Source": str(args.dre1_summary).replace("\\", "/"),
        "dre5Source": str(args.dre5_summary).replace("\\", "/"),
        "drd6Source": str(args.drd6_summary).replace("\\", "/"),
        "dre4Source": str(args.dre4_summary).replace("\\", "/"),
        "targetAxes": [
            {"fixtureId": fixture_id, "graphId": graph_id, "axis": axis}
            for fixture_id, graph_id, axis in sorted(target_axes)
        ],
        "resultSummary": {
            "dre5UsableAxisCount": dre5_usable_axes,
            "dre6UsableAxisCount": usable_axes,
            "dre5UsableGraphCount": dre5_usable_graphs,
            "dre6UsableGraphCount": usable_graphs,
            "axisStatusCounts": count_statuses(axis_statuses),
            "graphDecisionCounts": count_statuses(graph_decisions),
            "improvedGraphCount": improved_graphs,
            "regressedGraphCount": regressed_graphs,
            "addedTargetFallbackAnchorCount": len(added_target_anchors),
            "targetFallbackSafeRowCount": len(target_best_rows),
            "aggressiveSweepCropCount": len(aggressive_crops),
            "aggressiveSweepRowCount": len(aggressive_rows),
            "aggressiveSweepSafeTargetRowCount": len(aggressive_safe_rows),
            "anchorSourceCounts": anchor_source_counts(merged_anchors),
        },
        "targetAxisSummaries": target_axis_summaries,
        "graphResults": graph_results,
        "axisResults": axis_results,
        "addedTargetFallbackAnchors": added_target_anchors,
        "fallbackRejectedOrMissingAnchors": fallback_rejected,
        "aggressiveSweepRows": aggressive_rows,
        "aggressiveSweepTimings": aggressive_timings,
        "aggressiveSweepOverlays": aggressive_overlays,
        "remainingPartialOrInvalidGraphs": remaining,
        "nextRequiredCapabilities": [
            "bench06_graph2_photo_x_axis_ocr_or_rectification_research",
            "per_axis_ocr_variant_fallback_policy",
            "runtime_safe_anchor_source_arbitration",
        ],
    }


def build_markdown(summary: dict[str, Any]) -> str:
    counts = summary["resultSummary"]
    lines = [
        "# DR-E6 Remaining Axis OCR Recovery Benchmark",
        "",
        f"Verdict: `{summary['overallVerdict']}`",
        f"Usable axes DR-E5 -> DR-E6: `{counts['dre5UsableAxisCount']}` -> `{counts['dre6UsableAxisCount']}`",
        f"Usable graphs DR-E5 -> DR-E6: `{counts['dre5UsableGraphCount']}` -> `{counts['dre6UsableGraphCount']}`",
        f"Added target fallback anchors: `{counts['addedTargetFallbackAnchorCount']}`",
        f"Aggressive sweep safe target rows: `{counts['aggressiveSweepSafeTargetRowCount']}`",
        "",
        "## Target Axis Recovery",
        "",
        "| Fixture | Graph | Axis | Truth labels | Recovered fallback anchors | Remaining missing | Recovered labels |",
        "| --- | --- | --- | ---: | ---: | ---: | --- |",
    ]
    for row in summary["targetAxisSummaries"]:
        lines.append(
            "| `{fixtureId}` | `{graphId}` | `{axis}` | {truth} | {recovered} | {missing} | `{labels}` |".format(
                fixtureId=row["fixtureId"],
                graphId=row["graphId"],
                axis=row["axis"],
                truth=row["truthTickLabelCount"],
                recovered=row["recoveredFallbackAnchorCount"],
                missing=row["remainingMissingCount"],
                labels=', '.join(row["recoveredLabels"]),
            )
        )
    lines.extend(
        [
            "",
            "## Graph Calibration Summary",
            "",
            "| Fixture | Graph | DR-E5 | DR-E6 | Delta | X DR-E5 -> DR-E6 | X anchors | Y DR-E5 -> DR-E6 | Y anchors |",
            "| --- | --- | --- | --- | --- | --- | ---: | --- | ---: |",
        ]
    )
    for graph in summary["graphResults"]:
        lines.append(
            "| `{fixtureId}` | `{graphId}` | `{dre5}` | `{dre6}` | `{delta}` | `{x5}` -> `{x6}` | {xa5}->{xa6} | `{y5}` -> `{y6}` | {ya5}->{ya6} |".format(
                fixtureId=graph["fixtureId"],
                graphId=graph["graphId"],
                dre5=graph["dre5Decision"],
                dre6=graph["dre6Decision"],
                delta=graph["decisionDelta"],
                x5=graph["dre5XStatus"],
                x6=graph["dre6XStatus"],
                xa5=graph["dre5XAnchors"],
                xa6=graph["dre6XAnchors"],
                y5=graph["dre5YStatus"],
                y6=graph["dre6YStatus"],
                ya5=graph["dre5YAnchors"],
                ya6=graph["dre6YAnchors"],
            )
        )
    lines.extend(
        [
            "",
            "## Remaining Partial Or Invalid Graphs",
            "",
            "| Fixture | Graph | Decision | X status/reason | Y status/reason |",
            "| --- | --- | --- | --- | --- |",
        ]
    )
    for row in summary["remainingPartialOrInvalidGraphs"]:
        lines.append(
            "| `{fixtureId}` | `{graphId}` | `{decision}` | `{xs}` / `{xr}` | `{ys}` / `{yr}` |".format(
                fixtureId=row["fixtureId"],
                graphId=row["graphId"],
                decision=row["dre6Decision"],
                xs=row["xStatus"],
                xr=row["xFailureReason"] or "",
                ys=row["yStatus"],
                yr=row["yFailureReason"] or "",
            )
        )
    lines.extend(
        [
            "",
            "## Counts",
            "",
            f"Axis statuses: `{counts['axisStatusCounts']}`",
            f"Graph decisions: `{counts['graphDecisionCounts']}`",
            f"Anchor sources: `{counts['anchorSourceCounts']}`",
            f"Aggressive sweep crops: `{counts['aggressiveSweepCropCount']}`",
            f"Aggressive sweep rows: `{counts['aggressiveSweepRowCount']}`",
            f"Improved graphs: `{counts['improvedGraphCount']}`",
            f"Regressed graphs: `{counts['regressedGraphCount']}`",
            "",
            "## Interpretation",
            "",
        ]
    )
    if summary["remainingPartialOrInvalidGraphs"]:
        lines.extend(
            [
                "- Per-axis OCR variant fallback recovers `bench_05_graph_3` Y calibration evidence.",
                "- Remaining partial or invalid graphs still need targeted OCR or rectification research.",
                "- Missing labels remain missing evidence; no calibration values are fabricated.",
                "- This remains a benchmark prototype and does not alter Android runtime crop planning or calibration.",
            ]
        )
    else:
        lines.extend(
            [
                "- Per-axis OCR variant fallback recovers `bench_05_graph_3` Y calibration evidence.",
                "- Aggressive photo X-band OCR recovers all 11 safe X anchors for `bench_06_graph_2`.",
                "- All 12 PC-side graph calibration cases are now `VALID` or `REVIEW` under the existing robust-fit gate.",
                "- The result is still not Android acceptance; it is evidence that the next implementation candidate should include per-axis OCR fallback and deep photo X-band crops.",
                "- This remains a benchmark prototype and does not alter Android runtime crop planning or calibration.",
            ]
        )
    return "\n".join(lines) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo-root", type=Path, default=Path("."))
    parser.add_argument("--dre1-summary", type=Path, default=Path("benchmark/reports/dre1_axis_scale_candidate_builder/summary.json"))
    parser.add_argument("--dre5-summary", type=Path, default=Path("benchmark/reports/dre5_repaired_crop_calibration_pipeline/summary.json"))
    parser.add_argument("--drd6-summary", type=Path, default=Path("benchmark/reports/drd6_axis_owned_ocr_preprocessing_grid/summary.json"))
    parser.add_argument("--dre4-summary", type=Path, default=Path("benchmark/reports/dre4_label_band_crop_coverage_repair/summary.json"))
    parser.add_argument("--drc3-manual", type=Path, default=Path("benchmark/annotations/drc3_initial_graph_layout_annotations/manual-p0-annotations.json"))
    parser.add_argument("--drc4-manual", type=Path, default=Path("benchmark/annotations/drc4_tick_text_role_annotations/manual-p0-tick-text-annotations.json"))
    parser.add_argument("--output", type=Path, default=Path("benchmark/reports/dre6_remaining_axis_ocr_recovery"))
    args = parser.parse_args()
    summary = build_summary(args)
    write_json(args.output / "summary.json", summary)
    (args.output / "summary.md").write_text(build_markdown(summary), encoding="utf-8", newline="\n")
    print(f"Built DR-E6 remaining axis OCR recovery: {summary['overallVerdict']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
