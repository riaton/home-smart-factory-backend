# シーケンス図: 認証

## Home Smart Factory -- IoT設備監視基盤

------------------------------------------------------------------------

# 1. 正常系

## 1.1 ログイン（初回 / 2回目以降共通）

```mermaid
sequenceDiagram
    participant Browser as ブラウザ
    participant API as API Server
    participant Google as Google OAuth
    participant Redis as Redis
    participant Users as users (table)

    Browser->>API: GET /auth/google
    API-->>Browser: 302 Redirect<br/>→ Google認証ページ（state付き）

    Browser->>Google: Google認証ページ表示
    Note over Browser,Google: ユーザーがGoogleアカウントでログイン
    Google-->>Browser: 302 Redirect<br/>→ /auth/google/callback?code=...&state=...

    Browser->>API: GET /auth/google/callback?code=...&state=...
    Note over API: stateを検証（CSRF対策）
    API->>Google: codeをアクセストークンに交換
    Google-->>API: access_token

    API->>Google: GET ユーザー情報（email, google_id）
    Google-->>API: ユーザー情報

    API->>Users: SELECT WHERE google_id = ?
    alt 初回ログイン
        Users-->>API: 0件
        API->>Users: INSERT INTO users<br/>(google_id, email)
        Users-->>API: 新規ユーザー情報
    else 2回目以降
        Users-->>API: 既存ユーザー情報
    end

    API->>Redis: SET session_id → user_id（TTL: 24h）
    Redis-->>API: 成功

    API-->>Browser: 302 Redirect → フロントエンドトップページ<br/>Set-Cookie: session_id
```

---

## 1.2 ログアウト

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis

    React->>API: POST /auth/logout<br/>Cookie: session_id
    API->>Redis: DEL session_id
    Redis-->>API: 成功

    API-->>React: 204 No Content
    Note right of API: Set-Cookie: session_id= (Max-Age=0)
    Note over React: ログイン画面へリダイレクト
```

------------------------------------------------------------------------

# 2. エラー系

## 2.1 state 不一致（CSRF検証失敗）

**発生箇所:** API Server（コールバック受信時）

**原因:**
- CSRF攻撃
- セッション切れによりstate消失

```mermaid
sequenceDiagram
    participant Browser as ブラウザ
    participant API as API Server
    participant Google as Google OAuth

    Browser->>API: GET /auth/google/callback?code=...&state=...
    Note over API: stateが不一致 → CSRF検証失敗
    API-->>Browser: 400 Bad Request<br/>{ error: { code: "INVALID_STATE" } }
```

---

## 2.2 Google OAuth エラー

**発生箇所:** API Server → Google OAuth

**原因:**
- ユーザーが認証をキャンセル
- codeが無効 / 期限切れ

```mermaid
sequenceDiagram
    participant Browser as ブラウザ
    participant API as API Server
    participant Google as Google OAuth

    Browser->>API: GET /auth/google/callback?error=access_denied
    Note over API: Googleからエラーレスポンス
    API-->>Browser: 302 Redirect → ログイン画面<br/>?error=oauth_failed
```

---

## 2.3 Redis 障害（セッション保存失敗）

**発生箇所:** API Server → Redis

**原因:**
- Redis のダウン / 接続タイムアウト

```mermaid
sequenceDiagram
    participant Browser as ブラウザ
    participant API as API Server
    participant Google as Google OAuth
    participant Redis as Redis
    participant Users as users (table)

    Browser->>API: GET /auth/google/callback?code=...&state=...
    API->>Google: codeをアクセストークンに交換
    Google-->>API: access_token
    API->>Google: GET ユーザー情報
    Google-->>API: ユーザー情報
    API->>Users: SELECT / INSERT
    Users-->>API: ユーザー情報

    API->>Redis: SET session_id → user_id
    Redis-->>API: 接続エラー / タイムアウト

    Note over API: セッション発行不能 → ログイン失敗
    API-->>Browser: 500 Internal Server Error<br/>{ error: { code: "INTERNAL_SERVER_ERROR" } }
```

---

## 2.4 ログアウト時のセッション不在

**発生箇所:** API Server → Redis

**原因:**
- 既にログアウト済み
- セッションが有効期限切れ

```mermaid
sequenceDiagram
    participant React as React
    participant API as API Server
    participant Redis as Redis

    React->>API: POST /auth/logout<br/>Cookie: session_id
    API->>Redis: DEL session_id
    Redis-->>API: 0（該当なし）

    Note over API: セッションが存在しなくてもログアウト成功扱い
    API-->>React: 204 No Content
```

> **設計メモ:** ログアウトは冪等に扱う。セッションが存在しない場合も204を返す。

------------------------------------------------------------------------

# 3. エラー対応まとめ

| エラー箇所 | エラー内容 | 挙動 | 備考 |
|---|---|---|---|
| コールバック受信時 | state不一致 | 400 返却 | CSRF対策 |
| API → Google | 認証キャンセル / codeエラー | ログイン画面へリダイレクト | error=oauth_failed |
| API → Redis | セッション保存失敗 | 500 返却 | ログイン失敗 |
| POST /auth/logout | セッション不在 | 204 返却（正常扱い） | 冪等処理 |
