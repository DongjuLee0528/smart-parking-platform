from app.vehicle_detection.rtdetrv2_l_adapter import RtDetrv2LAdapter
from app.vehicle_detection.rtdetrv2_s_adapter import RtDetrv2SAdapter
from app.vehicle_detection.yolo11m_adapter import Yolo11mAdapter


def test_vehicle_detector_adapters_import() -> None:
    assert RtDetrv2SAdapter is not None
    assert RtDetrv2LAdapter is not None
    assert Yolo11mAdapter is not None
