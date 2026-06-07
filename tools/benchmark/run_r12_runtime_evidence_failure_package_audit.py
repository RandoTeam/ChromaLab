#!/usr/bin/env python3
"""Build R12 runtime evidence and failure package closure report.

This audit consumes the tracked Phase 9J benchmark records and checks whether
each Android fixture run has the core evidence/export artifacts needed for
product-level inspection. It does not change Android runtime, validators,
report gates, chromatographic math, model policy, or CalculationEngine.
"""

from __future__ import annotations

import argparse
import json
import shutil
from pathlib import Path
from typing import Any


PHASE9J_ROOT = Path("benchmark/examples/phase9j_truth_audit")
REPORT_ROOT = Path("benchmark/reports/r12_runtime_evidence_failure_package_closure")

CORE_ARTIFACT_TYPES = [
    "runtime_evidence_package",
    "validator_json",
    "validator_markdown",
    "final_report_json",
    "html_report",
    "markdown_report",
    "manifest",
]

OPTIONAL_OVERLAY_TYPES = [
    "graph_panel_overlay",
    "plot_area_overlay",
    "axis_tick_overlay",
    "trace_overlay",
    "peak_overlay",
]

BLOCKED_GATES = {"BLOCKED"}


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


def path_string(path: Path) -> str:
    return str(path).replace("\\", "/")


def load_case(case_dir: Path) -> dict[str, Any]:
    prediction = read_json(case_dir / "prediction.json")
    metrics = read_json(case_dir / "metrics.json")
    evidence = read_json(case_dir / "evidence-package.json")
    report_claims = read_json(case_dir / "report-claims.json")
    return {
        "caseDir": case_dir,
        "prediction": prediction,
        "metrics": metrics,
        "evidence": evidence,
        "reportClaims": report_claims,
    }


def stage_metrics(metrics: dict[str, Any], stage: str) -> dict[str, Any]:
    for row in metrics.get("stageScores", []):
        if row.get("stage") == stage:
            return row.get("metrics", {})
    return {}


def fixture_and_mode(case_id: str) -> tuple[str, str]:
    prefix = "phase9j_"
    value = case_id[len(prefix):] if case_id.startswith(prefix) else case_id
    if value.endswith("_model_enabled"):
        return value.removesuffix("_model_enabled"), "E2B"
    if value.endswith("_deterministic"):
        return value.removesuffix("_deterministic"), "deterministic"
    return value, "unknown"


def artifact_paths(evidence: dict[str, Any]) -> dict[str, list[str]]:
    grouped: dict[str, list[str]] = {}
    for artifact in evidence.get("artifacts", []):
        artifact_type = artifact.get("artifactType", "")
        path = artifact.get("path", "")
        if path:
            grouped.setdefault(artifact_type, []).append(path)
        if "graph_failure_package" in path:
            grouped.setdefault("graph_failure_package", []).append(path)
    return grouped


def local_path_visible(path: str) -> bool:
    return Path(path).exists()


