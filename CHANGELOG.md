# Changelog

Todas as mudanças relevantes do projeto são registradas aqui.

O formato segue o [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e o projeto usa [Versionamento Semântico](https://semver.org/lang/pt-BR/).

## [Não lançado]

### Adicionado

- `POST /api/transacoes/transferencias`: transferência para outro usuário, identificado pelo e-mail, com `Idempotency-Key` obrigatória. O débito, o crédito, a transação e os dois lançamentos são gravados **na mesma transação de banco**.
- Bloqueio das duas carteiras **sempre na mesma ordem**, para evitar *deadlock* em transferências cruzadas simultâneas.
- Novos erros `422`: `saldo-insuficiente`, `destinatario-nao-encontrado` e `transferencia-para-si-mesmo`.
- `UsuarioApi`: API pública do módulo Usuário para buscar um usuário pelo e-mail.
- Testes de concorrência: transferências simultâneas não gastam mais que o saldo e transferências cruzadas não causam *deadlock*.

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
