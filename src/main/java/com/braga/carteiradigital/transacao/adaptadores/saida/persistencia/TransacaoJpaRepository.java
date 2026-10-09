package com.braga.carteiradigital.transacao.adaptadores.saida.persistencia;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface TransacaoJpaRepository extends JpaRepository<TransacaoEntity, UUID> {

    boolean existsByIniciadaPorAndChaveIdempotencia(UUID iniciadaPor, String chaveIdempotencia);
}
