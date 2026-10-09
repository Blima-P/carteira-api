package com.braga.carteiradigital.carteira.dominio;

/** Resultado de uma operação na carteira: o novo estado e o lançamento que o justifica. */
public record Movimentacao(Carteira carteira, Lancamento lancamento) {
}
