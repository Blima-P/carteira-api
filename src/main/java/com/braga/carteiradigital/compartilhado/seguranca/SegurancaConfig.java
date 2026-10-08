package com.braga.carteiradigital.compartilhado.seguranca;

import java.nio.charset.StandardCharsets;
import java.time.Clock;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * API stateless protegida por JWT: cada requisição traz {@code Authorization: Bearer <token>}
 * e o Spring Security valida assinatura, emissor e expiração. Não existe sessão no servidor.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SegurancaConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF só é necessário quando o navegador envia credenciais automaticamente (cookies)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(HttpMethod.POST, "/api/auth/cadastro", "/api/auth/login").permitAll()
                        // Em produção só health e info são expostos (ver application.yml)
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    SecretKey chaveAssinaturaJwt(JwtProperties propriedades) {
        return new SecretKeySpec(propriedades.segredo().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey chaveAssinaturaJwt) {
        return NimbusJwtEncoder.withSecretKey(chaveAssinaturaJwt).build();
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey chaveAssinaturaJwt, JwtProperties propriedades) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chaveAssinaturaJwt)
                // Fixa o algoritmo: tokens assinados de outra forma (ex.: "alg": "none") são recusados
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Valida expiração (exp/nbf) e se o token foi emitido por esta API (iss)
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(propriedades.emissor()));
        return decoder;
    }

    @Bean
    Clock relogio() {
        return Clock.systemUTC();
    }
}
