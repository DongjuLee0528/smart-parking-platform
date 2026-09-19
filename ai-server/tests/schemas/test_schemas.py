from app.schemas.occupancy import OccupancyState


def test_occupancy_state_names_match_contract() -> None:
    assert {state.value for state in OccupancyState} == {"EMPTY", "OCCUPIED", "UNKNOWN"}
