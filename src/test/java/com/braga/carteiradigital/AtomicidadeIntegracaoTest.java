package com.braga.carteiradigital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.braga.carteiradigital.carteira.aplicacao.porta.saida.CarteiraRepository;
import com.braga.carteiradigital.carteira.aplicacao.porta.saida.LancamentoRepository;
import com.braga.carteiradigital.suporte.IntegracaoTestBase;

/**
 * Prova o "A" de ACID: quando um passo falha no meio de uma operação que envolve mais de um
 * módulo, o rollback desfaz também o que os passos anteriores já tinham gravado no banco.
 *
 * <p>Os spies se comportam como os repositórios reais, exceto quando um teste força uma falha.
 */
class AtomicidadeIntegracaoTest extends IntegracaoTestBase {

    @MockitoSpyBean
    private CarteiraRepository carteiras;

    @MockitoSpyBean
    private LancamentoRepository lancamentos;

    @Test
    void cadastroDeveSerDesfeitoSeACarteiraNaoPuderSerCriada() {
        doThrow(new IllegalStateException("falha simulada")).when(carteiras).inserir(any());

        assertThat(mvc.post().uri("/api/auth/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome": "Ana", "email": "ana@email.com", "senha": "senha-segura-123"}
                        """))
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .bodyJson().extractingPath("$.codigo").isEqualTo("erro-interno");

        // O usuário já tinha sido inserido (com flush) quando a criação da carteira falhou
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuarios", Integer.class)).isZero();
    }

    @Test
    void depositoDeveSerDesfeitoSeOLancamentoFalhar() {
        String token = novoUsuarioComToken("ana@email.com");
        doThrow(new IllegalStateException("falha simulada")).when(lancamentos).registrar(any());

        assertThat(mvc.post().uri("/api/transacoes/depositos")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", "chave-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"valor\": 10.00}"))
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);

        // Quando o lançamento falhou, a transação e o novo saldo (10.00) já estavam gravados com flush
        assertThat(jdbc.queryForObject("SELECT count(*) FROM transacoes", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT saldo FROM carteiras", BigDecimal.class)).isEqualByComparingTo("0.00");
    }
}
