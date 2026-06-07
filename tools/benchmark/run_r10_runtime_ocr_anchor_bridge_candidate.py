#!/usr/bin/env python3
"""Build R10 runtime OCR anchor bridge candidate records.

This is a Rust bridge contract benchmark. It consumes R9 safe OCR anchor
evidence, converts it into runtime-shaped anchor rows, and runs the Rust bridge
validator over those rows. It does not promote Android runtime calibration,
change validators, report gates, chromatographic math, model policy, or
CalculationEngine.
"""

from __future__ import annotations

import argparse
import json
import math
import os
import shutil
import statistics
import subprocess
import time
from pathlib import Path
from typing import Any

from PIL import Image, ImageDraw, ImageFont


R9_REPORT_ROOT = Path("benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate")
R9_SUMMARY = R9_REPORT_ROOT / "summary.json"
EXAMPLE_ROOT = Path("benchmark/examples/r10_runtime_ocr_anchor_bridge_candidate")
REPORT_ROOT = Path("benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate")
DETAIL_ROOT = REPORT_ROOT / "details"
OVERLAY_ROOT = REPORT_ROOT / "overlays"
BRIDGE_INPUT_ROOT = REPORT_ROOT / "bridge_inputs"
BRIDGE_OUTPUT_ROOT = REPORT_ROOT / "bridge_outputs"
RUST_MANIFEST = Path("rust/Cargo.toml")
RUST_BIN = "chromalab_cv_ocr_anchor_bridge"


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


def load_r9_summary() -> dict[str, Any]:
    if not R9_SUMMARY.exists():
        raise FileNotFoundError(f"R9 summary is required before R10: {R9_SUMMARY}")
    return read_json(R9_SUMMARY)


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


def draw_label(draw: ImageDraw.ImageDraw, xy: tuple[int, int], text: str, fill: tuple[int, int, int]) -> None:
    try:
        font = ImageFont.load_default()
    except OSError:
        font = None
    draw.text(xy, text, fill=fill, font=font)


def bridge_anchor_from_r9(anchor: dict[str, Any], axis: str, status: str) -> dict[str, Any]:
    source_ref = anchor.get("sourceDetectionId") or anchor.get("truthLabelKey") or anchor.get("ocrText") or "missing-source-ref"
    return {
        "anchor_id": str(anchor.get("truthLabelKey") or source_ref),
        "axis": axis,
        "ocr_text": str(anchor.get("ocrText", "")),
        "ocr_value": anchor.get("ocrValue"),
        "pixel_coordinate": anchor.get("pixelCoordinate"),
        "source_crop_ref": str(source_ref),
        "source_crop_path": None,
        "confidence": anchor.get("confidence"),
        "text_role": "tick_label",
        "geometry_source": "OCR_LABEL_BOX_PIXEL_PROJECTION",
        "numeric_source": "LOCAL_OCR_TEXT",
        "candidate_status": status,
        "residual_px": anchor.get("labelCenterOffsetPx"),
    }


def build_bridge_input(detail: dict[str, Any]) -> dict[str, Any]:
    graphs = []
    for graph in detail.get("graphOcrAnchorResults", []):
        anchors = []
        for axis_key, axis in [("xAxis", "X"), ("yAxis", "Y")]:
            axis_result = graph.get(axis_key, {})
            for anchor in axis_result.get("acceptedAnchors", []):
                anchors.append(bridge_anchor_from_r9(anchor, axis, "ACCEPTED"))
            for anchor in axis_result.get("rejectedAnchors", []):
                anchors.append(bridge_anchor_from_r9(anchor, axis, "UPSTREAM_REJECTED"))
        graphs.append(
            {
                "graph_id": graph["graphId"],
                "graph_index": int(graph["graphIndex"]),
                "anchors": anchors,
            }
        )
    return {
        "fixture_id": detail["fixtureId"],
        "graphs": graphs,
    }


def run_rust_bridge(input_path: Path) -> dict[str, Any]:
    cargo, env = local_cargo_env()
    command = [
        *cargo,
        "run",
        "--manifest-path",
        path_string(RUST_MANIFEST),
        "--quiet",
        "--bin",
        RUST_BIN,
        "--",
        path_string(input_path),
    ]
    completed = subprocess.run(
        command,
        check=True,
        capture_output=True,
        text=True,
        timeout=180,
        env=env,
    )
    return json.loads(completed.stdout)


