package com.example.smartfactory.api.threshold;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AnomalyThresholdTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    @DisplayName("update で null でないフィールドのみ更新されること")
    void update_onlyNonNullFieldsAreUpdated() {
        AnomalyThreshold threshold = AnomalyThreshold.create(USER_ID, "temperature",
                new BigDecimal("10.0"), new BigDecimal("35.0"));

        threshold.update(null, new BigDecimal("40.0"), false);

        assertThat(threshold.getMinValue()).isEqualByComparingTo("10.0");
        assertThat(threshold.getMaxValue()).isEqualByComparingTo("40.0");
        assertThat(threshold.isEnabled()).isFalse();
        assertThat(threshold.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("update で全フィールドが null の場合は既存値が保持されること")
    void update_allNullFields_keepsExistingValues() {
        AnomalyThreshold threshold = AnomalyThreshold.create(USER_ID, "humidity",
                new BigDecimal("20.0"), new BigDecimal("80.0"));

        threshold.update(null, null, null);

        assertThat(threshold.getMinValue()).isEqualByComparingTo("20.0");
        assertThat(threshold.getMaxValue()).isEqualByComparingTo("80.0");
        assertThat(threshold.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("create で enabled は true がデフォルトであること")
    void create_enabledDefaultsToTrue() {
        AnomalyThreshold threshold = AnomalyThreshold.create(USER_ID, "power_w",
                null, new BigDecimal("1000.0"));

        assertThat(threshold.isEnabled()).isTrue();
        assertThat(threshold.getMinValue()).isNull();
        assertThat(threshold.getMaxValue()).isEqualByComparingTo("1000.0");
        assertThat(threshold.getCreatedAt()).isNotNull();
        assertThat(threshold.getUpdatedAt()).isNotNull();
    }
}
