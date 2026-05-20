package com.example.smartfactory.batch.cleanup;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "report_downloads")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportDownload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
