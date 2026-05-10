# プロセスガイド (Process Guide)

## 基本原則

### 1. 具体例を豊富に含める

抽象的なルールだけでなく、具体的なコード例を提示します。

**悪い例**:
```
変数名は分かりやすくすること
```

**良い例**:
```java
// ✅ 良い例: 役割が明確
IotDataService iotDataService = new IotDataService(repository);
AnomalyLogRepository anomalyLogRepository = new AnomalyLogRepository();

// ❌ 悪い例: 曖昧
Service svc = new Service();
Repository repo = new Repository();
```

### 2. 理由を説明する

「なぜそうするのか」を明確にします。

**例**:
```
## 例外を握り潰さない

理由: catch して何もしないと、問題の原因究明が困難になります。
予期されるエラーは適切に処理し、予期しないエラーは上位に伝播させて
ログに記録できるようにします。
```

### 3. 測定可能な基準を設定

曖昧な表現を避け、具体的な数値を示します。

**悪い例**:
```
コードカバレッジは高く保つこと
```

**良い例**:
```
コードカバレッジ目標:
- ユニットテスト: 80%以上（Service 層は 90%以上）
- 統合テスト: 60%以上
- E2Eテスト: 主要フロー100%
```

---

## Git運用ルール

### ブランチ戦略（Git Flow採用）

**Git Flowとは**:
Vincent Driessenが提唱した、機能開発・リリース・ホットフィックスを体系的に管理するブランチモデル。明確な役割分担により、チーム開発での並行作業と安定したリリースを実現します。

**ブランチ構成**:
```
main (本番環境)
└── develop (開発・統合環境)
    ├── feature/* (新機能開発)
    ├── hotfix/* (バグ修正)
    └── release/* (リリース準備)※必要に応じて
```

**運用ルール**:
- **main**: 本番リリース済みの安定版コードのみを保持。タグでバージョン管理
- **develop**: 次期リリースに向けた最新の開発コードを統合。CIでの自動テスト実施
- **feature/\*、fix/\***: developから分岐し、作業完了後にPRでdevelopへマージ
- **直接コミット禁止**: すべてのブランチでPRレビューを必須とし、コード品質を担保
- **マージ方針**: feature→develop は squash merge、develop→main は merge commit を推奨

**Git Flowのメリット**:
- ブランチの役割が明確で、複数人での並行開発がしやすい
- 本番環境(main)が常にクリーンな状態に保たれる
- 緊急対応時はhotfixブランチで迅速に対応可能（必要に応じて導入）

### コミットメッセージの規約

**Conventional Commitsを推奨**:

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type一覧**:
```
feat: 新機能 (minor version up)
fix: バグ修正 (patch version up)
docs: ドキュメント
style: フォーマット (コードの動作に影響なし)
refactor: リファクタリング
perf: パフォーマンス改善
test: テスト追加・修正
build: ビルドシステム（Gradle設定等）
ci: CI/CD設定
chore: その他 (依存関係更新など)

BREAKING CHANGE: 破壊的変更 (major version up)
```

**良いコミットメッセージの例**:

```
feat(device): デバイス登録APIを追加

ユーザーが新しいIoTデバイスを登録できるようになりました。

実装内容:
- DeviceController に POST /api/devices エンドポイント追加
- DeviceService に登録ロジック実装
- Bean Validation で入力値を検証

破壊的変更:
- Device エンティティに deviceType 必須フィールドを追加
- 既存のデバイスデータはマイグレーションが必要です

Closes #123
BREAKING CHANGE: Device エンティティに deviceType 必須フィールド追加
```

### プルリクエストのテンプレート

**効果的なPRテンプレート**:

