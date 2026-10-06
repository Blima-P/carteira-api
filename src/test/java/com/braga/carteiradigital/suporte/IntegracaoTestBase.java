package com.braga.carteiradigital.suporte;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * Base dos testes de integração: sobe a aplicação inteira contra um PostgreSQL real.
 * O Flyway aplica as migrations ao iniciar o contexto.
 *
 * <p>Todas as classes de teste compartilham o mesmo contexto Spring (sobe uma vez só),
 * e o banco é esvaziado antes de cada teste para um não interferir no outro.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegracaoTestBase {

    /** Segredo usado apenas nos testes. */
    public static final String SEGREDO_JWT_TESTE = "segredo-de-teste-com-pelo-menos-32-caracteres";

    @Autowired
    protected MockMvcTester mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @DynamicPropertySource
    static void configurarAmbiente(DynamicPropertyRegistry registro) {
        EmbeddedPostgres postgres = PostgresEmbarcado.paraTestes();
        registro.add("spring.datasource.url", () -> postgres.getJdbcUrl("postgres", "postgres"));
        registro.add("spring.datasource.username", () -> "postgres");
        registro.add("spring.datasource.password", () -> "postgres");
        registro.add("seguranca.jwt.segredo", () -> SEGREDO_JWT_TESTE);
    }

    @BeforeEach
    void limparBanco() {
        jdbc.execute("TRUNCATE lancamentos, transacoes, carteiras, usuarios CASCADE");
    }
}
