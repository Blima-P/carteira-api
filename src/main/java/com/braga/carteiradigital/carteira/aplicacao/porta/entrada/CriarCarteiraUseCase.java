package com.braga.carteiradigital.carteira.aplicacao.porta.entrada;

import java.util.UUID;

import com.braga.carteiradigital.carteira.dominio.Carteira;

public interface CriarCarteiraUseCase {

    /** Idempotente: se o usuário já tiver carteira, devolve a existente. */
    Carteira criarPara(UUID usuarioId);
}
