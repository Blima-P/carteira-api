package com.braga.carteiradigital.transacao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.braga.carteiradigital.suporte.IntegracaoTestBase;

class ExtratoIntegracaoTest extends IntegracaoTestBase {

    @Test
    void deveListarDoMaisRecenteParaOMaisAntigoComSaldoEContraparte() {
        String tokenAna = cadastrarComToken("Ana Lima", "ana@email.com");
        cadastrarComToken("Bruno Souza", "bruno@email.com");
        depositar(tokenAna, "100.00", "Salário", "chave-1");
        transferir(tokenAna, "bruno@email.com", "30.00", "Almoço");

        MvcTestResult extrato = extrato(tokenAna, "");

        assertThat(extrato).hasStatusOk();
        assertThat(extrato).bodyText().contains(
                "\"valor\":30.00,\"saldoApos\":70.00", "\"valor\":100.00,\"saldoApos\":100.00");
        List<String> tipos = lerJson(extrato, "$.itens[*].tipo");
        List<String> naturezas = lerJson(extrato, "$.itens[*].natureza");
        List<String> descricoes = lerJson(extrato, "$.itens[*].descricao");
        List<String> contrapartes = lerJson(extrato, "$.itens[*].contraparte");
        assertThat(tipos).containsExactly("TRANSFERENCIA", "DEPOSITO");
        assertThat(naturezas).containsExactly("DEBITO", "CREDITO");
        assertThat(descricoes).containsExactly("Almoço", "Salário");
        assertThat(contrapartes).containsExactly("Bruno Souza", null);
        assertThat((Object) lerJson(extrato, "$.proximoCursor")).isNull();
    }

    @Test
    void cadaUsuarioDeveVerSomenteOProprioLadoDaTransferencia() {
        String tokenAna = cadastrarComToken("Ana Lima", "ana@email.com");
        String tokenBruno = cadastrarComToken("Bruno Souza", "bruno@email.com");
        depositar(tokenAna, "100.00", null, "chave-1");
        String transacaoId = lerJson(transferir(tokenAna, "bruno@email.com", "30.00", null), "$.transacaoId");

        MvcTestResult extrato = extrato(tokenBruno, "");

        assertThat(extrato).bodyText().contains("\"valor\":30.00,\"saldoApos\":30.00");
        List<String> ids = lerJson(extrato, "$.itens[*].transacaoId");
        assertThat(ids).containsExactly(transacaoId);
        assertThat((String) lerJson(extrato, "$.itens[0].natureza")).isEqualTo("CREDITO");
        assertThat((String) lerJson(extrato, "$.itens[0].contraparte")).isEqualTo("Ana Lima");
    }

    @Test
    void devePercorrerTodasAsPaginasSemRepetirNemPularItens() {
        String token = novoUsuarioComToken("ana@email.com");
        for (int valor = 1; valor <= 5; valor++) {
            depositar(token, valor + ".00", null, "chave-" + valor);
        }

        List<List<Double>> paginas = new ArrayList<>();
        String cursor = null;
        do {
            MvcTestResult pagina = extrato(token, "tamanho=2" + (cursor == null ? "" : "&cursor=" + cursor));
            assertThat(pagina).hasStatusOk();
            paginas.add(lerJson(pagina, "$.itens[*].valor"));
            cursor = lerJson(pagina, "$.proximoCursor");
        } while (cursor != null);

        assertThat(paginas).containsExactly(List.of(5.0, 4.0), List.of(3.0, 2.0), List.of(1.0));
    }

    @Test
    void lancamentoNovoNaoDeveDeslocarAsProximasPaginas() {
        String token = novoUsuarioComToken("ana@email.com");
        for (int valor = 1; valor <= 3; valor++) {
            depositar(token, valor + ".00", null, "chave-" + valor);
        }
        String cursor = lerJson(extrato(token, "tamanho=2"), "$.proximoCursor");

        // Com paginação por número de página, este depósito empurraria o "2" para a página 2 (item repetido)
        depositar(token, "9.00", null, "chave-nova");
        MvcTestResult segundaPagina = extrato(token, "tamanho=2&cursor=" + cursor);

        List<Double> valores = lerJson(segundaPagina, "$.itens[*].valor");
        assertThat(valores).containsExactly(1.0);
        assertThat((Object) lerJson(segundaPagina, "$.proximoCursor")).isNull();
    }

