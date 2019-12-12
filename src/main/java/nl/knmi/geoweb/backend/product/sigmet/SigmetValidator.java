package nl.knmi.geoweb.backend.product.sigmet;

import java.io.IOException;
import java.text.ParseException;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class SigmetValidator {
    @Autowired
    @Qualifier("sigmetObjectMapper")
    private ObjectMapper objectMapper;

    public SigmetValidator(ObjectMapper om) throws IOException {
        this.objectMapper=om;
    }

    public SigmetValidationResult validate(Sigmet sigmet)
                    throws ProcessingException, JSONException, IOException, ParseException {
        return validate(sigmet.toJSON(objectMapper));
    }

    public SigmetValidationResult validate(String sigmetStr)throws ProcessingException, JSONException, IOException, ParseException {
        /* Check if we can make a TAC */
		try{
			objectMapper.readValue(sigmetStr, Sigmet.class).toTAC();
		}catch(Exception e){
			ObjectMapper om = new ObjectMapper();
			return new SigmetValidationResult(false,
					(ObjectNode) om.readTree("{\"/sigmet/message\": [\"Unable to generate TAC report\"]}"));
		}

        return new SigmetValidationResult(true); //TODO: now always OK
    }
}
