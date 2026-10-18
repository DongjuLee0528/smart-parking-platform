package com.smartparking.floormap.application;

import com.smartparking.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class FloorMapException extends RuntimeException {
    private final ErrorCode code;

    public FloorMapException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
