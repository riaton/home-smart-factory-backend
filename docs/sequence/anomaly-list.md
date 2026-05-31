# シーケンス図: 異常一覧表示

## Home Smart Factory -- IoT設備監視基盤

------------------------------------------------------------------------

# 1. 正常系

## 1.1 異常一覧取得（フィルタなし）

異常検知ログを新しい順（detected_at DESC）で取得する。最新の異常を優先的に確認するため。

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis
    participant Logs as anomaly_logs (table)

    React->>API: GET /api/anomaly-logs<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: user_id

    API->>Logs: SELECT WHERE user_id = ?<br/>ORDER BY detected_at DESC<br/>LIMIT per_page(default:20) OFFSET (page(default:1)-1)*per_page
    Logs-->>API: 異常検知ログ一覧 + 総件数

    API-->>React: 200 OK<br/>{ data: [...], pagination: { total, page, per_page } }
```

---

## 1.2 異常一覧取得（フィルタあり）

フィルタ条件（`device_id` / `metric_type` / `from` / `to`）を組み合わせて絞り込む。

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis
    participant Logs as anomaly_logs (table)

    React->>API: GET /api/anomaly-logs<br/>?device_id=room01&metric_type=temperature&from=...&to=...<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: user_id

    API->>Logs: SELECT WHERE user_id = ?<br/>AND device_id = ? AND metric_type = ?<br/>AND detected_at BETWEEN ? AND ?<br/>ORDER BY detected_at DESC<br/>LIMIT per_page(default:20) OFFSET (page(default:1)-1)*per_page
    Logs-->>API: 絞り込み結果 + 総件数

    API-->>React: 200 OK<br/>{ data: [...], pagination: { total, page, per_page } }
```

---

## 1.3 異常なし（空リスト）

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis
    participant Logs as anomaly_logs (table)

    React->>API: GET /api/anomaly-logs<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: user_id

    API->>Logs: SELECT WHERE user_id = ?<br/>ORDER BY detected_at DESC
    Logs-->>API: 0件

    API-->>React: 200 OK<br/>{ data: [], pagination: { total: 0, page: 1, per_page: 20 } }
    Note over React: 「異常は検知されていません」を表示
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

    React->>API: GET /api/anomaly-logs<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: nil（セッションなし）

    API-->>React: 401 Unauthorized<br/>{ error: { code: "UNAUTHORIZED" } }
    Note over React: ログイン画面へリダイレクト
```

---

## 2.2 不正なクエリパラメータ

**発生箇所:** API Server（リクエスト受信時）

**原因:**
- `from` / `to` に不正な日時フォーマット
- `per_page` が上限（100件）を超えている
- `metric_type` に不正な値

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis

    React->>API: GET /api/anomaly-logs?from=invalid-date<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: user_id

    alt per_page > 100
        API-->>React: 400 Bad Request<br/>{ error: { code: "VALIDATION_ERROR", message: "per_pageは最大100です" } }
    else from/toが不正な日時フォーマット
        API-->>React: 400 Bad Request<br/>{ error: { code: "VALIDATION_ERROR", message: "..." } }
    else metric_typeが不正な値
        API-->>React: 400 Bad Request<br/>{ error: { code: "VALIDATION_ERROR", message: "..." } }
    end
```

------------------------------------------------------------------------

# 3. エラー対応まとめ

| エラー箇所 | エラー内容 | 挙動 | 備考 |
|---|---|---|---|
| React → API | セッション無効 | 401 返却・ログイン画面リダイレクト | 全エンドポイント共通 |
| API（バリデーション） | 不正なクエリパラメータ | 400 返却 | DBアクセス前にチェック |
