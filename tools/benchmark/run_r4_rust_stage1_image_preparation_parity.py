#!/usr/bin/env python3
"""Build R4 Rust Stage 1 image-preparation parity records.

This runner compares the Rust Stage 1 bridge against the R3 PC candidate.
It is shadow-only and does not change Android runtime, validators, report gates,
chromatographic math, graph-count metadata, model policy, or CalculationEngine.
"""

from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import time
from pathlib import Path
from typing import Any


ANDROID_METADATA_DIR = Path("composeApp/src/androidMain/assets/validation")
R3_SUMMARY = Path("benchmark/reports/r3_image_preparation_candidate/summary.json")
EXAMPLE_ROOT = Path("benchmark/examples/r4_rust_stage1_image_preparation_parity")
REPORT_ROOT = Path("benchmark/reports/r4_rust_stage1_image_preparation_parity")
RUST_REPORT_ROOT = REPORT_ROOT / "rust_reports"
RUST_ROOT = Path("rust")
RUST_BIN = RUST_ROOT / "target/debug/chromalab_cv_stage1_prep.exe"


EXPECTED_LAYOUTS = {
    "white_tiger_ion71": "SINGLE_TRACE_SINGLE_AXIS",
    "bench_01_mz71_screenshot_page": "MULTI_PANEL_SEPARATE_AXES",
    "bench_02_mz92_belyi_tigr": "SINGLE_TRACE_SINGLE_AXIS",
    "bench_03_small_tic_export": "LOW_RES_EXPORT_GRAPH",
    "bench_04_stacked_xic_resolution": "MULTI_PANEL_SEPARATE_AXES",
    "bench_05_tic_plus_ions": "TIC_PLUS_ION_PANELS",
    "bench_06_photo_two_graphs_page": "TWO_GRAPH_PAGE",
    "bench_07_rotated_page_photo": "ROTATED_PAGE_GRAPH",
}


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


def load_metadata() -> list[dict[str, Any]]:
    records = []
    for path in sorted(ANDROID_METADATA_DIR.glob("*.metadata.json")):
        data = read_json(path)
        data["_metadataPath"] = str(path).replace("\\", "/")
        records.append(data)
    return records


def load_r3_by_fixture() -> dict[str, dict[str, Any]]:
    summary = read_json(R3_SUMMARY)
    return {item["fixtureId"]: item for item in summary["records"]}


def local_cargo_env() -> tuple[list[str], dict[str, str]]:
    env = os.environ.copy()
    toolchain_root = Path("artifacts/rust-toolchain")
    local_cargo = toolchain_root / "cargo/bin/cargo.exe"
    if local_cargo.exists():
        env["RUSTUP_HOME"] = str((toolchain_root / "rustup").resolve())
        env["CARGO_HOME"] = str((toolchain_root / "cargo").resolve())
        env["PATH"] = f"{(toolchain_root / 'cargo/bin').resolve()}{os.pathsep}{env.get('PATH', '')}"
        return [str(local_cargo.resolve())], env
    return ["cargo"], env


def build_rust_binary() -> None:
    cargo, env = local_cargo_env()
    subprocess.run(
        [*cargo, "build", "--bin", "chromalab_cv_stage1_prep"],
        cwd=RUST_ROOT,
        env=env,
        check=True,
    )


def run_rust_report(image_path: Path, fixture_id: str) -> dict[str, Any]:
    if not RUST_BIN.exists():
        raise FileNotFoundError(f"Rust Stage 1 binary not found: {RUST_BIN}")
    started = time.perf_counter()
    completed = subprocess.run(
        [str(RUST_BIN.resolve()), str(image_path)],
        check=True,
        capture_output=True,
        text=True,
    )
    report = json.loads(completed.stdout)
    report["runnerElapsedMs"] = round((time.perf_counter() - started) * 1000.0, 3)
    out = RUST_REPORT_ROOT / f"{fixture_id}_rust_stage1_report.json"
    write_json(out, report)
    report["_reportPath"] = str(out).replace("\\", "/")
    return report


def resolve_image_path(metadata: dict[str, Any]) -> Path:
    image_path = Path(str(metadata["assetImagePath"]))
    if image_path.exists():
        return image_path
    return Path("composeApp/src/androidMain/assets") / image_path


def float_delta(left: float | int, right: float | int) -> float:
    return round(abs(float(left) - float(right)), 6)


