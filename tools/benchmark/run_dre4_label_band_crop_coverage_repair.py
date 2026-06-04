#!/usr/bin/env python3
"""Prototype label-band crop coverage repair for missing X anchors.

This PC-side benchmark creates repaired X tick-label crops for axes that remain
non-valid after DR-E2, runs RapidOCR on those crops, and measures recovered safe
anchors. It does not change Android runtime or production analysis.
"""

from __future__ import annotations

import argparse
import importlib.util
import json
import math
import statistics
import time
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image, ImageDraw


METHOD_ID = "rapidocr_repaired_x_label_band_rgb_x3_p2_v1"


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
    if x1 - x0 < 12 or y1 - y0 < 12:
        return None
    return (x0, y0, x1, y1)


def box_center(box: tuple[float, float, float, float]) -> tuple[float, float]:
    return ((box[0] + box[2]) / 2.0, (box[1] + box[3]) / 2.0)


def point_inside(point: tuple[float, float], box: tuple[float, float, float, float], margin: float = 0.0) -> bool:
    return box[0] - margin <= point[0] <= box[2] + margin and box[1] - margin <= point[1] <= box[3] + margin


def target_x_axes(dre3: dict[str, Any]) -> set[tuple[str, str]]:
    targets: set[tuple[str, str]] = set()
    for row in dre3.get("axisSummaries", []):
        if row["axis"] == "X" and row["missingAnchorCount"] > 0:
            targets.add((row["fixtureId"], row["graphId"]))
    return targets


def layout_graphs(layout_records: list[dict[str, Any]]) -> dict[tuple[str, str], dict[str, Any]]:
    graphs: dict[tuple[str, str], dict[str, Any]] = {}
    for record in layout_records:
        for graph in record.get("graphs", []):
            graphs[(record["fixtureId"], graph["graphId"])] = {
                "fixtureId": record["fixtureId"],
                "sourceImage": record["sourceImage"],
                "image": record["image"],
                "layoutClass": record.get("layoutClass"),
                "graph": graph,
            }
    return graphs


def build_repaired_x_crops(layout: dict[str, Any]) -> list[dict[str, Any]]:
    fixture_id = layout["fixtureId"]
    graph = layout["graph"]
    graph_id = graph["graphId"]
    image_width = int(layout["image"]["width"])
    image_height = int(layout["image"]["height"])
    plot = bbox_from_xywh(graph["plotArea"])
    panel = bbox_from_xywh(graph["graphPanel"])

    # Repair rule: allow the X label band to extend below the panel when panel
    # segmentation hugs the axis line too tightly. This is still geometry-based:
    # it uses plotArea, graphPanel, and image bounds, not fixture coordinates.
    y0 = plot[3] - 10.0
    y1 = max(panel[3] + 22.0, plot[3] + 42.0)
    x0 = plot[0] - 24.0
    x1 = plot[2] + 24.0
    wide = clamp_box((x0, y0, x1, y1), image_width, image_height)
    crops: list[dict[str, Any]] = []
    if wide is None:
        return crops
    crops.append(
        {
            "cropId": f"{fixture_id}_{graph_id}_x_tick_label_band_repaired_wide_1",
            "fixtureId": fixture_id,
            "graphId": graph_id,
            "cropType": "x_tick_label_band_repaired_wide",
            "context": "tick_label_candidate_context",
            "bboxTuple": wide,
            "bbox": bbox_to_json(wide),
            "source": "plot_bottom_extended_below_panel",
        }
    )

    width = wide[2] - wide[0]
    tile_count = max(4, min(8, int(math.ceil(width / 120.0))))
    tile_width = width / tile_count
    overlap = min(28.0, tile_width * 0.3)
    for index in range(tile_count):
        tile_x0 = wide[0] + index * tile_width - overlap
        tile_x1 = wide[0] + (index + 1) * tile_width + overlap
        tile = clamp_box((tile_x0, wide[1], tile_x1, wide[3]), image_width, image_height)
        if tile is None:
            continue
        crops.append(
            {
                "cropId": f"{fixture_id}_{graph_id}_x_tick_label_band_repaired_tile_{index + 1}",
                "fixtureId": fixture_id,
                "graphId": graph_id,
                "cropType": "x_tick_label_band_repaired_tile",
                "context": "tick_label_candidate_context",
                "bboxTuple": tile,
                "bbox": bbox_to_json(tile),
                "source": "overlapping_dense_label_band_tile",
            }
        )
    return crops


