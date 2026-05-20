# Trace Extraction Evidence Research - 2026-05-20

## Scope

This note supports Phase 4 realignment: autonomous trace extraction should produce trace quality evidence first, and the overlay UI should review or repair weak evidence.

## Source Matrix

| Source | Quality | Relevance | Decision affected | Not adopted |
| --- | --- | --- | --- | --- |
| [scikit-image skeletonize example](https://scikit-image.org/docs/0.25.x/auto_examples/edges/plot_skeleton.html) | Official maintained documentation | Skeletonization and medial-axis output are evidence-generating operations but require quality checks. | Keep trace metrics such as point count, gaps, branch points, and coverage. | Do not port scikit-image code into KMP. |
| [OpenCV ximgproc thinning docs](https://docs.opencv.org/4.x/javadoc/org/opencv/ximgproc/Ximgproc.html) | Official maintained documentation | Binary thinning can produce skeletons from masks; branch/noise artifacts must be reviewed. | Trace extraction quality gates should detect frame/text/grid contamination and branchy skeletons. | Do not assume Android OpenCV ximgproc is present on every device. |
| [OpenCV Hough line docs](https://docs.opencv.org/4.x/d9/db0/tutorial_hough_lines.html) | Official maintained documentation | Axes/grid/frame detection supports suppressing non-trace lines before trace evidence. | Trace gates should reject frame/grid-dominated candidates. | Do not count detected guide/candidate lines as peaks or trace. |
| [Skan skeleton analysis](https://skeleton-analysis.org/stable) | Maintained scientific Python package docs | Demonstrates branch/endpoint/path quantification for skeleton evidence. | ChromaLab trace evidence should track branch and endpoint-like metrics when available. | Do not add Python dependency to Android app. |

## Decisions

- A valid autonomous trace must have points inside plotArea, adequate coverage, confidence, and required overlay/centerline artifacts.
- Sparse, fragmented, contaminated, or artifact-dominated traces become `REVIEW` or `INVALID`.
- Phase 4 trace overlay UI remains useful as evidence review, not as the default production path.
- Manual trace drawing remains out of scope for this slice.

## Rejected Sources / Claims

- Largest connected component is not sufficient for trace selection.
- Visual overlay alone is not proof; the report needs metrics and artifacts.
- Candidate vertical lines or peak markers are not validated trace evidence.
