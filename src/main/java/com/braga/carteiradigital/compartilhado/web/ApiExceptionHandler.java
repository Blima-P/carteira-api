package com.braga.carteiradigital.compartilhado.web;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.braga.carteiradigital.compartilhado.dominio.ErroDeNegocio;

/**
 * Converte exceções em respostas no padrão RFC 9457 (Problem Details), sempre com um
 * {@code codigo} estável que o cliente pode usar para decidir o que fazer.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ErroDeNegocio.class)
    ResponseEntity<ProblemDetail> tratarErroDeNegocio(ErroDeNegocio erro) {
        HttpStatus status = switch (erro.getTipo()) {
            case CONFLITO -> HttpStatus.CONFLICT;
            case NAO_AUTORIZADO -> HttpStatus.UNAUTHORIZED;
            case NAO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case REGRA_VIOLADA -> HttpStatus.UNPROCESSABLE_CONTENT;
        };
        return ResponseEntity.status(status).body(problema(status, erro.getCodigo(), erro.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> tratarErroInesperado(Exception erro) {
        log.error("Erro inesperado", erro);
        return ResponseEntity.internalServerError().body(problema(HttpStatus.INTERNAL_SERVER_ERROR,
                "erro-interno", "Erro interno. Nenhuma alteração foi aplicada."));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException erro,
            HttpHeaders headers, HttpStatusCode status, WebRequest requisicao) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError campo : erro.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(campo.getField(), campo.getDefaultMessage());
        }
        ProblemDetail corpo = problema(HttpStatus.BAD_REQUEST, "dados-invalidos", "Um ou mais campos são inválidos.");
        corpo.setProperty("campos", campos);
        return ResponseEntity.badRequest().body(corpo);
    }

    /** Erros do próprio Spring MVC (JSON malformado, rota inexistente...) também recebem um {@code codigo}. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception erro, Object corpo, HttpHeaders headers,
            HttpStatusCode status, WebRequest requisicao) {
        if (corpo instanceof ProblemDetail problema
                && (problema.getProperties() == null || !problema.getProperties().containsKey("codigo"))) {
            problema.setProperty("codigo", status.is4xxClientError() ? "requisicao-invalida" : "erro-interno");
        }
        return super.handleExceptionInternal(erro, corpo, headers, status, requisicao);
    }

    private static ProblemDetail problema(HttpStatusCode status, String codigo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setType(URI.create("urn:carteira-api:erro:" + codigo));
        problema.setProperty("codigo", codigo);
        return problema;
    }
}