def near(left: float | int, right: float | int, tolerance: float) -> bool:
    return float_delta(left, right) <= tolerance


def parity_issues(rust_report: dict[str, Any], r3_detail: dict[str, Any]) -> list[str]:
    issues: list[str] = []
    rust_metrics = rust_report["sourceMetrics"]
    r3_metrics = r3_detail["sourceMetrics"]
    if rust_report["sourceSha256"] != r3_detail["sourceSha256"]:
        issues.append("SOURCE_SHA_MISMATCH")
    if rust_report["normalizedSha256"] != r3_detail["normalizedSha256"]:
        issues.append("NORMALIZED_SHA_MISMATCH")
    if rust_metrics["width"] != r3_metrics["width"] or rust_metrics["height"] != r3_metrics["height"]:
        issues.append("DIMENSION_MISMATCH")
    if rust_report["selectedVariantId"] != r3_detail["selectedVariantId"]:
        issues.append("SELECTED_VARIANT_MISMATCH")
    if rust_report["status"] != r3_detail["status"]:
        issues.append("STATUS_MISMATCH")
    if sorted(rust_report["warnings"]) != sorted(r3_detail["warnings"]):
        issues.append("WARNING_MISMATCH")
    if not near(rust_metrics["contrastP90P10"], r3_metrics["contrastP90P10"], 0.5):
        issues.append("CONTRAST_METRIC_DELTA")
    if not near(rust_metrics["edgeDensity"], r3_metrics["edgeDensity"], 0.002):
        issues.append("EDGE_DENSITY_DELTA")
    return issues


