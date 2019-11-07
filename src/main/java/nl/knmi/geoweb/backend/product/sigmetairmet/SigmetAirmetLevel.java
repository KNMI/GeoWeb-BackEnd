package nl.knmi.geoweb.backend.product.sigmetairmet;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public  class SigmetAirmetLevel {
    SigmetAirmetPart[] levels;
    SigmetAirmetLevelMode mode;

    public SigmetAirmetLevel() {
    }

    ;

    public SigmetAirmetLevel(SigmetAirmetPart lev1, SigmetAirmetLevelMode mode) {
        this.levels = new SigmetAirmetPart[1];
        this.levels[0] = lev1;
        this.mode = mode;
    }

    public SigmetAirmetLevel(SigmetAirmetPart lev1, SigmetAirmetPart lev2, SigmetAirmetLevelMode mode) {
        levels = new SigmetAirmetPart[2];
        this.levels[0] = lev1;
        this.levels[1] = lev2;
        this.mode = mode;
    }

    public String toTAC() {
        switch (this.mode) {
            case BETW:
                if ((this.levels[0] != null) && (this.levels[1] != null)) {
                    if (this.levels[0].getUnit().equals(this.levels[1].getUnit())) {
                        switch (this.levels[0].getUnit()) {
                            case FL:
                                return this.levels[0].toTAC() + "/" + this.levels[1].toTACValue();
                            case FT:
                                return this.levels[0].toTACValue() + "/" + this.levels[1].toTAC();
                            case M:
                                return this.levels[0].toTACValue() + "/" + this.levels[1].toTAC();
                        }
                        return "";
                    } else {
                        return this.levels[0].toTAC() + "/" + this.levels[1].toTAC();
                    }
                }
                break;
            case BETW_SFC:
                if (this.levels[1] != null) {
                    return "SFC/" + this.levels[1].toTAC();
                }
                break;
            case ABV:
                if (this.levels[0] != null) {
                    return "ABV " + this.levels[0].toTAC();
                }
                break;
            case AT:
                if (this.levels[0] != null) {
                    return "" + this.levels[0].toTAC();
                }
                break;
            case TOPS:
                if (this.levels[0] != null) {
                    return "TOP " + this.levels[0].toTAC();
                }
                break;
            case TOPS_ABV:
                if (this.levels[0] != null) {
                    return "TOP ABV " + this.levels[0].toTAC();
                }
                break;
            case TOPS_BLW:
                if (this.levels[0] != null) {
                    return "TOP BLW " + this.levels[0].toTAC();
                }
                break;
            default:
        }
        return "";
    }

    public enum SigmetAirmetLevelUnit {
        FT, FL, M;
    }

    public enum SigmetAirmetLevelMode {
        AT, ABV, BETW, BETW_SFC, TOPS, TOPS_ABV, TOPS_BLW;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    public static class SigmetAirmetPart {
        Integer value;
        SigmetAirmetLevelUnit unit;

        public SigmetAirmetPart() {
        }

        ;

        public SigmetAirmetPart(SigmetAirmetLevelUnit unit, int val) {
            this.unit = unit;
            this.value = val;
        }

        public String toTACValue() {
            if (value == null) {
                return "";
            }
            if (this.unit == SigmetAirmetLevelUnit.FL) {
                return String.format("%03d", value);
            }
            if (this.unit == SigmetAirmetLevelUnit.FT) {
                if (value > 9999) {
                    return String.format("%05d", value);
                } else {
                    return String.format("%04d", value);
                }
            }
            if (this.unit == SigmetAirmetLevelUnit.M) {
                if (value <= 9999) {
                    return String.format("%04d", value);
                }
            }
            return "";
        }

        public String toTAC() {
            if (value == null) {
                return "";
            }
            if (this.unit == SigmetAirmetLevelUnit.FL) {
                return "FL" + this.toTACValue();
            }
            if (this.unit == SigmetAirmetLevelUnit.FT) {
                return this.toTACValue() + "FT";
            }
            if (this.unit == SigmetAirmetLevelUnit.M) {
                return this.toTACValue() + "M";
            }
            return "";
        }
    }
}