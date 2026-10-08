# ADR 0001 — Monólito modular com arquitetura hexagonal

- **Status:** aceita
- **Data:** 2026-10-04

## Contexto

A carteira digital tem três responsabilidades bem distintas: **usuários** (cadastro e login), **carteiras** (saldo e extrato) e **transações** (depósitos e transferências).

Microsserviços seriam um exagero para o tamanho do projeto. Pior: uma transferência precisa debitar e creditar **na mesma transação de banco** (ACID), o que fica muito mais difícil com bancos separados (exigiria Saga e compensação).

Por outro lado, um monólito sem fronteiras tende a virar um emaranhado em que qualquer classe acessa qualquer outra.

## Decisão

1. **Monólito modular com Spring Modulith.** Cada pacote direto abaixo de `com.braga.carteiradigital` é um módulo: `usuario`, `carteira` e `transacao`.
   - Somente as classes no **pacote base** do módulo formam sua API pública. Subpacotes são internos.
   - Os módulos conversam por essa API pública ou por **eventos de domínio**.
   - Ciclos entre módulos são proibidos.

2. **Arquitetura hexagonal (portas e adaptadores) dentro de cada módulo:**
   - `dominio/`: regras de negócio em Java puro, sem Spring nem JPA.
   - `aplicacao/`: casos de uso e portas (`porta/entrada`, `porta/saida`).
   - `adaptadores/entrada/web`: controllers REST.
   - `adaptadores/saida/persistencia`: entidades JPA, repositórios e mapeadores.

3. **Regras verificadas por teste** (`ArquiteturaTest`): `ApplicationModules.verify()` e regras ArchUnit. Violações quebram o CI.

4. **Banco único e compartilhado**, com chaves estrangeiras entre tabelas de módulos diferentes. Em um sistema financeiro, a integridade referencial vale mais que o isolamento total dos dados. Cada tabela tem um único módulo dono, e só ele escreve nela.

5. **Nomes do domínio em português** (`Carteira`, `Transferencia`, `lancamentos`). Os sufixos técnicos ficam em inglês (`Controller`, `Service`, `Repository`), seguindo a convenção do ecossistema Spring.

## Consequências

- ✅ Transferências continuam ACID em uma única transação local.
- ✅ Fronteiras explícitas e testadas. Um módulo pode virar microsserviço no futuro sem reescrever o domínio.
- ✅ O domínio pode ser testado sem subir o Spring.
- ⚠️ Há mais classes e mapeamentos (domínio ↔ entidade JPA) do que em um CRUD em camadas simples.
- ⚠️ O banco compartilhado acopla os módulos no nível do schema. Mitigação: cada tabela tem um único módulo dono.
