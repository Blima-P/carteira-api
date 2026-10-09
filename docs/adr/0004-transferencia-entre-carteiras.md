# ADR 0004: Transferência entre carteiras

- **Status:** aceita
- **Data:** 2026-10-09

## Contexto

A transferência é a operação mais sensível do sistema: tira dinheiro de uma carteira e coloca em outra. Ela precisa garantir:

- que o débito e o crédito aconteçam juntos ou não aconteçam (atomicidade);
- que transferências simultâneas não gastem mais do que o saldo disponível;
- que transferências cruzadas simultâneas (A→B e B→A) não travem o banco (*deadlock*);
- que o módulo Transação não acesse o interior dos módulos Usuário e Carteira.

As decisões de [ADR 0003](0003-movimentacao-de-saldo.md) (dinheiro, livro-razão, bloqueio pessimista) continuam valendo.

## Decisões

1. **Uma única transação de banco.** O `TransferirService` grava a transação e chama `CarteiraApi.transferir`, que debita a origem, credita o destino e registra os dois lançamentos. `transferir` usa `Propagation.MANDATORY`: só roda dentro da transação de quem chama. Qualquer erro desfaz tudo. O `AtomicidadeIntegracaoTest` força uma falha no crédito do destinatário e prova que o débito também é desfeito.

2. **Bloqueio das duas carteiras sempre na mesma ordem.** Sem uma ordem, A→B bloqueia A e espera B, enquanto B→A bloqueia B e espera A: o PostgreSQL detecta o *deadlock* e aborta uma delas. As carteiras são bloqueadas da menor para a maior pelo id (`UUID.compareTo`), qualquer que seja o sentido da transferência. O teste de transferências cruzadas simultâneas falha se essa ordenação for removida.

3. **Saldo verificado com a carteira bloqueada.** `Carteira.debitar` lança `SaldoInsuficienteException` (`422 saldo-insuficiente`) se o saldo for menor que o valor. Como a verificação acontece depois do `SELECT ... FOR UPDATE`, duas transferências simultâneas não leem o mesmo saldo. A constraint `saldo >= 0` no banco é a última linha de defesa.

4. **Destinatário identificado pelo e-mail.** O cliente conhece o e-mail de quem recebe, não ids internos. O módulo Transação usa a nova API pública `UsuarioApi` por meio de uma porta própria (`BuscadorDeUsuarios`). As dependências ficam Transação → Usuário e Transação → Carteira, sem ciclos (verificado pelo `ArquiteturaTest`).

5. **Regras de negócio com `422`.** Destinatário inexistente (`destinatario-nao-encontrado`), transferência para si mesmo (`transferencia-para-si-mesmo`) e saldo insuficiente (`saldo-insuficiente`). Dados malformados continuam com `400 dados-invalidos`.

6. **Transferência recusada não consome a `Idempotency-Key`.** Como a recusa desfaz a transação inteira, a chave não fica gravada. O cliente pode reenviar a mesma chave depois de depositar. O tratamento do reenvio (devolver a resposta original) fica para a etapa de idempotência.

7. **O comprovante mostra só o saldo da origem.** O saldo do destinatário não é exposto a quem envia.

## Consequências

- ✅ Débito e crédito são indivisíveis, comprovado por testes com falha forçada.
- ✅ Testes de concorrência provam que não há saldo negativo nem *deadlock*.
- ✅ Os módulos continuam desacoplados, comunicando-se apenas por APIs públicas.
- ⚠️ Toda operação futura que bloquear mais de uma carteira precisa seguir a mesma ordem.
- ⚠️ Buscar o destinatário pelo e-mail permite descobrir se um e-mail está cadastrado (o erro `destinatario-nao-encontrado` revela isso). Em produção, seria preciso limitar as tentativas (*rate limiting*) ou usar uma chave de transferência (como a do Pix).