def audit_case(case: dict[str, Any]) -> dict[str, Any]:
    prediction = case["prediction"]
    metrics = case["metrics"]
    evidence = case["evidence"]
    report_claims = case["reportClaims"]
    fixture_id, mode = fixture_and_mode(prediction["caseId"])
    graph_metrics = stage_metrics(metrics, "graph_panel")
    calibration_metrics = stage_metrics(metrics, "calibration")
    trace_metrics = stage_metrics(metrics, "trace")
    peak_metrics = stage_metrics(metrics, "peaks")
    paths = artifact_paths(evidence)

    core_status = {
        artifact_type: {
            "available": bool(paths.get(artifact_type)),
            "paths": paths.get(artifact_type, []),
            "localVisible": all(local_path_visible(path) for path in paths.get(artifact_type, []))
            if paths.get(artifact_type)
            else False,
        }
        for artifact_type in CORE_ARTIFACT_TYPES
    }
    missing_core = [artifact_type for artifact_type, status in core_status.items() if not status["available"]]
    local_core_missing = [
        artifact_type
        for artifact_type, status in core_status.items()
        if status["available"] and not status["localVisible"]
    ]
    missing_reasons = evidence.get("missingArtifactReasons", [])
    missing_overlay_types = sorted(
        {
            reason.get("artifactType")
            for reason in missing_reasons
            if reason.get("artifactType") in OPTIONAL_OVERLAY_TYPES
        }
    )
    graph_failure_paths = paths.get("graph_failure_package", [])
    report_gate = prediction.get("reportGate")
    runtime_failure_class = prediction.get("runtimeFailureClass")
    first_failing_stage = prediction.get("firstFailingStage") or ""
    blocked = report_gate in BLOCKED_GATES
    blocked_with_failure_package = bool(graph_failure_paths)
    blocked_with_stage = bool(first_failing_stage)

    closure_decision = "EVIDENCE_COMPLETE"
    closure_reasons: list[str] = []
    if missing_core:
        closure_decision = "EVIDENCE_BLOCKED"
        closure_reasons.append("missing core artifacts: " + ", ".join(missing_core))
    if local_core_missing:
        closure_decision = "EVIDENCE_REVIEW" if closure_decision == "EVIDENCE_COMPLETE" else closure_decision
        closure_reasons.append("core artifact paths not visible locally: " + ", ".join(local_core_missing))
    if blocked and not blocked_with_failure_package:
        closure_decision = "EVIDENCE_BLOCKED"
        closure_reasons.append("blocked run lacks graph failure package")
    if blocked and not blocked_with_stage:
        closure_decision = "EVIDENCE_BLOCKED"
        closure_reasons.append("blocked run lacks first failing stage")
    if blocked and not runtime_failure_class:
        closure_decision = "EVIDENCE_BLOCKED"
        closure_reasons.append("blocked run lacks runtimeFailureClass")
    if missing_overlay_types and closure_decision == "EVIDENCE_COMPLETE":
        closure_decision = "EVIDENCE_REVIEW"
        closure_reasons.append("optional overlays missing with explicit reasons")
    if not closure_reasons:
        closure_reasons.append("core evidence/export artifacts are present")

    return {
        "caseId": prediction["caseId"],
        "runId": prediction["runId"],
        "fixtureId": fixture_id,
        "mode": mode,
        "reportGate": report_gate,
        "validatorVerdict": prediction.get("validatorVerdict"),
        "runtimeFailureClass": runtime_failure_class,
        "firstFailingStage": first_failing_stage,
        "expectedGraphCount": graph_metrics.get("expectedGraphCount"),
        "detectedGraphCount": graph_metrics.get("detectedGraphCount"),
        "xCalibration": calibration_metrics.get("xCalibration"),
        "yCalibration": calibration_metrics.get("yCalibration"),
        "traceStatus": trace_metrics.get("traceStatus"),
        "peakEvidenceStatus": peak_metrics.get("peakEvidenceStatus"),
        "peakCount": peak_metrics.get("peakCount"),
        "coreArtifacts": core_status,
        "graphFailurePackageAvailable": blocked_with_failure_package,
        "graphFailurePackagePaths": graph_failure_paths,
        "missingOverlayTypes": missing_overlay_types,
        "reportClaimsBlockedReasons": report_claims.get("blockedClaimReasons", []),
        "closureDecision": closure_decision,
        "closureReasons": closure_reasons,
    }


def build_summary(records: list[dict[str, Any]]) -> dict[str, Any]:
    no_export_records = [
        record for record in records
        if any(not status["available"] for status in record["coreArtifacts"].values())
    ]
    blocked_records = [record for record in records if record["reportGate"] == "BLOCKED"]
    blocked_without_failure_package = [
        record for record in blocked_records
        if not record["graphFailurePackageAvailable"]
    ]
    blocked_without_stage = [
        record for record in blocked_records
        if not record["firstFailingStage"]
    ]
    review_records = [record for record in records if record["reportGate"] == "REVIEW_ONLY"]
    release_ready_records = [record for record in records if record["reportGate"] == "RELEASE_READY"]
    local_core_missing = [
        record for record in records
        if any(status["available"] and not status["localVisible"] for status in record["coreArtifacts"].values())
    ]
    decision_counts: dict[str, int] = {}
    for record in records:
        decision_counts[record["closureDecision"]] = decision_counts.get(record["closureDecision"], 0) + 1

    if no_export_records or blocked_without_failure_package or blocked_without_stage:
        verdict = "R12_RUNTIME_EVIDENCE_FAILURE_PACKAGE_BLOCKED"
    elif local_core_missing:
        verdict = "R12_RUNTIME_EVIDENCE_FAILURE_PACKAGE_REVIEW"
    else:
        verdict = "R12_RUNTIME_EVIDENCE_FAILURE_PACKAGE_CLOSED"

    return {
        "schemaVersion": "chromalab.benchmark.r12_runtime_evidence_failure_package_closure_summary.v1",
        "productionImpact": "VALIDATOR_AND_AUDIT_CONTRACT_ONLY",
        "overallVerdict": verdict,
        "caseCount": len(records),
        "fixtureCount": len({record["fixtureId"] for record in records}),
        "coreArtifactCompleteCount": len(records) - len(no_export_records),
        "noExportStateCount": len(no_export_records),
        "blockedRunCount": len(blocked_records),
        "blockedWithGraphFailurePackageCount": len(blocked_records) - len(blocked_without_failure_package),
        "blockedMissingFailurePackageCount": len(blocked_without_failure_package),
        "blockedMissingFirstFailingStageCount": len(blocked_without_stage),
        "reviewOnlyCount": len(review_records),
        "releaseReadyCount": len(release_ready_records),
        "localCoreArtifactMissingCount": len(local_core_missing),
        "closureDecisionCounts": decision_counts,
        "nextRequiredWork": [
            "Produce Android runtime OCR anchor rows equivalent to R10/R11 benchmark rows.",
            "Persist or explicitly explain all graph-level crop and overlay artifacts for blocked runs.",
            "Keep BLOCKED runs blocked until graph/calibration/trace/peak evidence is complete.",
            "Keep E2B advisory-only and unable to alter graph count, calibration, trace, peaks, metrics, or report gates.",
        ],
        "records": records,
    }


