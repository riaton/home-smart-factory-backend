# シーケンス図: レポートダウンロード

## Home Smart Factory -- IoT設備監視基盤

------------------------------------------------------------------------

# 1. 正常系

## 1.1 レポート一覧取得

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis
    participant Reports as daily_reports (table)

    React->>API: GET /api/reports<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: user_id

    API->>Reports: SELECT WHERE user_id = ?<br/>ORDER BY report_date DESC<br/>LIMIT per_page OFFSET (page-1)*per_page
    Reports-->>API: レポート一覧 + 総件数

    API-->>React: 200 OK<br/>{ data: [...], pagination: { total, page, per_page } }
```

---

## 1.2 レポート詳細取得

`download_count_today` をレスポンスに含め、フロント側でダウンロードボタンの活性/非活性を制御する。

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis
    participant Reports as daily_reports (table)
    participant Downloads as report_downloads (table)

    React->>API: GET /api/reports/{id}<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: user_id

    API->>Reports: SELECT WHERE id = ? AND user_id = ?
    Reports-->>API: レポート詳細

    API->>Downloads: SELECT COUNT(*)<br/>WHERE user_id = ? AND download_date = CURRENT_DATE AT TIME ZONE Asia/Tokyo
    Downloads-->>API: download_count_today

    API-->>React: 200 OK<br/>{ data: { ..., download_count_today: 1 } }
    Note over React: download_count_today >= 3 の場合<br/>ダウンロードボタンを非活性にし<br/>本日のダウンロード上限(3回)に達しました。<br/>明日00:00以降に再度お試しくださいと表示
```

---

## 1.3 PDFダウンロード

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis
    participant Reports as daily_reports (table)
    participant Downloads as report_downloads (table)

    React->>API: POST /api/reports/{id}/download<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: user_id

    API->>Reports: SELECT WHERE id = ? AND user_id = ?
    Reports-->>API: レポートデータ

    API->>Downloads: SELECT COUNT(*)<br/>WHERE user_id = ? AND download_date = CURRENT_DATE AT TIME ZONE Asia/Tokyo
    Downloads-->>API: count（例: 1）

    Note over API: count < 3 → ダウンロード許可

    API->>API: "レポートデータからPDF生成<br/>使用ライブラリ: iText / Apache PDFBox 等<br/>テンプレート: HTML→PDF変換"

    API->>Downloads: INSERT INTO report_downloads<br/>(user_id, report_id, download_date, downloaded_at)
    Downloads-->>API: 成功

    API-->>React: "200 OK<br/>Content-Type: application/pdf<br/>Content-Disposition: attachment; filename=report_{report_date}.pdf<br/>Body: PDFバイナリ"
```

------------------------------------------------------------------------

# 2. エラー系

## 2.1 未認証（セッション無効）

**発生箇所:** React → API Server

**原因:**
- セッションの有効期限切れ
- 不正な session_id

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis

    React->>API: GET or POST /api/reports/...<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: nil（セッションなし）

    API-->>React: 401 Unauthorized<br/>{ error: { code: "UNAUTHORIZED" } }
    Note over React: ログイン画面へリダイレクト
```

---

## 2.2 レポートが存在しない / 他ユーザーのレポート

**発生箇所:** API Server → daily_reports

**原因:**
- 指定した `id` が存在しない
- 他ユーザーのレポートへのアクセス

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis
    participant Reports as daily_reports (table)

    React->>API: GET or POST /api/reports/{id}/...<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: user_id

    API->>Reports: SELECT WHERE id = ? AND user_id = ?
    Reports-->>API: 0件

    API-->>React: 404 Not Found<br/>{ error: { code: "REPORT_NOT_FOUND" } }
```

> **設計メモ:** 他ユーザーのレポートも404で返す。リソースの存在を推測させないため。

---

## 2.3 ダウンロード回数上限超過

**発生箇所:** API Server → report_downloads

**原因:**
- 当日のダウンロード回数が既に3回に達している

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis
    participant Reports as daily_reports (table)
    participant Downloads as report_downloads (table)

    React->>API: POST /api/reports/{id}/download<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: user_id

    API->>Reports: SELECT WHERE id = ? AND user_id = ?
    Reports-->>API: レポートデータ

    API->>Downloads: SELECT COUNT(*)<br/>WHERE user_id = ? AND download_date = CURRENT_DATE AT TIME ZONE Asia/Tokyo
    Downloads-->>API: count = 3

    Note over API: count >= 3 → ダウンロード拒否

    API-->>React: 429 Too Many Requests<br/>{ error: { code: "DOWNLOAD_LIMIT_EXCEEDED" } }
    Note over React: 「本日のダウンロード上限（3回）に達しました」を表示
```

---

## 2.4 Redis 障害

**発生箇所:** API Server → Redis

**原因:**
- Redis のダウン / 接続タイムアウト

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis

    React->>API: GET or POST /api/reports/...<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: 接続エラー / タイムアウト

    Note over API: セッション確認不能 → 認証不能
    API-->>React: 500 Internal Server Error<br/>{ error: { code: "INTERNAL_SERVER_ERROR" } }
```

---

## 2.5 RDS 障害

**発生箇所:** API Server → daily_reports / report_downloads

**原因:**
- RDS のダウン / 接続タイムアウト

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis
    participant Reports as daily_reports (table)
    participant Downloads as report_downloads (table)

    React->>API: POST /api/reports/{id}/download<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: user_id

    alt daily_reports SELECT 失敗
        API->>Reports: SELECT WHERE id = ? AND user_id = ?
        Reports-->>API: 接続エラー / タイムアウト
        API-->>React: 500 Internal Server Error
    else report_downloads COUNT SELECT 失敗
        API->>Reports: SELECT WHERE id = ? AND user_id = ?
        Reports-->>API: レポートデータ
        API->>Downloads: SELECT COUNT(*) WHERE user_id = ? AND download_date = 当日
        Downloads-->>API: 接続エラー / タイムアウト
        API-->>React: 500 Internal Server Error
    else PDF生成後に report_downloads INSERT 失敗
        API->>Reports: SELECT WHERE id = ? AND user_id = ?
        Reports-->>API: レポートデータ
        API->>Downloads: SELECT COUNT(*)
        Downloads-->>API: count（例: 1）
        API->>API: PDF生成
        API->>Downloads: INSERT INTO report_downloads
        Downloads-->>API: 接続エラー / タイムアウト
        Note over API: PDFは生成済みだがクライアントには返せない<br/>ダウンロード回数は消費されない（INSERTが失敗）
        API-->>React: 500 Internal Server Error
    end
```

> **設計メモ:** PDF生成後にINSERT失敗した場合、ダウンロード回数が消費されないためユーザーは再試行できる。PDFバイナリはサーバー上に保持しないため、再試行時は再生成する。

------------------------------------------------------------------------

# 3. エラー対応まとめ

| エラー箇所 | エラー内容 | 挙動 | 備考 |
|---|---|---|---|
| React → API | セッション無効 | 401 返却・ログイン画面リダイレクト | 全エンドポイント共通 |
| API → daily_reports | レポートが存在しない / 他ユーザー | 404 返却 | 存在推測を防ぐため404に統一 |
| API → report_downloads | ダウンロード回数上限（3回/日） | 429 返却 | フロントでも事前に非活性制御 |
| API → Redis | Redis 障害 | 500 返却 | セッション確認不能のため認証不能 |
| API → RDS | RDS 障害（SELECT / INSERT） | 500 返却 | PDF生成後のINSERT失敗時はダウンロード回数消費されず再試行可能 |
