package com.braga.carteiradigital.transacao.dominio;

import com.braga.carteiradigital.compartilhado.dominio.ErroDeNegocio;

/**
 * Duas requisições com a mesma chave chegaram ao mesmo tempo e a outra foi gravada primeiro.
 * Reenviando, o cliente recebe o comprovante da operação que foi concluída.
 */
public class ChaveIdempotenciaEmUsoException extends ErroDeNegocio {

    public ChaveIdempotenciaEmUsoException() {
        super(Tipo.CONFLITO, "chave-idempotencia-em-uso",
                "Outra requisição com esta Idempotency-Key estava sendo processada ao mesmo tempo. "
                        + "Reenvie a requisição para receber o resultado.");
    }
}
