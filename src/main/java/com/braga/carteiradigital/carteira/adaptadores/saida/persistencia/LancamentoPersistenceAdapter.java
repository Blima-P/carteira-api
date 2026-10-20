package com.braga.carteiradigital.carteira.adaptadores.saida.persistencia;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

import com.braga.carteiradigital.carteira.LancamentoDaCarteira;
import com.braga.carteiradigital.carteira.PosicaoNoLivroRazao;
import com.braga.carteiradigital.carteira.aplicacao.porta.saida.LancamentoRepository;
import com.braga.carteiradigital.carteira.dominio.Lancamento;
import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;

@Component
class LancamentoPersistenceAdapter implements LancamentoRepository {

    private final LancamentoJpaRepository jpa;

    LancamentoPersistenceAdapter(LancamentoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void registrar(Lancamento lancamento) {
        jpa.save(LancamentoEntity.de(lancamento));
    }

    @Override
    public Optional<Dinheiro> buscarSaldoApos(UUID carteiraId, UUID transacaoId) {
        return jpa.buscarSaldoApos(carteiraId, transacaoId).map(Dinheiro::new);
    }

    @Override
    public List<LancamentoDaCarteira> buscarPagina(UUID carteiraId, PosicaoNoLivroRazao depoisDe, int limite) {
        // Duas consultas separadas em vez de uma com parâmetros opcionais: o PostgreSQL não consegue
        // inferir o tipo de um parâmetro nulo, e cada consulta usa o índice da forma mais simples
        List<LancamentoEntity> entidades = depoisDe == null
                ? jpa.buscarMaisRecentes(carteiraId, Limit.of(limite))
                : jpa.buscarAnterioresA(carteiraId, depoisDe.criadoEm(), depoisDe.lancamentoId(), Limit.of(limite));
        return entidades.stream().map(LancamentoEntity::paraLeitura).toList();
    }
}
