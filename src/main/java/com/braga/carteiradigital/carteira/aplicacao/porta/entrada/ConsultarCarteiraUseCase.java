package com.braga.carteiradigital.carteira.aplicacao.porta.entrada;

import java.util.UUID;

import com.braga.carteiradigital.carteira.dominio.Carteira;

public interface ConsultarCarteiraUseCase {

    Carteira buscarDoUsuario(UUID usuarioId);
}
