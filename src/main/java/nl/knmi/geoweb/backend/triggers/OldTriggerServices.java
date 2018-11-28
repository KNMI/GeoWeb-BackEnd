package nl.knmi.geoweb.backend.triggers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/triggers") 
public class OldTriggerServices {
	OldTriggerStore oldTriggerStore;
	
	OldTriggerServices(final OldTriggerStore oldTriggerStore) throws IOException {
		this.oldTriggerStore = oldTriggerStore;
	}
	
	@RequestMapping("/gettriggers")
	public List<OldTrigger> getTriggers(@RequestParam("startdate")@DateTimeFormat(pattern="yyyy-MM-dd'T'HH:mm:ss'Z'") Date startDate,
										@RequestParam("duration")Integer duration) {
		List<OldTrigger> oldTriggers = oldTriggerStore.getLastTriggers(startDate, duration);
		for (OldTrigger oldTrigger : oldTriggers) {
			List<String>presets=new ArrayList<String>();
			for (int i=1; i<=3; i++) {
				presets.add(oldTrigger.getPhenomenon().getSource()+"_preset_"+i);
			}
			oldTrigger.setPresets(presets);
		}
		return oldTriggers;
	}

    @RequestMapping(path="/addtrigger", method=RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> addTrigger(@RequestBody OldTrigger.TriggerTransport transport) {
    	OldTrigger trig=new OldTrigger(transport);
    	try {
			oldTriggerStore.storeTrigger(trig);
	    	return ResponseEntity.status(HttpStatus.OK).body("OK");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("FAIL");
    }
}