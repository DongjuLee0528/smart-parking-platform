from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    ai_server_host: str = "0.0.0.0"
    ai_server_port: int = 8000
    spring_internal_api_base_url: str = "http://localhost:8080/internal/v1"
    spring_internal_api_token: str = Field(default="", repr=False)
    model_dir: str = "./models"
