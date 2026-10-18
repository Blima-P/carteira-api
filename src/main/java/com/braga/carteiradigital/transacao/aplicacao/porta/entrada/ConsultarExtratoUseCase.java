package com.braga.carteiradigital.transacao.aplicacao.porta.entrada;

import java.util.UUID;

import com.braga.carteiradigital.transacao.dominio.Extrato;
import com.braga.carteiradigital.transacao.dominio.PosicaoNoExtrato;

public interface ConsultarExtratoUseCase {

    Extrato consultar(Consulta consulta);

    /** {@code depoisDe} é {@code null} na primeira página. */
    record Consulta(UUID usuarioId, PosicaoNoExtrato depoisDe, int tamanho) {

        public Consulta {
            if (tamanho < 1) {
                throw new IllegalArgumentException("tamanho da página deve ser positivo");
            }
        }
    }
}
