package com.smartparking.parkinglot.api;

import com.smartparking.global.error.*;
import com.smartparking.parkinglot.application.ParkingLotException;
import java.util.*;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = {ParkingLotController.class, AdminParkingLotController.class})
public class ParkingLotErrorHandler {
    @ExceptionHandler(ParkingLotException.class)
    public ResponseEntity<ErrorResponse> domain(ParkingLotException exception) {
        var status = switch (exception.getCode()) {
            case PARKING_LOT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case VERSION_CONFLICT -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return response(status, exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> validation(Exception exception) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.VALIDATION_FAILED, "Invalid parking lot request");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> conflict(OptimisticLockingFailureException exception) {
        return response(HttpStatus.CONFLICT, ErrorCode.VERSION_CONFLICT, "Parking lot version has changed");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException exception) {
        return response(HttpStatus.CONFLICT, ErrorCode.VERSION_CONFLICT, "Parking lot data conflicts with existing data");
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, ErrorCode code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message, UUID.randomUUID(), Map.of()));
    }
}
