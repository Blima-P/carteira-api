package com.braga.carteiradigital.carteira;

import java.util.UUID;

import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;

/**
 * API pública do módulo Carteira: a única forma de outros módulos alterarem um saldo.
 *
 * <p>As operações exigem uma transação já aberta por quem chama, porque um crédito só faz
 * sentido junto da transação financeira que o originou (ou os dois são gravados, ou nenhum).
 */
public interface CarteiraApi {

    /** Lança um erro de negócio {@code carteira-nao-encontrada} (404) se o usuário não tiver carteira. */
    UUID idDaCarteiraDoUsuario(UUID usuarioId);

    /**
     * Soma {@code valor} ao saldo, registra o lançamento de crédito e devolve o novo saldo.
     * A carteira fica bloqueada até o fim da transação, então créditos simultâneos não se perdem.
     */
    Dinheiro creditar(UUID carteiraId, Dinheiro valor, UUID transacaoId);

    /**
     * Debita {@code valor} da origem e credita no destino, registrando os dois lançamentos,
     * e devolve o novo saldo da origem. As duas carteiras ficam bloqueadas até o fim da transação.
     *
     * <p>Lança um erro de negócio {@code saldo-insuficiente} (422) se a origem não tiver saldo.
     */
    Dinheiro transferir(UUID carteiraOrigemId, UUID carteiraDestinoId, Dinheiro valor, UUID transacaoId);
}