def run_rapidocr_on_crop(drd5: Any, ocr: Any, image: Image.Image, crop: dict[str, Any]) -> tuple[list[dict[str, Any]], float]:
    scale = 3
    pad = 2
    box = bbox_from_json(crop["bbox"])
    source_box = clamp_box((box[0] - pad, box[1] - pad, box[2] + pad, box[3] + pad), image.width, image.height) or box
    crop_image = image.crop(tuple(int(round(value)) for value in source_box)).convert("RGB")
    crop_image = crop_image.resize((crop_image.width * scale, crop_image.height * scale), Image.Resampling.LANCZOS)
    started = time.perf_counter()
    output = ocr(np.array(crop_image), use_det=True, use_cls=True, use_rec=True)
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
                "detectionId": f"{crop['cropId']}_{METHOD_ID}_{index + 1}",
                "methodId": METHOD_ID,
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


def safe_anchor_keys_from_rows(rows: list[dict[str, Any]]) -> set[str]:
    keys = set()
    for row in rows:
        if (
            row.get("matchedTruth")
            and row.get("truthRole") == "tick_label"
            and row.get("safePredictedRole") == "tick_label"
            and not row.get("safeFalseTickLabel")
            and row.get("truthLabelKey")
        ):
            keys.add(row["truthLabelKey"])
    return keys


def build_truth_label_index(labels: list[dict[str, Any]]) -> dict[str, dict[str, Any]]:
    return {label["labelKey"]: label for label in labels if label.get("truthRole") == "tick_label"}


def label_is_in_crop(label: dict[str, Any], crop: dict[str, Any]) -> bool:
    return point_inside(box_center(label["bboxTuple"]), bbox_from_json(crop["bbox"]), margin=4.0)


def summarize_axis(
    fixture_id: str,
    graph_id: str,
    labels: list[dict[str, Any]],
    baseline_safe: set[str],
    repaired_safe: set[str],
    crops: list[dict[str, Any]],
) -> dict[str, Any]:
    axis_labels = [
        label
        for label in labels
        if label["fixtureId"] == fixture_id
        and label["graphId"] == graph_id
        and label.get("truthRole") == "tick_label"
        and "_xlabel_" in str(label.get("textId") or "")
    ]
    baseline_count = sum(1 for label in axis_labels if label["labelKey"] in baseline_safe)
    repaired_count = sum(1 for label in axis_labels if label["labelKey"] in repaired_safe)
    recovered = [
        label
        for label in axis_labels
        if label["labelKey"] not in baseline_safe and label["labelKey"] in repaired_safe
    ]
    covered_after_repair = [
        label
        for label in axis_labels
        if any(label_is_in_crop(label, crop) for crop in crops)
    ]
    return {
        "fixtureId": fixture_id,
        "graphId": graph_id,
        "truthXLabelCount": len(axis_labels),
        "baselineSafeXAnchorCount": baseline_count,
        "repairedSafeXAnchorCount": repaired_count,
        "recoveredSafeXAnchorCount": len(recovered),
        "coveredAfterRepairCount": len(covered_after_repair),
        "remainingMissingCount": len(axis_labels) - repaired_count,
        "recoveredLabels": [label["truthText"] for label in recovered],
    }


def render_overlays(repo_root: Path, layout_by_key: dict[tuple[str, str], dict[str, Any]], crops_by_axis: dict[tuple[str, str], list[dict[str, Any]]], output: Path) -> list[dict[str, Any]]:
    output.mkdir(parents=True, exist_ok=True)
    overlays: list[dict[str, Any]] = []
    by_fixture: dict[str, list[dict[str, Any]]] = {}
    for crops in crops_by_axis.values():
        for crop in crops:
            by_fixture.setdefault(crop["fixtureId"], []).append(crop)
    source_by_fixture = {layout["fixtureId"]: layout["sourceImage"] for layout in layout_by_key.values()}
    for fixture_id, crops in by_fixture.items():
        image = Image.open(repo_root / source_by_fixture[fixture_id]).convert("RGB")
        draw = ImageDraw.Draw(image)
        for crop in crops:
            box = bbox_from_json(crop["bbox"])
            color = "#00a3ff" if crop["cropType"].endswith("wide") else "#ff2d55"
            draw.rectangle(box, outline=color, width=3)
            draw.text((box[0] + 3, box[1] + 3), crop["cropType"], fill=color)
        path = output / f"{fixture_id}_dre4_repaired_x_label_bands.png"
        image.save(path)
        overlays.append({"fixtureId": fixture_id, "path": str(path).replace("\\", "/")})
    return overlays


