#!/usr/bin/env python3
"""Build axis-scale candidates from safe owned OCR evidence.

This is a PC-side benchmark runner only. It consumes DR-D6 safe OCR evidence and
DR-C4 tick truth to measure whether incomplete OCR labels can support auditable
axis scale candidates. It does not change Android runtime or production
analysis.
"""

from __future__ import annotations

import argparse
import json
import math
import re
import statistics
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


def normalize_numeric_text(text: str | None) -> str:
    value = str(text or "").strip()
    value = value.replace("\u2212", "-").replace("\u2013", "-").replace("\u2014", "-")
    value = value.replace("\u00a0", " ").replace(",", ".")
    value = re.sub(r"\s+", "", value)
    return value


def parse_numeric(text: str | None) -> float | None:
    value = normalize_numeric_text(text)
    if not re.fullmatch(r"[+-]?\d+(?:\.\d+)?", value):
        return None
    try:
        return float(value)
    except ValueError:
        return None


def bbox_center(bbox: dict[str, Any]) -> tuple[float, float]:
    return ((float(bbox["x0"]) + float(bbox["x1"])) / 2.0, (float(bbox["y0"]) + float(bbox["y1"])) / 2.0)


def axis_from_text_id(text_id: str) -> str | None:
    lowered = text_id.lower()
    if "_xlabel_" in lowered:
        return "X"
    if "_ylabel_" in lowered:
        return "Y"
    return None


def index_from_text_id(text_id: str) -> int | None:
    match = re.search(r"_(?:x|y)label_(\d+)$", text_id.lower())
    return int(match.group(1)) if match else None


def tick_id_from_label(graph_id: str, axis: str, index: int) -> str:
    prefix = "xtick" if axis == "X" else "ytick"
    return f"{graph_id}_{prefix}_{index}"


def truth_label_key(fixture_id: str, graph_id: str, text_id: str) -> str:
    return f"{fixture_id}::{graph_id}::{text_id}"


def build_tick_truth(records: list[dict[str, Any]]) -> tuple[dict[str, dict[str, Any]], dict[str, dict[str, Any]]]:
    tick_by_label: dict[str, dict[str, Any]] = {}
    graph_truth: dict[str, dict[str, Any]] = {}
    for record in records:
        fixture_id = record["fixtureId"]
        for graph in record.get("graphs", []):
            graph_id = graph["graphId"]
            graph_key = f"{fixture_id}::{graph_id}"
            ticks_by_id = {tick["tickId"]: tick for tick in graph.get("tickPositions", [])}
            graph_truth[graph_key] = {
                "fixtureId": fixture_id,
                "graphId": graph_id,
                "layoutClass": record.get("layoutClass"),
                "expectedGraphCount": record.get("expectedPhysicalGraphCount"),
                "truthTicks": graph.get("tickPositions", []),
            }
            for label in graph.get("textRoleLabels", []):
                if label.get("role") != "tick_label":
                    continue
                text_id = str(label.get("textId") or label.get("labelId") or "")
                axis = axis_from_text_id(text_id)
                index = index_from_text_id(text_id)
                if axis is None or index is None:
                    continue
                tick = ticks_by_id.get(tick_id_from_label(graph_id, axis, index))
                if tick is None:
                    continue
                value = parse_numeric(label.get("text"))
                if value is None:
                    continue
                label_bbox = label.get("bbox") or {}
                tick_by_label[truth_label_key(fixture_id, graph_id, text_id)] = {
                    "fixtureId": fixture_id,
                    "graphId": graph_id,
                    "graphKey": graph_key,
                    "textId": text_id,
                    "axis": axis,
                    "labelIndex": index,
                    "truthText": label.get("text"),
                    "truthValue": value,
                    "truthLabelBbox": label_bbox,
                    "truthTickId": tick["tickId"],
                    "truthTickPixel": float(tick["pixel"]["x"] if axis == "X" else tick["pixel"]["y"]),
                    "truthTickPixel2D": tick["pixel"],
                }
    return tick_by_label, graph_truth


