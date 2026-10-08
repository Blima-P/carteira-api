package com.braga.carteiradigital.banco;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.braga.carteiradigital.suporte.IntegracaoTestBase;

/**
 * Garante que as regras críticas estão no próprio banco: mesmo que a aplicação tenha um bug,
 * o PostgreSQL recusa dados financeiros inválidos.
 */
class SchemaBancoDeDadosTest extends IntegracaoTestBase {

    private UUID usuarioId;
    private UUID carteiraId;

    @BeforeEach
    void prepararDados() {
        usuarioId = inserirUsuario("ana@email.com");
        carteiraId = inserirCarteira(usuarioId);
    }

    @Test
    void flywayDeveCriarTodasAsTabelas() {
        var tabelas = jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                """, String.class);

        assertThat(tabelas).contains("usuarios", "carteiras", "transacoes", "lancamentos", "flyway_schema_history");
    }

    @Test
    void deveRecusarEmailDuplicado() {
        assertThatThrownBy(() -> inserirUsuario("ana@email.com"))
                .hasMessageContaining("uk_usuarios_email");
    }

    @Test
    void deveRecusarSegundaCarteiraParaOMesmoUsuario() {
        assertThatThrownBy(() -> inserirCarteira(usuarioId))
                .hasMessageContaining("uk_carteiras_usuario");
    }

    @Test
    void deveRecusarSaldoNegativo() {
        assertThatThrownBy(() -> jdbc.update("UPDATE carteiras SET saldo = -0.01 WHERE id = ?", carteiraId))
                .hasMessageContaining("ck_carteiras_saldo_nao_negativo");
    }

    @Test
    void deveRecusarTransacaoComValorZero() {
        assertThatThrownBy(() -> inserirDeposito("0.00", "chave-1"))
                .hasMessageContaining("ck_transacoes_valor_positivo");
    }

    @Test
    void deveRecusarChaveDeIdempotenciaRepetidaParaOMesmoUsuario() {
        inserirDeposito("10.00", "chave-1");

        assertThatThrownBy(() -> inserirDeposito("10.00", "chave-1"))
                .hasMessageContaining("uk_transacoes_idempotencia");
    }

    @Test
    void deveRecusarTransferenciaParaAPropriaCarteira() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO transacoes (id, tipo, valor, carteira_origem_id, carteira_destino_id, iniciada_por,
                                        chave_idempotencia, hash_requisicao, criado_em)
                VALUES (?, 'TRANSFERENCIA', 10, ?, ?, ?, 'chave-1', 'hash', now())
                """, UUID.randomUUID(), carteiraId, carteiraId, usuarioId))
                .hasMessageContaining("ck_transacoes_carteiras");
    }

    @Test
    void lancamentosDevemSerSomenteInsercao() {
        UUID transacaoId = inserirDeposito("10.00", "chave-1");
        jdbc.update("""
                INSERT INTO lancamentos (transacao_id, carteira_id, natureza, valor, saldo_apos, criado_em)
                VALUES (?, ?, 'CREDITO', 10.00, 10.00, now())
                """, transacaoId, carteiraId);

        assertThatThrownBy(() -> jdbc.update("UPDATE lancamentos SET valor = 999"))
                .hasMessageContaining("somente-insercao");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM lancamentos"))
                .hasMessageContaining("somente-insercao");
    }

    private UUID inserirUsuario(String email) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO usuarios (id, nome, email, senha_hash, criado_em) VALUES (?, 'Ana', ?, 'hash', now())",
                id, email);
        return id;
    }

    private UUID inserirCarteira(UUID dono) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO carteiras (id, usuario_id, criado_em, atualizado_em) VALUES (?, ?, now(), now())",
                id, dono);
        return id;
    }

    private UUID inserirDeposito(String valor, String chaveIdempotencia) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO transacoes (id, tipo, valor, carteira_destino_id, iniciada_por,
                                        chave_idempotencia, hash_requisicao, criado_em)
                VALUES (?, 'DEPOSITO', ?::numeric, ?, ?, ?, 'hash', now())
                """, id, valor, carteiraId, usuarioId, chaveIdempotencia);
        return id;
    }
}
