from __future__ import annotations

import argparse
import time
from pathlib import Path
from typing import Any

import numpy as np

from tv2_common import (
    GOLDEN_QUERIES,
    EMBEDDING_PROFILES,
    evaluate_results,
    lexical_search,
    read_json,
    write_json,
)


def main() -> int:
    parser = argparse.ArgumentParser(description="Benchmark TV-2 lexical vs TurboVec retrieval.")
    parser.add_argument("--pack", default=Path("docs/knowledge/chromalab_knowledge_seed_v2.json"), type=Path)
    parser.add_argument("--artifacts", default=Path("artifacts/tv2-turbovec-knowledge"), type=Path)
    parser.add_argument("--out", required=True, type=Path)
    parser.add_argument("--k", type=int, default=8)
    args = parser.parse_args()

    pack = read_json(args.pack)
    report = {
        "phase": "TV-2",
        "pack_path": str(args.pack.as_posix()),
        "pack_version": pack.get("version"),
        "k": args.k,
        "lexical": run_lexical(pack, args.k),
        "dense_profiles": [],
    }

    for profile in EMBEDDING_PROFILES:
        sidecar_path = args.artifacts / profile.key / f"chromalab_knowledge_v2_{profile.key}_sidecar.json"
        index_path = args.artifacts / profile.key / f"chromalab_knowledge_v2_{profile.key}.tvim"
        report["dense_profiles"].append(
            run_dense_profile(
                profile=profile,
                pack=pack,
                sidecar_path=sidecar_path,
                index_path=index_path,
                k=args.k,
                lexical_cases=report["lexical"]["cases"],
            ),
        )

    report["decision"] = decide(report)
    args.out.mkdir(parents=True, exist_ok=True)
    write_json(args.out / "summary.json", report)
    write_summary(args.out / "summary.md", report)
    return 0


def run_lexical(pack: dict[str, Any], k: int) -> dict[str, Any]:
    started = time.perf_counter()
    cases = []
    for golden in GOLDEN_QUERIES:
        results = lexical_search(pack, golden["query"], k)
        evaluation = evaluate_results(results, golden["expected_entry_ids"])
        cases.append({**golden, **evaluation})
    return {
        "backend": "lexical_bm25_python_parity",
        "elapsed_ms": elapsed_ms(started),
        "hit_count": sum(1 for case in cases if case["hit"]),
        "top1_hit_count": sum(1 for case in cases if case["top1_hit"]),
        "case_count": len(cases),
        "cases": cases,
    }


def run_dense_profile(
    profile,
    pack: dict[str, Any],
    sidecar_path: Path,
    index_path: Path,
    k: int,
    lexical_cases: list[dict[str, Any]],
) -> dict[str, Any]:
    if not sidecar_path.exists() or not index_path.exists():
        return {
            "profile_key": profile.key,
            "model_id": profile.model_id,
            "status": "MISSING_INDEX",
            "cases": [],
        }
    try:
        from sentence_transformers import SentenceTransformer
        from turbovec import IdMapIndex
    except Exception as exc:
        return {
            "profile_key": profile.key,
            "model_id": profile.model_id,
            "status": "REJECTED_DEPENDENCY_IMPORT",
            "error": repr(exc),
            "cases": [],
        }

    started = time.perf_counter()
    try:
        sidecar = read_json(sidecar_path)
        id_to_entry = {int(item["u64"]): item["entry_id"] for item in sidecar["id_map"]}
        valid_entry_ids = {entry["entryId"] for entry in pack["entries"]}
        model = SentenceTransformer(profile.model_id)
        index = IdMapIndex.load(str(index_path))
        cases = []
        for golden in GOLDEN_QUERIES:
            query_vector = model.encode(
                [profile.query_prefix + golden["query"]],
                normalize_embeddings=True,
                show_progress_bar=False,
            )
            vectors = np.asarray(query_vector, dtype=np.float32)
            scores, ids = search_index(index, vectors, k)
            dense_results = []
            for score, numeric_id in zip(scores, ids):
                entry_id = id_to_entry.get(int(numeric_id))
                if entry_id is None:
                    dense_results.append(
                        {
                            "entry_id": f"UNKNOWN_U64:{int(numeric_id)}",
                            "score": float(score),
                            "valid_entry": False,
                        },
                    )
                else:
                    dense_results.append(
                        {
                            "entry_id": entry_id,
                            "score": float(score),
                            "valid_entry": entry_id in valid_entry_ids,
                        },
                    )
            evaluation = evaluate_results(dense_results, golden["expected_entry_ids"])
            lexical_case = next(case for case in lexical_cases if case["query_id"] == golden["query_id"])
            cases.append(
                {
                    **golden,
                    **evaluation,
                    "all_results_valid": all(result["valid_entry"] for result in dense_results),
                    "improved_vs_lexical": rank_better(evaluation["first_hit_rank"], lexical_case["first_hit_rank"]),
                    "regressed_vs_lexical": rank_worse(evaluation["first_hit_rank"], lexical_case["first_hit_rank"]),
                    "lexical_first_hit_rank": lexical_case["first_hit_rank"],
                },
            )
        safety_regressions = [
            case for case in cases
            if case["category"] == "safety" and case["regressed_vs_lexical"]
        ]
        invalid_results = [case for case in cases if not case["all_results_valid"]]
        status = "PASS" if not safety_regressions and not invalid_results else "REVIEW"
        return {
            "profile_key": profile.key,
            "model_id": profile.model_id,
            "status": status,
            "dimension": profile.expected_dim,
            "license": profile.license,
            "index_path": str(index_path.as_posix()),
            "sidecar_path": str(sidecar_path.as_posix()),
            "index_size_bytes": index_path.stat().st_size,
            "elapsed_ms": elapsed_ms(started),
            "hit_count": sum(1 for case in cases if case["hit"]),
            "top1_hit_count": sum(1 for case in cases if case["top1_hit"]),
            "case_count": len(cases),
            "improvement_count": sum(1 for case in cases if case["improved_vs_lexical"]),
            "regression_count": sum(1 for case in cases if case["regressed_vs_lexical"]),
            "safety_regression_count": len(safety_regressions),
            "invalid_result_count": len(invalid_results),
            "cases": cases,
        }
    except Exception as exc:
        return {
            "profile_key": profile.key,
            "model_id": profile.model_id,
            "status": "REJECTED_BENCHMARK",
            "error": repr(exc),
            "elapsed_ms": elapsed_ms(started),
            "cases": [],
        }


