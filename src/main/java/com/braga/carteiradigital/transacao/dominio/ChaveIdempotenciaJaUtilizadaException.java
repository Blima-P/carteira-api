package com.braga.carteiradigital.transacao.dominio;

import com.braga.carteiradigital.compartilhado.dominio.ErroDeNegocio;

public class ChaveIdempotenciaJaUtilizadaException extends ErroDeNegocio {

    public ChaveIdempotenciaJaUtilizadaException() {
        super(Tipo.CONFLITO, "chave-idempotencia-ja-utilizada",
                "Esta Idempotency-Key já foi usada. Gere uma nova chave para cada operação.");
    }
}
