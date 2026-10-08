package com.braga.carteiradigital.compartilhado.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class DinheiroTest {

    @Test
    void deveSempreTerDuasCasasDecimais() {
        assertThat(Dinheiro.de("10").quantia()).isEqualByComparingTo("10.00").hasScaleOf(2);
        assertThat(Dinheiro.de("10.5").toString()).isEqualTo("10.50");
    }

    @Test
    void deveSomarSemErroDeArredondamento() {
        // Com double: 0.1 + 0.2 = 0.30000000000000004
        assertThat(Dinheiro.de("0.10").somar(Dinheiro.de("0.20"))).isEqualTo(Dinheiro.de("0.30"));
    }

    @Test
    void deveRecusarFracaoDeCentavoEmVezDeArredondar() {
        assertThatThrownBy(() -> Dinheiro.de("10.001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 casas decimais");
    }

    @Test
    void deveAceitarZerosAlemDaSegundaCasa() {
        assertThat(new Dinheiro(new BigDecimal("10.500"))).isEqualTo(Dinheiro.de("10.50"));
    }

    @Test
    void deveSerIgualIndependenteDaEscalaDeEntrada() {
        assertThat(Dinheiro.de("1")).isEqualTo(Dinheiro.de("1.00")).hasSameHashCodeAs(Dinheiro.de("1.00"));
    }

    @Test
    void deveInformarSinal() {
        assertThat(Dinheiro.de("0.01").ehPositivo()).isTrue();
        assertThat(Dinheiro.ZERO.ehPositivo()).isFalse();
        assertThat(Dinheiro.ZERO.ehNegativo()).isFalse();
        assertThat(Dinheiro.de("-0.01").ehNegativo()).isTrue();
    }

    @Test
    void deveCompararPorValor() {
        assertThat(Dinheiro.de("2.00")).isGreaterThan(Dinheiro.de("1.99"));
    }
}
