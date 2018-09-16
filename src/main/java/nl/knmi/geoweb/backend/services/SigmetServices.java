package nl.knmi.geoweb.backend.services;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.knmi.geoweb.backend.aviation.FIRStore;
import nl.knmi.geoweb.backend.datastore.ProductExporter;
import nl.knmi.geoweb.backend.services.error.EmptySigmetException;
import nl.knmi.geoweb.backend.services.error.EntityDoesNotExistException;
import nl.knmi.geoweb.backend.services.error.EntityNotInConceptException;
import nl.knmi.geoweb.backend.services.error.GeoJsonConversionException;
import nl.knmi.geoweb.backend.services.error.GeoWebServiceException;
import nl.knmi.geoweb.backend.services.error.SigmetFeatureFirNotFoundException;
import nl.knmi.geoweb.backend.services.error.UnsupportedSigmetStatusException;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet.SigmetStatus;
import nl.knmi.geoweb.backend.product.sigmet.SigmetParameters;
import nl.knmi.geoweb.backend.product.sigmet.SigmetPhenomenaMapping;
import nl.knmi.geoweb.backend.product.sigmet.SigmetStore;
import nl.knmi.geoweb.backend.product.sigmet.converter.SigmetConverter;
import nl.knmi.geoweb.backend.services.model.SigmetFeature;
import nl.knmi.geoweb.backend.services.view.SigmetPaginationWrapper;
import nl.knmi.geoweb.backend.services.view.StoreSigmetResult;


@RestController
@RequestMapping("/sigmets")
public class SigmetServices {
	private static final Logger LOGGER = LoggerFactory.getLogger(SigmetServices.class);
	private SigmetStore sigmetStore;
	private ProductExporter<Sigmet> publishSigmetStore;

	SigmetServices (final SigmetStore sigmetStore, final ProductExporter<Sigmet> publishSigmetStore) {
		LOGGER.debug("INITING SigmetServices...");
		this.sigmetStore = sigmetStore;
		this.publishSigmetStore=publishSigmetStore;
	}

	@Autowired
	@Qualifier("sigmetObjectMapper")
	private ObjectMapper sigmetObjectMapper;

	@Autowired
	SigmetConverter sigmetConverter;

	@Autowired
	private FIRStore firStore;

