# Ambiente de desenvolvimento — johnPDF (Windows 11)

Estado verificado em 2026-09-29. O plano
(`docs/superpowers/plans/2026-09-28-johnpdf-leitor-android.md`) foi escrito para um
sandbox Linux; este documento registra o que foi instalado e **o que muda** nos
comandos do plano nesta máquina.

## O que está instalado

| Item | Versão / caminho |
|---|---|
| JDK | Temurin **21.0.12** — `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` |
| Android SDK | `D:\Android\Sdk` (movido do `%LOCALAPPDATA%` para liberar o C:) |
| Platforms | `android-36`, `android-37.0` |
| Build-tools | `35.0.0`, `36.0.0` |
| Platform-tools / adb | 37.0.1 |
| Emulator | 37.1.11 — aceleração **WHPX** disponível |
| System image | `system-images;android-36;google_apis;x86_64` |
| Gradle (CLI) | **8.14.3** em `D:\tools\gradle-8.14.3\bin` (só para gerar o wrapper) |
| Cache do Gradle | `D:\gradle` (`GRADLE_USER_HOME`) |
| Python | 3.13.7 via launcher `py -3`; venv do projeto em `.venv-tools` |
| mobile-mcp | `.mcp.json`, conectado e enxergando o emulador |

## Variáveis de ambiente (nível de usuário, já definidas)

```
ANDROID_HOME     = D:\Android\Sdk
ANDROID_SDK_ROOT = D:\Android\Sdk
ANDROID_AVD_HOME = D:\Android\avd
GRADLE_USER_HOME = D:\gradle
JAVA_HOME        = C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\
```

`PATH` do usuário recebeu, no início: `D:\Android\Sdk\platform-tools`,
`D:\Android\Sdk\emulator`, `D:\Android\Sdk\cmdline-tools\latest\bin`,
`D:\tools\gradle-8.14.3\bin`.

> Terminais abertos antes da preparação não enxergam essas mudanças — abra um novo.

## Emulador

AVD **`johnpdf_test`** (Pixel 6, API 36, google_apis, x86_64), em `D:\Android\avd`:
RAM 4 GB, partição de dados 6 GB, `hw.keyboard=yes` (necessário para o mobile-mcp
digitar a senha do PDF), sem device frame.

```powershell
emulator -avd johnpdf_test -no-window -no-audio -no-boot-anim
adb wait-for-device
adb shell getprop sys.boot_completed   # 1 = pronto
```

Boot headless verificado: `emulator-5554  device`, `ro.build.version.sdk = 36`.

## Diferenças em relação aos comandos do plano

| No plano | Nesta máquina |
|---|---|
| `source ~/.bashrc && …` | Nada a carregar: as variáveis são de usuário. Só ignore o prefixo. |
| `gradle wrapper --gradle-version 8.11.1` | Funciona: o `gradle` 8.14.3 do PATH gera o wrapper. |
| `./gradlew …` | Use `.\gradlew.bat …` no PowerShell (ou `./gradlew` no Git Bash). |
| `python3 -m venv .venv-tools && .venv-tools/bin/pip install …` | **Já feito.** O interpretador é `.venv-tools\Scripts\python.exe` (Windows não tem `bin/`). |
| `find ~/.gradle/caches -name 'fitz-*.aar'` (Task 7, Step 2) | O cache agora é `D:\gradle\caches`. |
| JDK 17 | JDK 21. AGP 8.10.1 + Gradle 8.11.1 + Kotlin 2.1.21 rodam nele; o `jvmTarget = 17` do `app/build.gradle.kts` continua valendo. |
| `-Xmx2g` e "não rode dois builds ao mesmo tempo" (por causa de ~7 GB de RAM) | A máquina tem **32 GB**. A restrição pode ser relaxada, mas o plano foi mantido como está — mude só se o build ficar lento. |

## Fixtures de PDF

O venv `.venv-tools` já tem `reportlab 5.0.1`, `pypdf 6.19.0` e `cryptography 50.0.1`.
Quando chegar na Task 7, rode apenas:

```powershell
.\.venv-tools\Scripts\python.exe tools\make_test_pdfs.py
```

## Rede / repositórios

Alcançáveis: `dl.google.com` (AGP), `repo.maven.apache.org`, `services.gradle.org`
e `maven.ghostscript.com`.

A última release do MuPDF `fitz` (Task 1, Step 1) hoje é **1.28.5** — o catálogo do
plano traz `1.26.3`. O plano manda usar o valor descoberto; confirme na hora.

## Espaço em disco

O `C:` está com pouco espaço livre (era 98% cheio antes desta preparação; o SDK e o
cache do Gradle saíram dele). Mantenha builds, caches e AVDs no `D:`.
