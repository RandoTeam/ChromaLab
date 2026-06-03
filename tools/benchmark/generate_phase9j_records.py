#!/usr/bin/env python3
"""Generate benchmark records from the Phase 9J truth-audit summary."""

from __future__ import annotations

import argparse
import json
import shutil
from pathlib import Path
from typing import Any


MODE_TO_BENCHMARK_MODE = {
    "deterministic": "deterministic",
    "E2B": "e2b_baseline",
    "model_enabled": "e2b_baseline",
}


def load_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def slug(value: str) -> str:
    return (
        value.lower()
        .replace(" ", "_")
        .replace("/", "_")
        .replace("\\", "_")
        .replace(":", "_")
    )


def stage_status(value: Any, *, available_values: set[str] | None = None) -> str:
    if value is None:
        return "MISSING"
    normalized = str(value).upper()
    available_values = available_values or {"PRESENT", "AVAILABLE"}
    if normalized in {"VALID", "PASS"}:
        return "VALID"
    if normalized in {"INVALID", "FAIL", "FAILED"}:
        return "INVALID"
    if normalized in {"REVIEW", "INFERRED"}:
        return "REVIEW"
    if normalized in {"MISSING", "ABSENT", "NONE"}:
        return "MISSING"
    if normalized in available_values:
        return "REVIEW"
    return "REVIEW"


def graph_stage_status(value: Any) -> str:
    if str(value).upper() == "PRESENT":
        return "VALID"
    return stage_status(value)


def terminal_state(row: dict[str, Any]) -> str:
    gate = row.get("reportGate")
    if gate == "RELEASE_READY":
        return "PASS"
    if gate == "REVIEW_ONLY":
        return "REVIEW"
    if gate == "DIAGNOSTIC_ONLY":
        return "DIAGNOSTIC_ONLY"
    return "BLOCKED"


def overall_decision(row: dict[str, Any]) -> str:
    product = str(row.get("productDecision") or row.get("product") or "").upper()
    if product in {"PASS", "REVIEW", "BLOCKED"}:
        return product
    gate = row.get("reportGate")
    if gate == "RELEASE_READY":
        return "PASS"
    if gate == "REVIEW_ONLY":
        return "REVIEW"
    if gate == "DIAGNOSTIC_ONLY":
        return "DIAGNOSTIC"
    return "BLOCKED"


def artifact_items(row: dict[str, Any]) -> tuple[list[dict[str, Any]], list[dict[str, str]]]:
    definitions = [
        ("original_image", row.get("inputImagePath"), "USER_REPORT_SAFE"),
        ("runtime_evidence_package", row.get("runtimeEvidencePackagePath"), "DIAGNOSTIC_ONLY"),
        ("validator_json", row.get("validatorJsonPath"), "DIAGNOSTIC_ONLY"),
        ("validator_markdown", row.get("validatorMarkdownPath"), "DIAGNOSTIC_ONLY"),
        ("final_report_json", row.get("finalReportJsonPath"), "USER_REPORT_SAFE"),
        ("html_report", row.get("htmlReportPath"), "USER_REPORT_SAFE"),
        ("markdown_report", row.get("markdownReportPath"), "USER_REPORT_SAFE"),
        ("manifest", row.get("manifestPath"), "DIAGNOSTIC_ONLY"),
        ("graph_panel_overlay", row.get("graphPanelOverlayPath"), "DIAGNOSTIC_ONLY"),
        ("plot_area_overlay", row.get("plotAreaOverlayPath"), "DIAGNOSTIC_ONLY"),
        ("axis_tick_overlay", row.get("axisTickOverlayPath"), "DIAGNOSTIC_ONLY"),
        ("trace_overlay", row.get("traceOverlayPath"), "DIAGNOSTIC_ONLY"),
        ("peak_overlay", row.get("peakOverlayPath"), "DIAGNOSTIC_ONLY"),
    ]
    artifacts: list[dict[str, Any]] = []
    missing: list[dict[str, str]] = []
    for artifact_type, path, privacy_class in definitions:
        if path:
            artifacts.append(
                {
                    "artifactType": artifact_type,
                    "path": str(path).replace("\\", "/"),
                    "privacyClass": privacy_class,
                }
            )
        else:
            missing.append(
                {
                    "artifactType": artifact_type,
                    "reason": f"Phase 9J summary did not record {artifact_type}.",
                }
            )
    if row.get("graphFailurePackagePath"):
        artifacts.append(
            {
                "artifactType": "manifest",
                "path": str(row["graphFailurePackagePath"]).replace("\\", "/"),
                "privacyClass": "DIAGNOSTIC_ONLY",
            }
        )
    return artifacts, missing


