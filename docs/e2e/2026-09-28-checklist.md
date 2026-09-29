# Task 14 — Validação E2E no emulador (johnpdf_test, API 36)

Ferramenta usada: **adb** (uiautomator dump + input tap/swipe/text + screencap), não mobile-mcp
(indisponível nesta sessão — controller autorizou o fallback via adb, cobrindo os mesmos 14
cenários com a mesma evidência).

Data: 2026-09-29. Emulador: `emulator-5554` (johnpdf_test, API 36).

## Preparação (Step 1)

```
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-x86_64-debug.apk   → Success
adb push (normal, long, landscape, password, corrupted).pdf → /sdcard/Download/
adb shell content call --method scan_volume --uri content://media --arg external_primary
adb shell appops set --uid com.johngabie.johnpdf MANAGE_EXTERNAL_STORAGE default
```
Todos os passos executados com sucesso; o app começou sem a permissão "Todos os arquivos".

## Cenários

| # | Cenário | Resultado | Evidência |
|---|---|---|---|
| 1 | Primeira abertura | **PASS** — header "johnPDF" + "📂 Abrir"; aba Recentes com "Os PDFs que você abrir vão aparecer aqui." | `img/01-first-open.png` |
| 2 | Permissão | **PASS** — tocar "Todos os PDFs" mostra tela de permissão; "Permitir" abre a tela de sistema "All files access" (alcançável por toque, sem precisar do `appops` fallback); toggle ativado; voltar ao app mostra os 5 PDFs de Download. Nota: a data mostrada oscilou entre "ontem"/"hoje" só por causa do fuso/hora do host cruzando a meia-noite entre o push dos arquivos e a leitura da lista (mtime do arquivo preservado pelo `adb push`) — comportamento correto do app, não é bug. | `img/02b-permitir-tap.png`, `img/02c-toggle-on.png`, `img/02-permission-granted-list.png` |
| 3 | Busca | **PASS** — digitar "land" mostra só `landscape.pdf` | `img/03-search-land.png` |
| 4 | Leitura + botões | **PASS** — abriu `long.pdf` em "Página 1 de 200"; 3× "⬇ Próxima" → "Página 4 de 200"; página nítida (texto "Pagina N" legível) | `img/04a-long-opened.png`, `img/04b-page4.png` |
| 5 | Rolagem | **PASS** — 6 swipes rápidos consecutivos; indicador acompanhou (chegou a "Página 16 de 200"), sem travar, sem página branca permanente, sem FATAL/OOM no logcat | `img/05a-fast-scroll.png` |
| 6 | Zoom | **PASS** — duplo toque (dois `input tap` com ~150ms de intervalo) ampliou a página ~2,5×; arrastar horizontalmente moveu a visão; duplo toque de novo voltou a 1×. **Nota de ferramenta**: com os dois `input tap` disparados sem qualquer intervalo (mesmo processo shell, <5ms de diferença) o gesto NÃO era reconhecido como duplo toque pelo Compose `detectTapGestures` — isso é uma particularidade da simulação via adb, não um bug do app (confirmado lendo `ReaderScreen.kt`/`ReaderViewModel.kt`: a lógica de zoom está correta; com timing humano normal funciona). | `img/06a3-gap150.png` (zoom aplicado), `img/06b-drag-horizontal.png` (arrasto), `img/06c-double-tap-back.png` (retorno a 1×) |
| 7 | Rotação livre | **PASS** — na "Página 4", girado para paisagem via `settings put system user_rotation 1`; continuou no mesmo arquivo (`long.pdf`), mesma página, sem reabrir | `img/07a-rotated-landscape.png` |
| 8 | Trava | **PASS** — "🔓 Gira sozinha" → "🔒 Travada"; ao tentar girar de volta para retrato via configurações do sistema, a tela permaneceu em paisagem (trava respeitada); voltado à Home e aberto `normal.pdf`, a trava continuou ("🔒 Travada" e orientação paisagem mantidas) | `img/08a-locked-stays-landscape.png`, `img/08d-normal-opened-lock-persists.png` |
| 9 | Senha | **PASS** — abriu `password.pdf`; senha `0000` → "Senha incorreta, tente de novo"; senha `1234` → abriu com "Página 1 de 3" | `img/09d-wrong-password2.png`, `img/09f-password-correct-opened.png` |
| 10 | Corrompido | **PASS** — abriu `corrupted.pdf` → diálogo "Não foi possível abrir este arquivo." (texto exato); "OK" voltou para a Home | `img/10a-corrupted-error.png`, `img/10b-back-to-home.png` |
| 11 | Recentes | **PASS** — aba Recentes lista os arquivos abertos com o mais novo primeiro (`corrupted.pdf`, `password.pdf`, `normal.pdf`, `long.pdf` — ordem exatamente inversa à de abertura); toque longo em `long.pdf` → "Remover da lista?"; "Sim" removeu o item da lista | `img/11a-recentes-tab.png`, `img/11b-longpress-remove.png`, `img/11c-removed.png` |
| 12 | Abrir com… | **PASS** (caminho tentado primeiro, e usado: app Arquivos/DocumentsUI) — aberto o app **Arquivos** do sistema (`com.google.android.documentsui`) → Download → toque em `normal.pdf` → chooser "Open with" → selecionado **johnPDF** → "Just once". Foi direto para o leitor ("Página 1 de 3"); o arquivo passou a aparecer em Recentes (como "normal.pdf · Outros · hoje", já que a origem via SAF/Open-with não é reconhecida como a pasta Download diretamente — comportamento esperado). Não foi necessário usar o fallback de intent direto. | `img/12b-tap-normal.png`, `img/12d-opened-clean.png` |
| 13 | Arquivo sumiu | **PASS** — `landscape.pdf` foi aberto uma vez (para entrar em Recentes) e então removido com `adb shell rm /sdcard/Download/landscape.pdf`; toque no item em Recentes → "Este arquivo não está mais disponível." (texto exato); "OK" fechou o diálogo e o item saiu da lista de Recentes | `img/13c-file-gone.png`, `img/13d-removed-confirmed.png` |
| 14 | Memória | **PASS** (com adaptação combinada pela controladora: pinça não é possível via adb) — abriu `long.pdf`, aplicou zoom de duplo toque (~2,5×, já que pinça via adb não existe) e rolou rapidamente mais de 50 páginas (chegou à página 56 de 200). Nenhum crash do app; `adb logcat -d \| grep -E "FATAL\|OutOfMemory"` → **vazio**; processo do app seguiu vivo (`pidof` retornou PID). **Observação de ambiente**: na primeira tentativa (25+30 swipes muito rápidos e sem pausa, imediatamente após o zoom) o **próprio emulador** (QEMU) travou e caiu — `emulator-...log` mostra "detected a hanging thread" nas threads de CPU/loop principal do QEMU, não um crash do app Android. Reiniciei o emulador (`emulator -avd johnpdf_test ...`), aguardei o boot, reabri `long.pdf` e repeti a rolagem em lotes menores com pequenas pausas; dessa vez tanto o emulador quanto o app permaneceram estáveis por 56 páginas de rolagem sob zoom. Isso é uma limitação do ambiente do emulador (host com RAM limitada), não um bug do johnPDF. | `img/14a-zoomed-max.png`, `img/14b-after-50-pages.png` |

