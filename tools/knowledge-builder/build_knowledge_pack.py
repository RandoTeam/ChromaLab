#!/usr/bin/env python3
"""Build scaffold for ChromaLab Knowledge Pack artifacts.

This Phase 6C builder validates reviewed source metadata and emits an
attribution/build manifest. It intentionally does not download external
databases or scrape remote sites.
"""

from __future__ import annotations

import argparse
import json
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable


@dataclass(frozen=True)
class SourceDefinition:
    source_id: str
    label: str
    url: str
    license_text: str
    license_status: str
    trust_tier: str
    can_bundle: bool
    can_transform: bool
    api_lookup_only: bool
    attribution_required: bool
    decision: str
    notes: str


def parse_bool(value: str) -> bool:
    lowered = value.strip().lower()
    if lowered in {"true", "yes"}:
        return True
    if lowered in {"false", "no"}:
        return False
    raise ValueError(f"Unsupported boolean value: {value!r}")


def parse_sources(path: Path) -> list[SourceDefinition]:
    # Conservative parser for this repository's simple source register shape.
    sources: list[dict[str, str]] = []
    current: dict[str, str] | None = None
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.rstrip()
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or stripped == "sources:":
            continue
        if stripped.startswith("- id:"):
            if current:
                sources.append(current)
            current = {"id": stripped.split(":", 1)[1].strip()}
            continue
        if current is None:
            continue
        if ":" in stripped:
            key, value = stripped.split(":", 1)
            current[key.strip()] = value.strip()
    if current:
        sources.append(current)

    parsed: list[SourceDefinition] = []
    for item in sources:
        parsed.append(
            SourceDefinition(
                source_id=item.get("id", ""),
                label=item.get("label", ""),
                url=item.get("url", ""),
                license_text=item.get("license", ""),
                license_status=item.get("license_status", ""),
                trust_tier=item.get("trust_tier", ""),
                can_bundle=parse_bool(item.get("can_bundle", "false")),
                can_transform=parse_bool(item.get("can_transform", "false")),
                api_lookup_only=parse_bool(item.get("api_lookup_only", "false")),
                attribution_required=parse_bool(item.get("attribution_required", "false")),
                decision=item.get("decision", ""),
                notes=item.get("notes", ""),
            )
        )
    return parsed


def validate_sources(sources: Iterable[SourceDefinition]) -> tuple[list[str], list[SourceDefinition]]:
    issues: list[str] = []
    rejected: list[SourceDefinition] = []
    seen: set[str] = set()
    allowed_bundle_status = {"INTERNAL_CURATED", "OPEN_VERIFIED", "ATTRIBUTION_REQUIRED"}
    for source in sources:
        if not source.source_id:
            issues.append("source.id missing")
        if source.source_id in seen:
            issues.append(f"duplicate source id {source.source_id}")
        seen.add(source.source_id)
        if not source.label:
            issues.append(f"{source.source_id}: label missing")
        if not source.license_status:
            issues.append(f"{source.source_id}: license_status missing")
        if source.can_bundle and source.license_status not in allowed_bundle_status:
            issues.append(f"{source.source_id}: can_bundle true with non-bundleable status {source.license_status}")
        if source.decision.startswith("rejected") or source.license_status in {"REJECTED", "PROPRIETARY_FORBIDDEN", "NEEDS_REVIEW"}:
            rejected.append(source)
    return issues, rejected


def write_manifest(path: Path, sources: list[SourceDefinition], rejected: list[SourceDefinition], entry_count: int | None) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    manifest = {
        "packId": "chromalab-knowledge",
        "version": "chromalab-knowledge-v2",
        "schemaVersion": "chromalab-knowledge-pack-1.0",
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "builderVersion": "phase6c-scaffold-1",
        "sourceIds": [source.source_id for source in sources],
        "entryCount": entry_count,
        "rejectedSourceIds": [source.source_id for source in rejected],
    }
    path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def write_rejected_report(path: Path, rejected: list[SourceDefinition]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    lines = ["# Rejected Or Review-Only Sources", ""]
    if not rejected:
        lines.append("No rejected sources.")
    for source in rejected:
        lines.append(f"- `{source.source_id}`: `{source.license_status}` / `{source.decision}`. {source.notes}")
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--sources", type=Path, default=Path("tools/knowledge-builder/sources.yaml"))
    parser.add_argument("--manifest", type=Path, default=Path("tools/knowledge-builder/output/knowledge_build_manifest_v2.json"))
    parser.add_argument("--rejected", type=Path, default=Path("tools/knowledge-builder/output/rejected_sources_v2.md"))
    parser.add_argument("--pack", type=Path, default=None, help="Optional generated pack JSON used only to record entry count.")
    args = parser.parse_args()

    sources = parse_sources(args.sources)
    issues, rejected = validate_sources(sources)
    if issues:
        for issue in issues:
            print(f"ERROR: {issue}")
        return 1
    entry_count = None
    if args.pack and args.pack.is_file():
        entry_count = len(json.loads(args.pack.read_text(encoding="utf-8")).get("entries", []))
    write_manifest(args.manifest, sources, rejected, entry_count)
    write_rejected_report(args.rejected, rejected)
    print(f"Validated {len(sources)} source definitions; {len(rejected)} require review or are rejected for bundling.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
