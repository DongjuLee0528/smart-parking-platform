package com.smartparking.streaming.api;

import com.smartparking.parkingfloor.infrastructure.ParkingFloorRepository;
import com.smartparking.parkinglot.domain.OperationStatus;
import com.smartparking.global.security.CurrentUserPrincipal;
import com.smartparking.global.security.Role;
import com.smartparking.streaming.application.FloorEventException;
import com.smartparking.streaming.application.FloorEventStreamService;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/v1/floors/{floorId}/events")
public class FloorEventController {
    private final ParkingFloorRepository floors;
    private final FloorEventStreamService stream;

    public FloorEventController(ParkingFloorRepository floors, FloorEventStreamService stream) {
        this.floors = floors;
        this.stream = stream;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Transactional(readOnly = true)
    public SseEmitter events(@PathVariable UUID floorId,
                             @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId,
                             @AuthenticationPrincipal CurrentUserPrincipal principal) {
        if (floors.findById(floorId).filter(floor -> floor.isActive() &&
            (principal.role() == Role.ADMIN || floor.getParkingLot().getOperationStatus() == OperationStatus.ACTIVE))
            .isEmpty()) {
            throw new FloorEventException("Parking floor not found");
        }
        return stream.subscribe(floorId, lastEventId);
    }
}
