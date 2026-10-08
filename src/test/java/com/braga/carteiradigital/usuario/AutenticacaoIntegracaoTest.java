package com.braga.carteiradigital.usuario;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.braga.carteiradigital.suporte.IntegracaoTestBase;
import com.jayway.jsonpath.JsonPath;

@RecordApplicationEvents
class AutenticacaoIntegracaoTest extends IntegracaoTestBase {

    private static final String SENHA = "senha-segura-123";

    @Autowired
    private ApplicationEvents eventos;

    // ---------- Cadastro ----------

    @Test
    void deveCadastrarUsuarioSemExporOHashDaSenha() {
        assertThat(cadastrar("Ana Silva", "Ana@Email.com", SENHA))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .doesNotHavePath("$.senha")
                .doesNotHavePath("$.senhaHash")
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.id").isNotNull();
                    assertThat(json).extractingPath("$.nome").isEqualTo("Ana Silva");
                    assertThat(json).extractingPath("$.email").isEqualTo("ana@email.com");
                });
    }

    @Test
    void deveGuardarSenhaComoHashBCrypt() {
        cadastrar("Ana", "ana@email.com", SENHA);

        String hash = jdbc.queryForObject("SELECT senha_hash FROM usuarios", String.class);
        assertThat(hash).startsWith("{bcrypt}$2").doesNotContain(SENHA);
    }

    @Test
    void devePublicarEventoUsuarioCadastrado() {
        cadastrar("Ana", "ana@email.com", SENHA);

        UUID idNoBanco = jdbc.queryForObject("SELECT id FROM usuarios", UUID.class);
        assertThat(eventos.stream(UsuarioCadastrado.class))
                .containsExactly(new UsuarioCadastrado(idNoBanco, "Ana", "ana@email.com"));
    }

    @Test
    void deveRecusarEmailJaCadastradoIgnorandoMaiusculas() {
        cadastrar("Ana", "ana@email.com", SENHA);

        assertThat(cadastrar("Outra Ana", "ANA@email.com", SENHA))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.codigo").isEqualTo("email-ja-cadastrado");
    }

    @Test
    void deveRecusarCadastroComDadosInvalidos() {
        assertThat(cadastrar("", "invalido", "123"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .hasPath("$.campos.nome")
                .hasPath("$.campos.email")
                .hasPath("$.campos.senha")
                .extractingPath("$.codigo").isEqualTo("dados-invalidos");
    }

    @Test
    void deveRecusarCorpoMalformadoComCodigoPadronizado() {
        assertThat(mvc.post().uri("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content("{nome:"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.codigo").isEqualTo("requisicao-invalida");
    }

    // ---------- Login ----------

    @Test
    void deveEmitirTokenComCredenciaisCorretas() {
        cadastrar("Ana", "ana@email.com", SENHA);

        assertThat(login("ANA@email.com", SENHA))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.token").asString().matches("^[\\w-]+\\.[\\w-]+\\.[\\w-]+$");
                    assertThat(json).extractingPath("$.tipo").isEqualTo("Bearer");
                    assertThat(json).extractingPath("$.expiraEm").isNotNull();
                });
    }

    @Test
    void deveRecusarSenhaErradaEEmailInexistenteComAMesmaResposta() {
        cadastrar("Ana", "ana@email.com", SENHA);

        assertThat(login("ana@email.com", "senha-errada"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.codigo").isEqualTo("credenciais-invalidas");
        assertThat(login("naoexiste@email.com", SENHA))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.codigo").isEqualTo("credenciais-invalidas");
    }

    // ---------- Endpoint protegido ----------

    @Test
    void deveRetornarUsuarioDoToken() {
        cadastrar("Ana", "ana@email.com", SENHA);
        String token = tokenDe("ana@email.com");

        assertThat(mvc.get().uri("/api/usuarios/eu").header("Authorization", "Bearer " + token))
                .hasStatusOk()
                .bodyJson().extractingPath("$.email").isEqualTo("ana@email.com");
    }

    @Test
    void deveExigirToken() {
        assertThat(mvc.get().uri("/api/usuarios/eu")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deveRecusarTokenAdulterado() {
        cadastrar("Ana", "ana@email.com", SENHA);
        String token = tokenDe("ana@email.com");
        String adulterado = token.substring(0, token.length() - 4) + (token.endsWith("AAAA") ? "BBBB" : "AAAA");

        assertThat(mvc.get().uri("/api/usuarios/eu").header("Authorization", "Bearer " + adulterado))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deveRecusarTokenExpirado() {
        // O Spring tolera até 60s de diferença de relógio, por isso o token venceu há 5 minutos
        String expirado = assinarTokenDeTeste("carteira-api", Instant.now().minusSeconds(300));

        assertThat(mvc.get().uri("/api/usuarios/eu").header("Authorization", "Bearer " + expirado))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deveRecusarTokenDeOutroEmissor() {
        String deOutroEmissor = assinarTokenDeTeste("outra-api", Instant.now().plusSeconds(3600));

        assertThat(mvc.get().uri("/api/usuarios/eu").header("Authorization", "Bearer " + deOutroEmissor))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deveRecusarTokenSemAssinatura() {
        String semAssinatura = base64Url("{\"alg\":\"none\"}") + "."
                + base64Url("{\"sub\":\"" + UUID.randomUUID() + "\",\"iss\":\"carteira-api\",\"exp\":"
                        + Instant.now().plusSeconds(3600).getEpochSecond() + "}")
                + ".";

        assertThat(mvc.get().uri("/api/usuarios/eu").header("Authorization", "Bearer " + semAssinatura))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    // ---------- Auxiliares ----------

    private MvcTestResult cadastrar(String nome, String email, String senha) {
        return mvc.post().uri("/api/auth/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome": "%s", "email": "%s", "senha": "%s"}
                        """.formatted(nome, email, senha))
                .exchange();
    }

    private MvcTestResult login(String email, String senha) {
        return mvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "senha": "%s"}
                        """.formatted(email, senha))
                .exchange();
    }

    private String tokenDe(String email) {
        MvcTestResult resultado = login(email, SENHA);
        assertThat(resultado).hasStatusOk();
        return JsonPath.read(new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8), "$.token");
    }

    /** Assina um JWT "na mão" com o segredo de teste, para simular tokens expirados ou de outro emissor. */
    private static String assinarTokenDeTeste(String emissor, Instant expiraEm) {
        String cabecalho = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String corpo = base64Url("{\"sub\":\"" + UUID.randomUUID() + "\",\"iss\":\"" + emissor + "\",\"iat\":"
                + expiraEm.minusSeconds(3600).getEpochSecond() + ",\"exp\":" + expiraEm.getEpochSecond() + "}");
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(SEGREDO_JWT_TESTE.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] assinatura = hmac.doFinal((cabecalho + "." + corpo).getBytes(StandardCharsets.UTF_8));
            return cabecalho + "." + corpo + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(assinatura);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String base64Url(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
