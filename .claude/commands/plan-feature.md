---
description: 機能の実装計画を立て、tasklist.md を生成する
---

# /plan-feature <機能名>

**引数:** 機能名 (例: `/plan-feature F11`)

---

## ステップ0: 再入チェック

`.steering/` 配下に引数の機能名を含むディレクトリが存在するか確認する。

- **存在する場合**: tasklist.md の現在の状態を表示して終了する。実装を再開するには `/implement-feature <機能名>` を使うこと。
- **存在しない場合**: 以下のステップに進む。

---

## ステップ1: 準備

1. 現在の日付を `YYYYMMDD` 形式で取得する
2. `.steering/[日付]-[機能名]/` ディレクトリを作成する
3. `.steering/[日付]-[機能名]/tasklist.md` を空ファイルとして作成する

---

## ステップ2: 仕様書の読み込み

`CLAUDE.md` を読んでプロジェクト全体像を把握した上で、以下の機能別参照ドキュメントマップに従い、実装する機能に対応するドキュメントを読む。

ベースパス: `docs/`

| 機能 | 必読ドキュメント |
|------|----------------|
| **F0: 基盤セットアップ** | `backend-design/db-design.md` |
| **F1: 認証** | `backend-design/api-design.md`<br>`backend-design/db-design.md`<br>`sequence/auth.md` |
| **F2: ユーザー管理** | `backend-design/api-design.md`<br>`backend-design/db-design.md`<br>`sequence/user-management.md` |
| **F3: デバイス管理** | `backend-design/api-design.md`<br>`backend-design/db-design.md`<br>`sequence/device-management.md` |
| **F4: Worker IoTデータ収集** | `backend-design/db-design.md`<br>`sequence/iot-data-collection.md`<br>`backend-design/ecs-worker-batch-logic.md`<br>`backend-design/iot-message-format.md` |
| **F5: 閾値設定 API** | `backend-design/api-design.md`<br>`backend-design/db-design.md`<br>`sequence/threshold-settings.md` |
| **F6: 異常検知** | `backend-design/db-design.md`<br>`sequence/anomaly-detection.md`<br>`backend-design/ecs-worker-batch-logic.md` |
| **F7: IoTデータ API** | `backend-design/api-design.md`<br>`backend-design/db-design.md` |
| **F8: 異常ログ API** | `backend-design/api-design.md`<br>`backend-design/db-design.md`<br>`sequence/anomaly-list.md` |
| **F9: Batch 日次レポート生成** | `backend-design/db-design.md`<br>`sequence/daily-report-generation.md`<br>`backend-design/ecs-worker-batch-logic.md` |
| **F10: レポート API** | `backend-design/api-design.md`<br>`backend-design/db-design.md`<br>`sequence/report-download.md` |

ドキュメントが存在しない場合はスキップしてよい。

---

## ステップ3: 既存パターンの調査

機能名に関連するキーワードで `src/main/java/` を grep し、既存の実装パターン・命名規則・コンポーネントの利用方法を把握する。

---

## ステップ4: tasklist.md の生成

`.claude/skills/steering/templates/tasklist.md` をフォーマットの雛形として読み込み、内容を機能に合わせて置き換えて `.steering/[日付]-[機能名]/tasklist.md` を生成する。

生成時の注意:
- **フェーズ0**: 読んだドキュメントを1件ずつチェックボックスで列挙する
- **フェーズ1〜N**: 具体的なファイルパス・クラス名・メソッド名まで記載する。実装順序（依存関係）を考慮して並べる
- **品質チェック・PR作成は含めない**: それらは `/ship-feature` が担当する
- **振り返りセクション**: 末尾に空欄で含めておく（`/ship-feature` が記入する）

**タスク粒度の基準（必ず守ること）**

良い例（クラス名・メソッドシグネチャまで書く）:
```
- [ ] report/DailyReport.java — @Entity (read-only、id に @GeneratedValue なし)
- [ ] report/DailyReportRepository.java — findByUserIdOrderByReportDateDesc(UUID, Pageable)
- [ ] report/dto/ReportListItemResponse.java — record + static from(DailyReport)
- [ ] report/ReportService.java — getReports(UUID, int, int): PagedResponse
```

悪い例（何を作るか不明確）:
```
- [ ] エンティティを作る
- [ ] リポジトリを実装する
- [ ] サービスを作る
```

「そのタスクだけを見て実装できるか」を基準にする。クラス名・メソッド名が入っていないタスクは粒度が粗すぎる。

生成完了後、次のステップを案内する:

```
計画が完了しました。
実装を開始するには: /implement-feature <機能名>
```
