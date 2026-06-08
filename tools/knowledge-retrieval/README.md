# TV-2 Knowledge Retrieval Tools

This folder contains PC-only tooling for the TurboVec Knowledge retrieval
prototype. It is not Android runtime code.

## Boundary

- Reads `docs/knowledge/chromalab_knowledge_seed_v2.json`.
- Builds local dense indexes for curated Knowledge entries only.
- Writes large generated artifacts under ignored `artifacts/tv2-turbovec-knowledge/`.
- Writes compact benchmark summaries under `benchmark/reports/tv2_turbovec_knowledge/`.
- Does not change graph detection, calibration, trace extraction, peak metrics,
  report gates, `CalculationEngine`, or Android runtime dependencies.

## Setup

Use Python 3.12 for current sentence-transformers / PyTorch compatibility.

```powershell
C:\Python312\python.exe -m venv artifacts\tv2-turbovec-knowledge\.venv
artifacts\tv2-turbovec-knowledge\.venv\Scripts\python.exe -m pip install -r tools\knowledge-retrieval\requirements.txt
```

## Build indexes

```powershell
artifacts\tv2-turbovec-knowledge\.venv\Scripts\python.exe tools\knowledge-retrieval\build_turbovec_indexes.py `
  --pack docs\knowledge\chromalab_knowledge_seed_v2.json `
  --out artifacts\tv2-turbovec-knowledge
```

## Run benchmark

```powershell
artifacts\tv2-turbovec-knowledge\.venv\Scripts\python.exe tools\knowledge-retrieval\run_turbovec_retrieval_benchmark.py `
  --out benchmark\reports\tv2_turbovec_knowledge
```

The benchmark compares lexical retrieval with TurboVec indexes for:

- `sentence-transformers/all-MiniLM-L6-v2`
- `BAAI/bge-base-en-v1.5`

Both models remain PC-only candidates until the benchmark and later Android
packaging gates pass.
