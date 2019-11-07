package nl.knmi.geoweb.backend.product.taf.augment;

import java.text.ParseException;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class AugmentAmountCoverageClouds {
	public static void augment(JsonNode input) throws ParseException {
		// FEW -> SCT -> BKN -> OVC
		List<JsonNode> forecasts = input.findParents("clouds");
		for (JsonNode forecast : forecasts) {
			if (forecast == null || forecast.isNull() || forecast.isMissingNode())
				continue;
			String prevAmount = null;
			JsonNode cloudsNode=forecast.findValue("clouds");
			if (cloudsNode.getClass().equals(String.class) || cloudsNode.getClass().equals(TextNode.class)) {
				continue;
			}
			ArrayNode node = (ArrayNode) cloudsNode;

			for (Iterator<JsonNode> it = node.iterator(); it.hasNext();) {
				JsonNode nextNode = it.next(); 
				if (nextNode == null || nextNode == NullNode.getInstance()) continue;
				ObjectNode cloudNode = (ObjectNode) nextNode;				

				JsonNode amountNode = cloudNode.findValue("amount");
				if (amountNode == null || amountNode.asText().equals("null"))
					continue;
				boolean isCBorTCU = false;
				JsonNode modNode = cloudNode.findValue("mod");
				if (modNode != null) {
					if (modNode.asText().equals("CB") || modNode.asText().equals("TCU")) {
						isCBorTCU = true;
					}
				}

				String amount = amountNode.asText();
				if (prevAmount != null && isCBorTCU == false ){
					/* SCT can only be preceded by FEW */
					if ("SCT".equals(amount)){
						if ("FEW".equals(prevAmount) == false){
							cloudNode.put("cloudsAmountAscending", false);
						}
					}
					/* BKN can only be preceded by FEW or SCT */
					if ("BKN".equals(amount)){
						if ("FEW".equals(prevAmount) == false && "SCT".equals(prevAmount) == false){
							cloudNode.put("cloudsAmountAscending", false);
						}
					}
					/* OVC can only be preceded by FEW or SCT or BKN */
					if ("OVC".equals(amount)){
						if ("FEW".equals(prevAmount) == false && "SCT".equals(prevAmount) == false && "BKN".equals(prevAmount) == false){
							cloudNode.put("cloudsAmountAscending", false);
						}
					}
				}
				prevAmount = amount;
			}
		}
	}
}
