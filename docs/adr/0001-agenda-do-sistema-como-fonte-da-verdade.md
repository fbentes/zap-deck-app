# ADR 0001 — A agenda do sistema é a fonte da verdade dos cartões

- **Status:** Proposto
- **Data:** 2026-10-17
- **Contexto de decisão:** migração do app para Flutter (Android + iOS) com restauração dos dados ao trocar de aparelho.

## 1. Contexto

Hoje o ZapDeck (Android/Kotlin) grava cada cartão na agenda do Android (`ContactsContract`) e guarda o que não tem campo próprio na **nota** do contato, em linhas `Chave: valor`, depois da marca `#ZapDeck`:

| Chave na nota | Conteúdo |
|---|---|
| `#ZapDeck` (e `#CardZap`, legado) | marca de que o contato é do app |
| `CreatedAt` | data de criação (ms) |
| `LandlinePhone`, `Email` | telefone fixo e e-mail |
| `Instagram`, `Facebook` | perfis |
| `PreferWhatsAppBusiness`, `InstagramFollowed` | preferências |
| `Observations` | serviços, especialidades, anotações manuscritas |
| `BackImageBase64` | imagem do verso do cartão em base64 |

Requisito do produto: **os dados devem ficar na lista de contatos de cada sistema operacional, para poderem ser restaurados ao trocar de aparelho.**

Restrições do iOS que tornam o formato atual inviável:

1. **Nota do contato.** Desde o iOS 13, ler e gravar a nota exige o entitlement `com.apple.developer.contacts.notes`, concedido pela Apple mediante solicitação e justificativa.
2. **Uma foto por contato**, reduzida e quadrada. Não comporta o verso nem a frente em alta resolução.
3. **Conta.** Contatos criados em "No meu iPhone" não sincronizam; só os da conta iCloud (ou Google, Exchange, CardDAV) são restaurados.
4. Não existe campo livre para metadados do app além de nota e campos rotulados.

## 2. Decisão

1. **A agenda do sistema é a fonte da verdade** dos dados textuais do cartão. O app lê e grava nela; o banco local, se existir, é apenas **cache de busca e pode ser apagado e refeito** a partir da agenda.
2. Cada dado usa o **campo nativo** que existe nos dois sistemas (seção 3), de modo que a restauração por iCloud Contacts ou conta Google funcione sem depender do app.
3. A marca "este contato é do ZapDeck" passa a ser um **site (URL) com rótulo `ZapDeck`**, e não mais o texto `#ZapDeck` na nota. Ela carrega um `id` estável e as preferências curtas (seção 4).
4. **Observações** vão para a **nota**, com solicitação do entitlement no iOS (Plano A). Se a Apple negar, vale o Plano B (seção 5).
5. **Imagens que não cabem na agenda** (verso e frente em alta) vão para um **backup separado, exportável** (seção 6). A foto do contato continua sendo a frente do cartão, reduzida.
6. O app **continua lendo o formato antigo** (nota com `#ZapDeck`/`#CardZap`) e o converte para o formato novo ao salvar (seção 8).

## 3. Mapeamento de campos

