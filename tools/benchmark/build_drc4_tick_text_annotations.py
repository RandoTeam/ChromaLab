#!/usr/bin/env python3
"""Build initial DR-C4 major tick and text-role annotations for P0 fixtures."""

from __future__ import annotations

import argparse
import json
import math
import shutil
from pathlib import Path
from typing import Any

from PIL import Image, ImageDraw


TickSpec = tuple[float, str]


def role_text(role: str, text: str, x: float, y: float, width: float, height: float, reason: str) -> dict[str, Any]:
    return {
        "role": role,
        "text": text,
        "bbox": {"x": x, "y": y, "width": width, "height": height},
        "rejectedAsTickLabel": role != "tick_label",
        "rejectionReason": reason if role != "tick_label" else None,
    }


def format_int_space(value: float) -> str:
    return f"{int(value):,}".replace(",", " ")


def stacked_xic_graph_spec(
    *,
    x_range: tuple[float, float],
    y_range: tuple[float, float],
    y_ticks: list[int],
    extra: list[dict[str, Any]],
) -> dict[str, Any]:
    return {
        "xRange": x_range,
        "xTicks": [(value, str(int(value))) for value in [1300, 1320, 1340, 1360, 1380, 1400, 1420]],
        "yRange": y_range,
        "yTicks": [(float(value), str(value)) for value in y_ticks],
        "extraText": extra,
    }


def ion_panel_graph_spec(*, y_origin: float, y_top: float, title: dict[str, Any]) -> dict[str, Any]:
    return {
        "xRange": (23.5, 29.5),
        "xTicks": [(value, f"{value:.2f}".replace(".", ",")) for value in [24.0, 24.5, 25.0, 25.5, 26.0, 26.5, 27.0, 27.5, 28.0, 28.5, 29.0]],
        "yRange": (0.0, 10_000.0),
        "yTicks": [(0.0, "0"), (10_000.0, "10 000")],
        "extraText": [
            title,
            role_text("axis_title", "Интенсивность", 10, y_top - 18, 112, 24, "y axis title"),
            role_text("axis_title", "Время-->", 10, y_origin - 18, 88, 25, "x axis title"),
        ],
    }


def photo_two_graph_spec(*, title: dict[str, Any]) -> dict[str, Any]:
    return {
        "xRange": (5.0, 60.0),
        "xTicks": [(value, f"{value:.2f}") for value in [5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55]],
        "yRange": (0.0, 140_000.0),
        "yTicks": [(float(value), str(value)) for value in [0, 10_000, 20_000, 30_000, 40_000, 50_000, 60_000, 70_000, 80_000, 90_000, 100_000, 110_000, 120_000, 130_000, 140_000]],
        "extraText": [
            title,
            role_text("axis_title", "Abundance", 150, 250, 90, 24, "y axis title"),
            role_text("axis_title", "Time->", 150, 660, 75, 24, "x axis title"),
        ],
    }


