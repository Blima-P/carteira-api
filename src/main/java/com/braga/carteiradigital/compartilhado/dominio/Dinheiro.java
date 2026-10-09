package com.braga.carteiradigital.compartilhado.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Valor monetário em reais, sempre com exatamente 2 casas decimais.
 *
 * <p>Usa {@link BigDecimal} (decimal exato). {@code double} e {@code float} acumulam erros de
 * arredondamento ({@code 0.1 + 0.2 = 0.30000000000000004}) e nunca devem representar dinheiro.
 */
public record Dinheiro(BigDecimal quantia) implements Comparable<Dinheiro> {

    private static final int CASAS_DECIMAIS = 2;

    public static final Dinheiro ZERO = new Dinheiro(BigDecimal.ZERO);

    public Dinheiro {
        Objects.requireNonNull(quantia, "quantia é obrigatória");
        try {
            // Frações de centavo são recusadas em vez de arredondadas silenciosamente
            quantia = quantia.setScale(CASAS_DECIMAIS, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException erro) {
            throw new IllegalArgumentException("valor não pode ter mais de 2 casas decimais: " + quantia.toPlainString());
        }
    }

    public static Dinheiro de(String quantia) {
        return new Dinheiro(new BigDecimal(quantia));
    }

    public Dinheiro somar(Dinheiro outro) {
        return new Dinheiro(quantia.add(outro.quantia));
    }

    public Dinheiro subtrair(Dinheiro outro) {
        return new Dinheiro(quantia.subtract(outro.quantia));
    }

    public boolean ehPositivo() {
        return quantia.signum() > 0;
    }

    public boolean ehNegativo() {
        return quantia.signum() < 0;
    }

    @Override
    public int compareTo(Dinheiro outro) {
        return quantia.compareTo(outro.quantia);
    }

    @Override
    public String toString() {
        return quantia.toPlainString();
    }
}
