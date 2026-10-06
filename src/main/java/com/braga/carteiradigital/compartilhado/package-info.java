/**
 * <b>Módulo Compartilhado</b>: infraestrutura transversal usada por todos os módulos
 * (segurança, tratamento de erros e o tipo base de erro de negócio).
 *
 * <p>É um módulo {@code OPEN}: os outros módulos podem usar seus subpacotes.
 * Não deve conter regra de negócio nem depender de nenhum outro módulo.
 */
@ApplicationModule(displayName = "Compartilhado", type = ApplicationModule.Type.OPEN)
package com.braga.carteiradigital.compartilhado;

import org.springframework.modulith.ApplicationModule;
