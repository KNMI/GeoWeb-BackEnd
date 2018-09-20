package nl.knmi.geoweb.backend.services;

import java.io.IOException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;

import nl.knmi.geoweb.backend.datastore.ProductExporter;
import nl.knmi.geoweb.backend.datastore.TafStore;
import nl.knmi.geoweb.backend.services.error.EntityDoesNotExistException;
import nl.knmi.geoweb.backend.services.error.EntityNotInConceptException;
import nl.knmi.geoweb.backend.services.error.GeoWebServiceException;
import nl.knmi.geoweb.backend.services.error.TafAlreadyPublishedException;
import nl.knmi.geoweb.backend.services.error.TafDoesNotMatchPrevious;
import nl.knmi.geoweb.backend.services.error.PreviousTafNotYetPublishedException;
import nl.knmi.geoweb.backend.services.error.UnableToPublishTafException;
import nl.knmi.geoweb.backend.services.error.UnsupportedTafTypeException;
import nl.knmi.geoweb.backend.product.taf.TAFtoTACMaps;
import nl.knmi.geoweb.backend.product.taf.Taf;
import nl.knmi.geoweb.backend.product.taf.Taf.TAFReportPublishedConcept;
import nl.knmi.geoweb.backend.product.taf.Taf.TAFReportType;
import nl.knmi.geoweb.backend.product.taf.TafSchemaStore;
import nl.knmi.geoweb.backend.product.taf.TafValidationResult;
import nl.knmi.geoweb.backend.product.taf.TafValidator;
import nl.knmi.geoweb.backend.product.taf.converter.TafConverter;
import nl.knmi.geoweb.backend.services.view.StoreTafResult;
import nl.knmi.geoweb.backend.services.view.TafPaginationWrapper;

@RestController
public class TafServices {
    private static final Logger LOGGER = LoggerFactory.getLogger(TafServices.class);

    @Autowired
    @Qualifier("tafObjectMapper")
    private ObjectMapper tafObjectMapper;

    @Autowired
    TafStore tafStore;
    ProductExporter<Taf> publishTafStore;
    TafSchemaStore tafSchemaStore;
    TafValidator tafValidator;

    @Autowired
    private TafConverter tafConverter;

    TafServices(final TafStore tafStore, final TafSchemaStore tafSchemaStore, final TafValidator tafValidator, final ProductExporter<Taf> publishTafStore) {
        this.tafStore = tafStore;
        this.tafSchemaStore = tafSchemaStore;
        this.tafValidator = tafValidator;
        this.publishTafStore = publishTafStore;
    }

