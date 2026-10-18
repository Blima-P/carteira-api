package com.braga.carteiradigital.carteira;

import java.util.Objects;
import java.util.UUID;

import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;

/** Um lançamento do livro-razão, como os outros módulos o enxergam (somente leitura). */
public record LancamentoDaCarteira(PosicaoNoLivroRazao posicao, UUID transacaoId, Natureza natureza, Dinheiro valor,
        Dinheiro saldoApos) {

    public enum Natureza {
        CREDITO,
        DEBITO
    }

    public LancamentoDaCarteira {
        Objects.requireNonNull(posicao, "posição é obrigatória");
        Objects.requireNonNull(transacaoId, "transação é obrigatória");
        Objects.requireNonNull(natureza, "natureza é obrigatória");
        Objects.requireNonNull(valor, "valor é obrigatório");
        Objects.requireNonNull(saldoApos, "saldo após o lançamento é obrigatório");
    }
}
