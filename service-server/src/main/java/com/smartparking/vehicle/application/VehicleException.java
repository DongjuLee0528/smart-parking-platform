package com.smartparking.vehicle.application;

import com.smartparking.global.error.ErrorCode;

public class VehicleException extends RuntimeException {
    private final ErrorCode code;

    public VehicleException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
