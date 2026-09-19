from app.camera.registry import CameraRegistry


def test_camera_registry_imports() -> None:
    assert CameraRegistry is not None
