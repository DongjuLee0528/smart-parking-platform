package com.smartparking.parkingzone.api;

import com.smartparking.global.api.ApiResponse;
import com.smartparking.global.security.CurrentUserPrincipal;
import com.smartparking.parkingzone.application.ParkingZoneService;
import com.smartparking.parkingzone.dto.request.SaveParkingZoneRequest;
import com.smartparking.parkingzone.dto.response.ParkingZoneResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class ParkingZoneController {
    private final ParkingZoneService service;

    public ParkingZoneController(ParkingZoneService service) { this.service = service; }

    @PostMapping("/api/v1/admin/floors/{floorId}/zones")
    public ResponseEntity<ApiResponse<ParkingZoneResponse>> create(
        @AuthenticationPrincipal CurrentUserPrincipal principal, @PathVariable UUID floorId,
        @Valid @RequestBody SaveParkingZoneRequest request) {
        var result = service.create(principal.userId(), floorId, request);
        return ResponseEntity.created(URI.create("/api/v1/admin/zones/" + result.id()))
            .body(new ApiResponse<>(result));
    }

    @PatchMapping("/api/v1/admin/zones/{zoneId}")
    public ApiResponse<ParkingZoneResponse> update(@AuthenticationPrincipal CurrentUserPrincipal principal,
        @PathVariable UUID zoneId, @Valid @RequestBody SaveParkingZoneRequest request) {
        return new ApiResponse<>(service.update(principal.userId(), zoneId, request));
    }
}
