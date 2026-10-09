package com.braga.carteiradigital.transacao.aplicacao.porta.entrada;

import java.util.UUID;

import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;
import com.braga.carteiradigital.transacao.dominio.ChaveIdempotencia;
import com.braga.carteiradigital.transacao.dominio.Comprovante;

public interface TransferirUseCase {

    Comprovante transferir(Comando comando);

    record Comando(UUID usuarioId, String emailDestinatario, Dinheiro valor, String descricao,
            ChaveIdempotencia chaveIdempotencia) {
    }
}
