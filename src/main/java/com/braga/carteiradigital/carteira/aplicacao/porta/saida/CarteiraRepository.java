package com.braga.carteiradigital.carteira.aplicacao.porta.saida;

import java.util.Optional;
import java.util.UUID;

import com.braga.carteiradigital.carteira.dominio.Carteira;

public interface CarteiraRepository {

    void inserir(Carteira carteira);

    void atualizar(Carteira carteira);

    Optional<Carteira> buscarPorUsuario(UUID usuarioId);

    /** Busca só o id, sem carregar a carteira (que deve ser carregada depois, já com bloqueio). */
    Optional<UUID> buscarIdPorUsuario(UUID usuarioId);

    /**
     * Busca a carteira e a bloqueia ({@code SELECT ... FOR UPDATE}) até o fim da transação.
     * Outra transação que tente bloquear a mesma carteira espera a primeira terminar.
     *
     * <p>Deve ser a primeira leitura desta carteira na transação: uma cópia carregada antes, sem
     * bloqueio, pode estar desatualizada quando o bloqueio for obtido.
     */
    Optional<Carteira> buscarPorIdComBloqueio(UUID id);
}
