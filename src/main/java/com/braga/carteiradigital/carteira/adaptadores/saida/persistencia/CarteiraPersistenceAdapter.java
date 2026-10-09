package com.braga.carteiradigital.carteira.adaptadores.saida.persistencia;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.braga.carteiradigital.carteira.aplicacao.porta.saida.CarteiraRepository;
import com.braga.carteiradigital.carteira.dominio.Carteira;
import com.braga.carteiradigital.carteira.dominio.CarteiraNaoEncontradaException;

@Component
class CarteiraPersistenceAdapter implements CarteiraRepository {

    private final CarteiraJpaRepository jpa;

    CarteiraPersistenceAdapter(CarteiraJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void inserir(Carteira carteira) {
        jpa.saveAndFlush(CarteiraEntity.de(carteira));
    }

    @Override
    public void atualizar(Carteira carteira) {
        // Dentro da transação, findById devolve a entidade já carregada (e bloqueada), sem novo SELECT
        CarteiraEntity entidade = jpa.findById(carteira.id()).orElseThrow(CarteiraNaoEncontradaException::new);
        entidade.aplicar(carteira);
        jpa.flush();
    }

    @Override
    public Optional<Carteira> buscarPorUsuario(UUID usuarioId) {
        return jpa.findByUsuarioId(usuarioId).map(CarteiraEntity::paraDominio);
    }

    @Override
    public Optional<UUID> buscarIdPorUsuario(UUID usuarioId) {
        return jpa.buscarIdPorUsuario(usuarioId);
    }

    @Override
    public Optional<Carteira> buscarPorIdComBloqueio(UUID id) {
        return jpa.buscarPorIdComBloqueio(id).map(CarteiraEntity::paraDominio);
    }
}
