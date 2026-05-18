# Export Workflow

Status: active implementation contract.

ChromaLab export actions must create real files, not UI-only confirmations.

## Current Android Behavior

- Final calculation report exports save text files to `Downloads/ChromaLab` through
  Android scoped storage / `MediaStore`.
- Share actions write a temporary file under the app cache and expose it through
  `com.chromalab.app.fileprovider`.
- Supported report files:
  - `peaks_<run>.csv`;
  - `calculation_<run>.json`;
  - `chromatogram_report_<run>.html`;
  - `chromatogram_report_ui_contract_<run>.json`;
  - `chromatogram_report_<run>.md`.
- Each final-report export row has a save action and a share action. Share opens the
  Android system share sheet for apps that accept the file MIME type.

## Format Rules

- Peak CSV uses comma-separated columns with `.` decimal separators independent of the
  device locale.
- CSV text cells containing commas, quotes, or newlines are quoted and quotes are
  doubled.
- JSON and HTML exports are generated from the structured report/export contracts.
- HTML is the current PDF-ready artifact: it is self-contained and printable by the
  receiving browser/viewer. A native PDF file generator is not yet implemented.

## Remaining Follow-Up

- Add a native PDF export action when the report layout is locked.
- Add automated Android UI coverage for save/share clicks when test harness support is
  available.
