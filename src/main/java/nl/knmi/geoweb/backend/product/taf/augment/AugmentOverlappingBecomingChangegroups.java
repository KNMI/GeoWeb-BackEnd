package nl.knmi.geoweb.backend.product.taf.augment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import nl.knmi.geoweb.backend.product.taf.TafValidator;

public class AugmentOverlappingBecomingChangegroups {
	public static void augment(JsonNode input) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

		List<Date> becmgEndTimes = new ArrayList<Date>();
		List<Date> becmgStartTimes = new ArrayList<Date>();
		
		JsonNode changeGroups = input.get("changegroups");
		if (changeGroups == null || changeGroups.isNull() || changeGroups.isMissingNode())
			return;
		
		
//		boolean prevHasWind = false;
//		boolean prevHasVisibility = false;
//		boolean prevHasWeather = false;
//		boolean prevHasCloud = false;
		for (Iterator<JsonNode> change = changeGroups.elements(); change.hasNext();) {
			JsonNode nextNode = change.next(); 
			if (nextNode == null || nextNode == NullNode.getInstance()) continue;
			ObjectNode changegroup = (ObjectNode) nextNode;

			JsonNode changeType = changegroup.findValue("changeType");
			JsonNode changeStart = changegroup.findValue("changeStart");
			if (changeType == null || changeType.isMissingNode() || changeType.isNull())
				continue;
			if (changeStart == null || changeStart.isMissingNode() || changeStart.isNull())
				continue;

			/* Check if in range */
			JsonNode changeEndNode = changegroup.findValue("changeEnd");
			JsonNode validityStart = input.findValue("validityStart");
			JsonNode validityEnd = input.findValue("validityEnd");

			if (TafValidator.checkIfNodeHasValue(validityEnd) &&
					TafValidator.checkIfNodeHasValue(validityStart) && 
					TafValidator.checkIfNodeHasValue(changeStart) && 
					TafValidator.checkIfNodeHasValue(changeEndNode)) {

				Date validityStartDate = formatter.parse(input.findValue("validityStart").asText());
				Date validityEndDate = formatter.parse(input.findValue("validityEnd").asText());
				Date changeStartDate = formatter.parse(changegroup.findValue("changeStart").asText());
				Date changeEndDate = formatter.parse(changegroup.findValue("changeEnd").asText());
				if (changeStartDate.compareTo(validityStartDate) < 0 || changeStartDate.compareTo(validityEndDate) > 0 ||
						changeEndDate.compareTo(validityStartDate) < 0 || changeEndDate.compareTo(validityEndDate) > 0){
					changegroup.put("changegroupDateOutsideRange", false);
				}		else {
					changegroup.put("changegroupDateOutsideRange", true);
				}
			}

			String type = changegroup.findValue("changeType").asText();
			if (!"BECMG".equals(type))
				continue;

//			Date becmgStart = formatter.parse(changegroup.findValue("changeStart").asText());
//			boolean overlap = false;
//			for (Date otherEnd : becmgEndTimes) {
//				if (becmgStart.before(otherEnd)) {
//					overlap = true;
//				}
//			}
			Date becmgStart = formatter.parse(changegroup.findValue("changeStart").asText());
			boolean overlap = false;
			for (Date otherStart: becmgStartTimes) {
				if (becmgStart.before(otherStart) || becmgStart.compareTo(otherStart) == 0) {
					overlap = true;
				}
			}

			if (changeEndNode != null && !changeEndNode.isNull() && !changeEndNode.isMissingNode()) {
				becmgEndTimes.add(formatter.parse(changeEndNode.asText()));
			}
			if (changeStart != null && !changeStart.isNull() && !changeStart.isMissingNode()) {
				becmgStartTimes.add(formatter.parse(changeStart.asText()));
			}
//			/* TODO: Test if phenomena differ, in this case changegroups are allowed to overlap */
			changegroup.put("changegroupBecomingOverlaps", overlap);
		}
	}
}
