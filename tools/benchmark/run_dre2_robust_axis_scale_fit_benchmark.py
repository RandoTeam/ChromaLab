#!/usr/bin/env python3
"""Benchmark robust axis-scale fits from safe OCR anchors.

This PC-side benchmark consumes DR-E1 safe axis anchors and compares all-anchor,
RANSAC-style, monotonic-subsequence, and two-anchor review fallback fits. It
does not change Android runtime or production analysis.
"""

from __future__ import annotations

import argparse
import itertools
import json
import math
import statistics
from pathlib import Path
from typing import Any


STRATEGIES = (
    "ALL_ANCHOR_LINEAR_FIT",
    "RANSAC_RESIDUAL_TRIMMED_FIT",
    "MONOTONIC_SUBSEQUENCE_FIT",
    "TWO_ANCHOR_REVIEW_FALLBACK",
)


def read_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def group_anchors(anchors: list[dict[str, Any]]) -> dict[tuple[str, str], list[dict[str, Any]]]:
    grouped: dict[tuple[str, str], list[dict[str, Any]]] = {}
    for anchor in anchors:
        grouped.setdefault((anchor["graphKey"], anchor["axis"]), []).append(anchor)
    for key in grouped:
        grouped[key].sort(key=lambda item: (float(item["pixelCoordinate"]), float(item["ocrValue"])))
    return grouped


def group_missing(rejected: list[dict[str, Any]]) -> dict[tuple[str, str], list[dict[str, Any]]]:
    grouped: dict[tuple[str, str], list[dict[str, Any]]] = {}
    for row in rejected:
        graph_key = f"{row['fixtureId']}::{row['graphId']}"
        grouped.setdefault((graph_key, row["axis"]), []).append(row)
    return grouped


def graph_axis_keys(graph_results: list[dict[str, Any]]) -> list[tuple[str, str, dict[str, Any]]]:
    keys: list[tuple[str, str, dict[str, Any]]] = []
    for graph in graph_results:
        graph_key = f"{graph['fixtureId']}::{graph['graphId']}"
        keys.append((graph_key, "X", graph))
        keys.append((graph_key, "Y", graph))
    return keys


def monotonic_ok(axis: str, anchors: list[dict[str, Any]]) -> bool:
    if len(anchors) < 2:
        return False
    ordered = sorted(anchors, key=lambda item: float(item["pixelCoordinate"]))
    values = [float(item["ocrValue"]) for item in ordered]
    if axis == "X":
        return all(a < b for a, b in zip(values, values[1:]))
    return all(a > b for a, b in zip(values, values[1:]))


def monotonic_reason(axis: str, anchors: list[dict[str, Any]]) -> str:
    if len(anchors) < 2:
        return "INSUFFICIENT_SCALE_ANCHORS"
    if monotonic_ok(axis, anchors):
        return "PIXEL_INCREASES_VALUE_INCREASES" if axis == "X" else "PIXEL_INCREASES_VALUE_DECREASES"
    return "LABEL_SEQUENCE_NON_MONOTONIC"


def line_from_points(a: dict[str, Any], b: dict[str, Any]) -> tuple[float, float] | None:
    value_a = float(a["ocrValue"])
    value_b = float(b["ocrValue"])
    if value_a == value_b:
        return None
    pixel_a = float(a["pixelCoordinate"])
    pixel_b = float(b["pixelCoordinate"])
    slope = (pixel_b - pixel_a) / (value_b - value_a)
    intercept = pixel_a - slope * value_a
    return slope, intercept


def linear_fit(anchors: list[dict[str, Any]]) -> tuple[float, float] | None:
    if len(anchors) < 2:
        return None
    values = [float(anchor["ocrValue"]) for anchor in anchors]
    pixels = [float(anchor["pixelCoordinate"]) for anchor in anchors]
    mean_value = statistics.mean(values)
    mean_pixel = statistics.mean(pixels)
    denominator = sum((value - mean_value) ** 2 for value in values)
    if denominator == 0:
        return None
    slope = sum((value - mean_value) * (pixel - mean_pixel) for value, pixel in zip(values, pixels)) / denominator
    intercept = mean_pixel - slope * mean_value
    return slope, intercept


