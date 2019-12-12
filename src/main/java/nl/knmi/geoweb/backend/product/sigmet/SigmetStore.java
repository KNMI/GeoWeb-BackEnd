package nl.knmi.geoweb.backend.product.sigmet;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.knmi.adaguc.tools.Tools;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet.Phenomenon;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetStatus;

@Slf4j
@Component
public class SigmetStore {

	private String directory;

	@Autowired
	@Qualifier("sigmetObjectMapper")
	private ObjectMapper sigmetObjectMapper;

	public ObjectMapper getOM(){ return sigmetObjectMapper;}

	public SigmetStore(@Value(value = "${geoweb.products.storeLocation}") String productstorelocation) throws IOException {
		this.setLocation(productstorelocation);
	}

	public void setLocation(String productstorelocation) throws IOException {
		String dir = productstorelocation + "/sigmets";
		log.debug("SigmetStore at " + dir);
		File f = new File(dir);
		if(f.exists() == false){
			Tools.mksubdirs(f.getAbsolutePath());
			log.debug("Creating SigmetStore at ["+f.getAbsolutePath()+"]");		}
		if(f.isDirectory() == false){
			log.error("Sigmet directory location is not a directory");
			throw new NotDirectoryException("Sigmet directory location is not a directorty");
		}

		this.directory=dir;
	}

	public void storeSigmet(ObjectMapper om, Sigmet sigmet) {
		String fn=String.format("%s/sigmet_%s.json", this.directory, sigmet.getUuid());
		sigmet.serializeSigmet(om, fn);
	}

	public void storeSigmet(Sigmet sigmet) {
		String fn=String.format("%s/sigmet_%s.json", this.directory, sigmet.getUuid());
		sigmet.serializeSigmet(sigmetObjectMapper, fn);
	}

	public synchronized int getNextSequence(Sigmet sigmetToPublish) {
		// Day zero means all sigmets of today since midnight UTC
		Sigmet[] allSigmets = getPublishedSigmetsOnDay(sigmetToPublish);
		Sigmet[] sigmets = null;
		Phenomenon sigmetPhenomenon = sigmetToPublish.getPhenomenon();
		if (sigmetPhenomenon == Phenomenon.VA_CLD) {
			sigmets = (Sigmet[]) Arrays.stream(allSigmets).filter(
					x -> x.getPhenomenon() == Phenomenon.VA_CLD).toArray(Sigmet[]::new);

		} else if (sigmetPhenomenon == Phenomenon.TROPICAL_CYCLONE) {
			sigmets = (Sigmet[]) Arrays.stream(allSigmets).filter(
					x -> x.getPhenomenon() == Phenomenon.TROPICAL_CYCLONE).toArray(Sigmet[]::new);
		} else {
			sigmets = (Sigmet[]) Arrays.stream(allSigmets).filter(
					x -> (x.getPhenomenon() != Phenomenon.VA_CLD &&
					x.getPhenomenon() != Phenomenon.TROPICAL_CYCLONE)).toArray(Sigmet[]::new);
		}

		int seq = 1;
		if (sigmets.length > 0){
			Arrays.sort(sigmets, (rhs, lhs) -> rhs.getSequence() < lhs.getSequence() ? 1 :
                    (rhs.getSequence() == lhs.getSequence() ? 0 : -1));
			seq = sigmets[0].getSequence() + 1;
		}
		log.info("Created sequence number " + seq + " for sigmet phenomenon " + sigmetPhenomenon.toString());
		return seq;
	}

	private Sigmet[] getPublishedSigmetsOnDay(Sigmet sigmetToPublish) {
		Sigmet[] sigmets = getSigmets(false, SigmetAirmetStatus.published);
		OffsetDateTime offsetSinceMidnight = sigmetToPublish.getIssuedate().withHour(0).withMinute(1).withSecond(0).withNano(0);

		return Arrays.stream(sigmets).filter(sigmet -> (
				sigmet.getValiddate().isAfter(offsetSinceMidnight) ||
				sigmet.getValiddate().isEqual(offsetSinceMidnight)
				)).toArray(Sigmet[]::new);
	}

	public Sigmet[] __getPublishedSigmetsSinceDay (int daysOffset) {
		Sigmet[] sigmets = getSigmets(false, SigmetAirmetStatus.published);
		OffsetDateTime offset = OffsetDateTime.now(ZoneId.of("Z")).minusDays(daysOffset);
		OffsetDateTime offsetSinceMidnight = offset.withHour(0).withMinute(1).withSecond(0).withNano(0);

		return Arrays.stream(sigmets).filter(sigmet -> (
				sigmet.getValiddate().isAfter(offsetSinceMidnight) ||
				sigmet.getValiddate().isEqual(offsetSinceMidnight)
				)).toArray(Sigmet[]::new);
	}

	public Sigmet[] getSigmets(boolean selectActive, SigmetAirmetStatus selectStatus) {
		Comparator<Sigmet> comp = new Comparator<Sigmet>() {
			public int compare(Sigmet lhs, Sigmet rhs) {
				if (rhs.getIssuedate() != null && lhs.getIssuedate() != null)
					return rhs.getIssuedate().compareTo(lhs.getIssuedate());
				else
					return rhs.getValiddate().compareTo(lhs.getValiddate());
			}
		};
		//Scan directory for sigmets
		File dir=new File(directory);
		File[] files=dir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return !name.contains("..") && name.contains("sigmet_")&&name.endsWith(".json");
			}
		});

		OffsetDateTime now= OffsetDateTime.now(ZoneId.of("Z"));

		if (files!=null) {
			List<Sigmet> sigmets=new ArrayList<Sigmet>();
			for (File f: files) {
				Sigmet sm;
				try {
					sm = Sigmet.getSigmetFromFile(sigmetObjectMapper, f);
					if (selectActive) {
						if ((sm.getStatus()==SigmetAirmetStatus.published)&&
								(sm.getValiddate_end().isAfter(now))) {
							sigmets.add(sm);
						}
					}else if (selectStatus != null) {
						if (selectStatus==SigmetAirmetStatus.canceled) {
							if (((sm.getStatus()==SigmetAirmetStatus.published)&&sm.getValiddate_end().isBefore(now))||(sm.getStatus()==SigmetAirmetStatus.canceled)) {
								sigmets.add(sm);
							}
						} else {
							if (sm.getStatus()==selectStatus) {
								sigmets.add(sm);
							}
						}
					} else {
						sigmets.add(sm);
					}
				} catch (JsonParseException e) {
					log.error(e.getMessage());
				} catch (JsonMappingException e) {
					log.error(e.getMessage());
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}
			sigmets.sort(comp);
			return sigmets.toArray(new Sigmet[0]);
		}
		return new Sigmet[0];
	}

	public Sigmet getByUuid(String uuid) {
		for (Sigmet sigmet: getSigmets(false, null)) {
			if (uuid.equals(sigmet.getUuid())){
				return sigmet;
			}
		}
		return null;
	}

	public boolean deleteSigmetByUuid(String uuid) {
		String fn=String.format("%s/sigmet_%s.json", this.directory, uuid);
		return Tools.rm(fn);
	}

	public boolean isPublished(String uuid) {
		Sigmet sigmet=getByUuid(uuid);
		if (sigmet!=null) {
			return (sigmet.getStatus()==SigmetAirmetStatus.published);
		}
		return false;
	}

	public boolean isCanceled(String uuid) {
		Sigmet sigmet=getByUuid(uuid);
		if (sigmet!=null) {
			return (sigmet.getStatus()==SigmetAirmetStatus.canceled);
		}
		return false;
	}
}
