package com.braga.carteiradigital.usuario.dominio;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Usuário da carteira digital. Imutável: qualquer alteração futura gera uma nova instância.
 *
 * <p>Guarda apenas o hash da senha, nunca a senha em texto puro.
 */
public record Usuario(UUID id, String nome, Email email, String senhaHash, Instant criadoEm) {

    public Usuario {
        Objects.requireNonNull(id, "id é obrigatório");
        Objects.requireNonNull(email, "e-mail é obrigatório");
        Objects.requireNonNull(senhaHash, "hash da senha é obrigatório");
        Objects.requireNonNull(criadoEm, "data de criação é obrigatória");
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("nome é obrigatório");
        }
        nome = nome.strip();
    }

    public static Usuario novo(String nome, Email email, String senhaHash, Clock relogio) {
        // O PostgreSQL guarda microssegundos; truncar evita diferença entre o objeto e o banco
        Instant agora = Instant.now(relogio).truncatedTo(ChronoUnit.MICROS);
        return new Usuario(UUID.randomUUID(), nome, email, senhaHash, agora);
    }

    @Override
    public String toString() {
        return "Usuario[id=" + id + ", email=" + email + "]";
    }
}