    @RequestMapping(path = "/tafs/verify", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    public ResponseEntity<String> verifyTAF(@RequestBody String tafStr) throws IOException, JSONException, ParseException {
        tafStr = URLDecoder.decode(tafStr, "UTF8");
        /* Add TAC */
        String TAC = "unable to create TAC";
        try {
            TAC = tafObjectMapper.readValue(tafStr, Taf.class).toTAC();
        } catch (Exception e) {
            LOGGER.error("unable to read TAC", e);
        }

        try {
            TafValidationResult jsonValidation = tafValidator.validate(tafStr);
            if (jsonValidation.isSucceeded() == false) {
                ObjectNode errors = jsonValidation.getErrors();
                String finalJson = new JSONObject()
                        .put("succeeded", false)
                        .put("errors", new JSONObject(errors.toString()))
                        .put("TAC", TAC)
                        .put("message", "TAF is not valid").toString();
                return ResponseEntity.ok(finalJson);
            } else {
                // If there is already a taf published for this location and airport; only for type==normal
                Taf taf = tafObjectMapper.readValue(tafStr, Taf.class);
                Taf[] tafs = tafStore.getTafs(true, TAFReportPublishedConcept.published, null, taf.metadata.getLocation());
                if (taf.metadata.getStatus() != TAFReportPublishedConcept.published && taf.metadata.getType() == TAFReportType.normal &&
                        Arrays.stream(tafs).anyMatch(publishedTaf -> publishedTaf.metadata.getLocation().equals(taf.metadata.getLocation()) &&
                                publishedTaf.metadata.getValidityStart().isEqual(taf.metadata.getValidityStart()))) {
                    String finalJson = new JSONObject()
                            .put("succeeded", false)
                            .put("TAC", TAC)
                            .put("message", "There is already a published TAF for " + taf.metadata.getLocation() + " at " + TAFtoTACMaps.toDDHH(taf.metadata.getValidityStart())).toString();
                    return ResponseEntity.ok(finalJson);
                }

                String json = new JSONObject().put("succeeded", true).put("message", "TAF is verified.").put("TAC", TAC).toString();
                return ResponseEntity.ok(json);
            }
        } catch (ProcessingException e) {
            LOGGER.error("Unable to validate TAF", e);
            String json = new JSONObject().
                    put("message", "Unable to validate taf").toString();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(json);
        }
    }

    /**
     * POST a TAF to the product store
     *
     * @param taf
     * @return
     * @throws IOException
     * @throws JSONException
     */
    @RequestMapping(
            path = "/tafs",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    public ResponseEntity<StoreTafResult> storeTAF(@RequestBody Taf taf) throws IOException, GeoWebServiceException {
        LOGGER.debug("storetaf");

        if (taf.metadata.getUuid() == null) {            //Generate random uuid
            taf.metadata.setUuid(UUID.randomUUID().toString());
        }

        if (taf.metadata.getType() == null) {
            taf.metadata.setType(TAFReportType.normal);
        }

        if (taf.metadata.getStatus() == null) {
            taf.metadata.setStatus(TAFReportPublishedConcept.concept);
        }

        LOGGER.debug("Posting TAF of type {}", taf.metadata.getType());
        switch (taf.metadata.getType()) {
            case normal:
                //Check if TAF with uuid is already published
                if (tafStore.isPublished(taf.getMetadata().getUuid())) {
                    throw new TafAlreadyPublishedException(taf.metadata.getUuid());
                }
                if (taf.getMetadata().getStatus() == TAFReportPublishedConcept.concept) {
                    taf.metadata.setIssueTime(null);
                    if (taf.metadata.getBaseTime() == null) {
                        taf.metadata.setBaseTime(taf.metadata.getValidityStart());
                    }
                    tafStore.storeTaf(taf);
                } else if (taf.getMetadata().getStatus() == TAFReportPublishedConcept.published) {
                    if (tafStore.isPublished(taf.getMetadata().getLocation(), taf.getMetadata().getValidityStart(), taf.getMetadata().getValidityEnd())) {
                        throw new TafAlreadyPublishedException(taf.metadata.getUuid());
                    }
                    // Publish it
                    if (taf.metadata.getBaseTime() == null) {
                        taf.metadata.setBaseTime(taf.metadata.getValidityStart());
                    }
                    //Set issuetime
                    taf.metadata.setIssueTime(OffsetDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.SECONDS));
                    String result = this.publishTafStore.export(taf, tafConverter, tafObjectMapper);

                    if (!StringUtils.equals(result, "OK")) {
                        throw new UnableToPublishTafException(taf.metadata.getLocation(), result);
                    }

                    //Only set to published if export has succeeded
                    tafStore.storeTaf(taf);

                    return ResponseEntity.ok(new StoreTafResult(true, "Taf with id " + taf.metadata.getUuid() + " is published", taf));
                }

                return ResponseEntity.ok(new StoreTafResult(true, "Taf with id " + taf.metadata.getUuid() + " is stored", taf));
            case amendment:
            case correction:
                if (tafStore.isPublished(taf.getMetadata().getUuid())) {
                    throw new TafAlreadyPublishedException(taf.metadata.getUuid());
                }
                if (!tafStore.isPublished(taf.getMetadata().getPreviousUuid())) {
                    throw new PreviousTafNotYetPublishedException(taf.metadata.getPreviousUuid());
                }
                Taf previousTaf = tafStore.getByUuid(taf.getMetadata().getPreviousUuid());
                assertTafMatchesPrevious(taf, previousTaf);

                if (taf.getMetadata().getStatus().equals(TAFReportPublishedConcept.concept)) {
                        if (taf.metadata.getType() == TAFReportType.amendment) { //Only change validityTime for amendments
                            Instant iValidityStart = Instant.now().truncatedTo(ChronoUnit.HOURS);
                            taf.getMetadata().setValidityStart(iValidityStart.atOffset(ZoneOffset.of("Z")));
                        }
                        taf.getMetadata().setIssueTime(null);
                        previousTaf.getMetadata().setPreviousMetadata(null); //Wipe previousMetadata of previousTaf object
                        taf.getMetadata().setPreviousMetadata(previousTaf.getMetadata());
                        taf.getMetadata().setBaseTime(previousTaf.getMetadata().getBaseTime());
                        tafStore.storeTaf(taf);

                        return ResponseEntity.ok(new StoreTafResult(true, "Taf with id " + taf.metadata.getUuid() + " is stored", taf));
                } else if (taf.getMetadata().getStatus().equals(TAFReportPublishedConcept.published)) {
                        if (previousTaf.getMetadata().getValidityEnd().isAfter(OffsetDateTime.now(ZoneId.of("UTC")))) {
                            taf.metadata.setIssueTime(OffsetDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.SECONDS));
                            previousTaf.getMetadata().setPreviousMetadata(null); //Wipe previousMetadata of previousTaf object
                            taf.metadata.setPreviousMetadata(previousTaf.getMetadata());
                            tafStore.storeTaf(taf);
                            /* Publish TAF */
                            if (taf.metadata.getBaseTime() == null) {
                                taf.metadata.setBaseTime(previousTaf.metadata.getBaseTime());
                            }
                            this.publishTafStore.export(taf, tafConverter, tafObjectMapper);
                            /* After successful publish make previous TAF inactive */
                            LOGGER.debug("Inactivating TAF with uuid {}", previousTaf.getMetadata().getUuid());
                            LOGGER.debug("Inactivating TAF with previousMetadata uuid {}", taf.getMetadata().getPreviousMetadata().getUuid());
                            previousTaf.getMetadata().setStatus(TAFReportPublishedConcept.inactive);
                            tafStore.storeTaf(previousTaf);

                            return ResponseEntity.ok(new StoreTafResult(true, "Taf with id " + taf.metadata.getUuid() + " is " + taf.metadata.getType(), taf));
                        } else {
                            //Error
                            LOGGER.error("Error: COR/AMD for old TAF");
                        }
                }

                return ResponseEntity.ok(new StoreTafResult(false, "Unable to store taf with uuid " + taf.metadata.getUuid(), taf));
            case canceled:
                if (tafStore.isPublished(taf.getMetadata().getUuid())) {
                    throw new TafAlreadyPublishedException(taf.metadata.getUuid());
                }
                if (!tafStore.isPublished(taf.getMetadata().getPreviousUuid())) {
                    throw new PreviousTafNotYetPublishedException(taf.getMetadata().getPreviousUuid());
                }

                previousTaf = tafStore.getByUuid(taf.getMetadata().getPreviousUuid());
                assertTafMatchesPrevious(taf, previousTaf);

                if (!previousTaf.getMetadata().getStatus().equals(TAFReportPublishedConcept.published)) {
                    throw new PreviousTafNotYetPublishedException(previousTaf.getMetadata().getUuid());
                }

                    Instant iValidityStart = Instant.now().truncatedTo(ChronoUnit.HOURS);
                    taf.getMetadata().setValidityStart(iValidityStart.atOffset(ZoneOffset.of("Z")));
                    taf.getMetadata().setIssueTime(OffsetDateTime.now(ZoneId.of("UTC")).truncatedTo(ChronoUnit.SECONDS));
                    taf.setForecast(null);
                    taf.setChangegroups(null);
                    previousTaf.getMetadata().setPreviousMetadata(null); //Wipe previousMetadata of previousTaf object
                    taf.getMetadata().setPreviousMetadata(previousTaf.getMetadata());

                    if (taf.metadata.getBaseTime() == null) {
                        taf.metadata.setBaseTime(previousTaf.metadata.getBaseTime());
                    }
                    LOGGER.debug("storing cancel");
                    tafStore.storeTaf(taf);
                    LOGGER.debug("publishing cancel");

                    this.publishTafStore.export(taf, tafConverter, tafObjectMapper);

                    return ResponseEntity.ok(new StoreTafResult(true, "Taf with id " + taf.metadata.getPreviousUuid() + " is canceled", taf));
            default:
                throw new UnsupportedTafTypeException(taf.metadata.getType());
        }
    }

