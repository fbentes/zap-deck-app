# Política de Segurança — ZapDeck

A equipe do **ZapDeck** adota práticas de segurança rigorosas para garantir a proteção do código-fonte, dados locais dos usuários e integridade da aplicação.

---

## 1. Gestão de Segredos e Chaves de Acesso

- **Nenhum Segredo no Código-Fonte**: O repositório não contém senhas de assinatura, chaves de criptografia, keystores de produção ou chaves de API com valores reais.
- **Injeção Dinâmica via Gradle Secrets**: Qualquer chave externa necessária (por exemplo, credencial da API Gemini para uso opcional de nuvem) é carregada via arquivo `.env` protegido e ignorado pelo controle de versão (`.gitignore`).
- **Assinatura de Release**: As credenciais para geração do APK/AAB de produção são configuradas via variáveis de ambiente seguras no ambiente de integração contínua / build local, nunca sendo expostas no repositório público.

---

## 2. Segurança no Armazenamento Local

- **Isolamento de Sandbox do Android**: A base de dados SQLite (Room) e as imagens de cartões são salvas no armazenamento privado e protegido do pacote do aplicativo (`context.filesDir` e diretório de banco de dados interno).
- **Trânsito Criptografado (HTTPS)**: Todas as conexões externas de rede (quando ativadas pelo usuário) utilizam estritamente TLS/HTTPS.
- **Minimização de Permissões**: O aplicativo solicita apenas permissões estritamente pertinentes às funcionalidades operacionais oferecidas.

---

## 3. Reporte Responsável de Vulnerabilidades

Caso identifique qualquer potencial vulnerabilidade de segurança no ZapDeck, solicitamos que não abra uma issue pública. Entre em contato diretamente com o responsável técnico:

- **Responsável**: Francisco A. C. Bentes
- **E-mail**: `facbentes@gmail.com`
- **Prazo de Resposta**: Em até 48 horas úteis realizaremos o diagnóstico e providenciaremos a correção necessária.
