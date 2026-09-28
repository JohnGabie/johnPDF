# johnPDF — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Construir o APK do johnPDF, um leitor de PDF Android com UI própria em Compose sobre o MuPDF, com recentes, lista de todos os PDFs, senha, zoom e rotação, testado em TDD e validado ponta a ponta no emulador via mobile-mcp.

**Architecture:** Activity única com Navigation Compose (Home e Reader). A UI conversa só com ViewModels. Os ViewModels usam repositórios (recentes em JSON, importação de `content://`, MediaStore, DataStore) e a interface `PdfEngine`. `MuPdfEngine` é a única classe que importa `com.artifex.mupdf` e serializa tudo numa thread dedicada. As dependências são montadas à mão num `AppContainer`, sem framework de DI.

**Tech Stack:** Kotlin 2.1, Jetpack Compose (Material 3), Navigation Compose (rotas type-safe), DataStore Preferences, kotlinx-serialization, MuPDF `fitz`, JUnit 4, Robolectric, Turbine, AndroidX Test e mobile-mcp.

**Spec:** `docs/superpowers/specs/2026-09-28-johnpdf-leitor-android-design.md`

## Global Constraints

- Pacote/applicationId: `com.johngabie.johnpdf`. Nome visível: `johnPDF`.
- `minSdk = 24`. `compileSdk = targetSdk = 36` (o SDK tem 36 e 37 instalados; ficamos no 36 porque o emulador é API 36 e o AGP 8.10 fixado não compila o 37).
- Ambiente: `source ~/.bashrc` antes de qualquer comando (JDK 17, SDK, Gradle 8.14.5 global). O emulador `johnpdf_test` (API 36, headless) come ~3,3 GB: **não** rode dois builds Gradle ao mesmo tempo.
- Todo texto de UI em **pt-BR**, exatamente como escrito neste plano.
- Texto com no mínimo **20sp**; alvos de toque com no mínimo **64dp** (`MinTouchTarget`); ícone sempre acompanhado de texto; nenhum menu ⋮.
- **Nenhuma** permissão `INTERNET`, SDK de anúncios ou analytics.
- Somente `engine/MuPdfEngine.kt` importa `com.artifex.mupdf.*`, e toda chamada ao MuPDF roda na thread única dele.
- Largura máxima de renderização: `MAX_RENDER_WIDTH_PX = 2048`.
- Recentes: no máximo `MAX_RECENTS = 20`.
- Zoom: `MIN_ZOOM = 1f`, `MAX_ZOOM = 4f`, `DOUBLE_TAP_ZOOM = 2.5f`.
- Licença do app: AGPL-3.0.
- Testes: **JUnit 4 em tudo** (desvio consciente da spec §5, que citava JUnit 5, para não manter dois runners). Robolectric roda com `sdk=35`.
- Mensagens de erro (texto exato): `"Não foi possível abrir este arquivo."`, `"Este arquivo não está mais disponível."`, `"Sem espaço no celular para abrir este arquivo."`, `"Não foi possível mostrar esta página."`.
- Gradle sempre com `-Xmx2g` (definido em `gradle.properties`) por causa dos ~7 GB de RAM.

## Review Focus

1. **Zoom 4× numa página grande estoura a memória.** Esperado: a renderização limita a largura a 2048px e, se ainda der `OutOfMemoryError`, tenta a metade; persistindo, mostra "Não foi possível mostrar esta página." *(Testes: Task 3, `render_caps_width_at_max`; Task 8, `render_oom_*`.)*
2. **Página com tamanho degenerado (0×0) num PDF malformado.** Esperado: a página aparece com proporção A4 padrão, sem crash de `aspectRatio`. *(Teste: Task 11, `page_with_zero_size_does_not_crash`.)*
3. **`recents.json` corrompido** (app morto no meio da escrita, versão antiga). Esperado: a lista abre vazia e o app segue funcionando. *(Teste: Task 4, `corrupted_store_loads_as_empty`.)*
4. **Permissão de arquivos revogada com o app em segundo plano** (a consulta ao MediaStore lança `SecurityException`). Esperado: lista vazia, sem crash. *(Teste: Task 9, `library_failure_results_in_empty_list`.)*
5. **Voltar do leitor enquanto páginas ainda renderizam.** Esperado: o motor fecha, as renderizações pendentes são canceladas e nada acessa um documento já destruído. *(Teste: Task 3, `close_while_rendering_does_not_crash`.)*

---

## Estrutura de arquivos

```
johnPDF/
├── settings.gradle.kts · build.gradle.kts · gradle.properties · gradle/libs.versions.toml
├── LICENSE (AGPL-3.0) · keystore.properties (gitignored)
├── tools/make_test_pdfs.py                      gerador das fixtures
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── res/drawable/ic_launcher.xml
        │   └── java/com/johngabie/johnpdf/
        │       ├── JohnPdfApp.kt · AppContainer.kt · MainActivity.kt
        │       ├── engine/   PdfEngine.kt · MuPdfEngine.kt
        │       ├── data/     Origin.kt · AppError.kt · RecentsRepository.kt · ImportRepository.kt
        │       │             PdfLibraryRepository.kt · StorageAccess.kt · OpenPdfUseCase.kt · SettingsRepository.kt
        │       ├── util/     FriendlyDate.kt · NameFilter.kt
        │       └── ui/
        │           ├── Routes.kt · AppNavHost.kt
        │           ├── theme/Theme.kt
        │           ├── common/ BigButton.kt · Dialogs.kt
        │           ├── home/   HomeViewModel.kt · HomeScreen.kt
        │           └── reader/ PageMath.kt · ReaderViewModel.kt · ReaderScreen.kt
        ├── test/  (JVM + Robolectric)
        │   ├── resources/robolectric.properties
        │   └── java/com/johngabie/johnpdf/…Test.kt, testutil/{FakePdfEngine,MainDispatcherRule}.kt
        └── androidTest/
            ├── assets/ normal.pdf · long.pdf · landscape.pdf · password.pdf · corrupted.pdf
            └── java/com/johngabie/johnpdf/engine/MuPdfEngineTest.kt
```

---

### Task 1: Esqueleto do projeto Gradle + tela mínima

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`, `app/src/main/res/drawable/ic_launcher.xml`
- Create: `app/src/main/java/com/johngabie/johnpdf/MainActivity.kt`
- Create: `app/src/test/resources/robolectric.properties`
- Create: `LICENSE`
- Modify: `.gitignore`
- Test: `app/src/test/java/com/johngabie/johnpdf/MainActivitySmokeTest.kt`

**Interfaces:**
- Consumes: ambiente do setup (`JAVA_HOME`, `ANDROID_HOME`, `gradle` no PATH; rodar `source ~/.bashrc` antes).
- Produces: build funcionando (`./gradlew testDebugUnitTest assembleDebug`), catálogo `libs.*` usado por todas as tasks e `MainActivity` exibindo o texto `johnPDF`.

- [ ] **Step 1: Descobrir a última versão do MuPDF `fitz`**

Run: `curl -s https://maven.ghostscript.com/com/artifex/mupdf/fitz/maven-metadata.xml | grep -o '<release>[^<]*'`
Expected: algo como `<release>1.26.x`. Use esse valor em `mupdf` no catálogo abaixo. As demais versões do catálogo são um conjunto conhecido e compatível entre si: **não atualize** durante este plano, a não ser que a resolução falhe.

- [ ] **Step 2: Criar os arquivos Gradle**

`settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.ghostscript.com")
    }
}
rootProject.name = "johnPDF"
include(":app")
```

`build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
```

`gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8
org.gradle.parallel=false
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
```

`gradle/libs.versions.toml` (troque `mupdf` pelo valor do Step 1):
```toml
[versions]
agp = "8.10.1"
kotlin = "2.1.21"
composeBom = "2025.05.00"
activityCompose = "1.10.1"
lifecycle = "2.9.0"
navigation = "2.9.0"
datastore = "1.1.7"
serialization = "1.8.1"
coroutines = "1.10.2"
mupdf = "1.26.3"
desugar = "2.1.5"
junit = "4.13.2"
robolectric = "4.14.1"
turbine = "1.2.0"
androidxTestCore = "1.6.1"
androidxTestExt = "1.2.1"
androidxTestRunner = "1.6.2"

[libraries]
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { module = "androidx.compose.ui:ui" }
androidx-compose-material3 = { module = "androidx.compose.material3:material3" }
androidx-compose-ui-test-junit4 = { module = "androidx.compose.ui:ui-test-junit4" }
androidx-compose-ui-test-manifest = { module = "androidx.compose.ui:ui-test-manifest" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }
androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
mupdf-fitz = { module = "com.artifex.mupdf:fitz", version.ref = "mupdf" }
desugar-jdk-libs = { module = "com.android.tools:desugar_jdk_libs", version.ref = "desugar" }
junit = { module = "junit:junit", version.ref = "junit" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
androidx-test-core = { module = "androidx.test:core", version.ref = "androidxTestCore" }
androidx-test-ext-junit = { module = "androidx.test.ext:junit", version.ref = "androidxTestExt" }
androidx-test-runner = { module = "androidx.test:runner", version.ref = "androidxTestRunner" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

`app/build.gradle.kts`:
```kotlin
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.johngabie.johnpdf"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.johngabie.johnpdf"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures { compose = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.mupdf.fitz)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
```

- [ ] **Step 3: Manifest, ícone, config do Robolectric, licença e .gitignore**

`app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="false"
        android:icon="@drawable/ic_launcher"
        android:label="johnPDF"
        android:largeHeap="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTask">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

`app/src/main/res/drawable/ic_launcher.xml` (folha branca com faixa vermelha "PDF" sobre fundo azul):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#0B4F9C" android:pathData="M0,0h108v108h-108z" />
    <path android:fillColor="#FFFFFF" android:pathData="M30,18h36l14,14v58h-50z" />
    <path android:fillColor="#C62828" android:pathData="M26,56h56v18h-56z" />
    <path android:fillColor="#0B4F9C" android:pathData="M38,34h30v4h-30zM38,44h30v4h-30zM38,82h30v4h-30z" />
</vector>
```

`app/src/test/resources/robolectric.properties`:
```properties
sdk=35
```

Run: `curl -sSL https://www.gnu.org/licenses/agpl-3.0.txt -o LICENSE && head -3 LICENSE`
Expected: `GNU AFFERO GENERAL PUBLIC LICENSE`.

Acrescente ao `.gitignore`:
```
keystore.properties
.venv-tools/
```

- [ ] **Step 4: Escrever o teste que falha**

`app/src/test/java/com/johngabie/johnpdf/MainActivitySmokeTest.kt`:
```kotlin
package com.johngabie.johnpdf

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun shows_app_name() {
        rule.onNodeWithText("johnPDF").assertIsDisplayed()
    }
}
```

- [ ] **Step 5: Gerar o wrapper e confirmar que o teste falha**

Run: `source ~/.bashrc && gradle wrapper --gradle-version 8.11.1 --distribution-type bin && ./gradlew testDebugUnitTest --tests '*MainActivitySmokeTest*'`
Expected: FAIL na compilação com `Unresolved reference: MainActivity`.

- [ ] **Step 6: Implementar a Activity mínima**

`app/src/main/java/com/johngabie/johnpdf/MainActivity.kt`:
```kotlin
package com.johngabie.johnpdf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Text("johnPDF") }
    }
}
```

- [ ] **Step 7: Rodar os testes e o build**

Run: `./gradlew testDebugUnitTest assembleDebug`
Expected: `BUILD SUCCESSFUL`; `ls app/build/outputs/apk/debug/` lista um APK por ABI e `app-universal-debug.apk`.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "build: esqueleto Gradle/Compose com MuPDF e teste de fumaça"
```

---

### Task 2: Utilitários puros (data amigável, origem, busca por nome, página dominante)

**Files:**
- Create: `app/src/main/java/com/johngabie/johnpdf/util/FriendlyDate.kt`
- Create: `app/src/main/java/com/johngabie/johnpdf/util/NameFilter.kt`
- Create: `app/src/main/java/com/johngabie/johnpdf/data/Origin.kt`
- Create: `app/src/main/java/com/johngabie/johnpdf/data/AppError.kt`
- Create: `app/src/main/java/com/johngabie/johnpdf/ui/reader/PageMath.kt`
- Test: `app/src/test/java/com/johngabie/johnpdf/util/FriendlyDateTest.kt`, `…/util/NameFilterTest.kt`, `…/data/OriginTest.kt`, `…/ui/reader/PageMathTest.kt`

**Interfaces:**
- Produces:
  - `fun friendlyDate(epochMillis: Long, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String`
  - `fun normalizeForSearch(s: String): String`; `fun <T> filterByName(items: List<T>, query: String, name: (T) -> String): List<T>`
  - `@Serializable enum class Origin(val label: String) { WHATSAPP, DOWNLOAD, DOCUMENTS, OTHER }`; `fun originFromPath(path: String): Origin`; `fun originFromAuthority(authority: String?): Origin`
  - `enum class AppError(val message: String) { CORRUPTED, GONE, NO_SPACE }`; `const val PAGE_RENDER_FAILED_MESSAGE`
  - `data class VisiblePage(val index: Int, val offset: Int, val size: Int)`; `fun dominantPage(pages: List<VisiblePage>, viewportStart: Int, viewportEnd: Int): Int?`; `fun pageLabel(current: Int, total: Int): String`

- [ ] **Step 1: Escrever os testes que falham**

`app/src/test/java/com/johngabie/johnpdf/util/FriendlyDateTest.kt`:
```kotlin
package com.johngabie.johnpdf.util

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class FriendlyDateTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private fun millis(y: Int, m: Int, d: Int, h: Int = 12) =
        LocalDateTime.of(y, m, d, h, 0).atZone(zone).toInstant().toEpochMilli()
    private val now = millis(2026, 9, 28, 15)

    @Test fun same_day_is_hoje() = assertEquals("hoje", friendlyDate(millis(2026, 9, 28, 0), now, zone))
    @Test fun previous_day_is_ontem() = assertEquals("ontem", friendlyDate(millis(2026, 9, 27, 23), now, zone))
    @Test fun same_year_shows_day_and_month() = assertEquals("12 de set.", friendlyDate(millis(2026, 9, 12), now, zone))
    @Test fun other_year_includes_year() = assertEquals("3 de mar. de 2025", friendlyDate(millis(2025, 3, 3), now, zone))
}
```

`app/src/test/java/com/johngabie/johnpdf/util/NameFilterTest.kt`:
```kotlin
package com.johngabie.johnpdf.util

import org.junit.Assert.assertEquals
import org.junit.Test

class NameFilterTest {
    private val names = listOf("Fatura_Setembro.pdf", "Receita médica.pdf", "boleto.PDF")

    @Test fun empty_query_returns_all() = assertEquals(names, filterByName(names, "  ") { it })
    @Test fun match_is_case_insensitive() = assertEquals(listOf("Fatura_Setembro.pdf"), filterByName(names, "fatura") { it })
    @Test fun match_ignores_accents_both_ways() {
        assertEquals(listOf("Receita médica.pdf"), filterByName(names, "medica") { it })
        assertEquals(listOf("boleto.PDF"), filterByName(names, "BOLÉTO") { it })
    }
    @Test fun no_match_returns_empty() = assertEquals(emptyList<String>(), filterByName(names, "xyz") { it })
}
```

`app/src/test/java/com/johngabie/johnpdf/data/OriginTest.kt`:
```kotlin
package com.johngabie.johnpdf.data

import org.junit.Assert.assertEquals
import org.junit.Test

