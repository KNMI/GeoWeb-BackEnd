package nl.knmi.geoweb.backend.product.sigmetairmet;

import lombok.Getter;

@Getter
public enum SigmetAirmetType {
    normal("normal"), test("test"), exercise("exercise");
    private String type;
    private SigmetAirmetType (String type) {
        this.type = type;
    }
    public static SigmetAirmetType getSigmetType(String itype){
        for (SigmetAirmetType stype: SigmetAirmetType.values()) {
            if (itype.equals(stype.toString())){
                return stype;
            }
        }
        return null;
    }

}

