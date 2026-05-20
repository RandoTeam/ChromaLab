# Builder Tests

Future builder tests should cover:

- source metadata validation;
- rejected-source report generation;
- duplicate alias detection;
- attribution manifest generation;
- deterministic JSON output;
- failure when a source is marked `NEEDS_REVIEW` but contributes bundled production entries.

Runtime Knowledge Pack tests live under `composeApp/src/commonTest/kotlin/com/chromalab/feature/knowledge`.
