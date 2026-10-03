package com.smartparking.vehicle.api;

import com.smartparking.global.api.ApiResponse;
import com.smartparking.global.security.CurrentUserPrincipal;
import com.smartparking.vehicle.application.VehicleService;
import com.smartparking.vehicle.dto.request.CreateVehicleRequest;
import com.smartparking.vehicle.dto.request.UpdateVehicleRequest;
import com.smartparking.vehicle.dto.response.VehiclePage;
import com.smartparking.vehicle.dto.response.VehicleResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {
    private final VehicleService service;

    public VehicleController(VehicleService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleResponse>> create(
        @AuthenticationPrincipal CurrentUserPrincipal principal, @Valid @RequestBody CreateVehicleRequest request
    ) {
        var response = service.create(principal.userId(), request);
        return ResponseEntity.created(URI.create("/api/v1/vehicles/" + response.id()))
            .body(new ApiResponse<>(response));
    }

    @GetMapping
    public ApiResponse<VehiclePage> list(
        @AuthenticationPrincipal CurrentUserPrincipal principal,
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,asc") String sort
    ) {
        return new ApiResponse<>(service.list(principal.userId(), page, size, sort));
    }

    @GetMapping("/{vehicleId}")
    public ApiResponse<VehicleResponse> detail(
        @AuthenticationPrincipal CurrentUserPrincipal principal, @PathVariable UUID vehicleId
    ) {
        return new ApiResponse<>(service.detail(principal.userId(), vehicleId));
    }

    @PatchMapping("/{vehicleId}")
    public ApiResponse<VehicleResponse> update(
        @AuthenticationPrincipal CurrentUserPrincipal principal, @PathVariable UUID vehicleId,
        @Valid @RequestBody UpdateVehicleRequest request
    ) {
        return new ApiResponse<>(service.update(principal.userId(), vehicleId, request));
    }

    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal CurrentUserPrincipal principal, @PathVariable UUID vehicleId,
        @RequestParam(required = false) Long version
    ) {
        service.delete(principal.userId(), vehicleId, version);
        return ResponseEntity.noContent().build();
    }
}
