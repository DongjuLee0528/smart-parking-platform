package com.smartparking.parkingspace.domain;

import com.smartparking.camera.domain.CameraSpaceMapping;
import com.smartparking.parkingzone.domain.ParkingZone;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "parking_spaces", uniqueConstraints = @UniqueConstraint(columnNames = {"zone_id", "space_number"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParkingSpace {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "zone_id", nullable = false)
    private ParkingZone zone;
    @Column(name = "space_number", nullable = false, length = 40)
    private String spaceNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ParkingSpaceType type;
    @Column(name = "map_polygon", nullable = false, columnDefinition = "text")
    private String mapPolygon;
    @Column(nullable = false)
    private boolean active;
    @ElementCollection
    @CollectionTable(name = "camera_space_mappings", joinColumns = @JoinColumn(name = "parking_space_id"))
    private List<CameraSpaceMapping> cameraMappings = new ArrayList<>();

    public ParkingSpace(ParkingZone zone, String spaceNumber, ParkingSpaceType type,
                        String mapPolygon, boolean active, List<CameraSpaceMapping> cameraMappings) {
        configure(zone, spaceNumber, type, mapPolygon, active, cameraMappings);
    }

    public void configure(ParkingZone zone, String spaceNumber, ParkingSpaceType type,
                          String mapPolygon, boolean active, List<CameraSpaceMapping> cameraMappings) {
        this.zone = zone;
        this.spaceNumber = spaceNumber.strip();
        this.type = type;
        this.mapPolygon = mapPolygon;
        this.active = active;
        this.cameraMappings.clear();
        this.cameraMappings.addAll(cameraMappings);
    }

    public void deactivate() {
        active = false;
    }
}
