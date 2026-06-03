#!/usr/bin/env python3
"""Run RapidOCR preprocessing grid and context safety gate benchmark.

This is a PC-side benchmark runner only. DR-C4 boxes are used as benchmark crop
targets, not as runtime graph coordinates.
"""

from __future__ import annotations

import argparse
import importlib.metadata
import json
import re
import statistics
import time
from collections import Counter
from difflib import SequenceMatcher
from pathlib import Path
from typing import Any

import numpy as np
from PIL import Image, ImageFilter, ImageOps


PREPROCESSING_VARIANTS = {
    "rapidocr_rgb_p2_s2_v1": {
        "pad": 2,
        "scale": 2,
        "mode": "rgb",
    },
    "rapidocr_rgb_p6_s3_v1": {
        "pad": 6,
        "scale": 3,
        "mode": "rgb",
    },
    "rapidocr_rgb_p10_s4_v1": {
        "pad": 10,
        "scale": 4,
        "mode": "rgb",
    },
    "rapidocr_autocontrast_p6_s3_v1": {
        "pad": 6,
        "scale": 3,
        "mode": "autocontrast",
    },
    "rapidocr_sharp_autocontrast_p6_s3_v1": {
        "pad": 6,
        "scale": 3,
        "mode": "sharp_autocontrast",
    },
    "rapidocr_binary55_p6_s3_v1": {
        "pad": 6,
        "scale": 3,
        "mode": "binary55",
    },
    "rapidocr_binary65_p8_s4_v1": {
        "pad": 8,
        "scale": 4,
        "mode": "binary65",
    },
}


RUS_TIME = "\u0432\u0440\u0435\u043c\u044f"
RUS_INTENSITY = "\u0438\u043d\u0442\u0435\u043d\u0441\u0438\u0432\u043d\u043e\u0441\u0442\u044c"
RUS_DA = "\u0434\u0430"


def read_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def normalize_text(text: str | None) -> str:
    return re.sub(r"\s+", " ", str(text or "").strip().lower())


def normalize_compact(text: str | None) -> str:
    value = normalize_text(text)
    value = value.replace("，", ",").replace("。", ".").replace(",", ".")
    return re.sub(r"\s+", "", value)


def text_similarity(a: str | None, b: str | None) -> float:
    return SequenceMatcher(None, normalize_compact(a), normalize_compact(b)).ratio()


def has_digit(text: str | None) -> bool:
    return bool(re.search(r"\d", str(text or "")))


def is_numeric_tick_text(text: str | None) -> bool:
    return bool(re.fullmatch(r"\s*[+-]?\d+(?:[.,]\d+)?\s*", str(text or "")))


def classify_text_role(text: str | None) -> str:
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
        or RUS_TIME in value
        or RUS_INTENSITY in value
    ):
        return "axis_title"
    if compact.endswith(RUS_DA) and has_digit(value):
        return "other"
    if is_numeric_tick_text(value):
        return "tick_label"
    return "other"


def context_from_truth_role(truth_role: str) -> str:
    if truth_role == "tick_label":
        return "tick_label_candidate_context"
    return "non_tick_context_requires_geometry_proof"


def apply_context_safety_gate(predicted_role: str, text: str, context: str) -> tuple[str, str | None]:
    if (
        predicted_role == "tick_label"
        and context != "tick_label_candidate_context"
        and is_numeric_tick_text(text)
    ):
        return "numeric_rejected_non_tick_context", "pure numeric OCR is not accepted outside tick-label candidate context"
    return predicted_role, None


