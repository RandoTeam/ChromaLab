use crate::Axis;
use serde::{Deserialize, Serialize};
use std::error::Error;
use std::fmt::{Display, Formatter};

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct RuntimeOcrAnchorBridgeSuiteInput {
    pub fixture_id: String,
    pub graphs: Vec<RuntimeOcrAnchorGraphInput>,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct RuntimeOcrAnchorGraphInput {
    pub graph_id: String,
    pub graph_index: u32,
    pub anchors: Vec<RuntimeOcrAnchorInput>,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct RuntimeOcrAnchorInput {
    pub anchor_id: String,
    pub axis: Axis,
    pub ocr_text: String,
    pub ocr_value: Option<f64>,
    pub pixel_coordinate: Option<f64>,
    pub source_crop_ref: String,
    pub source_crop_path: Option<String>,
    pub confidence: Option<f64>,
    pub text_role: String,
    pub geometry_source: String,
    pub numeric_source: String,
    pub candidate_status: String,
    pub residual_px: Option<f64>,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct RuntimeOcrAnchorBridgeSuiteReport {
    pub fixture_id: String,
    pub graph_count: usize,
    pub source_anchor_count: usize,
    pub accepted_anchor_count: usize,
    pub rejected_anchor_count: usize,
    pub missing_source_crop_file_count: usize,
    pub graphs: Vec<RuntimeOcrAnchorGraphReport>,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct RuntimeOcrAnchorGraphReport {
    pub graph_id: String,
    pub graph_index: u32,
    pub source_anchor_count: usize,
    pub accepted_anchor_count: usize,
    pub rejected_anchor_count: usize,
    pub missing_source_crop_file_count: usize,
    pub accepted_anchors: Vec<RuntimeOcrAcceptedAnchor>,
    pub rejected_anchors: Vec<RuntimeOcrRejectedAnchor>,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct RuntimeOcrAcceptedAnchor {
    pub runtime_row_id: String,
    pub anchor_id: String,
    pub graph_id: String,
    pub axis: Axis,
    pub ocr_text: String,
    pub ocr_value: f64,
    pub pixel_coordinate: f64,
    pub source_crop_ref: String,
    pub source_crop_path: Option<String>,
    pub confidence: Option<f64>,
    pub geometry_source: String,
    pub numeric_source: String,
    pub residual_px: Option<f64>,
    pub crop_file_available: bool,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct RuntimeOcrRejectedAnchor {
    pub anchor_id: String,
    pub graph_id: String,
    pub axis: Axis,
    pub ocr_text: String,
    pub source_crop_ref: String,
    pub rejection_reason: RuntimeOcrAnchorRejectionReason,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum RuntimeOcrAnchorRejectionReason {
    UpstreamRejectedAnchor,
    MissingOcrValue,
    MissingPixelGeometry,
    MissingSourceCropReference,
    ForbiddenTextRole,
    VlmGeometrySource,
    VlmNumericSource,
    InvalidConfidence,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum RuntimeOcrAnchorBridgeError {
    InvalidJson(String),
}

impl Display for RuntimeOcrAnchorBridgeError {
    fn fmt(&self, formatter: &mut Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::InvalidJson(message) => {
                write!(
                    formatter,
                    "invalid runtime OCR anchor bridge JSON: {message}"
                )
            }
        }
    }
}

impl Error for RuntimeOcrAnchorBridgeError {}

pub fn bridge_runtime_ocr_anchor_suite_json(
    json: &str,
) -> Result<RuntimeOcrAnchorBridgeSuiteReport, RuntimeOcrAnchorBridgeError> {
    let input: RuntimeOcrAnchorBridgeSuiteInput = serde_json::from_str(json)
        .map_err(|error| RuntimeOcrAnchorBridgeError::InvalidJson(error.to_string()))?;
    Ok(bridge_runtime_ocr_anchor_suite(input))
}

#[must_use]
pub fn bridge_runtime_ocr_anchor_suite(
    input: RuntimeOcrAnchorBridgeSuiteInput,
) -> RuntimeOcrAnchorBridgeSuiteReport {
    let mut source_anchor_count = 0;
    let mut accepted_anchor_count = 0;
    let mut rejected_anchor_count = 0;
    let mut missing_source_crop_file_count = 0;
    let mut graphs = Vec::with_capacity(input.graphs.len());

    for graph in input.graphs {
        let graph_report = bridge_graph(graph);
        source_anchor_count += graph_report.source_anchor_count;
        accepted_anchor_count += graph_report.accepted_anchor_count;
        rejected_anchor_count += graph_report.rejected_anchor_count;
        missing_source_crop_file_count += graph_report.missing_source_crop_file_count;
        graphs.push(graph_report);
    }

    RuntimeOcrAnchorBridgeSuiteReport {
        fixture_id: input.fixture_id,
        graph_count: graphs.len(),
        source_anchor_count,
        accepted_anchor_count,
        rejected_anchor_count,
        missing_source_crop_file_count,
        graphs,
    }
}

fn bridge_graph(graph: RuntimeOcrAnchorGraphInput) -> RuntimeOcrAnchorGraphReport {
    let mut accepted_anchors = Vec::new();
    let mut rejected_anchors = Vec::new();
    let source_anchor_count = graph.anchors.len();

    for anchor in graph.anchors {
        match validate_anchor(&anchor) {
            Ok(()) => {
                let crop_file_available = anchor
                    .source_crop_path
                    .as_ref()
                    .is_some_and(|path| !path.trim().is_empty());
                accepted_anchors.push(RuntimeOcrAcceptedAnchor {
                    runtime_row_id: format!("{}::{}", graph.graph_id, anchor.anchor_id),
                    anchor_id: anchor.anchor_id,
                    graph_id: graph.graph_id.clone(),
                    axis: anchor.axis,
                    ocr_text: anchor.ocr_text,
                    ocr_value: anchor.ocr_value.unwrap_or_default(),
                    pixel_coordinate: anchor.pixel_coordinate.unwrap_or_default(),
                    source_crop_ref: anchor.source_crop_ref,
                    source_crop_path: anchor.source_crop_path,
                    confidence: anchor.confidence,
                    geometry_source: anchor.geometry_source,
                    numeric_source: anchor.numeric_source,
                    residual_px: anchor.residual_px,
                    crop_file_available,
                });
            }
            Err(rejection_reason) => {
                rejected_anchors.push(RuntimeOcrRejectedAnchor {
                    anchor_id: anchor.anchor_id,
                    graph_id: graph.graph_id.clone(),
                    axis: anchor.axis,
                    ocr_text: anchor.ocr_text,
                    source_crop_ref: anchor.source_crop_ref,
                    rejection_reason,
                });
            }
        }
    }

    let missing_source_crop_file_count = accepted_anchors
        .iter()
        .filter(|anchor| !anchor.crop_file_available)
        .count();

    RuntimeOcrAnchorGraphReport {
        graph_id: graph.graph_id,
        graph_index: graph.graph_index,
        source_anchor_count,
        accepted_anchor_count: accepted_anchors.len(),
        rejected_anchor_count: rejected_anchors.len(),
        missing_source_crop_file_count,
        accepted_anchors,
        rejected_anchors,
    }
}

fn validate_anchor(anchor: &RuntimeOcrAnchorInput) -> Result<(), RuntimeOcrAnchorRejectionReason> {
    if anchor.candidate_status != "ACCEPTED" {
        return Err(RuntimeOcrAnchorRejectionReason::UpstreamRejectedAnchor);
    }
    if anchor.ocr_value.is_none() {
        return Err(RuntimeOcrAnchorRejectionReason::MissingOcrValue);
    }
    if anchor.pixel_coordinate.is_none() {
        return Err(RuntimeOcrAnchorRejectionReason::MissingPixelGeometry);
    }
    if anchor.source_crop_ref.trim().is_empty() {
        return Err(RuntimeOcrAnchorRejectionReason::MissingSourceCropReference);
    }
    if anchor.text_role != "tick_label" {
        return Err(RuntimeOcrAnchorRejectionReason::ForbiddenTextRole);
    }
    if anchor.geometry_source == "VLM_PIXEL" {
        return Err(RuntimeOcrAnchorRejectionReason::VlmGeometrySource);
    }
    if anchor.numeric_source == "VLM_NUMERIC" {
        return Err(RuntimeOcrAnchorRejectionReason::VlmNumericSource);
    }
    if let Some(confidence) = anchor.confidence {
        if !(0.0..=1.0).contains(&confidence) {
            return Err(RuntimeOcrAnchorRejectionReason::InvalidConfidence);
        }
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    fn accepted_anchor() -> RuntimeOcrAnchorInput {
        RuntimeOcrAnchorInput {
            anchor_id: "a1".to_string(),
            axis: Axis::X,
            ocr_text: "35.00".to_string(),
            ocr_value: Some(35.0),
            pixel_coordinate: Some(290.02),
            source_crop_ref: "crop-ref-1".to_string(),
            source_crop_path: Some("crop.png".to_string()),
            confidence: Some(0.99),
            text_role: "tick_label".to_string(),
            geometry_source: "OCR_LABEL_BOX_PIXEL_PROJECTION".to_string(),
            numeric_source: "LOCAL_OCR_TEXT".to_string(),
            candidate_status: "ACCEPTED".to_string(),
            residual_px: Some(3.1),
        }
    }

    #[test]
    fn accepts_tick_label_with_pixel_geometry() {
        let report = bridge_runtime_ocr_anchor_suite(RuntimeOcrAnchorBridgeSuiteInput {
            fixture_id: "fixture".to_string(),
            graphs: vec![RuntimeOcrAnchorGraphInput {
                graph_id: "graph_1".to_string(),
                graph_index: 1,
                anchors: vec![accepted_anchor()],
            }],
        });

        assert_eq!(report.accepted_anchor_count, 1);
        assert_eq!(report.rejected_anchor_count, 0);
        assert_eq!(report.graphs[0].accepted_anchors[0].ocr_value, 35.0);
    }

    #[test]
    fn rejects_non_tick_text_roles() {
        let mut anchor = accepted_anchor();
        anchor.text_role = "ion_or_mz_metadata".to_string();

        let report = bridge_runtime_ocr_anchor_suite(RuntimeOcrAnchorBridgeSuiteInput {
            fixture_id: "fixture".to_string(),
            graphs: vec![RuntimeOcrAnchorGraphInput {
                graph_id: "graph_1".to_string(),
                graph_index: 1,
                anchors: vec![anchor],
            }],
        });

        assert_eq!(report.accepted_anchor_count, 0);
        assert_eq!(
            report.graphs[0].rejected_anchors[0].rejection_reason,
            RuntimeOcrAnchorRejectionReason::ForbiddenTextRole
        );
    }

    #[test]
    fn rejects_vlm_geometry_source() {
        let mut anchor = accepted_anchor();
        anchor.geometry_source = "VLM_PIXEL".to_string();

        let report = bridge_runtime_ocr_anchor_suite(RuntimeOcrAnchorBridgeSuiteInput {
            fixture_id: "fixture".to_string(),
            graphs: vec![RuntimeOcrAnchorGraphInput {
                graph_id: "graph_1".to_string(),
                graph_index: 1,
                anchors: vec![anchor],
            }],
        });

        assert_eq!(report.accepted_anchor_count, 0);
        assert_eq!(
            report.graphs[0].rejected_anchors[0].rejection_reason,
            RuntimeOcrAnchorRejectionReason::VlmGeometrySource
        );
    }

    #[test]
    fn records_missing_crop_file_without_rejecting_crop_reference() {
        let mut anchor = accepted_anchor();
        anchor.source_crop_path = None;

        let report = bridge_runtime_ocr_anchor_suite(RuntimeOcrAnchorBridgeSuiteInput {
            fixture_id: "fixture".to_string(),
            graphs: vec![RuntimeOcrAnchorGraphInput {
                graph_id: "graph_1".to_string(),
                graph_index: 1,
                anchors: vec![anchor],
            }],
        });

        assert_eq!(report.accepted_anchor_count, 1);
        assert_eq!(report.missing_source_crop_file_count, 1);
        assert!(!report.graphs[0].accepted_anchors[0].crop_file_available);
    }
}
