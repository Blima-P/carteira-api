package com.braga.carteiradigital.carteira.adaptadores.saida.persistencia;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface LancamentoJpaRepository extends JpaRepository<LancamentoEntity, Long> {

    @Query("select l.saldoApos from LancamentoEntity l where l.carteiraId = :carteiraId and l.transacaoId = :transacaoId")
    Optional<BigDecimal> buscarSaldoApos(UUID carteiraId, UUID transacaoId);
}
