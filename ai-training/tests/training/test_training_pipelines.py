from smart_parking_training.training.rtdetrv2_l import RtDetrv2LTrainingPipeline
from smart_parking_training.training.rtdetrv2_s import RtDetrv2STrainingPipeline
from smart_parking_training.training.yolo11m import Yolo11mTrainingPipeline


def test_training_pipelines_import() -> None:
    assert RtDetrv2STrainingPipeline is not None
    assert RtDetrv2LTrainingPipeline is not None
    assert Yolo11mTrainingPipeline is not None
