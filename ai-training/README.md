# AI Training

Offline training baseline for parking occupancy detectors and parking-space candidate generation.

This module does not download datasets, download model weights, start training, or fabricate metrics. CLI commands validate required local inputs and fail with actionable messages until datasets and model configs are provided.

## Commands

```bash
uv run parking-training validate-dataset --dataset-root datasets/raw
uv run parking-training split-dataset --dataset-root datasets/raw --output-dir datasets/splits
uv run parking-training train-rtdetrv2-s --config configs/rtdetrv2_s/config.yaml
uv run parking-training train-rtdetrv2-l --config configs/rtdetrv2_l/config.yaml
uv run parking-training train-yolo11m --config configs/yolo11m/config.yaml
uv run parking-training compare-detectors --runs-dir runs
uv run parking-training train-mask-rcnn --config configs/parking_space_mask_rcnn/config.yaml
uv run parking-training evaluate-mask-rcnn --checkpoint checkpoints/mask_rcnn.pt --dataset-root datasets/processed
```

Keep datasets, runs, checkpoints, weights, caches, and experiment outputs out of Git.
