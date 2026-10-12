package com.braga.carteiradigital.transacao.aplicacao;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.braga.carteiradigital.transacao.aplicacao.porta.entrada.TransferirUseCase;
import com.braga.carteiradigital.transacao.aplicacao.porta.saida.BuscadorDeUsuarios;
import com.braga.carteiradigital.transacao.aplicacao.porta.saida.MovimentadorDeCarteira;
import com.braga.carteiradigital.transacao.dominio.Comprovante;
import com.braga.carteiradigital.transacao.dominio.DestinatarioNaoEncontradoException;
import com.braga.carteiradigital.transacao.dominio.Transacao;
import com.braga.carteiradigital.transacao.dominio.TransferenciaParaSiMesmoException;

@Service
class TransferirService implements TransferirUseCase {

    private final BuscadorDeUsuarios usuarios;
    private final MovimentadorDeCarteira carteiras;
    private final ExecutorIdempotente idempotencia;
    private final Clock relogio;

    TransferirService(BuscadorDeUsuarios usuarios, MovimentadorDeCarteira carteiras, ExecutorIdempotente idempotencia,
            Clock relogio) {
        this.usuarios = usuarios;
        this.carteiras = carteiras;
        this.idempotencia = idempotencia;
        this.relogio = relogio;
    }

    /**
     * Uma única transação de banco: registro da transação, débito na origem, crédito no destino e
     * os dois lançamentos. Se qualquer passo falhar (inclusive por saldo insuficiente), nada é gravado.
     */
    @Override
    @Transactional
    public Comprovante transferir(Comando comando) {
        UUID destinatarioId = usuarios.idDoUsuarioPorEmail(comando.emailDestinatario())
                .orElseThrow(DestinatarioNaoEncontradoException::new);
        if (destinatarioId.equals(comando.usuarioId())) {
            throw new TransferenciaParaSiMesmoException();
        }

        // Só os ids: as carteiras são carregadas depois, já bloqueadas, pelo módulo Carteira
        UUID carteiraOrigemId = carteiras.idDaCarteiraDoUsuario(comando.usuarioId());
        UUID carteiraDestinoId = carteiras.idDaCarteiraDoUsuario(destinatarioId);
        Transacao transferencia = Transacao.transferencia(carteiraOrigemId, carteiraDestinoId, comando.valor(),
                comando.descricao(), comando.usuarioId(), comando.chaveIdempotencia(), relogio);

        return idempotencia.executar(transferencia,
                gravada -> carteiras.transferir(carteiraOrigemId, carteiraDestinoId, gravada.valor(), gravada.id()));
    }
}
