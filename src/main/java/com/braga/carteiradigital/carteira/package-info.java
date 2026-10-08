/**
 * <b>Módulo Carteira</b>: saldo, livro-razão (lançamentos de crédito/débito) e extrato.
 *
 * <p>É o único módulo que altera saldos. Toda alteração gera um lançamento imutável.
 * Outros módulos usam a API pública {@link com.braga.carteiradigital.carteira.CarteiraApi}.
 *
 * <p>Tabelas: {@code carteiras}, {@code lancamentos}.
 */
@ApplicationModule(displayName = "Carteira")
package com.braga.carteiradigital.carteira;

import org.springframework.modulith.ApplicationModule;
