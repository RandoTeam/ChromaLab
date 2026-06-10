use crate::{ImageGeometry, plan_crops_from_axis_element_graph_json};
use jni::JNIEnv;
use jni::objects::{JObject, JString};
use jni::sys::{jboolean, jint, jstring};
use serde_json::json;
use std::fs;
use std::path::PathBuf;
use std::sync::mpsc;
use std::time::{Duration, Instant};
use turbovec::IdMapIndex;

pub const RUST_CV_BRIDGE_VERSION: &str = "dr2d-rust-cv-bridge-v1";
pub const RUST_CV_BRIDGE_CONTRACT: &str = "DR2D_JNI_PROBE_V1";
pub const AXIS_ELEMENT_CROP_CONTRACT: &str = "DR2F_AXIS_ELEMENT_CROP_PLAN_V1";
pub const TURBOVEC_APP_PRIVATE_CONTRACT: &str = "TV7_TURBOVEC_APP_PRIVATE_PROVIDER_V1";

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

pub fn turbovec_app_private_probe_json(app_private_root: &str, cleanup: bool) -> String {
    match run_turbovec_app_private_probe(app_private_root, cleanup) {
        Ok(report) => report,
        Err(error) => turbovec_app_private_error_json(&error),
    }
}

fn run_turbovec_app_private_probe(app_private_root: &str, cleanup: bool) -> Result<String, String> {
    if app_private_root.trim().is_empty() {
        return Err("app_private_root_missing".to_string());
    }

    let dim = 64usize;
    let bit_width = 4usize;
    let ids = [1001u64, 1002u64, 1003u64, 1004u64];
    let expected_top1 = 1002u64;
    let provider_dir = PathBuf::from(app_private_root).join("chromalab_tv7_turbovec");
    let index_path = provider_dir.join("chromalab_tv7_probe.tvim");
    let relative_index_path = "chromalab_tv7_turbovec/chromalab_tv7_probe.tvim";

    fs::create_dir_all(&provider_dir)
        .map_err(|error| format!("app_private_dir_create_failed:{error}"))?;
    let _ = fs::remove_file(&index_path);

    let rss_before_kb = rss_kb();
    let mut vectors = Vec::with_capacity(ids.len() * dim);
    for offset in 0..4usize {
        vectors.extend_from_slice(&basis_pattern(dim, offset));
    }
    let query = basis_pattern(dim, 1);

    let build_started = Instant::now();
    let mut index =
        IdMapIndex::new(dim, bit_width).map_err(|error| format!("index_create_failed:{error}"))?;
    index
        .add_with_ids(&vectors, &ids)
        .map_err(|error| format!("index_add_failed:{error}"))?;
    index.prepare();
    let build_ms = build_started.elapsed().as_millis();

    let write_started = Instant::now();
    index
        .write(&index_path)
        .map_err(|error| format!("index_write_failed:{error}"))?;
    let write_ms = write_started.elapsed().as_millis();
    let index_bytes = fs::metadata(&index_path)
        .map_err(|error| format!("index_metadata_failed:{error}"))?
        .len();
    drop(index);

    let load_query_started = Instant::now();
    let (query_sender, query_receiver) = mpsc::channel();
    let query_index_path = index_path.clone();
    std::thread::spawn(move || {
        let load_started = Instant::now();
        let loaded = match IdMapIndex::load(&query_index_path) {
            Ok(loaded) => loaded,
            Err(error) => {
                let _ = query_sender.send(Err(format!("index_load_failed:{error}")));
                return;
            }
        };
        let load_ms = load_started.elapsed().as_millis();
        let loaded_len = loaded.len();
        let loaded_dim = loaded.dim();
        let loaded_bit_width = loaded.bit_width();
        let query_started = Instant::now();
        let (scores, top_ids) = loaded.search(&query, 3);
        let query_ms = query_started.elapsed().as_millis();
        let _ = query_sender.send(Ok((
            load_ms,
            query_ms,
            loaded_len,
            loaded_dim,
            loaded_bit_width,
            scores,
            top_ids,
        )));
    });
    let load_query_result = match query_receiver.recv_timeout(Duration::from_secs(15)) {
        Ok(Ok(result)) => result,
        Ok(Err(error)) => return Err(error),
        Err(_) => (0, 0, 0, 0, 0, Vec::new(), Vec::new()),
    };
    let (load_ms, query_ms, loaded_len, loaded_dim, loaded_bit_width, scores, top_ids) =
        load_query_result;
    let query_timed_out =
        top_ids.is_empty() && load_query_started.elapsed() >= Duration::from_secs(15);
    let rss_after_kb = rss_kb();

    let top1_ok = top_ids.first().copied() == Some(expected_top1);
    let all_ids_valid = top_ids.iter().all(|id| ids.contains(id));
    let shape_ok = loaded_len == ids.len()
        && loaded_dim == dim
        && loaded_bit_width == bit_width
        && index_bytes > 0;
    let query_ok = top1_ok && all_ids_valid;
    let load_query_ok = shape_ok && query_ok && !query_timed_out;

    let cleanup_result = if cleanup {
        match fs::remove_file(&index_path) {
            Ok(()) => "deleted".to_string(),
            Err(error) if error.kind() == std::io::ErrorKind::NotFound => {
                "already_missing".to_string()
            }
            Err(error) => format!("delete_failed:{error}"),
        }
    } else {
        "not_requested".to_string()
    };
    let index_exists_after_cleanup = index_path.exists();
    let cleanup_ok = !cleanup || !index_exists_after_cleanup;
    let pass = load_query_ok && cleanup_ok;

    Ok(json!({
        "phase": "TV-7",
        "probe": "turbovec_app_private_provider",
        "status": if pass { "PASS" } else { "FAIL" },
        "ffiContract": TURBOVEC_APP_PRIVATE_CONTRACT,
        "backendId": "TURBOVEC_DENSE_SHADOW",
        "pathClass": "APP_PRIVATE",
        "indexRelativePath": relative_index_path,
        "appPrivateRootSanitized": sanitize_app_private_path(app_private_root),
        "dim": dim,
        "bitWidth": bit_width,
        "vectorCount": loaded_len,
        "indexBytes": index_bytes,
        "buildMs": build_ms,
        "writeMs": write_ms,
        "loadMs": load_ms,
        "queryMs": query_ms,
        "rssBeforeKb": rss_before_kb,
        "rssAfterKb": rss_after_kb,
        "topIds": top_ids,
        "scores": scores,
        "top1Expected": expected_top1,
        "top1Ok": top1_ok,
        "allIdsValid": all_ids_valid,
        "queryTimedOut": query_timed_out,
        "cleanupRequested": cleanup,
        "cleanupResult": cleanup_result,
        "indexExistsAfterCleanup": index_exists_after_cleanup,
        "runtimePromotion": false,
        "activeRetrievalOwnerUnchanged": true,
        "pixelGeometryAuthority": false,
        "calibrationAuthority": false,
        "peakMetricAuthority": false,
        "calculationEngineTouched": false
    })
    .to_string())
}

