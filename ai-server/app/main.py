from fastapi import FastAPI

from app.api.health import router as health_router
from app.core.settings import Settings


def create_app(settings: Settings | None = None) -> FastAPI:
    app = FastAPI(title="Smart Parking AI Server", version="0.1.0")
    app.state.settings = settings or Settings()
    app.include_router(health_router)
    return app


app = create_app()
