package it.eng.dome.invoicing.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages={"it.eng.dome.invoicing.engine", "it.eng.dome.invoicing.tedb"})
public class InvoicingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvoicingServiceApplication.class, args);
    }

}
