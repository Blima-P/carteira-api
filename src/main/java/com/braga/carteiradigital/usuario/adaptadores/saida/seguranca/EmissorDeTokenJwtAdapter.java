package com.braga.carteiradigital.usuario.adaptadores.saida.seguranca;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.braga.carteiradigital.compartilhado.seguranca.JwtProperties;
import com.braga.carteiradigital.usuario.aplicacao.porta.saida.EmissorDeToken;
import com.braga.carteiradigital.usuario.dominio.TokenAcesso;
import com.braga.carteiradigital.usuario.dominio.Usuario;

@Component
class EmissorDeTokenJwtAdapter implements EmissorDeToken {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties propriedades;
    private final Clock relogio;

    EmissorDeTokenJwtAdapter(JwtEncoder jwtEncoder, JwtProperties propriedades, Clock relogio) {
        this.jwtEncoder = jwtEncoder;
        this.propriedades = propriedades;
        this.relogio = relogio;
    }

    @Override
    public TokenAcesso emitir(Usuario usuario) {
        Instant agora = Instant.now(relogio);
        Instant expiraEm = agora.plus(propriedades.expiracao());

        // O payload do JWT é só Base64, qualquer um consegue ler: nunca coloque dados sensíveis aqui
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(propriedades.emissor())
                .subject(usuario.id().toString())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .claim("email", usuario.email().valor())
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new TokenAcesso(token, expiraEm);
    }
}