def graph_plot_by_id(detail: dict[str, Any]) -> dict[str, dict[str, Any]]:
    return {graph["graphId"]: graph["plotArea"] for graph in detail.get("graphOcrAnchorResults", [])}


def draw_axis_anchor(
    draw: ImageDraw.ImageDraw,
    plot: dict[str, Any],
    axis_name: str,
    pixel_coordinate: float,
    text: str,
) -> None:
    x0 = float(plot["x"])
    y0 = float(plot["y"])
    x1 = x0 + float(plot["width"])
    y1 = y0 + float(plot["height"])
    if axis_name == "X":
        x = int(round(pixel_coordinate))
        y = int(round(y1))
        color = (0, 140, 90)
        draw.line((x, int(y0), x, y), fill=color, width=1)
        draw.ellipse((x - 3, y - 3, x + 3, y + 3), outline=color, width=2)
        draw_label(draw, (x + 4, y - 14), text, color)
    else:
        x = int(round(x0))
        y = int(round(pixel_coordinate))
        color = (255, 120, 0)
        draw.line((x, y, int(x1), y), fill=color, width=1)
        draw.ellipse((x - 3, y - 3, x + 3, y + 3), outline=color, width=2)
        draw_label(draw, (x + 4, y + 4), text, color)


def render_overlay(image_path: Path, r9_detail: dict[str, Any], bridge_report: dict[str, Any], out: Path) -> str:
    image = Image.open(image_path).convert("RGB")
    draw = ImageDraw.Draw(image)
    plot_by_id = graph_plot_by_id(r9_detail)
    for graph in bridge_report.get("graphs", []):
        plot = plot_by_id.get(graph["graph_id"])
        if not plot:
            continue
        x0 = int(plot["x"])
        y0 = int(plot["y"])
        x1 = x0 + int(plot["width"])
        y1 = y0 + int(plot["height"])
        color = (0, 170, 255) if graph["accepted_anchor_count"] > 0 else (220, 40, 40)
        draw.rectangle((x0, y0, x1, y1), outline=color, width=3)
        draw_label(
            draw,
            (x0 + 4, y0 + 4),
            f"G{graph['graph_index']} bridge anchors={graph['accepted_anchor_count']}",
            color,
        )
        for anchor in graph.get("accepted_anchors", []):
            draw_axis_anchor(
                draw,
                plot,
                anchor["axis"],
                float(anchor["pixel_coordinate"]),
                str(anchor["ocr_text"])[:12],
            )
    out.parent.mkdir(parents=True, exist_ok=True)
    image.save(out)
    return path_string(out)


def bridge_status(r9_detail: dict[str, Any], bridge_report: dict[str, Any]) -> str:
    r9_anchors = int(r9_detail["acceptedAnchorCount"])
    bridge_anchors = int(bridge_report["accepted_anchor_count"])
    if r9_anchors == 0 and bridge_anchors == 0:
        return "MISSING"
    if r9_anchors != bridge_anchors:
        return "FAIL"
    return "REVIEW"


