package com.example.smartfactory.batch.report;

import java.math.BigDecimal;

public interface IotDataAggregate {

    BigDecimal getAvgTemperature();

    BigDecimal getAvgHumidity();

    BigDecimal getTotalPowerKwh();

    Long getTotalMotionMinutes();
}
