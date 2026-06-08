# TV-2 TurboVec Knowledge Index Prototype Closeout

Date: 2026-06-08

Status: `TV2_READY_FOR_TV3_AB_EVALUATION`

Scope: PC-only Knowledge retrieval prototype. TV-2 did not change Android
runtime behavior, chromatogram geometry, calibration, trace extraction, peak
metrics, report gates, validators, or `CalculationEngine`.

## Goal

TV-2 tested whether TurboVec can build repeatable local dense indexes for
ChromaLab Knowledge Pack v2 and whether dense retrieval can improve semantic
warning/caveat lookup without weakening safety boundaries.

The active app retrieval owner remains lexical:

```text
KnowledgeRetrievalEngine facade
    -> active: LexicalKnowledgeRetrievalBackend
    -> candidate: TurboVec PC prototype only
```

TurboVec is not active Android runtime code.

## Tools Added

| Path | Purpose |
|---|---|
| `tools/knowledge-retrieval/requirements.txt` | PC-only Python dependencies for TV-2. |
| `tools/knowledge-retrieval/build_turbovec_indexes.py` | Builds TurboVec `.tvim` indexes and sidecars from Knowledge Pack v2. |
| `tools/knowledge-retrieval/run_turbovec_retrieval_benchmark.py` | Compares lexical retrieval against TurboVec + MiniLM and TurboVec + BGE. |
| `tools/knowledge-retrieval/tv2_common.py` | Shared document building, stable id mapping, lexical scorer, goldens, and evaluation helpers. |
| `tools/knowledge-retrieval/README.md` | PC-only setup and command notes. |
| `benchmark/reports/tv2_turbovec_knowledge/summary.md` | Compact source-controlled benchmark summary. |
| `benchmark/reports/tv2_turbovec_knowledge/summary.json` | Machine-readable compact benchmark summary. |

Generated heavy artifacts are stored only under ignored
`artifacts/tv2-turbovec-knowledge/`.

## Dependency Gate

| Component | Version / model | Status | Notes |
|---|---|---|---|
| Python | 3.12.10 | PASS | Installed locally for PC tooling through Chocolatey. |
| TurboVec Python package | 0.7.0 | PASS | Alpha package; PC prototype only. |
| `sentence-transformers` | 5.1.2 | PASS | Used for local embedding generation in an ignored venv. |
| MiniLM profile | `sentence-transformers/all-MiniLM-L6-v2` | PASS | 384-dim, Apache-2.0 model card license. |
| BGE base profile | `BAAI/bge-base-en-v1.5` | PASS | 768-dim, MIT model card license. |

No Android dependency was added. No cloud vector database or remote search was
added.

## Index Build Results

Input pack: `docs/knowledge/chromalab_knowledge_seed_v2.json`

Pack version: `chromalab-knowledge-v2`

Pack hash:
`99a5343dc303492ee842e3856d87053040a5f1087d3f7e58c8b38c83023da8f7`

Entry count: `112`

TurboVec bit width: `4`

| Profile | Model | Status | Dimension | Index size | Peak memory | Build time |
|---|---|---:|---:|---:|---:|---:|
| `minilm` | `sentence-transformers/all-MiniLM-L6-v2` | PASS | 384 | 25,938 bytes | 26,046,837 bytes | 25,073 ms |
| `bge_base` | `BAAI/bge-base-en-v1.5` | PASS | 768 | 50,514 bytes | 27,060,092 bytes | 46,538 ms |

Sidecar mapping is stable and collision-free. Each TurboVec `u64` id maps back
to a valid `KnowledgeEntry.entryId`.

## Benchmark Results

Benchmark output:
`benchmark/reports/tv2_turbovec_knowledge/summary.md`

| Backend | Status | Expected-entry hits | Top-1 hits | Improvements | Regressions | Safety regressions |
|---|---|---:|---:|---:|---:|---:|
| `lexical_bm25` | PASS | 9/10 | 7/10 | 0 | 0 | 0 |
| `minilm` | PASS | 9/10 | 7/10 | 2 | 2 | 0 |
| `bge_base` | PASS | 10/10 | 6/10 | 2 | 2 | 0 |

Dense retrieval improved useful semantic cases:

- both dense profiles promoted `kp2-term-sn` to top rank for the S/N query;
- MiniLM promoted `kp2-snippet-ocr-ambiguity-warning` to top rank;
- BGE retrieved the compound-identification caveat for a natural-language
  photo-only compound-ID question that lexical missed.

Dense retrieval also showed non-safety regressions on exact/rule-like tasks,
including ion/channel and calibration-warning rank changes. That means TV-2
does not justify replacing the active lexical owner yet. TV-3 must evaluate an
arbitration or hybrid policy before promotion.

## Safety Result

TV-2 preserved the mandatory Knowledge safety boundary:

- no private chromatogram images, reports, logs, Android artifacts, or local
  diagnostic bundles were embedded;
- generated text is bounded to Knowledge Pack fields;
- every dense result maps back to a valid `KnowledgeEntry.entryId`;
- `usedEntryIds` compatibility remains intact through stable sidecar ids;
- no dense backend is allowed to create graph geometry, calibration
  coefficients, RT, height, area, FWHM, S/N, baseline, Kovats, peak count, or
  compound identity;
- forbidden-use goldens had 0 safety regressions.

## Decision

TV-2 passes as a PC-only prototype and should proceed to:

```text
TV-3 - Retrieval A/B Evaluation And Arbitration Policy
```

TV-3 must decide whether TurboVec becomes an active retrieval owner, remains a
shadow dense reranker, or is rejected. Until TV-3 passes, the active owner
remains `LexicalKnowledgeRetrievalBackend`.

## Remaining Risks

- TurboVec Python package maturity is alpha.
- Rust crate and Android native packaging were not tested in TV-2.
- Dense retrieval improved semantic/caveat recall but did not dominate lexical
  retrieval on exact policy/rule tasks.
- TV-2 artifacts in `artifacts/tv2-turbovec-knowledge/` are local and ignored;
  they can be rebuilt from the tracked scripts and Knowledge Pack.

## Validation

Commands run:

```powershell
git diff --check
python tools/knowledge-retrieval/build_turbovec_indexes.py --pack docs/knowledge/chromalab_knowledge_seed_v2.json --out artifacts/tv2-turbovec-knowledge
python tools/knowledge-retrieval/run_turbovec_retrieval_benchmark.py --out benchmark/reports/tv2_turbovec_knowledge
.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.knowledge.KnowledgeRetrievalLayerTest"
.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.knowledge.KnowledgePackV2Test"
.\gradlew.bat :composeApp:compileKotlinDesktop
```

No Android build is required for TV-2 because this phase is PC-only and does not
change Android runtime behavior.
