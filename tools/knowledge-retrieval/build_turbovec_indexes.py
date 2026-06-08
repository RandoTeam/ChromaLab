from __future__ import annotations

import argparse
import importlib.metadata
import os
import platform
import sys
import time
import tracemalloc
from pathlib import Path
from typing import Any

import numpy as np

from tv2_common import (
    EMBEDDING_PROFILES,
    bounded_documents,
    build_entry_id_map,
    read_json,
    sha256_file,
    write_json,
)


def main() -> int:
    parser = argparse.ArgumentParser(description="Build PC-only TurboVec Knowledge indexes.")
    parser.add_argument("--pack", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    parser.add_argument(
        "--models",
        default=",".join(profile.key for profile in EMBEDDING_PROFILES),
        help="Comma-separated profile keys. Defaults to all TV-2 profiles.",
    )
    parser.add_argument("--bit-width", type=int, default=4)
    args = parser.parse_args()

    os.environ.setdefault("HF_HUB_DISABLE_SYMLINKS_WARNING", "1")
    selected = {key.strip() for key in args.models.split(",") if key.strip()}
    profiles = [profile for profile in EMBEDDING_PROFILES if profile.key in selected]
    if not profiles:
        raise SystemExit(f"No matching profiles for {sorted(selected)}")

    pack = read_json(args.pack)
    documents = bounded_documents(pack)
    entry_to_numeric = build_entry_id_map(pack["entries"])
    pack_hash = sha256_file(args.pack)
    args.out.mkdir(parents=True, exist_ok=True)

    manifest: dict[str, Any] = {
        "phase": "TV-2",
        "status": "STARTED",
        "pack_path": str(args.pack.as_posix()),
        "pack_version": pack.get("version"),
        "pack_hash_sha256": pack_hash,
        "entry_count": len(documents),
        "python": sys.version,
        "platform": platform.platform(),
        "bit_width": args.bit_width,
        "profiles": [],
    }

    for profile in profiles:
        manifest["profiles"].append(
            build_profile_index(
                profile=profile,
                documents=documents,
                entry_to_numeric=entry_to_numeric,
                pack_hash=pack_hash,
                out_dir=args.out / profile.key,
                bit_width=args.bit_width,
            ),
        )

    passed = [profile for profile in manifest["profiles"] if profile["status"] == "PASS"]
    manifest["status"] = "PASS" if passed else "BLOCKED_TURBOVEC_INDEX_BUILD"
    manifest["passed_profile_keys"] = [profile["profile_key"] for profile in passed]
    write_json(args.out / "tv2_index_build_manifest.json", manifest)
    write_summary(args.out / "tv2_index_build_summary.md", manifest)
    return 0 if passed else 2


def build_profile_index(
    profile,
    documents: list[dict[str, Any]],
    entry_to_numeric: dict[str, int],
    pack_hash: str,
    out_dir: Path,
    bit_width: int,
) -> dict[str, Any]:
    started = time.perf_counter()
    out_dir.mkdir(parents=True, exist_ok=True)
    result: dict[str, Any] = {
        "profile_key": profile.key,
        "model_id": profile.model_id,
        "expected_dim": profile.expected_dim,
        "license": profile.license,
        "status": "STARTED",
    }
    try:
        from sentence_transformers import SentenceTransformer
        from turbovec import IdMapIndex
    except Exception as exc:
        result.update(
            status="REJECTED_DEPENDENCY_IMPORT",
            error=repr(exc),
            elapsed_ms=elapsed_ms(started),
        )
        return result

    try:
        tracemalloc.start()
        model = SentenceTransformer(profile.model_id)
        texts = [profile.document_prefix + doc["text"] for doc in documents]
        embeddings = model.encode(
            texts,
            batch_size=32,
            normalize_embeddings=True,
            show_progress_bar=False,
        )
        vectors = np.asarray(embeddings, dtype=np.float32)
        if vectors.ndim != 2:
            raise ValueError(f"expected 2-D embeddings, got shape {vectors.shape}")
        if vectors.shape[1] != profile.expected_dim:
            raise ValueError(f"expected dim {profile.expected_dim}, got {vectors.shape[1]}")
        ids = np.asarray([entry_to_numeric[doc["entry_id"]] for doc in documents], dtype=np.uint64)
        index = IdMapIndex(dim=profile.expected_dim, bit_width=bit_width)
        add_with_ids(index, vectors, ids)
        index_path = out_dir / f"chromalab_knowledge_v2_{profile.key}.tvim"
        index.write(str(index_path))
        current_bytes, peak_bytes = tracemalloc.get_traced_memory()
        tracemalloc.stop()

        sidecar = {
            "phase": "TV-2",
            "profile_key": profile.key,
            "model_id": profile.model_id,
            "dimension": profile.expected_dim,
            "license": profile.license,
            "bit_width": bit_width,
            "pack_hash_sha256": pack_hash,
            "entry_count": len(documents),
            "index_path": str(index_path.as_posix()),
            "index_size_bytes": index_path.stat().st_size,
            "id_map": [
                {
                    "u64": str(entry_to_numeric[doc["entry_id"]]),
                    "entry_id": doc["entry_id"],
                    "type": doc["type"],
                }
                for doc in documents
            ],
            "tracemalloc_current_bytes": current_bytes,
            "tracemalloc_peak_bytes": peak_bytes,
            "elapsed_ms": elapsed_ms(started),
            "turbovec_version": package_version("turbovec"),
            "sentence_transformers_version": package_version("sentence-transformers"),
        }
        sidecar_path = out_dir / f"chromalab_knowledge_v2_{profile.key}_sidecar.json"
        write_json(sidecar_path, sidecar)
        result.update(
            status="PASS",
            dimension=profile.expected_dim,
            index_path=str(index_path.as_posix()),
            sidecar_path=str(sidecar_path.as_posix()),
            index_size_bytes=sidecar["index_size_bytes"],
            tracemalloc_peak_bytes=peak_bytes,
            elapsed_ms=sidecar["elapsed_ms"],
            turbovec_version=sidecar["turbovec_version"],
        )
    except Exception as exc:
        if tracemalloc.is_tracing():
            tracemalloc.stop()
        result.update(
            status="REJECTED_INDEX_BUILD",
            error=repr(exc),
            elapsed_ms=elapsed_ms(started),
        )
    return result


def add_with_ids(index: Any, vectors: np.ndarray, ids: np.ndarray) -> None:
    try:
        index.add_with_ids(vectors, ids)
        return
    except TypeError:
        index.add_with_ids(vectors.reshape(-1), ids)


def package_version(package: str) -> str | None:
    try:
        return importlib.metadata.version(package)
    except importlib.metadata.PackageNotFoundError:
        return None


def elapsed_ms(started: float) -> int:
    return int((time.perf_counter() - started) * 1000)


def write_summary(path: Path, manifest: dict[str, Any]) -> None:
    lines = [
        "# TV-2 TurboVec Knowledge Index Build Summary",
        "",
        f"- Status: `{manifest['status']}`",
        f"- Pack: `{manifest['pack_path']}`",
        f"- Pack version: `{manifest['pack_version']}`",
        f"- Entry count: `{manifest['entry_count']}`",
        f"- Bit width: `{manifest['bit_width']}`",
        "",
        "| Profile | Model | Status | Dimension | Index size | Peak memory | Time |",
        "|---|---|---|---:|---:|---:|---:|",
    ]
    for profile in manifest["profiles"]:
        lines.append(
            "| {profile_key} | {model_id} | {status} | {dimension} | {index_size} | {memory} | {elapsed} |".format(
                profile_key=profile["profile_key"],
                model_id=profile["model_id"],
                status=profile["status"],
                dimension=profile.get("dimension", profile.get("expected_dim", "")),
                index_size=profile.get("index_size_bytes", ""),
                memory=profile.get("tracemalloc_peak_bytes", ""),
                elapsed=profile.get("elapsed_ms", ""),
            ),
        )
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


if __name__ == "__main__":
    raise SystemExit(main())
