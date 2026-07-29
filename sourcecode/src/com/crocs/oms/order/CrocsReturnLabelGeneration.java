package com.crocs.oms.order;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCConfigurator;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
/*
 * EOMS-734 Narvar Return Label Generation From OMS
 * This class is responsible for calling the Narvar Returns API to retrieve:
 * - Tracking Number
 * - Tracking URL
 * - Label URL
 * These values are fetched specifically for call center orders.
 * Once retrieved, the values will be persisted under custom attributes
 */

/* Sample Request to Narvar
  { "rma_number": "1077998OCUS", "carrier": "", "order_number": "01222SU34CUS",
  "locale": "", "type": "return", "order_items": [ { "quantity": 1,
  "return_reason_code": "312", "sku": "40002-001-M32", "customer_comment":
  "Changed Mind" 
  }
   ]
    }*/
 
/*
 * Sample Success Response from Narvar
{

    "return_reference_number": "gbZ1Aoq8DvyKaRmO",
    "order_number": "1077312O1CUS",
    "rma_number": "1077312021CUS",
    "return_details": [
        {
            "type": "MAIL",
            "carrier": {
                "name": "UPS",
                "additional_info": {}
            },
            "label_url": "https://returns.st.narvar.com/returns/crocs/viewlabel?return_id=gbZ1Aoq8DvyKaRmO",
            "tracking_number": "1Z6F86039013688713",
            "tracking_url": "https://tracking-service.st20.narvar.com/crocs/tracking/ups?tracking_numbers=1Z6F86039013688713&order_number=1077312O1CUS&bzip=94105&locale=fr_CA&type=ret",
            "qr_code_url": null
        }
    ],
    "status": "SUCCESS",
    "messages": [
        {
            "code": "response.status.success"
        }
    ]
}*/


/*Sample Failed Response from Narvar:
 * { "status": "FAILURE", "messages": [ { "code": "response.status.failure",
 * "message":
 * "Return not initiated for order 01222SU34CUS due to error: Failed to generate label for order number: 01222SU34CUS reason: ReturnsCarrierException: The requested service is unavailable between the selected locations."
 * }
 *  
 * ] 
 * }
 */
