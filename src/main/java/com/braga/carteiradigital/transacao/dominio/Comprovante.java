package com.braga.carteiradigital.transacao.dominio;

import java.time.Instant;
import java.util.UUID;

import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;

/** O que o cliente recebe após uma operação concluída. */
public record Comprovante(UUID transacaoId, TipoTransacao tipo, Dinheiro valor, String descricao,
        Dinheiro saldoAtual, Instant realizadaEm) {

    public static Comprovante de(Transacao transacao, Dinheiro saldoAtual) {
        return new Comprovante(transacao.id(), transacao.tipo(), transacao.valor(), transacao.descricao(),
                saldoAtual, transacao.criadoEm());
    }
}
