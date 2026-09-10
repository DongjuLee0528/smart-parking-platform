package com.smartparking.global.error;

import java.util.Map;
import java.util.UUID;

public record ErrorResponse(ErrorCode code, String message, UUID traceId, Map<String, Object> details) {
}
