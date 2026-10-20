package com.braga.carteiradigital.carteira.dominio;

import com.braga.carteiradigital.compartilhado.dominio.ErroDeNegocio;

public class SaldoInsuficienteException extends ErroDeNegocio {

    public SaldoInsuficienteException() {
        super(Tipo.REGRA_VIOLADA, "saldo-insuficiente", "Saldo insuficiente para esta operação.");
    }
}
