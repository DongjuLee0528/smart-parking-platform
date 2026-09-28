package com.smartparking.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartparking.parkinglot.domain.ParkingLot;
import com.smartparking.parkinglot.dto.request.CreateParkingLotRequest;
import com.smartparking.parkinglot.dto.response.ParkingLotDetailResponse;
import com.smartparking.parkinglot.infrastructure.ParkingLotRepository;
import jakarta.persistence.EntityManager;
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
}
