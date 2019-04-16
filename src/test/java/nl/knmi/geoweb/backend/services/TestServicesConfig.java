package nl.knmi.geoweb.backend.services;

import static org.mockito.Mockito.mock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import nl.knmi.geoweb.backend.aviation.FIRStore;
import nl.knmi.geoweb.backend.datastore.ProductExporter;
import nl.knmi.geoweb.backend.product.airmet.Airmet;
import nl.knmi.geoweb.backend.product.airmet.AirmetStore;

@Profile("test")
@Configuration
public class TestServicesConfig {

    @Bean
    @Primary
    public AirmetStore getAirmetStore() {
        return mock(AirmetStore.class);
    }

    @Bean
    @Primary
    public FIRStore getFirStore() {
        return mock(FIRStore.class);
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public <P> ProductExporter<P> getProductExporter() {
        return mock(ProductExporter.class);
    }
}
