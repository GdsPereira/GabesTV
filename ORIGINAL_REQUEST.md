# Original User Request

## 2026-09-06T18:58:48Z

This is a single self-contained fix; keep it small and focused. Implementar tela cheia imersiva total e preenchimento completo do display (eliminando barras pretas no topo/notch e ocultando a barra de navegação inferior por padrão) na interface móvel do aplicativo GabesTV.

Working directory: c:\Users\GP\Documents\Scripts\GabesTV
Integrity mode: demo

## Requirements

### R1. Modo Imersivo Completo em Todo o App Mobile
Em smartphones e tablets, a barra de navegação inferior e a barra de status devem ser completamente ocultadas em todas as telas (listagem de canais e player de vídeo). As barras do sistema só devem reaparecer de maneira translúcida temporária quando o usuário deslizar o dedo a partir da borda da tela (`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`).

### R2. Eliminação de Letterbox e Preenchimento da Área de Recorte (Notch / Cutout)
A tela do aplicativo deve ocupar a totalidade da área útil do display até as bordas físicas, estendendo-se por trás do recorte da câmera (display cutout / notch) no topo do aparelho, sem faixas pretas ou margens não utilizadas.

### R3. Preservação da Navegação e Layout de TV
Garantir que as alterações de janela e insets sejam aplicadas de modo a não interferir negativamente na navegação nativa e comportamento da interface em Android TV e TV Boxes.

## Verification Resources
- Build e testes unitários locais com JDK 21:
  `$env:JAVA_HOME="C:\Users\GP\.jdks\jbr-21.0.11"; ./gradlew assembleDebug test`
- Regras do projeto documentadas em `GEMINI.md`.

## Acceptance Criteria

### Comportamento Imersivo Mobile
- [ ] Ao abrir o aplicativo em smartphone/tablet, nem a barra de navegação (botões ou barra de gestos) nem a barra de status ficam visíveis continuamente.
- [ ] O topo do aplicativo preenche a tela até a borda física contornando o notch/câmera sem faixas pretas.
- [ ] Deslizar o dedo a partir da borda superior ou inferior da tela revela as barras do sistema de forma temporária/translúcida, recolhendo-as automaticamente após inatividade.
- [ ] Nenhum elemento interativo (logo, campo de busca, botões de ação ou cards de canais) fica cortado ou inacessível.

### Estabilidade e Build
- [ ] O comando de build e testes `$env:JAVA_HOME="C:\Users\GP\.jdks\jbr-21.0.11"; ./gradlew assembleDebug test` conclui com sucesso com status 0.
- [ ] Em dispositivos Android TV, a navegação via D-Pad e a orientação landscape mantêm-se totalmente funcionais.
