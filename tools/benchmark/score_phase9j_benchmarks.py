#!/usr/bin/env python3
"""Score Phase 9J benchmark records and list fixture truth gaps."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any


STAGES = [
    "graph_panel",
    "calibration",
    "trace",
    "peaks",
    "report_claims",
    "evidence_package",
]

TRUTH_FIELDS = [
    "input_image_hash",
    "expected_graph_count",
    "graph_panel_bounds",
    "plot_area_bounds",
    "axis_endpoints",
    "tick_or_grid_positions",
    "numeric_label_boxes",
    "calibration_anchors",
    "trace_reference",
    "peak_reference_metrics",
    "report_claim_expectations",
]


def read_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def count_value(counts: dict[str, int], value: Any) -> None:
    key = str(value or "MISSING")
    counts[key] = counts.get(key, 0) + 1


def case_fixture_id(case_id: str) -> str:
    if not case_id.startswith("phase9j_"):
        return case_id
    body = case_id.removeprefix("phase9j_")
    for suffix in ["_deterministic", "_model_enabled"]:
        if body.endswith(suffix):
            return body[: -len(suffix)]
    return body


def case_mode(prediction: dict[str, Any], case_id: str) -> str:
    if prediction.get("mode") == "e2b_baseline":
        return "model_enabled"
    if prediction.get("mode") == "deterministic":
        return "deterministic"
    if case_id.endswith("_model_enabled"):
        return "model_enabled"
    return "deterministic"


def stage_map(metrics: dict[str, Any]) -> dict[str, dict[str, Any]]:
    return {
        str(score.get("stage")): score
        for score in metrics.get("stageScores", [])
        if score.get("stage")
    }


def first_graph(prediction: dict[str, Any]) -> dict[str, Any]:
    graphs = prediction.get("graphs") or []
    if graphs and isinstance(graphs[0], dict):
        return graphs[0]
    return {}


def stage_statuses(metrics: dict[str, Any]) -> dict[str, str]:
    stages = stage_map(metrics)
    return {stage: str(stages.get(stage, {}).get("status") or "MISSING") for stage in STAGES}


def truth_gaps_for_case(metrics: dict[str, Any], prediction: dict[str, Any]) -> list[str]:
    gaps = ["input_image_hash"]
    graph = first_graph(prediction)
    stages = stage_map(metrics)
    graph_stage = stages.get("graph_panel", {})
    graph_metrics = graph_stage.get("metrics", {})
    calibration = graph.get("calibration", {})
    trace = graph.get("trace", {})
    peaks = graph.get("peaks", {})

    if graph_metrics.get("expectedGraphCount") is None:
        gaps.append("expected_graph_count")
    if graph.get("graphPanelStatus") != "VALID":
        gaps.append("graph_panel_bounds")
    if graph.get("plotAreaStatus") != "VALID":
        gaps.append("plot_area_bounds")

    x_status = calibration.get("xStatus")
    y_status = calibration.get("yStatus")
    if x_status not in {"VALID", "REVIEW"} or y_status not in {"VALID", "REVIEW"}:
        gaps.extend(
            [
                "axis_endpoints",
                "tick_or_grid_positions",
                "numeric_label_boxes",
                "calibration_anchors",
            ]
        )
    if (calibration.get("xAnchorCount") or 0) == 0 or (calibration.get("yAnchorCount") or 0) == 0:
        gaps.extend(["tick_or_grid_positions", "numeric_label_boxes", "calibration_anchors"])

    if trace.get("status") != "VALID":
        gaps.append("trace_reference")
    if peaks.get("status") != "VALID":
        gaps.append("peak_reference_metrics")

    report_status = stages.get("report_claims", {}).get("status")
    if report_status != "PASS":
        gaps.append("report_claim_expectations")

    return sorted(set(gap for gap in gaps if gap in TRUTH_FIELDS))


def annotation_priority(metrics: dict[str, Any], prediction: dict[str, Any]) -> str:
    decision = metrics.get("overallDecision")
    runtime_failure = prediction.get("runtimeFailureClass")
    stages = stage_map(metrics)
    graph_metrics = stages.get("graph_panel", {}).get("metrics", {})
    if decision == "BLOCKED":
        return "P0"
    if graph_metrics.get("graphCountMatches") is False:
        return "P0"
    if runtime_failure in {"CALIBRATION_FAILURE", "TICK_LOCALIZATION_FAILURE", "GRAPH_PANEL_FAILURE"}:
        return "P1"
    if runtime_failure in {"PEAK_EVIDENCE_FAILURE", "UNKNOWN_FAILURE"}:
        return "P1"
    return "P2"


def build_case_record(case_dir: Path) -> dict[str, Any]:
    metrics = read_json(case_dir / "metrics.json")
    prediction = read_json(case_dir / "prediction.json")
    case_id = metrics["caseId"]
    graph = first_graph(prediction)
    stages = stage_map(metrics)
    graph_metrics = stages.get("graph_panel", {}).get("metrics", {})
    calibration = graph.get("calibration", {})
    trace = graph.get("trace", {})
    peaks = graph.get("peaks", {})
    return {
        "caseId": case_id,
        "fixtureId": case_fixture_id(case_id),
        "mode": case_mode(prediction, case_id),
        "runId": metrics.get("runId"),
        "overallDecision": metrics.get("overallDecision"),
        "reportGate": prediction.get("reportGate"),
        "validatorVerdict": prediction.get("validatorVerdict"),
        "runtimeFailureClass": prediction.get("runtimeFailureClass"),
        "firstFailingStage": prediction.get("firstFailingStage") or "",
        "expectedGraphCount": graph_metrics.get("expectedGraphCount"),
        "detectedGraphCount": graph_metrics.get("detectedGraphCount"),
        "graphCountMatches": bool(graph_metrics.get("graphCountMatches")),
        "layoutClass": graph.get("layoutClass") or "UNKNOWN_REVIEW",
        "stageStatuses": stage_statuses(metrics),
        "xCalibration": calibration.get("xStatus") or "MISSING",
        "yCalibration": calibration.get("yStatus") or "MISSING",
        "xAnchorCount": calibration.get("xAnchorCount") or 0,
        "yAnchorCount": calibration.get("yAnchorCount") or 0,
        "traceStatus": trace.get("status") or "MISSING",
        "peakStatus": peaks.get("status") or "MISSING",
        "peakCount": peaks.get("peakCount") or 0,
        "zeroToleranceFailures": metrics.get("zeroToleranceFailures") or [],
        "truthGaps": truth_gaps_for_case(metrics, prediction),
        "annotationPriority": annotation_priority(metrics, prediction),
        "casePath": str(case_dir).replace("\\", "/"),
    }


def build_summary(input_root: Path) -> dict[str, Any]:
    records = [
        build_case_record(case_dir)
        for case_dir in sorted(input_root.iterdir())
        if case_dir.is_dir() and (case_dir / "metrics.json").exists() and (case_dir / "prediction.json").exists()
    ]
    decision_counts: dict[str, int] = {}
    gate_counts: dict[str, int] = {}
    validator_counts: dict[str, int] = {}
    failure_counts: dict[str, int] = {}
    stage_counts: dict[str, dict[str, int]] = {stage: {} for stage in STAGES}
    priority_counts: dict[str, int] = {}
    truth_gap_counts: dict[str, int] = {field: 0 for field in TRUTH_FIELDS}

    for record in records:
        count_value(decision_counts, record["overallDecision"])
        count_value(gate_counts, record["reportGate"])
        count_value(validator_counts, record["validatorVerdict"])
        count_value(failure_counts, record["runtimeFailureClass"] or "NONE")
        count_value(priority_counts, record["annotationPriority"])
        for stage, status in record["stageStatuses"].items():
            count_value(stage_counts[stage], status)
        for gap in record["truthGaps"]:
            truth_gap_counts[gap] += 1

    return {
        "schemaVersion": "chromalab.benchmark.phase9j_score.v1",
        "inputRoot": str(input_root).replace("\\", "/"),
        "recordCount": len(records),
        "fixtureCount": len({record["fixtureId"] for record in records}),
        "decisionCounts": decision_counts,
        "reportGateCounts": gate_counts,
        "validatorVerdictCounts": validator_counts,
        "failureClassCounts": failure_counts,
        "stageStatusCounts": stage_counts,
        "annotationPriorityCounts": priority_counts,
        "truthGapCounts": {key: value for key, value in truth_gap_counts.items() if value},
        "records": records,
    }


def write_summary_markdown(path: Path, summary: dict[str, Any]) -> None:
    lines = [
        "# Phase 9J Benchmark Score Summary",
        "",
        f"Input root: `{summary['inputRoot']}`",
        "",
        f"Records: `{summary['recordCount']}`",
        f"Fixtures: `{summary['fixtureCount']}`",
        "",
        "## Counts",
        "",
        "| Metric | Counts |",
        "| --- | --- |",
        f"| Decisions | `{summary['decisionCounts']}` |",
        f"| Report gates | `{summary['reportGateCounts']}` |",
        f"| Validator verdicts | `{summary['validatorVerdictCounts']}` |",
        f"| Failure classes | `{summary['failureClassCounts']}` |",
        f"| Annotation priority | `{summary['annotationPriorityCounts']}` |",
        "",
        "## Stage Status Counts",
        "",
        "| Stage | Counts |",
        "| --- | --- |",
    ]
    for stage in STAGES:
        lines.append(f"| `{stage}` | `{summary['stageStatusCounts'][stage]}` |")

    lines.extend(
        [
            "",
            "## Case Table",
            "",
            "| Case | Decision | Gate | Failure | Graphs | Calibration | Trace | Peaks | Priority |",
            "| --- | --- | --- | --- | --- | --- | --- | --- | --- |",
        ]
    )
    for record in summary["records"]:
        lines.append(
            "| `{caseId}` | {overallDecision} | {reportGate} | {runtimeFailureClass} | "
            "{detectedGraphCount}/{expectedGraphCount} | X:{xCalibration} Y:{yCalibration} | "
            "{traceStatus} | {peakStatus} ({peakCount}) | {annotationPriority} |".format(
                **{**record, "runtimeFailureClass": record["runtimeFailureClass"] or "NONE"}
            )
        )

    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def build_truth_gaps(summary: dict[str, Any]) -> dict[str, Any]:
    fixtures: dict[str, dict[str, Any]] = {}
    for record in summary["records"]:
        fixture = fixtures.setdefault(
            record["fixtureId"],
            {
                "fixtureId": record["fixtureId"],
                "modes": [],
                "highestPriority": "P2",
                "truthGaps": set(),
                "blockingReasons": set(),
            },
        )
        fixture["modes"].append(
            {
                "mode": record["mode"],
                "caseId": record["caseId"],
                "decision": record["overallDecision"],
                "runtimeFailureClass": record["runtimeFailureClass"],
                "priority": record["annotationPriority"],
            }
        )
        fixture["truthGaps"].update(record["truthGaps"])
        if record["runtimeFailureClass"]:
            fixture["blockingReasons"].add(record["runtimeFailureClass"])
        if record["annotationPriority"] < fixture["highestPriority"]:
            fixture["highestPriority"] = record["annotationPriority"]

    normalized = []
    for fixture in fixtures.values():
        normalized.append(
            {
                "fixtureId": fixture["fixtureId"],
                "highestPriority": fixture["highestPriority"],
                "truthGaps": sorted(fixture["truthGaps"]),
                "blockingReasons": sorted(fixture["blockingReasons"]),
                "modes": fixture["modes"],
            }
        )

    return {
        "schemaVersion": "chromalab.benchmark.phase9j_truth_gaps.v1",
        "fixtureCount": len(normalized),
        "fixtures": sorted(normalized, key=lambda row: (row["highestPriority"], row["fixtureId"])),
    }


def write_truth_gaps_markdown(path: Path, truth_gaps: dict[str, Any]) -> None:
    lines = [
        "# Phase 9J Fixture Truth Gaps",
        "",
        f"Fixtures: `{truth_gaps['fixtureCount']}`",
        "",
        "| Fixture | Priority | Blocking reasons | Missing truth needed before method comparison |",
        "| --- | --- | --- | --- |",
    ]
    for fixture in truth_gaps["fixtures"]:
        lines.append(
            "| `{fixtureId}` | {highestPriority} | {blockingReasons} | {truthGaps} |".format(
                fixtureId=fixture["fixtureId"],
                highestPriority=fixture["highestPriority"],
                blockingReasons=", ".join(fixture["blockingReasons"]) or "None",
                truthGaps=", ".join(fixture["truthGaps"]) or "None",
            )
        )
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--input",
        type=Path,
        default=Path("benchmark/examples/phase9j_truth_audit"),
        help="Phase 9J benchmark record root.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("benchmark/reports/phase9j_truth_audit_score"),
        help="Output directory for score summaries.",
    )
    args = parser.parse_args()

    if not args.input.exists():
        raise SystemExit(f"Benchmark input root not found: {args.input}")

    summary = build_summary(args.input)
    truth_gaps = build_truth_gaps(summary)
    write_json(args.output / "summary.json", summary)
    write_summary_markdown(args.output / "summary.md", summary)
    write_json(args.output / "truth-gaps.json", truth_gaps)
    write_truth_gaps_markdown(args.output / "truth-gaps.md", truth_gaps)

    print(
        "Scored {records} records from {fixtures} fixtures into {output}.".format(
            records=summary["recordCount"],
            fixtures=summary["fixtureCount"],
            output=args.output,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
