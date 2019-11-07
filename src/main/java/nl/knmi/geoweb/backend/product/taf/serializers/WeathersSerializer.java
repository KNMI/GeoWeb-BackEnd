package nl.knmi.geoweb.backend.product.taf.serializers;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import nl.knmi.geoweb.backend.product.taf.Taf.Forecast;


public class WeathersSerializer extends StdSerializer<List<Forecast.TAFWeather>> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public WeathersSerializer () {
		this(null);
	}
	
	public WeathersSerializer(Class<List<Forecast.TAFWeather>> t) {
		super(t);
	}
	
	@Override
	public void serialize(
			List<Forecast.TAFWeather> value, JsonGenerator jgen, SerializerProvider provider)
	throws IOException, JsonProcessingException {
		if (value != null && value.size() == 1 && value.get(0) != null) {
			Boolean v = value.get(0).getIsNSW();
			if (v != null && v == true)
				jgen.writeString("NSW");
			else 
				jgen.writeObject(value);
		} else {
			jgen.writeObject(value);
		}
	}
	//@Override //TODO
	public boolean isEmpty(SerializerProvider provider, List<Forecast.TAFWeather> value) {
		return value == null || value.size() == 0;
	}
}