def residuals_for_line(anchors: list[dict[str, Any]], slope: float, intercept: float) -> list[float]:
    return [float(anchor["pixelCoordinate"]) - (slope * float(anchor["ocrValue"]) + intercept) for anchor in anchors]


def truth_residuals_for_line(anchors: list[dict[str, Any]], slope: float, intercept: float) -> list[float]:
    return [float(anchor["truthTickPixel"]) - (slope * float(anchor["ocrValue"]) + intercept) for anchor in anchors]


def rmse(values: list[float]) -> float:
    return math.sqrt(statistics.mean(value * value for value in values)) if values else 0.0


def value_span(anchors: list[dict[str, Any]]) -> float:
    if not anchors:
        return 0.0
    values = [float(anchor["ocrValue"]) for anchor in anchors]
    return max(values) - min(values)


def summarize_candidate(
    *,
    strategy_id: str,
    axis: str,
    all_anchors: list[dict[str, Any]],
    accepted: list[dict[str, Any]],
    slope: float | None,
    intercept: float | None,
    failure_reason: str | None = None,
) -> dict[str, Any]:
    accepted = sorted(accepted, key=lambda item: float(item["pixelCoordinate"]))
    accepted_keys = {anchor["truthLabelKey"] for anchor in accepted}
    rejected = [
        {
            "truthLabelKey": anchor["truthLabelKey"],
            "ocrText": anchor["ocrText"],
            "ocrValue": anchor["ocrValue"],
            "pixelCoordinate": anchor["pixelCoordinate"],
            "rejectionReason": "REJECTED_WORSE_THAN_SELECTED_OR_OUTLIER",
        }
        for anchor in all_anchors
        if anchor["truthLabelKey"] not in accepted_keys
    ]
    if slope is None or intercept is None:
        return {
            "strategyId": strategy_id,
            "axis": axis,
            "status": "INVALID",
            "failureReason": failure_reason or "INSUFFICIENT_SCALE_ANCHORS",
            "acceptedAnchorCount": len(accepted),
            "rejectedAnchorCount": len(rejected),
            "acceptedAnchors": accepted,
            "rejectedAnchors": rejected,
        }
    fit_residuals = residuals_for_line(accepted, slope, intercept)
    truth_fit_residuals = truth_residuals_for_line(accepted, slope, intercept)
    fit_rmse = rmse(fit_residuals)
    fit_max_abs = max(abs(value) for value in fit_residuals) if fit_residuals else 0.0
    truth_rmse = rmse(truth_fit_residuals)
    truth_max_abs = max(abs(value) for value in truth_fit_residuals) if truth_fit_residuals else 0.0
    mono = monotonic_ok(axis, accepted)
    if len(accepted) < 2:
        status = "INVALID"
        reason = "INSUFFICIENT_SCALE_ANCHORS"
    elif not mono:
        status = "INVALID"
        reason = "LABEL_SEQUENCE_NON_MONOTONIC"
    elif len(accepted) >= 4 and fit_rmse <= 8.0 and fit_max_abs <= 18.0:
        status = "VALID"
        reason = "VALID_ROBUST_LOW_RESIDUAL"
    elif len(accepted) >= 2 and fit_rmse <= 18.0 and fit_max_abs <= 36.0:
        status = "REVIEW"
        reason = "REVIEW_LIMITED_OR_NOISY_ROBUST_SCALE"
    else:
        status = "INVALID"
        reason = "SCALE_FIT_HIGH_RESIDUAL"
    residual_table = []
    for anchor, residual, truth_residual in zip(accepted, fit_residuals, truth_fit_residuals):
        residual_table.append(
            {
                "truthLabelKey": anchor["truthLabelKey"],
                "ocrText": anchor["ocrText"],
                "ocrValue": anchor["ocrValue"],
                "pixelCoordinate": anchor["pixelCoordinate"],
                "truthTickPixel": anchor["truthTickPixel"],
                "fitResidualPx": round(residual, 4),
                "truthTickResidualPx": round(truth_residual, 4),
                "confidence": anchor["confidence"],
            }
        )
    return {
        "strategyId": strategy_id,
        "axis": axis,
        "status": status,
        "selectionReason": reason if status != "INVALID" else None,
        "failureReason": None if status != "INVALID" else reason,
        "acceptedAnchorCount": len(accepted),
        "rejectedAnchorCount": len(rejected),
        "valueSpan": round(value_span(accepted), 6),
        "monotonicDirection": monotonic_reason(axis, accepted),
        "slopeValueToPixel": round(slope, 8),
        "interceptPixel": round(intercept, 4),
        "rmsePx": round(fit_rmse, 4),
        "maxAbsResidualPx": round(fit_max_abs, 4),
        "truthTickRmsePx": round(truth_rmse, 4),
        "truthTickMaxAbsResidualPx": round(truth_max_abs, 4),
        "acceptedAnchors": accepted,
        "rejectedAnchors": rejected,
        "residualTable": residual_table,
    }


