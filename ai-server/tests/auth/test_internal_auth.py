from app.auth.internal_auth import InternalAuthBoundary


def test_internal_auth_boundary_imports() -> None:
    assert InternalAuthBoundary is not None
