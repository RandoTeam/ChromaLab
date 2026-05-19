# Git Branch / Review Protocol

## Use when

multi-agent branches, merges, reviews.

## Mandatory research

Your model knowledge may be outdated. Before using this skill, search current sources for: git best practices. Save notes to `docs/research/YYYY-MM-DD_skill_15_git_branch_review_protocol.md`.

## Inputs

- current task brief;
- relevant contracts;
- existing code paths;
- current evidence/test failures;
- applicable phase requirements.

## Outputs

- merge plan + review checklist;
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
