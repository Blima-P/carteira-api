package com.braga.carteiradigital.usuario.aplicacao.porta.saida;

public interface CodificadorDeSenha {

    String codificar(String senha);

    boolean confere(String senha, String senhaHash);
}
