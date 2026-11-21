package org.example.catalogoservice;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

@SpringBootApplication
@EnableCaching
public class CatalogoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogoServiceApplication.class, args);
    }

    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("evento", "eventos-listado", "eventos-busqueda");
        manager.setCaffeine(Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(Duration.ofSeconds(60)));
        return manager;
    }
}
