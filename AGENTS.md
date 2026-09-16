# Regras Mandatórias e Permanentes da Aplicação - ZapDeck

As diretrizes a seguir são regras estritas de funcionamento e design do aplicativo **ZapDeck** (conhecido na fase inicial de concepção pelo codinome histórico *Extrai Cartão*). Elas foram definidas pelo usuário e NUNCA devem ser alteradas, relaxadas ou esquecidas.

---

## 1. Processamento e Enquadramento da Imagem do Cartão (Card Framing & Shadow Removal)
- **Seleção Exclusiva do Cartão**: Em toda captura (frente ou verso, via câmera ou galeria), o sistema DEVE recortar/enquadrar com precisão milimétrica **apenas o retângulo do cartão de visita físico**, eliminando 100% de mesas de madeira, escrivaninhas, toalhas, dedos ou fundos externos (`CardImageProcessor.processCard`).
- **Remoção de Sombras**: O sistema DEVE remover sombras (sombras projetadas pelas mãos, celular ou iluminação ambiente desigual) e normalizar o contraste e brilho para que a superfície do cartão fique limpa, uniforme e nítida.
- **Orientação Automática**: O cartão DEVE ser rotacionado automaticamente (0°, 90°, 180° ou 270°) com base no reconhecimento de texto para sempre ser exibido na orientação correta de leitura humana.
- **Persistência e Visualização Padrão**: A imagem salva no banco de dados Room e exibida por padrão na interface de revisão DEVE ser SEMPRE a versão recortada, enquadrada e sem sombras (`capturedImageBase64`). O fundo da mesa NUNCA deve ser mantido na imagem final do contato.

---

## 2. Extração de Nome, Logotipo e Marca Comercial (Brand & Contact Name)
- O nome da empresa / clínica / consultório / profissional NUNCA deve ser cortado pela metade.
- **Análise Espacial**: Analisar elementos adjacentes ou empilhados. Por exemplo: se "AVIVAR" estiver estilizado como marca/logotipo acima de "Clínica de Saúde", o nome completo capturado DEVE ser:
  **`AVIVAR Clínica de Saúde`**
- Do mesmo modo: "Drogarias MAX - Sempre ao seu lado" deve conter o nome e slogan comercial se apresentados conjuntamente.
- O nome extraído deve ser limpo e categorizado com precisão como o Título principal do contato.

---

## 3. Classificação Rigorosa de Telefones (Telefone Fixo vs. WhatsApp)
- **Telefone Fixo (`landlinePhone`)**:
  - Números com 8 dígitos no corpo (ex: `(22) 3087-6777`).
  - Números acompanhados de ícone convencional de telefone (aparelho telefônico) ou palavras como "Tel", "Telefone", "Fixo".
- **WhatsApp Principal (`primaryPhone`)**:
  - Números com 9 dígitos no corpo (ex: `(22) 99781-0486`).
  - Números acompanhados de ícone do WhatsApp (balão de fala verde / símbolo do app) ou palavras como "WhatsApp", "Whats", "Zap".
- **Telefone Secundário (`secondaryPhone`)**:
  - Outros telefones de apoio quando existirem múltiplos canais.

---

## 4. Digitalização de Anotações Manuscritas (Caneta / Escrita Humana)
- Tudo o que estiver escrito à mão por um ser humano a caneta no cartão (seja na frente ou no verso), como nomes de atendentes, telefones extras, observações ou horários (ex: *"Osnila / Natalia 22 999603365"*), DEVE ser digitalizado e preservado no campo de **Observações** (`observations`).
- Jamais descartar dados manuscritos identificados no cartão.

---

## 5. Tratamento de Labels/Rótulos de Campos e Conteúdo Estrito de Observações
- **Labels Identificadores de Campos**: Quando uma palavra ou rótulo (ex: "WhatsApp", "Whats", "Zap", "Celular", "Telefone", "Tel", "Fixo", "Instagram", "Insta", "E-mail", "Email", "Endereço", etc.) aparecer acima ou antes de um número ou dado, ela serve EXCLUSIVAMENTE para distinguir o tipo do campo a que aquele dado pertence (diferenciando WhatsApp de telefone fixo, identificando redes sociais, etc.).
- **Proibição Absoluta em Observações**: NENHUM label/rótulo de campo DEVE ser colocado no campo de **Observações** (`observations`).
- **Conteúdo Estrito de Observações**: O campo de Observações DEVE conter APENAS o que realmente NÃO for campo, label ou dado estruturado encontrado anteriormente (por exemplo: anotações manuscritas a caneta, lista de especialidades ou serviços oferecidos como "Entrega em domicílio", facilidades ou horários de funcionamento). Palavras soltas de cabeçalho de campo NUNCA devem figurar nas observações.

