package com.smartparking.parkingzone.domain;

import com.smartparking.parkingfloor.domain.ParkingFloor;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "parking_zones", uniqueConstraints = @UniqueConstraint(columnNames = {"floor_id", "name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParkingZone {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "floor_id", nullable = false)
    private ParkingFloor floor;
    @Column(nullable = false, length = 100)
    private String name;

    public ParkingZone(ParkingFloor floor, String name) {
        this.floor = floor;
        this.name = name.strip();
    }
}
