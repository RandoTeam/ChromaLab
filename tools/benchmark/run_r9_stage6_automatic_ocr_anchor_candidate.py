#!/usr/bin/env python3
"""Build R9 Stage 6 automatic OCR anchor candidate records.

This is a PC-side shadow benchmark only. It consumes R8 calibration strategy
parity records plus DRD/DRE OCR reports. It does not change Android runtime,
validators, report gates, chromatographic math, model policy, or
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


R8_REPORT_ROOT = Path("benchmark/reports/r8_stage5_calibration_strategy_parity_candidate")
R8_SUMMARY = R8_REPORT_ROOT / "summary.json"
DRD2_SUMMARY = Path("benchmark/reports/drd2_rapidocr_crop_benchmark/summary.json")
DRD4_SUMMARY = Path("benchmark/reports/drd4_second_ocr_text_box_benchmark/summary.json")
DRD6_SUMMARY = Path("benchmark/reports/drd6_axis_owned_ocr_preprocessing_grid/summary.json")
DRE6_SUMMARY = Path("benchmark/reports/dre6_remaining_axis_ocr_recovery/summary.json")
DRC4_SUMMARY = Path("benchmark/annotations/drc4_tick_text_role_annotations/summary.json")
EXAMPLE_ROOT = Path("benchmark/examples/r9_stage6_automatic_ocr_anchor_candidate")
REPORT_ROOT = Path("benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate")
DETAIL_ROOT = REPORT_ROOT / "details"
OVERLAY_ROOT = REPORT_ROOT / "overlays"


def read_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def read_json_if_exists(path: Path) -> Any | None:
    return read_json(path) if path.exists() else None


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
        raise FileNotFoundError(f"{name} summary is required before R9: {path}")
    return read_json(path)


def graph_index_from_id(graph_id: str) -> int:
    suffix = graph_id.rsplit("_graph_", 1)[-1]
    return int(suffix)


def index_dre6_graphs(dre6_summary: dict[str, Any] | None) -> dict[tuple[str, int], dict[str, Any]]:
    indexed: dict[tuple[str, int], dict[str, Any]] = {}
    if not dre6_summary:
        return indexed
    for graph in dre6_summary.get("graphResults", []):
        indexed[(graph["fixtureId"], graph_index_from_id(graph["graphId"]))] = graph
    return indexed


def drc4_record_by_fixture(drc4_summary: dict[str, Any] | None) -> dict[str, dict[str, Any]]:
    if not drc4_summary:
        return {}
    return {record["fixtureId"]: record for record in drc4_summary.get("records", [])}


def best_drd4_fixture_summary(drd4_summary: dict[str, Any] | None, fixture_id: str) -> dict[str, Any] | None:
    if not drd4_summary:
        return None
    candidates = [
        item
        for item in drd4_summary.get("fixtureSummaries", [])
        if item.get("fixtureId") == fixture_id
    ]
    if not candidates:
        return None
    return max(
        candidates,
        key=lambda item: (
            float(item.get("boxRecall", 0.0)),
            float(item.get("safeRoleAccuracyMatched", 0.0)),
            -int(item.get("safeFalseTickLabelCount", 999)),
        ),
    )


def best_drd6_variant_summary(drd6_summary: dict[str, Any] | None) -> dict[str, Any] | None:
    if not drd6_summary:
        return None
    variants = drd6_summary.get("variantSummaries", [])
    if not variants:
        return None
    return max(
        variants,
        key=lambda item: (
            float(item.get("tickLabelRecall", 0.0)),
            float(item.get("safeRoleAccuracyMatched", 0.0)),
            -int(item.get("safeFalseTickLabelCount", 999)),
        ),
    )


def axis_summary(axis: dict[str, Any] | None) -> dict[str, Any]:
    if not axis:
        return {
            "status": "MISSING",
            "selectedStrategyId": "",
            "acceptedAnchorCount": 0,
            "rejectedAnchorCount": 0,
            "rmsePx": None,
            "maxAbsResidualPx": None,
            "truthTickRmsePx": None,
            "truthTickMaxAbsResidualPx": None,
            "missingSafeOcrLabelCount": 0,
            "selectionReason": "",
            "failureReason": "NO_DRE6_AXIS_RESULT",
            "anchorSourceCounts": {},
            "acceptedAnchors": [],
            "rejectedAnchors": [],
        }
    candidate = axis.get("selectedCandidate") or {}
    source_counts: dict[str, int] = {}
    for anchor in candidate.get("acceptedAnchors", []):
        source = str(anchor.get("anchorSource", "UNKNOWN"))
        source_counts[source] = source_counts.get(source, 0) + 1
    return {
        "status": axis.get("status", "MISSING"),
        "selectedStrategyId": axis.get("selectedStrategyId", ""),
        "acceptedAnchorCount": int(candidate.get("acceptedAnchorCount", 0)),
        "rejectedAnchorCount": int(candidate.get("rejectedAnchorCount", 0)),
        "rmsePx": candidate.get("rmsePx"),
        "maxAbsResidualPx": candidate.get("maxAbsResidualPx"),
        "truthTickRmsePx": candidate.get("truthTickRmsePx"),
        "truthTickMaxAbsResidualPx": candidate.get("truthTickMaxAbsResidualPx"),
        "missingSafeOcrLabelCount": int(axis.get("missingSafeOcrLabelCount", 0) or 0),
        "selectionReason": axis.get("selectionReason", ""),
        "failureReason": axis.get("failureReason"),
        "anchorSourceCounts": source_counts,
        "acceptedAnchors": candidate.get("acceptedAnchors", []),
        "rejectedAnchors": candidate.get("rejectedAnchors", []),
    }


def build_graph_result(
    fixture_id: str,
    graph: dict[str, Any],
    dre6_graph: dict[str, Any] | None,
) -> dict[str, Any]:
    graph_index = int(graph["graphIndex"])
    if not dre6_graph:
        return {
            "graphIndex": graph_index,
            "graphId": f"{fixture_id}_graph_{graph_index}",
            "plotArea": graph["plotArea"],
            "automaticOcrAvailable": False,
            "automaticOcrStatus": "MISSING",
            "decision": "MISSING",
            "decisionReason": "NO_DRE6_AUTOMATIC_OCR_ANCHOR_RESULT_FOR_GRAPH",
            "xAxis": axis_summary(None),
            "yAxis": axis_summary(None),
            "acceptedAnchorCount": 0,
            "reviewAnchorCount": 0,
            "rejectedAnchorCount": 0,
        }
    x_axis = axis_summary(dre6_graph.get("xAxis"))
    y_axis = axis_summary(dre6_graph.get("yAxis"))
    accepted = int(x_axis["acceptedAnchorCount"]) + int(y_axis["acceptedAnchorCount"])
    rejected = int(x_axis["rejectedAnchorCount"]) + int(y_axis["rejectedAnchorCount"])
    decision = dre6_graph.get("dre6Decision", "REVIEW")
    return {
        "graphIndex": graph_index,
        "graphId": dre6_graph["graphId"],
        "plotArea": graph["plotArea"],
        "automaticOcrAvailable": True,
        "automaticOcrStatus": decision,
        "decision": decision,
        "decisionReason": dre6_graph.get("decisionReason", ""),
        "xAxis": x_axis,
        "yAxis": y_axis,
        "acceptedAnchorCount": accepted,
        "reviewAnchorCount": accepted if decision == "REVIEW" else 0,
        "rejectedAnchorCount": rejected,
    }


def summarize_graph_results(graph_results: list[dict[str, Any]]) -> dict[str, Any]:
    available_graphs = [graph for graph in graph_results if graph["automaticOcrAvailable"]]
    valid_graphs = [graph for graph in available_graphs if graph["automaticOcrStatus"] == "VALID"]
    review_graphs = [graph for graph in available_graphs if graph["automaticOcrStatus"] == "REVIEW"]
    accepted_anchor_count = sum(int(graph["acceptedAnchorCount"]) for graph in available_graphs)
    rejected_anchor_count = sum(int(graph["rejectedAnchorCount"]) for graph in available_graphs)
    axis_rmses = []
    truth_rmses = []
    for graph in available_graphs:
        for axis_name in ["xAxis", "yAxis"]:
            axis = graph[axis_name]
            if axis["rmsePx"] is not None:
                axis_rmses.append(float(axis["rmsePx"]))
            if axis["truthTickRmsePx"] is not None:
                truth_rmses.append(float(axis["truthTickRmsePx"]))
    return {
        "graphCount": len(graph_results),
        "automaticCandidateGraphCount": len(available_graphs),
        "validCandidateGraphCount": len(valid_graphs),
        "reviewCandidateGraphCount": len(review_graphs),
        "acceptedAnchorCount": accepted_anchor_count,
        "rejectedAnchorCount": rejected_anchor_count,
        "meanFitRmsePx": round(float(statistics.mean(axis_rmses)), 6) if axis_rmses else -1.0,
        "meanTruthTickRmsePx": round(float(statistics.mean(truth_rmses)), 6) if truth_rmses else -1.0,
    }


def draw_label(draw: ImageDraw.ImageDraw, xy: tuple[int, int], text: str, fill: tuple[int, int, int]) -> None:
    try:
        font = ImageFont.load_default()
    except OSError:
        font = None
    draw.text(xy, text, fill=fill, font=font)


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
        color = (0, 150, 90)
        draw.line((x, int(y0), x, y), fill=color, width=1)
        draw.ellipse((x - 3, y - 3, x + 3, y + 3), outline=color, width=2)
        draw_label(draw, (x + 4, y - 14), text, color)
    else:
        x = int(round(x0))
        y = int(round(pixel_coordinate))
        color = (255, 125, 0)
        draw.line((x, y, int(x1), y), fill=color, width=1)
        draw.ellipse((x - 3, y - 3, x + 3, y + 3), outline=color, width=2)
        draw_label(draw, (x + 4, y + 4), text, color)


def render_overlay(image_path: Path, graph_results: list[dict[str, Any]], out: Path) -> str:
    image = Image.open(image_path).convert("RGB")
    draw = ImageDraw.Draw(image)
    for graph in graph_results:
        plot = graph["plotArea"]
        x0 = int(plot["x"])
        y0 = int(plot["y"])
        x1 = x0 + int(plot["width"])
        y1 = y0 + int(plot["height"])
        color = (0, 170, 255) if graph["automaticOcrAvailable"] else (220, 40, 40)
        draw.rectangle((x0, y0, x1, y1), outline=color, width=3)
        draw_label(
            draw,
            (x0 + 4, y0 + 4),
            f"G{graph['graphIndex']} {graph['automaticOcrStatus']} anchors={graph['acceptedAnchorCount']}",
            color,
        )
        if not graph["automaticOcrAvailable"]:
            continue
        for axis_name, axis_key in [("X", "xAxis"), ("Y", "yAxis")]:
            for anchor in graph[axis_key]["acceptedAnchors"]:
                label = str(anchor.get("ocrText", ""))[:12]
                draw_axis_anchor(draw, plot, axis_name, float(anchor["pixelCoordinate"]), label)
    out.parent.mkdir(parents=True, exist_ok=True)
    image.save(out)
    return path_string(out)


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
            f"{detail['ocrAnchorCandidateStatus']} graphs={detail['automaticCandidateGraphCount']}",
            (0, 0, 0),
        )
        draw_label(draw, (8, 278), f"anchors={detail['acceptedAnchorCount']}", (0, 0, 0))
        tiles.append(canvas)
    columns = 4
    tile_w, tile_h = 340, 290
    rows = math.ceil(len(tiles) / columns)
    sheet = Image.new("RGB", (columns * tile_w, rows * tile_h), "white")
    for index, tile in enumerate(tiles):
        sheet.paste(tile, ((index % columns) * tile_w, (index // columns) * tile_h))
    out = REPORT_ROOT / "contact_sheet_automatic_ocr_anchor_candidate.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    return path_string(out)


def fixture_status(metrics: dict[str, Any], has_truth: bool) -> str:
    if metrics["automaticCandidateGraphCount"] > 0:
        return "REVIEW"
    return "MISSING" if not has_truth else "REVIEW"


def build_record(
    r8_detail: dict[str, Any],
    r8_record: dict[str, Any],
    drc4_record: dict[str, Any] | None,
    drd4_fixture: dict[str, Any] | None,
    graph_results: list[dict[str, Any]],
    overlay_path: str,
    detail_path: str,
) -> tuple[dict[str, Any], dict[str, Any]]:
    fixture_id = r8_detail["fixtureId"]
    metrics = summarize_graph_results(graph_results)
    has_truth = drc4_record is not None
    status = fixture_status(metrics, has_truth)
    summary = (
        "Consumes DRE6 PC-side safe OCR anchors as automatic anchor evidence. "
        "This remains shadow-only and is not Android runtime calibration authority."
    )
    record_id = f"stage123456_{fixture_id}_pc_stage6_automatic_ocr_anchor_candidate_v1"
    artifact_paths = [
        overlay_path,
        detail_path,
        r8_detail["overlayPath"],
        r8_detail["recordPath"],
        path_string(DRE6_SUMMARY),
        path_string(DRD4_SUMMARY),
        path_string(DRD6_SUMMARY),
    ]
    record = {
        "schemaVersion": "chromalab.benchmark.stage123456_parity_record.v1",
        "recordId": record_id,
        "fixtureId": fixture_id,
        "mode": "pc_prototype",
        "sourceId": "r9_stage6_automatic_ocr_anchor_candidate_v1",
        "sourceKind": "PC_STAGE6_AUTOMATIC_OCR_ANCHOR_CANDIDATE",
        "productionImpact": "NONE_SHADOW_ONLY",
        "runtimeReadiness": "PC_STAGE6_AUTOMATIC_OCR_ANCHOR_CANDIDATE_NOT_ANDROID_RUNTIME_READY",
        "expectedGraphCount": int(r8_detail["expectedGraphCount"]),
        "detectedGraphCount": int(r8_detail["detectedGraphCount"]),
        "graphCountScore": r8_detail["graphCountScore"],
        "expectedLayoutClass": r8_detail["expectedLayoutClass"],
        "predictedLayoutClass": r8_detail["predictedLayoutClass"],
        "layoutClassScore": r8_detail["layoutClassScore"],
        "imagePreparation": r8_record["imagePreparation"],
        "graphDiscovery": r8_record["graphDiscovery"],
        "plotAreaLayout": r8_record["plotAreaLayout"],
        "axisFrameScale": r8_record["axisFrameScale"],
        "calibrationStrategy": r8_record["calibrationStrategy"],
        "ocrAnchorCandidate": {
            "status": status,
            "available": metrics["automaticCandidateGraphCount"] > 0,
            "summary": summary,
            "metrics": {
                "graphCount": metrics["graphCount"],
                "automaticCandidateGraphCount": metrics["automaticCandidateGraphCount"],
                "validCandidateGraphCount": metrics["validCandidateGraphCount"],
                "reviewCandidateGraphCount": metrics["reviewCandidateGraphCount"],
                "acceptedAnchorCount": metrics["acceptedAnchorCount"],
                "rejectedAnchorCount": metrics["rejectedAnchorCount"],
                "meanFitRmsePx": metrics["meanFitRmsePx"],
                "meanTruthTickRmsePx": metrics["meanTruthTickRmsePx"],
                "drd4BestBoxRecall": float(drd4_fixture.get("boxRecall", -1.0)) if drd4_fixture else -1.0,
                "drd4BestSafeRoleAccuracy": float(drd4_fixture.get("safeRoleAccuracyMatched", -1.0)) if drd4_fixture else -1.0,
            },
        },
        "failureClass": "",
        "firstFailingStage": "",
        "evidence": {
            "sourceImage": r8_detail["sourceImage"],
            "recordSource": path_string(REPORT_ROOT / "summary.json"),
            "artifactPaths": artifact_paths,
        },
        "promotionDecision": "STAGE6_CANDIDATE_REQUIRES_ANDROID_OCR_ANCHOR_PARITY",
        "notes": [
            "Shadow-only R9 Stage 6 automatic OCR anchor candidate.",
            "DRE6 safe OCR anchors are PC-side evidence only and are not Android runtime calibration evidence.",
            "Accepted anchors come from OCR label boxes with pixel geometry; no VLM pixel coordinates are used.",
            "Fixtures without DRC4/DRE6 OCR truth remain unscored instead of fabricated.",
            "No Android runtime, trace, peak, report-gate, or CalculationEngine authority.",
        ],
    }
    detail = {
        "fixtureId": fixture_id,
        "sourceImage": r8_detail["sourceImage"],
        "expectedGraphCount": int(r8_detail["expectedGraphCount"]),
        "detectedGraphCount": int(r8_detail["detectedGraphCount"]),
        "graphCountScore": r8_detail["graphCountScore"],
        "expectedLayoutClass": r8_detail["expectedLayoutClass"],
        "predictedLayoutClass": r8_detail["predictedLayoutClass"],
        "layoutClassScore": r8_detail["layoutClassScore"],
        "drc4TruthAvailable": has_truth,
        "drc4TruthGraphCount": int(drc4_record.get("graphCount", 0)) if drc4_record else 0,
        "ocrAnchorCandidateStatus": status,
        "automaticCandidateGraphCount": metrics["automaticCandidateGraphCount"],
        "validCandidateGraphCount": metrics["validCandidateGraphCount"],
        "reviewCandidateGraphCount": metrics["reviewCandidateGraphCount"],
        "acceptedAnchorCount": metrics["acceptedAnchorCount"],
        "rejectedAnchorCount": metrics["rejectedAnchorCount"],
        "meanFitRmsePx": metrics["meanFitRmsePx"],
        "meanTruthTickRmsePx": metrics["meanTruthTickRmsePx"],
        "drd4BestFixtureSummary": drd4_fixture,
        "graphOcrAnchorResults": graph_results,
        "overlayPath": overlay_path,
        "detailPath": detail_path,
        "recordPath": path_string(EXAMPLE_ROOT / record_id / "stage123456-parity-record.json"),
        "r8OverlayPath": r8_detail["overlayPath"],
        "r8RecordPath": r8_detail["recordPath"],
        "promotionDecision": record["promotionDecision"],
    }
    return record, detail


def write_examples(records: list[dict[str, Any]]) -> None:
    EXAMPLE_ROOT.mkdir(parents=True, exist_ok=True)
    for record in records:
        write_json(EXAMPLE_ROOT / record["recordId"] / "stage123456-parity-record.json", record)


def build_summary(
    details: list[dict[str, Any]],
    drd2_summary: dict[str, Any] | None,
    drd4_summary: dict[str, Any] | None,
    drd6_summary: dict[str, Any] | None,
    dre6_summary: dict[str, Any] | None,
) -> dict[str, Any]:
    graph_pass = sum(1 for detail in details if detail["graphCountScore"] == "PASS")
    layout_pass = sum(1 for detail in details if detail["layoutClassScore"] == "PASS")
    annotated_fixture_count = sum(1 for detail in details if detail["drc4TruthAvailable"])
    automatic_graph_count = sum(int(detail["automaticCandidateGraphCount"]) for detail in details)
    valid_graph_count = sum(int(detail["validCandidateGraphCount"]) for detail in details)
    review_graph_count = sum(int(detail["reviewCandidateGraphCount"]) for detail in details)
    accepted_anchor_count = sum(int(detail["acceptedAnchorCount"]) for detail in details)
    rejected_anchor_count = sum(int(detail["rejectedAnchorCount"]) for detail in details)
    fit_rmses = [float(detail["meanFitRmsePx"]) for detail in details if float(detail["meanFitRmsePx"]) >= 0]
    truth_rmses = [
        float(detail["meanTruthTickRmsePx"])
        for detail in details
        if float(detail["meanTruthTickRmsePx"]) >= 0
    ]
    status_counts: dict[str, int] = {}
    for detail in details:
        status = detail["ocrAnchorCandidateStatus"]
        status_counts[status] = status_counts.get(status, 0) + 1
    return {
        "schemaVersion": "chromalab.benchmark.r9_stage6_automatic_ocr_anchor_candidate_summary.v1",
        "productionImpact": "NONE_SHADOW_ONLY",
        "overallVerdict": "R9_STAGE6_AUTOMATIC_OCR_ANCHOR_CANDIDATE_REVIEW",
        "recordCount": len(details),
        "fixtureCount": len({detail["fixtureId"] for detail in details}),
        "graphCountPass": graph_pass,
        "layoutClassPass": layout_pass,
        "annotatedTruthFixtureCount": annotated_fixture_count,
        "automaticOcrCandidateGraphCount": automatic_graph_count,
        "validCandidateGraphCount": valid_graph_count,
        "reviewCandidateGraphCount": review_graph_count,
        "acceptedAnchorCount": accepted_anchor_count,
        "rejectedAnchorCount": rejected_anchor_count,
        "meanFitRmsePx": round(float(statistics.mean(fit_rmses)), 6) if fit_rmses else None,
        "meanTruthTickRmsePx": round(float(statistics.mean(truth_rmses)), 6) if truth_rmses else None,
        "statusCounts": status_counts,
        "sourceReports": {
            "drd2": path_string(DRD2_SUMMARY) if drd2_summary else "",
            "drd4": path_string(DRD4_SUMMARY) if drd4_summary else "",
            "drd6": path_string(DRD6_SUMMARY) if drd6_summary else "",
            "dre6": path_string(DRE6_SUMMARY) if dre6_summary else "",
        },
        "bestAxisOwnedOcrVariant": best_drd6_variant_summary(drd6_summary),
        "dre6ResultSummary": dre6_summary.get("resultSummary", {}) if dre6_summary else {},
        "nextRequiredWork": [
            "Keep R9 automatic OCR anchors shadow-only until the same anchor rows are produced by Android or Rust runtime.",
            "Bridge DRE6 safe OCR anchor generation into the replacement pipeline without adding duplicate stale layers.",
            "Require per-anchor provenance, forbidden-text rejection, residuals, and graph-level failure packages before runtime promotion.",
            "Do not promote OCR-derived scale anchors when there is no pixel geometry or when title/ion/m/z text is the source.",
        ],
        "records": details,
    }


def write_markdown(summary: dict[str, Any]) -> None:
    lines = [
        "# R9 Stage 6 Automatic OCR Anchor Candidate",
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
        f"Automatic OCR candidate graphs: `{summary['automaticOcrCandidateGraphCount']}`",
        f"Valid candidate graphs: `{summary['validCandidateGraphCount']}`",
        f"Review candidate graphs: `{summary['reviewCandidateGraphCount']}`",
        f"Accepted OCR anchors: `{summary['acceptedAnchorCount']}`",
        f"Rejected OCR anchors: `{summary['rejectedAnchorCount']}`",
        f"Mean fit RMSE px: `{summary['meanFitRmsePx']}`",
        f"Mean truth tick RMSE px: `{summary['meanTruthTickRmsePx']}`",
        "",
        "R9 consumes R8 calibration strategy evidence and DRD/DRE OCR benchmark outputs.",
        "It measures automatic OCR anchor readiness from PC-side safe OCR evidence and keeps it shadow-only.",
        "It does not change Android runtime behavior, validators, report gates, chromatographic math, model policy, or CalculationEngine.",
        "",
        "Contact sheet: `benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/contact_sheet_automatic_ocr_anchor_candidate.png`",
        "",
        "## Fixture Results",
        "",
        "| Fixture | Graphs | Layout | OCR candidate graphs | Valid | Review | Anchors | Mean fit RMSE px | Mean truth tick RMSE px | Overlay | Detail |",
        "|---|---:|---|---:|---:|---:|---:|---:|---:|---|---|",
    ]
    for detail in summary["records"]:
        lines.append(
            "| `{fixtureId}` | {detectedGraphCount} | `{layout}` | {candidateGraphs} | {validGraphs} | {reviewGraphs} | {anchors} | {fitRmse} | {truthRmse} | `{overlay}` | `{detail}` |".format(
                fixtureId=detail["fixtureId"],
                detectedGraphCount=detail["detectedGraphCount"],
                layout=detail["predictedLayoutClass"],
                candidateGraphs=detail["automaticCandidateGraphCount"],
                validGraphs=detail["validCandidateGraphCount"],
                reviewGraphs=detail["reviewCandidateGraphCount"],
                anchors=detail["acceptedAnchorCount"],
                fitRmse=detail["meanFitRmsePx"],
                truthRmse=detail["meanTruthTickRmsePx"],
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
    parser.add_argument("--clean", action="store_true", help="Remove previous R9 examples/report before writing.")
    args = parser.parse_args(argv)

    if args.clean:
        for path in [EXAMPLE_ROOT, REPORT_ROOT]:
            if path.exists():
                shutil.rmtree(path)

    r8_summary = load_required_summary(R8_SUMMARY, "R8")
    drd2_summary = read_json_if_exists(DRD2_SUMMARY)
    drd4_summary = read_json_if_exists(DRD4_SUMMARY)
    drd6_summary = read_json_if_exists(DRD6_SUMMARY)
    dre6_summary = read_json_if_exists(DRE6_SUMMARY)
    drc4_summary = read_json_if_exists(DRC4_SUMMARY)
    dre6_graphs = index_dre6_graphs(dre6_summary)
    drc4_records = drc4_record_by_fixture(drc4_summary)

    records = []
    details = []
    for r8_detail in r8_summary["records"]:
        fixture_id = r8_detail["fixtureId"]
        started = time.perf_counter()
        graph_results = []
        for graph in r8_detail.get("graphCalibrationResults", []):
            graph_index = int(graph["graphIndex"])
            graph_results.append(
                build_graph_result(fixture_id, graph, dre6_graphs.get((fixture_id, graph_index)))
            )
        overlay_path = render_overlay(
            Path(r8_detail["sourceImage"]),
            graph_results,
            OVERLAY_ROOT / f"{fixture_id}_automatic_ocr_anchor_overlay.png",
        )
        detail_path = path_string(DETAIL_ROOT / f"{fixture_id}_automatic_ocr_anchor_detail.json")
        r8_record = read_json(Path(r8_detail["recordPath"]))
        record, detail = build_record(
            r8_detail,
            r8_record,
            drc4_records.get(fixture_id),
            best_drd4_fixture_summary(drd4_summary, fixture_id),
            graph_results,
            overlay_path,
            detail_path,
        )
        detail["elapsedMs"] = round((time.perf_counter() - started) * 1000.0, 3)
        write_json(Path(detail_path), detail)
        records.append(record)
        details.append(detail)

    write_examples(records)
    summary = build_summary(details, drd2_summary, drd4_summary, drd6_summary, dre6_summary)
    summary["contactSheet"] = create_contact_sheet(details)
    write_json(REPORT_ROOT / "summary.json", summary)
    write_markdown(summary)
    print(
        "R9 Stage 6 automatic OCR anchor candidate wrote "
        f"{len(records)} records to {EXAMPLE_ROOT.as_posix()} and report to {REPORT_ROOT.as_posix()}."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
