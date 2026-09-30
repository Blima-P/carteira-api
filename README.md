# Carteira API

API REST de carteira digital: usuários podem **depositar**, **transferir saldo entre si** e **consultar o extrato**.

O foco do projeto é demonstrar requisitos essenciais de sistemas financeiros:

- **Transações ACID**: em uma transferência, o débito e o crédito acontecem na mesma transação de banco. Se algo falhar, tudo é desfeito (rollback).
- **Idempotência**: uma falha de rede que faça o cliente reenviar a requisição não processa a transferência duas vezes.
- **Segurança**: autenticação via JWT.

## Stack

- Java 21 + Spring Boot 4
- PostgreSQL + Flyway
- Spring Security (JWT)
- Docker / Docker Compose
- JUnit 5 + testes de integração

## Roadmap

- [x] Dia 0 — Estrutura inicial do projeto
- [ ] Dia 1 — Banco de dados (PostgreSQL via Docker + migrations)
- [ ] Dia 2 — Cadastro e login com JWT
- [ ] Dia 3 — Consulta de saldo e depósito
- [ ] Dia 4 — Transferência atômica (ACID + lock pessimista)
- [ ] Dia 5 — Idempotência (`Idempotency-Key`)
- [ ] Dia 6 — Extrato paginado
- [ ] Dia 7 — Testes de concorrência e rollback
- [ ] Dia 8 — Dockerfile + CI (GitHub Actions)
- [ ] Dia 9 — Documentação final + Swagger

## Como rodar

Pré-requisito: Java 21.

```bash
./mvnw spring-boot:run
```

Health check: <http://localhost:8080/actuator/health>

## Testes

```bash
./mvnw verify
```
