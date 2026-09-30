package com.smartparking.camera.api;

import com.smartparking.camera.application.CameraException;
import com.smartparking.global.error.ErrorCode;
import com.smartparking.global.error.ErrorResponse;
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

@RestControllerAdvice(assignableTypes = AdminCameraController.class)
public class CameraErrorHandler {
    @ExceptionHandler(CameraException.class)
    public ResponseEntity<ErrorResponse> domain(CameraException exception) {
        var status = switch (exception.getCode()) {
            case CAMERA_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case VERSION_CONFLICT, CAMERA_CONFLICT -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return response(status, exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> validation(Exception exception) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.VALIDATION_FAILED, "Invalid camera request");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> version(OptimisticLockingFailureException exception) {
        return response(HttpStatus.CONFLICT, ErrorCode.VERSION_CONFLICT, "Camera configuration version has changed");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException exception) {
        return response(HttpStatus.CONFLICT, ErrorCode.CAMERA_CONFLICT, "Camera data conflicts with existing data");
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, ErrorCode code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message, UUID.randomUUID(), Map.of()));
    }
}
