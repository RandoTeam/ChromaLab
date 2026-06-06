#!/usr/bin/env python3
"""Build R6 Stage 3 plotArea and graph-layout candidate records.

This is a PC-side shadow benchmark only. It consumes R5 graph discovery
evidence and partial manual graph-layout annotations, but it does not change
Android runtime, validators, report gates, graph-count metadata,
chromatographic math, model policy, or CalculationEngine.
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


ANDROID_METADATA_DIR = Path("composeApp/src/androidMain/assets/validation")
R5_REPORT_ROOT = Path("benchmark/reports/r5_stage2_graph_discovery_candidate")
R5_SUMMARY = R5_REPORT_ROOT / "summary.json"
ANNOTATION_PATH = Path("benchmark/annotations/drc3_initial_graph_layout_annotations/manual-p0-annotations.json")
EXAMPLE_ROOT = Path("benchmark/examples/r6_stage3_plotarea_layout_candidate")
REPORT_ROOT = Path("benchmark/reports/r6_stage3_plotarea_layout_candidate")
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


@dataclass(frozen=True)
class PlotAreaCandidate:
    graph_index: int
    graph_panel: Box
    plot_area: Box
    axis_row: int
    confidence: float
    status: str
    evidence_source: str
    rejection_reason: str


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


def load_metadata_by_fixture() -> dict[str, dict[str, Any]]:
    records = {}
    for path in sorted(ANDROID_METADATA_DIR.glob("*.metadata.json")):
        data = read_json(path)
        data["_metadataPath"] = str(path).replace("\\", "/")
        records[data["fixtureId"]] = data
    return records


def load_r5_summary() -> dict[str, Any]:
    if not R5_SUMMARY.exists():
        raise FileNotFoundError(f"R5 summary is required before R6 plotArea/layout: {R5_SUMMARY}")
    return read_json(R5_SUMMARY)


def load_annotations() -> dict[str, dict[str, Any]]:
    if not ANNOTATION_PATH.exists():
        return {}
    data = read_json(ANNOTATION_PATH)
    return {record["fixtureId"]: record for record in data.get("records", [])}


def resolve_image_path(metadata: dict[str, Any]) -> Path:
    image_path = Path(str(metadata["assetImagePath"]))
    if image_path.exists():
        return image_path
    return Path("composeApp/src/androidMain/assets") / image_path


def box_from_json(data: dict[str, Any]) -> Box:
    return Box(
        x=int(data["x"]),
        y=int(data["y"]),
        width=int(data["width"]),
        height=int(data["height"]),
    )


def box_to_json(box: Box) -> dict[str, int]:
    return {
        "x": box.x,
        "y": box.y,
        "width": box.width,
        "height": box.height,
    }


def longest_true_run(values: np.ndarray, min_gap_bridge: int = 0) -> tuple[int, int, int]:
    best_start = -1
    best_end = -1
    current_start = -1
    gap = 0
    last_true = -1
    for index, value in enumerate(values):
        if value:
            if current_start < 0:
                current_start = index
            last_true = index
            gap = 0
        elif current_start >= 0:
            gap += 1
            if gap > min_gap_bridge:
                if last_true - current_start > best_end - best_start:
                    best_start = current_start
                    best_end = last_true
                current_start = -1
                gap = 0
                last_true = -1
    if current_start >= 0 and last_true - current_start > best_end - best_start:
        best_start = current_start
        best_end = last_true
    if best_start < 0:
        return -1, -1, 0
    return best_start, best_end, best_end - best_start + 1


def classify_layout(image_width: int, image_height: int, detected_count: int, selected_rows: list[int]) -> str:
    if detected_count <= 0:
        return "UNKNOWN_REVIEW"
    if detected_count == 1:
        if image_height <= 240 or image_width * image_height < 80_000:
            return "LOW_RES_EXPORT_GRAPH"
        if image_width > image_height * 1.15:
            return "ROTATED_PAGE_GRAPH"
        return "SINGLE_TRACE_SINGLE_AXIS"
    rows = sorted(selected_rows)
    if detected_count == 2:
        if len(rows) >= 2:
            gap_ratio = (rows[1] - rows[0]) / max(1, image_height)
            second_ratio = rows[1] / max(1, image_height)
            if gap_ratio >= 0.38 or second_ratio >= 0.93:
                return "TWO_GRAPH_PAGE"
        return "MULTI_PANEL_SEPARATE_AXES"
    if detected_count == 4:
        first_ratio = rows[0] / max(1, image_height) if rows else 0.0
        gaps = [b - a for a, b in zip(rows, rows[1:])]
        median_gap_ratio = statistics.median(gaps) / max(1, image_height) if gaps else 0.0
        if first_ratio >= 0.35 and median_gap_ratio <= 0.22:
            return "TIC_PLUS_ION_PANELS"
        return "MULTI_PANEL_SEPARATE_AXES"
    return "MULTI_PANEL_SEPARATE_AXES"


def dark_mask_for_panel(gray: np.ndarray, panel: Box) -> np.ndarray:
    crop = gray[panel.y : panel.y2, panel.x : panel.x2]
    if crop.size == 0:
        return np.zeros((1, 1), dtype=bool)
    threshold = min(175.0, max(55.0, float(np.percentile(crop, 35))))
    return crop < threshold


def infer_axis_run(gray: np.ndarray, panel: Box, axis_row: int) -> tuple[int | None, int | None, float]:
    axis_local = max(0, min(panel.height - 1, axis_row - panel.y))
    band_y0 = max(0, axis_local - 5)
    band_y1 = min(panel.height, axis_local + 6)
    mask = dark_mask_for_panel(gray, panel)
    band = mask[band_y0:band_y1, :]
    if band.size == 0:
        return None, None, 0.0
    col_hit = band.mean(axis=0) > 0.18
    start, end, length = longest_true_run(col_hit, min_gap_bridge=10)
    if length < panel.width * 0.24:
        return None, None, 0.0
    confidence = min(1.0, length / max(1, panel.width) * 1.15)
    return panel.x + start, panel.x + end, confidence


def infer_y_axis(gray: np.ndarray, panel: Box, axis_row: int, x_hint: int) -> tuple[int | None, int | None, float]:
    mask = dark_mask_for_panel(gray, panel)
    axis_local = max(0, min(panel.height - 1, axis_row - panel.y))
    x_local = max(0, min(panel.width - 1, x_hint - panel.x))
    x0 = max(0, x_local - 45)
    x1 = min(panel.width, x_local + 46)
    y0 = max(0, axis_local - int(panel.height * 0.75))
    y1 = min(panel.height, axis_local + 1)
    window = mask[y0:y1, x0:x1]
    if window.size == 0:
        return None, None, 0.0

    best_column = None
    best_start = None
    best_len = 0
    for local_col in range(window.shape[1]):
        start, _end, length = longest_true_run(window[:, local_col], min_gap_bridge=4)
        if length > best_len:
            best_len = length
            best_start = start
            best_column = local_col
    if best_column is None or best_start is None or best_len < max(16, panel.height * 0.12):
        return None, None, 0.0
    y_top = panel.y + y0 + best_start
    x_axis = panel.x + x0 + best_column
    confidence = min(1.0, best_len / max(1, panel.height) * 1.5)
    return x_axis, y_top, confidence


def infer_plot_area(image_path: Path, panel_payload: dict[str, Any]) -> PlotAreaCandidate:
    image = Image.open(image_path).convert("L")
    gray = np.array(image)
    image_height, image_width = gray.shape
    graph_panel = box_from_json(panel_payload["graphPanel"]).clamp(image_width, image_height)
    axis_row = int(panel_payload["axisRow"])
    x_run_start, x_run_end, x_confidence = infer_axis_run(gray, graph_panel, axis_row)
    fallback_x0 = graph_panel.x + int(graph_panel.width * 0.10)
    fallback_x1 = graph_panel.x + int(graph_panel.width * 0.96)
    plot_x0 = x_run_start if x_run_start is not None else fallback_x0
    plot_x1 = x_run_end if x_run_end is not None else fallback_x1

    y_axis_x, y_axis_top, y_confidence = infer_y_axis(gray, graph_panel, axis_row, plot_x0)
    if y_axis_x is not None and y_axis_top is not None:
        plot_x0 = min(plot_x0, y_axis_x)
        plot_y0 = y_axis_top
    else:
        plot_y0 = max(graph_panel.y, axis_row - int(graph_panel.height * 0.58))

    plot_y1 = max(plot_y0 + 1, min(graph_panel.y2, axis_row))
    plot_box = Box(
        x=plot_x0,
        y=plot_y0,
        width=max(1, plot_x1 - plot_x0),
        height=max(1, plot_y1 - plot_y0),
    ).clamp(image_width, image_height)
    confidence = round(min(1.0, 0.22 + x_confidence * 0.45 + y_confidence * 0.33), 4)
    status = "REVIEW"
    if x_run_start is None and y_axis_x is None:
        evidence_source = "PANEL_MARGIN_FALLBACK"
        rejection_reason = "AXIS_RUN_NOT_STABLE_ENOUGH_FOR_AUTONOMOUS_PROMOTION"
    elif x_run_start is None:
        evidence_source = "Y_AXIS_WITH_PANEL_MARGIN_FALLBACK"
        rejection_reason = "X_AXIS_RUN_NOT_STABLE_ENOUGH_FOR_AUTONOMOUS_PROMOTION"
    elif y_axis_x is None:
        evidence_source = "X_AXIS_RUN_WITH_PANEL_MARGIN_FALLBACK"
        rejection_reason = "Y_AXIS_RUN_NOT_STABLE_ENOUGH_FOR_AUTONOMOUS_PROMOTION"
    else:
        evidence_source = "AXIS_RUN_PROJECTION"
        rejection_reason = ""
    return PlotAreaCandidate(
        graph_index=int(panel_payload["graphIndex"]),
        graph_panel=graph_panel,
        plot_area=plot_box,
        axis_row=axis_row,
        confidence=confidence,
        status=status,
        evidence_source=evidence_source,
        rejection_reason=rejection_reason,
    )


def iou(left: Box, right: Box) -> float:
    ix0 = max(left.x, right.x)
    iy0 = max(left.y, right.y)
    ix1 = min(left.x2, right.x2)
    iy1 = min(left.y2, right.y2)
    if ix1 <= ix0 or iy1 <= iy0:
        return 0.0
    intersection = (ix1 - ix0) * (iy1 - iy0)
    left_area = left.width * left.height
    right_area = right.width * right.height
    union = left_area + right_area - intersection
    return float(intersection / union) if union else 0.0


def score_against_annotation(
    candidates: list[PlotAreaCandidate],
    annotation: dict[str, Any] | None,
) -> dict[str, Any]:
    if not annotation:
        return {
            "truthAvailable": False,
            "graphPanelMeanIou": None,
            "plotAreaMeanIou": None,
            "pairs": [],
        }
    graphs = annotation.get("graphs", [])
    pairs = []
    graph_ious = []
    plot_ious = []
    for index, candidate in enumerate(candidates):
        if index >= len(graphs):
            break
        graph = graphs[index]
        truth_graph = box_from_json(graph["graphPanel"])
        truth_plot = box_from_json(graph["plotArea"])
        graph_iou = iou(candidate.graph_panel, truth_graph)
        plot_iou = iou(candidate.plot_area, truth_plot)
        graph_ious.append(graph_iou)
        plot_ious.append(plot_iou)
        pairs.append(
            {
                "graphIndex": candidate.graph_index,
                "truthGraphId": graph.get("graphId", ""),
                "graphPanelIou": round(graph_iou, 6),
                "plotAreaIou": round(plot_iou, 6),
                "truthGraphPanel": box_to_json(truth_graph),
                "truthPlotArea": box_to_json(truth_plot),
            }
        )
    return {
        "truthAvailable": True,
        "graphPanelMeanIou": round(float(statistics.mean(graph_ious)), 6) if graph_ious else 0.0,
        "plotAreaMeanIou": round(float(statistics.mean(plot_ious)), 6) if plot_ious else 0.0,
        "pairs": pairs,
    }


def candidate_to_json(candidate: PlotAreaCandidate) -> dict[str, Any]:
    return {
        "graphIndex": candidate.graph_index,
        "graphPanel": box_to_json(candidate.graph_panel),
        "plotArea": box_to_json(candidate.plot_area),
        "axisRow": candidate.axis_row,
        "confidence": candidate.confidence,
        "status": candidate.status,
        "evidenceSource": candidate.evidence_source,
        "rejectionReason": candidate.rejection_reason,
    }


def draw_label(draw: ImageDraw.ImageDraw, xy: tuple[int, int], text: str, fill: tuple[int, int, int]) -> None:
    try:
        font = ImageFont.load_default()
    except OSError:
        font = None
    draw.text(xy, text, fill=fill, font=font)


def render_overlay(
    image_path: Path,
    candidates: list[PlotAreaCandidate],
    annotation: dict[str, Any] | None,
    out: Path,
) -> str:
    image = Image.open(image_path).convert("RGB")
    draw = ImageDraw.Draw(image)

    if annotation:
        for graph in annotation.get("graphs", []):
            truth_panel = box_from_json(graph["graphPanel"])
            truth_plot = box_from_json(graph["plotArea"])
            draw.rectangle((truth_panel.x, truth_panel.y, truth_panel.x2, truth_panel.y2), outline=(255, 0, 220), width=3)
            draw.rectangle((truth_plot.x, truth_plot.y, truth_plot.x2, truth_plot.y2), outline=(120, 120, 255), width=3)

    for candidate in candidates:
        panel = candidate.graph_panel
        plot = candidate.plot_area
        draw.rectangle((panel.x, panel.y, panel.x2, panel.y2), outline=(0, 210, 90), width=4)
        draw.rectangle((plot.x, plot.y, plot.x2, plot.y2), outline=(0, 220, 255), width=4)
        draw.line((panel.x, candidate.axis_row, panel.x2, candidate.axis_row), fill=(255, 190, 0), width=3)
        draw_label(draw, (panel.x + 6, panel.y + 6), f"G{candidate.graph_index} {candidate.evidence_source}", (0, 210, 90))

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
        draw_label(draw, (8, 264), f"{detail['predictedLayoutClass']} / IoU {detail['plotAreaMeanIouText']}", (0, 0, 0))
        tiles.append(canvas)
    columns = 4
    tile_w, tile_h = 340, 280
    rows = math.ceil(len(tiles) / columns)
    sheet = Image.new("RGB", (columns * tile_w, rows * tile_h), "white")
    for index, tile in enumerate(tiles):
        sheet.paste(tile, ((index % columns) * tile_w, (index // columns) * tile_h))
    out = REPORT_ROOT / "contact_sheet_plotarea_layout.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    return str(out).replace("\\", "/")


def find_r5_record_path(r5_detail: dict[str, Any]) -> Path:
    return Path(str(r5_detail["recordPath"]))


def build_record(
    metadata: dict[str, Any],
    r5_detail: dict[str, Any],
    r5_record: dict[str, Any],
    candidates: list[PlotAreaCandidate],
    annotation: dict[str, Any] | None,
    overlay_path: str,
    detail_path: str,
) -> tuple[dict[str, Any], dict[str, Any]]:
    fixture_id = metadata["fixtureId"]
    image_path = resolve_image_path(metadata)
    with Image.open(image_path) as image:
        image_width, image_height = image.size
    expected_count = int(r5_detail["expectedGraphCount"])
    detected_count = int(r5_detail["detectedGraphCount"])
    selected_rows = [int(row) for row in r5_detail.get("selectedRows", [])]
    expected_layout = str(r5_detail["expectedLayoutClass"])
    predicted_layout = classify_layout(image_width, image_height, detected_count, selected_rows)
    layout_score = "PASS" if expected_layout == predicted_layout else "FAIL"
    graph_count_score = str(r5_detail["graphCountScore"])
    score = score_against_annotation(candidates, annotation)
    truth_available = bool(score["truthAvailable"])
    graph_iou_value = score["graphPanelMeanIou"]
    plot_iou_value = score["plotAreaMeanIou"]
    graph_iou_metric = float(graph_iou_value) if graph_iou_value is not None else -1.0
    plot_iou_metric = float(plot_iou_value) if plot_iou_value is not None else -1.0
    plotarea_status = "REVIEW"
    first_failing_stage = ""
    if graph_count_score != "PASS":
        plotarea_status = "FAIL"
        first_failing_stage = "GRAPH_DISCOVERY"
    elif layout_score != "PASS":
        plotarea_status = "FAIL"
        first_failing_stage = "PLOTAREA_LAYOUT_CLASSIFICATION"

    record_id = f"stage123_{fixture_id}_pc_stage3_plotarea_layout_candidate_v1"
    record = {
        "schemaVersion": "chromalab.benchmark.stage123_parity_record.v1",
        "recordId": record_id,
        "fixtureId": fixture_id,
        "mode": "pc_prototype",
        "sourceId": "r6_stage3_plotarea_layout_candidate_v1",
        "sourceKind": "PC_STAGE3_PLOTAREA_LAYOUT_CANDIDATE",
        "productionImpact": "NONE_SHADOW_ONLY",
        "runtimeReadiness": "PC_STAGE3_PLOTAREA_LAYOUT_CANDIDATE_NOT_RUNTIME_READY",
        "expectedGraphCount": expected_count,
        "detectedGraphCount": detected_count,
        "graphCountScore": graph_count_score,
        "expectedLayoutClass": expected_layout,
        "predictedLayoutClass": predicted_layout,
        "layoutClassScore": layout_score,
        "imagePreparation": r5_record["imagePreparation"],
        "graphDiscovery": r5_record["graphDiscovery"],
        "plotAreaLayout": {
            "status": plotarea_status,
            "available": True,
            "summary": "Builds deterministic plotArea candidates from R5 graphPanel candidates and axis-run projection, then scores semantic layout class. This remains shadow-only.",
            "metrics": {
                "plotAreaCandidateCount": len(candidates),
                "annotatedTruthAvailable": truth_available,
                "graphPanelMeanIou": round(graph_iou_metric, 6),
                "plotAreaMeanIou": round(plot_iou_metric, 6),
                "predictedLayoutMatchesExpected": layout_score == "PASS",
                "selectedRowsCount": len(selected_rows),
            },
        },
        "failureClass": "" if not first_failing_stage else "PLOTAREA_LAYOUT_REVIEW",
        "firstFailingStage": first_failing_stage,
        "evidence": {
            "sourceImage": str(image_path).replace("\\", "/"),
            "recordSource": str(REPORT_ROOT / "summary.json").replace("\\", "/"),
            "artifactPaths": [
                overlay_path,
                detail_path,
                r5_detail["overlayPath"],
                r5_detail["recordPath"],
            ],
        },
        "promotionDecision": "STAGE3_CANDIDATE_REQUIRES_AXIS_SCALE_PARITY",
        "notes": [
            "Shadow-only R6 Stage 3 plotArea/layout candidate.",
            "No calibration, trace, peak, report-gate, Android runtime, or CalculationEngine authority.",
            "plotArea candidates are measured evidence for the next axis/scale stage, not production graph ROI output.",
            "Manual annotation IoU is only available for the partial P0 annotation set.",
        ],
    }
    detail = {
        "fixtureId": fixture_id,
        "sourceImage": str(image_path).replace("\\", "/"),
        "expectedGraphCount": expected_count,
        "detectedGraphCount": detected_count,
        "graphCountScore": graph_count_score,
        "expectedLayoutClass": expected_layout,
        "predictedLayoutClass": predicted_layout,
        "layoutClassScore": layout_score,
        "plotAreaLayoutStatus": plotarea_status,
        "selectedRows": selected_rows,
        "plotAreaCandidates": [candidate_to_json(candidate) for candidate in candidates],
        "annotationTruthAvailable": truth_available,
        "graphPanelMeanIou": graph_iou_value,
        "plotAreaMeanIou": plot_iou_value,
        "plotAreaMeanIouText": "-" if plot_iou_value is None else f"{float(plot_iou_value):.3f}",
        "annotationPairs": score["pairs"],
        "overlayPath": overlay_path,
        "detailPath": detail_path,
        "recordPath": str(EXAMPLE_ROOT / record_id / "stage123-parity-record.json").replace("\\", "/"),
        "r5OverlayPath": r5_detail["overlayPath"],
        "r5RecordPath": r5_detail["recordPath"],
        "promotionDecision": record["promotionDecision"],
    }
    return record, detail


def write_examples(records: list[dict[str, Any]]) -> None:
    EXAMPLE_ROOT.mkdir(parents=True, exist_ok=True)
    for record in records:
        write_json(EXAMPLE_ROOT / record["recordId"] / "stage123-parity-record.json", record)


def build_summary(details: list[dict[str, Any]]) -> dict[str, Any]:
    graph_pass = sum(1 for detail in details if detail["graphCountScore"] == "PASS")
    layout_pass = sum(1 for detail in details if detail["layoutClassScore"] == "PASS")
    truth_count = sum(1 for detail in details if detail["annotationTruthAvailable"])
    plot_ious = [float(detail["plotAreaMeanIou"]) for detail in details if detail["plotAreaMeanIou"] is not None]
    graph_ious = [float(detail["graphPanelMeanIou"]) for detail in details if detail["graphPanelMeanIou"] is not None]
    status_counts: dict[str, int] = {}
    for detail in details:
        status = detail["plotAreaLayoutStatus"]
        status_counts[status] = status_counts.get(status, 0) + 1
    verdict = (
        "R6_STAGE3_LAYOUT_READY_PLOTAREA_REVIEW"
        if graph_pass == len(details) and layout_pass == len(details)
        else "R6_STAGE3_PLOTAREA_LAYOUT_CANDIDATE_REVIEW"
    )
    return {
        "schemaVersion": "chromalab.benchmark.r6_stage3_plotarea_layout_candidate_summary.v1",
        "productionImpact": "NONE_SHADOW_ONLY",
        "overallVerdict": verdict,
        "recordCount": len(details),
        "fixtureCount": len({detail["fixtureId"] for detail in details}),
        "graphCountPass": graph_pass,
        "graphCountFail": len(details) - graph_pass,
        "layoutClassPass": layout_pass,
        "layoutClassFail": len(details) - layout_pass,
        "annotatedTruthFixtureCount": truth_count,
        "annotatedGraphPanelMeanIou": round(float(statistics.mean(graph_ious)), 6) if graph_ious else None,
        "annotatedPlotAreaMeanIou": round(float(statistics.mean(plot_ious)), 6) if plot_ious else None,
        "statusCounts": status_counts,
        "nextRequiredWork": [
            "Keep R6 plotArea/layout shadow-only until axis/scale evidence is measured.",
            "Use R6 candidates to drive Stage 4 axis and scale evidence, not Android runtime promotion.",
            "Improve plotArea localization for photo/page cases before production integration.",
            "Do not treat layout pass as calibration, trace, peak, or report readiness.",
        ],
        "records": details,
    }


def write_markdown(summary: dict[str, Any]) -> None:
    lines = [
        "# R6 Stage 3 PlotArea And Layout Candidate",
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
        f"Annotated graphPanel mean IoU: `{summary['annotatedGraphPanelMeanIou']}`",
        f"Annotated plotArea mean IoU: `{summary['annotatedPlotAreaMeanIou']}`",
        "",
        "R6 consumes R5 graphPanel candidates and partial manual P0 layout annotations.",
        "It adds PC-side plotArea candidates and graph-layout classification. It does not change Android runtime behavior, validators, report gates, chromatographic math, model policy, or CalculationEngine.",
        "",
        "Contact sheet: `benchmark/reports/r6_stage3_plotarea_layout_candidate/contact_sheet_plotarea_layout.png`",
        "",
        "## Fixture Results",
        "",
        "| Fixture | Expected graphs | Detected graphs | Layout | Layout score | PlotArea IoU | Overlay | Detail |",
        "|---|---:|---:|---|---|---:|---|---|",
    ]
    for detail in summary["records"]:
        lines.append(
            "| `{fixtureId}` | {expectedGraphCount} | {detectedGraphCount} | `{predictedLayoutClass}` | {layoutClassScore} | {plotIou} | `{overlay}` | `{detail}` |".format(
                fixtureId=detail["fixtureId"],
                expectedGraphCount=detail["expectedGraphCount"],
                detectedGraphCount=detail["detectedGraphCount"],
                predictedLayoutClass=detail["predictedLayoutClass"],
                layoutClassScore=detail["layoutClassScore"],
                plotIou=detail["plotAreaMeanIouText"],
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
    parser.add_argument("--clean", action="store_true", help="Remove previous R6 examples/report before writing.")
    args = parser.parse_args(argv)

    if args.clean:
        for path in [EXAMPLE_ROOT, REPORT_ROOT]:
            if path.exists():
                shutil.rmtree(path)

    metadata_by_fixture = load_metadata_by_fixture()
    r5_summary = load_r5_summary()
    annotations = load_annotations()

    records = []
    details = []
    for r5_detail in r5_summary["records"]:
        fixture_id = r5_detail["fixtureId"]
        metadata = metadata_by_fixture[fixture_id]
        image_path = resolve_image_path(metadata)
        started = time.perf_counter()
        candidates = [infer_plot_area(image_path, panel) for panel in r5_detail.get("graphPanelCandidates", [])]
        annotation = annotations.get(fixture_id)
        overlay_path = render_overlay(
            image_path,
            candidates,
            annotation,
            OVERLAY_ROOT / f"{fixture_id}_plotarea_layout_overlay.png",
        )
        detail_path = str(DETAIL_ROOT / f"{fixture_id}_plotarea_layout_detail.json").replace("\\", "/")
        r5_record = read_json(find_r5_record_path(r5_detail))
        record, detail = build_record(metadata, r5_detail, r5_record, candidates, annotation, overlay_path, detail_path)
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
        "R6 Stage 3 plotArea/layout wrote "
        f"{len(records)} records to {EXAMPLE_ROOT.as_posix()} and report to {REPORT_ROOT.as_posix()}."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
