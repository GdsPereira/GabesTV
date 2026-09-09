# 🛡️ Governança de Segurança e Branch Protection - GabesTV

Este documento descreve as diretrizes de governança de código, responsabilidades de revisão e os passos para configurar e manter a proteção de branches no repositório GitHub do **GabesTV**.

---

## 👥 1. Code Owners (`.github/CODEOWNERS`)

O arquivo [`.github/CODEOWNERS`](../.github/CODEOWNERS) define os responsáveis obrigatórios pela aprovação de pull requests antes que o código possa ser integrado:

- **Workflows e CI/CD (`/.github/workflows/`)**: `@GdsPereira`
- **Código da Aplicação Android (`/app/`)**: `@GdsPereira`
- **Build, Dependências e Segurança (`build.gradle.kts`, `settings.gradle.kts`, etc.)**: `@GdsPereira`
- **Políticas e Governança (`SECURITY.md`, `/docs/`)**: `@GdsPereira`

---

## 🔒 2. Configuração de Branch Protection Rules no GitHub

Para aplicar a governança de forma mandatória, configure as regras de proteção para as branches **`main`** e **`develop`**:

### Passo a Passo no GitHub Web:
1. Acesse o repositório no GitHub: `https://github.com/GdsPereira/GabesTV`.
2. Clique em **Settings** > **Branches** (ou **Rules** > **Rulesets**).
3. Em *Branch protection rules*, clique em **Add branch protection rule** (uma para `main` e outra para `develop`).

### Parâmetros Obrigatórios para `main` e `develop`:
| Opção | Estado | Descrição |
| :--- | :---: | :--- |
| **Branch name pattern** | `main` / `develop` | Define a branch alvo da proteção. |
| **Require a pull request before merging** | ✅ Ativado | Impede `git push` direto na branch. |
| **Require approvals** | ✅ Ativado (`1`) | Exige aprovação de ao menos 1 revisor qualificado. |
| **Dismiss stale pull request approvals when new commits are pushed** | ✅ Ativado | Invalida aprovações prévias caso novos commits sejam enviados. |
| **Require review from Code Owners** | ✅ Ativado | Garante que o Code Owner da área modificada aprove a mudança. |
| **Require status checks to pass before merging** | ✅ Ativado | Bloqueia o merge se qualquer checagem automatizada falhar. |
| **Require branches to be up to date before merging** | ✅ Ativado | Garante que o PR foi testado com a versão mais recente da base. |
| **Require conversation resolution before merging** | ✅ Ativado | Exige que todas as threads de comentários do PR sejam resolvidas. |
| **Do not allow bypassing the above settings** | ✅ Ativado | Garante que nem administradores possam pular os gates de segurança. |

### Status Checks Obrigatórios para Seleção:
Após rodar os workflows pela primeira vez, marque como obrigatórios (*Required*) na lista de checks:
- `Build Android TV APK` (executado por `.github/workflows/build-apk.yml`)
- `Secret Scanning (Gitleaks)` (executado por `.github/workflows/security-scan.yml`)
- `CodeQL SAST (Java-Kotlin)` (executado por `.github/workflows/security-scan.yml`)
- `Dependency Review / Vulnerability Scan` (executado por `.github/workflows/security-scan.yml`)

---

## 🚀 3. Fluxo de Trabalho (Shift-Left Security)

1. **Desenvolvimento em Feature Branch**:
   ```bash
   git checkout -b feature/nome-da-funcionalidade
   ```
2. **Validação Local Pré-Push**:
   ```powershell
   $env:JAVA_HOME="C:\Users\GP\.jdks\jbr-21.0.11"; ./gradlew :app:testDebugUnitTest
   ```
3. **Abertura do Pull Request**:
   - Os status checks de segurança (Gitleaks, CodeQL, Trivy) e build/testes serão iniciados automaticamente.
   - O revisor definido no `CODEOWNERS` será notificado automaticamente para auditoria.
4. **Merge após Aprovação**:
   - Uma vez que todos os checks estiverem verdes e a aprovação for concedida, o merge é liberado para a branch alvo.
