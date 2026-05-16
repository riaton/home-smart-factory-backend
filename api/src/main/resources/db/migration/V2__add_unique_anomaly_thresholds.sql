-- anomaly_thresholds に UNIQUE(user_id, metric_type) 制約を追加
-- 1ユーザーにつき同一 metric_type の閾値設定は1件まで
ALTER TABLE anomaly_thresholds
    ADD CONSTRAINT uk_anomaly_thresholds_user_metric UNIQUE (user_id, metric_type);
