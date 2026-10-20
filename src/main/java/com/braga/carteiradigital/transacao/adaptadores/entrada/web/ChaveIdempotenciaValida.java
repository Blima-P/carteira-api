package com.braga.carteiradigital.transacao.adaptadores.entrada.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.braga.carteiradigital.transacao.dominio.ChaveIdempotencia;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Valida o cabeçalho {@code Idempotency-Key}: obrigatório e no formato de {@link ChaveIdempotencia}.
 * Agrupa as duas validações para não repeti-las em cada endpoint financeiro.
 */
@NotBlank(message = "é obrigatório")
@Pattern(regexp = ChaveIdempotencia.FORMATO,
        message = "deve ter de 1 a 64 caracteres entre letras, números, '-' e '_'")
@Constraint(validatedBy = {})
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface ChaveIdempotenciaValida {

    String message() default "inválida";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
