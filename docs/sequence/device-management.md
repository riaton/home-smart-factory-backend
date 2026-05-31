# シーケンス図: デバイス管理

## Home Smart Factory -- IoT設備監視基盤

------------------------------------------------------------------------

# 1. 正常系

## 1.1 デバイス一覧取得

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis
    participant Devices as devices (table)

    React->>API: GET /api/devices<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: user_id

    API->>Devices: SELECT WHERE user_id = ?
    Devices-->>API: デバイス一覧

    API-->>React: 200 OK<br/>{ data: [...] }
```

---

## 1.2 デバイス登録

**バリデーション:**
- `device_id`: 英数字・ハイフン、最大100文字
- `name`: 最大255文字

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis
    participant Devices as devices (table)

    React->>API: POST /api/devices<br/>Cookie: session_id<br/>{ device_id, name }
    API->>Redis: GET session_id
    Redis-->>API: user_id

    API->>Devices: INSERT INTO devices<br/>(user_id, device_id, name)
    Devices-->>API: 登録済みデバイス

    API-->>React: 201 Created<br/>{ data: { id, device_id, name, created_at } }
```

---

## 1.3 デバイス名更新

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis
    participant Devices as devices (table)

    React->>API: PATCH /api/devices/{id}<br/>Cookie: session_id<br/>{ name }
    API->>Redis: GET session_id
    Redis-->>API: user_id

    API->>Devices: SELECT WHERE id = ? AND user_id = ?
    Devices-->>API: デバイス情報

    API->>Devices: UPDATE devices SET name = ? WHERE id = ?
    Devices-->>API: 更新済みデバイス

    API-->>React: 200 OK<br/>{ data: { id, device_id, name, created_at } }
```

---

## 1.4 デバイス削除

関連する `iot_data`・`anomaly_logs` を同一トランザクション内で削除する。

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis
    participant DB as RDS (transaction)

    React->>API: DELETE /api/devices/{id}<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: user_id

    API->>DB: SELECT devices WHERE id = ? AND user_id = ?
    DB-->>API: デバイス情報

    API->>DB: BEGIN
    API->>DB: DELETE anomaly_logs WHERE device_id = ?
    API->>DB: DELETE iot_data WHERE device_id = ?
    API->>DB: DELETE devices WHERE id = ?
    API->>DB: COMMIT
    DB-->>API: 成功

    API-->>React: 204 No Content
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

    React->>API: GET/POST/PATCH/DELETE /api/devices/...<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: nil（セッションなし）

    API-->>React: 401 Unauthorized<br/>{ error: { code: "UNAUTHORIZED" } }
    Note over React: ログイン画面へリダイレクト
```

---

## 2.2 device_id 重複（デバイス登録時）

**発生箇所:** API Server → devices

**原因:**
- 同じ `device_id` が既に登録されている

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis
    participant Devices as devices (table)

    React->>API: POST /api/devices<br/>Cookie: session_id<br/>{ device_id, name }
    API->>Redis: GET session_id
    Redis-->>API: user_id

    API->>Devices: INSERT INTO devices<br/>(user_id, device_id, name)
    Devices-->>API: UNIQUE制約違反エラー（device_id）

    API-->>React: 409 Conflict<br/>{ error: { code: "DEVICE_ALREADY_EXISTS" } }
```

---

## 2.3 デバイスが存在しない / 他ユーザーのデバイス

**発生箇所:** API Server → devices

**原因:**
- 指定した `id` が存在しない
- 他ユーザーのデバイスへのアクセス

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis
    participant Devices as devices (table)

    React->>API: PATCH or DELETE /api/devices/{id}<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: user_id

    API->>Devices: SELECT WHERE id = ? AND user_id = ?
    Devices-->>API: 0件

    API-->>React: 404 Not Found<br/>{ error: { code: "DEVICE_NOT_FOUND" } }
```

> **設計メモ:** 他ユーザーのデバイスも404で返す。リソースの存在を推測させないため。

---

## 2.4 トランザクション失敗（デバイス削除時）

**発生箇所:** API Server → RDS

**原因:**
- RDS 障害 / 接続タイムアウト

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis
    participant DB as RDS (transaction)

    React->>API: DELETE /api/devices/{id}<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: user_id

    API->>DB: SELECT devices WHERE id = ? AND user_id = ?
    DB-->>API: デバイス情報

    API->>DB: BEGIN
    API->>DB: DELETE anomaly_logs WHERE device_id = ?
    Note over DB: 接続エラー / タイムアウト
    DB-->>API: エラー

    API->>DB: ROLLBACK
    Note over API: データは全件保持される

    API-->>React: 500 Internal Server Error<br/>{ error: { code: "INTERNAL_SERVER_ERROR" } }
```

> **設計メモ:** ROLLBACKにより部分削除は発生しない。

---

## 2.5 Redis 障害

**発生箇所:** API Server → Redis

**原因:**
- Redis のダウン / 接続タイムアウト

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis

    React->>API: GET/POST/PATCH/DELETE /api/devices/...<br/>Cookie: session_id
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
| API → devices | device_id 重複 | 409 返却 | 登録時のみ |
| API → devices | デバイスが存在しない / 他ユーザー | 404 返却 | 存在推測を防ぐため404に統一 |
| API → RDS | トランザクション失敗 | ROLLBACK → 500 返却 | 部分削除は発生しない |
| API → Redis | Redis 障害 | 500 返却 | セッション確認不能のため認証不能 |