def search_index(index: Any, vectors: np.ndarray, k: int) -> tuple[list[float], list[int]]:
    try:
        scores, ids = index.search(vectors, k=k)
    except TypeError:
        scores, ids = index.search(vectors.reshape(-1), k=k)
    return flatten_floats(scores), flatten_ints(ids)


def flatten_floats(values: Any) -> list[float]:
    array = np.asarray(values)
    return [float(value) for value in array.reshape(-1).tolist()]


def flatten_ints(values: Any) -> list[int]:
    array = np.asarray(values)
    return [int(value) for value in array.reshape(-1).tolist()]


def rank_better(dense_rank: int | None, lexical_rank: int | None) -> bool:
    if dense_rank is None:
        return False
    if lexical_rank is None:
        return True
    return dense_rank < lexical_rank


def rank_worse(dense_rank: int | None, lexical_rank: int | None) -> bool:
    if lexical_rank is None:
        return False
    if dense_rank is None:
        return True
    return dense_rank > lexical_rank


def decide(report: dict[str, Any]) -> dict[str, Any]:
    usable_profiles = [
        profile for profile in report["dense_profiles"]
        if profile.get("status") in {"PASS", "REVIEW"}
    ]
    safety_regressions = sum(profile.get("safety_regression_count", 0) for profile in usable_profiles)
    improvements = sum(profile.get("improvement_count", 0) for profile in usable_profiles)
    if not usable_profiles:
        verdict = "TV2_BLOCKED_NO_DENSE_INDEX"
        next_step = "Keep LexicalKnowledgeRetrievalBackend as the only active retrieval owner."
    elif safety_regressions:
        verdict = "TV2_REVIEW_DENSE_SAFETY_REGRESSION"
        next_step = "Do not promote; inspect safety regressions and keep lexical active."
    elif improvements:
        verdict = "TV2_READY_FOR_TV3_AB_EVALUATION"
        next_step = "Proceed to TV-3 A/B evaluation with the passing dense profile(s)."
    else:
        verdict = "TV2_REVIEW_NO_DENSE_IMPROVEMENT"
        next_step = "Do not promote yet; expand goldens or keep lexical active."
    return {
        "verdict": verdict,
        "usable_profile_keys": [profile["profile_key"] for profile in usable_profiles],
        "total_improvement_count": improvements,
        "total_safety_regression_count": safety_regressions,
        "next_step": next_step,
    }


def elapsed_ms(started: float) -> int:
    return int((time.perf_counter() - started) * 1000)


def write_summary(path: Path, report: dict[str, Any]) -> None:
    lines = [
        "# TV-2 TurboVec Knowledge Retrieval Benchmark",
        "",
        f"- Verdict: `{report['decision']['verdict']}`",
        f"- Next step: {report['decision']['next_step']}",
        f"- Pack: `{report['pack_path']}`",
        f"- Pack version: `{report['pack_version']}`",
        f"- k: `{report['k']}`",
        "",
        "## Backend Summary",
        "",
        "| Backend | Status | Hits | Top-1 hits | Improvements | Regressions | Safety regressions |",
        "|---|---|---:|---:|---:|---:|---:|",
        "| lexical_bm25 | PASS | {hits}/{cases} | {top1}/{cases} | 0 | 0 | 0 |".format(
            hits=report["lexical"]["hit_count"],
            top1=report["lexical"]["top1_hit_count"],
            cases=report["lexical"]["case_count"],
        ),
    ]
    for profile in report["dense_profiles"]:
        lines.append(
            "| {backend} | {status} | {hits}/{cases} | {top1}/{cases} | {improvements} | {regressions} | {safety} |".format(
                backend=profile["profile_key"],
                status=profile["status"],
                hits=profile.get("hit_count", 0),
                top1=profile.get("top1_hit_count", 0),
                cases=profile.get("case_count", 0),
                improvements=profile.get("improvement_count", 0),
                regressions=profile.get("regression_count", 0),
                safety=profile.get("safety_regression_count", 0),
            ),
        )
    lines.extend(["", "## Query Cases", ""])
    for golden in GOLDEN_QUERIES:
        lines.extend(
            [
                f"### {golden['query_id']}",
                "",
                f"- Query: `{golden['query']}`",
                f"- Expected: `{', '.join(golden['expected_entry_ids'])}`",
                f"- Lexical top ids: `{', '.join(next(case for case in report['lexical']['cases'] if case['query_id'] == golden['query_id'])['top_ids'])}`",
            ],
        )
        for profile in report["dense_profiles"]:
            case = next((case for case in profile.get("cases", []) if case["query_id"] == golden["query_id"]), None)
            top_ids = ", ".join(case["top_ids"]) if case else "n/a"
            lines.append(f"- {profile['profile_key']} top ids: `{top_ids}`")
        lines.append("")
    path.write_text("\n".join(lines), encoding="utf-8")


if __name__ == "__main__":
    raise SystemExit(main())
