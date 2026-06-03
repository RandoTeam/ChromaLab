#!/usr/bin/env python3
"""Compare P0 graph layout detection prototypes against annotation truth.

This is a PC-side research harness only. It does not change Android runtime,
production graph detection, validators, or chromatographic math.
"""

from __future__ import annotations

import argparse
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image, ImageDraw


MODES = ["deterministic", "model_enabled"]


@dataclass(frozen=True)
class RowCandidate:
    row: int
    score: float
    dark_run: int
    cluster_height: int
    below_dark_fraction: float
    above_dark_fraction: float


@dataclass(frozen=True)
class ImageFeatures:
    content_crop: tuple[int, int, int, int]
    row_candidates: list[RowCandidate]


@dataclass(frozen=True)
class MethodResult:
    method_id: str
    selected_rows: list[int]
    detected_graph_count: int
    predicted_layout_class: str
    confidence: str
    reason: str


def read_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


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
    for value in values:
        point = int(value)
        if last is None or point - last <= max_gap:
            current.append(point)
        else:
            clusters.append(current)
            current = [point]
        last = point
    if current:
        clusters.append(current)
    return clusters


def detect_image_features(image_path: Path) -> ImageFeatures:
    gray = np.array(Image.open(image_path).convert("L"))
    bright = gray > 150
    row_bright_fraction = bright.mean(axis=1)
    col_bright_fraction = bright.mean(axis=0)
    rows = np.where(row_bright_fraction > 0.18)[0]
    cols = np.where(col_bright_fraction > 0.18)[0]
    if len(rows) == 0 or len(cols) == 0:
        return ImageFeatures((0, 0, gray.shape[1], gray.shape[0]), [])

    y0 = max(0, int(rows[0]) - 8)
    y1 = min(gray.shape[0], int(rows[-1]) + 9)
    x0 = max(0, int(cols[0]) - 8)
    x1 = min(gray.shape[1], int(cols[-1]) + 9)
    crop = gray[y0:y1, x0:x1]

    threshold = min(185, max(65, float(np.percentile(crop, 28))))
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

    candidates: list[RowCandidate] = []
    for cluster in cluster_numbers(candidate_rows, max_gap=14):
        selected = max(cluster, key=lambda row: row_runs[row])
        below = mask[selected + 5 : min(mask.shape[0], selected + 65)]
        above = mask[max(0, selected - 65) : max(0, selected - 5)]
        candidates.append(
            RowCandidate(
                row=y0 + int(selected),
                score=float(row_scores[selected]),
                dark_run=int(row_runs[selected]),
                cluster_height=len(cluster),
                below_dark_fraction=float(below.mean()) if below.size else 0.0,
                above_dark_fraction=float(above.mean()) if above.size else 0.0,
            )
        )
    return ImageFeatures((x0, y0, x1, y1), candidates)


def predict_layout_class(graph_count: int) -> str:
    if graph_count <= 0:
        return "UNKNOWN_REVIEW"
    if graph_count == 1:
        return "SINGLE_TRACE_SINGLE_AXIS"
    return "MULTI_PANEL_SEPARATE_AXES"


def method_full_width_axis_projection(features: ImageFeatures) -> MethodResult:
    rows = [
        candidate.row
        for candidate in features.row_candidates
        if candidate.score >= 0.75
    ]
    return MethodResult(
        method_id="full_width_axis_projection_v1",
        selected_rows=rows,
        detected_graph_count=len(rows),
        predicted_layout_class=predict_layout_class(len(rows)),
        confidence="HIGH_FOR_EXPORTS_LOW_FOR_PHOTO_FOOTERS",
        reason="Counts long horizontal frame/axis rows; strong on PNG exports, vulnerable to page/footer lines.",
    )


def method_label_band_assisted_axis_projection(features: ImageFeatures) -> MethodResult:
    high_rows = [
        candidate.row
        for candidate in features.row_candidates
        if candidate.score >= 0.75
    ]
    if len(high_rows) >= 2:
        rows = high_rows
        reason = "Used high-confidence full-width row clusters."
    else:
        medium = [
            candidate
            for candidate in features.row_candidates
            if candidate.score >= 0.47
            and candidate.cluster_height <= 20
            and candidate.below_dark_fraction >= 0.10
            and candidate.above_dark_fraction <= candidate.below_dark_fraction + 0.02
        ]
        grouped: list[int] = []
        for cluster in cluster_numbers([candidate.row for candidate in medium], max_gap=60):
            group_candidates = [candidate for candidate in medium if candidate.row in cluster]
            selected = max(group_candidates, key=lambda candidate: candidate.score)
            grouped.append(selected.row)
        rows = grouped
        reason = "Fell back to medium axis rows with darker label-band evidence below the row."

    return MethodResult(
        method_id="label_band_assisted_axis_projection_v1",
        selected_rows=rows,
        detected_graph_count=len(rows),
        predicted_layout_class=predict_layout_class(len(rows)),
        confidence="PROTOTYPE_GRAPH_COUNT_ONLY",
        reason=reason,
    )