def build_record(
    r9_detail: dict[str, Any],
    bridge_input_path: str,
    bridge_output_path: str,
    bridge_report: dict[str, Any],
    overlay_path: str,
    detail_path: str,
) -> tuple[dict[str, Any], dict[str, Any]]:
    fixture_id = r9_detail["fixtureId"]
    status = bridge_status(r9_detail, bridge_report)
    r9_anchors = int(r9_detail["acceptedAnchorCount"])
    bridge_anchors = int(bridge_report["accepted_anchor_count"])
    parity_pass = r9_anchors == bridge_anchors
    record_id = f"r10_{fixture_id}_runtime_ocr_anchor_bridge_candidate_v1"
    metrics = {
        "r9AcceptedAnchorCount": r9_anchors,
        "bridgeAcceptedAnchorCount": bridge_anchors,
        "bridgeRejectedAnchorCount": int(bridge_report["rejected_anchor_count"]),
        "bridgeGraphCount": int(bridge_report["graph_count"]),
        "anchorCountParityPass": parity_pass,
        "missingSourceCropFileCount": int(bridge_report["missing_source_crop_file_count"]),
    }
    summary = (
        "Runs R9 OCR anchor rows through the Rust runtime OCR anchor bridge contract. "
        "This is shadow-only and records missing crop files as a promotion blocker."
    )
    record = {
        "schemaVersion": "chromalab.benchmark.runtime_ocr_anchor_bridge_record.v1",
        "recordId": record_id,
        "fixtureId": fixture_id,
        "sourceId": "r10_runtime_ocr_anchor_bridge_candidate_v1",
        "sourceKind": "RUST_RUNTIME_OCR_ANCHOR_BRIDGE_CANDIDATE",
        "productionImpact": "NONE_SHADOW_ONLY",
        "runtimeReadiness": "RUST_BRIDGE_CONTRACT_NOT_ANDROID_RUNTIME_READY",
        "expectedGraphCount": int(r9_detail["expectedGraphCount"]),
        "detectedGraphCount": int(r9_detail["detectedGraphCount"]),
        "graphCountScore": r9_detail["graphCountScore"],
        "expectedLayoutClass": r9_detail["expectedLayoutClass"],
        "predictedLayoutClass": r9_detail["predictedLayoutClass"],
        "layoutClassScore": r9_detail["layoutClassScore"],
        "runtimeOcrAnchorBridge": {
            "status": status,
            "available": bridge_anchors > 0,
            "summary": summary,
            "metrics": metrics,
        },
        "bridgeReport": bridge_report,
        "failureClass": "" if status != "FAIL" else "RUNTIME_OCR_ANCHOR_BRIDGE_PARITY_FAILURE",
        "firstFailingStage": "" if status != "FAIL" else "RUNTIME_OCR_ANCHOR_BRIDGE",
        "evidence": {
            "sourceImage": r9_detail["sourceImage"],
            "recordSource": path_string(REPORT_ROOT / "summary.json"),
            "artifactPaths": [
                overlay_path,
                detail_path,
                bridge_input_path,
                bridge_output_path,
                r9_detail["overlayPath"],
                r9_detail["recordPath"],
            ],
        },
        "promotionDecision": "RUNTIME_OCR_ANCHOR_BRIDGE_REQUIRES_ANDROID_PARITY",
        "notes": [
            "Shadow-only R10 Rust runtime OCR anchor bridge candidate.",
            "Accepted rows require OCR numeric values and deterministic pixel geometry.",
            "VLM geometry and VLM numeric sources are rejected by the Rust bridge contract.",
            "Source crop references are preserved; source crop image files are still missing and block promotion.",
            "No Android runtime calibration, trace, peak, report-gate, or CalculationEngine authority.",
        ],
    }
    detail = {
        "fixtureId": fixture_id,
        "sourceImage": r9_detail["sourceImage"],
        "expectedGraphCount": int(r9_detail["expectedGraphCount"]),
        "detectedGraphCount": int(r9_detail["detectedGraphCount"]),
        "graphCountScore": r9_detail["graphCountScore"],
        "expectedLayoutClass": r9_detail["expectedLayoutClass"],
        "predictedLayoutClass": r9_detail["predictedLayoutClass"],
        "layoutClassScore": r9_detail["layoutClassScore"],
        "runtimeOcrAnchorBridgeStatus": status,
        "r9AcceptedAnchorCount": r9_anchors,
        "bridgeAcceptedAnchorCount": bridge_anchors,
        "bridgeRejectedAnchorCount": int(bridge_report["rejected_anchor_count"]),
        "bridgeGraphCount": int(bridge_report["graph_count"]),
        "anchorCountParityPass": parity_pass,
        "missingSourceCropFileCount": int(bridge_report["missing_source_crop_file_count"]),
        "bridgeReport": bridge_report,
        "overlayPath": overlay_path,
        "detailPath": detail_path,
        "bridgeInputPath": bridge_input_path,
        "bridgeOutputPath": bridge_output_path,
        "recordPath": path_string(EXAMPLE_ROOT / record_id / "runtime-ocr-anchor-bridge-record.json"),
        "r9OverlayPath": r9_detail["overlayPath"],
        "r9RecordPath": r9_detail["recordPath"],
        "promotionDecision": record["promotionDecision"],
    }
    return record, detail


def write_examples(records: list[dict[str, Any]]) -> None:
    EXAMPLE_ROOT.mkdir(parents=True, exist_ok=True)
    for record in records:
        write_json(EXAMPLE_ROOT / record["recordId"] / "runtime-ocr-anchor-bridge-record.json", record)


