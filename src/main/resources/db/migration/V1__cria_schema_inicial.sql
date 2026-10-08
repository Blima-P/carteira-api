-- =====================================================================
-- V1 — Schema inicial da carteira digital
--
-- Regras financeiras críticas são garantidas pelo BANCO (constraints e trigger),
-- não só pela aplicação: mesmo um bug no código não consegue corromper os dados.
-- =====================================================================

-- Módulo Usuário ------------------------------------------------------

CREATE TABLE usuarios (
    id         UUID PRIMARY KEY,
    nome       VARCHAR(120) NOT NULL,
    email      VARCHAR(180) NOT NULL,
    senha_hash VARCHAR(100) NOT NULL,
    criado_em  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_usuarios_email UNIQUE (email)
);

-- Módulo Carteira -----------------------------------------------------

CREATE TABLE carteiras (
    id            UUID PRIMARY KEY,
    usuario_id    UUID           NOT NULL REFERENCES usuarios (id),
    -- NUMERIC é decimal exato. Nunca usar FLOAT/DOUBLE para dinheiro (0.1 + 0.2 <> 0.3).
    saldo         NUMERIC(19, 2) NOT NULL DEFAULT 0,
    -- Controle de concorrência otimista (@Version no JPA)
    versao        BIGINT         NOT NULL DEFAULT 0,
    criado_em     TIMESTAMPTZ    NOT NULL,
    atualizado_em TIMESTAMPTZ    NOT NULL,
    CONSTRAINT uk_carteiras_usuario UNIQUE (usuario_id),
    CONSTRAINT ck_carteiras_saldo_nao_negativo CHECK (saldo >= 0)
);

-- Módulo Transação ----------------------------------------------------

CREATE TABLE transacoes (
    id                  UUID PRIMARY KEY,
    tipo                VARCHAR(20)    NOT NULL,
    valor               NUMERIC(19, 2) NOT NULL,
    carteira_origem_id  UUID REFERENCES carteiras (id),
    carteira_destino_id UUID           NOT NULL REFERENCES carteiras (id),
    descricao           VARCHAR(140),
    iniciada_por        UUID           NOT NULL REFERENCES usuarios (id),
    chave_idempotencia  VARCHAR(64)    NOT NULL,
    -- SHA-256 do corpo da requisição: detecta reuso da mesma chave com dados diferentes
    hash_requisicao     VARCHAR(64)    NOT NULL,
    criado_em           TIMESTAMPTZ    NOT NULL,
    CONSTRAINT ck_transacoes_tipo CHECK (tipo IN ('DEPOSITO', 'TRANSFERENCIA')),
    CONSTRAINT ck_transacoes_valor_positivo CHECK (valor > 0),
    -- Depósito não tem origem; transferência tem origem diferente do destino
    CONSTRAINT ck_transacoes_carteiras CHECK (
        (tipo = 'DEPOSITO' AND carteira_origem_id IS NULL)
        OR (tipo = 'TRANSFERENCIA' AND carteira_origem_id IS NOT NULL
            AND carteira_origem_id <> carteira_destino_id)
    ),
    -- Idempotência: o mesmo usuário nunca gera duas transações com a mesma chave
    CONSTRAINT uk_transacoes_idempotencia UNIQUE (iniciada_por, chave_idempotencia)
);

-- Livro-razão (Módulo Carteira) ---------------------------------------
-- Cada crédito/débito gera um lançamento imutável. O saldo de uma carteira
-- sempre pode ser reconstruído somando seus lançamentos (auditoria).

CREATE TABLE lancamentos (
    id           BIGSERIAL PRIMARY KEY,
    transacao_id UUID           NOT NULL REFERENCES transacoes (id),
    carteira_id  UUID           NOT NULL REFERENCES carteiras (id),
    natureza     VARCHAR(7)     NOT NULL,
    valor        NUMERIC(19, 2) NOT NULL,
    saldo_apos   NUMERIC(19, 2) NOT NULL,
    criado_em    TIMESTAMPTZ    NOT NULL,
    CONSTRAINT ck_lancamentos_natureza CHECK (natureza IN ('CREDITO', 'DEBITO')),
    CONSTRAINT ck_lancamentos_valor_positivo CHECK (valor > 0),
    CONSTRAINT ck_lancamentos_saldo_nao_negativo CHECK (saldo_apos >= 0)
);

-- Extrato: lançamentos de uma carteira, do mais recente para o mais antigo
CREATE INDEX idx_lancamentos_carteira_criado_em ON lancamentos (carteira_id, criado_em DESC, id DESC);
CREATE INDEX idx_lancamentos_transacao ON lancamentos (transacao_id);

-- Lançamentos são somente-inserção: nunca podem ser alterados ou apagados.
CREATE FUNCTION impedir_alteracao_lancamento() RETURNS TRIGGER AS
$$
BEGIN
    RAISE EXCEPTION 'lancamentos sao somente-insercao: nao podem ser alterados ou removidos';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_lancamentos_somente_insercao
    BEFORE UPDATE OR DELETE ON lancamentos
    FOR EACH ROW EXECUTE FUNCTION impedir_alteracao_lancamento();
