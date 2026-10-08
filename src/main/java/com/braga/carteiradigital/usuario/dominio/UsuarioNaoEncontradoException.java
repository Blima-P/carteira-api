package com.braga.carteiradigital.usuario.dominio;

import com.braga.carteiradigital.compartilhado.dominio.ErroDeNegocio;

public class UsuarioNaoEncontradoException extends ErroDeNegocio {

    public UsuarioNaoEncontradoException() {
        super(Tipo.NAO_ENCONTRADO, "usuario-nao-encontrado", "Usuário não encontrado.");
    }
}
