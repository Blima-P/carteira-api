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
        exigirValorPositivo(valor, "crédito");
        return movimentar(Lancamento.Natureza.CREDITO, valor, saldo.somar(valor), transacaoId, relogio);
    }

    /** @throws SaldoInsuficienteException se o valor for maior que o saldo */
    public Movimentacao debitar(Dinheiro valor, UUID transacaoId, Clock relogio) {
        exigirValorPositivo(valor, "débito");
        if (saldo.compareTo(valor) < 0) {
            throw new SaldoInsuficienteException();
        }
        return movimentar(Lancamento.Natureza.DEBITO, valor, saldo.subtrair(valor), transacaoId, relogio);
    }

    private Movimentacao movimentar(Lancamento.Natureza natureza, Dinheiro valor, Dinheiro novoSaldo,
            UUID transacaoId, Clock relogio) {
        Objects.requireNonNull(transacaoId, "transação é obrigatória");
        Instant agora = agora(relogio);
        Carteira atualizada = new Carteira(id, usuarioId, novoSaldo, criadoEm, agora);
        Lancamento lancamento = new Lancamento(transacaoId, id, natureza, valor, novoSaldo, agora);
        return new Movimentacao(atualizada, lancamento);
    }

    private static void exigirValorPositivo(Dinheiro valor, String operacao) {
        Objects.requireNonNull(valor, "valor é obrigatório");
        if (!valor.ehPositivo()) {
            throw new IllegalArgumentException("valor do " + operacao + " deve ser positivo");
        }
    }

    // O PostgreSQL guarda microssegundos; truncar evita diferença entre o objeto e o banco
    private static Instant agora(Clock relogio) {
        return Instant.now(relogio).truncatedTo(ChronoUnit.MICROS);
    }
}
