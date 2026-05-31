# シーケンス図: 日次レポート生成

## Home Smart Factory -- IoT設備監視基盤

------------------------------------------------------------------------

# 1. 正常系

**前提条件:**
- EventBridge のスケジュールにより毎日午前3時に ECS バッチが起動する
- 集計対象は前日 00:00〜23:59 のデータ
- 全ユーザー分を順次処理する（1ユーザー1レポート）

```mermaid
sequenceDiagram
    participant EB as Amazon EventBridge
    participant Batch as ECS(Batch)
    participant Users as users (table)
    participant IoT as iot_data (table)
    participant Anomaly as anomaly_logs (table)
    participant Reports as daily_reports (table)

    EB->>Batch: スケジュール起動（毎日03:00）

    Batch->>Users: SELECT id FROM users
    Users-->>Batch: ユーザー一覧

    loop 各ユーザー
        Batch->>IoT: "SELECT<br/>AVG(temperature), AVG(humidity),<br/>SUM(power_w) / 60 / 1000 AS total_power_kwh,<br/>SUM(CASE WHEN motion=1 THEN 1 ELSE 0 END) AS total_motion_minutes<br/>WHERE user_id = ? AND recorded_at<br/>BETWEEN 前日00:00:00+09:00 AND 前日23:59:59+09:00<br/>(JST基準、UTCに変換して実行)"
        IoT-->>Batch: 集計結果

        Batch->>Anomaly: "SELECT COUNT(*) AS anomaly_count,<br/>JSON_AGG(JSON_BUILD_OBJECT(<br/>  device_id, metric_type,<br/>  COUNT(*), MAX(actual_value)<br/>)) AS anomaly_summary<br/>FROM anomaly_logs<br/>WHERE user_id = ? AND detected_at<br/>BETWEEN 前日00:00:00+09:00 AND 前日23:59:59+09:00<br/>GROUP BY device_id, metric_type"
        Anomaly-->>Batch: 集計結果

        Batch->>Reports: INSERT INTO daily_reports<br/>(user_id, report_date, total_power_kwh,<br/>avg_temperature, avg_humidity,<br/>total_motion_minutes, anomaly_count, anomaly_summary)
        Reports-->>Batch: 成功
    end

    Note over Batch: 全ユーザー処理完了
```

------------------------------------------------------------------------

# 2. エラー系

## 2.1 前日データなし（iot_data が0件）

**発生箇所:** ECS(Batch) → iot_data

**原因:**
- 前日に該当ユーザーのデバイスからデータが送信されなかった
- ユーザーが登録済みだがデバイスを持っていない

```mermaid
sequenceDiagram
    participant Batch as ECS(Batch)
    participant IoT as iot_data (table)
    participant Reports as daily_reports (table)

    Batch->>IoT: SELECT 集計クエリ WHERE user_id = ? AND recorded_at BETWEEN ...
    IoT-->>Batch: 0件（NULLまたは空）

    Note over Batch: 集計値を NULL としてレポートを生成する<br/>（データなし = レポートなし とはしない）
    Batch->>Reports: INSERT INTO daily_reports<br/>(user_id, report_date, total_power_kwh=NULL,<br/>avg_temperature=NULL, ..., anomaly_count=0)
    Reports-->>Batch: 成功
```

> **設計メモ:** データが0件でもレポートレコードは生成する。フロント側で NULL を「データなし」として表示する。

---

## 2.2 重複実行（同一 user_id + report_date）

**発生箇所:** ECS(Batch) → daily_reports

**原因:**
- EventBridge の二重起動
- 手動での再実行

```mermaid
sequenceDiagram
    participant Batch as ECS(Batch)
    participant Reports as daily_reports (table)

    Batch->>Reports: INSERT INTO daily_reports<br/>(user_id, report_date, ...)
    Reports-->>Batch: UNIQUE制約違反<br/>（user_id + report_date）

    Note over Batch: INSERT をスキップして次のユーザーの処理へ続行
```

> **設計メモ:** `INSERT ... ON CONFLICT (user_id, report_date) DO NOTHING` を使用し、重複時は静かにスキップする。

---

## 2.3 RDS 障害（集計・INSERT 失敗）

**発生箇所:** ECS(Batch) → RDS 各テーブル

**原因:**
- RDS 一時障害 / 接続タイムアウト