def all_anchor_fit(axis: str, anchors: list[dict[str, Any]]) -> dict[str, Any]:
    line = linear_fit(anchors)
    if line is None:
        return summarize_candidate(
            strategy_id="ALL_ANCHOR_LINEAR_FIT",
            axis=axis,
            all_anchors=anchors,
            accepted=anchors,
            slope=None,
            intercept=None,
            failure_reason="INSUFFICIENT_SCALE_ANCHORS",
        )
    return summarize_candidate(
        strategy_id="ALL_ANCHOR_LINEAR_FIT",
        axis=axis,
        all_anchors=anchors,
        accepted=anchors,
        slope=line[0],
        intercept=line[1],
    )


def ransac_fit(axis: str, anchors: list[dict[str, Any]], threshold_px: float = 14.0) -> dict[str, Any]:
    if len(anchors) < 2:
        return summarize_candidate(
            strategy_id="RANSAC_RESIDUAL_TRIMMED_FIT",
            axis=axis,
            all_anchors=anchors,
            accepted=anchors,
            slope=None,
            intercept=None,
            failure_reason="INSUFFICIENT_SCALE_ANCHORS",
        )
    best_subset: list[dict[str, Any]] = []
    best_line: tuple[float, float] | None = None
    best_score: tuple[int, float, float, float] | None = None
    for a, b in itertools.combinations(anchors, 2):
        line = line_from_points(a, b)
        if line is None:
            continue
        slope, intercept = line
        residuals = residuals_for_line(anchors, slope, intercept)
        subset = [anchor for anchor, residual in zip(anchors, residuals) if abs(residual) <= threshold_px]
        if len(subset) < 2 or not monotonic_ok(axis, subset):
            continue
        refined = linear_fit(subset)
        if refined is None:
            continue
        refined_residuals = residuals_for_line(subset, refined[0], refined[1])
        score = (
            len(subset),
            value_span(subset),
            -rmse(refined_residuals),
            -max(abs(value) for value in refined_residuals),
        )
        if best_score is None or score > best_score:
            best_score = score
            best_subset = subset
            best_line = refined
    if best_line is None:
        return summarize_candidate(
            strategy_id="RANSAC_RESIDUAL_TRIMMED_FIT",
            axis=axis,
            all_anchors=anchors,
            accepted=[],
            slope=None,
            intercept=None,
            failure_reason="NO_MONOTONIC_INLIER_SET",
        )
    return summarize_candidate(
        strategy_id="RANSAC_RESIDUAL_TRIMMED_FIT",
        axis=axis,
        all_anchors=anchors,
        accepted=best_subset,
        slope=best_line[0],
        intercept=best_line[1],
    )


