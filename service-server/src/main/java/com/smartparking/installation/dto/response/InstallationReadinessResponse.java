package com.smartparking.installation.dto.response;

import java.util.List;
import java.util.UUID;

public record InstallationReadinessResponse(UUID parkingLotId, boolean ready, List<Issue> issues) {
    public record Issue(String code, UUID floorId, UUID spaceId) {}
}
