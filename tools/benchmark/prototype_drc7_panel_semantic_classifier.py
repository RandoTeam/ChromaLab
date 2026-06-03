#!/usr/bin/env python3
"""Compare P0 panel semantic layout classifiers.

This is a PC-side research harness only. Annotation-assisted methods here are
upper bounds for required OCR/page-context features, not runtime-ready methods.
"""

from __future__ import annotations

import argparse
import json
from collections import Counter
from pathlib import Path
from typing import Any


BEST_GRAPH_COUNT_METHOD = "label_band_assisted_axis_projection_v1"


def read_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def normalize_text(value: str | None) -> str:
    return str(value or "").strip().lower()


def role_counts(graphs: list[dict[str, Any]]) -> dict[str, int]:
    counts: Counter[str] = Counter()
    for graph in graphs:
        for label in graph.get("textRoleLabels", []):
            counts[str(label.get("role") or "unknown")] += 1
    return dict(counts)


def panel_family(panel_group: str) -> str:
    normalized = normalize_text(panel_group)
    if normalized == "tic" or normalized.startswith("tic_"):
        return "tic"
    if normalized.startswith("ion_"):
        return "ion"
    if normalized.startswith("xic_"):
        return "xic"
    return "other"


def text_families(graphs: list[dict[str, Any]]) -> dict[str, int]:
    families: Counter[str] = Counter()
    for graph in graphs:
        families[panel_family(str(graph.get("panelGroup") or ""))] += 1
        for label in graph.get("textRoleLabels", []):
            role = str(label.get("role") or "")
            text = normalize_text(label.get("text"))
            if role == "chart_title" and "tic" in text:
                families["tic_text"] += 1
            if role == "ion_or_mz_metadata" or text.startswith("ion "):
                families["ion_text"] += 1
            if role == "legend":
                families["legend_text"] += 1
    return dict(families)


def rejected_region_families(record: dict[str, Any]) -> dict[str, int]:
    families: Counter[str] = Counter()
    for region in record.get("rejectedNonGraphRegions", []):
        region_id = normalize_text(region.get("regionId"))
        reason = normalize_text(region.get("reason"))
        if "hand" in region_id or "background" in region_id or "hand" in reason:
            families["hand_or_background"] += 1
        if "page_header" in region_id or "metadata" in reason:
            families["page_header"] += 1
    return dict(families)


def feature_record(drc3_record: dict[str, Any], drc4_record: dict[str, Any], graph_count: int) -> dict[str, Any]:
    graphs = drc4_record.get("graphs", [])
    panel_groups = [str(graph.get("panelGroup") or "") for graph in graphs]
    return {
        "fixtureId": drc4_record["fixtureId"],
        "detectedGraphCount": graph_count,
        "panelGroups": panel_groups,
        "panelFamilies": dict(Counter(panel_family(group) for group in panel_groups)),
        "textFamilies": text_families(graphs),
        "roleCounts": role_counts(graphs),
        "rejectedRegionFamilies": rejected_region_families(drc3_record),
        "source": {
            "graphCount": "benchmark/reports/drc6_p0_graph_layout_method_comparison/summary.json",
            "textRoles": "benchmark/annotations/drc4_tick_text_role_annotations/manual-p0-tick-text-annotations.json",
            "pageContext": "benchmark/annotations/drc3_initial_graph_layout_annotations/manual-p0-annotations.json",
        },
    }


def classify_geometry_only(features: dict[str, Any]) -> tuple[str, str]:
    graph_count = int(features["detectedGraphCount"])
    if graph_count <= 0:
        return "UNKNOWN_REVIEW", "No graph rows were detected."
    if graph_count == 1:
        return "SINGLE_TRACE_SINGLE_AXIS", "Geometry detected one graph row."
    return "MULTI_PANEL_SEPARATE_AXES", "Geometry detected multiple graph rows but no semantic panel type."


def classify_text_role_panel_family(features: dict[str, Any]) -> tuple[str, str]:
    graph_count = int(features["detectedGraphCount"])
    panel_families = features["panelFamilies"]
    text_families = features["textFamilies"]
    has_tic = panel_families.get("tic", 0) > 0 or text_families.get("tic_text", 0) > 0
    has_ion = panel_families.get("ion", 0) > 0 or text_families.get("ion_text", 0) > 0
    if graph_count > 1 and has_tic and has_ion:
        return "TIC_PLUS_ION_PANELS", "Text roles expose a TIC panel plus ion/mz panels."
    if graph_count > 1:
        return "MULTI_PANEL_SEPARATE_AXES", "Text roles show multiple panels but no TIC+ion mix."
    return classify_geometry_only(features)


