package org.example.inventarioservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        // H2 en memoria: el application.properties principal apunta a MySQL y en tests
        // no hay base levantada, por eso Hibernate no podía determinar el dialecto.
        "spring.datasource.url=jdbc:h2:mem:inventario_context;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class InventarioServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
