#!/usr/bin/env python3
"""Build R5 Stage 2 graph discovery candidate records.

This is a PC-side shadow benchmark only. It consumes R4 Rust Stage 1 evidence
but does not change Android runtime, validators, report gates, graph-count
metadata, chromatographic math, model policy, or CalculationEngine.
"""

from __future__ import annotations

import argparse
import json
import math
import shutil
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image, ImageDraw


ANDROID_METADATA_DIR = Path("composeApp/src/androidMain/assets/validation")
R4_REPORT_ROOT = Path("benchmark/reports/r4_rust_stage1_image_preparation_parity")
R4_SUMMARY = R4_REPORT_ROOT / "summary.json"
EXAMPLE_ROOT = Path("benchmark/examples/r5_stage2_graph_discovery_candidate")
REPORT_ROOT = Path("benchmark/reports/r5_stage2_graph_discovery_candidate")
OVERLAY_ROOT = REPORT_ROOT / "overlays"


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


@dataclass(frozen=True)
class RowCandidate:
    row: int
    score: float
    dark_run: int
    cluster_height: int
    below_dark_fraction: float
    above_dark_fraction: float
    rejection_reason: str = ""


@dataclass(frozen=True)
class GraphPanelCandidate:
    graph_index: int
    x: int
    y: int
    width: int
    height: int
    axis_row: int
    confidence: float
    evidence_source: str


@dataclass(frozen=True)
class GraphDiscoveryFeatures:
    image_width: int
    image_height: int
    content_crop: tuple[int, int, int, int]
    threshold: float
    row_candidates: list[RowCandidate]
    selected_rows: list[RowCandidate]
    graph_panels: list[GraphPanelCandidate]


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


def load_r4_by_fixture() -> dict[str, dict[str, Any]]:
    summary = read_json(R4_SUMMARY)
    return {item["fixtureId"]: item for item in summary["records"]}


def resolve_image_path(metadata: dict[str, Any]) -> Path:
    image_path = Path(str(metadata["assetImagePath"]))
    if image_path.exists():
        return image_path
    return Path("composeApp/src/androidMain/assets") / image_path


def longest_run(values: np.ndarray) -> int:
    best = 0
    current = 0
    for value in values:
        if value:
            current += 1
            best = max(best, current)
        else:
            current = 0
    return best


def cluster_numbers(values: list[int] | np.ndarray, max_gap: int) -> list[list[int]]:
    clusters: list[list[int]] = []
    current: list[int] = []
    last: int | None = None
    for value in sorted(set(int(item) for item in values)):
        if last is None or value - last <= max_gap:
            current.append(value)
        else:
            clusters.append(current)
            current = [value]
        last = value
    if current:
        clusters.append(current)
    return clusters


