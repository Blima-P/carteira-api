package com.braga.carteiradigital.transacao.aplicacao;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;
import com.braga.carteiradigital.transacao.aplicacao.porta.entrada.TransferirUseCase;
import com.braga.carteiradigital.transacao.aplicacao.porta.saida.BuscadorDeUsuarios;
import com.braga.carteiradigital.transacao.aplicacao.porta.saida.MovimentadorDeCarteira;
import com.braga.carteiradigital.transacao.aplicacao.porta.saida.TransacaoRepository;
import com.braga.carteiradigital.transacao.dominio.ChaveIdempotenciaJaUtilizadaException;
import com.braga.carteiradigital.transacao.dominio.Comprovante;
import com.braga.carteiradigital.transacao.dominio.DestinatarioNaoEncontradoException;
import com.braga.carteiradigital.transacao.dominio.Transacao;
import com.braga.carteiradigital.transacao.dominio.TransferenciaParaSiMesmoException;

@Service
class TransferirService implements TransferirUseCase {

    private final TransacaoRepository transacoes;
    private final BuscadorDeUsuarios usuarios;
    private final MovimentadorDeCarteira carteiras;
    private final Clock relogio;

    TransferirService(TransacaoRepository transacoes, BuscadorDeUsuarios usuarios, MovimentadorDeCarteira carteiras,
            Clock relogio) {
        this.transacoes = transacoes;
        this.usuarios = usuarios;
        this.carteiras = carteiras;
        this.relogio = relogio;
    }

    /**
     * Uma única transação de banco: registro da transação, débito na origem, crédito no destino e
     * os dois lançamentos. Se qualquer passo falhar (inclusive por saldo insuficiente), nada é gravado.
     */
    @Override
    @Transactional
    public Comprovante transferir(Comando comando) {
        if (transacoes.existePorChave(comando.usuarioId(), comando.chaveIdempotencia())) {
            throw new ChaveIdempotenciaJaUtilizadaException();
        }

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

        // A transação precisa ser gravada antes dos lançamentos, que a referenciam por chave estrangeira
        transacoes.salvar(transferencia);
        Dinheiro saldoAtual = carteiras.transferir(carteiraOrigemId, carteiraDestinoId, transferencia.valor(),
                transferencia.id());

        return Comprovante.de(transferencia, saldoAtual);
    }
}
