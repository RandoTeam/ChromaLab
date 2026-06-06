# Autonomous Analyzer Layer Owner Board

Date: 2026-06-06

Status: `R1_LAYER_OWNER_BOARD_UPDATED`

Scope: inventory only. This board identifies active owners and replacement
targets. It does not change runtime behavior.

## Owner States

- `ACTIVE_IMPLEMENTATION`: current production or validation code path.
- `EXPERIMENTAL_SHADOW`: prototype or bridge path used for parity/research.
- `HISTORICAL_REFERENCE`: useful history but not the current owner.
- `ACTIVE_TEST`: current test or fixture coverage.
- `GAP`: missing or insufficient coverage.

## Stage Owner Table

| Stage | Current active owner | Tests / fixtures | Current state | Replacement direction |
|---:|---|---|---|---|
| 0 Input/provenance | `feature/capture`, `processing/pipeline`, `feature/validation/AutonomousValidationFixtureContracts.kt`, Android `FileImportBridge`, validation runner/exporter | `AutonomousValidationFixtureAssetTest`, `AutonomousValidationFixtureContractTest`; Android assets under `composeApp/src/androidMain/assets/validation/` | `ACTIVE_IMPLEMENTATION`; provenance exists but source-of-truth is spread across capture, validation, and runtime evidence. | Consolidate input provenance contract before changing image preparation. |
| 1 Image preparation | `processing/normalize`, `processing/preprocess`, `processing/quality`, `processing/document`, `processing/perspective`; Android adapters for ML Kit scanner, normalizer, preprocessor, warper | `QualityCalculatorTest`; indirect coverage through bench/Android fixtures | `ACTIVE_IMPLEMENTATION`; likely heavy and variant-rich, not yet a clean Rust owner. | Replace with Rust/Kotlin contract after parity inventory; keep old path only as shadow during comparison. |
| 2 Graph discovery | `processing/graph`, `processing/geometry/ScreenshotEmbeddedChartDetector.kt`, `GraphMultiplicityResolver.kt`, `AutoSweepEngine.kt` | `AutoSweepGraphSelectionTest`, `GraphRegionOrderingTest`, `GraphMultiplicityResolverTest`, Android fixture suite | `ACTIVE_IMPLEMENTATION`; still a product blocker for multi-panel and 0-graph cases. | High-priority replacement target after R0: graph candidate generation and multiplicity resolution. |
| 3 PlotArea/layout semantics | `GraphPlotAreaDetector.kt`, `GraphLayoutClassifier.kt`, `GeometryPipelineRunner.kt` | `GraphLayoutClassifierTest`, `GeometryPipelineMultiplicityTest`, Phase 9J truth docs | `ACTIVE_IMPLEMENTATION`; layout classifier exists but report propagation and graph-count semantics remain weak. | Pair with Stage 2 in a broad graph/layout replacement phase. |
| 4 Axis/grid/OCR labels | `AxisDetector`, `AxisTickGeometryDetector`, `TickLocalizationPipeline`, `TickOcrCropRegions`, `TickOcrMatcher`, `AxisOcrReader` | `TickLocalizationPipelineTest`, `TickOcrMatcherTest`, `TickOcrCropRegionsTest`, `ChartPromptsAxisTickTest` | `ACTIVE_IMPLEMENTATION`; tick/label evidence remains brittle for bench_01 and related classes. | Replace only after graph/plotArea ownership is stable; do not let OCR create geometry. |
| 5 Calibration | `processing/calibration`, `AxisCalibrationFitter`, `AxisScaleResolver`, `CalibrationStrategyEnsemble` | `AxisCalibrationFitterTest`, `AxisScaleResolverTest`, `CalibrationStrategyEnsembleTest`, White Tiger regression shield docs | `ACTIVE_IMPLEMENTATION`; ensemble fixed White Tiger regression but broader blockers remain. | Preserve ensemble; improve only with residual/anchor evidence and no `CalculationEngine` changes. |
| 6 Trace extraction | `processing/curve`, `FragmentedTraceReconstruction`, `SkeletonGraphTrunkPath`, `processing/signal` | `CurveMaskPreparerPlotAreaTest`, guided trace tests, bench fixtures | `ACTIVE_IMPLEMENTATION`; sparse/dense/stacked trace evidence is active but depends on upstream geometry. | Replace with Rust trace mask/centerline only after Stage 2-5 contracts are stable. |
| 7 Peak detection / integration | `feature/calculation/core/CalculationEngine.kt`, `feature/calculation/algorithm/*`, `processing/peaks`, `reports/PeakEvidenceMapper.kt` | `CalculationCoreTest`, synthetic fixture tests, report/peak evidence tests | `ACTIVE_IMPLEMENTATION_PROTECTED`; math is not the current replacement target. | Do not modify unless an isolated math bug is proven after upstream evidence is stable. |
| 8 Model / Knowledge assistance | `processing/inference`, `processing/model`, `feature/knowledge`, Android LiteRT/GGUF/model manager files | model policy tests, Knowledge Pack tests, E2B acceptance matrix | `ACTIVE_IMPLEMENTATION`; E2B is advisory baseline; Knowledge retrieval is lexical and safe. | TurboVec may replace retrieval ranking only after benchmark gates; model remains semantic only. |
| 9 Runtime evidence validation | `processing/debug/RuntimeEvidencePackage.kt`, `RuntimeEvidencePackageValidator.kt`, `feature/validation` terminal/export files | `RuntimeEvidencePackageValidatorTest`, structured diagnostic tests, Phase 9J truth audit | `ACTIVE_IMPLEMENTATION`; this is the current truth gate. | Keep strict; expand completeness checks when replacing upstream layers. |
| 10 Report generation | `feature/reports`, `processing/report`, `calculation/export` | report renderer, validator, provenance, stored metadata tests | `ACTIVE_IMPLEMENTATION`; reports are evidence-gated but can still be visually clearer. | Do not redesign before analyzer truth improves; keep report gates honest. |
| 11 Export/privacy | `processing/export`, `processing/storage`, Android `FileSharer`, validation artifact exporter, debug exporter | `ExportEngineTest`, runtime evidence validator, public privacy/security docs | `ACTIVE_IMPLEMENTATION`; artifacts complete in recent truth audit, but `artifacts/` is ignored. | Preserve separation between user reports and diagnostic artifacts. |
| 12 Acceptance | `docs/PHASE9J_*`, `CHROMALAB_VALIDATION_SUMMARY.md`, regression dataset/matrix | Phase 9J truth audit, fixture assets, bench tests | `ACTIVE_SOURCE_OF_TRUTH`; Phase 9 remains not accepted. | Every replacement phase must update product/scientific/QA acceptance status. |