def classify_text_role_with_page_context(features: dict[str, Any]) -> tuple[str, str]:
    graph_count = int(features["detectedGraphCount"])
    rejected = features["rejectedRegionFamilies"]
    panel_families = features["panelFamilies"]
    if (
        graph_count == 2
        and rejected.get("hand_or_background", 0) > 0
        and panel_families.get("ion", 0) >= 2
    ):
        return "TWO_GRAPH_PAGE", "Page-context features show a photographed two-graph page with hand/background rejection."
    return classify_text_role_panel_family(features)


METHODS = {
    "geometry_only_panel_count_v1": {
        "classifier": classify_geometry_only,
        "featureSource": "drc6_graph_rows_only",
        "runtimeReadiness": "PROTOTYPE_GEOMETRY_ONLY",
        "notes": "Control method; cannot distinguish TIC+ion or two-graph page semantics.",
    },
    "annotation_text_role_panel_family_v1": {
        "classifier": classify_text_role_panel_family,
        "featureSource": "drc6_graph_rows_plus_drc4_text_role_annotations",
        "runtimeReadiness": "UPPER_BOUND_REQUIRES_AUTOMATIC_OCR_TEXT_ROLE_EXTRACTION",
        "notes": "Shows that OCR/text-role families can resolve TIC+ion panels.",
    },
    "annotation_text_role_page_context_upper_bound_v1": {
        "classifier": classify_text_role_with_page_context,
        "featureSource": "drc6_graph_rows_plus_drc4_text_roles_plus_drc3_page_context_annotations",
        "runtimeReadiness": "UPPER_BOUND_REQUIRES_AUTOMATIC_PAGE_CONTEXT_AND_TEXT_ROLE_EXTRACTION",
        "notes": "Shows that page/background context is needed to separate two-graph pages from generic ion panels.",
    },
}


def best_drc6_graph_counts(drc6_summary: dict[str, Any]) -> dict[str, int]:
    counts: dict[str, int] = {}
    for score in drc6_summary.get("prototypeScores", []):
        if score.get("methodId") == BEST_GRAPH_COUNT_METHOD:
            counts[str(score["fixtureId"])] = int(score["detectedGraphCount"])
    return counts


def score_prediction(predicted: str, expected: str) -> str:
    if predicted == expected:
        return "PASS"
    if predicted in {"UNKNOWN_REVIEW", "MISSING"}:
        return "MISSING"
    return "FAIL"


def build_case_scores(
    features_by_fixture: dict[str, dict[str, Any]],
    truth_by_fixture: dict[str, dict[str, Any]],
) -> list[dict[str, Any]]:
    cases: list[dict[str, Any]] = []
    for fixture_id, features in features_by_fixture.items():
        truth = truth_by_fixture[fixture_id]
        expected = truth["layoutClass"]
        for method_id, spec in METHODS.items():
            classifier = spec["classifier"]
            predicted, reason = classifier(features)
            cases.append(
                {
                    "methodId": method_id,
                    "fixtureId": fixture_id,
                    "expectedLayoutClass": expected,
                    "predictedLayoutClass": predicted,
                    "layoutClassScore": score_prediction(predicted, expected),
                    "detectedGraphCount": features["detectedGraphCount"],
                    "expectedGraphCount": truth["expectedPhysicalGraphCount"],
                    "featureSource": spec["featureSource"],
                    "runtimeReadiness": spec["runtimeReadiness"],
                    "reason": reason,
                }
            )
    return cases


def method_summaries(cases: list[dict[str, Any]]) -> list[dict[str, Any]]:
    summaries: list[dict[str, Any]] = []
    for method_id, spec in METHODS.items():
        selected = [case for case in cases if case["methodId"] == method_id]
        summaries.append(
            {
                "methodId": method_id,
                "caseCount": len(selected),
                "layoutClassPass": sum(1 for case in selected if case["layoutClassScore"] == "PASS"),
                "layoutClassFailOrMissing": sum(
                    1 for case in selected if case["layoutClassScore"] != "PASS"
                ),
                "featureSource": spec["featureSource"],
                "runtimeReadiness": spec["runtimeReadiness"],
                "notes": spec["notes"],
            }
        )
    return summaries


