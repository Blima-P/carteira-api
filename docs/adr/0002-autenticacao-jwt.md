# ADR 0002: Autenticação stateless com JWT

- **Status:** aceita
- **Data:** 2026-10-05

## Contexto

A API precisa identificar o usuário em cada operação financeira. Há duas abordagens comuns:

- **Sessão no servidor (cookie):** o servidor guarda quem está logado. Exige armazenamento compartilhado quando há mais de uma instância e precisa de proteção CSRF.
- **Token (JWT):** o próprio token carrega a identidade, assinado pelo servidor. Qualquer instância valida sem consultar o banco.

## Decisão

1. **JWT assinado com HS256** (HMAC-SHA256), emitido pela própria API no login. O segredo vem da variável de ambiente `JWT_SECRET`. A aplicação **não sobe** se ele estiver ausente ou tiver menos de 32 caracteres.

2. **Validação pelo Spring Security Resource Server** (`oauth2ResourceServer().jwt()`), em vez de um filtro escrito à mão. São verificados:
   - assinatura, com o algoritmo fixado em HS256 (recusa `"alg": "none"`);
   - expiração (`exp`), com tolerância de 60 segundos;
   - emissor (`iss = carteira-api`).

3. **Claims mínimas:** `sub` (id do usuário), `iss`, `iat`, `exp` e `email`. O payload de um JWT é apenas Base64 e qualquer pessoa consegue lê-lo, por isso nunca leva dados sensíveis.

4. **O id do usuário sempre vem do token** (`sub`), nunca do corpo ou da URL. Assim um usuário não consegue agir em nome de outro.

5. **Senhas com o `DelegatingPasswordEncoder`** (BCrypt hoje). O hash é salvo com o prefixo do algoritmo (`{bcrypt}...`), o que permite migrar de algoritmo no futuro sem invalidar as senhas existentes.

6. **Login sem vazamento de informação:**
   - E-mail inexistente e senha errada geram a mesma resposta (`401 credenciais-invalidas`).
   - Mesmo quando o e-mail não existe, o servidor compara a senha com um hash fictício, para que o tempo de resposta não revele quais e-mails estão cadastrados (*timing attack*).

## Consequências

- ✅ Stateless: escala horizontalmente sem sessão compartilhada e dispensa CSRF.
- ✅ Usa componentes padrão e auditados do Spring Security.
- ⚠️ Um token emitido não pode ser revogado antes de expirar. Mitigação: validade curta (1h). *Refresh token* e lista de revogação ficam para o futuro.
- ⚠️ Com HS256, quem valida também consegue emitir tokens (a chave é a mesma). Se outros serviços precisarem validar tokens, a migração natural é para RS256/ES256 com chave pública (JWKS).