## Active Fixture Assets

| Status | Path | Role |
|---|---|---|
| `FIXTURE_ASSET_ACTIVE` | `composeApp/src/androidMain/assets/validation/white_tiger_ion71_fixture.jpg` | Stable witness; must not regress from REVIEW_ONLY. |
| `FIXTURE_ASSET_ACTIVE` | `composeApp/src/androidMain/assets/validation/bench_01_mz71_screenshot_page.jpg` | Current blocked Y calibration/tick evidence class. |
| `FIXTURE_ASSET_ACTIVE` | `composeApp/src/androidMain/assets/validation/bench_02_mz92_belyi_tigr.jpg` | Graph/layout and single-graph semantics witness. |
| `FIXTURE_ASSET_ACTIVE` | `composeApp/src/androidMain/assets/validation/bench_03_small_tic_export.jpg` | Low-res REVIEW witness. |
| `FIXTURE_ASSET_ACTIVE` | `composeApp/src/androidMain/assets/validation/bench_04_stacked_xic_resolution.png` | Stacked/multi-panel layout witness. |
| `FIXTURE_ASSET_ACTIVE` | `composeApp/src/androidMain/assets/validation/bench_05_tic_plus_ions.png` | TIC+ions blocked layout/calibration witness. |
| `FIXTURE_ASSET_ACTIVE` | `composeApp/src/androidMain/assets/validation/bench_06_photo_two_graphs_page.jpg` | Two-graph page propagation witness. |
| `FIXTURE_ASSET_ACTIVE` | `composeApp/src/androidMain/assets/validation/bench_07_rotated_page_photo.jpg` | Rotated page REVIEW witness. |
| `FIXTURE_ASSET_ACTIVE` | `composeApp/src/androidMain/assets/validation/rust_axis_element/` | Rust parity corpus and graph-level axis-element contracts. |

## Replacement Priority

| Priority | Layer | Reason |
|---:|---|---|
| 1 | Stage 2-3 graph discovery/layout | Most downstream failures depend on correct graph count, panel grouping, and plotArea ownership. |
| 2 | Stage 1 image preparation | Preparation quality affects every graph/layout candidate and can be cleanly evaluated before calibration. |
| 3 | Stage 4-5 axis/scale/calibration | Still blocked on real fixtures, but should not be repaired before graph/plotArea ownership is stable. |
| 4 | Stage 6 trace extraction | Needs stable plotArea/calibration first. |
| 5 | Stage 10 Knowledge retrieval / TurboVec | Valuable for semantics and developer memory, but not the direct blocker for current analyzer failures. |

