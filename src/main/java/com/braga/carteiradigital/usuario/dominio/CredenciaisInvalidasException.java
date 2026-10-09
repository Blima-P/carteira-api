package com.braga.carteiradigital.usuario.dominio;

import com.braga.carteiradigital.compartilhado.dominio.ErroDeNegocio;

/**
 * Mesma mensagem para e-mail inexistente e senha errada: não revela quais e-mails estão cadastrados.
 */
public class CredenciaisInvalidasException extends ErroDeNegocio {

    public CredenciaisInvalidasException() {
        super(Tipo.NAO_AUTORIZADO, "credenciais-invalidas", "E-mail ou senha inválidos.");
    }
}