def create_contact_sheet(details: list[dict[str, Any]]) -> str:
    tiles = []
    for detail in details:
        image = Image.open(detail["overlayPath"]).convert("RGB")
        image.thumbnail((320, 240), Image.Resampling.LANCZOS)
        canvas = Image.new("RGB", (340, 290), "white")
        canvas.paste(image, ((340 - image.width) // 2, 8))
        draw = ImageDraw.Draw(canvas)
        draw_label(draw, (8, 250), detail["fixtureId"][:40], (0, 0, 0))
        draw_label(
            draw,
            (8, 264),
            f"{detail['runtimeOcrAnchorBridgeStatus']} bridge={detail['bridgeAcceptedAnchorCount']}",
            (0, 0, 0),
        )
        draw_label(draw, (8, 278), f"missing crop files={detail['missingSourceCropFileCount']}", (0, 0, 0))
        tiles.append(canvas)
    columns = 4
    tile_w, tile_h = 340, 290
    rows = math.ceil(len(tiles) / columns)
    sheet = Image.new("RGB", (columns * tile_w, rows * tile_h), "white")
    for index, tile in enumerate(tiles):
        sheet.paste(tile, ((index % columns) * tile_w, (index // columns) * tile_h))
    out = REPORT_ROOT / "contact_sheet_runtime_ocr_anchor_bridge.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    return path_string(out)


def build_summary(details: list[dict[str, Any]]) -> dict[str, Any]:
    graph_pass = sum(1 for detail in details if detail["graphCountScore"] == "PASS")
    layout_pass = sum(1 for detail in details if detail["layoutClassScore"] == "PASS")
    scoreable = [detail for detail in details if int(detail["r9AcceptedAnchorCount"]) > 0]
    parity_pass = sum(1 for detail in scoreable if detail["anchorCountParityPass"])
    accepted_count = sum(int(detail["bridgeAcceptedAnchorCount"]) for detail in details)
    rejected_count = sum(int(detail["bridgeRejectedAnchorCount"]) for detail in details)
    missing_crop_count = sum(int(detail["missingSourceCropFileCount"]) for detail in details)
    status_counts: dict[str, int] = {}
    for detail in details:
        status = detail["runtimeOcrAnchorBridgeStatus"]
        status_counts[status] = status_counts.get(status, 0) + 1
    bridge_counts = [int(detail["bridgeAcceptedAnchorCount"]) for detail in details]
    return {
        "schemaVersion": "chromalab.benchmark.r10_runtime_ocr_anchor_bridge_candidate_summary.v1",
        "productionImpact": "NONE_SHADOW_ONLY",
        "overallVerdict": "R10_RUNTIME_OCR_ANCHOR_BRIDGE_CANDIDATE_REVIEW",
        "recordCount": len(details),
        "fixtureCount": len({detail["fixtureId"] for detail in details}),
        "graphCountPass": graph_pass,
        "layoutClassPass": layout_pass,
        "scoreableFixtureCount": len(scoreable),
        "anchorCountParityPass": parity_pass,
        "bridgeAcceptedAnchorCount": accepted_count,
        "bridgeRejectedAnchorCount": rejected_count,
        "missingSourceCropFileCount": missing_crop_count,
        "meanBridgeAcceptedAnchorCount": round(float(statistics.mean(bridge_counts)), 6) if bridge_counts else None,
        "statusCounts": status_counts,
        "rustBridgeBinary": RUST_BIN,
        "nextRequiredWork": [
            "Produce the same bridge rows from Android or direct Rust OCR/crop runtime, not only from R9 benchmark evidence.",
            "Persist real source crop image paths for accepted anchors before promotion.",
            "Feed bridge rows into calibration ensemble shadow comparison in R11.",
            "Keep VLM advisory-only and reject any model-derived pixel geometry or numeric calibration values.",
        ],
        "records": details,
    }


def write_markdown(summary: dict[str, Any]) -> None:
    lines = [
        "# R10 Runtime OCR Anchor Bridge Candidate",
        "",
        f"Verdict: `{summary['overallVerdict']}`",
        "",
        "Production impact: `NONE_SHADOW_ONLY`",
        "",
        f"Records: `{summary['recordCount']}`",
        f"Fixtures: `{summary['fixtureCount']}`",
        f"Graph-count pass: `{summary['graphCountPass']}/{summary['recordCount']}`",
        f"Layout-class pass: `{summary['layoutClassPass']}/{summary['recordCount']}`",
        f"Scoreable fixtures: `{summary['scoreableFixtureCount']}`",
        f"Anchor-count parity pass: `{summary['anchorCountParityPass']}/{summary['scoreableFixtureCount']}`",
        f"Bridge accepted anchors: `{summary['bridgeAcceptedAnchorCount']}`",
        f"Bridge rejected anchors: `{summary['bridgeRejectedAnchorCount']}`",
        f"Missing source crop files: `{summary['missingSourceCropFileCount']}`",
        "",
        "R10 converts R9 safe OCR anchor evidence into runtime-shaped rows and validates them through the Rust OCR anchor bridge contract.",
        "It remains shadow-only and does not change Android runtime behavior, validators, report gates, chromatographic math, model policy, or CalculationEngine.",
        "",
        "Contact sheet: `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/contact_sheet_runtime_ocr_anchor_bridge.png`",
        "",
        "## Fixture Results",
        "",
        "| Fixture | Graphs | Layout | R9 anchors | Bridge anchors | Rejected | Missing crop files | Parity | Overlay | Detail |",
        "|---|---:|---|---:|---:|---:|---:|---|---|---|",
    ]
    for detail in summary["records"]:
        lines.append(
            "| `{fixtureId}` | {detectedGraphCount} | `{layout}` | {r9Anchors} | {bridgeAnchors} | {rejected} | {missingCrops} | `{parity}` | `{overlay}` | `{detail}` |".format(
                fixtureId=detail["fixtureId"],
                detectedGraphCount=detail["detectedGraphCount"],
                layout=detail["predictedLayoutClass"],
                r9Anchors=detail["r9AcceptedAnchorCount"],
                bridgeAnchors=detail["bridgeAcceptedAnchorCount"],
                rejected=detail["bridgeRejectedAnchorCount"],
                missingCrops=detail["missingSourceCropFileCount"],
                parity=detail["anchorCountParityPass"],
                overlay=detail["overlayPath"],
                detail=detail["detailPath"],
            )
        )
    lines.extend(["", "## Next Required Work", ""])
    for item in summary["nextRequiredWork"]:
        lines.append(f"- {item}")
    lines.append("")
    write_text(REPORT_ROOT / "summary.md", "\n".join(lines))


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--clean", action="store_true", help="Remove previous R10 examples/report before writing.")
    args = parser.parse_args(argv)

    if args.clean:
        for path in [EXAMPLE_ROOT, REPORT_ROOT]:
            if path.exists():
                shutil.rmtree(path)

    r9_summary = load_r9_summary()
    records = []
    details = []
    for r9_detail in r9_summary["records"]:
        fixture_id = r9_detail["fixtureId"]
        started = time.perf_counter()
        bridge_input = build_bridge_input(r9_detail)
        bridge_input_path = BRIDGE_INPUT_ROOT / f"{fixture_id}_runtime_ocr_anchor_bridge_input.json"
        write_json(bridge_input_path, bridge_input)
        bridge_report = run_rust_bridge(bridge_input_path)
        bridge_output_path = BRIDGE_OUTPUT_ROOT / f"{fixture_id}_runtime_ocr_anchor_bridge_output.json"
        write_json(bridge_output_path, bridge_report)
        overlay_path = render_overlay(
            Path(r9_detail["sourceImage"]),
            r9_detail,
            bridge_report,
            OVERLAY_ROOT / f"{fixture_id}_runtime_ocr_anchor_bridge_overlay.png",
        )
        detail_path = path_string(DETAIL_ROOT / f"{fixture_id}_runtime_ocr_anchor_bridge_detail.json")
        record, detail = build_record(
            r9_detail,
            path_string(bridge_input_path),
            path_string(bridge_output_path),
            bridge_report,
            overlay_path,
            detail_path,
        )
        detail["elapsedMs"] = round((time.perf_counter() - started) * 1000.0, 3)
        write_json(Path(detail_path), detail)
        records.append(record)
        details.append(detail)

    write_examples(records)
    summary = build_summary(details)
    summary["contactSheet"] = create_contact_sheet(details)
    write_json(REPORT_ROOT / "summary.json", summary)
    write_markdown(summary)
    print(
        "R10 runtime OCR anchor bridge candidate wrote "
        f"{len(records)} records to {EXAMPLE_ROOT.as_posix()} and report to {REPORT_ROOT.as_posix()}."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
