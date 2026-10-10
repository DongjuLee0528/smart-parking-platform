package com.smartparking.streaming.application;

import com.smartparking.occupancy.application.OccupancyChangedEvent;
import com.smartparking.streaming.domain.FloorEventType;
import com.smartparking.streaming.dto.response.FloorEventResponse;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class FloorEventStreamService {
    private static final int HISTORY_LIMIT = 256;
    private static final long STREAM_TIMEOUT_MILLIS = 30 * 60 * 1000L;
    private final Map<UUID, List<SseEmitter>> subscribers = new ConcurrentHashMap<>();
    // shortcut: replay is process-local and bounded; use shared durable events when running multiple instances.
    private final Deque<PublishedEvent> history = new ArrayDeque<>();

    public SseEmitter subscribe(UUID floorId, String lastEventId) {
        var emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        var floorSubscribers = subscribers.computeIfAbsent(floorId, ignored -> new ArrayList<>());
        synchronized (floorSubscribers) {
            floorSubscribers.add(emitter);
            emitter.onCompletion(() -> remove(floorId, emitter));
            emitter.onTimeout(() -> remove(floorId, emitter));
            emitter.onError(ignored -> remove(floorId, emitter));

            var missed = new ArrayList<PublishedEvent>();
            boolean found = false;
            if (lastEventId != null) {
                synchronized (history) {
                    for (var event : history) {
                        if (event.id().equals(lastEventId) && event.response().floorId().equals(floorId)) {
                            found = true;
                        } else if (found && event.response().floorId().equals(floorId)) {
                            missed.add(event);
                        }
                    }
                }
            }
            try {
                if (!found) {
                    emitter.send(SseEmitter.event().name(FloorEventType.FLOOR_SNAPSHOT_REQUIRED.name())
                        .data(new FloorEventResponse(FloorEventType.FLOOR_SNAPSHOT_REQUIRED, floorId, null)));
                } else {
                    for (var event : missed) {
                        send(emitter, event);
                    }
                }
            } catch (IOException | IllegalStateException exception) {
                remove(floorId, emitter);
                emitter.completeWithError(exception);
            }
        }
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(OccupancyChangedEvent event) {
        var published = new PublishedEvent(UUID.randomUUID().toString(),
            new FloorEventResponse(FloorEventType.OCCUPANCY_CHANGED, event.floorId(), event.occupancy()));
        var floorSubscribers = subscribers.computeIfAbsent(event.floorId(), ignored -> new ArrayList<>());
        synchronized (floorSubscribers) {
            synchronized (history) {
                history.addLast(published);
                if (history.size() > HISTORY_LIMIT) {
                    history.removeFirst();
                }
            }
            for (var emitter : List.copyOf(floorSubscribers)) {
                try {
                    send(emitter, published);
                } catch (IOException | IllegalStateException exception) {
                    remove(event.floorId(), emitter);
                    emitter.completeWithError(exception);
                }
            }
        }
    }

    private void send(SseEmitter emitter, PublishedEvent event) throws IOException {
        emitter.send(SseEmitter.event().id(event.id()).name(event.response().type().name())
            .data(event.response()));
    }

    private void remove(UUID floorId, SseEmitter emitter) {
        var floorSubscribers = subscribers.get(floorId);
        if (floorSubscribers != null) {
            synchronized (floorSubscribers) {
                floorSubscribers.remove(emitter);
            }
        }
    }

    private record PublishedEvent(String id, FloorEventResponse response) {}
}