GRAPH_SPECS: dict[str, dict[str, Any]] = {
    "bench_01_graph_1": {
        "xRange": (30.0, 70.0),
        "xTicks": [(value, f"{value:.2f}") for value in [35, 40, 45, 50, 55, 60, 65]],
        "yRange": (0.0, 350.0),
        "yTicks": [(value, str(int(value))) for value in [0, 50, 100, 150, 200, 250, 300, 350]],
        "extraText": [
            role_text("ion_or_mz_metadata", "Ion 217.00: 0301002.D", 410, 300, 245, 28, "ion title, not a tick label"),
            role_text("axis_title", "Abundance", 103, 315, 105, 22, "y axis title"),
            role_text("axis_title", "Time ->", 116, 704, 82, 22, "x axis title"),
        ],
    },
    "bench_01_graph_2": {
        "xRange": (30.0, 70.0),
        "xTicks": [(value, f"{value:.2f}") for value in [35, 40, 45, 50, 55, 60, 65]],
        "yRange": (0.0, 350.0),
        "yTicks": [(value, str(int(value))) for value in [0, 50, 100, 150, 200, 250, 300, 350]],
        "extraText": [
            role_text("ion_or_mz_metadata", "Ion 218.00: 0301002.D", 420, 715, 245, 26, "ion title, not a tick label"),
            role_text("axis_title", "Abundance", 116, 721, 110, 22, "y axis title"),
            role_text("axis_title", "Time ->", 132, 1096, 82, 22, "x axis title"),
        ],
    },
    "bench_04_graph_1": stacked_xic_graph_spec(
        x_range=(1280.0, 1428.0),
        y_range=(0.0, 700.0),
        y_ticks=[0, 100, 200, 300, 400, 500, 600, 700],
        extra=[
            role_text("legend", "XIC(198,0315±0,2)", 209, 229, 190, 26, "legend, not a tick label"),
            role_text("other", "0,2 Да", 468, 27, 62, 26, "signal window text, not a tick label"),
            role_text("axis_title", "Время, с", 0, 224, 82, 24, "x axis title"),
        ],
    ),
    "bench_04_graph_2": stacked_xic_graph_spec(
        x_range=(1280.0, 1428.0),
        y_range=(0.0, 350.0),
        y_ticks=[0, 50, 100, 150, 200, 250, 300, 350],
        extra=[
            role_text("legend", "XIC(198,0315±0,02)", 205, 505, 200, 25, "legend, not a tick label"),
            role_text("other", "0,02 Да", 468, 302, 70, 26, "signal window text, not a tick label"),
            role_text("axis_title", "Время, с", 0, 498, 82, 24, "x axis title"),
        ],
    ),
    "bench_04_graph_3": stacked_xic_graph_spec(
        x_range=(1280.0, 1428.0),
        y_range=(0.0, 250.0),
        y_ticks=[0, 50, 100, 150, 200, 250],
        extra=[
            role_text("legend", "XIC(198,0315±0,002)", 205, 786, 210, 25, "legend, not a tick label"),
            role_text("other", "0,002 Да", 456, 584, 78, 26, "signal window text, not a tick label"),
            role_text("axis_title", "Время, с", 0, 780, 82, 24, "x axis title"),
        ],
    ),
    "bench_04_graph_4": stacked_xic_graph_spec(
        x_range=(1280.0, 1428.0),
        y_range=(0.0, 250.0),
        y_ticks=[0, 50, 100, 150, 200, 250],
        extra=[
            role_text("legend", "XIC(198,0315±0,0002)", 204, 1064, 222, 25, "legend, not a tick label"),
            role_text("other", "0,0002 Да", 446, 890, 88, 26, "signal window text, not a tick label"),
            role_text("axis_title", "Время, с", 0, 1060, 82, 24, "x axis title"),
        ],
    ),
    "bench_05_graph_1": {
        "xRange": (5.0, 35.0),
        "xTicks": [(value, f"{value:.2f}".replace(".", ",")) for value in [5, 10, 15, 20, 25, 30]],
        "yRange": (0.0, 2_000_000.0),
        "yTicks": [
            (value, format_int_space(value))
            for value in [0, 200_000, 400_000, 600_000, 800_000, 1_000_000, 1_200_000, 1_400_000, 1_600_000, 1_800_000, 2_000_000]
        ],
        "extraText": [
            role_text("chart_title", "TIC: NERPA1.D", 282, 12, 140, 24, "chart title"),
            role_text("axis_title", "Интенсивность", 10, 24, 112, 24, "y axis title"),
            role_text("axis_title", "Время-->", 10, 355, 88, 25, "x axis title"),
        ],
    },
    "bench_05_graph_2": ion_panel_graph_spec(
        y_origin=507,
        y_top=443,
        title=role_text("ion_or_mz_metadata", "Ion 326.00 (325.70 to 326.70): NERPA1.D", 180, 412, 340, 25, "ion title, not a tick label"),
    ),
    "bench_05_graph_3": ion_panel_graph_spec(
        y_origin=645,
        y_top=582,
        title=role_text("ion_or_mz_metadata", "Ion 360.00 (359.70 to 360.70): NERPA1.D", 180, 555, 350, 25, "ion title, not a tick label"),
    ),
    "bench_05_graph_4": ion_panel_graph_spec(
        y_origin=782,
        y_top=724,
        title=role_text("ion_or_mz_metadata", "Ion 394.00 (393.70 to 394.70): NERPA1.D", 180, 696, 350, 25, "ion title, not a tick label"),
    ),
    "bench_06_graph_1": photo_two_graph_spec(
        title=role_text("ion_or_mz_metadata", "Ion 83.00 (82.70 to 83.70): BELY TIGR_1.D", 390, 235, 420, 24, "ion title, not a tick label")
    ),
    "bench_06_graph_2": photo_two_graph_spec(
        title=role_text("ion_or_mz_metadata", "Ion 92.00 (91.70 to 92.70): BELY TIGR_1.D", 350, 676, 430, 24, "ion title, not a tick label")
    ),
}


