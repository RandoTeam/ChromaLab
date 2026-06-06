use image::{DynamicImage, GrayImage, ImageBuffer, Luma};
use serde::{Deserialize, Serialize};
use sha2::{Digest, Sha256};
use std::cmp::Ordering;
use std::error::Error;
use std::fmt::{Display, Formatter};
use std::fs;
use std::path::Path;
use std::time::Instant;

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Stage1ImagePreparationReport {
    pub schema_version: String,
    pub production_impact: String,
    pub runtime_readiness: String,
    pub decoder: String,
    pub source_image: String,
    pub source_sha256: String,
    pub normalized_sha256: String,
    pub source_metrics: Stage1ImageMetrics,
    pub variant_scores: Vec<Stage1VariantScore>,
    pub selected_variant_id: String,
    pub selected_variant_score: f64,
    pub status: Stage1Status,
    pub warnings: Vec<Stage1Warning>,
    pub elapsed_ms: f64,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum Stage1Status {
    Pass,
    Review,
}

impl Stage1Status {
    #[must_use]
    pub const fn as_record_status(&self) -> &'static str {
        match self {
            Self::Pass => "PASS",
            Self::Review => "REVIEW",
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "SCREAMING_SNAKE_CASE")]
pub enum Stage1Warning {
    LowResolutionInput,
    LowContrastInput,
    LowEdgeDensity,
    WeakPreprocessingVariantScore,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Stage1VariantScore {
    pub variant_id: String,
    pub score: f64,
    pub metrics: Stage1ImageMetrics,
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Stage1ImageMetrics {
    pub width: u32,
    pub height: u32,
    pub megapixels: f64,
    pub aspect_ratio: f64,
    pub luminance_mean: f64,
    pub luminance_std: f64,
    pub p05: f64,
    pub p10: f64,
    pub p50: f64,
    pub p90: f64,
    pub p95: f64,
    #[serde(rename = "contrastP90P10")]
    pub contrast_p90p10: f64,
    pub dark_pixel_fraction: f64,
    pub edge_density: f64,
}

#[derive(Debug)]
pub enum Stage1ImagePrepError {
    ReadFailed(String),
    DecodeFailed(String),
    EmptyImage,
}

impl Display for Stage1ImagePrepError {
    fn fmt(&self, formatter: &mut Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::ReadFailed(message) => write!(formatter, "failed to read image: {message}"),
            Self::DecodeFailed(message) => write!(formatter, "failed to decode image: {message}"),
            Self::EmptyImage => write!(formatter, "decoded image is empty"),
        }
    }
}

impl Error for Stage1ImagePrepError {}

pub fn prepare_stage1_image_from_path(
    path: &Path,
) -> Result<Stage1ImagePreparationReport, Stage1ImagePrepError> {
    let started = Instant::now();
    let bytes =
        fs::read(path).map_err(|error| Stage1ImagePrepError::ReadFailed(error.to_string()))?;
    let source_sha256 = sha256_hex(&bytes);
    let decoded = image::load_from_memory(&bytes)
        .map_err(|error| Stage1ImagePrepError::DecodeFailed(error.to_string()))?;
    prepare_stage1_image(path, &source_sha256, decoded, started)
}

fn prepare_stage1_image(
    path: &Path,
    source_sha256: &str,
    decoded: DynamicImage,
    started: Instant,
) -> Result<Stage1ImagePreparationReport, Stage1ImagePrepError> {
    let normalized = decoded.to_rgb8();
    if normalized.width() == 0 || normalized.height() == 0 {
        return Err(Stage1ImagePrepError::EmptyImage);
    }
    let normalized_sha256 =
        sha256_normalized_image(normalized.as_raw(), normalized.width(), normalized.height());
    let variants = build_variants(&normalized);
    let mut variant_scores = variants
        .into_iter()
        .map(|(variant_id, image)| {
            let metrics = image_metrics(&image);
            let score = score_variant(&metrics, variant_id);
            Stage1VariantScore {
                variant_id: variant_id.to_string(),
                score,
                metrics,
            }
        })
        .collect::<Vec<_>>();
    variant_scores.sort_by(|left, right| {
        right
            .score
            .partial_cmp(&left.score)
            .unwrap_or(Ordering::Equal)
            .then_with(|| left.variant_id.cmp(&right.variant_id))
    });
    let selected = variant_scores
        .first()
        .expect("Stage 1 always creates at least one variant");
    let source_metrics = image_metrics(&rgb_to_gray(
        normalized.as_raw(),
        normalized.width(),
        normalized.height(),
    ));
    let (status, warnings) = status_for(&source_metrics, selected.score);
    Ok(Stage1ImagePreparationReport {
        schema_version: "chromalab.rust.stage1_image_preparation.v1".to_string(),
        production_impact: "NONE_SHADOW_ONLY".to_string(),
        runtime_readiness: "RUST_STAGE1_PARITY_BRIDGE_NOT_RUNTIME_READY".to_string(),
        decoder: "rust-image-0.25.10-jpeg-png-no-exif-transpose".to_string(),
        source_image: path.to_string_lossy().replace('\\', "/"),
        source_sha256: source_sha256.to_string(),
        normalized_sha256,
        source_metrics,
        selected_variant_id: selected.variant_id.clone(),
        selected_variant_score: selected.score,
        variant_scores,
        status,
        warnings,
        elapsed_ms: round3(started.elapsed().as_secs_f64() * 1000.0),
    })
}

fn build_variants(source_rgb: &image::RgbImage) -> Vec<(&'static str, GrayImage)> {
    let grayscale = rgb_to_gray(source_rgb.as_raw(), source_rgb.width(), source_rgb.height());
    let autocontrast = autocontrast(&grayscale);
    let sharpened_once = sharpen(&autocontrast);
    let sharpened_twice = sharpen(&sharpened_once);
    let threshold = otsu_threshold(&autocontrast);
    let binary = threshold_binary(&autocontrast, threshold);
    vec![
        ("source_rgb", grayscale.clone()),
        ("grayscale", grayscale),
        ("autocontrast", autocontrast),
        ("sharpened_autocontrast", sharpened_twice),
        ("otsu_binary", binary),
    ]
}

fn rgb_to_gray(bytes: &[u8], width: u32, height: u32) -> GrayImage {
    let mut out = GrayImage::new(width, height);
    for (index, pixel) in out.pixels_mut().enumerate() {
        let offset = index * 3;
        let red = bytes[offset] as f64;
        let green = bytes[offset + 1] as f64;
        let blue = bytes[offset + 2] as f64;
        let luminance = (red * 0.299) + (green * 0.587) + (blue * 0.114);
        *pixel = Luma([luminance.round().clamp(0.0, 255.0) as u8]);
    }
    out
}

fn autocontrast(image: &GrayImage) -> GrayImage {
    let mut min_value = u8::MAX;
    let mut max_value = u8::MIN;
    for pixel in image.pixels() {
        min_value = min_value.min(pixel[0]);
        max_value = max_value.max(pixel[0]);
    }
    if max_value <= min_value {
        return image.clone();
    }
    let scale = 255.0 / f64::from(max_value - min_value);
    ImageBuffer::from_fn(image.width(), image.height(), |x, y| {
        let value = image.get_pixel(x, y)[0];
        let mapped = (f64::from(value.saturating_sub(min_value)) * scale).round();
        Luma([mapped.clamp(0.0, 255.0) as u8])
    })
}

fn sharpen(image: &GrayImage) -> GrayImage {
    ImageBuffer::from_fn(image.width(), image.height(), |x, y| {
        let mut sum = i32::from(image.get_pixel(x, y)[0]) * 32;
        for dy in [-1_i32, 0, 1] {
            for dx in [-1_i32, 0, 1] {
                let nx = ((x as i32) + dx).clamp(0, (image.width() - 1) as i32) as u32;
                let ny = ((y as i32) + dy).clamp(0, (image.height() - 1) as i32) as u32;
                if nx == x && ny == y {
                    continue;
                }
                sum -= i32::from(image.get_pixel(nx, ny)[0]) * 2;
            }
        }
        Luma([(sum / 16).clamp(0, 255) as u8])
    })
}

fn threshold_binary(image: &GrayImage, threshold: u8) -> GrayImage {
    ImageBuffer::from_fn(image.width(), image.height(), |x, y| {
        if image.get_pixel(x, y)[0] > threshold {
            Luma([255])
        } else {
            Luma([0])
        }
    })
}

fn otsu_threshold(image: &GrayImage) -> u8 {
    let mut hist = [0_u64; 256];
    for pixel in image.pixels() {
        hist[pixel[0] as usize] += 1;
    }
    let total = u64::from(image.width()) * u64::from(image.height());
    if total == 0 {
        return 128;
    }
    let sum_total = hist
        .iter()
        .enumerate()
        .map(|(value, count)| (value as f64) * (*count as f64))
        .sum::<f64>();
    let mut sum_background = 0.0;
    let mut weight_background = 0_u64;
    let mut best_variance = -1.0;
    let mut best_threshold = 128_u8;
    for (threshold, count) in hist.iter().enumerate() {
        weight_background += *count;
        if weight_background == 0 {
            continue;
        }
        let weight_foreground = total - weight_background;
        if weight_foreground == 0 {
            break;
        }
        sum_background += (threshold as f64) * (*count as f64);
        let mean_background = sum_background / (weight_background as f64);
        let mean_foreground = (sum_total - sum_background) / (weight_foreground as f64);
        let variance = (weight_background as f64)
            * (weight_foreground as f64)
            * (mean_background - mean_foreground).powi(2);
        if variance > best_variance {
            best_variance = variance;
            best_threshold = threshold as u8;
        }
    }
    best_threshold
}

fn image_metrics(image: &GrayImage) -> Stage1ImageMetrics {
    let width = image.width();
    let height = image.height();
    let values = image.pixels().map(|pixel| pixel[0]).collect::<Vec<_>>();
    let mut sorted = values.clone();
    sorted.sort_unstable();
    let len = values.len().max(1) as f64;
    let mean = values.iter().map(|value| f64::from(*value)).sum::<f64>() / len;
    let variance = values
        .iter()
        .map(|value| (f64::from(*value) - mean).powi(2))
        .sum::<f64>()
        / len;
    let p05 = percentile(&sorted, 0.05);
    let p10 = percentile(&sorted, 0.10);
    let p50 = percentile(&sorted, 0.50);
    let p90 = percentile(&sorted, 0.90);
    let p95 = percentile(&sorted, 0.95);
    let dark_threshold = 35.0_f64.max(p10);
    let dark_count = values
        .iter()
        .filter(|value| f64::from(**value) < dark_threshold)
        .count();
    Stage1ImageMetrics {
        width,
        height,
        megapixels: round4(f64::from(width) * f64::from(height) / 1_000_000.0),
        aspect_ratio: round4(f64::from(width) / f64::from(height.max(1))),
        luminance_mean: round4(mean),
        luminance_std: round4(variance.sqrt()),
        p05: round4(p05),
        p10: round4(p10),
        p50: round4(p50),
        p90: round4(p90),
        p95: round4(p95),
        contrast_p90p10: round4(p90 - p10),
        dark_pixel_fraction: round6((dark_count as f64) / len),
        edge_density: round6(edge_density(image)),
    }
}

fn percentile(sorted: &[u8], percentile: f64) -> f64 {
    if sorted.is_empty() {
        return 0.0;
    }
    if sorted.len() == 1 {
        return f64::from(sorted[0]);
    }
    let position = ((sorted.len() - 1) as f64) * percentile;
    let lower_index = position.floor() as usize;
    let upper_index = position.ceil() as usize;
    if lower_index == upper_index {
        return f64::from(sorted[lower_index]);
    }
    let fraction = position - (lower_index as f64);
    let lower = f64::from(sorted[lower_index]);
    let upper = f64::from(sorted[upper_index]);
    lower + ((upper - lower) * fraction)
}

fn edge_density(image: &GrayImage) -> f64 {
    let width = image.width();
    let height = image.height();
    let vertical = if width > 1 {
        let mut count = 0_u64;
        let total = u64::from(height) * u64::from(width - 1);
        for y in 0..height {
            for x in 0..(width - 1) {
                let left = i16::from(image.get_pixel(x, y)[0]);
                let right = i16::from(image.get_pixel(x + 1, y)[0]);
                if (right - left).abs() > 28 {
                    count += 1;
                }
            }
        }
        (count as f64) / (total as f64)
    } else {
        0.0
    };
    let horizontal = if height > 1 {
        let mut count = 0_u64;
        let total = u64::from(height - 1) * u64::from(width);
        for y in 0..(height - 1) {
            for x in 0..width {
                let top = i16::from(image.get_pixel(x, y)[0]);
                let bottom = i16::from(image.get_pixel(x, y + 1)[0]);
                if (bottom - top).abs() > 28 {
                    count += 1;
                }
            }
        }
        (count as f64) / (total as f64)
    } else {
        0.0
    };
    (vertical + horizontal) / 2.0
}

fn score_variant(metrics: &Stage1ImageMetrics, variant_id: &str) -> f64 {
    let contrast = clamp_ratio(metrics.contrast_p90p10, 25.0, 170.0);
    let edges = clamp_ratio(metrics.edge_density, 0.015, 0.18);
    let dark_score = 1.0 - (metrics.dark_pixel_fraction - 0.12).abs().min(0.18) / 0.18;
    let mut penalty = 0.0;
    if variant_id == "otsu_binary" {
        penalty += 0.12;
    }
    if metrics.p95 < 120.0 {
        penalty += 0.15;
    }
    round6(((contrast * 0.45) + (edges * 0.35) + (dark_score * 0.20) - penalty).max(0.0))
}

fn status_for(
    metrics: &Stage1ImageMetrics,
    selected_score: f64,
) -> (Stage1Status, Vec<Stage1Warning>) {
    let mut warnings = Vec::new();
    if metrics.megapixels < 0.35 {
        warnings.push(Stage1Warning::LowResolutionInput);
    }
    if metrics.contrast_p90p10 < 30.0 {
        warnings.push(Stage1Warning::LowContrastInput);
    }
    if metrics.edge_density < 0.015 {
        warnings.push(Stage1Warning::LowEdgeDensity);
    }
    if selected_score < 0.32 {
        warnings.push(Stage1Warning::WeakPreprocessingVariantScore);
    }
    if warnings.is_empty() {
        (Stage1Status::Pass, warnings)
    } else {
        (Stage1Status::Review, warnings)
    }
}

fn clamp_ratio(value: f64, low: f64, high: f64) -> f64 {
    if high <= low {
        return 0.0;
    }
    ((value - low) / (high - low)).clamp(0.0, 1.0)
}

fn sha256_hex(bytes: &[u8]) -> String {
    let digest = Sha256::digest(bytes);
    let mut output = String::with_capacity(64);
    for byte in digest {
        output.push_str(&format!("{byte:02x}"));
    }
    output
}

fn sha256_normalized_image(bytes: &[u8], width: u32, height: u32) -> String {
    let mut hasher = Sha256::new();
    hasher.update(bytes);
    hasher.update(format!("({}, {})", width, height).as_bytes());
    let digest = hasher.finalize();
    let mut output = String::with_capacity(64);
    for byte in digest {
        output.push_str(&format!("{byte:02x}"));
    }
    output
}

fn round3(value: f64) -> f64 {
    (value * 1_000.0).round() / 1_000.0
}

fn round4(value: f64) -> f64 {
    (value * 10_000.0).round() / 10_000.0
}

fn round6(value: f64) -> f64 {
    (value * 1_000_000.0).round() / 1_000_000.0
}

#[cfg(test)]
mod tests {
    use super::*;
    use image::Rgb;

    #[test]
    fn chooses_autocontrast_variant_for_low_dynamic_range_image() {
        let image = image::RgbImage::from_fn(40, 30, |x, y| {
            let value = 100 + (((x + y) % 20) as u8);
            Rgb([value, value, value])
        });

        let variants = build_variants(&image);
        let mut scores = variants
            .iter()
            .map(|(id, image)| (*id, score_variant(&image_metrics(image), id)))
            .collect::<Vec<_>>();
        scores.sort_by(|left, right| right.1.partial_cmp(&left.1).unwrap());

        assert!(matches!(
            scores[0].0,
            "autocontrast" | "sharpened_autocontrast"
        ));
    }

    #[test]
    fn low_resolution_input_is_review() {
        let image = GrayImage::from_pixel(20, 20, Luma([120]));
        let metrics = image_metrics(&image);

        let (status, warnings) = status_for(&metrics, 0.5);

        assert_eq!(status, Stage1Status::Review);
        assert!(warnings.contains(&Stage1Warning::LowResolutionInput));
    }

    #[test]
    fn normalized_hash_matches_r3_shape_suffix() {
        let bytes = [255_u8, 0, 0, 0, 255, 0];
        let hash = sha256_normalized_image(&bytes, 2, 1);

        assert_eq!(hash.len(), 64);
        assert_ne!(hash, sha256_hex(&bytes));
    }
}
