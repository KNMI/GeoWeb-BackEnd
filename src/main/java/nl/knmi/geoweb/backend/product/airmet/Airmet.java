package nl.knmi.geoweb.backend.product.airmet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.geojson.Polygon;

import lombok.Getter;
import lombok.Setter;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.Tools;

import nl.knmi.geoweb.backend.product.GeoWebProduct;
import nl.knmi.geoweb.backend.product.IExportable;
import nl.knmi.geoweb.backend.product.ProductConverter;
import nl.knmi.geoweb.backend.product.sigmetairmet.ObsFc;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetChange;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetLevel;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetMovement;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetStatus;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetType;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetUtils;

@JsonInclude(Include.NON_EMPTY)
@Getter
@Setter
public class Airmet implements GeoWebProduct, IExportable<Airmet> {
    public static final Duration WAVALIDTIME = Duration.ofHours(4); //4*3600*1000;

    private GeoJsonObject geojson;
    private Phenomenon phenomenon;
    private List<ObscuringPhenomenonList.ObscuringPhenomenon> obscuring;
    private AirmetWindInfo wind;
    private AirmetCloudLevelInfo cloudLevels;

    private AirmetValue visibility;
    private ObsFc obs_or_forecast;
    private SigmetAirmetLevel levelinfo;
    private AirmetMovementType movement_type;
    private SigmetAirmetMovement movement;
    private SigmetAirmetChange change;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime issuedate;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime validdate;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime validdate_end;
    private String firname;
    private String location_indicator_icao;
    private String location_indicator_mwo;
    private String uuid;
    private SigmetAirmetStatus status;
    private SigmetAirmetType type;
    private int sequence;

    @JsonInclude(Include.NON_NULL)
    private Integer cancels;
    @JsonInclude(Include.NON_NULL)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime cancelsStart;

    @JsonIgnore
    private Feature firFeature;

    private String obscuringToTAC() {
        if (this.obscuring != null && this.obscuring.size() > 0) {
            ObscuringPhenomenonList.ObscuringPhenomenon obscuring = this.obscuring.get(0);
            if (obscuring != null && obscuring.getName() != null && obscuring.getCode() != null) {
                return "(" + obscuring.toTAC() + ")";
            }
        }
        return "";
    }

    private String visibilityToTAC() {
        if ((this.visibility != null) && (this.visibility.val != null) && (this.visibility.unit != null)) {
            if (this.visibility.val >= 10000) {
                this.visibility.val = Double.valueOf(9999);
            }
            return String.format("%04.0f", this.visibility.val) + this.visibility.unit;
        }
        return "";
    }

    public enum ParamInfo {
        WITH_CLOUDLEVELS, WITH_OBSCURATION, WITH_WIND;
    }

    @Getter
    @Setter
    public static class AirmetWindInfo {
        private AirmetValue speed;
        private AirmetValue  direction;

        public AirmetWindInfo(double speed, String speedUnit, double direction, String unit) {
            this.speed=new AirmetValue(speed, speedUnit);
            this.direction=new AirmetValue(direction, unit);
        }

        public AirmetWindInfo(double speed, double direction) {
            this(speed, "KT", direction, "degrees");
        }

        public AirmetWindInfo(){
        }

        public String toTAC() {
            if ((this.speed.val != null) && (this.speed.unit != null) && (this.direction.val != null)) {
                return String.format("%03.0f", this.direction.val) + "/" + String.format("%02.0f",this.speed.val) + this.speed.unit;
            }
            return "";
        }
    }

    @Getter
    public static class LowerCloudLevel extends AirmetValue {
        Boolean surface;

        public LowerCloudLevel() {}

        public LowerCloudLevel(boolean isSurface) {
            this.surface=isSurface;
        }

        public LowerCloudLevel(double level, String unit) {
            this.setVal(level);
            this.setUnit(unit);
        }

        public String toTAC() {
            if (surface == true) {
                return "SFC";
            }
            Double val = this.getVal();
            String unit = this.getUnit();
            if ((val != null) && (unit != null)) {
                return String.format("%04.0f", val);
            }
            return "";
        }
    }

