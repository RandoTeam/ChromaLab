# ChromaLab Phase 0 Baseline Audit - 2026-05-19

Status: desktop baseline captured; mobile runtime baseline still open.

## Research Gate

The implementation owner must treat built-in model knowledge as stale. Before this
baseline is used to close any later phase, current methods must be checked again.
For this slice, the current web/documentation check covered:

- OpenCV Hough line transforms for deterministic line/frame/axis detection:
  https://docs.opencv.org/4.x/d9/db0/tutorial_hough_lines.html
- Google CameraX / ML Kit analyzer integration for Android image analysis:
  https://developer.android.com/media/camera/camerax/mlkitanalyzer
- Google ML Kit Text Recognition v2 for on-device OCR:
  https://developers.google.com/ml-kit/vision/text-recognition/v2/android
- scikit-image skeletonization as a reference for trace topology extraction:
  https://scikit-image.org/docs/0.25.x/auto_examples/edges/plot_skeleton.html
- SciPy `find_peaks` as a reference for local maxima, prominence, width, and noisy
  signal caveats:
  https://docs.scipy.org/doc/scipy/reference/generated/scipy.signal.find_peaks.html
- pyOpenMS `PeakIntegrator` as a reference for chromatographic integration and
  baseline options:
  https://pyopenms.readthedocs.io/en/latest/apidocs/_autosummary/pyopenms/pyopenms.PeakIntegrator.html
- WebPlotDigitizer / PlotDigitizer / Plot Extractor as UX and architecture
  references for graph digitization:
  https://automeris.io/docs/digitize/
  https://plotdigitizer.com/docs
  https://plotextractor.com/
- MOCCA chromatography processing as a reference for automated peak picking,
  baseline correction, deconvolution, and compound tracking:
  https://mocca.readthedocs.io/en/latest/

Conclusion for phase 0: the current direction remains correct. Numeric chromatogram
metrics must be deterministic and evidence-backed. VLMs can assist with text and
review, but cannot own pixel geometry or peak metrics.

## Commands Run

```powershell
.\gradlew.bat :composeApp:desktopTest --rerun-tasks --tests "com.chromalab.feature.processing.fixtures.ChromatogramBenchFixtureTest"
```

Result:

```text
ChromatogramBenchFixtureTest[desktop]: 11 tests, 0 skipped, 0 failures, 0 errors.
Reported test time: 67.745 s.
```

Persistent desktop evidence packages were generated locally under:

```text
artifacts/phase0_baseline_20260519/
```

Generation command:

```powershell
.\gradlew.bat :composeApp:run --args="--offline-analysis --image <fixture> --out artifacts\phase0_baseline_20260519\<fixture-id> --source <fixture-id> --expected-graphs <n>"
```

Those generated artifacts are local review output and are not committed.

## Fixture Baseline Summary

| Fixture | Expected graphs | Detected graphs | Ready for calculation | Blocking stage | Baseline decision |
| --- | ---: | ---: | --- | --- | --- |
| `bench_01_mz71_screenshot_page` | 2 | 2 | No | `axis_calibration` | Graph split works; calibration remains blocked without runtime OCR/manual anchors. |
| `bench_02_mz92_belyi_tigr` | 1 | 1 | No | `crop_quality` | Dominant early peak/top crop risk remains explicit. |
| `bench_03_small_tic_export` | 1 | 1 | No | `axis_calibration` | Low-res labels and ticks remain calibration/OCR evidence work; fixture recovery stays test-only. |
| `bench_04_stacked_xic_resolution` | 4 | 4 | No | `axis_calibration` | Multi-panel split works; sparse panels remain review-grade after calibration is available. |
| `bench_05_tic_plus_ions` | 4 | 4 | No | `axis_calibration` | TIC + ion panels split correctly; stacked sparse traces require review-grade handling. |
| `bench_06_photo_two_graphs_page` | 2 | 2 | No | `axis_detect` | Graph count works; graph 1 still lacks robust Y-axis detection. |
| `bench_07_rotated_page_photo` | 1 | 1 | No | `axis_calibration` | Rotation correction works; calibration remains blocked. |
| `bench_08_mz71_duplicate_candidate` | 1 | 1 | No | `axis_calibration` | Duplicate/six-report issue is guarded on desktop; calibration remains blocked. |

## Per-Graph Evidence Snapshot

