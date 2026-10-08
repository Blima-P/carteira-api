package com.braga.carteiradigital.transacao.aplicacao.porta.entrada;

import java.util.UUID;

import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;
import com.braga.carteiradigital.transacao.dominio.ChaveIdempotencia;
import com.braga.carteiradigital.transacao.dominio.Comprovante;

public interface DepositarUseCase {

    Comprovante depositar(Comando comando);

    record Comando(UUID usuarioId, Dinheiro valor, String descricao, ChaveIdempotencia chaveIdempotencia) {
    }
}