    @Test
    void reenvioIdempotenteNaoDeveAparecerDuasVezes() {
        String token = novoUsuarioComToken("ana@email.com");
        depositar(token, "50.00", null, "mesma-chave");
        depositar(token, "50.00", null, "mesma-chave");

        List<Object> itens = lerJson(extrato(token, ""), "$.itens");
        assertThat(itens).hasSize(1);
    }

    @Test
    void carteiraSemMovimentacaoDeveTerExtratoVazio() {
        MvcTestResult extrato = extrato(novoUsuarioComToken("ana@email.com"), "");

        assertThat(extrato).hasStatusOk();
        List<Object> itens = lerJson(extrato, "$.itens");
        assertThat(itens).isEmpty();
        assertThat((Object) lerJson(extrato, "$.proximoCursor")).isNull();
    }

    @Test
    void deveRecusarTamanhoForaDoLimite() {
        String token = novoUsuarioComToken("ana@email.com");

        for (String tamanho : List.of("0", "101")) {
            MvcTestResult resposta = extrato(token, "tamanho=" + tamanho);
            assertThat(resposta).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat((String) lerJson(resposta, "$.codigo")).isEqualTo("dados-invalidos");
            assertThat((String) lerJson(resposta, "$.campos.tamanho")).isNotBlank();
        }
    }

    @Test
    void deveRecusarCursorQueNaoFoiGeradoPelaApi() {
        String token = novoUsuarioComToken("ana@email.com");
        String semSeparador = "c2VtLXNlcGFyYWRvcg"; // "sem-separador" em base64url
        String idNegativo = "MjAyNi0wMS0wMVQwMDowMDowMFp8LTE"; // "2026-01-01T00:00:00Z|-1"

        for (String cursor : List.of("nao-e-base64!", semSeparador, idNegativo, "A".repeat(200))) {
            MvcTestResult resposta = extrato(token, "cursor=" + cursor);
            assertThat(resposta).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat((String) lerJson(resposta, "$.campos.cursor")).isNotBlank();
        }
    }

    @Test
    void deveExigirToken() {
        assertThat(mvc.get().uri("/api/transacoes/extrato")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private MvcTestResult extrato(String token, String parametros) {
        return mvc.get().uri("/api/transacoes/extrato?" + parametros)
                .header("Authorization", "Bearer " + token)
                .exchange();
    }

    /** {@link #novoUsuarioComToken} cadastra todos como "Teste"; aqui o nome importa para a contraparte. */
    private String cadastrarComToken(String nome, String email) {
        String cadastro = """
                {"nome": "%s", "email": "%s", "senha": "senha-segura-123"}
                """.formatted(nome, email);
        assertThat(mvc.post().uri("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(cadastro))
                .hasStatus(HttpStatus.CREATED);
        MvcTestResult login = mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(cadastro).exchange();
        return lerJson(login, "$.token");
    }

    private void depositar(String token, String valor, String descricao, String chave) {
        String corpo = descricao == null
                ? "{\"valor\": %s}".formatted(valor)
                : "{\"valor\": %s, \"descricao\": \"%s\"}".formatted(valor, descricao);
        assertThat(mvc.post().uri("/api/transacoes/depositos")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", chave)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo))
                .hasStatus(HttpStatus.CREATED);
    }

    private MvcTestResult transferir(String token, String email, String valor, String descricao) {
        String corpo = descricao == null
                ? "{\"emailDestinatario\": \"%s\", \"valor\": %s}".formatted(email, valor)
                : "{\"emailDestinatario\": \"%s\", \"valor\": %s, \"descricao\": \"%s\"}".formatted(email, valor, descricao);
        MvcTestResult resposta = mvc.post().uri("/api/transacoes/transferencias")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo)
                .exchange();
        assertThat(resposta).hasStatus(HttpStatus.CREATED);
        return resposta;
    }
}
