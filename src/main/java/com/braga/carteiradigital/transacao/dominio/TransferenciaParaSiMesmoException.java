package com.braga.carteiradigital.transacao.dominio;

import com.braga.carteiradigital.compartilhado.dominio.ErroDeNegocio;

public class TransferenciaParaSiMesmoException extends ErroDeNegocio {

    public TransferenciaParaSiMesmoException() {
        super(Tipo.REGRA_VIOLADA, "transferencia-para-si-mesmo", "Não é possível transferir para a própria carteira.");
    }
}
