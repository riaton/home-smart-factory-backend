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

設計ドキュメントは別リポジトリ `~/Desktop/home-smart-factory-base/docs/` に集約されている。

| 種別 | パス |
|---|---|
| 機能要件定義書 | `docs/spec-requirements/機能要件.md` |
| API 設計書 | `docs/basic-design/API設計書.md` |
| DB 設計書 | `docs/basic-design/DB設計書.md` |
| シーケンス図 | `docs/basic-design/sequence/` |
| 詳細設計書 | `docs/detailed-design/` |

---

## 技術スタック

- **言語 / FW**: Java 25 / Spring Boot 4.0.x
- **ビルド**: Gradle (マルチプロジェクト)
- **インフラ**: AWS ECS Fargate
- **DB**: (詳細設計書参照)
- **CI/CD**: GitHub Actions

---

## ブランチ戦略

```
main (本番環境)
└── develop (開発・統合環境)
    ├── feature/* (新機能開発)
    ├── hotfix/* (バグ修正)
    └── release/* (リリース準備 ※必要に応じて)
```

- `feature/*` / `hotfix/*` は develop から分岐し、完了後に PR で develop へマージ
- `develop` → `main` : リリース時に PR を出す
- **直接コミット禁止**: すべてのブランチで PR レビューを必須とする
- **マージ方針**: feature / hotfix → develop は squash merge、develop → main は merge commit

---

## コーディング規約

`.claude/skills/development-guideline-java/guides/implementation-guide.md` を必ず参照すること。

主要ルール:
- レイヤー構成: `Controller → Service → Repository`
- DI: コンストラクタインジェクション必須（`@Autowired` フィールド注入は禁止）
- 命名: PascalCase (クラス) / camelCase (メソッド・変数) / UPPER_SNAKE_CASE (定数)

---

## カスタムコマンド / エージェント

| コマンド / エージェント | 用途 |
|---|---|
| `/add-feature-backend <機能名>` | 新機能を既存パターンに従って自動実装 |
| `implementation-validator-java` (サブエージェント) | 実装コードと仕様書の整合性検証 |

---

## 新機能を追加するときの流れ

1. `feature/<機能名>` ブランチを切る
2. `/add-feature-backend <機能名>` を実行する
3. 実装後、`implementation-validator-java` エージェントで検証する
4. PR を `develop` に出す

---

## ローカル開発環境

`.devcontainer/devcontainer.json` に Dev Container 設定あり。VS Code の「Reopen in Container」で起動可能。

```bash
# ビルド
./gradlew build

# テスト
./gradlew test

# 特定モジュールのみ
./gradlew :api:test
```
