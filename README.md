# home-smart-factory-backend
Java / Spring Boot backend running on ECS Fargate (Gradle multi-project).
Handles IoT data ingestion (Worker), daily report generation (Batch), and REST API for the frontend.

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

## 新機能を追加するときの流れ

1. `feature/<機能名>` ブランチを切る
2. `/plan-feature <機能名>` で計画を立てる
3. `/implement-feature <機能名>` で実装する
4. `/ship-feature <機能名>` で検証・PR を出す
