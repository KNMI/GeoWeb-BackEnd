package nl.knmi.geoweb.backend.product.testproduct.converter;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import fi.fmi.avi.converter.AviMessageConverter;
import nl.knmi.geoweb.backend.product.ProductConverter;
import nl.knmi.geoweb.backend.product.testproduct.TestProduct;
import nl.knmi.geoweb.iwxxm_2_1.converter.conf.GeoWebConverterConfig;

@Configuration
@Import({ GeoWebConverterConfig.class})
public class TestProductConverter implements ProductConverter<TestProduct>{
    @Override
    public AviMessageConverter aviMessageConverter() {
        return null;
    }

    @Override
    public String ToIWXXM_2_1(TestProduct p) {
        return null;
    }

    @Override
    public String getLocationIndicatorWMO() {
        return null;
    }

}