def longest_monotonic_subset(axis: str, anchors: list[dict[str, Any]]) -> list[dict[str, Any]]:
    ordered = sorted(anchors, key=lambda item: float(item["pixelCoordinate"]))
    best: list[dict[str, Any]] = []
    # Anchor counts in the current P0 corpus are small enough for exhaustive subsets.
    for size in range(len(ordered), 1, -1):
        for indexes in itertools.combinations(range(len(ordered)), size):
            subset = [ordered[index] for index in indexes]
            if monotonic_ok(axis, subset):
                if not best or value_span(subset) > value_span(best):
                    best = subset
        if best:
            break
    return best


def monotonic_subsequence_fit(axis: str, anchors: list[dict[str, Any]]) -> dict[str, Any]:
    subset = longest_monotonic_subset(axis, anchors)
    line = linear_fit(subset)
    if line is None:
        return summarize_candidate(
            strategy_id="MONOTONIC_SUBSEQUENCE_FIT",
            axis=axis,
            all_anchors=anchors,
            accepted=subset,
            slope=None,
            intercept=None,
            failure_reason="NO_MONOTONIC_SUBSEQUENCE",
        )
    return summarize_candidate(
        strategy_id="MONOTONIC_SUBSEQUENCE_FIT",
        axis=axis,
        all_anchors=anchors,
        accepted=subset,
        slope=line[0],
        intercept=line[1],
    )


def two_anchor_fallback(axis: str, anchors: list[dict[str, Any]]) -> dict[str, Any]:
    best_pair: list[dict[str, Any]] = []
    best_span = -1.0
    for a, b in itertools.combinations(anchors, 2):
        pair = sorted([a, b], key=lambda item: float(item["pixelCoordinate"]))
        if not monotonic_ok(axis, pair):
            continue
        span = value_span(pair)
        if span > best_span:
            best_pair = pair
            best_span = span
    line = linear_fit(best_pair)
    if line is None:
        return summarize_candidate(
            strategy_id="TWO_ANCHOR_REVIEW_FALLBACK",
            axis=axis,
            all_anchors=anchors,
            accepted=best_pair,
            slope=None,
            intercept=None,
            failure_reason="NO_MONOTONIC_TWO_ANCHOR_PAIR",
        )
    candidate = summarize_candidate(
        strategy_id="TWO_ANCHOR_REVIEW_FALLBACK",
        axis=axis,
        all_anchors=anchors,
        accepted=best_pair,
        slope=line[0],
        intercept=line[1],
    )
    if candidate["status"] == "VALID":
        candidate["status"] = "REVIEW"
        candidate["selectionReason"] = "SELECTED_REVIEW_TWO_ANCHOR_FALLBACK"
        candidate["failureReason"] = None
    return candidate


def candidate_rank(candidate: dict[str, Any]) -> tuple[int, int, float, float, float, int]:
    status_rank = {"VALID": 3, "REVIEW": 2, "INVALID": 1}.get(candidate["status"], 0)
    # Prefer robust strategies over all-anchor if evidence is otherwise similar.
    strategy_rank = {
        "RANSAC_RESIDUAL_TRIMMED_FIT": 4,
        "MONOTONIC_SUBSEQUENCE_FIT": 3,
        "ALL_ANCHOR_LINEAR_FIT": 2,
        "TWO_ANCHOR_REVIEW_FALLBACK": 1,
    }.get(candidate["strategyId"], 0)
    return (
        status_rank,
        int(candidate.get("acceptedAnchorCount") or 0),
        float(candidate.get("valueSpan") or 0.0),
        -float(candidate.get("rmsePx") or 999999.0),
        -float(candidate.get("truthTickRmsePx") or 999999.0),
        strategy_rank,
    )


