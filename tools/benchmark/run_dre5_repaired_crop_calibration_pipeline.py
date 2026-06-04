#!/usr/bin/env python3
"""Run repaired label-band crops through end-to-end calibration scoring.

This PC-side benchmark consumes DR-E1 safe anchors, DR-E2 baseline robust fits,
and DR-E4 repaired label-band OCR rows. It measures whether repaired crop
coverage changes graph-level calibration decisions when the same robust fit
rules are reused. It does not change Android runtime or production analysis.
"""

from __future__ import annotations

import argparse
import importlib.util
import json
from pathlib import Path
from typing import Any


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


def axis_key_from_graph_axis(graph_key: str, axis: str) -> tuple[str, str]:
    return (graph_key, axis)


def graph_key(fixture_id: str, graph_id: str) -> str:
    return f"{fixture_id}::{graph_id}"


def status_rank(status: str) -> int:
    return {"INVALID": 0, "PARTIAL": 1, "REVIEW": 2, "VALID": 3}.get(status, -1)


def usable_status(status: str) -> bool:
    return status in {"VALID", "REVIEW"}


def select_best_repaired_rows(dre4: dict[str, Any]) -> dict[str, dict[str, Any]]:
    best: dict[str, dict[str, Any]] = {}
    for row in dre4.get("ocrRows", []):
        if (
            row.get("matchedTruth")
            and row.get("truthRole") == "tick_label"
            and row.get("safePredictedRole") == "tick_label"
            and not row.get("safeFalseTickLabel")
            and row.get("truthLabelKey")
        ):
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


def tag_anchors(anchors: list[dict[str, Any]], source: str) -> list[dict[str, Any]]:
    tagged = []
    for anchor in anchors:
        copy = dict(anchor)
        copy["anchorSource"] = source
        tagged.append(copy)
    return tagged


def merge_anchors(
    baseline_anchors: list[dict[str, Any]],
    repaired_anchors: list[dict[str, Any]],
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    merged_by_key: dict[str, dict[str, Any]] = {}
    added_repaired: list[dict[str, Any]] = []
    for anchor in tag_anchors(baseline_anchors, "DR_E1_BASELINE_SAFE_OCR"):
        merged_by_key[anchor["truthLabelKey"]] = anchor
    for anchor in tag_anchors(repaired_anchors, "DR_E4_REPAIRED_LABEL_BAND_SAFE_OCR"):
        key = anchor["truthLabelKey"]
        if key not in merged_by_key:
            merged_by_key[key] = anchor
            added_repaired.append(anchor)
    return list(merged_by_key.values()), added_repaired


def filter_rejected(dre1_rejected: list[dict[str, Any]], merged_anchors: list[dict[str, Any]]) -> list[dict[str, Any]]:
    recovered_keys = {anchor["truthLabelKey"] for anchor in merged_anchors}
    return [row for row in dre1_rejected if row["truthLabelKey"] not in recovered_keys]


def count_by(items: list[dict[str, Any]], key: str) -> dict[str, int]:
    counts: dict[str, int] = {}
    for item in items:
        value = str(item.get(key))
        counts[value] = counts.get(value, 0) + 1
    return counts


def graph_decision_delta(before: str, after: str) -> str:
    before_rank = status_rank(before)
    after_rank = status_rank(after)
    if after_rank > before_rank:
        return "IMPROVED"
    if after_rank < before_rank:
        return "REGRESSED"
    return "UNCHANGED"


def build_axis_results(dre2: Any, dre1: dict[str, Any], anchors: list[dict[str, Any]], rejected: list[dict[str, Any]]) -> list[dict[str, Any]]:
    grouped = dre2.group_anchors(anchors)
    missing = dre2.group_missing(rejected)
    axis_results: list[dict[str, Any]] = []
    for graph_key_value, axis, _graph in dre2.graph_axis_keys(dre1["graphResults"]):
        axis_results.append(
            dre2.build_axis_result(
                graph_key_value,
                axis,
                grouped.get(axis_key_from_graph_axis(graph_key_value, axis), []),
                len(missing.get(axis_key_from_graph_axis(graph_key_value, axis), [])),
            )
        )
    return axis_results


def build_graph_results(dre2: Any, dre1: dict[str, Any], dre2_baseline: dict[str, Any], axis_results: list[dict[str, Any]]) -> list[dict[str, Any]]:
    axis_by_key = {(axis["graphKey"], axis["axis"]): axis for axis in axis_results}
    baseline_by_graph = {graph_key(row["fixtureId"], row["graphId"]): row for row in dre2_baseline["graphResults"]}
    graph_results: list[dict[str, Any]] = []
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
                "dre2Decision": baseline["decision"],
                "dre5Decision": decision,
                "decisionDelta": graph_decision_delta(baseline["decision"], decision),
                "decisionReason": reason,
                "dre2XStatus": baseline["xAxis"]["status"],
                "dre5XStatus": x_axis["status"],
                "dre2XAnchors": baseline["xAxis"]["selectedCandidate"].get("acceptedAnchorCount", 0),
                "dre5XAnchors": x_axis["selectedCandidate"].get("acceptedAnchorCount", 0),
                "dre2YStatus": baseline["yAxis"]["status"],
                "dre5YStatus": y_axis["status"],
                "dre2YAnchors": baseline["yAxis"]["selectedCandidate"].get("acceptedAnchorCount", 0),
                "dre5YAnchors": y_axis["selectedCandidate"].get("acceptedAnchorCount", 0),
                "xAxis": x_axis,
                "yAxis": y_axis,
            }
        )
    return graph_results