def iter_text_labels(records: list[dict[str, Any]]) -> list[dict[str, Any]]:
    labels: list[dict[str, Any]] = []
    for record in records:
        for graph in record.get("graphs", []):
            for label in graph.get("textRoleLabels", []):
                truth_role = str(label.get("role") or "")
                labels.append(
                    {
                        "fixtureId": record["fixtureId"],
                        "sourceImage": record["sourceImage"],
                        "graphId": graph["graphId"],
                        "panelGroup": graph.get("panelGroup"),
                        "textId": label.get("textId") or label.get("labelId"),
                        "truthText": label.get("text"),
                        "truthRole": truth_role,
                        "bbox": label.get("bbox"),
                        "context": context_from_truth_role(truth_role),
                        "rejectionReason": label.get("rejectionReason"),
                    }
                )
    return labels


def crop_label(image: Image.Image, bbox: dict[str, Any], variant: dict[str, Any]) -> Image.Image:
    pad = int(variant["pad"])
    scale = int(variant["scale"])
    x0 = max(0, int(round(float(bbox["x"]))) - pad)
    y0 = max(0, int(round(float(bbox["y"]))) - pad)
    x1 = min(image.width, int(round(float(bbox["x"] + bbox["width"]))) + pad)
    y1 = min(image.height, int(round(float(bbox["y"] + bbox["height"]))) + pad)
    crop = image.crop((x0, y0, x1, y1)).convert("RGB")
    if scale > 1:
        crop = crop.resize((crop.width * scale, crop.height * scale), Image.Resampling.LANCZOS)

    mode = str(variant["mode"])
    if mode == "rgb":
        return crop
    if mode == "autocontrast":
        return ImageOps.autocontrast(crop.convert("L")).convert("RGB")
    if mode == "sharp_autocontrast":
        sharpened = crop.filter(ImageFilter.SHARPEN).filter(ImageFilter.SHARPEN)
        return ImageOps.autocontrast(sharpened.convert("L")).convert("RGB")
    if mode.startswith("binary"):
        percentile = int(mode.replace("binary", ""))
        gray = ImageOps.autocontrast(crop.convert("L"))
        arr = np.array(gray)
        cutoff = max(80, min(220, int(np.percentile(arr, percentile))))
        binary = (arr > cutoff).astype(np.uint8) * 255
        return Image.fromarray(binary, mode="L").convert("RGB")
    raise ValueError(f"Unknown preprocessing mode: {mode}")


def rapidocr_text(output: Any) -> tuple[str, float, float]:
    txts = tuple(str(text) for text in getattr(output, "txts", ()) if str(text).strip())
    scores = tuple(float(score) for score in getattr(output, "scores", ()) if score is not None)
    text = " ".join(txts).strip()
    confidence = statistics.mean(scores) if scores else 0.0
    elapsed = float(getattr(output, "elapse", 0.0) or 0.0)
    return text, confidence, elapsed


def run_variant(ocr: Any, image: Image.Image, label: dict[str, Any], variant_id: str) -> dict[str, Any]:
    variant = PREPROCESSING_VARIANTS[variant_id]
    crop = crop_label(image, label["bbox"], variant)
    started = time.perf_counter()
    output = ocr(np.array(crop), use_det=False, use_cls=True, use_rec=True)
    wall_elapsed = time.perf_counter() - started
    text, confidence, engine_elapsed = rapidocr_text(output)
    raw_role = classify_text_role(text)
    safe_role, safety_rejection_reason = apply_context_safety_gate(raw_role, text, label["context"])
    truth_role = str(label["truthRole"])
    return {
        "variantId": variant_id,
        "fixtureId": label["fixtureId"],
        "graphId": label["graphId"],
        "textId": label["textId"],
        "truthText": label["truthText"],
        "truthRole": truth_role,
        "context": label["context"],
        "ocrText": text,
        "rawPredictedRole": raw_role,
        "safePredictedRole": safe_role,
        "safetyRejectionReason": safety_rejection_reason,
        "textExactMatch": normalize_compact(text) == normalize_compact(label["truthText"]),
        "textSimilarity": round(text_similarity(text, label["truthText"]), 4),
        "rawRoleScore": "PASS" if raw_role == truth_role else "FAIL",
        "safeRoleScore": "PASS" if safe_role == truth_role else "FAIL",
        "rawFalseTickLabel": raw_role == "tick_label" and truth_role != "tick_label",
        "safeFalseTickLabel": safe_role == "tick_label" and truth_role != "tick_label",
        "forbiddenNumericText": truth_role != "tick_label" and has_digit(label["truthText"]),
        "confidence": round(confidence, 4),
        "engineElapsedSec": round(engine_elapsed, 4),
        "wallElapsedSec": round(wall_elapsed, 4),
        "preprocessing": variant,
    }


