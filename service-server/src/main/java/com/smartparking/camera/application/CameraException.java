package com.smartparking.camera.application;

import com.smartparking.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class CameraException extends RuntimeException {
    private final ErrorCode code;

    public CameraException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
