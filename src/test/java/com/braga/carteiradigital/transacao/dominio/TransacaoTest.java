package com.braga.carteiradigital.transacao.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;

class TransacaoTest {

    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneOffset.UTC);

    private final UUID carteiraId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();
    private final ChaveIdempotencia chave = new ChaveIdempotencia("chave-1");

    @Test
    void depositoNaoDeveTerCarteiraDeOrigem() {
        Transacao deposito = deposito("50.00", "Mesada");

        assertThat(deposito.tipo()).isEqualTo(TipoTransacao.DEPOSITO);
        assertThat(deposito.carteiraOrigemId()).isNull();
        assertThat(deposito.carteiraDestinoId()).isEqualTo(carteiraId);
        assertThat(deposito.iniciadaPor()).isEqualTo(usuarioId);
        assertThat(deposito.criadoEm()).isEqualTo(Instant.parse("2026-10-01T12:00:00Z"));
    }

    @Test
    void hashDeveSerOMesmoParaOMesmoPedido() {
        assertThat(deposito("50.00", "Mesada").hashRequisicao())
                .hasSize(64)
                .isEqualTo(deposito("50.0", "  Mesada ").hashRequisicao());
    }

    @Test
    void hashDeveMudarQuandoOPedidoMuda() {
        String original = deposito("50.00", "Mesada").hashRequisicao();

        assertThat(deposito("50.01", "Mesada").hashRequisicao()).isNotEqualTo(original);
        assertThat(deposito("50.00", "Outra").hashRequisicao()).isNotEqualTo(original);
    }

    @Test
    void descricaoEmBrancoDeveVirarNula() {
        assertThat(deposito("50.00", "   ").descricao()).isNull();
        assertThat(deposito("50.00", null).descricao()).isNull();
    }

    @Test
    void deveRecusarDescricaoLonga() {
        assertThatThrownBy(() -> deposito("50.00", "x".repeat(141)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRecusarValorZero() {
        assertThatThrownBy(() -> deposito("0.00", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transferenciaNaoPodeTerOrigemIgualAoDestino() {
        assertThatThrownBy(() -> new Transacao(UUID.randomUUID(), TipoTransacao.TRANSFERENCIA, Dinheiro.de("1.00"),
                carteiraId, carteiraId, null, usuarioId, chave, "hash", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "com espaco", "acentuação", "a/b" })
    void chaveDeIdempotenciaDeveRecusarFormatoInvalido(String valor) {
        assertThatThrownBy(() -> new ChaveIdempotencia(valor)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void chaveDeIdempotenciaDeveAceitarUuidERecusarMaisDe64Caracteres() {
        assertThat(new ChaveIdempotencia(UUID.randomUUID().toString()).valor()).hasSize(36);
        assertThatThrownBy(() -> new ChaveIdempotencia("a".repeat(65))).isInstanceOf(IllegalArgumentException.class);
    }

    private Transacao deposito(String valor, String descricao) {
        return Transacao.deposito(carteiraId, Dinheiro.de(valor), descricao, usuarioId, chave, RELOGIO);
    }
}
