package com.braga.carteiradigital.suporte;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * PostgreSQL real (binário embarcado) para testes e desenvolvimento local, sem precisar de Docker.
 */
public final class PostgresEmbarcado {

    private static EmbeddedPostgres paraTestes;

    private PostgresEmbarcado() {
    }

    /** Instância única e descartável, compartilhada por toda a suíte de testes. */
    public static synchronized EmbeddedPostgres paraTestes() {
        if (paraTestes == null) {
            paraTestes = iniciar(EmbeddedPostgres.builder());
        }
        return paraTestes;
    }

    /** Instância com dados persistidos em {@code target/postgres-local}, na porta fixa 5433. */
    public static EmbeddedPostgres paraDesenvolvimento() {
        return iniciar(EmbeddedPostgres.builder()
                .setPort(5433)
                .setDataDirectory(Path.of("target", "postgres-local"))
                .setCleanDataDirectory(false));
    }

    private static EmbeddedPostgres iniciar(EmbeddedPostgres.Builder builder) {
        try {
            EmbeddedPostgres postgres = builder.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    postgres.close();
                } catch (IOException ignorada) {
                    // JVM encerrando
                }
            }));
            return postgres;
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao iniciar o PostgreSQL embarcado", e);
        }
    }
}
