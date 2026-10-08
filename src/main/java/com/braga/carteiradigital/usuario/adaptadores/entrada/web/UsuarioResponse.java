package com.braga.carteiradigital.usuario.adaptadores.entrada.web;

import java.time.Instant;
import java.util.UUID;

import com.braga.carteiradigital.usuario.dominio.Usuario;

/** Dados públicos do usuário. O hash da senha nunca sai da API. */
record UsuarioResponse(UUID id, String nome, String email, Instant criadoEm) {

    static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.id(), usuario.nome(), usuario.email().valor(), usuario.criadoEm());
    }
}
