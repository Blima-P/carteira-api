package com.braga.carteiradigital.transacao.aplicacao;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.braga.carteiradigital.transacao.aplicacao.porta.entrada.DepositarUseCase;
import com.braga.carteiradigital.transacao.aplicacao.porta.saida.MovimentadorDeCarteira;
import com.braga.carteiradigital.transacao.dominio.Comprovante;
import com.braga.carteiradigital.transacao.dominio.Transacao;

@Service
class DepositarService implements DepositarUseCase {

    private final MovimentadorDeCarteira carteiras;
    private final ExecutorIdempotente idempotencia;
    private final Clock relogio;

    DepositarService(MovimentadorDeCarteira carteiras, ExecutorIdempotente idempotencia, Clock relogio) {
        this.carteiras = carteiras;
        this.idempotencia = idempotencia;
        this.relogio = relogio;
    }

    /**
     * Uma única transação de banco: registro da transação, novo saldo e lançamento.
     * Se qualquer passo falhar, o rollback desfaz todos.
     */
    @Override
    @Transactional
    public Comprovante depositar(Comando comando) {
        UUID carteiraId = carteiras.idDaCarteiraDoUsuario(comando.usuarioId());
        Transacao deposito = Transacao.deposito(carteiraId, comando.valor(), comando.descricao(),
                comando.usuarioId(), comando.chaveIdempotencia(), relogio);

        return idempotencia.executar(deposito,
                gravado -> carteiras.creditar(carteiraId, gravado.valor(), gravado.id()));
    }
}
