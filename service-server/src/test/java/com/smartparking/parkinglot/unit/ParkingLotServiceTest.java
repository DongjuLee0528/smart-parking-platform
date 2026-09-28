package com.smartparking.parkinglot.unit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.smartparking.audit.domain.AuditLog;
import com.smartparking.audit.infrastructure.AuditLogRepository;
import com.smartparking.global.error.ErrorCode;
import com.smartparking.parkinglot.application.*;
import com.smartparking.parkinglot.domain.*;
import com.smartparking.parkinglot.dto.request.*;
import com.smartparking.parkinglot.infrastructure.ParkingLotRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

class ParkingLotServiceTest {
    private final ParkingLotRepository repository = mock(ParkingLotRepository.class);
    private final AuditLogRepository audits = mock(AuditLogRepository.class);
    private final ParkingLotService service = new ParkingLotService(repository, audits);
    private final UUID actor = UUID.randomUUID();
    private final UUID id = UUID.randomUUID();

    private CreateParkingLotRequest request() {
        return new CreateParkingLotRequest("Manual lot", "Sample address", 37.5, 127.0,
            "09:00-18:00", "Hourly rate", List.of(
                new CreateParkingLotRequest.Floor("B1", -1, List.of("A", "B"))));
    }

    private ParkingLot lot() {
        var lot = new ParkingLot(request());
        ReflectionTestUtils.setField(lot, "id", id);
        ReflectionTestUtils.setField(lot, "version", 0L);
        return lot;
    }

    @Test
    void createsInactiveDraftWithFloorsAndTransactionalAudit() {
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            var lot = invocation.getArgument(0);
            ReflectionTestUtils.setField(lot, "id", id);
            ReflectionTestUtils.setField(lot, "version", 0L);
            return lot;
        });
        var result = service.create(actor, request());
        assertThat(result.operationStatus()).isEqualTo(OperationStatus.INACTIVE);
        assertThat(result.setupStatus()).isEqualTo(SetupStatus.DRAFT);
        assertThat(result.latitude()).isEqualTo(37.5);
        assertThat(result.longitude()).isEqualTo(127.0);
        assertThat(result.floors().get(0).zones()).hasSize(2);
        var audit = ArgumentCaptor.forClass(AuditLog.class);
        verify(audits).save(audit.capture());
        assertThat(audit.getValue().getActorId()).isEqualTo(actor);
        assertThat(audit.getValue().getBeforeJson()).isNull();
        assertThat(audit.getValue().getAfterJson()).contains("Manual lot");
    }

    @Test
    void updatesDetailsPreservingStateAndFloorIdentityAndRecordsBeforeAfter() {
        var lot = lot();
        when(repository.findById(id)).thenReturn(Optional.of(lot));
        doAnswer(invocation -> { ReflectionTestUtils.setField(lot, "version", 1L); return null; })
            .when(repository).flush();
        var result = service.update(actor, id,
            new UpdateParkingLotRequest(0L, "Updated", "New address", 38.0, 128.0, "Always", "Free"));
        assertThat(result.name()).isEqualTo("Updated");
        assertThat(result.version()).isEqualTo(1L);
        assertThat(result.operationStatus()).isEqualTo(OperationStatus.INACTIVE);
        assertThat(result.floors()).hasSize(1);
        var audit = ArgumentCaptor.forClass(AuditLog.class);
        verify(audits).save(audit.capture());
        assertThat(audit.getValue().getBeforeJson()).contains("Manual lot");
        assertThat(audit.getValue().getAfterJson()).contains("Updated");
    }

    @Test
    void rejectsStaleVersionWithoutMutationOrAudit() {
        var lot = lot();
        when(repository.findById(id)).thenReturn(Optional.of(lot));
        assertThatThrownBy(() -> service.update(actor, id,
            new UpdateParkingLotRequest(3L, "Updated", "Address", 0.0, 0.0, "", "")))
            .isInstanceOfSatisfying(ParkingLotException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.VERSION_CONFLICT));
        assertThat(lot.getName()).isEqualTo("Manual lot");
        verify(repository, never()).flush();
        verifyNoInteractions(audits);
    }

    @Test
    void returnsNotFoundForUnknownOrInactivePublicLot() {
        when(repository.findByIdAndOperationStatus(id, OperationStatus.ACTIVE)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.detail(id)).isInstanceOf(ParkingLotException.class);
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.adminDetail(id)).isInstanceOf(ParkingLotException.class);
    }

    @Test
    void publicListQueriesOnlyActiveLotsWithBoundedPaging() {
        when(repository.findByOperationStatus(eq(OperationStatus.ACTIVE), any())).thenReturn(Page.empty());
        assertThat(service.list(0, 20, "name,asc", false).items()).isEmpty();
        verify(repository).findByOperationStatus(eq(OperationStatus.ACTIVE), any(Pageable.class));
        assertThatThrownBy(() -> service.list(0, 101, "name,asc", false)).isInstanceOf(ParkingLotException.class);
        assertThatThrownBy(() -> service.list(-1, 20, "name,asc", false)).isInstanceOf(ParkingLotException.class);
        assertThatThrownBy(() -> service.list(0, 20, "location,asc", false)).isInstanceOf(ParkingLotException.class);
    }

    @Test
    void rejectsDuplicateNormalizedFloorAndZoneNames() {
        var duplicate = new CreateParkingLotRequest("Lot", "Address", 0.0, 0.0, "", "",
            List.of(new CreateParkingLotRequest.Floor("B1", -1, List.of("A", " A "))));
        assertThatThrownBy(() -> service.create(actor, duplicate)).isInstanceOf(ParkingLotException.class);
        verifyNoInteractions(repository, audits);
    }
}
