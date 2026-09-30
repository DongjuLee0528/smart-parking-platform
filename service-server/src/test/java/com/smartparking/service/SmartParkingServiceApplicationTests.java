package com.smartparking.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartparking.camera.domain.Camera;
import com.smartparking.camera.domain.CameraStatus;
import com.smartparking.camera.dto.response.CameraResponse;
import com.smartparking.camera.infrastructure.CameraRepository;
import org.springframework.data.domain.PageRequest;
import com.smartparking.parkinglot.domain.ParkingLot;
import com.smartparking.parkinglot.dto.request.CreateParkingLotRequest;
import com.smartparking.parkinglot.dto.response.ParkingLotDetailResponse;
import com.smartparking.parkinglot.infrastructure.ParkingLotRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import com.smartparking.global.security.FirebaseTokenVerifier;

@SpringBootTest
@ActiveProfiles("test")
class SmartParkingServiceApplicationTests {

    @MockitoBean
    FirebaseTokenVerifier firebaseTokenVerifier;

    @Autowired
    ParkingLotRepository parkingLotRepository;

    @Autowired
    CameraRepository cameraRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    void contextLoads() {
        assertThat(firebaseTokenVerifier).isNotNull();
    }

    @Test
    @Transactional
    void parkingLotCoordinatesAndFloorsRoundTripThroughH2() {
        var request = new CreateParkingLotRequest("Lot", "Address", 37.5, 127.0, "", "",
            List.of(new CreateParkingLotRequest.Floor("B1", -1, List.of("A"))));
        var saved = parkingLotRepository.saveAndFlush(new ParkingLot(request));
        var id = saved.getId();
        entityManager.clear();

        var result = ParkingLotDetailResponse.from(parkingLotRepository.findById(id).orElseThrow());
        assertThat(result.latitude()).isEqualTo(37.5);
        assertThat(result.longitude()).isEqualTo(127.0);
        assertThat(result.floors()).hasSize(1);
        assertThat(result.floors().get(0).zones()).hasSize(1);
    }

    @Test
    @Transactional
    void cameraAndZoneAssociationRoundTripThroughH2() {
        var request = new CreateParkingLotRequest("Camera lot", "Address", 37.5, 127.0, "", "",
            List.of(new CreateParkingLotRequest.Floor("B1", -1, List.of("A"))));
        var lot = parkingLotRepository.saveAndFlush(new ParkingLot(request));
        var camera = cameraRepository.saveAndFlush(new Camera(lot.getFloors().get(0).getZones().get(0),
            "Entrance", "CAMERA_ENTRANCE_RTSP"));
        var cameraId = camera.getId();
        var lotId = lot.getId();
        entityManager.clear();

        var result = CameraResponse.from(cameraRepository.findById(cameraId).orElseThrow());
        assertThat(result.parkingLotId()).isEqualTo(lotId);
        assertThat(result.floorId()).isNotNull();
        assertThat(result.zoneId()).isNotNull();
        assertThat(result.streamKeyRef()).isEqualTo("CAMERA_ENTRANCE_RTSP");
        assertThat(result.status()).isEqualTo(CameraStatus.OFFLINE);
        assertThat(result.configVersion()).isZero();
    }

    @Test
    @Transactional
    void cameraPageLoadsAssociatedLocationsWithBoundedQueries() {
        for (int index = 0; index < 2; index++) {
            var request = new CreateParkingLotRequest("Camera lot " + index, "Address", 37.5, 127.0, "", "",
                List.of(new CreateParkingLotRequest.Floor("B1", -1, List.of("A"))));
            var lot = parkingLotRepository.saveAndFlush(new ParkingLot(request));
            cameraRepository.saveAndFlush(new Camera(lot.getFloors().get(0).getZones().get(0),
                "Entrance " + index, "CAMERA_ENTRANCE_RTSP"));
        }
        entityManager.clear();
        var statistics = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        var results = cameraRepository.findAll(PageRequest.of(0, 100)).map(CameraResponse::from);

        assertThat(results.getContent()).hasSize(2);
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
    }
}
