use chromalab_cv_core::{ImageGeometry, plan_crops_from_axis_element_graph_json};
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
    let json = fs::read_to_string(&args.axis_element_graph_json).map_err(|error| {
        format!(
            "failed to read {}: {error}",
            args.axis_element_graph_json.display()
        )
    })?;
    let report = plan_crops_from_axis_element_graph_json(
        ImageGeometry::new(args.image_width, args.image_height),
        &json,
    )
    .map_err(|error| error.to_string())?;
    serde_json::to_string_pretty(&report)
        .map_err(|error| format!("failed to serialize bridge report: {error}"))
}

#[derive(Debug)]
struct Args {
    axis_element_graph_json: PathBuf,
    image_width: u32,
    image_height: u32,
}

impl Args {
    fn parse(args: Vec<String>) -> Result<Self, String> {
        if args.len() != 3 || args.iter().any(|arg| arg == "--help" || arg == "-h") {
            return Err(usage());
        }
        let axis_element_graph_json = PathBuf::from(&args[0]);
        let image_width = args[1]
            .parse::<u32>()
            .map_err(|_| format!("invalid image width '{}'\n\n{}", args[1], usage()))?;
        let image_height = args[2]
            .parse::<u32>()
            .map_err(|_| format!("invalid image height '{}'\n\n{}", args[2], usage()))?;
        Ok(Self {
            axis_element_graph_json,
            image_width,
            image_height,
        })
    }
}

fn usage() -> String {
    "Usage: chromalab_cv_bridge <axis_element_graph.json> <image_width> <image_height>".to_string()
}
