#!/usr/bin/env python3
"""Classify missing axis anchors by crop coverage and OCR detection evidence.

This PC-side benchmark consumes DR-D5 crop plans, DR-D6 OCR evidence, DR-E1
missing anchors, and DR-E2 robust fit results. It identifies whether remaining
scale blockers are caused by crop coverage, OCR detection, OCR text/role, or
matching gaps. It does not change Android runtime or production analysis.
"""

from __future__ import annotations

import argparse
import json
import math
from collections import Counter
from pathlib import Path
from typing import Any


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


def bbox_from_json(box: dict[str, Any] | None) -> tuple[float, float, float, float] | None:
    if not box:
        return None
    return (float(box["x0"]), float(box["y0"]), float(box["x1"]), float(box["y1"]))


def bbox_center(box: tuple[float, float, float, float]) -> tuple[float, float]:
    return ((box[0] + box[2]) / 2.0, (box[1] + box[3]) / 2.0)


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


def point_inside(point: tuple[float, float], box: tuple[float, float, float, float], margin: float = 0.0) -> bool:
    return box[0] - margin <= point[0] <= box[2] + margin and box[1] - margin <= point[1] <= box[3] + margin


def center_distance(a: tuple[float, float, float, float], b: tuple[float, float, float, float]) -> float:
    ax, ay = bbox_center(a)
    bx, by = bbox_center(b)
    return math.hypot(ax - bx, ay - by)


def axis_from_text_id(text_id: str) -> str | None:
    lowered = text_id.lower()
    if "_xlabel_" in lowered:
        return "X"
    if "_ylabel_" in lowered:
        return "Y"
    return None


def truth_label_key(fixture_id: str, graph_id: str, text_id: str) -> str:
    return f"{fixture_id}::{graph_id}::{text_id}"


def crop_type_for_axis(axis: str) -> str:
    return "x_tick_label_band" if axis == "X" else "y_tick_label_band"


def build_truth_labels(records: list[dict[str, Any]]) -> dict[str, dict[str, Any]]:
    labels: dict[str, dict[str, Any]] = {}
    for record in records:
        for graph in record.get("graphs", []):
            for label in graph.get("textRoleLabels", []):
                if label.get("role") != "tick_label":
                    continue
                text_id = str(label.get("textId") or label.get("labelId") or "")
                axis = axis_from_text_id(text_id)
                if axis is None:
                    continue
                key = truth_label_key(record["fixtureId"], graph["graphId"], text_id)
                bbox = bbox_from_xywh(label["bbox"])
                labels[key] = {
                    "truthLabelKey": key,
                    "fixtureId": record["fixtureId"],
                    "graphId": graph["graphId"],
                    "graphKey": f"{record['fixtureId']}::{graph['graphId']}",
                    "axis": axis,
                    "textId": text_id,
                    "truthText": label.get("text"),
                    "bbox": {"x0": bbox[0], "y0": bbox[1], "x1": bbox[2], "y1": bbox[3]},
                    "bboxTuple": bbox,
                }
    return labels


def index_crop_plans(crops: list[dict[str, Any]]) -> dict[tuple[str, str, str], list[dict[str, Any]]]:
    indexed: dict[tuple[str, str, str], list[dict[str, Any]]] = {}
    for crop in crops:
        indexed.setdefault((crop["fixtureId"], crop["graphId"], crop["cropType"]), []).append(crop)
    return indexed


def index_detections(drd6: dict[str, Any], variant_id: str) -> dict[tuple[str, str, str], list[dict[str, Any]]]:
    indexed: dict[tuple[str, str, str], list[dict[str, Any]]] = {}
    for row in drd6.get("detectionScores", []):
        if row.get("methodId") != variant_id:
            continue
        if not row.get("detectionId") or not row.get("bbox"):
            continue
        indexed.setdefault((row["fixtureId"], row["graphId"], row["cropType"]), []).append(row)
    return indexed


def safe_anchor_keys(dre1: dict[str, Any]) -> set[str]:
    return {anchor["truthLabelKey"] for anchor in dre1.get("safeAnchors", [])}


def missing_anchor_keys(dre1: dict[str, Any]) -> set[str]:
    return {row["truthLabelKey"] for row in dre1.get("rejectedOrMissingAnchors", [])}