    @Getter
    public static class UpperCloudLevel extends AirmetValue {
        Boolean above;

        public UpperCloudLevel() {}

        public UpperCloudLevel(double level, String unit) {
            this(false, level, unit);
        }

        public UpperCloudLevel(boolean isAbove, double level, String unit) {
            if (isAbove) {
                this.above=isAbove;
            }
            this.setVal(level);
            this.setUnit(unit);
        }

        public String toTAC() {
            Double val = this.getVal();
            String unit = this.getUnit();
            String above = this.above == true ? "ABV" : "";
            if ((val != null) && (unit != null)) {
                return  above + String.format("%04.0f", val) + unit;
            }
            return "";
        }
    }

    @Getter
    @Setter
    public static class AirmetCloudLevelInfo {

        private LowerCloudLevel lower;
        private UpperCloudLevel upper;

        public AirmetCloudLevelInfo(double upper) {
            this.lower=new LowerCloudLevel(true);
            this.upper=new UpperCloudLevel(upper, "FT");
        }

        public AirmetCloudLevelInfo(boolean above, double upper) {
            this(upper);
            this.getUpper().above=above;
        }

        public AirmetCloudLevelInfo(double lower, double upper) {
            this.lower=new LowerCloudLevel(lower, "FT");
            this.upper=new UpperCloudLevel(upper, "FT");
        }

        public AirmetCloudLevelInfo(double lower, boolean above, double upper, String unit) {
            this.lower=new LowerCloudLevel(lower, unit);
            this.upper=new UpperCloudLevel(above, upper, unit);
        }

        public AirmetCloudLevelInfo(boolean isSurface, boolean above, double upper, String unit) {
            this.lower = new LowerCloudLevel(isSurface);
            this.upper = new UpperCloudLevel(above, upper, unit);
        }

        public AirmetCloudLevelInfo(){}

        public String toTAC() {
            String lowerTAC = this.lower.toTAC();
            String upperTAC = this.upper.toTAC();

            if ((!lowerTAC.isEmpty()) && (!upperTAC.isEmpty())) {
                return lowerTAC + "/" + upperTAC;
            }
            return "";
        }
    }

    @Getter
    @Setter
    public static class AirmetValue {
        private Double val;
        private String unit;
        public AirmetValue(double val, String unit) {
            this.val=val;
            this.unit=unit;
        }
        public AirmetValue(){
        }
    }

    @Getter
    public enum Phenomenon {
        BKN_CLD("BKN CLD", ParamInfo.WITH_CLOUDLEVELS),
        OVC_CLD("OVC CLD", ParamInfo.WITH_CLOUDLEVELS),
        FRQ_CB("FRQ CB"),
        FRQ_TCU("FRQ TCU"),
        ISOL_CB("ISOL CB"),
        ISOL_TCU("ISOL TCU"),
        ISOL_TS("ISOL TS"),
        ISOL_TSGR("ISOL TSGR"),
        MOD_ICE("MOD ICE"),
        MOD_MTW("MOD MTW"),
        MOD_TURB("MOD TURB"),
        MT_OBSC("MT OBSC"),
        OCNL_CB("OCNL CB"),
        OCNL_TS("OCNL TS"),
        OCNL_TSGR("OCNL TSGR"),
        OCNL_TCU("OCNL TCU"),
        SFC_VIS("SFC VIS", ParamInfo.WITH_OBSCURATION),
        SFC_WIND("SFC WIND", ParamInfo.WITH_WIND);


        private String description;
        private String shortDescription;
        private ParamInfo paramInfo;

        public static Phenomenon getRandomPhenomenon() {
            int i=(int)(Math.random()*Phenomenon.values().length);
            return Phenomenon.valueOf(Phenomenon.values()[i].toString());
        }

        private Phenomenon(String shrt, ParamInfo paramInfo) {
            this(shrt, "", paramInfo);
        }

        private Phenomenon(String shrt) {
            this(shrt, "", null);
        }

        private Phenomenon(String shrt, String description, ParamInfo paramInfo) {
            this.shortDescription=shrt;
            this.description=description;
            this.paramInfo = paramInfo;
        }

