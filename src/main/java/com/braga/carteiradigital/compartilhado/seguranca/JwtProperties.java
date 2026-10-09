package com.braga.carteiradigital.compartilhado.seguranca;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Configuração dos tokens JWT ({@code seguranca.jwt.*}).
 * A aplicação não sobe se o segredo estiver ausente ou curto demais.
 */
@Validated
@ConfigurationProperties("seguranca.jwt")
public record JwtProperties(
        @NotBlank(message = "defina a variável de ambiente JWT_SECRET")
        @Size(min = 32, message = "deve ter no mínimo 32 caracteres (256 bits) para HS256")
        String segredo,
        @NotBlank String emissor,
        @NotNull Duration expiracao) {
}
