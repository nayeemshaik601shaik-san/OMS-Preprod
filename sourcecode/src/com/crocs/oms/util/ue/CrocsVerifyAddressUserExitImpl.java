package com.crocs.oms.util.ue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.json.JSONArray;
import org.apache.commons.json.JSONException;
import org.apache.commons.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.pca.ycd.japi.ue.YCDVerifyAddressWithAVSUE;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCConfigurator;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSUserExitException;
/**
 * EOMS-812
 * 
 */
public class CrocsVerifyAddressUserExitImpl implements YCDVerifyAddressWithAVSUE, CrocsConstant {
	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsVerifyAddressUserExitImpl.class);

	@Override
	/**
	 * Below is verifyAddressWithAVS method of YCDVerifyAddressWithAVSUE.
	 * It prepares AVS input and invoke AVS end point .
	 * From AVs Response it will prepares YCDVerifyAddressWithAVSUE output
	 * @param arg0
	 * @param @inDoc
	 */
	public Document verifyAddressWithAVS(YFSEnvironment arg0, Document inDoc) throws YFSUserExitException {

		logger.beginTimer("CrocsVerifyAddress.verifyAddressWithAVS");
		logger.debug("verifyAddressWithAVS Input Document : " + XMLUtil.getXMLString(inDoc));

		Document outDoc = null;

		Element inRoot = inDoc.getDocumentElement();
		String countryCode = inRoot.getAttribute(CrocsXmlConstants.A_COUNTRY);
		String addressLine1 = inRoot.getAttribute(CrocsXmlConstants.A_ADDRESS_LINE_1);
		String addressLine2 = inRoot.getAttribute(CrocsXmlConstants.A_ADDRESS_LINE_2);

		String city = inRoot.getAttribute(CrocsXmlConstants.A_CITY);
		String state = inRoot.getAttribute(CrocsXmlConstants.A_STATE);
		String zipCode = inRoot.getAttribute(CrocsXmlConstants.A_ZIP_CODE);

		// the Json input for AVS end point
		JSONObject jRoot = new JSONObject();
		JSONArray jAddresses = new JSONArray();
		JSONObject jAddress = new JSONObject();
		
		String sKey=YFCConfigurator.getInstance().getProperty(CrocsConstant.CROCS_AVS_KEY);
		
		try {
			jRoot.put(CrocsXmlConstants.A_KEY, sKey);
			jAddress.put(CrocsXmlConstants.A_ADDRESS_1, addressLine1);
			jAddress.put(CrocsXmlConstants.A_ADDRESS_2, addressLine2);
			jAddress.put(CrocsXmlConstants.A_LOCALITY, city);
			jAddress.put(CrocsXmlConstants.A_ADIMNISTRATIVEAREA, state);
			jAddress.put(CrocsXmlConstants.A_COUNTRY, countryCode);
			jAddress.put(CrocsXmlConstants.A_POSTALCODE, zipCode);
			jAddresses.add(jAddress);
			jRoot.put(CrocsXmlConstants.A_ADDRESSES, jAddresses);
		} catch (JSONException e) {

			logger.debug("Error :"+e.getMessage());
			e.printStackTrace();
		}

		HttpURLConnection httpRes;
		try {
			String sURl=YFCConfigurator.getInstance().getProperty(CrocsConstant.CROCS_AVS_URL);
			httpRes = getAVSResponse(sURl, jRoot);
			// prepare response body
			BufferedReader in = new BufferedReader(new InputStreamReader(httpRes.getInputStream()));
			String inputLine;
			StringBuilder responseBody = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				responseBody.append(inputLine);
			}
			in.close();
			outDoc = verifyAddress(responseBody, httpRes.getResponseCode());

		} catch (IOException | JSONException e) {
			logger.debug("Error :"+e.getMessage());
			e.printStackTrace();
		}

		logger.verbose("outDoc : " + SCXmlUtil.getString(outDoc));
		logger.endTimer("CrocsVerifyAddress.verifyAddressWithAVS");
		return outDoc;

	}
