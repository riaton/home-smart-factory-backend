package com.example.smartfactory.batch.report;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "iot_data")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IotData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
