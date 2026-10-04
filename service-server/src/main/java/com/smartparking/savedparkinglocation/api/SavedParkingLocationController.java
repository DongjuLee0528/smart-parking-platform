package com.smartparking.savedparkinglocation.api;

import com.smartparking.global.api.ApiResponse;
import com.smartparking.global.security.CurrentUserPrincipal;
import com.smartparking.savedparkinglocation.application.SavedParkingLocationService;
import com.smartparking.savedparkinglocation.dto.request.SaveParkingLocationRequest;
import com.smartparking.savedparkinglocation.dto.response.SavedParkingLocationResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/saved-parking-locations")
public class SavedParkingLocationController {
    private final SavedParkingLocationService service;

    public SavedParkingLocationController(SavedParkingLocationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SavedParkingLocationResponse>> save(
        @AuthenticationPrincipal CurrentUserPrincipal principal, @Valid @RequestBody SaveParkingLocationRequest request
    ) {
        var response = service.save(principal.userId(), request);
        return ResponseEntity.created(URI.create("/api/v1/saved-parking-locations/" + response.id()))
            .body(new ApiResponse<>(response));
    }

    @GetMapping("/active")
    public ApiResponse<SavedParkingLocationResponse> active(@AuthenticationPrincipal CurrentUserPrincipal principal) {
        return new ApiResponse<>(service.active(principal.userId()));
    }

    @DeleteMapping("/{locationId}")
    public ResponseEntity<Void> release(@AuthenticationPrincipal CurrentUserPrincipal principal,
                                        @PathVariable UUID locationId) {
        service.release(principal.userId(), locationId);
        return ResponseEntity.noContent().build();
    }
}