def artifact_set(row: dict[str, Any]) -> dict[str, Any]:
    overlays = [
        row.get("graphPanelOverlayPath"),
        row.get("plotAreaOverlayPath"),
        row.get("axisTickOverlayPath"),
        row.get("traceOverlayPath"),
        row.get("peakOverlayPath"),
    ]
    return {
        key: value.replace("\\", "/")
        for key, value in {
            "runtimeEvidencePackage": row.get("runtimeEvidencePackagePath"),
            "validatorJson": row.get("validatorJsonPath"),
            "validatorMarkdown": row.get("validatorMarkdownPath"),
            "finalReportJson": row.get("finalReportJsonPath"),
            "htmlReport": row.get("htmlReportPath"),
            "markdownReport": row.get("markdownReportPath"),
        }.items()
        if value
    } | {
        "overlayPaths": [path.replace("\\", "/") for path in overlays if path],
    }


def build_prediction(row: dict[str, Any]) -> dict[str, Any]:
    failure_subreasons = []
    failure_subreasons.extend(row.get("allTickSubreasons") or [])
    failure_subreasons.extend(row.get("allScaleSubreasons") or [])
    if row.get("runtimeFailureClass"):
        failure_subreasons.append(str(row["runtimeFailureClass"]))
    if row.get("reason"):
        failure_subreasons.append(str(row["reason"]))

    graph_id = "graph_1" if (row.get("graphCount") or row.get("reportGraphCount") or 0) else "graph_failure_1"
    graph = {
        "graphId": graph_id,
        "layoutClass": row.get("layoutClass") or "UNKNOWN_REVIEW",
        "graphPanelStatus": graph_stage_status(row.get("graphPanelStatus")),
        "plotAreaStatus": graph_stage_status(row.get("plotAreaStatus")),
        "calibration": {
            "xStatus": stage_status(row.get("xCalibration")),
            "yStatus": stage_status(row.get("yCalibration")),
            "xAnchorCount": row.get("xAnchorCount") or 0,
            "yAnchorCount": row.get("yAnchorCount") or 0,
        },
        "trace": {
            "status": stage_status(row.get("traceStatus")),
            "pointCount": int(row.get("tracePointCount") or 0),
            "xCoverageRatio": float(row.get("traceCoverageRatio") or 0),
            "maxGapColumns": int(row.get("traceMaxGapColumns") or 0),
        },
        "peaks": {
            "status": stage_status(row.get("peakEvidenceStatus")),
            "peakCount": int(row.get("peakCount") or 0),
            "reportablePeakCount": 0 if row.get("reportGate") != "RELEASE_READY" else int(row.get("peakCount") or 0),
            "reviewPeakCount": int(row.get("peakCount") or 0) if row.get("reportGate") != "RELEASE_READY" else 0,
            "rejectedPeakCount": 0,
        },
        "failureSubreasons": sorted(set(reason for reason in failure_subreasons if reason)),
    }
    if row.get("selectedXCalibrationStrategy"):
        graph["calibration"]["selectedXStrategy"] = row["selectedXCalibrationStrategy"]
    if row.get("selectedYCalibrationStrategy"):
        graph["calibration"]["selectedYStrategy"] = row["selectedYCalibrationStrategy"]

    return {
        "schemaVersion": "chromalab.benchmark.prediction.v1",
        "caseId": benchmark_case_id(row),
        "runId": row["runId"],
        "mode": MODE_TO_BENCHMARK_MODE.get(row.get("mode"), "deterministic"),
        "pipelineVersion": "phase9j-truth-audit",
        "reportGate": row["reportGate"],
        "validatorVerdict": row.get("validatorVerdict") or "MISSING",
        "runtimeFailureClass": row.get("runtimeFailureClass"),
        "firstFailingStage": row.get("firstFailingStage") or "",
        "graphs": [graph],
        "artifacts": artifact_set(row),
        "timingsMs": {"total": float(row.get("totalDurationMs") or 0)},
    }


