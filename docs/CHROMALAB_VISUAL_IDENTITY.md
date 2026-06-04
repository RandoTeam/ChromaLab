# ChromaLab Visual Identity

Status: RP_2_VISUAL_IDENTITY_READY

This document defines the first public visual identity pass for ChromaLab. It is intended for the public GitHub repository, OpenAI subsidy review materials, future release notes, and later Android launcher icon refinement.

## Design Brief

ChromaLab should look like a serious scientific analysis tool, not a generic AI demo.

The visual identity must communicate:

- chromatogram analysis;
- calibrated scientific evidence;
- local/offline AI assistance;
- student and research usefulness;
- honest uncertainty and review gates;
- mobile-first but technically deep engineering.

The identity must not imply:

- certified medical diagnostics;
- guaranteed compound identification;
- black-box AI authority;
- production-ready validation before the evidence supports it.

## Core Symbol

The primary symbol combines four ideas:

1. A calibrated axis grid.
2. A chromatogram trace with peak structure.
3. Evidence nodes that mark accepted/inspectable analysis points.
4. A compact local-AI chip mark for on-device model assistance.

This keeps the brand specific to ChromaLab instead of making it look like a general chat app or a generic chemistry logo.

## Brand Assets

| Asset | Path | Use |
|---|---|---|
| Primary icon | `assets/brand/chromalab-icon.svg` | App identity, small repository visuals, future launcher icon source. |
| Logo lockup | `assets/brand/chromalab-logo-lockup.svg` | Documentation headers, presentations, subsidy materials. |
| README hero | `assets/brand/chromalab-hero.svg` | Root README visual introduction. |
| Social preview source | `assets/brand/chromalab-social-preview.svg` | Source for GitHub social preview export. |

The current assets are SVG sources. PNG exports can be generated later for GitHub social preview and Android launcher density buckets after the final direction is approved.

## Color System

| Token | Hex | Role |
|---|---:|---|
| Deep navy | `#07111F` | Primary background, scientific workspace tone. |
| Ink blue | `#0D1B2A` | Icon surface, high-contrast panels. |
| Lab blue | `#10233D` | Secondary gradient tone. |
| Signal teal | `#23D3A6` | Accepted evidence, trace confidence, local AI support. |
| Cyan trace | `#62E0FF` | Signal extraction and graph highlights. |
| Amber peak | `#F7C948` | Peak review, warning emphasis, important markers. |
| Pale axis | `#D7E7F7` | Axis/grid lines and primary dark-background text. |
| Muted slate | `#9FB6CB` | Secondary text on dark backgrounds. |

The palette deliberately avoids a one-note purple/blue AI gradient. The teal/cyan/amber accents map to evidence, trace, and peak review rather than decoration.

## Typography Direction

Recommended public typography:

- Primary UI/document font: Inter, Segoe UI, or a similar modern sans-serif.
- Tone: precise, readable, scientific, not playful.
- Headings should be strong but not oversized.
- Avoid decorative fonts, handwritten chemistry styling, or futuristic sci-fi type.

## Usage Rules

Use the icon when:

- the surface needs a compact identity mark;
- the product is being represented as ChromaLab;
- the context is scientific analysis, validation, local AI, or reports.

Use the logo lockup when:

- there is enough horizontal space;
- a document, deck, or README section needs brand recognition;
- the word ChromaLab should be explicit.

Use the hero or social preview when:

- the repository or presentation needs a first-impression visual;
- the visual should explain the product category before the reader reaches the text.

Do not:

- use the mark as a medical diagnostic badge;
- pair it with unsupported "100 percent accurate" claims;
- use AI-brain imagery as the main symbol;
- use compound-identification language without evidence;
- present review-only results as final scientific proof.

## README Integration

The root README now references `assets/brand/chromalab-hero.svg` as its first public visual.

This gives public reviewers an immediate visual signal: ChromaLab is about chromatogram evidence, local AI assistance, and scientific reporting.

## Android Launcher Icon Policy

Do not replace Android launcher icons in this phase.

Reason:

- Android launcher assets require density-specific PNGs and adaptive-icon foreground/background checks.
- The current SVG direction should be approved first.
- The next implementation step should export launcher-safe PNGs and verify them on Android before committing app icon replacement.

Recommended future launcher workflow:

1. Export `chromalab-icon.svg` to high-resolution PNG.
2. Create adaptive foreground/background layers.
3. Generate mdpi, hdpi, xhdpi, xxhdpi, and xxxhdpi launcher assets.
4. Check icon legibility at small sizes.
5. Build Android debug APK.
6. Inspect launcher icon on device/emulator.

## GitHub Social Preview Policy

`assets/brand/chromalab-social-preview.svg` is the source design for a 1280 x 640 GitHub social preview.

Before uploading it to GitHub repository settings:

1. Export to PNG.
2. Inspect text at 1280 x 640 and 640 x 320.
3. Confirm that no unsupported production claim appears in the image.
4. Upload the PNG through GitHub repository social preview settings.

## Acceptance Status

RP-2 is complete when:

- the visual direction exists as reusable assets;
- README includes the hero visual;
- the brand document defines color, symbol, usage, and safety rules;
- Android launcher replacement is explicitly deferred until raster export and device QA.
