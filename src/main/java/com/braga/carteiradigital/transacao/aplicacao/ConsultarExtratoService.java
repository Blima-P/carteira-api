package com.braga.carteiradigital.transacao.aplicacao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.braga.carteiradigital.transacao.aplicacao.porta.entrada.ConsultarExtratoUseCase;
import com.braga.carteiradigital.transacao.aplicacao.porta.saida.BuscadorDeUsuarios;
import com.braga.carteiradigital.transacao.aplicacao.porta.saida.MovimentadorDeCarteira;
import com.braga.carteiradigital.transacao.aplicacao.porta.saida.MovimentadorDeCarteira.Lancamento;
import com.braga.carteiradigital.transacao.aplicacao.porta.saida.TransacaoRepository;
import com.braga.carteiradigital.transacao.dominio.Extrato;
import com.braga.carteiradigital.transacao.dominio.ItemDoExtrato;
import com.braga.carteiradigital.transacao.dominio.PosicaoNoExtrato;
import com.braga.carteiradigital.transacao.dominio.TipoTransacao;
import com.braga.carteiradigital.transacao.dominio.Transacao;

/**
 * Monta o extrato a partir do livro-razão da carteira (valores e saldos) e das transações
 * (tipo, descrição e contraparte). Cada página faz um número fixo de consultas, em lote,
 * independente da quantidade de itens.
 */
@Service
class ConsultarExtratoService implements ConsultarExtratoUseCase {

    private final MovimentadorDeCarteira carteiras;
    private final TransacaoRepository transacoes;
    private final BuscadorDeUsuarios usuarios;

    ConsultarExtratoService(MovimentadorDeCarteira carteiras, TransacaoRepository transacoes,
            BuscadorDeUsuarios usuarios) {
        this.carteiras = carteiras;
        this.transacoes = transacoes;
        this.usuarios = usuarios;
    }

    @Override
    @Transactional(readOnly = true)
    public Extrato consultar(Consulta consulta) {
        UUID minhaCarteira = carteiras.idDaCarteiraDoUsuario(consulta.usuarioId());

        // Um item a mais só para saber se existe uma próxima página, sem precisar de um COUNT
        List<Lancamento> lancamentos = carteiras.lancamentos(minhaCarteira, consulta.depoisDe(), consulta.tamanho() + 1);
        boolean temProximaPagina = lancamentos.size() > consulta.tamanho();
        List<Lancamento> pagina = temProximaPagina ? lancamentos.subList(0, consulta.tamanho()) : lancamentos;
        if (pagina.isEmpty()) {
            return new Extrato(List.of(), null);
        }

        Set<UUID> transacaoIds = pagina.stream().map(Lancamento::transacaoId).collect(Collectors.toSet());
        Map<UUID, Transacao> transacoesPorId = transacoes.buscarPorIds(transacaoIds).stream()
                .collect(Collectors.toMap(Transacao::id, Function.identity()));
        Map<UUID, String> contrapartes = nomesDasContrapartes(minhaCarteira, transacoesPorId.values());

        List<ItemDoExtrato> itens = pagina.stream().map(lancamento -> {
            Transacao transacao = transacoesPorId.get(lancamento.transacaoId());
            if (transacao == null) {
                throw new IllegalStateException("lançamento sem transação: " + lancamento.transacaoId());
            }
            return new ItemDoExtrato(transacao.id(), transacao.tipo(), lancamento.natureza(), lancamento.valor(),
                    lancamento.saldoApos(), transacao.descricao(), contrapartes.get(transacao.id()),
                    lancamento.posicao().criadoEm());
        }).toList();

        PosicaoNoExtrato proximaPagina = temProximaPagina ? pagina.getLast().posicao() : null;
        return new Extrato(itens, proximaPagina);
    }

    /** O nome do outro usuário de cada transferência, indexado pelo id da transação. */
    private Map<UUID, String> nomesDasContrapartes(UUID minhaCarteira, Iterable<Transacao> transacoesDaPagina) {
        Map<UUID, UUID> outraCarteiraPorTransacao = new HashMap<>();
        for (Transacao transacao : transacoesDaPagina) {
            if (transacao.tipo() == TipoTransacao.TRANSFERENCIA) {
                UUID outra = minhaCarteira.equals(transacao.carteiraOrigemId())
                        ? transacao.carteiraDestinoId()
                        : transacao.carteiraOrigemId();
                outraCarteiraPorTransacao.put(transacao.id(), outra);
            }
        }
        if (outraCarteiraPorTransacao.isEmpty()) {
            return Map.of();
        }

        Map<UUID, UUID> donos = carteiras.donosDasCarteiras(new HashSet<>(outraCarteiraPorTransacao.values()));
        Map<UUID, String> nomes = usuarios.nomesPorId(new HashSet<>(donos.values()));

        Map<UUID, String> contrapartes = new HashMap<>();
        outraCarteiraPorTransacao.forEach((transacaoId, carteira) -> {
            String nome = nomes.get(donos.get(carteira));
            if (nome != null) {
                contrapartes.put(transacaoId, nome);
            }
        });
        return contrapartes;
    }
}