def metric_status_from_gate(row: dict[str, Any], stage: str) -> str:
    if stage == "graph_panel":
        expected = row.get("expectedGraphCount")
        detected = row.get("graphCount")
        if expected == detected and row.get("graphPanelStatus") == "PRESENT":
            return "PASS"
        if row.get("graphPanelStatus") == "PRESENT":
            return "REVIEW"
        return "FAIL"
    if stage == "calibration":
        x_status = stage_status(row.get("xCalibration"))
        y_status = stage_status(row.get("yCalibration"))
        if x_status == "VALID" and y_status == "VALID":
            return "PASS"
        if x_status in {"VALID", "REVIEW"} and y_status in {"VALID", "REVIEW"}:
            return "REVIEW"
        return "FAIL"
    if stage == "trace":
        return "PASS" if row.get("traceStatus") == "AVAILABLE" and row.get("reportGate") == "RELEASE_READY" else (
            "REVIEW" if row.get("traceStatus") == "AVAILABLE" else "FAIL"
        )
    if stage == "peaks":
        return "PASS" if row.get("peakEvidenceStatus") == "AVAILABLE" and row.get("reportGate") == "RELEASE_READY" else (
            "REVIEW" if (row.get("peakCount") or 0) > 0 else "FAIL"
        )
    if stage == "evidence_package":
        required = [
            row.get("runtimeEvidencePackageAvailable"),
            row.get("validatorJsonAvailable"),
            row.get("validatorMarkdownAvailable"),
            row.get("finalReportJsonAvailable"),
        ]
        return "PASS" if all(required) else "FAIL"
    return "REVIEW" if row.get("reportGate") == "REVIEW_ONLY" else ("PASS" if row.get("reportGate") == "RELEASE_READY" else "FAIL")


def build_metrics(row: dict[str, Any]) -> dict[str, Any]:
    expected = row.get("expectedGraphCount") or 0
    detected = row.get("graphCount") or 0
    gate = row.get("reportGate")
    failure_reasons = [reason for reason in [row.get("runtimeFailureClass"), row.get("firstFailingStage"), row.get("reason")] if reason]
    return {
        "schemaVersion": "chromalab.benchmark.metrics.v1",
        "caseId": benchmark_case_id(row),
        "runId": row["runId"],
        "overallDecision": overall_decision(row),
        "stageScores": [
            {
                "stage": "graph_panel",
                "status": metric_status_from_gate(row, "graph_panel"),
                "metrics": {
                    "expectedGraphCount": expected,
                    "detectedGraphCount": detected,
                    "graphCountMatches": expected == detected,
                    "graphPanelStatus": row.get("graphPanelStatus") or "MISSING",
                    "plotAreaStatus": row.get("plotAreaStatus") or "MISSING",
                },
                "failureReasons": [] if expected == detected else ["graph count differs from Phase 9J expected graph count"],
            },
            {
                "stage": "calibration",
                "status": metric_status_from_gate(row, "calibration"),
                "metrics": {
                    "xCalibration": row.get("xCalibration") or "MISSING",
                    "yCalibration": row.get("yCalibration") or "MISSING",
                    "xAnchorCount": row.get("xAnchorCount") or 0,
                    "yAnchorCount": row.get("yAnchorCount") or 0,
                },
                "failureReasons": failure_reasons if row.get("firstFailingStage") == "Y_CALIBRATION" else [],
            },
            {
                "stage": "trace",
                "status": metric_status_from_gate(row, "trace"),
                "metrics": {
                    "traceStatus": row.get("traceStatus") or "MISSING",
                    "releaseReady": gate == "RELEASE_READY",
                },
            },
            {
                "stage": "peaks",
                "status": metric_status_from_gate(row, "peaks"),
                "metrics": {
                    "peakEvidenceStatus": row.get("peakEvidenceStatus") or "MISSING",
                    "peakCount": row.get("peakCount") or 0,
                    "releaseReady": gate == "RELEASE_READY",
                },
                "failureReasons": [row["runtimeFailureClass"]] if row.get("runtimeFailureClass") else [],
            },
            {
                "stage": "report_claims",
                "status": "PASS" if gate == "RELEASE_READY" else ("REVIEW" if gate == "REVIEW_ONLY" else "FAIL"),
                "metrics": {
                    "reportGate": gate,
                    "validatorVerdict": row.get("validatorVerdict") or "MISSING",
                    "releaseGateCorrect": gate != "RELEASE_READY" or not row.get("runtimeFailureClass"),
                },
            },
            {
                "stage": "evidence_package",
                "status": metric_status_from_gate(row, "evidence_package"),
                "metrics": {
                    "runtimeEvidencePackageAvailable": bool(row.get("runtimeEvidencePackageAvailable")),
                    "validatorJsonAvailable": bool(row.get("validatorJsonAvailable")),
                    "validatorMarkdownAvailable": bool(row.get("validatorMarkdownAvailable")),
                    "finalReportJsonAvailable": bool(row.get("finalReportJsonAvailable")),
                },
            },
        ],
        "zeroToleranceFailures": zero_tolerance_failures(row),
        "notes": [str(row.get("reason"))] if row.get("reason") else [],
    }