	//Store sigmet, publish or cancel
	@RequestMapping(
			path = "/ORG",
			method = RequestMethod.POST, 
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public StoreSigmetResult storeJSONSigmetORG(@RequestBody Sigmet sm) throws GeoWebServiceException {
		LOGGER.debug("storesigmetORG: {}", sm);
		if (sm.getStatus() == null) {
			throw new EmptySigmetException();
		}

		switch (sm.getStatus()) {
			case concept:
				//Store
				if (sm.getUuid()==null) {
					sm.setUuid(UUID.randomUUID().toString());
				}
				LOGGER.debug("Storing {}", sm.getUuid());
				sigmetStore.storeSigmet(sm);
				break;

			case published:
				//publish
				sm.setIssuedate(OffsetDateTime.now(ZoneId.of("Z")));
				sm.setSequence(sigmetStore.getNextSequence());
				LOGGER.debug("Publishing {}", sm.getUuid());
				sigmetStore.storeSigmet(sm);
				sm.setFirFeature(firStore.lookup(sm.getLocation_indicator_icao(), true));
				publishSigmetStore.export(sm, sigmetConverter, sigmetObjectMapper);
				break;
			case canceled:
				//cancel
				Sigmet toBeCancelled = sigmetStore.getByUuid(sm.getUuid()); //Has to have status published and an uuid
				Sigmet cancelSigmet = new Sigmet(toBeCancelled);
				toBeCancelled.setStatus(SigmetStatus.canceled);
				cancelSigmet.setUuid(UUID.randomUUID().toString());
				cancelSigmet.setStatus(SigmetStatus.published);
				cancelSigmet.setCancels(toBeCancelled.getSequence());
				cancelSigmet.setCancelsStart(toBeCancelled.getValiddate());
				OffsetDateTime start = OffsetDateTime.now(ZoneId.of("Z"));
				cancelSigmet.setValiddate(start);
				cancelSigmet.setValiddate_end(toBeCancelled.getValiddate_end());
				cancelSigmet.setIssuedate(start);
				cancelSigmet.setSequence(sigmetStore.getNextSequence());
				LOGGER.debug("Canceling {}", sm.getUuid());
				sigmetStore.storeSigmet(cancelSigmet);
				sigmetStore.storeSigmet(toBeCancelled);
				cancelSigmet.setFirFeature(firStore.lookup(cancelSigmet.getLocation_indicator_icao(), true));
				publishSigmetStore.export(cancelSigmet, sigmetConverter, sigmetObjectMapper);

				break;
			default:
				throw new UnsupportedSigmetStatusException(sm.getStatus());
		}

		return new StoreSigmetResult(sm.getUuid(), sm.getStatus());
	}

	//Store sigmet, publish or cancel
	@RequestMapping(
			path = "",
			method = RequestMethod.POST,
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public StoreSigmetResult storeJSONSigmet(@RequestBody Sigmet sm) throws GeoWebServiceException {
		LOGGER.info("storesigmet: {}", sm);

        if (sm.getStatus() == null) {
			throw new EmptySigmetException();
		}

        switch (sm.getStatus()) {
			case concept:
				//Store
				if (sm.getUuid() == null) {
					sm.setUuid(UUID.randomUUID().toString());
				}
				LOGGER.info("Storing {}", sm.getUuid());
				sigmetStore.storeSigmet(sm);
				return new StoreSigmetResult(sm.getUuid(), sm.getStatus());
			case published:
				sm.setIssuedate(OffsetDateTime.now(ZoneId.of("Z")));
				sm.setSequence(sigmetStore.getNextSequence());
				LOGGER.info("Publishing {}", sm.getUuid());
				sigmetStore.storeSigmet(sm);
				sm.setFirFeature(firStore.lookup(sm.getLocation_indicator_icao(), true));
				publishSigmetStore.export(sm, sigmetConverter, sigmetObjectMapper);
				return new StoreSigmetResult(sm.getUuid(), sm.getStatus());
			case canceled:
				Sigmet toBeCancelled = sigmetStore.getByUuid(sm.getUuid()); //Has to have status published and an uuid
				Sigmet cancelSigmet = new Sigmet(toBeCancelled);
				toBeCancelled.setStatus(SigmetStatus.canceled);
				cancelSigmet.setUuid(UUID.randomUUID().toString());
				cancelSigmet.setStatus(SigmetStatus.published);
				cancelSigmet.setCancels(toBeCancelled.getSequence());
				cancelSigmet.setCancelsStart(toBeCancelled.getValiddate());
				OffsetDateTime start = OffsetDateTime.now(ZoneId.of("Z"));
				cancelSigmet.setValiddate(start);
				cancelSigmet.setValiddate_end(toBeCancelled.getValiddate_end());
				cancelSigmet.setIssuedate(start);
				cancelSigmet.setSequence(sigmetStore.getNextSequence());
				LOGGER.info("Canceling {}", sm.getUuid());
				sigmetStore.storeSigmet(cancelSigmet);
				sigmetStore.storeSigmet(toBeCancelled);
				cancelSigmet.setFirFeature(firStore.lookup(cancelSigmet.getLocation_indicator_icao(), true));
				publishSigmetStore.export(cancelSigmet, sigmetConverter, sigmetObjectMapper);
				return new StoreSigmetResult(sm.getUuid(), sm.getStatus());
			default:
				throw new UnsupportedSigmetStatusException(sm.getStatus());
		}
	}

	@RequestMapping(path="/{uuid}", method=RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Sigmet getSigmetAsJson(@PathVariable String uuid) {
		return sigmetStore.getByUuid(uuid);
	}

	@RequestMapping(path="/{uuid}",
			method = RequestMethod.GET,
			produces = MediaType.TEXT_PLAIN_VALUE)
	public String getTacById(@PathVariable String uuid) {
		Sigmet sm = sigmetStore.getByUuid(uuid);
		Feature FIR=firStore.lookup(sm.getFirname(), true);
		return sm.toTAC(FIR);
	}

	@RequestMapping(path="/{uuid}",
			method = RequestMethod.GET,
			produces = MediaType.APPLICATION_XML_VALUE)
	public String getIWXXM21ById(@PathVariable String uuid) {
		Sigmet sigmet=sigmetStore.getByUuid(uuid);
		return sigmetConverter.ToIWXXM_2_1(sigmet);
	}

	/**
	 * Delete a SIGMET by its uuid
	 * @param uuid
	 * @return ok if the SIGMET was successfully deleted, BAD_REQUEST if the SIGMET didn't exist, is not in concept, or if some other error occurred
	 */
	@RequestMapping(path="/{uuid}",
			method = RequestMethod.DELETE,
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public String deleteSigmetById(@PathVariable String uuid) throws GeoWebServiceException {
		Sigmet sigmet = sigmetStore.getByUuid(uuid);
		if (sigmet == null) {
			throw new EntityDoesNotExistException("SIGMET", uuid);
		}
		if (sigmet.getStatus() != SigmetStatus.concept) {
			throw new EntityNotInConceptException("SIGMET", uuid);
		}
		boolean ret = sigmetStore.deleteSigmetByUuid(uuid);
		if(ret) {
			return "deleted " + uuid;
		} else {
			throw new RuntimeException("Unable to delete SIGMET with id " + uuid);
		}
	}

	static SigmetParameters sigmetParameters;
	@RequestMapping(path="/getsigmetparameters", method=RequestMethod.GET)
	public SigmetParameters getSigmetParameters() {
		if (sigmetParameters==null) {
			sigmetParameters=new SigmetParameters();
		}
		return sigmetParameters;
	}

	@RequestMapping(path="/putsigmetparameters", method=RequestMethod.GET)
	public ResponseEntity<String> storeSigmetParameters(String json) { 
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);				
	}

	@RequestMapping(path="/getsigmetphenomena", method=RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public List<SigmetPhenomenaMapping.SigmetPhenomenon> SigmetPhenomena() {
		return new SigmetPhenomenaMapping().getPhenomena();
	}

	@RequestMapping(
			path = "/sigmetintersections", 
			method = RequestMethod.POST, 
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Feature SigmetIntersections(@RequestBody SigmetFeature feature) throws GeoJsonConversionException, GeoWebServiceException {
		String FIRName=feature.getFirname();
		Feature FIR=firStore.lookup(FIRName, true);

		if (FIR == null) {
			throw new SigmetFeatureFirNotFoundException(FIRName);
		}
		LOGGER.debug("SigmetIntersections for {} {}", FIRName, FIR);
		Feature f=feature.getFeature();
		GeoJsonObject intersectionGeometry;
		String intersectionSelectionType;
		String providedFeatureSelectionType = f.getProperty("selectionType");

		if (StringUtils.equals(providedFeatureSelectionType, "fir")) {
			intersectionGeometry = FIR.getGeometry();
			intersectionSelectionType = "poly";
		} else {
			intersectionGeometry = GeoJsonIntersectionHelper.intersection(f.getGeometry(), FIR.getGeometry());
			intersectionSelectionType = providedFeatureSelectionType;
		}

		Feature intersectionFeature = new Feature();
		intersectionFeature.setGeometry(intersectionGeometry);
		intersectionFeature.setProperty("selectionType", intersectionSelectionType);

		return intersectionFeature;
	}

	@RequestMapping(
			path = "",
			method = RequestMethod.GET,
			produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public SigmetPaginationWrapper getSigmetList(@RequestParam(value="active", required=true) Boolean active,
			@RequestParam(value="status", required=false) SigmetStatus status,
			@RequestParam(value="page", required=false) Integer page,
			@RequestParam(value="count", required=false) Integer count) {
		LOGGER.debug("getSigmetList");
		Sigmet[] sigmets=sigmetStore.getSigmets(active, status);
		LOGGER.debug("SIGMETLIST has length of {}", sigmets.length);
		return new SigmetPaginationWrapper(sigmets,page,count);
	}

	@RequestMapping(path="/getfir", method=RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Feature getFirByName(@RequestParam(value="name", required=true) String firName) {
		return firStore.lookup(firName, true);
	}
}