def target_axis_keys(dre2: dict[str, Any]) -> set[tuple[str, str]]:
    targets: set[tuple[str, str]] = set()
    for axis in dre2.get("axisResults", []):
        if axis["status"] != "VALID":
            targets.add((axis["graphKey"], axis["axis"]))
    return targets


def find_covering_crops(label: dict[str, Any], crop_index: dict[tuple[str, str, str], list[dict[str, Any]]]) -> list[dict[str, Any]]:
    crop_type = crop_type_for_axis(label["axis"])
    crops = crop_index.get((label["fixtureId"], label["graphId"], crop_type), [])
    center = bbox_center(label["bboxTuple"])
    out = []
    for crop in crops:
        crop_box = bbox_from_json(crop["bbox"])
        if crop_box and point_inside(center, crop_box, margin=4.0):
            out.append(crop)
    return out


def nearby_detections(label: dict[str, Any], detections: list[dict[str, Any]]) -> list[dict[str, Any]]:
    label_box = label["bboxTuple"]
    out = []
    for row in detections:
        det_box = bbox_from_json(row.get("bbox"))
        if det_box is None:
            continue
        iou = bbox_iou(label_box, det_box)
        distance = center_distance(label_box, det_box)
        if iou >= 0.03 or distance <= 45.0:
            enriched = dict(row)
            enriched["nearbyIou"] = round(iou, 4)
            enriched["nearbyCenterDistance"] = round(distance, 2)
            out.append(enriched)
    out.sort(key=lambda item: (float(item.get("nearbyIou") or 0.0), -float(item.get("nearbyCenterDistance") or 9999.0)), reverse=True)
    return out


def matching_rows_for_label(drd6: dict[str, Any], variant_id: str, key: str) -> list[dict[str, Any]]:
    return [
        row
        for row in drd6.get("detectionScores", [])
        if row.get("methodId") == variant_id and row.get("truthLabelKey") == key
    ]


def classify_missing_label(
    label: dict[str, Any],
    *,
    drd6: dict[str, Any],
    variant_id: str,
    crop_index: dict[tuple[str, str, str], list[dict[str, Any]]],
    detection_index: dict[tuple[str, str, str], list[dict[str, Any]]],
) -> dict[str, Any]:
    crop_type = crop_type_for_axis(label["axis"])
    covering = find_covering_crops(label, crop_index)
    rows_for_label = matching_rows_for_label(drd6, variant_id, label["truthLabelKey"])
    detections = detection_index.get((label["fixtureId"], label["graphId"], crop_type), [])
    nearby = nearby_detections(label, detections)
    matched_rows = [row for row in rows_for_label if row.get("matchedTruth") and row.get("detectionId")]
    if not covering:
        root_cause = "CROP_COVERAGE_MISSING"
        next_fix = "expand or correct deterministic axis label-band crop coverage"
    elif matched_rows:
        best = max(matched_rows, key=lambda row: float(row.get("confidence") or 0.0))
        if best.get("safePredictedRole") != "tick_label":
            root_cause = "OCR_ROLE_OR_SAFETY_REJECTED"
            next_fix = "improve OCR text role classification or context ownership"
        else:
            root_cause = "ANCHOR_SELECTION_GAP"
            next_fix = "inspect safe anchor selection and numeric parser"
    elif nearby:
        root_cause = "OCR_DETECTED_NEAR_LABEL_BUT_NOT_MATCHED"
        next_fix = "improve OCR box-to-label matching or crop-local grouping"
    elif detections:
        root_cause = "OCR_DETECTION_MISSING_FOR_LABEL"
        next_fix = "improve crop preprocessing or split dense label bands"
    else:
        root_cause = "OCR_DETECTION_EMPTY_FOR_CROP"
        next_fix = "improve crop subdivision, scale, contrast, or OCR detector choice"
    return {
        "truthLabelKey": label["truthLabelKey"],
        "fixtureId": label["fixtureId"],
        "graphId": label["graphId"],
        "axis": label["axis"],
        "truthText": label["truthText"],
        "labelBbox": {k: round(float(v), 2) for k, v in label["bbox"].items()},
        "coverageStatus": "COVERED" if covering else "MISSING",
        "coveringCropIds": [crop["cropId"] for crop in covering],
        "sameCropDetectionCount": len(detections),
        "nearbyDetectionCount": len(nearby),
        "nearbyDetections": [
            {
                "detectionId": row.get("detectionId"),
                "ocrText": row.get("ocrText"),
                "safePredictedRole": row.get("safePredictedRole"),
                "confidence": row.get("confidence"),
                "nearbyIou": row.get("nearbyIou"),
                "nearbyCenterDistance": row.get("nearbyCenterDistance"),
            }
            for row in nearby[:5]
        ],
        "rootCause": root_cause,
        "nextFix": next_fix,
    }


