package nl.knmi.geoweb.backend.product;

import fi.fmi.avi.converter.AviMessageConverter;

public interface ProductConverter<P> {
	public AviMessageConverter aviMessageConverter();
	public String ToIWXXM_2_1(P p);
	public String getLocationIndicatorWMO();

}
