from app.api.health import router


def test_health_router_imports() -> None:
    assert router is not None
