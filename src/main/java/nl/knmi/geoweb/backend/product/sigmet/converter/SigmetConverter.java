package nl.knmi.geoweb.backend.product.sigmet.converter;

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
import fi.fmi.avi.model.sigmet.SIGMET;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.geoweb.backend.product.ProductConverter;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet;
import nl.knmi.geoweb.iwxxm_2_1.converter.GeoWebSIGMETConverter;
import nl.knmi.geoweb.iwxxm_2_1.converter.conf.GeoWebConverterConfig;

@Configuration
@Import({IWXXMConverter.class,
	GeoWebSIGMETConverter.class, GeoWebConverterConfig.class})
public class SigmetConverter implements ProductConverter<Sigmet>{
	@Autowired
	private AviMessageSpecificConverter<SIGMET, String> sigmetIWXXMStringSerializer;
	
	@Autowired
	private AviMessageSpecificConverter<SIGMET, Document> sigmetIWXXMDOMSerializer;
	
	@Autowired
	@Qualifier("aviSigmetSpecificMessageConverter")
	private AviMessageSpecificConverter<Sigmet,SIGMET> geoWebSigmetImporter;
	
	@Bean("aviSigmetMessageConverter")
	public AviMessageConverter aviMessageConverter() {
		AviMessageConverter p = new AviMessageConverter();
		p.setMessageSpecificConverter(GeoWebConverterConfig.GEOWEBSIGMET_TO_SIGMET_POJO, geoWebSigmetImporter);
		p.setMessageSpecificConverter(IWXXMConverter.SIGMET_POJO_TO_IWXXM21_DOM, sigmetIWXXMDOMSerializer);
		p.setMessageSpecificConverter(IWXXMConverter.SIGMET_POJO_TO_IWXXM21_STRING, sigmetIWXXMStringSerializer);
		return p;
	}
	
	public String ToIWXXM_2_1(Sigmet geoWebSigmet) {


		ConversionResult<SIGMET> result = geoWebSigmetImporter.convertMessage(geoWebSigmet, ConversionHints.SIGMET);
		if (ConversionResult.Status.SUCCESS == result.getStatus()) {
			Debug.errprintln("SUCCESS");
			SIGMET pojo = result.getConvertedMessage().get();
			Debug.errprintln("POJO:"+pojo);
			ConversionResult<String>iwxxmResult=sigmetIWXXMStringSerializer.convertMessage(pojo, ConversionHints.SIGMET);
			if (ConversionResult.Status.SUCCESS == iwxxmResult.getStatus()) {
				return iwxxmResult.getConvertedMessage().get();
			} else {
				Debug.errprintln("ERR: "+iwxxmResult.getStatus());
				for (ConversionIssue iss:iwxxmResult.getConversionIssues()) {
					Debug.errprintln("iss: "+iss.getMessage());
				}
			}
		}else {
			Debug.errprintln("Sigmet2IWXXM failed");
			Debug.errprintln("ERR: "+result.getStatus());
			for (ConversionIssue iss:result.getConversionIssues()) {
				Debug.errprintln("iss: "+iss.getMessage());
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
