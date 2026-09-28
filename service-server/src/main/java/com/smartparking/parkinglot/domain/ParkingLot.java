package com.smartparking.parkinglot.domain;

import com.smartparking.parkingfloor.domain.ParkingFloor;
import com.smartparking.parkinglot.dto.request.CreateParkingLotRequest;
import com.smartparking.parkinglot.dto.request.UpdateParkingLotRequest;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.*;

@Entity
@Table(name = "parking_lots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParkingLot {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, length = 200)
    private String name;
    @Column(nullable = false, length = 500)
    private String address;
    @JdbcTypeCode(SqlTypes.GEOGRAPHY)
    @Column(nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;
    @Column(name = "operating_hours", nullable = false, length = 2000)
    private String operatingHours;
    @Column(name = "fee_information", nullable = false, length = 2000)
    private String feeInformation;
    @Enumerated(EnumType.STRING) @Column(name = "operation_status", nullable = false)
    private OperationStatus operationStatus = OperationStatus.INACTIVE;
    @Enumerated(EnumType.STRING) @Column(name = "setup_status", nullable = false)
    private SetupStatus setupStatus = SetupStatus.DRAFT;
    @Version @Column(nullable = false)
    private Long version;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @OneToMany(mappedBy = "parkingLot", cascade = CascadeType.PERSIST)
    @OrderBy("floorOrder ASC, name ASC")
    private List<ParkingFloor> floors = new ArrayList<>();

    public ParkingLot(CreateParkingLotRequest request) {
        setDetails(request.name(), request.address(), request.latitude(), request.longitude(),
            request.operatingHours(), request.feeInformation());
        createdAt = updatedAt;
        request.floors().forEach(floor -> floors.add(new ParkingFloor(this, floor)));
    }

    public void update(UpdateParkingLotRequest request) {
        setDetails(request.name(), request.address(), request.latitude(), request.longitude(),
            request.operatingHours(), request.feeInformation());
    }

    private void setDetails(String name, String address, double latitude, double longitude,
                            String operatingHours, String feeInformation) {
        this.name = name.strip();
        this.address = address.strip();
        this.location = new GeometryFactory(new PrecisionModel(), 4326)
            .createPoint(new Coordinate(longitude, latitude));
        this.operatingHours = operatingHours;
        this.feeInformation = feeInformation;
        this.updatedAt = Instant.now();
    }
}
