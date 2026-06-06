#!/usr/bin/env python3
"""Build R7 Stage 4 axis/frame/scale evidence candidate records.

This is a PC-side shadow benchmark only. It consumes R6 plotArea/layout
candidate evidence and partial DR-C4 manual tick/text annotations. It does not
change Android runtime, validators, report gates, chromatographic math, model
policy, or CalculationEngine.
"""

from __future__ import annotations

import argparse
import json
import math
import shutil
import statistics
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image, ImageDraw, ImageFont


R6_REPORT_ROOT = Path("benchmark/reports/r6_stage3_plotarea_layout_candidate")
R6_SUMMARY = R6_REPORT_ROOT / "summary.json"
DRC4_ANNOTATION_PATH = Path("benchmark/annotations/drc4_tick_text_role_annotations/manual-p0-tick-text-annotations.json")
EXAMPLE_ROOT = Path("benchmark/examples/r7_stage4_axis_frame_scale_candidate")
REPORT_ROOT = Path("benchmark/reports/r7_stage4_axis_frame_scale_candidate")
DETAIL_ROOT = REPORT_ROOT / "details"
OVERLAY_ROOT = REPORT_ROOT / "overlays"


@dataclass(frozen=True)
class Box:
    x: int
    y: int
    width: int
    height: int

    @property
    def x2(self) -> int:
        return self.x + self.width

    @property
    def y2(self) -> int:
        return self.y + self.height

    def clamp(self, image_width: int, image_height: int) -> "Box":
        x = max(0, min(self.x, image_width - 1))
        y = max(0, min(self.y, image_height - 1))
        x2 = max(x + 1, min(self.x2, image_width))
        y2 = max(y + 1, min(self.y2, image_height))
        return Box(x=x, y=y, width=x2 - x, height=y2 - y)


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


def box_from_json(data: dict[str, Any]) -> Box:
    return Box(
        x=int(round(float(data["x"]))),
        y=int(round(float(data["y"]))),
        width=int(round(float(data["width"]))),
        height=int(round(float(data["height"]))),
    )


def box_to_json(box: Box) -> dict[str, int]:
    return {"x": box.x, "y": box.y, "width": box.width, "height": box.height}


def load_r6_summary() -> dict[str, Any]:
    if not R6_SUMMARY.exists():
        raise FileNotFoundError(f"R6 summary is required before R7 axis/frame/scale: {R6_SUMMARY}")
    return read_json(R6_SUMMARY)


def load_tick_annotations() -> dict[str, dict[str, Any]]:
    if not DRC4_ANNOTATION_PATH.exists():
        return {}
    data = read_json(DRC4_ANNOTATION_PATH)
    return {record["fixtureId"]: record for record in data.get("records", [])}


def dark_threshold(gray: np.ndarray, box: Box) -> float:
    crop = gray[box.y : box.y2, box.x : box.x2]
    if crop.size == 0:
        return 120.0
    return min(180.0, max(45.0, float(np.percentile(crop, 35))))


def edge_support(gray: np.ndarray, box: Box, orientation: str, position: int, threshold: float) -> float:
    if orientation == "horizontal":
        y0 = max(0, position - 2)
        y1 = min(gray.shape[0], position + 3)
        x0 = max(0, box.x)
        x1 = min(gray.shape[1], box.x2)
        region = gray[y0:y1, x0:x1]
    else:
        x0 = max(0, position - 2)
        x1 = min(gray.shape[1], position + 3)
        y0 = max(0, box.y)
        y1 = min(gray.shape[0], box.y2)
        region = gray[y0:y1, x0:x1]
    if region.size == 0:
        return 0.0
    return round(float((region < threshold).mean()), 6)


def clusters(indices: np.ndarray, max_gap: int) -> list[list[int]]:
    out: list[list[int]] = []
    current: list[int] = []
    last: int | None = None
    for value in sorted(int(item) for item in indices):
        if last is None or value - last <= max_gap:
            current.append(value)
        else:
            out.append(current)
            current = [value]
        last = value
    if current:
        out.append(current)
    return out