        public static Phenomenon getPhenomenon(String desc) {
            for (Phenomenon phen: Phenomenon.values()) {
                if (desc.equals(phen.toString())){
                    return phen;
                }
            }
            return null;
            //throw new Exception("You NOOB: Non existing pheonomenon!!!" + desc);
        }
    }

    @Getter
    public enum AirmetStatus {
        concept("concept"), canceled("canceled"), published("published");
        private String status;
        private AirmetStatus (String status) {
            this.status = status;
        }
        public static AirmetStatus getAirmetStatus(String status){
            Debug.println("AIRMET status: " + status);

            for (AirmetStatus sstatus: AirmetStatus.values()) {
                if (status.equals(sstatus.toString())){
                    return sstatus;
                }
            }
            return null;
        }

    }

    @Getter
    public enum AirmetType {
        normal("normal"), test("test"), exercise("exercise");
        private String type;
        private AirmetType (String type) {
            this.type = type;
        }
        public static AirmetType getAirmetType(String itype){
            for (AirmetType stype: AirmetType.values()) {
                if (itype.equals(stype.toString())){
                    return stype;
                }
            }
            return null;
        }

    }

    public enum AirmetMovementType {
        STATIONARY, MOVEMENT;
    }

 	public String toTAC() {
		if (this.firFeature!=null) {
			return this.toTAC(this.firFeature);
		}
		return "";
	}

	public String toTAC(Feature FIR) {
        GeoJsonObject effectiveStartGeometry = SigmetAirmetUtils.findStartGeometry(this.geojson);
        if ((effectiveStartGeometry == null)
                || (((Feature) effectiveStartGeometry).getProperty("selectionType") == null)) {
            return "Missing geometry";
        }
        if (!((Feature) effectiveStartGeometry).getProperty("selectionType").equals("box")
                && !((Feature) effectiveStartGeometry).getProperty("selectionType").equals("fir")
                && !((Feature) effectiveStartGeometry).getProperty("selectionType").equals("point")) {
            GeoJsonObject intersected = SigmetAirmetUtils.extractSingleStartGeometry(this.geojson);
            try {
				int sz=((Polygon)((Feature)intersected).getGeometry()).getCoordinates().get(0).size();
				if (sz<=7)  {
					effectiveStartGeometry = intersected; // Use intersection result
				}
			}
			catch(Exception e) {}
        }
        StringBuilder sb = new StringBuilder();
        String validdateFormatted = String.format("%02d", this.validdate.getDayOfMonth())
                + String.format("%02d", this.validdate.getHour()) + String.format("%02d", this.validdate.getMinute());
        String validdateEndFormatted = String.format("%02d", this.validdate_end.getDayOfMonth())
                + String.format("%02d", this.validdate_end.getHour())
                + String.format("%02d", this.validdate_end.getMinute());

        sb.append(this.location_indicator_icao).append(" AIRMET ").append(this.sequence).append(" VALID ")
                .append(validdateFormatted).append('/').append(validdateEndFormatted).append(' ')
                .append(this.location_indicator_mwo).append('-');
        sb.append('\n');

        sb.append(this.location_indicator_icao).append(' ').append(this.firname);

        if (this.cancels != null && this.cancelsStart != null) {
            String validdateCancelled = String.format("%02d", this.cancelsStart.getDayOfMonth())
                    + String.format("%02d", this.cancelsStart.getHour())
                    + String.format("%02d", this.cancelsStart.getMinute());

            sb.append(' ').append("CNL AIRMET ").append(this.cancels).append(" ").append(validdateCancelled).append('/')
                    .append(validdateEndFormatted);
            return sb.toString();
        }
        sb.append('\n');
        /* Test or exercise */
        SigmetAirmetType type = this.type == null ? SigmetAirmetType.normal : this.type;
        switch (type) {
        case test:
            sb.append("TEST ");
            break;
        case exercise:
            sb.append("EXER ");
            break;
        default:
        }

        Debug.println("phen: " + this.phenomenon);
        sb.append(this.phenomenon.getShortDescription());

        switch (this.phenomenon) {
            case SFC_WIND:
                if (this.wind != null) {
                    sb.append(" ");
                    sb.append(this.wind.toTAC());
                }
                break;
            case SFC_VIS:
                sb.append(" ");
                sb.append(this.visibilityToTAC());
                sb.append(" ");
                sb.append(this.obscuringToTAC());
                break;
            case BKN_CLD:
            case OVC_CLD:
                if (this.cloudLevels != null) {
                    sb.append(" ");
                    sb.append(this.cloudLevels.toTAC());
                }
                break;
            default:
                break;
        }

        sb.append('\n');
        if (this.getObs_or_forecast() != null) {
            sb.append(this.obs_or_forecast.toTAC());
            sb.append('\n');
        }
        sb.append(SigmetAirmetUtils.featureToTAC((Feature) effectiveStartGeometry, FIR));
        sb.append('\n');

        String levelInfoText = this.levelinfo != null
            ? this.levelinfo.toTAC()
            : "";
        if (!levelInfoText.isEmpty()) {
            sb.append(levelInfoText);
            sb.append('\n');
        }

        if (this.movement_type == null) {
            this.movement_type = AirmetMovementType.STATIONARY;
        }

        switch (this.movement_type) {
        case STATIONARY:
            sb.append("STNR ");
            break;
        case MOVEMENT:
            if (this.movement != null) {
                sb.append(this.movement.toTAC());
                sb.append('\n');
            }
            break;
        }

        if (this.change != null) {
            sb.append(this.change.toTAC());
            sb.append('\n');
        }

        return sb.toString();
    }

