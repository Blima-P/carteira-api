package com.braga.carteiradigital.usuario.adaptadores.saida.persistencia;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, UUID> {

    boolean existsByEmail(String email);

    Optional<UsuarioEntity> findByEmail(String email);
}