def detect_graph_features(image_path: Path) -> GraphDiscoveryFeatures:
    gray = np.array(Image.open(image_path).convert("L"))
    image_height, image_width = gray.shape
    bright = gray > 150
    row_bright_fraction = bright.mean(axis=1)
    col_bright_fraction = bright.mean(axis=0)
    bright_rows = np.where(row_bright_fraction > 0.18)[0]
    bright_cols = np.where(col_bright_fraction > 0.18)[0]
    if len(bright_rows) == 0 or len(bright_cols) == 0:
        return GraphDiscoveryFeatures(
            image_width=image_width,
            image_height=image_height,
            content_crop=(0, 0, image_width, image_height),
            threshold=0.0,
            row_candidates=[],
            selected_rows=[],
            graph_panels=[],
        )

    y0 = max(0, int(bright_rows[0]) - 8)
    y1 = min(image_height, int(bright_rows[-1]) + 9)
    x0 = max(0, int(bright_cols[0]) - 8)
    x1 = min(image_width, int(bright_cols[-1]) + 9)
    crop = gray[y0:y1, x0:x1]
    threshold = min(185.0, max(65.0, float(np.percentile(crop, 28))))
    mask = crop < threshold
    dilated = mask.copy()
    for shift in range(1, 7):
        dilated[:, shift:] |= mask[:, :-shift]
        dilated[:, :-shift] |= mask[:, shift:]

    row_runs = np.array([longest_run(row) for row in dilated])
    row_dark_fraction = dilated.mean(axis=1)
    width = max(1, crop.shape[1])
    row_scores = (row_runs / width) * 0.75 + np.minimum(row_dark_fraction * 3, 1) * 0.25
    candidate_rows = np.where((row_runs > width * 0.28) & (row_scores > 0.19))[0]
    candidate_rows = candidate_rows[candidate_rows > crop.shape[0] * 0.10]

    row_candidates: list[RowCandidate] = []
    for cluster in cluster_numbers(candidate_rows, max_gap=14):
        selected = max(cluster, key=lambda row: row_runs[row])
        absolute_row = y0 + int(selected)
        below = mask[selected + 5 : min(mask.shape[0], selected + 65)]
        above = mask[max(0, selected - 65) : max(0, selected - 5)]
        below_dark_fraction = float(below.mean()) if below.size else 0.0
        above_dark_fraction = float(above.mean()) if above.size else 0.0
        cluster_height = len(cluster)
        score = float(row_scores[selected])
        rejection_reason = ""
        if (
            cluster_height >= 100
            and absolute_row < image_height * 0.70
            and above_dark_fraction >= 0.45
            and below_dark_fraction >= 0.45
        ):
            rejection_reason = "REJECTED_GIANT_INTERIOR_DENSE_BAND"
        if absolute_row < image_height * 0.16 and above_dark_fraction >= 0.85 and below_dark_fraction >= 0.85:
            rejection_reason = "REJECTED_TOP_PAGE_OR_TITLE_BAND"
        row_candidates.append(
            RowCandidate(
                row=absolute_row,
                score=score,
                dark_run=int(row_runs[selected]),
                cluster_height=cluster_height,
                below_dark_fraction=below_dark_fraction,
                above_dark_fraction=above_dark_fraction,
                rejection_reason=rejection_reason,
            )
        )

    usable = [candidate for candidate in row_candidates if not candidate.rejection_reason]
    high_rows = [candidate for candidate in usable if candidate.score >= 0.75]
    high_rows = collapse_close_rows(high_rows, max_gap=70)
    medium_rows = [
        candidate
        for candidate in usable
        if candidate.score >= 0.47
        and candidate.cluster_height <= 20
        and candidate.below_dark_fraction >= 0.10
        and candidate.above_dark_fraction <= candidate.below_dark_fraction + 0.02
    ]
    medium_rows = collapse_close_rows(medium_rows, max_gap=60)
    selected_rows = high_rows if len(high_rows) >= 2 or len(medium_rows) <= len(high_rows) else medium_rows
    graph_panels = build_panel_candidates(
        content_crop=(x0, y0, x1, y1),
        image_width=image_width,
        image_height=image_height,
        selected_rows=selected_rows,
    )
    return GraphDiscoveryFeatures(
        image_width=image_width,
        image_height=image_height,
        content_crop=(x0, y0, x1, y1),
        threshold=threshold,
        row_candidates=row_candidates,
        selected_rows=selected_rows,
        graph_panels=graph_panels,
    )


def collapse_close_rows(rows: list[RowCandidate], max_gap: int) -> list[RowCandidate]:
    selected: list[RowCandidate] = []
    by_row = {row.row: row for row in rows}
    for cluster in cluster_numbers([row.row for row in rows], max_gap=max_gap):
        group = [by_row[row] for row in cluster]
        selected.append(max(group, key=lambda row: (row.score, row.dark_run)))
    return selected


def build_panel_candidates(
    content_crop: tuple[int, int, int, int],
    image_width: int,
    image_height: int,
    selected_rows: list[RowCandidate],
) -> list[GraphPanelCandidate]:
    x0, y0, x1, y1 = content_crop
    if not selected_rows:
        return []
    rows = sorted(selected_rows, key=lambda item: item.row)
    panels: list[GraphPanelCandidate] = []
    previous_bottom = y0
    for index, row in enumerate(rows, start=1):
        top = previous_bottom
        if index > 1:
            top = max(y0, min(row.row - 120, previous_bottom + 8))
        bottom = min(image_height, max(row.row + 42, row.row + int(image_height * 0.03)))
        if index < len(rows):
            next_row = rows[index].row
            bottom = min(bottom, next_row - 8)
        if bottom <= top:
            top = max(y0, row.row - 160)
            bottom = min(y1, row.row + 42)
        panels.append(
            GraphPanelCandidate(
                graph_index=index,
                x=max(0, x0),
                y=max(0, top),
                width=max(1, min(image_width, x1) - max(0, x0)),
                height=max(1, min(image_height, bottom) - max(0, top)),
                axis_row=row.row,
                confidence=round(min(1.0, row.score), 4),
                evidence_source="ROW_AXIS_PROJECTION",
            )
        )
        previous_bottom = bottom
    return panels


