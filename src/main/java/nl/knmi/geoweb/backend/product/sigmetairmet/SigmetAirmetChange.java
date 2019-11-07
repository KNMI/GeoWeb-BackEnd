package nl.knmi.geoweb.backend.product.sigmetairmet;

import java.util.Arrays;

import lombok.Getter;

@Getter
public enum SigmetAirmetChange {
    INTSF("Intensifying"), WKN("Weakening"), NC("No change");
    private String description;
    private SigmetAirmetChange(String desc) {
        this.description=desc;
    }
    public String toTAC() {
        return Arrays.stream(values())
                .filter(sc -> sc.description.equalsIgnoreCase(this.description))
                .findFirst()
                .orElse(null).toString();
    }
}