class OriginTest {
    @Test fun whatsapp_path() = assertEquals(Origin.WHATSAPP,
        originFromPath("/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Documents/a.pdf"))
    @Test fun download_path() = assertEquals(Origin.DOWNLOAD, originFromPath("/storage/emulated/0/Download/a.pdf"))
    @Test fun documents_path() = assertEquals(Origin.DOCUMENTS, originFromPath("/storage/emulated/0/Documents/a.pdf"))
    @Test fun other_path() = assertEquals(Origin.OTHER, originFromPath("/storage/emulated/0/Pictures/a.pdf"))
    @Test fun whatsapp_authority() = assertEquals(Origin.WHATSAPP, originFromAuthority("com.whatsapp.provider.media"))
    @Test fun downloads_authority() = assertEquals(Origin.DOWNLOAD, originFromAuthority("com.android.providers.downloads.documents"))
    @Test fun null_authority() = assertEquals(Origin.OTHER, originFromAuthority(null))
    @Test fun labels_are_friendly() = assertEquals(listOf("WhatsApp", "Download", "Documentos", "Outros"), Origin.entries.map { it.label })
}
```

`app/src/test/java/com/johngabie/johnpdf/ui/reader/PageMathTest.kt`:
```kotlin
package com.johngabie.johnpdf.ui.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PageMathTest {
    @Test fun picks_page_occupying_most_of_viewport() {
        val pages = listOf(VisiblePage(2, offset = -700, size = 1000), VisiblePage(3, offset = 312, size = 1000))
        assertEquals(3, dominantPage(pages, viewportStart = 0, viewportEnd = 1000))
    }
    @Test fun tie_goes_to_upper_page() {
        val pages = listOf(VisiblePage(0, offset = -500, size = 1000), VisiblePage(1, offset = 500, size = 1000))
        assertEquals(0, dominantPage(pages, 0, 1000))
    }
    @Test fun empty_returns_null() = assertNull(dominantPage(emptyList(), 0, 1000))
    @Test fun label_is_one_based() = assertEquals("Página 3 de 12", pageLabel(2, 12))
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./gradlew testDebugUnitTest --tests '*FriendlyDateTest*' --tests '*NameFilterTest*' --tests '*OriginTest*' --tests '*PageMathTest*'`
Expected: FAIL na compilação (`Unresolved reference: friendlyDate`, etc.).

- [ ] **Step 3: Implementar**

`app/src/main/java/com/johngabie/johnpdf/util/FriendlyDate.kt`:
```kotlin
package com.johngabie.johnpdf.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")
private val SAME_YEAR = DateTimeFormatter.ofPattern("d 'de' MMM", PT_BR)
private val OTHER_YEAR = DateTimeFormatter.ofPattern("d 'de' MMM 'de' yyyy", PT_BR)

/** "hoje", "ontem", "12 de set." ou "3 de mar. de 2025". */
fun friendlyDate(epochMillis: Long, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    return when {
        date == today -> "hoje"
        date == today.minusDays(1) -> "ontem"
        date.year == today.year -> date.format(SAME_YEAR)
        else -> date.format(OTHER_YEAR)
    }
}
```

`app/src/main/java/com/johngabie/johnpdf/util/NameFilter.kt`:
```kotlin
package com.johngabie.johnpdf.util

import java.text.Normalizer

private val DIACRITICS = Regex("\\p{Mn}+")

fun normalizeForSearch(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(DIACRITICS, "").lowercase()

fun <T> filterByName(items: List<T>, query: String, name: (T) -> String): List<T> {
    val q = normalizeForSearch(query.trim())
    if (q.isEmpty()) return items
    return items.filter { q in normalizeForSearch(name(it)) }
}
```

`app/src/main/java/com/johngabie/johnpdf/data/Origin.kt`:
```kotlin
package com.johngabie.johnpdf.data

import kotlinx.serialization.Serializable

@Serializable
enum class Origin(val label: String) {
    WHATSAPP("WhatsApp"),
    DOWNLOAD("Download"),
    DOCUMENTS("Documentos"),
    OTHER("Outros"),
}

fun originFromPath(path: String): Origin {
    val p = path.lowercase()
    return when {
        "whatsapp" in p -> Origin.WHATSAPP
        "/download/" in p || "/downloads/" in p -> Origin.DOWNLOAD
        "/documents/" in p || "/documentos/" in p -> Origin.DOCUMENTS
        else -> Origin.OTHER
    }
}

fun originFromAuthority(authority: String?): Origin {
    val a = authority?.lowercase() ?: return Origin.OTHER
    return when {
        "whatsapp" in a -> Origin.WHATSAPP
        "downloads" in a -> Origin.DOWNLOAD
        else -> Origin.OTHER
    }
}
```

`app/src/main/java/com/johngabie/johnpdf/data/AppError.kt`:
```kotlin
package com.johngabie.johnpdf.data

enum class AppError(val message: String) {
    CORRUPTED("Não foi possível abrir este arquivo."),
    GONE("Este arquivo não está mais disponível."),
    NO_SPACE("Sem espaço no celular para abrir este arquivo."),
}

const val PAGE_RENDER_FAILED_MESSAGE = "Não foi possível mostrar esta página."
```

`app/src/main/java/com/johngabie/johnpdf/ui/reader/PageMath.kt`:
```kotlin
package com.johngabie.johnpdf.ui.reader

data class VisiblePage(val index: Int, val offset: Int, val size: Int)

/** Índice da página com a maior área visível; empate fica com a de cima. */
fun dominantPage(pages: List<VisiblePage>, viewportStart: Int, viewportEnd: Int): Int? =
    pages.maxByOrNull { p ->
        (minOf(p.offset + p.size, viewportEnd) - maxOf(p.offset, viewportStart)).coerceAtLeast(0)
    }?.index

fun pageLabel(current: Int, total: Int): String = "Página ${current + 1} de $total"
```

- [ ] **Step 4: Rodar e ver passar**

Run: `./gradlew testDebugUnitTest --tests '*FriendlyDateTest*' --tests '*NameFilterTest*' --tests '*OriginTest*' --tests '*PageMathTest*'`
Expected: PASS (20 testes).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: utilitários de data, origem, busca e página dominante"
```

---

### Task 3: Motor de PDF (`PdfEngine` + `MuPdfEngine`) com fixtures

**Files:**
- Create: `tools/make_test_pdfs.py`
- Create: `app/src/androidTest/assets/{normal,long,landscape,password,corrupted}.pdf` (gerados)
- Create: `app/src/main/java/com/johngabie/johnpdf/engine/PdfEngine.kt`
- Create: `app/src/main/java/com/johngabie/johnpdf/engine/MuPdfEngine.kt`
- Test: `app/src/androidTest/java/com/johngabie/johnpdf/engine/MuPdfEngineTest.kt`

**Interfaces:**
- Produces:
```kotlin
data class PageSize(val width: Float, val height: Float)
sealed interface OpenResult { data class Success(val pageCount: Int); data object NeedsPassword; data object WrongPassword; data object Corrupted }
interface PdfEngine {
    suspend fun open(file: File, password: String? = null): OpenResult
    suspend fun pageSizes(): List<PageSize>
    suspend fun render(index: Int, widthPx: Int): Bitmap   // pode lançar OutOfMemoryError / RuntimeException
    fun close()
}
const val MAX_RENDER_WIDTH_PX = 2048
class MuPdfEngine(maxRenderWidthPx: Int = MAX_RENDER_WIDTH_PX) : PdfEngine
```
- Contrato: `open` pode ser chamado de novo na mesma instância e no mesmo arquivo com uma senha; o documento continua aberto entre as chamadas.

- [ ] **Step 1: Gerar as fixtures**

`tools/make_test_pdfs.py`:
```python
#!/usr/bin/env python3
"""Gera os PDFs de teste em app/src/androidTest/assets/.

Uso:
  python3 -m venv .venv-tools
  .venv-tools/bin/pip install reportlab pypdf cryptography
  .venv-tools/bin/python tools/make_test_pdfs.py
"""
from pathlib import Path

from pypdf import PdfReader, PdfWriter
from reportlab.lib.pagesizes import A4, landscape
from reportlab.pdfgen import canvas

OUT = Path(__file__).resolve().parent.parent / "app/src/androidTest/assets"


def make(path: Path, pages: int, size=A4) -> None:
    c = canvas.Canvas(str(path), pagesize=size)
    w, h = size
    for n in range(1, pages + 1):
        c.setFillColorRGB(0, 0, 0)
        # Quadrado preto de 100pt a 40pt do canto superior esquerdo (usado nos testes de pixel).
        c.rect(40, h - 140, 100, 100, fill=1, stroke=0)
        c.setFont("Helvetica", 48)
        c.drawString(40, h / 2, f"Pagina {n}")
        c.showPage()
    c.save()


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    make(OUT / "normal.pdf", 3)
    make(OUT / "long.pdf", 200)
    make(OUT / "landscape.pdf", 1, landscape(A4))
    writer = PdfWriter(clone_from=PdfReader(OUT / "normal.pdf"))
    writer.encrypt(user_password="1234", owner_password="dono-1234", algorithm="AES-128")
    with open(OUT / "password.pdf", "wb") as f:
        writer.write(f)
    (OUT / "corrupted.pdf").write_bytes(b"%PDF-1.7\n" + bytes(range(256)) * 8 + b"\n%%EOF\n")
    print("\n".join(sorted(p.name for p in OUT.glob("*.pdf"))))


if __name__ == "__main__":
    main()
```

Run: `python3 -m venv .venv-tools && .venv-tools/bin/pip install -q reportlab pypdf cryptography && .venv-tools/bin/python tools/make_test_pdfs.py`
Expected: lista `corrupted.pdf landscape.pdf long.pdf normal.pdf password.pdf`.

- [ ] **Step 2: Conferir a API real do MuPDF**

Run:
```bash
AAR=$(find ~/.gradle/caches -name 'fitz-*.aar' | head -1); mkdir -p /tmp/fitz && unzip -o -q "$AAR" classes.jar -d /tmp/fitz
javap -cp /tmp/fitz/classes.jar com.artifex.mupdf.fitz.Document com.artifex.mupdf.fitz.Page com.artifex.mupdf.fitz.Matrix com.artifex.mupdf.fitz.android.AndroidDrawDevice | grep -E 'openDocument|needsPassword|authenticatePassword|countPages|loadPage|getBounds|run\(|Matrix\(|AndroidDrawDevice\(|destroy|close'
```
Expected: existem `static Document openDocument(String)`, `boolean needsPassword()`, `boolean authenticatePassword(String)`, `int countPages()`, `Page loadPage(int)`, `Rect getBounds()`, `void run(Device, Matrix, Cookie)`, `Matrix(float,float,float,float,float,float)`, `AndroidDrawDevice(Bitmap,int,int)`, `destroy()` e `close()`. Se alguma assinatura for diferente, adapte **somente** `MuPdfEngine.kt` no Step 5; a interface `PdfEngine` não muda.

- [ ] **Step 3: Escrever os testes instrumentados que falham**

`app/src/androidTest/java/com/johngabie/johnpdf/engine/MuPdfEngineTest.kt`:
```kotlin
package com.johngabie.johnpdf.engine

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MuPdfEngineTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val engine = MuPdfEngine()

    private fun asset(name: String): File =
        File(instrumentation.targetContext.cacheDir, name).also { f ->
            instrumentation.context.assets.open(name).use { i -> f.outputStream().use { i.copyTo(it) } }
        }

    @After fun tearDown() = engine.close()

    @Test fun opens_normal_pdf() = runBlocking {
        assertEquals(OpenResult.Success(3), engine.open(asset("normal.pdf")))
    }

    @Test fun page_sizes_are_a4_portrait() = runBlocking {
        engine.open(asset("normal.pdf"))
        val sizes = engine.pageSizes()
        assertEquals(3, sizes.size)
        assertEquals(595.3f, sizes[0].width, 1f)
        assertEquals(841.9f, sizes[0].height, 1f)
    }

    @Test fun landscape_page_is_wider_than_tall() = runBlocking {
        engine.open(asset("landscape.pdf"))
        val size = engine.pageSizes().single()
        assertTrue(size.width > size.height)
    }

    @Test fun long_pdf_has_200_pages() = runBlocking {
        assertEquals(OpenResult.Success(200), engine.open(asset("long.pdf")))
    }

    @Test fun render_has_requested_width_and_page_aspect() = runBlocking {
        engine.open(asset("normal.pdf"))
        val bmp = engine.render(0, 600)
        assertEquals(600, bmp.width)
        assertEquals(849f, bmp.height.toFloat(), 2f)
    }

    @Test fun render_draws_content_on_white_background() = runBlocking {
        engine.open(asset("normal.pdf"))
        val bmp = engine.render(0, 600)
        assertEquals(Color.WHITE, bmp.getPixel(bmp.width - 3, bmp.height - 3))
        val inSquare = bmp.getPixel(91, 91)
        assertTrue("esperava preto, veio ${Integer.toHexString(inSquare)}", Color.red(inSquare) < 60)
    }

    @Test fun render_caps_width_at_max() = runBlocking {
        engine.open(asset("normal.pdf"))
        assertEquals(MAX_RENDER_WIDTH_PX, engine.render(0, 5000).width)
    }

    @Test fun password_flow() = runBlocking {
        val file = asset("password.pdf")
        assertEquals(OpenResult.NeedsPassword, engine.open(file))
        assertEquals(OpenResult.WrongPassword, engine.open(file, "0000"))
        assertEquals(OpenResult.Success(3), engine.open(file, "1234"))
        assertEquals(600, engine.render(0, 600).width)
    }

    @Test fun corrupted_file_returns_corrupted() = runBlocking {
        assertEquals(OpenResult.Corrupted, engine.open(asset("corrupted.pdf")))
    }

    @Test fun missing_file_returns_corrupted() = runBlocking {
        assertEquals(OpenResult.Corrupted, engine.open(File("/nao/existe.pdf")))
    }

    @Test fun concurrent_renders_do_not_crash() = runBlocking {
        engine.open(asset("long.pdf"))
        val results = (0 until 30).map { i -> async(Dispatchers.Default) { engine.render(i, 400).width } }.awaitAll()
        assertTrue(results.all { it == 400 })
    }

    @Test fun close_while_rendering_does_not_crash() = runBlocking {
        engine.open(asset("long.pdf"))
        val jobs = (0 until 10).map { i -> launch(Dispatchers.Default) { runCatching { engine.render(i, 800) } } }
        engine.close()
        jobs.forEach { it.join() }
    }
}
```

- [ ] **Step 4: Rodar e ver falhar**

Run: `adb devices` (o emulador `johnpdf_test` deve aparecer como `device`), depois `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.johngabie.johnpdf.engine.MuPdfEngineTest`
Expected: FAIL na compilação (`Unresolved reference: MuPdfEngine`).

- [ ] **Step 5: Implementar**

`app/src/main/java/com/johngabie/johnpdf/engine/PdfEngine.kt`:
```kotlin
package com.johngabie.johnpdf.engine

import android.graphics.Bitmap
import java.io.File

const val MAX_RENDER_WIDTH_PX = 2048

data class PageSize(val width: Float, val height: Float)

sealed interface OpenResult {
    data class Success(val pageCount: Int) : OpenResult
    data object NeedsPassword : OpenResult
    data object WrongPassword : OpenResult
    data object Corrupted : OpenResult
}

interface PdfEngine {
    /** Pode ser chamado de novo no mesmo arquivo com a senha. */
    suspend fun open(file: File, password: String? = null): OpenResult
    suspend fun pageSizes(): List<PageSize>
    /** Largura limitada a MAX_RENDER_WIDTH_PX. Pode lançar OutOfMemoryError. */
    suspend fun render(index: Int, widthPx: Int): Bitmap
    fun close()
}
```

`app/src/main/java/com/johngabie/johnpdf/engine/MuPdfEngine.kt`:
```kotlin
package com.johngabie.johnpdf.engine

import android.graphics.Bitmap
import android.graphics.Color
import android.util.LruCache
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.roundToInt
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext

/** Único ponto de contato com o MuPDF. Todas as chamadas rodam numa só thread (MuPDF não é thread-safe). */
class MuPdfEngine(private val maxRenderWidthPx: Int = MAX_RENDER_WIDTH_PX) : PdfEngine {
    private val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "mupdf") }
    private val dispatcher = executor.asCoroutineDispatcher()
    private var document: Document? = null
    private var authenticated = false
    private val cache = object : LruCache<Long, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()) {
        override fun sizeOf(key: Long, value: Bitmap) = value.byteCount / 1024
    }

    override suspend fun open(file: File, password: String?): OpenResult = withContext(dispatcher) {
        val doc = document ?: try {
            Document.openDocument(file.absolutePath).also { document = it }
        } catch (e: Exception) {
            return@withContext OpenResult.Corrupted
        }
        if (!authenticated && doc.needsPassword()) {
            if (password == null) return@withContext OpenResult.NeedsPassword
            if (!doc.authenticatePassword(password)) return@withContext OpenResult.WrongPassword
            authenticated = true
        }
        val count = try { doc.countPages() } catch (e: Exception) { 0 }
        if (count <= 0) OpenResult.Corrupted else OpenResult.Success(count)
    }

    override suspend fun pageSizes(): List<PageSize> = withContext(dispatcher) {
        val doc = checkNotNull(document) { "open() primeiro" }
        List(doc.countPages()) { i ->
            val page = doc.loadPage(i)
            try {
                val b = page.bounds
                PageSize(b.x1 - b.x0, b.y1 - b.y0)
            } finally {
                page.destroy()
            }
        }
    }

    override suspend fun render(index: Int, widthPx: Int): Bitmap = withContext(dispatcher) {
        val width = widthPx.coerceIn(1, maxRenderWidthPx)
        val key = (index.toLong() shl 32) or width.toLong()
        cache.get(key)?.let { return@withContext it }
        val doc = checkNotNull(document) { "open() primeiro" }
        val page = doc.loadPage(index)
        try {
            val b = page.bounds
            val scale = width / (b.x1 - b.x0)
            val height = ((b.y1 - b.y0) * scale).roundToInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            val device = AndroidDrawDevice(bitmap, 0, 0)
            try {
                page.run(device, Matrix(scale, 0f, 0f, scale, -b.x0 * scale, -b.y0 * scale), null)
                device.close()
            } finally {
                device.destroy()
            }
            cache.put(key, bitmap)
            bitmap
        } finally {
            page.destroy()
        }
    }

    /** Destrói o documento depois das tarefas já enfileiradas; chamadas novas são canceladas. */
    override fun close() {
        if (executor.isShutdown) return
        executor.execute {
            cache.evictAll()
            document?.destroy()
            document = null
        }
        executor.shutdown()
    }
}
```

- [ ] **Step 6: Rodar e ver passar**

Run: `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.johngabie.johnpdf.engine.MuPdfEngineTest`
Expected: PASS (12 testes). Se `render_draws_content_on_white_background` falhar com fundo transparente (`0`), o `AndroidDrawDevice` está limpando o bitmap. Nesse caso, desenhe num bitmap separado e componha sobre o branco (`Canvas(bitmapBranco).drawBitmap(desenhado, 0f, 0f, null)`). O teste não muda.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: PdfEngine sobre MuPDF com thread única, cache e senha"
```

---

### Task 4: Repositório de recentes (JSON)

**Files:**
- Create: `app/src/main/java/com/johngabie/johnpdf/data/RecentsRepository.kt`
- Test: `app/src/test/java/com/johngabie/johnpdf/data/RecentsRepositoryTest.kt`

**Interfaces:**
- Consumes: `Origin` (Task 2).
- Produces:
```kotlin
@Serializable data class RecentItem(val name: String, val origin: Origin, val path: String, val openedAt: Long, val imported: Boolean)
const val MAX_RECENTS = 20
class RecentsRepository(storeFile: File, ioDispatcher: CoroutineDispatcher = Dispatchers.IO, maxItems: Int = MAX_RECENTS) {
    val items: StateFlow<List<RecentItem>>      // mais recente primeiro
    suspend fun load()
    suspend fun add(item: RecentItem)           // dedup por path; sobe para o topo; excedentes saem (e a cópia importada é apagada)
    suspend fun remove(path: String)            // apaga a cópia se imported
}
```

- [ ] **Step 1: Escrever os testes que falham**

`app/src/test/java/com/johngabie/johnpdf/data/RecentsRepositoryTest.kt`:
```kotlin
package com.johngabie.johnpdf.data

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RecentsRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private val store get() = File(tmp.root, "recents.json")
    private fun repo(max: Int = MAX_RECENTS) = RecentsRepository(store, Dispatchers.Unconfined, max)
    private fun item(path: String, at: Long, imported: Boolean = false) =
        RecentItem(name = File(path).name, origin = Origin.OTHER, path = path, openedAt = at, imported = imported)

    @Test fun add_orders_newest_first() = runTest {
        val r = repo()
        r.add(item("/a.pdf", 1)); r.add(item("/b.pdf", 2))
        assertEquals(listOf("/b.pdf", "/a.pdf"), r.items.value.map { it.path })
    }

    @Test fun adding_same_path_moves_to_top_without_duplicate() = runTest {
        val r = repo()
        r.add(item("/a.pdf", 1)); r.add(item("/b.pdf", 2)); r.add(item("/a.pdf", 3))
        assertEquals(listOf("/a.pdf", "/b.pdf"), r.items.value.map { it.path })
    }

    @Test fun cap_evicts_oldest_and_deletes_its_imported_copy() = runTest {
        val r = repo(max = 2)
        val oldCopy = tmp.newFile("old.pdf")
        r.add(item(oldCopy.path, 1, imported = true)); r.add(item("/b.pdf", 2)); r.add(item("/c.pdf", 3))
        assertEquals(listOf("/c.pdf", "/b.pdf"), r.items.value.map { it.path })
        assertFalse(oldCopy.exists())
    }

    @Test fun evicting_non_imported_keeps_user_file() = runTest {
        val r = repo(max = 1)
        val userFile = tmp.newFile("user.pdf")
        r.add(item(userFile.path, 1)); r.add(item("/b.pdf", 2))
        assertTrue(userFile.exists())
    }

    @Test fun remove_deletes_imported_copy() = runTest {
        val r = repo()
        val copy = tmp.newFile("copy.pdf")
        r.add(item(copy.path, 1, imported = true))
        r.remove(copy.path)
        assertEquals(emptyList<RecentItem>(), r.items.value)
        assertFalse(copy.exists())
    }

    @Test fun persists_across_instances() = runTest {
        repo().add(item("/a.pdf", 1))
        val reloaded = repo().also { it.load() }
        assertEquals(listOf("/a.pdf"), reloaded.items.value.map { it.path })
    }

    @Test fun corrupted_store_loads_as_empty() = runTest {
        store.writeText("{isto não é json")
        val r = repo().also { it.load() }
        assertEquals(emptyList<RecentItem>(), r.items.value)
        r.add(item("/a.pdf", 1))
        assertEquals(1, r.items.value.size)
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./gradlew testDebugUnitTest --tests '*RecentsRepositoryTest*'`
Expected: FAIL na compilação (`Unresolved reference: RecentsRepository`).

- [ ] **Step 3: Implementar**

`app/src/main/java/com/johngabie/johnpdf/data/RecentsRepository.kt`:
```kotlin
package com.johngabie.johnpdf.data

import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val MAX_RECENTS = 20

@Serializable
data class RecentItem(
    val name: String,
    val origin: Origin,
    val path: String,
    val openedAt: Long,
    /** true = cópia em filesDir/imports, apagada quando o item sai da lista. */
    val imported: Boolean,
)

class RecentsRepository(
    private val storeFile: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val maxItems: Int = MAX_RECENTS,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private val _items = MutableStateFlow<List<RecentItem>>(emptyList())
    val items: StateFlow<List<RecentItem>> = _items.asStateFlow()

    suspend fun load() = mutex.withLock { _items.value = read() }

    suspend fun add(item: RecentItem) = mutex.withLock {
        val updated = (listOf(item) + read().filterNot { it.path == item.path })
            .sortedByDescending { it.openedAt }
        updated.drop(maxItems).forEach { deleteCopy(it) }
        write(updated.take(maxItems))
    }

    suspend fun remove(path: String) = mutex.withLock {
        val current = read()
        current.filter { it.path == path }.forEach { deleteCopy(it) }
        write(current.filterNot { it.path == path })
    }

    private suspend fun read(): List<RecentItem> = withContext(ioDispatcher) {
        if (!storeFile.exists()) return@withContext emptyList()
        runCatching { json.decodeFromString<List<RecentItem>>(storeFile.readText()) }.getOrDefault(emptyList())
    }

    private suspend fun write(list: List<RecentItem>) {
        withContext(ioDispatcher) {
            storeFile.parentFile?.mkdirs()
            val tmp = File(storeFile.path + ".tmp")
            tmp.writeText(json.encodeToString(list))
            if (!tmp.renameTo(storeFile)) {
                storeFile.writeText(tmp.readText())
                tmp.delete()
            }
        }
        _items.value = list
    }

    private suspend fun deleteCopy(item: RecentItem) {
        if (item.imported) withContext(ioDispatcher) { File(item.path).delete() }
    }
}
```

- [ ] **Step 4: Rodar e ver passar**

Run: `./gradlew testDebugUnitTest --tests '*RecentsRepositoryTest*'`
Expected: PASS (7 testes).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: repositório de recentes com limite de 20 e limpeza de cópias"
```

---

### Task 5: Importação de `content://` (Abrir com… / seletor)

**Files:**
- Create: `app/src/main/java/com/johngabie/johnpdf/data/ImportRepository.kt`
- Test: `app/src/test/java/com/johngabie/johnpdf/data/ImportRepositoryTest.kt`

**Interfaces:**
- Consumes: `Origin`, `originFromAuthority` (Task 2).
- Produces:
```kotlin
sealed interface ImportResult { data class Success(val file: File, val displayName: String, val origin: Origin); data object NoSpace; data object Failed }
fun interface Importer { suspend fun import(uri: Uri): ImportResult }
class ImportRepository(resolver: ContentResolver, importsDir: File, io: CoroutineDispatcher = Dispatchers.IO) : Importer
internal fun isNoSpace(e: Throwable): Boolean
```

- [ ] **Step 1: Escrever os testes que falham**

`app/src/test/java/com/johngabie/johnpdf/data/ImportRepositoryTest.kt`:
```kotlin
package com.johngabie.johnpdf.data

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class ImportRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val importsDir = File(context.filesDir, "imports")
    private val repo = ImportRepository(context.contentResolver, importsDir, Dispatchers.Unconfined)

    @Suppress("DEPRECATION")
    private fun register(uri: Uri, stream: InputStream) = shadowOf(context.contentResolver).registerInputStream(uri, stream)

    @Test fun copies_content_into_imports_dir() = runTest {
        val uri = Uri.parse("content://com.whatsapp.provider.media/item/42/Fatura.pdf")
        val bytes = "%PDF-1.7 conteudo".toByteArray()
        register(uri, ByteArrayInputStream(bytes))

        val result = repo.import(uri) as ImportResult.Success

        assertArrayEquals(bytes, result.file.readBytes())
        assertEquals(importsDir, result.file.parentFile)
        assertTrue(result.file.name.endsWith(".pdf"))
        assertEquals("Fatura.pdf", result.displayName)
        assertEquals(Origin.WHATSAPP, result.origin)
    }

    @Test fun same_uri_reuses_same_file_name() = runTest {
        val uri = Uri.parse("content://x/doc.pdf")
        register(uri, ByteArrayInputStream(byteArrayOf(1)))
        val first = repo.import(uri) as ImportResult.Success
        register(uri, ByteArrayInputStream(byteArrayOf(2)))
        val second = repo.import(uri) as ImportResult.Success
        assertEquals(first.file, second.file)
        assertEquals(1, importsDir.listFiles()!!.size)
    }

    @Test fun no_space_is_reported_and_partial_file_removed() = runTest {
        val uri = Uri.parse("content://x/grande.pdf")
        register(uri, object : InputStream() {
            override fun read(): Int = throw IOException("write failed: ENOSPC (No space left on device)")
        })
        assertEquals(ImportResult.NoSpace, repo.import(uri))
        assertTrue(importsDir.listFiles().orEmpty().none { it.name.endsWith(".part") })
    }

    @Test fun other_io_error_is_failed() = runTest {
        val uri = Uri.parse("content://x/quebrado.pdf")
        register(uri, object : InputStream() { override fun read(): Int = throw IOException("boom") })
        assertEquals(ImportResult.Failed, repo.import(uri))
    }

    @Test fun no_space_detection_looks_at_causes() {
        assertTrue(isNoSpace(IOException("wrapper", IOException("No space left on device"))))
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./gradlew testDebugUnitTest --tests '*ImportRepositoryTest*'`
Expected: FAIL na compilação (`Unresolved reference: ImportRepository`).

- [ ] **Step 3: Implementar**

`app/src/main/java/com/johngabie/johnpdf/data/ImportRepository.kt`:
```kotlin
package com.johngabie.johnpdf.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface ImportResult {
    data class Success(val file: File, val displayName: String, val origin: Origin) : ImportResult
    data object NoSpace : ImportResult
    data object Failed : ImportResult
}

fun interface Importer {
    suspend fun import(uri: Uri): ImportResult
}

/** Copia um PDF recebido por URI (acesso temporário) para filesDir/imports. */
class ImportRepository(
    private val resolver: ContentResolver,
    private val importsDir: File,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : Importer {

    override suspend fun import(uri: Uri): ImportResult = withContext(io) {
        val name = displayName(uri)
        val target = File(importsDir, "%08x.pdf".format(uri.toString().hashCode()))
        val partial = File(importsDir, target.name + ".part")
        try {
            importsDir.mkdirs()
            val input = resolver.openInputStream(uri) ?: return@withContext ImportResult.Failed
            input.use { i -> partial.outputStream().use { o -> i.copyTo(o) } }
            if (target.exists()) target.delete()
            if (!partial.renameTo(target)) return@withContext ImportResult.Failed
            ImportResult.Success(target, name, originFromAuthority(uri.authority))
        } catch (e: IOException) {
            if (isNoSpace(e)) ImportResult.NoSpace else ImportResult.Failed
        } catch (e: SecurityException) {
            ImportResult.Failed
        } finally {
            partial.delete()
        }
    }

    private fun displayName(uri: Uri): String =
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: "documento.pdf"
}

internal fun isNoSpace(e: Throwable): Boolean =
    generateSequence(e) { it.cause }.any { t ->
        val msg = t.message.orEmpty()
        "ENOSPC" in msg || msg.contains("No space left", ignoreCase = true)
    }
```

- [ ] **Step 4: Rodar e ver passar**

Run: `./gradlew testDebugUnitTest --tests '*ImportRepositoryTest*'`
Expected: PASS (5 testes).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: importação de PDFs recebidos por content:// com detecção de falta de espaço"
```

---

### Task 6: Lista de PDFs do aparelho (MediaStore) + checagem de permissão

**Files:**
- Create: `app/src/main/java/com/johngabie/johnpdf/data/PdfLibraryRepository.kt`
- Create: `app/src/main/java/com/johngabie/johnpdf/data/StorageAccess.kt`
- Test: `app/src/test/java/com/johngabie/johnpdf/data/PdfLibraryRepositoryTest.kt`

**Interfaces:**
- Consumes: `Origin`, `originFromPath` (Task 2).
- Produces:
```kotlin
data class PdfFile(val name: String, val path: String, val origin: Origin, val modifiedAt: Long)
fun interface PdfLibrary { suspend fun queryAll(): List<PdfFile> }
class PdfLibraryRepository(resolver: ContentResolver, io: CoroutineDispatcher = Dispatchers.IO) : PdfLibrary
internal fun parsePdfCursor(cursor: Cursor): List<PdfFile>   // ordenado por modifiedAt desc, sem paths nulos/duplicados
object StorageAccess { fun hasAllFilesAccess(context: Context): Boolean; fun settingsIntents(context: Context): List<Intent> }
```

- [ ] **Step 1: Escrever os testes que falham**

`app/src/test/java/com/johngabie/johnpdf/data/PdfLibraryRepositoryTest.kt`:
```kotlin
package com.johngabie.johnpdf.data

import android.database.MatrixCursor
import android.provider.MediaStore.MediaColumns
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PdfLibraryRepositoryTest {
    @Suppress("DEPRECATION")
    private fun cursor(vararg rows: Array<Any?>) =
        MatrixCursor(arrayOf(MediaColumns.DISPLAY_NAME, MediaColumns.DATA, MediaColumns.DATE_MODIFIED)).apply {
            rows.forEach { addRow(it) }
        }

    @Test fun maps_rows_and_converts_seconds_to_millis() {
        val result = parsePdfCursor(cursor(arrayOf("Fatura.pdf", "/storage/emulated/0/Download/Fatura.pdf", 1_700_000_000L)))
        assertEquals(listOf(PdfFile("Fatura.pdf", "/storage/emulated/0/Download/Fatura.pdf", Origin.DOWNLOAD, 1_700_000_000_000L)), result)
    }

    @Test fun skips_rows_without_path() {
        assertEquals(emptyList<PdfFile>(), parsePdfCursor(cursor(arrayOf("x.pdf", null, 1L))))
    }

    @Test fun falls_back_to_file_name_when_display_name_missing() {
        assertEquals("b.pdf", parsePdfCursor(cursor(arrayOf(null, "/sdcard/Documents/b.pdf", 1L))).single().name)
    }

    @Test fun sorts_newest_first_and_removes_duplicates() {
        val result = parsePdfCursor(cursor(
            arrayOf("a.pdf", "/sdcard/a.pdf", 10L),
            arrayOf("b.pdf", "/sdcard/b.pdf", 20L),
            arrayOf("a.pdf", "/sdcard/a.pdf", 10L),
        ))
        assertEquals(listOf("/sdcard/b.pdf", "/sdcard/a.pdf"), result.map { it.path })
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./gradlew testDebugUnitTest --tests '*PdfLibraryRepositoryTest*'`
Expected: FAIL na compilação (`Unresolved reference: parsePdfCursor`).

- [ ] **Step 3: Implementar**

`app/src/main/java/com/johngabie/johnpdf/data/PdfLibraryRepository.kt`:
```kotlin
package com.johngabie.johnpdf.data

import android.content.ContentResolver
import android.database.Cursor
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PdfFile(val name: String, val path: String, val origin: Origin, val modifiedAt: Long)

fun interface PdfLibrary {
    suspend fun queryAll(): List<PdfFile>
}

/** Lista os PDFs indexados pelo Android. Exige "acesso a todos os arquivos" (11+) ou READ_EXTERNAL_STORAGE (7–10). */
class PdfLibraryRepository(
    private val resolver: ContentResolver,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : PdfLibrary {

    override suspend fun queryAll(): List<PdfFile> = withContext(io) {
        resolver.query(
            MediaStore.Files.getContentUri("external"),
            PROJECTION,
            "${MediaColumns.MIME_TYPE} = ? OR ${DATA_COLUMN} LIKE ?",
            arrayOf("application/pdf", "%.pdf"),
            "${MediaColumns.DATE_MODIFIED} DESC",
        )?.use(::parsePdfCursor) ?: emptyList()
    }

    private companion object {
        @Suppress("DEPRECATION")
        const val DATA_COLUMN = MediaColumns.DATA
        val PROJECTION = arrayOf(MediaColumns.DISPLAY_NAME, DATA_COLUMN, MediaColumns.DATE_MODIFIED)
    }
}

@Suppress("DEPRECATION")
internal fun parsePdfCursor(cursor: Cursor): List<PdfFile> {
    val nameCol = cursor.getColumnIndexOrThrow(MediaColumns.DISPLAY_NAME)
    val dataCol = cursor.getColumnIndexOrThrow(MediaColumns.DATA)
    val dateCol = cursor.getColumnIndexOrThrow(MediaColumns.DATE_MODIFIED)
    val result = mutableListOf<PdfFile>()
    while (cursor.moveToNext()) {
        val path = cursor.getString(dataCol) ?: continue
        val name = cursor.getString(nameCol) ?: File(path).name
        result += PdfFile(name, path, originFromPath(path), cursor.getLong(dateCol) * 1000)
    }
    return result.distinctBy { it.path }.sortedByDescending { it.modifiedAt }
}
```

`app/src/main/java/com/johngabie/johnpdf/data/StorageAccess.kt`:
```kotlin
package com.johngabie.johnpdf.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat

object StorageAccess {
    fun hasAllFilesAccess(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }

    /** Android 11+: tela do app primeiro; se o aparelho não tiver, a lista geral. */
    fun settingsIntents(context: Context): List<Intent> = listOf(
        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}")),
        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
    )
}
```

- [ ] **Step 4: Rodar e ver passar**

Run: `./gradlew testDebugUnitTest --tests '*PdfLibraryRepositoryTest*'`
Expected: PASS (4 testes).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: consulta de PDFs no MediaStore e checagem de acesso a arquivos"
```

---

### Task 7: Caso de uso de abertura (`OpenPdfUseCase`)

**Files:**
- Create: `app/src/main/java/com/johngabie/johnpdf/data/OpenPdfUseCase.kt`
- Test: `app/src/test/java/com/johngabie/johnpdf/data/OpenPdfUseCaseTest.kt`

**Interfaces:**
- Consumes: `Importer`/`ImportResult` (Task 5), `RecentsRepository`/`RecentItem` (Task 4), `PdfFile` (Task 6), `AppError` (Task 2).
- Produces:
```kotlin
sealed interface OpenOutcome { data class Ready(val path: String, val name: String); data class Failed(val error: AppError) }
class OpenPdfUseCase(importer: Importer, recents: RecentsRepository, clock: () -> Long = System::currentTimeMillis) {
    suspend fun openUri(uri: Uri): OpenOutcome
    suspend fun openFile(pdf: PdfFile): OpenOutcome
    suspend fun openRecent(item: RecentItem): OpenOutcome   // arquivo sumiu → remove dos recentes + Failed(GONE)
}
```

- [ ] **Step 1: Escrever os testes que falham**

`app/src/test/java/com/johngabie/johnpdf/data/OpenPdfUseCaseTest.kt`:
```kotlin
package com.johngabie.johnpdf.data

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpenPdfUseCaseTest {
    @get:Rule val tmp = TemporaryFolder()
    private val recents by lazy { RecentsRepository(File(tmp.root, "r.json"), Dispatchers.Unconfined) }
    private var importResult: ImportResult = ImportResult.Failed
    private val useCase by lazy { OpenPdfUseCase({ importResult }, recents, clock = { 1000L }) }

    @Test fun open_uri_success_registers_imported_recent() = runTest {
        val copy = tmp.newFile("copy.pdf")
        importResult = ImportResult.Success(copy, "Fatura.pdf", Origin.WHATSAPP)

        assertEquals(OpenOutcome.Ready(copy.path, "Fatura.pdf"), useCase.openUri(Uri.parse("content://x/1")))
        assertEquals(listOf(RecentItem("Fatura.pdf", Origin.WHATSAPP, copy.path, 1000L, imported = true)), recents.items.value)
    }

    @Test fun open_uri_no_space_fails() = runTest {
        importResult = ImportResult.NoSpace
        assertEquals(OpenOutcome.Failed(AppError.NO_SPACE), useCase.openUri(Uri.parse("content://x/1")))
    }

    @Test fun open_uri_generic_failure_is_corrupted() = runTest {
        importResult = ImportResult.Failed
        assertEquals(OpenOutcome.Failed(AppError.CORRUPTED), useCase.openUri(Uri.parse("content://x/1")))
    }

    @Test fun open_file_registers_non_imported_recent() = runTest {
        val f = tmp.newFile("a.pdf")
        assertEquals(OpenOutcome.Ready(f.path, "a.pdf"), useCase.openFile(PdfFile("a.pdf", f.path, Origin.DOWNLOAD, 5L)))
        assertEquals(false, recents.items.value.single().imported)
    }

    @Test fun open_missing_file_is_gone() = runTest {
        assertEquals(OpenOutcome.Failed(AppError.GONE), useCase.openFile(PdfFile("x.pdf", "/nao/existe.pdf", Origin.OTHER, 1L)))
    }

    @Test fun open_missing_recent_removes_it() = runTest {
        recents.add(RecentItem("x.pdf", Origin.OTHER, "/nao/existe.pdf", 1L, imported = false))
        assertEquals(OpenOutcome.Failed(AppError.GONE), useCase.openRecent(recents.items.value.single()))
        assertEquals(emptyList<RecentItem>(), recents.items.value)
    }

    @Test fun open_recent_bumps_timestamp() = runTest {
        val f = tmp.newFile("a.pdf")
        recents.add(RecentItem("a.pdf", Origin.OTHER, f.path, 1L, imported = false))
        useCase.openRecent(recents.items.value.single())
        assertEquals(1000L, recents.items.value.single().openedAt)
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./gradlew testDebugUnitTest --tests '*OpenPdfUseCaseTest*'`
Expected: FAIL na compilação (`Unresolved reference: OpenPdfUseCase`).

- [ ] **Step 3: Implementar**

`app/src/main/java/com/johngabie/johnpdf/data/OpenPdfUseCase.kt`:
```kotlin
package com.johngabie.johnpdf.data

import android.net.Uri
import java.io.File

sealed interface OpenOutcome {
    data class Ready(val path: String, val name: String) : OpenOutcome
    data class Failed(val error: AppError) : OpenOutcome
}

/** Resolve de onde vem o PDF, registra nos recentes e devolve o caminho que o leitor deve abrir. */
class OpenPdfUseCase(
    private val importer: Importer,
    private val recents: RecentsRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun openUri(uri: Uri): OpenOutcome = when (val r = importer.import(uri)) {
        is ImportResult.Success -> register(RecentItem(r.displayName, r.origin, r.file.absolutePath, clock(), imported = true))
        ImportResult.NoSpace -> OpenOutcome.Failed(AppError.NO_SPACE)
        ImportResult.Failed -> OpenOutcome.Failed(AppError.CORRUPTED)
    }

    suspend fun openFile(pdf: PdfFile): OpenOutcome =
        if (!File(pdf.path).exists()) OpenOutcome.Failed(AppError.GONE)
        else register(RecentItem(pdf.name, pdf.origin, pdf.path, clock(), imported = false))

    suspend fun openRecent(item: RecentItem): OpenOutcome =
        if (!File(item.path).exists()) {
            recents.remove(item.path)
            OpenOutcome.Failed(AppError.GONE)
        } else {
            register(item.copy(openedAt = clock()))
        }

    private suspend fun register(item: RecentItem): OpenOutcome {
        recents.add(item)
        return OpenOutcome.Ready(item.path, item.name)
    }
}
```

- [ ] **Step 4: Rodar e ver passar**

Run: `./gradlew testDebugUnitTest --tests '*OpenPdfUseCaseTest*'`
Expected: PASS (7 testes).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: caso de uso de abertura (URI, arquivo, recente)"
```

---

### Task 8: `ReaderViewModel` + preferência de rotação

**Files:**
- Create: `app/src/main/java/com/johngabie/johnpdf/data/SettingsRepository.kt`
- Create: `app/src/main/java/com/johngabie/johnpdf/ui/reader/ReaderViewModel.kt`
- Create: `app/src/test/java/com/johngabie/johnpdf/testutil/FakePdfEngine.kt`
- Create: `app/src/test/java/com/johngabie/johnpdf/testutil/MainDispatcherRule.kt`
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/reader/ReaderViewModelTest.kt`, `app/src/test/java/com/johngabie/johnpdf/data/SettingsRepositoryTest.kt`

**Interfaces:**
- Consumes: `PdfEngine`, `OpenResult`, `PageSize` (Task 3); `AppError` (Task 2).
- Produces:
```kotlin
interface RotationLockSetting { val rotationLocked: Flow<Boolean>; suspend fun setRotationLocked(locked: Boolean) }
class SettingsRepository(dataStore: DataStore<Preferences>) : RotationLockSetting
sealed interface ReaderStatus { data object Loading; data class NeedsPassword(val wrongAttempt: Boolean); data class Ready(val pageSizes: List<PageSize>); data class Failed(val error: AppError) }
data class ReaderUiState(val title: String, val status: ReaderStatus = Loading, val currentPage: Int = 0, val zoom: Float = 1f, val rotationLocked: Boolean = false) { val pageCount: Int }
class ReaderViewModel(file: File, title: String, engine: PdfEngine, settings: RotationLockSetting) : ViewModel() {
    val state: StateFlow<ReaderUiState>
    fun submitPassword(password: String); fun onPageVisible(index: Int); fun setZoom(zoom: Float)
    fun toggleDoubleTapZoom(); fun toggleRotationLock()
    suspend fun renderPage(index: Int, widthPx: Int): Bitmap?   // null = falhou (mostrar PAGE_RENDER_FAILED_MESSAGE)
    companion object { MIN_ZOOM = 1f; MAX_ZOOM = 4f; DOUBLE_TAP_ZOOM = 2.5f }
}
// test utils:
class FakePdfEngine(var sizes: List<PageSize> = List(3) { PageSize(595f, 842f) }, var password: String? = null, var corrupted: Boolean = false) : PdfEngine {
    var closed: Boolean; val renderFailures: ArrayDeque<Throwable>; val renderRequests: MutableList<Pair<Int, Int>>
}
class MainDispatcherRule(dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher
```

- [ ] **Step 1: Criar os utilitários de teste**

`app/src/test/java/com/johngabie/johnpdf/testutil/MainDispatcherRule.kt`:
```kotlin
package com.johngabie.johnpdf.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
```

`app/src/test/java/com/johngabie/johnpdf/testutil/FakePdfEngine.kt`:
```kotlin
package com.johngabie.johnpdf.testutil

import android.graphics.Bitmap
import com.johngabie.johnpdf.engine.OpenResult
import com.johngabie.johnpdf.engine.PageSize
import com.johngabie.johnpdf.engine.PdfEngine
import java.io.File

/** Requer Robolectric (usa Bitmap). */
class FakePdfEngine(
    var sizes: List<PageSize> = List(3) { PageSize(595f, 842f) },
    var password: String? = null,
    var corrupted: Boolean = false,
) : PdfEngine {
    var closed = false
    val renderFailures = ArrayDeque<Throwable>()
    val renderRequests = mutableListOf<Pair<Int, Int>>()

    override suspend fun open(file: File, password: String?): OpenResult = when {
        corrupted -> OpenResult.Corrupted
        this.password == null -> OpenResult.Success(sizes.size)
        password == null -> OpenResult.NeedsPassword
        password != this.password -> OpenResult.WrongPassword
        else -> OpenResult.Success(sizes.size)
    }

    override suspend fun pageSizes(): List<PageSize> = sizes

    override suspend fun render(index: Int, widthPx: Int): Bitmap {
        renderRequests += index to widthPx
        renderFailures.removeFirstOrNull()?.let { throw it }
        return Bitmap.createBitmap(widthPx, widthPx, Bitmap.Config.ARGB_8888)
    }

    override fun close() { closed = true }
}
```

- [ ] **Step 2: Escrever os testes que falham**

`app/src/test/java/com/johngabie/johnpdf/data/SettingsRepositoryTest.kt`:
```kotlin
package com.johngabie.johnpdf.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun rotation_lock_defaults_false_and_persists() = runTest {
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope) { File(tmp.root, "s.preferences_pb") }
        val repo = SettingsRepository(store)
        assertFalse(repo.rotationLocked.first())
        repo.setRotationLocked(true)
        assertTrue(repo.rotationLocked.first())
    }
}
```

`app/src/test/java/com/johngabie/johnpdf/ui/reader/ReaderViewModelTest.kt`:
```kotlin
package com.johngabie.johnpdf.ui.reader

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.johngabie.johnpdf.data.AppError
import com.johngabie.johnpdf.data.RotationLockSetting
import com.johngabie.johnpdf.engine.PageSize
import com.johngabie.johnpdf.testutil.FakePdfEngine
import com.johngabie.johnpdf.testutil.MainDispatcherRule
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReaderViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private class FakeRotation : RotationLockSetting {
        override val rotationLocked = MutableStateFlow(false)
        override suspend fun setRotationLocked(locked: Boolean) { rotationLocked.value = locked }
    }

    private val engine = FakePdfEngine()
    private val rotation = FakeRotation()
    private fun vm() = ReaderViewModel(File("/x.pdf"), "x.pdf", engine, rotation)

    @Test fun opens_ready_with_page_sizes() {
        val s = vm().state.value
        assertEquals("x.pdf", s.title)
        assertEquals(ReaderStatus.Ready(List(3) { PageSize(595f, 842f) }), s.status)
        assertEquals(3, s.pageCount)
    }

    @Test fun password_flow() {
        engine.password = "1234"
        val vm = vm()
        assertEquals(ReaderStatus.NeedsPassword(wrongAttempt = false), vm.state.value.status)
        vm.submitPassword("0000")
        assertEquals(ReaderStatus.NeedsPassword(wrongAttempt = true), vm.state.value.status)
        vm.submitPassword("1234")
        assertTrue(vm.state.value.status is ReaderStatus.Ready)
    }

    @Test fun corrupted_fails() {
        engine.corrupted = true
        assertEquals(ReaderStatus.Failed(AppError.CORRUPTED), vm().state.value.status)
    }

    @Test fun zoom_is_clamped() {
        val vm = vm()
        vm.setZoom(0.3f); assertEquals(1f, vm.state.value.zoom)
        vm.setZoom(10f); assertEquals(4f, vm.state.value.zoom)
    }

    @Test fun double_tap_toggles_between_1_and_2_5() {
        val vm = vm()
        vm.toggleDoubleTapZoom(); assertEquals(2.5f, vm.state.value.zoom)
        vm.toggleDoubleTapZoom(); assertEquals(1f, vm.state.value.zoom)
        vm.setZoom(3.2f); vm.toggleDoubleTapZoom(); assertEquals(1f, vm.state.value.zoom)
    }

    @Test fun page_visible_is_clamped() {
        val vm = vm()
        vm.onPageVisible(1); assertEquals(1, vm.state.value.currentPage)
        vm.onPageVisible(99); assertEquals(2, vm.state.value.currentPage)
    }

    @Test fun render_oom_retries_at_half_width() = runTest {
        engine.renderFailures += OutOfMemoryError("fake")
        val bmp = vm().renderPage(0, 800)
        assertEquals(400, bmp!!.width)
        assertEquals(listOf(0 to 800, 0 to 400), engine.renderRequests)
    }

    @Test fun render_oom_twice_returns_null() = runTest {
        engine.renderFailures += OutOfMemoryError("1"); engine.renderFailures += OutOfMemoryError("2")
        assertNull(vm().renderPage(0, 800))
    }

    @Test fun render_runtime_exception_returns_null() = runTest {
        engine.renderFailures += RuntimeException("página quebrada")
        assertNull(vm().renderPage(0, 800))
    }

    @Test fun rotation_lock_toggles_through_setting() {
        val vm = vm()
        vm.toggleRotationLock()
        assertTrue(rotation.rotationLocked.value)
        assertTrue(vm.state.value.rotationLocked)
    }

    @Test fun clearing_view_model_closes_engine() {
        val store = ViewModelStore()
        ViewModelProvider(store, viewModelFactory { initializer { vm() } })[ReaderViewModel::class.java]
        store.clear()
        assertTrue(engine.closed)
    }
}
```

- [ ] **Step 3: Rodar e ver falhar**

Run: `./gradlew testDebugUnitTest --tests '*ReaderViewModelTest*' --tests '*SettingsRepositoryTest*'`
Expected: FAIL na compilação (`Unresolved reference: ReaderViewModel`, `SettingsRepository`).

- [ ] **Step 4: Implementar**

`app/src/main/java/com/johngabie/johnpdf/data/SettingsRepository.kt`:
```kotlin
package com.johngabie.johnpdf.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface RotationLockSetting {
    val rotationLocked: Flow<Boolean>
    suspend fun setRotationLocked(locked: Boolean)
}

class SettingsRepository(private val dataStore: DataStore<Preferences>) : RotationLockSetting {
    override val rotationLocked: Flow<Boolean> = dataStore.data.map { it[ROTATION_LOCKED] ?: false }

    override suspend fun setRotationLocked(locked: Boolean) {
        dataStore.edit { it[ROTATION_LOCKED] = locked }
    }

    private companion object {
        val ROTATION_LOCKED = booleanPreferencesKey("rotation_locked")
    }
}
```

`app/src/main/java/com/johngabie/johnpdf/ui/reader/ReaderViewModel.kt`:
```kotlin
package com.johngabie.johnpdf.ui.reader

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.johngabie.johnpdf.data.AppError
import com.johngabie.johnpdf.data.RotationLockSetting
import com.johngabie.johnpdf.engine.OpenResult
import com.johngabie.johnpdf.engine.PageSize
import com.johngabie.johnpdf.engine.PdfEngine
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ReaderStatus {
    data object Loading : ReaderStatus
    data class NeedsPassword(val wrongAttempt: Boolean) : ReaderStatus
    data class Ready(val pageSizes: List<PageSize>) : ReaderStatus
    data class Failed(val error: AppError) : ReaderStatus
}

data class ReaderUiState(
    val title: String,
    val status: ReaderStatus = ReaderStatus.Loading,
    val currentPage: Int = 0,
    val zoom: Float = 1f,
    val rotationLocked: Boolean = false,
) {
    val pageCount: Int get() = (status as? ReaderStatus.Ready)?.pageSizes?.size ?: 0
}

class ReaderViewModel(
    private val file: File,
    title: String,
    private val engine: PdfEngine,
    private val settings: RotationLockSetting,
) : ViewModel() {
    private val _state = MutableStateFlow(ReaderUiState(title))
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settings.rotationLocked.collect { locked -> _state.update { it.copy(rotationLocked = locked) } }
        }
        open(password = null)
    }

    fun submitPassword(password: String) = open(password)

    private fun open(password: String?) {
        viewModelScope.launch {
            val status = try {
                when (engine.open(file, password)) {
                    is OpenResult.Success -> ReaderStatus.Ready(engine.pageSizes())
                    OpenResult.NeedsPassword -> ReaderStatus.NeedsPassword(wrongAttempt = false)
                    OpenResult.WrongPassword -> ReaderStatus.NeedsPassword(wrongAttempt = true)
                    OpenResult.Corrupted -> ReaderStatus.Failed(AppError.CORRUPTED)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ReaderStatus.Failed(AppError.CORRUPTED)
            }
            _state.update { it.copy(status = status) }
        }
    }

    fun onPageVisible(index: Int) =
        _state.update { it.copy(currentPage = index.coerceIn(0, maxOf(0, it.pageCount - 1))) }

    fun setZoom(zoom: Float) = _state.update { it.copy(zoom = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)) }

    fun toggleDoubleTapZoom() =
        _state.update { it.copy(zoom = if (it.zoom > MIN_ZOOM) MIN_ZOOM else DOUBLE_TAP_ZOOM) }

    fun toggleRotationLock() {
        viewModelScope.launch { settings.setRotationLocked(!_state.value.rotationLocked) }
    }

    /** null = não foi possível renderizar (mostrar PAGE_RENDER_FAILED_MESSAGE). Só OOM tenta de novo, com metade da largura. */
    suspend fun renderPage(index: Int, widthPx: Int): Bitmap? = try {
        engine.render(index, widthPx)
    } catch (e: OutOfMemoryError) {
        tryRender(index, widthPx / 2)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private suspend fun tryRender(index: Int, widthPx: Int): Bitmap? = try {
        engine.render(index, widthPx)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        null
    }

    override fun onCleared() = engine.close()

    companion object {
        const val MIN_ZOOM = 1f
        const val MAX_ZOOM = 4f
        const val DOUBLE_TAP_ZOOM = 2.5f
    }
}
```

- [ ] **Step 5: Rodar e ver passar**

Run: `./gradlew testDebugUnitTest --tests '*ReaderViewModelTest*' --tests '*SettingsRepositoryTest*'`
Expected: PASS (12 testes).

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: ReaderViewModel (senha, zoom, página atual, OOM) e preferência de rotação"
```

---

### Task 9: `HomeViewModel` + rotas de navegação

**Files:**
- Create: `app/src/main/java/com/johngabie/johnpdf/ui/Routes.kt`
- Create: `app/src/main/java/com/johngabie/johnpdf/ui/home/HomeViewModel.kt`
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/home/HomeViewModelTest.kt`

**Interfaces:**
- Consumes: `RecentsRepository` (Task 4), `PdfLibrary`/`PdfFile` (Task 6), `OpenPdfUseCase`/`OpenOutcome` (Task 7), `Importer`/`ImportResult` (Task 5), `filterByName` (Task 2), `MainDispatcherRule` (Task 8).
- Produces:
```kotlin
@Serializable data object HomeRoute
@Serializable data class ReaderRoute(val path: String, val title: String)
enum class HomeTab { RECENTS, ALL }
data class HomeUiState(tab, recents: List<RecentItem>, allPdfs: List<PdfFile>, query: String, hasFilesAccess: Boolean, loadingAll: Boolean, busy: Boolean, error: AppError?) { val filteredPdfs: List<PdfFile> }
class HomeViewModel(recents: RecentsRepository, library: PdfLibrary, openPdf: OpenPdfUseCase, hasFilesAccess: () -> Boolean) : ViewModel() {
    val state: StateFlow<HomeUiState>; val navigation: Flow<ReaderRoute>
    fun refresh(); fun selectTab(tab: HomeTab); fun setQuery(query: String)
    fun openUri(uri: Uri); fun openFile(pdf: PdfFile); fun openRecent(item: RecentItem); fun removeRecent(item: RecentItem); fun dismissError()
}
```

- [ ] **Step 1: Escrever os testes que falham**

`app/src/test/java/com/johngabie/johnpdf/ui/home/HomeViewModelTest.kt`:
```kotlin
package com.johngabie.johnpdf.ui.home

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.johngabie.johnpdf.data.AppError
import com.johngabie.johnpdf.data.ImportResult
import com.johngabie.johnpdf.data.OpenPdfUseCase
import com.johngabie.johnpdf.data.Origin
import com.johngabie.johnpdf.data.PdfFile
import com.johngabie.johnpdf.data.PdfLibrary
import com.johngabie.johnpdf.data.RecentItem
import com.johngabie.johnpdf.data.RecentsRepository
import com.johngabie.johnpdf.testutil.MainDispatcherRule
import com.johngabie.johnpdf.ui.ReaderRoute
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    @get:Rule val tmp = TemporaryFolder()

    private val recents by lazy { RecentsRepository(File(tmp.root, "r.json"), Dispatchers.Unconfined) }
    private var access = false
    private var libraryCalls = 0
    private var library: PdfLibrary = PdfLibrary { libraryCalls++; pdfs }
    private var pdfs = listOf(
        PdfFile("Fatura Setembro.pdf", "/sdcard/Download/f.pdf", Origin.DOWNLOAD, 2L),
        PdfFile("Receita médica.pdf", "/sdcard/Documents/r.pdf", Origin.DOCUMENTS, 1L),
    )
    private var importResult: ImportResult = ImportResult.Failed
    private fun vm() = HomeViewModel(recents, library, OpenPdfUseCase({ importResult }, recents, { 100L }), { access })

    @Test fun refresh_without_access_does_not_query() {
        val vm = vm()
        vm.refresh()
        assertFalse(vm.state.value.hasFilesAccess)
        assertEquals(0, libraryCalls)
    }

    @Test fun refresh_with_access_loads_pdfs() {
        access = true
        val vm = vm()
        vm.refresh()
        assertTrue(vm.state.value.hasFilesAccess)
        assertEquals(pdfs, vm.state.value.allPdfs)
        assertFalse(vm.state.value.loadingAll)
    }

    @Test fun library_failure_results_in_empty_list() {
        access = true
        library = PdfLibrary { throw SecurityException("revogada") }
        val vm = vm()
        vm.refresh()
        assertEquals(emptyList<PdfFile>(), vm.state.value.allPdfs)
    }

    @Test fun query_filters_ignoring_accents() {
        access = true
        val vm = vm()
        vm.refresh()
        vm.setQuery("medica")
        assertEquals(listOf("Receita médica.pdf"), vm.state.value.filteredPdfs.map { it.name })
    }

    @Test fun open_existing_file_navigates_and_adds_recent() = runTest {
        val f = tmp.newFile("a.pdf")
        val vm = vm()
        vm.navigation.test {
            vm.openFile(PdfFile("a.pdf", f.path, Origin.OTHER, 1L))
            assertEquals(ReaderRoute(f.path, "a.pdf"), awaitItem())
        }
        assertEquals(listOf(f.path), vm.state.value.recents.map { it.path })
    }

    @Test fun open_missing_recent_shows_gone_and_removes() = runTest {
        recents.add(RecentItem("x.pdf", Origin.OTHER, "/nao/existe.pdf", 1L, imported = false))
        val vm = vm()
        vm.openRecent(vm.state.value.recents.single())
        assertEquals(AppError.GONE, vm.state.value.error)
        assertEquals(emptyList<RecentItem>(), vm.state.value.recents)
        vm.dismissError()
        assertEquals(null, vm.state.value.error)
    }

    @Test fun open_uri_without_space_shows_error() {
        importResult = ImportResult.NoSpace
        val vm = vm()
        vm.openUri(Uri.parse("content://x/1"))
        assertEquals(AppError.NO_SPACE, vm.state.value.error)
        assertFalse(vm.state.value.busy)
    }

    @Test fun remove_recent_updates_list() = runTest {
        recents.add(RecentItem("a.pdf", Origin.OTHER, "/a.pdf", 1L, imported = false))
        val vm = vm()
        vm.removeRecent(vm.state.value.recents.single())
        assertEquals(emptyList<RecentItem>(), vm.state.value.recents)
    }

    @Test fun select_tab() {
        val vm = vm()
        vm.selectTab(HomeTab.ALL)
        assertEquals(HomeTab.ALL, vm.state.value.tab)
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./gradlew testDebugUnitTest --tests '*HomeViewModelTest*'`
Expected: FAIL na compilação (`Unresolved reference: HomeViewModel`).

- [ ] **Step 3: Implementar**

`app/src/main/java/com/johngabie/johnpdf/ui/Routes.kt`:
```kotlin
package com.johngabie.johnpdf.ui

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data class ReaderRoute(val path: String, val title: String)
```

`app/src/main/java/com/johngabie/johnpdf/ui/home/HomeViewModel.kt`:
```kotlin
package com.johngabie.johnpdf.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.johngabie.johnpdf.data.AppError
import com.johngabie.johnpdf.data.OpenOutcome
import com.johngabie.johnpdf.data.OpenPdfUseCase
import com.johngabie.johnpdf.data.PdfFile
import com.johngabie.johnpdf.data.PdfLibrary
import com.johngabie.johnpdf.data.RecentItem
import com.johngabie.johnpdf.data.RecentsRepository
import com.johngabie.johnpdf.ui.ReaderRoute
import com.johngabie.johnpdf.util.filterByName
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HomeTab { RECENTS, ALL }

data class HomeUiState(
    val tab: HomeTab = HomeTab.RECENTS,
    val recents: List<RecentItem> = emptyList(),
    val allPdfs: List<PdfFile> = emptyList(),
    val query: String = "",
    val hasFilesAccess: Boolean = false,
    val loadingAll: Boolean = false,
    val busy: Boolean = false,
    val error: AppError? = null,
) {
    val filteredPdfs: List<PdfFile> get() = filterByName(allPdfs, query) { it.name }
}

class HomeViewModel(
    private val recents: RecentsRepository,
    private val library: PdfLibrary,
    private val openPdf: OpenPdfUseCase,
    private val hasFilesAccess: () -> Boolean,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _navigation = Channel<ReaderRoute>(Channel.BUFFERED)
    val navigation: Flow<ReaderRoute> = _navigation.receiveAsFlow()

    init {
        viewModelScope.launch { recents.load() }
        viewModelScope.launch { recents.items.collect { items -> _state.update { it.copy(recents = items) } } }
    }

    /** Chamado a cada ON_RESUME: a permissão pode ter mudado nas configurações. */
    fun refresh() {
        val access = hasFilesAccess()
        _state.update { it.copy(hasFilesAccess = access) }
        if (!access) return
        viewModelScope.launch {
            _state.update { it.copy(loadingAll = true) }
            val pdfs = try {
                library.queryAll()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
            _state.update { it.copy(allPdfs = pdfs, loadingAll = false) }
        }
    }

    fun selectTab(tab: HomeTab) = _state.update { it.copy(tab = tab) }
    fun setQuery(query: String) = _state.update { it.copy(query = query) }
    fun dismissError() = _state.update { it.copy(error = null) }

    fun openUri(uri: Uri) = launchOpen { openPdf.openUri(uri) }
    fun openFile(pdf: PdfFile) = launchOpen { openPdf.openFile(pdf) }
    fun openRecent(item: RecentItem) = launchOpen { openPdf.openRecent(item) }

    fun removeRecent(item: RecentItem) {
        viewModelScope.launch { recents.remove(item.path) }
    }

    private fun launchOpen(block: suspend () -> OpenOutcome) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            when (val outcome = block()) {
                is OpenOutcome.Ready -> _navigation.send(ReaderRoute(outcome.path, outcome.name))
                is OpenOutcome.Failed -> _state.update { it.copy(error = outcome.error) }
            }
            _state.update { it.copy(busy = false) }
        }
    }
}
```

- [ ] **Step 4: Rodar e ver passar**

Run: `./gradlew testDebugUnitTest --tests '*HomeViewModelTest*'`
Expected: PASS (9 testes).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: HomeViewModel (abas, busca, abertura, erros) e rotas"
```

---

### Task 10: Tema acessível + componentes comuns (botão grande e diálogos)

**Files:**
- Create: `app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/johngabie/johnpdf/ui/common/BigButton.kt`
- Create: `app/src/main/java/com/johngabie/johnpdf/ui/common/Dialogs.kt`
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/common/DialogsTest.kt`

**Interfaces:**
- Consumes: `AppError` (Task 2).
- Produces:
```kotlin
val MinTouchTarget: Dp /* 64.dp */; val PageGapColor: Color
@Composable fun JohnPdfTheme(content: @Composable () -> Unit)
@Composable fun BigButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true)
@Composable fun ErrorDialog(error: AppError, onDismiss: () -> Unit)
@Composable fun ConfirmDialog(question: String, onYes: () -> Unit, onNo: () -> Unit)
@Composable fun PasswordDialog(wrongAttempt: Boolean, onSubmit: (String) -> Unit, onCancel: () -> Unit)
```

- [ ] **Step 1: Escrever os testes que falham**

`app/src/test/java/com/johngabie/johnpdf/ui/common/DialogsTest.kt`:
```kotlin
package com.johngabie.johnpdf.ui.common

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.johngabie.johnpdf.data.AppError
import com.johngabie.johnpdf.ui.theme.JohnPdfTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DialogsTest {
    @get:Rule val rule = createComposeRule()

    @Test fun error_dialog_shows_message_and_ok_dismisses() {
        var dismissed = false
        rule.setContent { JohnPdfTheme { ErrorDialog(AppError.GONE) { dismissed = true } } }
        rule.onNodeWithText("Este arquivo não está mais disponível.").assertIsDisplayed()
        rule.onNodeWithText("OK").performClick()
        assertTrue(dismissed)
    }

    @Test fun confirm_dialog_yes() {
        var yes = false
        rule.setContent { JohnPdfTheme { ConfirmDialog("Remover da lista?", onYes = { yes = true }, onNo = {}) } }
        rule.onNodeWithText("Remover da lista?").assertIsDisplayed()
        rule.onNodeWithText("Sim").performClick()
        assertTrue(yes)
    }

    @Test fun password_dialog_submits_typed_password() {
        var submitted: String? = null
        rule.setContent { JohnPdfTheme { PasswordDialog(wrongAttempt = false, onSubmit = { submitted = it }, onCancel = {}) } }
        rule.onNodeWithText("Abrir").assertIsNotEnabled()
        rule.onNodeWithTag("password_field").performTextInput("1234")
        rule.onNodeWithText("Abrir").performClick()
        assertEquals("1234", submitted)
    }

    @Test fun password_dialog_shows_wrong_attempt_message() {
        rule.setContent { JohnPdfTheme { PasswordDialog(wrongAttempt = true, onSubmit = {}, onCancel = {}) } }
        rule.onNodeWithText("Senha incorreta, tente de novo").assertIsDisplayed()
    }

    @Test fun password_dialog_toggles_keyboard_label() {
        rule.setContent { JohnPdfTheme { PasswordDialog(wrongAttempt = false, onSubmit = {}, onCancel = {}) } }
        rule.onNodeWithText("abc  Usar letras").performClick()
        rule.onNodeWithText("123  Usar números").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./gradlew testDebugUnitTest --tests '*DialogsTest*'`
Expected: FAIL na compilação (`Unresolved reference: JohnPdfTheme`).

- [ ] **Step 3: Implementar**

`app/src/main/java/com/johngabie/johnpdf/ui/theme/Theme.kt`:
```kotlin
package com.johngabie.johnpdf.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val MinTouchTarget = 64.dp
val PageGapColor = Color(0xFFBDBDBD)

private val Colors = lightColorScheme(
    primary = Color(0xFF0B4F9C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4F7),
    onPrimaryContainer = Color(0xFF0A2540),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF111111),
    surface = Color.White,
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFE8EDF3),
    onSurfaceVariant = Color(0xFF333333),
    error = Color(0xFFB00020),
)

private val Base = Typography()
private val BigTypography = Typography(
    headlineMedium = Base.headlineMedium.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold),
    titleLarge = Base.titleLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.copy(fontSize = 20.sp, lineHeight = 28.sp),
    bodyMedium = Base.bodyMedium.copy(fontSize = 20.sp, lineHeight = 28.sp),
    labelLarge = Base.labelLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = Base.labelMedium.copy(fontSize = 20.sp),
)

@Composable
fun JohnPdfTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = BigTypography, content = content)
}
```

`app/src/main/java/com/johngabie/johnpdf/ui/common/BigButton.kt`:
```kotlin
package com.johngabie.johnpdf.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.johngabie.johnpdf.ui.theme.MinTouchTarget

@Composable
fun BigButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = MinTouchTarget),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}
```

`app/src/main/java/com/johngabie/johnpdf/ui/common/Dialogs.kt`:
```kotlin
package com.johngabie.johnpdf.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.johngabie.johnpdf.data.AppError
import com.johngabie.johnpdf.ui.theme.MinTouchTarget

@Composable
fun ErrorDialog(error: AppError, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(error.message, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = { BigButton("OK", onDismiss) },
    )
}

@Composable
fun ConfirmDialog(question: String, onYes: () -> Unit, onNo: () -> Unit) {
    AlertDialog(
        onDismissRequest = onNo,
        text = { Text(question, style = MaterialTheme.typography.titleMedium) },
        confirmButton = { BigButton("Sim", onYes) },
        dismissButton = { BigButton("Não", onNo) },
    )
}

@Composable
fun PasswordDialog(wrongAttempt: Boolean, onSubmit: (String) -> Unit, onCancel: () -> Unit) {
    var password by rememberSaveable { mutableStateOf("") }
    var numeric by rememberSaveable { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Este PDF tem senha", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Senha") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (numeric) KeyboardType.NumberPassword else KeyboardType.Password,
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("password_field"),
                )
                if (wrongAttempt) {
                    Text(
                        "Senha incorreta, tente de novo",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                TextButton(onClick = { numeric = !numeric }, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                    Text(if (numeric) "abc  Usar letras" else "123  Usar números", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        confirmButton = { BigButton("Abrir", onClick = { onSubmit(password) }, enabled = password.isNotEmpty()) },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text("Cancelar", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}
```

- [ ] **Step 4: Rodar e ver passar**

Run: `./gradlew testDebugUnitTest --tests '*DialogsTest*'`
Expected: PASS (5 testes).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: tema de alto contraste, botão grande e diálogos (erro, confirmação, senha)"
```

---

### Task 11: Tela de leitura (rolagem contínua, zoom, barra inferior, rotação)

**Files:**
- Create: `app/src/main/java/com/johngabie/johnpdf/ui/reader/ReaderScreen.kt`
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/reader/ReaderContentTest.kt`

**Interfaces:**
- Consumes: `ReaderViewModel`, `ReaderUiState`, `ReaderStatus` (Task 8); `dominantPage`, `VisiblePage`, `pageLabel` (Task 2); `PageSize` (Task 3); `PAGE_RENDER_FAILED_MESSAGE` (Task 2); `BigButton`, `ErrorDialog`, `PasswordDialog`, `MinTouchTarget`, `PageGapColor`, `JohnPdfTheme` (Task 10).
- Produces:
```kotlin
@Composable fun ReaderScreen(viewModel: ReaderViewModel, onBack: () -> Unit)
@Composable fun ReaderContent(state: ReaderUiState, onBack: () -> Unit, onPageVisible: (Int) -> Unit, onZoomChange: (Float) -> Unit,
    onDoubleTap: () -> Unit, onToggleRotation: () -> Unit, onSubmitPassword: (String) -> Unit, renderPage: suspend (Int, Int) -> Bitmap?)
```
- Layout da barra inferior (duas linhas, para caber em 360dp com 20sp): linha 1 = `Página X de N` + botão de rotação; linha 2 = `[⬆ Anterior] [⬇ Próxima]`, cada um com metade da largura. Rótulos da rotação: `"🔓 Gira sozinha"` (livre) e `"🔒 Travada"` (travada).

- [ ] **Step 1: Escrever os testes que falham**

`app/src/test/java/com/johngabie/johnpdf/ui/reader/ReaderContentTest.kt`:
```kotlin
package com.johngabie.johnpdf.ui.reader

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.johngabie.johnpdf.data.AppError
import com.johngabie.johnpdf.data.PAGE_RENDER_FAILED_MESSAGE
import com.johngabie.johnpdf.engine.PageSize
import com.johngabie.johnpdf.ui.theme.JohnPdfTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReaderContentTest {
    @get:Rule val rule = createComposeRule()

    private val a4 = PageSize(595f, 842f)
    private var backCalls = 0
    private var rotationToggles = 0
    private var submitted: String? = null

    private fun show(initial: ReaderUiState, render: suspend (Int, Int) -> Bitmap? = { _, w -> Bitmap.createBitmap(w, w, Bitmap.Config.ARGB_8888) }) {
        rule.setContent {
            var state by remember { mutableStateOf(initial) }
            JohnPdfTheme {
                ReaderContent(
                    state = state,
                    onBack = { backCalls++ },
                    onPageVisible = { state = state.copy(currentPage = it) },
                    onZoomChange = { state = state.copy(zoom = it) },
                    onDoubleTap = {},
                    onToggleRotation = { rotationToggles++; state = state.copy(rotationLocked = !state.rotationLocked) },
                    onSubmitPassword = { submitted = it },
                    renderPage = render,
                )
            }
        }
    }

    @Test fun ready_shows_page_label_and_button_states() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(3) { a4 })))
        rule.onNodeWithText("doc.pdf").assertIsDisplayed()
        rule.onNodeWithText("Página 1 de 3").assertIsDisplayed()
        rule.onNodeWithText("⬆ Anterior").assertIsNotEnabled()
        rule.onNodeWithText("⬇ Próxima").assertIsEnabled()
    }

    @Test fun next_button_scrolls_to_next_page() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(3) { a4 })))
        rule.onNodeWithText("⬇ Próxima").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Página 2 de 3").assertIsDisplayed()
    }

    @Test fun back_button_calls_on_back() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(1) { a4 })))
        rule.onNodeWithText("← Voltar").performClick()
        assertEquals(1, backCalls)
    }

    @Test fun rotation_button_toggles_label() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(1) { a4 })))
        rule.onNodeWithText("🔓 Gira sozinha").performClick()
        rule.onNodeWithText("🔒 Travada").assertIsDisplayed()
        assertEquals(1, rotationToggles)
    }

    @Test fun needs_password_shows_dialog() {
        show(ReaderUiState("doc.pdf", ReaderStatus.NeedsPassword(wrongAttempt = false)))
        rule.onNodeWithText("Este PDF tem senha").assertIsDisplayed()
    }

    @Test fun failed_shows_error_and_ok_goes_back() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Failed(AppError.CORRUPTED)))
        rule.onNodeWithText("Não foi possível abrir este arquivo.").assertIsDisplayed()
        rule.onNodeWithText("OK").performClick()
        assertEquals(1, backCalls)
    }

    @Test fun failed_render_shows_page_message() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(List(1) { a4 })), render = { _, _ -> null })
        rule.onNodeWithText(PAGE_RENDER_FAILED_MESSAGE).assertIsDisplayed()
    }

    @Test fun page_with_zero_size_does_not_crash() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Ready(listOf(PageSize(0f, 0f)))))
        rule.onNodeWithText("Página 1 de 1").assertIsDisplayed()
    }

    @Test fun loading_shows_progress_text() {
        show(ReaderUiState("doc.pdf", ReaderStatus.Loading))
        rule.onNodeWithText("Abrindo…").assertIsDisplayed()
        assertTrue(submitted == null)
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./gradlew testDebugUnitTest --tests '*ReaderContentTest*'`
Expected: FAIL na compilação (`Unresolved reference: ReaderContent`).

- [ ] **Step 3: Implementar**

`app/src/main/java/com/johngabie/johnpdf/ui/reader/ReaderScreen.kt`:
```kotlin
package com.johngabie.johnpdf.ui.reader

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.johngabie.johnpdf.data.PAGE_RENDER_FAILED_MESSAGE
import com.johngabie.johnpdf.engine.PageSize
import com.johngabie.johnpdf.ui.common.BigButton
import com.johngabie.johnpdf.ui.common.ErrorDialog
import com.johngabie.johnpdf.ui.common.PasswordDialog
import com.johngabie.johnpdf.ui.theme.MinTouchTarget
import com.johngabie.johnpdf.ui.theme.PageGapColor
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

private const val A4_ASPECT = 595f / 842f

@Composable
fun ReaderScreen(viewModel: ReaderViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ApplyRotationLock(state.rotationLocked)
    ReaderContent(
        state = state,
        onBack = onBack,
        onPageVisible = viewModel::onPageVisible,
        onZoomChange = viewModel::setZoom,
        onDoubleTap = viewModel::toggleDoubleTapZoom,
        onToggleRotation = viewModel::toggleRotationLock,
        onSubmitPassword = viewModel::submitPassword,
        renderPage = viewModel::renderPage,
    )
}

@Composable
private fun ApplyRotationLock(locked: Boolean) {
    val activity = LocalActivity.current ?: return
    DisposableEffect(locked) {
        activity.requestedOrientation =
            if (locked) ActivityInfo.SCREEN_ORIENTATION_LOCKED else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
}

@Composable
fun ReaderContent(
    state: ReaderUiState,
    onBack: () -> Unit,
    onPageVisible: (Int) -> Unit,
    onZoomChange: (Float) -> Unit,
    onDoubleTap: () -> Unit,
    onToggleRotation: () -> Unit,
    onSubmitPassword: (String) -> Unit,
    renderPage: suspend (Int, Int) -> Bitmap?,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val status = state.status

    Scaffold(
        topBar = { ReaderTopBar(state.title, onBack) },
        bottomBar = {
            if (status is ReaderStatus.Ready) {
                ReaderBottomBar(
                    current = state.currentPage,
                    total = state.pageCount,
                    rotationLocked = state.rotationLocked,
                    onPrevious = { scope.launch { listState.animateScrollToItem((state.currentPage - 1).coerceAtLeast(0)) } },
                    onNext = { scope.launch { listState.animateScrollToItem((state.currentPage + 1).coerceAtMost(state.pageCount - 1)) } },
                    onToggleRotation = onToggleRotation,
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (status) {
                ReaderStatus.Loading -> Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator()
                    Text("Abrindo…", style = MaterialTheme.typography.bodyLarge)
                }
                is ReaderStatus.NeedsPassword -> PasswordDialog(status.wrongAttempt, onSubmitPassword, onCancel = onBack)
                is ReaderStatus.Failed -> ErrorDialog(status.error, onDismiss = onBack)
                is ReaderStatus.Ready -> {
                    TrackVisiblePage(listState, onPageVisible)
                    PageList(status.pageSizes, state.zoom, listState, onZoomChange, onDoubleTap, renderPage)
                }
            }
        }
    }
}

@Composable
private fun TrackVisiblePage(listState: LazyListState, onPageVisible: (Int) -> Unit) {
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            dominantPage(
                info.visibleItemsInfo.map { VisiblePage(it.index, it.offset, it.size) },
                info.viewportStartOffset,
                info.viewportEndOffset,
            )
        }.filterNotNull().distinctUntilChanged().collect { onPageVisible(it) }
    }
}

@Composable
private fun ReaderTopBar(title: String, onBack: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text("← Voltar", style = MaterialTheme.typography.labelLarge)
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun ReaderBottomBar(
    current: Int,
    total: Int,
    rotationLocked: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleRotation: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(pageLabel(current, total), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                BigButton(if (rotationLocked) "🔒 Travada" else "🔓 Gira sozinha", onToggleRotation)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BigButton("⬆ Anterior", onPrevious, Modifier.weight(1f), enabled = current > 0)
                BigButton("⬇ Próxima", onNext, Modifier.weight(1f), enabled = current < total - 1)
            }
        }
    }
}

@Composable
private fun PageList(
    pageSizes: List<PageSize>,
    zoom: Float,
    listState: LazyListState,
    onZoomChange: (Float) -> Unit,
    onDoubleTap: () -> Unit,
    renderPage: suspend (Int, Int) -> Bitmap?,
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(PageGapColor)) {
        val contentWidth = maxWidth * zoom
        val widthPx = with(LocalDensity.current) { contentWidth.roundToPx() }
        var pinch by remember { mutableFloatStateOf(1f) }
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(zoom) {
                    detectPinch(
                        onPinch = { s -> pinch = (zoom * s).coerceIn(ReaderViewModel.MIN_ZOOM, ReaderViewModel.MAX_ZOOM) / zoom },
                        onPinchEnd = { s -> pinch = 1f; onZoomChange(zoom * s) },
                    )
                }
                .pointerInput(Unit) { detectTapGestures(onDoubleTap = { onDoubleTap() }) }
                .graphicsLayer { scaleX = pinch; scaleY = pinch }
                .horizontalScroll(rememberScrollState(), enabled = zoom > 1f),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.width(contentWidth).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                itemsIndexed(pageSizes) { index, size -> PdfPage(index, size, widthPx, renderPage) }
            }
        }
    }
}

/** Pinça com dois dedos, interceptada na passagem Initial para não brigar com a rolagem da lista. */
private suspend fun PointerInputScope.detectPinch(onPinch: (Float) -> Unit, onPinchEnd: (Float) -> Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var scale = 1f
        var pinching = false
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.count { it.pressed } >= 2) {
                pinching = true
                scale *= event.calculateZoom()
                onPinch(scale)
                event.changes.forEach { it.consume() }
            }
        } while (event.changes.any { it.pressed })
        if (pinching) onPinchEnd(scale)
    }
}

private sealed interface PageImage {
    data object Loading : PageImage
    data class Loaded(val image: ImageBitmap) : PageImage
    data object Failed : PageImage
}

@Composable
private fun PdfPage(index: Int, size: PageSize, widthPx: Int, renderPage: suspend (Int, Int) -> Bitmap?) {
    // Mantém a imagem anterior enquanto re-renderiza após zoom (evita piscar em branco).
    var image by remember(index) { mutableStateOf<PageImage>(PageImage.Loading) }
    LaunchedEffect(index, widthPx) {
        image = renderPage(index, widthPx)?.let { PageImage.Loaded(it.asImageBitmap()) } ?: PageImage.Failed
    }
    val aspect = (size.width / size.height).takeIf { it.isFinite() && it > 0f } ?: A4_ASPECT
    Box(
        Modifier.fillMaxWidth().aspectRatio(aspect).background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        when (val img = image) {
            is PageImage.Loaded -> Image(
                img.image,
                contentDescription = "Página ${index + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
            PageImage.Failed -> Text(
                PAGE_RENDER_FAILED_MESSAGE,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
            )
            PageImage.Loading -> Unit
        }
    }
}
```

- [ ] **Step 4: Rodar e ver passar**

Run: `./gradlew testDebugUnitTest --tests '*ReaderContentTest*'`
Expected: PASS (9 testes). Se `next_button_scrolls_to_next_page` falhar porque a tela padrão do Robolectric é baixa demais para que a página 2 fique dominante, anote a classe com `@Config(qualifiers = "w360dp-h800dp")` (`org.robolectric.annotation.Config`). Não altere a lógica.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: tela de leitura com rolagem contínua, zoom, barra grande e trava de rotação"
```

---

### Task 12: Tela inicial (Header, bottom menu, Recentes, Todos os PDFs, permissão)

**Files:**
- Create: `app/src/main/java/com/johngabie/johnpdf/ui/home/HomeScreen.kt`
- Test: `app/src/test/java/com/johngabie/johnpdf/ui/home/HomeContentTest.kt`

**Interfaces:**
- Consumes: `HomeViewModel`, `HomeUiState`, `HomeTab` (Task 9); `RecentItem` (Task 4); `PdfFile`, `StorageAccess` (Task 6); `friendlyDate` (Task 2); `BigButton`, `ConfirmDialog`, `ErrorDialog`, `MinTouchTarget` (Task 10).
- Produces:
```kotlin
@Composable fun HomeScreen(viewModel: HomeViewModel)
@Composable fun HomeContent(state: HomeUiState, onOpenPicker: () -> Unit, onSelectTab: (HomeTab) -> Unit, onQueryChange: (String) -> Unit,
    onOpenRecent: (RecentItem) -> Unit, onRemoveRecent: (RecentItem) -> Unit, onOpenPdf: (PdfFile) -> Unit,
    onRequestPermission: () -> Unit, onDismissError: () -> Unit, nowMillis: Long = System.currentTimeMillis())
```

- [ ] **Step 1: Escrever os testes que falham**

`app/src/test/java/com/johngabie/johnpdf/ui/home/HomeContentTest.kt`:
```kotlin
package com.johngabie.johnpdf.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.johngabie.johnpdf.data.AppError
import com.johngabie.johnpdf.data.Origin
import com.johngabie.johnpdf.data.PdfFile
import com.johngabie.johnpdf.data.RecentItem
import com.johngabie.johnpdf.ui.theme.JohnPdfTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeContentTest {
    @get:Rule val rule = createComposeRule()

    private val now = 1_790_000_000_000L
    private val recent = RecentItem("Fatura.pdf", Origin.WHATSAPP, "/x/Fatura.pdf", now, imported = true)
    private val events = mutableListOf<String>()

    private fun show(state: HomeUiState) = rule.setContent {
        JohnPdfTheme {
            HomeContent(
                state = state,
                onOpenPicker = { events += "picker" },
                onSelectTab = { events += "tab:$it" },
                onQueryChange = { events += "query:$it" },
                onOpenRecent = { events += "recent:${it.name}" },
                onRemoveRecent = { events += "remove:${it.name}" },
                onOpenPdf = { events += "pdf:${it.name}" },
                onRequestPermission = { events += "permission" },
                onDismissError = { events += "dismiss" },
                nowMillis = now,
            )
        }
    }

    @Test fun header_and_bottom_menu_are_visible() {
        show(HomeUiState())
        rule.onNodeWithText("johnPDF").assertIsDisplayed()
        rule.onNodeWithText("📂 Abrir").performClick()
        rule.onNodeWithText("Todos os PDFs").performClick()
        assertEquals(listOf("picker", "tab:ALL"), events)
    }

    @Test fun empty_recents_shows_hint() {
        show(HomeUiState())
        rule.onNodeWithText("Os PDFs que você abrir vão aparecer aqui.").assertIsDisplayed()
    }

    @Test fun recent_card_shows_origin_and_date_and_opens() {
        show(HomeUiState(recents = listOf(recent)))
        rule.onNodeWithText("WhatsApp · hoje").assertIsDisplayed()
        rule.onNodeWithText("Fatura.pdf").performClick()
        assertEquals(listOf("recent:Fatura.pdf"), events)
    }

    @Test fun long_press_asks_before_removing() {
        show(HomeUiState(recents = listOf(recent)))
        rule.onNodeWithText("Fatura.pdf").performTouchInput { longClick() }
        rule.onNodeWithText("Remover da lista?").assertIsDisplayed()
        rule.onNodeWithText("Sim").performClick()
        assertEquals(listOf("remove:Fatura.pdf"), events)
    }

    @Test fun all_tab_without_access_asks_permission() {
        show(HomeUiState(tab = HomeTab.ALL, hasFilesAccess = false))
        rule.onNodeWithText("Para mostrar os PDFs do celular, o johnPDF precisa de permissão.").assertIsDisplayed()
        rule.onNodeWithText("Permitir").performClick()
        assertEquals(listOf("permission"), events)
    }

    @Test fun all_tab_lists_filtered_pdfs() {
        val pdfs = listOf(PdfFile("Boleto.pdf", "/d/Boleto.pdf", Origin.DOWNLOAD, now), PdfFile("Receita.pdf", "/d/R.pdf", Origin.DOCUMENTS, now))
        show(HomeUiState(tab = HomeTab.ALL, hasFilesAccess = true, allPdfs = pdfs, query = "bol"))
        rule.onNodeWithText("Boleto.pdf").performClick()
        rule.onNodeWithText("Receita.pdf").assertDoesNotExist()
        assertEquals(listOf("pdf:Boleto.pdf"), events)
    }

    @Test fun all_tab_empty_with_query_says_no_match() {
        show(HomeUiState(tab = HomeTab.ALL, hasFilesAccess = true, query = "zzz"))
        rule.onNodeWithText("Nenhum PDF com esse nome.").assertIsDisplayed()
    }

    @Test fun error_dialog_is_shown() {
        show(HomeUiState(error = AppError.NO_SPACE))
        rule.onNodeWithText("Sem espaço no celular para abrir este arquivo.").assertIsDisplayed()
        rule.onNodeWithText("OK").performClick()
        assertEquals(listOf("dismiss"), events)
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./gradlew testDebugUnitTest --tests '*HomeContentTest*'`
Expected: FAIL na compilação (`Unresolved reference: HomeContent`).

- [ ] **Step 3: Implementar**

`app/src/main/java/com/johngabie/johnpdf/ui/home/HomeScreen.kt`:
```kotlin
package com.johngabie.johnpdf.ui.home

import android.Manifest
import android.content.ActivityNotFoundException
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.johngabie.johnpdf.data.PdfFile
import com.johngabie.johnpdf.data.RecentItem
import com.johngabie.johnpdf.data.StorageAccess
import com.johngabie.johnpdf.ui.common.BigButton
import com.johngabie.johnpdf.ui.common.ConfirmDialog
import com.johngabie.johnpdf.ui.common.ErrorDialog
import com.johngabie.johnpdf.util.friendlyDate

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::openUri)
    }
    val legacyPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refresh()
    }
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    HomeContent(
        state = state,
        onOpenPicker = { picker.launch(arrayOf("application/pdf")) },
        onSelectTab = viewModel::selectTab,
        onQueryChange = viewModel::setQuery,
        onOpenRecent = viewModel::openRecent,
        onRemoveRecent = viewModel::removeRecent,
        onOpenPdf = viewModel::openFile,
        onRequestPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                StorageAccess.settingsIntents(context).firstOrNull { intent ->
                    try { context.startActivity(intent); true } catch (e: ActivityNotFoundException) { false }
                }
            } else {
                legacyPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        },
        onDismissError = viewModel::dismissError,
    )
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onOpenPicker: () -> Unit,
    onSelectTab: (HomeTab) -> Unit,
    onQueryChange: (String) -> Unit,
    onOpenRecent: (RecentItem) -> Unit,
    onRemoveRecent: (RecentItem) -> Unit,
    onOpenPdf: (PdfFile) -> Unit,
    onRequestPermission: () -> Unit,
    onDismissError: () -> Unit,
    nowMillis: Long = System.currentTimeMillis(),
) {
    var pendingRemoval by remember { mutableStateOf<RecentItem?>(null) }

    Scaffold(
        topBar = { HomeHeader(onOpenPicker) },
        bottomBar = { HomeBottomBar(state.tab, onSelectTab) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (state.tab) {
                HomeTab.RECENTS -> RecentsTab(state.recents, nowMillis, onOpenRecent, onLongPress = { pendingRemoval = it })
                HomeTab.ALL ->
                    if (!state.hasFilesAccess) PermissionContent(onRequestPermission)
                    else AllPdfsTab(state.filteredPdfs, state.query, state.loadingAll, nowMillis, onQueryChange, onOpenPdf)
            }
            if (state.busy) CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }

    pendingRemoval?.let { item ->
        ConfirmDialog(
            "Remover da lista?",
            onYes = { onRemoveRecent(item); pendingRemoval = null },
            onNo = { pendingRemoval = null },
        )
    }
    state.error?.let { ErrorDialog(it, onDismissError) }
}

@Composable
private fun HomeHeader(onOpenPicker: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("johnPDF", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            BigButton("📂 Abrir", onOpenPicker)
        }
    }
}

@Composable
private fun HomeBottomBar(tab: HomeTab, onSelectTab: (HomeTab) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = tab == HomeTab.RECENTS,
            onClick = { onSelectTab(HomeTab.RECENTS) },
            icon = { Text("🕘", fontSize = 28.sp) },
            label = { Text("Recentes", style = MaterialTheme.typography.labelMedium) },
        )
        NavigationBarItem(
            selected = tab == HomeTab.ALL,
            onClick = { onSelectTab(HomeTab.ALL) },
            icon = { Text("📚", fontSize = 28.sp) },
            label = { Text("Todos os PDFs", style = MaterialTheme.typography.labelMedium) },
        )
    }
}