def build_record(
    metadata: dict[str, Any],
    rust_report: dict[str, Any],
    r3_detail: dict[str, Any],
    issues: list[str],
) -> tuple[dict[str, Any], dict[str, Any]]:
    fixture_id = metadata["fixtureId"]
    expected_count = int(metadata.get("expectedGraphCount") or 0)
    status = "PASS" if not issues else "REVIEW"
    selected_delta = float_delta(rust_report["selectedVariantScore"], r3_detail["selectedVariantScore"])
    record_id = f"stage123_{fixture_id}_rust_stage1_image_preparation_parity_v1"
    rust_metrics = rust_report["sourceMetrics"]
    r3_metrics = r3_detail["sourceMetrics"]
    artifact_paths = [
        rust_report["_reportPath"],
        r3_detail["recordPath"],
        r3_detail["sourcePreview"],
        r3_detail["selectedPreview"],
    ]
    record = {
        "schemaVersion": "chromalab.benchmark.stage123_parity_record.v1",
        "recordId": record_id,
        "fixtureId": fixture_id,
        "mode": "pc_prototype",
        "sourceId": "r4_rust_stage1_image_preparation_parity_v1",
        "sourceKind": "RUST_STAGE1_IMAGE_PREP_PARITY_BRIDGE",
        "productionImpact": "NONE_SHADOW_ONLY",
        "runtimeReadiness": "RUST_STAGE1_PARITY_BRIDGE_NOT_RUNTIME_READY",
        "expectedGraphCount": expected_count,
        "detectedGraphCount": 0,
        "graphCountScore": "NOT_SCOREABLE",
        "expectedLayoutClass": EXPECTED_LAYOUTS.get(fixture_id, "UNKNOWN_REVIEW"),
        "predictedLayoutClass": "UNKNOWN_REVIEW",
        "layoutClassScore": "NOT_SCOREABLE",
        "imagePreparation": {
            "status": status,
            "available": True,
            "summary": "R4 Rust Stage 1 bridge decoded the image, emitted preprocessing metrics, and compared against the R3 PC candidate.",
            "metrics": {
                "rustWidth": rust_metrics["width"],
                "rustHeight": rust_metrics["height"],
                "r3Width": r3_metrics["width"],
                "r3Height": r3_metrics["height"],
                "sourceShaParity": rust_report["sourceSha256"] == r3_detail["sourceSha256"],
                "normalizedShaParity": rust_report["normalizedSha256"] == r3_detail["normalizedSha256"],
                "selectedVariantParity": rust_report["selectedVariantId"] == r3_detail["selectedVariantId"],
                "statusParity": rust_report["status"] == r3_detail["status"],
                "warningParity": sorted(rust_report["warnings"]) == sorted(r3_detail["warnings"]),
                "rustSelectedVariantScore": rust_report["selectedVariantScore"],
                "r3SelectedVariantScore": r3_detail["selectedVariantScore"],
                "selectedVariantScoreDelta": selected_delta,
                "rustElapsedMs": rust_report["elapsedMs"],
                "runnerElapsedMs": rust_report["runnerElapsedMs"],
            },
        },
        "graphDiscovery": {
            "status": "NOT_SCORED",
            "available": False,
            "summary": "R4 is Stage 1 Rust parity only; graph discovery remains the next replacement layer.",
        },
        "plotAreaLayout": {
            "status": "NOT_SCORED",
            "available": False,
            "summary": "R4 is Stage 1 Rust parity only; plotArea/layout remains the next replacement layer.",
        },
        "failureClass": "",
        "firstFailingStage": "",
        "evidence": {
            "sourceImage": rust_report["sourceImage"],
            "recordSource": str(REPORT_ROOT / "summary.json").replace("\\", "/"),
            "artifactPaths": artifact_paths,
        },
        "promotionDecision": "STAGE1_CANDIDATE_REQUIRES_STAGE2_PARITY",
        "notes": [
            "Shadow-only Rust Stage 1 parity bridge.",
            "No graphPanel, plotArea, calibration, trace, peak, or report-gate authority.",
            *issues,
        ],
    }
    detail = {
        "fixtureId": fixture_id,
        "status": status,
        "issues": issues,
        "expectedGraphCount": expected_count,
        "expectedLayoutClass": record["expectedLayoutClass"],
        "rustReportPath": rust_report["_reportPath"],
        "recordPath": str(EXAMPLE_ROOT / record_id / "stage123-parity-record.json").replace("\\", "/"),
        "sourceImage": rust_report["sourceImage"],
        "sourceShaParity": record["imagePreparation"]["metrics"]["sourceShaParity"],
        "normalizedShaParity": record["imagePreparation"]["metrics"]["normalizedShaParity"],
        "selectedVariantParity": record["imagePreparation"]["metrics"]["selectedVariantParity"],
        "statusParity": record["imagePreparation"]["metrics"]["statusParity"],
        "warningParity": record["imagePreparation"]["metrics"]["warningParity"],
        "rustSelectedVariantId": rust_report["selectedVariantId"],
        "r3SelectedVariantId": r3_detail["selectedVariantId"],
        "rustSelectedVariantScore": rust_report["selectedVariantScore"],
        "r3SelectedVariantScore": r3_detail["selectedVariantScore"],
        "selectedVariantScoreDelta": selected_delta,
        "rustStatus": rust_report["status"],
        "r3Status": r3_detail["status"],
        "rustWarnings": rust_report["warnings"],
        "r3Warnings": r3_detail["warnings"],
        "rustElapsedMs": rust_report["elapsedMs"],
        "runnerElapsedMs": rust_report["runnerElapsedMs"],
        "r3SourcePreview": r3_detail["sourcePreview"],
        "r3SelectedPreview": r3_detail["selectedPreview"],
    }
    return record, detail


def write_examples(records: list[dict[str, Any]]) -> None:
    EXAMPLE_ROOT.mkdir(parents=True, exist_ok=True)
    for record in records:
        write_json(EXAMPLE_ROOT / record["recordId"] / "stage123-parity-record.json", record)


def build_summary(details: list[dict[str, Any]]) -> dict[str, Any]:
    status_counts: dict[str, int] = {}
    issue_counts: dict[str, int] = {}
    for detail in details:
        status_counts[detail["status"]] = status_counts.get(detail["status"], 0) + 1
        for issue in detail["issues"]:
            issue_counts[issue] = issue_counts.get(issue, 0) + 1
    blocking_issues = [
        detail for detail in details if "DIMENSION_MISMATCH" in detail["issues"] or "SOURCE_SHA_MISMATCH" in detail["issues"]
    ]
    exact_selected = sum(1 for detail in details if detail["selectedVariantParity"])
    exact_status = sum(1 for detail in details if detail["statusParity"])
    if blocking_issues:
        verdict = "R4_RUST_STAGE1_PARITY_BLOCKED_BY_INPUT_IDENTITY_MISMATCH"
    elif issue_counts:
        verdict = "R4_RUST_STAGE1_PARITY_REVIEW_READY_WITH_DECODER_LIMITS"
    else:
        verdict = "R4_RUST_STAGE1_PARITY_READY_FOR_STAGE2_SHADOW"
    return {
        "schemaVersion": "chromalab.benchmark.r4_rust_stage1_image_preparation_parity_summary.v1",
        "productionImpact": "NONE_SHADOW_ONLY",
        "overallVerdict": verdict,
        "recordCount": len(details),
        "fixtureCount": len({detail["fixtureId"] for detail in details}),
        "statusCounts": status_counts,
        "issueCounts": issue_counts,
        "selectedVariantParityCount": exact_selected,
        "statusParityCount": exact_status,
        "nextRequiredWork": [
            "Keep Rust Stage 1 shadow-only until Stage 2 graph discovery consumes the selected variant.",
            "Treat Rust/Pillow JPEG normalized-byte hash differences as decoder evidence, not runtime failure.",
            "Add Stage 2 graph discovery candidate only after this Rust Stage 1 bridge remains stable.",
            "Keep Android runtime unchanged until Stage 1-3 promotion gates pass.",
        ],
        "records": details,
    }


