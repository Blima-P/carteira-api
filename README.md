# Carteira API

[![CI](https://github.com/Blima-P/carteira-api/actions/workflows/ci.yml/badge.svg?branch=develop)](https://github.com/Blima-P/carteira-api/actions/workflows/ci.yml)

API REST de carteira digital: usuários podem **depositar**, **transferir saldo entre si** e **consultar o extrato**.

O foco do projeto é demonstrar requisitos essenciais de sistemas financeiros:

- **Transações ACID**: em uma transferência, o débito e o crédito acontecem na mesma transação de banco. Se algo falhar, tudo é desfeito (rollback).
- **Idempotência**: uma falha de rede que faça o cliente reenviar a requisição não processa a transferência duas vezes.
- **Segurança**: autenticação via JWT.

## Stack

- Java 21 + Spring Boot 4
- Spring Modulith (monólito modular) + arquitetura hexagonal
- PostgreSQL + Flyway
- Spring Security (JWT)
- Docker / Docker Compose
- JUnit 5 + testes de integração

## Roadmap

Histórico de versões no [CHANGELOG](CHANGELOG.md).

- [x] Dia 0 — Estrutura inicial do projeto
- [x] Dia 0.5 — Fluxo Git (develop/homolog/main), proteção de branches e CI
- [x] Dia 1 — Banco de dados (migrations) e estrutura modular
- [x] Dia 2 — Cadastro e login com JWT
- [x] Dia 3 — Consulta de saldo e depósito
- [x] Dia 4 — Transferência atômica (ACID + lock pessimista)
- [ ] Dia 5 — Idempotência (`Idempotency-Key`)
- [ ] Dia 6 — Extrato paginado
- [ ] Dia 7 — Testes de concorrência e rollback
- [ ] Dia 8 — Dockerfile + deploy simulado em homolog/produção (GitHub Actions + GHCR)
- [ ] Dia 9 — Documentação final + Swagger

## Arquitetura

**Monólito modular** ([Spring Modulith](https://spring.io/projects/spring-modulith)) com **arquitetura hexagonal** dentro de cada módulo. Decisão registrada em [ADR 0001](docs/adr/0001-monolito-modular-hexagonal.md).

```text
com.braga.carteiradigital
├── usuario        → cadastro e autenticação (JWT)
├── carteira       → saldo, lançamentos e extrato (único módulo que altera saldo)
├── transacao      → depósitos e transferências (ACID + idempotência)
└── compartilhado  → segurança, tratamento de erros (RFC 9457) e tipo base de erro de negócio
```

Os módulos se comunicam por **eventos** (ex.: `UsuarioCadastrado`, que cria a carteira na mesma transação do cadastro) ou pela **API pública** no pacote base de cada módulo (ex.: `CarteiraApi`, a única forma de alterar um saldo, e `UsuarioApi`, usada para encontrar o destinatário de uma transferência).

Dentro de cada módulo:

```text
<modulo>
├── dominio/                  regras de negócio em Java puro (sem Spring/JPA)
├── aplicacao/                casos de uso + portas (interfaces) de entrada e saída
└── adaptadores/
    ├── entrada/web/          controllers REST
    ├── entrada/eventos/      listeners de eventos de outros módulos
    └── saida/                persistência (JPA), segurança (BCrypt, JWT), outros módulos...
```

As fronteiras são verificadas **automaticamente** pelo `ArquiteturaTest`: o build falha se um módulo acessar o interior de outro, se houver ciclo entre módulos ou se o domínio depender de framework.

## Como rodar

Pré-requisito: Java 21.

### Sem Docker (modo dev)

Sobe a API com um PostgreSQL 18 embarcado na porta `5433`. Os dados ficam em `target/postgres-local`.

```bash
./mvnw spring-boot:test-run
```

- Health: <http://localhost:8080/actuator/health>
- Migrations aplicadas: <http://localhost:8080/actuator/flyway>

Encerre com `Ctrl+C`. Para zerar o banco, rode `./mvnw clean`.

### Com Docker

```bash
cp .env.example .env          # opcional: ajuste credenciais
docker compose up -d          # sobe o PostgreSQL 18
export JWT_SECRET="$(openssl rand -base64 48)"   # obrigatório fora do modo dev
./mvnw spring-boot:run        # o Flyway cria as tabelas na inicialização
```

## Endpoints

| Método | Rota                        | Autenticação                    | Descrição                                        |
|--------|-----------------------------|---------------------------------|--------------------------------------------------|
| POST   | `/api/auth/cadastro`        | —                               | Cadastra usuário e cria sua carteira (`201`)     |
| POST   | `/api/auth/login`           | —                               | Retorna um token JWT válido por 1h               |
| GET    | `/api/usuarios/eu`          | Bearer JWT                      | Dados do usuário dono do token                   |
| GET    | `/api/carteiras/minha`      | Bearer JWT                      | Saldo da carteira do dono do token               |
| POST   | `/api/transacoes/depositos` | Bearer JWT + `Idempotency-Key`  | Deposita na própria carteira (`201` + comprovante) |
| POST   | `/api/transacoes/transferencias` | Bearer JWT + `Idempotency-Key` | Transfere para outro usuário, pelo e-mail (`201` + comprovante) |

Exemplos prontos em [`http/autenticacao.http`](http/autenticacao.http) e [`http/carteira.http`](http/carteira.http) (extensão **REST Client** do VS Code). Com `curl`:

```bash
curl -X POST localhost:8080/api/auth/cadastro -H "Content-Type: application/json" \
  -d '{"nome":"Pedro","email":"pedro@email.com","senha":"senha-segura-123"}'

TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"pedro@email.com","senha":"senha-segura-123"}' | jq -r .token)

curl localhost:8080/api/usuarios/eu -H "Authorization: Bearer $TOKEN"

# Depósito: gere uma Idempotency-Key nova para cada operação (reenviar a mesma não deposita duas vezes)
curl -X POST localhost:8080/api/transacoes/depositos -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: $(uuidgen)" -H "Content-Type: application/json" \
  -d '{"valor": 150.50, "descricao": "Primeiro depósito"}'

# Transferência: o destinatário (já cadastrado) é identificado pelo e-mail
curl -X POST localhost:8080/api/transacoes/transferencias -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: $(uuidgen)" -H "Content-Type: application/json" \
  -d '{"emailDestinatario": "ana@email.com", "valor": 50.00, "descricao": "Almoço"}'

curl localhost:8080/api/carteiras/minha -H "Authorization: Bearer $TOKEN"
```

Erros seguem o padrão **Problem Details (RFC 9457)**, sempre com um `codigo` estável:

```json
{ "status": 409, "codigo": "email-ja-cadastrado", "detail": "Já existe um usuário com este e-mail." }
```

Decisões de segurança em [ADR 0002](docs/adr/0002-autenticacao-jwt.md). Decisões sobre dinheiro, livro-razão e concorrência em [ADR 0003](docs/adr/0003-movimentacao-de-saldo.md). Decisões da transferência em [ADR 0004](docs/adr/0004-transferencia-entre-carteiras.md).

## Modelo de dados

```text
usuarios 1──1 carteiras 1──* lancamentos *──1 transacoes
```

| Tabela        | Papel                                                                   |
|---------------|-------------------------------------------------------------------------|
| `usuarios`    | Usuários (e-mail único, senha com hash)                                 |
| `carteiras`   | Uma carteira por usuário; saldo em `NUMERIC(19,2)` e nunca negativo     |
| `transacoes`  | Depósitos e transferências; `(iniciada_por, chave_idempotencia)` é único |
| `lancamentos` | Livro-razão **somente-inserção**: cada crédito/débito gera um lançamento |

As regras críticas (saldo ≥ 0, valor > 0, idempotência, lançamentos imutáveis) são garantidas **pelo próprio PostgreSQL** via constraints e trigger, não apenas pela aplicação.

## Testes

Os testes de integração usam um PostgreSQL real embarcado (não precisam de Docker):

```bash
./mvnw verify
```

O build também gera diagramas dos módulos em `target/spring-modulith-docs`.

Destaques:

- `AtomicidadeIntegracaoTest` prova o rollback entre módulos: uma falha no meio do depósito ou da transferência (ex.: no crédito do destinatário) desfaz tudo, inclusive o débito.
- `DepositoIntegracaoTest` dispara depósitos simultâneos na mesma carteira para provar que nenhum se perde.
- `TransferenciaIntegracaoTest` dispara 15 transferências de R$ 10 ao mesmo tempo com saldo de R$ 100: exatamente 5 são recusadas e o saldo nunca fica negativo. Também dispara transferências cruzadas (A→B e B→A) simultâneas para provar que não há *deadlock*.

## Fluxo de desenvolvimento

`feat/*` → `develop` → `homolog` → `main`, com Pull Requests, CI obrigatório, Conventional Commits e versionamento SemVer via tags.
Detalhes em [CONTRIBUTING.md](CONTRIBUTING.md).