def phase9j_case_id(fixture_id: str, mode: str) -> str:
    return f"phase9j_{fixture_id}_{mode}"


def current_phase9j_result(benchmark_root: Path, fixture_id: str, mode: str) -> MethodResult:
    metrics = read_json(benchmark_root / phase9j_case_id(fixture_id, mode) / "metrics.json")
    graph_panel_stage = next(
        (
            stage
            for stage in metrics.get("stageScores", [])
            if stage.get("stage") == "graph_panel"
        ),
        {},
    )
    graph_metrics = graph_panel_stage.get("metrics", {})
    detected = int(graph_metrics.get("detectedGraphCount") or 0)
    return MethodResult(
        method_id="android_phase9j_current",
        selected_rows=[],
        detected_graph_count=detected,
        predicted_layout_class="NOT_EXPOSED",
        confidence="ANDROID_BASELINE",
        reason="Current normalized Phase 9J benchmark output.",
    )


def score_result(result: MethodResult, truth: dict[str, Any]) -> dict[str, Any]:
    expected_count = int(truth["expectedPhysicalGraphCount"])
    expected_layout = truth["layoutClass"]
    graph_count_score = "PASS" if result.detected_graph_count == expected_count else "FAIL"
    if result.predicted_layout_class == expected_layout:
        layout_score = "PASS"
    elif result.predicted_layout_class in {"UNKNOWN_REVIEW", "NOT_EXPOSED"}:
        layout_score = "MISSING"
    else:
        layout_score = "FAIL"
    return {
        "methodId": result.method_id,
        "fixtureId": truth["fixtureId"],
        "expectedGraphCount": expected_count,
        "detectedGraphCount": result.detected_graph_count,
        "graphCountScore": graph_count_score,
        "expectedLayoutClass": expected_layout,
        "predictedLayoutClass": result.predicted_layout_class,
        "layoutClassScore": layout_score,
        "selectedRows": result.selected_rows,
        "confidence": result.confidence,
        "reason": result.reason,
    }


def render_overlay(image_path: Path, output_path: Path, features: ImageFeatures, result: MethodResult) -> None:
    image = Image.open(image_path).convert("RGB")
    draw = ImageDraw.Draw(image)
    x0, y0, x1, y1 = features.content_crop
    draw.rectangle((x0, y0, x1, y1), outline=(255, 190, 0), width=3)
    for candidate in features.row_candidates:
        color = (90, 90, 255)
        if candidate.row in result.selected_rows:
            color = (0, 210, 90)
        draw.line((x0, candidate.row, x1, candidate.row), fill=color, width=3)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    image.save(output_path)


def build_markdown(summary: dict[str, Any]) -> str:
    lines = [
        "# DR-C6 P0 Graph Layout Method Comparison",
        "",
        f"Verdict: `{summary['overallVerdict']}`",
        f"Fixtures: `{summary['fixtureCount']}`",
        "",
        "## Method Summary",
        "",
        "| Method | Cases | Graph-count pass | Layout pass | Notes |",
        "| --- | ---: | ---: | ---: | --- |",
    ]
    for method in summary["methodSummaries"]:
        lines.append(
            "| `{methodId}` | {caseCount} | {graphCountPass} | {layoutClassPass} | {notes} |".format(
                **method
            )
        )

    lines.extend(
        [
            "",
            "## Prototype Scores",
            "",
            "| Method | Fixture | Expected graphs | Detected | Graph count | Expected layout | Predicted layout | Layout score | Selected rows |",
            "| --- | --- | ---: | ---: | --- | --- | --- | --- | --- |",
        ]
    )
    for case in summary["prototypeScores"]:
        lines.append(
            "| `{methodId}` | `{fixtureId}` | {expectedGraphCount} | {detectedGraphCount} | {graphCountScore} | "
            "{expectedLayoutClass} | {predictedLayoutClass} | {layoutClassScore} | {selectedRows} |".format(
                **{
                    **case,
                    "selectedRows": ", ".join(str(row) for row in case["selectedRows"]) or "-",
                }
            )
        )

    lines.extend(
        [
            "",
            "## Current Android Baseline Scores",
            "",
            "| Fixture | Mode | Expected graphs | Detected | Graph count |",
            "| --- | --- | ---: | ---: | --- |",
        ]
    )
    for case in summary["currentBaselineScores"]:
        lines.append(
            "| `{fixtureId}` | {mode} | {expectedGraphCount} | {detectedGraphCount} | {graphCountScore} |".format(
                **case
            )
        )

    lines.extend(
        [
            "",
            "## Interpretation",
            "",
            "- `label_band_assisted_axis_projection_v1` is not production-ready, but it proves the P0 graph-count problem is not inherently blocked by the images.",
            "- Exact layout class remains unresolved without text-role or panel semantics: `TIC_PLUS_ION_PANELS` and `TWO_GRAPH_PAGE` cannot be safely separated from geometry-only row counts.",
            "- Next step should add a second-stage panel classifier using graph bounds, title/ion text roles, and axis ownership before any Android runtime port.",
        ]
    )
    return "\n".join(lines) + "\n"


