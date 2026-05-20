# Phase 9 Model Installation And Discovery

Date: 2026-05-20

## Model Store

Validation builds discover local models from app-private storage:

```text
/data/user/0/com.chromalab.app.validation/files/models/<model-id>/
```

Expected files:

```text
files/models/gemma4-e4b/gemma-4-E4B-it.litertlm
files/models/gemma4-e2b/gemma-4-E2B-it.litertlm
```

Do not commit large model files to the repository.

## Safe ADB Install Pattern

Use `/data/local/tmp` as the temporary transfer location, then copy into app-private storage with `run-as`:

```powershell
adb push C:\path\to\gemma-4-E4B-it.litertlm /data/local/tmp/gemma-4-E4B-it.litertlm
adb shell run-as com.chromalab.app.validation mkdir -p files/models/gemma4-e4b
adb shell run-as com.chromalab.app.validation cp /data/local/tmp/gemma-4-E4B-it.litertlm files/models/gemma4-e4b/gemma-4-E4B-it.litertlm
adb shell run-as com.chromalab.app.validation ls -l files/models/gemma4-e4b
```

For E2B:

```powershell
adb push C:\path\to\gemma-4-E2B-it.litertlm /data/local/tmp/gemma-4-E2B-it.litertlm
adb shell run-as com.chromalab.app.validation mkdir -p files/models/gemma4-e2b
adb shell run-as com.chromalab.app.validation cp /data/local/tmp/gemma-4-E2B-it.litertlm files/models/gemma4-e2b/gemma-4-E2B-it.litertlm
adb shell run-as com.chromalab.app.validation ls -l files/models/gemma4-e2b
```

## Validation Commands

Deterministic no-model baseline:

```powershell
adb shell am start -S -n com.chromalab.app.validation/com.chromalab.app.MainActivity -a com.chromalab.app.RUN_VALIDATION_FIXTURE --es fixture white_tiger_ion71 --es modelMode deterministic
```

Model-enabled fallback run:

```powershell
adb shell am start -S -n com.chromalab.app.validation/com.chromalab.app.MainActivity -a com.chromalab.app.RUN_VALIDATION_FIXTURE --es fixture white_tiger_ion71 --es modelMode model_enabled
```

`model_enabled` in validation fixture mode means:

1. run deterministic graphPanel, plotArea, tick, and X/Y calibration first;
2. activate Gemma after deterministic calibration;
3. export model diagnostics with the report evidence;
4. never let model output replace deterministic numeric evidence.