def render_overlay(image_path: Path, features: GraphDiscoveryFeatures, out: Path) -> str:
    image = Image.open(image_path).convert("RGB")
    draw = ImageDraw.Draw(image)
    x0, y0, x1, y1 = features.content_crop
    draw.rectangle((x0, y0, x1, y1), outline=(255, 190, 0), width=3)
    selected_rows = {candidate.row for candidate in features.selected_rows}
    for candidate in features.row_candidates:
        if candidate.row in selected_rows:
            color = (0, 210, 90)
            width = 4
        elif candidate.rejection_reason:
            color = (230, 40, 40)
            width = 2
        else:
            color = (90, 90, 255)
            width = 2
        draw.line((x0, candidate.row, x1, candidate.row), fill=color, width=width)
    for panel in features.graph_panels:
        draw.rectangle(
            (panel.x, panel.y, panel.x + panel.width, panel.y + panel.height),
            outline=(0, 210, 90),
            width=4,
        )
    out.parent.mkdir(parents=True, exist_ok=True)
    image.save(out)
    return str(out).replace("\\", "/")


def create_contact_sheet(details: list[dict[str, Any]]) -> str:
    tiles = []
    for detail in details:
        image = Image.open(detail["overlayPath"]).convert("RGB")
        image.thumbnail((320, 240), Image.Resampling.LANCZOS)
        canvas = Image.new("RGB", (340, 270), "white")
        canvas.paste(image, ((340 - image.width) // 2, 8))
        tiles.append(canvas)
    columns = 4
    tile_w, tile_h = 340, 270
    rows = math.ceil(len(tiles) / columns)
    sheet = Image.new("RGB", (columns * tile_w, rows * tile_h), "white")
    for index, tile in enumerate(tiles):
        sheet.paste(tile, ((index % columns) * tile_w, (index // columns) * tile_h))
    out = REPORT_ROOT / "contact_sheet_graph_discovery.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    return str(out).replace("\\", "/")


def row_candidate_to_json(candidate: RowCandidate) -> dict[str, Any]:
    return {
        "row": candidate.row,
        "score": round(candidate.score, 6),
        "darkRun": candidate.dark_run,
        "clusterHeight": candidate.cluster_height,
        "belowDarkFraction": round(candidate.below_dark_fraction, 6),
        "aboveDarkFraction": round(candidate.above_dark_fraction, 6),
        "rejectionReason": candidate.rejection_reason,
    }


def panel_to_json(panel: GraphPanelCandidate) -> dict[str, Any]:
    return {
        "graphIndex": panel.graph_index,
        "graphPanel": {
            "x": panel.x,
            "y": panel.y,
            "width": panel.width,
            "height": panel.height,
        },
        "axisRow": panel.axis_row,
        "confidence": panel.confidence,
        "evidenceSource": panel.evidence_source,
    }


def build_record(
    metadata: dict[str, Any],
    r4_detail: dict[str, Any],
    features: GraphDiscoveryFeatures,
    overlay_path: str,
) -> tuple[dict[str, Any], dict[str, Any]]:
    fixture_id = metadata["fixtureId"]
    expected_count = int(metadata.get("expectedGraphCount") or 0)
    detected_count = len(features.graph_panels)
    graph_count_score = "PASS" if detected_count == expected_count else "FAIL"
    graph_status = "REVIEW"
    record_id = f"stage123_{fixture_id}_pc_stage2_graph_discovery_candidate_v1"
    row_candidates = [row_candidate_to_json(candidate) for candidate in features.row_candidates]
    panels = [panel_to_json(panel) for panel in features.graph_panels]
    rejected_count = sum(1 for candidate in features.row_candidates if candidate.rejection_reason)
    record = {
        "schemaVersion": "chromalab.benchmark.stage123_parity_record.v1",
        "recordId": record_id,
        "fixtureId": fixture_id,
        "mode": "pc_prototype",
        "sourceId": "r5_stage2_graph_discovery_candidate_v1",
        "sourceKind": "PC_STAGE2_GRAPH_DISCOVERY_CANDIDATE",
        "productionImpact": "NONE_SHADOW_ONLY",
        "runtimeReadiness": "PC_STAGE2_GRAPH_DISCOVERY_CANDIDATE_NOT_RUNTIME_READY",
        "expectedGraphCount": expected_count,
        "detectedGraphCount": detected_count,
        "graphCountScore": graph_count_score,
        "expectedLayoutClass": EXPECTED_LAYOUTS.get(fixture_id, "UNKNOWN_REVIEW"),
        "predictedLayoutClass": "UNKNOWN_REVIEW",
        "layoutClassScore": "NOT_SCOREABLE",
        "imagePreparation": {
            "status": r4_detail["rustStatus"],
            "available": True,
            "summary": "Uses R4 Rust Stage 1 selected preprocessing evidence as the upstream image-preparation input.",
            "metrics": {
                "r4SelectedVariantScore": r4_detail["rustSelectedVariantScore"],
                "r4SelectedVariantParity": r4_detail["selectedVariantParity"],
                "r4StatusParity": r4_detail["statusParity"],
            },
        },
        "graphDiscovery": {
            "status": graph_status,
            "available": True,
            "summary": "Detects graph count from deterministic row-axis projection candidates, with rejected giant/title/page bands preserved as evidence.",
            "metrics": {
                "expectedGraphCount": expected_count,
                "detectedGraphCount": detected_count,
                "rowCandidateCount": len(features.row_candidates),
                "selectedRowCount": len(features.selected_rows),
                "rejectedRowCandidateCount": rejected_count,
                "threshold": round(features.threshold, 4),
                "contentCropX": features.content_crop[0],
                "contentCropY": features.content_crop[1],
                "contentCropWidth": features.content_crop[2] - features.content_crop[0],
                "contentCropHeight": features.content_crop[3] - features.content_crop[1],
            },
        },
        "plotAreaLayout": {
            "status": "NOT_SCORED",
            "available": False,
            "summary": "R5 is Stage 2 graph discovery only; plotArea and semantic layout remain Stage 3 work.",
        },
        "failureClass": "",
        "firstFailingStage": "" if graph_count_score == "PASS" else "GRAPH_DISCOVERY_REVIEW",
        "evidence": {
            "sourceImage": str(resolve_image_path(metadata)).replace("\\", "/"),
            "recordSource": str(REPORT_ROOT / "summary.json").replace("\\", "/"),
            "artifactPaths": [
                overlay_path,
                r4_detail["rustReportPath"],
            ],
        },
        "promotionDecision": "STAGE2_CANDIDATE_REQUIRES_STAGE3_PARITY",
        "notes": [
            "Shadow-only Stage 2 graph discovery candidate.",
            "No plotArea, calibration, trace, peak, report-gate, or CalculationEngine authority.",
            "Graph count is scored, but graphPanel localization is candidate-only until Stage 3 IoU/plotArea scoring.",
            "Layout class is intentionally NOT_SCOREABLE in R5.",
        ],
    }
    detail = {
        "fixtureId": fixture_id,
        "expectedGraphCount": expected_count,
        "detectedGraphCount": detected_count,
        "graphCountScore": graph_count_score,
        "graphStatus": graph_status,
        "expectedLayoutClass": record["expectedLayoutClass"],
        "predictedLayoutClass": record["predictedLayoutClass"],
        "layoutClassScore": record["layoutClassScore"],
        "selectedRows": [candidate.row for candidate in features.selected_rows],
        "rowCandidates": row_candidates,
        "graphPanelCandidates": panels,
        "overlayPath": overlay_path,
        "recordPath": str(EXAMPLE_ROOT / record_id / "stage123-parity-record.json").replace("\\", "/"),
        "r4SelectedVariantId": r4_detail["rustSelectedVariantId"],
        "r4SelectedVariantScore": r4_detail["rustSelectedVariantScore"],
        "r4Status": r4_detail["rustStatus"],
    }
    return record, detail


def write_examples(records: list[dict[str, Any]]) -> None:
    EXAMPLE_ROOT.mkdir(parents=True, exist_ok=True)
    for record in records:
        write_json(EXAMPLE_ROOT / record["recordId"] / "stage123-parity-record.json", record)


def build_summary(details: list[dict[str, Any]]) -> dict[str, Any]:
    graph_pass = sum(1 for detail in details if detail["graphCountScore"] == "PASS")
    status_counts: dict[str, int] = {}
    for detail in details:
        status_counts[detail["graphStatus"]] = status_counts.get(detail["graphStatus"], 0) + 1
    verdict = (
        "R5_STAGE2_GRAPH_DISCOVERY_COUNT_READY_FOR_STAGE3_SHADOW"
        if graph_pass == len(details)
        else "R5_STAGE2_GRAPH_DISCOVERY_CANDIDATE_REVIEW"
    )
    return {
        "schemaVersion": "chromalab.benchmark.r5_stage2_graph_discovery_candidate_summary.v1",
        "productionImpact": "NONE_SHADOW_ONLY",
        "overallVerdict": verdict,
        "recordCount": len(details),
        "fixtureCount": len({detail["fixtureId"] for detail in details}),
        "graphCountPass": graph_pass,
        "graphCountFail": len(details) - graph_pass,
        "statusCounts": status_counts,
        "nextRequiredWork": [
            "Keep R5 graph discovery shadow-only until Stage 3 plotArea/layout semantics are measured.",
            "Add Stage 3 plotArea and semantic layout candidate before any Android runtime promotion.",
            "Do not treat R5 graph-count pass as calibration or report readiness.",
            "Keep Android runtime unchanged until Stage 1-3 promotion gates pass.",
        ],
        "records": details,
    }


def write_markdown(summary: dict[str, Any]) -> None:
    lines = [
        "# R5 Stage 2 Graph Discovery Candidate",
        "",
        f"Verdict: `{summary['overallVerdict']}`",
        "",
        "Production impact: `NONE_SHADOW_ONLY`",
        "",
        f"Records: `{summary['recordCount']}`",
        f"Fixtures: `{summary['fixtureCount']}`",
        f"Graph-count pass: `{summary['graphCountPass']}/{summary['recordCount']}`",
        "",
        "R5 consumes R4 Rust Stage 1 evidence and builds PC-side graphPanel candidate evidence.",
        "It does not change Android runtime behavior, validators, report gates, graph-count metadata, chromatographic math, model policy, or CalculationEngine.",
        "",
        "Graph count is scored here. GraphPanel localization remains candidate-only until Stage 3 IoU/plotArea scoring.",
        "",
        "Contact sheet: `benchmark/reports/r5_stage2_graph_discovery_candidate/contact_sheet_graph_discovery.png`",
        "",
        "## Fixture Results",
        "",
        "| Fixture | Expected graphs | Detected graphs | Graph count | Selected rows | R4 variant | Overlay |",
        "|---|---:|---:|---|---|---|---|",
    ]
    for detail in summary["records"]:
        rows = ", ".join(str(row) for row in detail["selectedRows"]) or "-"
        lines.append(
            "| `{fixtureId}` | {expectedGraphCount} | {detectedGraphCount} | {graphCountScore} | {rows} | `{variant}` | `{overlay}` |".format(
                fixtureId=detail["fixtureId"],
                expectedGraphCount=detail["expectedGraphCount"],
                detectedGraphCount=detail["detectedGraphCount"],
                graphCountScore=detail["graphCountScore"],
                rows=rows,
                variant=detail["r4SelectedVariantId"],
                overlay=detail["overlayPath"],
            )
        )
    lines.extend(["", "## Next Required Work", ""])
    for item in summary["nextRequiredWork"]:
        lines.append(f"- {item}")
    lines.append("")
    write_text(REPORT_ROOT / "summary.md", "\n".join(lines))


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--clean", action="store_true", help="Remove previous R5 examples/report before writing.")
    args = parser.parse_args(argv)

    if args.clean:
        for path in [EXAMPLE_ROOT, REPORT_ROOT]:
            if path.exists():
                shutil.rmtree(path)

    if not R4_SUMMARY.exists():
        raise FileNotFoundError(f"R4 summary is required before R5 graph discovery: {R4_SUMMARY}")

    r4_by_fixture = load_r4_by_fixture()
    records = []
    details = []
    for item in load_metadata():
        fixture_id = item["fixtureId"]
        image_path = resolve_image_path(item)
        started = time.perf_counter()
        features = detect_graph_features(image_path)
        overlay_path = render_overlay(image_path, features, OVERLAY_ROOT / f"{fixture_id}_graph_discovery_overlay.png")
        record, detail = build_record(item, r4_by_fixture[fixture_id], features, overlay_path)
        detail["elapsedMs"] = round((time.perf_counter() - started) * 1000.0, 3)
        records.append(record)
        details.append(detail)

    write_examples(records)
    summary = build_summary(details)
    summary["contactSheet"] = create_contact_sheet(details)
    write_json(REPORT_ROOT / "summary.json", summary)
    write_markdown(summary)
    print(
        "R5 Stage 2 graph discovery wrote "
        f"{len(records)} records to {EXAMPLE_ROOT.as_posix()} and report to {REPORT_ROOT.as_posix()}."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
