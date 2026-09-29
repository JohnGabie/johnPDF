# Teste em dispositivo físico — tema escuro e troca manual no moto g41

**Data:** 2026-09-29
**Dispositivo:** motorola moto g41 (corfu_g), Android 12, arm64-v8a, serial 0078153143
**Build testado:** `app-universal-debug.apk` (worktree `johnPDF-darkmode-switch`, `./gradlew :app:assembleDebug`, BUILD SUCCESSFUL)
**Sistema do aparelho:** modo escuro **ligado** — foi de propósito, porque é a configuração
que expõe o flash branco de abertura e os ícones ilegíveis na barra de status.

## Resumo

O esquema escuro e a troca manual passaram em todos os cenários. A troca acontece
no mesmo frame do toque, sem recriar a Activity e sem piscar. Nenhum lilás do
baseline do M3 apareceu em nenhuma superfície.

## Tabela de cenários

| # | Cenário | Resultado | Observação |
|---|---------|-----------|------------|
| 1 | Abrir o app com o sistema no escuro | PASS | Home sobe direto no escuro, sem flash branco. Antes do `values-night/`, o `Theme.Material.Light` do manifesto pintava a janela de branco até o primeiro frame do Compose. |
| 2 | Ícones da barra de status no escuro | PASS | Ícones claros sobre a barra transparente. Era o caso que o `SystemBarStyle.light(...)` fixo quebrava. |
| 3 | Barra de abas da Home no escuro | PASS | `barContainer` se lê como plano separado do conteúdo; rótulos Recentes/Todos os PDFs legíveis; pílula do item selecionado visível. |
| 4 | Ícone do PDF na lista (errorContainer) no escuro | PASS | Vermelho escuro com o glifo claro por cima — continua sendo "vermelho de PDF" e não some no fundo. |
| 5 | Diálogo de erro no escuro | PASS | Container neutro (cinza azulado), texto legível, "OK" no azul do tema. Nenhum lilás — era exatamente o sintoma do `darkColorScheme()` parcial. |
| 6 | Campo de busca ("Todos os PDFs") no escuro | PASS | A pílula `surfaceContainerHigh` se destaca do fundo; placeholder e lupa legíveis. |
| 7 | Estado vazio no escuro | PASS | Ícone, título e texto de apoio legíveis; "Abrir PDF" em azul sobre o fundo escuro. |
| 8 | **Troca manual: toque no botão do header** | PASS | Um toque levou de escuro a claro **na hora**: fundo, barras, ícone do botão (lua → sol) e ícones da barra de status do sistema trocaram juntos, sem reabrir a tela. |
| 9 | Ciclo dos três modos | PASS | seguir o sistema (ícone `brightness_auto`) → claro (sol) → escuro (lua) → volta. O tooltip/`stateDescription` anuncia o modo atual. |
| 10 | Leitor no escuro (`layout-duas-colunas.pdf`) | PASS | Página branca sobre a moldura escura, barra de topo escura, faixa de páginas como plano distinto da moldura. O papel do PDF continua branco — inverter o PDF é "modo noturno", outra feature. |
| 11 | Setas de página desabilitadas no escuro | PASS | PDF de 1 página: as duas setas ficam desabilitadas e **continuam claramente visíveis**. É o efeito do `DisabledContentAlpha = 0.60`; com os 0,38 do M3 ficariam em ~2,7:1. |
| 12 | Leitor no claro (mesmo arquivo) | PASS | O par mais apertado do app: papel branco sobre a moldura clara (1,28:1 medido). A faixa da moldura acima da página e entre a página e a faixa inferior fica visível. |
| 13 | A escolha sobrevive | PASS | O modo escolhido é gravado em SharedPreferences no toque; reabrir o app volta no mesmo modo. |

## O que este teste não cobre

- **Troca do modo do sistema com o app aberto** estando em `SYSTEM`: o Android
  recria a Activity nesse caso e o caminho é o mesmo do cenário 1.
- **Transição animada** entre claro e escuro: a troca é instantânea, sem
  cross-fade. Foi uma decisão, não um esquecimento — animar exigiria animar o
  `ColorScheme` inteiro, e um fade de tela cheia a cada toque incomoda mais do
  que ajuda numa ação que o usuário faz de propósito.

## Nota de comportamento (não é bug)

Num ciclo de três com "seguir o sistema" dentro, **um dos passos nunca muda a
cor**: num celular claro, `SYSTEM` e `LIGHT` pintam igual; num escuro, `SYSTEM` e
`DARK`. O que muda nesse passo é o ícone e o que o TalkBack anuncia. Não tem
conserto sem tornar a ordem do botão imprevisível, e há um teste
(`ThemeSwitchTest`) registrando isso para ninguém "arrumar" depois.

## Limpeza realizada

- `layout-duas-colunas.pdf` e `normal.pdf` copiados para `/sdcard/Download/`
  durante o teste; remover com
  `adb shell rm /sdcard/Download/layout-duas-colunas.pdf /sdcard/Download/normal.pdf`.
- Modo de tema do app deixado em **claro**; um toque no botão do header volta ao
  automático em dois passos.
