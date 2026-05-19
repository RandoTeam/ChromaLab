# No Hardcode Policy

Forbidden:

- hardcoded image dimensions;
- hardcoded run IDs;
- hardcoded filenames;
- hardcoded coordinates;
- special-case branch for a single fixture;
- “if this sample” logic;
- changing expected test output to match broken behavior.

Allowed:

- normalized geometry ratios;
- IoU/containment thresholds;
- axis/tick completeness rules;
- calibration residual thresholds;
- trace quality metrics;
- text classification rules;
- general graph digitization invariants.

Every real image is a regression fixture representing a class, not a hardcode target.
