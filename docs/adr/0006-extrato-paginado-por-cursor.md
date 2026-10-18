# ADR 0006: Extrato paginado por cursor

- **Status:** aceita
- **Data:** 2026-10-17

## Contexto

O usuário precisa ver o histórico da carteira: depósitos, transferências enviadas e recebidas, com o saldo após cada uma. O histórico só cresce, então a consulta precisa ser paginada.

Os dados estão em dois módulos. O módulo Carteira tem o livro-razão (`lancamentos`: valor, natureza e saldo após). O módulo Transação tem o tipo, a descrição e as carteiras envolvidas (`transacoes`). O nome da outra pessoa está no módulo Usuário.

## Decisões

1. **Paginação por cursor (keyset), não por número de página.** Com `?pagina=2`, cada lançamento novo empurra os itens para as páginas seguintes. Quem estiver navegando vê um item repetido, ou perde um, se algo for apagado. O cursor diz "continue depois deste lançamento", então lançamentos novos não deslocam as páginas seguintes. A consulta também fica rápida em qualquer página: o banco não precisa ler e descartar as páginas anteriores, como faz com `OFFSET`.

2. **Ordem `criado_em DESC, id DESC`.** O `id` do lançamento desempata itens do mesmo instante, então a ordem é sempre total e estável. É a ordem do índice `idx_lancamentos_carteira_criado_em (carteira_id, criado_em DESC, id DESC)`, criado já na V1 para esta consulta. A condição da página seguinte é `criado_em < :c OR (criado_em = :c AND id < :id)`.

3. **O cursor é opaco, mas não secreto.** É o base64url de `"<instante>|<id do lançamento>"`. O cliente só devolve o valor de `proximoCursor`, sem interpretar. O cursor não é assinado, porque a consulta sempre filtra pela carteira do dono do token: um cursor forjado só muda o ponto de partida dentro do próprio extrato. Um cursor malformado vira `400 dados-invalidos`, com `campos.cursor`.

4. **Próxima página sem `COUNT`.** A API busca `tamanho + 1` lançamentos. Se vier o item extra, existe próxima página e o `proximoCursor` aponta para o último item exibido. Na última página, `proximoCursor` é `null`. O `tamanho` vai de 1 a 100 (padrão 20).

5. **O módulo Transação monta o extrato.** Ele já depende de Carteira e de Usuário. Montar no módulo Carteira criaria um ciclo (Carteira → Transação → Carteira), que o `ArquiteturaTest` recusa. A `CarteiraApi` passou a expor a página do livro-razão (`lancamentos`) e os donos das carteiras (`donosDasCarteiras`). A `UsuarioApi` passou a expor `nomesPorId`. Cada página faz um número fixo de consultas em lote, sem N+1.

6. **A contraparte mostra só o nome.** Para quem recebeu uma transferência, aparece o nome de quem enviou, e vice-versa. O e-mail do outro usuário não é exposto. Em depósitos, `contraparte` é `null`.

7. **`realizadaEm` é o momento do lançamento.** É quando o saldo mudou, então a ordem do extrato e os valores de `saldoApos` são sempre coerentes entre si. Pode diferir em milissegundos do `realizadaEm` do comprovante, que registra quando o pedido foi aceito, antes de esperar o bloqueio da carteira.

## Consequências

- ✅ Páginas estáveis com lançamentos chegando ao mesmo tempo, coberto por teste: um depósito entre a página 1 e a 2 não repete nem pula itens.
- ✅ Cada usuário vê o próprio lado da transferência (`DEBITO` para quem enviou, `CREDITO` para quem recebeu), com o saldo da própria carteira.
- ✅ Um reenvio idempotente não aparece duas vezes, porque não gera lançamento.
- ⚠️ Não é possível pular direto para uma página qualquer (ex.: página 7), só avançar. Para um extrato isso basta; um filtro por período pode ser adicionado depois sem mudar o formato do cursor.
- ⚠️ Mudanças no formato do cursor invalidam cursores antigos. Como eles são temporários (o cliente começa de novo pela primeira página), não há compatibilidade a manter.
