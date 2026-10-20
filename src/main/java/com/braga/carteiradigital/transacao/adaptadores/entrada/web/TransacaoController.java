package com.braga.carteiradigital.transacao.adaptadores.entrada.web;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;
import com.braga.carteiradigital.transacao.aplicacao.porta.entrada.DepositarUseCase;
import com.braga.carteiradigital.transacao.aplicacao.porta.entrada.TransferirUseCase;
import com.braga.carteiradigital.transacao.dominio.ChaveIdempotencia;
import com.braga.carteiradigital.transacao.dominio.Comprovante;

import jakarta.validation.Valid;

/**
 * Endpoints financeiros. Todos exigem o cabeçalho {@code Idempotency-Key}, e o cabeçalho é
 * declarado com {@code required = false} para que a ausência vire um erro {@code dados-invalidos}
 * igual aos dos outros campos.
 *
 * <p>Um reenvio (mesma chave, mesmos dados) recebe a mesma resposta da primeira vez, mais o
 * cabeçalho {@code Idempotent-Replayed: true}, para o cliente saber que nada foi movimentado de novo.
 */
@RestController
@RequestMapping("/api/transacoes")
class TransacaoController {

    static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    static final String IDEMPOTENT_REPLAYED = "Idempotent-Replayed";

    private final DepositarUseCase depositar;
    private final TransferirUseCase transferir;

    TransacaoController(DepositarUseCase depositar, TransferirUseCase transferir) {
        this.depositar = depositar;
        this.transferir = transferir;
    }

    /** O depósito sempre vai para a carteira do dono do token. */
    @PostMapping("/depositos")
    ResponseEntity<ComprovanteResponse> depositar(
            @AuthenticationPrincipal Jwt token,
            @RequestHeader(name = IDEMPOTENCY_KEY, required = false) @ChaveIdempotenciaValida String chaveIdempotencia,
            @Valid @RequestBody DepositoRequest requisicao) {
        var comando = new DepositarUseCase.Comando(UUID.fromString(token.getSubject()),
                new Dinheiro(requisicao.valor()), requisicao.descricao(), new ChaveIdempotencia(chaveIdempotencia));
        return responder(depositar.depositar(comando));
    }

    /** Sai da carteira do dono do token e vai para a carteira do usuário com o e-mail informado. */
    @PostMapping("/transferencias")
    ResponseEntity<ComprovanteResponse> transferir(
            @AuthenticationPrincipal Jwt token,
            @RequestHeader(name = IDEMPOTENCY_KEY, required = false) @ChaveIdempotenciaValida String chaveIdempotencia,
            @Valid @RequestBody TransferenciaRequest requisicao) {
        var comando = new TransferirUseCase.Comando(UUID.fromString(token.getSubject()),
                requisicao.emailDestinatario(), new Dinheiro(requisicao.valor()), requisicao.descricao(),
                new ChaveIdempotencia(chaveIdempotencia));
        return responder(transferir.transferir(comando));
    }

    private static ResponseEntity<ComprovanteResponse> responder(Comprovante comprovante) {
        var resposta = ResponseEntity.status(HttpStatus.CREATED);
        if (comprovante.reenvio()) {
            resposta.header(IDEMPOTENT_REPLAYED, "true");
        }
        return resposta.body(ComprovanteResponse.de(comprovante));
    }
}
