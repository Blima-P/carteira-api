package com.braga.carteiradigital.usuario.adaptadores.saida.seguranca;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.braga.carteiradigital.usuario.aplicacao.porta.saida.CodificadorDeSenha;

/**
 * Usa o encoder "delegante" do Spring Security: o hash é salvo com o algoritmo como prefixo
 * ({@code {bcrypt}$2a$10$...}). Se no futuro trocarmos de algoritmo, as senhas antigas continuam válidas.
 */
@Component
class CodificadorDeSenhaAdapter implements CodificadorDeSenha {

    private final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Override
    public String codificar(String senha) {
        return encoder.encode(senha);
    }

    @Override
    public boolean confere(String senha, String senhaHash) {
        return encoder.matches(senha, senhaHash);
    }
}
