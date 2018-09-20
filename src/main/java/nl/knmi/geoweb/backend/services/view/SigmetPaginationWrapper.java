package nl.knmi.geoweb.backend.services.view;

import com.fasterxml.jackson.annotation.JsonProperty;

import nl.knmi.geoweb.backend.product.sigmet.Sigmet;

public class SigmetPaginationWrapper extends AbstractPaginationWrapper<Sigmet> {
	private int count;

	public SigmetPaginationWrapper(Sigmet sigmets[], Integer page, Integer cnt) {
		super(sigmets, page, cnt);
		this.count = cnt == null ? 0 : cnt;
	}

	@JsonProperty
	public Sigmet[] getSigmets() {
		return ws;
	}

	@JsonProperty
	public int getNsigmets() {
		return nws;
	}

	public int getCount() {
		return count;
	}
}
