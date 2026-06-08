from __future__ import annotations

import argparse
import time
from pathlib import Path
from typing import Any

import numpy as np

from tv2_common import (
    EMBEDDING_PROFILES,
    GOLDEN_QUERIES,
    evaluate_results,
    lexical_search,
    read_json,
    sha256_file,
    write_json,
)


HYBRID_POLICIES = {
    "HYBRID_LEXICAL_GUARD_BGE": "bge_base",
    "HYBRID_LEXICAL_GUARD_MINILM": "minilm",
}


def main() -> int:
    parser = argparse.ArgumentParser(description="Benchmark lexical vs TurboVec Knowledge retrieval.")
    parser.add_argument("--mode", choices=("tv2", "tv3"), default="tv2")
    parser.add_argument("--pack", default=Path("docs/knowledge/chromalab_knowledge_seed_v2.json"), type=Path)
    parser.add_argument("--artifacts", default=Path("artifacts/tv2-turbovec-knowledge"), type=Path)
    parser.add_argument("--out", required=True, type=Path)
    parser.add_argument("--k", type=int, default=8)
    args = parser.parse_args()

    pack = read_json(args.pack)
    lexical = run_lexical(pack, args.k)
    dense_profiles = [
        run_dense_profile(
            profile=profile,
            pack=pack,
            sidecar_path=args.artifacts / profile.key / f"chromalab_knowledge_v2_{profile.key}_sidecar.json",
            index_path=args.artifacts / profile.key / f"chromalab_knowledge_v2_{profile.key}.tvim",
            k=args.k,
            lexical_cases=lexical["cases"],
        )
        for profile in EMBEDDING_PROFILES
    ]
    report: dict[str, Any] = {
        "phase": "TV-3" if args.mode == "tv3" else "TV-2",
        "mode": args.mode,
        "pack_path": str(args.pack.as_posix()),
        "pack_version": pack.get("version"),
        "pack_hash": sha256_file(args.pack),
        "k": args.k,
        "lexical": lexical,
        "dense_profiles": dense_profiles,
    }
    if args.mode == "tv3":
        report["policy_candidates"] = build_policy_candidates(lexical, dense_profiles, args.k)
        report["decision"] = decide_tv3(report)
    else:
        report["decision"] = decide_tv2(report)

    args.out.mkdir(parents=True, exist_ok=True)
    write_json(args.out / "summary.json", report)
    if args.mode == "tv3":
        write_tv3_summary(args.out / "summary.md", report)
    else:
        write_tv2_summary(args.out / "summary.md", report)
    return 0


def run_lexical(pack: dict[str, Any], k: int) -> dict[str, Any]:
    started = time.perf_counter()
    cases = []
    for golden in GOLDEN_QUERIES:
        results = lexical_search(pack, golden["query"], k)
        evaluation = evaluate_results(
            results,
            golden["expected_entry_ids"],
            golden.get("required_entry_ids"),
            golden.get("forbidden_entry_ids"),
        )
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
            evaluation = evaluate_results(
                dense_results,
                golden["expected_entry_ids"],
                golden.get("required_entry_ids"),
                golden.get("forbidden_entry_ids"),
            )
            lexical_case = case_by_id(lexical_cases, golden["query_id"])
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
        safety_regressions = [case for case in cases if is_safety_regression(case, case_by_id(lexical_cases, case["query_id"]))]
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


def build_policy_candidates(
    lexical: dict[str, Any],
    dense_profiles: list[dict[str, Any]],
    k: int,
) -> list[dict[str, Any]]:
    policies: list[dict[str, Any]] = [
        build_policy_result("LEXICAL_ONLY", "lexical", lexical["cases"], lexical["cases"], k),
    ]
    profiles = {profile["profile_key"]: profile for profile in dense_profiles}
    for profile_key, policy_name in (("minilm", "MINILM_ONLY"), ("bge_base", "BGE_ONLY")):
        profile = profiles.get(profile_key)
        if profile is None or not profile.get("cases"):
            policies.append(missing_policy(policy_name, profile_key))
        else:
            policies.append(build_policy_result(policy_name, profile_key, profile["cases"], lexical["cases"], k))
    for policy_name, profile_key in HYBRID_POLICIES.items():
        profile = profiles.get(profile_key)
        if profile is None or not profile.get("cases"):
            policies.append(missing_policy(policy_name, profile_key))
        else:
            hybrid_cases = [
                hybrid_lexical_guard_case(
                    policy_name=policy_name,
                    lexical_case=lexical_case,
                    dense_case=case_by_id(profile["cases"], lexical_case["query_id"]),
                    dense_profile=profile_key,
                    k=k,
                )
                for lexical_case in lexical["cases"]
            ]
            policies.append(build_policy_result(policy_name, f"lexical+{profile_key}", hybrid_cases, lexical["cases"], k))
    profiles_for_rrf = [profile for profile in (profiles.get("bge_base"), profiles.get("minilm")) if profile and profile.get("cases")]
    if len(profiles_for_rrf) == 2:
        rrf_cases = [
            hybrid_rrf_case(
                lexical_case=lexical_case,
                dense_cases=[case_by_id(profile["cases"], lexical_case["query_id"]) for profile in profiles_for_rrf],
                k=k,
            )
            for lexical_case in lexical["cases"]
        ]
        policies.append(build_policy_result("HYBRID_UNION_RRF", "lexical+bge_base+minilm", rrf_cases, lexical["cases"], k))
    else:
        policies.append(missing_policy("HYBRID_UNION_RRF", "bge_base+minilm"))
    return policies


