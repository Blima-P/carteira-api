package com.braga.carteiradigital.transacao.dominio;

import java.time.Instant;
import java.util.Objects;

/**
 * Posição de um item na ordem do extrato (do mais recente para o mais antigo): o momento do
 * lançamento e o id do lançamento, que desempata itens do mesmo instante.
 */
public record PosicaoNoExtrato(Instant criadoEm, long lancamentoId) {

    public PosicaoNoExtrato {
        Objects.requireNonNull(criadoEm, "data de criação é obrigatória");
        if (lancamentoId <= 0) {
            throw new IllegalArgumentException("id do lançamento deve ser positivo");
        }
    }
}
