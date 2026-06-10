from __future__ import annotations

import argparse
import importlib.metadata
import time
from pathlib import Path
from typing import Any

import numpy as np

from tv2_common import EMBEDDING_PROFILES, GOLDEN_QUERIES, read_json, sha256_file, write_json


DEFAULT_QUERY_IDS = (
    "sn_signal_to_noise",
    "ion71_title_channel",
    "knowledge_cannot_create_metrics",
)


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Build TV-8 Android gate query vectors for a real TurboVec Knowledge index.",
    )
    parser.add_argument("--pack", required=True, type=Path)
    parser.add_argument("--sidecar", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    parser.add_argument("--profile", default="minilm")
    parser.add_argument("--query-ids", default=",".join(DEFAULT_QUERY_IDS))
    args = parser.parse_args()

    started = time.perf_counter()
    profile = next((item for item in EMBEDDING_PROFILES if item.key == args.profile), None)
    if profile is None:
        raise SystemExit(f"Unknown embedding profile: {args.profile}")
    pack = read_json(args.pack)
    sidecar = read_json(args.sidecar)
    if sidecar.get("profile_key") != profile.key:
        raise SystemExit(
            f"Sidecar profile mismatch: expected {profile.key}, got {sidecar.get('profile_key')}",
        )
    if sidecar.get("dimension") != profile.expected_dim:
        raise SystemExit(
            f"Sidecar dimension mismatch: expected {profile.expected_dim}, got {sidecar.get('dimension')}",
        )

    selected_ids = [value.strip() for value in args.query_ids.split(",") if value.strip()]
    goldens = [query for query in GOLDEN_QUERIES if query["query_id"] in selected_ids]
    missing = sorted(set(selected_ids) - {query["query_id"] for query in goldens})
    if missing:
        raise SystemExit(f"Unknown query ids: {missing}")

    try:
        from sentence_transformers import SentenceTransformer
    except Exception as exc:
        raise SystemExit(f"sentence-transformers import failed: {exc!r}") from exc

    model = SentenceTransformer(profile.model_id)
    texts = [profile.query_prefix + query["query"] for query in goldens]
    embeddings = model.encode(
        texts,
        batch_size=8,
        normalize_embeddings=True,
        show_progress_bar=False,
    )
    vectors = np.asarray(embeddings, dtype=np.float32)
    if vectors.ndim != 2 or vectors.shape[1] != profile.expected_dim:
        raise SystemExit(f"Unexpected vector shape: {vectors.shape}")

    payload: dict[str, Any] = {
        "phase": "TV-8",
        "profileKey": profile.key,
        "modelId": profile.model_id,
        "dimension": profile.expected_dim,
        "license": profile.license,
        "packVersion": pack.get("version"),
        "packHashSha256": sha256_file(args.pack),
        "sidecarPackHashSha256": sidecar.get("pack_hash_sha256"),
        "queryEmbeddingRuntime": "PC_SENTENCE_TRANSFORMERS_REFERENCE",
        "localAndroidEmbeddingAvailable": False,
        "localAndroidEmbeddingBlocker": "No Android MiniLM/ONNX/TFLite embedding runtime is present in the app.",
        "sentenceTransformersVersion": package_version("sentence-transformers"),
        "queries": [],
        "elapsedMs": int((time.perf_counter() - started) * 1000),
    }
    for query, vector in zip(goldens, vectors):
        payload["queries"].append(
            {
                "queryId": query["query_id"],
                "query": query["query"],
                "queryClass": query["query_class"],
                "safetyCritical": query["safety_critical"],
                "category": query["category"],
                "k": 8,
                "expectedEntryIds": query["expected_entry_ids"],
                "requiredEntryIds": query.get("required_entry_ids", query["expected_entry_ids"]),
                "forbiddenEntryIds": query.get("forbidden_entry_ids", []),
                "vector": [round(float(value), 8) for value in vector.tolist()],
            },
        )

    write_json(args.out, payload)
    return 0


def package_version(package: str) -> str | None:
    try:
        return importlib.metadata.version(package)
    except importlib.metadata.PackageNotFoundError:
        return None


if __name__ == "__main__":
    raise SystemExit(main())
