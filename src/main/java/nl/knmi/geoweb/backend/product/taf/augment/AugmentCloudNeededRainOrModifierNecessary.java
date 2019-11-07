package nl.knmi.geoweb.backend.product.taf.augment;

import java.util.Iterator;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AugmentCloudNeededRainOrModifierNecessary {
	// 	TODO: Check if FM handled correctly
	public static void augment(JsonNode input) {
		ObjectNode forecast = (ObjectNode) input.get("forecast");
		if (forecast == null || forecast.isNull() || forecast.isMissingNode())
			return;

		JsonNode forecastWeather = input.get("forecast").get("weather");
		JsonNode forecastClouds = input.get("forecast").get("clouds");

		processWeatherAndCloudGroup(forecast, forecastWeather, forecastClouds);
		JsonNode changeGroups = input.get("changegroups");
		if (changeGroups == null || changeGroups.isNull() || changeGroups.isMissingNode())
			return;

		for (Iterator<JsonNode> change = changeGroups.elements(); change.hasNext();) {
			JsonNode nextNode = change.next();
			if (nextNode == null || nextNode == NullNode.getInstance()) continue;
			ObjectNode changegroup = (ObjectNode) nextNode;

			if (!changegroup.has("forecast")) {
				continue;
			}
			JsonNode changeForecastNode = changegroup.get("forecast");
			if (changeForecastNode.isNull() || changeForecastNode == null || changeForecastNode.isMissingNode()) {
				continue;
			}
			String changeGroupChangeAsText = "";
			if (changegroup.get("changeType") != null ) {
				changeGroupChangeAsText = changegroup.get("changeType").asText();
			}

			/* From groups are treated as a new TAF */
			if (!changeGroupChangeAsText.equals("FM")) {
				ObjectNode changeForecast = (ObjectNode) changeForecastNode;
				JsonNode changeWeather = changeForecast.get("weather");
				if (changeWeather == null) {
					changeWeather = forecastWeather;
				}
				JsonNode changeClouds = changeForecast.get("clouds");
				if (changeClouds == null) {
					changeClouds = forecastClouds;
				}

				processWeatherAndCloudGroup(changeForecast, changeWeather, changeClouds);

				if (!changeGroupChangeAsText.startsWith("PROB") && !changeGroupChangeAsText.equalsIgnoreCase("TEMPO")) {
					forecastWeather = changeWeather;
					forecastClouds = changeClouds;
				}
			}
			else {
				ObjectNode changeForecast = (ObjectNode) changeForecastNode;
				JsonNode changeWeather = changeForecast.get("weather");
				if (changeWeather!=null) {
                    if (changeWeather.asText().equals("NSW")) {
                        forecast.put("forecastGivenWithFM", false);
                    } else {
                        forecast.put("forecastGivenWithFM", true);
                    }
                }
			}
		}
	}


	private static void processWeatherAndCloudGroup(ObjectNode forecast, JsonNode forecastWeather,
			JsonNode forecastClouds) {
		if (forecastWeather != null && !forecastWeather.asText().equals("NSW")) {
			boolean requiresClouds = false;
			boolean requiresCB = false;
			boolean requiresCBorTCU = false;
			boolean rainOrThunderstormPresent = false;
			ArrayNode weatherArray = (ArrayNode) forecastWeather;
			for (Iterator<JsonNode> weather = weatherArray.elements(); weather.hasNext();) {
				JsonNode weatherDescriptor = weather.next();
				if (weatherDescriptor.has("descriptor")
						&& weatherDescriptor.get("descriptor").asText().equals("showers")) {
					requiresClouds = true;
					requiresCBorTCU = true;
					rainOrThunderstormPresent = true;
				}
				if (weatherDescriptor.has("descriptor")
						&& weatherDescriptor.get("descriptor").asText().equals("thunderstorm")) {
					requiresCB = true;
					rainOrThunderstormPresent = true;
				}
			}

			if (requiresClouds) {
				if (forecastClouds == null || forecastClouds.asText().equals("NSC")) {
					forecast.put("cloudsNeededAndPresent", false);
				} else {
					ArrayNode cloudsArray = (ArrayNode) forecastClouds;
					forecast.put("cloudsNeededAndPresent", cloudsArray.size() > 0);
				}
			}

			if (requiresCB) {
				if (forecastClouds == null || forecastClouds.asText().equals("NSC")) {
					forecast.put("cloudsCBNeededAndPresent", false);
				} else {
					ArrayNode cloudsArray = (ArrayNode) forecastClouds;
					forecast.put("cloudsCBNeededAndPresent", StreamSupport.stream(cloudsArray.spliterator(), true)
							.anyMatch(cloud -> cloud.has("mod") && cloud.get("mod").asText().equals("CB")));
				}
			}
			if (requiresCBorTCU) {
				if (forecastClouds == null || forecastClouds.asText().equals("NSC")) {
					forecast.put("cloudsCBorTCUNeededAndPresent", false);
				} else {
					ArrayNode cloudsArray = (ArrayNode) forecastClouds;
					forecast.put("cloudsCBorTCUNeededAndPresent",
							StreamSupport.stream(cloudsArray.spliterator(), true)
							.anyMatch(cloud -> cloud.has("mod") && (cloud.get("mod").asText().equals("CB")
									|| cloud.get("mod").asText().equals("TCU"))));
				}
			}
		}
	}
}
