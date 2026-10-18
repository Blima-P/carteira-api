package com.braga.carteiradigital.transacao.dominio;

import java.util.List;
import java.util.Objects;

/**
 * Uma página do extrato. {@code proximaPagina} é a posição a partir da qual a próxima página começa,
 * ou {@code null} se esta for a última.
 */
public record Extrato(List<ItemDoExtrato> itens, PosicaoNoExtrato proximaPagina) {

    public Extrato {
        itens = List.copyOf(Objects.requireNonNull(itens, "itens são obrigatórios"));
    }
}
