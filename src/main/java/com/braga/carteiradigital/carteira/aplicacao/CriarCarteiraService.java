package com.braga.carteiradigital.carteira.aplicacao;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.braga.carteiradigital.carteira.aplicacao.porta.entrada.CriarCarteiraUseCase;
import com.braga.carteiradigital.carteira.aplicacao.porta.saida.CarteiraRepository;
import com.braga.carteiradigital.carteira.dominio.Carteira;

@Service
class CriarCarteiraService implements CriarCarteiraUseCase {

    private final CarteiraRepository carteiras;
    private final Clock relogio;

    CriarCarteiraService(CarteiraRepository carteiras, Clock relogio) {
        this.carteiras = carteiras;
        this.relogio = relogio;
    }

    @Override
    @Transactional
    public Carteira criarPara(UUID usuarioId) {
        return carteiras.buscarPorUsuario(usuarioId).orElseGet(() -> {
            Carteira carteira = Carteira.nova(usuarioId, relogio);
            carteiras.inserir(carteira);
            return carteira;
        });
    }
}
