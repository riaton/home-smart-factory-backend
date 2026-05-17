package com.example.smartfactory.api.iotdata;

import com.example.smartfactory.api.iotdata.dto.IotDataResponse;
import com.example.smartfactory.common.response.PagedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class IotDataServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private IotDataRepository iotDataRepository;

    private IotDataService iotDataService;

    @BeforeEach
    void setup() {
        iotDataService = new IotDataService(iotDataRepository);
    }

    private IotData mockIotData() {
        IotData d = mock(IotData.class);
        given(d.getId()).willReturn(1L);
        given(d.getDeviceId()).willReturn("room01");
        given(d.getTemperature()).willReturn(new BigDecimal("25.30"));
        given(d.getHumidity()).willReturn(new BigDecimal("60.10"));
        given(d.getMotion()).willReturn(1);
        given(d.getPowerW()).willReturn(new BigDecimal("120.50"));
        given(d.getRecordedAt()).willReturn(OffsetDateTime.parse("2026-01-15T01:00:00Z"));
        return d;
    }

    @Test
    @DisplayName("findAll はリポジトリの結果を PagedResponse に変換して返すこと")
    void findAll_returnsPagedResponse() {
        OffsetDateTime from = OffsetDateTime.parse("2026-01-15T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-01-15T23:59:59Z");
        Page<IotData> page = new PageImpl<>(List.of(mockIotData()), PageRequest.of(0, 100), 1);
        given(iotDataRepository.findByFilter(eq(USER_ID), eq(null), eq(from), eq(to), any()))
                .willReturn(page);

        PagedResponse<IotDataResponse> result = iotDataService.findAll(USER_ID, null, from, to, 1, 100);

        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).deviceId()).isEqualTo("room01");
        assertThat(result.data().get(0).temperature()).isEqualByComparingTo("25.30");
        assertThat(result.pagination().total()).isEqualTo(1);
        assertThat(result.pagination().page()).isEqualTo(1);
        assertThat(result.pagination().perPage()).isEqualTo(100);
    }

    @Test
    @DisplayName("page パラメータは 0-indexed に変換して Pageable に渡されること")
    void findAll_convertsPageToZeroIndexed() {
        OffsetDateTime from = OffsetDateTime.parse("2026-01-15T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-01-15T23:59:59Z");
        Page<IotData> emptyPage = new PageImpl<>(List.of(), PageRequest.of(1, 50), 0);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        given(iotDataRepository.findByFilter(eq(USER_ID), eq(null), eq(from), eq(to),
                pageableCaptor.capture())).willReturn(emptyPage);

        iotDataService.findAll(USER_ID, null, from, to, 2, 50);

        Pageable captured = pageableCaptor.getValue();
        assertThat(captured.getPageNumber()).isEqualTo(1);
        assertThat(captured.getPageSize()).isEqualTo(50);
    }

    @Test
    @DisplayName("per_page が 1000 を超えた場合は 1000 にクランプされること")
    void findAll_clampsPerPageToMax() {
        OffsetDateTime from = OffsetDateTime.parse("2026-01-15T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-01-15T23:59:59Z");
        Page<IotData> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 1000), 0);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        given(iotDataRepository.findByFilter(eq(USER_ID), eq(null), eq(from), eq(to),
                pageableCaptor.capture())).willReturn(emptyPage);

        iotDataService.findAll(USER_ID, null, from, to, 1, 5000);

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1000);
    }

    @Test
    @DisplayName("page が 0 以下の場合 IllegalArgumentException をスローすること")
    void findAll_pageZero_throwsIllegalArgumentException() {
        OffsetDateTime from = OffsetDateTime.parse("2026-01-15T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-01-15T23:59:59Z");

        assertThatThrownBy(() -> iotDataService.findAll(USER_ID, null, from, to, 0, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("page");
    }

    @Test
    @DisplayName("device_id フィルタが指定された場合はリポジトリに渡されること")
    void findAll_passesDeviceIdFilter() {
        OffsetDateTime from = OffsetDateTime.parse("2026-01-15T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-01-15T23:59:59Z");
        Page<IotData> page = new PageImpl<>(List.of(mockIotData()), PageRequest.of(0, 100), 1);
        given(iotDataRepository.findByFilter(eq(USER_ID), eq("room01"), eq(from), eq(to), any()))
                .willReturn(page);

        PagedResponse<IotDataResponse> result = iotDataService.findAll(USER_ID, "room01", from, to, 1, 100);

        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).deviceId()).isEqualTo("room01");
    }
}
