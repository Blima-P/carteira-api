package com.braga.carteiradigital.usuario.aplicacao.porta.saida;

import com.braga.carteiradigital.usuario.dominio.TokenAcesso;
import com.braga.carteiradigital.usuario.dominio.Usuario;

public interface EmissorDeToken {

    TokenAcesso emitir(Usuario usuario);
}
