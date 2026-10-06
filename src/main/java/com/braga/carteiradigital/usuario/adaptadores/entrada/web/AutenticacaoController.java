package com.braga.carteiradigital.usuario.adaptadores.entrada.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.braga.carteiradigital.usuario.aplicacao.porta.entrada.AutenticarUsuarioUseCase;
import com.braga.carteiradigital.usuario.aplicacao.porta.entrada.CadastrarUsuarioUseCase;

import jakarta.validation.Valid;

/** Endpoints públicos (não exigem token). */
@RestController
@RequestMapping("/api/auth")
class AutenticacaoController {

    private final CadastrarUsuarioUseCase cadastrarUsuario;
    private final AutenticarUsuarioUseCase autenticarUsuario;

    AutenticacaoController(CadastrarUsuarioUseCase cadastrarUsuario, AutenticarUsuarioUseCase autenticarUsuario) {
        this.cadastrarUsuario = cadastrarUsuario;
        this.autenticarUsuario = autenticarUsuario;
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    UsuarioResponse cadastrar(@Valid @RequestBody CadastroRequest requisicao) {
        var comando = new CadastrarUsuarioUseCase.Comando(requisicao.nome(), requisicao.email(), requisicao.senha());
        return UsuarioResponse.de(cadastrarUsuario.cadastrar(comando));
    }

    @PostMapping("/login")
    TokenResponse login(@Valid @RequestBody LoginRequest requisicao) {
        return TokenResponse.de(autenticarUsuario.autenticar(requisicao.email(), requisicao.senha()));
    }
}
