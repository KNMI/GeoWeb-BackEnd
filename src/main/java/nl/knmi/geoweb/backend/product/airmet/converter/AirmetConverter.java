package nl.knmi.geoweb.backend.product.airmet.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.w3c.dom.Document;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.iwxxm.conf.IWXXMConverter;
import fi.fmi.avi.model.sigmet.AIRMET;
import lombok.extern.slf4j.Slf4j;
import nl.knmi.geoweb.backend.product.ProductConverter;
import nl.knmi.geoweb.backend.product.airmet.Airmet;
import nl.knmi.geoweb.iwxxm_2_1.converter.GeoWebAIRMETConverter;
import nl.knmi.geoweb.iwxxm_2_1.converter.conf.GeoWebConverterConfig;

@Slf4j
@Configuration
@Import({ IWXXMConverter.class,
		GeoWebAIRMETConverter.class, GeoWebConverterConfig.class})
public class AirmetConverter implements ProductConverter<Airmet>{
	@Autowired
	private AviMessageSpecificConverter<AIRMET, String> airmetIWXXMStringSerializer;
	
	@Autowired
	private AviMessageSpecificConverter<AIRMET, Document> airmetIWXXMDOMSerializer;
	
	@Autowired
	@Qualifier("aviAirmetSpecificMessageConverter")
	private AviMessageSpecificConverter<Airmet,AIRMET> geoWebAirmetImporter;

	@Bean("aviAirmetMessageConverter")
	public AviMessageConverter aviMessageConverter() {
		AviMessageConverter p = new AviMessageConverter();
		p.setMessageSpecificConverter(GeoWebConverterConfig.GEOWEBAIRMET_TO_AIRMET_POJO, geoWebAirmetImporter);
		p.setMessageSpecificConverter(IWXXMConverter.AIRMET_POJO_TO_IWXXM21_DOM, airmetIWXXMDOMSerializer);
		p.setMessageSpecificConverter(IWXXMConverter.AIRMET_POJO_TO_IWXXM21_STRING, airmetIWXXMStringSerializer);
		return p;
	}

	public String ToIWXXM_2_1(Airmet geoWebAirmet) {
		ConversionResult<AIRMET> result = geoWebAirmetImporter.convertMessage(geoWebAirmet, ConversionHints.AIRMET);
		if (ConversionResult.Status.SUCCESS == result.getStatus()) {
			AIRMET pojo = result.getConvertedMessage().get();
			ConversionResult<String>iwxxmResult=airmetIWXXMStringSerializer.convertMessage(pojo, ConversionHints.AIRMET);
			if (ConversionResult.Status.SUCCESS == iwxxmResult.getStatus()) {
				return iwxxmResult.getConvertedMessage().get();
			} else {
				log.error("IWXXM conversion failed: " + iwxxmResult.getStatus());
				for (ConversionIssue iss:iwxxmResult.getConversionIssues()) {
					log.error("issue: " + iss.getMessage());
				}
			}
		}else {
			log.error("Airmet2IWXXM failed: " + result.getStatus());
			for (ConversionIssue iss:result.getConversionIssues()) {
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