```markdown
## 変更の種類
- [ ] 新機能 (feat)
- [ ] バグ修正 (fix)
- [ ] リファクタリング (refactor)
- [ ] ドキュメント (docs)
- [ ] その他 (chore)

## 変更内容
### 何を変更したか
[簡潔な説明]

### なぜ変更したか
[背景・理由]

### どのように変更したか
- [変更点1]
- [変更点2]

## テスト
### 実施したテスト
- [ ] ユニットテスト追加（JUnit 5 / Mockito）
- [ ] 統合テスト追加（@SpringBootTest / @WebMvcTest）
- [ ] 手動テスト実施

### テスト結果
[テスト結果の説明]

## 関連Issue
Closes #[番号]
Refs #[番号]

## レビューポイント
[レビュアーに特に見てほしい点]
```

---

## テスト戦略

### テストピラミッド

```
       /\
      /E2E\       少 (遅い、高コスト)
     /------\
    / 統合   \     中  (@SpringBootTest / @WebMvcTest)
   /----------\
  / ユニット   \   多 (速い、低コスト) (JUnit 5 / Mockito)
 /--------------\
```

**目標比率**:
- ユニットテスト: 70%
- 統合テスト: 20%
- E2Eテスト: 10%

### テストの書き方

**Given-When-Then パターン**:

```java
class IotDataServiceTest {

    @Test
    @DisplayName("正常なペイロードでIoTデータを保存できる")
    void saveFromSqs_validPayload_savesData() {
        // Given: 準備
        IotMessagePayload payload = new IotMessagePayload("device-001", 25.5, 60.0, null, Instant.now());
        given(iotDataRepository.save(any())).willReturn(IotData.from(payload));

        // When: 実行
        iotDataService.saveFromSqs(payload);

        // Then: 検証
        then(iotDataRepository).should().save(any(IotData.class));
        then(anomalyDetector).should().check(any(IotData.class));
    }

    @Test
    @DisplayName("タイトルが空の場合ValidationExceptionをスローする")
    void saveFromSqs_blankDeviceId_throwsException() {
        // Given: 準備
        IotMessagePayload payload = new IotMessagePayload("", 25.5, 60.0, null, Instant.now());

        // When / Then: 実行と検証
        assertThatThrownBy(() -> iotDataService.saveFromSqs(payload))
                .isInstanceOf(ValidationException.class);
    }
}
```

### カバレッジ目標

**測定可能な目標（Gradle + JaCoCo）**:

```groovy
// build.gradle
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = 'LINE'
                value = 'COVEREDRATIO'
                minimum = 0.80  // 全体: 80%以上
            }
        }
        rule {
            element = 'PACKAGE'
            includes = ['com.example.smartfactory.application.*']
            limit {
                counter = 'LINE'
                value = 'COVEREDRATIO'
                minimum = 0.90  // Service 層: 90%以上
            }
        }
    }
}
```

**理由**:
- 重要なビジネスロジック（application/ 以下）は高いカバレッジを要求
- Controller 層は @WebMvcTest で別途検証
- 100%を目指さない（コストと効果のバランス）

---

## コードレビュープロセス

### レビューの目的

1. **品質保証**: バグの早期発見
2. **知識共有**: チーム全体でコードベースを理解
3. **学習機会**: ベストプラクティスの共有

### 効果的なレビューのポイント

**レビュアー向け**:

1. **建設的なフィードバック**
```markdown
## ❌ 悪い例
このコードはダメです。

## ✅ 良い例
この実装だと N+1 クエリが発生します。
`JOIN FETCH` を使うと1クエリに改善できます:

```java
@Query("SELECT d FROM Device d JOIN FETCH d.thresholds WHERE d.userId = :userId")
List<Device> findByUserIdWithThresholds(@Param("userId") String userId);
```
```

2. **優先度の明示**
```markdown
[必須] セキュリティ: APIキーがコードにハードコードされています
[必須] バグ: @Transactional がないため複数のDB操作がアトミックになりません
[推奨] パフォーマンス: ループ内でのリポジトリ呼び出しを避けましょう
[提案] 可読性: このメソッド名をもっと明確にできませんか？
[質問] この処理の意図を教えてください
```

3. **ポジティブなフィードバックも**
```markdown
✨ この実装は分かりやすいですね！
👍 エッジケースがしっかり考慮されています
💡 このパターンは他でも使えそうです
```