def build_summary(args: argparse.Namespace) -> dict[str, Any]:
    drd5 = load_script("drd5", "run_drd5_axis_owned_ocr_crop_planner.py")
    dre1 = load_script("dre1", "run_dre1_axis_scale_candidate_builder.py")
    dre2 = load_script("dre2", "run_dre2_robust_axis_scale_fit_benchmark.py")

    truth = read_json(args.drc4_manual)
    layout = read_json(args.drc3_manual)
    dre1_summary = read_json(args.dre1_summary)
    dre2_summary = read_json(args.dre2_summary)
    dre3_summary = read_json(args.dre3_summary)

    targets = target_x_axes(dre3_summary)
    layout_by_key = layout_graphs(layout["records"])
    truth_labels = drd5.iter_truth_labels(truth["records"])
    truth_label_index = build_truth_label_index(truth_labels)
    baseline_safe = {anchor["truthLabelKey"] for anchor in dre1_summary.get("safeAnchors", [])}

    crops_by_axis = {
        key: build_repaired_x_crops(layout_by_key[key])
        for key in targets
        if key in layout_by_key
    }
    overlays = render_overlays(args.repo_root, layout_by_key, crops_by_axis, args.output / "overlays")

    from rapidocr import RapidOCR

    ocr = RapidOCR()
    rows: list[dict[str, Any]] = []
    crop_timings: list[dict[str, Any]] = []
    images: dict[str, Image.Image] = {}
    for key, crops in crops_by_axis.items():
        layout_info = layout_by_key[key]
        fixture_id = layout_info["fixtureId"]
        if fixture_id not in images:
            images[fixture_id] = Image.open(args.repo_root / layout_info["sourceImage"]).convert("RGB")
        image = images[fixture_id]
        for crop in crops:
            owned = [
                label
                for label in truth_labels
                if label["fixtureId"] == crop["fixtureId"]
                and label["graphId"] == crop["graphId"]
                and label.get("truthRole") == "tick_label"
                and label_is_in_crop(label, crop)
            ]
            detections, elapsed = run_rapidocr_on_crop(drd5, ocr, image, crop)
            rows.extend(drd5.match_crop_detections(METHOD_ID, crop, detections, owned))
            crop_timings.append(
                {
                    "cropId": crop["cropId"],
                    "fixtureId": crop["fixtureId"],
                    "graphId": crop["graphId"],
                    "cropType": crop["cropType"],
                    "detectionCount": len(detections),
                    "elapsedSec": round(elapsed, 4),
                }
            )

    repaired_safe = baseline_safe | safe_anchor_keys_from_rows(rows)
    axis_summaries = [
        summarize_axis(fixture_id, graph_id, truth_labels, baseline_safe, repaired_safe, crops_by_axis[(fixture_id, graph_id)])
        for fixture_id, graph_id in sorted(crops_by_axis)
    ]

    # Re-run robust fit only for target X axes with baseline + repaired safe anchors.
    tick_truth, _graph_truth = dre1.build_tick_truth(truth["records"])
    repaired_ocr_rows = {
        row["truthLabelKey"]: row
        for row in rows
        if row.get("truthLabelKey") in truth_label_index
        and row.get("safePredictedRole") == "tick_label"
        and row.get("matchedTruth")
    }
    repaired_anchors, _repaired_rejected = dre1.build_anchors(tick_truth, repaired_ocr_rows)
    anchor_by_key = {anchor["truthLabelKey"]: anchor for anchor in dre1_summary["safeAnchors"]}
    for anchor in repaired_anchors:
        anchor_by_key[anchor["truthLabelKey"]] = anchor
    grouped = dre2.group_anchors(list(anchor_by_key.values()))
    robust_results = []
    dre2_axis_by_key = {(axis["graphKey"], axis["axis"]): axis for axis in dre2_summary["axisResults"]}
    for fixture_id, graph_id in sorted(crops_by_axis):
        graph_key = f"{fixture_id}::{graph_id}"
        repaired_axis = dre2.build_axis_result(
            graph_key,
            "X",
            grouped.get((graph_key, "X"), []),
            0,
        )
        baseline_axis = dre2_axis_by_key.get((graph_key, "X"))
        robust_results.append(
            {
                "fixtureId": fixture_id,
                "graphId": graph_id,
                "baselineStatus": baseline_axis["status"] if baseline_axis else None,
                "baselineStrategy": baseline_axis["selectedStrategyId"] if baseline_axis else None,
                "repairedStatus": repaired_axis["status"],
                "repairedStrategy": repaired_axis["selectedStrategyId"],
                "repairedAcceptedAnchorCount": repaired_axis["selectedCandidate"].get("acceptedAnchorCount", 0),
                "repairedFailureReason": repaired_axis.get("failureReason"),
            }
        )

    recovered_total = sum(row["recoveredSafeXAnchorCount"] for row in axis_summaries)
    axes_improved = sum(
        1
        for row in axis_summaries
        if row["repairedSafeXAnchorCount"] > row["baselineSafeXAnchorCount"]
    )
    invalid_to_usable = sum(
        1
        for row in robust_results
        if row["baselineStatus"] == "INVALID" and row["repairedStatus"] in {"VALID", "REVIEW"}
    )
    if invalid_to_usable:
        verdict = "LABEL_BAND_COVERAGE_REPAIR_RECOVERS_SCALE_CANDIDATES"
    elif recovered_total:
        verdict = "LABEL_BAND_COVERAGE_REPAIR_RECOVERS_ANCHORS_NOT_SCALE_READY"
    else:
        verdict = "LABEL_BAND_COVERAGE_REPAIR_NO_RECOVERY"

    return {
        "schemaVersion": "chromalab.benchmark.dre4_label_band_crop_coverage_repair.v1",
        "overallVerdict": verdict,
        "productionImpact": "NONE_RESEARCH_ONLY",
        "truthSource": str(args.drc4_manual).replace("\\", "/"),
        "layoutSource": str(args.drc3_manual).replace("\\", "/"),
        "dre1Source": str(args.dre1_summary).replace("\\", "/"),
        "dre2Source": str(args.dre2_summary).replace("\\", "/"),
        "dre3Source": str(args.dre3_summary).replace("\\", "/"),
        "methodId": METHOD_ID,
        "targetAxisCount": len(crops_by_axis),
        "repairedCropCount": sum(len(crops) for crops in crops_by_axis.values()),
        "recoveredSafeXAnchorCount": recovered_total,
        "axesWithAnchorImprovementCount": axes_improved,
        "invalidToUsableAxisCount": invalid_to_usable,
        "axisSummaries": axis_summaries,
        "robustFitAfterRepair": robust_results,
        "cropTimings": crop_timings,
        "cropPlanOverlays": overlays,
        "ocrRows": rows,
        "nextRequiredCapabilities": [
            "integrate_repaired_x_label_band_crop_rule_into_axis_owned_planner",
            "dense_label_band_tile_OCR_recovery",
            "rerun_DRD6_DRE1_DRE2_with_repaired_crop_plans",
            "preserve_missing_anchor_evidence_for_unrecovered_labels",
        ],
    }


