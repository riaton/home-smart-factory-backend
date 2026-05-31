# ECS Worker / Batch 処理ロジック設計書

## Home Smart Factory -- IoT設備監視基盤

------------------------------------------------------------------------

# 1. 概要

| コンポーネント | 役割 |
|---|---|
| ECS Worker | SQSからIoTデータを受信し、DB保存・異常検知・通知を行う常駐プロセス |
| ECS Batch | 毎日03:00 JSTに起動し、日次レポート生成・データ削除を行う単発タスク |

------------------------------------------------------------------------

# 2. ECS Worker: 異常検知アルゴリズム

## 2.1 実行タイミング

`iot_data` へのINSERT成功直後に実行する。INSERT失敗時は閾値チェックを行わない。

## 2.2 閾値取得

```sql
SELECT metric_type, min_value, max_value
FROM anomaly_thresholds
WHERE user_id = $1
  AND enabled = true;
```

- 結果が0件の場合 → 異常検知をスキップして正常終了
- `metric_type` は `temperature` / `humidity` / `power_w` の3種類

## 2.3 閾値比較ロジック

各 `metric_type` について以下のルールで判定する。

```
for each threshold in thresholds:
    actual_value = iot_data[threshold.metric_type]

    if actual_value is null:
        skip  # センサー未搭載

    is_anomaly = false
    threshold_value = null

    if threshold.metric_type != 'power_w':
        if threshold.min_value is not null and actual_value < threshold.min_value:
            is_anomaly = true
            threshold_value = threshold.min_value
        elif threshold.max_value is not null and actual_value > threshold.max_value:
            is_anomaly = true
            threshold_value = threshold.max_value
    else:  # power_w は上限のみ
        if threshold.max_value is not null and actual_value > threshold.max_value:
            is_anomaly = true
            threshold_value = threshold.max_value

    if is_anomaly:
        INSERT anomaly_logs
        SNS Publish
```

**判定ルール詳細:**

| metric_type | min_value | max_value | 判定条件 |
|---|---|---|---|
| temperature | 設定あり | 設定あり | `actual < min` または `actual > max` |
| temperature | null | 設定あり | `actual > max` のみ |
| temperature | 設定あり | null | `actual < min` のみ |
| humidity | （temperatureと同様） | | |
| power_w | 無視 | 設定あり | `actual > max` のみ（APIの仕様上 min_value は無効） |

## 2.4 anomaly_logs INSERT

```sql
INSERT INTO anomaly_logs (
    user_id, device_id, metric_type,
    threshold_value, actual_value, message, detected_at
) VALUES (
    $1, $2, $3, $4, $5, $6, NOW()
);
```

**message フィールドの生成ルール:**

| 条件 | message 例 |
|---|---|
| 上限超過 | `温度が上限閾値(35.0℃)を超えました: 38.2℃` |
| 下限超過 | `温度が下限閾値(10.0℃)を下回りました: 7.5℃` |
| power_w 上限超過 | `消費電力が上限閾値(500.0W)を超えました: 620.3W` |

**単位サフィックス:**

| metric_type | サフィックス |
|---|---|
| temperature | ℃ |
| humidity | % |
| power_w | W |

## 2.5 SNS Publish

```
トピック: iot-anomaly-notification
メッセージ形式:
  件名: [異常検知] デバイス {device_id} - {metric_type}異常
  本文:
    デバイス: {device_id}
    検知項目: {metric_type}
    設定閾値: {threshold_value}{単位}（上限 or 下限）
    実測値: {actual_value}{単位}
    検知日時: {detected_at を JST に変換して表示}
```

- anomaly_logs INSERTが失敗した場合、SNS Publishは行わない
- SNS Publishが失敗した場合（リトライ上限超過）、エラーログを出力して許容する（anomaly_logsは保存済み）

**重複通知について:**

SNSはAt-least-once配信保証のため、anomaly_logs INSERT成功後・SNS Publish前にWorkerがクラッシュし、SQSメッセージが再配信された場合、同一センサーデータに対して重複通知が発生しうる。これは許容する（重複防止には分散ロック等が必要で複雑度が上がるため）。

------------------------------------------------------------------------

