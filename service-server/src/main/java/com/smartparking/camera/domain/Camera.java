package com.smartparking.camera.domain;

import com.smartparking.parkingzone.domain.ParkingZone;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cameras")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Camera {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "zone_id", nullable = false)
    private ParkingZone zone;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(name = "stream_key_ref", nullable = false, length = 128)
    private String streamKeyRef;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private CameraStatus status = CameraStatus.OFFLINE;
    @Column(name = "last_frame_at")
    private Instant lastFrameAt;
    @Version @Column(name = "config_version", nullable = false)
    private Long configVersion;

    public Camera(ParkingZone zone, String name, String streamKeyRef) {
        configure(zone, name, streamKeyRef);
    }

    public void configure(ParkingZone zone, String name, String streamKeyRef) {
        this.zone = zone;
        this.name = name.strip();
        this.streamKeyRef = streamKeyRef;
        this.status = CameraStatus.OFFLINE;
        this.lastFrameAt = null;
    }
}
