# home-smart-factory-backend

## プロジェクト概要

家庭用スマート工場の IoT バックエンド。Java / Spring Boot (Gradle マルチプロジェクト) で構成され、AWS ECS Fargate 上で稼働する。

| モジュール | 役割 |
|---|---|
| Worker | IoT デバイスからのデータ受信・蓄積 |
| Batch | 日次レポート生成 |
| API | フロントエンド向け REST API |

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
- **ビルド**: Gradle (マルチプロジェクト)
- **インフラ**: AWS ECS Fargate
- **DB**: (詳細設計書参照)
- **CI/CD**: GitHub Actions

---

## コーディング規約

`.claude/skills/development-guideline-java/guides/implementation-guide.md` を必ず参照すること。

---

## カスタムコマンド / エージェント

| コマンド / エージェント | 用途 |
|---|---|
| `/plan-feature <機能名>` | 仕様書を読み、tasklist.md を生成する |
| `/implement-feature <機能名>` | tasklist.md に従ってコードを実装する |
| `/ship-feature <機能名>` | 検証・CI・振り返り・PR 作成を行う |
| `implementation-validator-java` (サブエージェント) | 実装コードと仕様書の整合性検証 |

---

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
