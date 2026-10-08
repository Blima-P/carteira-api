package com.braga.carteiradigital.carteira.adaptadores.entrada.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.braga.carteiradigital.carteira.dominio.Carteira;

record CarteiraResponse(UUID id, BigDecimal saldo, Instant atualizadoEm) {

    static CarteiraResponse de(Carteira carteira) {
        return new CarteiraResponse(carteira.id(), carteira.saldo().quantia(), carteira.atualizadoEm());
    }
}
