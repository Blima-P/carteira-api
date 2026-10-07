package com.braga.carteiradigital.carteira;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.IllegalTransactionStateException;

import com.braga.carteiradigital.carteira.aplicacao.porta.entrada.CriarCarteiraUseCase;
import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;
import com.braga.carteiradigital.suporte.IntegracaoTestBase;

class CarteiraIntegracaoTest extends IntegracaoTestBase {

    @Autowired
    private CriarCarteiraUseCase criarCarteira;

    @Autowired
    private CarteiraApi carteiraApi;

    @Test
    void cadastroDeveCriarCarteiraComSaldoZero() {
        String token = novoUsuarioComToken("ana@email.com");

        assertThat(mvc.get().uri("/api/carteiras/minha").header("Authorization", "Bearer " + token))
                .hasStatusOk()
                .bodyText().contains("\"saldo\":0.00");

        UUID usuarioId = jdbc.queryForObject("SELECT id FROM usuarios", UUID.class);
        assertThat(jdbc.queryForObject("SELECT saldo FROM carteiras WHERE usuario_id = ?", BigDecimal.class, usuarioId))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void cadaUsuarioDeveVerApenasASuaCarteira() {
        String tokenAna = novoUsuarioComToken("ana@email.com");
        String tokenBruno = novoUsuarioComToken("bruno@email.com");

        String idDaAna = idDaCarteira(tokenAna);
        String idDoBruno = idDaCarteira(tokenBruno);

        assertThat(idDaAna).isNotEqualTo(idDoBruno);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM carteiras", Integer.class)).isEqualTo(2);
    }

    @Test
    void criacaoDeCarteiraDeveSerIdempotente() {
        novoUsuarioComToken("ana@email.com");
        UUID usuarioId = jdbc.queryForObject("SELECT id FROM usuarios", UUID.class);

        // Simula o evento sendo entregue de novo: não pode surgir uma segunda carteira
        UUID existente = jdbc.queryForObject("SELECT id FROM carteiras", UUID.class);
        assertThat(criarCarteira.criarPara(usuarioId).id()).isEqualTo(existente);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM carteiras", Integer.class)).isEqualTo(1);
    }

    @Test
    void creditoForaDeUmaTransacaoDeveSerRecusado() {
        novoUsuarioComToken("ana@email.com");
        UUID carteiraId = jdbc.queryForObject("SELECT id FROM carteiras", UUID.class);

        // Propagation.MANDATORY: um crédito solto, sem a transação financeira que o origina, não é permitido
        assertThatThrownBy(() -> carteiraApi.creditar(carteiraId, Dinheiro.de("10.00"), UUID.randomUUID()))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThat(jdbc.queryForObject("SELECT saldo FROM carteiras", BigDecimal.class)).isEqualByComparingTo("0.00");
    }

    @Test
    void deveExigirToken() {
        assertThat(mvc.get().uri("/api/carteiras/minha")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private String idDaCarteira(String token) {
        MvcTestResult resposta = mvc.get().uri("/api/carteiras/minha").header("Authorization", "Bearer " + token)
                .exchange();
        assertThat(resposta).hasStatusOk();
        return lerJson(resposta, "$.id");
    }
}
