package com.braga.carteiradigital.carteira.aplicacao;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.braga.carteiradigital.carteira.aplicacao.porta.entrada.ConsultarCarteiraUseCase;
import com.braga.carteiradigital.carteira.aplicacao.porta.saida.CarteiraRepository;
import com.braga.carteiradigital.carteira.dominio.Carteira;
import com.braga.carteiradigital.carteira.dominio.CarteiraNaoEncontradaException;

@Service
class ConsultarCarteiraService implements ConsultarCarteiraUseCase {

    private final CarteiraRepository carteiras;

    ConsultarCarteiraService(CarteiraRepository carteiras) {
        this.carteiras = carteiras;
    }

    @Override
    @Transactional(readOnly = true)
    public Carteira buscarDoUsuario(UUID usuarioId) {
        return carteiras.buscarPorUsuario(usuarioId).orElseThrow(CarteiraNaoEncontradaException::new);
    }
}