fn turbovec_app_private_error_json(message: &str) -> String {
    json!({
        "phase": "TV-7",
        "probe": "turbovec_app_private_provider",
        "status": "ERROR",
        "ffiContract": TURBOVEC_APP_PRIVATE_CONTRACT,
        "backendId": "TURBOVEC_DENSE_SHADOW",
        "pathClass": "APP_PRIVATE",
        "error": sanitize_error_message(message),
        "runtimePromotion": false,
        "activeRetrievalOwnerUnchanged": true,
        "pixelGeometryAuthority": false,
        "calibrationAuthority": false,
        "peakMetricAuthority": false,
        "calculationEngineTouched": false
    })
    .to_string()
}

fn basis_pattern(dim: usize, offset: usize) -> Vec<f32> {
    (0..dim)
        .map(|i| if i % 4 == offset { 1.0 } else { 0.0 })
        .collect()
}

fn rss_kb() -> u64 {
    let Ok(statm) = fs::read_to_string("/proc/self/statm") else {
        return 0;
    };
    let Some(pages) = statm
        .split_whitespace()
        .nth(1)
        .and_then(|value| value.parse::<u64>().ok())
    else {
        return 0;
    };
    pages * 4
}

fn sanitize_app_private_path(path: &str) -> String {
    let value = path.replace('\\', "/");
    if value.starts_with("/data/") {
        return "/data/<private>".to_string();
    }
    sanitize_error_message(&value)
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

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_chromalab_feature_processing_rust_RustCvBridge_nativeTurboVecAppPrivateProbeJson(
    mut env: JNIEnv<'_>,
    _this: JObject<'_>,
    app_private_root: JString<'_>,
    cleanup: jboolean,
) -> jstring {
    let result = match env.get_string(&app_private_root) {
        Ok(value) => turbovec_app_private_probe_json(&String::from(value), cleanup != 0),
        Err(error) => turbovec_app_private_error_json(&format!("jni_string_read_failed:{error}")),
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

    #[test]
    fn turbovec_app_private_probe_uses_app_private_path_and_cleans_up() {
        let root = std::env::temp_dir().join(format!(
            "chromalab_tv7_probe_test_{}",
            std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap()
                .as_nanos()
        ));
        std::fs::create_dir_all(&root).unwrap();

        let response = turbovec_app_private_probe_json(root.to_str().unwrap(), true);
        let parsed: Value = serde_json::from_str(&response).expect("valid tv7 response json");

        assert_eq!(parsed["phase"], "TV-7");
        assert_eq!(parsed["status"], "PASS");
        assert_eq!(parsed["ffiContract"], TURBOVEC_APP_PRIVATE_CONTRACT);
        assert_eq!(parsed["pathClass"], "APP_PRIVATE");
        assert_eq!(parsed["top1Ok"], true);
        assert_eq!(parsed["allIdsValid"], true);
        assert_eq!(parsed["runtimePromotion"], false);
        assert_eq!(parsed["activeRetrievalOwnerUnchanged"], true);
        assert_eq!(parsed["indexExistsAfterCleanup"], false);

        let _ = std::fs::remove_dir_all(&root);
    }

    #[test]
    fn turbovec_app_private_probe_reports_missing_root_without_promotion() {
        let parsed: Value =
            serde_json::from_str(&turbovec_app_private_probe_json("", true)).unwrap();

        assert_eq!(parsed["status"], "ERROR");
        assert_eq!(parsed["error"], "app_private_root_missing");
        assert_eq!(parsed["runtimePromotion"], false);
        assert_eq!(parsed["activeRetrievalOwnerUnchanged"], true);
    }
}
