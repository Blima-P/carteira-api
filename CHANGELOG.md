# Changelog

Todas as mudanças relevantes do projeto são registradas aqui.

O formato segue o [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e o projeto usa [Versionamento Semântico](https://semver.org/lang/pt-BR/).

## [Não lançado]

### Adicionado

- Documentação OpenAPI e Swagger UI no modo local, com autenticação JWT e endpoints públicos de cadastro/login corretamente identificados. Fora do modo local, exige `API_DOCS_ENABLED=true`.
- Relatórios HTML e XML de cobertura gerados pelo JaCoCo na fase `verify`.
- Testes de integração da documentação habilitada e desabilitada, preservando a proteção das rotas de negócio.
- `POST /api/transacoes/transferencias`: transferência para outro usuário, identificado pelo e-mail, com `Idempotency-Key` obrigatória. O débito, o crédito, a transação e os dois lançamentos são gravados **na mesma transação de banco**.
- Bloqueio das duas carteiras **sempre na mesma ordem**, para evitar *deadlock* em transferências cruzadas simultâneas.
- Novos erros `422`: `saldo-insuficiente`, `destinatario-nao-encontrado` e `transferencia-para-si-mesmo`.
- `UsuarioApi`: API pública do módulo Usuário para buscar um usuário pelo e-mail.
- Testes de concorrência: transferências simultâneas não gastam mais que o saldo e transferências cruzadas não causam *deadlock*.
- **Reenvio idempotente**: reenviar uma operação com a mesma `Idempotency-Key` e os mesmos dados devolve o comprovante original (`201`, mesmo corpo, cabeçalho `Idempotent-Replayed: true`), sem movimentar saldo de novo.
- Novo erro `422 chave-idempotencia-reutilizada`: a mesma chave enviada com dados diferentes.
- `GET /api/transacoes/extrato`: extrato do mais recente para o mais antigo, com tipo, natureza (`CREDITO`/`DEBITO`), valor, saldo após cada operação, descrição e o nome da contraparte nas transferências.
- **Paginação por cursor** (`tamanho` de 1 a 100, padrão 20, e `proximoCursor`): lançamentos novos não deslocam as páginas seguintes. Parâmetros inválidos retornam `400 dados-invalidos`.
- `CarteiraApi` expõe a página do livro-razão e os donos das carteiras; `UsuarioApi` expõe os nomes por id.

### Alterado

- Uma chave repetida não retorna mais `409 chave-idempotencia-ja-utilizada`. O `409` agora é `chave-idempotencia-em-uso` e só ocorre quando duas requisições com a mesma chave chegam ao mesmo tempo; basta reenviar.

## [0.1.0] - 2026-10-08

Primeira versão: cadastro, autenticação, saldo e depósito.

### Adicionado

- Cadastro de usuários e login com **JWT** (HS256, validade de 1h). Senhas guardadas com BCrypt.
- `GET /api/usuarios/eu`: dados do usuário autenticado.
- Carteira criada automaticamente, **na mesma transação** do cadastro.
- `GET /api/carteiras/minha`: consulta de saldo.
- `POST /api/transacoes/depositos`: depósito com cabeçalho **`Idempotency-Key`** obrigatório. Uma chave repetida não gera um segundo depósito.
- Livro-razão imutável: cada alteração de saldo gera um lançamento com o saldo após a operação.
- Bloqueio pessimista (`SELECT ... FOR UPDATE`) e `@Version` contra perda de atualização em operações simultâneas.
- Erros no padrão **Problem Details (RFC 9457)**, com `codigo` estável.
- Banco PostgreSQL versionado com Flyway.
- Arquitetura modular (Spring Modulith) com portas e adaptadores em cada módulo.
- CI no GitHub Actions (build, testes e validação do título do PR), Dependabot e fluxo `develop` → `homolog` → `main` com branches protegidas.
- ADRs 0001 a 0003 com as decisões de arquitetura.

[Não lançado]: https://github.com/Blima-P/carteira-api/compare/v0.1.0...develop
[0.1.0]: https://github.com/Blima-P/carteira-api/releases/tag/v0.1.0
