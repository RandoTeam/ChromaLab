# Report Gate & Provenance

## Use when

release vs diagnostic reports.

## Mandatory research

Your model knowledge may be outdated. Before using this skill, search current sources for: scientific report provenance. Save notes to `docs/research/YYYY-MM-DD_skill_12_report_gate_provenance.md`.

## Inputs

- current task brief;
- relevant contracts;
- existing code paths;
- current evidence/test failures;
- applicable phase requirements.

## Outputs

- report contract gates;
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