def zero_tolerance_failures(row: dict[str, Any]) -> list[str]:
    failures: list[str] = []
    if row.get("reportGate") == "RELEASE_READY" and row.get("runtimeFailureClass"):
        failures.append("release_ready_with_runtime_failure_class")
    if not row.get("runtimeEvidencePackageAvailable"):
        failures.append("missing_runtime_evidence_package")
    if not row.get("validatorJsonAvailable"):
        failures.append("missing_validator_json")
    if not row.get("validatorMarkdownAvailable"):
        failures.append("missing_validator_markdown")
    if not row.get("finalReportJsonAvailable"):
        failures.append("missing_final_report_json")
    return failures


def claim_status_for_gate(row: dict[str, Any], claim_type: str) -> str:
    if row.get("reportGate") == "BLOCKED":
        return "MISSING_EVIDENCE"
    if row.get("reportGate") == "RELEASE_READY":
        return "SUPPORTED"
    if claim_type == "graph_count" and row.get("expectedGraphCount") == row.get("graphCount"):
        return "SUPPORTED"
    return "REVIEW"


def build_report_claims(row: dict[str, Any]) -> dict[str, Any]:
    claims = []
    for claim_type in ["graph_count", "calibration", "trace_present", "peak_count", "peak_area"]:
        claims.append(
            {
                "claimId": f"claim_{claim_type}",
                "claimType": claim_type,
                "status": claim_status_for_gate(row, claim_type),
                "evidenceRefs": [
                    f"phase9j_summary.{row['fixtureId']}.{row['mode']}.{claim_type}"
                ],
                "forbiddenSourceAccepted": False,
                "reason": row.get("reason") or "Phase 9J benchmark record.",
            }
        )
    return {
        "schemaVersion": "chromalab.benchmark.report_claims.v1",
        "caseId": benchmark_case_id(row),
        "runId": row["runId"],
        "reportGate": row["reportGate"],
        "claims": claims,
        "blockedClaimReasons": [row["runtimeFailureClass"]] if row.get("runtimeFailureClass") else [],
    }


def build_evidence_package(row: dict[str, Any]) -> dict[str, Any]:
    artifacts, missing = artifact_items(row)
    return {
        "schemaVersion": "chromalab.benchmark.evidence_package.v1",
        "caseId": benchmark_case_id(row),
        "runId": row["runId"],
        "terminalState": terminal_state(row),
        "runtimeFailureClass": row.get("runtimeFailureClass"),
        "artifacts": artifacts,
        "missingArtifactReasons": missing,
    }


