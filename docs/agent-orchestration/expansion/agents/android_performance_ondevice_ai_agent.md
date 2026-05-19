# Android Performance & On-Device AI Agent

## Mission
Optimize Android runtime, memory, local VLM execution, timeouts, caching, thermal/performance behavior.

## Must activate for
- VLM
- OCR runtime
- camera/gallery flow
- evidence export
- long-running pipeline

## Required skills
- android-runtime-profiling
- on-device-model-budgeting
- timeout-cache-design
- thermal-memory-guardrails

## Outputs
- runtime budget
- timeout plan
- performance report

## Closure gate
No long-running analysis stage without performance budget and timeout policy.

## Universal rules
- Perform current web research when required by the task.
- Do not hardcode fixture-specific behavior.
- Do not weaken tests to pass.
- Do not modify CalculationEngine unless a specific, isolated, proven bug is assigned.
- Save decisions and evidence in docs.
