package nl.knmi.geoweb.backend.product.airmet;

import java.io.IOException;
import java.text.ParseException;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;

import nl.knmi.geoweb.backend.product.airmet.Airmet;
import nl.knmi.geoweb.backend.product.airmet.AirmetValidationResult;

@Component
public class AirmetValidator {
    @Autowired
    @Qualifier("airmetObjectMapper")
    private ObjectMapper objectMapper;

    public AirmetValidator(ObjectMapper om) throws IOException {
        this.objectMapper=om;
    }

    public AirmetValidationResult validate(Airmet airmet)
                    throws ProcessingException, JSONException, IOException, ParseException {
        return validate(airmet.toJSON(objectMapper));
    }

    public AirmetValidationResult validate(String sigmetStr)throws ProcessingException, JSONException, IOException, ParseException {
        /* Check if we can make a TAC */
		try{
			objectMapper.readValue(sigmetStr, Airmet.class).toTAC();
		}catch(Exception e){
			ObjectMapper om = new ObjectMapper();
			return new AirmetValidationResult(false,
					(ObjectNode) om.readTree("{\"/airmet/message\": [\"Unable to generate TAC report\"]}"));
		}

        return new AirmetValidationResult(true); //TODO: now always OK
    }
}
