package com.braga.carteiradigital.transacao.dominio;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Valor do cabeçalho {@code Idempotency-Key}, gerado pelo cliente (ex.: um UUID) para cada operação.
 * Se a requisição for reenviada por falha de rede, a mesma chave impede que a operação seja feita duas vezes.
 */
public record ChaveIdempotencia(String valor) {

    public static final int TAMANHO_MAXIMO = 64;
    public static final String FORMATO = "^[A-Za-z0-9_-]{1,64}$";

    private static final Pattern PADRAO = Pattern.compile(FORMATO);

    public ChaveIdempotencia {
        Objects.requireNonNull(valor, "chave de idempotência é obrigatória");
        if (!PADRAO.matcher(valor).matches()) {
            throw new IllegalArgumentException(
                    "chave de idempotência deve ter de 1 a 64 caracteres entre letras, números, '-' e '_'");
        }
    }

    @Override
    public String toString() {
        return valor;
    }
}
