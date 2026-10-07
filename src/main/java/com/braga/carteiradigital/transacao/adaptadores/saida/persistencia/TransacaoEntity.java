package com.braga.carteiradigital.transacao.adaptadores.saida.persistencia;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Immutable;
import org.springframework.data.domain.Persistable;

import com.braga.carteiradigital.transacao.dominio.TipoTransacao;
import com.braga.carteiradigital.transacao.dominio.Transacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Immutable
@Table(name = "transacoes")
class TransacaoEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoTransacao tipo;

    @Column(name = "valor", nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(name = "carteira_origem_id")
    private UUID carteiraOrigemId;

    @Column(name = "carteira_destino_id", nullable = false)
    private UUID carteiraDestinoId;

    @Column(name = "descricao", length = 140)
    private String descricao;

    @Column(name = "iniciada_por", nullable = false)
    private UUID iniciadaPor;

    @Column(name = "chave_idempotencia", nullable = false, length = 64)
    private String chaveIdempotencia;

    @Column(name = "hash_requisicao", nullable = false, length = 64)
    private String hashRequisicao;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Transient
    private boolean novo = true;

    protected TransacaoEntity() {
    }

    static TransacaoEntity de(Transacao transacao) {
        TransacaoEntity entidade = new TransacaoEntity();
        entidade.id = transacao.id();
        entidade.tipo = transacao.tipo();
        entidade.valor = transacao.valor().quantia();
        entidade.carteiraOrigemId = transacao.carteiraOrigemId();
        entidade.carteiraDestinoId = transacao.carteiraDestinoId();
        entidade.descricao = transacao.descricao();
        entidade.iniciadaPor = transacao.iniciadaPor();
        entidade.chaveIdempotencia = transacao.chaveIdempotencia().valor();
        entidade.hashRequisicao = transacao.hashRequisicao();
        entidade.criadoEm = transacao.criadoEm();
        return entidade;
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
