package com.smartparking.streaming.api;

import com.smartparking.global.error.ErrorCode;
import com.smartparking.global.error.ErrorResponse;
import com.smartparking.streaming.application.FloorEventException;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = FloorEventController.class)
public class FloorEventErrorHandler {
    @ExceptionHandler(FloorEventException.class)
    public ResponseEntity<ErrorResponse> floorNotFound(FloorEventException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
            ErrorCode.PARKING_FLOOR_NOT_FOUND, exception.getMessage(), UUID.randomUUID(), Map.of()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> invalidFloorId(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErrorResponse(
            ErrorCode.VALIDATION_FAILED, "Invalid floor ID", UUID.randomUUID(), Map.of()));
    }
}
