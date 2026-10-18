package com.braga.carteiradigital.carteira.adaptadores.saida.persistencia;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface LancamentoJpaRepository extends JpaRepository<LancamentoEntity, Long> {

    @Query("select l.saldoApos from LancamentoEntity l where l.carteiraId = :carteiraId and l.transacaoId = :transacaoId")
    Optional<BigDecimal> buscarSaldoApos(UUID carteiraId, UUID transacaoId);

    // As duas consultas seguem a ordem do índice idx_lancamentos_carteira_criado_em (carteira_id, criado_em DESC, id DESC)

    @Query("select l from LancamentoEntity l where l.carteiraId = :carteiraId order by l.criadoEm desc, l.id desc")
    List<LancamentoEntity> buscarMaisRecentes(UUID carteiraId, Limit limite);

    /** Paginação por posição (keyset): só os lançamentos que vêm depois de (criadoEm, id) na ordem decrescente. */
    @Query("""
            select l from LancamentoEntity l
            where l.carteiraId = :carteiraId
              and (l.criadoEm < :criadoEm or (l.criadoEm = :criadoEm and l.id < :id))
            order by l.criadoEm desc, l.id desc""")
    List<LancamentoEntity> buscarAnterioresA(UUID carteiraId, Instant criadoEm, long id, Limit limite);
}