def build_policy_result(
    policy_id: str,
    source: str,
    cases: list[dict[str, Any]],
    lexical_cases: list[dict[str, Any]],
    k: int,
) -> dict[str, Any]:
    evaluated = []
    for case in cases:
        top_results = [{"entry_id": entry_id, "score": float(k - index)} for index, entry_id in enumerate(case["top_ids"][:k])]
        evaluation = evaluate_results(
            top_results,
            case["expected_entry_ids"],
            case.get("required_entry_ids"),
            case.get("forbidden_entry_ids"),
        )
        lexical_case = case_by_id(lexical_cases, case["query_id"])
        evaluated.append(
            {
                **case,
                **evaluation,
                "lexical_first_hit_rank": lexical_case["first_hit_rank"],
                "lexical_top1_hit": lexical_case["top1_hit"],
                "lexical_missing_required_entry_ids": lexical_case["missing_required_entry_ids"],
                "improved_vs_lexical": rank_better(evaluation["first_hit_rank"], lexical_case["first_hit_rank"]),
                "regressed_vs_lexical": rank_worse(evaluation["first_hit_rank"], lexical_case["first_hit_rank"]),
                "recovered_lexical_miss": (not lexical_case["hit"]) and evaluation["hit"],
                "safety_regression": is_safety_regression({**case, **evaluation}, lexical_case),
                "exact_rule_top1_regression": exact_rule_top1_regression({**case, **evaluation}, lexical_case),
            },
        )
    safety_required_misses = [
        case for case in evaluated
        if case["safety_critical"] and case["missing_required_entry_ids"]
    ]
    safety_regressions = [case for case in evaluated if case["safety_regression"]]
    exact_rule_top1_regressions = [case for case in evaluated if case["exact_rule_top1_regression"]]
    semantic_miss_recoveries = [
        case for case in evaluated
        if case["recovered_lexical_miss"] and case["query_class"] in {"semantic_caveat", "natural_language", "warning_explanation"}
    ]
    required_misses = [case for case in evaluated if case["missing_required_entry_ids"]]
    return {
        "policy_id": policy_id,
        "source": source,
        "status": "PASS" if not safety_regressions and not exact_rule_top1_regressions else "REVIEW",
        "hit_count": sum(1 for case in evaluated if case["hit"]),
        "top1_hit_count": sum(1 for case in evaluated if case["top1_hit"]),
        "case_count": len(evaluated),
        "improvement_count": sum(1 for case in evaluated if case["improved_vs_lexical"]),
        "regression_count": sum(1 for case in evaluated if case["regressed_vs_lexical"]),
        "semantic_miss_recovered_count": len(semantic_miss_recoveries),
        "required_miss_count": len(required_misses),
        "safety_required_miss_count": len(safety_required_misses),
        "safety_regression_count": len(safety_regressions),
        "exact_rule_top1_regression_count": len(exact_rule_top1_regressions),
        "cases": evaluated,
    }


def missing_policy(policy_id: str, source: str) -> dict[str, Any]:
    return {
        "policy_id": policy_id,
        "source": source,
        "status": "MISSING_PROFILE",
        "case_count": 0,
        "hit_count": 0,
        "top1_hit_count": 0,
        "improvement_count": 0,
        "regression_count": 0,
        "semantic_miss_recovered_count": 0,
        "required_miss_count": 0,
        "safety_required_miss_count": 0,
        "safety_regression_count": 0,
        "exact_rule_top1_regression_count": 0,
        "cases": [],
    }


