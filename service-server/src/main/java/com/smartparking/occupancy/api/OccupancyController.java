package com.smartparking.occupancy.api;

import com.smartparking.global.api.ApiResponse;
import com.smartparking.occupancy.application.OccupancyService;
import com.smartparking.occupancy.dto.response.OccupancyStateResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/floors/{floorId}/occupancy")
public class OccupancyController {
    private final OccupancyService service;

    public OccupancyController(OccupancyService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<OccupancyStateResponse>> floorSnapshot(@PathVariable UUID floorId) {
        return new ApiResponse<>(service.floorSnapshot(floorId));
    }
}
