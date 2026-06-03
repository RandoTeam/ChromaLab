#!/usr/bin/env python3
"""Create DR-C1 graph/layout/axis annotation tasks from Phase 9J truth gaps."""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any


DRC1_FIELDS = [
    "expected_physical_graph_count",
    "layout_class",
    "panel_groups",
    "graph_panel_bounds",
    "plot_area_bounds",
    "axis_endpoints",
    "plot_frame_edges",
    "tick_or_grid_positions",
    "numeric_label_boxes",
    "text_role_labels",
    "rejected_non_graph_regions",
]


def read_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def case_dir(case_id: str, benchmark_root: Path) -> Path:
    return benchmark_root / case_id


def original_image_path(case_id: str, benchmark_root: Path) -> str | None:
    evidence_path = case_dir(case_id, benchmark_root) / "evidence-package.json"
    if not evidence_path.exists():
        return None
    evidence = read_json(evidence_path)
    for artifact in evidence.get("artifacts", []):
        if artifact.get("artifactType") == "original_image":
            return artifact.get("path")
    return None


def drc1_priority(fixture: dict[str, Any]) -> str:
    priority = fixture.get("highestPriority") or "P2"
    if priority == "P0":
        return "P0_GRAPH_LAYOUT_BLOCKER"
    if priority == "P1":
        return "P1_REVIEW_TRUTH_GAP"
    return "P2_BACKGROUND_REVIEW"


def required_fields_for_fixture(fixture: dict[str, Any]) -> list[str]:
    gaps = set(fixture.get("truthGaps") or [])
    fields = {
        "expected_physical_graph_count",
        "layout_class",
        "panel_groups",
        "graph_panel_bounds",
        "plot_area_bounds",
        "rejected_non_graph_regions",
    }
    if "axis_endpoints" in gaps:
        fields.add("axis_endpoints")
    if "tick_or_grid_positions" in gaps:
        fields.update(["axis_endpoints", "plot_frame_edges", "tick_or_grid_positions"])
    if "numeric_label_boxes" in gaps:
        fields.update(["numeric_label_boxes", "text_role_labels"])
    if "calibration_anchors" in gaps:
        fields.update(["axis_endpoints", "tick_or_grid_positions", "numeric_label_boxes"])
    return [field for field in DRC1_FIELDS if field in fields]


def source_images_for_fixture(fixture: dict[str, Any], benchmark_root: Path) -> list[str]:
    images = []
    for mode in fixture.get("modes", []):
        path = original_image_path(mode["caseId"], benchmark_root)
        if path and path not in images:
            images.append(path)
    return images


def task_for_fixture(fixture: dict[str, Any], benchmark_root: Path) -> dict[str, Any]:
    required_fields = required_fields_for_fixture(fixture)
    return {
        "fixtureId": fixture["fixtureId"],
        "priority": drc1_priority(fixture),
        "sourceImages": source_images_for_fixture(fixture, benchmark_root),
        "blockingReasons": fixture.get("blockingReasons") or [],
        "modes": fixture.get("modes") or [],
        "requiredFields": required_fields,
        "coordinateSpace": "NORMALIZED_IMAGE",
        "annotationStatus": "NEEDS_ANNOTATION",
        "drCReadyWhen": [
            "graph_panel_bounds are present for every physical graph",
            "plot_area_bounds are present and inside graph_panel_bounds",
            "expected_physical_graph_count is signed off for stacked or multi-panel layouts",
            "axis_endpoints are present or an explicit diagnostic-only reason is recorded",
            "numeric_label_boxes are classified by text role",
            "tick_or_grid_positions are annotated when visible",
        ],
        "doNotUseAs": [
            "fixture-specific candidate-selection coordinates",
            "release-ready numeric accuracy proof",
            "VLM numeric authority",
        ],
    }


def build_plan(truth_gaps: dict[str, Any], benchmark_root: Path) -> dict[str, Any]:
    tasks = [task_for_fixture(fixture, benchmark_root) for fixture in truth_gaps.get("fixtures", [])]
    return {
        "schemaVersion": "chromalab.benchmark.drc1_annotation_plan.v1",
        "sourceTruthGaps": "benchmark/reports/phase9j_truth_audit_score/truth-gaps.json",
        "benchmarkRoot": str(benchmark_root).replace("\\", "/"),
        "taskCount": len(tasks),
        "coordinateSpace": "NORMALIZED_IMAGE",
        "requiredFields": DRC1_FIELDS,
        "tasks": tasks,
    }


def write_markdown(path: Path, plan: dict[str, Any]) -> None:
    lines = [
        "# DR-C1 Graph Layout And Axis Annotation Plan",
        "",
        f"Tasks: `{plan['taskCount']}`",
        f"Coordinate space: `{plan['coordinateSpace']}`",
        "",
        "| Fixture | Priority | Source images | Required fields | Blocking reasons |",
        "| --- | --- | --- | --- | --- |",
    ]
    for task in plan["tasks"]:
        lines.append(
            "| `{fixtureId}` | {priority} | {images} | {fields} | {blocking} |".format(
                fixtureId=task["fixtureId"],
                priority=task["priority"],
                images=", ".join(f"`{path}`" for path in task["sourceImages"]) or "MISSING",
                fields=", ".join(task["requiredFields"]),
                blocking=", ".join(task["blockingReasons"]) or "None",
            )
        )

    lines.extend(
        [
            "",
            "## Annotation Rules",
            "",
            "- Expected graph count is truth, not an input to candidate selection.",
            "- Graph panels include the complete physical graph, titles, axis labels, ticks, and plot frame context.",
            "- Plot areas contain the data region only and exclude titles, legends, and tick-label bands.",
            "- Numeric label boxes must be role-labeled before they can be used by calibration.",
            "- ION, m/z, title, legend, and peak annotation numbers must be explicitly rejected as tick labels.",
            "- E2B/VLM may describe or warn, but cannot provide pixel geometry or numeric calibration truth.",
        ]
    )
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--truth-gaps",
        type=Path,
        default=Path("benchmark/reports/phase9j_truth_audit_score/truth-gaps.json"),
        help="DR-B3 truth gaps JSON.",
    )
    parser.add_argument(
        "--benchmark-root",
        type=Path,
        default=Path("benchmark/examples/phase9j_truth_audit"),
        help="Phase 9J benchmark record root.",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("benchmark/annotations/drc1_graph_axis_annotation_plan"),
        help="Output directory.",
    )
    args = parser.parse_args()

    if not args.truth_gaps.exists():
        raise SystemExit(f"Truth gaps file not found: {args.truth_gaps}")
    if not args.benchmark_root.exists():
        raise SystemExit(f"Benchmark root not found: {args.benchmark_root}")

    plan = build_plan(read_json(args.truth_gaps), args.benchmark_root)
    write_json(args.output / "annotation-plan.json", plan)
    write_markdown(args.output / "annotation-plan.md", plan)
    print(f"Created {plan['taskCount']} DR-C1 annotation tasks in {args.output}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
