#!/usr/bin/env python3
"""Build R11 runtime calibration closure shadow records.

This benchmark consumes R10 runtime OCR anchor bridge rows and builds
calibration strategy evidence from those rows. It does not change Android
runtime, validators, report gates, chromatographic math, model policy, or
CalculationEngine.
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

from PIL import Image, ImageDraw, ImageFont


R8_SUMMARY = Path("benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/summary.json")
R9_SUMMARY = Path("benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/summary.json")
R10_SUMMARY = Path("benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/summary.json")
EXAMPLE_ROOT = Path("benchmark/examples/r11_runtime_calibration_closure_candidate")
REPORT_ROOT = Path("benchmark/reports/r11_runtime_calibration_closure_candidate")
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


def path_string(path: Path) -> str:
    return str(path).replace("\\", "/")


def load_required_summary(path: Path, name: str) -> dict[str, Any]:
    if not path.exists():
        raise FileNotFoundError(f"{name} summary is required before R11: {path}")
    return read_json(path)


def records_by_fixture(summary: dict[str, Any]) -> dict[str, dict[str, Any]]:
    return {record["fixtureId"]: record for record in summary.get("records", [])}


def graph_by_index(graphs: list[dict[str, Any]]) -> dict[int, dict[str, Any]]:
    return {int(graph["graphIndex"]): graph for graph in graphs}


def graph_by_id_or_index(graphs: list[dict[str, Any]]) -> dict[str, dict[str, Any]]:
    indexed: dict[str, dict[str, Any]] = {}
    for graph in graphs:
        indexed[str(graph.get("graphId", ""))] = graph
        indexed[str(int(graph["graphIndex"]))] = graph
    return indexed


def simple_linear_fit(points: list[tuple[float, float]]) -> tuple[float, float] | None:
    if len(points) < 2:
        return None
    xs = [point[0] for point in points]
    ys = [point[1] for point in points]
    mean_x = statistics.mean(xs)
    mean_y = statistics.mean(ys)
    denominator = sum((x - mean_x) ** 2 for x in xs)
    if denominator == 0:
        return None
    slope = sum((x - mean_x) * (y - mean_y) for x, y in points) / denominator
    intercept = mean_y - slope * mean_x
    return slope, intercept


def axis_direction(values: list[float]) -> tuple[bool, str]:
    increasing = all(b >= a for a, b in zip(values, values[1:]))
    decreasing = all(b <= a for a, b in zip(values, values[1:]))
    if increasing and not decreasing:
        return True, "INCREASING_WITH_PIXEL"
    if decreasing and not increasing:
        return True, "DECREASING_WITH_PIXEL"
    if increasing and decreasing:
        return False, "CONSTANT_VALUES"
    return False, "NON_MONOTONIC"


def fit_axis(anchors: list[dict[str, Any]], endpoint_only: bool) -> dict[str, Any]:
    ordered = sorted(anchors, key=lambda item: float(item["pixel_coordinate"]))
    if not ordered:
        return {
            "status": "MISSING",
            "anchorCount": 0,
            "fitAnchorCount": 0,
            "rmseValue": None,
            "maxResidualValue": None,
            "slope": None,
            "intercept": None,
            "monotonic": False,
            "axisDirection": "MISSING",
            "meanConfidence": None,
            "meanSourceResidualPx": None,
            "maxSourceResidualPx": None,
            "cropFileAvailableCount": 0,
            "missingSourceCropFileCount": 0,
            "rejectionReason": "REJECTED_INSUFFICIENT_ANCHORS",
        }
    if len(ordered) < 2:
        return {
            "status": "INVALID",
            "anchorCount": len(ordered),
            "fitAnchorCount": len(ordered),
            "rmseValue": None,
            "maxResidualValue": None,
            "slope": None,
            "intercept": None,
            "monotonic": False,
            "axisDirection": "INSUFFICIENT_ANCHORS",
            "meanConfidence": _mean_optional(anchor.get("confidence") for anchor in ordered),
            "meanSourceResidualPx": _mean_abs_optional(anchor.get("residual_px") for anchor in ordered),
            "maxSourceResidualPx": _max_abs_optional(anchor.get("residual_px") for anchor in ordered),
            "cropFileAvailableCount": sum(1 for anchor in ordered if anchor.get("crop_file_available")),
            "missingSourceCropFileCount": sum(1 for anchor in ordered if not anchor.get("crop_file_available")),
            "rejectionReason": "REJECTED_INSUFFICIENT_ANCHORS",
        }
    values = [float(anchor["ocr_value"]) for anchor in ordered]
    monotonic, direction = axis_direction(values)
    if not monotonic:
        return {
            "status": "INVALID",
            "anchorCount": len(ordered),
            "fitAnchorCount": 0,
            "rmseValue": None,
            "maxResidualValue": None,
            "slope": None,
            "intercept": None,
            "monotonic": False,
            "axisDirection": direction,
            "meanConfidence": _mean_optional(anchor.get("confidence") for anchor in ordered),
            "meanSourceResidualPx": _mean_abs_optional(anchor.get("residual_px") for anchor in ordered),
            "maxSourceResidualPx": _max_abs_optional(anchor.get("residual_px") for anchor in ordered),
            "cropFileAvailableCount": sum(1 for anchor in ordered if anchor.get("crop_file_available")),
            "missingSourceCropFileCount": sum(1 for anchor in ordered if not anchor.get("crop_file_available")),
            "rejectionReason": "REJECTED_NON_MONOTONIC_ANCHORS",
        }
    fit_anchors = [ordered[0], ordered[-1]] if endpoint_only else ordered
    points = [(float(anchor["pixel_coordinate"]), float(anchor["ocr_value"])) for anchor in fit_anchors]
    fit = simple_linear_fit(points)
    if fit is None:
        return {
            "status": "INVALID",
            "anchorCount": len(ordered),
            "fitAnchorCount": len(fit_anchors),
            "rmseValue": None,
            "maxResidualValue": None,
            "slope": None,
            "intercept": None,
            "monotonic": monotonic,
            "axisDirection": direction,
            "meanConfidence": _mean_optional(anchor.get("confidence") for anchor in ordered),
            "meanSourceResidualPx": _mean_abs_optional(anchor.get("residual_px") for anchor in ordered),
            "maxSourceResidualPx": _max_abs_optional(anchor.get("residual_px") for anchor in ordered),
            "cropFileAvailableCount": sum(1 for anchor in ordered if anchor.get("crop_file_available")),
            "missingSourceCropFileCount": sum(1 for anchor in ordered if not anchor.get("crop_file_available")),
            "rejectionReason": "REJECTED_NO_PIXEL_GEOMETRY",
        }
    slope, intercept = fit
    residuals = [
        float(anchor["ocr_value"]) - (slope * float(anchor["pixel_coordinate"]) + intercept)
        for anchor in ordered
    ]
    rmse = math.sqrt(sum(residual * residual for residual in residuals) / len(residuals))
    max_residual = max(abs(residual) for residual in residuals)
    return {
        "status": "REVIEW",
        "anchorCount": len(ordered),
        "fitAnchorCount": len(fit_anchors),
        "rmseValue": round(rmse, 6),
        "maxResidualValue": round(max_residual, 6),
        "slope": round(float(slope), 10),
        "intercept": round(float(intercept), 6),
        "monotonic": True,
        "axisDirection": direction,
        "meanConfidence": _mean_optional(anchor.get("confidence") for anchor in ordered),
        "meanSourceResidualPx": _mean_abs_optional(anchor.get("residual_px") for anchor in ordered),
        "maxSourceResidualPx": _max_abs_optional(anchor.get("residual_px") for anchor in ordered),
        "cropFileAvailableCount": sum(1 for anchor in ordered if anchor.get("crop_file_available")),
        "missingSourceCropFileCount": sum(1 for anchor in ordered if not anchor.get("crop_file_available")),
        "rejectionReason": "",
    }


def _mean_optional(values: Any) -> float | None:
    present = [float(value) for value in values if value is not None]
    return round(float(statistics.mean(present)), 6) if present else None


def _mean_abs_optional(values: Any) -> float | None:
    present = [abs(float(value)) for value in values if value is not None]
    return round(float(statistics.mean(present)), 6) if present else None


def _max_abs_optional(values: Any) -> float | None:
    present = [abs(float(value)) for value in values if value is not None]
    return round(float(max(present)), 6) if present else None


def combine_strategy_status(x_fit: dict[str, Any], y_fit: dict[str, Any]) -> tuple[str, bool, str]:
    if x_fit["status"] == "REVIEW" and y_fit["status"] == "REVIEW":
        return "REVIEW", True, ""
    if x_fit["status"] == "MISSING" and y_fit["status"] == "MISSING":
        return "MISSING", False, "REJECTED_NO_R10_BRIDGE_ANCHORS"
    if x_fit["status"] in {"INVALID", "MISSING"}:
        return "INVALID", False, f"X_{x_fit['rejectionReason']}"
    if y_fit["status"] in {"INVALID", "MISSING"}:
        return "INVALID", False, f"Y_{y_fit['rejectionReason']}"
    return "INVALID", False, "REJECTED_UNKNOWN_AXIS_STATE"


def build_strategy(strategy_id: str, anchors: list[dict[str, Any]], endpoint_only: bool) -> dict[str, Any]:
    x_anchors = [anchor for anchor in anchors if anchor["axis"] == "X"]
    y_anchors = [anchor for anchor in anchors if anchor["axis"] == "Y"]
    x_fit = fit_axis(x_anchors, endpoint_only)
    y_fit = fit_axis(y_anchors, endpoint_only)
    status, eligible, rejection_reason = combine_strategy_status(x_fit, y_fit)
    missing_crop_count = int(x_fit["missingSourceCropFileCount"]) + int(y_fit["missingSourceCropFileCount"])
    if status == "REVIEW" and missing_crop_count > 0:
        selection_reason = "SELECTED_REVIEW_MISSING_RUNTIME_CROP_FILES"
    elif status == "REVIEW":
        selection_reason = "SELECTED_REVIEW_R10_BRIDGE_FIT"
    else:
        selection_reason = ""
    return {
        "strategyId": strategy_id,
        "status": status,
        "selectionEligible": eligible,
        "x": x_fit,
        "y": y_fit,
        "source": "R10_RUNTIME_OCR_ANCHOR_BRIDGE_ROWS",
        "selectionReason": selection_reason,
        "rejectionReason": rejection_reason,
        "missingSourceCropFileCount": missing_crop_count,
    }


def r8_reference_for_graph(r8_record: dict[str, Any] | None, graph_index: int) -> dict[str, Any]:
    if not r8_record:
        return {"available": False}
    graph = graph_by_index(r8_record.get("graphCalibrationResults", [])).get(graph_index)
    if not graph:
        return {"available": False}
    selected = graph.get("selectedStrategy") or {}
    return {
        "available": True,
        "selectedStrategyId": graph.get("selectedStrategyId", ""),
        "selectedStatus": graph.get("selectedStatus", "MISSING"),
        "annotatedAnchorCount": int(graph.get("annotatedAnchorCount", 0)),
        "xAnchorCount": int((selected.get("x") or {}).get("anchorCount", 0) or 0),
        "yAnchorCount": int((selected.get("y") or {}).get("anchorCount", 0) or 0),
        "xRmse": (selected.get("x") or {}).get("rmse"),
        "yRmse": (selected.get("y") or {}).get("rmse"),
    }


def r9_reference_for_graph(r9_record: dict[str, Any] | None, graph_id: str, graph_index: int) -> dict[str, Any]:
    if not r9_record:
        return {"available": False}
    indexed = graph_by_id_or_index(r9_record.get("graphOcrAnchorResults", []))
    graph = indexed.get(graph_id) or indexed.get(str(graph_index))
    if not graph:
        return {"available": False}
    return {
        "available": True,
        "decision": graph.get("decision", "MISSING"),
        "decisionReason": graph.get("decisionReason", ""),
        "acceptedAnchorCount": int(graph.get("acceptedAnchorCount", 0)),
        "rejectedAnchorCount": int(graph.get("rejectedAnchorCount", 0)),
        "xStatus": (graph.get("xAxis") or {}).get("status", "MISSING"),
        "yStatus": (graph.get("yAxis") or {}).get("status", "MISSING"),
        "xAcceptedAnchorCount": int((graph.get("xAxis") or {}).get("acceptedAnchorCount", 0)),
        "yAcceptedAnchorCount": int((graph.get("yAxis") or {}).get("acceptedAnchorCount", 0)),
    }


def plot_area_for_graph(r9_record: dict[str, Any], graph_id: str, graph_index: int) -> dict[str, Any] | None:
    indexed = graph_by_id_or_index(r9_record.get("graphOcrAnchorResults", []))
    graph = indexed.get(graph_id) or indexed.get(str(graph_index))
    return graph.get("plotArea") if graph else None


def build_graph_closure(
    graph: dict[str, Any],
    r8_record: dict[str, Any] | None,
    r9_record: dict[str, Any],
) -> dict[str, Any]:
    graph_id = graph["graph_id"]
    graph_index = int(graph["graph_index"])
    accepted = graph.get("accepted_anchors", [])
    rejected = graph.get("rejected_anchors", [])
    all_strategy = build_strategy("R10_BRIDGE_ALL_ANCHOR_FIT", accepted, endpoint_only=False)
    endpoint_strategy = build_strategy("R10_BRIDGE_ENDPOINT_FIT", accepted, endpoint_only=True)
    rejected_audit = {
        "strategyId": "R10_BRIDGE_REJECTED_ROW_AUDIT",
        "status": "REVIEW" if rejected else "MISSING",
        "selectionEligible": False,
        "x": {},
        "y": {},
        "source": "R10_RUNTIME_OCR_ANCHOR_BRIDGE_REJECTIONS",
        "selectionReason": "",
        "rejectionReason": "NOT_A_CALIBRATION_STRATEGY",
        "rejectedBridgeRowCount": len(rejected),
    }
    strategies = [all_strategy, endpoint_strategy, rejected_audit]
    selected = next((strategy for strategy in strategies if strategy["strategyId"] == "R10_BRIDGE_ALL_ANCHOR_FIT" and strategy["selectionEligible"]), None)
    if selected is None:
        selected = next((strategy for strategy in strategies if strategy["strategyId"] == "R10_BRIDGE_ENDPOINT_FIT" and strategy["selectionEligible"]), None)
    r8_ref = r8_reference_for_graph(r8_record, graph_index)
    r9_ref = r9_reference_for_graph(r9_record, graph_id, graph_index)
    status = selected["status"] if selected else ("MISSING" if not accepted else "INVALID")
    if selected:
        closure_reason = selected["selectionReason"]
    elif not accepted:
        closure_reason = "NO_R10_BRIDGE_ANCHORS_AVAILABLE_FOR_GRAPH"
    else:
        closure_reason = "NO_USABLE_R10_BRIDGE_CALIBRATION_STRATEGY"
    return {
        "graphId": graph_id,
        "graphIndex": graph_index,
        "plotArea": plot_area_for_graph(r9_record, graph_id, graph_index),
        "status": status,
        "acceptedBridgeAnchorCount": len(accepted),
        "rejectedBridgeAnchorCount": len(rejected),
        "selectedStrategyId": selected["strategyId"] if selected else "",
        "selectedStrategy": selected,
        "strategies": strategies,
        "r8Reference": r8_ref,
        "r9Reference": r9_ref,
        "closureReason": closure_reason,
        "promotionBlockers": promotion_blockers_for_graph(selected, accepted),
    }


def promotion_blockers_for_graph(selected: dict[str, Any] | None, accepted: list[dict[str, Any]]) -> list[str]:
    blockers = []
    if not selected:
        blockers.append("NO_SELECTED_R10_CALIBRATION_STRATEGY")
    if accepted and any(not anchor.get("crop_file_available") for anchor in accepted):
        blockers.append("SOURCE_CROP_FILES_NOT_PERSISTED")
    if accepted:
        blockers.append("ANDROID_RUNTIME_OCR_GENERATION_NOT_PROVEN")
    return blockers


def build_closure_report(
    r10_record: dict[str, Any],
    r9_record: dict[str, Any],
    r8_record: dict[str, Any] | None,
) -> dict[str, Any]:
    bridge_report = r10_record["bridgeReport"]
    graphs = [
        build_graph_closure(graph, r8_record, r9_record)
        for graph in bridge_report.get("graphs", [])
    ]
    selected = [graph for graph in graphs if graph["selectedStrategyId"]]
    invalid = [graph for graph in graphs if graph["status"] == "INVALID"]
    missing = [graph for graph in graphs if graph["status"] == "MISSING"]
    target_blocker = ""
    if r10_record["fixtureId"] == "bench_01_mz71_screenshot_page":
        target_blocker = "R10 bridge rows produce review-grade graph calibration fits; Android Y-calibration remains blocked until equivalent runtime OCR rows and crop files are exported."
    elif r10_record["fixtureId"] == "bench_05_tic_plus_ions":
        target_blocker = "R10 bridge rows produce review-grade graph calibration fits for TIC+ions panels; Android layout propagation and Y-calibration evidence remain runtime blockers."
    if selected:
        status = "REVIEW"
    elif invalid:
        status = "INVALID"
    else:
        status = "MISSING"
    return {
        "fixtureId": r10_record["fixtureId"],
        "status": status,
        "graphCount": len(graphs),
        "selectedCalibrationGraphCount": len(selected),
        "invalidCalibrationGraphCount": len(invalid),
        "missingCalibrationGraphCount": len(missing),
        "acceptedBridgeAnchorCount": int(bridge_report.get("accepted_anchor_count", 0)),
        "rejectedBridgeAnchorCount": int(bridge_report.get("rejected_anchor_count", 0)),
        "missingSourceCropFileCount": int(bridge_report.get("missing_source_crop_file_count", 0)),
        "targetBlockerAssessment": target_blocker,
        "graphs": graphs,
    }


def draw_label(draw: ImageDraw.ImageDraw, xy: tuple[int, int], text: str, fill: tuple[int, int, int]) -> None:
    try:
        font = ImageFont.load_default()
    except OSError:
        font = None
    draw.text(xy, text, fill=fill, font=font)


def draw_anchor(draw: ImageDraw.ImageDraw, plot: dict[str, Any], anchor: dict[str, Any]) -> None:
    x0 = float(plot["x"])
    y0 = float(plot["y"])
    x1 = x0 + float(plot["width"])
    y1 = y0 + float(plot["height"])
    if anchor["axis"] == "X":
        x = int(round(float(anchor["pixel_coordinate"])))
        color = (0, 145, 90)
        draw.line((x, int(y0), x, int(y1)), fill=color, width=1)
        draw.ellipse((x - 3, int(y1) - 3, x + 3, int(y1) + 3), outline=color, width=2)
        draw_label(draw, (x + 4, int(y1) - 14), str(anchor["ocr_text"])[:10], color)
    else:
        y = int(round(float(anchor["pixel_coordinate"])))
        color = (255, 120, 0)
        draw.line((int(x0), y, int(x1), y), fill=color, width=1)
        draw.ellipse((int(x0) - 3, y - 3, int(x0) + 3, y + 3), outline=color, width=2)
        draw_label(draw, (int(x0) + 4, y + 4), str(anchor["ocr_text"])[:10], color)


def render_overlay(image_path: Path, closure_report: dict[str, Any], bridge_report: dict[str, Any], out: Path) -> str:
    image = Image.open(image_path).convert("RGB")
    draw = ImageDraw.Draw(image)
    bridge_by_id = {graph["graph_id"]: graph for graph in bridge_report.get("graphs", [])}
    for graph in closure_report["graphs"]:
        plot = graph.get("plotArea")
        if not plot:
            continue
        x0 = int(plot["x"])
        y0 = int(plot["y"])
        x1 = x0 + int(plot["width"])
        y1 = y0 + int(plot["height"])
        color = (0, 170, 255) if graph["selectedStrategyId"] else (220, 40, 40)
        draw.rectangle((x0, y0, x1, y1), outline=color, width=3)
        draw_label(
            draw,
            (x0 + 4, y0 + 4),
            f"G{graph['graphIndex']} {graph['status']} {graph['selectedStrategyId'] or 'no fit'}",
            color,
        )
        bridge_graph = bridge_by_id.get(graph["graphId"], {})
        for anchor in bridge_graph.get("accepted_anchors", []):
            draw_anchor(draw, plot, anchor)
    out.parent.mkdir(parents=True, exist_ok=True)
    image.save(out)
    return path_string(out)


def stage_status(closure_report: dict[str, Any]) -> str:
    if closure_report["selectedCalibrationGraphCount"] > 0:
        return "REVIEW"
    if closure_report["invalidCalibrationGraphCount"] > 0:
        return "FAIL"
    return "MISSING"


def build_record(
    r10_record: dict[str, Any],
    r9_record: dict[str, Any],
    r8_record: dict[str, Any] | None,
    closure_report: dict[str, Any],
    overlay_path: str,
    detail_path: str,
) -> tuple[dict[str, Any], dict[str, Any]]:
    fixture_id = r10_record["fixtureId"]
    status = stage_status(closure_report)
    record_id = f"r11_{fixture_id}_runtime_calibration_closure_candidate_v1"
    metrics = {
        "selectedCalibrationGraphCount": int(closure_report["selectedCalibrationGraphCount"]),
        "invalidCalibrationGraphCount": int(closure_report["invalidCalibrationGraphCount"]),
        "missingCalibrationGraphCount": int(closure_report["missingCalibrationGraphCount"]),
        "acceptedBridgeAnchorCount": int(closure_report["acceptedBridgeAnchorCount"]),
        "rejectedBridgeAnchorCount": int(closure_report["rejectedBridgeAnchorCount"]),
        "missingSourceCropFileCount": int(closure_report["missingSourceCropFileCount"]),
    }
    summary = (
        "Feeds R10 runtime OCR bridge rows into shadow calibration strategy fits. "
        "This remains review-only and does not promote Android runtime calibration."
    )
    references = {
        "r8RecordPath": (r8_record or {}).get("recordPath", ""),
        "r9RecordPath": r9_record.get("recordPath", ""),
        "r10RecordPath": r10_record.get("recordPath", ""),
    }
    artifact_paths = [
        overlay_path,
        detail_path,
        r10_record.get("detailPath", ""),
        r10_record.get("bridgeInputPath", ""),
        r10_record.get("bridgeOutputPath", ""),
        references["r8RecordPath"],
        references["r9RecordPath"],
        references["r10RecordPath"],
    ]
    record = {
        "schemaVersion": "chromalab.benchmark.runtime_calibration_closure_record.v1",
        "recordId": record_id,
        "fixtureId": fixture_id,
        "sourceId": "r11_runtime_calibration_closure_candidate_v1",
        "sourceKind": "RUNTIME_CALIBRATION_CLOSURE_SHADOW_CANDIDATE",
        "productionImpact": "NONE_SHADOW_ONLY",
        "runtimeReadiness": "R10_BRIDGE_ROWS_TO_CALIBRATION_SHADOW_NOT_ANDROID_RUNTIME",
        "expectedGraphCount": int(r10_record["expectedGraphCount"]),
        "detectedGraphCount": int(r10_record["detectedGraphCount"]),
        "graphCountScore": r10_record["graphCountScore"],
        "expectedLayoutClass": r10_record["expectedLayoutClass"],
        "predictedLayoutClass": r10_record["predictedLayoutClass"],
        "layoutClassScore": r10_record["layoutClassScore"],
        "runtimeCalibrationClosure": {
            "status": status,
            "available": status == "REVIEW",
            "summary": summary,
            "metrics": metrics,
        },
        "calibrationClosureReport": closure_report,
        "references": references,
        "failureClass": "" if status != "FAIL" else "RUNTIME_CALIBRATION_CLOSURE_FAILURE",
        "firstFailingStage": "" if status != "FAIL" else "RUNTIME_CALIBRATION_CLOSURE",
        "evidence": {
            "sourceImage": r10_record["sourceImage"],
            "recordSource": path_string(REPORT_ROOT / "summary.json"),
            "artifactPaths": [path for path in artifact_paths if path],
        },
        "promotionDecision": "RUNTIME_CALIBRATION_REQUIRES_ANDROID_EVIDENCE_PACKAGE_PARITY",
        "notes": [
            "Shadow-only R11 calibration closure candidate.",
            "R10 bridge rows are used as calibration anchors only inside benchmark parity output.",
            "Selected REVIEW fits do not become Android runtime calibration authority.",
            "Missing source crop image files remain a promotion blocker.",
            "No Android runtime behavior, validator, report gate, model policy, or CalculationEngine authority changed.",
        ],
    }
    detail = {
        "fixtureId": fixture_id,
        "sourceImage": r10_record["sourceImage"],
        "expectedGraphCount": int(r10_record["expectedGraphCount"]),
        "detectedGraphCount": int(r10_record["detectedGraphCount"]),
        "graphCountScore": r10_record["graphCountScore"],
        "expectedLayoutClass": r10_record["expectedLayoutClass"],
        "predictedLayoutClass": r10_record["predictedLayoutClass"],
        "layoutClassScore": r10_record["layoutClassScore"],
        "runtimeCalibrationClosureStatus": status,
        "closureReport": closure_report,
        "overlayPath": overlay_path,
        "detailPath": detail_path,
        "recordPath": path_string(EXAMPLE_ROOT / record_id / "runtime-calibration-closure-record.json"),
        "references": references,
        "promotionDecision": record["promotionDecision"],
    }
    return record, detail


def write_examples(records: list[dict[str, Any]]) -> None:
    EXAMPLE_ROOT.mkdir(parents=True, exist_ok=True)
    for record in records:
        write_json(EXAMPLE_ROOT / record["recordId"] / "runtime-calibration-closure-record.json", record)


def create_contact_sheet(details: list[dict[str, Any]]) -> str:
    tiles = []
    for detail in details:
        image = Image.open(detail["overlayPath"]).convert("RGB")
        image.thumbnail((320, 240), Image.Resampling.LANCZOS)
        canvas = Image.new("RGB", (340, 292), "white")
        canvas.paste(image, ((340 - image.width) // 2, 8))
        draw = ImageDraw.Draw(canvas)
        report = detail["closureReport"]
        draw_label(draw, (8, 248), detail["fixtureId"][:40], (0, 0, 0))
        draw_label(
            draw,
            (8, 264),
            f"{detail['runtimeCalibrationClosureStatus']} selected={report['selectedCalibrationGraphCount']}",
            (0, 0, 0),
        )
        draw_label(draw, (8, 278), f"missing crop files={report['missingSourceCropFileCount']}", (0, 0, 0))
        tiles.append(canvas)
    columns = 4
    tile_w, tile_h = 340, 292
    rows = math.ceil(len(tiles) / columns)
    sheet = Image.new("RGB", (columns * tile_w, rows * tile_h), "white")
    for index, tile in enumerate(tiles):
        sheet.paste(tile, ((index % columns) * tile_w, (index // columns) * tile_h))
    out = REPORT_ROOT / "contact_sheet_runtime_calibration_closure.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    return path_string(out)


def build_summary(details: list[dict[str, Any]]) -> dict[str, Any]:
    graph_pass = sum(1 for detail in details if detail["graphCountScore"] == "PASS")
    layout_pass = sum(1 for detail in details if detail["layoutClassScore"] == "PASS")
    selected_graphs = sum(int(detail["closureReport"]["selectedCalibrationGraphCount"]) for detail in details)
    accepted = sum(int(detail["closureReport"]["acceptedBridgeAnchorCount"]) for detail in details)
    rejected = sum(int(detail["closureReport"]["rejectedBridgeAnchorCount"]) for detail in details)
    missing_crops = sum(int(detail["closureReport"]["missingSourceCropFileCount"]) for detail in details)
    status_counts: dict[str, int] = {}
    for detail in details:
        status = detail["runtimeCalibrationClosureStatus"]
        status_counts[status] = status_counts.get(status, 0) + 1
    target_findings = [
        {
            "fixtureId": detail["fixtureId"],
            "assessment": detail["closureReport"]["targetBlockerAssessment"],
        }
        for detail in details
        if detail["closureReport"]["targetBlockerAssessment"]
    ]
    return {
        "schemaVersion": "chromalab.benchmark.r11_runtime_calibration_closure_candidate_summary.v1",
        "productionImpact": "NONE_SHADOW_ONLY",
        "overallVerdict": "R11_RUNTIME_CALIBRATION_CLOSURE_CANDIDATE_REVIEW",
        "recordCount": len(details),
        "fixtureCount": len({detail["fixtureId"] for detail in details}),
        "graphCountPass": graph_pass,
        "layoutClassPass": layout_pass,
        "selectedCalibrationGraphCount": selected_graphs,
        "acceptedBridgeAnchorCount": accepted,
        "rejectedBridgeAnchorCount": rejected,
        "missingSourceCropFileCount": missing_crops,
        "statusCounts": status_counts,
        "targetBlockerFindings": target_findings,
        "nextRequiredWork": [
            "Produce equivalent OCR anchor rows from Android runtime evidence packages, not only R10 benchmark rows.",
            "Persist source crop image files for accepted anchors before promotion.",
            "Compare shadow-selected calibration strategies with Android RuntimeEvidencePackage selected/rejected strategy export.",
            "Keep E2B advisory-only and unable to alter calibration strategy selection or metrics.",
        ],
        "records": details,
    }


def write_markdown(summary: dict[str, Any]) -> None:
    lines = [
        "# R11 Runtime Calibration Closure Candidate",
        "",
        f"Verdict: `{summary['overallVerdict']}`",
        "",
        "Production impact: `NONE_SHADOW_ONLY`",
        "",
        f"Records: `{summary['recordCount']}`",
        f"Fixtures: `{summary['fixtureCount']}`",
        f"Graph-count pass: `{summary['graphCountPass']}/{summary['recordCount']}`",
        f"Layout-class pass: `{summary['layoutClassPass']}/{summary['recordCount']}`",
        f"Selected calibration graphs: `{summary['selectedCalibrationGraphCount']}`",
        f"Accepted bridge anchors: `{summary['acceptedBridgeAnchorCount']}`",
        f"Rejected bridge anchors: `{summary['rejectedBridgeAnchorCount']}`",
        f"Missing source crop files: `{summary['missingSourceCropFileCount']}`",
        "",
        "R11 feeds R10 runtime OCR bridge rows into shadow calibration strategy fits.",
        "It remains shadow-only and does not change Android runtime behavior, validators, report gates, chromatographic math, model policy, or CalculationEngine.",
        "",
        "Contact sheet: `benchmark/reports/r11_runtime_calibration_closure_candidate/contact_sheet_runtime_calibration_closure.png`",
        "",
        "## Fixture Results",
        "",
        "| Fixture | Graphs | Layout | Status | Selected calibration graphs | Accepted anchors | Missing crop files | Target blocker note | Overlay | Detail |",
        "|---|---:|---|---|---:|---:|---:|---|---|---|",
    ]
    for detail in summary["records"]:
        report = detail["closureReport"]
        lines.append(
            "| `{fixture}` | {graphs} | `{layout}` | `{status}` | {selected} | {accepted} | {missing} | {note} | `{overlay}` | `{detail}` |".format(
                fixture=detail["fixtureId"],
                graphs=detail["detectedGraphCount"],
                layout=detail["predictedLayoutClass"],
                status=detail["runtimeCalibrationClosureStatus"],
                selected=report["selectedCalibrationGraphCount"],
                accepted=report["acceptedBridgeAnchorCount"],
                missing=report["missingSourceCropFileCount"],
                note=report["targetBlockerAssessment"] or "-",
                overlay=detail["overlayPath"],
                detail=detail["detailPath"],
            )
        )
    lines.extend(["", "## Target Blocker Findings", ""])
    if summary["targetBlockerFindings"]:
        for finding in summary["targetBlockerFindings"]:
            lines.append(f"- `{finding['fixtureId']}`: {finding['assessment']}")
    else:
        lines.append("- No target blocker findings recorded.")
    lines.extend(["", "## Next Required Work", ""])
    for item in summary["nextRequiredWork"]:
        lines.append(f"- {item}")
    lines.append("")
    write_text(REPORT_ROOT / "summary.md", "\n".join(lines))


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--clean", action="store_true", help="Remove previous R11 examples/report before writing.")
    args = parser.parse_args(argv)

    if args.clean:
        for path in [EXAMPLE_ROOT, REPORT_ROOT]:
            if path.exists():
                shutil.rmtree(path)

    r8_summary = load_required_summary(R8_SUMMARY, "R8")
    r9_summary = load_required_summary(R9_SUMMARY, "R9")
    r10_summary = load_required_summary(R10_SUMMARY, "R10")
    r8_records = records_by_fixture(r8_summary)
    r9_records = records_by_fixture(r9_summary)
    records = []
    details = []
    for r10_record in r10_summary["records"]:
        started = time.perf_counter()
        fixture_id = r10_record["fixtureId"]
        r9_record = r9_records[fixture_id]
        r8_record = r8_records.get(fixture_id)
        closure_report = build_closure_report(r10_record, r9_record, r8_record)
        overlay_path = render_overlay(
            Path(r10_record["sourceImage"]),
            closure_report,
            r10_record["bridgeReport"],
            OVERLAY_ROOT / f"{fixture_id}_runtime_calibration_closure_overlay.png",
        )
        detail_path = path_string(DETAIL_ROOT / f"{fixture_id}_runtime_calibration_closure_detail.json")
        record, detail = build_record(
            r10_record,
            r9_record,
            r8_record,
            closure_report,
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
        "R11 runtime calibration closure candidate wrote "
        f"{len(records)} records to {EXAMPLE_ROOT.as_posix()} and report to {REPORT_ROOT.as_posix()}."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
