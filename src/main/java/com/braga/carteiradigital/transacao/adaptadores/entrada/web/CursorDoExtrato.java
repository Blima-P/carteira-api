package com.braga.carteiradigital.transacao.adaptadores.entrada.web;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import com.braga.carteiradigital.transacao.dominio.PosicaoNoExtrato;

/**
 * Converte a posição no extrato em um texto opaco para o cliente devolver na próxima requisição.
 *
 * <p>Não precisa ser secreto nem assinado: mesmo um cursor forjado só filtra dentro da carteira
 * do dono do token, então não dá acesso a lançamentos de outra pessoa.
 */
final class CursorDoExtrato {

    /** Folga sobre o tamanho real (~60 caracteres), só para recusar textos absurdos antes de decodificar. */
    static final int TAMANHO_MAXIMO = 128;

    private static final String SEPARADOR = "|";

    private CursorDoExtrato() {
    }

    static String codificar(PosicaoNoExtrato posicao) {
        String conteudo = posicao.criadoEm() + SEPARADOR + posicao.lancamentoId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(conteudo.getBytes(StandardCharsets.UTF_8));
    }

    /** @throws IllegalArgumentException se o texto não for um cursor gerado por {@link #codificar} */
    static PosicaoNoExtrato decodificar(String cursor) {
        if (cursor.length() > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException("cursor longo demais");
        }
        try {
            String conteudo = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int separador = conteudo.indexOf(SEPARADOR);
            if (separador < 0) {
                throw new IllegalArgumentException("cursor sem separador");
            }
            return new PosicaoNoExtrato(Instant.parse(conteudo.substring(0, separador)),
                    Long.parseLong(conteudo.substring(separador + 1)));
        } catch (RuntimeException erro) {
            throw new IllegalArgumentException("cursor inválido", erro);
        }
    }
}
