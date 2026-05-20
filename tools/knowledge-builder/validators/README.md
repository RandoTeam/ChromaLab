# Validators

Future validators in this folder should check:

- source IDs are unique;
- license metadata is present;
- only bundleable source statuses produce bundled entries;
- entries have allowed and forbidden use policies;
- no production entry contains sample-specific measured RT, height, area, FWHM, S/N, baseline, or Kovats values;
- attribution manifest entries match source definitions.

The runtime Kotlin validator remains the authority for app-side pack validation.
