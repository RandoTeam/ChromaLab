use jni::JNIEnv;
use jni::objects::JObject;
use jni::sys::jstring;
use serde_json::json;

pub const RUST_CV_BRIDGE_VERSION: &str = "dr2d-rust-cv-bridge-v1";
pub const RUST_CV_BRIDGE_CONTRACT: &str = "DR2D_JNI_PROBE_V1";

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
}
