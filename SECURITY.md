# 🛡️ Política de Segurança (Security Policy) - GabesTV

A equipe do **GabesTV** leva a segurança e a privacidade de seus usuários, dados e infraestrutura com seriedade. Este documento descreve as diretrizes para relato responsável de vulnerabilidades, prazos de atendimento (SLA), matriz de versões suportadas e nosso compromisso de *Safe Harbor*.

---

## 📦 Versões Suportadas

Oferecemos correções de segurança ativamente para as seguintes versões do projeto:

| Versão / Branch | Suportada | Notas |
| :--- | :---: | :--- |
| **Branch `main` (Último commit)** | ✅ Sim | Versão em produção / branch padrão de desenvolvimento estável. |
| **Última Release Oficial (`v*`)** | ✅ Sim | Binários estáveis distribuídos via GitHub Releases. |
| **Versões Anteriores (< última estável)** | ❌ Não | Recomendamos atualizar imediatamente para a versão mais recente. |

---

## 🚨 Como Reportar uma Vulnerabilidade (Coordinated Vulnerability Disclosure)

> [!CAUTION]
> **NUNCA crie uma Issue pública para reportar uma vulnerabilidade de segurança.** Isso expõe usuários a riscos antes que uma correção esteja disponível.

Para relatar uma falha de segurança de maneira responsável e confidencial, utilize um dos seguintes canais:

1. **Canal Preferencial (GitHub Security Advisories)**:
   - Acesse: **[GitHub Private Vulnerability Reporting](https://github.com/GdsPereira/GabesTV/security/advisories/new)**
   - Este canal permite que pesquisadores e mantenedores discutam, criem patches privados e validem correções em sigilo antes da publicação.

2. **Canal Alternativo (E-mail)**:
   - Caso não utilize o GitHub, envie um e-mail com o assunto `[SECURITY GABESTV] - Vulnerabilidade` para o mantenedor principal.

### 📝 O que incluir no seu relatório:
- **Resumo**: Descrição clara da vulnerabilidade encontrada.
- **Severidade estimada**: Baseada na métrica CVSS v3.1 ou impacto prático percebido.
- **Passos para reprodução**: Instruções detalhadas, endpoints afetados, payloads de teste ou links para vídeos demonstrativos.
- **Código de Prova de Conceito (PoC)**: Script, chamada HTTP ou fluxo no app que demonstre a falha sem causar dano ou negação de serviço.
- **Mitigações sugeridas**: Caso tenha uma sugestão de correção em código (Pull Request ou patch).

---

## ⏱️ SLAs de Resposta e Janela de Correção

Nos comprometemos com os seguintes tempos de resposta para vulnerabilidades legítimas reportadas:

| Nível de Severidade | CVSS Score | Confirmação Inicial | Triagem e Validação | Prazo Máximo de Correção (Patch/Hotfix) |
| :--- | :---: | :---: | :---: | :---: |
| 🔴 **Crítica** | 9.0 – 10.0 | ≤ 24 horas | ≤ 48 horas | **24 a 72 horas** |
| 🟠 **Alta** | 7.0 – 8.9 | ≤ 48 horas | ≤ 72 horas | **Até 14 dias** |
| 🟡 **Média** | 4.0 – 6.9 | ≤ 48 horas | ≤ 5 dias | **Até 30 dias** |
| 🟢 **Baixa** | 0.1 – 3.9 | ≤ 72 horas | ≤ 7 dias | **Próxima release programada (≤ 60 dias)** |

Após a disponibilização do patch corretivo, um **Security Advisory** público será emitido creditando o pesquisador que reportou a vulnerabilidade (caso este deseje reconhecimento).

---

## 🤝 Diretrizes de Safe Harbor

Consideramos pesquisas de segurança de boa-fé essenciais para a segurança de nosso ecossistema. Não moveremos ações legais contra pesquisadores que:
- Realizem testes sem comprometer a estabilidade, integridade ou disponibilidade dos servidores e usuários.
- Não acessem, modifiquem ou vazem dados privados de terceiros.
- Não realizem ataques de Negação de Serviço (DoS / DDoS), phishing ou ataques de engenharia social contra mantenedores ou usuários.
- Deem tempo razoável aos mantenedores para corrigir a vulnerabilidade antes de qualquer divulgação pública (divulgação coordenada).
