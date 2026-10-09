package com.braga.carteiradigital.transacao.adaptadores.saida.carteira;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.braga.carteiradigital.carteira.CarteiraApi;
import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;
import com.braga.carteiradigital.transacao.aplicacao.porta.saida.MovimentadorDeCarteira;

/**
 * Liga a porta {@link MovimentadorDeCarteira} à API pública do módulo Carteira.
 * A aplicação deste módulo não conhece o módulo Carteira; só este adaptador conhece.
 */
@Component
class MovimentadorDeCarteiraAdapter implements MovimentadorDeCarteira {

    private final CarteiraApi carteiraApi;

    MovimentadorDeCarteiraAdapter(CarteiraApi carteiraApi) {
        this.carteiraApi = carteiraApi;
    }

    @Override
    public UUID idDaCarteiraDoUsuario(UUID usuarioId) {
        return carteiraApi.idDaCarteiraDoUsuario(usuarioId);
    }

    @Override
    public Dinheiro creditar(UUID carteiraId, Dinheiro valor, UUID transacaoId) {
        return carteiraApi.creditar(carteiraId, valor, transacaoId);
    }

    @Override
    public Dinheiro transferir(UUID carteiraOrigemId, UUID carteiraDestinoId, Dinheiro valor, UUID transacaoId) {
        return carteiraApi.transferir(carteiraOrigemId, carteiraDestinoId, valor, transacaoId);
    }
}
