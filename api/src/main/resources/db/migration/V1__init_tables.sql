-- Home Smart Factory — 初期テーブル作成
-- DB設計書 (basic-design/DB設計書.md) に準拠

-- 1. users（ユーザー）
CREATE TABLE users (
    id         UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    google_id  VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_users_google_id UNIQUE (google_id)
);

CREATE INDEX idx_users_email ON users (email);

-- 2. devices（デバイス）
CREATE TABLE devices (
    id         UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    device_id  VARCHAR(100) NOT NULL,
    user_id    UUID         NOT NULL,
    name       VARCHAR(255),
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_devices_device_id UNIQUE (device_id),
    CONSTRAINT fk_devices_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_devices_user_id ON devices (user_id);

-- 3. iot_data（IoTデータ）
-- UNIQUE(device_id, recorded_at): ECS Worker クラッシュ時の再処理による重複INSERT防止
CREATE TABLE iot_data (
    id          BIGSERIAL    NOT NULL PRIMARY KEY,
    device_id   VARCHAR(100) NOT NULL,
    user_id     UUID         NOT NULL,
    temperature DECIMAL(5, 2),
    humidity    DECIMAL(5, 2),
    motion      SMALLINT,
    power_w     DECIMAL(8, 2),
    recorded_at TIMESTAMPTZ  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_iot_data_device_recorded UNIQUE (device_id, recorded_at),
    CONSTRAINT fk_iot_data_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_iot_data_device_recorded ON iot_data (device_id, recorded_at);
CREATE INDEX idx_iot_data_user_recorded ON iot_data (user_id, recorded_at);

-- 4. anomaly_thresholds（異常検知閾値設定）
CREATE TABLE anomaly_thresholds (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID         NOT NULL,
    metric_type VARCHAR(50)  NOT NULL,
    min_value   DECIMAL(8, 2),
    max_value   DECIMAL(8, 2),
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_anomaly_thresholds_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_anomaly_thresholds_user_metric ON anomaly_thresholds (user_id, metric_type);

-- 5. anomaly_logs（異常検知ログ）
CREATE TABLE anomaly_logs (
    id              BIGSERIAL    NOT NULL PRIMARY KEY,
    user_id         UUID         NOT NULL,
    device_id       VARCHAR(100) NOT NULL,
    metric_type     VARCHAR(50)  NOT NULL,
    threshold_value DECIMAL(8, 2),
    actual_value    DECIMAL(8, 2),
    message         TEXT,
    detected_at     TIMESTAMPTZ  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_anomaly_logs_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_anomaly_logs_user_detected ON anomaly_logs (user_id, detected_at);

-- 6. daily_reports（日次レポート）
-- UNIQUE(user_id, report_date): ECS バッチの冪等性保証（ON CONFLICT DO NOTHING と対で使用）
CREATE TABLE daily_reports (
    id                   UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id              UUID        NOT NULL,
    report_date          DATE        NOT NULL,
    total_power_kwh      DECIMAL(10, 4),
    avg_temperature      DECIMAL(5, 2),
    avg_humidity         DECIMAL(5, 2),
    total_motion_minutes INT,
    anomaly_count        INT         NOT NULL DEFAULT 0,
    anomaly_summary      JSONB,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_daily_reports_user_date UNIQUE (user_id, report_date),
    CONSTRAINT fk_daily_reports_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_daily_reports_user_date ON daily_reports (user_id, report_date DESC);

-- 7. report_downloads（レポートダウンロード履歴）
-- ダウンロード回数制限（1日3回）管理に使用
CREATE TABLE report_downloads (
    id            BIGSERIAL   NOT NULL PRIMARY KEY,
    user_id       UUID        NOT NULL,
    report_id     UUID        NOT NULL,
    download_date DATE        NOT NULL,
    downloaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_report_downloads_user_id   FOREIGN KEY (user_id)   REFERENCES users (id)          ON DELETE CASCADE,
    CONSTRAINT fk_report_downloads_report_id FOREIGN KEY (report_id) REFERENCES daily_reports (id)  ON DELETE CASCADE
);

CREATE INDEX idx_report_downloads_user_date ON report_downloads (user_id, download_date);
