package com.braga.carteiradigital.suporte;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;

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

    /** Cadastra um usuário pela API, faz login e devolve o token JWT. */
    protected String novoUsuarioComToken(String email) {
        String credenciais = """
                {"nome": "Teste", "email": "%s", "senha": "senha-segura-123"}
                """.formatted(email);
        assertThat(mvc.post().uri("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(credenciais))
                .hasStatus(HttpStatus.CREATED);

        MvcTestResult login = mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credenciais).exchange();
        assertThat(login).hasStatusOk();
        return lerJson(login, "$.token");
    }

    protected static <T> T lerJson(MvcTestResult resultado, String caminho) {
        return JsonPath.read(new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8), caminho);
    }
}