def summarize_method(method_id: str, cases: list[dict[str, Any]], notes: str) -> dict[str, Any]:
    selected = [case for case in cases if case["methodId"] == method_id]
    return {
        "methodId": method_id,
        "caseCount": len(selected),
        "graphCountPass": sum(1 for case in selected if case["graphCountScore"] == "PASS"),
        "layoutClassPass": sum(1 for case in selected if case["layoutClassScore"] == "PASS"),
        "notes": notes,
    }


def build_summary(args: argparse.Namespace) -> dict[str, Any]:
    drc3 = read_json(args.drc3_summary)
    output = args.output
    overlay_dir = output / "overlays"

    prototype_scores: list[dict[str, Any]] = []
    current_scores: list[dict[str, Any]] = []
    feature_records: list[dict[str, Any]] = []
    for truth in drc3["records"]:
        fixture_id = truth["fixtureId"]
        image_path = args.repo_root / truth["sourceImage"]
        features = detect_image_features(image_path)
        feature_records.append(
            {
                "fixtureId": fixture_id,
                "sourceImage": truth["sourceImage"],
                "contentCrop": list(features.content_crop),
                "rowCandidates": [
                    {
                        "row": candidate.row,
                        "score": round(candidate.score, 4),
                        "darkRun": candidate.dark_run,
                        "clusterHeight": candidate.cluster_height,
                        "belowDarkFraction": round(candidate.below_dark_fraction, 4),
                        "aboveDarkFraction": round(candidate.above_dark_fraction, 4),
                    }
                    for candidate in features.row_candidates
                ],
            }
        )

        for method_result in [
            method_full_width_axis_projection(features),
            method_label_band_assisted_axis_projection(features),
        ]:
            score = score_result(method_result, truth)
            overlay_path = overlay_dir / f"{fixture_id}_{method_result.method_id}.png"
            render_overlay(image_path, overlay_path, features, method_result)
            score["overlayPath"] = str(overlay_path).replace("\\", "/")
            prototype_scores.append(score)

        for mode in MODES:
            current_result = current_phase9j_result(args.benchmark_root, fixture_id, mode)
            current_score = score_result(current_result, truth)
            current_score["mode"] = mode
            current_scores.append(current_score)

    method_summaries = [
        summarize_method(
            "android_phase9j_current",
            current_scores,
            "Current Android output still collapses P0 multi-panel fixtures to one graph.",
        ),
        summarize_method(
            "full_width_axis_projection_v1",
            prototype_scores,
            "Strong on PNG/export panels; weak on photographed pages with footer/page-frame lines.",
        ),
        summarize_method(
            "label_band_assisted_axis_projection_v1",
            prototype_scores,
            "Best PC prototype for P0 graph count; still lacks exact semantic layout classification.",
        ),
    ]
    best = next(
        method for method in method_summaries if method["methodId"] == "label_band_assisted_axis_projection_v1"
    )
    verdict = (
        "PC_PROTOTYPE_IMPROVES_GRAPH_COUNT_NOT_READY_FOR_RUNTIME"
        if best["graphCountPass"] > method_summaries[0]["graphCountPass"]
        else "NO_PROTOTYPE_IMPROVEMENT"
    )
    return {
        "schemaVersion": "chromalab.benchmark.drc6_p0_graph_layout_method_comparison.v1",
        "truthSource": str(args.drc3_summary).replace("\\", "/"),
        "benchmarkRoot": str(args.benchmark_root).replace("\\", "/"),
        "fixtureCount": len(drc3["records"]),
        "overallVerdict": verdict,
        "methodSummaries": method_summaries,
        "currentBaselineScores": current_scores,
        "prototypeScores": prototype_scores,
        "imageFeatures": feature_records,
        "productionImpact": "NONE_RESEARCH_ONLY",
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo-root", type=Path, default=Path("."))
    parser.add_argument(
        "--benchmark-root",
        type=Path,
        default=Path("benchmark/examples/phase9j_truth_audit"),
    )
    parser.add_argument(
        "--drc3-summary",
        type=Path,
        default=Path("benchmark/annotations/drc3_initial_graph_layout_annotations/summary.json"),
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("benchmark/reports/drc6_p0_graph_layout_method_comparison"),
    )
    args = parser.parse_args()
    summary = build_summary(args)
    write_json(args.output / "summary.json", summary)
    (args.output / "summary.md").write_text(build_markdown(summary), encoding="utf-8")
    print(
        "Compared P0 graph layout methods: {verdict}".format(
            verdict=summary["overallVerdict"]
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
