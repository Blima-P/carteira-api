package com.braga.carteiradigital.usuario.adaptadores.entrada.web;

import jakarta.validation.constraints.NotBlank;

record LoginRequest(
        @NotBlank(message = "é obrigatório") String email,
        @NotBlank(message = "é obrigatória") String senha) {

    @Override
    public String toString() {
        return "LoginRequest[email=" + email + "]";
    }
}
