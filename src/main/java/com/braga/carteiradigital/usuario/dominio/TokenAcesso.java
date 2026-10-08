package com.braga.carteiradigital.usuario.dominio;

import java.time.Instant;

/** Token de acesso emitido após um login bem-sucedido. */
public record TokenAcesso(String valor, Instant expiraEm) {

    @Override
    public String toString() {
        return "TokenAcesso[expiraEm=" + expiraEm + "]";
    }
}
