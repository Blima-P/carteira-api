package com.braga.carteiradigital.transacao.aplicacao.porta.saida;

import java.util.Optional;
import java.util.UUID;

/** O que o módulo Transação precisa saber sobre usuários. */
public interface BuscadorDeUsuarios {

    Optional<UUID> idDoUsuarioPorEmail(String email);
}
