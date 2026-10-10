package com.smartparking.streaming.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockConstruction;

import com.smartparking.occupancy.domain.OccupancyState;
import com.smartparking.occupancy.application.OccupancyChangedEvent;
import com.smartparking.occupancy.dto.response.OccupancyStateResponse;
import com.smartparking.streaming.application.FloorEventStreamService;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class FloorEventStreamServiceTest {
    @Test
    void stalledClientOnOneFloorDoesNotBlockAnotherFloor() throws Exception {
        var service = new FloorEventStreamService();
        var floorA = UUID.randomUUID();
        var floorB = UUID.randomUUID();
        var blocked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try (var emitters = mockConstruction(SseEmitter.class)) {
            service.subscribe(floorA, null);
            service.subscribe(floorB, null);
            doAnswer(invocation -> {
                blocked.countDown();
                assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
                return null;
            }).when(emitters.constructed().get(0)).send(any(SseEmitter.SseEventBuilder.class));

            var first = executor.submit(() -> service.publish(event(floorA)));
            try {
                assertThat(blocked.await(2, TimeUnit.SECONDS)).isTrue();
                executor.submit(() -> service.publish(event(floorB))).get(2, TimeUnit.SECONDS);
            } finally {
                release.countDown();
            }
            first.get(2, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    private OccupancyChangedEvent event(UUID floorId) {
        return new OccupancyChangedEvent(floorId, new OccupancyStateResponse(UUID.randomUUID(), UUID.randomUUID(),
            "A-01", true, OccupancyState.OCCUPIED, 0.9, Instant.parse("2026-10-01T00:00:00Z")));
    }
}
