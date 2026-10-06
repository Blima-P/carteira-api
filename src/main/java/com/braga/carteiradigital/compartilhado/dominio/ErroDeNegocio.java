package com.braga.carteiradigital.compartilhado.dominio;

/**
 * Base de todas as violações de regra de negócio.
 *
 * <p>É Java puro: o domínio informa apenas o {@link Tipo} do erro, e o adaptador web
 * decide qual status HTTP corresponde a cada tipo.
 */
public abstract class ErroDeNegocio extends RuntimeException {

    public enum Tipo {
        CONFLITO,
        NAO_AUTORIZADO,
        NAO_ENCONTRADO,
        REGRA_VIOLADA
    }

    private final Tipo tipo;
    private final String codigo;

    protected ErroDeNegocio(Tipo tipo, String codigo, String mensagem) {
        super(mensagem);
        this.tipo = tipo;
        this.codigo = codigo;
    }

    public Tipo getTipo() {
        return tipo;
    }

    /** Identificador estável do erro (ex.: {@code email-ja-cadastrado}), útil para o cliente da API. */
    public String getCodigo() {
        return codigo;
    }
}
