# Contributing To ChromaLab

Thank you for your interest in ChromaLab. This project is research-alpha
software: it is public and actively developed, but it is not production-ready
autonomous chromatogram analysis software.

Contributions are welcome when they preserve the project's core rule:
scientific measurements must come from deterministic, auditable evidence.

## Project Status

Current status:

- Android/Kotlin Multiplatform app: active research alpha;
- deterministic calculation engine: present and separated from AI/model stages;
- local model runtime: LiteRT/Gemma and GGUF paths under validation;
- real Android fixture validation: active, with review-only and blocked cases;
- Rust CV work: prototype and bridge direction, not yet full production core.

Do not present a contribution as production-ready unless the evidence gates,
validators, and fixture results support that claim.

## Setup

Recommended local requirements:

- JDK 17;
- Android SDK 35;
- Android NDK `27.2.12479018`;
- CMake `3.22.1`;
- Android Studio or a compatible Gradle setup.

Common checks:

```bash
./gradlew :composeApp:compileKotlinDesktop --no-daemon
./gradlew :composeApp:assembleAndroidMain --no-daemon
./gradlew :androidApp:assembleDebug --no-daemon
./gradlew :composeApp:desktopTest --no-daemon
```

On Windows PowerShell:

```powershell
.\gradlew.bat :composeApp:compileKotlinDesktop --no-daemon
.\gradlew.bat :composeApp:assembleAndroidMain --no-daemon
.\gradlew.bat :androidApp:assembleDebug --no-daemon
.\gradlew.bat :composeApp:desktopTest --no-daemon
```

## Contribution Rules

### Do Not Overclaim

Do not claim:

- production-ready autonomous analysis;
- 99 percent success rate;
- certified laboratory, medical, forensic, legal, or regulatory use;
- guaranteed calibration on arbitrary photos;
- guaranteed compound identification;
- AI-generated chromatographic numeric metrics.

Keep blocked and review-only status visible.

### Do Not Make VLM Or LLM Output Numeric Authority

Local AI may assist:

- OCR and local crop text reading;
- title, ion, channel, and warning classification;
- Knowledge Pack grounded explanations;
- overlay review comments;
- user-facing report language.

Local AI must not create or override:

- graph pixel coordinates;
- calibration coefficients;
- retention time;
- height;
- area;
- FWHM;
- S/N;
- baseline;
- Kovats/retention index;
- peak boundaries;
- final compound identity.

### Preserve Calculation Boundaries

Do not modify `CalculationEngine` or chromatographic math unless the change is a
focused, proven, reviewed calculation fix with tests.

Vision/model/report changes must not silently change deterministic peak metrics.

### Validation Fixture Rules

When adding or changing validation fixtures:

- include metadata;
- do not hardcode fixture-specific coordinates;
- preserve expected graph count unless Product/QA/Scientific review changes it;
- export runtime evidence packages;
- keep validator JSON/Markdown;
- keep graphPanel, plotArea, calibration, trace, and peak evidence when stages
  run;
- classify failures with precise failure classes and subreasons.

### Report Gate Rules

Report gates are evidence gates:

- `RELEASE_READY` requires complete deterministic evidence and no critical
  blocker;
- `REVIEW_ONLY` means useful evidence exists but human review is required;
- `DIAGNOSTIC_ONLY` is for debugging or learning;
- `BLOCKED` means a critical stage failed or required evidence is missing.

Do not turn `BLOCKED` into `REVIEW_ONLY` just to make a result look better.

## Opening Issues

Use issue templates when available:

- bug report for app/runtime defects;
- validation fixture issue for fixture-specific analysis failures;
- feature request for scoped product improvements;
- security report link for vulnerability reporting guidance.

For bugs, include:

- app version or commit;
- device and Android version when relevant;
- mode: deterministic, E2B, E4B, GGUF, or desktop;
- expected behavior;
- actual behavior;
- logs or artifacts only after removing private images, local paths, and
  sensitive data.

## Pull Requests

Before opening a pull request:

1. Keep the change focused.
2. Update docs when behavior, model support, validation, reports, or public
   claims change.
3. Run the most relevant checks.
4. Do not include local model files, APKs, keystores, `local.properties`, raw
   diagnostic bundles, or private chromatogram images.
5. Describe validation honestly.

## Security And Privacy

Read:

- [SECURITY.md](SECURITY.md)
- [PRIVACY.md](PRIVACY.md)

Do not post private chromatogram images, raw logs, evidence packages, signing
configuration, or local paths publicly.