def write_markdown(summary: dict[str, Any]) -> None:
    lines = [
        "# R4 Rust Stage 1 Image Preparation Parity",
        "",
        f"Verdict: `{summary['overallVerdict']}`",
        "",
        "Production impact: `NONE_SHADOW_ONLY`",
        "",
        f"Records: `{summary['recordCount']}`",
        f"Fixtures: `{summary['fixtureCount']}`",
        "",
        "R4 compares the Rust Stage 1 bridge against the R3 PC image-preparation candidate.",
        "It does not change Android runtime behavior, validators, report gates, graph-count expectations, chromatographic math, model policy, or CalculationEngine.",
        "",
        "R3 visual reference contact sheet: `benchmark/reports/r3_image_preparation_candidate/contact_sheet.png`",
        "",
        "## Fixture Parity",
        "",
        "| Fixture | Status | Rust variant | R3 variant | Variant parity | Status parity | SHA parity | Score delta | Issues |",
        "|---|---|---|---|---|---|---|---:|---|",
    ]
    for detail in summary["records"]:
        issues = ", ".join(detail["issues"]) if detail["issues"] else "NONE"
        sha = "source+normalized" if detail["sourceShaParity"] and detail["normalizedShaParity"] else (
            "source-only" if detail["sourceShaParity"] else "mismatch"
        )
        lines.append(
            "| `{fixtureId}` | {status} | `{rustVariant}` | `{r3Variant}` | {variantParity} | {statusParity} | {sha} | {delta} | {issues} |".format(
                fixtureId=detail["fixtureId"],
                status=detail["status"],
                rustVariant=detail["rustSelectedVariantId"],
                r3Variant=detail["r3SelectedVariantId"],
                variantParity="PASS" if detail["selectedVariantParity"] else "REVIEW",
                statusParity="PASS" if detail["statusParity"] else "REVIEW",
                sha=sha,
                delta=detail["selectedVariantScoreDelta"],
                issues=issues,
            )
        )
    lines.extend(["", "## Next Required Work", ""])
    for item in summary["nextRequiredWork"]:
        lines.append(f"- {item}")
    lines.append("")
    write_text(REPORT_ROOT / "summary.md", "\n".join(lines))


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--clean", action="store_true", help="Remove previous R4 examples/report before writing.")
    args = parser.parse_args(argv)

    if args.clean:
        for path in [EXAMPLE_ROOT, REPORT_ROOT]:
            if path.exists():
                shutil.rmtree(path)

    if not R3_SUMMARY.exists():
        raise FileNotFoundError(f"R3 summary is required before R4 parity: {R3_SUMMARY}")

    build_rust_binary()
    metadata = load_metadata()
    r3_by_fixture = load_r3_by_fixture()
    records = []
    details = []
    for item in metadata:
        fixture_id = item["fixtureId"]
        image_path = resolve_image_path(item)
        rust_report = run_rust_report(image_path, fixture_id)
        r3_detail = r3_by_fixture[fixture_id]
        issues = parity_issues(rust_report, r3_detail)
        record, detail = build_record(item, rust_report, r3_detail, issues)
        records.append(record)
        details.append(detail)

    write_examples(records)
    summary = build_summary(details)
    write_json(REPORT_ROOT / "summary.json", summary)
    write_markdown(summary)
    print(
        "R4 Rust Stage 1 parity wrote "
        f"{len(records)} records to {EXAMPLE_ROOT.as_posix()} and report to {REPORT_ROOT.as_posix()}."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