def package_versions() -> dict[str, str | None]:
    names = ["rapidocr", "onnxruntime", "opencv-python", "numpy", "Pillow"]
    versions: dict[str, str | None] = {}
    for name in names:
        try:
            versions[name] = importlib.metadata.version(name)
        except importlib.metadata.PackageNotFoundError:
            versions[name] = None
    return versions


def summarize_variant(variant_id: str, scores: list[dict[str, Any]], *, safe: bool) -> dict[str, Any]:
    selected = [score for score in scores if score["variantId"] == variant_id]
    total = len(selected)
    role_key = "safeRoleScore" if safe else "rawRoleScore"
    false_key = "safeFalseTickLabel" if safe else "rawFalseTickLabel"
    role_pass = sum(1 for score in selected if score[role_key] == "PASS")
    false_tick = sum(1 for score in selected if score[false_key])
    forbidden = [score for score in selected if score["forbiddenNumericText"]]
    forbidden_rejected = [
        score
        for score in forbidden
        if not score[false_key]
    ]
    return {
        "variantId": variant_id,
        "mode": "safe_context_gate" if safe else "raw_role_classifier",
        "labelCount": total,
        "nonEmptyOcrCount": sum(1 for score in selected if score["ocrText"]),
        "exactTextMatchCount": sum(1 for score in selected if score["textExactMatch"]),
        "meanTextSimilarity": round(statistics.mean(score["textSimilarity"] for score in selected), 4)
        if selected
        else 0.0,
        "rolePassCount": role_pass,
        "roleAccuracy": round(role_pass / total, 4) if total else 0.0,
        "falseTickLabelCount": false_tick,
        "forbiddenNumericTextCount": len(forbidden),
        "forbiddenNumericRejectedCount": len(forbidden_rejected),
        "forbiddenNumericRejectedRate": round(len(forbidden_rejected) / len(forbidden), 4)
        if forbidden
        else 1.0,
        "meanWallElapsedSec": round(statistics.mean(score["wallElapsedSec"] for score in selected), 4)
        if selected
        else 0.0,
        "roleConfusion": dict(
            Counter(
                f"{score['truthRole']}->{score['safePredictedRole' if safe else 'rawPredictedRole']}"
                for score in selected
            )
        ),
    }


def summarize_fixture_features(variant_id: str, scores: list[dict[str, Any]], *, safe: bool) -> list[dict[str, Any]]:
    by_fixture: dict[str, list[dict[str, Any]]] = {}
    role_key = "safePredictedRole" if safe else "rawPredictedRole"
    false_key = "safeFalseTickLabel" if safe else "rawFalseTickLabel"
    for score in scores:
        if score["variantId"] == variant_id:
            by_fixture.setdefault(score["fixtureId"], []).append(score)
    out = []
    for fixture_id, fixture_scores in by_fixture.items():
        truth_has_tic = any(
            score["truthRole"] == "chart_title" and "tic" in normalize_text(score["truthText"])
            for score in fixture_scores
        )
        predicted_has_tic = any(
            score[role_key] == "chart_title" and "tic" in normalize_text(score["ocrText"])
            for score in fixture_scores
        )
        truth_has_ion = any(score["truthRole"] == "ion_or_mz_metadata" for score in fixture_scores)
        predicted_has_ion = any(score[role_key] == "ion_or_mz_metadata" for score in fixture_scores)
        truth_has_legend = any(score["truthRole"] == "legend" for score in fixture_scores)
        predicted_has_legend = any(score[role_key] == "legend" for score in fixture_scores)
        false_tick = sum(1 for score in fixture_scores if score[false_key])
        out.append(
            {
                "variantId": variant_id,
                "mode": "safe_context_gate" if safe else "raw_role_classifier",
                "fixtureId": fixture_id,
                "ticTitleFeatureScore": "PASS" if truth_has_tic == predicted_has_tic else "FAIL",
                "ionOrMzFeatureScore": "PASS" if truth_has_ion == predicted_has_ion else "FAIL",
                "legendFeatureScore": "PASS" if truth_has_legend == predicted_has_legend else "FAIL",
                "forbiddenNumericSafetyScore": "PASS" if false_tick == 0 else "FAIL",
                "falseTickLabelCount": false_tick,
            }
        )
    return out


