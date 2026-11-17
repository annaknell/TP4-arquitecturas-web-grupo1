package com.tudai.filavirtualservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FilaVirtualServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FilaVirtualServiceApplication.class, args);
    }

}
