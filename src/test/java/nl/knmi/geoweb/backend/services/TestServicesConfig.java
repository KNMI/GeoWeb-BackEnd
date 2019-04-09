package nl.knmi.geoweb.backend.services;

import static org.mockito.Mockito.mock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import nl.knmi.geoweb.backend.product.airmet.AirmetStore;

@Profile("test")
@Configuration
public class TestServicesConfig {

    @Bean
    @Primary
    public AirmetStore getAirmetStore() {
        return mock(AirmetStore.class);
    }
}
