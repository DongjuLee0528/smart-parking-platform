package com.smartparking.parkingfloor.api;

import com.smartparking.global.api.ApiResponse;
import com.smartparking.global.security.CurrentUserPrincipal;
import com.smartparking.parkingfloor.application.ParkingFloorService;
import com.smartparking.parkingfloor.dto.request.SaveParkingFloorRequest;
import com.smartparking.parkingfloor.dto.response.ParkingFloorResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class ParkingFloorController {
    private final ParkingFloorService service;

    public ParkingFloorController(ParkingFloorService service) { this.service = service; }

    @GetMapping("/api/v1/parking-lots/{lotId}/floors")
    public ApiResponse<List<ParkingFloorResponse>> publicList(@PathVariable UUID lotId) {
        return new ApiResponse<>(service.list(lotId, false));
    }

    @GetMapping("/api/v1/admin/parking-lots/{lotId}/floors")
    public ApiResponse<List<ParkingFloorResponse>> adminList(@PathVariable UUID lotId) {
        return new ApiResponse<>(service.list(lotId, true));
    }

    @PostMapping("/api/v1/admin/parking-lots/{lotId}/floors")
    public ResponseEntity<ApiResponse<ParkingFloorResponse>> create(
        @AuthenticationPrincipal CurrentUserPrincipal principal, @PathVariable UUID lotId,
        @Valid @RequestBody SaveParkingFloorRequest request) {
        var result = service.create(principal.userId(), lotId, request);
        return ResponseEntity.created(URI.create("/api/v1/admin/floors/" + result.id()))
            .body(new ApiResponse<>(result));
    }

    @PatchMapping("/api/v1/admin/floors/{floorId}")
    public ApiResponse<ParkingFloorResponse> update(@AuthenticationPrincipal CurrentUserPrincipal principal,
        @PathVariable UUID floorId, @Valid @RequestBody SaveParkingFloorRequest request) {
        return new ApiResponse<>(service.update(principal.userId(), floorId, request));
    }
}
