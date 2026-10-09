package com.braga.carteiradigital.transacao.dominio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

import com.braga.carteiradigital.compartilhado.dominio.Dinheiro;

/** Operação financeira (depósito ou transferência). Imutável depois de criada. */
public record Transacao(UUID id, TipoTransacao tipo, Dinheiro valor, UUID carteiraOrigemId, UUID carteiraDestinoId,
        String descricao, UUID iniciadaPor, ChaveIdempotencia chaveIdempotencia, String hashRequisicao,
        Instant criadoEm) {

    public static final int TAMANHO_MAXIMO_DESCRICAO = 140;

    public Transacao {
        Objects.requireNonNull(id, "id é obrigatório");
        Objects.requireNonNull(tipo, "tipo é obrigatório");
        Objects.requireNonNull(valor, "valor é obrigatório");
        Objects.requireNonNull(carteiraDestinoId, "carteira de destino é obrigatória");
        Objects.requireNonNull(iniciadaPor, "usuário que iniciou a transação é obrigatório");
        Objects.requireNonNull(chaveIdempotencia, "chave de idempotência é obrigatória");
        Objects.requireNonNull(hashRequisicao, "hash da requisição é obrigatório");
        Objects.requireNonNull(criadoEm, "data de criação é obrigatória");
        if (!valor.ehPositivo()) {
            throw new IllegalArgumentException("valor da transação deve ser positivo");
        }
        if (tipo == TipoTransacao.DEPOSITO && carteiraOrigemId != null) {
            throw new IllegalArgumentException("depósito não tem carteira de origem");
        }
        if (tipo == TipoTransacao.TRANSFERENCIA
                && (carteiraOrigemId == null || carteiraOrigemId.equals(carteiraDestinoId))) {
            throw new IllegalArgumentException("transferência exige carteira de origem diferente da de destino");
        }
        descricao = normalizarDescricao(descricao);
    }

    public static Transacao deposito(UUID carteiraDestinoId, Dinheiro valor, String descricao, UUID iniciadaPor,
            ChaveIdempotencia chave, Clock relogio) {
        return criar(TipoTransacao.DEPOSITO, null, carteiraDestinoId, valor, descricao, iniciadaPor, chave, relogio);
    }

    public static Transacao transferencia(UUID carteiraOrigemId, UUID carteiraDestinoId, Dinheiro valor,
            String descricao, UUID iniciadaPor, ChaveIdempotencia chave, Clock relogio) {
        return criar(TipoTransacao.TRANSFERENCIA, carteiraOrigemId, carteiraDestinoId, valor, descricao, iniciadaPor,
                chave, relogio);
    }

    private static Transacao criar(TipoTransacao tipo, UUID carteiraOrigemId, UUID carteiraDestinoId, Dinheiro valor,
            String descricao, UUID iniciadaPor, ChaveIdempotencia chave, Clock relogio) {
        String descricaoNormalizada = normalizarDescricao(descricao);
        // Impressão digital do pedido do cliente: permite detectar a mesma chave reutilizada com outros dados.
        // No depósito o destino é sempre a carteira de quem pede; na transferência, o destino faz parte do pedido.
        String destinoDoPedido = tipo == TipoTransacao.TRANSFERENCIA ? "|" + carteiraDestinoId : "";
        String hash = sha256(tipo + destinoDoPedido + "|" + valor + "|" + Objects.toString(descricaoNormalizada, ""));
        return new Transacao(UUID.randomUUID(), tipo, valor, carteiraOrigemId, carteiraDestinoId,
                descricaoNormalizada, iniciadaPor, chave, hash, Instant.now(relogio).truncatedTo(ChronoUnit.MICROS));
    }

    private static String normalizarDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            return null;
        }
        String normalizada = descricao.strip();
        if (normalizada.length() > TAMANHO_MAXIMO_DESCRICAO) {
            throw new IllegalArgumentException("descrição deve ter no máximo 140 caracteres");
        }
        return normalizada;
    }

    private static String sha256(String conteudo) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(conteudo.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException erro) {
            throw new IllegalStateException("SHA-256 é obrigatório em toda JVM", erro);
        }
    }
}
