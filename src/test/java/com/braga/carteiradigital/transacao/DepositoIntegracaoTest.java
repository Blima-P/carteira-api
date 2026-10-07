package com.braga.carteiradigital.transacao;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;
import com.braga.carteiradigital.suporte.IntegracaoTestBase;
import com.braga.carteiradigital.transacao.aplicacao.porta.entrada.DepositarUseCase;
import com.braga.carteiradigital.transacao.dominio.ChaveIdempotencia;

class DepositoIntegracaoTest extends IntegracaoTestBase {

    @Autowired
    private DepositarUseCase depositarUseCase;

    private String token;

    @BeforeEach
    void prepararUsuario() {
        token = novoUsuarioComToken("ana@email.com");
    }

    @Test
    void deveDepositarERetornarComprovante() {
        assertThat(depositar("150.50", "Salário", "chave-1"))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.transacaoId").isNotNull();
                    assertThat(json).extractingPath("$.tipo").isEqualTo("DEPOSITO");
                    assertThat(json).extractingPath("$.descricao").isEqualTo("Salário");
                    assertThat(json).extractingPath("$.realizadaEm").isNotNull();
                });
    }

    @Test
    void valoresDevemSairComDuasCasasDecimais() {
        assertThat(depositar("150.5", null, "chave-1"))
                .hasStatus(HttpStatus.CREATED)
                .bodyText().contains("\"valor\":150.50", "\"saldoAtual\":150.50");
    }

    @Test
    void depositoDeveAtualizarOSaldoDaCarteira() {
        depositar("150.50", null, "chave-1");

        assertThat(mvc.get().uri("/api/carteiras/minha").header("Authorization", "Bearer " + token))
                .hasStatusOk()
                .bodyText().contains("\"saldo\":150.50");
    }

    @Test
    void depositosDevemSomarSemErroDeArredondamento() {
        depositar("0.10", null, "chave-1");

        assertThat(depositar("0.20", null, "chave-2")).bodyText().contains("\"saldoAtual\":0.30");
    }

    @Test
    void deveGravarTransacaoELancamentoNoLivroRazao() {
        UUID transacaoId = UUID.fromString(lerJson(depositar("150.50", "Salário", "chave-1"), "$.transacaoId"));

        Map<String, Object> transacao = jdbc.queryForMap("SELECT * FROM transacoes WHERE id = ?", transacaoId);
        assertThat(transacao)
                .containsEntry("tipo", "DEPOSITO")
                .containsEntry("carteira_origem_id", null)
                .containsEntry("chave_idempotencia", "chave-1");
        assertThat((String) transacao.get("hash_requisicao")).matches("[0-9a-f]{64}");

        Map<String, Object> lancamento = jdbc.queryForMap("SELECT * FROM lancamentos WHERE transacao_id = ?", transacaoId);
        assertThat(lancamento)
                .containsEntry("natureza", "CREDITO")
                .containsEntry("carteira_id", transacao.get("carteira_destino_id"));
        assertThat((BigDecimal) lancamento.get("valor")).isEqualByComparingTo("150.50");
        assertThat((BigDecimal) lancamento.get("saldo_apos")).isEqualByComparingTo("150.50");
    }

    @ParameterizedTest
    @ValueSource(strings = { "0", "-10.00", "10.001", "null", "\"abc\"" })
    void deveRecusarValorInvalidoSemGravarNada(String valor) {
        MvcTestResult resposta = mvc.post().uri("/api/transacoes/depositos")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", "chave-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valor\": " + valor + "}")
                .exchange();

        assertThat(resposta).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM transacoes", Integer.class)).isZero();
    }

    @Test
    void deveIndicarQualCampoEstaInvalido() {
        assertThat(depositar("-1", "x".repeat(141), "chave-1"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPath("$.campos.valor")
                .hasPath("$.campos.descricao")
                .extractingPath("$.codigo").isEqualTo("dados-invalidos");
    }

    @Test
    void deveExigirIdempotencyKey() {
        assertThat(depositar("10.00", null, null))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPath("$.campos['Idempotency-Key']")
                .extractingPath("$.codigo").isEqualTo("dados-invalidos");
    }

    @ParameterizedTest
    @ValueSource(strings = { "com espaco", "chave/com/barra", "uma-chave-grande-demais-uma-chave-grande-demais-uma-chave-grande-demais" })
    void deveRecusarIdempotencyKeyComFormatoInvalido(String chave) {
        assertThat(depositar("10.00", null, chave))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().hasPath("$.campos['Idempotency-Key']");
    }

    @Test
    void chaveRepetidaNaoDeveDepositarDuasVezes() {
        assertThat(depositar("10.00", null, "chave-1")).hasStatus(HttpStatus.CREATED);

        // Ex.: o cliente não recebeu a primeira resposta (falha de rede) e reenviou a requisição
        assertThat(depositar("10.00", null, "chave-1"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.codigo").isEqualTo("chave-idempotencia-ja-utilizada");

        assertThat(jdbc.queryForObject("SELECT saldo FROM carteiras", BigDecimal.class)).isEqualByComparingTo("10.00");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lancamentos", Integer.class)).isEqualTo(1);
    }

    @Test
    void usuariosDiferentesPodemUsarAMesmaChave() {
        String tokenBruno = novoUsuarioComToken("bruno@email.com");
        depositar("10.00", null, "chave-1");

        assertThat(mvc.post().uri("/api/transacoes/depositos")
                .header("Authorization", "Bearer " + tokenBruno)
                .header("Idempotency-Key", "chave-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valor\": 10.00}"))
                .hasStatus(HttpStatus.CREATED);
    }

    @Test
    void deveExigirToken() {
        assertThat(mvc.post().uri("/api/transacoes/depositos")
                .header("Idempotency-Key", "chave-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valor\": 10.00}"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void depositosSimultaneosNaMesmaCarteiraNaoDevemSePerder() throws Exception {
        UUID usuarioId = jdbc.queryForObject("SELECT id FROM usuarios", UUID.class);
        int quantidade = 8;
        CountDownLatch largada = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(quantidade)) {
            List<Future<?>> tarefas = new ArrayList<>();
            for (int i = 0; i < quantidade; i++) {
                ChaveIdempotencia chave = new ChaveIdempotencia("paralelo-" + i);
                tarefas.add(executor.submit(() -> {
                    largada.await(); // todas as threads começam juntas
                    return depositarUseCase.depositar(
                            new DepositarUseCase.Comando(usuarioId, Dinheiro.de("10.00"), null, chave));
                }));
            }
            largada.countDown();
            for (Future<?> tarefa : tarefas) {
                tarefa.get();
            }
        }

        // Sem o bloqueio da carteira, duas threads poderiam ler o mesmo saldo e uma sobrescreveria a outra
        assertThat(jdbc.queryForObject("SELECT saldo FROM carteiras", BigDecimal.class)).isEqualByComparingTo("80.00");
        assertThat(jdbc.queryForList("SELECT saldo_apos FROM lancamentos ORDER BY saldo_apos", BigDecimal.class))
                .extracting(BigDecimal::toPlainString)
                .containsExactly("10.00", "20.00", "30.00", "40.00", "50.00", "60.00", "70.00", "80.00");
    }

    private MvcTestResult depositar(String valor, String descricao, String chave) {
        var requisicao = mvc.post().uri("/api/transacoes/depositos")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(descricao == null
                        ? "{\"valor\": %s}".formatted(valor)
                        : "{\"valor\": %s, \"descricao\": \"%s\"}".formatted(valor, descricao));
        if (chave != null) {
            requisicao.header("Idempotency-Key", chave);
        }
        return requisicao.exchange();
    }
}
