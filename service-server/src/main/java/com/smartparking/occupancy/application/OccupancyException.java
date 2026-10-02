package com.smartparking.occupancy.application;

import com.smartparking.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class OccupancyException extends RuntimeException {
    private final ErrorCode code;

    public OccupancyException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