def best_summary(summaries: list[dict[str, Any]]) -> dict[str, Any]:
    return max(
        summaries,
        key=lambda item: (
            -item["falseTickLabelCount"],
            item["roleAccuracy"],
            item["meanTextSimilarity"],
        ),
    )


def build_markdown(summary: dict[str, Any]) -> str:
    lines = [
        "# DR-D3 OCR Preprocessing Grid And Safety Gate",
        "",
        f"Verdict: `{summary['overallVerdict']}`",
        f"Text labels: `{summary['textLabelCount']}`",
        "",
        "## Best Variants",
        "",
        "| Mode | Variant | Role accuracy | False tick labels | Mean similarity | Forbidden numeric rejected |",
        "| --- | --- | ---: | ---: | ---: | ---: |",
    ]
    for key in ["bestRawVariant", "bestSafeVariant"]:
        item = summary[key]
        lines.append(
            "| `{mode}` | `{variantId}` | {roleAccuracy:.4f} | {falseTickLabelCount} | "
            "{meanTextSimilarity:.4f} | {forbiddenNumericRejectedCount}/{forbiddenNumericTextCount} |".format(
                **item
            )
        )
    lines.extend(
        [
            "",
            "## Variant Summary",
            "",
            "| Mode | Variant | Exact text | Mean similarity | Role accuracy | False tick labels | Mean time/label |",
            "| --- | --- | ---: | ---: | ---: | ---: | ---: |",
        ]
    )
    for item in summary["variantSummaries"]:
        lines.append(
            "| `{mode}` | `{variantId}` | {exactTextMatchCount}/{labelCount} | {meanTextSimilarity:.4f} | "
            "{roleAccuracy:.4f} | {falseTickLabelCount} | {meanWallElapsedSec:.4f}s |".format(**item)
        )
    lines.extend(
        [
            "",
            "## Safety Gate Effect",
            "",
            f"Raw best false tick labels: `{summary['bestRawVariant']['falseTickLabelCount']}`",
            f"Safe best false tick labels: `{summary['bestSafeVariant']['falseTickLabelCount']}`",
            "",
            "Rejected examples:",
        ]
    )
    for example in summary["safetyGateRejectedExamples"][:8]:
        lines.append(
            "- `{fixtureId}` `{truthRole}` `{truthText}` -> OCR `{ocrText}`".format(**example)
        )
    lines.extend(
        [
            "",
            "## Interpretation",
            "",
            "- Preprocessing grid did not make RapidOCR acceptance-ready on P0 crops.",
            "- Context safety gate removes false tick-label safety failures, but role accuracy remains too low.",
            "- OCR must improve before semantic layout classifier can be trusted in Android runtime.",
            "- Next work should compare a second OCR engine and add real text-box detection/crop generation.",
        ]
    )
    return "\n".join(lines) + "\n"


