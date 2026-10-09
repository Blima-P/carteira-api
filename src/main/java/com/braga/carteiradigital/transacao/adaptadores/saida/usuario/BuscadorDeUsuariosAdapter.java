package com.braga.carteiradigital.transacao.adaptadores.saida.usuario;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.braga.carteiradigital.transacao.aplicacao.porta.saida.BuscadorDeUsuarios;
import com.braga.carteiradigital.usuario.UsuarioApi;

/** Liga a porta {@link BuscadorDeUsuarios} à API pública do módulo Usuário. */
@Component
class BuscadorDeUsuariosAdapter implements BuscadorDeUsuarios {

    private final UsuarioApi usuarioApi;

    BuscadorDeUsuariosAdapter(UsuarioApi usuarioApi) {
        this.usuarioApi = usuarioApi;
    }

    @Override
    public Optional<UUID> idDoUsuarioPorEmail(String email) {
        return usuarioApi.idDoUsuarioPorEmail(email);
    }
}