def summarize_axis(graph_key: str, axis: str, labels: list[dict[str, Any]], missing_details: list[dict[str, Any]], safe_keys: set[str]) -> dict[str, Any]:
    axis_labels = [label for label in labels if label["graphKey"] == graph_key and label["axis"] == axis]
    safe_count = sum(1 for label in axis_labels if label["truthLabelKey"] in safe_keys)
    details = [detail for detail in missing_details if f"{detail['fixtureId']}::{detail['graphId']}" == graph_key and detail["axis"] == axis]
    root_counts = dict(Counter(detail["rootCause"] for detail in details))
    fixture_id, graph_id = graph_key.split("::", 1)
    primary = max(root_counts.items(), key=lambda item: item[1])[0] if root_counts else "NO_MISSING_ANCHORS"
    return {
        "fixtureId": fixture_id,
        "graphId": graph_id,
        "axis": axis,
        "truthTickLabelCount": len(axis_labels),
        "safeAnchorCount": safe_count,
        "missingAnchorCount": len(details),
        "coverageMissingCount": root_counts.get("CROP_COVERAGE_MISSING", 0),
        "ocrDetectionMissingCount": root_counts.get("OCR_DETECTION_MISSING_FOR_LABEL", 0)
        + root_counts.get("OCR_DETECTION_EMPTY_FOR_CROP", 0),
        "nearbyUnmatchedDetectionCount": root_counts.get("OCR_DETECTED_NEAR_LABEL_BUT_NOT_MATCHED", 0),
        "rootCauseCounts": root_counts,
        "primaryRootCause": primary,
        "recommendedFix": {
            "CROP_COVERAGE_MISSING": "repair axis label-band crop coverage",
            "OCR_DETECTION_EMPTY_FOR_CROP": "split or enhance dense label-band OCR crops",
            "OCR_DETECTION_MISSING_FOR_LABEL": "add crop-local preprocessing or detector variant",
            "OCR_DETECTED_NEAR_LABEL_BUT_NOT_MATCHED": "improve OCR box-to-label matching",
            "OCR_ROLE_OR_SAFETY_REJECTED": "repair text role/safety classifier for owned labels",
            "ANCHOR_SELECTION_GAP": "inspect anchor selector/parser",
            "NO_MISSING_ANCHORS": "none",
        }.get(primary, "inspect missing-anchor evidence"),
    }


def build_markdown(summary: dict[str, Any]) -> str:
    lines = [
        "# DR-E3 Missing Anchor Recovery And Label-Band Coverage Benchmark",
        "",
        f"Verdict: `{summary['overallVerdict']}`",
        f"OCR variant: `{summary['ocrVariantId']}`",
        f"Target non-valid axes: `{summary['targetAxisCount']}`",
        f"Missing anchors inspected: `{summary['missingAnchorCount']}`",
        "",
        "## Axis Root Cause Summary",
        "",
        "| Fixture | Graph | Axis | Truth labels | Safe anchors | Missing | Primary root cause | Recommended fix |",
        "| --- | --- | --- | ---: | ---: | ---: | --- | --- |",
    ]
    for row in summary["axisSummaries"]:
        lines.append(
            "| `{fixtureId}` | `{graphId}` | `{axis}` | {truthTickLabelCount} | {safeAnchorCount} | {missingAnchorCount} | `{primaryRootCause}` | {recommendedFix} |".format(
                **row
            )
        )
    lines.extend(
        [
            "",
            "## Root Cause Counts",
            "",
            f"`{summary['rootCauseCounts']}`",
            "",
            "## Interpretation",
            "",
            "- Remaining blockers are mostly missing safe OCR anchors, not robust-fit failures.",
            "- Covered labels with no OCR detection point to crop subdivision/preprocessing gaps.",
            "- Nearby detections that do not match point to OCR box-to-label matching gaps.",
            "- No calibration labels are fabricated; every missing anchor remains explicit evidence.",
        ]
    )
    return "\n".join(lines) + "\n"