| Fixture | Graph | GraphPanel | PlotArea | Axis/origin | Ticks | Calibration | Curve points | Coverage | Key warnings |
| --- | ---: | --- | --- | --- | --- | --- | ---: | ---: | --- |
| `bench_01` | 1 | `x=91,y=278,w=815,h=475` | `x=185,y=356,w=624,h=340` | Yes | `x=9,y=2` | Not ready | 596 | 0.955 | signal extends above axis; manual calibration required; high artifact risk. |
| `bench_01` | 2 | `x=24,y=704,w=885,h=473` | `x=24,y=704,w=873,h=413` | Yes | `x=13,y=1` | Not ready | 844 | 0.944 | insufficient Y ticks; manual calibration required. |
| `bench_02` | 1 | `x=10,y=480,w=558,h=364` | `x=90,y=480,w=437,h=357` | Yes | `x=26,y=2` | Not ready | 410 | 0.478 | possible clipped top peak; manual calibration required. |
| `bench_03` | 1 | `x=0,y=0,w=381,h=132` | `x=13,y=2,w=367,h=99` | Yes | `x=1,y=1` | Not ready | 252 | 0.529 | full image crop; insufficient ticks; manual calibration required. |
| `bench_04` | 1 | `x=22,y=0,w=512,h=238` | `x=25,y=0,w=505,h=228` | Yes | `x=1,y=1` | Not ready | 485 | 0.899 | edge-touch crop; insufficient ticks; artifact guard. |
| `bench_04` | 2 | `x=22,y=258,w=512,h=250` | `x=25,y=268,w=503,h=232` | Yes | `x=2,y=1` | Not ready | 466 | 0.835 | insufficient Y ticks; manual calibration required. |
| `bench_04` | 3 | `x=22,y=542,w=512,h=250` | `x=25,y=552,w=503,h=230` | Yes | `x=2,y=1` | Not ready | 152 | 0.159 | sparse trace accepted for review; manual calibration required. |
| `bench_04` | 4 | `x=22,y=822,w=512,h=248` | `x=25,y=832,w=503,h=230` | Yes | `x=2,y=1` | Not ready | 25 | 0.068 | localized sparse review required; manual calibration required. |
| `bench_05` | 1 | `x=79,y=20,w=604,h=350` | `x=83,y=26,w=594,h=339` | Yes | `x=10,y=1` | Not ready | 571 | 0.961 | insufficient Y ticks; manual calibration required. |
| `bench_05` | 2 | `x=81,y=354,w=602,h=161` | `x=86,y=443,w=590,h=66` | Yes | `x=1,y=1` | Not ready | 232 | 0.146 | sparse trace accepted for review; manual calibration required. |
| `bench_05` | 3 | `x=81,y=499,w=602,h=154` | `x=86,y=581,w=590,h=66` | Yes | `x=1,y=1` | Not ready | 238 | 0.144 | sparse trace accepted for review; manual calibration required. |
| `bench_05` | 4 | `x=81,y=637,w=602,h=154` | `x=86,y=719,w=590,h=66` | Yes | `x=1,y=1` | Not ready | 97 | 0.054 | sparse trace accepted for review; manual calibration required. |
| `bench_06` | 1 | `x=137,y=242,w=736,h=414` | `x=146,y=242,w=720,h=368` | No | `x=29,y=1` | Not ready | 642 | 0.732 | Y-axis not detected; signal extends above axis; top-band text risk. |
| `bench_06` | 2 | `x=131,y=649,w=785,h=511` | `x=140,y=649,w=757,h=498` | Yes | `x=1,y=1` | Not ready | 685 | 0.863 | insufficient ticks; artifact guard; manual calibration required. |
| `bench_07` | 1 | `x=24,y=239,w=849,h=508` | `x=29,y=239,w=835,h=462` | Yes | `x=32,y=14` | Not ready | 697 | 0.660 | rotation corrected; manual calibration required. |
| `bench_08` | 1 | `x=14,y=492,w=548,h=290` | `x=51,y=492,w=511,h=281` | Yes | `x=33,y=1` | Not ready | 452 | 0.468 | duplicate count guarded; insufficient Y ticks; manual calibration required. |

## Real Android Baseline References

The latest committed real-device docs remain part of the baseline:

- `docs/CHROMATOGRAM_REAL_ANDROID_E332_ROI_MULTIPLICITY.md`
  - Old six-report bug root cause: overlapping/nested ROI candidates were emitted
    as physical graphs.
  - Expected current behavior: one resolved physical graph plus duplicate/nesting
    evidence.
- `docs/CHROMATOGRAM_REAL_ANDROID_9A0674D6_GRAPH_PANEL_AUDIT.md`
  - Old selected graphPanel was a right-side subregion.
  - Expected current behavior: full white chart panel should win; if diagnostic-only
    remains, the next blocker should be calibration/tick localization.
- `docs/RUNTIME_EVIDENCE_REAL_DEVICE_VALIDATION_CHECKLIST.md`
  - Runtime success must not be claimed until a real Android package validates with
    available referenced artifacts.

Phase 0 is not fully closed for mobile until a fresh Android run exports:

- RuntimeEvidencePackage JSON;
- validator JSON;
- validator Markdown;
- graphPanel/plotArea/axis/tick/trace overlays;
- final report contract JSON;
- relevant logcat and screen after analysis.

## Next Implementation Slice

Proceed to phase 1 only after accepting that this baseline is the current regression
contract. The first phase 1 target should be source provenance and normalization
parity, not peak tuning.

