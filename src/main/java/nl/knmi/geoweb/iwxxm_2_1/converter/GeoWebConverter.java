package nl.knmi.geoweb.iwxxm_2_1.converter;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.model.AviationWeatherMessage;
import nl.knmi.geoweb.backend.product.taf.Taf;

public interface GeoWebConverter <T extends AviationWeatherMessage> extends AviMessageSpecificConverter<Taf, T>{

}
