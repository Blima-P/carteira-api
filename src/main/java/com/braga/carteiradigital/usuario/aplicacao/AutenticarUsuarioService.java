package com.braga.carteiradigital.usuario.aplicacao;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.braga.carteiradigital.usuario.aplicacao.porta.entrada.AutenticarUsuarioUseCase;
import com.braga.carteiradigital.usuario.aplicacao.porta.saida.CodificadorDeSenha;
import com.braga.carteiradigital.usuario.aplicacao.porta.saida.EmissorDeToken;
import com.braga.carteiradigital.usuario.aplicacao.porta.saida.UsuarioRepository;
import com.braga.carteiradigital.usuario.dominio.CredenciaisInvalidasException;
import com.braga.carteiradigital.usuario.dominio.Email;
import com.braga.carteiradigital.usuario.dominio.TokenAcesso;
import com.braga.carteiradigital.usuario.dominio.Usuario;

@Service
class AutenticarUsuarioService implements AutenticarUsuarioUseCase {

    private final UsuarioRepository usuarios;
    private final CodificadorDeSenha codificadorDeSenha;
    private final EmissorDeToken emissorDeToken;
    /** Hash de uma senha qualquer, usado quando o e-mail não existe (ver {@link #autenticar}). */
    private final String hashFicticio;

    AutenticarUsuarioService(UsuarioRepository usuarios, CodificadorDeSenha codificadorDeSenha,
            EmissorDeToken emissorDeToken) {
        this.usuarios = usuarios;
        this.codificadorDeSenha = codificadorDeSenha;
        this.emissorDeToken = emissorDeToken;
        this.hashFicticio = codificadorDeSenha.codificar("senha-ficticia-para-tempo-constante");
    }

    @Override
    @Transactional(readOnly = true)
    public TokenAcesso autenticar(String emailInformado, String senha) {
        Optional<Usuario> usuario = buscarUsuario(emailInformado);

        // Mesmo sem usuário, compara a senha com um hash fictício. Assim o tempo de resposta é
        // parecido nos dois casos e um atacante não descobre quais e-mails existem (timing attack).
        String hash = usuario.map(Usuario::senhaHash).orElse(hashFicticio);
        boolean senhaConfere = codificadorDeSenha.confere(senha, hash);

        if (usuario.isEmpty() || !senhaConfere) {
            throw new CredenciaisInvalidasException();
        }
        return emissorDeToken.emitir(usuario.get());
    }

    private Optional<Usuario> buscarUsuario(String emailInformado) {
        try {
            return usuarios.buscarPorEmail(new Email(emailInformado));
        } catch (IllegalArgumentException emailInvalido) {
            return Optional.empty();
        }
    }
}
