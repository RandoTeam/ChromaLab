# Guided ROI Visual Design

Status: Phase 2 visual design contract

## Design Goals

The guided ROI editor should feel like a scientific instrument surface: precise, quiet, high-contrast, and readable over imperfect chromatogram images. It should not look like a generic demo editor.

## Palette

Phase 2 uses existing ChromaLab theme colors and adds a small ROI-specific token set:

| Token | Purpose |
| --- | --- |
| `background` | OLED-safe dark background behind the image viewer. |
| `panelSurface` | Slightly elevated translucent control surface. |
| `graphPanel` | Blue overlay for full graph block. |
| `plotArea` | Amber overlay for coordinate rectangle. |
| `confirmed` | Green state indicator for user-confirmed geometry. |
| `warning` | Yellow state indicator for review-grade geometry. |
| `invalid` | Red state indicator for blocking geometry. |
| `handle` | High-contrast handle fill. |
| `outsideScrim` | Dims content outside graphPanel. |
| `guideLine` | Subtle grid to aid positioning. |

The graphPanel and plotArea colors are intentionally distinct. Color alone is not the only state signal; labels and status chips are always present.

## Layout

- Top app bar: short title and back action.
- Instruction/status band: compact status chips and current-stage guidance.
- Image canvas: full available height, black background, zoom/pan.
- Bottom action bar: reset, confirm, and disabled Phase 3 handoff.

The editor avoids nested decorative cards. The image is the working surface, while controls are full-width bands.

## Overlays

GraphPanel:

- semi-transparent fill;
- strong outline;
- outside scrim to emphasize selected panel.

PlotArea:

- semi-transparent fill;
- strong outline;
- diagonal cross-lines to distinguish it from graphPanel even for color-impaired users.

Handles:

- 48 dp interaction target;
- smaller visible circle inside the target;
- high-contrast border matching active ROI state.

## Typography

- Headings use Material title style.
- Guidance uses body-small text to avoid covering the image.
- Status chips use short labels and avoid long explanatory prose inside the image.

## Accessibility Constraints

- Do not rely on low-alpha thin lines alone.
- Do not hide warnings inside color-only state.
- Keep controls at least 48 dp tall.
- Keep strings localizable; Phase 2 provides RU/EN structure.

## Future Reuse

The same viewer architecture should support later overlays:

- calibration anchor placement;
- trace centerline confirmation;
- peak apex/window overlays;
- rejected artifact overlays.

Phase 2 deliberately keeps these overlays out of the UI until their phases define the contracts.
