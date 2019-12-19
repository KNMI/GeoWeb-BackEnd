package nl.knmi.geoweb.backend.product.taf.augment;

import java.util.Iterator;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AugmentMaxVisibility {
	public static void augment(JsonNode input) {
		ObjectNode forecast = (ObjectNode) input.get("forecast");
		if (forecast == null || forecast.isNull() || forecast.isMissingNode()) {
			log.warn("augmentMaxVisibility: No forecast");
			return;
		}


		JsonNode forecastWeather = input.get("forecast").get("weather");
		JsonNode forecastVisibility = input.get("forecast").get("visibility");
		if (forecastWeather != null && !forecastWeather.isNull() && !forecastWeather.isMissingNode()
				&& forecastVisibility != null && !forecastVisibility.isNull() && !forecastVisibility.isMissingNode()) {
			int visibility = forecastVisibility.get("value").asInt();
			for (Iterator<JsonNode> weatherNode = forecastWeather.elements(); weatherNode.hasNext();) {
				JsonNode nextNode = weatherNode.next();
				if (nextNode == null || nextNode == NullNode.getInstance()) continue;
				JsonNode weatherGroup = (ObjectNode) nextNode;
				checkVisibilityWithinLimit (weatherGroup, forecast, visibility);
			}
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
				return;

			JsonNode changeWeather = changeForecast.get("weather");
			JsonNode changeVisibility = changeForecast.get("visibility");

			if ((changeWeather == null || changeWeather.isNull() || changeWeather.isMissingNode())
					&& (changeVisibility == null || changeVisibility.isNull() || changeVisibility.isMissingNode()))
				return;

			String changeGroupChangeAsText = "";
			if (changegroup.get("changeType") != null ) {
				changeGroupChangeAsText = changegroup.get("changeType").asText();
			}
			if (changeGroupChangeAsText.equals("BECMG") || changeGroupChangeAsText.equals("TEMPO")) {
				if (changeWeather == null || changeWeather.isNull() || changeWeather.isMissingNode()) {
					changeWeather = forecastWeather;
				}
				if (changeVisibility == null || changeVisibility.isNull() || changeVisibility.isMissingNode()) {
					changeVisibility = forecastVisibility;
				}
			}
			if (changeWeather == null || changeVisibility == null)
				continue;
			int visibility = changeVisibility.get("value").asInt();
			for (Iterator<JsonNode> weatherNode = changeWeather.elements(); weatherNode.hasNext();) {
				JsonNode weatherGroup = (ObjectNode) weatherNode.next();
				checkVisibilityWithinLimit (weatherGroup, changeForecast, visibility);
			}

			if (changegroup.get("changeType") != null && !changegroup.get("changeType").asText().startsWith("PROB")
					&& !changegroup.get("changeType").asText().equalsIgnoreCase("TEMPO")) {
				forecastWeather = changeWeather;
				forecastVisibility = changeVisibility;
			}
		}
	}	

	/**
	 * Checks if visibility is in range for either a changegroup (weatherGroup) or forecast (weatherGroup)
	 * @param weatherGroup
	 * @param forecast
	 * @param visibility
	 */
	private static void checkVisibilityWithinLimit (JsonNode weatherGroup, ObjectNode forecast, int visibility ){
		if (!weatherGroup.has("phenomena"))
			return;
		ArrayNode phenomena = (ArrayNode) weatherGroup.get("phenomena");
		boolean isFoggy = StreamSupport.stream(phenomena.spliterator(), false)
				.anyMatch(phenomenon -> phenomenon.asText().equals("fog"));
		if (isFoggy) {
			if (!weatherGroup.has("descriptor")) {
				/* Standard fog without descriptor */
				forecast.put("visibilityAndFogWithoutDescriptorWithinLimit", visibility < 1000);
			} else {
				String descriptor = weatherGroup.get("descriptor").asText();
				if (descriptor.equals("freezing")) {
					forecast.put("visibilityWithinLimit", visibility < 1000);
/*
				} else if (descriptor.equals("shallow")) {
					*/
/* Shallow fog MIFG *//*
					// forecast.put("visibilityWithinLimit", visibility > 1000); 
*/
				} else {
					forecast.put("visibilityWithinLimit", true);
				}
			}
		}

		if (StreamSupport.stream(phenomena.spliterator(), false)
		.anyMatch(phenomenon -> phenomenon.asText().equals("smoke")))  {
			forecast.put("visibilityAndSmokeWithinLimit", visibility <= 5000);
		}

		if (StreamSupport.stream(phenomena.spliterator(), false)
		.anyMatch(phenomenon -> phenomenon.asText().equals("widespread dust")))  {
			forecast.put("visibilityAndDustWithinLimit", visibility <= 5000);
		}
		
		/* VA, SA, DRSA, BLSA mogen ook bij zichten > 5000 meter (volgens MBG, 05-09-2018
		if (StreamSupport.stream(phenomena.spliterator(), false)
				.anyMatch(phenomenon -> phenomenon.asText().equals("smoke")
						|| phenomenon.asText().equals("widespread dust") 
						|| phenomenon.asText().equals("sand")
						|| phenomenon.asText().equals("volcanic ash"))) {
			forecast.put("visibilityWithinLimit", visibility <= 5000);
		}*/

		if (StreamSupport.stream(phenomena.spliterator(), false)
				.anyMatch(phenomenon -> phenomenon.asText().equals("mist"))) {
			forecast.put("visibilityWithinLimit", visibility >= 1000 && visibility <= 5000);
		}

		if (StreamSupport.stream(phenomena.spliterator(), false)
				.anyMatch(phenomenon -> phenomenon.asText().equals("haze"))) {
			forecast.put("visibilityWithinLimit", visibility <= 5000);
		}	
	}

}
