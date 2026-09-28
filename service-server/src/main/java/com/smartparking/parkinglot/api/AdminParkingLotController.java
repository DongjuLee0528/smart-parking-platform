package com.smartparking.parkinglot.api;

import com.smartparking.global.api.ApiResponse;
import com.smartparking.global.security.CurrentUserPrincipal;
import com.smartparking.parkinglot.application.ParkingLotService;
import com.smartparking.parkinglot.dto.request.*;
import com.smartparking.parkinglot.dto.response.*;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/parking-lots")
public class AdminParkingLotController {
    private final ParkingLotService service;
    public AdminParkingLotController(ParkingLotService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ApiResponse<ParkingLotDetailResponse>> create(
        @AuthenticationPrincipal CurrentUserPrincipal principal, @Valid @RequestBody CreateParkingLotRequest request
    ) {
        var result = service.create(principal.userId(), request);
        return ResponseEntity.created(java.net.URI.create("/api/v1/admin/parking-lots/" + result.id()))
            .body(new ApiResponse<>(result));
    }

    @PatchMapping("/{lotId}")
    public ApiResponse<ParkingLotDetailResponse> update(
        @AuthenticationPrincipal CurrentUserPrincipal principal,
        @PathVariable UUID lotId, @Valid @RequestBody UpdateParkingLotRequest request
    ) {
        return new ApiResponse<>(service.update(principal.userId(), lotId, request));
    }

    @GetMapping("/{lotId}")
    public ApiResponse<ParkingLotDetailResponse> detail(@PathVariable UUID lotId) {
        return new ApiResponse<>(service.adminDetail(lotId));
    }

    @GetMapping
    public ApiResponse<ParkingLotPage> list(
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "name,asc") String sort
    ) {
        return new ApiResponse<>(service.list(page, size, sort, true));
    }
}
