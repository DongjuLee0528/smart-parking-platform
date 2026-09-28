package com.smartparking.parkinglot.api;

import com.smartparking.global.api.ApiResponse;
import com.smartparking.parkinglot.application.ParkingLotService;
import com.smartparking.parkinglot.dto.response.*;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/parking-lots")
public class ParkingLotController {
    private final ParkingLotService service;
    public ParkingLotController(ParkingLotService service) { this.service = service; }

    @GetMapping("/{lotId}")
    public ApiResponse<ParkingLotDetailResponse> detail(@PathVariable UUID lotId) {
        return new ApiResponse<>(service.detail(lotId));
    }

    @GetMapping
    public ApiResponse<ParkingLotPage> list(
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "name,asc") String sort
    ) {
        return new ApiResponse<>(service.list(page, size, sort, false));
    }
}
