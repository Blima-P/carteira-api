package com.braga.carteiradigital.suporte;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * Base dos testes de integração: sobe a aplicação inteira contra um PostgreSQL real.
 * O Flyway aplica as migrations ao iniciar o contexto.
 */
@SpringBootTest
public abstract class IntegracaoTestBase {

    @DynamicPropertySource
    static void configurarBanco(DynamicPropertyRegistry registro) {
        EmbeddedPostgres postgres = PostgresEmbarcado.paraTestes();
        registro.add("spring.datasource.url", () -> postgres.getJdbcUrl("postgres", "postgres"));
        registro.add("spring.datasource.username", () -> "postgres");
        registro.add("spring.datasource.password", () -> "postgres");
    }
}
