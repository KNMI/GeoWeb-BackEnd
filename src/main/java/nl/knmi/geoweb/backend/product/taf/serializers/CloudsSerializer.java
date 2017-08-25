package nl.knmi.geoweb.backend.product.taf.serializers;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import nl.knmi.geoweb.backend.product.taf.Taf.Forecast;

public class CloudsSerializer extends StdSerializer<List<Forecast.TAFCloudType>> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public CloudsSerializer () {
		this(null);
	}
	
	public CloudsSerializer(Class<List<Forecast.TAFCloudType>> t) {
		super(t);
	}
	
	@Override
	public void serialize(
			List<Forecast.TAFCloudType> value, JsonGenerator jgen, SerializerProvider provider)
	throws IOException, JsonProcessingException {
		if (value.size() == 1 && value.get(0).getIsNSC()) {
			jgen.writeString("NSC");
		} else {
			jgen.writeObject(value);
		}
	}
	@Override
	public boolean isEmpty(SerializerProvider provider, List<Forecast.TAFCloudType> value) {
		return value == null || value.size() == 0;
	}
			
}