package com.braga.carteiradigital.transacao.adaptadores.entrada.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

record DepositoRequest(
        // BigDecimal: o JSON "10.10" chega exatamente como 10.10, sem passar por double
        @NotNull(message = "é obrigatório")
        @Positive(message = "deve ser maior que zero")
        @Digits(integer = 13, fraction = 2, message = "deve ter no máximo 13 dígitos inteiros e 2 casas decimais")
        BigDecimal valor,

        @Size(max = 140, message = "deve ter no máximo 140 caracteres")
        String descricao) {
}
