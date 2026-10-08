package com.braga.carteiradigital.carteira.adaptadores.saida.persistencia;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.Immutable;

import com.braga.carteiradigital.carteira.dominio.Lancamento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** {@code @Immutable}: o Hibernate nunca gera UPDATE para esta entidade (o banco também proíbe, via trigger). */
@Entity
@Immutable
@Table(name = "lancamentos")
class LancamentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transacao_id", nullable = false)
    private UUID transacaoId;

    @Column(name = "carteira_id", nullable = false)
    private UUID carteiraId;

    @Enumerated(EnumType.STRING)
    @Column(name = "natureza", nullable = false, length = 7)
    private Lancamento.Natureza natureza;

    @Column(name = "valor", nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(name = "saldo_apos", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoApos;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected LancamentoEntity() {
    }

    static LancamentoEntity de(Lancamento lancamento) {
        LancamentoEntity entidade = new LancamentoEntity();
        entidade.transacaoId = lancamento.transacaoId();
        entidade.carteiraId = lancamento.carteiraId();
        entidade.natureza = lancamento.natureza();
        entidade.valor = lancamento.valor().quantia();
        entidade.saldoApos = lancamento.saldoApos().quantia();
        entidade.criadoEm = lancamento.criadoEm();
        return entidade;
    }
}
