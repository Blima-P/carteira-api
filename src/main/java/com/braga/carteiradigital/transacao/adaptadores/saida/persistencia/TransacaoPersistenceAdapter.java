package com.braga.carteiradigital.transacao.adaptadores.saida.persistencia;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.braga.carteiradigital.transacao.aplicacao.porta.saida.TransacaoRepository;
import com.braga.carteiradigital.transacao.dominio.ChaveIdempotencia;
import com.braga.carteiradigital.transacao.dominio.ChaveIdempotenciaJaUtilizadaException;
import com.braga.carteiradigital.transacao.dominio.Transacao;

@Component
class TransacaoPersistenceAdapter implements TransacaoRepository {

    private final TransacaoJpaRepository jpa;

    TransacaoPersistenceAdapter(TransacaoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void salvar(Transacao transacao) {
        try {
            // flush imediato: grava antes dos lançamentos e verifica a constraint de idempotência agora
            jpa.saveAndFlush(TransacaoEntity.de(transacao));
        } catch (DataIntegrityViolationException erro) {
            String mensagem = erro.getMostSpecificCause().getMessage();
            if (mensagem != null && mensagem.contains("uk_transacoes_idempotencia")) {
                throw new ChaveIdempotenciaJaUtilizadaException();
            }
            throw erro;
        }
    }

    @Override
    public boolean existePorChave(UUID iniciadaPor, ChaveIdempotencia chave) {
        return jpa.existsByIniciadaPorAndChaveIdempotencia(iniciadaPor, chave.valor());
    }
}
