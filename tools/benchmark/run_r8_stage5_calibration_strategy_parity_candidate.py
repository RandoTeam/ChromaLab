#!/usr/bin/env python3
"""Build R8 Stage 5 calibration strategy parity candidate records.

This is a PC-side shadow benchmark only. It consumes R7 axis/frame/scale
evidence and partial DR-C4 manual tick/text annotations. It does not change
Android runtime, validators, report gates, chromatographic math, model policy,
or CalculationEngine.
"""

from __future__ import annotations

import argparse
import json
import math
import shutil
import statistics
import time
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image, ImageDraw, ImageFont


R7_REPORT_ROOT = Path("benchmark/reports/r7_stage4_axis_frame_scale_candidate")
R7_SUMMARY = R7_REPORT_ROOT / "summary.json"
DRC4_ANNOTATION_PATH = Path("benchmark/annotations/drc4_tick_text_role_annotations/manual-p0-tick-text-annotations.json")
EXAMPLE_ROOT = Path("benchmark/examples/r8_stage5_calibration_strategy_parity_candidate")
REPORT_ROOT = Path("benchmark/reports/r8_stage5_calibration_strategy_parity_candidate")
DETAIL_ROOT = REPORT_ROOT / "details"
OVERLAY_ROOT = REPORT_ROOT / "overlays"


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


def load_r7_summary() -> dict[str, Any]:
    if not R7_SUMMARY.exists():
        raise FileNotFoundError(f"R7 summary is required before R8 calibration parity: {R7_SUMMARY}")
    return read_json(R7_SUMMARY)


def load_tick_annotations() -> dict[str, dict[str, Any]]:
    if not DRC4_ANNOTATION_PATH.exists():
        return {}
    data = read_json(DRC4_ANNOTATION_PATH)
    return {record["fixtureId"]: record for record in data.get("records", [])}


def graph_annotation_by_order(record: dict[str, Any] | None, index: int) -> dict[str, Any] | None:
    if not record:
        return None
    graphs = record.get("graphs", [])
    if index < 0 or index >= len(graphs):
        return None
    return graphs[index]


def split_anchors(graph_annotation: dict[str, Any] | None) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    if not graph_annotation:
        return [], []
    x_anchors = []
    y_anchors = []
    for anchor in graph_annotation.get("calibrationAnchorCandidates", []):
        if anchor.get("axis") == "X":
            x_anchors.append(anchor)
        elif anchor.get("axis") == "Y":
            y_anchors.append(anchor)
    return x_anchors, y_anchors


def coordinate(anchor: dict[str, Any], axis: str) -> float:
    pixel = anchor["pixel"]
    return float(pixel["x"] if axis == "X" else pixel["y"])


def fit_axis(anchors: list[dict[str, Any]], axis: str, endpoint_only: bool) -> dict[str, Any]:
    if len(anchors) < 2:
        return {
            "status": "MISSING",
            "anchorCount": len(anchors),
            "fitAnchorCount": len(anchors),
            "rmse": None,
            "maxResidual": None,
            "slope": None,
            "intercept": None,
            "monotonic": False,
            "rejectionReason": "REJECTED_INSUFFICIENT_ANCHORS",
        }
    ordered = sorted(anchors, key=lambda item: coordinate(item, axis))
    fit_anchors = [ordered[0], ordered[-1]] if endpoint_only else ordered
    coords_fit = np.array([coordinate(anchor, axis) for anchor in fit_anchors], dtype=float)
    values_fit = np.array([float(anchor["value"]) for anchor in fit_anchors], dtype=float)
    coords_all = np.array([coordinate(anchor, axis) for anchor in ordered], dtype=float)
    values_all = np.array([float(anchor["value"]) for anchor in ordered], dtype=float)
    slope, intercept = np.polyfit(coords_fit, values_fit, deg=1)
    predicted = slope * coords_all + intercept
    residuals = values_all - predicted
    sorted_values = values_all.tolist()
    increasing = all(b >= a for a, b in zip(sorted_values, sorted_values[1:]))
    decreasing = all(b <= a for a, b in zip(sorted_values, sorted_values[1:]))
    monotonic = bool(increasing or decreasing)
    rmse = float(math.sqrt(float(np.mean(residuals * residuals))))
    max_residual = float(np.max(np.abs(residuals)))
    status = "REVIEW" if monotonic else "INVALID"
    rejection_reason = "" if monotonic else "REJECTED_NON_MONOTONIC_ANCHORS"
    return {
        "status": status,
        "anchorCount": len(anchors),
        "fitAnchorCount": len(fit_anchors),
        "rmse": round(rmse, 6),
        "maxResidual": round(max_residual, 6),
        "slope": round(float(slope), 10),
        "intercept": round(float(intercept), 6),
        "monotonic": monotonic,
        "rejectionReason": rejection_reason,
    }


