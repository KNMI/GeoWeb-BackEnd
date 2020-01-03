package nl.knmi.geoweb.backend.product.taf.augment;

import java.text.ParseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class AugmentQualifiers {
	public static void augment(JsonNode input) throws ParseException {

        try {
            ObjectNode forecast = (ObjectNode) input.get("forecast");
            if (forecast.isNull() || !forecast.has("weather") || forecast.get("weather").isTextual()) {
                return;
            }
            if (validateWeather(forecast)) {
                return;
            }

            ArrayNode changegroups = (ArrayNode) input.get("changegroups");
            for (JsonNode changegroup : changegroups) {
                ObjectNode changegroupforecast = (ObjectNode) changegroup.get("forecast");
                if (changegroupforecast.isNull() || !changegroupforecast.has("weather") || changegroupforecast.get("weather").isTextual()) {
                    return;
                }
                if (validateWeather(changegroupforecast)) {
                    return;
                }    
            }

        } catch (NullPointerException e) {
            // Nothing to do, this node does not qualify for the required checks
            return;
        }
    }
    
    public static boolean validateWeather(ObjectNode forecast) {        

        ArrayNode weather = (ArrayNode) forecast.get("weather");
        for (JsonNode  subWeather : weather) {
            ArrayNode phenomena = (ArrayNode) subWeather.get("phenomena");
            if (!subWeather.has("qualifier")) {
                continue;
            }
            JsonNode qualifier = subWeather.get("qualifier");
            for (JsonNode  phenomenon : phenomena) {
                if (phenomenon.isValueNode() && qualifier.asText().equals("light") && phenomenon.asText().equals("funnel clouds")) {
                    forecast.put("funnelCloudsModerateOrHeavy", false);
                    return true;
                }
            }
        }
        return false;
    }
}