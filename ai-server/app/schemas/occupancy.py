from enum import StrEnum

from pydantic import BaseModel


class OccupancyState(StrEnum):
    EMPTY = "EMPTY"
    OCCUPIED = "OCCUPIED"
    UNKNOWN = "UNKNOWN"


class OccupancyResult(BaseModel):
    pass
