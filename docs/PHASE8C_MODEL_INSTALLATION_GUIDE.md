# Phase 8C Model Installation Guide

## Policy

ChromaLab does not commit large Gemma LiteRT-LM or GGUF model files to the repository. Validation builds must discover models from device-local app storage and report missing assets explicitly.

## Expected Device Layout

`ModelManager` scans the package-private directory:

```text
/data/user/0/<package>/files/models/<model-id>/<model-file>
```

For the side-by-side validation package:

```text
/data/user/0/com.chromalab.app.validation/files/models/
```

Expected model ids should match registry ids for the packaged metadata. If a model is imported through the app UI, the app creates the correct directory and preference entries.

## Preferred Installation

1. Install the validation build:

```powershell
.\gradlew.bat :androidApp:assembleValidation
adb install -r androidApp\build\outputs\apk\validation\androidApp-validation.apk
```

2. Open ChromaLab validation package and import/select the chromatogram VLM from the model manager.
3. Run the fixture:

```powershell
adb shell am start -S -n com.chromalab.app.validation/com.chromalab.app.MainActivity -a com.chromalab.app.RUN_VALIDATION_FIXTURE --es fixture white_tiger_ion71
```

## Debug Placement Option

For debuggable validation builds, model files may be staged with `run-as` if the device allows it:

```powershell
adb push C:\path\to\model.litertlm /data/local/tmp/model.litertlm
adb shell run-as com.chromalab.app.validation mkdir -p files/models/<model-id>
adb shell run-as com.chromalab.app.validation cp /data/local/tmp/model.litertlm files/models/<model-id>/<model-file-name>
```

Then launch fixture validation. If the model is not selected explicitly, `activateForPipeline()` scans downloaded models and chooses a chromatogram-eligible model by priority.

## Diagnostics

When no model is available, the validation output must include:

- `modelAvailabilityDiagnostics` in the runtime evidence package;
- `model_availability_diagnostics_<run_id>.json`;
- validator Markdown `Model Availability` section;
- log summary entries.

Interpretation:

- `NOT_CONFIGURED`: no selected/executed local chromatogram VLM was visible to the app.
- `UNAVAILABLE`: a selected model existed but did not load.
- `LOAD_FAILED`: an executed/selected model failed to become ready.
- `MODEL_ASSET_MISSING`: model metadata points to missing files.
- `VLM_MODEL_UNAVAILABLE`: VLM semantic layer unavailable; deterministic CV fallback must still run.
