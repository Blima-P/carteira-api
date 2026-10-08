package com.braga.carteiradigital.carteira.adaptadores.entrada.web;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.braga.carteiradigital.carteira.aplicacao.porta.entrada.ConsultarCarteiraUseCase;

@RestController
@RequestMapping("/api/carteiras")
class CarteiraController {

    private final ConsultarCarteiraUseCase consultarCarteira;

    CarteiraController(ConsultarCarteiraUseCase consultarCarteira) {
        this.consultarCarteira = consultarCarteira;
    }

    /** Carteira do dono do token. Não existe rota com id: ninguém consulta a carteira de outra pessoa. */
    @GetMapping("/minha")
    CarteiraResponse minha(@AuthenticationPrincipal Jwt token) {
        return CarteiraResponse.de(consultarCarteira.buscarDoUsuario(UUID.fromString(token.getSubject())));
    }
}