def detect_grid_lines(gray: np.ndarray, box: Box, threshold: float) -> tuple[list[int], list[int]]:
    crop = gray[box.y : box.y2, box.x : box.x2]
    if crop.size == 0:
        return [], []
    mask = crop < threshold
    vertical_projection = mask.mean(axis=0)
    horizontal_projection = mask.mean(axis=1)
    vertical_hits = np.where(vertical_projection > 0.12)[0]
    horizontal_hits = np.where(horizontal_projection > 0.12)[0]
    vertical = [box.x + int(round(statistics.mean(group))) for group in clusters(vertical_hits, max_gap=3)]
    horizontal = [box.y + int(round(statistics.mean(group))) for group in clusters(horizontal_hits, max_gap=3)]
    return vertical, horizontal


def label_bands(candidate: dict[str, Any], image_width: int, image_height: int) -> tuple[Box, Box]:
    plot = box_from_json(candidate["plotArea"]).clamp(image_width, image_height)
    panel = box_from_json(candidate["graphPanel"]).clamp(image_width, image_height)
    x_band_y = min(image_height - 1, plot.y2 + 2)
    x_band_bottom = min(image_height, max(x_band_y + 1, panel.y2))
    x_band = Box(plot.x, x_band_y, plot.width, max(1, x_band_bottom - x_band_y)).clamp(image_width, image_height)
    y_band_x = max(0, panel.x)
    y_band_width = max(1, plot.x - y_band_x)
    y_band = Box(y_band_x, plot.y, y_band_width, plot.height).clamp(image_width, image_height)
    return x_band, y_band


def center_in_box(bbox: dict[str, Any], box: Box) -> bool:
    cx = float(bbox["x"]) + float(bbox["width"]) / 2.0
    cy = float(bbox["y"]) + float(bbox["height"]) / 2.0
    return box.x <= cx <= box.x2 and box.y <= cy <= box.y2


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


def fit_axis(anchors: list[dict[str, Any]], axis: str) -> dict[str, Any]:
    if len(anchors) < 2:
        return {
            "status": "MISSING",
            "anchorCount": len(anchors),
            "rmse": None,
            "maxResidual": None,
            "slope": None,
            "intercept": None,
            "monotonic": False,
        }
    coords = np.array(
        [float(anchor["pixel"]["x"] if axis == "X" else anchor["pixel"]["y"]) for anchor in anchors],
        dtype=float,
    )
    values = np.array([float(anchor["value"]) for anchor in anchors], dtype=float)
    slope, intercept = np.polyfit(coords, values, deg=1)
    predicted = slope * coords + intercept
    residuals = values - predicted
    ordered = sorted(zip(coords.tolist(), values.tolist()), key=lambda item: item[0])
    sorted_values = [item[1] for item in ordered]
    increasing = all(b >= a for a, b in zip(sorted_values, sorted_values[1:]))
    decreasing = all(b <= a for a, b in zip(sorted_values, sorted_values[1:]))
    rmse = float(math.sqrt(float(np.mean(residuals * residuals))))
    max_residual = float(np.max(np.abs(residuals)))
    return {
        "status": "REVIEW",
        "anchorCount": len(anchors),
        "rmse": round(rmse, 6),
        "maxResidual": round(max_residual, 6),
        "slope": round(float(slope), 8),
        "intercept": round(float(intercept), 6),
        "monotonic": bool(increasing or decreasing),
    }


def axis_alignment(candidate: dict[str, Any], x_anchors: list[dict[str, Any]], y_anchors: list[dict[str, Any]]) -> dict[str, Any]:
    plot = box_from_json(candidate["plotArea"])
    x_axis_y = int(candidate["axisRow"])
    y_axis_x = plot.x
    x_distances = [abs(float(anchor["pixel"]["y"]) - x_axis_y) for anchor in x_anchors]
    y_distances = [abs(float(anchor["pixel"]["x"]) - y_axis_x) for anchor in y_anchors]
    return {
        "xAxisMeanPixelError": round(float(statistics.mean(x_distances)), 6) if x_distances else None,
        "yAxisMeanPixelError": round(float(statistics.mean(y_distances)), 6) if y_distances else None,
        "xAxisMaxPixelError": round(float(max(x_distances)), 6) if x_distances else None,
        "yAxisMaxPixelError": round(float(max(y_distances)), 6) if y_distances else None,
    }


