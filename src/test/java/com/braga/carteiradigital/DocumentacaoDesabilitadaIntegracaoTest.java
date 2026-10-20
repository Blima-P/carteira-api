package com.braga.carteiradigital;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.braga.carteiradigital.suporte.IntegracaoTestBase;

class DocumentacaoDesabilitadaIntegracaoTest extends IntegracaoTestBase {

    @Test
    void documentacaoNaoDeveSerPublicaPorPadrao() {
        assertThat(mvc.get().uri("/v3/api-docs")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/swagger-ui.html")).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
