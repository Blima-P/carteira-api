package com.braga.carteiradigital.carteira.dominio;

import com.braga.carteiradigital.compartilhado.dominio.ErroDeNegocio;

public class CarteiraNaoEncontradaException extends ErroDeNegocio {

    public CarteiraNaoEncontradaException() {
        super(Tipo.NAO_ENCONTRADO, "carteira-nao-encontrada", "Carteira não encontrada.");
    }
}
