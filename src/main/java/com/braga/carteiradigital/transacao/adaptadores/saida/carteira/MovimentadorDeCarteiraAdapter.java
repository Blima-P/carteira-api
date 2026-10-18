package com.braga.carteiradigital.transacao.adaptadores.saida.carteira;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.braga.carteiradigital.carteira.CarteiraApi;
import com.braga.carteiradigital.carteira.LancamentoDaCarteira;
import com.braga.carteiradigital.carteira.PosicaoNoLivroRazao;
import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;
import com.braga.carteiradigital.transacao.aplicacao.porta.saida.MovimentadorDeCarteira;
import com.braga.carteiradigital.transacao.dominio.ItemDoExtrato;
import com.braga.carteiradigital.transacao.dominio.PosicaoNoExtrato;

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

    @Override
    public Dinheiro saldoAposTransacao(UUID carteiraId, UUID transacaoId) {
        return carteiraApi.saldoAposTransacao(carteiraId, transacaoId);
    }

    @Override
    public List<Lancamento> lancamentos(UUID carteiraId, PosicaoNoExtrato depoisDe, int limite) {
        PosicaoNoLivroRazao posicao = depoisDe == null
                ? null
                : new PosicaoNoLivroRazao(depoisDe.criadoEm(), depoisDe.lancamentoId());
        return carteiraApi.lancamentos(carteiraId, posicao, limite).stream()
                .map(MovimentadorDeCarteiraAdapter::traduzir)
                .toList();
    }

    @Override
    public Map<UUID, UUID> donosDasCarteiras(Set<UUID> carteiraIds) {
        return carteiraApi.donosDasCarteiras(carteiraIds);
    }

    private static Lancamento traduzir(LancamentoDaCarteira lancamento) {
        var posicao = new PosicaoNoExtrato(lancamento.posicao().criadoEm(), lancamento.posicao().lancamentoId());
        var natureza = ItemDoExtrato.Natureza.valueOf(lancamento.natureza().name());
        return new Lancamento(posicao, lancamento.transacaoId(), natureza, lancamento.valor(), lancamento.saldoApos());
    }
}
