from __future__ import annotations

import hashlib
import json
import math
import re
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any


@dataclass(frozen=True)
class EmbeddingProfile:
    key: str
    model_id: str
    expected_dim: int
    license: str
    query_prefix: str = ""
    document_prefix: str = ""


EMBEDDING_PROFILES: tuple[EmbeddingProfile, ...] = (
    EmbeddingProfile(
        key="minilm",
        model_id="sentence-transformers/all-MiniLM-L6-v2",
        expected_dim=384,
        license="Apache-2.0",
    ),
    EmbeddingProfile(
        key="bge_base",
        model_id="BAAI/bge-base-en-v1.5",
        expected_dim=768,
        license="MIT",
        query_prefix="Represent this sentence for searching relevant passages: ",
    ),
)


GOLDEN_QUERIES: tuple[dict[str, Any], ...] = (
    {
        "query_id": "sn_signal_to_noise",
        "query": "S/N signal to noise",
        "expected_entry_ids": ["kp2-term-sn"],
        "required_entry_ids": ["kp2-term-sn"],
        "forbidden_entry_ids": [],
        "category": "glossary",
        "query_class": "exact_rule",
        "safety_critical": False,
    },
    {
        "query_id": "ion71_title_channel",
        "query": "Ion 71.00 (70.70 to 71.70) text classification",
        "expected_entry_ids": ["kp2-rule-ion-title-not-peak"],
        "required_entry_ids": ["kp2-rule-ion-title-not-peak"],
        "forbidden_entry_ids": [],
        "category": "text_classification",
        "query_class": "exact_rule",
        "safety_critical": True,
    },
    {
        "query_id": "sim_channel",
        "query": "SIM selected ion monitoring channel",
        "expected_entry_ids": ["kp2-ms-sim", "kp2-ms-selected-ion-monitoring"],
        "required_entry_ids": ["kp2-ms-sim"],
        "forbidden_entry_ids": [],
        "category": "mass_spectrometry",
        "query_class": "exact_rule",
        "safety_critical": False,
    },
    {
        "query_id": "peak_label_signal_verification",
        "query": "peak label requires signal verification",
        "expected_entry_ids": ["kp2-rule-peak-annotation-signal-verified"],
        "required_entry_ids": ["kp2-rule-peak-annotation-signal-verified"],
        "forbidden_entry_ids": [],
        "category": "text_classification",
        "query_class": "exact_rule",
        "safety_critical": True,
    },
    {
        "query_id": "kovats_without_reference",
        "query": "Kovats without reference series",
        "expected_entry_ids": ["kp2-caveat-no-kovats-without-reference"],
        "required_entry_ids": ["kp2-caveat-no-kovats-without-reference"],
        "forbidden_entry_ids": [],
        "category": "report_caveat",
        "query_class": "safety_boundary",
        "safety_critical": True,
    },
    {
        "query_id": "compound_without_evidence",
        "query": "compound assignment without explicit evidence",
        "expected_entry_ids": ["kp2-caveat-no-compound-assignment"],
        "required_entry_ids": ["kp2-caveat-no-compound-assignment"],
        "forbidden_entry_ids": [
            "kp2-compound-stub-n-c12-alkane",
            "kp2-compound-stub-n-c24-alkane",
            "kp2-compound-stub-n-c27-alkane",
            "kp2-compound-stub-n-c28-alkane",
        ],
        "category": "report_caveat",
        "query_class": "safety_boundary",
        "safety_critical": True,
    },
    {
        "query_id": "knowledge_cannot_create_metrics",
        "query": "knowledge cannot create numeric metrics",
        "expected_entry_ids": ["kp2-safety-knowledge-cannot-measure"],
        "required_entry_ids": ["kp2-safety-knowledge-cannot-measure"],
        "forbidden_entry_ids": [],
        "category": "safety",
        "query_class": "safety_boundary",
        "safety_critical": True,
    },
    {
        "query_id": "calibration_warning",
        "query": "calibration invalid warning missing anchors residual checks",
        "expected_entry_ids": ["kp2-snippet-calibration-invalid-warning", "kp2-caveat-calibration-required"],
        "required_entry_ids": ["kp2-snippet-calibration-invalid-warning"],
        "forbidden_entry_ids": [],
        "category": "warning_explanation",
        "query_class": "warning_explanation",
        "safety_critical": True,
    },
    {
        "query_id": "ocr_ambiguity_warning",
        "query": "ambiguous OCR crop provenance uncertain text into metrics",
        "expected_entry_ids": ["kp2-snippet-ocr-ambiguity-warning"],
        "required_entry_ids": ["kp2-snippet-ocr-ambiguity-warning"],
        "forbidden_entry_ids": [],
        "category": "warning_explanation",
        "query_class": "warning_explanation",
        "safety_critical": True,
    },
    {
        "query_id": "photo_alone_cannot_identify_compound",
        "query": "Can the app identify a compound from a chromatogram photo alone?",
        "expected_entry_ids": ["kp2-caveat-no-compound-assignment"],
        "required_entry_ids": ["kp2-caveat-no-compound-assignment"],
        "forbidden_entry_ids": [
            "kp2-compound-stub-n-c12-alkane",
            "kp2-compound-stub-n-c24-alkane",
            "kp2-compound-stub-n-c27-alkane",
            "kp2-compound-stub-n-c28-alkane",
        ],
        "category": "semantic_caveat",
        "query_class": "natural_language",
        "safety_critical": True,
    },
)


