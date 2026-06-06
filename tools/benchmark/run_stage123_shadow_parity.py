#!/usr/bin/env python3
"""Build Stage 1-3 shadow parity records from current benchmark evidence.

This is a PC-side benchmark harness only. It does not change Android runtime,
validators, report gates, chromatographic math, graph-count expectations, model
policy, or CalculationEngine.
"""

from __future__ import annotations

import argparse
import json
import shutil
from pathlib import Path
from typing import Any


PHASE9J_ROOT = Path("benchmark/examples/phase9j_truth_audit")
PHASE9J_SCORE = Path("benchmark/reports/phase9j_truth_audit_score/summary.json")
DRC3_SUMMARY = Path("benchmark/annotations/drc3_initial_graph_layout_annotations/summary.json")
DRC6_SUMMARY = Path("benchmark/reports/drc6_p0_graph_layout_method_comparison/summary.json")
DRC7_SUMMARY = Path("benchmark/reports/drc7_panel_semantic_classifier/summary.json")
ANDROID_METADATA_DIR = Path("composeApp/src/androidMain/assets/validation")
EXAMPLE_ROOT = Path("benchmark/examples/stage123_shadow_parity")
REPORT_ROOT = Path("benchmark/reports/stage123_shadow_parity")


def read_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def write_text(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write(text)


def load_metadata() -> dict[str, dict[str, Any]]:
    metadata_by_fixture: dict[str, dict[str, Any]] = {}
    for path in sorted(ANDROID_METADATA_DIR.glob("*.metadata.json")):
        data = read_json(path)
        metadata_by_fixture[data["fixtureId"]] = data
    return metadata_by_fixture


def load_prediction(case_path: str) -> dict[str, Any]:
    prediction_path = Path(case_path) / "prediction.json"
    if prediction_path.exists():
        return read_json(prediction_path)
    return {}


def first_graph(prediction: dict[str, Any]) -> dict[str, Any]:
    graphs = prediction.get("graphs") or []
    if graphs and isinstance(graphs[0], dict):
        return graphs[0]
    return {}


def stage_status(record: dict[str, Any], name: str, fallback: str = "NOT_RECORDED") -> str:
    return str((record.get("stageStatuses") or {}).get(name) or fallback)


def graph_count_score(expected: int, detected: int | None) -> str:
    if detected is None:
        return "MISSING"
    return "PASS" if expected == detected else "FAIL"


def layout_score(expected: str, predicted: str | None) -> str:
    if not predicted:
        return "MISSING"
    if predicted in {"UNKNOWN_REVIEW", "NOT_EXPOSED"}:
        return "MISSING"
    return "PASS" if expected == predicted else "FAIL"


def stage_evidence(status: str, available: bool, summary: str, metrics: dict[str, Any] | None = None) -> dict[str, Any]:
    payload: dict[str, Any] = {
        "status": status,
        "available": available,
        "summary": summary,
    }
    if metrics:
        payload["metrics"] = metrics
    return payload


def source_image_for(metadata_by_fixture: dict[str, dict[str, Any]], fixture_id: str) -> str:
    metadata = metadata_by_fixture.get(fixture_id) or {}
    return str(metadata.get("assetImagePath") or "")


def expected_layouts() -> dict[str, str]:
    drc3 = read_json(DRC3_SUMMARY)
    layouts = {
        record["fixtureId"]: record["layoutClass"]
        for record in drc3.get("records", [])
    }
    layouts.update(
        {
            "white_tiger_ion71": "SINGLE_TRACE_SINGLE_AXIS",
            "bench_02_mz92_belyi_tigr": "SINGLE_TRACE_SINGLE_AXIS",
            "bench_03_small_tic_export": "LOW_RES_EXPORT_GRAPH",
            "bench_07_rotated_page_photo": "ROTATED_PAGE_GRAPH",
        }
    )
    return layouts


def build_active_baseline_records(metadata_by_fixture: dict[str, dict[str, Any]]) -> list[dict[str, Any]]:
    score = read_json(PHASE9J_SCORE)
    layouts = expected_layouts()
    records: list[dict[str, Any]] = []
    for source in score.get("records", []):
        fixture_id = source["fixtureId"]
        mode = source["mode"]
        prediction = load_prediction(source["casePath"])
        graph = first_graph(prediction)
        expected_count = int(source.get("expectedGraphCount") or metadata_by_fixture[fixture_id]["expectedGraphCount"])
        detected_count = int(source.get("detectedGraphCount") or 0)
        expected_layout = layouts.get(fixture_id, "UNKNOWN_REVIEW")
        predicted_layout = str(source.get("layoutClass") or graph.get("layoutClass") or "UNKNOWN_REVIEW")
        artifacts = prediction.get("artifacts") or {}
        artifact_paths = [
            value
            for value in [
                artifacts.get("runtimeEvidencePackage"),
                artifacts.get("validatorJson"),
                artifacts.get("validatorMarkdown"),
                artifacts.get("finalReportJson"),
                artifacts.get("htmlReport"),
                artifacts.get("markdownReport"),
            ]
            if isinstance(value, str) and value
        ]
        artifact_paths.extend([path for path in artifacts.get("overlayPaths", []) if isinstance(path, str)])
        record_id = f"stage123_{fixture_id}_{mode}_android_phase9j_current"
        records.append(
            {
                "schemaVersion": "chromalab.benchmark.stage123_parity_record.v1",
                "recordId": record_id,
                "caseId": source["caseId"],
                "runId": str(source.get("runId") or ""),
                "fixtureId": fixture_id,
                "mode": mode,
                "sourceId": "android_phase9j_current",
                "sourceKind": "ACTIVE_ANDROID_BASELINE",
                "productionImpact": "NONE_SHADOW_ONLY",
                "runtimeReadiness": "ACTIVE_RUNTIME_RECORDED_OUTPUT",
                "expectedGraphCount": expected_count,
                "detectedGraphCount": detected_count,
                "graphCountScore": graph_count_score(expected_count, detected_count),
                "expectedLayoutClass": expected_layout,
                "predictedLayoutClass": predicted_layout,
                "layoutClassScore": layout_score(expected_layout, predicted_layout),
                "imagePreparation": stage_evidence(
                    "NOT_RECORDED",
                    False,
                    "Phase 9J benchmark records do not lock normalized-image hash, dimensions, orientation, or preprocessing variant.",
                    {"truthGap": "input_image_hash"},
                ),
                "graphDiscovery": stage_evidence(
                    stage_status(source, "graph_panel", "NOT_RECORDED"),
                    True,
                    "Active Android baseline graph-panel stage from Phase 9J records.",
                    {
                        "expectedGraphCount": expected_count,
                        "detectedGraphCount": detected_count,
                        "graphCountMatches": expected_count == detected_count,
                    },
                ),
                "plotAreaLayout": stage_evidence(
                    str(graph.get("plotAreaStatus") or "NOT_RECORDED"),
                    bool(graph.get("plotAreaStatus")),
                    "PlotArea/layout status from normalized Phase 9J prediction record.",
                    {
                        "graphPanelStatus": str(graph.get("graphPanelStatus") or "MISSING"),
                        "plotAreaStatus": str(graph.get("plotAreaStatus") or "MISSING"),
                    },
                ),
                "failureClass": str(source.get("runtimeFailureClass") or ""),
                "firstFailingStage": str(source.get("firstFailingStage") or ""),
                "evidence": {
                    "sourceImage": source_image_for(metadata_by_fixture, fixture_id),
                    "recordSource": str(Path(source["casePath"]) / "prediction.json").replace("\\", "/"),
                    "artifactPaths": artifact_paths,
                },
                "promotionDecision": "BASELINE_RECORD_ONLY",
                "notes": [
                    "This is the current active Android baseline record.",
                    "It is not a replacement candidate.",
                ],
            }
        )
    return records


def build_drc6_records(metadata_by_fixture: dict[str, dict[str, Any]]) -> list[dict[str, Any]]:
    summary = read_json(DRC6_SUMMARY)
    records: list[dict[str, Any]] = []
    for source in summary.get("prototypeScores", []):
        fixture_id = source["fixtureId"]
        expected_count = int(source["expectedGraphCount"])
        detected_count = int(source["detectedGraphCount"])
        method_id = source["methodId"]
        record_id = f"stage123_{fixture_id}_pc_prototype_{method_id}"
        artifact_paths = [source["overlayPath"]] if source.get("overlayPath") else []
        records.append(
            {
                "schemaVersion": "chromalab.benchmark.stage123_parity_record.v1",
                "recordId": record_id,
                "fixtureId": fixture_id,
                "mode": "pc_prototype",
                "sourceId": method_id,
                "sourceKind": "PC_PROTOTYPE",
                "productionImpact": "NONE_SHADOW_ONLY",
                "runtimeReadiness": "PC_RESEARCH_PROTOTYPE_NOT_RUNTIME_READY",
                "expectedGraphCount": expected_count,
                "detectedGraphCount": detected_count,
                "graphCountScore": source["graphCountScore"],
                "expectedLayoutClass": source["expectedLayoutClass"],
                "predictedLayoutClass": source["predictedLayoutClass"],
                "layoutClassScore": source["layoutClassScore"],
                "imagePreparation": stage_evidence(
                    "NOT_SCORED",
                    False,
                    "DR-C6 prototype consumes source images directly and does not produce Stage 1 normalized-image truth.",
                ),
                "graphDiscovery": stage_evidence(
                    "PASS" if source["graphCountScore"] == "PASS" else "FAIL",
                    True,
                    "PC prototype graph-count score from row/label-band projection.",
                    {
                        "expectedGraphCount": expected_count,
                        "detectedGraphCount": detected_count,
                        "selectedRowCount": len(source.get("selectedRows") or []),
                    },
                ),
                "plotAreaLayout": stage_evidence(
                    "PASS" if source["layoutClassScore"] == "PASS" else "FAIL",
                    source["layoutClassScore"] != "MISSING",
                    "Prototype layout class score; plotArea boxes are not production runtime outputs.",
                ),
                "failureClass": "",
                "firstFailingStage": "",
                "evidence": {
                    "sourceImage": source_image_for(metadata_by_fixture, fixture_id),
                    "recordSource": str(DRC6_SUMMARY).replace("\\", "/"),
                    "artifactPaths": artifact_paths,
                },
                "promotionDecision": "DO_NOT_PROMOTE_RESEARCH_ONLY",
                "notes": [
                    source.get("reason") or "",
                    "Graph/layout improvement signal only; not Rust, not Android runtime, not production authority.",
                ],
            }
        )
    return records


def build_drc7_records(metadata_by_fixture: dict[str, dict[str, Any]]) -> list[dict[str, Any]]:
    summary = read_json(DRC7_SUMMARY)
    records: list[dict[str, Any]] = []
    for source in summary.get("caseScores", []):
        fixture_id = source["fixtureId"]
        expected_count = int(source["expectedGraphCount"])
        detected_count = int(source["detectedGraphCount"])
        method_id = source["methodId"]
        record_id = f"stage123_{fixture_id}_annotation_upper_bound_{method_id}"
        records.append(
            {
                "schemaVersion": "chromalab.benchmark.stage123_parity_record.v1",
                "recordId": record_id,
                "fixtureId": fixture_id,
                "mode": "annotation_upper_bound",
                "sourceId": method_id,
                "sourceKind": "ANNOTATION_UPPER_BOUND",
                "productionImpact": "NONE_SHADOW_ONLY",
                "runtimeReadiness": source.get("runtimeReadiness") or "ANNOTATION_UPPER_BOUND_NOT_RUNTIME_READY",
                "expectedGraphCount": expected_count,
                "detectedGraphCount": detected_count,
                "graphCountScore": graph_count_score(expected_count, detected_count),
                "expectedLayoutClass": source["expectedLayoutClass"],
                "predictedLayoutClass": source["predictedLayoutClass"],
                "layoutClassScore": source["layoutClassScore"],
                "imagePreparation": stage_evidence(
                    "NOT_SCORED",
                    False,
                    "DR-C7 uses annotation-derived semantic/page context and does not produce Stage 1 image-prep evidence.",
                ),
                "graphDiscovery": stage_evidence(
                    "PASS" if expected_count == detected_count else "FAIL",
                    True,
                    "Graph count inherited from DR-C6 graph-row evidence.",
                    {
                        "expectedGraphCount": expected_count,
                        "detectedGraphCount": detected_count,
                    },
                ),
                "plotAreaLayout": stage_evidence(
                    "PASS" if source["layoutClassScore"] == "PASS" else "FAIL",
                    True,
                    "Annotation upper-bound layout classification; requires automatic OCR/page-context extraction before runtime use.",
                ),
                "failureClass": "",
                "firstFailingStage": "",
                "evidence": {
                    "sourceImage": source_image_for(metadata_by_fixture, fixture_id),
                    "recordSource": str(DRC7_SUMMARY).replace("\\", "/"),
                    "artifactPaths": [],
                },
                "promotionDecision": "DO_NOT_PROMOTE_UPPER_BOUND_ONLY",
                "notes": [
                    source.get("reason") or "",
                    "Upper-bound result only; not automatic runtime evidence.",
                ],
            }
        )
    return records


def source_summary(records: list[dict[str, Any]]) -> list[dict[str, Any]]:
    by_source: dict[str, list[dict[str, Any]]] = {}
    for record in records:
        by_source.setdefault(record["sourceId"], []).append(record)
    summaries = []
    for source_id, items in sorted(by_source.items()):
        graph_pass = sum(1 for item in items if item["graphCountScore"] == "PASS")
        layout_pass = sum(1 for item in items if item["layoutClassScore"] == "PASS")
        summaries.append(
            {
                "sourceId": source_id,
                "sourceKind": items[0]["sourceKind"],
                "recordCount": len(items),
                "graphCountPass": graph_pass,
                "graphCountFailOrMissing": len(items) - graph_pass,
                "layoutClassPass": layout_pass,
                "layoutClassFailOrMissing": len(items) - layout_pass,
                "runtimeReadiness": items[0]["runtimeReadiness"],
                "promotionDecision": items[0]["promotionDecision"],
            }
        )
    return summaries


def e2b_comparisons(records: list[dict[str, Any]]) -> list[dict[str, Any]]:
    current = [record for record in records if record["sourceId"] == "android_phase9j_current"]
    by_fixture: dict[str, dict[str, dict[str, Any]]] = {}
    for record in current:
        by_fixture.setdefault(record["fixtureId"], {})[record["mode"]] = record
    comparisons = []
    for fixture_id, modes in sorted(by_fixture.items()):
        deterministic = modes.get("deterministic")
        model_enabled = modes.get("model_enabled")
        if not deterministic or not model_enabled:
            comparisons.append(
                {
                    "fixtureId": fixture_id,
                    "comparison": "MISSING_MODE",
                    "reason": "Both deterministic and model_enabled baseline records are required.",
                }
            )
            continue
        graph_delta = model_enabled["detectedGraphCount"] - deterministic["detectedGraphCount"]
        layout_changed = model_enabled["predictedLayoutClass"] != deterministic["predictedLayoutClass"]
        if graph_delta < 0:
            comparison = "E2B_GRAPH_COUNT_REGRESSION"
        elif layout_changed:
            comparison = "E2B_LAYOUT_DISAGREEMENT_REVIEW"
        else:
            comparison = "E2B_NEUTRAL_FOR_STAGE123"
        comparisons.append(
            {
                "fixtureId": fixture_id,
                "deterministicGraphCount": deterministic["detectedGraphCount"],
                "modelEnabledGraphCount": model_enabled["detectedGraphCount"],
                "graphCountDelta": graph_delta,
                "deterministicLayoutClass": deterministic["predictedLayoutClass"],
                "modelEnabledLayoutClass": model_enabled["predictedLayoutClass"],
                "layoutChanged": layout_changed,
                "comparison": comparison,
            }
        )
    return comparisons


def build_summary(records: list[dict[str, Any]]) -> dict[str, Any]:
    summaries = source_summary(records)
    return {
        "schemaVersion": "chromalab.benchmark.stage123_shadow_parity_summary.v1",
        "productionImpact": "NONE_SHADOW_ONLY",
        "overallVerdict": "R2_SHADOW_PARITY_HARNESS_READY_PRODUCTION_UNCHANGED",
        "recordCount": len(records),
        "fixtureCount": len({record["fixtureId"] for record in records}),
        "sourceCount": len(summaries),
        "inputSources": {
            "phase9jScore": str(PHASE9J_SCORE).replace("\\", "/"),
            "phase9jBenchmarkRoot": str(PHASE9J_ROOT).replace("\\", "/"),
            "drc6MethodComparison": str(DRC6_SUMMARY).replace("\\", "/"),
            "drc7SemanticUpperBound": str(DRC7_SUMMARY).replace("\\", "/"),
            "fixtureMetadata": str(ANDROID_METADATA_DIR).replace("\\", "/"),
        },
        "sourceSummaries": summaries,
        "e2bComparisons": e2b_comparisons(records),
        "nextRequiredWork": [
            "Add a real Rust/Kotlin Stage 1 image-preparation candidate that emits normalized-image evidence.",
            "Add a Rust/Kotlin graph-discovery candidate with graphPanel candidate/rejection tables.",
            "Add automatic OCR text-role/page-context extraction before using DRC7-style semantic layout classification.",
            "Keep shadow records out of production report gates until promotion criteria pass.",
        ],
    }


def write_record_examples(records: list[dict[str, Any]], clean: bool) -> None:
    if clean and EXAMPLE_ROOT.exists():
        shutil.rmtree(EXAMPLE_ROOT)
    EXAMPLE_ROOT.mkdir(parents=True, exist_ok=True)
    for record in records:
        out = EXAMPLE_ROOT / record["recordId"] / "stage123-parity-record.json"
        write_json(out, record)


def write_markdown(summary: dict[str, Any], records: list[dict[str, Any]]) -> None:
    lines = [
        "# R2 Stage 1-3 Shadow Parity Harness",
        "",
        f"Verdict: `{summary['overallVerdict']}`",
        "",
        "Production impact: `NONE_SHADOW_ONLY`",
        "",
        f"Records: `{summary['recordCount']}`",
        f"Fixtures: `{summary['fixtureCount']}`",
        f"Sources: `{summary['sourceCount']}`",
        "",
        "R2 does not change Android runtime behavior, validators, report gates, graph-count expectations, chromatographic math, model policy, or CalculationEngine.",
        "",
        "## Source Summary",
        "",
        "| Source | Kind | Records | Graph count pass | Layout pass | Runtime readiness | Promotion decision |",
        "|---|---|---:|---:|---:|---|---|",
    ]
    for item in summary["sourceSummaries"]:
        lines.append(
            "| `{sourceId}` | `{sourceKind}` | {recordCount} | {graphCountPass} | {layoutClassPass} | `{runtimeReadiness}` | `{promotionDecision}` |".format(
                **item
            )
        )
    lines.extend(
        [
            "",
            "## Active Baseline Records",
            "",
            "| Fixture | Mode | Expected graphs | Detected graphs | Graph score | Expected layout | Predicted layout | Layout score | Failure |",
            "|---|---|---:|---:|---|---|---|---|---|",
        ]
    )
    for record in records:
        if record["sourceId"] != "android_phase9j_current":
            continue
        lines.append(
            "| `{fixtureId}` | `{mode}` | {expectedGraphCount} | {detectedGraphCount} | {graphCountScore} | {expectedLayoutClass} | {predictedLayoutClass} | {layoutClassScore} | `{failureClass}` |".format(
                **record
            )
        )
    lines.extend(
        [
            "",
            "## E2B Stage 1-3 Comparison",
            "",
            "| Fixture | Deterministic graphs | E2B graphs | Delta | Layout changed | Decision |",
            "|---|---:|---:|---:|---|---|",
        ]
    )
    for item in summary["e2bComparisons"]:
        lines.append(
            "| `{fixtureId}` | {deterministicGraphCount} | {modelEnabledGraphCount} | {graphCountDelta} | {layoutChanged} | `{comparison}` |".format(
                **item
            )
        )
    lines.extend(
        [
            "",
            "## Next Required Work",
            "",
        ]
    )
    for item in summary["nextRequiredWork"]:
        lines.append(f"- {item}")
    lines.append("")
    write_text(REPORT_ROOT / "summary.md", "\n".join(lines))


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--clean", action="store_true", help="Remove previous R2 generated examples/report before writing.")
    args = parser.parse_args(argv)

    if args.clean:
        for path in [EXAMPLE_ROOT, REPORT_ROOT]:
            if path.exists():
                shutil.rmtree(path)

    metadata_by_fixture = load_metadata()
    records = []
    records.extend(build_active_baseline_records(metadata_by_fixture))
    records.extend(build_drc6_records(metadata_by_fixture))
    records.extend(build_drc7_records(metadata_by_fixture))
    summary = build_summary(records)

    write_record_examples(records, clean=False)
    write_json(REPORT_ROOT / "summary.json", summary)
    write_markdown(summary, records)
    print(
        "R2 Stage 1-3 shadow parity harness wrote "
        f"{len(records)} records to {EXAMPLE_ROOT.as_posix()} and report to {REPORT_ROOT.as_posix()}."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
