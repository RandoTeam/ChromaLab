#!/usr/bin/env python3
"""Build DR-C2 annotation workflow records from the DR-C1 annotation plan."""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
from pathlib import Path
from typing import Any


FIELD_DEFINITIONS = {
    "expected_physical_graph_count": {
        "owner": "Chromatography SME + QA",
        "instruction": "Count physical graph outputs from visual evidence; distinguish stacked traces from separate panels.",
    },
    "layout_class": {
        "owner": "Geometry + Chromatography SME",
        "instruction": "Classify the graph layout taxonomy before graph count scoring.",
    },
    "panel_groups": {
        "owner": "Geometry",
        "instruction": "Group panels that share axes and separate panels with distinct axes.",
    },
    "graph_panel_bounds": {
        "owner": "Geometry",
        "instruction": "Annotate complete graphPanel boxes including titles, axes, labels, ticks, and plot frame context.",
    },
    "plot_area_bounds": {
        "owner": "Geometry",
        "instruction": "Annotate data-only plotArea boxes inside graphPanel boxes.",
    },
    "axis_endpoints": {
        "owner": "Geometry",
        "instruction": "Annotate X/Y axis endpoints or record a diagnostic-only missing reason.",
    },
    "plot_frame_edges": {
        "owner": "Geometry",
        "instruction": "Annotate visible plot frame edges that can support axis scale recovery.",
    },
    "tick_or_grid_positions": {
        "owner": "Geometry + OCR",
        "instruction": "Annotate visible tick marks or grid positions; do not invent hidden ticks.",
    },
    "numeric_label_boxes": {
        "owner": "OCR / Text Semantics",
        "instruction": "Annotate numeric OCR label boxes in axis bands with parsed values when legible.",
    },
    "text_role_labels": {
        "owner": "OCR / Text Semantics + Scientific Reporting",
        "instruction": "Role-label all relevant text, including tick labels, axis titles, legends, ion/mz metadata, and rejected text.",
    },
    "rejected_non_graph_regions": {
        "owner": "QA + Geometry",
        "instruction": "Mark title, table, legend, UI, and non-plot regions that must not become graph panels.",
    },
}


def read_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def image_info(image_path: str, repo_root: Path) -> dict[str, Any]:
    path = repo_root / image_path
    if not path.exists():
        return {
            "path": image_path,
            "exists": False,
            "sha256": None,
            "bytes": None,
            "missingReason": "Source image path from DR-C1 annotation plan was not found.",
        }
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    return {
        "path": image_path,
        "exists": True,
        "sha256": digest,
        "bytes": path.stat().st_size,
    }


def field_record(field: str) -> dict[str, Any]:
    definition = FIELD_DEFINITIONS[field]
    return {
        "field": field,
        "status": "NEEDS_ANNOTATION",
        "owner": definition["owner"],
        "instruction": definition["instruction"],
        "value": None,
        "missingReason": "Not annotated in DR-C2 workflow setup.",
        "evidencePath": None,
    }


def workflow_record(task: dict[str, Any], repo_root: Path) -> dict[str, Any]:
    return {
        "schemaVersion": "chromalab.benchmark.drc2_annotation_workflow.v1",
        "fixtureId": task["fixtureId"],
        "priority": task["priority"],
        "annotationStatus": "NOT_STARTED",
        "coordinateSpace": task.get("coordinateSpace") or "NORMALIZED_IMAGE",
        "sourceImages": [image_info(path, repo_root) for path in task.get("sourceImages", [])],
        "blockingReasons": task.get("blockingReasons") or [],
        "modes": task.get("modes") or [],
        "fields": [field_record(field) for field in task.get("requiredFields", [])],
        "acceptanceChecks": [
            "Every required field is ANNOTATED or has an explicit DIAGNOSTIC_ONLY missing reason.",
            "Graph count signoff is separate from candidate selection.",
            "PlotArea is inside graphPanel.",
            "Text roles reject ion/mz/title/legend numbers as tick labels.",
            "No VLM/E2B pixel geometry or numeric calibration truth is accepted.",
        ],
    }


