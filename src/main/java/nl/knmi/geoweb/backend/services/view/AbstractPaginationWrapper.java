package nl.knmi.geoweb.backend.services.view;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class AbstractPaginationWrapper<W> {
	@JsonIgnore
	protected W[] ws;
	@JsonIgnore
	protected int nws;
	private int page;
	private int npages;

	protected AbstractPaginationWrapper(W ws[], Integer page, Integer cnt) {
		int nws = ws.length;
		int count = cnt == null ? 0 : cnt;
		if (page == null) {
			page = 0;
		}
		if (nws == 0) {
			this.npages = 1;
			this.nws = 0;
			this.ws = ws;
		} else {
			int first;
			int last;
			if (count != 0) {
				/* Select all Ws for requested page/count*/
				if (nws <= count) {
					first = 0;
					last = nws;
				} else {
					first = page * count;
					last = Math.min(first + count, nws);
				}
				this.npages = (nws / count) + ((nws % count) > 0 ? 1 : 0);
			} else {
				/* Select all sigmets when count or page are not set*/
				first = 0;
				last = nws;
				this.npages = 1;
			}
			if (first < nws && first >= 0 && last >= first && page < this.npages) {
				this.ws = Arrays.copyOfRange(ws, first, last);
			}
			this.page = page;
			this.nws = nws;
		}
	}

	public int getPage() {
		return page;
	}

	public int getNpages() {
		return npages;
	}
}