    private void assertTafMatchesPrevious(Taf taf, Taf previousTaf) throws TafDoesNotMatchPrevious {
        if (!followupMetadataMatches(previousTaf.getMetadata(), taf.getMetadata())) {
            throw new TafDoesNotMatchPrevious(taf.getMetadata().getUuid(), previousTaf.getMetadata().getUuid());
        }
    }

    private boolean followupMetadataMatches(Taf.Metadata previousMetadata, Taf.Metadata nextMetadata) {
        return
                previousMetadata.getLocation().equals(nextMetadata.getLocation()) &&
                previousMetadata.getValidityEnd().equals(nextMetadata.getValidityEnd());
    }

    /**
     * Get list of tafs
     *
     * @param active
     * @param status
     * @param page
     * @param count
     * @return
     */
    @RequestMapping(
            path = "/tafs",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<TafPaginationWrapper> getTafList(
        @RequestParam(value = "active", required = true) Boolean active,
        @RequestParam(value = "status", required = false) TAFReportPublishedConcept status,
        @RequestParam(value = "uuid", required = false) String uuid,
        @RequestParam(value = "location", required = false) String location,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "count", required = false) Integer count
    ) throws IOException {
        LOGGER.debug("getTafList");

            final Taf[] tafs = tafStore.getTafs(active, status, uuid, location);
            Taf[] filteredTafs = Arrays.stream(tafs).filter(
                    // The TAF is still valid....
                    taf -> taf.metadata.getValidityEnd().isAfter(OffsetDateTime.now(ZoneId.of("Z"))) &&
                            // And there is no other taf...
                            Arrays.stream(tafs).noneMatch(
                                    otherTaf -> (!otherTaf.equals(taf) &&
                                            // For this location
                                            otherTaf.metadata.getLocation().equals(taf.metadata.getLocation()) &&
                                            // Such that the other TAF has a validity start later than *this* TAF...
                                            otherTaf.metadata.getValidityEnd().isAfter(taf.metadata.getValidityEnd()) &&
                                            // And the other TAF is already in its validity window
                                            otherTaf.metadata.getValidityStart().isBefore(OffsetDateTime.now(ZoneId.of("Z"))
                                            ))
                            )).toArray(Taf[]::new);

        return ResponseEntity.ok(new TafPaginationWrapper(filteredTafs, page, count));
    }

