package com.braga.carteiradigital.usuario.adaptadores.saida.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.braga.carteiradigital.suporte.IntegracaoTestBase;
import com.braga.carteiradigital.usuario.aplicacao.porta.saida.UsuarioRepository;
import com.braga.carteiradigital.usuario.dominio.Email;
import com.braga.carteiradigital.usuario.dominio.EmailJaCadastradoException;
import com.braga.carteiradigital.usuario.dominio.Usuario;

class UsuarioPersistenceAdapterTest extends IntegracaoTestBase {

    @Autowired
    private UsuarioRepository usuarios;

    @Test
    void deveSalvarEReconstruirUsuario() {
        Usuario ana = Usuario.novo("Ana", new Email("ana@email.com"), "hash", Clock.systemUTC());

        usuarios.salvar(ana);

        assertThat(usuarios.buscarPorId(ana.id())).contains(ana);
        assertThat(usuarios.buscarPorEmail(new Email("ANA@email.com"))).contains(ana);
    }

    /**
     * Simula dois cadastros simultâneos: os dois passam pela verificação {@code existePorEmail}
     * antes de qualquer um salvar. Quem barra o segundo é a constraint do banco.
     */
    @Test
    void deveConverterViolacaoDeEmailUnicoEmErroDeNegocio() {
        usuarios.salvar(Usuario.novo("Ana", new Email("ana@email.com"), "hash", Clock.systemUTC()));
        Usuario duplicado = Usuario.novo("Outra Ana", new Email("ana@email.com"), "hash", Clock.systemUTC());

        assertThatThrownBy(() -> usuarios.salvar(duplicado)).isInstanceOf(EmailJaCadastradoException.class);
    }
}
