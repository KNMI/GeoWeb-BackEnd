package nl.knmi.geoweb.backend.product.taf.augment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AugmentChangegroupsIncreasingInTime {
	public static void augment(JsonNode input) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date prevChangeStart;
		Date tafStartTime;
		try {
			prevChangeStart = formatter.parse(input.findValue("validityStart").asText());
			tafStartTime = (Date) prevChangeStart.clone();
		} catch (ParseException e) {
			return;
		}
		JsonNode changeGroups = input.get("changegroups");
		if (changeGroups == null || changeGroups.isNull() || changeGroups.isMissingNode())
			return;
		for (Iterator<JsonNode> change = changeGroups.elements(); change.hasNext();) {
			JsonNode nextNode = change.next(); 
			if (nextNode == null || nextNode == NullNode.getInstance()) continue;
			ObjectNode changegroup = (ObjectNode) nextNode;
			JsonNode changeStartNode = changegroup.findValue("changeStart");

			if (changeStartNode == null)
				continue;
			String changeStart = changeStartNode.asText();
			JsonNode changeTypeNode = changegroup.findValue("changeType");
			if (changeTypeNode == null)
				continue;
			String changeType = changeTypeNode.asText();
			try {
				Date parsedDate = formatter.parse(changeStart);
				boolean comesAfter = parsedDate.compareTo(prevChangeStart) >= 0
						|| (parsedDate.equals(prevChangeStart) && changeType.startsWith("PROB"))
						|| (parsedDate.equals(prevChangeStart) && changeType.startsWith("BECMG")
								&& parsedDate.equals(tafStartTime))
						|| (parsedDate.equals(prevChangeStart) && changeType.startsWith("TEMPO")
								&& parsedDate.equals(tafStartTime));
				if ("FM".equals(changeType) && parsedDate.compareTo(prevChangeStart) <= 0) {
					comesAfter = false;
				}
				changegroup.put("changegroupsAscending", comesAfter);
				prevChangeStart = parsedDate;
			} catch (ParseException e) {
				changegroup.put("changegroupsAscending", false);
			}
		}
	}
}
