from pathlib import Path
from typing import Annotated

import typer

app = typer.Typer(no_args_is_help=True)


def require_path(path: Path, label: str) -> None:
    if not path.exists():
        raise typer.BadParameter(f"{label} not found: {path}")


@app.command()
def validate_dataset(dataset_root: Annotated[Path, typer.Option()]) -> None:
    require_path(dataset_root, "Dataset root")
    typer.echo(f"Dataset root exists: {dataset_root}")


@app.command()
def split_dataset(
    dataset_root: Annotated[Path, typer.Option()],
    output_dir: Annotated[Path, typer.Option()],
) -> None:
    require_path(dataset_root, "Dataset root")
    raise typer.BadParameter(f"Split configuration is required before writing {output_dir}")


def train_from_config(config: Path, detector: str) -> None:
    require_path(config, f"{detector} config")
    raise typer.BadParameter(f"{detector} training is not configured with local data yet")


@app.command()
def train_rtdetrv2_s(config: Annotated[Path, typer.Option()]) -> None:
    train_from_config(config, "RT-DETRv2-S")


@app.command()
def train_rtdetrv2_l(config: Annotated[Path, typer.Option()]) -> None:
    train_from_config(config, "RT-DETRv2-L")


@app.command()
def train_yolo11m(config: Annotated[Path, typer.Option()]) -> None:
    train_from_config(config, "YOLO11m")


@app.command()
def compare_detectors(runs_dir: Annotated[Path, typer.Option()]) -> None:
    require_path(runs_dir, "Runs directory")
    raise typer.BadParameter("Detector result files are required before comparison")


@app.command()
def train_mask_rcnn(config: Annotated[Path, typer.Option()]) -> None:
    train_from_config(config, "Mask R-CNN R50-FPN")


@app.command()
def evaluate_mask_rcnn(
    checkpoint: Annotated[Path, typer.Option()],
    dataset_root: Annotated[Path, typer.Option()],
) -> None:
    require_path(checkpoint, "Mask R-CNN checkpoint")
    require_path(dataset_root, "Dataset root")
    raise typer.BadParameter("Evaluation config is required before running Mask R-CNN evaluation")
