#!/usr/bin/env python3
"""Validate ChromaLab benchmark schemas and example documents."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

try:
    from jsonschema import Draft202012Validator
except ModuleNotFoundError as exc:  # pragma: no cover - user-facing dependency guard
    raise SystemExit(
        "Missing dependency: jsonschema. Install with "
        "`python -m pip install -r tools/benchmark/requirements.txt`."
    ) from exc


SCHEMA_BY_EXAMPLE_NAME = {
    "truth.json": "truth.schema.json",
    "prediction.json": "prediction.schema.json",
    "metrics.json": "metrics.schema.json",
    "evidence-package.json": "evidence-package.schema.json",
    "report-claims.json": "report-claims.schema.json",
    "stage123-parity-record.json": "stage123-parity-record.schema.json",
    "stage1234-parity-record.json": "stage1234-parity-record.schema.json",
    "stage12345-parity-record.json": "stage12345-parity-record.schema.json",
    "stage123456-parity-record.json": "stage123456-parity-record.schema.json",
}


def load_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def validate_schema(schema_path: Path) -> None:
    schema = load_json(schema_path)
    Draft202012Validator.check_schema(schema)


def validate_document(document_path: Path, schema_path: Path) -> list[str]:
    schema = load_json(schema_path)
    document = load_json(document_path)
    validator = Draft202012Validator(schema)
    errors = sorted(validator.iter_errors(document), key=lambda item: item.path)
    messages: list[str] = []
    for error in errors:
        location = ".".join(str(part) for part in error.absolute_path) or "<root>"
        messages.append(f"{document_path}: {location}: {error.message}")
    return messages


def collect_example_documents(examples_dir: Path) -> list[Path]:
    documents: list[Path] = []
    for name in SCHEMA_BY_EXAMPLE_NAME:
        documents.extend(sorted(examples_dir.rglob(name)))
    return sorted(documents)


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path.cwd(), help="Repository root.")
    parser.add_argument(
        "--schemas-dir",
        type=Path,
        default=None,
        help="Schema directory. Defaults to <root>/benchmark/schemas.",
    )
    parser.add_argument(
        "--examples-dir",
        type=Path,
        default=None,
        help="Example directory. Defaults to <root>/benchmark/examples.",
    )
    args = parser.parse_args(argv)

    root = args.root.resolve()
    schemas_dir = (args.schemas_dir or root / "benchmark" / "schemas").resolve()
    examples_dir = (args.examples_dir or root / "benchmark" / "examples").resolve()

    if not schemas_dir.exists():
        print(f"Schema directory not found: {schemas_dir}", file=sys.stderr)
        return 2
    if not examples_dir.exists():
        print(f"Examples directory not found: {examples_dir}", file=sys.stderr)
        return 2

    schema_paths = sorted(schemas_dir.glob("*.schema.json"))
    if not schema_paths:
        print(f"No schemas found in {schemas_dir}", file=sys.stderr)
        return 2

    for schema_path in schema_paths:
        validate_schema(schema_path)

    example_documents = collect_example_documents(examples_dir)
    if not example_documents:
        print(f"No benchmark example documents found in {examples_dir}", file=sys.stderr)
        return 2

    all_errors: list[str] = []
    validated_count = 0
    for document_path in example_documents:
        schema_name = SCHEMA_BY_EXAMPLE_NAME[document_path.name]
        schema_path = schemas_dir / schema_name
        errors = validate_document(document_path, schema_path)
        all_errors.extend(errors)
        validated_count += 1

    if all_errors:
        print("Benchmark schema validation failed:", file=sys.stderr)
        for message in all_errors:
            print(f"- {message}", file=sys.stderr)
        return 1

    print(
        "Benchmark schema validation passed: "
        f"{len(schema_paths)} schemas, {validated_count} example documents."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
