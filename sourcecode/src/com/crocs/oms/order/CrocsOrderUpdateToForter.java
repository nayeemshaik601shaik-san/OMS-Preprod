package com.crocs.oms.order;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.commons.json.JSONObject;
import org.w3c.dom.Document;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCConfigurator;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
//EOMS-1485
//the order statuses are updated in Forter when an order is fully canceled,
//fully shipped, or a full return is processed.

public class CrocsOrderUpdateToForter implements CrocsConstant {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsOrderUpdateToForter.class);

	public Document publishOrderUpdateToForter(YFSEnvironment env, Document inDoc) {

		logger.beginTimer("CrocsOrderUpdateToFoter.publishOrderUpdateToForter");
		logger.verbose("CrocsOrderUpdateToFoter Input XML: " + SCXmlUtil.getString(inDoc));
		try {
			String orderNo = SCXmlUtil.getXpathAttribute(inDoc.getDocumentElement(), XPATH_CONFIRM_SHIPEMNT_ORDER_NO);

			/**
			 * Take SO from Derived for to check whether order is fully return or not
			 */
			if (YFCCommon.isVoid(orderNo))
				orderNo = SCXmlUtil.getXpathAttribute(inDoc.getDocumentElement(), XPATH_DERIVED_FROM_ORDER_ORDER_NO);

			/**
			 * Take Order No if Order is fully Cancelled or not
			 */
			if (YFCCommon.isVoid(orderNo))
				orderNo = SCXmlUtil.getXpathAttribute(inDoc.getDocumentElement(), XPATH_ORDER_NO);

			if (!YFCCommon.isVoid(orderNo)) {
				logger.verbose(
						"CrocsOrderUpdateToForter.publishOrderUpdateToForter() update status in forter for OrderNo: "
								+ orderNo);
				logger.info(
						"CrocsOrderUpdateToForter.publishOrderUpdateToForter() update status in forter for OrderNo: "
								+ orderNo);
				updateToForter(env, orderNo);
			}
		} catch (Exception e) {
			logger.info("CrocsOrderUpdateToForter.publishOrderUpdateToForter() update status in forter for OrderNo: "
					+ inDoc);
			throw new YFSException("CrocsOrderUpdateToForter.publishOrderUpdateToForter :Expection" + e.getMessage());

		}

		return inDoc;

	}

	// This method will prepare json input for order status update to forter and
	// will invoke forter status API.
	private void updateToForter(YFSEnvironment env, String orderNo) {
		logger.beginTimer("CrocsOrderUpdateToFoter.updateToForter");

		String updatedStatus = null;
		HttpURLConnection connection = null;

		try {
			Document getOrderListInDoc = SCXmlUtil.createDocument(E_ORDER);
			getOrderListInDoc.getDocumentElement().setAttribute(OrderNo, orderNo);
			Document getOrderListOut = CommonUtil.invokeAPI(env, TEMPLATE_GET_ORDER_LIST_FORTER_UPDATE,
					API_GET_ORDER_LIST, getOrderListInDoc);
			String maxOrderStatus = SCXmlUtil.getXpathAttribute(getOrderListOut.getDocumentElement(),
					XPATH_MAX_ORDER_STATUS);
			String minOrderStatus = SCXmlUtil.getXpathAttribute(getOrderListOut.getDocumentElement(),
					XPATH_MIN_ORDER_STATUS);

			if (maxOrderStatus.equalsIgnoreCase(STATUS_SHIPPED) && minOrderStatus.equalsIgnoreCase(STATUS_SHIPPED))
				updatedStatus = STR_COMPLETED;
			else if (maxOrderStatus.equalsIgnoreCase(STR_STATUS_CANCELLED)
					&& minOrderStatus.equalsIgnoreCase(STR_STATUS_CANCELLED))
				
			/** EOMS-5731 START 
			 *  Purpose:- 
			 *  	if cancellation happens during the first 30 minutes of order placement( Remorse Hold ) 
			 *               updatedStatus = CANCELED_BY_CUSTOMER
			 *      else 
			 *        	Another cancellation will be Forter( Fraud Decline ) in this case /OrderList/Order/Extn/@ExtnFraudStatus will not be 
			 *      	updated to ACCEPT and 
			 *                updatedStatus = CANCELED_BY_MERCHANT 
			 **/
			{
				String extnFraudStatus = SCXmlUtil.getXpathAttribute(getOrderListOut.getDocumentElement(),
						XPATH_EXTN_FRAUD_STATUS);
				if (!A_ACCEPT.equalsIgnoreCase(extnFraudStatus))
					updatedStatus = STR_CANCELED_BY_MERCHANT;
				else
					updatedStatus = STR_CANCELED_BY_CUSTOMER;
			}
			
			// EOMS-5731 END
			else if (maxOrderStatus.equalsIgnoreCase(STATUS_RETURN_CREATED)
					&& minOrderStatus.equalsIgnoreCase(STATUS_RETURN_CREATED))
				updatedStatus = STR_RETURNED;
			if (!YFCCommon.isVoid(updatedStatus)) {
				// Create the main JSON object
				JSONObject jsonObject = new JSONObject();

				String strCurrency = SCXmlUtil.getXpathAttribute(getOrderListOut.getDocumentElement(),
						XPATH_PRICE_INFO_CURRENCY);
				String strTotalAmount = SCXmlUtil.getXpathAttribute(getOrderListOut.getDocumentElement(),
						XPATH_PRICE_INFO_TOTAL_AMOUNT);
				String strMaxOrderStatusDesc = SCXmlUtil.getXpathAttribute(getOrderListOut.getDocumentElement(),
						XPATH_MAX_ORDER_STATUS_DESC);

				// orderId
				jsonObject.put(A_ORDER_ID_F, orderNo);

				// eventTime
				jsonObject.put(F_EVENT_TIME, System.currentTimeMillis());

				// updatedMerchantStatus
				jsonObject.put(F_UPDATED_MERCHANT_STATUS, strMaxOrderStatusDesc);

				// updatedTotalAmount
				JSONObject updatedTotalAmount = new JSONObject();
				updatedTotalAmount.put(A_AMOUNT_LOCAL_CURRENCY, strTotalAmount);
				updatedTotalAmount.put(A_CURRENCY_F, strCurrency);
				jsonObject.put(F_UPDATED_TOTAL_AMOUNT, updatedTotalAmount);

				// updatedStatus
				jsonObject.put(F_UPDATED_STATUS, updatedStatus);

				// Output the final JSON object

				logger.verbose("CrocsOrderUpdateToForter.updateToForter() update status in forter jsonObject " + (jsonObject.toString()));
				logger.info("CrocsOrderUpdateToForter.updateToForter() update status in forter jsonObject "	+ jsonObject.toString());

				// Setup URL and connection
				String baseURL = YFCConfigurator.getInstance().getProperty(FORTER_API) + STR_STATUS_F;
				String fullURL = baseURL + orderNo;

				logger.info("Forter URL" + fullURL);

				URI uri = new URI(fullURL);
				URL url = uri.toURL();

				connection = (HttpURLConnection) url.openConnection();
				logger.info("Connection to the Forter URL is open");

				
				// EOMS-7592 – Updated HeyDude Forter configuration with new API key, site ID, and API version
				String strUserName="";
				String strForterSiteID="";
				String strApiVersion;
				String strEnterpriseCode= SCXmlUtil.getXpathAttribute(getOrderListOut.getDocumentElement(),
						STR_XPATH_ORDERLIST_ENTERPRISE_CODE);
				if(HEYDUDE_US.equalsIgnoreCase(strEnterpriseCode) || HEYDUDE_CA.equalsIgnoreCase(strEnterpriseCode)
					//EOMS-9722 : EMEA HEYDUDE Enterprises
					|| (strEnterpriseCode != null && CROCS_EMEA_ENTERPRISES.contains(strEnterpriseCode)
                        && strEnterpriseCode.startsWith("HEYDUDE_")))
				{
					strUserName=YFCConfigurator.getInstance().getProperty(HD_FORTER_API_KEY);
					strForterSiteID=YFCConfigurator.getInstance().getProperty(HD_FORTER_SITE_ID);
					strApiVersion=YFCConfigurator.getInstance().getProperty(HD_FORTER_API_VERSION);
				}
				//EOMS-11550 : Shipment Notification to Forter for Korea
				else if(CROCS_AU.equalsIgnoreCase(strEnterpriseCode) 
						|| CROCS_SG.equalsIgnoreCase(strEnterpriseCode) || CROCS_KR.equalsIgnoreCase(strEnterpriseCode))
				{
					strUserName=YFCConfigurator.getInstance().getProperty(CAU_FORTER_API_KEY);
					strForterSiteID=YFCConfigurator.getInstance().getProperty(CAU_FORTER_SITE_ID);
					strApiVersion=YFCConfigurator.getInstance().getProperty(CAU_FORTER_API_VERSION);
				}
				else {
					// Dynamically fetch the API key from SMA for the user name
					strUserName = YFCConfigurator.getInstance().getProperty(CrocsConstant.FORTER_API_KEY);
					// Dynamically fetch the Forter site ID from SMA
					strForterSiteID = YFCConfigurator.getInstance().getProperty(CrocsConstant.FORTER_SITE_ID);
					strApiVersion = YFCConfigurator.getInstance().getProperty(FORTER_API_VERSION);
				}
				String encodedAuth = Base64.getEncoder()
						.encodeToString((strUserName + ":").getBytes(StandardCharsets.UTF_8));

				connection.setRequestProperty(CrocsConstant.A_AUTHORIZATION, CrocsConstant.BASIC + encodedAuth);


				connection.setRequestProperty(CrocsConstant.FORTER_SITE_ID1, strForterSiteID);
				connection.setRequestMethod(CrocsConstant.HTTP_POST_REQUEST);

				String strContentType = YFCConfigurator.getInstance().getProperty(A_CONTENT_TYPE);
			

				connection.setRequestProperty(CrocsConstant.CONTENT_TYPE, strContentType);
				connection.setRequestProperty(CrocsConstant.API_VERSION, strApiVersion);
				connection.setDoOutput(true);
				connection.connect();
				
				if (!YFCCommon.isVoid(jsonObject)) {
					String strPostBody = jsonObject.toString();
					byte[] postData = strPostBody.getBytes(StandardCharsets.UTF_8);
					OutputStream outputStream = connection.getOutputStream();
					outputStream.write(postData);
					outputStream.flush();

				}

				// Get the response code and handle the response
				int responseCode = connection.getResponseCode();
				logger.verbose("Forter Status ResponseCode" + responseCode);
				logger.info("Forter Status ResponseCode" + responseCode);

			}

		} catch (Exception e) {
			logger.verbose("CrocsOrderUpdateToForter.updateToForter() update status in forter for OrderNo" + orderNo);
			logger.info("CrocsOrderUpdateToForter.updateToForter() update status in forter for OrderNo"	+ orderNo + " Error" + e.getMessage());
			throw new YFSException(e.getMessage(),"","CrocsOrderUpdateToForter.updateToForter() update status in forter for OrderNo"+orderNo);
		}

		logger.endTimer("CrocsOrderUpdateToFoter.publishOrderUpdateToForter");
	}
}
