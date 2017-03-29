package nl.knmi.geoweb.backend.product.sigmet;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import nl.knmi.geoweb.backend.product.sigmet.Sigmet.SigmetStatus;

public class SigmetStore {
	private String directory;
	public SigmetStore(String dir) {
		this.directory=dir;
	}

	public void storeSigmet(Sigmet sigmet) {
		String fn=String.format("%s/sigmet_%s.json", this.directory, sigmet.getUuid());
		sigmet.serializeSigmet(fn);	
	}

	public synchronized int getSequence() {
		Sigmet[]sigmets=getSigmets(true, null);
		int seq=-1;
		Date now=new Date();
		if (sigmets.length>0){
			seq=sigmets[0].getSequence();
			if (seq==-1) {
				seq=1;
			} else if (sigmets[0].getIssuedate().getDate()!=now.getDate()) {
				seq=1;
			}
		}else {
			seq=1;
		}
		return 1;
	}

	public Sigmet[] getSigmets(boolean selectActive, SigmetStatus selectStatus) {
		Comparator<Sigmet> comp = new Comparator<Sigmet>() {
			public int compare(Sigmet lhs, Sigmet rhs) {
				return rhs.getIssuedate().compareTo(lhs.getIssuedate());
			}
		};
		//				(Sigmet lhs, Sigmet rhs) -> { return rhs.compareTo(lhs);};

		//Scan directory for sigmets
		File dir=new File(directory);
		File[] files=dir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return !name.contains("..") && name.contains("sigmet_")&&name.endsWith(".json");
			}
		});

		Date now=new Date();

		if (files!=null) {
			List<Sigmet> sigmets=new ArrayList<Sigmet>();
			for (File f: files) {
				Sigmet sm;
				try {
					sm = Sigmet.getSigmetFromFile(f);
					if (selectActive) {
						if ((sm.getStatus()==SigmetStatus.PUBLISHED)&&(sm.getValiddate().getTime()+Sigmet.WSVALIDTIME)<now.getTime()) {
							sigmets.add(sm);
						}
					}else if (selectStatus != null) {
						if (sm.getStatus()==selectStatus) {
							sigmets.add(sm);
						}
					} else {
						sigmets.add(sm);
					}
				} catch (JsonParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JsonMappingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			sigmets.sort(comp);
			return sigmets.toArray(new Sigmet[0]);
		}
		return null;
	}

	public Sigmet getByUuid(String uuid) {
		for (Sigmet sigmet: getSigmets(false, null)) {
			if (uuid.equals(sigmet.getUuid())){
				return sigmet;
			}
		}
		return null;
	}
}
