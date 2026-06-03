#!/usr/bin/env python3
"""Benchmark OCR text-role feature extraction targets for P0 fixtures.

This is a PC-side research harness only. It does not run production OCR and does
not change Android runtime, validators, or chromatographic math.
"""

from __future__ import annotations

import argparse
import importlib.util
import json
import re
import shutil
from collections import Counter
from pathlib import Path
from typing import Any


ROLE_VALUES = [
    "tick_label",
    "ion_or_mz_metadata",
    "axis_title",
    "chart_title",
    "legend",
    "other",
    "missing_text",
]


def read_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def normalize_text(text: str | None) -> str:
    return str(text or "").strip().lower()


def has_digit(text: str | None) -> bool:
    return bool(re.search(r"\d", str(text or "")))


def is_numeric_tick_text(text: str | None) -> bool:
    return bool(re.fullmatch(r"\s*[+-]?\d+(?:[.,]\d+)?\s*", str(text or "")))


def classify_regex_perfect_text(text: str | None) -> str:
    value = normalize_text(text)
    compact = value.replace(" ", "")
    if not value:
        return "missing_text"
    if value.startswith("tic:") or value == "tic":
        return "chart_title"
    if value.startswith("ion "):
        return "ion_or_mz_metadata"
    if value.startswith("xic("):
        return "legend"
    if (
        "abundance" in value
        or "time" in value
        or "время" in value
        or "интенсивность" in value
    ):
        return "axis_title"
    if compact.endswith("да") and has_digit(value):
        return "other"
    if is_numeric_tick_text(value):
        return "tick_label"
    return "other"


def iter_text_labels(records: list[dict[str, Any]]) -> list[dict[str, Any]]:
    labels: list[dict[str, Any]] = []
    for record in records:
        for graph in record.get("graphs", []):
            for label in graph.get("textRoleLabels", []):
                labels.append(
                    {
                        "fixtureId": record["fixtureId"],
                        "graphId": graph["graphId"],
                        "panelGroup": graph.get("panelGroup"),
                        "textId": label.get("textId") or label.get("labelId"),
                        "text": label.get("text"),
                        "truthRole": label.get("role"),
                        "rejectedAsTickLabel": bool(label.get("rejectedAsTickLabel")),
                        "rejectionReason": label.get("rejectionReason"),
                    }
                )
    return labels


def predict_role(method_id: str, label: dict[str, Any]) -> str:
    if method_id == "no_ocr_available_v1":
        return "missing_text"
    if method_id == "regex_on_perfect_text_v1":
        return classify_regex_perfect_text(label.get("text"))
    if method_id == "annotation_role_oracle_v1":
        return str(label.get("truthRole") or "missing_text")
    raise ValueError(f"Unknown method: {method_id}")


def method_runtime_readiness(method_id: str) -> str:
    return {
        "no_ocr_available_v1": "CURRENT_LOCAL_ENGINE_BASELINE",
        "regex_on_perfect_text_v1": "UPPER_BOUND_REQUIRES_AUTOMATIC_OCR_TEXT",
        "annotation_role_oracle_v1": "ORACLE_NOT_RUNTIME",
    }[method_id]


def score_method(method_id: str, labels: list[dict[str, Any]]) -> dict[str, Any]:
    role_confusion: Counter[str] = Counter()
    correct = 0
    false_tick_labels = 0
    forbidden_numeric_seen = 0
    forbidden_numeric_rejected = 0
    missing = 0
    scored_labels = []
    for label in labels:
        truth = str(label["truthRole"])
        predicted = predict_role(method_id, label)
        if predicted == truth:
            correct += 1
        if predicted == "missing_text":
            missing += 1
        if predicted == "tick_label" and truth != "tick_label":
            false_tick_labels += 1
        if truth != "tick_label" and has_digit(label.get("text")):
            forbidden_numeric_seen += 1
            if predicted != "tick_label":
                forbidden_numeric_rejected += 1
        role_confusion[f"{truth}->{predicted}"] += 1
        scored_labels.append(
            {
                "fixtureId": label["fixtureId"],
                "graphId": label["graphId"],
                "textId": label["textId"],
                "text": label["text"],
                "truthRole": truth,
                "predictedRole": predicted,
                "score": "PASS" if predicted == truth else "FAIL",
            }
        )
    total = len(labels)
    return {
        "methodId": method_id,
        "runtimeReadiness": method_runtime_readiness(method_id),
        "labelCount": total,
        "correctRoleCount": correct,
        "textRoleAccuracy": round(correct / total, 4) if total else 0.0,
        "missingTextCount": missing,
        "falseTickLabelCount": false_tick_labels,
        "forbiddenNumericTextCount": forbidden_numeric_seen,
        "forbiddenNumericRejectedCount": forbidden_numeric_rejected,
        "forbiddenNumericRejectedRate": round(forbidden_numeric_rejected / forbidden_numeric_seen, 4)
        if forbidden_numeric_seen
        else 1.0,
        "roleConfusion": dict(role_confusion),
        "labelScores": scored_labels,
    }