# 3. ECS Batch: 日次レポート生成

## 3.1 実行タイミング・対象期間

| 項目 | 値 |
|---|---|
| 起動時刻 | 毎日 03:00 JST（EventBridge cron: `0 18 * * ? *` UTC） |
| 集計対象 | 前日 00:00:00〜23:59:59 JST（UTCに変換してクエリ実行） |

**JST → UTC 変換:**
```
前日 00:00:00 JST = 前日 15:00:00 UTC（または前々日 15:00:00 UTC）
前日 23:59:59 JST = 当日 14:59:59 UTC

例) 2026-01-15 03:00 JST に実行する場合:
  対象: 2026-01-14 00:00:00+09:00 〜 2026-01-14 23:59:59+09:00
      = 2026-01-13 15:00:00 UTC 〜 2026-01-14 14:59:59 UTC
```

## 3.2 処理フロー

```
1. SELECT 全ユーザー
2. for each user:
   a. iot_data 集計クエリ実行
   b. anomaly_logs 集計クエリ実行
   c. daily_reports INSERT（ON CONFLICT DO NOTHING）
3. データ削除バッチ実行
```

**実行時間の規模想定:**

1ユーザーあたりの処理時間は集計クエリ2本 + INSERT で最大1秒を想定する。初期リリース時点での想定ユーザー規模（〜10ユーザー）では10秒以内に完了し、翌日03:00 JSTの次回起動との重複は発生しない。ユーザー数が100を超える場合はユーザー単位の並列処理を検討する。

## 3.3 iot_data 集計クエリ

```sql
SELECT
    AVG(temperature)                                    AS avg_temperature,
    AVG(humidity)                                       AS avg_humidity,
    SUM(power_w) / 60.0 / 1000.0                       AS total_power_kwh,
    SUM(CASE WHEN motion = 1 THEN 1 ELSE 0 END)        AS total_motion_minutes
FROM iot_data
WHERE user_id = $1
  AND recorded_at >= $2   -- 前日 00:00:00 UTC
  AND recorded_at <  $3;  -- 当日 00:00:00 UTC（= 前日 23:59:59+1秒）
```

**集計値の計算ルール:**

| カラム | 計算式 | 備考 |
|---|---|---|
| `avg_temperature` | AVG(temperature) | NULL を除いた平均。全件NULLなら NULL |
| `avg_humidity` | AVG(humidity) | 同上 |
| `total_power_kwh` | SUM(power_w) / 60 / 1000 | 1分間隔データのため、W×分÷60÷1000=kWh |
| `total_motion_minutes` | SUM(motion=1の件数) | 1分間隔データのため、件数=分数 |

- iot_dataが0件の場合: 全カラムを NULL としてレポートを生成する（「データなし」を記録するためレポート自体は生成する）

> **スコープ外（初期リリース）:** 要件定義書 5.2 の「デバイス別消費電力量」は初期リリースでは `daily_reports` への保存対象外とする。デバイス別の異常情報は `anomaly_summary`（セクション3.4）で代替する。将来的に必要となった場合は `device_power_summary (JSONB)` カラムを追加して対応する。

## 3.4 anomaly_logs 集計クエリ

```sql
SELECT
    COUNT(*)                                            AS anomaly_count,
    JSON_AGG(
        JSON_BUILD_OBJECT(
            'device_id',   device_id,
            'metric_type', metric_type,
            'count',       cnt,
            'max_value',   max_actual
        )
    )                                                   AS anomaly_summary
FROM (
    SELECT
        device_id,
        metric_type,
        COUNT(*)            AS cnt,
        MAX(actual_value)   AS max_actual
    FROM anomaly_logs
    WHERE user_id = $1
      AND detected_at >= $2
      AND detected_at <  $3
    GROUP BY device_id, metric_type
) sub;
```

- 異常が0件の場合: `anomaly_count = 0`、`anomaly_summary = NULL`

## 3.5 daily_reports INSERT

```sql
INSERT INTO daily_reports (
    user_id, report_date,
    total_power_kwh, avg_temperature, avg_humidity,
    total_motion_minutes, anomaly_count, anomaly_summary
) VALUES (
    $1, $2, $3, $4, $5, $6, $7, $8
)
ON CONFLICT (user_id, report_date) DO NOTHING;
```

