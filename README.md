<div align="center">

# 📺 GabesTV

### *O reprodutor IPTV definitivo, nativo e ultrarrápido para Android TV e Google TV.*

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android TV](https://img.shields.io/badge/Android%20TV-API%2024%2B%20(SDK%2034)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/tv)
[![Compose for TV](https://img.shields.io/badge/Jetpack%20Compose-TV%20Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose/tv)
[![Media3 ExoPlayer](https://img.shields.io/badge/Media3-ExoPlayer%201.3.1-FF6F00?style=for-the-badge&logo=google&logoColor=white)](https://developer.android.com/media/media3)
[![Dagger Hilt](https://img.shields.io/badge/Dagger-Hilt%202.51.1-009688?style=for-the-badge&logo=dagger&logoColor=white)](https://dagger.dev/hilt/)
[![CI/CD Pipeline](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions%20%26%20Azure-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)](.github/workflows/build-apk.yml)

<br/>

> **GabesTV** é um cliente IPTV de alta performance desenvolvido do zero com foco exclusivo na experiência de 3 metros (*10-foot UI*). Projetado para Smart TVs (TCL, Philips, Sony), Google TV, Fire TV e TV Boxes, eliminando layouts herdados de celulares, WebViews lentas ou interfaces confusas.

</div>

---

## ⚡ Destaques & Diferenciais

- 📺 **100% Jetpack Compose for TV**: Construído com `androidx.tv.material3` e `tv-foundation`. Renderização fluida a 60/120 fps com o design system mais moderno da Google.
- ⚡ **Zapping Ultrarrápido**: Configuração de `DefaultLoadControl` personalizada com **buffer inicial de apenas 1,5s**, permitindo transições instantâneas entre transmissões ao vivo.
- 🎮 **Foco e Navegação Nativos por D-Pad**: Desenvolvido sob medida para controle remoto — escala dinâmica (1.08x zoom), brilho periférico nos cards e sombras profundas ao receber foco.
- 📡 **Engine HLS (.m3u8) & MPEG-TS (.ts)**: Alimentado pelo **AndroidX Media3 (ExoPlayer)** com `DefaultHttpDataSource` resiliente e suporte a cabeçalhos customizados (*User-Agent* e *Referer*).
- 🔄 **Reconexão Inteligente & Resiliência**: Sistema de auto-retry com *exponential backoff* para recuperar streams instáveis automaticamente sem travar o aplicativo.
- 🗂️ **Drawer de Categorias Retrátil**: Navegação em split-screen com filtros dinâmicos, contagem de canais em tempo real e categorias organizadas.
- 🎬 **HUD / OSD Cinematográfico**: Overlay elegante que exibe logotipo (com suporte a SVG vetorial via Coil), nome do canal, categoria, badge "AO VIVO" e dicas de atalhos, com esmaecimento automático após 4 segundos de inatividade.
- 🌐 **Integração Threadfin / Middleware**: Conexão nativa com proxies M3U como **Threadfin**, além de mecanismo de *fallback* automático para lista local bundled em caso de indisponibilidade de rede.
- 📦 **CI/CD Automatizado**: Pipelines prontas no GitHub Actions e Azure Pipelines gerando APKs assinados e otimizados com R8/ProGuard.

---

## 🎮 Mapeamento do Controle Remoto (D-Pad)

Projetado para ser operado com total ergonomia pelo controle da sua Smart TV:

| Botão / Tecla | Ação no Modo Player (Tela Cheia) | Ação no Modo Grade / Menu |
| :--- | :--- | :--- |
| <kbd>◀ Esquerda</kbd> / <kbd>CH -</kbd> | **Canal Anterior** (Zapping rápido) | Navegar para o Drawer de Categorias |
| <kbd>▶ Direita</kbd> / <kbd>CH +</kbd> | **Próximo Canal** (Zapping rápido) | Navegar para os Cards de Canais |
| <kbd>▲ Cima</kbd> / <kbd>▼ Baixo</kbd> | Exibir / Ocultar o HUD informativo | Navegar verticalmente pela lista de canais |
| <kbd>OK</kbd> / <kbd>Enter</kbd> | **Play / Pause** do stream | Abrir o canal selecionado em tela cheia |
| <kbd>Voltar</kbd> / <kbd>Back</kbd> | **Sair do player** e voltar para a grade | Sair do aplicativo |

---

## 🏗️ Arquitetura & Stack Tecnológica

O projeto adota os padrões recomendados pelo Google para desenvolvimento moderno no Android:

```
GabesTV/
├── app/
│   ├── src/main/
│   │   ├── assets/
│   │   │   └── sample_channels.m3u          # Playlist local de contingência
│   │   ├── java/com/gabestv/iptv/
│   │   │   ├── data/
│   │   │   │   └── ChannelRepository.kt     # Gerenciamento de playlists, cache e zapping
│   │   │   ├── di/
│   │   │   │   └── AppModule.kt             # Injeção de dependências com Dagger Hilt
│   │   │   ├── model/
│   │   │   │   └── Channel.kt               # Modelos de dados (Channel, Category, Playlist)
│   │   │   ├── parser/
│   │   │   │   └── M3UParser.kt             # Parser M3U/M3U8 de alta performance com Regex
│   │   │   ├── player/
│   │   │   │   └── PlayerManager.kt         # Wrapper do Media3 ExoPlayer com políticas de buffer
│   │   │   ├── ui/
│   │   │   │   ├── components/
│   │   │   │   │   ├── CategoryDrawer.kt    # Menu lateral de navegação D-Pad
│   │   │   │   │   └── ChannelCard.kt       # Card com zoom 1.08x e suporte a logos SVG/PNG
│   │   │   │   ├── player/
│   │   │   │   │   └── PlayerScreen.kt      # Player tela cheia com HUD OSD animado
│   │   │   │   ├── theme/
│   │   │   │   │   └── Theme.kt             # Design System Dark Theme para telas OLED/LED
│   │   │   │   └── MainScreen.kt            # Grid principal de canais em split-screen
│   │   │   ├── viewmodel/
│   │   │   │   └── MainViewModel.kt         # Gerenciamento de estado com StateFlow e UDF
│   │   │   ├── GabesTVApplication.kt        # Classe de inicialização Hilt
│   │   │   └── MainActivity.kt              # Ponto de entrada compatível com Leanback Launcher
│   │   └── AndroidManifest.xml              # Configurações de TV, no-touchscreen e Leanback
```

### Tecnologias Utilizadas:
* **Linguagem:** [Kotlin 1.9](https://kotlinlang.org/)
* **UI Toolkit:** [Jetpack Compose for TV](https://developer.android.com/jetpack/compose/tv) (Material 3 TV `1.0.0-rc02` + Foundation `1.0.0-alpha11`)
* **Media & Streaming:** [AndroidX Media3 ExoPlayer 1.3.1](https://developer.android.com/media/media3) (HLS, OkHttp DataSource, Session)
* **Injeção de Dependências:** [Dagger Hilt 2.51.1](https://dagger.dev/hilt/) + KSP
* **Networking:** [OkHttp 4.12](https://square.github.io/okhttp/) com Logging Interceptor e headers de IPTV
* **Carregamento de Imagens:** [Coil 2.6](https://coil-kt.github.io/coil/) com decodificação de SVG para logos nítidos em 4K
* **Concorrência:** Kotlin Coroutines & `StateFlow`
* **Testes Unitários:** JUnit 4, Google Truth e `kotlinx-coroutines-test`

---

## 🚀 Como Compilar e Rodar

### Pré-requisitos
* **Java Development Kit (JDK):** Versão 17
* **Android SDK:** Compile SDK 34 / Min SDK 24
* **Gradle:** 8.6+ (ou use o wrapper `./gradlew` incluído)

### 1. Clonar o Repositório
```bash
git clone https://github.com/GdsPereira/GabesTV.git
cd GabesTV
```

### 2. Compilar o APK (Debug ou Release)
Para gerar o APK em modo Debug:
```bash
./gradlew :app:assembleDebug
```
O arquivo será gerado em: `app/build/outputs/apk/debug/app-debug.apk`.

Para rodar os testes unitários da suíte de testes:
```bash
./gradlew testDebugUnitTest
```

---

## 📺 Instalando na TV via ADB (Sem fios / Wi-Fi)

Você pode instalar e testar o GabesTV diretamente na sua Smart TV (TCL, Google TV, Mi Box, etc.) pela rede local:

1. **Ative o Modo Desenvolvedor na TV:**
   * Vá em **Configurações** > **Sistema** > **Sobre**.
   * Pressione **OK** 7 vezes sobre a opção **Número da Versão (Build)**.
2. **Ative a Depuração USB / Depuração de Rede:**
   * Volte para **Configurações** > **Opções do Desenvolvedor** > ative **Depuração USB / Rede**.
3. **Conecte pelo computador e instale:**
   ```bash
   # Conectar à TV via IP local (exemplo)
   adb connect 192.168.1.150:5555

   # Instalar o APK na TV
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## ⚙️ Configurando sua Lista M3U / Threadfin

Por padrão, o GabesTV busca sua lista no endpoint configurado no repositório (`ChannelRepository.kt`):

```kotlin
companion object {
    const val DEFAULT_THREADFIN_URL = "https://tv.gabesp.com.br/m3u/threadfin.m3u"
}
```

* Caso o servidor esteja offline ou a rede caia durante a inicialização, o aplicativo faz **fallback gracioso automático** para a lista de contingência embutida (`app/src/main/assets/sample_channels.m3u`).

---

## 🔄 CI / CD (Automação de Builds)

O repositório conta com integração e entrega contínuas pré-configuradas:

* **GitHub Actions (`.github/workflows/build-apk.yml`):**
  * Compila o APK e executa testes a cada `push` na branch `main`.
  * Gera e disponibiliza o artefato `GabesTV-AndroidTV.apk` pronto para download.
  * Ao criar uma tag de versão (ex: `v1.0.0`), cria automaticamente uma **GitHub Release** com o binário anexado.
* **Azure Pipelines (`azure-pipelines.yml`):**
  * Pipeline multiplataforma pronta para agentes Ubuntu corporativos com publicação de relatórios JUnit.

---

## 📄 Licença

Este projeto está sob a licença [MIT](LICENSE).

<div align="center">
Desenvolvido com 💜 por <b><a href="https://github.com/GdsPereira">Gabriel Pereira</a></b>
</div>
