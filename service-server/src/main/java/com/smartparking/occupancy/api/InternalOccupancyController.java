package com.smartparking.occupancy.api;

import com.smartparking.occupancy.application.OccupancyService;
import com.smartparking.occupancy.dto.request.AiOccupancyResultRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/occupancy-results")
public class InternalOccupancyController {
    private final OccupancyService service;

    public InternalOccupancyController(OccupancyService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ingest(@Valid @RequestBody AiOccupancyResultRequest request) {
        service.ingest(request);
    }
}