def select_best_ocr_rows(drd6_summary: dict[str, Any], variant_id: str) -> dict[str, dict[str, Any]]:
    rows = [
        row
        for row in drd6_summary.get("detectionScores", [])
        if row.get("methodId") == variant_id
        and row.get("matchedTruth")
        and row.get("truthRole") == "tick_label"
        and row.get("safePredictedRole") == "tick_label"
        and not row.get("safeFalseTickLabel")
        and row.get("truthLabelKey")
    ]
    best: dict[str, dict[str, Any]] = {}
    for row in rows:
        key = row["truthLabelKey"]
        current = best.get(key)
        score = (float(row.get("confidence") or 0.0), float(row.get("textSimilarity") or 0.0))
        current_score = (
            float(current.get("confidence") or 0.0),
            float(current.get("textSimilarity") or 0.0),
        ) if current else (-1.0, -1.0)
        if current is None or score > current_score:
            best[key] = row
    return best


def build_anchors(tick_truth: dict[str, dict[str, Any]], ocr_rows: dict[str, dict[str, Any]]) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    anchors: list[dict[str, Any]] = []
    rejected: list[dict[str, Any]] = []
    for key, truth in tick_truth.items():
        row = ocr_rows.get(key)
        if row is None:
            rejected.append(
                {
                    "truthLabelKey": key,
                    "fixtureId": truth["fixtureId"],
                    "graphId": truth["graphId"],
                    "axis": truth["axis"],
                    "truthText": truth["truthText"],
                    "truthValue": truth["truthValue"],
                    "rejectionReason": "MISSING_SAFE_OCR_LABEL",
                }
            )
            continue
        value = parse_numeric(row.get("ocrText"))
        if value is None:
            rejected.append(
                {
                    "truthLabelKey": key,
                    "fixtureId": truth["fixtureId"],
                    "graphId": truth["graphId"],
                    "axis": truth["axis"],
                    "truthText": truth["truthText"],
                    "ocrText": row.get("ocrText"),
                    "rejectionReason": "OCR_NUMERIC_PARSE_FAILED",
                }
            )
            continue
        center_x, center_y = bbox_center(row["bbox"])
        pixel = center_x if truth["axis"] == "X" else center_y
        anchors.append(
            {
                "truthLabelKey": key,
                "fixtureId": truth["fixtureId"],
                "graphId": truth["graphId"],
                "graphKey": truth["graphKey"],
                "axis": truth["axis"],
                "labelIndex": truth["labelIndex"],
                "ocrText": row.get("ocrText"),
                "ocrValue": value,
                "truthText": truth["truthText"],
                "truthValue": truth["truthValue"],
                "valueError": round(value - truth["truthValue"], 6),
                "pixelCoordinate": round(pixel, 3),
                "truthTickPixel": round(truth["truthTickPixel"], 3),
                "labelCenterOffsetPx": round(pixel - truth["truthTickPixel"], 3),
                "confidence": row.get("confidence"),
                "textSimilarity": row.get("textSimilarity"),
                "sourceDetectionId": row.get("detectionId"),
            }
        )
    return anchors, rejected


def linear_fit_value_to_pixel(anchors: list[dict[str, Any]]) -> dict[str, Any]:
    values = [float(anchor["ocrValue"]) for anchor in anchors]
    pixels = [float(anchor["pixelCoordinate"]) for anchor in anchors]
    mean_value = statistics.mean(values)
    mean_pixel = statistics.mean(pixels)
    denominator = sum((value - mean_value) ** 2 for value in values)
    if denominator == 0:
        return {"fitStatus": "INVALID", "failureReason": "DUPLICATE_SCALE_VALUES"}
    slope = sum((value - mean_value) * (pixel - mean_pixel) for value, pixel in zip(values, pixels)) / denominator
    intercept = mean_pixel - slope * mean_value
    residuals = [pixel - (slope * value + intercept) for value, pixel in zip(values, pixels)]
    rmse = math.sqrt(statistics.mean(residual * residual for residual in residuals))
    max_abs = max(abs(residual) for residual in residuals)
    return {
        "slopeValueToPixel": slope,
        "interceptPixel": intercept,
        "rmsePx": rmse,
        "maxAbsResidualPx": max_abs,
        "residuals": residuals,
    }


