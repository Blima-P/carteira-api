package com.braga.carteiradigital;

import org.springframework.boot.SpringApplication;

import com.braga.carteiradigital.suporte.PostgresEmbarcado;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * Sobe a API localmente com um PostgreSQL embarcado, sem Docker:
 *
 * <pre>./mvnw spring-boot:test-run</pre>
 *
 * Os dados ficam em target/postgres-local e são apagados por {@code ./mvnw clean}.
 */
public final class CarteiraDigitalLocalApplication {

    private CarteiraDigitalLocalApplication() {
    }

    public static void main(String[] args) {
        EmbeddedPostgres postgres = PostgresEmbarcado.paraDesenvolvimento();
        System.setProperty("spring.datasource.url", postgres.getJdbcUrl("postgres", "postgres"));
        System.setProperty("spring.datasource.username", "postgres");
        System.setProperty("spring.datasource.password", "postgres");

        SpringApplication.from(CarteiraDigitalApplication::main)
                .withAdditionalProfiles("local")
                .run(args);
    }
}