## Step 3 — Correções

Nenhum FAIL real de app encontrado. Os dois "quase-FAIL" foram artefatos da automação via adb:
- Cenário 6: dois `input tap` sem intervalo não formam um duplo-toque válido para o
  `detectTapGestures` do Compose — resolvido usando ~150ms de intervalo entre os toques.
- Cenário 14 (1ª tentativa): rajada de 55 `input swipe` sem pausas sobrecarregou o próprio
  QEMU/emulador (não o app) e ele travou — resolvido reiniciando o emulador e espaçando os swipes.

Nenhuma mudança de código foi necessária.

## Step 4 — Testes finais e build de release

```
./gradlew testDebugUnitTest connectedDebugAndroidTest assembleRelease
```
- `testDebugUnitTest`: **88 testes, 0 falhas, 0 erros, 0 pulados**
- `connectedDebugAndroidTest` (no emulador `johnpdf_test(AVD) - 16`): **13 testes, 0 falhas, 0 erros**
- `assembleRelease`: **BUILD SUCCESSFUL**

APK final para a família: `app/build/outputs/apk/release/app-universal-release.apk` (49 MB).
APKs por ABI também gerados (`app-arm64-v8a-release.apk`, `app-armeabi-v7a-release.apk`,
`app-x86_64-release.apk`).

## Resumo

**14/14 cenários PASS.** Nenhum bug de app encontrado; nenhuma alteração de código necessária.
Todos os 101 testes automatizados (88 unitários + 13 instrumentados) passam. Build de release
gerado com sucesso.