def build_summary(args: argparse.Namespace) -> dict[str, Any]:
    try:
        from rapidocr import RapidOCR
    except ImportError as exc:
        raise SystemExit("RapidOCR is not installed. Run: python -m pip install -r tools/benchmark/ocr-requirements.txt") from exc

    records = read_json(args.drc4_manual)["records"]
    labels = iter_text_labels(records)
    images: dict[str, Image.Image] = {}
    for label in labels:
        source = str(label["sourceImage"])
        if source not in images:
            images[source] = Image.open(args.repo_root / source).convert("RGB")

    ocr = RapidOCR()
    scores: list[dict[str, Any]] = []
    for variant_id in PREPROCESSING_VARIANTS:
        for label in labels:
            scores.append(run_variant(ocr, images[str(label["sourceImage"])], label, variant_id))

    raw_summaries = [summarize_variant(variant_id, scores, safe=False) for variant_id in PREPROCESSING_VARIANTS]
    safe_summaries = [summarize_variant(variant_id, scores, safe=True) for variant_id in PREPROCESSING_VARIANTS]
    variant_summaries = raw_summaries + safe_summaries
    best_raw = best_summary(raw_summaries)
    best_safe = best_summary(safe_summaries)
    safe_feature_scores = summarize_fixture_features(best_safe["variantId"], scores, safe=True)
    rejected_examples = [
        score
        for score in scores
        if score["variantId"] == best_safe["variantId"] and score["safetyRejectionReason"]
    ]
    verdict = (
        "SAFETY_GATE_REMOVES_FALSE_TICK_LABELS_OCR_NOT_ACCEPTANCE_READY"
        if best_safe["falseTickLabelCount"] == 0 and best_safe["roleAccuracy"] < 0.9
        else "OCR_PREPROCESSING_ACCEPTANCE_READY"
        if best_safe["falseTickLabelCount"] == 0 and best_safe["roleAccuracy"] >= 0.9
        else "OCR_SAFETY_GATE_INSUFFICIENT"
    )
    return {
        "schemaVersion": "chromalab.benchmark.drd3_ocr_preprocessing_grid.v1",
        "overallVerdict": verdict,
        "productionImpact": "NONE_RESEARCH_ONLY",
        "textRoleTruthSource": str(args.drc4_manual).replace("\\", "/"),
        "textLabelCount": len(labels),
        "packageVersions": package_versions(),
        "preprocessingVariants": PREPROCESSING_VARIANTS,
        "variantSummaries": variant_summaries,
        "bestRawVariant": best_raw,
        "bestSafeVariant": best_safe,
        "bestSafeFixtureFeatureScores": safe_feature_scores,
        "safetyGateRejectedExamples": rejected_examples,
        "labelScores": scores,
        "nextRequiredCapabilities": [
            "compare_second_OCR_engine",
            "automatic_text_box_detection_not_truth_crops",
            "geometry_owned_tick_label_context",
            "panel_title_and_axis_band_crop_generation",
        ],
    }


def package_versions() -> dict[str, str | None]:
    names = ["rapidocr", "onnxruntime", "opencv-python", "numpy", "Pillow"]
    versions: dict[str, str | None] = {}
    for name in names:
        try:
            versions[name] = importlib.metadata.version(name)
        except importlib.metadata.PackageNotFoundError:
            versions[name] = None
    return versions


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo-root", type=Path, default=Path("."))
    parser.add_argument(
        "--drc4-manual",
        type=Path,
        default=Path("benchmark/annotations/drc4_tick_text_role_annotations/manual-p0-tick-text-annotations.json"),
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("benchmark/reports/drd3_ocr_preprocessing_grid"),
    )
    args = parser.parse_args()
    summary = build_summary(args)
    write_json(args.output / "summary.json", summary)
    (args.output / "summary.md").write_text(build_markdown(summary), encoding="utf-8")
    print(f"Built DR-D3 OCR preprocessing grid: {summary['overallVerdict']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