def monotonic_status(axis: str, anchors: list[dict[str, Any]]) -> tuple[bool, str]:
    sorted_anchors = sorted(anchors, key=lambda anchor: float(anchor["pixelCoordinate"]))
    values = [float(anchor["ocrValue"]) for anchor in sorted_anchors]
    if len(values) < 2:
        return False, "INSUFFICIENT_SCALE_ANCHORS"
    increasing = all(a < b for a, b in zip(values, values[1:]))
    decreasing = all(a > b for a, b in zip(values, values[1:]))
    if axis == "X":
        return increasing, "PIXEL_INCREASES_VALUE_INCREASES" if increasing else "LABEL_SEQUENCE_NON_MONOTONIC"
    return decreasing, "PIXEL_INCREASES_VALUE_DECREASES" if decreasing else "LABEL_SEQUENCE_NON_MONOTONIC"


def build_axis_candidate(graph_key: str, axis: str, anchors: list[dict[str, Any]], missing_count: int) -> dict[str, Any]:
    selected = sorted(
        [anchor for anchor in anchors if anchor["graphKey"] == graph_key and anchor["axis"] == axis],
        key=lambda anchor: float(anchor["pixelCoordinate"]),
    )
    if len(selected) < 2:
        return {
            "axis": axis,
            "status": "INVALID",
            "failureReason": "INSUFFICIENT_SCALE_ANCHORS",
            "anchorCount": len(selected),
            "missingSafeOcrLabelCount": missing_count,
            "anchors": selected,
            "rejectedReason": "need at least two safe OCR numeric anchors",
        }
    monotonic, direction = monotonic_status(axis, selected)
    if not monotonic:
        return {
            "axis": axis,
            "status": "INVALID",
            "failureReason": "LABEL_SEQUENCE_NON_MONOTONIC",
            "anchorCount": len(selected),
            "missingSafeOcrLabelCount": missing_count,
            "monotonicDirection": direction,
            "anchors": selected,
        }
    fit = linear_fit_value_to_pixel(selected)
    if fit.get("fitStatus") == "INVALID":
        return {
            "axis": axis,
            "status": "INVALID",
            "failureReason": fit["failureReason"],
            "anchorCount": len(selected),
            "missingSafeOcrLabelCount": missing_count,
            "monotonicDirection": direction,
            "anchors": selected,
        }
    rmse = float(fit["rmsePx"])
    max_abs = float(fit["maxAbsResidualPx"])
    if len(selected) >= 4 and rmse <= 8.0 and max_abs <= 18.0:
        status = "VALID"
        reason = "VALID_LOW_RESIDUAL_SAFE_OCR_SCALE"
    elif len(selected) >= 2 and rmse <= 18.0 and max_abs <= 36.0:
        status = "REVIEW"
        reason = "REVIEW_LIMITED_OR_NOISY_SAFE_OCR_SCALE"
    else:
        status = "INVALID"
        reason = "SCALE_FIT_HIGH_RESIDUAL"
    residual_table = []
    for anchor, residual in zip(selected, fit["residuals"]):
        residual_table.append(
            {
                "truthLabelKey": anchor["truthLabelKey"],
                "axis": axis,
                "ocrText": anchor["ocrText"],
                "ocrValue": anchor["ocrValue"],
                "pixelCoordinate": anchor["pixelCoordinate"],
                "residualPx": round(residual, 4),
                "confidence": anchor["confidence"],
            }
        )
    return {
        "axis": axis,
        "status": status,
        "selectionReason": reason if status != "INVALID" else None,
        "failureReason": None if status != "INVALID" else reason,
        "anchorCount": len(selected),
        "missingSafeOcrLabelCount": missing_count,
        "monotonicDirection": direction,
        "slopeValueToPixel": round(float(fit["slopeValueToPixel"]), 8),
        "interceptPixel": round(float(fit["interceptPixel"]), 4),
        "rmsePx": round(rmse, 4),
        "maxAbsResidualPx": round(max_abs, 4),
        "anchors": selected,
        "residualTable": residual_table,
    }


