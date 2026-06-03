use crate::{ImageGeometry, plan_crops_from_axis_element_graph_json};
use jni::JNIEnv;
use jni::objects::{JObject, JString};
use jni::sys::{jint, jstring};
use serde_json::json;

pub const RUST_CV_BRIDGE_VERSION: &str = "dr2d-rust-cv-bridge-v1";
pub const RUST_CV_BRIDGE_CONTRACT: &str = "DR2D_JNI_PROBE_V1";
pub const AXIS_ELEMENT_CROP_CONTRACT: &str = "DR2F_AXIS_ELEMENT_CROP_PLAN_V1";

pub fn probe_json() -> String {
    json!({
        "bridge": "chromalab_cv_core",
        "bridgeVersion": RUST_CV_BRIDGE_VERSION,
        "crateVersion": env!("CARGO_PKG_VERSION"),
        "ffiContract": RUST_CV_BRIDGE_CONTRACT,
        "nativeStatus": "AVAILABLE",
        "algorithmAuthority": "none",
        "pixelGeometryAuthority": false,
        "calibrationAuthority": false,
        "peakMetricAuthority": false,
        "calculationEngineTouched": false
    })
    .to_string()
}

pub fn plan_axis_element_crops_json_for_jni(
    image_width: i32,
    image_height: i32,
    axis_element_graph_json: &str,
) -> String {
    if image_width <= 0 || image_height <= 0 {
        return axis_element_crop_error_json("invalid_image_geometry");
    }

    let image = ImageGeometry::new(image_width as u32, image_height as u32);
    match plan_crops_from_axis_element_graph_json(image, axis_element_graph_json) {
        Ok(report) => json!({
            "status": "OK",
            "ffiContract": AXIS_ELEMENT_CROP_CONTRACT,
            "bridgeVersion": RUST_CV_BRIDGE_VERSION,
            "image": {
                "width": image.width,
                "height": image.height
            },
            "algorithmAuthority": "crop_planning_only",
            "pixelGeometryAuthority": false,
            "calibrationAuthority": false,
            "peakMetricAuthority": false,
            "calculationEngineTouched": false,
            "report": report
        })
        .to_string(),
        Err(error) => axis_element_crop_error_json(&error.to_string()),
    }
}

fn axis_element_crop_error_json(message: &str) -> String {
    json!({
        "status": "ERROR",
        "ffiContract": AXIS_ELEMENT_CROP_CONTRACT,
        "bridgeVersion": RUST_CV_BRIDGE_VERSION,
        "error": sanitize_error_message(message),
        "algorithmAuthority": "crop_planning_only",
        "pixelGeometryAuthority": false,
        "calibrationAuthority": false,
        "peakMetricAuthority": false,
        "calculationEngineTouched": false
    })
    .to_string()
}

fn sanitize_error_message(message: &str) -> String {
    message
        .replace('\\', "/")
        .replace("C:/Users/", "C:/Users/<private>/")
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_chromalab_feature_processing_rust_RustCvBridge_nativeProbeJson(
    env: JNIEnv<'_>,
    _this: JObject<'_>,
) -> jstring {
    match env.new_string(probe_json()) {
        Ok(value) => value.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_chromalab_feature_processing_rust_RustCvBridge_nativePlanAxisElementCropsJson(
    mut env: JNIEnv<'_>,
    _this: JObject<'_>,
    image_width: jint,
    image_height: jint,
    axis_element_graph_json: JString<'_>,
) -> jstring {
    let result = match env.get_string(&axis_element_graph_json) {
        Ok(value) => {
            plan_axis_element_crops_json_for_jni(image_width, image_height, &String::from(value))
        }
        Err(error) => axis_element_crop_error_json(&format!("jni_string_read_failed:{error}")),
    };

    match env.new_string(result) {
        Ok(value) => value.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::Value;

    #[test]
    fn probe_json_declares_no_algorithm_authority() {
        let parsed: Value = serde_json::from_str(&probe_json()).expect("valid bridge probe json");
        assert_eq!(parsed["bridge"], "chromalab_cv_core");
        assert_eq!(parsed["ffiContract"], RUST_CV_BRIDGE_CONTRACT);
        assert_eq!(parsed["nativeStatus"], "AVAILABLE");
        assert_eq!(parsed["algorithmAuthority"], "none");
        assert_eq!(parsed["pixelGeometryAuthority"], false);
        assert_eq!(parsed["calibrationAuthority"], false);
        assert_eq!(parsed["peakMetricAuthority"], false);
        assert_eq!(parsed["calculationEngineTouched"], false);
    }

    #[test]
    fn axis_element_crop_jni_json_wraps_real_crop_plan() {
        let json = r#"{
            "graphIndex": 1,
            "labelBands": {
                "xLabelBand": { "x": 51, "y": 762, "width": 511, "height": 20 },
                "yLabelBand": { "x": 14, "y": 492, "width": 165, "height": 281 },
                "titleBand": { "x": 14, "y": 492, "width": 548, "height": 22 }
            }
        }"#;

        let parsed: Value =
            serde_json::from_str(&plan_axis_element_crops_json_for_jni(576, 1280, json))
                .expect("valid jni crop response json");

        assert_eq!(parsed["status"], "OK");
        assert_eq!(parsed["ffiContract"], AXIS_ELEMENT_CROP_CONTRACT);
        assert_eq!(parsed["report"]["source_label_band_count"], 3);
        assert_eq!(
            parsed["report"]["crop_plan"]["accepted"]
                .as_array()
                .unwrap()
                .len(),
            3
        );
        assert_eq!(parsed["pixelGeometryAuthority"], false);
        assert_eq!(parsed["calibrationAuthority"], false);
        assert_eq!(parsed["peakMetricAuthority"], false);
        assert_eq!(parsed["calculationEngineTouched"], false);
    }

    #[test]
    fn axis_element_crop_jni_json_reports_invalid_dimensions() {
        let parsed: Value =
            serde_json::from_str(&plan_axis_element_crops_json_for_jni(0, 1280, "{}"))
                .expect("valid jni error json");

        assert_eq!(parsed["status"], "ERROR");
        assert_eq!(parsed["ffiContract"], AXIS_ELEMENT_CROP_CONTRACT);
        assert_eq!(parsed["error"], "invalid_image_geometry");
    }

    #[test]
    fn axis_element_crop_jni_json_reports_invalid_graph_json() {
        let parsed: Value =
            serde_json::from_str(&plan_axis_element_crops_json_for_jni(576, 1280, "{bad"))
                .expect("valid jni error json");

        assert_eq!(parsed["status"], "ERROR");
        assert!(
            parsed["error"]
                .as_str()
                .unwrap()
                .contains("invalid axis element graph JSON")
        );
    }
}
