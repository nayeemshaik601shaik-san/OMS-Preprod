package com.crocs.oms.order;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCException;
import com.yantra.yfs.core.YFSSystem;

/**
 * EOMS - 809 Tax Call Implementation 
 * This class is used to call the Vertex End point
 */ 

public class CrocsVertexUtil {
	
	private static final String SOAP_ENDPOINT_URL = YFSSystem.getProperty(CrocsConstant.A_VERTEX_URL);

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsVertexUtil.class);

	public  Document sendSoapRequest(Document docSoapRequest)   {

		Element eleRoot = docSoapRequest.getDocumentElement();
		String soapRequestXml = XMLUtil.getElementXMLString(eleRoot);

		if (soapRequestXml == null || soapRequestXml.isEmpty()) {
			throw new IllegalArgumentException("SOAP request XML cannot be null or empty");
		}

		HttpURLConnection connection = null;
		try {
			URL url = new URL(SOAP_ENDPOINT_URL);
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod(CrocsConstant.HTTP_POST_REQUEST); 
			connection.setRequestProperty(CrocsConstant.CONTENT_TYPE, "text/xml; charset=UTF-8"); 
			connection.setConnectTimeout(10000);
			connection.setReadTimeout(10000);

			try (OutputStream os = connection.getOutputStream()) {
				os.write(soapRequestXml.getBytes(StandardCharsets.UTF_8));
			}

			int responseCode = connection.getResponseCode();
			String responseMessage = connection.getResponseMessage();

			InputStream responseStream = (responseCode >= 200 && responseCode < 300) ? connection.getInputStream()
					: connection.getErrorStream();

			if (responseStream == null) {
				throw new IOException("Received error response but no error stream available.");
			}

			String responseXml = IOUtils.toString(responseStream, StandardCharsets.UTF_8);
			if (responseCode >= 200) {
				logger.verbose("CrocsVertexUtil: Response Received " + SCXmlUtil.createFromString(responseXml));
				return SCXmlUtil.createFromString(responseXml);

			} else {
				logger.error("Error response from Vertex service: {} - {}", responseCode, responseMessage);
				throw new IOException("Received error response: " + responseXml);
			}
		} catch (Exception e) {

			logger.error("CrocsVertexUtil :Error :" + e.getMessage());

			throw new YFCException("CrocsVertexUtil :Error : " + e.getMessage());
		}

		finally {
			if (connection != null) {
				connection.disconnect();
			}
		}

	}
}