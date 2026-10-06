package com.braga.carteiradigital.usuario.adaptadores.entrada.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CadastroRequest(
        @NotBlank(message = "é obrigatório")
        @Size(max = 120, message = "deve ter no máximo 120 caracteres")
        String nome,

        @NotBlank(message = "é obrigatório")
        @Email(message = "deve ser um e-mail válido")
        @Size(max = 180, message = "deve ter no máximo 180 caracteres")
        String email,

        // BCrypt considera no máximo 72 bytes da senha
        @NotBlank(message = "é obrigatória")
        @Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres")
        String senha) {

    @Override
    public String toString() {
        return "CadastroRequest[nome=" + nome + ", email=" + email + "]";
    }
}
