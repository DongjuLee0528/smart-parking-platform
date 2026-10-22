package com.smartparking.floormap.api;

import com.smartparking.floormap.application.FloorMapException;
import com.smartparking.global.error.ErrorCode;
import com.smartparking.global.error.ErrorResponse;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = {FloorMapController.class, AdminFloorMapController.class})
public class FloorMapErrorHandler {
    @ExceptionHandler(FloorMapException.class)
    public ResponseEntity<ErrorResponse> domain(FloorMapException exception) {
        var status = switch (exception.getCode()) {
            case PARKING_FLOOR_NOT_FOUND, MAP_DRAWING_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case VERSION_CONFLICT -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(status).body(new ErrorResponse(
            exception.getCode(), exception.getMessage(), UUID.randomUUID(), Map.of()));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
        MissingServletRequestPartException.class, MultipartException.class})
    public ResponseEntity<ErrorResponse> invalidRequest(Exception exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErrorResponse(
            ErrorCode.VALIDATION_FAILED, "Invalid floor map request", UUID.randomUUID(), Map.of()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> oversized(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErrorResponse(
            ErrorCode.VALIDATION_FAILED, "Floor drawing exceeds upload limit", UUID.randomUUID(), Map.of()));
    }
}