def build_workflow(plan: dict[str, Any], repo_root: Path) -> dict[str, Any]:
    records = [workflow_record(task, repo_root) for task in plan.get("tasks", [])]
    priority_counts: dict[str, int] = {}
    field_counts: dict[str, int] = {field: 0 for field in FIELD_DEFINITIONS}
    missing_images = []
    for record in records:
        priority = record["priority"]
        priority_counts[priority] = priority_counts.get(priority, 0) + 1
        for field in record["fields"]:
            field_counts[field["field"]] += 1
        for image in record["sourceImages"]:
            if not image["exists"]:
                missing_images.append({"fixtureId": record["fixtureId"], "path": image["path"]})
    return {
        "schemaVersion": "chromalab.benchmark.drc2_annotation_workflow_summary.v1",
        "sourcePlan": "benchmark/annotations/drc1_graph_axis_annotation_plan/annotation-plan.json",
        "recordCount": len(records),
        "priorityCounts": priority_counts,
        "fieldCounts": {field: count for field, count in field_counts.items() if count},
        "missingImages": missing_images,
        "records": records,
    }


def write_record_files(output: Path, workflow: dict[str, Any]) -> None:
    records_root = output / "records"
    for record in workflow["records"]:
        write_json(records_root / f"{record['fixtureId']}.json", record)


def write_markdown(path: Path, workflow: dict[str, Any]) -> None:
    lines = [
        "# DR-C2 Annotation Workflow",
        "",
        f"Records: `{workflow['recordCount']}`",
        "",
        "| Fixture | Priority | Images | Required fields | Blocking reasons | Status |",
        "| --- | --- | --- | ---: | --- | --- |",
    ]
    for record in workflow["records"]:
        image_status = ", ".join(
            f"`{image['path']}` ({'ok' if image['exists'] else 'missing'})"
            for image in record["sourceImages"]
        )
        lines.append(
            "| `{fixtureId}` | {priority} | {images} | {field_count} | {blocking} | {status} |".format(
                fixtureId=record["fixtureId"],
                priority=record["priority"],
                images=image_status or "MISSING",
                field_count=len(record["fields"]),
                blocking=", ".join(record["blockingReasons"]) or "None",
                status=record["annotationStatus"],
            )
        )
    lines.extend(
        [
            "",
            "## Field Checklist",
            "",
            "| Field | Fixtures needing it | Owner |",
            "| --- | ---: | --- |",
        ]
    )
    for field, count in workflow["fieldCounts"].items():
        lines.append(f"| `{field}` | {count} | {FIELD_DEFINITIONS[field]['owner']} |")
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def write_dashboard_rows(output: Path, workflow: dict[str, Any]) -> None:
    rows = []
    for record in workflow["records"]:
        modes = record.get("modes") or []
        mode_decisions = ", ".join(f"{mode['mode']}={mode['decision']}" for mode in modes)
        runtime_failures = sorted(
            set(mode.get("runtimeFailureClass") for mode in modes if mode.get("runtimeFailureClass"))
        )
        rows.append(
            {
                "fixtureId": record["fixtureId"],
                "priority": record["priority"],
                "annotationStatus": record["annotationStatus"],
                "requiredFieldCount": len(record["fields"]),
                "sourceImageCount": len(record["sourceImages"]),
                "allImagesExist": all(image["exists"] for image in record["sourceImages"]),
                "blockingReasons": ", ".join(record["blockingReasons"]) or "None",
                "modeDecisions": mode_decisions,
                "runtimeFailures": ", ".join(runtime_failures) or "None",
            }
        )
    write_json(output / "dashboard-data.json", {"rows": rows})


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--plan",
        type=Path,
        default=Path("benchmark/annotations/drc1_graph_axis_annotation_plan/annotation-plan.json"),
        help="DR-C1 annotation plan JSON.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("benchmark/annotations/drc2_graph_layout_annotation_workflow"),
        help="Output directory.",
    )
    parser.add_argument(
        "--clean",
        action="store_true",
        help="Delete output directory before writing workflow records.",
    )
    args = parser.parse_args()

    if not args.plan.exists():
        raise SystemExit(f"DR-C1 annotation plan not found: {args.plan}")
    if args.clean and args.output.exists():
        shutil.rmtree(args.output)

    workflow = build_workflow(read_json(args.plan), Path.cwd())
    write_json(args.output / "workflow-summary.json", workflow)
    write_markdown(args.output / "workflow-summary.md", workflow)
    write_record_files(args.output, workflow)
    write_dashboard_rows(args.output, workflow)
    print(f"Created {workflow['recordCount']} DR-C2 workflow records in {args.output}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
