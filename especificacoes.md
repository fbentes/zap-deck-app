# Documento de Especificações Técnicas - ZapDeck

Este documento apresenta as especificações completas de funcionamento e arquitetura do aplicativo **ZapDeck** (conhecido na fase inicial de concepção pelo codinome histórico *Extrai Cartão*), projetado para automatizar a captura, enquadramento, digitalização e categorização de dados de cartões de visita físicos, com armazenamento local seguro e agilidade no contato direto via WhatsApp, NFC e QR Code.

---

## 1. Visão Geral do Sistema

O **ZapDeck** é uma ferramenta de produtividade para profissionais em campo, vendedores, empresários e prestadores de serviço. O aplicativo automatiza um fluxo de trabalho moroso:
1. Recebimento ou visualização de um cartão de visita físico (frente e verso).
2. Enquadramento milimétrico, eliminação de fundo (mesas/escrivaninhas) e remoção de sombras.
3. Leitura e estruturação instantânea de contatos por visão computacional local (On-Device ML Kit).
4. Persistência offline em banco de dados local seguro (Room / SQLite).
5. Envio ágil de mensagem de introdução no WhatsApp para registrar o contato.
6. Compartilhamento digital via QR Code dinâmico, NFC Beam (transmissão por aproximação) e vCard nativo.

O processamento padrão do **ZapDeck** é **100% On-Device (Local)** através do Google ML Kit Latin Text Recognition e do motor proprietário `CardImageProcessor`. De forma opcional e com transparência na interface, o usuário pode acionar um enriquecimento multimodal na nuvem via API do Gemini (quando conectado à internet e com credencial configurada).

---

## 2. Requisitos Funcionais (RF)

### RF01 - Captura Multimodal de Cartões (Frente e Verso)
O usuário pode alimentar o sistema de duas formas:
- **Câmera**: Capturar uma nova fotografia em tempo real (com suporte a captura sequencial de frente e verso).
- **Galeria**: Selecionar fotografias salvas previamente no álbum do dispositivo.

### RF02 - Enquadramento Automático e Remoção de Sombras (`CardImageProcessor`)
O sistema processa a imagem para:
- Detectar a geometria retangular do cartão físico, descartando 100% de fundos externos (madeira, toalhas, dedos).
- Remover sombras pontuais (mãos, celular) e equalizar iluminação e contraste.
- Detectar a orientação correta de leitura (0°, 90°, 180°, 270°) e rotacionar a imagem automaticamente.
- Salvar e exibir por padrão a versão otimizada e limpa.

### RF03 - Extração Estruturada Local e Classificação Rigorosa
O motor local analisa espacialmente e categoriza:
- **Nome / Razão Social / Marca**: Nome completo da empresa, clínica, consultório ou profissional (ex: "AVIVAR Clínica de Saúde", "DROGARIAS MAX").
- **WhatsApp Principal**: Celulares de 9 dígitos ou acompanhados de ícones e termos de WhatsApp.
- **Telefone Fixo**: Números de 8 dígitos convencionais ou marcados com telefone tradicional.
- **Telefone Secundário**: Canais telefônicos adicionais ou identificados no verso.
- **Endereço Comercial**: Logradouro, número, complementos (lojas, salas), bairro, cidade e CEP.
- **Redes Sociais e E-mail**: Instagram (@perfil) e e-mail comercial.
- **Anotações Manuscritas e Serviços**: Digitalização de notas feitas a caneta (atendentes, horários, telefones extras) e serviços ofertados.

### RF04 - Fallback Remoto Inteligente (Gemini API)
Caso o usuário deseje enriquecer a extração ou em cartões com diagramação incomum, é possível solicitar uma análise adicional via API Gemini, informando na interface quando a requisição é online.

### RF05 - Tela de Confirmação, Edição e Visualização Alternada
Antes de salvar, o usuário revisa todos os campos, podendo editar dados e alternar a visualização da imagem entre o cartão enquadrado/sem sombras e a foto original de captura.

