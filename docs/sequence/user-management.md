# シーケンス図: ユーザー管理

## Home Smart Factory -- IoT設備監視基盤

------------------------------------------------------------------------

# 1. 正常系

## 1.1 ユーザー情報取得

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis
    participant Users as users (table)

    React->>API: GET /api/users/me<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: user_id

    API->>Users: SELECT WHERE id = ?
    Users-->>API: ユーザー情報

    API-->>React: 200 OK<br/>{ data: { id, email, created_at } }
```

---

## 1.2 アカウント削除

関連する全データを同一トランザクション内で削除する。

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis
    participant DB as RDS (transaction)

    React->>API: DELETE /api/users/me<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: user_id

    API->>DB: BEGIN

    API->>DB: DELETE report_downloads WHERE user_id = ? -- (1)
    API->>DB: DELETE daily_reports WHERE user_id = ? -- (2) report_downloadsが参照するため先に削除
    API->>DB: DELETE anomaly_logs WHERE user_id = ? -- (3)
    API->>DB: DELETE anomaly_thresholds WHERE user_id = ? -- (4)
    API->>DB: DELETE iot_data WHERE user_id = ? -- (5)
    API->>DB: DELETE devices WHERE user_id = ? -- (6) iot_data/anomaly_logsがdevice_idを参照するため後に削除
    API->>DB: DELETE users WHERE id = ? -- (7) 最後に削除

    API->>DB: COMMIT
    DB-->>API: 成功

    API->>Redis: DEL session_id
    Redis-->>API: 成功

    API-->>React: 204 No Content
    Note over React: ログイン画面へリダイレクト
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

    React->>API: GET or DELETE /api/users/me<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: nil（セッションなし）

    API-->>React: 401 Unauthorized<br/>{ error: { code: "UNAUTHORIZED" } }
    Note over React: ログイン画面へリダイレクト
```

---

## 2.2 トランザクション失敗（アカウント削除時）

**発生箇所:** API Server → RDS

**原因:**
- RDS 障害 / 接続タイムアウト
- いずれかのDELETEが失敗

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis
    participant DB as RDS (transaction)

    React->>API: DELETE /api/users/me<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: user_id

    API->>DB: BEGIN
    API->>DB: DELETE report_downloads WHERE user_id = ?
    API->>DB: DELETE daily_reports WHERE user_id = ?
    Note over DB: 接続エラー / タイムアウト
    DB-->>API: エラー

    API->>DB: ROLLBACK
    Note over API: データは全件保持される

    API-->>React: 500 Internal Server Error<br/>{ error: { code: "INTERNAL_SERVER_ERROR" } }
```

> **設計メモ:** ROLLBACKにより部分削除は発生しない。セッションはそのまま維持される。

---

## 2.3 Redis 障害

**発生箇所:** API Server → Redis

**原因:**
- Redis のダウン / 接続タイムアウト

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis

    React->>API: GET or DELETE /api/users/me<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: 接続エラー / タイムアウト

    Note over API: セッション確認不能 → 認証不能
    API-->>React: 500 Internal Server Error<br/>{ error: { code: "INTERNAL_SERVER_ERROR" } }
```

------------------------------------------------------------------------

# 3. エラー対応まとめ

| エラー箇所 | エラー内容 | 挙動 | 備考 |
|---|---|---|---|
| React → API | セッション無効 | 401 返却・ログイン画面リダイレクト | 全エンドポイント共通 |
| API → RDS | トランザクション失敗 | ROLLBACK → 500 返却 | 部分削除は発生しない |
| API → Redis | Redis 障害 | 500 返却 | セッション確認不能のため認証不能 |