    /**
     * Delete a TAF by its uuid
     *
     * @param uuid
     * @return ok if the TAF was successfully deleted, BAD_REQUEST if the taf didn't exist, is not in concept, or if some other error occurred
     */
    @RequestMapping(path = "/tafs/{uuid}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String deleteTafById(@PathVariable String uuid) throws IOException, GeoWebServiceException {
        Taf taf = tafStore.getByUuid(uuid);
        if (taf == null) {
            throw new EntityDoesNotExistException("TAF", uuid);
        }
        if (taf.metadata.getStatus() != TAFReportPublishedConcept.concept) {
            throw new EntityNotInConceptException("TAF", uuid);
        }
        boolean ret = tafStore.deleteTafByUuid(uuid);
        if (ret) {
            return "deleted " + uuid;
        } else {
            throw new RuntimeException("Unable to delete TAF with id " + uuid);
        }
    }


    @RequestMapping(path = "/tafs/{uuid}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Taf getTafById(@PathVariable String uuid) throws IOException {
        return tafStore.getByUuid(uuid);
    }

    @RequestMapping(path = "/tafs/{uuid}",
            method = RequestMethod.GET,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public String getTacById(@PathVariable String uuid) throws IOException {
        return tafStore.getByUuid(uuid).toTAC();
    }

    @RequestMapping(path = "/tafs/{uuid}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_XML_VALUE)
    public String getIWXXM21ById(@PathVariable String uuid) throws IOException {
        Taf taf = tafStore.getByUuid(uuid);
        return tafConverter.ToIWXXM_2_1(taf);
    }

    @Deprecated
    @RequestMapping(path = "/gettaf", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Taf getTaf(@RequestParam(value = "uuid", required = true) String uuid) throws IOException {
        return tafStore.getByUuid(uuid);
    }

    @RequestMapping(path = "/publishtaf", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String publishTaf(String uuid) throws IOException {
        return "taf " + tafStore.getByUuid(uuid) + " published";
    }
}