**レビュイー向け**:

1. **セルフレビューを実施**
   - PR作成前に自分でコードを見直す
   - 説明が必要な箇所にコメントを追加

2. **小さなPRを心がける**
   - 1PR = 1機能
   - 変更ファイル数: 10ファイル以内を推奨
   - 変更行数: 300行以内を推奨

3. **説明を丁寧に**
   - なぜこの実装にしたか
   - 検討した代替案
   - 特に見てほしいポイント

### レビュー時間の目安

- 小規模PR (100行以下): 15分
- 中規模PR (100-300行): 30分
- 大規模PR (300行以上): 1時間以上

**原則**: 大規模PRは避け、分割する

---

## 自動化の推進

### 品質チェックの自動化

**自動化項目と採用ツール**:

1. **コードスタイルチェック**
   - **Checkstyle**
     - Google Java Style Guide をベースにチームルールを定義
     - 設定ファイル: `config/checkstyle/checkstyle.xml`
     - `./gradlew checkstyleMain` で実行

2. **静的解析**
   - **SpotBugs**
     - バグパターン（NullPointer、リソースリーク等）を自動検出
     - `./gradlew spotbugsMain` で実行
   - **PMD**（任意）
     - コードの複雑度・重複を検出

3. **テスト実行**
   - **JUnit 5 + Mockito**
     - ユニットテスト / スライステスト（@WebMvcTest / @DataJpaTest）
     - `./gradlew test` で実行
   - **JaCoCo**
     - カバレッジ測定: `./gradlew jacocoTestReport`
     - カバレッジ検証: `./gradlew jacocoTestCoverageVerification`

4. **ビルド確認**
   - `./gradlew build` で全チェック + コンパイル + テストを一括実行

**build.gradle 設定例**:

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.3'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'checkstyle'
    id 'com.github.spotbugs' version '6.0.0'
    id 'jacoco'
}

checkstyle {
    toolVersion = '10.21.0'
    configFile = file('config/checkstyle/checkstyle.xml')
}

spotbugs {
    toolVersion = '4.9.0'
    excludeFilter = file('config/spotbugs/exclude.xml')
}

jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
}

test {
    useJUnitPlatform()
    finalizedBy jacocoTestReport
}
```

**CI/CD (GitHub Actions)**:

```yaml
# .github/workflows/ci.yml
name: CI
on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
          cache: gradle

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Checkstyle
        run: ./gradlew checkstyleMain checkstyleTest

      - name: SpotBugs
        run: ./gradlew spotbugsMain

      - name: Test & Coverage
        run: ./gradlew test jacocoTestCoverageVerification

      - name: Build
        run: ./gradlew build -x test
```

**Pre-commit フック（git hook + Gradle）**:

```bash
# .git/hooks/pre-commit
#!/bin/sh
echo "Running pre-commit checks..."
./gradlew checkstyleMain spotbugsMain test --daemon
if [ $? -ne 0 ]; then
  echo "Pre-commit checks failed. Fix errors before committing."
  exit 1
fi
```

**導入効果**:
- コミット前に自動チェックが走り、不具合コードの混入を防止
- PR作成時に自動でCI実行され、マージ前に品質を担保
- 早期発見により、修正コストを大幅削減

**この構成を選んだ理由**:
- 2025年時点での Java / Spring Boot エコシステムにおける標準的な構成
- Checkstyle + SpotBugs は Spring Boot 公式プロジェクトでも採用実績あり
- Gradle との親和性が高く、既存ビルドに追加しやすい

---

## チェックリスト

- [ ] ブランチ戦略が決まっている
- [ ] コミットメッセージ規約が明確である
- [ ] PRテンプレートが用意されている
- [ ] テストの種類とカバレッジ目標が設定されている（JaCoCo）
- [ ] コードレビュープロセスが定義されている
- [ ] CI/CDパイプラインが構築されている（GitHub Actions + Gradle）
- [ ] Checkstyle / SpotBugs の設定ファイルがリポジトリに含まれている
