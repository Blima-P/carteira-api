package com.braga.carteiradigital.transacao.adaptadores.entrada.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/**
 * Valida o parâmetro {@code cursor} do extrato: ausente (primeira página) ou um valor devolvido
 * em {@code proximoCursor}. Um cursor inválido vira {@code dados-invalidos} (400), igual aos outros campos.
 */
@Constraint(validatedBy = CursorValido.Validador.class)
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface CursorValido {

    String message() default "inválido; use o valor de proximoCursor da página anterior";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<CursorValido, String> {

        @Override
        public boolean isValid(String cursor, ConstraintValidatorContext contexto) {
            if (cursor == null) {
                return true;
            }
            try {
                CursorDoExtrato.decodificar(cursor);
                return true;
            } catch (IllegalArgumentException cursorInvalido) {
                return false;
            }
        }
    }
}