def build_repaired_axis_table(
    dre2_baseline: dict[str, Any],
    graph_results: list[dict[str, Any]],
    added_repaired_anchors: list[dict[str, Any]],
) -> list[dict[str, Any]]:
    baseline_axis = {(axis["graphKey"], axis["axis"]): axis for axis in dre2_baseline["axisResults"]}
    repaired_by_axis: dict[tuple[str, str], int] = {}
    for anchor in added_repaired_anchors:
        key = (anchor["graphKey"], anchor["axis"])
        repaired_by_axis[key] = repaired_by_axis.get(key, 0) + 1
    table = []
    for graph in graph_results:
        key = graph_key(graph["fixtureId"], graph["graphId"])
        for axis in ("X", "Y"):
            axis_result = graph["xAxis"] if axis == "X" else graph["yAxis"]
            baseline = baseline_axis[(key, axis)]
            added = repaired_by_axis.get((key, axis), 0)
            if added or baseline["status"] != axis_result["status"]:
                table.append(
                    {
                        "fixtureId": graph["fixtureId"],
                        "graphId": graph["graphId"],
                        "axis": axis,
                        "addedRepairedAnchorCount": added,
                        "dre2Status": baseline["status"],
                        "dre5Status": axis_result["status"],
                        "dre2AcceptedAnchorCount": baseline["selectedCandidate"].get("acceptedAnchorCount", 0),
                        "dre5AcceptedAnchorCount": axis_result["selectedCandidate"].get("acceptedAnchorCount", 0),
                        "dre5SelectedStrategy": axis_result["selectedStrategyId"],
                        "dre5FailureReason": axis_result.get("failureReason"),
                    }
                )
    return table


def summarize_counts(axis_results: list[dict[str, Any]], graph_results: list[dict[str, Any]]) -> dict[str, Any]:
    axis_status_counts = count_by(axis_results, "status")
    graph_decision_counts = count_by([{"decision": row["dre5Decision"]} for row in graph_results], "decision")
    strategy_counts = count_by(axis_results, "selectedStrategyId")
    improved_graphs = sum(1 for row in graph_results if row["decisionDelta"] == "IMPROVED")
    regressed_graphs = sum(1 for row in graph_results if row["decisionDelta"] == "REGRESSED")
    usable_axes = axis_status_counts.get("VALID", 0) + axis_status_counts.get("REVIEW", 0)
    usable_graphs = graph_decision_counts.get("VALID", 0) + graph_decision_counts.get("REVIEW", 0)
    return {
        "axisStatusCounts": axis_status_counts,
        "graphDecisionCounts": graph_decision_counts,
        "selectedStrategyCounts": strategy_counts,
        "usableAxisCount": usable_axes,
        "usableGraphCount": usable_graphs,
        "improvedGraphCount": improved_graphs,
        "regressedGraphCount": regressed_graphs,
    }


