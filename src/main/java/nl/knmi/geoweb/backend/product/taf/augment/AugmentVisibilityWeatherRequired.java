package nl.knmi.geoweb.backend.product.taf.augment;

import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AugmentVisibilityWeatherRequired {
	public static void augment(JsonNode input) {
		ObjectNode forecastNode = (ObjectNode) input.get("forecast");
		if (forecastNode == null || forecastNode.isNull() || forecastNode.isMissingNode())
			return;

		JsonNode forecastWeather = forecastNode.get("weather");
		JsonNode visibilityNode = forecastNode.findValue("visibility");

		if (visibilityNode != null && visibilityNode.get("value") != null
				&& visibilityNode.get("value").asInt() < 5000)
			forecastNode.put("visibilityWeatherRequiredAndPresent",
					forecastWeather != null && forecastWeather.isArray());

		JsonNode changeGroups = input.get("changegroups");
		if (changeGroups == null || changeGroups.isNull() || changeGroups.isMissingNode())
			return;
		for (Iterator<JsonNode> change = changeGroups.elements(); change.hasNext();) {
			JsonNode nextNode = change.next(); 
			if (nextNode == null || nextNode == NullNode.getInstance()) continue;
			ObjectNode changegroup = (ObjectNode) nextNode;

			ObjectNode changegroupForecast = (ObjectNode) changegroup.get("forecast");
			if (changegroupForecast == null || changegroupForecast.isNull() || changegroupForecast.isMissingNode())
				continue;

			JsonNode changeVisibilityNode = changegroupForecast.findValue("visibility");
			if (changeVisibilityNode == null || !changeVisibilityNode.has("value")) {
				changeVisibilityNode = visibilityNode;
			}
			;
			int visibility;
			if (changeVisibilityNode == null || !changeVisibilityNode.has("value")) {
				// cavok
				visibility = 9999;
			} else {
				visibility = changeVisibilityNode.get("value").asInt();
			}
			JsonNode weather = changegroupForecast.findValue("weather");
			if (weather == null) {
				weather = forecastWeather;
			}
			// TODO: Check visibility <=5000 or 5000
			if (visibility < 5000) {
				changegroupForecast.put("visibilityWeatherRequiredAndPresent", weather != null && weather.isArray());
			}
			JsonNode changeType = changegroup.get("changeType");
			if (changeType != null && !changeType.asText().startsWith("PROB")) {
				if (weather != null) {
					forecastWeather = weather;
				}
				if (changeVisibilityNode != null) {
					visibilityNode = changeVisibilityNode;
				}
			}
		}
	}

	
}
