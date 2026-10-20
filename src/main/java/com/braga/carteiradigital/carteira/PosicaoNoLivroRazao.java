package com.braga.carteiradigital.carteira;

import java.time.Instant;
import java.util.Objects;

/**
 * Onde um lançamento fica na ordem do livro-razão: data de criação e, para desempatar, o id do lançamento.
 * Serve de marcador para pedir os lançamentos seguintes de uma consulta paginada.
 */
public record PosicaoNoLivroRazao(Instant criadoEm, long lancamentoId) {

    public PosicaoNoLivroRazao {
        Objects.requireNonNull(criadoEm, "data de criação é obrigatória");
        if (lancamentoId <= 0) {
            throw new IllegalArgumentException("id do lançamento deve ser positivo");
        }
    }
}
