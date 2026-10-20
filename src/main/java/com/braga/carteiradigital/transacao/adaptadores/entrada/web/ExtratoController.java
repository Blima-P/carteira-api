package com.braga.carteiradigital.transacao.adaptadores.entrada.web;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.braga.carteiradigital.transacao.aplicacao.porta.entrada.ConsultarExtratoUseCase;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Extrato da carteira do dono do token, do mais recente para o mais antigo.
 *
 * <p>Paginação por cursor: a primeira página vem sem {@code cursor}; as seguintes usam o
 * {@code proximoCursor} da página anterior, até ele vir {@code null}.
 */
@RestController
@RequestMapping("/api/transacoes")
class ExtratoController {

    static final int TAMANHO_MAXIMO = 100;

    private final ConsultarExtratoUseCase consultarExtrato;

    ExtratoController(ConsultarExtratoUseCase consultarExtrato) {
        this.consultarExtrato = consultarExtrato;
    }

    @GetMapping("/extrato")
    ExtratoResponse extrato(
            @AuthenticationPrincipal Jwt token,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "deve ser no mínimo 1")
            @Max(value = TAMANHO_MAXIMO, message = "deve ser no máximo " + TAMANHO_MAXIMO) int tamanho,
            @RequestParam(required = false) @CursorValido String cursor) {
        var consulta = new ConsultarExtratoUseCase.Consulta(UUID.fromString(token.getSubject()),
                cursor == null ? null : CursorDoExtrato.decodificar(cursor), tamanho);
        return ExtratoResponse.de(consultarExtrato.consultar(consulta));
    }
}
