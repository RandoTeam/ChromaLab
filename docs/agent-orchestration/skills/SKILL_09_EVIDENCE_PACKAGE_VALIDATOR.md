# Evidence Package Validator

## Use when

runtime evidence and gates.

## Mandatory research

Your model knowledge may be outdated. Before using this skill, search current sources for: QA/golden artifact practices. Save notes to `docs/research/YYYY-MM-DD_skill_09_evidence_package_validator.md`.

## Inputs

- current task brief;
- relevant contracts;
- existing code paths;
- current evidence/test failures;
- applicable phase requirements.

## Outputs

- validator + diagnostics;
- tests;
- documentation updates;
- evidence/validator updates if runtime behavior changes.

## Must

- Keep changes general, not fixture-specific.
- Add or update tests.
- Record assumptions and risks.
- Update acceptance status.

## Must not

- Hardcode one graph/image.
- Hide invalid geometry/calibration.
- Use VLM as numeric truth.
- Close task without regression.
