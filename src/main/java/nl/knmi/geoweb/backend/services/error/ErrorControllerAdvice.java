package nl.knmi.geoweb.backend.services.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
@RestController
public class ErrorControllerAdvice {
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(GeoWebServiceException.class)
	public final ErrorMessage handleGeoWebServiceException(GeoWebServiceException ex, WebRequest request) {
		return new ErrorMessage(ex.getMessage());
	}

	@ResponseStatus(HttpStatus.NOT_FOUND)
	@ExceptionHandler(EntityDoesNotExistException.class)
	public final ErrorMessage handleEntityDoesNotExistException(EntityDoesNotExistException ex, WebRequest request) {
		return new ErrorMessage(ex.getMessage());
	}

	@ResponseStatus(HttpStatus.CONFLICT)
	@ExceptionHandler(EntityNotInConceptException.class)
	public final ErrorMessage handleEntityNotInConceptException(EntityNotInConceptException ex, WebRequest request) {
		return new ErrorMessage(ex.getMessage());
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(GeoJsonConversionException.class)
	public final ErrorMessage handleGeoJsonConversionException(GeoJsonConversionException ex, WebRequest request) {
		return new ErrorMessage(ex.getMessage());
	}
}
