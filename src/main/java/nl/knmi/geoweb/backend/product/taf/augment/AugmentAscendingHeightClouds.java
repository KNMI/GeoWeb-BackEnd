package nl.knmi.geoweb.backend.product.taf.augment;

import java.text.ParseException;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class AugmentAscendingHeightClouds {
	public static void augment(JsonNode input) throws ParseException {
		List<JsonNode> forecasts = input.findParents("clouds");
		for (JsonNode forecast : forecasts) {
			if (forecast == null || forecast.isNull() || forecast.isMissingNode())
				continue;
			int prevHeight = -1;
			boolean cloudModCB = false;
			boolean cloudModTCU = false;
			
			JsonNode cloudsNode=forecast.findValue("clouds");
			if (cloudsNode.getClass().equals(String.class) || cloudsNode.getClass().equals(TextNode.class)) {
				continue;
			}
			ArrayNode node = (ArrayNode) cloudsNode;

			for (Iterator<JsonNode> it = node.iterator(); it.hasNext();) {
				JsonNode nextNode = it.next(); 
				if (nextNode == null || nextNode == NullNode.getInstance()) continue;
				ObjectNode cloudNode = (ObjectNode) nextNode;				

				JsonNode cloudHeight = cloudNode.findValue("height");
				JsonNode cloudMod = cloudNode.findValue("mod");
				if (cloudHeight == null || cloudHeight.asText().equals("null"))
					continue;
				int height = Integer.parseInt(cloudHeight.asText());	
				
				if (cloudMod != null && cloudModCB && cloudMod.asText().equals("CB") || cloudModTCU && cloudMod.asText().equals("TCU")) {
					cloudNode.put("onlyOneCloudsCBorTCUPresent", false);
				}
				else {
					cloudNode.put("onlyOneCloudsCBorTCUPresent", true);
				}
				
				if (height <= prevHeight) {
					if (cloudMod != null && cloudMod.asText().equals("CB") && height == prevHeight){
						cloudNode.put("cloudsHeightAscending", true);	
					}else {
						cloudNode.put("cloudsHeightAscending", false);
					}
				}
				else {
					cloudNode.put("cloudsHeightAscending", true);
				}
				
				if (cloudMod != null && !cloudModCB && cloudMod.asText().equals("CB")) {
					cloudModCB = true;
				}
				if (cloudMod != null && !cloudModTCU && cloudMod.asText().equals("TCU")) {
					cloudModTCU = true;
				}
				prevHeight = height;
				
			}
		}
	}
}
