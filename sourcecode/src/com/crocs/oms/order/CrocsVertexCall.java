package com.crocs.oms.order;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;

public class CrocsVertexCall {

	private static final String SOAP_ENDPOINT_URL = "https://crocs.na1.ondemand.vertexinc.com:443/vertex-ws/services/CalculateTax90";
	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsVertexCall.class);

	public Document getResponseFromVertex(Document inDoc) {
		System.out.println("CrocsVertexCall : Input document " + XMLUtil.getXMLString(inDoc));
		logger.verbose("CrocsVertexCall : Input document " + XMLUtil.getXMLString(inDoc));
		Document outputdoc = null;
		try {
			Element eleRoot = inDoc.getDocumentElement();
			String soapRequestXml = XMLUtil.getElementXMLString(eleRoot);
			System.out.println("CrocsVertexCall : soapRequestXml " + soapRequestXml);
			logger.verbose("CrocsVertexCall : soapRequestXml " + soapRequestXml);
			// Send the SOAP request and get the response
			String response = sendSoapRequest(soapRequestXml);

			// Print response
			if (response != null && !response.isEmpty()) {
				System.out.println("CrocsVertexCall : response " + response);
				logger.verbose("CrocsVertexCall : response " + response);
				outputdoc = SCXmlUtil.createFromString(response);

			} else {
				System.out.println("CrocsVertexCall : No response received ");
				logger.verbose("CrocsVertexCall : No response received ");
			}

		} catch (Exception e) {
			System.out.println("Error in CrocsVertexCall: " + e.getMessage());
			logger.error("Error in CrocsVertexCall: " + e.getMessage(), e);
		}
		return outputdoc;
	}

	private static String sendSoapRequest(String soapRequestXml) throws IOException {
		URL url = new URL(SOAP_ENDPOINT_URL);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		// Set up HTTP connection
		connection.setDoOutput(true);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");

		// Send the request
		try (OutputStream os = connection.getOutputStream()) {
			byte[] input = soapRequestXml.getBytes(StandardCharsets.UTF_8);
			os.write(input, 0, input.length);
		}

		//logger.verbose("CrocsVertexCall : SoapResponseCode" + connection.getResponseCode());

		// Get the response
		try (BufferedReader in = new BufferedReader(
				new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
			return IOUtils.toString(in);
		} catch (IOException e) {
			// Log error response from the server if available
			System.out.println("Error response from Vertex service: " + connection.getResponseCode() + " - " + connection.getResponseMessage());
            logger.error("Error response from Vertex service: " + connection.getResponseCode() + " - " + connection.getResponseMessage());
			try (BufferedReader errorIn = new BufferedReader(
					new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
				return IOUtils.toString(errorIn);
			}catch (IOException ex) {
				System.out.println("Failed to read error stream from Vertex service: " + ex.getMessage());
                logger.error("Failed to read error stream from Vertex service: " + ex.getMessage(), ex);
                throw ex;  // Re-throw exception if unable to read error stream
            }
		}
	}

}
