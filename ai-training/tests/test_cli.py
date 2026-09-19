from pathlib import Path

import pytest
import typer

from smart_parking_training.cli import require_path


def test_require_path_accepts_existing_path(tmp_path: Path) -> None:
    require_path(tmp_path, "Dataset root")


def test_require_path_rejects_missing_path(tmp_path: Path) -> None:
    with pytest.raises(typer.BadParameter, match="Dataset root not found"):
        require_path(tmp_path / "missing", "Dataset root")