def graph_decision(x_axis: dict[str, Any], y_axis: dict[str, Any]) -> tuple[str, str]:
    statuses = {x_axis["status"], y_axis["status"]}
    if statuses == {"VALID"}:
        return "VALID", "both axes have valid safe OCR scale candidates"
    if "INVALID" not in statuses:
        return "REVIEW", "both axes have usable safe OCR scale candidates with review caveats"
    if x_axis["status"] != "INVALID" or y_axis["status"] != "INVALID":
        return "PARTIAL", "one axis is usable and one axis is invalid"
    return "INVALID", "both axes lack usable safe OCR scale candidates"


def build_graph_candidates(
    graph_truth: dict[str, dict[str, Any]],
    anchors: list[dict[str, Any]],
    rejected: list[dict[str, Any]],
) -> list[dict[str, Any]]:
    rejected_by_graph_axis: dict[tuple[str, str], list[dict[str, Any]]] = {}
    for row in rejected:
        graph_key = f"{row['fixtureId']}::{row['graphId']}"
        rejected_by_graph_axis.setdefault((graph_key, row["axis"]), []).append(row)

    graph_results: list[dict[str, Any]] = []
    for graph_key, graph in sorted(graph_truth.items()):
        x_candidate = build_axis_candidate(
            graph_key,
            "X",
            anchors,
            len(rejected_by_graph_axis.get((graph_key, "X"), [])),
        )
        y_candidate = build_axis_candidate(
            graph_key,
            "Y",
            anchors,
            len(rejected_by_graph_axis.get((graph_key, "Y"), [])),
        )
        decision, reason = graph_decision(x_candidate, y_candidate)
        graph_results.append(
            {
                "fixtureId": graph["fixtureId"],
                "graphId": graph["graphId"],
                "layoutClass": graph.get("layoutClass"),
                "decision": decision,
                "decisionReason": reason,
                "xAxis": x_candidate,
                "yAxis": y_candidate,
            }
        )
    return graph_results


def summarize_results(graph_results: list[dict[str, Any]], anchors: list[dict[str, Any]], rejected: list[dict[str, Any]]) -> dict[str, Any]:
    axis_candidates = [result["xAxis"] for result in graph_results] + [result["yAxis"] for result in graph_results]
    decision_counts: dict[str, int] = {}
    axis_status_counts: dict[str, int] = {}
    for result in graph_results:
        decision_counts[result["decision"]] = decision_counts.get(result["decision"], 0) + 1
    for axis in axis_candidates:
        axis_status_counts[axis["status"]] = axis_status_counts.get(axis["status"], 0) + 1
    return {
        "graphCount": len(graph_results),
        "axisCandidateCount": len(axis_candidates),
        "safeAnchorCount": len(anchors),
        "rejectedOrMissingAnchorCount": len(rejected),
        "graphDecisionCounts": decision_counts,
        "axisStatusCounts": axis_status_counts,
    }


