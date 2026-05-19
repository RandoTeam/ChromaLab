# Regression Policy

After Phase 2 and every following phase:

1. Run all tests for current phase.
2. Run all previous phase tests.
3. Run core fixture suite.
4. Run validator suite.
5. Run at least one real-device validation if runtime Android changed.

## Regression matrix must track

- graph count;
- graphPanel status;
- plotArea status;
- calibration status;
- trace status;
- peak status;
- report gate;
- evidence export status;
- runtime duration;
- VLM timeout/calls.

No phase closes if old behavior regresses without explicit reviewed acceptance.