def build_axis_result(graph_key: str, axis: str, anchors: list[dict[str, Any]], missing_count: int) -> dict[str, Any]:
    candidates = [
        all_anchor_fit(axis, anchors),
        ransac_fit(axis, anchors),
        monotonic_subsequence_fit(axis, anchors),
        two_anchor_fallback(axis, anchors),
    ]
    selected = max(candidates, key=candidate_rank)
    selected_reason = {
        "VALID": "SELECTED_VALID_ROBUST_CANDIDATE",
        "REVIEW": "SELECTED_REVIEW_BEST_AVAILABLE_CANDIDATE",
        "INVALID": "NO_USABLE_ROBUST_SCALE_CANDIDATE",
    }[selected["status"]]
    return {
        "graphKey": graph_key,
        "axis": axis,
        "status": selected["status"],
        "selectedStrategyId": selected["strategyId"],
        "selectionReason": selected_reason,
        "failureReason": selected.get("failureReason"),
        "missingSafeOcrLabelCount": missing_count,
        "selectedCandidate": selected,
        "candidateStrategies": candidates,
    }


def graph_decision(x_axis: dict[str, Any], y_axis: dict[str, Any]) -> tuple[str, str]:
    statuses = {x_axis["status"], y_axis["status"]}
    if statuses == {"VALID"}:
        return "VALID", "both axes selected valid robust scale candidates"
    if "INVALID" not in statuses:
        return "REVIEW", "both axes selected usable robust scale candidates"
    if x_axis["status"] != "INVALID" or y_axis["status"] != "INVALID":
        return "PARTIAL", "one axis remains invalid after robust fit"
    return "INVALID", "both axes remain invalid after robust fit"


def build_summary(args: argparse.Namespace) -> dict[str, Any]:
    dre1 = read_json(args.dre1_summary)
    grouped = group_anchors(dre1["safeAnchors"])
    missing = group_missing(dre1["rejectedOrMissingAnchors"])
    axis_results: list[dict[str, Any]] = []
    graph_results: list[dict[str, Any]] = []
    for graph_key, axis, graph in graph_axis_keys(dre1["graphResults"]):
        result = build_axis_result(
            graph_key,
            axis,
            grouped.get((graph_key, axis), []),
            len(missing.get((graph_key, axis), [])),
        )
        axis_results.append(result)
    axis_by_key = {(axis["graphKey"], axis["axis"]): axis for axis in axis_results}
    for graph in dre1["graphResults"]:
        graph_key = f"{graph['fixtureId']}::{graph['graphId']}"
        x_axis = axis_by_key[(graph_key, "X")]
        y_axis = axis_by_key[(graph_key, "Y")]
        decision, reason = graph_decision(x_axis, y_axis)
        graph_results.append(
            {
                "fixtureId": graph["fixtureId"],
                "graphId": graph["graphId"],
                "layoutClass": graph.get("layoutClass"),
                "decision": decision,
                "decisionReason": reason,
                "xAxis": x_axis,
                "yAxis": y_axis,
                "dre1Decision": graph["decision"],
                "dre1XStatus": graph["xAxis"]["status"],
                "dre1YStatus": graph["yAxis"]["status"],
            }
        )
    axis_status_counts: dict[str, int] = {}
    strategy_counts: dict[str, int] = {}
    graph_decision_counts: dict[str, int] = {}
    for axis in axis_results:
        axis_status_counts[axis["status"]] = axis_status_counts.get(axis["status"], 0) + 1
        strategy_counts[axis["selectedStrategyId"]] = strategy_counts.get(axis["selectedStrategyId"], 0) + 1
    for graph in graph_results:
        graph_decision_counts[graph["decision"]] = graph_decision_counts.get(graph["decision"], 0) + 1
    dre1_axis_counts = dre1["resultSummary"]["axisStatusCounts"]
    dre1_usable = dre1_axis_counts.get("VALID", 0) + dre1_axis_counts.get("REVIEW", 0)
    dre2_usable = axis_status_counts.get("VALID", 0) + axis_status_counts.get("REVIEW", 0)
    if axis_status_counts.get("INVALID", 0) == 0:
        verdict = "ROBUST_AXIS_SCALE_FIT_REVIEW_READY"
    elif dre2_usable > dre1_usable:
        verdict = "ROBUST_AXIS_SCALE_FIT_IMPROVES_OUTLIERS_NOT_ACCEPTANCE_READY"
    else:
        verdict = "ROBUST_AXIS_SCALE_FIT_NO_USABLE_IMPROVEMENT"
    return {
        "schemaVersion": "chromalab.benchmark.dre2_robust_axis_scale_fit.v1",
        "overallVerdict": verdict,
        "productionImpact": "NONE_RESEARCH_ONLY",
        "dre1Source": str(args.dre1_summary).replace("\\", "/"),
        "ocrVariantId": dre1["ocrVariantId"],
        "resultSummary": {
            "graphCount": len(graph_results),
            "axisCandidateCount": len(axis_results),
            "dre1UsableAxisCount": dre1_usable,
            "dre2UsableAxisCount": dre2_usable,
            "axisStatusCounts": axis_status_counts,
            "graphDecisionCounts": graph_decision_counts,
            "selectedStrategyCounts": strategy_counts,
        },
        "graphResults": graph_results,
        "axisResults": axis_results,
        "nextRequiredCapabilities": [
            "robust_fit_gate_policy_for_review_reports",
            "safe_OCR_missing_anchor_export",
            "runtime_axis_scale_candidate_ensemble",
            "calibration_truth_error_thresholds",
        ],
    }