def graph_annotation_by_order(record: dict[str, Any] | None, index: int) -> dict[str, Any] | None:
    if not record:
        return None
    graphs = record.get("graphs", [])
    if index < 0 or index >= len(graphs):
        return None
    return graphs[index]


def build_graph_evidence(
    gray: np.ndarray,
    candidate: dict[str, Any],
    graph_annotation: dict[str, Any] | None,
) -> dict[str, Any]:
    image_height, image_width = gray.shape
    plot = box_from_json(candidate["plotArea"]).clamp(image_width, image_height)
    x_band, y_band = label_bands(candidate, image_width, image_height)
    threshold = dark_threshold(gray, plot)
    vertical_lines, horizontal_lines = detect_grid_lines(gray, plot, threshold)
    x_anchors, y_anchors = split_anchors(graph_annotation)
    x_fit = fit_axis(x_anchors, "X")
    y_fit = fit_axis(y_anchors, "Y")
    labels = graph_annotation.get("numericLabelBoxes", []) if graph_annotation else []
    x_label_count = sum(1 for label in labels if label.get("axis") == "X" and center_in_box(label["bbox"], x_band))
    y_label_count = sum(1 for label in labels if label.get("axis") == "Y" and center_in_box(label["bbox"], y_band))
    text_roles = graph_annotation.get("textRoleLabels", []) if graph_annotation else []
    rejected_text_count = sum(1 for role in text_roles if role.get("rejectedAsTickLabel"))
    alignment = axis_alignment(candidate, x_anchors, y_anchors)
    x_axis_support = edge_support(gray, plot, "horizontal", int(candidate["axisRow"]), threshold)
    y_axis_support = edge_support(gray, plot, "vertical", plot.x, threshold)
    top_frame_support = edge_support(gray, plot, "horizontal", plot.y, threshold)
    right_frame_support = edge_support(gray, plot, "vertical", plot.x2, threshold)
    scale_status = "REVIEW" if x_fit["status"] == "REVIEW" and y_fit["status"] == "REVIEW" else "MISSING"
    if scale_status == "REVIEW" and (not x_fit["monotonic"] or not y_fit["monotonic"]):
        scale_status = "INVALID"
    return {
        "graphIndex": int(candidate["graphIndex"]),
        "plotArea": box_to_json(plot),
        "xAxisCandidate": {
            "start": {"x": plot.x, "y": int(candidate["axisRow"])},
            "end": {"x": plot.x2, "y": int(candidate["axisRow"])},
            "support": x_axis_support,
        },
        "yAxisCandidate": {
            "start": {"x": plot.x, "y": int(candidate["axisRow"])},
            "end": {"x": plot.x, "y": plot.y},
            "support": y_axis_support,
        },
        "frameSupport": {
            "bottom": x_axis_support,
            "left": y_axis_support,
            "top": top_frame_support,
            "right": right_frame_support,
        },
        "gridLineCandidates": {
            "verticalCount": len(vertical_lines),
            "horizontalCount": len(horizontal_lines),
            "vertical": vertical_lines[:24],
            "horizontal": horizontal_lines[:24],
        },
        "labelBands": {
            "x": box_to_json(x_band),
            "y": box_to_json(y_band),
            "annotatedXLabelCountInBand": x_label_count,
            "annotatedYLabelCountInBand": y_label_count,
            "rejectedTextRoleCount": rejected_text_count,
        },
        "manualReviewScaleFit": {
            "status": scale_status,
            "x": x_fit,
            "y": y_fit,
            "alignment": alignment,
            "source": "DRC4_MANUAL_REVIEW_ANCHORS" if graph_annotation else "NO_DRC4_ANNOTATION",
        },
        "status": "REVIEW",
        "rejectionReason": "SHADOW_AXIS_FRAME_SCALE_EVIDENCE_NOT_RUNTIME_CALIBRATION",
    }


