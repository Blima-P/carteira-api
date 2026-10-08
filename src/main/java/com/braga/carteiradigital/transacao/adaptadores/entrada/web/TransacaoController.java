package com.braga.carteiradigital.transacao.adaptadores.entrada.web;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;
import com.braga.carteiradigital.transacao.aplicacao.porta.entrada.DepositarUseCase;
import com.braga.carteiradigital.transacao.dominio.ChaveIdempotencia;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/api/transacoes")
class TransacaoController {

    static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private final DepositarUseCase depositar;

    TransacaoController(DepositarUseCase depositar) {
        this.depositar = depositar;
    }

    /** O depósito sempre vai para a carteira do dono do token. */
    @PostMapping("/depositos")
    @ResponseStatus(HttpStatus.CREATED)
    ComprovanteResponse depositar(
            @AuthenticationPrincipal Jwt token,
            // required = false: a ausência vira um erro "dados-invalidos" igual aos outros campos
            @RequestHeader(name = IDEMPOTENCY_KEY, required = false)
            @NotBlank(message = "é obrigatório")
            @Pattern(regexp = ChaveIdempotencia.FORMATO,
                    message = "deve ter de 1 a 64 caracteres entre letras, números, '-' e '_'")
            String chaveIdempotencia,
            @Valid @RequestBody DepositoRequest requisicao) {
        var comando = new DepositarUseCase.Comando(UUID.fromString(token.getSubject()),
                new Dinheiro(requisicao.valor()), requisicao.descricao(), new ChaveIdempotencia(chaveIdempotencia));
        return ComprovanteResponse.de(depositar.depositar(comando));
    }
}
