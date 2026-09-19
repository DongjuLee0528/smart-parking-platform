from app.streaming.reconnect_policy import RtspReconnectPolicy


def test_rtsp_reconnect_policy_imports() -> None:
    assert RtspReconnectPolicy is not None
