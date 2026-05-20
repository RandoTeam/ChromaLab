# Phase 9 Model Activation Audit

Date: 2026-05-20

## Device And Package

- Device: `I2407`
- Android: `16` / SDK `36`
- Validation package: `com.chromalab.app.validation`
- Fixture: `white_tiger_ion71`
- ADB action: `com.chromalab.app.RUN_VALIDATION_FIXTURE`

## Model Discovery

| Model | Intended role | Device status | Observed path | Size | Result |
| --- | --- | --- | --- | ---: | --- |
| Gemma-4-E4B LiteRT-LM | FULL_ANALYSIS primary | Not installed in validation package | `files/models/gemma4-e4b/gemma-4-E4B-it.litertlm` | n/a | Not run. |
| Gemma-4-E2B LiteRT-LM | FAST/fallback | Installed | `files/models/gemma4-e2b/gemma-4-E2B-it.litertlm` | `2588147712` bytes | Loaded through LiteRT GPU. |
| GGUF fallback | Compatibility/benchmark only | Not used | n/a | n/a | Not run. |

Checksum was not computed during Phase 9 because the installed E2B model is 2.4 GB and the audit needed to preserve device runtime. A future model-import screen should compute and cache checksum during import.

## Activation Evidence

Final model-enabled validation run:

- Run id: `white_tiger_ion71_20260520_192400`
- Selected model: `gemma4-e2b`
- Executed model: `gemma4-e2b`
- Backend: LiteRT GPU
- Load result: `loaded`
- Runtime diagnostic status: `AVAILABLE`
- Deterministic fallback: preserved

Earlier model-enabled run `white_tiger_ion71_20260520_191649` loaded E2B successfully but exposed a blocker: loading/running VLM before deterministic calibration caused a calibration regression. Phase 9 fixes this by deferring validation-fixture model activation until deterministic X/Y calibration is complete.

## Remaining Model Gap

E4B FULL_ANALYSIS was not installed on the attached validation package. Phase 9 therefore validates:

- deterministic no-model path;
- E2B model-enabled fallback path.

E4B remains a required follow-up validation item once the E4B LiteRT-LM file is installed.