def combine_axis_status(x_fit: dict[str, Any], y_fit: dict[str, Any], source: str) -> str:
    if x_fit["status"] == "REVIEW" and y_fit["status"] == "REVIEW":
        return "REVIEW"
    if x_fit["status"] == "INVALID" or y_fit["status"] == "INVALID":
        return "INVALID"
    if source == "GEOMETRY_ONLY":
        return "INVALID"
    return "MISSING"


def text_rejection_count(graph_annotation: dict[str, Any] | None) -> int:
    if not graph_annotation:
        return 0
    return sum(1 for role in graph_annotation.get("textRoleLabels", []) if role.get("rejectedAsTickLabel"))


def build_strategy_results(graph_evidence: dict[str, Any], graph_annotation: dict[str, Any] | None) -> dict[str, Any]:
    x_anchors, y_anchors = split_anchors(graph_annotation)
    all_x = fit_axis(x_anchors, "X", endpoint_only=False)
    all_y = fit_axis(y_anchors, "Y", endpoint_only=False)
    endpoint_x = fit_axis(x_anchors, "X", endpoint_only=True)
    endpoint_y = fit_axis(y_anchors, "Y", endpoint_only=True)
    forbidden_text_count = text_rejection_count(graph_annotation)
    all_anchor_status = combine_axis_status(all_x, all_y, source="ANNOTATION")
    endpoint_status = combine_axis_status(endpoint_x, endpoint_y, source="ANNOTATION")
    strategies = [
        {
            "strategyId": "MANUAL_REVIEW_ALL_ANCHOR_FIT",
            "status": all_anchor_status,
            "selectionEligible": all_anchor_status == "REVIEW",
            "x": all_x,
            "y": all_y,
            "source": "DRC4_MANUAL_REVIEW_ANCHORS" if graph_annotation else "NO_DRC4_ANNOTATION",
            "selectionReason": "",
            "rejectionReason": "" if all_anchor_status == "REVIEW" else "REJECTED_NO_DRC4_SCALE_TRUTH",
        },
        {
            "strategyId": "TWO_ANCHOR_ENDPOINT_FIT",
            "status": endpoint_status,
            "selectionEligible": endpoint_status == "REVIEW",
            "x": endpoint_x,
            "y": endpoint_y,
            "source": "DRC4_ENDPOINT_ANCHORS" if graph_annotation else "NO_DRC4_ANNOTATION",
            "selectionReason": "",
            "rejectionReason": "" if endpoint_status == "REVIEW" else "REJECTED_INSUFFICIENT_ANCHORS",
        },
        {
            "strategyId": "R7_AXIS_FRAME_GEOMETRY_ONLY",
            "status": "INVALID",
            "selectionEligible": False,
            "x": {
                "status": "INVALID",
                "anchorCount": 0,
                "fitAnchorCount": 0,
                "rmse": None,
                "maxResidual": None,
                "slope": None,
                "intercept": None,
                "monotonic": False,
                "rejectionReason": "REJECTED_NO_NUMERIC_VALUES",
            },
            "y": {
                "status": "INVALID",
                "anchorCount": 0,
                "fitAnchorCount": 0,
                "rmse": None,
                "maxResidual": None,
                "slope": None,
                "intercept": None,
                "monotonic": False,
                "rejectionReason": "REJECTED_NO_NUMERIC_VALUES",
            },
            "source": "R7_AXIS_FRAME_SCALE_EVIDENCE",
            "selectionReason": "",
            "rejectionReason": "REJECTED_NO_NUMERIC_VALUES",
        },
        {
            "strategyId": "FORBIDDEN_TEXT_REJECTION_AUDIT",
            "status": "REVIEW" if forbidden_text_count > 0 else "MISSING",
            "selectionEligible": False,
            "x": {},
            "y": {},
            "source": "DRC4_TEXT_ROLE_ANNOTATIONS" if graph_annotation else "NO_DRC4_ANNOTATION",
            "forbiddenTextRejectedCount": forbidden_text_count,
            "selectionReason": "",
            "rejectionReason": "NOT_A_CALIBRATION_STRATEGY",
        },
    ]
    selected = next((strategy for strategy in strategies if strategy["strategyId"] == "MANUAL_REVIEW_ALL_ANCHOR_FIT" and strategy["selectionEligible"]), None)
    if selected is None:
        selected = next((strategy for strategy in strategies if strategy["strategyId"] == "TWO_ANCHOR_ENDPOINT_FIT" and strategy["selectionEligible"]), None)
    if selected is not None:
        selected = dict(selected)
        selected["selectionReason"] = "SELECTED_REVIEW_SCORING_TRUTH"
        selected_strategy_id = selected["strategyId"]
    else:
        selected_strategy_id = ""
    annotated_anchor_count = len(x_anchors) + len(y_anchors)
    return {
        "graphIndex": graph_evidence["graphIndex"],
        "plotArea": graph_evidence["plotArea"],
        "annotationAvailable": graph_annotation is not None,
        "annotatedAnchorCount": annotated_anchor_count,
        "selectedStrategyId": selected_strategy_id,
        "selectedStatus": selected["status"] if selected else "MISSING",
        "selectedStrategy": selected,
        "strategies": strategies,
        "axisAlignment": graph_evidence.get("manualReviewScaleFit", {}).get("alignment", {}),
        "rejectionReason": "" if selected else "NO_USABLE_CALIBRATION_STRATEGY_IN_SHADOW_RECORD",
    }


