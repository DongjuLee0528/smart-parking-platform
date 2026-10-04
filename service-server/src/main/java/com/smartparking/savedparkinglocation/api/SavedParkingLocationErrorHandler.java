package com.smartparking.savedparkinglocation.api;

import com.smartparking.global.error.ErrorCode;
import com.smartparking.global.error.ErrorResponse;
import com.smartparking.savedparkinglocation.application.SavedParkingLocationException;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = SavedParkingLocationController.class)
public class SavedParkingLocationErrorHandler {
    @ExceptionHandler(SavedParkingLocationException.class)
    public ResponseEntity<ErrorResponse> domain(SavedParkingLocationException exception) {
        var status = switch (exception.getCode()) {
            case AUTH_TOKEN_INVALID -> HttpStatus.UNAUTHORIZED;
            case VEHICLE_NOT_FOUND, PARKING_SPACE_NOT_FOUND, SAVED_PARKING_LOCATION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case EMPTY_SPACE_CONFIRM_REQUIRED, DUPLICATE_SELECTION_CONFIRM_REQUIRED -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return response(status, exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> validation(Exception exception) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.VALIDATION_FAILED,
            "Invalid saved parking location request");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException exception) {
        return response(HttpStatus.CONFLICT, ErrorCode.SAVED_PARKING_LOCATION_CONFLICT,
            "Saved parking location conflicts with existing data");
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, ErrorCode code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message, UUID.randomUUID(), Map.of()));
    }
}
