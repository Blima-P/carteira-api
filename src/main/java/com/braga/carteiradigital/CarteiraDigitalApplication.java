package com.braga.carteiradigital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Monólito modular: cada subpacote direto (usuario, carteira, transacao) é um módulo de negócio
 * independente, verificado pelo Spring Modulith. Veja docs/adr/0001-monolito-modular-hexagonal.md.
 */
@SpringBootApplication
public class CarteiraDigitalApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarteiraDigitalApplication.class, args);
    }
}
