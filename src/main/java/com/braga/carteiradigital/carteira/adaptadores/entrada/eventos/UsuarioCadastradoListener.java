package com.braga.carteiradigital.carteira.adaptadores.entrada.eventos;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.braga.carteiradigital.carteira.aplicacao.porta.entrada.CriarCarteiraUseCase;
import com.braga.carteiradigital.usuario.UsuarioCadastrado;

/**
 * Cria a carteira assim que um usuário se cadastra.
 *
 * <p>{@code @EventListener} roda de forma síncrona, dentro da transação do cadastro: se a carteira
 * não puder ser criada, o cadastro inteiro é desfeito (nunca existe usuário sem carteira).
 * Um {@code @ApplicationModuleListener} rodaria depois do commit, em outra transação, e abriria
 * uma janela em que o usuário existe mas ainda não tem carteira.
 */
@Component
class UsuarioCadastradoListener {

    private final CriarCarteiraUseCase criarCarteira;

    UsuarioCadastradoListener(CriarCarteiraUseCase criarCarteira) {
        this.criarCarteira = criarCarteira;
    }

    @EventListener
    void aoCadastrarUsuario(UsuarioCadastrado evento) {
        criarCarteira.criarPara(evento.usuarioId());
    }
}
