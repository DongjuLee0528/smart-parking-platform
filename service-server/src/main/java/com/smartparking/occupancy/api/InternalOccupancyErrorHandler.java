package com.smartparking.occupancy.api;

import com.smartparking.global.error.ErrorCode;
import com.smartparking.global.error.ErrorResponse;
import com.smartparking.occupancy.application.OccupancyException;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = {InternalOccupancyController.class, OccupancyController.class})
public class InternalOccupancyErrorHandler {
    @ExceptionHandler(OccupancyException.class)
    public ResponseEntity<ErrorResponse> domain(OccupancyException exception) {
        var status = switch (exception.getCode()) {
            case CAMERA_NOT_FOUND, PARKING_SPACE_NOT_FOUND, PARKING_FLOOR_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case VERSION_CONFLICT -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return response(status, exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> validation(Exception exception) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.VALIDATION_FAILED,
            "Invalid occupancy request");
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ErrorResponse> conflict(Exception exception) {
        return response(HttpStatus.CONFLICT, ErrorCode.VERSION_CONFLICT,
            "Occupancy result conflicts with newer data");
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, ErrorCode code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message, UUID.randomUUID(), Map.of()));
    }
}
