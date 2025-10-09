package it.eng.dome.invoicing.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import it.eng.dome.brokerage.utils.enumappers.TMF622EnumModule;
import it.eng.dome.brokerage.utils.enumappers.TMF637EnumModule;

@SpringBootApplication(scanBasePackages={"it.eng.dome.invoicing.engine", "it.eng.dome.invoicing.tedb"})
public class InvoicingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvoicingServiceApplication.class, args);
    }
    
    @Bean
    public RestTemplate getRestTemplate() {
       return new RestTemplate();
    }

 	@Bean
 	public com.fasterxml.jackson.databind.Module getTmf637EnumModule() {
        return new TMF637EnumModule();
    }

    @Bean
 	public com.fasterxml.jackson.databind.Module getTmf622EnumModule() {
        return new TMF622EnumModule();
    }

}