/**
 * This method validates Change shipping address .
 * @param responseBody
 * @param responseCode
 * @return
 * @throws JSONException
 */
	private static Document verifyAddress(StringBuilder responseBody, int responseCode) throws JSONException {

		logger.beginTimer("CrocsVerifyAddress.verifyAddress");
		logger.verbose("Response : " + responseBody.toString());
	
		
		Document outDoc =null;

		JSONArray httpResponse = new JSONArray(responseBody.toString());
		 outDoc = SCXmlUtil.createDocument("PersonInfoList");
		Element personInfoListEle = outDoc.getDocumentElement();
		Element personInfo = SCXmlUtil.createChild(personInfoListEle, "PersonInfo");
		Element addressVerificationResponseMessagesEle = SCXmlUtil.createChild(personInfoListEle,
				"AddressVerificationResponseMessages");
		Element addressVerificationResponseMessageEle = SCXmlUtil.createChild(addressVerificationResponseMessagesEle,
				"AddressVerificationResponseMessage");

		JSONObject address = httpResponse.getJSONObject(0);
		JSONArray matchesAddressArray = address.getJSONArray("Matches");
		JSONObject matchedAddress = matchesAddressArray.getJSONObject(0);
		String strLocality ="";
		String strAddress1 ="";
		String strAddress2 ="";
		String strAdministrativeArea = "";
		if(!matchedAddress.isNull(CrocsXmlConstants.A_LOCALITY))
			strLocality=matchedAddress.getString(CrocsXmlConstants.A_LOCALITY);
		
		if(!matchedAddress.isNull(CrocsXmlConstants.A_ADDRESS_1))
			strAddress1= matchedAddress.getString(CrocsXmlConstants.A_ADDRESS_1);
		
		if(!matchedAddress.isNull(CrocsXmlConstants.A_ADDRESS_2))
			strAddress2= matchedAddress.getString(CrocsXmlConstants.A_ADDRESS_2);
		if(!matchedAddress.isNull(A_ADIMNISTRATIVEAREA))
			strAdministrativeArea=matchedAddress.getString(A_ADIMNISTRATIVEAREA);
		
		
		String strAQI = matchedAddress.getString(AVS_AQI);
		String strCountry = matchedAddress.getString(A_COUNTRY);
		String strPostalCode = matchedAddress.getString(A_POSTALCODE);
		String validAQIValues=YFCConfigurator.getInstance().getProperty(CrocsConstant.CROCS_AVS_AQI);
		List<String> validAQIsList = Arrays.asList(validAQIValues.split(","));
		if (matchesAddressArray.length() == 1 && CrocsXmlConstants.RESPONSE_CODE.equalsIgnoreCase(String.valueOf(responseCode))) {

			if (validAQIsList.contains(strAQI)) {

				logger.verbose("Entered address is valid");
				
				personInfoListEle.setAttribute(CrocsXmlConstants.A_TOTAL_NO_OF_RECORDERS, "1");
				personInfoListEle.setAttribute(CrocsXmlConstants.A_PROCEED_WITH_SINGLE_AVS_RESULT, FLAG_Y);
				personInfo.setAttribute(CrocsXmlConstants.A_IS_ADDRESS_VERIFIED, FLAG_Y);
				personInfo.setAttribute(CrocsXmlConstants.A_AVS_RETURN_CODE, CrocsXmlConstants.AVS_RETURN_CODE_VERIFIED);
			} else {
				logger.verbose("Entered address is inValid");
				
				personInfoListEle.setAttribute(CrocsXmlConstants.A_PROCEED_WITH_SINGLE_AVS_RESULT, FLAG_Y);
				personInfo.setAttribute(CrocsXmlConstants.A_IS_ADDRESS_VERIFIED, FLAG_N);
				personInfoListEle.setAttribute(CrocsXmlConstants.A_TOTAL_NO_OF_RECORDERS, "1");
				personInfo.setAttribute(CrocsXmlConstants.A_AVS_RETURN_CODE, CrocsXmlConstants.AVS_RETURN_CODE_FALILED);
				}
		}
		else {
			personInfoListEle.setAttribute(CrocsXmlConstants.A_PROCEED_WITH_SINGLE_AVS_RESULT, FLAG_N);
			personInfoListEle.setAttribute(CrocsXmlConstants.A_TOTAL_NO_OF_RECORDERS, String.valueOf(matchesAddressArray.length()));
			personInfo.setAttribute(CrocsXmlConstants.A_IS_ADDRESS_VERIFIED, FLAG_N);
			personInfo.setAttribute(CrocsXmlConstants.A_AVS_RETURN_CODE, CrocsXmlConstants.AVS_RETURN_CODE_FALILED);

		}
		
		personInfo.setAttribute(CrocsXmlConstants.A_ADDRESS_LINE_1, strAddress1.toUpperCase());
		personInfo.setAttribute(CrocsXmlConstants.A_ADDRESS_LINE_2, strAddress2.toUpperCase());
		personInfo.setAttribute(CrocsXmlConstants.A_CITY, strLocality.toUpperCase());
		personInfo.setAttribute(CrocsXmlConstants.A_STATE, strAdministrativeArea.toUpperCase());
		personInfo.setAttribute(CrocsXmlConstants.A_COUNTRY, strCountry.toUpperCase());
		personInfo.setAttribute(CrocsXmlConstants.A_ZIPCODE, strPostalCode.toUpperCase());
		addressVerificationResponseMessageEle.setAttribute(CrocsXmlConstants.A_MESSAGE_CODE, strAQI);
		logger.debug("City"+strLocality.toUpperCase());
		logger.debug("Address"+strAddress1.toUpperCase());
		logger.debug("Address"+strAddress2.toUpperCase());
		logger.debug("State"+strAdministrativeArea.toUpperCase());
		logger.debug("Country"+strCountry.toUpperCase());
		logger.debug("Zipcode"+ strPostalCode.toUpperCase());
		logger.debug("outDoc"+outDoc.toString());
		logger.endTimer("CrocsVerifyAddress.verifyAddress");
		return outDoc;
	}
/**
 * This Method calling post request
 * Method returns the HttpsURLConnection, which can be used to read the server's response.
 * @param surl
 * @param jRoot
 * @return
 * @throws IOException
 */
	private static HttpURLConnection getAVSResponse(String surl, JSONObject jRoot) throws IOException {
		logger.beginTimer("CrocsVerifyAddress.getAVSResponse");
		URL url = new URL(surl);
		HttpsURLConnection httpsCon = (HttpsURLConnection) url.openConnection();
		httpsCon.setRequestMethod(HTTP_POST_REQUEST);
		String strContentType=YFCConfigurator.getInstance().getProperty(A_CONTENT_TYPE);
		httpsCon.setRequestProperty(CONTENT_TYPE, strContentType);
		httpsCon.setDoOutput(true);
		httpsCon.connect();
		// prepare request body
		if (!YFCCommon.isVoid(jRoot)) {
			String strPostBody = jRoot.toString();
			byte[] postData = strPostBody.getBytes(StandardCharsets.UTF_8);
			OutputStream outputStream = httpsCon.getOutputStream();
			outputStream.write(postData);
			outputStream.flush();
		}
		logger.endTimer("CrocsVerifyAddress.getAVSResponse");
		return httpsCon;
	}

}