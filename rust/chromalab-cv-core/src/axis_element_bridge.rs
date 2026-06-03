use crate::{
    AxisLabelCropPlan, ImageGeometry, LabelBand, LabelBandKind, Rect, plan_axis_label_crops,
};
use serde::{Deserialize, Serialize};
use std::error::Error;
use std::fmt::{Display, Formatter};

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct AxisElementGraphCropBridgeReport {
    pub graph_index: u32,
    pub source_label_band_count: usize,
    pub crop_plan: AxisLabelCropPlan,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum AxisElementBridgeError {
    InvalidJson(String),
    MissingGraphIndex,
}

impl Display for AxisElementBridgeError {
    fn fmt(&self, formatter: &mut Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::InvalidJson(message) => {
                write!(formatter, "invalid axis element graph JSON: {message}")
            }
            Self::MissingGraphIndex => {
                write!(formatter, "axis element graph JSON is missing graphIndex")
            }
        }
    }
}

impl Error for AxisElementBridgeError {}

pub fn plan_crops_from_axis_element_graph_json(
    image: ImageGeometry,
    json: &str,
) -> Result<AxisElementGraphCropBridgeReport, AxisElementBridgeError> {
    let graph: AxisElementGraphJson = serde_json::from_str(json)
        .map_err(|error| AxisElementBridgeError::InvalidJson(error.to_string()))?;
    let graph_index = graph
        .graph_index
        .ok_or(AxisElementBridgeError::MissingGraphIndex)?;
    let bands = graph
        .label_bands
        .map(AxisElementGraphLabelBandsJson::into_label_bands)
        .unwrap_or_default();
    let crop_plan = plan_axis_label_crops(image, &bands);
    Ok(AxisElementGraphCropBridgeReport {
        graph_index,
        source_label_band_count: bands.len(),
        crop_plan,
    })
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
struct AxisElementGraphJson {
    graph_index: Option<u32>,
    label_bands: Option<AxisElementGraphLabelBandsJson>,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
struct AxisElementGraphLabelBandsJson {
    x_label_band: Option<GraphRegionJson>,
    y_label_band: Option<GraphRegionJson>,
    title_band: Option<GraphRegionJson>,
}

impl AxisElementGraphLabelBandsJson {
    fn into_label_bands(self) -> Vec<LabelBand> {
        let mut bands = Vec::with_capacity(3);
        if let Some(region) = self.x_label_band {
            bands.push(LabelBand::new(
                LabelBandKind::XTickLabels,
                region.into_rect(),
            ));
        }
        if let Some(region) = self.y_label_band {
            bands.push(LabelBand::new(
                LabelBandKind::YTickLabels,
                region.into_rect(),
            ));
        }
        if let Some(region) = self.title_band {
            bands.push(LabelBand::new(
                LabelBandKind::TitleOrMetadata,
                region.into_rect(),
            ));
        }
        bands
    }
}

#[derive(Debug, Deserialize)]
struct GraphRegionJson {
    x: i32,
    y: i32,
    width: u32,
    height: u32,
}

impl GraphRegionJson {
    const fn into_rect(self) -> Rect {
        Rect::new(self.x, self.y, self.width, self.height)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::CropBandRejectionReason;

    #[test]
    fn reads_axis_element_graph_label_bands_into_crop_plan() {
        let json = r#"{
            "graphIndex": 1,
            "labelBands": {
                "xLabelBand": { "x": 51, "y": 762, "width": 511, "height": 20, "label": "X label projection band" },
                "yLabelBand": { "x": 14, "y": 492, "width": 165, "height": 281, "label": "Y label projection band" },
                "titleBand": { "x": 14, "y": 492, "width": 548, "height": 22, "label": "Title and ion rejection band" }
            }
        }"#;

        let report =
            plan_crops_from_axis_element_graph_json(ImageGeometry::new(576, 1280), json).unwrap();

        assert_eq!(report.graph_index, 1);
        assert_eq!(report.source_label_band_count, 3);
        assert_eq!(report.crop_plan.rejected, []);
        assert_eq!(report.crop_plan.accepted.len(), 3);
        assert_eq!(
            report.crop_plan.accepted[0].band_kind,
            LabelBandKind::XTickLabels
        );
        assert_eq!(
            report.crop_plan.accepted[0].clamped_rect,
            Rect::new(51, 762, 511, 20)
        );
        assert_eq!(report.crop_plan.accepted[2].band_kind.axis(), None);
    }

    #[test]
    fn preserves_rejection_reason_for_outside_label_band() {
        let json = r#"{
            "graphIndex": 2,
            "labelBands": {
                "xLabelBand": { "x": 999, "y": 999, "width": 10, "height": 10, "label": "bad" }
            }
        }"#;

        let report =
            plan_crops_from_axis_element_graph_json(ImageGeometry::new(100, 80), json).unwrap();

        assert_eq!(report.source_label_band_count, 1);
        assert!(report.crop_plan.accepted.is_empty());
        assert_eq!(
            report.crop_plan.rejected[0].reason,
            CropBandRejectionReason::OutsideImage,
        );
    }

    #[test]
    fn rejects_invalid_json_without_guessing() {
        let error =
            plan_crops_from_axis_element_graph_json(ImageGeometry::new(1, 1), "{bad").unwrap_err();

        assert!(matches!(error, AxisElementBridgeError::InvalidJson(_)));
    }
}
