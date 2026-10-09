package com.braga.carteiradigital.transacao.aplicacao;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;
import com.braga.carteiradigital.transacao.aplicacao.porta.entrada.DepositarUseCase;
import com.braga.carteiradigital.transacao.aplicacao.porta.saida.MovimentadorDeCarteira;
import com.braga.carteiradigital.transacao.aplicacao.porta.saida.TransacaoRepository;
import com.braga.carteiradigital.transacao.dominio.ChaveIdempotenciaJaUtilizadaException;
import com.braga.carteiradigital.transacao.dominio.Comprovante;
import com.braga.carteiradigital.transacao.dominio.Transacao;

@Service
class DepositarService implements DepositarUseCase {

    private final TransacaoRepository transacoes;
    private final MovimentadorDeCarteira carteiras;
    private final Clock relogio;

    DepositarService(TransacaoRepository transacoes, MovimentadorDeCarteira carteiras, Clock relogio) {
        this.transacoes = transacoes;
        this.carteiras = carteiras;
        this.relogio = relogio;
    }

    /**
     * Uma única transação de banco: registro da transação, novo saldo e lançamento.
     * Se qualquer passo falhar, o rollback desfaz todos.
     */
    @Override
    @Transactional
    public Comprovante depositar(Comando comando) {
        // Verificação rápida. Duas requisições simultâneas com a mesma chave passam daqui, mas a
        // constraint uk_transacoes_idempotencia barra a segunda (o repositório converte para a mesma exceção).
        if (transacoes.existePorChave(comando.usuarioId(), comando.chaveIdempotencia())) {
            throw new ChaveIdempotenciaJaUtilizadaException();
        }

        UUID carteiraId = carteiras.idDaCarteiraDoUsuario(comando.usuarioId());
        Transacao deposito = Transacao.deposito(carteiraId, comando.valor(), comando.descricao(),
                comando.usuarioId(), comando.chaveIdempotencia(), relogio);

        // A transação precisa ser gravada antes do lançamento, que a referencia por chave estrangeira
        transacoes.salvar(deposito);
        Dinheiro saldoAtual = carteiras.creditar(carteiraId, deposito.valor(), deposito.id());

        return Comprovante.de(deposito, saldoAtual);
    }
}
