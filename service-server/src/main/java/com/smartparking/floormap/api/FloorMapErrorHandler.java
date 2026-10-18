package com.smartparking.floormap.api;

import com.smartparking.floormap.application.FloorMapException;
import com.smartparking.global.error.ErrorCode;
import com.smartparking.global.error.ErrorResponse;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = FloorMapController.class)
public class FloorMapErrorHandler {
    @ExceptionHandler(FloorMapException.class)
    public ResponseEntity<ErrorResponse> floorNotFound(FloorMapException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
            exception.getCode(), exception.getMessage(), UUID.randomUUID(), Map.of()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> invalidFloorId(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErrorResponse(
            ErrorCode.VALIDATION_FAILED, "Invalid floor ID", UUID.randomUUID(), Map.of()));
    }
}
