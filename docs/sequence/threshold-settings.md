# シーケンス図: 閾値設定

## Home Smart Factory -- IoT設備監視基盤

------------------------------------------------------------------------

# 1. 正常系

## 1.1 閾値設定一覧取得

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis
    participant Thresholds as anomaly_thresholds (table)

    React->>API: GET /api/anomaly-thresholds<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: user_id

    API->>Thresholds: SELECT WHERE user_id = ?
    Thresholds-->>API: 閾値設定一覧

    API-->>React: 200 OK<br/>{ data: [...] }
```

---

## 1.2 閾値設定新規作成

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis
    participant Thresholds as anomaly_thresholds (table)

    React->>API: POST /api/anomaly-thresholds<br/>Cookie: session_id<br/>{ metric_type, min_value, max_value }
    API->>Redis: GET session_id
    Redis-->>API: user_id

    API->>API: バリデーション<br/>・metric_type が temperature / humidity / power_w のいずれか<br/>・power_w の場合、min_value は無効(送信されても無視)<br/>・min_value と max_value を両方省略していないか

    API->>Thresholds: INSERT INTO anomaly_thresholds<br/>(user_id, metric_type, min_value, max_value, enabled=true)
    Thresholds-->>API: 成功

    API-->>React: 201 Created<br/>{ data: { id, metric_type, min_value, max_value, enabled, ... } }
```

---

## 1.3 閾値設定更新

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis
    participant Thresholds as anomaly_thresholds (table)

    React->>API: PATCH /api/anomaly-thresholds/{id}<br/>Cookie: session_id<br/>{ max_value, enabled }
    API->>Redis: GET session_id
    Redis-->>API: user_id

    API->>Thresholds: SELECT WHERE id = ? AND user_id = ?
    Thresholds-->>API: 閾値設定

    API->>Thresholds: UPDATE anomaly_thresholds<br/>SET max_value = ?, enabled = ?, updated_at = now()<br/>WHERE id = ? AND user_id = ?
    Thresholds-->>API: 成功

    API-->>React: 200 OK<br/>{ data: { id, metric_type, min_value, max_value, enabled, ... } }
```

---

## 1.4 閾値設定削除

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis
    participant Thresholds as anomaly_thresholds (table)

    React->>API: DELETE /api/anomaly-thresholds/{id}<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: user_id

    API->>Thresholds: SELECT WHERE id = ? AND user_id = ?
    Thresholds-->>API: 閾値設定

    API->>Thresholds: DELETE FROM anomaly_thresholds<br/>WHERE id = ? AND user_id = ?
    Thresholds-->>API: 成功

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

    React->>API: GET（or POST / PATCH / DELETE）<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: nil（セッションなし）

    API-->>React: 401 Unauthorized<br/>{ error: { code: "UNAUTHORIZED" } }
    Note over React: ログイン画面へリダイレクト
```

---

## 2.2 重複登録（同一 metric_type）

**発生箇所:** API Server → anomaly_thresholds

**原因:**
- 同一ユーザーで同一 `metric_type` の閾値設定が既に存在する

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis
    participant Thresholds as anomaly_thresholds (table)

    React->>API: POST /api/anomaly-thresholds<br/>{ metric_type: "temperature", ... }
    API->>Redis: GET session_id
    Redis-->>API: user_id

    API->>Thresholds: INSERT INTO anomaly_thresholds
    Thresholds-->>API: UNIQUE制約違反<br/>（user_id + metric_type）

    API-->>React: 409 Conflict<br/>{ error: { code: "THRESHOLD_ALREADY_EXISTS" } }
```

---

## 2.3 存在しないリソースへの操作（PATCH / DELETE）

**発生箇所:** API Server → anomaly_thresholds

**原因:**
- 指定した `id` が存在しない
- 他ユーザーの閾値設定への操作

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis
    participant Thresholds as anomaly_thresholds (table)

    React->>API: PATCH（or DELETE）/api/anomaly-thresholds/{id}<br/>Cookie: session_id
    API->>Redis: GET session_id
    Redis-->>API: user_id

    API->>Thresholds: SELECT WHERE id = ? AND user_id = ?
    Thresholds-->>API: 0件

    API-->>React: 404 Not Found<br/>{ error: { code: "THRESHOLD_NOT_FOUND" } }
```

> **設計メモ:** `id` が存在しても `user_id` が一致しない場合は 404 を返す（403 ではなく）。他ユーザーのリソースの存在を推測させないため。

---

## 2.4 バリデーションエラー

**発生箇所:** API Server（リクエスト受信時）

**原因:**
- `metric_type` が不正な値
- `min_value` と `max_value` の両方が省略されている

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis

    React->>API: POST /api/anomaly-thresholds<br/>{ metric_type: "invalid_type" }
    API->>Redis: GET session_id
    Redis-->>API: user_id

    API->>API: バリデーション → エラー

    API-->>React: 400 Bad Request<br/>{ error: { code: "VALIDATION_ERROR", message: "..." } }
```

------------------------------------------------------------------------

# 3. エラー対応まとめ

| エラー箇所 | エラー内容 | 挙動 | 備考 |
|---|---|---|---|
| React → API | セッション無効 | 401 返却・ログイン画面リダイレクト | 全エンドポイント共通 |
| API → anomaly_thresholds | 重複登録（同一metric_type） | 409 返却 | UNIQUE(user_id, metric_type) |
| API → anomaly_thresholds | 存在しないID / 他ユーザーのリソース | 404 返却 | 存在推測を防ぐため404に統一 |
| API（バリデーション） | 不正な metric_type / 値の省略 | 400 返却 | DBアクセス前にチェック |
