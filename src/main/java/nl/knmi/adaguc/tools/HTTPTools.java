package nl.knmi.adaguc.tools;

import java.net.URLDecoder;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

public class HTTPTools {
	public static class InvalidHTTPKeyValueTokensException extends Exception {
		private static final long serialVersionUID = 1L;
		String message = null;

		public InvalidHTTPKeyValueTokensException(String result) {
			this.message= result;
		}

		public String getMessage() {
			return message;
		}
	}

	static byte[] validTokens = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
			'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
			'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
			'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '-', '|', '&',
			'.', ',', '~', ' ','/',':','?','_','#','=' ,'(',')',';','%','[',']'};
	/**
	 * Validates input for valid tokens, preventing XSS attacks. Throws Exception when invalid tokens are encountered.
	 * @param input The string as input
	 * @return returns the same string
	 * @throws Exception when invalid tokens are encountered
	 */
	public static String validateInputTokens(String input) throws Exception {
		if(input == null)return null;

		byte[] str = input.getBytes();
		for (int c = 0; c < str.length; c++) {
			boolean found = false;
			for (int v = 0; v < validTokens.length; v++) {
				if (validTokens[v] == str[c]) {
					found = true;
					break;
				}
			}
			if (found == false) {

				String message = "Invalid token given: '"
						+ Character.toString((char) str[c]) + "', code (" + str[c] + ").";
				Debug.errprintln("Invalid string given: " + message + " in string "+input);
				throw new InvalidHTTPKeyValueTokensException(message);
			}
		}
		return input;
	}

	/**
	 * Returns the value of a key, but does checking on valid tokens for XSS attacks and decodes the URL.
	 * @param request The HTTPServlet containing the KVP's
	 * @param name Name of the key
	 * @return The value of the key
	 * @throws Exception (UnsupportedEncoding and InvalidHTTPKeyValueTokensException)
	 */
	public static String getHTTPParam(HttpServletRequest request, String name)
			throws Exception {
		//String param = request.getParameter(name);

		Map<String, String[]> paramMap= request.getParameterMap();
		String [] value = null;
		for (Entry<String, String[]> entry : paramMap.entrySet()){
			String key = entry.getKey();
			if(key.equalsIgnoreCase(name)){
				value = entry.getValue();
				break;
			}
		}

		if(value==null||value[0]==null||value.length==0){
			throw new Exception("UnableFindParam " + name);
		}

		String paramValue = value[0];
		paramValue = URLDecoder.decode(paramValue, "UTF-8");
		paramValue = validateInputTokens(paramValue);
		return paramValue;
	}
}
