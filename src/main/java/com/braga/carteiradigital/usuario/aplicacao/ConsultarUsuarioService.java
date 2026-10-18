package com.braga.carteiradigital.usuario.aplicacao;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.braga.carteiradigital.usuario.UsuarioApi;
import com.braga.carteiradigital.usuario.aplicacao.porta.entrada.ConsultarUsuarioUseCase;
import com.braga.carteiradigital.usuario.aplicacao.porta.saida.UsuarioRepository;
import com.braga.carteiradigital.usuario.dominio.Email;
import com.braga.carteiradigital.usuario.dominio.Usuario;
import com.braga.carteiradigital.usuario.dominio.UsuarioNaoEncontradoException;

@Service
class ConsultarUsuarioService implements ConsultarUsuarioUseCase, UsuarioApi {

    private final UsuarioRepository usuarios;

    ConsultarUsuarioService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario buscarPorId(UUID id) {
        return usuarios.buscarPorId(id).orElseThrow(UsuarioNaoEncontradoException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> idDoUsuarioPorEmail(String email) {
        Email emailNormalizado;
        try {
            emailNormalizado = new Email(email);
        } catch (IllegalArgumentException | NullPointerException emailInvalido) {
            return Optional.empty();
        }
        return usuarios.buscarPorEmail(emailNormalizado).map(Usuario::id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> nomesPorId(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return usuarios.buscarPorIds(ids).stream().collect(Collectors.toMap(Usuario::id, Usuario::nome));
    }
}
