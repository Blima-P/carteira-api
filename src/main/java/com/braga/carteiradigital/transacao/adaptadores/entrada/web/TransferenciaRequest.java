package com.braga.carteiradigital.transacao.adaptadores.entrada.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

record TransferenciaRequest(
        @NotBlank(message = "é obrigatório")
        @Email(message = "deve ser um e-mail válido")
        @Size(max = 180, message = "deve ter no máximo 180 caracteres")
        String emailDestinatario,

        @NotNull(message = "é obrigatório")
        @Positive(message = "deve ser maior que zero")
        @Digits(integer = 13, fraction = 2, message = "deve ter no máximo 13 dígitos inteiros e 2 casas decimais")
        BigDecimal valor,

        @Size(max = 140, message = "deve ter no máximo 140 caracteres")
        String descricao) {
}