### RF06 - Persistência Local (Offline-First com Room)
Todos os contatos e imagens são salvos localmente no dispositivo em SQLite via Room. Nenhuma informação pessoal é enviada a servidores sem a ação expressa do usuário.

### RF07 - Identificação do Proprietário (`usuario_android`)
O proprietário cadastra seu nome nas configurações do app para personalizar automaticamente as saudações e mensagens de introdução.

### RF08 - Envio Ágil de Mensagem no WhatsApp
Gera saudações dinâmicas conforme a hora local ("Bom dia", "Boa tarde", "Boa noite") acompanhadas de mensagem de apresentação, abrindo a conversa via Intent padrão sem exigir pré-cadastro na agenda.

### RF09 - Sincronização com a Agenda Nativa do Android
Opção direta de exportar qualquer contato salvo para a agenda de contatos do Android (`ContactsContract`).

### RF10 - Compartilhamento Digital (QR Code, NFC e vCard)
- **QR Code**: Geração de QR Code vCard para que terceiros capturem o contato diretamente pela câmera.
- **NFC Beam**: Transmissão por aproximação entre dispositivos com NFC habilitado.
- **Compartilhamento de APK**: Permite compartilhar o instalador do ZapDeck diretamente com outros aparelhos.

---

### 3. Requisitos Não Funcionais (RNF)

### RNF01 - Velocidade e Tempo de Resposta
O processamento On-Device (ML Kit + enquadramento e remoção de sombras) ocorre em menos de 1 segundo diretamente no smartphone, sem depender de conexão de rede. Caso o fallback online do Gemini seja acionado pelo usuário, a inferência deve responder em até 5 segundos sob conexão estável.

### RNF02 - Design de UI Moderno (Material Design 3)
Aplicação desenvolvida 100% em Jetpack Compose, utilizando as diretrizes do Material 3 com tipografia legível, contraste aprimorado, tema claro e escuro, e layout adaptável para uso ágil com uma só mão.

### RNF03 - Privacidade, LGPD e Minimização de Dados
O aplicativo funciona em modo local-first. As imagens e contatos ficam restritos ao banco Room no armazenamento privado do dispositivo. Nenhuma foto ou contato é enviado para servidores externos sem ação expressa do usuário. Não são coletados dados analíticos ou de telemetria sem consentimento.

---

## 4. Arquitetura da Solução

O sistema adota o padrão MVVM com separação modular de responsabilidades:

```
───────────────────────────────────────────────────────────────────────
|                     INTERFACE JETPACK COMPOSE (UI)                   |
───────────────────────────────────────────────────────────────────────
                                  │
                       Observa estado via StateFlow
                                  ▼
───────────────────────────────────────────────────────────────────────
|                             VIEWMODEL                               |
───────────────────────────────────────────────────────────────────────
       │                          │                         │
       ▼                          ▼                         ▼
───────────────          ───────────────────       ────────────────────
| ROOM DAO/DB |          | OFFLINE ML KIT  |       | GEMINI SERVICE   |
| Persistência|          | & IMAGE ENGINE  |       | Fallback Remoto  |
───────────────          ───────────────────       ────────────────────
```

### Tecnologias-Chave:
- **Linguagem**: Kotlin
- **Compilação**: Android SDK 36 (compatibilidade a partir do Android 7.0 - minSdk 24)
- **Interface**: Jetpack Compose com Material Design 3
- **Visão Computacional Local**: Google ML Kit Latin Text Recognition + `CardImageProcessor`
- **Banco de Dados Local**: Room Database com SQLite
- **Integrações de Sistema**: NFC (HCE e NDEF Beam), ZXing QR Code, ContactsContract, WhatsApp Intent
- **IA Multimodal (Fallback)**: Retrofit / Moshi com API Gemini (opcional e transparente)
