# シーケンス図: 異常検知

## Home Smart Factory -- IoT設備監視基盤

------------------------------------------------------------------------

# 1. 正常系

IoTデータ保存後、ECS(Worker) が閾値チェックを行い、異常を検知した場合は anomaly_logs へ記録し SNS でメール通知する。

**前提条件:**
- ECS(Worker) が SQS からメッセージを受信し、iot_data への INSERT が完了済み
- ユーザーの閾値設定（anomaly_thresholds）が1件以上 enabled=true で登録されている

```mermaid
sequenceDiagram
    participant Worker as ECS(Worker)
    participant Thresholds as anomaly_thresholds (table)
    participant Logs as anomaly_logs (table)
    participant SNS as Amazon SNS

    Note over Worker: iot_data への INSERT 完了後に閾値チェックを実行

    Worker->>Thresholds: SELECT WHERE user_id = ? AND enabled = true
    Thresholds-->>Worker: 閾値設定一覧

    loop 各 metric_type（temperature / humidity / power_w）
        Worker->>Worker: 実測値 vs 閾値を比較
        alt 閾値超過あり
            Worker->>Logs: INSERT INTO anomaly_logs<br/>(device_id, metric_type, threshold_value, actual_value, detected_at)
            Logs-->>Worker: 成功
            Worker->>SNS: Publish<br/>（デバイスID・検知内容・検知時刻・閾値・実測値）
            SNS-->>Worker: 200 OK
            Note over SNS: ユーザーのメールアドレスへ即時送信
        else 閾値内（正常）
            Note over Worker: 何もしない
        end
    end
```

------------------------------------------------------------------------

# 2. エラー系

## 2.1 閾値設定が存在しない（未設定 / すべて disabled）

**発生箇所:** ECS(Worker) → anomaly_thresholds

**原因:**
- ユーザーが閾値を一度も設定していない
- すべての閾値設定が `enabled=false`

```mermaid
sequenceDiagram
    participant Worker as ECS(Worker)
    participant Thresholds as anomaly_thresholds (table)

    Worker->>Thresholds: SELECT WHERE user_id = ? AND enabled = true
    Thresholds-->>Worker: 0件

    Note over Worker: 閾値設定なし → 異常検知をスキップ<br/>正常終了（ログ出力のみ）
```

---

## 2.2 anomaly_logs 書き込み失敗

**発生箇所:** ECS(Worker) → anomaly_logs

**原因:**
- RDS 一時障害 / 接続タイムアウト

```mermaid
sequenceDiagram
    participant Worker as ECS(Worker)
    participant Logs as anomaly_logs (table)
    participant CW as CloudWatch
    participant SNSAdmin as SNS（cloudwatch-alarms）
    participant Admin as 管理者

    Worker->>Logs: INSERT INTO anomaly_logs

    alt RDS 一時障害
        Logs-->>Worker: 500 エラー / タイムアウト
        Note over Worker: ユーザーへの SNS 通知は行わない<br/>（ログ未保存のまま通知しない）
        Worker->>CW: ERROR ログ出力<br/>"anomaly_logs INSERT failed"
        Note over CW: メトリクスフィルターで検知<br/>AnomalyInsertFailureCount ≥ 1
        CW->>SNSAdmin: アラート発報（ALARM）
        SNSAdmin-->>Admin: メール通知「異常ログ書き込み失敗」
    end
```

> **設計メモ:** anomaly_logs の INSERT に失敗した場合、ユーザーへの SNS 通知は行わない（ログ未保存のまま通知しない）。ただしエラーログ（"anomaly_logs INSERT failed"）を CloudWatch Logs へ出力し、メトリクスフィルター + Alarm 経由で管理者へ通知する。

---

## 2.3 SNS 通知失敗

**発生箇所:** ECS(Worker) → Amazon SNS

**原因:**
- SNS 一時障害
- 宛先メールアドレスの不正

```mermaid
sequenceDiagram
    participant Worker as ECS(Worker)
    participant Logs as anomaly_logs (table)
    participant SNS as Amazon SNS（異常通知）
    participant CW as CloudWatch
    participant SNSAdmin as SNS（cloudwatch-alarms）
    participant Admin as 管理者

    Worker->>Logs: INSERT INTO anomaly_logs
    Logs-->>Worker: 成功

    Worker->>SNS: Publish
    alt SNS 一時障害
        SNS-->>Worker: 500 ServiceUnavailable
        Note over Worker: 指数バックオフでリトライ<br/>（1s → 2s → 4s → 最大60s、最大3回まで）
        Worker->>SNS: Publish（リトライ）
        SNS-->>Worker: 200 OK
    else リトライ上限超過
        Worker->>CW: ERROR ログ出力<br/>"SNS Publish failed: retry limit exceeded"
        Note over CW: メトリクスフィルターで検知<br/>SNSPublishFailureCount ≥ 1
        CW->>SNSAdmin: アラート発報（ALARM）
        SNSAdmin-->>Admin: メール通知「SNS異常通知の送信失敗」
    else 宛先不正
        SNS-->>Worker: 400 InvalidParameter
        Worker->>CW: ERROR ログ出力<br/>"SNS Publish failed: invalid destination"
        Note over CW: メトリクスフィルターで検知<br/>SNSPublishFailureCount ≥ 1
        CW->>SNSAdmin: アラート発報（ALARM）
        SNSAdmin-->>Admin: メール通知「SNS宛先設定エラー」
    end
```

> **設計メモ:** ユーザーへの SNS 通知失敗は許容する（anomaly_logs は保存済みのため、ユーザーは画面から異常一覧を確認可能）。ただしリトライ上限超過・宛先不正は管理者へ CloudWatch Alarm で通知する。

------------------------------------------------------------------------

# 3. エラー対応まとめ

> **補足:** 異常検知は IoTデータ収集フロー（iot_data への INSERT）が完了した後に実行される。閾値チェック・ログ保存・SNS通知は独立した処理として扱い、失敗しても SQS のリトライ対象にはならない。

| エラー箇所 | エラー内容 | 挙動 | データロスト |
|---|---|---|---|
| ECS(Worker) → anomaly_thresholds | 閾値設定なし / 全disabled | 異常検知スキップ・正常終了 | なし |
| ECS(Worker) → anomaly_logs | RDS一時障害 | ユーザー通知しない・CloudWatch Alarm で管理者へ通知 | あり（異常ログ） |
| ECS(Worker) → SNS | SNS一時障害 | 指数バックオフでリトライ | なし（ログは保存済み） |
| ECS(Worker) → SNS | リトライ上限超過 | CloudWatch Alarm で管理者へ通知 | なし（ログは保存済み） |
| ECS(Worker) → SNS | 宛先不正（400） | リトライしない・CloudWatch Alarm で管理者へ通知 | なし（ログは保存済み） |
