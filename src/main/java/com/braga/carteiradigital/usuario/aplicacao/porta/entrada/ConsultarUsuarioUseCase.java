package com.braga.carteiradigital.usuario.aplicacao.porta.entrada;

import java.util.UUID;

import com.braga.carteiradigital.usuario.dominio.Usuario;

public interface ConsultarUsuarioUseCase {

    Usuario buscarPorId(UUID id);
}
