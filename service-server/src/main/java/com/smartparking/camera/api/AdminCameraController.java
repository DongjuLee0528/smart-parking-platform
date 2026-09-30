package com.smartparking.camera.api;

import com.smartparking.camera.application.CameraService;
import com.smartparking.camera.dto.request.CreateCameraRequest;
import com.smartparking.camera.dto.request.UpdateCameraConfigurationRequest;
import com.smartparking.camera.dto.response.CameraPage;
import com.smartparking.camera.dto.response.CameraResponse;
import com.smartparking.global.api.ApiResponse;
import com.smartparking.global.security.CurrentUserPrincipal;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/cameras")
public class AdminCameraController {
    private final CameraService service;

    public AdminCameraController(CameraService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CameraResponse>> create(
        @AuthenticationPrincipal CurrentUserPrincipal principal, @Valid @RequestBody CreateCameraRequest request
    ) {
        var result = service.create(principal.userId(), request);
        return ResponseEntity.created(URI.create("/api/v1/admin/cameras/" + result.id()))
            .body(new ApiResponse<>(result));
    }

    @PatchMapping("/{cameraId}")
    public ApiResponse<CameraResponse> update(
        @AuthenticationPrincipal CurrentUserPrincipal principal, @PathVariable UUID cameraId,
        @Valid @RequestBody UpdateCameraConfigurationRequest request
    ) {
        return new ApiResponse<>(service.update(principal.userId(), cameraId, request));
    }

    @GetMapping("/{cameraId}")
    public ApiResponse<CameraResponse> detail(@PathVariable UUID cameraId) {
        return new ApiResponse<>(service.detail(cameraId));
    }

    @GetMapping
    public ApiResponse<CameraPage> list(
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "name,asc") String sort
    ) {
        return new ApiResponse<>(service.list(page, size, sort));
    }
}
