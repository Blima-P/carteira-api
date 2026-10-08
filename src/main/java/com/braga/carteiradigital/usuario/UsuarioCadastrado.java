package com.braga.carteiradigital.usuario;

import java.util.UUID;

/**
 * Evento publicado quando um novo usuário se cadastra.
 *
 * <p>Faz parte da API pública do módulo (pacote base): outros módulos podem reagir a ele
 * sem conhecer as classes internas de {@code usuario}. Ex.: o módulo {@code carteira}
 * criará a carteira do usuário ao receber este evento.
 */
public record UsuarioCadastrado(UUID usuarioId, String nome, String email) {
}
