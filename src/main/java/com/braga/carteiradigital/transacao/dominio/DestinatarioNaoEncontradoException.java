package com.braga.carteiradigital.transacao.dominio;

import com.braga.carteiradigital.compartilhado.dominio.ErroDeNegocio;

public class DestinatarioNaoEncontradoException extends ErroDeNegocio {

    public DestinatarioNaoEncontradoException() {
        super(Tipo.REGRA_VIOLADA, "destinatario-nao-encontrado", "Nenhum usuário cadastrado com este e-mail.");
    }
}
