package com.braga.carteiradigital.usuario.aplicacao.porta.entrada;

import com.braga.carteiradigital.usuario.dominio.TokenAcesso;

public interface AutenticarUsuarioUseCase {

    TokenAcesso autenticar(String email, String senha);
}