def build_summary(args: argparse.Namespace) -> dict[str, Any]:
    dre1_module = load_script("dre1_axis_scale_candidate_builder", "run_dre1_axis_scale_candidate_builder.py")
    dre2_module = load_script("dre2_robust_axis_scale_fit", "run_dre2_robust_axis_scale_fit_benchmark.py")
    dre1 = read_json(args.dre1_summary)
    dre2 = read_json(args.dre2_summary)
    dre4 = read_json(args.dre4_summary)
    truth_records = read_json(args.drc4_manual)["records"]

    tick_truth, _graph_truth = dre1_module.build_tick_truth(truth_records)
    repaired_rows = select_best_repaired_rows(dre4)
    repaired_anchors, repaired_rejected = dre1_module.build_anchors(tick_truth, repaired_rows)
    merged_anchors, added_repaired_anchors = merge_anchors(dre1["safeAnchors"], repaired_anchors)
    merged_rejected = filter_rejected(dre1["rejectedOrMissingAnchors"], merged_anchors)

    axis_results = build_axis_results(dre2_module, dre1, merged_anchors, merged_rejected)
    graph_results = build_graph_results(dre2_module, dre1, dre2, axis_results)
    repaired_axis_table = build_repaired_axis_table(dre2, graph_results, added_repaired_anchors)
    result_summary = summarize_counts(axis_results, graph_results)

    baseline_counts = dre2["resultSummary"]
    baseline_usable_axes = baseline_counts["dre2UsableAxisCount"]
    baseline_graph_counts = baseline_counts["graphDecisionCounts"]
    baseline_usable_graphs = baseline_graph_counts.get("VALID", 0) + baseline_graph_counts.get("REVIEW", 0)
    result_summary["dre2UsableAxisCount"] = baseline_usable_axes
    result_summary["dre5UsableAxisCount"] = result_summary["usableAxisCount"]
    result_summary["dre2UsableGraphCount"] = baseline_usable_graphs
    result_summary["dre5UsableGraphCount"] = result_summary["usableGraphCount"]
    result_summary["baselineGraphDecisionCounts"] = baseline_graph_counts
    result_summary["addedRepairedAnchorCount"] = len(added_repaired_anchors)
    result_summary["repairedSafeAnchorInputCount"] = len(repaired_anchors)
    result_summary["repairedRejectedOrMissingInputCount"] = len(repaired_rejected)

    if result_summary["regressedGraphCount"]:
        verdict = "REPAIRED_CROP_PIPELINE_REGRESSION_REQUIRES_REVIEW"
    elif result_summary["dre5UsableAxisCount"] > result_summary["dre2UsableAxisCount"]:
        verdict = "REPAIRED_CROP_PIPELINE_IMPROVES_CALIBRATION_NOT_ACCEPTANCE_READY"
    elif result_summary["dre5UsableAxisCount"] == result_summary["dre2UsableAxisCount"]:
        verdict = "REPAIRED_CROP_PIPELINE_NO_END_TO_END_IMPROVEMENT"
    else:
        verdict = "REPAIRED_CROP_PIPELINE_UNEXPECTED_REGRESSION"

    remaining_invalid = [
        {
            "fixtureId": row["fixtureId"],
            "graphId": row["graphId"],
            "dre5Decision": row["dre5Decision"],
            "xStatus": row["dre5XStatus"],
            "xFailureReason": row["xAxis"].get("failureReason"),
            "xMissingSafeOcrLabelCount": row["xAxis"].get("missingSafeOcrLabelCount"),
            "yStatus": row["dre5YStatus"],
            "yFailureReason": row["yAxis"].get("failureReason"),
            "yMissingSafeOcrLabelCount": row["yAxis"].get("missingSafeOcrLabelCount"),
        }
        for row in graph_results
        if row["dre5Decision"] in {"PARTIAL", "INVALID"}
    ]

    return {
        "schemaVersion": "chromalab.benchmark.dre5_repaired_crop_calibration_pipeline.v1",
        "overallVerdict": verdict,
        "productionImpact": "NONE_RESEARCH_ONLY",
        "dre1Source": str(args.dre1_summary).replace("\\", "/"),
        "dre2Source": str(args.dre2_summary).replace("\\", "/"),
        "dre4Source": str(args.dre4_summary).replace("\\", "/"),
        "textRoleTruthSource": str(args.drc4_manual).replace("\\", "/"),
        "baselineOcrVariantId": dre1["ocrVariantId"],
        "repairedOcrMethodId": dre4["methodId"],
        "resultSummary": result_summary,
        "graphResults": graph_results,
        "axisResults": axis_results,
        "repairedAxisTable": repaired_axis_table,
        "addedRepairedAnchors": added_repaired_anchors,
        "remainingInvalidOrPartialGraphs": remaining_invalid,
        "nextRequiredCapabilities": [
            "promote_repaired_crop_plan_to_runtime_candidate_after_android_parity",
            "bench06_graph2_ocr_visibility_recovery",
            "graph_level_calibration_gate_policy_for_repaired_crop_evidence",
        ],
    }