| Dado do cartão | Android (`ContactsContract`) | iOS (`Contacts.framework`) | Observação |
|---|---|---|---|
| Nome | `StructuredName.DISPLAY_NAME` | `givenName` / `organizationName` | Nome de empresa vai também em `organizationName` no iOS, para a agenda exibir como empresa. |
| WhatsApp principal | `Phone` tipo `MOBILE` | `phoneNumbers` rótulo `mobile` | |
| WhatsApp secundário | `Phone` tipo `OTHER` | `phoneNumbers` rótulo `other` | |
| Telefone fixo | `Phone` tipo `HOME` | `phoneNumbers` rótulo `home` | Mantém a regra de classificação do `AGENTS.md` (8 dígitos = fixo). |
| E-mail | `Email` tipo `WORK` | `emailAddresses` rótulo `work` | |
| Endereço | `StructuredPostal.FORMATTED_ADDRESS` tipo `WORK` | `postalAddresses` rótulo `work` | No iOS, preencher também `street`/`city`/`state` quando possível, para o mapa funcionar. |
| Instagram | `Website` URL, rótulo `Instagram` | `urlAddresses` rótulo `Instagram` | Valor: `https://instagram.com/<perfil>` (sem `@`). |
| Facebook | `Website` URL, rótulo `Facebook` | `urlAddresses` rótulo `Facebook` | Ver seção 3.1. |
| Observações | `Note` | `note` (exige entitlement) | Ver seção 5. |
| Foto do cartão (frente) | `Photo` | `imageData` | Reduzida (≤ 720 px). A original vai no backup. |
| Marca ZapDeck | `Website` URL, rótulo `ZapDeck` | `urlAddresses` rótulo `ZapDeck` | Ver seção 4. |

### 3.1 Facebook

O cartão normalmente traz só o **nome da página**, não o usuário. Regra:

- Se o texto lido for um link ou usuário válido (`facebook.com/x`, `fb.com/x`), gravar `https://facebook.com/<x>`.
- Se for apenas o nome, gravar `https://www.facebook.com/search/top?q=<nome codificado>`. É uma URL válida que abre a busca pela página, e o nome é recuperado do parâmetro `q` ao restaurar.

## 4. Marca ZapDeck

Formato do valor do site rotulado `ZapDeck`:

```
zapdeck://c/<uuid>?v=1&t=<criado_em_ms>&wb=<0|1>&igf=<0|1>
```

| Parte | Significado |
|---|---|
| `<uuid>` | identificador estável do cartão (evita duplicata ao restaurar) |
| `v` | versão do formato |
| `t` | data de criação, em ms desde 1970 |
| `wb` | preferir WhatsApp Business |
| `igf` | já segue o perfil no Instagram |

- Esquema próprio (`zapdeck://`) em vez de `https://`, para não depender de domínio registrado.
- Teste de aceitação obrigatório: o valor sobrevive à sincronização Google/iCloud sem ser reescrito, em ambos os sistemas.
- O app reconhece como "contato do ZapDeck" todo contato que tenha esse site **ou** a nota legada.

## 5. Observações e o entitlement do iOS

- **Plano A (preferido):** solicitar `com.apple.developer.contacts.notes` à Apple, com a justificativa de que o app é um digitalizador de cartões de visita e a nota guarda serviços e anotações escritas à mão. Android e iOS passam a usar a nota do mesmo jeito.
- **Plano B (se negado ou enquanto não aprovado):**
  - iOS: resumo curto das observações (até ~120 caracteres) em `jobTitle`/`departmentName`, e o texto completo no backup (seção 6).
  - Android: segue usando a nota normalmente.
- O código deve isolar isso atrás de uma interface (`ObservationsStore`) com duas implementações, para trocar de plano sem refatorar.

## 6. Backup de imagens e dados extras

O que a agenda não guarda (verso, frente em alta resolução, observações completas no Plano B) fica num **arquivo de backup do ZapDeck**:

- Formato: `.zip` com `manifest.json` (lista de `<uuid>` e metadados) e as imagens `<uuid>-frente.jpg` e `<uuid>-verso.jpg`.
- Exportar: pelo menu do app, gravando em iCloud Drive / Google Drive / Arquivos (seletor de documentos do sistema).
- Importar: ao abrir o app em aparelho novo, oferecer "Restaurar imagens do backup". O vínculo com o contato é o `<uuid>` da marca ZapDeck.
- Entre Android e iOS a restauração das imagens é **manual** (exportar e importar o arquivo). A restauração dos textos é automática.
- Opcional, fora deste ADR: backup automático em iCloud Drive (iOS) e Drive `appDataFolder` (Android).

## 7. Contas e sincronização

