package com.smartparking.parkingfloor.domain;

import com.smartparking.parkinglot.domain.ParkingLot;
import com.smartparking.parkinglot.dto.request.CreateParkingLotRequest;
import com.smartparking.parkingzone.domain.ParkingZone;
import jakarta.persistence.*;
import java.util.*;
import lombok.*;

@Entity
@Table(name = "parking_floors", uniqueConstraints = @UniqueConstraint(columnNames = {"parking_lot_id", "name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParkingFloor {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parking_lot_id", nullable = false)
    private ParkingLot parkingLot;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(name = "floor_order", nullable = false)
    private int floorOrder;
    @OneToMany(mappedBy = "floor", cascade = CascadeType.PERSIST)
    @OrderBy("name ASC")
    private List<ParkingZone> zones = new ArrayList<>();

    public ParkingFloor(ParkingLot lot, CreateParkingLotRequest.Floor request) {
        parkingLot = lot;
        name = request.name().strip();
        floorOrder = request.floorOrder();
        request.zones().forEach(zone -> zones.add(new ParkingZone(this, zone)));
    }
}
