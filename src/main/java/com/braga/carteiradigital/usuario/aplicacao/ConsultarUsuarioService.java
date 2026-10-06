package com.braga.carteiradigital.usuario.aplicacao;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.braga.carteiradigital.usuario.aplicacao.porta.entrada.ConsultarUsuarioUseCase;
import com.braga.carteiradigital.usuario.aplicacao.porta.saida.UsuarioRepository;
import com.braga.carteiradigital.usuario.dominio.Usuario;
import com.braga.carteiradigital.usuario.dominio.UsuarioNaoEncontradoException;

@Service
class ConsultarUsuarioService implements ConsultarUsuarioUseCase {

    private final UsuarioRepository usuarios;

    ConsultarUsuarioService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario buscarPorId(UUID id) {
        return usuarios.buscarPorId(id).orElseThrow(UsuarioNaoEncontradoException::new);
    }
}
