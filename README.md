# ZapDeck

Aplicativo Android nativo para captura inteligente, enquadramento milimétrico, digitalização offline e gerenciamento de cartões de visita comerciais e profissionais, com integração rápida ao WhatsApp, agenda do sistema, QR Code e NFC.

---

## 1. Finalidade

O **ZapDeck** tem como propósito eliminar a digitação manual de contatos comerciais a partir de cartões de visita físicos (frente e verso). Através de visão computacional em dispositivo (*On-Device*) e processamento de imagem dedicado, o aplicativo enquadra o cartão físico, remove fundos indesejados (como mesas ou escrivaninhas), atenua sombras e extrai de forma estruturada:
- Nome fantasia / Marca comercial / Razão social completa;
- Telefone WhatsApp principal (identificado por DDD, 9 dígitos ou ícones do mensageiro);
- Telefone fixo (8 dígitos ou marcadores tradicionais de telefone);
- Telefones secundários adicionais;
- Endereço comercial completo;
- Redes sociais (@perfil Instagram) e e-mails comerciais;
- Digitalização de notas manuscritas à caneta (nomes de atendentes, horários, telefones extras).

Após o reconhecimento, os dados são salvos localmente e podem ser transmitidos diretamente ao WhatsApp através de modelo pré-formatado com saudação dinâmica ("Bom dia", "Boa tarde", "Boa noite"), exportados para a agenda telefônica nativa do Android ou compartilhados via QR Code dinâmico e NFC Beam.

---

## 2. Principais Funcionalidades

- **Captura Multimodal (Frente e Verso)**: Suporte para fotografia pela Câmera ou seleção a partir da Galeria do dispositivo.
- **Enquadramento Preciso e Remoção de Sombras (`CardImageProcessor`)**: Identificação dos contornos do cartão de visita com recorte automatizado, eliminação de superfícies externas e neutralização de sombras causadas pelas mãos ou aparelho.
- **Orientação Automática**: Detecção do ângulo de leitura e rotação automática (0°, 90°, 180°, 270°).
- **Processamento 100% Offline (Local-First)**: Reconhecimento de caracteres alimentado pelo Google ML Kit Text Recognition rodando inteiramente na CPU/GPU do smartphone.
- **Fallback Remoto Opcional (Gemini Multimodal API)**: Possibilidade de consulta secundária à nuvem com feedback transparente na interface para casos complexos.
- **Persistência Local Segura**: Armazenamento completo em SQLite através do Jetpack Room, garantindo privacidade estrita e autonomia total sem internet.
- **Integração Ágil com WhatsApp**: Disparo de mensagens personalizadas sem necessidade de salvar o número previamente na agenda.
- **Compartilhamento Digital**:
  - Geração de QR Code vCard para leitura rápida por outros celulares;
  - NFC Card Emulation (HCE) e NDEF Beam para transmissão por aproximação;
  - Compartilhamento direto do arquivo de instalação do aplicativo (.apk).

---

## 3. Arquitetura

O projeto adota o padrão moderno **MVVM (Model-View-ViewModel)** com **Jetpack Compose** e **Material Design 3**:

```
app/src/main/java/com/example/ (migração planejada para br.com.facbentes.zapdeck)
├── api/             # Modelos e cliente Retrofit para o fallback Gemini
├── data/            # Entidades Room, Data Access Object (DAO) e Repositório local
├── ui/              # Telas, animações de processamento, diálogos e tema Material 3
├── utils/           # Processamento de imagem, scanner local ML Kit, NFC, QR Code e agenda
└── viewmodel/       # Gestão de estado reativo através de StateFlow
```

---

## 4. Pré-requisitos e Ambiente de Desenvolvimento

- **Android Studio**: Ladybug / Jellyfish ou superior.
- **JDK**: Java 17 ou Java 21.
- **Android SDK**:
  - `minSdk`: 24 (Android 7.0 Nougat)
  - `compileSdk`: 36
  - `targetSdk`: 36
- **Gradle**: 8.9+ com Gradle Kotlin DSL (`build.gradle.kts`).

---

## 5. Configuração Local e Gerenciamento de Segredos

O **ZapDeck** é projetado segundo a premissa de minimização de privilégios e segurança de credenciais:
1. Copie o arquivo `.env.example` para `.env`:
   ```bash
   cp .env.example .env
   ```
2. Caso deseje utilizar o fallback online opcional da API Gemini, adicione sua chave de API no arquivo `.env`:
   ```env
   GEMINI_API_KEY=sua_chave_aqui
   ```
3. O Secrets Gradle Plugin injeta a chave automaticamente em tempo de compilação dentro do `BuildConfig.GEMINI_API_KEY`. Se a chave não for informada, o aplicativo operará normalmente em modo 100% offline via ML Kit.
4. **Nunca** faça commit de arquivos `.env` ou chaves privadas. O arquivo `.gitignore` do repositório já está configurado para proteger essas credenciais.

---

## 6. Compilação e Testes

Para compilar e verificar a integridade da aplicação:

### Executar Testes Unitários e Robolectric:
```bash
gradle :app:testDebugUnitTest
```

### Compilar APK em modo Debug:
```bash
gradle :app:assembleDebug
```

---

## 7. Limitações Conhecidas

- O enquadramento automático depende de contraste mínimo entre a borda do cartão e a superfície em que ele estiver apoiado.
- A transmissão via aproximação (NFC) requer que ambos os dispositivos possuam hardware NFC ativo e compatível.
- A abertura direta de conversa no WhatsApp requer que o aplicativo WhatsApp (ou WhatsApp Business) esteja instalado no aparelho do usuário.

---

## 8. Titularidade, Autoria e Licença

- **Produto**: ZapDeck
- **Repositório Oficial**: `https://github.com/facbentes/zap-deck-app`
- **Autor / Titular**: Todos os direitos patrimoniais e autorais reservados. Software desenvolvido para registro perante o Instituto Nacional da Propriedade Industrial (INPI).
