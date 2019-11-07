package nl.knmi.geoweb.backend.product.taf.augment;

import java.io.IOException;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AugmentNonRepeatingChanges {
	public static void augment(JsonNode input) throws JsonProcessingException, IOException {
		ObjectNode currentForecast = (ObjectNode) input.get("forecast");
		if (currentForecast == null || currentForecast.isNull() || currentForecast.isMissingNode())
			return;
		JsonNode currentWeather = currentForecast.get("weather");

		// If the base forecast contains no weather group, this means NSW.
		// This is because space on punch cards is expensive. Because that matters here.
		if (currentWeather == null || currentWeather.isNull() || currentWeather.isMissingNode()) {
			currentForecast.setAll((ObjectNode) (new ObjectMapper().readTree("{\"weather\":\"NSW\"}")));
		}
		JsonNode changeGroups = input.get("changegroups");
		if (changeGroups == null || changeGroups.isNull() || changeGroups.isMissingNode())
			return;

		for (Iterator<JsonNode> change = changeGroups.elements(); change.hasNext();) {
			JsonNode nextNode = change.next(); 
			if (nextNode == null || nextNode == NullNode.getInstance()) continue;
			ObjectNode changegroup = (ObjectNode) nextNode;

			ObjectNode changeForecast = (ObjectNode) changegroup.get("forecast");
			if (changeForecast == null || changeForecast.isNull() || changeForecast.isMissingNode())
				continue;

			String changeGroupChangeAsText = "";
			if (changegroup.get("changeType") != null ) {
				changeGroupChangeAsText = changegroup.get("changeType").asText();
			}

			boolean nonRepeatingChange = false;

			JsonNode forecastWind = currentForecast.get("wind");
			JsonNode changeWind = changeForecast.get("wind");

			JsonNode forecastVisibility = currentForecast.get("visibility");
			JsonNode changeVisibility = changeForecast.get("visibility");

			JsonNode forecastWeather = currentForecast.get("weather");
			JsonNode changeWeather = changeForecast.get("weather");

			JsonNode forecastClouds = currentForecast.get("clouds");
			JsonNode changeClouds = changeForecast.get("clouds");

			/* From groups are treated as a new TAF */
			if (!changeGroupChangeAsText.equals("FM")) {

				if (forecastWind != null && !forecastWind.isNull() && !forecastWind.isMissingNode())
					nonRepeatingChange |= forecastWind.equals(changeWind);
				if (forecastVisibility != null && !forecastVisibility.isNull() && !forecastVisibility.isMissingNode())
					nonRepeatingChange |= forecastVisibility.equals(changeVisibility);
				if (forecastWeather != null && !forecastWeather.isNull() && !forecastWeather.isMissingNode())
					nonRepeatingChange |= forecastWeather.equals(changeWeather);
				if (forecastClouds != null && !forecastClouds.isNull() && !forecastClouds.isMissingNode())
					nonRepeatingChange |= forecastClouds.equals(changeClouds);

				changegroup.put("repeatingChange", nonRepeatingChange);
			}else {
				changegroup.put("repeatingChange", false);
			}
			if (!changeGroupChangeAsText.startsWith("PROB") && !changeGroupChangeAsText.equalsIgnoreCase("TEMPO")) {
				currentForecast = changeForecast;
			}

		}
	}
}