def build_summary(rows: list[dict[str, Any]]) -> dict[str, Any]:
    decisions: dict[str, int] = {}
    report_gates: dict[str, int] = {}
    validator_verdicts: dict[str, int] = {}
    failure_classes: dict[str, int] = {}
    records = []
    for row in rows:
        decision = overall_decision(row)
        report_gate = row.get("reportGate") or "MISSING"
        validator_verdict = row.get("validatorVerdict") or "MISSING"
        failure_class = row.get("runtimeFailureClass") or "NONE"
        decisions[decision] = decisions.get(decision, 0) + 1
        report_gates[report_gate] = report_gates.get(report_gate, 0) + 1
        validator_verdicts[validator_verdict] = validator_verdicts.get(validator_verdict, 0) + 1
        failure_classes[failure_class] = failure_classes.get(failure_class, 0) + 1
        records.append(
            {
                "caseId": benchmark_case_id(row),
                "fixtureId": row.get("fixtureId"),
                "mode": row.get("mode"),
                "runId": row.get("runId"),
                "expectedGraphCount": row.get("expectedGraphCount"),
                "detectedGraphCount": row.get("graphCount"),
                "reportGate": report_gate,
                "validatorVerdict": validator_verdict,
                "runtimeFailureClass": row.get("runtimeFailureClass"),
                "firstFailingStage": row.get("firstFailingStage") or "",
                "peakCount": row.get("peakCount") or 0,
                "decision": decision,
                "reason": row.get("reason") or "",
            }
        )
    return {
        "schemaVersion": "chromalab.benchmark.phase9j_summary.v1",
        "source": "artifacts/phase9j-truth-audit/phase9j_summary.json",
        "recordCount": len(rows),
        "decisionCounts": decisions,
        "reportGateCounts": report_gates,
        "validatorVerdictCounts": validator_verdicts,
        "failureClassCounts": failure_classes,
        "records": records,
    }


def write_summary_markdown(path: Path, summary: dict[str, Any]) -> None:
    lines = [
        "# Phase 9J Benchmark Records Summary",
        "",
        f"Source: `{summary['source']}`",
        "",
        f"Record count: `{summary['recordCount']}`",
        "",
        "## Counts",
        "",
        "| Category | Counts |",
        "| --- | --- |",
        f"| Product decisions | `{summary['decisionCounts']}` |",
        f"| Report gates | `{summary['reportGateCounts']}` |",
        f"| Validator verdicts | `{summary['validatorVerdictCounts']}` |",
        f"| Failure classes | `{summary['failureClassCounts']}` |",
        "",
        "## Records",
        "",
        "| Case | Expected graphs | Detected graphs | Gate | Validator | Failure | Stage | Peaks | Decision | Reason |",
        "| --- | ---: | ---: | --- | --- | --- | --- | ---: | --- | --- |",
    ]
    for record in summary["records"]:
        lines.append(
            "| `{caseId}` | {expectedGraphCount} | {detectedGraphCount} | {reportGate} | "
            "{validatorVerdict} | {runtimeFailureClass} | {firstFailingStage} | {peakCount} | "
            "{decision} | {reason} |".format(
                caseId=record["caseId"],
                expectedGraphCount=record["expectedGraphCount"],
                detectedGraphCount=record["detectedGraphCount"],
                reportGate=record["reportGate"],
                validatorVerdict=record["validatorVerdict"],
                runtimeFailureClass=record["runtimeFailureClass"] or "",
                firstFailingStage=record["firstFailingStage"],
                peakCount=record["peakCount"],
                decision=record["decision"],
                reason=str(record["reason"]).replace("|", "/"),
            )
        )
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def benchmark_case_id(row: dict[str, Any]) -> str:
    return f"phase9j_{row['fixtureId']}_{slug(row['mode'])}"


def output_dir_for_row(output_root: Path, row: dict[str, Any]) -> Path:
    return output_root / benchmark_case_id(row)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--summary",
        type=Path,
        default=Path("artifacts/phase9j-truth-audit/phase9j_summary.json"),
        help="Phase 9J summary JSON path.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("benchmark/examples/phase9j_truth_audit"),
        help="Output directory for benchmark records.",
    )
    parser.add_argument(
        "--clean",
        action="store_true",
        help="Delete output directory before generating records.",
    )
    args = parser.parse_args()

    if not args.summary.exists():
        raise SystemExit(f"Phase 9J summary not found: {args.summary}")

    rows = load_json(args.summary)
    if not isinstance(rows, list):
        raise SystemExit("Phase 9J summary must be a JSON array.")

    if args.clean and args.output.exists():
        shutil.rmtree(args.output)

    for row in rows:
        row_output = output_dir_for_row(args.output, row)
        write_json(row_output / "prediction.json", build_prediction(row))
        write_json(row_output / "metrics.json", build_metrics(row))
        write_json(row_output / "evidence-package.json", build_evidence_package(row))
        write_json(row_output / "report-claims.json", build_report_claims(row))

    summary = build_summary(rows)
    write_json(args.output / "summary.json", summary)
    write_summary_markdown(args.output / "summary.md", summary)

    print(f"Generated {len(rows)} Phase 9J benchmark records in {args.output}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
