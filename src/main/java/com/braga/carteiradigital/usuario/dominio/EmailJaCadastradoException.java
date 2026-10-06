package com.braga.carteiradigital.usuario.dominio;

import com.braga.carteiradigital.compartilhado.dominio.ErroDeNegocio;

public class EmailJaCadastradoException extends ErroDeNegocio {

    public EmailJaCadastradoException() {
        super(Tipo.CONFLITO, "email-ja-cadastrado", "Já existe um usuário com este e-mail.");
    }
}
