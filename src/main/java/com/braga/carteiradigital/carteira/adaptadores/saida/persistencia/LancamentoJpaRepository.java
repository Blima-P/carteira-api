package com.braga.carteiradigital.carteira.adaptadores.saida.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

interface LancamentoJpaRepository extends JpaRepository<LancamentoEntity, Long> {
}
