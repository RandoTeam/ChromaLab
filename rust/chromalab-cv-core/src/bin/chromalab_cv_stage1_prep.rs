use chromalab_cv_core::stage1_image_prep::prepare_stage1_image_from_path;
use std::env;
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
    let report =
        prepare_stage1_image_from_path(&args.image_path).map_err(|error| error.to_string())?;
    serde_json::to_string_pretty(&report)
        .map_err(|error| format!("failed to serialize Stage 1 report: {error}"))
}

#[derive(Debug)]
struct Args {
    image_path: PathBuf,
}

impl Args {
    fn parse(args: Vec<String>) -> Result<Self, String> {
        if args.len() != 1 || args.iter().any(|arg| arg == "--help" || arg == "-h") {
            return Err(usage());
        }
        Ok(Self {
            image_path: PathBuf::from(&args[0]),
        })
    }
}

fn usage() -> String {
    "Usage: chromalab_cv_stage1_prep <image_path>".to_string()
}