def build_markdown(summary: dict[str, Any]) -> str:
    counts = summary["resultSummary"]
    lines = [
        "# DR-E5 Repaired Crop Pipeline End-To-End Calibration Benchmark",
        "",
        f"Verdict: `{summary['overallVerdict']}`",
        f"Baseline OCR variant: `{summary['baselineOcrVariantId']}`",
        f"Repaired OCR method: `{summary['repairedOcrMethodId']}`",
        f"Added repaired anchors: `{counts['addedRepairedAnchorCount']}`",
        f"Usable axes DR-E2 -> DR-E5: `{counts['dre2UsableAxisCount']}` -> `{counts['dre5UsableAxisCount']}`",
        f"Usable graphs DR-E2 -> DR-E5: `{counts['dre2UsableGraphCount']}` -> `{counts['dre5UsableGraphCount']}`",
        "",
        "## Graph Calibration Summary",
        "",
        "| Fixture | Graph | DR-E2 | DR-E5 | Delta | X DR-E2 -> DR-E5 | X anchors | Y DR-E2 -> DR-E5 | Y anchors |",
        "| --- | --- | --- | --- | --- | --- | ---: | --- | ---: |",
    ]
    for graph in summary["graphResults"]:
        lines.append(
            "| `{fixtureId}` | `{graphId}` | `{dre2}` | `{dre5}` | `{delta}` | `{x2}` -> `{x5}` | {xa2}->{xa5} | `{y2}` -> `{y5}` | {ya2}->{ya5} |".format(
                fixtureId=graph["fixtureId"],
                graphId=graph["graphId"],
                dre2=graph["dre2Decision"],
                dre5=graph["dre5Decision"],
                delta=graph["decisionDelta"],
                x2=graph["dre2XStatus"],
                x5=graph["dre5XStatus"],
                xa2=graph["dre2XAnchors"],
                xa5=graph["dre5XAnchors"],
                y2=graph["dre2YStatus"],
                y5=graph["dre5YStatus"],
                ya2=graph["dre2YAnchors"],
                ya5=graph["dre5YAnchors"],
            )
        )
    lines.extend(
        [
            "",
            "## Repaired Axis Effects",
            "",
            "| Fixture | Graph | Axis | Added repaired anchors | DR-E2 | DR-E5 | Accepted anchors | Failure reason |",
            "| --- | --- | --- | ---: | --- | --- | ---: | --- |",
        ]
    )
    for row in summary["repairedAxisTable"]:
        lines.append(
            "| `{fixtureId}` | `{graphId}` | `{axis}` | {added} | `{before}` | `{after}` | {anchors} | `{reason}` |".format(
                fixtureId=row["fixtureId"],
                graphId=row["graphId"],
                axis=row["axis"],
                added=row["addedRepairedAnchorCount"],
                before=row["dre2Status"],
                after=row["dre5Status"],
                anchors=row["dre5AcceptedAnchorCount"],
                reason=row["dre5FailureReason"] or "",
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
    for row in summary["remainingInvalidOrPartialGraphs"]:
        lines.append(
            "| `{fixtureId}` | `{graphId}` | `{decision}` | `{xs}` / `{xr}` | `{ys}` / `{yr}` |".format(
                fixtureId=row["fixtureId"],
                graphId=row["graphId"],
                decision=row["dre5Decision"],
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
            f"Baseline graph decisions: `{counts['baselineGraphDecisionCounts']}`",
            f"Selected strategies: `{counts['selectedStrategyCounts']}`",
            f"Improved graphs: `{counts['improvedGraphCount']}`",
            f"Regressed graphs: `{counts['regressedGraphCount']}`",
            "",
            "## Interpretation",
            "",
            "- Repaired label-band crops are evaluated through the same robust-fit rules as DR-E2.",
            "- Added OCR labels remain evidence only; they are not fabricated coordinates or calibration values.",
            "- `bench_04_graph_3` and `bench_05_graph_2/3/4` become graph-level calibration candidates.",
            "- `bench_06_graph_2` remains partial because the repaired crop coverage did not recover X anchors.",
            "- This remains a benchmark prototype and does not alter Android runtime crop planning or calibration.",
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
        "--dre2-summary",
        type=Path,
        default=Path("benchmark/reports/dre2_robust_axis_scale_fit/summary.json"),
    )
    parser.add_argument(
        "--dre4-summary",
        type=Path,
        default=Path("benchmark/reports/dre4_label_band_crop_coverage_repair/summary.json"),
    )
    parser.add_argument(
        "--drc4-manual",
        type=Path,
        default=Path("benchmark/annotations/drc4_tick_text_role_annotations/manual-p0-tick-text-annotations.json"),
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("benchmark/reports/dre5_repaired_crop_calibration_pipeline"),
    )
    args = parser.parse_args()
    summary = build_summary(args)
    write_json(args.output / "summary.json", summary)
    (args.output / "summary.md").write_text(build_markdown(summary), encoding="utf-8", newline="\n")
    print(f"Built DR-E5 repaired crop calibration pipeline: {summary['overallVerdict']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
