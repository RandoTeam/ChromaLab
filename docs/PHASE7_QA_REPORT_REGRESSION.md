# Phase 7 QA Report Regression

Date: 2026-05-20

## Added Coverage

- UI contract v2 keeps raw Markdown out of primary UI and exposes gate evidence.
- HTML renderer shows report gate evidence, export privacy classes, peak evidence, and release-ready status when evidence package is explicit.
- Markdown renderer shows report gate evidence and diagnostic-only state when evidence package is missing.
- Validator warns on semantic-only compound names.
- Validator errors on calculated Kovats/RI without reference retention times.

## Required Regression Commands

- `git diff --check`
- `.\gradlew.bat :composeApp:compileKotlinDesktop`
- `.\gradlew.bat :composeApp:assembleAndroidMain`
- `.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.reports.*"`
- `.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.debug.RuntimeEvidencePackageValidatorTest"`
- `.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.fixtures.ChromatogramBenchFixtureTest"`
- `.\gradlew.bat :composeApp:desktopTest --rerun-tasks`

## Acceptance

Phase 7 can close when all required validation commands pass and no CalculationEngine/math files are changed.
