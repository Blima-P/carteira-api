package com.braga.carteiradigital.transacao.aplicacao.porta.saida;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;
import com.braga.carteiradigital.transacao.dominio.ItemDoExtrato;
import com.braga.carteiradigital.transacao.dominio.PosicaoNoExtrato;

/** O que o módulo Transação precisa do módulo Carteira, descrito com as palavras deste módulo. */
public interface MovimentadorDeCarteira {

    UUID idDaCarteiraDoUsuario(UUID usuarioId);

    /** Credita o valor e devolve o saldo atualizado da carteira. */
    Dinheiro creditar(UUID carteiraId, Dinheiro valor, UUID transacaoId);

    /** Move o valor entre as carteiras e devolve o saldo atualizado da origem. */
    Dinheiro transferir(UUID carteiraOrigemId, UUID carteiraDestinoId, Dinheiro valor, UUID transacaoId);

    /** Saldo da carteira logo após a transação, para refazer o comprovante de um reenvio. */
    Dinheiro saldoAposTransacao(UUID carteiraId, UUID transacaoId);

    /** Até {@code limite} lançamentos da carteira, do mais recente ao mais antigo, depois de {@code depoisDe} (se houver). */
    List<Lancamento> lancamentos(UUID carteiraId, PosicaoNoExtrato depoisDe, int limite);

    /** O id do dono de cada carteira, indexado pelo id da carteira. */
    Map<UUID, UUID> donosDasCarteiras(Set<UUID> carteiraIds);

    /** Um lançamento do livro-razão da carteira: o efeito de uma transação no saldo. */
    record Lancamento(PosicaoNoExtrato posicao, UUID transacaoId, ItemDoExtrato.Natureza natureza, Dinheiro valor,
            Dinheiro saldoApos) {
    }
}
