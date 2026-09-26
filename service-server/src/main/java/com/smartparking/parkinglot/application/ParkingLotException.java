package com.smartparking.parkinglot.application;

import com.smartparking.global.error.ErrorCode;

public class ParkingLotException extends RuntimeException {
    private final ErrorCode code;
    public ParkingLotException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }
    public ErrorCode getCode() { return code; }
}
