package com.braga.carteiradigital.transacao.adaptadores.saida.persistencia;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface TransacaoJpaRepository extends JpaRepository<TransacaoEntity, UUID> {

    Optional<TransacaoEntity> findByIniciadaPorAndChaveIdempotencia(UUID iniciadaPor, String chaveIdempotencia);
}