def draw_label(draw: ImageDraw.ImageDraw, xy: tuple[int, int], text: str, fill: tuple[int, int, int]) -> None:
    try:
        font = ImageFont.load_default()
    except OSError:
        font = None
    draw.text(xy, text, fill=fill, font=font)


def render_overlay(image_path: Path, graph_results: list[dict[str, Any]], annotations: dict[str, Any] | None, out: Path) -> str:
    image = Image.open(image_path).convert("RGB")
    draw = ImageDraw.Draw(image)
    annotation_graphs = annotations.get("graphs", []) if annotations else []
    for graph in graph_results:
        plot = graph["plotArea"]
        x0 = int(plot["x"])
        y0 = int(plot["y"])
        x1 = x0 + int(plot["width"])
        y1 = y0 + int(plot["height"])
        draw.rectangle((x0, y0, x1, y1), outline=(0, 220, 255), width=3)
        color = (0, 160, 80) if graph["selectedStrategyId"] else (220, 40, 40)
        draw_label(draw, (x0 + 4, y0 + 4), f"G{graph['graphIndex']} {graph['selectedStatus']}", color)
    for annotation_graph in annotation_graphs:
        for anchor in annotation_graph.get("calibrationAnchorCandidates", []):
            x = float(anchor["pixel"]["x"])
            y = float(anchor["pixel"]["y"])
            color = (0, 180, 90) if anchor.get("axis") == "X" else (255, 130, 0)
            draw.ellipse((x - 3, y - 3, x + 3, y + 3), outline=color, width=2)
    out.parent.mkdir(parents=True, exist_ok=True)
    image.save(out)
    return str(out).replace("\\", "/")