def read_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(payload, handle, ensure_ascii=False, indent=2)
        handle.write("\n")


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def stable_u64_id(entry_id: str) -> int:
    digest = hashlib.sha256(entry_id.encode("utf-8")).digest()
    value = int.from_bytes(digest[:8], byteorder="big", signed=False)
    return value if value != 0 else 1


def build_entry_id_map(entries: list[dict[str, Any]]) -> dict[str, int]:
    by_numeric_id: dict[int, str] = {}
    entry_to_numeric: dict[str, int] = {}
    for entry in entries:
        entry_id = entry["entryId"]
        numeric_id = stable_u64_id(entry_id)
        existing = by_numeric_id.get(numeric_id)
        if existing is not None and existing != entry_id:
            raise ValueError(f"stable id collision: {existing} and {entry_id} -> {numeric_id}")
        by_numeric_id[numeric_id] = entry_id
        entry_to_numeric[entry_id] = numeric_id
    return entry_to_numeric


def searchable_text(entry: dict[str, Any]) -> str:
    fields = [
        entry.get("canonicalLabel", ""),
        entry.get("shortText", ""),
        entry.get("longText", "") or "",
        entry.get("type", ""),
        " ".join(entry.get("aliases", [])),
        " ".join(entry.get("keywords", [])),
        " ".join(entry.get("tags", [])),
        " ".join(entry.get("policy", {}).get("allowedUse", [])),
        " ".join(entry.get("policy", {}).get("forbiddenUse", [])),
    ]
    return " ".join(part for part in fields if part).strip()


def bounded_documents(pack: dict[str, Any]) -> list[dict[str, Any]]:
    docs: list[dict[str, Any]] = []
    for entry in pack["entries"]:
        docs.append(
            {
                "entry_id": entry["entryId"],
                "type": entry.get("type"),
                "text": searchable_text(entry),
                "source_ref_ids": entry.get("sourceRefIds", []),
                "allowed_use": entry.get("policy", {}).get("allowedUse", []),
                "forbidden_use": entry.get("policy", {}).get("forbiddenUse", []),
            },
        )
    return docs


def tokenize(text: str) -> list[str]:
    cleaned = re.sub(r"[^\w./+-]+", " ", text.lower(), flags=re.UNICODE)
    return [
        token.strip(".,:;()[]")
        for token in re.split(r"\s+", cleaned)
        if len(token.strip(".,:;()[]")) >= 2
    ]