def build_summary(args: argparse.Namespace) -> dict[str, Any]:
    truth = read_json(args.drc4_manual)
    drd5 = read_json(args.drd5_summary)
    drd6 = read_json(args.drd6_summary)
    dre1 = read_json(args.dre1_summary)
    dre2 = read_json(args.dre2_summary)
    variant_id = args.variant_id or dre2["ocrVariantId"]

    labels_by_key = build_truth_labels(truth["records"])
    safe_keys = safe_anchor_keys(dre1)
    missing_keys = missing_anchor_keys(dre1)
    target_axes = target_axis_keys(dre2)
    crop_index = index_crop_plans(drd5.get("cropPlans", []))
    detection_index = index_detections(drd6, variant_id)

    target_labels = [
        label
        for label in labels_by_key.values()
        if (label["graphKey"], label["axis"]) in target_axes
    ]
    missing_target_labels = [
        label
        for label in target_labels
        if label["truthLabelKey"] in missing_keys and label["truthLabelKey"] not in safe_keys
    ]
    missing_details = [
        classify_missing_label(
            label,
            drd6=drd6,
            variant_id=variant_id,
            crop_index=crop_index,
            detection_index=detection_index,
        )
        for label in missing_target_labels
    ]
    axis_summaries = [
        summarize_axis(graph_key, axis, target_labels, missing_details, safe_keys)
        for graph_key, axis in sorted(target_axes)
    ]
    root_counts = dict(Counter(detail["rootCause"] for detail in missing_details))
    coverage_missing = root_counts.get("CROP_COVERAGE_MISSING", 0)
    detection_missing = root_counts.get("OCR_DETECTION_EMPTY_FOR_CROP", 0) + root_counts.get("OCR_DETECTION_MISSING_FOR_LABEL", 0)
    nearby_unmatched = root_counts.get("OCR_DETECTED_NEAR_LABEL_BUT_NOT_MATCHED", 0)
    if coverage_missing:
        verdict = "MISSING_ANCHOR_RECOVERY_BLOCKED_BY_CROP_COVERAGE"
    elif detection_missing or nearby_unmatched:
        verdict = "MISSING_ANCHOR_RECOVERY_BLOCKED_BY_OCR_DETECTION_AND_MATCHING"
    elif missing_details:
        verdict = "MISSING_ANCHOR_RECOVERY_BLOCKED_BY_CLASSIFICATION_OR_SELECTION"
    else:
        verdict = "MISSING_ANCHOR_RECOVERY_NO_REMAINING_MISSING_ANCHORS"
    return {
        "schemaVersion": "chromalab.benchmark.dre3_missing_anchor_coverage.v1",
        "overallVerdict": verdict,
        "productionImpact": "NONE_RESEARCH_ONLY",
        "truthSource": str(args.drc4_manual).replace("\\", "/"),
        "cropPlanSource": str(args.drd5_summary).replace("\\", "/"),
        "ocrEvidenceSource": str(args.drd6_summary).replace("\\", "/"),
        "dre1Source": str(args.dre1_summary).replace("\\", "/"),
        "dre2Source": str(args.dre2_summary).replace("\\", "/"),
        "ocrVariantId": variant_id,
        "targetAxisCount": len(target_axes),
        "targetTruthLabelCount": len(target_labels),
        "missingAnchorCount": len(missing_details),
        "rootCauseCounts": root_counts,
        "axisSummaries": axis_summaries,
        "missingAnchorDetails": missing_details,
        "nextRequiredCapabilities": [
            "crop_local_dense_tick_label_splitting",
            "OCR_box_to_label_sequence_matching",
            "axis_label_band_coverage_validation",
            "safe_anchor_recovery_without_fabricating_labels",
        ],
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
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
        "--drd6-summary",
        type=Path,
        default=Path("benchmark/reports/drd6_axis_owned_ocr_preprocessing_grid/summary.json"),
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
        "--output",
        type=Path,
        default=Path("benchmark/reports/dre3_missing_anchor_coverage"),
    )
    parser.add_argument("--variant-id", default=None)
    args = parser.parse_args()
    summary = build_summary(args)
    write_json(args.output / "summary.json", summary)
    (args.output / "summary.md").write_text(build_markdown(summary), encoding="utf-8", newline="\n")
    print(f"Built DR-E3 missing anchor coverage benchmark: {summary['overallVerdict']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
