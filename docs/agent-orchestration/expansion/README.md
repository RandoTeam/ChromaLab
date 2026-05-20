# ChromaLab Agent & Skill Expansion Pack

This expansion adds specialist agents, expanded skills, protocols, prompts, templates, and activation rules.

Product modes:
- AUTONOMOUS_PRODUCTION: primary target, automatic evidence may become release-ready only when all gates pass.
- AUTO_DIAGNOSTIC: automatic incomplete/review attempt, diagnostic by default, every terminal state exports evidence.
- ASSISTED_REVIEW: user reviews or corrects only failed/low-confidence autonomous stages.
- MANUAL_ADVANCED: fallback for difficult images, user can manually define geometry, calibration, trace, and peak decisions.
- GUIDED_PRODUCTION: deprecated compatibility alias for earlier guided docs.

VLM/LLM boundary:
Allowed: local crop OCR, title/ion/channel/axis-label reading, text classification, overlay judging, and warning explanation.
Forbidden: exact numeric geometry for calculation, RT as final measurement, height, area, FWHM, S/N, baseline, Kovats/retention index, final peak count, or chromatographic quantitative metrics.
