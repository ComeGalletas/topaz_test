package com.example.supportorders.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    /** Reloj inyectable: las fechas las genera el servidor y los tests pueden fijarlas. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
