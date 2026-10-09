# ADR 0003: Movimentação de saldo (dinheiro, livro-razão e concorrência)

- **Status:** aceita
- **Data:** 2026-10-06

## Contexto

O depósito é a primeira operação que altera um saldo. As decisões tomadas aqui valem também para transferências e precisam garantir:

- valores exatos, sem erro de arredondamento;
- que todo saldo possa ser auditado;
- que duas operações simultâneas na mesma carteira não percam dinheiro;
- que uma operação nunca fique pela metade.

## Decisão

1. **Dinheiro é `BigDecimal` com 2 casas** (`Dinheiro`, no módulo compartilhado) e `NUMERIC(19,2)` no banco. Frações de centavo são **recusadas**, não arredondadas. Nunca se usa `double`: nele, `0.1 + 0.2 = 0.30000000000000004`.

2. **Livro-razão:** cada alteração de saldo grava um `lancamento` imutável (crédito/débito, valor e saldo após a operação). O saldo da carteira é um valor derivado: sempre pode ser conferido somando os lançamentos.

3. **Uma transação de banco por operação.** O registro em `transacoes`, a atualização de `carteiras` e o `lancamento` são gravados juntos. Se qualquer passo falhar, o rollback desfaz todos.

4. **A carteira nasce junto com o usuário.** O módulo Carteira escuta `UsuarioCadastrado` com `@EventListener` síncrono, dentro da transação do cadastro. Se a carteira falhar, o cadastro também é desfeito. Foi preferido a um `@ApplicationModuleListener` assíncrono, que deixaria uma janela com usuário sem carteira.

5. **Bloqueio pessimista** (`SELECT ... FOR UPDATE`) antes de alterar o saldo. Em dinheiro, o conflito é esperado (ex.: vários pagamentos simultâneos para a mesma conta). Com o bloqueio, as operações esperam a vez em vez de falhar e exigir nova tentativa. O `@Version` continua como rede de segurança: uma atualização sem bloqueio falha em vez de sobrescrever um valor.

6. **Só o módulo Carteira altera saldos.** O módulo Transação usa a API pública `CarteiraApi`, por meio de uma porta própria (`MovimentadorDeCarteira`). `creditar` usa `Propagation.MANDATORY`: falha se não houver uma transação aberta por quem chama, o que impede um crédito solto, sem a operação que o originou.

7. **`Idempotency-Key` obrigatória desde o primeiro endpoint financeiro.** A chave é única por usuário (constraint no banco). Por enquanto, uma chave repetida retorna `409`. O reenvio que devolve a resposta original fica para a etapa de idempotência.

## Consequências

- ✅ Saldo exato, auditável e protegido por constraints no próprio banco.
- ✅ Testes provam o rollback entre módulos e a ausência de perda em depósitos simultâneos.
- ⚠️ O bloqueio serializa as operações na mesma carteira. Para uma carteira com volume altíssimo (ex.: a de um grande lojista), seria preciso outra estratégia, como lançamentos sem saldo consolidado. Não é o caso deste projeto.
- ⚠️ Transferências bloqueiam duas carteiras. Para evitar *deadlock*, os bloqueios devem ser obtidos sempre na mesma ordem (a ser feito na transferência).