```mermaid
sequenceDiagram
    participant EB as Amazon EventBridge
    participant Batch as ECS(Batch)
    participant RDS as RDS（各テーブル）
    participant CW as CloudWatch
    participant SNSAdmin as SNS（cloudwatch-alarms）
    participant Admin as 管理者

    EB->>Batch: スケジュール起動（03:00）

    loop 各ユーザーの処理
        Batch->>RDS: SELECT 集計クエリ / INSERT daily_reports
        RDS-->>Batch: タイムアウト / 500エラー

        Batch->>CW: ERROR ログ出力<br/>"batch report generation failed: user_id=?"
        Note over CW: メトリクスフィルターで検知<br/>BatchReportFailureCount ≥ 1
        CW->>SNSAdmin: アラート発報（ALARM）
        SNSAdmin-->>Admin: メール通知「バッチレポート生成失敗」

        Note over Batch: 次ユーザーの処理へ続行<br/>（処理済みユーザー分は保存済み）
    end
```

> **設計メモ:** 個別ユーザーのRDS障害は ERROR ログ出力 + CloudWatch Alarm で管理者へ通知する。タスクは継続し処理済みユーザー分のレポートは保存される。ECS タスク自体が異常終了した場合は後述 2.4 を参照。

---

## 2.4 ECS Batch 異常終了・起動失敗

**発生箇所:** Amazon EventBridge → ECS(Batch)

**原因:**
- コンテナイメージ取得失敗（TaskFailedToStart）
- OOM / プロセス異常終了（実行中クラッシュ）

```mermaid
sequenceDiagram
    participant EB as Amazon EventBridge
    participant ECS as Amazon ECS
    participant Batch as ECS(Batch)
    participant SNSBatch as SNS（batch-task-failure）
    participant Lambda as Lambda（batch-restart-function）
    participant SNSAdmin as SNS（cloudwatch-alarms）
    participant Admin as 管理者

    EB->>ECS: RunTask（Batch起動）

    alt 起動失敗（TaskFailedToStart）
        Note over ECS: コンテナ起動不可で停止<br/>（イメージ取得失敗等）
    else 実行中クラッシュ（OOM等）
        ECS->>Batch: タスク起動
        Note over Batch: 異常終了（OOM等）
    end

    ECS->>EB: Task STOPPED（exitCode ≠ 0）
    Note over EB: batch-task-stopped-rule 発火<br/>startedBy ≠ "lambda-restart"

    EB->>SNSBatch: イベント転送

    par 自動再実行
        SNSBatch->>Lambda: Lambda起動
        Lambda->>ECS: RunTask（startedBy="lambda-restart"）
        ECS->>Batch: タスク再起動（1回のみ）
    and 管理者通知
        SNSBatch-->>Admin: メール通知「Batchタスク異常終了」
    end

    alt 再実行も失敗
        ECS->>EB: Task STOPPED（startedBy="lambda-restart"）
        Note over EB: lambda-restart-failed-rule 発火<br/>startedBy="lambda-restart" 専用ルール
        EB->>SNSAdmin: アラート発報
        SNSAdmin-->>Admin: メール通知「Batch再実行も失敗・手動対応要」
    end
```

> **設計メモ:** `batch-task-failure` SNS に管理者メールサブスクリプションを追加し、Lambda 自動再実行と並列で管理者へ通知する。Lambda 再実行後も失敗した場合は `lambda-restart-failed-rule`（EventBridge, 新規）が `startedBy="lambda-restart"` のタスク停止を検知し `cloudwatch-alarms` SNS 経由で管理者へ追加通知する。

------------------------------------------------------------------------

# 3. エラー対応まとめ

| エラー箇所 | エラー内容 | 挙動 | データロスト |
|---|---|---|---|
| ECS(Batch) → iot_data | 前日データ0件 | NULL値でレポートを生成 | なし |
| ECS(Batch) → daily_reports | 重複実行 | INSERTスキップ（ON CONFLICT DO NOTHING）・次ユーザーへ続行 | なし |
| ECS(Batch) → RDS | RDS一時障害（個別ユーザー） | ERRORログ出力・CloudWatch Alarm で管理者通知・次ユーザーへ続行 | あり（該当ユーザー分） |
| EventBridge → ECS(Batch) | 起動失敗・実行中クラッシュ | EventBridge→SNS→Lambda 自動再実行（1回）+ 管理者メール通知 | なし（再実行で復旧） |
| EventBridge → ECS(Batch) | 再実行も失敗 | lambda-restart-failed-rule → cloudwatch-alarms → 管理者メール通知 | あり（未処理ユーザー分） |
