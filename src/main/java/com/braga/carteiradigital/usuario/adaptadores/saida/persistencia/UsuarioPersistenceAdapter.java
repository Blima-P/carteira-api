package com.braga.carteiradigital.usuario.adaptadores.saida.persistencia;

import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.braga.carteiradigital.usuario.aplicacao.porta.saida.UsuarioRepository;
import com.braga.carteiradigital.usuario.dominio.Email;
import com.braga.carteiradigital.usuario.dominio.EmailJaCadastradoException;
import com.braga.carteiradigital.usuario.dominio.Usuario;

/** Implementa a porta {@link UsuarioRepository} usando JPA + PostgreSQL. */
@Component
class UsuarioPersistenceAdapter implements UsuarioRepository {

    private final UsuarioJpaRepository jpa;

    UsuarioPersistenceAdapter(UsuarioJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void salvar(Usuario usuario) {
        try {
            // flush imediato para a constraint do banco ser verificada aqui, e não só no commit
            jpa.saveAndFlush(UsuarioEntity.de(usuario));
        } catch (DataIntegrityViolationException erro) {
            if (violou(erro, "uk_usuarios_email")) {
                throw new EmailJaCadastradoException();
            }
            throw erro;
        }
    }

    @Override
    public boolean existePorEmail(Email email) {
        return jpa.existsByEmail(email.valor());
    }

    @Override
    public Optional<Usuario> buscarPorEmail(Email email) {
        return jpa.findByEmail(email.valor()).map(UsuarioEntity::paraDominio);
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        return jpa.findById(id).map(UsuarioEntity::paraDominio);
    }

    private static boolean violou(DataIntegrityViolationException erro, String constraint) {
        String mensagem = erro.getMostSpecificCause().getMessage();
        return mensagem != null && mensagem.contains(constraint);
    }
}