def render_overlay(image_path: Path, graph_evidence: list[dict[str, Any]], out: Path) -> str:
    image = Image.open(image_path).convert("RGB")
    draw = ImageDraw.Draw(image)
    for graph in graph_evidence:
        plot = box_from_json(graph["plotArea"])
        draw.rectangle((plot.x, plot.y, plot.x2, plot.y2), outline=(0, 220, 255), width=3)
        x_axis = graph["xAxisCandidate"]
        y_axis = graph["yAxisCandidate"]
        draw.line((x_axis["start"]["x"], x_axis["start"]["y"], x_axis["end"]["x"], x_axis["end"]["y"]), fill=(255, 190, 0), width=3)
        draw.line((y_axis["start"]["x"], y_axis["start"]["y"], y_axis["end"]["x"], y_axis["end"]["y"]), fill=(255, 80, 0), width=3)
        for x in graph["gridLineCandidates"]["vertical"]:
            draw.line((x, plot.y, x, plot.y2), fill=(90, 180, 255), width=1)
        for y in graph["gridLineCandidates"]["horizontal"]:
            draw.line((plot.x, y, plot.x2, y), fill=(90, 180, 255), width=1)
        x_band = box_from_json(graph["labelBands"]["x"])
        y_band = box_from_json(graph["labelBands"]["y"])
        draw.rectangle((x_band.x, x_band.y, x_band.x2, x_band.y2), outline=(160, 80, 255), width=2)
        draw.rectangle((y_band.x, y_band.y, y_band.x2, y_band.y2), outline=(160, 80, 255), width=2)
        draw_label(draw, (plot.x + 4, plot.y + 4), f"G{graph['graphIndex']} {graph['manualReviewScaleFit']['status']}", (0, 0, 0))
    out.parent.mkdir(parents=True, exist_ok=True)
    image.save(out)
    return str(out).replace("\\", "/")


def draw_label(draw: ImageDraw.ImageDraw, xy: tuple[int, int], text: str, fill: tuple[int, int, int]) -> None:
    try:
        font = ImageFont.load_default()
    except OSError:
        font = None
    draw.text(xy, text, fill=fill, font=font)


