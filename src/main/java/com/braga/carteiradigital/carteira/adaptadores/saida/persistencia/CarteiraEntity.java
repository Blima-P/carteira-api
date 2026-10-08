package com.braga.carteiradigital.carteira.adaptadores.saida.persistencia;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Persistable;

import com.braga.carteiradigital.carteira.dominio.Carteira;
import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

@Entity
@Table(name = "carteiras")
class CarteiraEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(name = "saldo", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldo;

    // Rede de segurança: se alguém atualizar a carteira sem bloqueá-la antes, o Hibernate
    // detecta a escrita concorrente (UPDATE ... WHERE versao = ?) em vez de perder um valor
    @Version
    @Column(name = "versao", nullable = false)
    private long versao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Transient
    private boolean novo = true;

    protected CarteiraEntity() {
    }

    static CarteiraEntity de(Carteira carteira) {
        CarteiraEntity entidade = new CarteiraEntity();
        entidade.id = carteira.id();
        entidade.usuarioId = carteira.usuarioId();
        entidade.criadoEm = carteira.criadoEm();
        entidade.aplicar(carteira);
        return entidade;
    }

    void aplicar(Carteira carteira) {
        this.saldo = carteira.saldo().quantia();
        this.atualizadoEm = carteira.atualizadoEm();
    }

    Carteira paraDominio() {
        return new Carteira(id, usuarioId, new Dinheiro(saldo), criadoEm, atualizadoEm);
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
