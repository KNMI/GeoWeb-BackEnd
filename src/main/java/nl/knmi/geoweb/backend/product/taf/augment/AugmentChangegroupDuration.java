package nl.knmi.geoweb.backend.product.taf.augment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AugmentChangegroupDuration {
	public static void augment(JsonNode input) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
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
			try {
				Date changeStart = formatter.parse(changeStartNode.asText());
				Date changeEnd = null;
				JsonNode end = changegroup.findValue("changeEnd");
				if (end != null) {
					changeEnd = formatter.parse(end.asText());
				} else {
					changeEnd = formatter.parse(input.findValue("validityEnd").asText());
				}
				long diffInMillies = Math.abs(changeEnd.getTime() - changeStart.getTime());
				long diffInHours = TimeUnit.HOURS.convert(diffInMillies, TimeUnit.MILLISECONDS);
				changegroup.put("changeDurationInHours", diffInHours);
			} catch (ParseException e) {
				continue;
			}
		}
	}
}
