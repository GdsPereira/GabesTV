# GabesTV - Diretrizes e Regras do Projeto

Este repositório contém o aplicativo **GabesTV**, um reprodutor de IPTV híbrido desenvolvido em Kotlin e Jetpack Compose com suporte simultâneo a **Android TV / TV Box** e **Smartphones / Tablets**.

---

## ⚙️ Ambiente de Build e JDK

- **Versão do Java no Host (Windows)**: O sistema local possui o Java 25 no PATH, que é incompatível com o Gradle 8.6 (`FAILURE: Build failed with an exception. What went wrong: 25.0.2`).
- **Regra**: Sempre execute comandos Gradle definindo explicitamente a variável `JAVA_HOME` para o JDK 21 (ou JDK 17):
  ```powershell
  $env:JAVA_HOME="C:\Users\GP\.jdks\jbr-21.0.11"; ./gradlew test
  $env:JAVA_HOME="C:\Users\GP\.jdks\jbr-21.0.11"; ./gradlew assembleDebug assembleRelease
  ```
  *Nota: Não commitar caminhos locais de Windows no `gradle.properties` do repositório para não quebrar a execução no CI (Ubuntu).*

---

## 📱📺 Arquitetura Híbrida (Android TV vs Mobile)

1. **Manifesto Multiplataforma**:
   - `android.software.leanback` e `android.hardware.screen.landscape` devem sempre ter `android:required="false"` no `AndroidManifest.xml` para permitir instalação e funcionamento transparente em celulares e tablets.
2. **Separação de Componentes Compose**:
   - Componentes para TV utilizam `androidx.tv.material3` com foco gerenciado para D-Pad (10-foot UI).
   - Componentes Mobile utilizam `androidx.compose.material3` padrão (Scaffold, TopAppBar, Chips de categorias, gestos touch de brilho/volume e suporte a Picture-in-Picture).
   - A detecção de dispositivo é realizada centralizadamente via `DeviceType` (`isTvDevice`).
3. **Estado Unificado no ViewModel**:
   - O `MainViewModel` é a única fonte da verdade para a categoria ativa (`selectedCategoryId`) e canal em reprodução (`activePlayingChannel`). Telas de UI não devem duplicar esses estados internamente para evitar descompasso em recomposições.

---

## 🌐 Rede, Streaming IPTV e Segurança

- **Endpoint Padrão**: Playlist e canais são consumidos preferencialmente via Cloudflare Tunnel em `https://tv.gabesp.com.br`.
- **Segurança de Rede**: O arquivo `network_security_config.xml` proíbe tráfego HTTP inseguro por padrão (`cleartextTrafficPermitted="false"`), permitindo apenas domínios autorizados e faixas de rede local (`10.0.0.0`, `192.168.0.0`).
- **Resiliência do Player**: No `PlayerManager`, utilize sempre `OkHttpDataSource` configurado com cliente permissivo para tolerar certificados autoassinados/locais comuns em provedores IPTV e Threadfin.

---

## 🧪 Qualidade, ProGuard e CI

- **ProGuard / R8**: Para builds de produção (`assembleRelease`), garanta que classes de modelo, Dagger/Hilt, Coroutines, OkHttp, Coil e ExoPlayer/Media3 estejam preservadas em `app/proguard-rules.pro`.
- **Testes Unitários**: Testes para `ChannelRepository` e `MainViewModel` devem ser mantidos e validados antes de novos deploys.
- **CI / Workflows**: No GitHub Actions, utilize `actions/setup-java@v5` com JDK 17 e gere ambos os artefatos (`debug` e `release`).