def build_markdown(summary: dict[str, Any]) -> str:
    lines = [
        "# DR-E1 Axis Scale Candidate Builder From Safe Owned OCR",
        "",
        f"Verdict: `{summary['overallVerdict']}`",
        f"Best OCR variant: `{summary['ocrVariantId']}`",
        f"Graphs: `{summary['resultSummary']['graphCount']}`",
        f"Safe anchors: `{summary['resultSummary']['safeAnchorCount']}`",
        "",
        "## Graph Candidate Summary",
        "",
        "| Fixture | Graph | X status | X anchors | X RMSE | Y status | Y anchors | Y RMSE | Decision |",
        "| --- | --- | --- | ---: | ---: | --- | ---: | ---: | --- |",
    ]
    for result in summary["graphResults"]:
        x_axis = result["xAxis"]
        y_axis = result["yAxis"]
        lines.append(
            "| `{fixtureId}` | `{graphId}` | `{xstatus}` | {xanchors} | {xrmse} | `{ystatus}` | {yanchors} | {yrmse} | `{decision}` |".format(
                fixtureId=result["fixtureId"],
                graphId=result["graphId"],
                xstatus=x_axis["status"],
                xanchors=x_axis["anchorCount"],
                xrmse=x_axis.get("rmsePx", "-"),
                ystatus=y_axis["status"],
                yanchors=y_axis["anchorCount"],
                yrmse=y_axis.get("rmsePx", "-"),
                decision=result["decision"],
            )
        )
    lines.extend(
        [
            "",
            "## Counts",
            "",
            f"Graph decisions: `{summary['resultSummary']['graphDecisionCounts']}`",
            f"Axis statuses: `{summary['resultSummary']['axisStatusCounts']}`",
            "",
            "## Interpretation",
            "",
            "- Safe owned OCR can build usable scale candidates on several axes.",
            "- This is still benchmark evidence only; it does not alter Android calibration.",
            "- Missing OCR labels remain explicit missing anchors, not fabricated tick values.",
            "- The next step should score calibration candidates against truth and define release/review gates for incomplete safe OCR evidence.",
        ]
    )
    return "\n".join(lines) + "\n"


def build_summary(args: argparse.Namespace) -> dict[str, Any]:
    truth_records = read_json(args.drc4_manual)["records"]
    drd6_summary = read_json(args.drd6_summary)
    variant_id = args.variant_id or drd6_summary["bestVariant"]["variantId"]
    tick_truth, graph_truth = build_tick_truth(truth_records)
    ocr_rows = select_best_ocr_rows(drd6_summary, variant_id)
    anchors, rejected = build_anchors(tick_truth, ocr_rows)
    graph_results = build_graph_candidates(graph_truth, anchors, rejected)
    result_summary = summarize_results(graph_results, anchors, rejected)

    axis_counts = result_summary["axisStatusCounts"]
    if axis_counts.get("INVALID", 0) == 0 and axis_counts.get("VALID", 0) == result_summary["axisCandidateCount"]:
        verdict = "AXIS_SCALE_CANDIDATES_ACCEPTANCE_CANDIDATE"
    elif axis_counts.get("INVALID", 0) == 0:
        verdict = "AXIS_SCALE_CANDIDATES_REVIEW_READY"
    elif axis_counts.get("VALID", 0) or axis_counts.get("REVIEW", 0):
        verdict = "AXIS_SCALE_CANDIDATES_PARTIAL_SAFE_EVIDENCE_NOT_ACCEPTANCE_READY"
    else:
        verdict = "AXIS_SCALE_CANDIDATES_BLOCKED_INSUFFICIENT_SAFE_OCR"

    return {
        "schemaVersion": "chromalab.benchmark.dre1_axis_scale_candidate_builder.v1",
        "overallVerdict": verdict,
        "productionImpact": "NONE_RESEARCH_ONLY",
        "textRoleTruthSource": str(args.drc4_manual).replace("\\", "/"),
        "ocrEvidenceSource": str(args.drd6_summary).replace("\\", "/"),
        "ocrVariantId": variant_id,
        "resultSummary": result_summary,
        "graphResults": graph_results,
        "safeAnchors": anchors,
        "rejectedOrMissingAnchors": rejected,
        "nextRequiredCapabilities": [
            "calibration_candidate_gate_thresholds",
            "axis_scale_fit_scoring_against_truth",
            "incomplete_safe_ocr_review_policy",
            "automatic_runtime_label_band_ownership",
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
        "--drd6-summary",
        type=Path,
        default=Path("benchmark/reports/drd6_axis_owned_ocr_preprocessing_grid/summary.json"),
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("benchmark/reports/dre1_axis_scale_candidate_builder"),
    )
    parser.add_argument("--variant-id", default=None)
    args = parser.parse_args()
    summary = build_summary(args)
    write_json(args.output / "summary.json", summary)
    (args.output / "summary.md").write_text(build_markdown(summary), encoding="utf-8", newline="\n")
    print(f"Built DR-E1 axis scale candidate builder: {summary['overallVerdict']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
