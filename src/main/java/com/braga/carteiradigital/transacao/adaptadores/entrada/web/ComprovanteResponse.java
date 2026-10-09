package com.braga.carteiradigital.transacao.adaptadores.entrada.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.braga.carteiradigital.transacao.dominio.Comprovante;
import com.braga.carteiradigital.transacao.dominio.TipoTransacao;

record ComprovanteResponse(UUID transacaoId, TipoTransacao tipo, BigDecimal valor, String descricao,
        BigDecimal saldoAtual, Instant realizadaEm) {

    static ComprovanteResponse de(Comprovante comprovante) {
        return new ComprovanteResponse(comprovante.transacaoId(), comprovante.tipo(), comprovante.valor().quantia(),
                comprovante.descricao(), comprovante.saldoAtual().quantia(), comprovante.realizadaEm());
    }
}