def create_contact_sheet(details: list[dict[str, Any]]) -> str:
    tiles = []
    for detail in details:
        image = Image.open(detail["overlayPath"]).convert("RGB")
        image.thumbnail((320, 240), Image.Resampling.LANCZOS)
        canvas = Image.new("RGB", (340, 280), "white")
        canvas.paste(image, ((340 - image.width) // 2, 8))
        draw = ImageDraw.Draw(canvas)
        draw_label(draw, (8, 250), detail["fixtureId"][:40], (0, 0, 0))
        draw_label(draw, (8, 264), f"{detail['axisFrameScaleStatus']} / scale {detail['manualReviewScaleGraphCount']}", (0, 0, 0))
        tiles.append(canvas)
    columns = 4
    tile_w, tile_h = 340, 280
    rows = math.ceil(len(tiles) / columns)
    sheet = Image.new("RGB", (columns * tile_w, rows * tile_h), "white")
    for index, tile in enumerate(tiles):
        sheet.paste(tile, ((index % columns) * tile_w, (index // columns) * tile_h))
    out = REPORT_ROOT / "contact_sheet_axis_frame_scale.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    return str(out).replace("\\", "/")


def aggregate_graph_metrics(graph_evidence: list[dict[str, Any]]) -> dict[str, Any]:
    x_support = [graph["xAxisCandidate"]["support"] for graph in graph_evidence]
    y_support = [graph["yAxisCandidate"]["support"] for graph in graph_evidence]
    x_errors = [
        graph["manualReviewScaleFit"]["alignment"]["xAxisMeanPixelError"]
        for graph in graph_evidence
        if graph["manualReviewScaleFit"]["alignment"]["xAxisMeanPixelError"] is not None
    ]
    y_errors = [
        graph["manualReviewScaleFit"]["alignment"]["yAxisMeanPixelError"]
        for graph in graph_evidence
        if graph["manualReviewScaleFit"]["alignment"]["yAxisMeanPixelError"] is not None
    ]
    scale_review_count = sum(1 for graph in graph_evidence if graph["manualReviewScaleFit"]["status"] == "REVIEW")
    return {
        "graphCount": len(graph_evidence),
        "xAxisSupportMean": round(float(statistics.mean(x_support)), 6) if x_support else 0.0,
        "yAxisSupportMean": round(float(statistics.mean(y_support)), 6) if y_support else 0.0,
        "manualReviewScaleGraphCount": scale_review_count,
        "xAxisMeanPixelError": round(float(statistics.mean(x_errors)), 6) if x_errors else -1.0,
        "yAxisMeanPixelError": round(float(statistics.mean(y_errors)), 6) if y_errors else -1.0,
        "verticalGridLineCount": sum(graph["gridLineCandidates"]["verticalCount"] for graph in graph_evidence),
        "horizontalGridLineCount": sum(graph["gridLineCandidates"]["horizontalCount"] for graph in graph_evidence),
    }


def build_record(
    r6_detail: dict[str, Any],
    r6_record: dict[str, Any],
    graph_evidence: list[dict[str, Any]],
    overlay_path: str,
    detail_path: str,
) -> tuple[dict[str, Any], dict[str, Any]]:
    fixture_id = r6_detail["fixtureId"]
    metrics = aggregate_graph_metrics(graph_evidence)
    annotation_available = any(graph["manualReviewScaleFit"]["source"] == "DRC4_MANUAL_REVIEW_ANCHORS" for graph in graph_evidence)
    status = "REVIEW"
    first_failing_stage = ""
    if r6_detail["graphCountScore"] != "PASS":
        status = "FAIL"
        first_failing_stage = "GRAPH_DISCOVERY"
    elif r6_detail["layoutClassScore"] != "PASS":
        status = "FAIL"
        first_failing_stage = "PLOTAREA_LAYOUT_CLASSIFICATION"
    record_id = f"stage1234_{fixture_id}_pc_stage4_axis_frame_scale_candidate_v1"
    record = {
        "schemaVersion": "chromalab.benchmark.stage1234_parity_record.v1",
        "recordId": record_id,
        "fixtureId": fixture_id,
        "mode": "pc_prototype",
        "sourceId": "r7_stage4_axis_frame_scale_candidate_v1",
        "sourceKind": "PC_STAGE4_AXIS_FRAME_SCALE_CANDIDATE",
        "productionImpact": "NONE_SHADOW_ONLY",
        "runtimeReadiness": "PC_STAGE4_AXIS_FRAME_SCALE_CANDIDATE_NOT_RUNTIME_READY",
        "expectedGraphCount": int(r6_detail["expectedGraphCount"]),
        "detectedGraphCount": int(r6_detail["detectedGraphCount"]),
        "graphCountScore": r6_detail["graphCountScore"],
        "expectedLayoutClass": r6_detail["expectedLayoutClass"],
        "predictedLayoutClass": r6_detail["predictedLayoutClass"],
        "layoutClassScore": r6_detail["layoutClassScore"],
        "imagePreparation": r6_record["imagePreparation"],
        "graphDiscovery": r6_record["graphDiscovery"],
        "plotAreaLayout": r6_record["plotAreaLayout"],
        "axisFrameScale": {
            "status": status,
            "available": True,
            "summary": "Builds deterministic axis/frame/grid/label-band evidence from R6 plotArea candidates and scores manual-review scale fits where DRC4 anchors exist. This is not runtime calibration.",
            "metrics": {
                "graphCount": metrics["graphCount"],
                "xAxisSupportMean": metrics["xAxisSupportMean"],
                "yAxisSupportMean": metrics["yAxisSupportMean"],
                "manualReviewScaleGraphCount": metrics["manualReviewScaleGraphCount"],
                "annotationTruthAvailable": annotation_available,
                "xAxisMeanPixelError": metrics["xAxisMeanPixelError"],
                "yAxisMeanPixelError": metrics["yAxisMeanPixelError"],
                "verticalGridLineCount": metrics["verticalGridLineCount"],
                "horizontalGridLineCount": metrics["horizontalGridLineCount"],
            },
        },
        "failureClass": "" if not first_failing_stage else "AXIS_FRAME_SCALE_REVIEW",
        "firstFailingStage": first_failing_stage,
        "evidence": {
            "sourceImage": r6_detail["sourceImage"],
            "recordSource": str(REPORT_ROOT / "summary.json").replace("\\", "/"),
            "artifactPaths": [
                overlay_path,
                detail_path,
                r6_detail["overlayPath"],
                r6_detail["recordPath"],
            ],
        },
        "promotionDecision": "STAGE4_CANDIDATE_REQUIRES_CALIBRATION_PARITY",
        "notes": [
            "Shadow-only R7 Stage 4 axis/frame/scale evidence candidate.",
            "Manual-review DRC4 anchors are used only for scoring candidate evidence, not production calibration.",
            "No Android runtime, OCR runtime, calibration, trace, peak, report-gate, or CalculationEngine authority.",
            "Unannotated fixtures keep scale truth unavailable and cannot be promoted from this record alone.",
        ],
    }
    detail = {
        "fixtureId": fixture_id,
        "sourceImage": r6_detail["sourceImage"],
        "expectedGraphCount": int(r6_detail["expectedGraphCount"]),
        "detectedGraphCount": int(r6_detail["detectedGraphCount"]),
        "graphCountScore": r6_detail["graphCountScore"],
        "expectedLayoutClass": r6_detail["expectedLayoutClass"],
        "predictedLayoutClass": r6_detail["predictedLayoutClass"],
        "layoutClassScore": r6_detail["layoutClassScore"],
        "axisFrameScaleStatus": status,
        "annotationTruthAvailable": annotation_available,
        "manualReviewScaleGraphCount": metrics["manualReviewScaleGraphCount"],
        "xAxisSupportMean": metrics["xAxisSupportMean"],
        "yAxisSupportMean": metrics["yAxisSupportMean"],
        "xAxisMeanPixelError": metrics["xAxisMeanPixelError"],
        "yAxisMeanPixelError": metrics["yAxisMeanPixelError"],
        "graphEvidence": graph_evidence,
        "overlayPath": overlay_path,
        "detailPath": detail_path,
        "recordPath": str(EXAMPLE_ROOT / record_id / "stage1234-parity-record.json").replace("\\", "/"),
        "r6OverlayPath": r6_detail["overlayPath"],
        "r6RecordPath": r6_detail["recordPath"],
        "promotionDecision": record["promotionDecision"],
    }
    return record, detail


def write_examples(records: list[dict[str, Any]]) -> None:
    EXAMPLE_ROOT.mkdir(parents=True, exist_ok=True)
    for record in records:
        write_json(EXAMPLE_ROOT / record["recordId"] / "stage1234-parity-record.json", record)


def build_summary(details: list[dict[str, Any]]) -> dict[str, Any]:
    graph_pass = sum(1 for detail in details if detail["graphCountScore"] == "PASS")
    layout_pass = sum(1 for detail in details if detail["layoutClassScore"] == "PASS")
    annotated_fixture_count = sum(1 for detail in details if detail["annotationTruthAvailable"])
    scale_graph_count = sum(int(detail["manualReviewScaleGraphCount"]) for detail in details)
    x_support = [float(detail["xAxisSupportMean"]) for detail in details]
    y_support = [float(detail["yAxisSupportMean"]) for detail in details]
    x_errors = [float(detail["xAxisMeanPixelError"]) for detail in details if float(detail["xAxisMeanPixelError"]) >= 0]
    y_errors = [float(detail["yAxisMeanPixelError"]) for detail in details if float(detail["yAxisMeanPixelError"]) >= 0]
    status_counts: dict[str, int] = {}
    for detail in details:
        status = detail["axisFrameScaleStatus"]
        status_counts[status] = status_counts.get(status, 0) + 1
    return {
        "schemaVersion": "chromalab.benchmark.r7_stage4_axis_frame_scale_candidate_summary.v1",
        "productionImpact": "NONE_SHADOW_ONLY",
        "overallVerdict": "R7_STAGE4_AXIS_FRAME_SCALE_EVIDENCE_REVIEW",
        "recordCount": len(details),
        "fixtureCount": len({detail["fixtureId"] for detail in details}),
        "graphCountPass": graph_pass,
        "layoutClassPass": layout_pass,
        "annotatedTruthFixtureCount": annotated_fixture_count,
        "manualReviewScaleGraphCount": scale_graph_count,
        "xAxisSupportMean": round(float(statistics.mean(x_support)), 6) if x_support else 0.0,
        "yAxisSupportMean": round(float(statistics.mean(y_support)), 6) if y_support else 0.0,
        "annotatedXAxisMeanPixelError": round(float(statistics.mean(x_errors)), 6) if x_errors else None,
        "annotatedYAxisMeanPixelError": round(float(statistics.mean(y_errors)), 6) if y_errors else None,
        "statusCounts": status_counts,
        "nextRequiredWork": [
            "Keep R7 axis/frame/scale evidence shadow-only until automatic OCR/scale anchors are measured.",
            "Use R7 support and alignment metrics to guide Stage 5 calibration strategy parity.",
            "Do not treat manual-review DRC4 anchor fits as runtime calibration evidence.",
            "Do not promote to Android runtime until axis/frame/scale output beats or equals current evidence gates.",
        ],
        "records": details,
    }


def write_markdown(summary: dict[str, Any]) -> None:
    lines = [
        "# R7 Stage 4 Axis, Frame, And Scale Evidence Candidate",
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
        f"Manual-review scale graph count: `{summary['manualReviewScaleGraphCount']}`",
        f"Mean x-axis support: `{summary['xAxisSupportMean']}`",
        f"Mean y-axis support: `{summary['yAxisSupportMean']}`",
        f"Annotated x-axis mean pixel error: `{summary['annotatedXAxisMeanPixelError']}`",
        f"Annotated y-axis mean pixel error: `{summary['annotatedYAxisMeanPixelError']}`",
        "",
        "R7 consumes R6 plotArea/layout candidates and DR-C4 manual-review tick/text annotations.",
        "It adds PC-side axis/frame/grid/label-band evidence and manual-review scale-fit scoring where annotation truth exists.",
        "It does not change Android runtime behavior, validators, report gates, chromatographic math, model policy, or CalculationEngine.",
        "",
        "Contact sheet: `benchmark/reports/r7_stage4_axis_frame_scale_candidate/contact_sheet_axis_frame_scale.png`",
        "",
        "## Fixture Results",
        "",
        "| Fixture | Graphs | Layout | Scale graphs | X support | Y support | X error | Y error | Overlay | Detail |",
        "|---|---:|---|---:|---:|---:|---:|---:|---|---|",
    ]
    for detail in summary["records"]:
        lines.append(
            "| `{fixtureId}` | {detectedGraphCount} | `{predictedLayoutClass}` | {scaleGraphs} | {xSupport} | {ySupport} | {xError} | {yError} | `{overlay}` | `{detail}` |".format(
                fixtureId=detail["fixtureId"],
                detectedGraphCount=detail["detectedGraphCount"],
                predictedLayoutClass=detail["predictedLayoutClass"],
                scaleGraphs=detail["manualReviewScaleGraphCount"],
                xSupport=detail["xAxisSupportMean"],
                ySupport=detail["yAxisSupportMean"],
                xError=detail["xAxisMeanPixelError"],
                yError=detail["yAxisMeanPixelError"],
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
    parser.add_argument("--clean", action="store_true", help="Remove previous R7 examples/report before writing.")
    args = parser.parse_args(argv)

    if args.clean:
        for path in [EXAMPLE_ROOT, REPORT_ROOT]:
            if path.exists():
                shutil.rmtree(path)

    r6_summary = load_r6_summary()
    annotations = load_tick_annotations()
    records = []
    details = []
    for r6_detail in r6_summary["records"]:
        fixture_id = r6_detail["fixtureId"]
        started = time.perf_counter()
        image_path = Path(r6_detail["sourceImage"])
        gray = np.array(Image.open(image_path).convert("L"))
        annotation_record = annotations.get(fixture_id)
        graph_evidence = []
        for index, candidate in enumerate(r6_detail.get("plotAreaCandidates", [])):
            graph_annotation = graph_annotation_by_order(annotation_record, index)
            graph_evidence.append(build_graph_evidence(gray, candidate, graph_annotation))
        overlay_path = render_overlay(
            image_path,
            graph_evidence,
            OVERLAY_ROOT / f"{fixture_id}_axis_frame_scale_overlay.png",
        )
        detail_path = str(DETAIL_ROOT / f"{fixture_id}_axis_frame_scale_detail.json").replace("\\", "/")
        r6_record = read_json(Path(r6_detail["recordPath"]))
        record, detail = build_record(r6_detail, r6_record, graph_evidence, overlay_path, detail_path)
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
        "R7 Stage 4 axis/frame/scale wrote "
        f"{len(records)} records to {EXAMPLE_ROOT.as_posix()} and report to {REPORT_ROOT.as_posix()}."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