def build_markdown(summary: dict[str, Any]) -> str:
    lines = [
        "# DR-E4 Label-Band Crop Coverage Repair Prototype",
        "",
        f"Verdict: `{summary['overallVerdict']}`",
        f"Method: `{summary['methodId']}`",
        f"Target X axes: `{summary['targetAxisCount']}`",
        f"Repaired crops: `{summary['repairedCropCount']}`",
        f"Recovered safe X anchors: `{summary['recoveredSafeXAnchorCount']}`",
        "",
        "## Axis Recovery Summary",
        "",
        "| Fixture | Graph | Truth X labels | Baseline safe | Repaired safe | Recovered | Remaining missing |",
        "| --- | --- | ---: | ---: | ---: | ---: | ---: |",
    ]
    for row in summary["axisSummaries"]:
        lines.append(
            "| `{fixtureId}` | `{graphId}` | {truthXLabelCount} | {baselineSafeXAnchorCount} | {repairedSafeXAnchorCount} | {recoveredSafeXAnchorCount} | {remainingMissingCount} |".format(
                **row
            )
        )
    lines.extend(
        [
            "",
            "## Robust Fit After Repair",
            "",
            "| Fixture | Graph | Baseline X | Repaired X | Repaired anchors | Failure reason |",
            "| --- | --- | --- | --- | ---: | --- |",
        ]
    )
    for row in summary["robustFitAfterRepair"]:
        lines.append(
            "| `{fixtureId}` | `{graphId}` | `{baselineStatus}` | `{repairedStatus}` | {repairedAcceptedAnchorCount} | `{reason}` |".format(
                reason=row.get("repairedFailureReason") or "",
                **row,
            )
        )
    lines.extend(
        [
            "",
            "## Overlays",
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
            "- Extending X label bands below tight graph panels can recover anchors that DR-E3 classified as coverage gaps.",
            "- Overlapping dense tiles are now measured separately from wide crops.",
            "- Recovered anchors are still OCR evidence only; robust fit must accept them before calibration can use them.",
            "- This remains a benchmark prototype and does not alter Android runtime crop planning.",
        ]
    )
    return "\n".join(lines) + "\n"


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
        "--dre1-summary",
        type=Path,
        default=Path("benchmark/reports/dre1_axis_scale_candidate_builder/summary.json"),
    )
    parser.add_argument(
        "--dre2-summary",
        type=Path,
        default=Path("benchmark/reports/dre2_robust_axis_scale_fit/summary.json"),
    )
    parser.add_argument(
        "--dre3-summary",
        type=Path,
        default=Path("benchmark/reports/dre3_missing_anchor_coverage/summary.json"),
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("benchmark/reports/dre4_label_band_crop_coverage_repair"),
    )
    args = parser.parse_args()
    summary = build_summary(args)
    write_json(args.output / "summary.json", summary)
    (args.output / "summary.md").write_text(build_markdown(summary), encoding="utf-8", newline="\n")
    print(f"Built DR-E4 label-band crop coverage repair: {summary['overallVerdict']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