def build_markdown(summary: dict[str, Any]) -> str:
    lines = [
        "# DR-E2 Robust Axis Scale Fit And Outlier Rejection Benchmark",
        "",
        f"Verdict: `{summary['overallVerdict']}`",
        f"OCR variant: `{summary['ocrVariantId']}`",
        f"Usable axes DR-E1 -> DR-E2: `{summary['resultSummary']['dre1UsableAxisCount']}` -> `{summary['resultSummary']['dre2UsableAxisCount']}`",
        "",
        "## Graph Summary",
        "",
        "| Fixture | Graph | DR-E1 | DR-E2 | X strategy | X status | X anchors | Y strategy | Y status | Y anchors |",
        "| --- | --- | --- | --- | --- | --- | ---: | --- | --- | ---: |",
    ]
    for graph in summary["graphResults"]:
        lines.append(
            "| `{fixtureId}` | `{graphId}` | `{dre1}` | `{decision}` | `{xs}` | `{xstatus}` | {xa} | `{ys}` | `{ystatus}` | {ya} |".format(
                fixtureId=graph["fixtureId"],
                graphId=graph["graphId"],
                dre1=graph["dre1Decision"],
                decision=graph["decision"],
                xs=graph["xAxis"]["selectedStrategyId"],
                xstatus=graph["xAxis"]["status"],
                xa=graph["xAxis"]["selectedCandidate"].get("acceptedAnchorCount", 0),
                ys=graph["yAxis"]["selectedStrategyId"],
                ystatus=graph["yAxis"]["status"],
                ya=graph["yAxis"]["selectedCandidate"].get("acceptedAnchorCount", 0),
            )
        )
    lines.extend(
        [
            "",
            "## Counts",
            "",
            f"Axis statuses: `{summary['resultSummary']['axisStatusCounts']}`",
            f"Graph decisions: `{summary['resultSummary']['graphDecisionCounts']}`",
            f"Selected strategies: `{summary['resultSummary']['selectedStrategyCounts']}`",
            "",
            "## Interpretation",
            "",
            "- Robust fitting can rescue high-residual and non-monotonic safe OCR evidence only when enough monotonic inliers exist.",
            "- Two-anchor fallback is review-only, never release-ready.",
            "- Missing anchors remain explicit missing evidence; no tick labels are fabricated.",
            "- This remains a benchmark contract and does not change Android calibration.",
        ]
    )
    return "\n".join(lines) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--dre1-summary",
        type=Path,
        default=Path("benchmark/reports/dre1_axis_scale_candidate_builder/summary.json"),
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("benchmark/reports/dre2_robust_axis_scale_fit"),
    )
    args = parser.parse_args()
    summary = build_summary(args)
    write_json(args.output / "summary.json", summary)
    (args.output / "summary.md").write_text(build_markdown(summary), encoding="utf-8", newline="\n")
    print(f"Built DR-E2 robust axis scale fit benchmark: {summary['overallVerdict']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