    public String toJSON(ObjectMapper om) throws JsonProcessingException {
        return om.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    }

    public Airmet() {
        this.sequence=-1;
        this.obscuring = new ArrayList<>();
    }

    public Airmet(String firname, String location, String issuing_mwo, String uuid) {
        this.firname=firname;
        this.location_indicator_icao=location;
        this.location_indicator_mwo=issuing_mwo;
        this.uuid=uuid;
        this.sequence=-1;
        this.obscuring = new ArrayList<>();
        this.phenomenon = null;
        // If an AIRMET is posted, this has no effect
        this.status= SigmetAirmetStatus.concept;
        this.type= SigmetAirmetType.normal;
        this.change= SigmetAirmetChange.NC;
    }

    public Airmet(Airmet otherAirmet) {
        this.firname=otherAirmet.getFirname();
        this.location_indicator_icao=otherAirmet.getLocation_indicator_icao();
        this.location_indicator_mwo=otherAirmet.getLocation_indicator_mwo();
        this.sequence=-1;
        this.obscuring = otherAirmet.getObscuring();
        this.phenomenon = otherAirmet.getPhenomenon();
        this.change = otherAirmet.getChange();
        this.geojson = otherAirmet.getGeojson();
        this.levelinfo = otherAirmet.getLevelinfo();
        this.movement = otherAirmet.getMovement();
        this.obs_or_forecast = otherAirmet.getObs_or_forecast();
        this.movement_type = otherAirmet.getMovement_type();
        this.visibility = otherAirmet.getVisibility();
        this.wind = otherAirmet.getWind();
        this.cloudLevels = otherAirmet.getCloudLevels();
        this.validdate = otherAirmet.getValiddate();
        this.validdate_end = otherAirmet.getValiddate_end();
        this.issuedate = otherAirmet.getIssuedate();
        this.firFeature = otherAirmet.getFirFeature();
        this.type=otherAirmet.getType();
    }