def build_markdown(summary: dict[str, Any]) -> str:
    lines = [
        "# DR-C7 Panel Semantic Layout Classifier Prototype",
        "",
        f"Verdict: `{summary['overallVerdict']}`",
        f"Fixtures: `{summary['fixtureCount']}`",
        "",
        "## Method Summary",
        "",
        "| Method | Cases | Layout pass | Fail/missing | Runtime readiness | Notes |",
        "| --- | ---: | ---: | ---: | --- | --- |",
    ]
    for method in summary["methodSummaries"]:
        lines.append(
            "| `{methodId}` | {caseCount} | {layoutClassPass} | {layoutClassFailOrMissing} | `{runtimeReadiness}` | {notes} |".format(
                **method
            )
        )
    lines.extend(
        [
            "",
            "## Case Scores",
            "",
            "| Method | Fixture | Truth layout | Predicted layout | Score | Graphs | Reason |",
            "| --- | --- | --- | --- | --- | ---: | --- |",
        ]
    )
    for case in summary["caseScores"]:
        lines.append(
            "| `{methodId}` | `{fixtureId}` | {expectedLayoutClass} | {predictedLayoutClass} | "
            "{layoutClassScore} | {detectedGraphCount} | {reason} |".format(
                **{
                    **case,
                    "reason": str(case["reason"]).replace("|", "/"),
                }
            )
        )
    lines.extend(
        [
            "",
            "## Semantic Feature Audit",
            "",
            "| Fixture | Panel groups | Panel families | Text families | Page context |",
            "| --- | --- | --- | --- | --- |",
        ]
    )
    for feature in summary["semanticFeatures"]:
        lines.append(
            "| `{fixtureId}` | {panelGroups} | {panelFamilies} | {textFamilies} | {rejectedRegionFamilies} |".format(
                **{
                    **feature,
                    "panelGroups": ", ".join(feature["panelGroups"]),
                    "panelFamilies": json.dumps(feature["panelFamilies"], ensure_ascii=False),
                    "textFamilies": json.dumps(feature["textFamilies"], ensure_ascii=False),
                    "rejectedRegionFamilies": json.dumps(feature["rejectedRegionFamilies"], ensure_ascii=False),
                }
            )
        )
    lines.extend(
        [
            "",
            "## Interpretation",
            "",
            "- Geometry-only row counting solves P0 physical graph count but not semantic layout class.",
            "- OCR/text-role families are sufficient to identify `TIC_PLUS_ION_PANELS` in this P0 set.",
            "- Page/background context is needed to separate `TWO_GRAPH_PAGE` from generic two ion-panel layouts.",
            "- The upper-bound method uses annotation features; it is not runtime-ready until those features are produced automatically.",
        ]
    )
    return "\n".join(lines) + "\n"


def build_summary(args: argparse.Namespace) -> dict[str, Any]:
    drc3 = read_json(args.drc3_manual)
    drc4 = read_json(args.drc4_manual)
    drc6 = read_json(args.drc6_summary)
    drc3_by_fixture = {record["fixtureId"]: record for record in drc3["records"]}
    drc4_by_fixture = {record["fixtureId"]: record for record in drc4["records"]}
    graph_counts = best_drc6_graph_counts(drc6)

    semantic_features = []
    for fixture_id, graph_count in graph_counts.items():
        semantic_features.append(
            feature_record(
                drc3_by_fixture[fixture_id],
                drc4_by_fixture[fixture_id],
                graph_count,
            )
        )
    features_by_fixture = {feature["fixtureId"]: feature for feature in semantic_features}
    case_scores = build_case_scores(features_by_fixture, drc3_by_fixture)
    summaries = method_summaries(case_scores)
    best_summary = next(
        method
        for method in summaries
        if method["methodId"] == "annotation_text_role_page_context_upper_bound_v1"
    )
    verdict = (
        "ANNOTATION_SEMANTIC_UPPER_BOUND_SOLVES_P0_LAYOUT_CLASS_NOT_RUNTIME_READY"
        if best_summary["layoutClassPass"] == len(semantic_features)
        else "SEMANTIC_LAYOUT_REMAINS_UNRESOLVED"
    )
    return {
        "schemaVersion": "chromalab.benchmark.drc7_panel_semantic_classifier.v1",
        "overallVerdict": verdict,
        "fixtureCount": len(semantic_features),
        "graphCountSource": str(args.drc6_summary).replace("\\", "/"),
        "layoutTruthSource": str(args.drc3_manual).replace("\\", "/"),
        "textRoleSource": str(args.drc4_manual).replace("\\", "/"),
        "productionImpact": "NONE_RESEARCH_ONLY",
        "methodSummaries": summaries,
        "caseScores": case_scores,
        "semanticFeatures": semantic_features,
        "nextRequiredRuntimeCapabilities": [
            "automatic_ocr_text_role_extraction",
            "automatic_panel_title_family_classification",
            "automatic_page_context_region_rejection",
            "axis_ownership_per_panel",
        ],
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--drc3-manual",
        type=Path,
        default=Path("benchmark/annotations/drc3_initial_graph_layout_annotations/manual-p0-annotations.json"),
    )
    parser.add_argument(
        "--drc4-manual",
        type=Path,
        default=Path("benchmark/annotations/drc4_tick_text_role_annotations/manual-p0-tick-text-annotations.json"),
    )
    parser.add_argument(
        "--drc6-summary",
        type=Path,
        default=Path("benchmark/reports/drc6_p0_graph_layout_method_comparison/summary.json"),
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("benchmark/reports/drc7_panel_semantic_classifier"),
    )
    args = parser.parse_args()
    summary = build_summary(args)
    write_json(args.output / "summary.json", summary)
    (args.output / "summary.md").write_text(build_markdown(summary), encoding="utf-8")
    print(f"Compared DR-C7 panel semantic classifiers: {summary['overallVerdict']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
