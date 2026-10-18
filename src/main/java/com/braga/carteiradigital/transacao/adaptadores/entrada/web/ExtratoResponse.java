package com.braga.carteiradigital.transacao.adaptadores.entrada.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.braga.carteiradigital.transacao.dominio.Extrato;
import com.braga.carteiradigital.transacao.dominio.ItemDoExtrato;
import com.braga.carteiradigital.transacao.dominio.TipoTransacao;

/** {@code proximoCursor} é {@code null} na última página. */
record ExtratoResponse(List<Item> itens, String proximoCursor) {

    record Item(UUID transacaoId, TipoTransacao tipo, ItemDoExtrato.Natureza natureza, BigDecimal valor,
            BigDecimal saldoApos, String descricao, String contraparte, Instant realizadaEm) {

        static Item de(ItemDoExtrato item) {
            return new Item(item.transacaoId(), item.tipo(), item.natureza(), item.valor().quantia(),
                    item.saldoApos().quantia(), item.descricao(), item.contraparte(), item.realizadaEm());
        }
    }

    static ExtratoResponse de(Extrato extrato) {
        String proximoCursor = extrato.proximaPagina() == null ? null : CursorDoExtrato.codificar(extrato.proximaPagina());
        return new ExtratoResponse(extrato.itens().stream().map(Item::de).toList(), proximoCursor);
    }
}
