# TV-3 Retrieval A/B Arbitration Policy Closeout

Date: 2026-06-08

Status: `TV3_READY_FOR_TV4_BACKEND_PROMOTION_CANDIDATE`

Scope: PC-only Knowledge retrieval evaluation. TV-3 did not change Android
runtime behavior, chromatogram geometry, calibration, trace extraction, peak
metrics, report gates, validators, or `CalculationEngine`.

## Goal

TV-3 evaluated whether TurboVec dense retrieval can be used safely as part of a
Knowledge retrieval policy after TV-2 showed both useful semantic improvements
and exact/rule-like rank regressions.

The active runtime owner remains:

```text
KnowledgeRetrievalEngine facade
    -> active: LexicalKnowledgeRetrievalBackend
```

TV-3 selects a benchmark target for the next implementation gate. It does not
promote TurboVec to Android runtime.

## Inputs

| Input | Path / value |
|---|---|
| Knowledge pack | `docs/knowledge/chromalab_knowledge_seed_v2.json` |
| Pack version | `chromalab-knowledge-v2` |
| Pack hash | `99a5343dc303492ee842e3856d87053040a5f1087d3f7e58c8b38c83023da8f7` |
| MiniLM index | ignored local `artifacts/tv2-turbovec-knowledge/minilm/*.tvim` |
| BGE index | ignored local `artifacts/tv2-turbovec-knowledge/bge_base/*.tvim` |
| TV-3 report | `benchmark/reports/tv3_retrieval_ab_arbitration/summary.md` |

## Query Model Added

TV-3 expands the benchmark query model with:

- `query_class`: `exact_rule`, `semantic_caveat`, `warning_explanation`,
  `safety_boundary`, or `natural_language`;
- `required_entry_ids`;
- `forbidden_entry_ids`;
- `safety_critical`.

Safety-critical cases cover numeric metric authority, calibration caveats,
Kovats/reference caveats, compound-identification caveats, peak annotation
rules, OCR ambiguity, and model-authority boundaries.

## Policies Evaluated

| Policy | Meaning |
|---|---|
| `LEXICAL_ONLY` | Current lexical BM25-style baseline. |
| `MINILM_ONLY` | TurboVec dense search using `sentence-transformers/all-MiniLM-L6-v2`. |
| `BGE_ONLY` | TurboVec dense search using `BAAI/bge-base-en-v1.5`. |
| `HYBRID_LEXICAL_GUARD_BGE` | Lexical first for exact/safety/warning queries; BGE first for natural/semantic queries. |
| `HYBRID_LEXICAL_GUARD_MINILM` | Same guard policy using MiniLM. |
| `HYBRID_UNION_RRF` | Reciprocal-rank fusion over lexical + BGE + MiniLM with lexical top-1 pinned for safety-critical exact-rule queries. |

## Result

| Policy | Status | Hits | Top-1 | Improvements | Regressions | Semantic miss recoveries | Required misses | Safety misses | Safety regressions | Exact top-1 regressions |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| `LEXICAL_ONLY` | PASS | 9/10 | 7/10 | 0 | 0 | 0 | 1 | 1 | 0 | 0 |
| `MINILM_ONLY` | REVIEW | 9/10 | 7/10 | 2 | 2 | 0 | 1 | 1 | 2 | 1 |
| `BGE_ONLY` | REVIEW | 10/10 | 6/10 | 2 | 2 | 1 | 0 | 0 | 1 | 1 |
| `HYBRID_LEXICAL_GUARD_BGE` | PASS | 10/10 | 7/10 | 1 | 0 | 1 | 0 | 0 | 0 | 0 |
| `HYBRID_LEXICAL_GUARD_MINILM` | PASS | 9/10 | 7/10 | 0 | 0 | 0 | 1 | 1 | 0 | 0 |
| `HYBRID_UNION_RRF` | PASS | 10/10 | 9/10 | 3 | 0 | 1 | 0 | 0 | 0 | 0 |

Selected policy:

```text
HYBRID_UNION_RRF
```

Why it wins:

- recovers the lexical miss for the photo-only compound-identification caveat;
- improves S/N and OCR-ambiguity ranking;
- preserves safety-critical exact-rule top-1 behavior;
- has 0 required-entry safety misses;
- has 0 safety regressions;
- maps every result to a valid `KnowledgeEntry.entryId`.

Dense-only policies remain rejected as promotion targets because they regress
safety-critical exact-rule ranking and, in MiniLM's case, can surface a compound
stub in a compound-caveat query.

## Decision

TV-3 passes and should proceed to:

```text
TV-4 - Knowledge Retrieval Backend Promotion Candidate
```

TV-4 may use `HYBRID_UNION_RRF` as the benchmark target, but it must still keep
the active runtime owner lexical until implementation and tests prove the policy
can preserve `usedEntryIds`, policy validation, deterministic ordering, and
forbidden-use safety in the Kotlin retrieval facade.

## Boundaries

TurboVec remains Knowledge retrieval only:

- no graph geometry;
- no calibration coefficients;
- no trace or peak metrics;
- no RT, height, area, FWHM, S/N, baseline, Kovats, or compound-ID authority;
- no report-gate authority;
- no Android dependency in TV-3.

## Validation

Commands run:

```powershell
git diff --check
artifacts\tv2-turbovec-knowledge\.venv\Scripts\python.exe tools\knowledge-retrieval\build_turbovec_indexes.py --pack docs\knowledge\chromalab_knowledge_seed_v2.json --out artifacts\tv2-turbovec-knowledge
artifacts\tv2-turbovec-knowledge\.venv\Scripts\python.exe tools\knowledge-retrieval\run_turbovec_retrieval_benchmark.py --mode tv3 --out benchmark\reports\tv3_retrieval_ab_arbitration
artifacts\tv2-turbovec-knowledge\.venv\Scripts\python.exe -m py_compile tools\knowledge-retrieval\*.py
.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.knowledge.KnowledgeRetrievalLayerTest"
.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.knowledge.KnowledgePackV2Test"
.\gradlew.bat :composeApp:compileKotlinDesktop
```

No Android build is required because TV-3 is PC-only and does not change app
runtime behavior.