def create_contact_sheet(details: list[dict[str, Any]]) -> str:
    tiles = []
    for detail in details:
        image = Image.open(detail["overlayPath"]).convert("RGB")
        image.thumbnail((320, 240), Image.Resampling.LANCZOS)
        canvas = Image.new("RGB", (340, 280), "white")
        canvas.paste(image, ((340 - image.width) // 2, 8))
        draw = ImageDraw.Draw(canvas)
        draw_label(draw, (8, 250), detail["fixtureId"][:40], (0, 0, 0))
        draw_label(draw, (8, 264), f"{detail['calibrationStrategyStatus']} / selected {detail['selectedCalibrationGraphCount']}", (0, 0, 0))
        tiles.append(canvas)
    columns = 4
    tile_w, tile_h = 340, 280
    rows = math.ceil(len(tiles) / columns)
    sheet = Image.new("RGB", (columns * tile_w, rows * tile_h), "white")
    for index, tile in enumerate(tiles):
        sheet.paste(tile, ((index % columns) * tile_w, (index // columns) * tile_h))
    out = REPORT_ROOT / "contact_sheet_calibration_strategy.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    return str(out).replace("\\", "/")


def selected_residuals(graph_results: list[dict[str, Any]], axis: str) -> list[float]:
    values = []
    for graph in graph_results:
        selected = graph.get("selectedStrategy") or {}
        fit = selected.get(axis.lower(), {})
        if fit.get("rmse") is not None:
            values.append(float(fit["rmse"]))
    return values


def summarize_fixture(graph_results: list[dict[str, Any]]) -> dict[str, Any]:
    selected_count = sum(1 for graph in graph_results if graph["selectedStrategyId"])
    annotated_graph_count = sum(1 for graph in graph_results if graph["annotationAvailable"])
    selected_review_count = sum(1 for graph in graph_results if graph["selectedStatus"] == "REVIEW")
    anchor_count = sum(int(graph["annotatedAnchorCount"]) for graph in graph_results)
    x_rmse = selected_residuals(graph_results, "X")
    y_rmse = selected_residuals(graph_results, "Y")
    return {
        "graphCount": len(graph_results),
        "annotatedGraphCount": annotated_graph_count,
        "selectedCalibrationGraphCount": selected_count,
        "selectedReviewGraphCount": selected_review_count,
        "annotatedAnchorCount": anchor_count,
        "selectedXMeanRmse": round(float(statistics.mean(x_rmse)), 6) if x_rmse else -1.0,
        "selectedYMeanRmse": round(float(statistics.mean(y_rmse)), 6) if y_rmse else -1.0,
    }


def build_record(
    r7_detail: dict[str, Any],
    r7_record: dict[str, Any],
    graph_results: list[dict[str, Any]],
    overlay_path: str,
    detail_path: str,
) -> tuple[dict[str, Any], dict[str, Any]]:
    fixture_id = r7_detail["fixtureId"]
    metrics = summarize_fixture(graph_results)
    status = "REVIEW"
    first_failing_stage = ""
    if r7_detail["graphCountScore"] != "PASS":
        status = "FAIL"
        first_failing_stage = "GRAPH_DISCOVERY"
    elif r7_detail["layoutClassScore"] != "PASS":
        status = "FAIL"
        first_failing_stage = "PLOTAREA_LAYOUT_CLASSIFICATION"
    elif metrics["selectedCalibrationGraphCount"] == 0:
        status = "REVIEW"
        first_failing_stage = ""
    record_id = f"stage12345_{fixture_id}_pc_stage5_calibration_strategy_parity_candidate_v1"
    record = {
        "schemaVersion": "chromalab.benchmark.stage12345_parity_record.v1",
        "recordId": record_id,
        "fixtureId": fixture_id,
        "mode": "pc_prototype",
        "sourceId": "r8_stage5_calibration_strategy_parity_candidate_v1",
        "sourceKind": "PC_STAGE5_CALIBRATION_STRATEGY_PARITY_CANDIDATE",
        "productionImpact": "NONE_SHADOW_ONLY",
        "runtimeReadiness": "PC_STAGE5_CALIBRATION_STRATEGY_PARITY_CANDIDATE_NOT_RUNTIME_READY",
        "expectedGraphCount": int(r7_detail["expectedGraphCount"]),
        "detectedGraphCount": int(r7_detail["detectedGraphCount"]),
        "graphCountScore": r7_detail["graphCountScore"],
        "expectedLayoutClass": r7_detail["expectedLayoutClass"],
        "predictedLayoutClass": r7_detail["predictedLayoutClass"],
        "layoutClassScore": r7_detail["layoutClassScore"],
        "imagePreparation": r7_record["imagePreparation"],
        "graphDiscovery": r7_record["graphDiscovery"],
        "plotAreaLayout": r7_record["plotAreaLayout"],
        "axisFrameScale": r7_record["axisFrameScale"],
        "calibrationStrategy": {
            "status": status,
            "available": True,
            "summary": "Compares shadow calibration strategy candidates using DRC4 manual-review anchors as scoring truth only. This is not runtime calibration.",
            "metrics": {
                "graphCount": metrics["graphCount"],
                "annotatedGraphCount": metrics["annotatedGraphCount"],
                "selectedCalibrationGraphCount": metrics["selectedCalibrationGraphCount"],
                "selectedReviewGraphCount": metrics["selectedReviewGraphCount"],
                "annotatedAnchorCount": metrics["annotatedAnchorCount"],
                "selectedXMeanRmse": metrics["selectedXMeanRmse"],
                "selectedYMeanRmse": metrics["selectedYMeanRmse"],
            },
        },
        "failureClass": "" if not first_failing_stage else "CALIBRATION_STRATEGY_REVIEW",
        "firstFailingStage": first_failing_stage,
        "evidence": {
            "sourceImage": r7_detail["sourceImage"],
            "recordSource": str(REPORT_ROOT / "summary.json").replace("\\", "/"),
            "artifactPaths": [
                overlay_path,
                detail_path,
                r7_detail["overlayPath"],
                r7_detail["recordPath"],
            ],
        },
        "promotionDecision": "STAGE5_CANDIDATE_REQUIRES_RUNTIME_CALIBRATION_PARITY",
        "notes": [
            "Shadow-only R8 Stage 5 calibration strategy parity candidate.",
            "DRC4 manual-review anchors are scoring truth only, not production calibration evidence.",
            "Geometry-only candidates with no numeric values are rejected.",
            "No Android runtime, OCR runtime, trace, peak, report-gate, or CalculationEngine authority.",
        ],
    }
    detail = {
        "fixtureId": fixture_id,
        "sourceImage": r7_detail["sourceImage"],
        "expectedGraphCount": int(r7_detail["expectedGraphCount"]),
        "detectedGraphCount": int(r7_detail["detectedGraphCount"]),
        "graphCountScore": r7_detail["graphCountScore"],
        "expectedLayoutClass": r7_detail["expectedLayoutClass"],
        "predictedLayoutClass": r7_detail["predictedLayoutClass"],
        "layoutClassScore": r7_detail["layoutClassScore"],
        "calibrationStrategyStatus": status,
        "selectedCalibrationGraphCount": metrics["selectedCalibrationGraphCount"],
        "selectedReviewGraphCount": metrics["selectedReviewGraphCount"],
        "annotatedGraphCount": metrics["annotatedGraphCount"],
        "annotatedAnchorCount": metrics["annotatedAnchorCount"],
        "selectedXMeanRmse": metrics["selectedXMeanRmse"],
        "selectedYMeanRmse": metrics["selectedYMeanRmse"],
        "graphCalibrationResults": graph_results,
        "overlayPath": overlay_path,
        "detailPath": detail_path,
        "recordPath": str(EXAMPLE_ROOT / record_id / "stage12345-parity-record.json").replace("\\", "/"),
        "r7OverlayPath": r7_detail["overlayPath"],
        "r7RecordPath": r7_detail["recordPath"],
        "promotionDecision": record["promotionDecision"],
    }
    return record, detail


def write_examples(records: list[dict[str, Any]]) -> None:
    EXAMPLE_ROOT.mkdir(parents=True, exist_ok=True)
    for record in records:
        write_json(EXAMPLE_ROOT / record["recordId"] / "stage12345-parity-record.json", record)


def build_summary(details: list[dict[str, Any]]) -> dict[str, Any]:
    graph_pass = sum(1 for detail in details if detail["graphCountScore"] == "PASS")
    layout_pass = sum(1 for detail in details if detail["layoutClassScore"] == "PASS")
    annotated_fixture_count = sum(1 for detail in details if detail["annotatedGraphCount"] > 0)
    selected_graph_count = sum(int(detail["selectedCalibrationGraphCount"]) for detail in details)
    annotated_anchor_count = sum(int(detail["annotatedAnchorCount"]) for detail in details)
    x_rmse = [float(detail["selectedXMeanRmse"]) for detail in details if float(detail["selectedXMeanRmse"]) >= 0]
    y_rmse = [float(detail["selectedYMeanRmse"]) for detail in details if float(detail["selectedYMeanRmse"]) >= 0]
    status_counts: dict[str, int] = {}
    for detail in details:
        status = detail["calibrationStrategyStatus"]
        status_counts[status] = status_counts.get(status, 0) + 1
    return {
        "schemaVersion": "chromalab.benchmark.r8_stage5_calibration_strategy_parity_candidate_summary.v1",
        "productionImpact": "NONE_SHADOW_ONLY",
        "overallVerdict": "R8_STAGE5_CALIBRATION_STRATEGY_PARITY_REVIEW",
        "recordCount": len(details),
        "fixtureCount": len({detail["fixtureId"] for detail in details}),
        "graphCountPass": graph_pass,
        "layoutClassPass": layout_pass,
        "annotatedTruthFixtureCount": annotated_fixture_count,
        "selectedCalibrationGraphCount": selected_graph_count,
        "annotatedAnchorCount": annotated_anchor_count,
        "selectedXMeanRmse": round(float(statistics.mean(x_rmse)), 6) if x_rmse else None,
        "selectedYMeanRmse": round(float(statistics.mean(y_rmse)), 6) if y_rmse else None,
        "statusCounts": status_counts,
        "nextRequiredWork": [
            "Keep R8 calibration strategy parity shadow-only until automatic OCR/runtime anchors are measured.",
            "Use selected/rejected strategy tables to guide runtime calibration ensemble parity.",
            "Do not treat DRC4 manual-review anchor fits as production calibration evidence.",
            "Do not promote to Android runtime until calibration candidates are produced from automatic evidence.",
        ],
        "records": details,
    }


def write_markdown(summary: dict[str, Any]) -> None:
    lines = [
        "# R8 Stage 5 Calibration Strategy Parity Candidate",
        "",
        f"Verdict: `{summary['overallVerdict']}`",
        "",
        "Production impact: `NONE_SHADOW_ONLY`",
        "",
        f"Records: `{summary['recordCount']}`",
        f"Fixtures: `{summary['fixtureCount']}`",
        f"Graph-count pass: `{summary['graphCountPass']}/{summary['recordCount']}`",
        f"Layout-class pass: `{summary['layoutClassPass']}/{summary['recordCount']}`",
        f"Annotated truth fixtures: `{summary['annotatedTruthFixtureCount']}`",
        f"Selected calibration graph count: `{summary['selectedCalibrationGraphCount']}`",
        f"Annotated anchor count: `{summary['annotatedAnchorCount']}`",
        f"Selected X mean RMSE: `{summary['selectedXMeanRmse']}`",
        f"Selected Y mean RMSE: `{summary['selectedYMeanRmse']}`",
        "",
        "R8 consumes R7 axis/frame/scale evidence and DR-C4 manual-review tick/text annotations.",
        "It compares shadow calibration strategies and keeps manual anchors as scoring truth only.",
        "It does not change Android runtime behavior, validators, report gates, chromatographic math, model policy, or CalculationEngine.",
        "",
        "Contact sheet: `benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/contact_sheet_calibration_strategy.png`",
        "",
        "## Fixture Results",
        "",
        "| Fixture | Graphs | Layout | Selected graphs | Anchors | X RMSE | Y RMSE | Overlay | Detail |",
        "|---|---:|---|---:|---:|---:|---:|---|---|",
    ]
    for detail in summary["records"]:
        lines.append(
            "| `{fixtureId}` | {detectedGraphCount} | `{predictedLayoutClass}` | {selectedGraphs} | {anchors} | {xRmse} | {yRmse} | `{overlay}` | `{detail}` |".format(
                fixtureId=detail["fixtureId"],
                detectedGraphCount=detail["detectedGraphCount"],
                predictedLayoutClass=detail["predictedLayoutClass"],
                selectedGraphs=detail["selectedCalibrationGraphCount"],
                anchors=detail["annotatedAnchorCount"],
                xRmse=detail["selectedXMeanRmse"],
                yRmse=detail["selectedYMeanRmse"],
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
    parser.add_argument("--clean", action="store_true", help="Remove previous R8 examples/report before writing.")
    args = parser.parse_args(argv)

    if args.clean:
        for path in [EXAMPLE_ROOT, REPORT_ROOT]:
            if path.exists():
                shutil.rmtree(path)

    r7_summary = read_json(R7_SUMMARY)
    annotations = load_tick_annotations()
    records = []
    details = []
    for r7_detail in r7_summary["records"]:
        fixture_id = r7_detail["fixtureId"]
        started = time.perf_counter()
        annotation_record = annotations.get(fixture_id)
        graph_results = []
        for index, graph_evidence in enumerate(r7_detail.get("graphEvidence", [])):
            graph_annotation = graph_annotation_by_order(annotation_record, index)
            graph_results.append(build_strategy_results(graph_evidence, graph_annotation))
        image_path = Path(r7_detail["sourceImage"])
        overlay_path = render_overlay(
            image_path,
            graph_results,
            annotation_record,
            OVERLAY_ROOT / f"{fixture_id}_calibration_strategy_overlay.png",
        )
        detail_path = str(DETAIL_ROOT / f"{fixture_id}_calibration_strategy_detail.json").replace("\\", "/")
        r7_record = read_json(Path(r7_detail["recordPath"]))
        record, detail = build_record(r7_detail, r7_record, graph_results, overlay_path, detail_path)
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
        "R8 Stage 5 calibration strategy parity wrote "
        f"{len(records)} records to {EXAMPLE_ROOT.as_posix()} and report to {REPORT_ROOT.as_posix()}."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
