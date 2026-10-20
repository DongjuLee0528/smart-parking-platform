package com.smartparking.floormap.api;

import com.smartparking.floormap.application.FloorMapService;
import com.smartparking.floormap.dto.response.MapVersionResponse;
import com.smartparking.global.api.ApiResponse;
import com.smartparking.global.security.CurrentUserPrincipal;
import java.io.IOException;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/floors/{floorId}/map")
public class AdminFloorMapController {
    private final FloorMapService service;

    public AdminFloorMapController(FloorMapService service) {
        this.service = service;
    }

    @PutMapping(consumes = "multipart/form-data")
    public ApiResponse<MapVersionResponse> save(
        @AuthenticationPrincipal CurrentUserPrincipal principal, @PathVariable UUID floorId,
        @RequestParam long expectedVersion, @RequestPart("file") MultipartFile file
    ) throws IOException {
        return new ApiResponse<>(service.saveDrawing(principal.userId(), floorId, expectedVersion, file.getBytes()));
    }
}
