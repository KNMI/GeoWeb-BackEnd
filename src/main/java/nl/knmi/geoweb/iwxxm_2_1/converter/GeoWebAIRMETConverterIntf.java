package nl.knmi.geoweb.iwxxm_2_1.converter;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.model.AviationWeatherMessage;

public interface GeoWebAIRMETConverterIntf<T extends AviationWeatherMessage> extends AviMessageSpecificConverter<nl.knmi.geoweb.backend.product.airmet.Airmet, T>{

}
