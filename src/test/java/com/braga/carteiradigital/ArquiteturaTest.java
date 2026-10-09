package com.braga.carteiradigital;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * Testes de arquitetura: falham o build se alguém violar as fronteiras entre módulos
 * ou as regras da arquitetura hexagonal dentro de um módulo.
 */
class ArquiteturaTest {

    private static final ApplicationModules MODULOS = ApplicationModules.of(CarteiraDigitalApplication.class);

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.braga.carteiradigital");

    @Test
    void modulosDevemRespeitarSuasFronteiras() {
        // Sem ciclos entre módulos e sem acesso a pacotes internos de outro módulo
        MODULOS.verify();
    }

    @Test
    void dominioNaoDeveDependerDeFrameworksNemDeOutrasCamadas() {
        noClasses().that().resideInAPackage("..dominio..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta.persistence..", "..aplicacao..", "..adaptadores..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void aplicacaoNaoDeveDependerDeAdaptadores() {
        noClasses().that().resideInAPackage("..aplicacao..")
                .should().dependOnClassesThat().resideInAPackage("..adaptadores..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void geraDocumentacaoDosModulos() {
        // Diagramas C4/PlantUML em target/spring-modulith-docs
        new Documenter(MODULOS).writeDocumentation();
    }
}
