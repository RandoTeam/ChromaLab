# Image Viewer ROI Editor

## Use when

zoom/pan/drag ROI UI.

## Mandatory research

Your model knowledge may be outdated. Before using this skill, search current sources for: Compose canvas/image gesture docs. Save notes to `docs/research/YYYY-MM-DD_skill_03_image_viewer_roi_editor.md`.

## Inputs

- current task brief;
- relevant contracts;
- existing code paths;
- current evidence/test failures;
- applicable phase requirements.

## Outputs

- ROI editor UI + tests;
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
