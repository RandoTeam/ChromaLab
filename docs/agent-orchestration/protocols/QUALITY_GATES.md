# Quality Gates

## Phase close gate

Фаза закрывается только если:

1. `compileKotlinDesktop` passed.
2. `assembleAndroidMain` passed.
3. Full `desktopTest` passed или documented known failures не затрагиваются phase.
4. Phase-specific tests passed.
5. `ChromatogramBenchFixtureTest` passed или documented reviewed failure.
6. RuntimeEvidencePackageValidator tests passed.
7. Research notes saved.
8. Docs updated.
9. Evidence package behavior updated if runtime changed.
10. Regression over previous phases passed.

## Release-quality report gate

Release report allowed only if:

- graphPanel valid/user-confirmed;
- plotArea valid/user-confirmed;
- X calibration VALID/User-confirmed;
- Y calibration VALID/User-confirmed;
- trace VALID/User-confirmed;
- peak overlay confirmed/reviewed;
- report contract complete;
- evidence package exported.

Otherwise: `DIAGNOSTIC_ONLY` or `REVIEW`.
