package com.braga.carteiradigital.transacao;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.braga.carteiradigital.carteira.dominio.SaldoInsuficienteException;
import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;
import com.braga.carteiradigital.suporte.IntegracaoTestBase;
import com.braga.carteiradigital.transacao.aplicacao.porta.entrada.TransferirUseCase;
import com.braga.carteiradigital.transacao.dominio.ChaveIdempotencia;

class TransferenciaIntegracaoTest extends IntegracaoTestBase {

    @Autowired
    private TransferirUseCase transferirUseCase;

    private String tokenAna;
    private String tokenBruno;

    @BeforeEach
    void prepararUsuarios() {
        tokenAna = novoUsuarioComToken("ana@email.com");
        tokenBruno = novoUsuarioComToken("bruno@email.com");
        depositar(tokenAna, "100.00");
    }

    @Test
    void deveTransferirERetornarComprovante() {
        assertThat(transferir(tokenAna, "bruno@email.com", "30.00", "Almoço", "chave-1"))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.transacaoId").isNotNull();
                    assertThat(json).extractingPath("$.tipo").isEqualTo("TRANSFERENCIA");
                    assertThat(json).extractingPath("$.descricao").isEqualTo("Almoço");
                });
    }

    @Test
    void comprovanteDeveMostrarSomenteOSaldoDeQuemTransferiu() {
        assertThat(transferir(tokenAna, "bruno@email.com", "30.00", null, "chave-1"))
                .bodyText().contains("\"valor\":30.00", "\"saldoAtual\":70.00");
    }

    @Test
    void deveDebitarDaOrigemECreditarNoDestino() {
        transferir(tokenAna, "bruno@email.com", "30.00", null, "chave-1");

        assertThat(saldo(tokenAna)).isEqualTo("70.00");
        assertThat(saldo(tokenBruno)).isEqualTo("30.00");
    }

    @Test
    void deveGravarUmaTransacaoEDoisLancamentos() {
        UUID transacaoId = UUID.fromString(lerJson(transferir(tokenAna, "bruno@email.com", "30.00", null, "chave-1"),
                "$.transacaoId"));

        Map<String, Object> transacao = jdbc.queryForMap("SELECT * FROM transacoes WHERE id = ?", transacaoId);
        assertThat(transacao)
                .containsEntry("tipo", "TRANSFERENCIA")
                .containsEntry("carteira_origem_id", carteiraDe("ana@email.com"))
                .containsEntry("carteira_destino_id", carteiraDe("bruno@email.com"));

        List<Map<String, Object>> lancamentos = jdbc.queryForList(
                "SELECT carteira_id, natureza, valor, saldo_apos FROM lancamentos WHERE transacao_id = ? ORDER BY natureza",
                transacaoId);
        assertThat(lancamentos).hasSize(2);
        assertThat(lancamentos.get(0))
                .containsEntry("natureza", "CREDITO")
                .containsEntry("carteira_id", carteiraDe("bruno@email.com"));
        assertThat((BigDecimal) lancamentos.get(0).get("saldo_apos")).isEqualByComparingTo("30.00");
        assertThat(lancamentos.get(1))
                .containsEntry("natureza", "DEBITO")
                .containsEntry("carteira_id", carteiraDe("ana@email.com"));
        assertThat((BigDecimal) lancamentos.get(1).get("saldo_apos")).isEqualByComparingTo("70.00");
    }

    @Test
    void devePermitirTransferirOSaldoInteiro() {
        assertThat(transferir(tokenAna, "bruno@email.com", "100.00", null, "chave-1"))
                .hasStatus(HttpStatus.CREATED)
                .bodyText().contains("\"saldoAtual\":0.00");
    }

    @Test
    void deveEncontrarODestinatarioSemDiferenciarMaiusculas() {
        assertThat(transferir(tokenAna, "Bruno@Email.COM", "10.00", null, "chave-1")).hasStatus(HttpStatus.CREATED);

        assertThat(saldo(tokenBruno)).isEqualTo("10.00");
    }

    @Test
    void deveRecusarSaldoInsuficienteSemGravarNada() {
        assertThat(transferir(tokenAna, "bruno@email.com", "100.01", null, "chave-1"))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().extractingPath("$.codigo").isEqualTo("saldo-insuficiente");

        // Quando o saldo foi verificado, a transação já tinha sido gravada (com flush): o rollback a desfez
        assertThat(saldo(tokenAna)).isEqualTo("100.00");
        assertThat(saldo(tokenBruno)).isEqualTo("0.00");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM transacoes WHERE tipo = 'TRANSFERENCIA'", Integer.class))
                .isZero();
    }

    @Test
    void transferenciaRecusadaNaoConsomeAChave() {
        assertThat(transferir(tokenAna, "bruno@email.com", "150.00", null, "chave-1"))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        depositar(tokenAna, "50.00");

        assertThat(transferir(tokenAna, "bruno@email.com", "150.00", null, "chave-1")).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void deveRecusarDestinatarioInexistente() {
        assertThat(transferir(tokenAna, "ninguem@email.com", "10.00", null, "chave-1"))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().extractingPath("$.codigo").isEqualTo("destinatario-nao-encontrado");
    }

    @Test
    void deveRecusarTransferenciaParaSiMesmo() {
        assertThat(transferir(tokenAna, "ANA@email.com", "10.00", null, "chave-1"))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson().extractingPath("$.codigo").isEqualTo("transferencia-para-si-mesmo");

        assertThat(saldo(tokenAna)).isEqualTo("100.00");
    }

    @Test
    void deveIndicarQuaisCamposEstaoInvalidos() {
        assertThat(transferir(tokenAna, "nao-e-um-email", "10.001", "x".repeat(141), "chave-1"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPath("$.campos.emailDestinatario")
                .hasPath("$.campos.valor")
                .hasPath("$.campos.descricao")
                .extractingPath("$.codigo").isEqualTo("dados-invalidos");
    }

    @Test
    void deveExigirDestinatarioEValor() {
        assertThat(mvc.post().uri("/api/transacoes/transferencias")
                .header("Authorization", "Bearer " + tokenAna)
                .header("Idempotency-Key", "chave-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPath("$.campos.emailDestinatario")
                .hasPath("$.campos.valor");
    }

    @Test
    void deveExigirIdempotencyKey() {
        assertThat(transferir(tokenAna, "bruno@email.com", "10.00", null, null))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().hasPath("$.campos['Idempotency-Key']");
    }

    @Test
    void chaveRepetidaNaoDeveTransferirDuasVezes() {
        assertThat(transferir(tokenAna, "bruno@email.com", "10.00", null, "chave-1")).hasStatus(HttpStatus.CREATED);

        assertThat(transferir(tokenAna, "bruno@email.com", "10.00", null, "chave-1"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.codigo").isEqualTo("chave-idempotencia-ja-utilizada");

        assertThat(saldo(tokenAna)).isEqualTo("90.00");
        assertThat(saldo(tokenBruno)).isEqualTo("10.00");
    }

    @Test
    void deveExigirToken() {
        assertThat(mvc.post().uri("/api/transacoes/transferencias")
                .header("Idempotency-Key", "chave-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"emailDestinatario\": \"bruno@email.com\", \"valor\": 10.00}"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void transferenciasSimultaneasNaoDevemGastarMaisQueOSaldo() throws Exception {
        UUID ana = usuarioId("ana@email.com");
        List<Callable<Object>> tarefas = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            ChaveIdempotencia chave = new ChaveIdempotencia("paralelo-" + i);
            tarefas.add(() -> transferirUseCase.transferir(
                    new TransferirUseCase.Comando(ana, "bruno@email.com", Dinheiro.de("10.00"), null, chave)));
        }

        List<Throwable> erros = executarAoMesmoTempo(tarefas);

        // Saldo de 100.00: só 10 transferências de 10.00 cabem. Sem o bloqueio, várias threads veriam
        // o mesmo saldo "suficiente" e a carteira terminaria negativa (ou a constraint do banco falharia)
        assertThat(erros).hasSize(5).allMatch(SaldoInsuficienteException.class::isInstance);
        assertThat(saldo(tokenAna)).isEqualTo("0.00");
        assertThat(saldo(tokenBruno)).isEqualTo("100.00");
    }

    @Test
    void transferenciasCruzadasSimultaneasNaoDevemCausarDeadlock() throws Exception {
        depositar(tokenBruno, "100.00");
        UUID ana = usuarioId("ana@email.com");
        UUID bruno = usuarioId("bruno@email.com");
        List<Callable<Object>> tarefas = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            ChaveIdempotencia chave = new ChaveIdempotencia("cruzada-" + i);
            // Metade Ana → Bruno, metade Bruno → Ana, todas ao mesmo tempo
            var comando = i % 2 == 0
                    ? new TransferirUseCase.Comando(ana, "bruno@email.com", Dinheiro.de("5.00"), null, chave)
                    : new TransferirUseCase.Comando(bruno, "ana@email.com", Dinheiro.de("5.00"), null, chave);
            tarefas.add(() -> transferirUseCase.transferir(comando));
        }

        // Se cada transferência bloqueasse primeiro a própria origem, o PostgreSQL detectaria deadlocks
        // e abortaria algumas delas
        assertThat(executarAoMesmoTempo(tarefas)).isEmpty();
        assertThat(saldo(tokenAna)).isEqualTo("100.00");
        assertThat(saldo(tokenBruno)).isEqualTo("100.00");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM lancamentos", Integer.class)).isEqualTo(2 + 20 * 2);
    }

    /** Executa as tarefas em paralelo, liberando todas no mesmo instante, e devolve os erros. */
    private static List<Throwable> executarAoMesmoTempo(List<Callable<Object>> tarefas) throws InterruptedException {
        CountDownLatch largada = new CountDownLatch(1);
        List<Throwable> erros = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(tarefas.size())) {
            List<Future<Object>> resultados = new ArrayList<>();
            for (Callable<Object> tarefa : tarefas) {
                resultados.add(executor.submit(() -> {
                    largada.await();
                    return tarefa.call();
                }));
            }
            largada.countDown();
            for (Future<Object> resultado : resultados) {
                try {
                    resultado.get();
                } catch (ExecutionException erro) {
                    erros.add(erro.getCause());
                }
            }
        }
        return erros;
    }

    private void depositar(String token, String valor) {
        assertThat(mvc.post().uri("/api/transacoes/depositos")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valor\": %s}".formatted(valor)))
                .hasStatus(HttpStatus.CREATED);
    }

    private MvcTestResult transferir(String token, String email, String valor, String descricao, String chave) {
        String corpo = descricao == null
                ? "{\"emailDestinatario\": \"%s\", \"valor\": %s}".formatted(email, valor)
                : "{\"emailDestinatario\": \"%s\", \"valor\": %s, \"descricao\": \"%s\"}".formatted(email, valor, descricao);
        var requisicao = mvc.post().uri("/api/transacoes/transferencias")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo);
        if (chave != null) {
            requisicao.header("Idempotency-Key", chave);
        }
        return requisicao.exchange();
    }

    private String saldo(String token) {
        MvcTestResult resposta = mvc.get().uri("/api/carteiras/minha").header("Authorization", "Bearer " + token).exchange();
        assertThat(resposta).hasStatusOk();
        // Lido do texto da resposta para não passar por double
        String json = new String(resposta.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        return json.replaceAll(".*\"saldo\":([0-9.]+).*", "$1");
    }

    private UUID usuarioId(String email) {
        return jdbc.queryForObject("SELECT id FROM usuarios WHERE email = ?", UUID.class, email);
    }

    private UUID carteiraDe(String email) {
        return jdbc.queryForObject(
                "SELECT c.id FROM carteiras c JOIN usuarios u ON u.id = c.usuario_id WHERE u.email = ?", UUID.class, email);
    }
}
