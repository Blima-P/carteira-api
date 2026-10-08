package com.braga.carteiradigital.usuario.adaptadores.entrada.web;

import java.time.Instant;

import com.braga.carteiradigital.usuario.dominio.TokenAcesso;

record TokenResponse(String token, String tipo, Instant expiraEm) {

    static TokenResponse de(TokenAcesso tokenAcesso) {
        return new TokenResponse(tokenAcesso.valor(), "Bearer", tokenAcesso.expiraEm());
    }
}
