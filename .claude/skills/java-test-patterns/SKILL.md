---
name: java-test-patterns
description: このプロジェクト（Spring Boot 4.0 / Mockito BDD）固有のテスト実装パターン。Controller テストの standaloneSetup、Service テストのコンストラクタ注入、BDD スタイルの Mock 記法、テストメソッド命名規則を網羅する。テストクラスを新規作成するとき、既存テストを修正するとき、またはテストの書き方を確認するときに必ず参照する。
---

# Java テストパターン（このプロジェクト専用）

## 絶対に使わないもの

| 禁止 | 代替 |
|------|------|
| `@WebMvcTest` | `MockMvcBuilders.standaloneSetup(...)` |
| `@SpringBootTest` | `@ExtendWith(MockitoExtension.class)` |
| `@MockBean` | `@Mock` |
| `@InjectMocks` | `@BeforeEach` でコンストラクタ直接呼び出し |
| `@Autowired` (テスト内) | コンストラクタ注入 |

Spring のDI コンテキストを起動するテストは書かない。全テストは Mockito のみで完結させる。

---

## Controller テストの雛形

```java
@ExtendWith(MockitoExtension.class)
class XxxControllerTest {

    private static final String USER_ID = "00000000-0000-0000-0000-000000000001";
    private static final String RESOURCE_ID = "00000000-0000-0000-0000-000000000002";

    @Mock
    private XxxService xxxService;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new XxxController(xxxService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // セッション付きリクエストが多い場合はヘルパーメソッド化する
    private MockHttpSession session() {
        MockHttpSession s = new MockHttpSession();
        s.setAttribute(AuthService.SESSION_KEY_USER_ID, USER_ID);
        return s;
    }

    // テストデータが複数テストで使い回せるなら private メソッドにまとめる
    private XxxResponse sampleResponse() {
        return new XxxResponse(UUID.fromString(RESOURCE_ID), ...);
    }
}
```

**ポイント:**
- `standaloneSetup(new Controller(mock))` — Controller を直接 `new` してモックを注入する
- `setControllerAdvice(new GlobalExceptionHandler())` を**必ず**追加する（例外処理テストが機能しなくなる）
- セッションが1テストにしか使わない場合はインラインで作る

---

## Service テストの雛形

```java
@ExtendWith(MockitoExtension.class)
class XxxServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private XxxRepository xxxRepository;

    @Mock
    private AnotherRepository anotherRepository;

    private XxxService service;

    @BeforeEach
    void setup() {
        service = new XxxService(xxxRepository, anotherRepository);
    }
}
```

**ポイント:**
- Controller テストの UUID は String 定数、Service テストは UUID 定数（モックに渡すときに型が一致するため）
- エンティティのモックは `mock(Entity.class)` + `lenient().when(...)`

---

## Mock 記法（BDD スタイル）

全て `org.mockito.BDDMockito.*` の静的インポートを使う。

```java
// 戻り値あり
given(service.findById(any())).willReturn(response);
given(service.create(eq(USER_ID), any())).willReturn(response);

// void メソッド
willDoNothing().given(service).delete(any(), any());

// 例外スロー（戻り値あり）
given(service.findById(any())).willThrow(new ResourceNotFoundException("..."));

// 例外スロー（void メソッド）
willThrow(new ResourceNotFoundException("...")).given(service).delete(any(), any());

// 呼び出し検証
then(service).should().delete(UUID.fromString(USER_ID), UUID.fromString(RESOURCE_ID));
then(repository).should(never()).save(any());
then(repository).shouldHaveNoInteractions();
```

`Mockito.verify()` は使わず、必ず `BDDMockito.then()` を使う。

---

## MockMvc リクエスト記法

```java
// GET（セッションなし）
mvc.perform(get("/health"))
    .andExpect(status().isOk());

// GET（セッション付き）
mvc.perform(get("/api/reports").session(session()))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data[0].id").value(REPORT_ID));

// POST（JSON ボディ）
mvc.perform(post("/api/anomaly-thresholds")
        .session(session())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"metric_type\":\"temperature\",\"max_value\":35.0}"))
    .andExpect(status().isCreated())
    .andExpect(jsonPath("$.data.id").value(THRESHOLD_ID));

// PATCH
mvc.perform(patch("/api/anomaly-thresholds/" + THRESHOLD_ID)
        .session(session())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"enabled\":false}"))
    .andExpect(status().isOk());

// DELETE
mvc.perform(delete("/api/anomaly-thresholds/" + THRESHOLD_ID).session(session()))
    .andExpect(status().isNoContent());
```

