package com.braga.carteiradigital.carteira.adaptadores.saida.persistencia;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

interface CarteiraJpaRepository extends JpaRepository<CarteiraEntity, UUID> {

    Optional<CarteiraEntity> findByUsuarioId(UUID usuarioId);

    @Query("select c.id from CarteiraEntity c where c.usuarioId = :usuarioId")
    Optional<UUID> buscarIdPorUsuario(UUID usuarioId);

    /** Gera {@code SELECT ... FOR UPDATE} no PostgreSQL. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CarteiraEntity c where c.id = :id")
    Optional<CarteiraEntity> buscarPorIdComBloqueio(UUID id);
}
