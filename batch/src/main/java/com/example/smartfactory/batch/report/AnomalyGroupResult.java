package com.example.smartfactory.batch.report;

import java.math.BigDecimal;

public interface AnomalyGroupResult {

    String getDeviceId();

    String getMetricType();

    long getCount();

    BigDecimal getMaxActualValue();
}
