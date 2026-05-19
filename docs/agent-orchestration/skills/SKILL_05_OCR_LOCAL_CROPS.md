# OCR Local Crops

## Use when

ML Kit/local OCR around labels/ticks.

## Mandatory research

Your model knowledge may be outdated. Before using this skill, search current sources for: ML Kit text recognition docs. Save notes to `docs/research/YYYY-MM-DD_skill_05_ocr_local_crops.md`.

## Inputs

- current task brief;
- relevant contracts;
- existing code paths;
- current evidence/test failures;
- applicable phase requirements.

## Outputs

- OCR crop evidence;
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