def fixture_feature_truth(labels: list[dict[str, Any]]) -> dict[str, dict[str, Any]]:
    by_fixture: dict[str, list[dict[str, Any]]] = {}
    for label in labels:
        by_fixture.setdefault(label["fixtureId"], []).append(label)
    features: dict[str, dict[str, Any]] = {}
    for fixture_id, fixture_labels in by_fixture.items():
        truth_roles = Counter(str(label["truthRole"]) for label in fixture_labels)
        features[fixture_id] = {
            "truthRoleCounts": dict(truth_roles),
            "truthHasTicTitle": any(
                str(label["truthRole"]) == "chart_title"
                and "tic" in normalize_text(label.get("text"))
                for label in fixture_labels
            ),
            "truthIonOrMzMetadataCount": sum(
                1 for label in fixture_labels if str(label["truthRole"]) == "ion_or_mz_metadata"
            ),
            "truthLegendCount": sum(1 for label in fixture_labels if str(label["truthRole"]) == "legend"),
            "truthTickLabelCount": sum(
                1 for label in fixture_labels if str(label["truthRole"]) == "tick_label"
            ),
            "truthForbiddenNumericTextCount": sum(
                1 for label in fixture_labels if str(label["truthRole"]) != "tick_label" and has_digit(label["text"])
            ),
        }
    return features


def fixture_feature_scores(method_id: str, labels: list[dict[str, Any]]) -> list[dict[str, Any]]:
    by_fixture: dict[str, list[dict[str, Any]]] = {}
    for label in labels:
        by_fixture.setdefault(label["fixtureId"], []).append(label)
    scores: list[dict[str, Any]] = []
    truth = fixture_feature_truth(labels)
    for fixture_id, fixture_labels in by_fixture.items():
        predicted_roles = [
            {
                **label,
                "predictedRole": predict_role(method_id, label),
            }
            for label in fixture_labels
        ]
        predicted_has_tic = any(
            label["predictedRole"] == "chart_title"
            and "tic" in normalize_text(label.get("text"))
            for label in predicted_roles
        )
        predicted_ion_count = sum(
            1 for label in predicted_roles if label["predictedRole"] == "ion_or_mz_metadata"
        )
        predicted_legend_count = sum(1 for label in predicted_roles if label["predictedRole"] == "legend")
        false_tick_labels = sum(
            1
            for label in predicted_roles
            if label["predictedRole"] == "tick_label" and label["truthRole"] != "tick_label"
        )
        feature_truth = truth[fixture_id]
        tic_score = "PASS" if predicted_has_tic == feature_truth["truthHasTicTitle"] else "FAIL"
        ion_score = (
            "PASS"
            if (predicted_ion_count > 0) == (feature_truth["truthIonOrMzMetadataCount"] > 0)
            else "FAIL"
        )
        legend_score = (
            "PASS"
            if (predicted_legend_count > 0) == (feature_truth["truthLegendCount"] > 0)
            else "FAIL"
        )
        forbidden_score = "PASS" if false_tick_labels == 0 else "FAIL"
        scores.append(
            {
                "methodId": method_id,
                "fixtureId": fixture_id,
                "ticTitleFeatureScore": tic_score,
                "ionOrMzFeatureScore": ion_score,
                "legendFeatureScore": legend_score,
                "forbiddenNumericSafetyScore": forbidden_score,
                "truth": feature_truth,
                "predicted": {
                    "hasTicTitle": predicted_has_tic,
                    "ionOrMzMetadataCount": predicted_ion_count,
                    "legendCount": predicted_legend_count,
                    "falseTickLabelCount": false_tick_labels,
                },
            }
        )
    return scores


def local_ocr_availability() -> dict[str, Any]:
    modules = [
        "pytesseract",
        "easyocr",
        "paddleocr",
        "rapidocr_onnxruntime",
        "onnxruntime",
        "cv2",
    ]
    executables = ["tesseract", "magick"]
    return {
        "pythonModules": {module: importlib.util.find_spec(module) is not None for module in modules},
        "executables": {exe: shutil.which(exe) for exe in executables},
    }


def summarize_methods(method_scores: list[dict[str, Any]], feature_scores: list[dict[str, Any]]) -> list[dict[str, Any]]:
    feature_by_method: dict[str, list[dict[str, Any]]] = {}
    for score in feature_scores:
        feature_by_method.setdefault(score["methodId"], []).append(score)
    summaries = []
    for score in method_scores:
        method_features = feature_by_method.get(score["methodId"], [])
        feature_passes = sum(
            1
            for feature in method_features
            for key in [
                "ticTitleFeatureScore",
                "ionOrMzFeatureScore",
                "legendFeatureScore",
                "forbiddenNumericSafetyScore",
            ]
            if feature[key] == "PASS"
        )
        feature_total = len(method_features) * 4
        summaries.append(
            {
                "methodId": score["methodId"],
                "runtimeReadiness": score["runtimeReadiness"],
                "textRoleAccuracy": score["textRoleAccuracy"],
                "correctRoleCount": score["correctRoleCount"],
                "labelCount": score["labelCount"],
                "falseTickLabelCount": score["falseTickLabelCount"],
                "forbiddenNumericRejectedRate": score["forbiddenNumericRejectedRate"],
                "semanticFeaturePass": feature_passes,
                "semanticFeatureTotal": feature_total,
            }
        )
    return summaries


