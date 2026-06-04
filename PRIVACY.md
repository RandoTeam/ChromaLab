# Privacy

ChromaLab is designed as an offline-first chromatogram analysis project. The
default product direction is local processing: users should be able to analyze
photos, screenshots, local models, and reports without uploading data to a cloud
service by default.

This document describes the intended privacy model for the public repository. It
does not claim that every future build or integration has been externally
audited.

## Data The App May Handle

ChromaLab may handle:

- chromatogram photos and screenshots;
- normalized/cropped graph images;
- graphPanel, plotArea, axis, tick, calibration, trace, and peak overlays;
- runtime evidence packages;
- validator JSON and Markdown;
- final report JSON, HTML, and Markdown;
- diagnostic bundles;
- logcat or app logs;
- local model files;
- local Knowledge Pack files;
- local database records and app preferences.

## Local Processing

The product goal is:

- no default cloud upload for chromatogram images;
- no default cloud upload for reports or evidence packages;
- local OCR/model/runtime execution where supported;
- explicit user action for export or sharing.

API credits or cloud tools used during development do not change the app's
offline-first product direction.

## Exports And Sharing

Users and developers should treat exported artifacts carefully.

User-facing exports may include:

- report HTML;
- report Markdown;
- report contract JSON.

Technical or diagnostic exports may include:

- source images or crops;
- overlays;
- rejected candidates;
- calibration residuals;
- logs;
- local paths;
- model/runtime diagnostics.

Diagnostic artifacts should not be shared publicly unless they have been
reviewed for private images, personal data, local paths, and sensitive
scientific context.

## Evidence Packages And Logs

Evidence packages are useful for validation, but they can contain sensitive
information. They may include user images, graph crops, file names, runtime
metadata, and local device details.

Logs can contain device information, model names, paths, error messages, and
stage timings. Do not post raw logs publicly without review.

## Model Files

Model files are local assets and may have their own licenses, terms, and storage
requirements. Do not commit downloaded model files, local model paths, signing
files, or private credentials to the repository.

## Developer Guidance

When adding privacy-sensitive code:

- keep user reports separate from diagnostic bundles;
- do not leak raw logs into user-facing reports;
- avoid exposing local absolute paths in public artifacts;
- preserve explicit export/share boundaries;
- redact sensitive data before adding fixtures or artifacts to Git;
- keep `local.properties`, keystores, and model files out of source control.

## Current Status

ChromaLab is research-alpha software under active validation. Privacy and
security policies are now documented, but the project still needs continued
review of Android storage, exports, diagnostics, model handling, and native
runtime behavior.
