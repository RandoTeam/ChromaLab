use chromalab_cv_core::runtime_ocr_anchor_bridge::bridge_runtime_ocr_anchor_suite_json;
use std::env;
use std::fs;
use std::path::PathBuf;
use std::process::ExitCode;

fn main() -> ExitCode {
    match run() {
        Ok(output) => {
            println!("{output}");
            ExitCode::SUCCESS
        }
        Err(error) => {
            eprintln!("{error}");
            ExitCode::from(2)
        }
    }
}

fn run() -> Result<String, String> {
    let args = Args::parse(env::args().skip(1).collect())?;
    let json = fs::read_to_string(&args.runtime_ocr_anchor_input).map_err(|error| {
        format!(
            "failed to read {}: {error}",
            args.runtime_ocr_anchor_input.display()
        )
    })?;
    let report = bridge_runtime_ocr_anchor_suite_json(&json).map_err(|error| error.to_string())?;
    serde_json::to_string_pretty(&report)
        .map_err(|error| format!("failed to serialize runtime OCR anchor bridge report: {error}"))
}

#[derive(Debug)]
struct Args {
    runtime_ocr_anchor_input: PathBuf,
}

impl Args {
    fn parse(args: Vec<String>) -> Result<Self, String> {
        if args.len() != 1 || args.iter().any(|arg| arg == "--help" || arg == "-h") {
            return Err(usage());
        }
        Ok(Self {
            runtime_ocr_anchor_input: PathBuf::from(&args[0]),
        })
    }
}

fn usage() -> String {
    "Usage: chromalab_cv_ocr_anchor_bridge <runtime_ocr_anchor_input.json>".to_string()
}