def render_markdown(summary: dict[str, Any]) -> str:
    lines = [
        "# R12 Runtime Evidence And Failure Package Closure",
        "",
        f"Verdict: `{summary['overallVerdict']}`",
        "",
        f"Production impact: `{summary['productionImpact']}`",
        "",
        f"Cases: `{summary['caseCount']}`",
        f"Fixtures: `{summary['fixtureCount']}`",
        f"Core artifact complete: `{summary['coreArtifactCompleteCount']}/{summary['caseCount']}`",
        f"No-export states: `{summary['noExportStateCount']}`",
        f"Blocked runs: `{summary['blockedRunCount']}`",
        f"Blocked with graph failure package: `{summary['blockedWithGraphFailurePackageCount']}/{summary['blockedRunCount']}`",
        f"Blocked missing first failing stage: `{summary['blockedMissingFirstFailingStageCount']}`",
        f"Review-only runs: `{summary['reviewOnlyCount']}`",
        f"Release-ready runs: `{summary['releaseReadyCount']}`",
        "",
        "R12 audits evidence/export closure only. It does not change Android runtime behavior, chromatographic math, model policy, report gates, or CalculationEngine.",
        "",
        "## Fixture Results",
        "",
        "| Fixture | Mode | Gate | Validator | Graphs | Calibration | Trace | Peaks | Failure | Stage | Core artifacts | Failure package | Decision | Reason |",
        "|---|---|---|---|---:|---|---|---:|---|---|---|---|---|---|",
    ]
    for record in summary["records"]:
        reason = "<br>".join(record["closureReasons"])
        core = "yes" if all(status["available"] for status in record["coreArtifacts"].values()) else "no"
        failure_package = "yes" if record["graphFailurePackageAvailable"] else "-"
        lines.append(
            "| `{fixture}` | {mode} | `{gate}` | `{validator}` | {graphs} | X:{xcal} / Y:{ycal} | {trace} | {peaks} | `{failure}` | `{stage}` | {core} | {failure_package} | `{decision}` | {reason} |".format(
                fixture=record["fixtureId"],
                mode=record["mode"],
                gate=record["reportGate"],
                validator=record["validatorVerdict"],
                graphs=record["detectedGraphCount"],
                xcal=record["xCalibration"],
                ycal=record["yCalibration"],
                trace=record["traceStatus"],
                peaks=record["peakCount"],
                failure=record["runtimeFailureClass"],
                stage=record["firstFailingStage"] or "-",
                core=core,
                failure_package=failure_package,
                decision=record["closureDecision"],
                reason=reason,
            )
        )
    lines.extend([
        "",
        "## Next Required Work",
        "",
    ])
    lines.extend(f"- {item}" for item in summary["nextRequiredWork"])
    lines.append("")
    return "\n".join(lines)


def run(clean: bool) -> None:
    if not PHASE9J_ROOT.exists():
        raise FileNotFoundError(f"Phase 9J benchmark root is required before R12: {PHASE9J_ROOT}")
    if clean and REPORT_ROOT.exists():
        shutil.rmtree(REPORT_ROOT)
    cases = [load_case(path) for path in sorted(PHASE9J_ROOT.iterdir()) if path.is_dir()]
    records = [audit_case(case) for case in cases]
    summary = build_summary(records)
    write_json(REPORT_ROOT / "summary.json", summary)
    write_text(REPORT_ROOT / "summary.md", render_markdown(summary))
    print(
        "R12 runtime evidence failure package closure wrote "
        f"{len(records)} audited records to {path_string(REPORT_ROOT)}."
    )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--clean", action="store_true", help="Remove previous R12 report output first.")
    args = parser.parse_args()
    run(clean=args.clean)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
