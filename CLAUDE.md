# home-smart-factory-backend

## プロジェクト概要

家庭用スマート工場の IoT バックエンド。Java / Spring Boot (Gradle マルチプロジェクト) で構成され、AWS ECS Fargate 上で稼働する。

Worker（IoT データ受信）/ Batch（日次レポート生成）/ API（REST API）の3モジュール構成。

---

## ドキュメント

設計ドキュメントはこのリポジトリの `docs/` に集約されている。

| 種別 | パス |
|---|---|
| API 設計書 | `docs/backend-design/api-design.md` |
| DB 設計書 | `docs/backend-design/db-design.md` |
| Worker/Batch ロジック | `docs/backend-design/ecs-worker-batch-logic.md` |
| IoT メッセージ形式 | `docs/backend-design/iot-message-format.md` |
| シーケンス図 | `docs/sequence/` |

---

## 技術スタック

- **言語 / FW**: Java 25 / Spring Boot 4.0.x
- **ビルド**: Gradle 9.0.0 (マルチプロジェクト)
- **インフラ**: AWS ECS Fargate
- **DB**: PostgreSQL 18
- **ORM**: Spring Data JPA / Hibernate 7.2.4.Final
- **CI/CD**: GitHub Actions

---

## ディレクトリ構造

### api モジュール
- `api/src/main/java/.../api/<機能名>/` — Controller・Service・Repository・Entity・dto/ をドメインごとに配置
- `api/src/main/java/.../api/config/` — 設定クラス・例外ハンドラー
- `api/src/main/resources/` — application.yml・Flyway マイグレーション

### worker モジュール
- `worker/src/main/java/.../worker/sqs/` — SQS ポーリング
- `worker/src/main/java/.../worker/iotdata/` — IoT データ処理
- `worker/src/main/java/.../worker/anomaly/` — 異常検知
- `worker/src/main/java/.../worker/sns/` — SNS 通知
- `worker/src/main/java/.../worker/device/` — デバイス参照
- `worker/src/main/java/.../worker/config/` — 設定クラス

### batch モジュール
- `batch/src/main/java/.../batch/report/` — 日次レポート生成
- `batch/src/main/java/.../batch/cleanup/` — データクリーンアップ

### common モジュール
- `common/src/main/java/.../common/exception/` — 共通例外クラス
- `common/src/main/java/.../common/response/` — 共通レスポンスラッパー

---

## コーディング規約

`.claude/skills/development-guideline-java/guides/implementation-guide.md` を必ず参照すること。

## 行動規範

### 基本的な行動規範
- 3ステップ以上のタスクは必ずPlanモードで開始する
- 変更は必要な箇所のみ。影響範囲を最小化する

### コンテキスト圧迫時の行動規範（焦ったら止まれ）
- コードを読まずに書かない
- 検証を省略しない
- Planモードを飛ばさない
- サブエージェントを使う（コンテキスト節約）
- 中途半端に終わらせるなら止まる
- 焦りを自覚したら宣言する
