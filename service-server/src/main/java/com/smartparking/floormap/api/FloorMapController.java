package com.smartparking.floormap.api;

import com.smartparking.floormap.application.FloorMapService;
import com.smartparking.floormap.dto.response.FloorMapSnapshotResponse;
import com.smartparking.global.api.ApiResponse;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/floors/{floorId}/map")
public class FloorMapController {
    private final FloorMapService service;

    public FloorMapController(FloorMapService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<FloorMapSnapshotResponse> snapshot(@PathVariable UUID floorId) {
        return new ApiResponse<>(service.snapshot(floorId));
    }

    @GetMapping("/drawing")
    public ResponseEntity<byte[]> drawing(@PathVariable UUID floorId) {
        var drawing = service.drawing(floorId);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(drawing.getMediaType()))
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .header("X-Content-Type-Options", "nosniff")
            .header("Content-Security-Policy", "sandbox; default-src 'none'")
            .body(drawing.getContent());
    }
}