def hybrid_lexical_guard_case(
    policy_name: str,
    lexical_case: dict[str, Any],
    dense_case: dict[str, Any],
    dense_profile: str,
    k: int,
) -> dict[str, Any]:
    if lexical_case["query_class"] in {"exact_rule", "safety_boundary", "warning_explanation"}:
        top_ids = merge_ids(lexical_case["top_ids"], dense_case["top_ids"], k)
        rule = "lexical_guard"
    else:
        top_ids = merge_ids(dense_case["top_ids"], lexical_case["top_ids"], k)
        rule = f"{dense_profile}_semantic_first"
    return {
        **lexical_case,
        "top_ids": top_ids,
        "policy_id": policy_name,
        "policy_rule": rule,
    }


def hybrid_rrf_case(
    lexical_case: dict[str, Any],
    dense_cases: list[dict[str, Any]],
    k: int,
) -> dict[str, Any]:
    scores: dict[str, float] = {}
    for source_case in [lexical_case, *dense_cases]:
        for rank, entry_id in enumerate(source_case["top_ids"], start=1):
            scores[entry_id] = scores.get(entry_id, 0.0) + 1.0 / (60.0 + rank)
    ordered = sorted(scores, key=lambda entry_id: (-scores[entry_id], entry_id))
    rule = "rrf_union"
    if lexical_case["safety_critical"] and lexical_case["query_class"] == "exact_rule" and lexical_case["top_ids"]:
        pinned = lexical_case["top_ids"][0]
        ordered = [pinned] + [entry_id for entry_id in ordered if entry_id != pinned]
        rule = "rrf_union_lexical_top1_pinned"
    return {
        **lexical_case,
        "top_ids": ordered[:k],
        "policy_id": "HYBRID_UNION_RRF",
        "policy_rule": rule,
    }


def merge_ids(primary: list[str], secondary: list[str], k: int) -> list[str]:
    merged: list[str] = []
    for entry_id in [*primary, *secondary]:
        if entry_id not in merged:
            merged.append(entry_id)
        if len(merged) >= k:
            break
    return merged


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


def rank_better(candidate_rank: int | None, lexical_rank: int | None) -> bool:
    if candidate_rank is None:
        return False
    if lexical_rank is None:
        return True
    return candidate_rank < lexical_rank


def rank_worse(candidate_rank: int | None, lexical_rank: int | None) -> bool:
    if lexical_rank is None:
        return False
    if candidate_rank is None:
        return True
    return candidate_rank > lexical_rank


def is_safety_regression(candidate_case: dict[str, Any], lexical_case: dict[str, Any]) -> bool:
    if candidate_case["forbidden_entry_present"]:
        return True
    if not candidate_case["safety_critical"]:
        return False
    lexical_missing = set(lexical_case.get("missing_required_entry_ids", []))
    candidate_missing = set(candidate_case.get("missing_required_entry_ids", []))
    if candidate_missing - lexical_missing:
        return True
    return exact_rule_top1_regression(candidate_case, lexical_case)


def exact_rule_top1_regression(candidate_case: dict[str, Any], lexical_case: dict[str, Any]) -> bool:
    return bool(
        candidate_case["safety_critical"]
        and candidate_case["query_class"] == "exact_rule"
        and lexical_case["top1_hit"]
        and not candidate_case["top1_hit"]
    )


def decide_tv2(report: dict[str, Any]) -> dict[str, Any]:
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


def decide_tv3(report: dict[str, Any]) -> dict[str, Any]:
    policies = report["policy_candidates"]
    hybrid_policies = [policy for policy in policies if policy["policy_id"].startswith("HYBRID_")]
    passing = [
        policy for policy in hybrid_policies
        if policy["status"] == "PASS"
        and policy["semantic_miss_recovered_count"] >= 1
        and policy["safety_regression_count"] == 0
        and policy["safety_required_miss_count"] == 0
        and policy["exact_rule_top1_regression_count"] == 0
    ]
    if passing:
        selected = sorted(
            passing,
            key=lambda policy: (
                -policy["semantic_miss_recovered_count"],
                -policy["hit_count"],
                -policy["top1_hit_count"],
                policy["regression_count"],
                policy["policy_id"],
            ),
        )[0]
        return {
            "verdict": "TV3_READY_FOR_TV4_BACKEND_PROMOTION_CANDIDATE",
            "selected_policy_id": selected["policy_id"],
            "next_step": "Proceed to TV-4 backend abstraction/promotion candidate using the selected policy as the benchmark target.",
        }
    if any(policy.get("safety_regression_count", 0) for policy in policies):
        return {
            "verdict": "TV3_REJECT_DENSE_POLICY_SAFETY_REGRESSION",
            "selected_policy_id": None,
            "next_step": "Do not promote TurboVec; keep lexical active and inspect rejected policy cases.",
        }
    if any(policy.get("improvement_count", 0) for policy in policies):
        return {
            "verdict": "TV3_REVIEW_KEEP_TURBOVEC_SHADOW",
            "selected_policy_id": None,
            "next_step": "Keep TurboVec shadow-only; expand policy or goldens before promotion.",
        }
    return {
        "verdict": "TV3_REJECT_NO_DENSE_IMPROVEMENT",
        "selected_policy_id": None,
        "next_step": "Keep lexical retrieval as the only active owner.",
    }


