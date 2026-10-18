package com.braga.carteiradigital.transacao.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;

/**
 * Uma linha do extrato, do ponto de vista de quem o consulta: a mesma transferência aparece como
 * {@code DEBITO} para quem enviou e como {@code CREDITO} para quem recebeu.
 *
 * <p>{@code contraparte} é o nome do outro usuário da transferência ({@code null} em depósitos).
 * {@code realizadaEm} é o momento em que o saldo mudou, por isso a ordem do extrato e os valores
 * de {@code saldoApos} são sempre coerentes entre si.
 */
public record ItemDoExtrato(UUID transacaoId, TipoTransacao tipo, Natureza natureza, Dinheiro valor,
        Dinheiro saldoApos, String descricao, String contraparte, Instant realizadaEm) {

    public enum Natureza {
        CREDITO,
        DEBITO
    }

    public ItemDoExtrato {
        Objects.requireNonNull(transacaoId, "transação é obrigatória");
        Objects.requireNonNull(tipo, "tipo é obrigatório");
        Objects.requireNonNull(natureza, "natureza é obrigatória");
        Objects.requireNonNull(valor, "valor é obrigatório");
        Objects.requireNonNull(saldoApos, "saldo após a transação é obrigatório");
        Objects.requireNonNull(realizadaEm, "data é obrigatória");
    }
}