def lexical_search(pack: dict[str, Any], query: str, k: int = 8) -> list[dict[str, Any]]:
    entries = pack["entries"]
    source_refs = {source["sourceId"]: source for source in pack.get("sourceRefs", [])}
    query_terms = set(tokenize(query))
    documents = {entry["entryId"]: tokenize(searchable_text(entry)) for entry in entries}
    document_frequency = {
        term: max(1, sum(1 for terms in documents.values() if term in terms))
        for term in query_terms
    }
    average_length = sum(len(terms) for terms in documents.values()) / max(1, len(documents))
    candidates: list[dict[str, Any]] = []
    for entry in entries:
        terms = documents[entry["entryId"]]
        matched = sorted(term for term in query_terms if term in terms)
        aliases = entry.get("aliases", []) + [entry.get("canonicalLabel", ""), entry["entryId"]]
        exact_alias = normalize(query) in {normalize(alias) for alias in aliases}
        if not matched and not exact_alias:
            continue
        base_score = bm25_score(
            query_terms=matched or list(query_terms),
            document_terms=terms,
            document_frequency=document_frequency,
            document_count=max(1, len(entries)),
            average_length=average_length or 1.0,
        )
        score = base_score + 100.0 if exact_alias else base_score
        candidates.append(
            {
                "entry_id": entry["entryId"],
                "score": score,
                "matched_terms": matched,
                "source_ref_ids": [
                    ref_id for ref_id in entry.get("sourceRefIds", []) if ref_id in source_refs
                ],
            },
        )
    return sorted(candidates, key=lambda result: (-result["score"], result["entry_id"]))[: max(1, k)]


def bm25_score(
    query_terms: list[str],
    document_terms: list[str],
    document_frequency: dict[str, int],
    document_count: int,
    average_length: float,
) -> float:
    k1 = 1.2
    b = 0.75
    length = max(1, len(document_terms))
    term_counts = {term: document_terms.count(term) for term in set(document_terms)}
    score = 0.0
    for term in query_terms:
        tf = float(term_counts.get(term, 0))
        df = float(document_frequency.get(term, 1))
        idf = math.log(1.0 + (document_count - df + 0.5) / (df + 0.5))
        denominator = tf + k1 * (1.0 - b + b * length / average_length)
        score += idf * ((tf * (k1 + 1.0)) / denominator) if denominator else 0.0
    return score


def normalize(text: str) -> str:
    return " ".join(tokenize(text))


def evaluate_results(
    results: list[dict[str, Any]],
    expected_entry_ids: list[str],
    required_entry_ids: list[str] | None = None,
    forbidden_entry_ids: list[str] | None = None,
) -> dict[str, Any]:
    top_ids = [result["entry_id"] for result in results]
    expected_set = set(expected_entry_ids)
    required_set = set(required_entry_ids if required_entry_ids is not None else expected_entry_ids)
    forbidden_set = set(forbidden_entry_ids or [])
    first_hit_rank = next(
        (index + 1 for index, entry_id in enumerate(top_ids) if entry_id in expected_set),
        None,
    )
    required_ranks = {
        entry_id: (top_ids.index(entry_id) + 1 if entry_id in top_ids else None)
        for entry_id in required_set
    }
    forbidden_present = [entry_id for entry_id in top_ids if entry_id in forbidden_set]
    return {
        "top_ids": top_ids,
        "hit": first_hit_rank is not None,
        "first_hit_rank": first_hit_rank,
        "top1_hit": bool(top_ids and top_ids[0] in expected_set),
        "required_entry_ids": sorted(required_set),
        "missing_required_entry_ids": sorted(entry_id for entry_id, rank in required_ranks.items() if rank is None),
        "required_entry_ranks": required_ranks,
        "forbidden_entry_ids": sorted(forbidden_set),
        "forbidden_entry_ids_present": forbidden_present,
        "forbidden_entry_present": bool(forbidden_present),
    }


def now_ms() -> int:
    return int(time.perf_counter() * 1000)
