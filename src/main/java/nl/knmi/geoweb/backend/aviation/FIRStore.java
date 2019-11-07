package nl.knmi.geoweb.backend.aviation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.Tools;
import nl.knmi.geoweb.backend.product.sigmet.geo.GeoUtils;

@Getter
@Setter
@Component
public class FIRStore implements Cloneable {
    private String worldFIRFile;
    private String delegatedFile;
    private String simplifiedFIRFile;
    private String directory;
    private Map<String, Feature> worldFIRInfos;
    private Map<String, Feature> simplifiedFIRInfos;
    private Map<String, List<Feature>> delegatedAirspaces;

    public FIRStore(@Value(value = "${geoweb.products.storeLocation}") final String productstorelocation) {
        this.directory = productstorelocation + "/admin/config";
        try {
            Tools.mksubdirs(productstorelocation + "/admin/config");
        } catch (final IOException e) {
            Debug.errprintln("Creation of " + productstorelocation + "/admin/config" + " failed");
        }
        this.worldFIRFile = "world_firs.json";
        this.delegatedFile = "delegated.json";
        this.simplifiedFIRFile = "simplified_firs.json";
    }

    public void initStore() throws IOException {
        this.worldFIRInfos = new HashMap<String, Feature>();
        this.delegatedAirspaces = new HashMap<String, List<Feature>>();
        this.simplifiedFIRInfos = new HashMap<String, Feature>();
        final File fn = new File(this.directory + "/" + this.worldFIRFile);
        if (!(fn.exists() && fn.isFile())) {
            Debug.errprintln("No FIR file found, copying one from resources dir to " + this.directory);
            final String s = Tools.readResource(this.worldFIRFile);
            final String FIRText = String.format("%s/%s", this.directory, this.worldFIRFile);
            Tools.writeFile(FIRText, s);
        }
        final File simplifiedFn = new File(this.directory + "/" + this.simplifiedFIRFile);
        if (!(simplifiedFn.exists() && simplifiedFn.isFile())) {
            Debug.errprintln("No simplified FIR file found, copying one from resources dir to " + this.directory);
            final String s = Tools.readResource(this.simplifiedFIRFile);
            final String FIRText = String.format("%s/%s", this.directory, this.simplifiedFIRFile);
            Tools.writeFile(FIRText, s);
        }
        final File delegatedFn = new File(this.directory + "/" + this.delegatedFile);
        if (!(delegatedFn.exists() && delegatedFn.isFile())) {
            Debug.errprintln("No delegated areas FIR file found, copying one from resources dir to " + this.directory);
            // TODO: since the lists of coordinates for delegated area (EHAA) doesn't align
            // with the FIR boundary,
            // we moved a coordinate to make them intersect:
            // [3.16004722222222,52.9310027777778] -> [3.163, 52.92]
            final String s = Tools.readResource(this.delegatedFile);
            final String FIRText = String.format("%s/%s", this.directory, this.delegatedFile);
            Tools.writeFile(FIRText, s);
        }

        final ObjectMapper om = new ObjectMapper();

        try {
            final GeoJsonObject FIRInfo = om.readValue(fn, GeoJsonObject.class);
            final FeatureCollection fc = (FeatureCollection) FIRInfo;
            for (final Feature f : fc.getFeatures()) {
                final String FIRname = f.getProperty("FIRname");
                final String ICAOCode = f.getProperty("ICAOCODE");
                worldFIRInfos.put(FIRname, f);
                worldFIRInfos.put(ICAOCode, f);
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
        Debug.errprintln("Found " + worldFIRInfos.size() + " records of FIRinfo");

        try {
            final GeoJsonObject simplifiedFIRInfo = om.readValue(simplifiedFn, GeoJsonObject.class);
            final FeatureCollection simplifiedFc = (FeatureCollection) simplifiedFIRInfo;
            for (final Feature f : simplifiedFc.getFeatures()) {
                final String FIRname = f.getProperty("FIRname");
                final String ICAOCode = f.getProperty("ICAOCODE");
                simplifiedFIRInfos.put(FIRname, f);
                simplifiedFIRInfos.put(ICAOCode, f);
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
        Debug.errprintln("Found " + simplifiedFIRInfos.size() + " records of simplified FIRinfo");

        try {
            final GeoJsonObject DelegatedInfo = om.readValue(delegatedFn, GeoJsonObject.class);
            final FeatureCollection fc = (FeatureCollection) DelegatedInfo;
            for (final Feature f : fc.getFeatures()) {
                final String FIRname = f.getProperty("FIRname");
                final String ICAOCode = f.getProperty("ICAONAME");
                if (!delegatedAirspaces.containsKey(FIRname)) {
                    final List<Feature> delegated = new ArrayList<Feature>();
                    delegatedAirspaces.put(FIRname, delegated);
                    delegatedAirspaces.put(ICAOCode, delegated);
                }
                delegatedAirspaces.get(FIRname).add(f);
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public static Feature cloneThroughSerialize(final Feature t) {
        try {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            serializeToOutputStream(t, bos);
            final byte[] bytes = bos.toByteArray();
            final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            return (Feature) ois.readObject();
        } catch (final Exception e) {
            return null;
        }
    }

    private static void serializeToOutputStream(final Serializable ser, final OutputStream os) throws IOException {
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(os);
            oos.writeObject(ser);
            oos.flush();
        } finally {
            oos.close();
        }
    }

    public Feature lookup(final String name, final boolean addDelegated) {
        if (worldFIRInfos == null) {
            try {
                initStore();
            } catch (final IOException e) {
                return null;
            }
        }

        Feature feature = null;
        if (simplifiedFIRInfos.containsKey(name)) {
            feature = cloneThroughSerialize(simplifiedFIRInfos.get(name));
        } else if (worldFIRInfos.containsKey(name)) {
            feature = cloneThroughSerialize(worldFIRInfos.get(name));
        }

        if (addDelegated) {
            if (delegatedAirspaces.containsKey(name)) {
                for (final Feature f : delegatedAirspaces.get(name)) {
                    // Merge f with feature
                    // Debug.println("Adding delegated area for "+name);
                    feature = GeoUtils.merge(feature, f);
                }
            }
        }

        return feature;
    }

}
