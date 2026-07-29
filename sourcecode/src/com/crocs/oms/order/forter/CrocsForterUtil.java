package com.crocs.oms.order.forter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import com.crocs.oms.common.util.CrocsAPIConstants;
import com.crocs.oms.common.util.CrocsTemplateConstants;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsErrorConstants;
import com.crocs.oms.common.util.CrocsPropertyEncrypterImpl;
import com.crocs.oms.util.ue.CrocsCheckFraudOnOrderUserExitImpl;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCConfigurator;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;


public class CrocsForterUtil implements CrocsConstant{
	
	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsForterUtil.class);
	static CrocsPropertyEncrypterImpl objCrocsPropertyEncrypterImpl = null;
	static CrocsCheckFraudOnOrderUserExitImpl objCrocsCheckFraudOnOrderUserExitImpl =null;

	public static Element fetchOrderList(YFSEnvironment env, String orderHeaderkey) throws RemoteException{
		
		logger.beginTimer("CrocsForterUtil::fetchOrderList");
		
		Document getOrderListOutDoc = null;
		// Prepare input for getOrderList
        Document getOrderListInput = SCXmlUtil.createDocument(E_ORDER);
        Element orderElement = getOrderListInput.getDocumentElement();
        orderElement.setAttribute(A_ORDER_HEADER_KEY, orderHeaderkey);
        
        logger.info("CrocsForterUtil : fetchOrderList : getOrderListInput is: " + SCXmlUtil.getString(getOrderListInput));

        // Call getOrderList
		getOrderListOutDoc = CommonUtil.invokeService(env, CrocsConstant.CROCS_GET_ORDER_LIST_FOR_FORTER,
				getOrderListInput);
		
        logger.info("CrocsForterUtil : fetchOrderList : getOrderListOutDoc is: " + SCXmlUtil.getString(getOrderListOutDoc));

        Element orderListEle = SCXmlUtil.getChildElement(getOrderListOutDoc.getDocumentElement(), E_ORDER);
        
        logger.endTimer("CrocsForterUtil::fetchOrderList");
		return orderListEle;
		
	}
	
	/**
	 * This method is gets the Reason category 
	 * @param env
	 * @param commonCodeType
	 * @return
	 */
	public static String getCommonCodeDesc(YFSEnvironment env, String commonCodeType, String codeValue) {
		try {
			String reasonCategory = "";
			logger.beginTimer("CrocsCompensationUpdateToForter::getCommonCodeDesc");

			/*prepare the input to getCommonCodeList api*/
			Document inDoc = SCXmlUtil.createDocument(A_COMMON_CODE);
			Element inDocEle = inDoc.getDocumentElement();
			inDocEle.setAttribute(A_CODE_TYPE, commonCodeType);
			inDocEle.setAttribute(A_ORGANIZATION_CODE, STR_CROCS);
			inDocEle.setAttribute(A_CODE_VALUE, codeValue);

			logger.info("CrocsForterUtil :getCommonCodeDesc Input: " + SCXmlUtil.getString(inDoc));

			Document docGetCommonCodeListOutput = CommonUtil.invokeAPI(env, CrocsTemplateConstants.TEMPLATE_GET_COMMON_CODE_LIST,
					CrocsAPIConstants.API_GET_COMMON_CODE_LIST, inDoc);
			logger.info("CrocsForterUtil :getCommonCodeDesc Outdoc: " + SCXmlUtil.getString(docGetCommonCodeListOutput));

			if (!YFCCommon.isVoid(docGetCommonCodeListOutput)) {
				reasonCategory = SCXmlUtil.getXpathAttribute(
						docGetCommonCodeListOutput.getDocumentElement(),
						"/CommonCodeList/CommonCode/@CodeLongDescription");
			}
			logger.endTimer("CrocsCompensationUpdateToForter::getCommonCodeDesc");
			return reasonCategory;
		} catch (Exception e) {
			logger.verbose("CrocsCompensationUpdateToForter.getBuildForterAppeasementInput :Expection" + e.getMessage());
			throw new YFSException("Error invoking getCommonCodeList API: " + e.getMessage());
		}
	}

	
	/**
	 * @param custAttributesEle
	 * @return
	 * @throws Exception
	 */
	public static Map<String, String> getCustomerSecurityDetails(Element custAttributesEle) throws Exception {

		String strCustomerIP=CUSTOMER_IP;
		String strUserAgent=USERAGENT;
		
		Map<String, String> securityDetails = new HashMap<>();
	    securityDetails.put("CUSTOMER_IP", strCustomerIP);
	    securityDetails.put("USER_AGENT", strUserAgent);
		
	    logger.beginTimer("CrocsCompensationUpdateToForter::getCustomerSecurityDetails");
	
		if(!YFCCommon.isVoid(custAttributesEle)) {
			
			objCrocsPropertyEncrypterImpl = new CrocsPropertyEncrypterImpl();
			if(!YFCCommon.isVoid(custAttributesEle.getAttribute(A_TEXT_2)))
				strCustomerIP = objCrocsPropertyEncrypterImpl.decrypt(custAttributesEle.getAttribute(A_TEXT_2));
				securityDetails.put("CUSTOMER_IP",strCustomerIP);
				
			if(!YFCCommon.isVoid(custAttributesEle.getAttribute(A_TEXT_11)) || !YFCCommon.isVoid(custAttributesEle.getAttribute(A_TEXT_12))
					|| !YFCCommon.isVoid(custAttributesEle.getAttribute(A_TEXT_13)) || !YFCCommon.isVoid(custAttributesEle.getAttribute(A_TEXT_14))
					|| !YFCCommon.isVoid(custAttributesEle.getAttribute(A_TEXT_15))) {
				String strConcatUserAgent = custAttributesEle.getAttribute(A_TEXT_11) + custAttributesEle.getAttribute(A_TEXT_12) + custAttributesEle.getAttribute(A_TEXT_13) + custAttributesEle.getAttribute(A_TEXT_14)+ custAttributesEle.getAttribute(A_TEXT_15);
				strUserAgent = objCrocsPropertyEncrypterImpl.decrypt(strConcatUserAgent);
				securityDetails.put("CUSTOMER_IP",strCustomerIP);
			}
			logger.info("strCustomerIP:" + strCustomerIP + "strUserAgent:" + strUserAgent);
		}
		return securityDetails;
	}
	
	/**
	 * Invokes Forter Compensation API.
	 * @param env
	 * @param orderNo
	 * @param orderHdrKey
	 * @param strEnterpriseCode
	 * @param reshipDetails
	 */
	public static void invokeForter(YFSEnvironment env, String orderNo, String orderHdrKey, String strEnterpriseCode, JSONObject reshipDetails, String callType) {
		logger.beginTimer("CrocsCompensationUpdateToForter::invokeForter");
		objCrocsCheckFraudOnOrderUserExitImpl = new CrocsCheckFraudOnOrderUserExitImpl();
		
		String accountId = "";
		String strUserName="";
		String strForterSiteID ="";
		String strApiVersion="";
		String baseURL = "";

		try {
			// Send the request to Forter API
			HttpURLConnection connection = null;
				
			// Setup URL and connection
			if(callType.equalsIgnoreCase("orderStatus")){
				accountId = reshipDetails.getString("orderId");
				baseURL =YFCConfigurator.getInstance().getProperty(FORTER_API)+ FORTER_ORDER_STATUS_API;
			}else {
				accountId = reshipDetails.getString("accountId");
				baseURL =YFCConfigurator.getInstance().getProperty(FORTER_API)+ FORTER_COMPENSATION_API;
			}

			String fullURL = baseURL +"/"+ accountId;

			URI uri = new URI(fullURL);
			URL url = uri.toURL();
			connection = (HttpURLConnection) url.openConnection();
			
			// Dynamically fetch the API key, Site ID and API version from SMA for the user name
			if(HEYDUDE_US.equalsIgnoreCase(strEnterpriseCode))
			{
				strUserName=YFCConfigurator.getInstance().getProperty(HD_FORTER_API_KEY);
				strForterSiteID=YFCConfigurator.getInstance().getProperty(HD_FORTER_SITE_ID);
				strApiVersion=YFCConfigurator.getInstance().getProperty(HD_FORTER_API_VERSION);
			}
			else if(CROCS_AU.equalsIgnoreCase(strEnterpriseCode))
			{
				strUserName=YFCConfigurator.getInstance().getProperty(CAU_FORTER_API_KEY);
				strForterSiteID=YFCConfigurator.getInstance().getProperty(CAU_FORTER_SITE_ID);
				strApiVersion=YFCConfigurator.getInstance().getProperty(CAU_FORTER_API_VERSION);
			}else {
				strUserName = YFCConfigurator.getInstance().getProperty(FORTER_API_KEY);
				strForterSiteID = YFCConfigurator.getInstance().getProperty(FORTER_SITE_ID);
				strApiVersion=YFCConfigurator.getInstance().getProperty(FORTER_API_VERSION);
			}
			
			String encodedAuth = Base64.getEncoder()
					.encodeToString((strUserName + ":").getBytes(StandardCharsets.UTF_8));
			connection.setRequestProperty(A_AUTHORIZATION, BASIC + encodedAuth);
			logger.verbose(" CrocsCompensationUpdateToForter : invokeForter : strUserName: " + strUserName);
			
			connection.setRequestProperty(FORTER_SITE_ID1, strForterSiteID);
			logger.verbose("CrocsCompensationUpdateToForter : invokeForter : strForterSiteID: " + strForterSiteID);

			connection.setRequestMethod(HTTP_POST_REQUEST);
			String strContentType=YFCConfigurator.getInstance().getProperty(A_CONTENT_TYPE);
			

			connection.setRequestProperty(CONTENT_TYPE, strContentType);
			connection.setRequestProperty(API_VERSION, strApiVersion);
			connection.setDoOutput(true);
			connection.connect();

			if (!YFCCommon.isVoid(reshipDetails)) {
				String strPostBody = reshipDetails.toString();
				byte[] postData = strPostBody.getBytes(StandardCharsets.UTF_8);
				OutputStream outputStream = connection.getOutputStream();
				outputStream.write(postData);
				outputStream.flush();
				logger.verbose("CrocsCompensationUpdateToForter : invokeForter :connection:outputStream:" + outputStream.toString());
			}

			// Get the response code and handle the response
			int responseCode = connection.getResponseCode();

			logger.verbose("CrocsCompensationUpdateToForter : invokeForter :Connection response: " + connection.getResponseMessage());
			
			// Read the response body
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			
			String inputLine;
			StringBuilder response = new StringBuilder();
			logger.verbose("CrocsCompensationUpdateToForter : invokeForter : StringBuilder :response" + response.toString());
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
				logger.verbose("CrocsCompensationUpdateToForter : invokeForter : inside while block :response" + response.toString());
			}
			
			logger.info("CrocsCompensationUpdateToForter : invokeForter : response" + response.toString());
			
			if (responseCode == HttpURLConnection.HTTP_OK) {
				logger.info("CrocsCompensationUpdateToForter: Compensation request updated Successfully");
			}else
				logger.info("CrocsCompensationUpdateToForter failed: " + responseCode);
				
		} catch (Exception e) {
			logger.info("CrocsCompensationUpdateToForter : invokeForter : Forter Connection Exception: "+ orderNo);
			logger.verbose("Execption connection: " + e.getMessage());
			
			String refernceValue=e.getMessage();
			// Raising alert
			objCrocsCheckFraudOnOrderUserExitImpl.createAlertOnOrder(env, orderNo,orderHdrKey,strEnterpriseCode,refernceValue);
			throw new YFSException(CrocsErrorConstants.VAL_ERROR_DESCRIPTION_FORTER, CrocsErrorConstants.VAL_ERROR_CODE_EXTN_003,CrocsErrorConstants.VAL_ERROR_DESCRIPTION_EXTN_003);
		}
		logger.endTimer("CrocsCompensationUpdateToForter::invokeForter");
	}
	
}