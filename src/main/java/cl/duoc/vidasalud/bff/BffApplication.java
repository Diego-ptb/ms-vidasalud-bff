package cl.duoc.vidasalud.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import cl.duoc.vidasalud.bff.config.DownstreamProperties;
import cl.duoc.vidasalud.bff.config.SecurityProperties;

@SpringBootApplication
@EnableConfigurationProperties({ DownstreamProperties.class, SecurityProperties.class })
public class BffApplication {

    public static void main(String[] args) {
        SpringApplication.run(BffApplication.class, args);
    }
}
