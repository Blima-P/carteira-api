package com.braga.carteiradigital;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.braga.carteiradigital.suporte.IntegracaoTestBase;

@SpringBootTest(properties = {
        "springdoc.api-docs.enabled=true",
        "springdoc.swagger-ui.enabled=true"
})
class DocumentacaoIntegracaoTest extends IntegracaoTestBase {

    @Test
    void deveDocumentarSomenteOsEndpointsDisponiveisComSegurancaJwt() {
        MvcTestResult resposta = mvc.get().uri("/v3/api-docs").exchange();
        assertThat(resposta).hasStatusOk();
        String versaoDocumentada = lerJson(resposta, "$.info.version");
        assertThat(versaoDocumentada).isNotBlank().doesNotContain("@");

        Map<String, Object> rotas = lerJson(resposta, "$.paths");
        assertThat(rotas).containsOnlyKeys("/api/auth/cadastro", "/api/auth/login",
                "/api/usuarios/eu", "/api/carteiras/minha",
                "/api/transacoes/depositos", "/api/transacoes/transferencias");
        String esquemaAutenticacao = lerJson(resposta, "$.components.securitySchemes.bearerAuth.scheme");
        assertThat(esquemaAutenticacao).isEqualTo("bearer");
        List<Map<String, Object>> seguranca = lerJson(resposta, "$.security");
        assertThat(seguranca).containsExactly(Map.of("bearerAuth", List.of()));
        List<Object> segurancaLogin = lerJson(resposta, "$.paths['/api/auth/login'].post.security");
        assertThat(segurancaLogin).isEmpty();
        List<Object> segurancaCadastro = lerJson(resposta, "$.paths['/api/auth/cadastro'].post.security");
        assertThat(segurancaCadastro).isEmpty();
    }

    @Test
    void deveDisponibilizarSwaggerSemLiberarEndpointsProtegidos() {
        assertThat(mvc.get().uri("/swagger-ui.html")).hasStatus(HttpStatus.FOUND);
        assertThat(mvc.get().uri("/swagger-ui/index.html")).hasStatusOk();
        assertThat(mvc.get().uri("/api/carteiras/minha")).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