public class CrocsReturnLabelGeneration implements CrocsConstant {
	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsReturnLabelGeneration.class);
	/*
	 * This methods is responsible for calling the Narvar Returns API :
	 */

	public Document generateReturnLabel(YFSEnvironment env, Document inDoc) throws Exception {
		logger.beginTimer("CrocsReturnLabelGeneration.generateReturnLabel");
		logger.debug("generateReturnLabel Input Document : " + XMLUtil.getXMLString(inDoc));
		Element eleOrder = inDoc.getDocumentElement();
		String strOrderNo = SCXmlUtil.getXpathAttribute(inDoc.getDocumentElement(), XPATH_DERIVED_FROM_ORDER_ORDER_NO);

		Document getOrderListInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER);
		getOrderListInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_ORDER_NO, strOrderNo);
		Document getOrderListout = CommonUtil.invokeAPI(env, TEMPLATE_GET_ORDER_LIST_FOR_EXTN_CUSTOMER_LOCALE, API_GET_ORDER_LIST, getOrderListInDoc);
		logger.debug("getOrderListout:" + getOrderListout);
		Element getOrderListEle=SCXmlUtil.getXpathElement(getOrderListout.getDocumentElement(), XPATH_ORDER_ELEMENT);
		Element eleExtn=SCXmlUtil.getXpathElement(getOrderListEle, E_EXTN);
		String strExtnCustomerLocale=eleExtn.getAttribute(EXTN_CUSTOMER_LOCALE);
		
		String strOrderHeaderKey = eleOrder.getAttribute(OrderHeaderKey);
		String strReutrnOrderNo = eleOrder.getAttribute(OrderNo);
		JSONObject json = new JSONObject();
		
		if(YFCCommon.isVoid(strExtnCustomerLocale) || STR_DEFAULT.equalsIgnoreCase(strExtnCustomerLocale))
			json.put(N_LOCALE, STR_EN_US);
		else
			json.put(N_LOCALE, strExtnCustomerLocale);
		
		// Add simple key-value pairs
		json.put(N_RMA_NUMBER, strReutrnOrderNo);
		json.put(N_CARRIER, "");
		json.put(N_ORDER_NUMBER, strOrderNo);
		json.put(TYPE, N_TYPE_VAL);

		//EOMS-6250 : HeyDude Narvar Label generation : START
		String strEnterpriseCode = eleOrder.getAttribute(A_ENTERPRISE_CODE);	
		JSONObject attributes = new JSONObject();
		if (HEYDUDE_US.equalsIgnoreCase(strEnterpriseCode) || HEYDUDE_CA.equalsIgnoreCase(strEnterpriseCode) ||
				HEYDUDE_DE.equalsIgnoreCase(strEnterpriseCode) || HEYDUDE_EU.equalsIgnoreCase(strEnterpriseCode) ||
				HEYDUDE_FR.equalsIgnoreCase(strEnterpriseCode) || HEYDUDE_GB.equalsIgnoreCase(strEnterpriseCode) ) {
		    attributes.put(STR_CHECKOUT_BRAND, STR_HEYDUDE_BRAND);
		}else {
			attributes.put(STR_CHECKOUT_BRAND, STR_CROCS_BRAND);
		}
		json.put(STR_ATTRIBUTES,attributes);
		//EOMS-6250 : HeyDude Narvar Label generation : END
		
		// Create order_items array
		JSONArray orderItems = new JSONArray();

		NodeList orderLineNodeList = inDoc.getDocumentElement().getElementsByTagName(E_ORDER_LINE);
		for (int i = 0; i < orderLineNodeList.getLength(); i++) {
			JSONObject item = new JSONObject();
			Element orderLineEle = (Element) orderLineNodeList.item(i);
			String strOrderedQty = SCXmlUtil.getAttribute(orderLineEle, A_ORDERED_QTY);
			String strReturnReason = SCXmlUtil.getAttribute(orderLineEle, STR_RETURN_REASON);
			String strReturnReasonLongDesc = SCXmlUtil.getAttribute(orderLineEle, STR_RETURN_REASON_LONG_DESC);
			Element itemEle = SCXmlUtil.getChildElement(orderLineEle, E_ITEM);
			String strItemID = SCXmlUtil.getAttribute(itemEle, A_ITEM_ID);

			item.put(POSTMEN_QUANTITY, (int) (Double.parseDouble(strOrderedQty)));
			item.put(N_RETURN_REASON_CODE, strReturnReason);
			item.put(SKU, strItemID);
			item.put(N_CUSTOMER_COMMENT, strReturnReasonLongDesc);

			orderItems.put(item);

		}

		json.put(N_ORDER_ITEMS, orderItems);
		logger.verbose("Json Object" + json.toString());
		logger.verbose("Request : " + json.toString());
		// Pretty print with indentation
		HttpURLConnection connection = null;
		String strUserName = "";
		String strPassword = "";
		try {
			// Setup URL and connection
			String sURl = YFCConfigurator.getInstance().getProperty(CROCS_NARVAR_API_FOR_RETURN);

			URI uri = new URI(sURl);
			URL url = uri.toURL();
			connection = (HttpURLConnection) url.openConnection();
			// Dynamically fetch the API key from SMA for the user name
			//EOMS-6250 : HeyDude Narvar Label generation : START
			if (HEYDUDE_US.equalsIgnoreCase(strEnterpriseCode) || HEYDUDE_CA.equalsIgnoreCase(strEnterpriseCode)) {
				strUserName = YFCConfigurator.getInstance().getProperty(HEYDUDE_NARVAR_USERNAME);
				strPassword = YFCConfigurator.getInstance().getProperty(HEYDUDE_NARVAR_PASSWORD);
			}else {
				strUserName = YFCConfigurator.getInstance().getProperty(CROCS_NARVAR_USERNAME);
				strPassword = YFCConfigurator.getInstance().getProperty(CROCS_NARVAR_PASSWORD);
			}
			//EOMS-6250 : HeyDude Narvar Label generation : END
			String encodedAuth = Base64.getEncoder()
					.encodeToString((strUserName + ":" + strPassword).getBytes(StandardCharsets.UTF_8));
			connection.setRequestProperty(CrocsConstant.A_AUTHORIZATION, CrocsConstant.BASIC + encodedAuth);

			connection.setRequestMethod(CrocsConstant.HTTP_POST_REQUEST);
			String strContentType=YFCConfigurator.getInstance().getProperty(A_CONTENT_TYPE);
			connection.setRequestProperty(CrocsConstant.CONTENT_TYPE, strContentType);
			connection.setDoOutput(true);
			connection.connect();

			if (!YFCCommon.isVoid(json)) {
				String strPostBody = json.toString();
				byte[] postData = strPostBody.getBytes(StandardCharsets.UTF_8);
				OutputStream outputStream = connection.getOutputStream();
				outputStream.write(postData);
				outputStream.flush();

			}

			// Get the response code and handle the response

			// prepare response body
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuilder responseBody = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				responseBody.append(inputLine);
			}
			in.close();
			logger.verbose("responseBody" + responseBody.toString());
			logger.verbose("Response : " + responseBody.toString());
			logger.info(OMS_LOG_INFO + "CrocsReturnLabelGeneration.generateReturnLabel() : Narvar to OMS return order label generation : SO:" + strOrderNo+" RO:"+strReutrnOrderNo);
			
			JSONObject jsonResponse = new JSONObject(responseBody.toString());
			String status = jsonResponse.getString(STATUS);
			if (V_SUCCESS.equals(status)) {
				JSONArray returnDetails = jsonResponse.getJSONArray(N_RETURN_DETAILS);
				if (returnDetails.length() > 0) {
					JSONObject details = returnDetails.getJSONObject(0);
					JSONObject carrier = details.getJSONObject(N_CARRIER);

					String trackingNumber = details.optString(N_TRACKING_NUMBER);
					String trackingUrl = details.optString(N_TRACKING_URL);
					String labelUrl = details.optString(N_LABEL_URL);
					String scac = carrier.optString(N_NAME);
					Document changeOrderInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER);
					Element changeOrderEle = changeOrderInDoc.getDocumentElement();
					changeOrderEle.setAttribute(CrocsConstant.OrderHeaderKey, strOrderHeaderKey);
					changeOrderEle.setAttribute(CrocsXmlConstants.A_OVERRIDE, CrocsXmlConstants.FLAG_Y);
					changeOrderEle.setAttribute(A_SELECT_METHOD, VAL_WAIT);
					Element changeOrderNotesEle = SCXmlUtil.createChild(changeOrderEle, A_NOTES);
					Element changeOrderNoteEle = SCXmlUtil.createChild(changeOrderNotesEle, A_NOTE);
					changeOrderNoteEle.setAttribute(CrocsXmlConstants.A_REASON_CODE, STR_LABEL_URL);
					changeOrderNoteEle.setAttribute(NOTE_TEXT, labelUrl);
					Element changeOrderLinesEle = SCXmlUtil.createChild(changeOrderEle, E_ORDER_LINES);
					for (int i = 0; i < orderLineNodeList.getLength(); i++) {
						Element orderLineEle = (Element) orderLineNodeList.item(i);
						String strOrderLineKey = SCXmlUtil.getAttribute(orderLineEle, A_ORDER_LINE_KEY);
						Element changeOrderLineEle = SCXmlUtil.createChild(changeOrderLinesEle, E_ORDER_LINE);
						changeOrderLineEle.setAttribute(A_ORDER_LINE_KEY, strOrderLineKey);
						Element customAttributesEle = SCXmlUtil.createChild(changeOrderLineEle, A_CUSTOM_ATTRIBUTES);
						customAttributesEle.setAttribute(STR_TEXT_1, trackingNumber);
						customAttributesEle.setAttribute(STR_TEXT_2, trackingUrl);
						customAttributesEle.setAttribute(STR_TEXT_3, labelUrl);
						Element extnEle = SCXmlUtil.createChild(changeOrderLineEle, E_EXTN);
						extnEle.setAttribute(A_EXTN_TRACKING_URL, trackingUrl);
						extnEle.setAttribute(EXTN_TRACKING_NO, trackingNumber);
						extnEle.setAttribute(EXTN_SHIP_CARRIER, scac);

				}

				logger.debug("changeOrderInput:- " + changeOrderInDoc);
				CommonUtil.invokeAPI(env, "", API_CHANGE_ORDER, changeOrderInDoc);

				}
			} else {
				retryLabelGeneration(env, inDoc);
			}

		} catch (YFSException e) {
			logger.verbose("Exception" + e.toString());
			throw new YFSException(e.getMessage(), e.getErrorCode(), e.getErrorDescription());

		}
		logger.endTimer("CrocsReturnLabelGeneration.generateReturnLabel");
		return inDoc;

	}


	
	/**    
	 * EOMS-4442 Retry on Narvar Label Generation Failures
	 * @param env
	 * @param inDoc
	 * @throws RemoteException
	 */
	private void retryLabelGeneration(YFSEnvironment env, Document inDoc) throws RemoteException {

		logger.verbose("CrocsReturnLabelGeneration.retryLabelGeneration START" +SCXmlUtil.getString(inDoc));
		String strEnterPriseCode = inDoc.getDocumentElement().getAttribute(EnterpriseCode);

		if (inDoc.getDocumentElement().hasAttribute(A_RETRY_COUNT)) {
			String retryValue = inDoc.getDocumentElement().getAttribute(A_RETRY_COUNT);

			if (Integer.parseInt(retryValue) < Integer
					.parseInt(YFCConfigurator.getInstance().getProperty(CROCS_NARVAR_RETRY_COUNT))) {
				inDoc.getDocumentElement().setAttribute(A_RETRY_COUNT,
						String.valueOf(Integer.parseInt(retryValue) + 1));

				if (CROCS_US.equals(strEnterPriseCode) || HEYDUDE_US.equalsIgnoreCase(strEnterPriseCode))
					CommonUtil.invokeService(env, STR_CROCS_US_NARVAR_LABEL_POST_TO_Q, inDoc);
                else if (CROCS_AU.equals(strEnterPriseCode))
                    CommonUtil.invokeService(env, STR_CROCS_AU_NARVAR_LABEL_POST_TO_Q, inDoc);
				else if (CROCS_SG.equals(strEnterPriseCode))
					CommonUtil.invokeService(env, STR_CROCS_SG_NARVAR_LABEL_POST_TO_Q, inDoc);
				else if(CROCS_EMEA_ENTERPRISES.contains(strEnterPriseCode))
					CommonUtil.invokeService(env, STR_CROCS_EMEA_NARVAR_LABEL_POST_TO_Q, inDoc);
				else
					CommonUtil.invokeService(env, STR_CROCS_CA_NARVAR_LABEL_POST_TO_Q, inDoc);

			}
		} else {
			inDoc.getDocumentElement().setAttribute(A_RETRY_COUNT, STR_COUNT_1);

			if (CROCS_US.equals(strEnterPriseCode) || HEYDUDE_US.equalsIgnoreCase(strEnterPriseCode))
				CommonUtil.invokeService(env, STR_CROCS_US_NARVAR_LABEL_POST_TO_Q, inDoc);
            else if (CROCS_AU.equals(strEnterPriseCode))
                CommonUtil.invokeService(env, STR_CROCS_AU_NARVAR_LABEL_POST_TO_Q, inDoc);
			else if (CROCS_SG.equals(strEnterPriseCode))
                CommonUtil.invokeService(env, STR_CROCS_SG_NARVAR_LABEL_POST_TO_Q, inDoc);
			else if(CROCS_EMEA_ENTERPRISES.contains(strEnterPriseCode))
				CommonUtil.invokeService(env, STR_CROCS_EMEA_NARVAR_LABEL_POST_TO_Q, inDoc);
			else
				CommonUtil.invokeService(env, STR_CROCS_CA_NARVAR_LABEL_POST_TO_Q, inDoc);
		}
		logger.verbose("CrocsReturnLabelGeneration.retryLabelGeneration END" +SCXmlUtil.getString(inDoc));

	}
	
}