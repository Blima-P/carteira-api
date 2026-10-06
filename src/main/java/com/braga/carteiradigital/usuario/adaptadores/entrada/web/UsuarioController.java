package com.braga.carteiradigital.usuario.adaptadores.entrada.web;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.braga.carteiradigital.usuario.aplicacao.porta.entrada.ConsultarUsuarioUseCase;

/** Endpoints protegidos: exigem {@code Authorization: Bearer <token>}. */
@RestController
@RequestMapping("/api/usuarios")
class UsuarioController {

    private final ConsultarUsuarioUseCase consultarUsuario;

    UsuarioController(ConsultarUsuarioUseCase consultarUsuario) {
        this.consultarUsuario = consultarUsuario;
    }

    /** O id vem do próprio token (claim "sub"): um usuário nunca consegue consultar dados de outro. */
    @GetMapping("/eu")
    UsuarioResponse eu(@AuthenticationPrincipal Jwt token) {
        return UsuarioResponse.de(consultarUsuario.buscarPorId(UUID.fromString(token.getSubject())));
    }
}