## Shadow / Legacy Paths To Control

| Path / component | Current classification | Replacement risk |
|---|---|---|
| `processing/bench/OfflineAnalysisRunner.kt` | `EXPERIMENTAL_SHADOW` for app runtime, active for desktop/bench tooling | Useful for corpus work, but must not bypass runtime evidence gates if promoted. |
| `processing/document/DocumentDetector*` and `processing/perspective/PerspectiveWarper*` | `EXPERIMENTAL_SHADOW` / fallback in autonomous flow | Page rectification can change graph geometry; promote only with explicit contract and visual parity. |
| `processing/geometry/CvQuadrilateralCandidateDetector*` | `EXPERIMENTAL_SHADOW` | Do not treat as trusted page homography until Android evidence proves it. |
| Guided/manual screens under `processing/guided`, `processing/crop`, `processing/curve`, calibration screens | fallback / assisted tools | Keep as review/repair paths, not autonomous production authority. |
| Rust bridge diagnostics under `processing/debug/Rust*` and `processing/rust/RustCvBridge.android.kt` | `EXPERIMENTAL_SHADOW` | Rust can replace a layer only after contract/parity/promotion gates. |
| `RuntimePeakRecoveryEvaluator` | runtime review evidence, not peak authority | Can add review-grade recovered candidates only with local signal evidence; cannot replace `CalculationEngine`. |
| `tools/benchmark/*` | active benchmark tooling with prototype scripts mixed in | Use for replacement measurement; do not let prototype scripts become runtime logic silently. |

## Test And Fixture Gaps From R0

| Gap | Why it blocks replacement |
|---|---|
| Numeric truth is not fully locked for all fixtures. | Current fixtures are stress/evidence contracts, not guaranteed final scientific answers. Replacement gates must compare stage evidence, not just final numbers. |
| bench_01 and bench_05 remain hard calibration/layout blockers. | New graph/layout/image-prep work must show whether it improves or precisely classifies these fixtures. |
| White Tiger, bench_03, and bench_07 are regression witnesses. | Any new layer must not lower them from current REVIEW behavior. |
| E2B `UNKNOWN_FAILURE` / semantic unavailability appears in summaries. | Model-enabled mode must remain advisory and cannot hide deterministic failures. |
| Generated Phase 9 artifacts are ignored. | If they are needed for parity, copy/archive them before machine cleanup or regenerate on Android. |

## Completed Broad Phases

Completed:

```text
R1 - Graph/Layout And Image Preparation Replacement Contract
R2 - Stage 1-3 Shadow Parity Harness
R3 - Stage 1 Image Preparation Candidate
```

R1 is now documented in:

- `docs/R1_GRAPH_LAYOUT_IMAGE_PREP_REPLACEMENT_CONTRACT.md`;
- `docs/R1_GRAPH_LAYOUT_IMAGE_PREP_PARITY_PLAN.md`;
- `docs/R1_GRAPH_LAYOUT_IMAGE_PREP_CLOSEOUT.md`.

R1 closed as contract/planning only. No runtime behavior, validators,
chromatographic math, report gates, or graph-count metadata changed.

R2 is documented in:

- `docs/R2_STAGE123_SHADOW_PARITY_HARNESS_CLOSEOUT.md`;
- `benchmark/examples/stage123_shadow_parity/`;
- `benchmark/reports/stage123_shadow_parity/summary.md`.

R2 closed as benchmark/shadow measurement only. It did not change Android
runtime behavior, validators, chromatographic math, report gates, graph-count
metadata, model policy, or `CalculationEngine`.

R3 is documented in:

- `docs/R3_IMAGE_PREPARATION_CANDIDATE_CLOSEOUT.md`;
- `benchmark/examples/r3_image_preparation_candidate/`;
- `benchmark/reports/r3_image_preparation_candidate/summary.md`.

R3 closed as PC/shadow Stage 1 candidate only. It did not change Android runtime
behavior, validators, chromatographic math, report gates, graph-count metadata,
model policy, or `CalculationEngine`.

## Next Broad Phase

Recommended:

```text
R4 - Rust Stage 1 Image Preparation Parity Bridge
```

R4 should port or bridge the R3 Stage 1 image-preparation candidate into
Rust-owned primitives, compare Rust output against the R3 PC records, and keep
production runtime unchanged until parity and promotion gates pass.
