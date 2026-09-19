from app.integration.spring_client import SpringInternalApiClient


def test_spring_internal_api_client_imports() -> None:
    assert SpringInternalApiClient is not None
