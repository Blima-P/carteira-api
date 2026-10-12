package com.braga.carteiradigital.transacao.aplicacao;

import java.util.Optional;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;
import com.braga.carteiradigital.transacao.aplicacao.porta.saida.MovimentadorDeCarteira;
import com.braga.carteiradigital.transacao.aplicacao.porta.saida.TransacaoRepository;
import com.braga.carteiradigital.transacao.dominio.ChaveIdempotenciaReutilizadaException;
import com.braga.carteiradigital.transacao.dominio.Comprovante;
import com.braga.carteiradigital.transacao.dominio.Transacao;

/**
 * Regra de idempotência comum a todas as operações financeiras. Roda dentro da transação de quem chama.
 *
 * <ul>
 * <li>Chave nova: grava a transação, movimenta os saldos e devolve o comprovante.</li>
 * <li>Chave já usada no mesmo pedido (reenvio após falha de rede): não movimenta nada e devolve o
 * comprovante original.</li>
 * <li>Chave já usada em um pedido diferente: {@code chave-idempotencia-reutilizada}.</li>
 * </ul>
 */
@Component
class ExecutorIdempotente {

    private final TransacaoRepository transacoes;
    private final MovimentadorDeCarteira carteiras;

    ExecutorIdempotente(TransacaoRepository transacoes, MovimentadorDeCarteira carteiras) {
        this.transacoes = transacoes;
        this.carteiras = carteiras;
    }

    /**
     * @param movimentar altera os saldos da transação gravada e devolve o saldo de quem a iniciou
     */
    Comprovante executar(Transacao pedido, Function<Transacao, Dinheiro> movimentar) {
        Optional<Transacao> original = transacoes.buscarPorChave(pedido.iniciadaPor(), pedido.chaveIdempotencia());
        if (original.isPresent()) {
            return reenviar(original.get(), pedido);
        }

        // A transação precisa ser gravada antes dos lançamentos, que a referenciam por chave estrangeira.
        // Se uma requisição simultânea com a mesma chave gravar primeiro, a constraint única barra esta.
        transacoes.salvar(pedido);
        return Comprovante.de(pedido, movimentar.apply(pedido));
    }

    private Comprovante reenviar(Transacao original, Transacao pedido) {
        if (!original.ehMesmoPedidoQue(pedido)) {
            throw new ChaveIdempotenciaReutilizadaException();
        }
        // O saldo de agora pode ser outro; o comprovante original mostra o saldo logo após a operação
        Dinheiro saldoNaEpoca = carteiras.saldoAposTransacao(original.carteiraDeQuemIniciou(), original.id());
        return Comprovante.reenvioDe(original, saldoNaEpoca);
    }
}