レスポンス検証のパス:
- 単一オブジェクト: `$.data.field`
- リスト: `$.data[0].field`
- ページネーション: `$.pagination.total`, `$.pagination.page`, `$.pagination.per_page`
- エラー: `$.error.code`, `$.error.message`

---

## テストメソッド命名規則

```
メソッド名_シナリオ_期待結果

例:
getReports_returns200WithPagedList
getReport_notFound_returns404
create_invalidMetricType_returns400
detect_temperatureExceedsMax_savesLogAndPublishes
handleCallback_invalidState_throwsInvalidStateException
logout_invalidatesSession
```

`@DisplayName` は日本語で「何をするか・何が起きるか」を記述する:

```java
@Test
@DisplayName("GET /api/reports/{id} でレポートが存在しない場合は 404 を返すこと")
void getReport_notFound_returns404() throws Exception { ... }
```

---

## ArgumentCaptor（保存オブジェクトの検証）

保存された Entity のフィールドを検証したい場合に使う:

```java
ArgumentCaptor<AnomalyLog> captor = ArgumentCaptor.forClass(AnomalyLog.class);
then(anomalyLogRepository).should().save(captor.capture());
AnomalyLog log = captor.getValue();
assertThat(log.getMetricType()).isEqualTo("temperature");
assertThat(log.getActualValue()).isEqualByComparingTo("38.2");
```

---

## 例外テスト

```java
// Service のスローをそのままテスト
assertThatThrownBy(() -> service.handleCallback("code", "wrong-state", session))
        .isInstanceOf(InvalidStateException.class);

// Controller → GlobalExceptionHandler 経由でテスト
mvc.perform(get("/api/reports/" + REPORT_ID).session(session()))
    .andExpect(status().isNotFound())
    .andExpect(jsonPath("$.error.code").value("REPORT_NOT_FOUND"));
```

---

## エンティティのモック（lenient）

JPA エンティティをモックしたい場合（`save()` の戻り値として使うなど）:

```java
AnomalyThreshold t = mock(AnomalyThreshold.class);
lenient().when(t.getMetricType()).thenReturn("temperature");
lenient().when(t.getMinValue()).thenReturn(new BigDecimal("10.0"));
lenient().when(t.getMaxValue()).thenReturn(new BigDecimal("35.0"));
```

`lenient()` を使う理由: Mockito の strictness により「使われなかったスタブ」が UnnecessaryStubbingException になるのを防ぐため（テストによって参照するフィールドが変わる場合）。

---

## テストデータの UUID 定数

```java
// Controller テスト（JSONの .value() と一致させるため String のまま）
private static final String USER_ID    = "00000000-0000-0000-0000-000000000001";
private static final String RESOURCE_ID = "00000000-0000-0000-0000-000000000002";

// Service テスト（メソッドに渡す型に合わせて UUID）
private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
```

---

## Spring Boot 4.0 / Jackson 3.x 固有の注意点

Spring Boot 4.0 は Jackson 3.x を使用するため、パッケージが変わっている。

```java
// ✅ 正しい（Spring Boot 4.0）
import tools.jackson.databind.ObjectMapper;

// ❌ 誤り（Spring Boot 3.x以前）
import com.fasterxml.jackson.databind.ObjectMapper;
```

`ObjectMapper` を `@Mock` でモックして `readValue()` の返り値を設定する例:
```java
@Mock
private ObjectMapper objectMapper;

// given
given(objectMapper.readValue(anyString(), eq(SomePayload.class))).willReturn(payload);

// 例外スロー
given(objectMapper.readValue(anyString(), eq(SomePayload.class))).willThrow(new Exception("parse error"));
```

---

## よく使う static import

```java
// Mockito BDD
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;

// AssertJ
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// MockMvc
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
```
