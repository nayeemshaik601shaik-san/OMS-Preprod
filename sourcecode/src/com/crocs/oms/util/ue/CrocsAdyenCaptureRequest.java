package com.crocs.oms.util.ue;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.io.IOException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCException;
import com.yantra.yfs.core.YFSSystem;

/**
 * class used for the hit Adyen and get the response sample Input to the java
 * code <Request AdyenRequestPayload=
 * "{body:{reference:797907097097,amount:{currency:USD,value:10023},merchantAccount:CrocsUS},pspReference:79070970707}"/>
 */

public class CrocsAdyenCaptureRequest implements CrocsConstant {
	private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsAdyenCaptureRequest.class);

	public String crocsAdyenCaptureRequest(Document inDoc)   {

		logger.beginTimer("CrocsAdyenCaptureRequest.crocsAdyenCaptureRequest: Begin");
		logger.verbose(
				"CrocsAdyenCaptureRequest.crocsAdyenCaptureRequest:Input Document: " + XMLUtil.getXMLString(inDoc));

		String strJson = inDoc.getDocumentElement().getAttribute(CrocsConstant.STR_ADEYN_REQUEST_PAYLOAD);
	      

		HttpURLConnection conn = null;
		String response = "";

		try {
			
			
			JSONObject jsonObject = new JSONObject(strJson);

			String strRequest = inDoc.getDocumentElement().getAttribute(CrocsConstant.STR_ADYEN_REQUEST);

			
			// SMA Property for ADYEN Capture URL
			String apiUrl = YFSSystem.getProperty(CrocsConstant.A_ADYEN_PAYMENT_CAPTURE_URL);
			
			//EOMS-4253 : ADYEN X-APi-key null check changes : START
			StringBuilder keyBuilder = new StringBuilder();

			String[] apiKeys = {
			    YFSSystem.getProperty(CrocsConstant.A_ADYEN_X_API_KEY1),
			    YFSSystem.getProperty(CrocsConstant.A_ADYEN_X_API_KEY2),
			    YFSSystem.getProperty(CrocsConstant.A_ADYEN_X_API_KEY3),
			    YFSSystem.getProperty(CrocsConstant.A_ADYEN_X_API_KEY4)
			};

			for (String apiKey : apiKeys) {
			    if (!YFCCommon.isVoid(apiKey) && apiKey != null && !apiKey.isEmpty()) {
			        keyBuilder.append(apiKey);
			    }
			}

			String key = keyBuilder.toString();

			/**EOMS - 9099 CROCS_AU using new API-key to connect to Adyen
			 * If MerchantAccount = "CrocsAU", fetch the key from CAU_ADYEN_X-API-KEY5 (configured for CROCS_AU).**/
			/**
			 "AdyenRequest": {
			 "Request": "CAPTURE",
			 "AdyenRequestPayload": {
			 "body": {
			 "reference": "Y100011101",
			 "amount": {
			 "currency": "AUD",
			 "value": 4994
			 },
			 "merchantAccount": "CrocsAU"
			 },
			 "pspReference": "J3CDGN26RNVR5569"
			 }
			 }**/
			JSONObject body = jsonObject.getJSONObject(A_CAU_ADYEN_CAPTURE_REQUEST_BODY);
			String strMerchantAccount = body.get(CrocsConstant.ADYEN_MERCHANT_ACCOUNT).toString();
			if (A_ADYEN_MERCHANT_ACCOUNT_CAU.equalsIgnoreCase(strMerchantAccount) 
					|| A_ADYEN_MERCHANT_ACCOUNT_CSG.equalsIgnoreCase(strMerchantAccount) || A_ADYEN_MERCHANT_ACCOUNT_CKR.equalsIgnoreCase(strMerchantAccount)) {
				key = YFSSystem.getProperty(CrocsConstant.A_CAU_ADYEN_X_API_KEY5);
				logger.info("Adyen Capture Request is sent for merchantAccount CrocsAU");
			}

			if (strMerchantAccount != null && CROCS_EMEA_ADYEN.contains(strMerchantAccount)){
				key = YFSSystem.getProperty(CrocsConstant.A_EMEA_ADYEN_X_API_KEY6);
				logger.info("Adyen Capture Request is sent for EMEA merchantAccount: " + strMerchantAccount);
			}
			
			//EOMS-4253 : ADYEN X-APi-key null check changes : END
			
			String pspReference = jsonObject.get(CrocsConstant.ADYEN_PSP_REFERENCE).toString();
			String captureRequest = jsonObject.get(CrocsConstant.STR_BODY).toString();
			String requestUrl = "";
			if (CrocsXmlConstants.A_EVENT_CODE_CAPTURE.equalsIgnoreCase(strRequest))
				requestUrl = apiUrl + pspReference + CrocsConstant.STR_URL_CAPTURE;
			else if (CrocsXmlConstants.A_EVENT_CODE_REFUND.equalsIgnoreCase(strRequest))
				requestUrl = apiUrl + pspReference + CrocsConstant.STR_URL_REFUND;
			else if (CrocsXmlConstants.A_EVENT_CODE_CANCEL.equalsIgnoreCase(strRequest))
				requestUrl = apiUrl + pspReference + CrocsConstant.STR_VOID_CANCELS;

			logger.verbose("Request Payload: " + captureRequest);
			
			

			URL url = new URL(requestUrl);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod(CrocsConstant.HTTP_POST_REQUEST);
			conn.setRequestProperty(CrocsConstant.CONTENT_TYPE, CrocsConstant.APPLICATION_JSON);
			conn.setRequestProperty(CrocsConstant.A_X_API_KEY, key);
			conn.setDoOutput(true);

			// Send request
			try (OutputStream os = conn.getOutputStream()) {
				os.write(captureRequest.getBytes(StandardCharsets.UTF_8));
			}

			logger.verbose("Response Code: " + conn.getResponseCode());

			// Read response
			response = readResponse(conn);
			logger.verbose("Response: " + response);


		} catch (Exception | NoClassDefFoundError e) {
			logger.error("Error in crocsAdyenCaptureRequest: " + e.getMessage(), e);
			throw new YFCException("Error in crocsAdyenCaptureRequest: " + e.getMessage());
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
		logger.verbose("CrocsAdyenCaptureRequest.crocsAdyenCaptureRequest:Output Document: " + response);
		logger.endTimer("CrocsAdyenCaptureRequest.crocsAdyenCaptureRequest: End");

		return response;
	}

	private String readResponse(HttpURLConnection conn) throws IOException  {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(
				conn.getResponseCode() == 201 && conn.getResponseCode() < 300 ? conn.getInputStream()
						: conn.getErrorStream(),
				StandardCharsets.UTF_8))) {
			return br.lines().collect(Collectors.joining("\n"));
		}
	}
}