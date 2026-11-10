package com.tudai.pagos.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita @Scheduled en el servicio de pagos (lo usa ReintegroScheduler). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
