package it.eng.dome.invoicing.engine;


import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
class InvoicingSchedulerApplicationTests {

    @Autowired
    private InvoicingServiceApplication invoicingServiceController;

    @Test
    void contextLoads() {
        // to ensure that controller is getting created inside the application context
        assertNotNull(invoicingServiceController);
    }

}
