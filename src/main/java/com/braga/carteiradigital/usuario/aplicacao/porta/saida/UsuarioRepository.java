package com.braga.carteiradigital.usuario.aplicacao.porta.saida;

import java.util.Optional;
import java.util.UUID;

import com.braga.carteiradigital.usuario.dominio.Email;
import com.braga.carteiradigital.usuario.dominio.Usuario;

/** Porta de saída: o caso de uso não sabe se os usuários estão no PostgreSQL, em memória ou em outro lugar. */
public interface UsuarioRepository {

    /** @throws com.braga.carteiradigital.usuario.dominio.EmailJaCadastradoException se o e-mail já existir */
    void salvar(Usuario usuario);

    boolean existePorEmail(Email email);

    Optional<Usuario> buscarPorEmail(Email email);

    Optional<Usuario> buscarPorId(UUID id);
}