def read_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def interpolate(axis_line: dict[str, Any], value: float, value_range: tuple[float, float]) -> dict[str, float]:
    start = axis_line["start"]
    end = axis_line["end"]
    low, high = value_range
    if math.isclose(high, low):
        raise ValueError("Axis value range cannot be zero.")
    t = (value - low) / (high - low)
    return {
        "x": round(float(start["x"]) + t * (float(end["x"]) - float(start["x"])), 2),
        "y": round(float(start["y"]) + t * (float(end["y"]) - float(start["y"])), 2),
    }


def numeric_label_box(axis: str, pixel: dict[str, float], text: str) -> dict[str, float]:
    width = max(22, len(text) * 8 + 8)
    if axis == "X":
        return {
            "x": round(pixel["x"] - width / 2, 2),
            "y": round(pixel["y"] + 5, 2),
            "width": width,
            "height": 18,
        }
    return {
        "x": round(pixel["x"] - width - 8, 2),
        "y": round(pixel["y"] - 9, 2),
        "width": width,
        "height": 18,
    }


def build_axis_annotations(
    graph: dict[str, Any],
    axis: str,
    ticks: list[TickSpec],
    value_range: tuple[float, float],
) -> tuple[list[dict[str, Any]], list[dict[str, Any]], list[dict[str, Any]]]:
    axis_key = "xAxis" if axis == "X" else "yAxis"
    tick_positions = []
    numeric_labels = []
    anchors = []
    for index, (value, text) in enumerate(ticks):
        pixel = interpolate(graph["axisLines"][axis_key], value, value_range)
        tick_id = f"{graph['graphId']}_{axis.lower()}tick_{index + 1}"
        tick_positions.append(
            {
                "tickId": tick_id,
                "axis": axis,
                "value": value,
                "pixel": pixel,
                "source": "manual_initial_review_label_projection",
                "confidence": "REVIEW",
                "status": "ANNOTATED_MAJOR_TICK",
            }
        )
        label_id = f"{graph['graphId']}_{axis.lower()}label_{index + 1}"
        label_box = numeric_label_box(axis, pixel, text)
        numeric_labels.append(
            {
                "labelId": label_id,
                "axis": axis,
                "text": text,
                "numericValue": value,
                "bbox": label_box,
                "role": "tick_label",
                "pairedTickId": tick_id,
                "confidence": "REVIEW",
                "status": "ANNOTATED_MAJOR_LABEL",
            }
        )
        anchors.append(
            {
                "anchorId": f"{graph['graphId']}_{axis.lower()}anchor_{index + 1}",
                "axis": axis,
                "pixel": pixel,
                "value": value,
                "tickId": tick_id,
                "labelId": label_id,
                "evidenceType": "manual_initial_review_tick_label_pair",
                "confidence": "REVIEW",
                "status": "REVIEW_ANCHOR",
            }
        )
    return tick_positions, numeric_labels, anchors


def annotate_graph(graph: dict[str, Any]) -> dict[str, Any]:
    spec = GRAPH_SPECS[graph["graphId"]]
    x_ticks, x_labels, x_anchors = build_axis_annotations(graph, "X", spec["xTicks"], spec["xRange"])
    y_ticks, y_labels, y_anchors = build_axis_annotations(graph, "Y", spec["yTicks"], spec["yRange"])
    rejected = [
        {**item, "textId": f"{graph['graphId']}_text_{index + 1}"}
        for index, item in enumerate(spec.get("extraText", []))
    ]
    return {
        "graphId": graph["graphId"],
        "panelGroup": graph["panelGroup"],
        "tickPositions": x_ticks + y_ticks,
        "numericLabelBoxes": x_labels + y_labels,
        "calibrationAnchorCandidates": x_anchors + y_anchors,
        "textRoleLabels": rejected + [
            {
                "textId": label["labelId"],
                "role": "tick_label",
                "text": label["text"],
                "bbox": label["bbox"],
                "axis": label["axis"],
                "rejectedAsTickLabel": False,
                "rejectionReason": None,
            }
            for label in x_labels + y_labels
        ],
        "minorTickStatus": "NOT_ANNOTATED",
        "gridLineStatus": "NO_VISIBLE_GRID_OR_NOT_ANNOTATED",
        "annotationConfidence": "INITIAL_REVIEW",
    }


