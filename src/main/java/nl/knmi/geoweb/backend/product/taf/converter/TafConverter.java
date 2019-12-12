package nl.knmi.geoweb.backend.product.taf.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.immutable.GeoPositionImpl;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.immutable.TAFImpl;
import fi.fmi.avi.model.taf.immutable.TAFReferenceImpl;
import lombok.extern.slf4j.Slf4j;
import nl.knmi.geoweb.backend.aviation.AirportInfo;
import nl.knmi.geoweb.backend.aviation.AirportStore;
import nl.knmi.geoweb.backend.product.ProductConverter;
import nl.knmi.geoweb.backend.product.taf.Taf;
import nl.knmi.geoweb.iwxxm_2_1.converter.GeoWebTAFConverter;
import nl.knmi.geoweb.iwxxm_2_1.converter.GeoWebTafInConverter;
import nl.knmi.geoweb.iwxxm_2_1.converter.conf.GeoWebConverterConfig;

@Slf4j
@Configuration
// @ComponentScan(value = "nl.knmi.geoweb.backend.admin")
@Import({ IWXXMConverter.class, GeoWebTAFConverter.class, GeoWebTafInConverter.class })
public class TafConverter implements ProductConverter<Taf> {

    @Autowired
    private AviMessageSpecificConverter<TAF, Document> tafIWXXMDOMSerializer;

    @Autowired
    private AviMessageSpecificConverter<TAF, String> tafIWXXMStringSerializer;

    @Autowired
    @Qualifier("aviTafSpecificMessageConverter")
    private AviMessageSpecificConverter<Taf, TAF> geoWebTafImporter;

    @Autowired
    @Qualifier("aviTafInSpecificMessageConverter")
    private AviMessageSpecificConverter<TAF, Taf> geoWebTafInImporter;

    @Autowired
    @Qualifier("tafObjectMapper")
    private ObjectMapper tafObjectMapper;

    @Bean
    public AviMessageConverter aviMessageConverter() {
        AviMessageConverter p = new AviMessageConverter();
        p.setMessageSpecificConverter(IWXXMConverter.TAF_POJO_TO_IWXXM21_DOM, tafIWXXMDOMSerializer);
        p.setMessageSpecificConverter(IWXXMConverter.TAF_POJO_TO_IWXXM21_STRING, tafIWXXMStringSerializer);
        p.setMessageSpecificConverter(GeoWebConverterConfig.GEOWEBTAF_TO_TAF_POJO, geoWebTafImporter);
        p.setMessageSpecificConverter(GeoWebConverterConfig.TAF_TO_GEOWEBTAF_POJO, geoWebTafInImporter);
        return p;
    }

    @Autowired
    AirportStore airportStore;

    public String ToIWXXM_2_1(Taf geoWebTaf) {
        ConversionResult<TAF> result = geoWebTafImporter.convertMessage(geoWebTaf, ConversionHints.TAF);
        if (ConversionResult.Status.SUCCESS == result.getStatus()) {
            TAFImpl.Builder convertedTAF = TAFImpl.Builder.from(result.getConvertedMessage().get());
            String airportName = geoWebTaf.getMetadata().getLocation();
            AirportInfo airportInfo = airportStore.lookup(airportName);
            if (airportInfo != null) {
                AerodromeImpl.Builder ad = AerodromeImpl.Builder.from(convertedTAF.getAerodrome());

                GeoPositionImpl.Builder refPoint = GeoPositionImpl.builder()
                        .setCoordinateReferenceSystemId(airportInfo.getGeoLocation().getEPSG())
                        .addCoordinates(new double[] { airportInfo.getGeoLocation().getLon(),
                                airportInfo.getGeoLocation().getLat() })
                        .setElevationValue(airportInfo.getFieldElevation()).setElevationUom("m");
                ad.setReferencePoint(refPoint.build());

                ad.setFieldElevationValue(airportInfo.getFieldElevation());
                ad.setLocationIndicatorICAO(airportInfo.getICAOName());
                ad.setName(airportInfo.getName());
                ad.setDesignator(airportName);

                convertedTAF.setAerodrome(ad.build());

                if (convertedTAF.getReferredReport().isPresent()) {
                    AerodromeImpl.Builder referredAd = AerodromeImpl.Builder
                            .from(convertedTAF.getReferredReport().get().getAerodrome());
                    referredAd.setLocationIndicatorICAO(airportInfo.getICAOName());
                    referredAd.setReferencePoint(refPoint.build());
                    referredAd.setDesignator(airportName);
                    referredAd.setName(airportInfo.getName());
                    referredAd.setFieldElevationValue(airportInfo.getFieldElevation());
                    TAFReferenceImpl.Builder tafReference = TAFReferenceImpl.Builder
                            .from(convertedTAF.getReferredReport().get());
                    tafReference.setAerodrome(referredAd.build());
                    convertedTAF.setReferredReport(tafReference.build());
                }
            } else {
                log.error("airportinfo for " + airportName + " not found");
            }
            ConversionResult<String> iwxxmResult = tafIWXXMStringSerializer.convertMessage(convertedTAF.build(), ConversionHints.TAF);
            if ((ConversionResult.Status.SUCCESS == iwxxmResult.getStatus())||(ConversionResult.Status.WITH_WARNINGS == iwxxmResult.getStatus())) {
                for (ConversionIssue iss : iwxxmResult.getConversionIssues()) {
                    log.error("iss: " + iss.getMessage());
                }
                return iwxxmResult.getConvertedMessage().get();
            } else {
                log.error("IWXXM conversation failed: " + iwxxmResult.getStatus());
                for (ConversionIssue iss : iwxxmResult.getConversionIssues()) {
                    log.error("issue: " + iss.getMessage());
                }
            } //TODO
        } else {
            log.error("Taf2IWXXM failed: " + result.getStatus());
            for (ConversionIssue iss : result.getConversionIssues()) {
                log.error("issue: " + iss.getMessage());
            }
        }
        return "FAIL";
    }

	@Value("${geoweb.locationIndicatorWMO}")
	String locationIndicatorWMO;

	@Override
	public String getLocationIndicatorWMO() {
		return locationIndicatorWMO;
	}
}
