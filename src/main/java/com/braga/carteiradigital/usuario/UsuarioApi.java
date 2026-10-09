package com.braga.carteiradigital.usuario;

import java.util.Optional;
import java.util.UUID;

/** API pública do módulo Usuário para os outros módulos. */
public interface UsuarioApi {

    /**
     * Id do usuário dono do e-mail (sem diferenciar maiúsculas de minúsculas).
     * Vazio se nenhum usuário tiver esse e-mail ou se o e-mail for inválido.
     */
    Optional<UUID> idDoUsuarioPorEmail(String email);
}
