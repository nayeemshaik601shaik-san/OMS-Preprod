package com.crocs.oms.util.ue;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;
import org.w3c.dom.Document;

import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.XMLUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.core.YFSSystem;
import com.yantra.yfs.japi.YFSException;

/**
 * class used for the hit Adyen and get the response
 * sample Input to the java code
 * <Request AdyenRequestPayload="{body:{reference:797907097097,amount:{currency:USD,value:10023},merchantAccount:CrocsUS},pspReference:79070970707}"/>
 */

public class CrocsAdyenCaptureRequestForTesting {
    private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsAdyenCaptureRequestForTesting.class);

    public String crocsAdyenCaptureRequest(Document inDoc) throws Exception {
    	
        logger.beginTimer("CrocsAdyenCaptureRequest.crocsAdyenCaptureRequest: Begin");
        logger.verbose("CrocsAdyenCaptureRequest.crocsAdyenCaptureRequest:Input Document: " + XMLUtil.getXMLString(inDoc));
       

        String strJson = inDoc.getDocumentElement().getAttribute(CrocsConstant.STR_ADEYN_REQUEST_PAYLOAD);
        String apiUrl = "https://checkout-test.adyen.com/v71/paymentss/";
        String key = "AQEqhmfxKIjHbBBKw0m/n3Q5qf3Va5lCDoBkeUiK2jyj5R5/c6t581IubFlpEMFdWw2+5HzctViMSCJMYAc=-BRT1pe07yBuxGYZAfWJeK7Uv+24VKmfOHvw19Ra70n4=-i1i7}_)w9}%mHqu>rHg";
        
        //SMA Properties
        HttpURLConnection conn = null;
        String response = "";
		try {
	//	  String apiUrl =	  YFSSystem.getProperty(CrocsConstant.A_ADYEN_PAYMENT_CAPTURE_URL); 
	//	  String  adyenApiKey1 = YFSSystem.getProperty(CrocsConstant.A_ADYEN_X_API_KEY1);
	//	  String adyenApiKey2 =  YFSSystem.getProperty(CrocsConstant.A_ADYEN_X_API_KEY2); 
	//	  String adyenApiKey3  = YFSSystem.getProperty(CrocsConstant.A_ADYEN_X_API_KEY3); 
	//	  String  adyenApiKey4 = YFSSystem.getProperty(CrocsConstant.A_ADYEN_X_API_KEY4);
	//	  String key = adyenApiKey1+adyenApiKey2+adyenApiKey3+adyenApiKey4;
        logger.verbose("key: " + key);
        
             
        JSONObject jsonObject = new JSONObject(strJson);
        
        
        String pspReference = jsonObject.get(CrocsConstant.ADYEN_PSP_REFERENCE).toString();
        String captureRequest =  jsonObject.get(CrocsConstant.STR_BODY).toString();
      
      
        String requestUrl = apiUrl + pspReference + CrocsConstant.STR_URL_CAPTURE;
        logger.verbose("Request URL: " + requestUrl);
        logger.verbose("Request Payload: " + captureRequest);
       
        
//        HttpURLConnection conn = null;
//		try {
			URL url = new URL(requestUrl);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod(CrocsConstant.HTTP_POST_REQUEST);
			conn.setRequestProperty(CrocsConstant.CONTENT_TYPE, CrocsConstant.APPLICATION_JSON);
			conn.setRequestProperty(CrocsConstant.A_X_API_KEY, key);
			conn.setDoOutput(true);

			try (OutputStream os = conn.getOutputStream()) {
				byte[] input = captureRequest.toString().getBytes(StandardCharsets.UTF_8);
				os.write(input, 0, input.length);
				os.flush();
			}

			logger.verbose("Response Code: " + conn.getResponseCode());

			if (201 == conn.getResponseCode()) {
				try (BufferedReader br = new BufferedReader(
						new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
					StringBuilder responseBuilder = new StringBuilder();
					String line;
					while ((line = br.readLine()) != null) {
						responseBuilder.append(line);
					}
					response = responseBuilder.toString();
					logger.verbose("Response: " + response);

				}
			} else {
				try (BufferedReader br = new BufferedReader(
						new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
					StringBuilder responseBuilder = new StringBuilder();
					String line;
					while ((line = br.readLine()) != null) {
						responseBuilder.append(line);
					}
					response = responseBuilder.toString();
					logger.verbose("Response: " + response);

				}
			}
		} catch (Exception /* | NoClassDefFoundError */ e) {
            logger.error("Error in crocsAdyenCaptureRequest: " + e.getMessage(), e);
            throw new Exception("Error in crocsAdyenCaptureRequest: "+e.getMessage());
            //return "Error: " + e.getMessage();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        logger.verbose("CrocsAdyenCaptureRequest.crocsAdyenCaptureRequest:Output Document: " + response);
        logger.endTimer("CrocsAdyenCaptureRequest.crocsAdyenCaptureRequest: End");
        
        return response;
    }
}
