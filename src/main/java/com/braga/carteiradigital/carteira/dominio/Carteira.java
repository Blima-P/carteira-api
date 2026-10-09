package com.braga.carteiradigital.carteira.dominio;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;

/**
 * Carteira de um usuário. Imutável: cada operação devolve uma nova instância com o saldo
 * atualizado, junto do {@link Lancamento} que registra a operação no livro-razão.
 */
public record Carteira(UUID id, UUID usuarioId, Dinheiro saldo, Instant criadoEm, Instant atualizadoEm) {

    public Carteira {
        Objects.requireNonNull(id, "id é obrigatório");
        Objects.requireNonNull(usuarioId, "usuário é obrigatório");
        Objects.requireNonNull(saldo, "saldo é obrigatório");
        Objects.requireNonNull(criadoEm, "data de criação é obrigatória");
        Objects.requireNonNull(atualizadoEm, "data de atualização é obrigatória");
        if (saldo.ehNegativo()) {
            throw new IllegalArgumentException("saldo não pode ser negativo");
        }
    }

    public static Carteira nova(UUID usuarioId, Clock relogio) {
        Instant agora = agora(relogio);
        return new Carteira(UUID.randomUUID(), usuarioId, Dinheiro.ZERO, agora, agora);
    }

    public Movimentacao creditar(Dinheiro valor, UUID transacaoId, Clock relogio) {
        Objects.requireNonNull(transacaoId, "transação é obrigatória");
        if (!valor.ehPositivo()) {
            throw new IllegalArgumentException("valor do crédito deve ser positivo");
        }
        Instant agora = agora(relogio);
        Carteira atualizada = new Carteira(id, usuarioId, saldo.somar(valor), criadoEm, agora);
        Lancamento lancamento = new Lancamento(transacaoId, id, Lancamento.Natureza.CREDITO, valor,
                atualizada.saldo(), agora);
        return new Movimentacao(atualizada, lancamento);
    }

    // O PostgreSQL guarda microssegundos; truncar evita diferença entre o objeto e o banco
    private static Instant agora(Clock relogio) {
        return Instant.now(relogio).truncatedTo(ChronoUnit.MICROS);
    }
}