def case_by_id(cases: list[dict[str, Any]], query_id: str) -> dict[str, Any]:
    return next(case for case in cases if case["query_id"] == query_id)


def elapsed_ms(started: float) -> int:
    return int((time.perf_counter() - started) * 1000)


def write_tv2_summary(path: Path, report: dict[str, Any]) -> None:
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
                f"- Lexical top ids: `{', '.join(case_by_id(report['lexical']['cases'], golden['query_id'])['top_ids'])}`",
            ],
        )
        for profile in report["dense_profiles"]:
            case = next((case for case in profile.get("cases", []) if case["query_id"] == golden["query_id"]), None)
            top_ids = ", ".join(case["top_ids"]) if case else "n/a"
            lines.append(f"- {profile['profile_key']} top ids: `{top_ids}`")
        lines.append("")
    path.write_text("\n".join(lines), encoding="utf-8")


def write_tv3_summary(path: Path, report: dict[str, Any]) -> None:
    lines = [
        "# TV-3 TurboVec Retrieval A/B Arbitration Benchmark",
        "",
        f"- Verdict: `{report['decision']['verdict']}`",
        f"- Selected policy: `{report['decision'].get('selected_policy_id') or 'none'}`",
        f"- Next step: {report['decision']['next_step']}",
        f"- Pack: `{report['pack_path']}`",
        f"- Pack version: `{report['pack_version']}`",
        f"- Pack hash: `{report['pack_hash']}`",
        f"- k: `{report['k']}`",
        "",
        "## Policy Summary",
        "",
        "| Policy | Status | Hits | Top-1 | Improvements | Regressions | Semantic miss recoveries | Required misses | Safety misses | Safety regressions | Exact top-1 regressions |",
        "|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|",
    ]
    for policy in report["policy_candidates"]:
        lines.append(
            "| {policy_id} | {status} | {hits}/{cases} | {top1}/{cases} | {improvements} | {regressions} | {recoveries} | {required_miss} | {safety_miss} | {safety_regression} | {exact_regression} |".format(
                policy_id=policy["policy_id"],
                status=policy["status"],
                hits=policy.get("hit_count", 0),
                top1=policy.get("top1_hit_count", 0),
                cases=policy.get("case_count", 0),
                improvements=policy.get("improvement_count", 0),
                regressions=policy.get("regression_count", 0),
                recoveries=policy.get("semantic_miss_recovered_count", 0),
                required_miss=policy.get("required_miss_count", 0),
                safety_miss=policy.get("safety_required_miss_count", 0),
                safety_regression=policy.get("safety_regression_count", 0),
                exact_regression=policy.get("exact_rule_top1_regression_count", 0),
            ),
        )
    lines.extend(["", "## Query Decisions", ""])
    for golden in GOLDEN_QUERIES:
        lines.extend(
            [
                f"### {golden['query_id']}",
                "",
                f"- Class: `{golden['query_class']}`",
                f"- Safety critical: `{golden['safety_critical']}`",
                f"- Required: `{', '.join(golden['required_entry_ids'])}`",
            ],
        )
        for policy in report["policy_candidates"]:
            case = next((case for case in policy.get("cases", []) if case["query_id"] == golden["query_id"]), None)
            if case is None:
                lines.append(f"- {policy['policy_id']}: `n/a`")
            else:
                lines.append(
                    "- {policy}: rank `{rank}`, top1 `{top1}`, missing `{missing}`, forbidden `{forbidden}`, top ids `{top_ids}`".format(
                        policy=policy["policy_id"],
                        rank=case["first_hit_rank"],
                        top1=case["top1_hit"],
                        missing=", ".join(case["missing_required_entry_ids"]) or "none",
                        forbidden=", ".join(case["forbidden_entry_ids_present"]) or "none",
                        top_ids=", ".join(case["top_ids"]),
                    ),
                )
        lines.append("")
    path.write_text("\n".join(lines), encoding="utf-8")


if __name__ == "__main__":
    raise SystemExit(main())
