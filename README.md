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

- [x] Dia 0 — Estrutura inicial do projeto
- [x] Dia 0.5 — Fluxo Git (develop/homolog/main), proteção de branches e CI
- [x] Dia 1 — Banco de dados (migrations) e estrutura modular
- [ ] Dia 2 — Cadastro e login com JWT
- [ ] Dia 3 — Consulta de saldo e depósito
- [ ] Dia 4 — Transferência atômica (ACID + lock pessimista)
- [ ] Dia 5 — Idempotência (`Idempotency-Key`)
- [ ] Dia 6 — Extrato paginado
- [ ] Dia 7 — Testes de concorrência e rollback
- [ ] Dia 8 — Dockerfile + deploy simulado em homolog/produção (GitHub Actions + GHCR)
- [ ] Dia 9 — Documentação final + Swagger

## Arquitetura

**Monólito modular** ([Spring Modulith](https://spring.io/projects/spring-modulith)) com **arquitetura hexagonal** dentro de cada módulo. Decisão registrada em [ADR 0001](docs/adr/0001-monolito-modular-hexagonal.md).

```text
com.braga.carteiradigital
├── usuario      → cadastro e autenticação
├── carteira     → saldo, lançamentos e extrato (único módulo que altera saldo)
└── transacao    → depósitos e transferências (ACID + idempotência)
```

Dentro de cada módulo:

```text
<modulo>
├── dominio/                  regras de negócio em Java puro (sem Spring/JPA)
├── aplicacao/                casos de uso + portas (interfaces) de entrada e saída
└── adaptadores/
    ├── entrada/web/          controllers REST
    └── saida/persistencia/   entidades JPA e repositórios
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
./mvnw spring-boot:run        # o Flyway cria as tabelas na inicialização
```

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

## Fluxo de desenvolvimento

`feat/*` → `develop` → `homolog` → `main`, com Pull Requests, CI obrigatório, Conventional Commits e versionamento SemVer via tags.
Detalhes em [CONTRIBUTING.md](CONTRIBUTING.md).
