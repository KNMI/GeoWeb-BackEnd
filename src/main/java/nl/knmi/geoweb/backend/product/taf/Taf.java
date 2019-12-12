package nl.knmi.geoweb.backend.product.taf;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import nl.knmi.adaguc.tools.Tools;
import nl.knmi.geoweb.backend.product.GeoWebProduct;
import nl.knmi.geoweb.backend.product.IExportable;
import nl.knmi.geoweb.backend.product.ProductConverter;
import nl.knmi.geoweb.backend.product.taf.converter.TafConverter;
import nl.knmi.geoweb.backend.product.taf.serializers.CloudsSerializer;
import nl.knmi.geoweb.backend.product.taf.serializers.WeathersSerializer;
import nl.knmi.geoweb.backend.traceability.ProductTraceability;

@Slf4j
@Getter
@Setter
public class Taf implements GeoWebProduct, IExportable<Taf> {
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((changegroups == null) ? 0 : changegroups.hashCode());
        result = prime * result + ((forecast == null) ? 0 : forecast.hashCode());
        result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Taf other = (Taf) obj;
        if (changegroups == null) {
            if (other.changegroups != null)
                return false;
        } else if (!changegroups.equals(other.changegroups))
            return false;
        if (forecast == null) {
            if (other.forecast != null)
                return false;
        } else if (!forecast.equals(other.forecast))
            return false;
        if (metadata == null) {
            if (other.metadata != null)
                return false;
        } else if (!metadata.equals(other.metadata))
            return false;
        return true;
    }

    public static final String DATEFORMAT_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    public enum TAFReportType {
        retarded, normal, amendment, canceled, correction, missing;
    }

    public enum TAFReportPublishedConcept {
        concept, published, inactive
    }

    public enum TAFWindSpeedOperator {
        above, below
    }

    @Getter
    @Setter
    public static class Metadata {
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((baseTime == null) ? 0 : baseTime.hashCode());
            result = prime * result + ((extraInfo == null) ? 0 : extraInfo.hashCode());
            result = prime * result + ((issueTime == null) ? 0 : issueTime.hashCode());
            result = prime * result + ((location == null) ? 0 : location.hashCode());
            result = prime * result + ((previousUuid == null) ? 0 : previousUuid.hashCode());
            result = prime * result + ((status == null) ? 0 : status.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
            result = prime * result + ((validityEnd == null) ? 0 : validityEnd.hashCode());
            result = prime * result + ((validityStart == null) ? 0 : validityStart.hashCode());
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Metadata other = (Metadata) obj;
            if (baseTime == null) {
                if (other.baseTime != null)
                    return false;
            } else if (!baseTime.equals(other.baseTime))
                return false;
            if (extraInfo == null) {
                if (other.extraInfo != null)
                    return false;
            } else if (!extraInfo.equals(other.extraInfo))
                return false;
            if (issueTime == null) {
                if (other.issueTime != null)
                    return false;
            } else if (!issueTime.equals(other.issueTime))
                return false;
            if (location == null) {
                if (other.location != null)
                    return false;
            } else if (!location.equals(other.location))
                return false;
            if (previousUuid == null) {
                if (other.previousUuid != null)
                    return false;
            } else if (!previousUuid.equals(other.previousUuid))
                return false;
            if (status != other.status)
                return false;
            if (type != other.type)
                return false;
            if (uuid == null) {
                if (other.uuid != null)
                    return false;
            } else if (!uuid.equals(other.uuid))
                return false;
            if (validityEnd == null) {
                if (other.validityEnd != null)
                    return false;
            } else if (!validityEnd.equals(other.validityEnd))
                return false;
            if (validityStart == null) {
                if (other.validityStart != null)
                    return false;
            } else if (!validityStart.equals(other.validityStart))
                return false;
            return true;
        }
        private String previousUuid = null;
        private Metadata previousMetadata; //TODO: add this field to equals()/hashcode()
        private String uuid = null;
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        OffsetDateTime issueTime;
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        OffsetDateTime validityStart;
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        OffsetDateTime validityEnd;
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        OffsetDateTime baseTime;

        ObjectNode extraInfo;

        String location;
        TAFReportPublishedConcept status;
        TAFReportType type;

    };

    public Metadata metadata;

    @Setter
    @Getter
    public static class Forecast {
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((CaVOK == null) ? 0 : CaVOK.hashCode());
            result = prime * result + ((clouds == null) ? 0 : clouds.hashCode());
            result = prime * result + ((temperature == null) ? 0 : temperature.hashCode());
            result = prime * result + ((vertical_visibility == null) ? 0 : vertical_visibility.hashCode());
            result = prime * result + ((visibility == null) ? 0 : visibility.hashCode());
            result = prime * result + ((weather == null) ? 0 : weather.hashCode());
            result = prime * result + ((wind == null) ? 0 : wind.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Forecast other = (Forecast) obj;
            if (CaVOK == null) {
                if (other.CaVOK != null)
                    return false;
            } else if (!CaVOK.equals(other.CaVOK))
                return false;
            if (clouds == null) {
                if (other.clouds != null)
                    return false;
            } else if (!clouds.equals(other.clouds))
                return false;
            if (temperature == null) {
                if (other.temperature != null)
                    return false;
            } else if (!temperature.equals(other.temperature))
                return false;
            if (vertical_visibility == null) {
                if (other.vertical_visibility != null)
                    return false;
            } else if (!vertical_visibility.equals(other.vertical_visibility))
                return false;
            if (visibility == null) {
                if (other.visibility != null)
                    return false;
            } else if (!visibility.equals(other.visibility))
                return false;
            if (weather == null) {
                if (other.weather != null)
                    return false;
            } else if (!weather.equals(other.weather))
                return false;
            if (wind == null) {
                if (other.wind != null)
                    return false;
            } else if (!wind.equals(other.wind))
                return false;
            return true;
        }

        @Getter
        @Setter
        public static class TAFCloudType {
            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + ((amount == null) ? 0 : amount.hashCode());
                result = prime * result + ((height == null) ? 0 : height.hashCode());
                result = prime * result + ((isNSC == null) ? 0 : isNSC.hashCode());
                result = prime * result + ((mod == null) ? 0 : mod.hashCode());
                return result;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj)
                    return true;
                if (obj == null)
                    return false;
                if (getClass() != obj.getClass())
                    return false;
                TAFCloudType other = (TAFCloudType) obj;
                if (amount == null) {
                    if (other.amount != null)
                        return false;
                } else if (!amount.equals(other.amount))
                    return false;
                if (height == null) {
                    if (other.height != null)
                        return false;
                } else if (!height.equals(other.height))
                    return false;
                if (isNSC == null) {
                    if (other.isNSC != null)
                        return false;
                } else if (!isNSC.equals(other.isNSC))
                    return false;
                if (mod == null) {
                    if (other.mod != null)
                        return false;
                } else if (!mod.equals(other.mod))
                    return false;
                return true;
            }

            @JsonInclude(JsonInclude.Include.NON_NULL)
            Boolean isNSC = null;
            String amount;
            String mod;
            Integer height;

            public TAFCloudType() {
                this.isNSC = null;
            }

            public TAFCloudType(String cld) {
                if ("NSC".equalsIgnoreCase(cld)) {
                    isNSC = true;
                }
            }

            public TAFCloudType(TAFCloudType cld) {
                this.isNSC=cld.getIsNSC();
                this.amount=cld.getAmount();
                this.mod=cld.getMod();
                this.height=cld.getHeight();
            }

            public String toTAC() {
                StringBuilder sb = new StringBuilder();
                if (isNSC != null && isNSC) {
                    sb.append("NSC");
                } else {
                    sb.append(amount.toString());
                    sb.append(String.format("%03d", height));
                    if (mod != null) {
                        sb.append(mod);
                    }
                }
                return sb.toString();
            }
        }

        // @JsonInclude(JsonInclude.Include.NON_NULL)
        Integer vertical_visibility;

        @JsonSerialize(using = CloudsSerializer.class)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<TAFCloudType> clouds;

        @Getter
        @Setter
        public static class TAFWeather {
            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + ((descriptor == null) ? 0 : descriptor.hashCode());
                result = prime * result + ((isNSW == null) ? 0 : isNSW.hashCode());
                result = prime * result + ((phenomena == null) ? 0 : phenomena.hashCode());
                result = prime * result + ((qualifier == null) ? 0 : qualifier.hashCode());
                return result;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj)
                    return true;
                if (obj == null)
                    return false;
                if (getClass() != obj.getClass())
                    return false;
                TAFWeather other = (TAFWeather) obj;
                if (descriptor == null) {
                    if (other.descriptor != null)
                        return false;
                } else if (!descriptor.equals(other.descriptor))
                    return false;
                if (isNSW == null) {
                    if (other.isNSW != null)
                        return false;
                } else if (!isNSW.equals(other.isNSW))
                    return false;
                if (phenomena == null) {
                    if (other.phenomena != null)
                        return false;
                } else if (!phenomena.equals(other.phenomena))
                    return false;
                if (qualifier == null) {
                    if (other.qualifier != null)
                        return false;
                } else if (!qualifier.equals(other.qualifier))
                    return false;
                return true;
            }

            @JsonInclude(JsonInclude.Include.NON_NULL)
            Boolean isNSW = null;
            String qualifier;
            String descriptor;
            List<String> phenomena;

            TAFWeather(String ww) {
                if ("NSW".equalsIgnoreCase(ww)) {
                    isNSW = true;
                }
            }

            public TAFWeather() {
                isNSW = null;
            }

            public TAFWeather(TAFWeather w) {
                this.isNSW=w.getIsNSW();
                this.phenomena=w.getPhenomena();
                this.qualifier=w.qualifier;
            }

            public String toString() {
                StringBuilder sb = new StringBuilder();
                if (this.isNSW != null && this.isNSW == true) {
                    return "NSW";
                }
                if (this.qualifier != null) {
                    sb.append(TAFtoTACMaps.getQualifier(this.qualifier));
                }
                if (this.descriptor != null) {
                    sb.append(TAFtoTACMaps.getDescriptor(this.descriptor));
                }
                if (this.phenomena != null && !this.phenomena.isEmpty()) {
                    for (String phenomenon : this.phenomena) {
                        sb.append(TAFtoTACMaps.getPhenomena(phenomenon));
                    }
                }
                return sb.toString();
            }
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @JsonSerialize(using = WeathersSerializer.class)
        List<TAFWeather> weather;

        @Setter
        @Getter
        public static class TAFVisibility {
            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + ((unit == null) ? 0 : unit.hashCode());
                result = prime * result + ((value == null) ? 0 : value.hashCode());
                return result;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj)
                    return true;
                if (obj == null)
                    return false;
                if (getClass() != obj.getClass())
                    return false;
                TAFVisibility other = (TAFVisibility) obj;
                if (unit == null) {
                    if (other.unit != null)
                        return false;
                } else if (!unit.equals(other.unit))
                    return false;
                if (value == null) {
                    if (other.value != null)
                        return false;
                } else if (!value.equals(other.value))
                    return false;
                return true;
            }

            Integer value;
            String unit;

            public String toTAC() {
                if (unit == null || unit.equalsIgnoreCase("M")) {
                    return String.format("%04d", value);
                }
                if (unit.equals("KM")) {
                    return String.format("%02d", value) + "KM";
                }
                throw new IllegalArgumentException("Unknown unit found for visibility");
            }
        }

        TAFVisibility visibility;

        @Getter
        @Setter
        public static class TAFWind {
            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + ((direction == null) ? 0 : direction.hashCode());
                result = prime * result + ((gusts == null) ? 0 : gusts.hashCode());
                result = prime * result + ((gustsOperator == null) ? 0 : gustsOperator.hashCode());
                result = prime * result + ((speed == null) ? 0 : speed.hashCode());
                result = prime * result + ((speedOperator == null) ? 0 : speedOperator.hashCode());
                result = prime * result + ((unit == null) ? 0 : unit.hashCode());
                return result;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj)
                    return true;
                if (obj == null)
                    return false;
                if (getClass() != obj.getClass())
                    return false;
                TAFWind other = (TAFWind) obj;
                if (direction == null) {
                    if (other.direction != null)
                        return false;
                } else if (!direction.equals(other.direction))
                    return false;
                if (gusts == null) {
                    if (other.gusts != null)
                        return false;
                } else if (!gusts.equals(other.gusts))
                    return false;
                if (gustsOperator != other.gustsOperator)
                    return false;
                if (speed == null) {
                    if (other.speed != null)
                        return false;
                } else if (!speed.equals(other.speed))
                    return false;
                if (speedOperator != other.speedOperator)
                    return false;
                if (unit == null) {
                    if (other.unit != null)
                        return false;
                } else if (!unit.equals(other.unit))
                    return false;
                return true;
            }

            Object direction;
            Integer speed;
            Integer gusts;
            String unit;
            TAFWindSpeedOperator speedOperator;
            TAFWindSpeedOperator gustsOperator;

            public String toTAC() {
                StringBuilder sb = new StringBuilder();
                if (direction.toString().equals("VRB")) {
                    sb.append("VRB");
                } else {
                    sb.append(String.format("%03.0f", Double.parseDouble(direction.toString())));
                }
                if (speedOperator != null) {
                    if (speedOperator.equals(TAFWindSpeedOperator.above)) {
                        sb.append("P");
                    }
                    if (speedOperator.equals(TAFWindSpeedOperator.below)) {
                        sb.append("M"); // TODO: Is this possible?
                    }
                }
                sb.append(String.format("%02d", speed));
                if (gusts != null) {
                    sb.append(String.format("G"));
                    if (gustsOperator != null) {
                        if (gustsOperator.equals(TAFWindSpeedOperator.above)) {
                            sb.append("P");
                        }
                        if (gustsOperator.equals(TAFWindSpeedOperator.below)) {
                            sb.append("M"); // TODO: Is this possible?
                        }
                    }
                    sb.append(String.format("%02d", gusts));
                }
                sb.append(unit.toString());
                return sb.toString();
            }
        }

        TAFWind wind;

        @Getter
        @Setter
        public class TAFTemperature {
            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + getOuterType().hashCode();
                result = prime * result + ((maxTime == null) ? 0 : maxTime.hashCode());
                result = prime * result + ((maximum == null) ? 0 : maximum.hashCode());
                result = prime * result + ((minTime == null) ? 0 : minTime.hashCode());
                result = prime * result + ((minimum == null) ? 0 : minimum.hashCode());
                return result;
            }
            @Override
            public boolean equals(Object obj) {
                if (this == obj)
                    return true;
                if (obj == null)
                    return false;
                if (getClass() != obj.getClass())
                    return false;
                TAFTemperature other = (TAFTemperature) obj;
                if (!getOuterType().equals(other.getOuterType()))
                    return false;
                if (maxTime == null) {
                    if (other.maxTime != null)
                        return false;
                } else if (!maxTime.equals(other.maxTime))
                    return false;
                if (maximum == null) {
                    if (other.maximum != null)
                        return false;
                } else if (!maximum.equals(other.maximum))
                    return false;
                if (minTime == null) {
                    if (other.minTime != null)
                        return false;
                } else if (!minTime.equals(other.minTime))
                    return false;
                if (minimum == null) {
                    if (other.minimum != null)
                        return false;
                } else if (!minimum.equals(other.minimum))
                    return false;
                return true;
            }
            Float maximum;
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            @JsonInclude(JsonInclude.Include.NON_NULL)
            OffsetDateTime maxTime;
            Float minimum;
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            @JsonInclude(JsonInclude.Include.NON_NULL)
            OffsetDateTime minTime;
            private Forecast getOuterType() {
                return Forecast.this;
            }
        }

        TAFTemperature temperature;

        Boolean CaVOK;

        /**
         * Converts Forecast to TAC
         *
         * @return String with TAC representation of Forecast
         */
        public String toTAC() {
            StringBuilder sb = new StringBuilder();
            if (getWind() != null) {
                sb.append(" "+getWind().toTAC());
            }
            if (CaVOK != null && CaVOK == true) {
                sb.append(" CAVOK");
            } else {
                if (visibility != null && visibility.value != null) {
                    sb.append(" "+visibility.toTAC());
                }
                if (getWeather() != null) {
                    for (TAFWeather w : getWeather()) {
                        sb.append(" " + w);
                    }
                }

                if (getVertical_visibility() != null) {
                    sb.append(String.format(" VV%03d", getVertical_visibility()));
                }

                if (getClouds() != null) {
                    for (TAFCloudType tp : getClouds()) {
                        sb.append(" ");
                        sb.append(tp.toTAC());
                    }
                }
            }
            return sb.toString();
        }
    }

    Forecast forecast;

    @Getter
    @Setter
    public static class ChangeForecast {
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((changeEnd == null) ? 0 : changeEnd.hashCode());
            result = prime * result + ((changeStart == null) ? 0 : changeStart.hashCode());
            result = prime * result + ((changeType == null) ? 0 : changeType.hashCode());
            result = prime * result + ((forecast == null) ? 0 : forecast.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ChangeForecast other = (ChangeForecast) obj;
            if (changeEnd == null) {
                if (other.changeEnd != null)
                    return false;
            } else if (!changeEnd.equals(other.changeEnd))
                return false;
            if (changeStart == null) {
                if (other.changeStart != null)
                    return false;
            } else if (!changeStart.equals(other.changeStart))
                return false;
            if (changeType == null) {
                if (other.changeType != null)
                    return false;
            } else if (!changeType.equals(other.changeType))
                return false;
            if (forecast == null) {
                if (other.forecast != null)
                    return false;
            } else if (!forecast.equals(other.forecast))
                return false;
            return true;
        }

        String changeType;
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        OffsetDateTime changeStart;
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        OffsetDateTime changeEnd;
        Forecast forecast;

        public String toTAC() {
            StringBuilder sb = new StringBuilder();
            if (changeType.equals("FM")) {
                sb.append("FM"+TAFtoTACMaps.toDDHHMM(changeStart));
            }else {
                sb.append(changeType);
                sb.append(" " + TAFtoTACMaps.toDDHH(changeStart));
                if (changeEnd!=null) {
                    sb.append("/" + TAFtoTACMaps.toDDHH24(changeEnd));
                }
            }
            if (forecast!=null)sb.append(forecast.toTAC());
            return sb.toString();
        }
    }

    List<ChangeForecast> changegroups;

    public String toJSON(ObjectMapper om) throws JsonProcessingException {
        return om.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    }

    public static Taf fromJSONString(String tafJson, ObjectMapper om) throws JsonParseException, JsonMappingException, IOException{
        Taf taf = om.readValue(tafJson, Taf.class);
        return taf;
    }

    public static Taf fromFile(File f, ObjectMapper om) throws JsonParseException, JsonMappingException, IOException {
        return fromJSONString(Tools.readFile(f.getAbsolutePath()), om);
    }


    public String toIWWXM(TafConverter tafConverter) {
        return tafConverter.ToIWXXM_2_1(this);
    }

    public String toTAC() {
        StringBuilder sb = new StringBuilder();
        sb.append("TAF ");
        if (this.metadata.type !=null) switch (this.metadata.type) {
            case amendment:
            case canceled: //cancellation is also an AMD
                sb.append("AMD ");
                break;
            case correction:
                sb.append("COR ");
                break;
            case retarded:
                sb.append("RTD ");
                break;
            default:
                // Append nothing here
                break;
        }

        sb.append(this.metadata.location);

        /* Add issuetime */
        if (this.metadata.issueTime != null) {
            sb.append(" " + TAFtoTACMaps.toDDHHMM_Z(this.metadata.issueTime));
        } else{
            sb.append(" " + "<not yet issued>");
        }

        if (this.metadata.type !=null) switch (this.metadata.type) {
            case missing:
                // If missing, we're done here
                sb.append(" NIL");
                return sb.toString();
            default:
                // do nothing
                break;
        }

        if (this.metadata.type !=null) switch (this.metadata.type) {
            case canceled:
                /* Add date */
                sb.append(" " + TAFtoTACMaps.toDDHH(this.metadata.validityStart) + "/" + TAFtoTACMaps.toDDHH24(this.metadata.validityEnd));
                // In case of a cancel there are no change groups so we're done here
                sb.append(" CNL");
                return sb.toString();
            default:
                /* Add date */
                sb.append(" " + TAFtoTACMaps.toDDHH(this.metadata.validityStart) + "/"
                        + TAFtoTACMaps.toDDHH24(this.metadata.validityEnd));
                // do nothing
                break;
        }
        // Add the rest of the TAC
        if (this.forecast!=null) {
            sb.append(this.forecast.toTAC());
        }
        if (this.changegroups != null) {
            for (ChangeForecast ch : this.changegroups) {
                sb.append("\n" + ch.toTAC());
            }
        }

        return sb.toString();
    }

    // Same as TAC, but maximum line with 69 chars where words (e.g. "BKN040") are not splitted
    // Also has a header and footer to the message
    private String getPublishableTAC(ProductConverter<Taf> converter) {
        String line = "";
        String publishTAC = "";
        String[] TACwords = this.toTAC().split("\\s+");
        for(int i = 0; i < TACwords.length; ++i) {
            if (line.length() + TACwords[i].length() + 1 <= 69) {
                if (line.length() > 0) line += " ";
                line += TACwords[i];
            } else {
                publishTAC += line + '\n';
                line = TACwords[i];
            }
        }
        publishTAC += line;
        OffsetDateTime minusOne;
        if (this.metadata.baseTime != null) {
            log.debug("baseTime not null");
            minusOne = this.metadata.baseTime.minusHours(1);
        } else {
            minusOne = this.metadata.validityStart.minusHours(1);
        }
        String time = TAFtoTACMaps.toDDHH(minusOne) + "00";
        String status = "";
        switch (this.metadata.type) {
            case amendment:
                status = " AMD";
                break;
            case correction:
                status = " COR";
                break;
            case retarded:
                status = " RTD";
                break;
            case missing:
                status = " NIL";
                break;
            case canceled:
                status = " CNL";
                break;
            default:
                // Append nothing here
                break;

        }
       
        String header = "FTNL99 " + converter.getLocationIndicatorWMO() + " " + time + status +'\n';
        String footer = "=";
        return header + publishTAC + footer;
    }

    @Override
    public String export(File path, ProductConverter<Taf> converter, ObjectMapper om) {
        //TODO Make LTNL99 configurable
        String time = OffsetDateTime.now(ZoneId.of("Z")).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        OffsetDateTime reportTime=this.metadata.getBaseTime();
        if (reportTime==null) {
            log.warn("Missing baseTime, using validityStart");
            reportTime=this.metadata.getValidityStart();
        }
        String validTime = reportTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmm"));
        String name = "TAF_" + this.metadata.getLocation() + "_" + validTime + "_" + time;

        String action = (this.metadata.type.toString() == "normal") ? "publish": this.metadata.type.toString();
        ProductTraceability.TraceProduct(action,"TAF", this.metadata.getUuid(), this.metadata.getLocation(), validTime, name);
        
        try {
            Tools.writeFile(path.getPath() + "/" + name + ".tac", this.getPublishableTAC(converter));
        } catch (Exception e) {
            return "creation of TAC failed";
        }
        try {
            Tools.writeFile(path.getPath() + "/" + name + ".json", this.toJSON(om));
        } catch(Exception e) {
            return "saving of JSON failed";
        }
        try {
            String iwxxmName="A_"+"LTNL99"+this.metadata.getLocation()+reportTime.format(DateTimeFormatter.ofPattern("ddHHmm"));
            switch (this.metadata.type) {
                case amendment:
                    iwxxmName+="AMD";
                    break;
                case correction:
                    iwxxmName+="COR";
                    break;
                case canceled:
                    iwxxmName+="CNL";
                    break;
                default:
                    break;
            }

            iwxxmName+="_C_"+this.metadata.getLocation()+"_"+time;
            Tools.writeFile(path.getPath() + "/" + iwxxmName + ".xml", converter.ToIWXXM_2_1(this));
        } catch (Exception e) {
            log.error("Creation of IWXXM failed: " + e.getMessage());
            return "creation of IWXXM failed";
        }
        return "OK";
    }
}
