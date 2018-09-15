package nl.knmi.geoweb.backend.services.view;

import com.fasterxml.jackson.annotation.JsonProperty;

import nl.knmi.geoweb.backend.product.taf.Taf;

public class TafPaginationWrapper extends AbstractPaginationWrapper<Taf> {
	public TafPaginationWrapper(Taf tafs[], Integer page, Integer cnt) {
		super(tafs, page, cnt);
	}

	@JsonProperty
	public Taf[] getTafs() {
		return ws;
	}

	@JsonProperty
	public int getNtafs() {
		return nws;
	}
}