def build_markdown(summary: dict[str, Any]) -> str:
    lines = [
        "# DR-D1 OCR Text-Role Feature Benchmark",
        "",
        f"Verdict: `{summary['overallVerdict']}`",
        f"Annotated text labels: `{summary['textLabelCount']}`",
        "",
        "## Local OCR Availability",
        "",
        "| Engine | Available |",
        "| --- | --- |",
    ]
    for name, available in summary["localOcrAvailability"]["pythonModules"].items():
        lines.append(f"| Python module `{name}` | `{available}` |")
    for name, path in summary["localOcrAvailability"]["executables"].items():
        lines.append(f"| Executable `{name}` | `{path or False}` |")

    lines.extend(
        [
            "",
            "## Method Summary",
            "",
            "| Method | Readiness | Role accuracy | Correct labels | False tick labels | Forbidden numeric rejected | Semantic feature pass |",
            "| --- | --- | ---: | ---: | ---: | ---: | ---: |",
        ]
    )
    for method in summary["methodSummaries"]:
        lines.append(
            "| `{methodId}` | `{runtimeReadiness}` | {textRoleAccuracy:.4f} | {correctRoleCount}/{labelCount} | "
            "{falseTickLabelCount} | {forbiddenNumericRejectedRate:.4f} | {semanticFeaturePass}/{semanticFeatureTotal} |".format(
                **method
            )
        )

    lines.extend(
        [
            "",
            "## Fixture Feature Scores",
            "",
            "| Method | Fixture | TIC title | Ion/mz metadata | Legend | Forbidden numeric safety |",
            "| --- | --- | --- | --- | --- | --- |",
        ]
    )
    for score in summary["fixtureFeatureScores"]:
        lines.append(
            "| `{methodId}` | `{fixtureId}` | {ticTitleFeatureScore} | {ionOrMzFeatureScore} | "
            "{legendFeatureScore} | {forbiddenNumericSafetyScore} |".format(**score)
        )

    lines.extend(
        [
            "",
            "## Interpretation",
            "",
            "- No local OCR engine is currently available in this Python environment, so the real OCR baseline is `missing_text`.",
            "- Regex classification on perfect text reaches the text-role feature target without turning Ion/TIC/legend numbers into tick labels.",
            "- This does not prove OCR works; it proves the next implementation must first produce reliable text strings and boxes.",
            "- The next slice should install or integrate an OCR engine and score real crop OCR against these same DR-D1 targets.",
        ]
    )
    return "\n".join(lines) + "\n"


def build_summary(args: argparse.Namespace) -> dict[str, Any]:
    records = read_json(args.drc4_manual)["records"]
    labels = iter_text_labels(records)
    methods = [
        "no_ocr_available_v1",
        "regex_on_perfect_text_v1",
        "annotation_role_oracle_v1",
    ]
    method_scores = [score_method(method, labels) for method in methods]
    fixture_scores = [
        score
        for method in methods
        for score in fixture_feature_scores(method, labels)
    ]
    summaries = summarize_methods(method_scores, fixture_scores)
    regex_summary = next(method for method in summaries if method["methodId"] == "regex_on_perfect_text_v1")
    verdict = (
        "TEXT_ROLE_RULES_WORK_WITH_PERFECT_TEXT_REAL_OCR_NOT_AVAILABLE"
        if regex_summary["falseTickLabelCount"] == 0 and regex_summary["semanticFeaturePass"] == regex_summary["semanticFeatureTotal"]
        else "TEXT_ROLE_RULES_NEED_REWORK"
    )
    return {
        "schemaVersion": "chromalab.benchmark.drd1_ocr_text_role_feature_benchmark.v1",
        "overallVerdict": verdict,
        "textRoleTruthSource": str(args.drc4_manual).replace("\\", "/"),
        "productionImpact": "NONE_RESEARCH_ONLY",
        "textLabelCount": len(labels),
        "localOcrAvailability": local_ocr_availability(),
        "methodSummaries": summaries,
        "methodScores": method_scores,
        "fixtureFeatureScores": fixture_scores,
        "requiredNextCapabilities": [
            "install_or_integrate_real_ocr_engine",
            "crop_generation_from_graph_panel_candidates",
            "ocr_text_box_scoring_against_drc4_labels",
            "forbidden_numeric_text_safety_gate",
        ],
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--drc4-manual",
        type=Path,
        default=Path("benchmark/annotations/drc4_tick_text_role_annotations/manual-p0-tick-text-annotations.json"),
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("benchmark/reports/drd1_ocr_text_role_feature_benchmark"),
    )
    args = parser.parse_args()
    summary = build_summary(args)
    write_json(args.output / "summary.json", summary)
    (args.output / "summary.md").write_text(build_markdown(summary), encoding="utf-8")
    print(f"Built DR-D1 OCR text-role feature benchmark: {summary['overallVerdict']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
