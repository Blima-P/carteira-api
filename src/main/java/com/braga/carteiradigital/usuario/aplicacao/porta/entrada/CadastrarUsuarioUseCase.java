package com.braga.carteiradigital.usuario.aplicacao.porta.entrada;

import com.braga.carteiradigital.usuario.dominio.Usuario;

public interface CadastrarUsuarioUseCase {

    Usuario cadastrar(Comando comando);

    record Comando(String nome, String email, String senha) {

        @Override
        public String toString() {
            return "Comando[nome=" + nome + ", email=" + email + "]";
        }
    }
}
