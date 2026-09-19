from app.rtsp.stream_client import RtspStreamClient


def test_rtsp_stream_client_imports() -> None:
    assert RtspStreamClient is not None
