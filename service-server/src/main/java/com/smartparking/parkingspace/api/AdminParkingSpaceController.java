package com.smartparking.parkingspace.api;

import com.smartparking.global.api.ApiResponse;
import com.smartparking.global.security.CurrentUserPrincipal;
import com.smartparking.parkingspace.application.ParkingSpaceService;
import com.smartparking.parkingspace.dto.request.SaveParkingSpacesRequest;
import com.smartparking.parkingspace.dto.response.ParkingSpacesResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/floors/{floorId}/parking-spaces")
public class AdminParkingSpaceController {
    private final ParkingSpaceService service;

    public AdminParkingSpaceController(ParkingSpaceService service) {
        this.service = service;
    }

    @PutMapping
    public ApiResponse<ParkingSpacesResponse> save(
        @AuthenticationPrincipal CurrentUserPrincipal principal, @PathVariable UUID floorId,
        @Valid @RequestBody SaveParkingSpacesRequest request
    ) {
        return new ApiResponse<>(service.save(principal.userId(), floorId, request));
    }

    @GetMapping
    public ApiResponse<ParkingSpacesResponse> detail(@PathVariable UUID floorId) {
        return new ApiResponse<>(service.detail(floorId));
    }
}