def build_annotations(drc3: dict[str, Any]) -> dict[str, Any]:
    records = []
    for record in drc3["records"]:
        graph_annotations = [annotate_graph(graph) for graph in record["graphs"]]
        records.append(
            {
                "fixtureId": record["fixtureId"],
                "sourceImage": record["sourceImage"],
                "image": record["image"],
                "layoutClass": record["layoutClass"],
                "expectedPhysicalGraphCount": record["expectedPhysicalGraphCount"],
                "annotationStatus": "PARTIAL_DETAILED_REVIEW",
                "coordinateSpace": drc3["coordinateSpace"],
                "graphs": graph_annotations,
                "remainingGaps": [
                    "minor_tick_positions",
                    "pixel_perfect_numeric_label_boxes",
                    "full_ocr_text_transcription_review",
                ],
            }
        )
    return {
        "schemaVersion": "chromalab.benchmark.drc4_tick_text_role_annotations.v1",
        "annotationStatus": "PARTIAL_DETAILED_REVIEW",
        "sourceLayoutAnnotations": "benchmark/annotations/drc3_initial_graph_layout_annotations/manual-p0-annotations.json",
        "coordinateSpace": drc3["coordinateSpace"],
        "records": records,
    }


def draw_bbox(draw: ImageDraw.ImageDraw, bbox: dict[str, Any], color: tuple[int, int, int], label: str) -> None:
    x = float(bbox["x"])
    y = float(bbox["y"])
    w = float(bbox["width"])
    h = float(bbox["height"])
    draw.rectangle((x, y, x + w, y + h), outline=color, width=2)
    draw.text((x + 2, y + 2), label, fill=color)


def render_overlay(layout_record: dict[str, Any], annotation_record: dict[str, Any], output_dir: Path) -> str:
    image = Image.open(layout_record["sourceImage"]).convert("RGB")
    draw = ImageDraw.Draw(image)
    graph_by_id = {graph["graphId"]: graph for graph in layout_record["graphs"]}

    for graph_annotation in annotation_record["graphs"]:
        graph = graph_by_id[graph_annotation["graphId"]]
        draw_bbox(draw, graph["graphPanel"], (40, 155, 65), graph["graphId"])
        draw_bbox(draw, graph["plotArea"], (0, 145, 220), "plot")
        for tick in graph_annotation["tickPositions"]:
            pixel = tick["pixel"]
            color = (175, 0, 210) if tick["axis"] == "X" else (230, 120, 0)
            draw.ellipse((pixel["x"] - 4, pixel["y"] - 4, pixel["x"] + 4, pixel["y"] + 4), fill=color)
        for label in graph_annotation["numericLabelBoxes"]:
            draw_bbox(draw, label["bbox"], (230, 190, 0), f"{label['axis']} {label['text']}")
        for text in graph_annotation["textRoleLabels"]:
            if text.get("rejectedAsTickLabel"):
                draw_bbox(draw, text["bbox"], (190, 0, 0), text["role"])

    output_dir.mkdir(parents=True, exist_ok=True)
    overlay_path = output_dir / f"{annotation_record['fixtureId']}_tick_text_overlay.png"
    image.save(overlay_path)
    return str(overlay_path).replace("\\", "/")


