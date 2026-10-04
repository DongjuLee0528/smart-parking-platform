package com.smartparking.savedparkinglocation.application;

import com.smartparking.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class SavedParkingLocationException extends RuntimeException {
    private final ErrorCode code;

    public SavedParkingLocationException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }
}
