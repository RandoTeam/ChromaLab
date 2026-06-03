#!/usr/bin/env python3
"""Score Phase 9J P0 graph/layout output against DR-C3/DR-C4 annotation truth."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any


MODES = ["deterministic", "model_enabled"]


def read_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def stage_map(metrics: dict[str, Any]) -> dict[str, dict[str, Any]]:
    return {
        str(score.get("stage")): score
        for score in metrics.get("stageScores", [])
        if score.get("stage")
    }


def case_id(fixture_id: str, mode: str) -> str:
    return f"phase9j_{fixture_id}_{mode}"


def load_case(benchmark_root: Path, fixture_id: str, mode: str) -> tuple[dict[str, Any], dict[str, Any]]:
    root = benchmark_root / case_id(fixture_id, mode)
    return read_json(root / "metrics.json"), read_json(root / "prediction.json")


def first_prediction_graph(prediction: dict[str, Any]) -> dict[str, Any]:
    graphs = prediction.get("graphs") or []
    if graphs and isinstance(graphs[0], dict):
        return graphs[0]
    return {}


def report_gate_rank(gate: str | None) -> int:
    return {
        "RELEASE_READY": 4,
        "REVIEW_ONLY": 3,
        "DIAGNOSTIC_ONLY": 2,
        "BLOCKED": 1,
    }.get(str(gate or ""), 0)


def score_layout_class(predicted: str | None, expected: str) -> str:
    if predicted == expected:
        return "PASS"
    if predicted in {None, "", "UNKNOWN_REVIEW"}:
        return "MISSING"
    return "FAIL"


def current_output_decision(graph_count_score: str, layout_class_score: str) -> str:
    if graph_count_score == "FAIL":
        return "FAIL_GRAPH_COUNT"
    if layout_class_score == "FAIL":
        return "FAIL_LAYOUT_CLASS"
    if layout_class_score == "MISSING":
        return "REVIEW_LAYOUT_CLASS_MISSING"
    return "REVIEW_BOUNDS_NOT_SCOREABLE"


def score_case(
    *,
    benchmark_root: Path,
    truth_record: dict[str, Any],
    tick_truth: dict[str, Any],
    mode: str,
) -> dict[str, Any]:
    metrics, prediction = load_case(benchmark_root, truth_record["fixtureId"], mode)
    graph_panel_stage = stage_map(metrics).get("graph_panel", {})
    graph_panel_metrics = graph_panel_stage.get("metrics", {})
    predicted_graph = first_prediction_graph(prediction)

    expected_graph_count = int(truth_record["expectedPhysicalGraphCount"])
    detected_graph_count = int(graph_panel_metrics.get("detectedGraphCount") or 0)
    graph_count_score = "PASS" if expected_graph_count == detected_graph_count else "FAIL"
    predicted_layout = predicted_graph.get("layoutClass") or "MISSING"
    expected_layout = truth_record["layoutClass"]
    layout_class_score = score_layout_class(predicted_layout, expected_layout)
    decision = current_output_decision(graph_count_score, layout_class_score)

    return {
        "fixtureId": truth_record["fixtureId"],
        "mode": mode,
        "caseId": metrics["caseId"],
        "runId": metrics.get("runId"),
        "expectedGraphCount": expected_graph_count,
        "detectedGraphCount": detected_graph_count,
        "graphCountScore": graph_count_score,
        "expectedLayoutClass": expected_layout,
        "predictedLayoutClass": predicted_layout,
        "layoutClassScore": layout_class_score,
        "graphPanelStageStatus": graph_panel_stage.get("status") or "MISSING",
        "graphPanelStatus": graph_panel_metrics.get("graphPanelStatus") or "MISSING",
        "plotAreaStatus": graph_panel_metrics.get("plotAreaStatus") or "MISSING",
        "reportGate": prediction.get("reportGate"),
        "validatorVerdict": prediction.get("validatorVerdict"),
        "runtimeFailureClass": prediction.get("runtimeFailureClass"),
        "firstFailingStage": prediction.get("firstFailingStage") or "",
        "predictedBoundsScore": "NOT_SCOREABLE_MISSING_PREDICTED_BOUNDS",
        "truthGraphOverlay": truth_record.get("overlayPath"),
        "truthTickTextOverlay": tick_truth.get("overlayPath"),
        "truthMajorTickCount": tick_truth.get("majorTickCount") or 0,
        "truthReviewAnchorCount": tick_truth.get("reviewAnchorCount") or 0,
        "currentOutputDecision": decision,
        "reason": reason_for_case(
            expected_graph_count=expected_graph_count,
            detected_graph_count=detected_graph_count,
            expected_layout=expected_layout,
            predicted_layout=predicted_layout,
            graph_count_score=graph_count_score,
            layout_class_score=layout_class_score,
        ),
    }


def reason_for_case(
    *,
    expected_graph_count: int,
    detected_graph_count: int,
    expected_layout: str,
    predicted_layout: str,
    graph_count_score: str,
    layout_class_score: str,
) -> str:
    if graph_count_score == "FAIL":
        return f"Detected {detected_graph_count} graph(s), but P0 annotation truth has {expected_graph_count} physical graph(s)."
    if layout_class_score == "FAIL":
        return f"Predicted layout {predicted_layout}, but annotation truth is {expected_layout}."
    if layout_class_score == "MISSING":
        return "Graph count matched, but current output did not expose a scoreable layout class."
    return "Graph count and layout class are review-grade, but predicted graph bounds are not available for IoU scoring."


def compare_modes(fixture_id: str, cases: list[dict[str, Any]]) -> dict[str, Any]:
    by_mode = {case["mode"]: case for case in cases if case["fixtureId"] == fixture_id}
    deterministic = by_mode.get("deterministic")
    model_enabled = by_mode.get("model_enabled")
    if not deterministic or not model_enabled:
        return {
            "fixtureId": fixture_id,
            "e2bComparison": "MISSING_MODE",
            "reason": "Both deterministic and model_enabled cases are required.",
        }

    graph_delta = model_enabled["detectedGraphCount"] - deterministic["detectedGraphCount"]
    gate_delta = report_gate_rank(model_enabled["reportGate"]) - report_gate_rank(deterministic["reportGate"])
    layout_changed = model_enabled["predictedLayoutClass"] != deterministic["predictedLayoutClass"]
    if graph_delta < 0 or gate_delta < 0:
        comparison = "E2B_REGRESSION"
    elif graph_delta > 0 or gate_delta > 0:
        comparison = "E2B_IMPROVED_REVIEW_SIGNAL"
    elif layout_changed:
        comparison = "E2B_LAYOUT_DISAGREEMENT_NO_COUNT_REGRESSION"
    else:
        comparison = "E2B_NEUTRAL"

    return {
        "fixtureId": fixture_id,
        "deterministicGraphCount": deterministic["detectedGraphCount"],
        "e2bGraphCount": model_enabled["detectedGraphCount"],
        "graphCountDelta": graph_delta,
        "deterministicGate": deterministic["reportGate"],
        "e2bGate": model_enabled["reportGate"],
        "reportGateDelta": gate_delta,
        "deterministicLayoutClass": deterministic["predictedLayoutClass"],
        "e2bLayoutClass": model_enabled["predictedLayoutClass"],
        "layoutChanged": layout_changed,
        "e2bComparison": comparison,
    }


def build_summary(
    *,
    benchmark_root: Path,
    drc3_summary: dict[str, Any],
    drc4_summary: dict[str, Any],
) -> dict[str, Any]:
    tick_truth_by_fixture = {record["fixtureId"]: record for record in drc4_summary["records"]}
    case_scores = []
    for truth_record in drc3_summary["records"]:
        tick_truth = tick_truth_by_fixture[truth_record["fixtureId"]]
        for mode in MODES:
            case_scores.append(
                score_case(
                    benchmark_root=benchmark_root,
                    truth_record=truth_record,
                    tick_truth=tick_truth,
                    mode=mode,
                )
            )

    fixture_ids = [record["fixtureId"] for record in drc3_summary["records"]]
    comparisons = [compare_modes(fixture_id, case_scores) for fixture_id in fixture_ids]

    graph_count_pass = sum(1 for case in case_scores if case["graphCountScore"] == "PASS")
    layout_pass = sum(1 for case in case_scores if case["layoutClassScore"] == "PASS")
    e2b_regressions = sum(1 for comparison in comparisons if comparison["e2bComparison"] == "E2B_REGRESSION")
    return {
        "schemaVersion": "chromalab.benchmark.drc5_p0_graph_layout_score.v1",
        "benchmarkRoot": str(benchmark_root).replace("\\", "/"),
        "truthSource": "benchmark/annotations/drc3_initial_graph_layout_annotations/summary.json",
        "tickTextTruthSource": "benchmark/annotations/drc4_tick_text_role_annotations/summary.json",
        "caseCount": len(case_scores),
        "fixtureCount": len(fixture_ids),
        "graphCountPass": graph_count_pass,
        "graphCountFail": len(case_scores) - graph_count_pass,
        "layoutClassPass": layout_pass,
        "layoutClassFailOrMissing": len(case_scores) - layout_pass,
        "e2bRegressionCount": e2b_regressions,
        "overallVerdict": "CURRENT_OUTPUT_FAILS_P0_GRAPH_LAYOUT_TRUTH"
        if graph_count_pass < len(case_scores)
        else "CURRENT_OUTPUT_REVIEW_READY_FOR_BOUNDS_IOU_SCORING",
        "caseScores": case_scores,
        "e2bComparisons": comparisons,
    }


def write_markdown(path: Path, summary: dict[str, Any]) -> None:
    lines = [
        "# DR-C5 P0 Graph Layout Score",
        "",
        f"Verdict: `{summary['overallVerdict']}`",
        f"Cases: `{summary['caseCount']}`",
        f"Fixtures: `{summary['fixtureCount']}`",
        f"Graph count pass/fail: `{summary['graphCountPass']}` / `{summary['graphCountFail']}`",
        f"Layout class pass/fail-or-missing: `{summary['layoutClassPass']}` / `{summary['layoutClassFailOrMissing']}`",
        f"E2B regressions: `{summary['e2bRegressionCount']}`",
        "",
        "## Case Scores",
        "",
        "| Fixture | Mode | Truth graphs | Detected | Graph count | Truth layout | Predicted layout | Layout score | Gate | Runtime failure | Decision | Reason |",
        "| --- | --- | ---: | ---: | --- | --- | --- | --- | --- | --- | --- | --- |",
    ]
    for case in summary["caseScores"]:
        lines.append(
            "| `{fixtureId}` | {mode} | {expectedGraphCount} | {detectedGraphCount} | "
            "{graphCountScore} | {expectedLayoutClass} | {predictedLayoutClass} | "
            "{layoutClassScore} | {reportGate} | {runtimeFailureClass} | {currentOutputDecision} | {reason} |".format(
                **{
                    **case,
                    "runtimeFailureClass": case["runtimeFailureClass"] or "NONE",
                    "reason": str(case["reason"]).replace("|", "/"),
                }
            )
        )

    lines.extend(
        [
            "",
            "## E2B Comparison",
            "",
            "| Fixture | Deterministic graphs | E2B graphs | Gate delta | Layout changed | Comparison |",
            "| --- | ---: | ---: | ---: | --- | --- |",
        ]
    )
    for comparison in summary["e2bComparisons"]:
        lines.append(
            "| `{fixtureId}` | {deterministicGraphCount} | {e2bGraphCount} | {reportGateDelta} | "
            "{layoutChanged} | {e2bComparison} |".format(**comparison)
        )
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--benchmark-root",
        type=Path,
        default=Path("benchmark/examples/phase9j_truth_audit"),
        help="Phase 9J benchmark records root.",
    )
    parser.add_argument(
        "--drc3-summary",
        type=Path,
        default=Path("benchmark/annotations/drc3_initial_graph_layout_annotations/summary.json"),
        help="DR-C3 graph layout annotation summary.",
    )
    parser.add_argument(
        "--drc4-summary",
        type=Path,
        default=Path("benchmark/annotations/drc4_tick_text_role_annotations/summary.json"),
        help="DR-C4 tick/text annotation summary.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("benchmark/reports/drc5_p0_graph_layout_score"),
        help="Output directory.",
    )
    args = parser.parse_args()

    summary = build_summary(
        benchmark_root=args.benchmark_root,
        drc3_summary=read_json(args.drc3_summary),
        drc4_summary=read_json(args.drc4_summary),
    )
    write_json(args.output / "summary.json", summary)
    write_markdown(args.output / "summary.md", summary)
    print(
        "Scored {cases} P0 graph/layout cases: {passed} graph-count pass, {failed} fail.".format(
            cases=summary["caseCount"],
            passed=summary["graphCountPass"],
            failed=summary["graphCountFail"],
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
