package com.braga.carteiradigital.usuario.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Teste de unidade puro: o domínio não depende de Spring, então roda em milissegundos. */
class EmailTest {

    @Test
    void deveNormalizarParaMinusculasSemEspacos() {
        assertThat(new Email("  Ana.Silva@Email.COM ").valor()).isEqualTo("ana.silva@email.com");
    }

    @Test
    void emailsQueDiferemSoNaCaixaDevemSerIguais() {
        assertThat(new Email("ANA@email.com")).isEqualTo(new Email("ana@EMAIL.com"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ana", "ana@", "@email.com", "ana@email", "ana silva@email.com", "a@b@c.com"})
    void deveRecusarFormatoInvalido(String valor) {
        assertThatThrownBy(() -> new Email(valor)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRecusarEmailMaiorQueOLimiteDoBanco() {
        String longo = "a".repeat(175) + "@x.com";

        assertThatThrownBy(() -> new Email(longo)).isInstanceOf(IllegalArgumentException.class);
    }
}
