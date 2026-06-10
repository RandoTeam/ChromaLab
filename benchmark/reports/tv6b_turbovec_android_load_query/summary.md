# TV-6B On-Device TurboVec Load And Query Summary

Date: 2026-06-10

Status: `TV6B_ON_DEVICE_SHELL_LOAD_QUERY_PASSED_RUNTIME_NOT_PROMOTED`

## Result

TurboVec `0.8.1` passed a real on-device shell probe on an `arm64-v8a` Android
device.

The probe:

- created an `IdMapIndex`;
- wrote `/data/local/tmp/chromalab_tv6b_probe.tvim`;
- loaded it back;
- executed top-k search;
- returned stable valid ids across 3 runs.

## Device

| Field | Value |
|---|---|
| Model | `I2407` |
| ABI | `arm64-v8a` |
| SDK | `36` |

## Runs

| Run | Status | Top ids | Index bytes | Build ms | Load ms | Query ms | RSS before KB | RSS after KB |
|---:|---|---|---:|---:|---:|---:|---:|---:|
| 1 | PASS | `[1002, 1001, 1003]` | 706 | 166 | 0 | 159 | 3916 | 4948 |
| 2 | PASS | `[1002, 1001, 1003]` | 706 | 163 | 0 | 158 | 3692 | 4980 |
| 3 | PASS | `[1002, 1001, 1003]` | 706 | 183 | 0 | 158 | 3688 | 5036 |

## Decision

Do not promote TurboVec into product runtime yet.

Next: `TV-7 - App-Private TurboVec Provider Prototype`.

