package com.braga.carteiradigital.transacao.aplicacao.porta.saida;

import java.util.UUID;

import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;

/** O que o módulo Transação precisa do módulo Carteira, descrito com as palavras deste módulo. */
public interface MovimentadorDeCarteira {

    UUID idDaCarteiraDoUsuario(UUID usuarioId);

    /** Credita o valor e devolve o saldo atualizado da carteira. */
    Dinheiro creditar(UUID carteiraId, Dinheiro valor, UUID transacaoId);
}
