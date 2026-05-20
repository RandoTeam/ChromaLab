# Phase 7 Knowledge-Grounded Reporting

Date: 2026-05-20

## Allowed Uses

Knowledge Pack and VLM can:

- explain terminology;
- classify local text crops;
- explain warnings;
- cite caveats;
- support report wording with `used_entry_ids`;
- distinguish title/ion/channel text from peak annotations.

## Forbidden Uses

Knowledge Pack and VLM must not create or override:

- RT;
- height;
- area;
- FWHM;
- S/N;
- baseline;
- Kovats/RI;
- calibration coefficients;
- peak integration metrics;
- compound identities without explicit evidence.

## Phase 7 Implementation

- Report renderers label semantic-only compound names as candidate hypotheses.
- Validator warns on knowledge/model-only compound names without identity evidence.
- Existing Phase 6 Knowledge Pack tests enforce `used_entry_ids`, unsupported-claim REVIEW/REJECTED, and no numeric metric creation.

## Phase 8 Handoff

Add first-class report model records for retrieved Knowledge Pack cards used in each final explanation.
