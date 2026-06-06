//! ChromaLab deterministic computer-vision core foundation.
//!
//! This crate is intentionally small in DR-2A. It defines the geometry and crop
//! planning contracts that Kotlin/Android can later call through FFI, without
//! changing chromatographic calculations or production report gates.

pub mod android_bridge;
mod axis_element_bridge;
pub mod stage1_image_prep;

pub use axis_element_bridge::{
    AxisElementBridgeError, AxisElementGraphCropBridgeReport,
    plan_crops_from_axis_element_graph_json,
};

use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub struct ImageGeometry {
    pub width: u32,
    pub height: u32,
}

impl ImageGeometry {
    #[must_use]
    pub const fn new(width: u32, height: u32) -> Self {
        Self { width, height }
    }

    #[must_use]
    pub const fn is_empty(self) -> bool {
        self.width == 0 || self.height == 0
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub struct Rect {
    pub x: i32,
    pub y: i32,
    pub width: u32,
    pub height: u32,
}

impl Rect {
    #[must_use]
    pub const fn new(x: i32, y: i32, width: u32, height: u32) -> Self {
        Self {
            x,
            y,
            width,
            height,
        }
    }

    #[must_use]
    pub const fn is_empty(self) -> bool {
        self.width == 0 || self.height == 0
    }

    #[must_use]
    pub fn right(self) -> i32 {
        self.x.saturating_add(u32_to_i32_saturating(self.width))
    }

    #[must_use]
    pub fn bottom(self) -> i32 {
        self.y.saturating_add(u32_to_i32_saturating(self.height))
    }

    #[must_use]
    pub fn area(self) -> u64 {
        u64::from(self.width) * u64::from(self.height)
    }

    #[must_use]
    pub fn clamp_to_image(self, image: ImageGeometry) -> Option<Self> {
        if image.is_empty() || self.is_empty() {
            return None;
        }

        let image_right = u32_to_i32_saturating(image.width);
        let image_bottom = u32_to_i32_saturating(image.height);
        let left = self.x.clamp(0, image_right);
        let top = self.y.clamp(0, image_bottom);
        let right = self.right().clamp(0, image_right);
        let bottom = self.bottom().clamp(0, image_bottom);
        if right <= left || bottom <= top {
            return None;
        }
        Some(Self::new(
            left,
            top,
            (right - left) as u32,
            (bottom - top) as u32,
        ))
    }

    #[must_use]
    pub fn intersection(self, other: Self) -> Option<Self> {
        let left = self.x.max(other.x);
        let top = self.y.max(other.y);
        let right = self.right().min(other.right());
        let bottom = self.bottom().min(other.bottom());
        if right <= left || bottom <= top {
            return None;
        }
        Some(Self::new(
            left,
            top,
            (right - left) as u32,
            (bottom - top) as u32,
        ))
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum Axis {
    X,
    Y,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum LabelBandKind {
    XTickLabels,
    YTickLabels,
    TitleOrMetadata,
}

impl LabelBandKind {
    #[must_use]
    pub const fn axis(self) -> Option<Axis> {
        match self {
            Self::XTickLabels => Some(Axis::X),
            Self::YTickLabels => Some(Axis::Y),
            Self::TitleOrMetadata => None,
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub struct LabelBand {
    pub kind: LabelBandKind,
    pub rect: Rect,
}

impl LabelBand {
    #[must_use]
    pub const fn new(kind: LabelBandKind, rect: Rect) -> Self {
        Self { kind, rect }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum CropVariantKind {
    Original,
    Grayscale,
    Scale2Grayscale,
    Scale4Grayscale,
    Scale4Contrast,
    Scale4OtsuThreshold,
    Scale4OtsuThresholdInverted,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct CropPlan {
    pub band_kind: LabelBandKind,
    pub source_rect: Rect,
    pub clamped_rect: Rect,
    pub variants: Vec<CropVariantKind>,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct RejectedCropBand {
    pub band_kind: LabelBandKind,
    pub source_rect: Rect,
    pub reason: CropBandRejectionReason,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum CropBandRejectionReason {
    EmptyImage,
    EmptyBand,
    OutsideImage,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct AxisLabelCropPlan {
    pub accepted: Vec<CropPlan>,
    pub rejected: Vec<RejectedCropBand>,
}

#[must_use]
pub fn default_axis_label_crop_variants() -> Vec<CropVariantKind> {
    vec![
        CropVariantKind::Original,
        CropVariantKind::Grayscale,
        CropVariantKind::Scale2Grayscale,
        CropVariantKind::Scale4Grayscale,
        CropVariantKind::Scale4Contrast,
        CropVariantKind::Scale4OtsuThreshold,
        CropVariantKind::Scale4OtsuThresholdInverted,
    ]
}

#[must_use]
pub fn plan_axis_label_crops(image: ImageGeometry, bands: &[LabelBand]) -> AxisLabelCropPlan {
    let variants = default_axis_label_crop_variants();
    let mut accepted = Vec::with_capacity(bands.len());
    let mut rejected = Vec::new();

    for band in bands {
        if image.is_empty() {
            rejected.push(RejectedCropBand {
                band_kind: band.kind,
                source_rect: band.rect,
                reason: CropBandRejectionReason::EmptyImage,
            });
            continue;
        }
        if band.rect.is_empty() {
            rejected.push(RejectedCropBand {
                band_kind: band.kind,
                source_rect: band.rect,
                reason: CropBandRejectionReason::EmptyBand,
            });
            continue;
        }
        match band.rect.clamp_to_image(image) {
            Some(clamped_rect) => accepted.push(CropPlan {
                band_kind: band.kind,
                source_rect: band.rect,
                clamped_rect,
                variants: variants.clone(),
            }),
            None => rejected.push(RejectedCropBand {
                band_kind: band.kind,
                source_rect: band.rect,
                reason: CropBandRejectionReason::OutsideImage,
            }),
        }
    }

    AxisLabelCropPlan { accepted, rejected }
}

const fn u32_to_i32_saturating(value: u32) -> i32 {
    if value > i32::MAX as u32 {
        i32::MAX
    } else {
        value as i32
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn clamps_partially_outside_label_band() {
        let image = ImageGeometry::new(100, 80);
        let bands = [LabelBand::new(
            LabelBandKind::XTickLabels,
            Rect::new(80, 70, 40, 20),
        )];

        let plan = plan_axis_label_crops(image, &bands);

        assert_eq!(plan.rejected, []);
        assert_eq!(plan.accepted.len(), 1);
        assert_eq!(plan.accepted[0].clamped_rect, Rect::new(80, 70, 20, 10));
        assert_eq!(plan.accepted[0].variants.len(), 7);
    }

    #[test]
    fn rejects_empty_and_outside_bands_with_reasons() {
        let image = ImageGeometry::new(100, 80);
        let bands = [
            LabelBand::new(LabelBandKind::YTickLabels, Rect::new(10, 10, 0, 20)),
            LabelBand::new(LabelBandKind::TitleOrMetadata, Rect::new(120, 90, 10, 10)),
        ];

        let plan = plan_axis_label_crops(image, &bands);

        assert!(plan.accepted.is_empty());
        assert_eq!(
            plan.rejected
                .iter()
                .map(|item| item.reason)
                .collect::<Vec<_>>(),
            vec![
                CropBandRejectionReason::EmptyBand,
                CropBandRejectionReason::OutsideImage,
            ],
        );
    }

    #[test]
    fn title_band_has_no_axis_authority() {
        assert_eq!(LabelBandKind::XTickLabels.axis(), Some(Axis::X));
        assert_eq!(LabelBandKind::YTickLabels.axis(), Some(Axis::Y));
        assert_eq!(LabelBandKind::TitleOrMetadata.axis(), None);
    }
}
