package com.braga.carteiradigital.usuario.dominio;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object de e-mail: sempre normalizado (sem espaços e em minúsculas), garantindo
 * que {@code Ana@Email.com} e {@code ana@email.com} sejam tratados como o mesmo usuário.
 */
public record Email(String valor) {

    private static final Pattern FORMATO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int TAMANHO_MAXIMO = 180;

    public Email {
        Objects.requireNonNull(valor, "e-mail é obrigatório");
        valor = valor.strip().toLowerCase(Locale.ROOT);
        if (valor.length() > TAMANHO_MAXIMO || !FORMATO.matcher(valor).matches()) {
            throw new IllegalArgumentException("e-mail inválido");
        }
    }

    @Override
    public String toString() {
        return valor;
    }
}
