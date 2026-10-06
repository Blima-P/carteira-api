package com.braga.carteiradigital.usuario.aplicacao;

import java.time.Clock;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.braga.carteiradigital.usuario.UsuarioCadastrado;
import com.braga.carteiradigital.usuario.aplicacao.porta.entrada.CadastrarUsuarioUseCase;
import com.braga.carteiradigital.usuario.aplicacao.porta.saida.CodificadorDeSenha;
import com.braga.carteiradigital.usuario.aplicacao.porta.saida.UsuarioRepository;
import com.braga.carteiradigital.usuario.dominio.Email;
import com.braga.carteiradigital.usuario.dominio.EmailJaCadastradoException;
import com.braga.carteiradigital.usuario.dominio.Usuario;

@Service
class CadastrarUsuarioService implements CadastrarUsuarioUseCase {

    private final UsuarioRepository usuarios;
    private final CodificadorDeSenha codificadorDeSenha;
    private final ApplicationEventPublisher eventos;
    private final Clock relogio;

    CadastrarUsuarioService(UsuarioRepository usuarios, CodificadorDeSenha codificadorDeSenha,
            ApplicationEventPublisher eventos, Clock relogio) {
        this.usuarios = usuarios;
        this.codificadorDeSenha = codificadorDeSenha;
        this.eventos = eventos;
        this.relogio = relogio;
    }

    @Override
    @Transactional
    public Usuario cadastrar(Comando comando) {
        Email email = new Email(comando.email());
        // Verificação rápida. Se dois cadastros simultâneos passarem daqui, a constraint
        // uk_usuarios_email do banco barra o segundo (o repositório converte para a mesma exceção).
        if (usuarios.existePorEmail(email)) {
            throw new EmailJaCadastradoException();
        }

        Usuario usuario = Usuario.novo(comando.nome(), email, codificadorDeSenha.codificar(comando.senha()), relogio);
        usuarios.salvar(usuario);

        // Publicado dentro da transação: quem escutar de forma síncrona participa do mesmo commit/rollback
        eventos.publishEvent(new UsuarioCadastrado(usuario.id(), usuario.nome(), usuario.email().valor()));
        return usuario;
    }
}
