package com.smartparking.parkingspace.application;

import com.smartparking.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class ParkingSpaceException extends RuntimeException {
    private final ErrorCode code;

    public ParkingSpaceException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
