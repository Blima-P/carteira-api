package com.braga.carteiradigital.carteira.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;

/**
 * Registro imutável de um crédito ou débito no livro-razão. Somando os lançamentos de uma
 * carteira, chega-se sempre ao saldo atual (auditoria).
 */
public record Lancamento(UUID transacaoId, UUID carteiraId, Natureza natureza, Dinheiro valor,
        Dinheiro saldoApos, Instant criadoEm) {

    public enum Natureza {
        CREDITO,
        DEBITO
    }

    public Lancamento {
        Objects.requireNonNull(transacaoId, "transação é obrigatória");
        Objects.requireNonNull(carteiraId, "carteira é obrigatória");
        Objects.requireNonNull(natureza, "natureza é obrigatória");
        Objects.requireNonNull(valor, "valor é obrigatório");
        Objects.requireNonNull(saldoApos, "saldo após o lançamento é obrigatório");
        Objects.requireNonNull(criadoEm, "data de criação é obrigatória");
        if (!valor.ehPositivo()) {
            throw new IllegalArgumentException("valor do lançamento deve ser positivo");
        }
    }
}
