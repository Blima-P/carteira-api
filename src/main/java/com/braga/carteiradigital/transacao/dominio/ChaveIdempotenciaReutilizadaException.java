package com.braga.carteiradigital.transacao.dominio;

import com.braga.carteiradigital.compartilhado.dominio.ErroDeNegocio;

/** A chave já pertence a outra operação: reenviá-la com dados diferentes é um erro do cliente, não um reenvio. */
public class ChaveIdempotenciaReutilizadaException extends ErroDeNegocio {

    public ChaveIdempotenciaReutilizadaException() {
        super(Tipo.REGRA_VIOLADA, "chave-idempotencia-reutilizada",
                "Esta Idempotency-Key já foi usada em uma operação com outros dados. Gere uma nova chave para cada operação.");
    }
}
