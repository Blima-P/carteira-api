# ADR 0005: Idempotência com reenvio da resposta original

- **Status:** aceita
- **Data:** 2026-10-12

## Contexto

Em uma API financeira, o cliente nunca sabe com certeza se uma requisição sem resposta foi processada. A conexão pode cair antes do envio (nada aconteceu) ou depois do processamento (o dinheiro já saiu). Se o cliente reenvia e a API processa de novo, o usuário paga duas vezes.

Desde o [ADR 0003](0003-movimentacao-de-saldo.md), toda operação exige uma `Idempotency-Key`, única por usuário no banco. Até aqui, uma chave repetida retornava `409`. Isso impedia a duplicidade, mas deixava o cliente sem saber o resultado da primeira tentativa.

A referência é o rascunho do IETF [*The Idempotency-Key HTTP Header Field*](https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/), seguido por APIs como a da Stripe.

## Decisões

1. **Mesma chave e mesmo pedido: devolve a resposta original.** Nada é movimentado de novo. A resposta tem o mesmo status (`201`) e o mesmo corpo da primeira vez, mais o cabeçalho `Idempotent-Replayed: true`. Para o cliente, reenviar é sempre seguro.

2. **"Mesmo pedido" é decidido pelo hash.** Cada transação guarda o SHA-256 do pedido (`hash_requisicao`): tipo, destino, valor e descrição normalizados. Mudar qualquer um deles muda o hash. Assim, `50.0` e `50.00` ou uma descrição com espaços extras continuam sendo o mesmo pedido.

3. **Mesma chave e pedido diferente: `422 chave-idempotencia-reutilizada`.** Provavelmente é um bug no cliente, que reaproveitou a chave. Devolver o comprovante antigo esconderia o erro, e processar criaria uma segunda operação com a mesma chave. Vale também entre tipos: a chave de um depósito não serve para uma transferência.

4. **O saldo do comprovante reenviado vem do livro-razão.** O comprovante original mostra o saldo logo após a operação. No reenvio, esse valor é lido do `saldo_apos` do lançamento da transação, na carteira de quem a iniciou (`CarteiraApi.saldoAposTransacao`). Assim a resposta é idêntica, mesmo que o saldo tenha mudado depois, e não foi preciso guardar a resposta inteira numa nova coluna.

5. **Requisições simultâneas com a mesma chave: `409 chave-idempotencia-em-uso`.** As duas passam pela busca antes de qualquer gravação. A constraint única `uk_transacoes_idempotencia` deixa só uma gravar; a outra espera o commit da primeira e então falha. Como o PostgreSQL aborta a transação que falhou, não dá para consultar o resultado dentro dela. O cliente recebe `409` e, ao reenviar, cai no caso 1. O rascunho do IETF recomenda exatamente o `409` para esse caso.

6. **Pedidos recusados não consomem a chave.** Uma recusa (ex.: saldo insuficiente) desfaz a transação inteira, então nada fica gravado com a chave. O cliente pode corrigir o problema (ex.: depositar) e reenviar a mesma chave. As validações do pedido (ex.: destinatário existente) rodam antes da verificação da chave. A de saldo não: um reenvio não movimenta nada, então devolve o comprovante mesmo que o saldo atual não seja mais suficiente.

7. **A regra fica em um só lugar.** O `ExecutorIdempotente` (camada de aplicação do módulo Transação) é usado pelo depósito e pela transferência e roda dentro da transação do caso de uso: busca pela chave, compara o pedido, grava e movimenta, ou refaz o comprovante.

## Consequências

- ✅ O cliente pode reenviar qualquer operação financeira sem risco de duplicidade e sempre recebe o resultado real.
- ✅ Testes cobrem o reenvio (corpo idêntico e o cabeçalho), a chave reutilizada com outros dados e 8 requisições simultâneas com a mesma chave (um único depósito).
- ⚠️ As chaves não expiram. Em produção, seria comum descartá-las após um prazo (ex.: 24 horas), mas aqui elas fazem parte do registro da transação e são mantidas para auditoria.
- ⚠️ No caso simultâneo, o cliente precisa reenviar para receber o comprovante. Um *retry* automático no servidor evitaria isso, ao custo de mais complexidade.