- `report_date` は前日のJST日付（`DATE`型）
- 重複実行時（EventBridge二重起動・手動再実行）は `DO NOTHING` でスキップ

------------------------------------------------------------------------

# 4. ECS Batch: データ削除

レポート生成処理が全ユーザー完了した後に実行する。

## 4.1 削除対象と条件

| テーブル | 保持期間 | 削除条件 |
|---|---|---|
| `iot_data` | 90日 | `recorded_at < NOW() - INTERVAL '90 days'` |
| `anomaly_logs` | 1年 | `detected_at < NOW() - INTERVAL '1 year'` |
| `daily_reports` | 1年 | `report_date < CURRENT_DATE - INTERVAL '1 year'` |
| `report_downloads` | 1年 | `download_date < CURRENT_DATE - INTERVAL '1 year'` |

## 4.2 削除クエリ

```sql
-- iot_data（大量データのため分割削除）
-- 疑似コード:
-- while True:
--     deleted_count = execute(DELETE ... LIMIT 10000)
--     if deleted_count == 0:
--         break
--     sleep(100ms)  # DB負荷を分散するため短いスリープ
DELETE FROM iot_data
WHERE id IN (
    SELECT id FROM iot_data
    WHERE recorded_at < NOW() - INTERVAL '90 days'
    LIMIT 10000
);

-- anomaly_logs
DELETE FROM anomaly_logs
WHERE detected_at < NOW() - INTERVAL '1 year';

-- daily_reports
DELETE FROM daily_reports
WHERE report_date < CURRENT_DATE - INTERVAL '1 year';

-- report_downloads
DELETE FROM report_downloads
WHERE download_date < CURRENT_DATE - INTERVAL '1 year';
```

> **NOTE:** `iot_data` は大量レコードが蓄積するため、`LIMIT 10000` で分割削除する。1回のDELETEで全件削除するとロック時間が長くなり、Workerの書き込みに影響するため。

## 4.3 削除の実行順序

`report_downloads` を `daily_reports` より先に明示的に削除することで、CASCADE削除に依存しない冪等性を確保する。なお `daily_reports` 削除時には CASCADE設定により `report_downloads` も削除されるが、先に明示削除済みのためNOOP（削除対象0件）となる。

```
1. iot_data 削除（分割削除ループ）
2. anomaly_logs 削除
3. report_downloads 削除
4. daily_reports 削除
```

------------------------------------------------------------------------

# 5. エラーハンドリング方針

| フェーズ | エラー内容 | 挙動 |
|---|---|---|
| 全ユーザー取得失敗 | RDS障害 | ERRORログ出力・タスク異常終了 → EventBridge→Lambda で1回自動再実行（詳細はインフラ定義書 セクション9.2・10.1参照。再実行も失敗した場合はアラート通知のみで自動回復なし） |
| 個別ユーザー集計失敗 | RDS一時障害 | ERRORログ出力・次ユーザーへ続行（該当ユーザー分はレポートなし） |
| daily_reports INSERT失敗 | RDS一時障害 | ERRORログ出力・次ユーザーへ続行 |
| データ削除失敗 | RDS一時障害 | ERRORログ出力・タスク終了（次回実行時に対象として再削除される） |

**個別ユーザー集計失敗時の補足:**

- 失敗したユーザーの当日レポートは欠損する。CloudWatch Logs に該当ユーザーIDと ERROR ログを出力する。
- 手動再実行または EventBridge 二重起動でBatchが再実行された場合、成功済みユーザーは `ON CONFLICT DO NOTHING` によりスキップされ、失敗ユーザーのみが再集計される。この冪等性は意図した設計である。
- 手動再実行による欠損レポートの補完手順については運用ドキュメントに記載する。

**レポート欠損時のユーザー体験:**

レポート一覧画面（S-06）で当日分のレポートが表示されない。ユーザーからの問い合わせが見込まれるため、運用担当者は CloudWatch Logs Insights（クエリ例はインフラ定義書 セクション11.4参照）で失敗ユーザーを特定し、手動再実行で当日中に補完することを目標とする。
