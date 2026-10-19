package com.aiproject.scaffold;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AiScaffoldApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiScaffoldApplication.class, args);
    }
}