def summarize(annotations: dict[str, Any], layout: dict[str, Any], output_dir: Path) -> dict[str, Any]:
    layout_by_fixture = {record["fixtureId"]: record for record in layout["records"]}
    records = []
    for record in annotations["records"]:
        tick_count = sum(len(graph["tickPositions"]) for graph in record["graphs"])
        label_count = sum(len(graph["numericLabelBoxes"]) for graph in record["graphs"])
        anchor_count = sum(len(graph["calibrationAnchorCandidates"]) for graph in record["graphs"])
        rejected_count = sum(
            1
            for graph in record["graphs"]
            for text in graph["textRoleLabels"]
            if text.get("rejectedAsTickLabel")
        )
        overlay_path = render_overlay(layout_by_fixture[record["fixtureId"]], record, output_dir / "overlays")
        records.append(
            {
                "fixtureId": record["fixtureId"],
                "layoutClass": record["layoutClass"],
                "graphCount": len(record["graphs"]),
                "majorTickCount": tick_count,
                "numericLabelBoxCount": label_count,
                "reviewAnchorCount": anchor_count,
                "rejectedTextRoleCount": rejected_count,
                "overlayPath": overlay_path,
                "remainingGaps": record["remainingGaps"],
                "annotationStatus": record["annotationStatus"],
            }
        )
    return {
        "schemaVersion": "chromalab.benchmark.drc4_tick_text_role_summary.v1",
        "annotationStatus": annotations["annotationStatus"],
        "recordCount": len(records),
        "graphCount": sum(record["graphCount"] for record in records),
        "majorTickCount": sum(record["majorTickCount"] for record in records),
        "numericLabelBoxCount": sum(record["numericLabelBoxCount"] for record in records),
        "reviewAnchorCount": sum(record["reviewAnchorCount"] for record in records),
        "rejectedTextRoleCount": sum(record["rejectedTextRoleCount"] for record in records),
        "records": records,
    }


def write_markdown(path: Path, summary: dict[str, Any]) -> None:
    lines = [
        "# DR-C4 Tick/Grid And Text Role Annotation Summary",
        "",
        f"Status: `{summary['annotationStatus']}`",
        f"Fixtures: `{summary['recordCount']}`",
        f"Graphs: `{summary['graphCount']}`",
        f"Major tick positions: `{summary['majorTickCount']}`",
        f"Numeric label boxes: `{summary['numericLabelBoxCount']}`",
        f"Review calibration anchors: `{summary['reviewAnchorCount']}`",
        f"Rejected non-tick text roles: `{summary['rejectedTextRoleCount']}`",
        "",
        "| Fixture | Layout | Graphs | Major ticks | Numeric labels | Review anchors | Rejected text | Overlay | Remaining gaps |",
        "| --- | --- | ---: | ---: | ---: | ---: | ---: | --- | --- |",
    ]
    for record in summary["records"]:
        lines.append(
            "| `{fixtureId}` | {layoutClass} | {graphCount} | {majorTickCount} | "
            "{numericLabelBoxCount} | {reviewAnchorCount} | {rejectedTextRoleCount} | "
            "`{overlayPath}` | {remainingGaps} |".format(
                fixtureId=record["fixtureId"],
                layoutClass=record["layoutClass"],
                graphCount=record["graphCount"],
                majorTickCount=record["majorTickCount"],
                numericLabelBoxCount=record["numericLabelBoxCount"],
                reviewAnchorCount=record["reviewAnchorCount"],
                rejectedTextRoleCount=record["rejectedTextRoleCount"],
                overlayPath=record["overlayPath"],
                remainingGaps=", ".join(record["remainingGaps"]),
            )
        )
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--layout",
        type=Path,
        default=Path("benchmark/annotations/drc3_initial_graph_layout_annotations/manual-p0-annotations.json"),
        help="DR-C3 layout annotation JSON.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("benchmark/annotations/drc4_tick_text_role_annotations"),
        help="Output directory.",
    )
    parser.add_argument("--clean", action="store_true", help="Delete output directory before generation.")
    args = parser.parse_args()

    if args.clean and args.output.exists():
        shutil.rmtree(args.output)

    layout = read_json(args.layout)
    annotations = build_annotations(layout)
    summary = summarize(annotations, layout, args.output)
    write_json(args.output / "manual-p0-tick-text-annotations.json", annotations)
    write_json(args.output / "summary.json", summary)
    write_markdown(args.output / "summary.md", summary)
    print(
        "Built DR-C4 annotations for {fixtures} fixtures, {graphs} graphs, {ticks} major ticks.".format(
            fixtures=summary["recordCount"],
            graphs=summary["graphCount"],
            ticks=summary["majorTickCount"],
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
