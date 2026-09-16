# Changelog — ZapDeck

Todas as alterações notáveis deste projeto serão documentadas neste arquivo.

O formato baseia-se no [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/),
e este projeto adere ao [Versionamento Semântico](https://semver.org/lang/pt-BR/).

---

## [1.0.1] - 2026-09-15

### Adicionado
- **Vitrine e Seletor de Novos Ícones**: Diálogo interativo para escolha de opções de design de ícone do aplicativo com pré-visualização e confirmação.
- **Créditos do Criador**:
  - Inclusão da foto real do criador Fábio A. C. Bentes na tela de seleção de novos ícones;
  - Modal de visualização expandida da foto do criador com acabamento e alta fidelidade visual;
  - Ícone e atalho direto integrado para o perfil oficial no LinkedIn (`https://www.linkedin.com/in/fabio-bentes/`).
- **Asset Vetorial**: Ícone vetorial oficial customizado do LinkedIn (`ic_linkedin_custom.xml`).

---

## [1.0.0] - 2026-09-14

### Adicionado
- **Nome Oficial**: Consolidação da marca e identificação do produto como **ZapDeck**.
- **Processamento On-Device de Alta Precisão**:
  - Motor proprietário `CardImageProcessor` para enquadramento geométrico e recorte milimétrico do retângulo do cartão;
  - Eliminação de fundos indesejados (mesas de madeira, toalhas, escrivaninhas, dedos);
  - Normalização de sombras projetadas pelas mãos ou aparelho móvel;
  - Rotação automática inteligente (0°, 90°, 180°, 270°) conforme a orientação do texto.
- **Digitalização de Notas Manuscritas**:
  - Reconhecimento e preservação de anotações feitas a caneta (nomes de atendentes, horários, telefones extras) no campo de observações.
- **Classificação Rigorosa de Telefones**:
  - Separação precisa entre WhatsApp principal (9 dígitos / marcadores do app), telefone fixo (8 dígitos) e canais secundários.
- **Compartilhamento Digital Avançado**:
  - Geração de QR Code dinâmico para leitura rápida de vCard;
  - Módulo NFC Beam e Host Card Emulation (HCE) para transmissão de cartão por aproximação;
  - Compartilhamento direto do arquivo de instalação do aplicativo (.apk).
- **Documentação Formal e Preparação para o INPI**:
  - Inclusão de `README.md`, `PRIVACY.md` (em conformidade com a LGPD), `SECURITY.md`, `THIRD_PARTY_NOTICES.md` e `INPI-MANIFESTO.txt`.

### Modificado
- Versão atualizada para `1.0.0` no arquivo `app/build.gradle.kts`.
- Atualização e alinhamento de `especificacoes.md` e `plano_execucao.md` refletindo a arquitetura híbrida local-first (ML Kit primário e Gemini opcional).
- Tratamento seguro de credenciais, garantindo que builds de desenvolvimento operem perfeitamente sem exigir chaves de produção.
