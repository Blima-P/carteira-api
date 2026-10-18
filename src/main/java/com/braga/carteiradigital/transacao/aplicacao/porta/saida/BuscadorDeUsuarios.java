package com.braga.carteiradigital.transacao.aplicacao.porta.saida;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** O que o módulo Transação precisa saber sobre usuários. */
public interface BuscadorDeUsuarios {

    Optional<UUID> idDoUsuarioPorEmail(String email);

    /** O nome de cada usuário, indexado pelo id. */
    Map<UUID, String> nomesPorId(Set<UUID> ids);
}