- **Android:** continua gravando na conta Google, como hoje. Sem conta Google, avisar que o contato ficará só no aparelho.
- **iOS:** gravar no contêiner da conta iCloud. Se o iCloud Contacts estiver desligado ou não houver conta que sincronize, **avisar o usuário** antes de salvar e mostrar o caminho em Ajustes.
- O app nunca grava em conta de terceiros (WhatsApp, Telegram etc.).

## 8. Migração do formato atual

1. Ao abrir, listar contatos com marca nova **ou** nota legada.
2. Para cada contato legado, ler as chaves da seção 1 e reescrever nos campos da seção 3, gerando um `<uuid>` e a marca nova.
3. Manter a nota original intacta até a conversão terminar sem erro; só então remover as linhas convertidas (`Instagram:`, `Facebook:`, `Email:` etc.) da nota, preservando `Observations`.
4. `BackImageBase64`: extrair para o backup (`<uuid>-verso.jpg`) e remover a linha da nota, que hoje deixa o contato pesado.
5. Conversão idempotente: rodar duas vezes não duplica nem perde dado.

## 9. Restauração em aparelho novo

1. O usuário entra com a conta (Google/iCloud) e a agenda sincroniza.
2. O app abre, pede permissão de contatos e lista tudo que tiver a marca ZapDeck: nome, telefones, Instagram, Facebook, endereço, observações (se permitido) e foto reduzida.
3. O app oferece restaurar imagens do backup, se o usuário tiver o arquivo.
4. O cache local é reconstruído a partir da agenda.

## 10. Alternativas rejeitadas

| Alternativa | Por que não |
|---|---|
| Banco local como fonte da verdade, com sync para a agenda | A restauração dependeria do backup do app (iCloud Backup/Auto Backup) e não ocorreria entre Android e iOS. Contraria o requisito. |
| Backend próprio com conta de usuário | Custo, privacidade e login de usuário; foge do escopo e do posicionamento "offline". |
| Tudo na nota (como hoje) | Inviável no iOS sem o entitlement; inaceitável como único caminho. |
| Imagem em base64 na nota | Deixa o contato pesado, quebra limites de sincronização e não existe no iOS sem o entitlement. |
| Campos de data/IM do sistema para metadados | Pouco portáveis entre Google e iCloud e confusos na agenda do usuário. A URL com rótulo é mais estável. |

## 11. Consequências

- **Positivas:** restauração de textos e telefones automática nos dois sistemas; independe de servidor; contatos continuam úteis fora do app (WhatsApp, ligação, mapa).
- **Negativas:** verso e imagens em alta exigem o backup manual; observações no iOS dependem da Apple (Plano A) ou ficam resumidas (Plano B); leitura e escrita na agenda são mais lentas que num banco local, o que reforça o cache local.
- **Riscos:** a Apple negar o entitlement; sincronização Google/iCloud alterar o formato da URL de marca; usuários com contatos em conta que não sincroniza.

## 12. Critérios de aceite

1. Salvar um cartão no Android, sincronizar via Google para um iPhone de teste e ver nome, telefones, e-mail, endereço, Instagram e Facebook no app iOS.
2. Salvar um cartão no iPhone, restaurar em outro iPhone com o mesmo iCloud e ver os mesmos dados.
3. A marca ZapDeck e o `<uuid>` sobrevivem à sincronização sem alteração.
4. Contato do formato antigo é convertido sem perda de dado e sem duplicar; a conversão é idempotente.
5. Backup exportado e importado devolve frente e verso ao contato correto pelo `<uuid>`.
6. Sem conta que sincronize, o app avisa antes de salvar.

## 13. Pontos em aberto

- Quando solicitar o entitlement de notas à Apple (antes da primeira submissão à loja).
- Se o backup automático em nuvem entra na primeira versão ou depois.
- Decidir se o Facebook por busca (`search/top?q=`) é aceitável como formato, ou se o campo vira um site com texto livre.
- Limite de tamanho da foto do contato por sistema, a validar em aparelho real.
