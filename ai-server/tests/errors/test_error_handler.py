from app.errors.error_handler import AiServerError


def test_error_handler_imports() -> None:
    assert AiServerError is not None
