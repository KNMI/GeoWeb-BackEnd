package nl.knmi.geoweb.backend.product.taf.augment;

import java.text.ParseException;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AugmentWindGust {
	public static void augment(JsonNode input) throws ParseException {
		List<JsonNode> windGroups = input.findValues("wind");
		if (windGroups == null)
			return;
		for (JsonNode node : windGroups) {
			ObjectNode windNode = (ObjectNode) node;
			JsonNode gustField = node.findValue("gusts");
			if (gustField == null)
				continue;
			try {
				int gust = Integer.parseInt(gustField.asText());
				int windspeed = Integer.parseInt(node.findValue("speed").asText());
				windNode.put("gustFastEnough", gust >= (windspeed + 10));
			} catch (NumberFormatException e) {
				continue;
			}
		}
	}
}
