# Política de Privacidade — ZapDeck

**Última atualização:** 14 de setembro de 2026  
**Versão:** 1.0.0  
**Aplicação:** ZapDeck (Android)  

A presente Política de Privacidade descreve como o aplicativo **ZapDeck** trata, armazena e protege dados pessoais, em estrita observância à Lei Geral de Proteção de Dados Pessoais do Brasil (LGPD — Lei nº 13.709/2018) e às melhores práticas internacionais de segurança e minimização de dados (*Privacy by Design* e *Privacy by Default*).

---

## 1. Princípio Fundamental: Arquitetura Local-First (On-Device)

O **ZapDeck** foi concebido prioritariamente sob a premissa de processamento local (*On-Device*). Isso significa que:
1. **As imagens de cartões de visita capturadas ou importadas permanecem gravadas exclusivamente no armazenamento seguro e privado do seu dispositivo móvel** (banco de dados SQLite protegido pelo sandbox do Android).
2. **O reconhecimento ótico de caracteres (OCR) e a estruturação de nomes, telefones, endereços e anotações são executados localmente** através dos modelos incorporados do Google ML Kit Latin Text Recognition e do motor proprietário `CardImageProcessor`.
3. **Não mantemos servidores centrais de armazenamento de contatos, nem comercializamos, compartilhamos ou temos acesso aos dados que você digitaliza.**

---

## 2. Dados Tratados pelo Aplicativo

O ZapDeck processa apenas os dados estritamente necessários para o cumprimento das suas funções de produtividade:

- **Dados Extraídos de Cartões**: Nome de pessoas ou empresas, telefones (WhatsApp, fixo, celular), e-mails, endereços comerciais e anotações manuscritas encontradas no cartão.
- **Identificação do Proprietário (`usuario_android`)**: Nome configurado voluntariamente pelo usuário nas configurações do app para preencher o modelo de apresentação de mensagens.
- **Imagens Fotográficas**: Fotos da frente e do verso de cartões de visita capturadas pela Câmera ou selecionadas da Galeria, armazenadas em formato comprimido na base de dados local do app.

---

## 3. Uso Opcional de Serviços Remotos (API Gemini)

Caso o usuário opte expressamente por acionar o recurso secundário de análise na nuvem (botão identificado na interface):
- Apenas a imagem do cartão em análise é transmitida de forma criptografada (HTTPS/TLS) diretamente aos servidores da Google Gemini API para interpretação multimodal.
- Essa transmissão só ocorre mediante ação do usuário e conexão ativa com a internet.
- Nenhuma chave de acesso, credencial ou identificador pessoal do usuário é compartilhado com terceiros além do provedor do serviço de inferência.

---

## 4. Permissões do Android e Justificativas de Uso

O ZapDeck solicita apenas as permissões essenciais para execução das suas tarefas:

| Permissão | Finalidade |
|---|---|
| `CAMERA` | Capturar fotografias em tempo real de cartões de visita físicos (frente e verso). |
| `READ_EXTERNAL_STORAGE` / Photo Picker | Selecionar imagens de cartões já existentes no álbum de fotos do usuário. |
| `READ_CONTACTS` / `WRITE_CONTACTS` | Permitir que o usuário exporte o contato salvo no ZapDeck diretamente para a agenda de contatos nativa do Android. |
| `NFC` | Transmitir e receber cartões digitais por aproximação entre dispositivos compatíveis. |
| `INTERNET` | Acessar o serviço opcional de enriquecimento de IA na nuvem e consultar deep links quando solicitado. |

---

## 5. Retenção e Exclusão de Dados

- **Controle Total do Usuário**: Você pode editar, corrigir ou excluir qualquer contato ou imagem salva no ZapDeck a qualquer momento.
- **Exclusão Definitiva**: Ao excluir um contato na tela de detalhes ou desinstalar o aplicativo do smartphone, todos os registros e imagens associadas são permanentemente removidos do banco de dados local do dispositivo.

---

## 6. Compartilhamento de Dados pelo Usuário

O compartilhamento de contatos (via WhatsApp, vCard, QR Code ou NFC) é sempre iniciado **manualmente e sob total controle do usuário**. O ZapDeck não realiza envios automáticos em segundo plano.

---

## 7. Contato do Encarregado pelo Tratamento de Dados (DPO)

Dúvidas, solicitações de esclarecimentos ou requisições sobre o tratamento de dados pessoais no âmbito do ZapDeck podem ser encaminhadas ao desenvolvedor responsável:
- **E-mail de Contato**: `facbentes@gmail.com`
