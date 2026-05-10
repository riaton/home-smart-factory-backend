---
name: development-guideline-java
description: Java / Spring Boot でコードを実装する際に参照するコーディング規約・開発プロセスガイド。実装・レビュー・テスト設計・リリース準備時に使用する。
allowed-tools: Read, Write, Edit
---

# 開発ガイドラインスキル

チーム開発に必要な2つの要素をカバーします:
1. 実装時のコーディング規約 (implementation-guide.md)
2. 開発プロセスの標準化 (process-guide.md)

## 前提条件

本ガイドラインは以下の技術スタックを前提とします:
- **言語**: Java 25
- **フレームワーク**: Spring Boot 4.0
- **ビルドツール**: Gradle
- **テスト**: JUnit 5 / Mockito
- **静的解析**: Checkstyle / SpotBugs / JaCoCo

## 参照ドキュメントの場所
以下を参照する。
  - ./guides/implementation-guide.md: Java / Spring Boot コーディング規約
  - ./guides/process-guide.md: 開発プロセス・Git運用・CI/CD

## クイックリファレンス

### コード実装時
コード実装時のルールと規約: ./guides/implementation-guide.md

含まれる内容:
- Java / Spring Boot 規約
- 命名規則（クラス・メソッド・定数・パッケージ）
- レイヤー構成と責務（Controller / Service / Repository）
- 入力検証（Bean Validation）
- 例外処理（カスタム例外 / @ControllerAdvice）
- DI・セキュリティ・パフォーマンス
- テストコード実装（JUnit 5 / Mockito / @WebMvcTest）

### 開発プロセス時
開発プロセスの標準化: ./guides/process-guide.md

含まれる内容:
- Git Flow ブランチ戦略
- Conventional Commits コミットメッセージ規約
- プルリクエストテンプレート
- テスト戦略（ピラミッド / JaCoCo カバレッジ目標）
- コードレビュープロセス
- CI/CD 自動化（GitHub Actions + Gradle + Checkstyle / SpotBugs）

## 使用シーン別ガイド

### 新規開発時
1. ./guides/implementation-guide.md で命名規則・コーディング規約を確認
2. ./guides/process-guide.md でブランチ戦略・PR処理を確認
3. テストを先に書く（TDD）

### コードレビュー時
- ./guides/process-guide.md の「コードレビュープロセス」を参照
- ./guides/implementation-guide.md で規約違反がないか確認

### テスト設計時
- ./guides/process-guide.md の「テスト戦略」（ピラミッド、JaCoCo カバレッジ）
- ./guides/implementation-guide.md の「テストコード」（JUnit 5 / MockMvcTester 実装パターン）

### リリース準備時
- ./guides/process-guide.md の「Git運用ルール」（main へのマージ方針）
- コミットメッセージが Conventional Commits に従っているか確認
- `./gradlew build` が成功することを確認