@Composable
private fun RecentsTab(items: List<RecentItem>, nowMillis: Long, onOpen: (RecentItem) -> Unit, onLongPress: (RecentItem) -> Unit) {
    if (items.isEmpty()) {
        CenteredMessage("Os PDFs que você abrir vão aparecer aqui.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = { it.path }) { item ->
            PdfCard(
                name = item.name,
                subtitle = "${item.origin.label} · ${friendlyDate(item.openedAt, nowMillis)}",
                onClick = { onOpen(item) },
                onLongClick = { onLongPress(item) },
            )
        }
    }
}

@Composable
private fun AllPdfsTab(
    pdfs: List<PdfFile>,
    query: String,
    loading: Boolean,
    nowMillis: Long,
    onQueryChange: (String) -> Unit,
    onOpen: (PdfFile) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("🔍 Buscar pelo nome…", style = MaterialTheme.typography.bodyLarge) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 64.dp),
        )
        when {
            loading && pdfs.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            pdfs.isEmpty() -> CenteredMessage(if (query.isBlank()) "Nenhum PDF encontrado no celular." else "Nenhum PDF com esse nome.")
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(pdfs, key = { it.path }) { pdf ->
                    PdfCard(
                        name = pdf.name,
                        subtitle = "${pdf.origin.label} · ${friendlyDate(pdf.modifiedAt, nowMillis)}",
                        onClick = { onOpen(pdf) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionContent(onRequestPermission: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Para mostrar os PDFs do celular, o johnPDF precisa de permissão.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            "1. Toque em Permitir\n2. Ative a opção do johnPDF\n3. Volte para o app",
            style = MaterialTheme.typography.bodyLarge,
        )
        BigButton("Permitir", onRequestPermission, Modifier.fillMaxWidth())
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PdfCard(name: String, subtitle: String, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("📄", fontSize = 32.sp, modifier = Modifier.padding(end = 16.dp))
            Column {
                Text(name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
```

- [ ] **Step 4: Rodar e ver passar**

Run: `./gradlew testDebugUnitTest --tests '*HomeContentTest*'`
Expected: PASS (8 testes).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: tela inicial com header, bottom menu, recentes, todos os PDFs e permissão"
```

---

### Task 13: Ligação da app (container, navegação, "Abrir com…", permissões, APK assinado)

**Files:**
- Create: `app/src/main/java/com/johngabie/johnpdf/JohnPdfApp.kt`
- Create: `app/src/main/java/com/johngabie/johnpdf/AppContainer.kt`
- Create: `app/src/main/java/com/johngabie/johnpdf/ui/AppNavHost.kt`
- Modify: `app/src/main/java/com/johngabie/johnpdf/MainActivity.kt` (substituir inteiro)
- Modify: `app/src/main/AndroidManifest.xml` (substituir inteiro)
- Create (fora do repo): `~/.android-keys/johnpdf.jks`; `keystore.properties` (gitignored)
- Test: `app/src/test/java/com/johngabie/johnpdf/MainActivitySmokeTest.kt` (ampliar)

**Interfaces:**
- Consumes: todos os tipos das Tasks 3–12.
- Produces: `class AppContainer(context: Context) { recents; importer; library; settings; openPdf; fun hasFilesAccess(): Boolean }`, `class JohnPdfApp : Application { val container: AppContainer }`, `@Composable fun AppNavHost(homeViewModel: HomeViewModel, container: AppContainer)`.

- [ ] **Step 1: Ampliar o teste de fumaça (falha)**

Substitua `app/src/test/java/com/johngabie/johnpdf/MainActivitySmokeTest.kt`:
```kotlin
package com.johngabie.johnpdf

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun shows_app_name() {
        rule.onNodeWithText("johnPDF").assertIsDisplayed()
    }

    @Test
    fun home_starts_on_recents_and_switches_to_all() {
        rule.onNodeWithText("Os PDFs que você abrir vão aparecer aqui.").assertIsDisplayed()
        rule.onNodeWithText("Todos os PDFs").performClick()
        rule.onNodeWithText("Permitir").assertIsDisplayed()
    }
}
```

Run: `./gradlew testDebugUnitTest --tests '*MainActivitySmokeTest*'`
Expected: FAIL em `home_starts_on_recents_and_switches_to_all` (a Activity ainda só mostra "johnPDF").

- [ ] **Step 2: Implementar container, Application e navegação**

`app/src/main/java/com/johngabie/johnpdf/AppContainer.kt`:
```kotlin
package com.johngabie.johnpdf

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.johngabie.johnpdf.data.ImportRepository
import com.johngabie.johnpdf.data.OpenPdfUseCase
import com.johngabie.johnpdf.data.PdfLibraryRepository
import com.johngabie.johnpdf.data.RecentsRepository
import com.johngabie.johnpdf.data.SettingsRepository
import com.johngabie.johnpdf.data.StorageAccess
import java.io.File

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class AppContainer(context: Context) {
    private val app = context.applicationContext
    val recents = RecentsRepository(File(app.filesDir, "recents.json"))
    val importer = ImportRepository(app.contentResolver, File(app.filesDir, "imports"))
    val library = PdfLibraryRepository(app.contentResolver)
    val settings = SettingsRepository(app.settingsDataStore)
    val openPdf = OpenPdfUseCase(importer, recents)
    fun hasFilesAccess(): Boolean = StorageAccess.hasAllFilesAccess(app)
}
```

`app/src/main/java/com/johngabie/johnpdf/JohnPdfApp.kt`:
```kotlin
package com.johngabie.johnpdf

import android.app.Application

class JohnPdfApp : Application() {
    val container by lazy { AppContainer(this) }
}
```

`app/src/main/java/com/johngabie/johnpdf/ui/AppNavHost.kt`:
```kotlin
package com.johngabie.johnpdf.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.johngabie.johnpdf.AppContainer
import com.johngabie.johnpdf.engine.MuPdfEngine
import com.johngabie.johnpdf.ui.home.HomeScreen
import com.johngabie.johnpdf.ui.home.HomeViewModel
import com.johngabie.johnpdf.ui.reader.ReaderScreen
import com.johngabie.johnpdf.ui.reader.ReaderViewModel
import java.io.File

@Composable
fun AppNavHost(homeViewModel: HomeViewModel, container: AppContainer) {
    val nav = rememberNavController()
    LaunchedEffect(Unit) {
        homeViewModel.navigation.collect { route ->
            // Um leitor por vez: abrir outro PDF substitui o atual (e fecha o motor dele).
            nav.navigate(route) { popUpTo<HomeRoute>() }
        }
    }
    NavHost(nav, startDestination = HomeRoute) {
        composable<HomeRoute> { HomeScreen(homeViewModel) }
        composable<ReaderRoute> { entry ->
            val route = entry.toRoute<ReaderRoute>()
            val vm: ReaderViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { ReaderViewModel(File(route.path), route.title, MuPdfEngine(), container.settings) }
                },
            )
            ReaderScreen(vm, onBack = { nav.popBackStack() })
        }
    }
}
```

Substitua `app/src/main/java/com/johngabie/johnpdf/MainActivity.kt`:
```kotlin
package com.johngabie.johnpdf

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.johngabie.johnpdf.ui.AppNavHost
import com.johngabie.johnpdf.ui.home.HomeViewModel
import com.johngabie.johnpdf.ui.theme.JohnPdfTheme

class MainActivity : ComponentActivity() {
    private val container: AppContainer get() = (application as JohnPdfApp).container

    private val homeViewModel: HomeViewModel by viewModels {
        viewModelFactory {
            initializer {
                HomeViewModel(container.recents, container.library, container.openPdf, container::hasFilesAccess)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Só na primeira criação: após rotação o intent é o mesmo e não deve reimportar.
        if (savedInstanceState == null) handleViewIntent(intent)
        setContent { JohnPdfTheme { AppNavHost(homeViewModel, container) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleViewIntent(intent)
    }

    private fun handleViewIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) intent.data?.let(homeViewModel::openUri)
    }
}
```

Substitua `app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission
        android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission
        android:name="android.permission.MANAGE_EXTERNAL_STORAGE"
        tools:ignore="ScopedStorage" />

    <application
        android:name=".JohnPdfApp"
        android:allowBackup="false"
        android:icon="@drawable/ic_launcher"
        android:label="johnPDF"
        android:largeHeap="true"
        android:requestLegacyExternalStorage="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTask">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:scheme="content" android:mimeType="application/pdf" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:scheme="file" android:mimeType="application/pdf" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 3: Rodar a suíte JVM inteira**

Run: `./gradlew testDebugUnitTest`
Expected: PASS em todos os testes (Tasks 1–13).

- [ ] **Step 4: Criar a keystore e gerar o APK assinado**

```bash
mkdir -p ~/.android-keys
PASS=$(openssl rand -base64 18)
keytool -genkeypair -v -keystore ~/.android-keys/johnpdf.jks -alias johnpdf -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "$PASS" -keypass "$PASS" -dname "CN=johnPDF, O=Familia, C=BR"
cat > keystore.properties <<EOF
storeFile=$HOME/.android-keys/johnpdf.jks
storePassword=$PASS
keyAlias=johnpdf
keyPassword=$PASS
EOF
./gradlew assembleRelease && ls -lh app/build/outputs/apk/release/
```
Expected: `app-arm64-v8a-release.apk`, `app-armeabi-v7a-release.apk`, `app-x86_64-release.apk` e `app-universal-release.apk`. `git status` **não** mostra `keystore.properties`. Avise o usuário para fazer backup de `~/.android-keys/johnpdf.jks` e de `keystore.properties`: sem eles, as atualizações do APK não instalam por cima.

- [ ] **Step 5: Rodar os testes instrumentados de novo (regressão do motor)**

Run: `./gradlew connectedDebugAndroidTest`
Expected: PASS (12 testes do `MuPdfEngineTest`).

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: ligação da app — navegação, Abrir com…, permissões e APK assinado"
```

---

### Task 14: Validação ponta a ponta no emulador com mobile-mcp

**Files:**
- Create: `docs/e2e/2026-09-28-checklist.md` (resultado da execução, com prints salvos em `docs/e2e/img/`)
- Modify: qualquer arquivo que um bug encontrado exigir, **sempre** com um teste de regressão primeiro (TDD, skill `superpowers:systematic-debugging`).

**Interfaces:**
- Consumes: APK debug (`app/build/outputs/apk/debug/app-x86_64-debug.apk`), fixtures de `app/src/androidTest/assets/` e as ferramentas do mobile-mcp (carregar com `ToolSearch` query `"mobile"`; usar as de listar dispositivos, abrir app, screenshot, listar elementos, tocar por coordenada, deslizar, digitar e mudar orientação).
- Produces: checklist preenchido com PASS/FAIL por cenário e evidência (screenshot).

- [ ] **Step 1: Preparar o emulador**

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-x86_64-debug.apk
for f in normal long landscape password corrupted; do adb push app/src/androidTest/assets/$f.pdf /sdcard/Download/$f.pdf; done
adb shell content call --method scan_volume --uri content://media --arg external_primary
adb shell appops set --uid com.johngabie.johnpdf MANAGE_EXTERNAL_STORAGE default
```
Expected: `Success` na instalação. A última linha garante que o app começa **sem** a permissão.

- [ ] **Step 2: Executar os cenários com mobile-mcp**

Para cada cenário: tire um screenshot antes e depois, compare com o esperado e registre PASS/FAIL no checklist.

| # | Cenário | Passos | Esperado |
|---|---|---|---|
| 1 | Primeira abertura | Abrir o app | Header "johnPDF" + "📂 Abrir"; aba Recentes com "Os PDFs que você abrir vão aparecer aqui." |
| 2 | Permissão | Tocar "Todos os PDFs" → "Permitir" → ativar o johnPDF nas configurações → voltar | A lista mostra os 5 PDFs de `Download` com "Download · hoje" |
| 3 | Busca | Digitar "land" | Só `landscape.pdf` |
| 4 | Leitura + botões | Abrir `long.pdf`; tocar "⬇ Próxima" 3× | "Página 4 de 200"; páginas nítidas |
| 5 | Rolagem | Deslizar para cima rapidamente várias vezes | O indicador acompanha, sem travar nem mostrar páginas brancas permanentes |
| 6 | Zoom | Tocar duas vezes na página; depois arrastar para o lado | O texto fica ~2,5× maior e dá para mover na horizontal; dois toques de novo voltam a 1× |
| 7 | Rotação livre | Em "Página 4", girar para paisagem (ferramenta de orientação) | Continua no mesmo arquivo, perto da página 4, sem reabrir |
| 8 | Trava | Tocar "🔓 Gira sozinha" → vira "🔒 Travada"; girar o aparelho | A tela não gira; ao voltar e abrir outro PDF, a trava continua |
| 9 | Senha | Abrir `password.pdf`; digitar `0000` → Abrir; depois `1234` | "Senha incorreta, tente de novo"; depois abre 3 páginas |
| 10 | Corrompido | Abrir `corrupted.pdf` | "Não foi possível abrir este arquivo." → OK volta para a Home |
| 11 | Recentes | Voltar para a Home → aba Recentes | Os arquivos abertos, o mais novo primeiro; toque longo → "Remover da lista?" → Sim remove |
| 12 | Abrir com… | Abrir o app **Arquivos** do sistema → Downloads → tocar `normal.pdf` → escolher johnPDF | Vai direto para o leitor; o arquivo aparece nos Recentes |
| 13 | Arquivo sumiu | `adb shell rm /sdcard/Download/landscape.pdf`; nos Recentes, tocar em `landscape.pdf` | "Este arquivo não está mais disponível."; o item sai da lista |
| 14 | Memória | Abrir `long.pdf`, dar zoom com pinça até o máximo e rolar 50 páginas | Sem crash (conferir com `adb logcat -d | grep -E "FATAL|OutOfMemory"` → vazio) |

- [ ] **Step 3: Corrigir falhas (se houver)**

Para cada FAIL: aplique a skill `superpowers:systematic-debugging`, escreva um teste JVM ou instrumentado que reproduza o problema, veja o teste falhar, corrija, rode `./gradlew testDebugUnitTest connectedDebugAndroidTest` e repita o cenário no emulador.

- [ ] **Step 4: Gerar o APK final e fazer commit**

```bash
./gradlew testDebugUnitTest connectedDebugAndroidTest assembleRelease
git add -A
git commit -m "test: validação E2E no emulador com mobile-mcp"
git push
```
Expected: todos os testes passam; `app/build/outputs/apk/release/app-universal-release.apk` é o arquivo para mandar para a família (os APKs por ABI são opcionais e menores, e o `arm64-v8a` serve para quase todos os celulares atuais).
