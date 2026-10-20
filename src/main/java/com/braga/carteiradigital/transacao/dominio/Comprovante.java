package com.braga.carteiradigital.transacao.dominio;

import java.time.Instant;
import java.util.UUID;

import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;

/**
 * O que o cliente recebe após uma operação concluída.
 *
 * <p>{@code reenvio} indica que a operação já tinha sido feita antes, com a mesma chave de
 * idempotência. Nesse caso o comprovante é o original, inclusive o saldo logo após a operação.
 */
public record Comprovante(UUID transacaoId, TipoTransacao tipo, Dinheiro valor, String descricao,
        Dinheiro saldoAtual, Instant realizadaEm, boolean reenvio) {

    public static Comprovante de(Transacao transacao, Dinheiro saldoAtual) {
        return criar(transacao, saldoAtual, false);
    }

    public static Comprovante reenvioDe(Transacao original, Dinheiro saldoAposAOperacao) {
        return criar(original, saldoAposAOperacao, true);
    }

    private static Comprovante criar(Transacao transacao, Dinheiro saldo, boolean reenvio) {
        return new Comprovante(transacao.id(), transacao.tipo(), transacao.valor(), transacao.descricao(), saldo,
                transacao.criadoEm(), reenvio);
    }
}
