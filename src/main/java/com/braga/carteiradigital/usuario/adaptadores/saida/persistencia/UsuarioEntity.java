package com.braga.carteiradigital.usuario.adaptadores.saida.persistencia;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Persistable;

import com.braga.carteiradigital.usuario.dominio.Email;
import com.braga.carteiradigital.usuario.dominio.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Representação da tabela {@code usuarios}. Fica no adaptador para que o domínio
 * ({@link Usuario}) não dependa de JPA.
 */
@Entity
@Table(name = "usuarios")
class UsuarioEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    // O id é gerado pela aplicação; sem isso o Spring Data faria um SELECT antes de cada INSERT
    @Transient
    private boolean novo = true;

    protected UsuarioEntity() {
    }

    static UsuarioEntity de(Usuario usuario) {
        UsuarioEntity entidade = new UsuarioEntity();
        entidade.id = usuario.id();
        entidade.nome = usuario.nome();
        entidade.email = usuario.email().valor();
        entidade.senhaHash = usuario.senhaHash();
        entidade.criadoEm = usuario.criadoEm();
        return entidade;
    }

    Usuario paraDominio() {
        return new Usuario(id, nome, new Email(email), senhaHash, criadoEm);
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return novo;
    }

    @PostLoad
    @PostPersist
    void marcarComoPersistido() {
        this.novo = false;
    }
}