    public void serializeAirmet(ObjectMapper om, String fn) {
        Debug.println("serializeAirmet to "+fn);
        if(/*this.geojson == null ||*/ this.phenomenon == null) {
            throw new IllegalArgumentException("GeoJSON and Phenomenon are required");
        }
        // .... value from constructor is lost here, set it explicitly. (Why?)
        if(this.status == null) {
            this.status = SigmetAirmetStatus.concept;
        }
        if(this.type == null) {
            this.type = SigmetAirmetType.normal;
        }
        if(this.change == null) {
            this.change = SigmetAirmetChange.NC;
        }
        try {
            om.writeValue(new File(fn), this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String dumpAirmetGeometryInfo() {
        StringWriter sw=new StringWriter();
        PrintWriter pw=new PrintWriter(sw);
        pw.println("AIRMET ");
        FeatureCollection fc=(FeatureCollection)this.geojson;
        for (Feature f: fc.getFeatures()) {
            pw.print((f.getId()==null)?"  ":f.getId());
            pw.print(" ");
            pw.print((f.getProperty("featureFunction")==null)?"  ":f.getProperty("featureFunction").toString());
            pw.print(" ");
            pw.print((f.getProperty("selectionType")==null)?"  ":f.getProperty("selectionType").toString());
            pw.print(" ");
            pw.print((f.getProperty("relatesTo")==null)?"  ":f.getProperty("relatesTo").toString());
            pw.println();
        }
        return sw.toString();
    }

    public String serializeAirmetToString(ObjectMapper om) throws JsonProcessingException {
        return om.writeValueAsString(this);
    }

    public static Airmet getAirmetFromFile(ObjectMapper om, File f) throws JsonParseException, JsonMappingException, IOException {
        Airmet sm=om.readValue(f, Airmet.class);
        //		Debug.println("Airmet from "+f.getName());
        //		Debug.println(sm.dumpAirmetGeometryInfo());
        return sm;
    }

    @Override
    public String export(final File path, final ProductConverter<Airmet> converter, final ObjectMapper om) {
        List<String> toDeleteIfError=new ArrayList<>(); // List of products to delete in case of error
        try {
            OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Z"));
            String time = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String tacHeaderTime = now.format(DateTimeFormatter.ofPattern("ddHHmm"));
            String validTime = this.getValiddate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmm"));

            String bulletinHeader = "WANL31";
            String iwxxmBulletinHeader = "LWNL31";
            String tacHeaderLocation = this.getLocation_indicator_mwo();

            String tacFileName = bulletinHeader + tacHeaderLocation + "_" + validTime + "_" + time;
            String tacFilePath = path.getPath() + "/" + tacFileName + ".tac";

            // Create TAC file
            String tacHeader = "ZCZC\n" + bulletinHeader + " " + tacHeaderLocation + " " + tacHeaderTime + "\n";
            String tacCode = this.toTAC(this.getFirFeature())
                .replaceAll("(?m)^[ \\t]*\\r?\\n", "").trim(); // remove empty lines
            String tacFooter = "=\nNNNN\n";
            Tools.writeFile(tacFilePath, tacHeader + tacCode +  tacFooter);
            toDeleteIfError.add(tacFilePath);

            // Create JSON file
            String jsonFileName = "AIRMET_" + tacHeaderLocation + "_" + validTime + "_" + time;
            String jsonFilePath = path.getPath() + "/" + jsonFileName + ".json";
            Tools.writeFile(jsonFilePath, this.toJSON(om));
            toDeleteIfError.add(jsonFilePath);

            String iwxxmName="A_"+iwxxmBulletinHeader+this.getLocation_indicator_mwo()+this.getValiddate().format(DateTimeFormatter.ofPattern("ddHHmm"));
            if (status.equals(SigmetAirmetStatus.canceled)){
                iwxxmName+="CNL";
            }
            iwxxmName+="_C_"+this.getLocation_indicator_mwo()+"_"+time;
            String s=converter.ToIWXXM_2_1(this);
            if ("FAIL".equals(s)) {
                Debug.println(" ToIWXXM_2_1 failed");
                toDeleteIfError.stream().forEach(f ->  {Debug.println("REMOVING "+f); Tools.rm(f); });
                return "ERROR: airmet.ToIWXXM_2_1() failed";
            } else {
                Tools.writeFile(path.getPath() + "/" + iwxxmName + ".xml", s);
            }
        } catch (IOException | NullPointerException e) {
            toDeleteIfError.stream()
                .forEach(f ->  {
                    Debug.println("REMOVING "+f); Tools.rm(f);
                });
            return "ERROR: "+e.getMessage();
        }
        return "OK";
    }
}
