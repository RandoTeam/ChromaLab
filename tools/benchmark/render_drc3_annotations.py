#!/usr/bin/env python3
"""Render and summarize DR-C3 initial graph layout annotations."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

from PIL import Image, ImageDraw


COLORS = {
    "graphPanel": (38, 160, 70),
    "plotArea": (0, 145, 220),
    "xAxis": (220, 120, 0),
    "yAxis": (200, 0, 120),
    "rejected": (180, 0, 0),
    "labelBand": (230, 190, 0),
}


def read_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def bbox_tuple(bbox: dict[str, Any]) -> tuple[float, float, float, float]:
    x = float(bbox["x"])
    y = float(bbox["y"])
    return (x, y, x + float(bbox["width"]), y + float(bbox["height"]))


def draw_bbox(draw: ImageDraw.ImageDraw, bbox: dict[str, Any], color: tuple[int, int, int], label: str) -> None:
    xy = bbox_tuple(bbox)
    draw.rectangle(xy, outline=color, width=3)
    draw.text((xy[0] + 4, xy[1] + 4), label, fill=color)


def draw_axis(draw: ImageDraw.ImageDraw, axis: dict[str, Any], color: tuple[int, int, int], label: str) -> None:
    start = axis["start"]
    end = axis["end"]
    draw.line((start["x"], start["y"], end["x"], end["y"]), fill=color, width=3)
    draw.text((start["x"] + 4, start["y"] + 4), label, fill=color)


def render_record(record: dict[str, Any], repo_root: Path, overlay_root: Path) -> dict[str, Any]:
    image_path = repo_root / record["sourceImage"]
    image = Image.open(image_path).convert("RGB")
    draw = ImageDraw.Draw(image)

    for graph in record["graphs"]:
        graph_id = graph["graphId"].replace(f"{record['fixtureId']}_", "")
        draw_bbox(draw, graph["graphPanel"], COLORS["graphPanel"], f"{graph_id} graphPanel")
        draw_bbox(draw, graph["plotArea"], COLORS["plotArea"], f"{graph_id} plotArea")
        axes = graph.get("axisLines") or {}
        if axes.get("xAxis"):
            draw_axis(draw, axes["xAxis"], COLORS["xAxis"], f"{graph_id} X")
        if axes.get("yAxis"):
            draw_axis(draw, axes["yAxis"], COLORS["yAxis"], f"{graph_id} Y")
        for band_name, band in (graph.get("labelBands") or {}).items():
            draw_bbox(draw, band, COLORS["labelBand"], f"{graph_id} {band_name}")

    for rejected in record.get("rejectedNonGraphRegions", []):
        draw_bbox(draw, rejected["bbox"], COLORS["rejected"], rejected["regionId"])

    overlay_root.mkdir(parents=True, exist_ok=True)
    overlay_path = overlay_root / f"{record['fixtureId']}_overlay.png"
    image.save(overlay_path)

    return {
        "fixtureId": record["fixtureId"],
        "sourceImage": record["sourceImage"],
        "overlayPath": str(overlay_path).replace("\\", "/"),
        "expectedPhysicalGraphCount": record["expectedPhysicalGraphCount"],
        "annotatedGraphCount": len(record["graphs"]),
        "layoutClass": record["layoutClass"],
        "annotationConfidence": record["annotationConfidence"],
        "remainingGaps": record.get("remainingGaps") or [],
    }


def build_summary(records: list[dict[str, Any]], overlay_records: list[dict[str, Any]]) -> dict[str, Any]:
    graph_count_matches = sum(
        1 for record in overlay_records if record["expectedPhysicalGraphCount"] == record["annotatedGraphCount"]
    )
    return {
        "schemaVersion": "chromalab.benchmark.drc3_initial_annotation_summary.v1",
        "recordCount": len(records),
        "graphCountMatches": graph_count_matches,
        "overlayCount": len(overlay_records),
        "status": "PARTIAL_INITIAL_REVIEW",
        "records": overlay_records,
    }


def write_markdown(path: Path, summary: dict[str, Any]) -> None:
    lines = [
        "# DR-C3 Initial P0 Graph Layout Annotation Summary",
        "",
        f"Status: `{summary['status']}`",
        f"Records: `{summary['recordCount']}`",
        f"Overlays: `{summary['overlayCount']}`",
        "",
        "| Fixture | Layout | Expected graphs | Annotated graphs | Overlay | Remaining gaps |",
        "| --- | --- | ---: | ---: | --- | --- |",
    ]
    for record in summary["records"]:
        lines.append(
            "| `{fixtureId}` | {layoutClass} | {expectedPhysicalGraphCount} | {annotatedGraphCount} | "
            "`{overlayPath}` | {remainingGaps} |".format(
                fixtureId=record["fixtureId"],
                layoutClass=record["layoutClass"],
                expectedPhysicalGraphCount=record["expectedPhysicalGraphCount"],
                annotatedGraphCount=record["annotatedGraphCount"],
                overlayPath=record["overlayPath"],
                remainingGaps=", ".join(record["remainingGaps"]) or "None",
            )
        )
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--annotations",
        type=Path,
        default=Path("benchmark/annotations/drc3_initial_graph_layout_annotations/manual-p0-annotations.json"),
        help="Manual DR-C3 annotation JSON.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("benchmark/annotations/drc3_initial_graph_layout_annotations"),
        help="Output directory for overlays and summary.",
    )
    args = parser.parse_args()

    payload = read_json(args.annotations)
    repo_root = Path.cwd()
    overlay_root = args.output / "overlays"
    overlay_records = [render_record(record, repo_root, overlay_root) for record in payload["records"]]
    summary = build_summary(payload["records"], overlay_records)
    write_json(args.output / "summary.json", summary)
    write_markdown(args.output / "summary.md", summary)
    print(f"Rendered {len(overlay_records)} DR-C3 annotation overlays in {overlay_root}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
