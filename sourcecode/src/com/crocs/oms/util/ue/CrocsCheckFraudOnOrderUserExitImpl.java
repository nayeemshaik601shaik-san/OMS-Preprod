package com.crocs.oms.util.ue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsErrorConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.pca.ycd.japi.ue.YCDProcessOrderFraudCheckUE;
import com.crocs.oms.common.util.CrocsPropertyEncrypterImpl;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCConfigurator;
import com.yantra.yfs.core.YFSSystem;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import com.yantra.yfs.japi.YFSUserExitException;

/*This class related to Fraud validation with forter 
 * EOMS - 587 based on the user exit prepared the forter request.
 * once response got from forter we are returning as outDoc
 */
public class CrocsCheckFraudOnOrderUserExitImpl implements YCDProcessOrderFraudCheckUE, CrocsConstant {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsCheckFraudOnOrderUserExitImpl.class);
	CrocsPropertyEncrypterImpl objCrocsPropertyEncrypterImpl = null;

	@Override
	public Document processOrderFraudCheck(YFSEnvironment env, Document inDoc) throws YFSUserExitException {

		logger.verbose("CrocsCheckFraudOnOrderUserExitImpl : processOrderFraudCheck : Input : Start" + XMLUtil.getXMLString(inDoc));

		String strItemCostCurrency = null;
		String strCustomerIP=CUSTOMER_IP;
		String strMerchantDomain="";
		String strForterTokenCookie="";
		String strUserAgent=USERAGENT;
		String strDeliveryMethod="";
		
		//EOMS-8616 Changes Start - Additional Fields For APP Orders
		String strForterMobileUID = "";
		String strMobileAppVersion = "";
		String strMobileDeviceBrand = "";
		String strMobileDeviceModel = "";
		String strMobileOSType = "";
		String strMerchantDeviceIdentifier = "";
		//EOMS-8616 Changes End
		
		Document outDoc = SCXmlUtil.createDocument(E_ORDER);
		Element order = inDoc.getDocumentElement();
		String orderNo = order.getAttribute(A_ORDER_NO);
		String strOrderHeaderKey=order.getAttribute(A_ORDER_HEADER_KEY);
		String strEnterpriseCode = order.getAttribute(CrocsConstant.A_ENTERPRISE_CODE);
		String strSuccess= order.getAttribute(C_SUCCESS);
		String strEntryType = order.getAttribute(A_ENTRY_TYPE);
		
		try {
		
	

		Element personBillToInfo = SCXmlUtil.getChildElement(order, E_PERSON_INFO_BILL_TO);
		Element personShipToInfo = SCXmlUtil.getChildElement(order, E_PERSON_INFO_SHIP_TO);

		Element eleChargeTransactionDetails = SCXmlUtil.getChildElement(order, E_CHARGE_TRANSACTION_DETAILS);
		Element eleChargeTransactionDetail = SCXmlUtil.getChildElement(eleChargeTransactionDetails,
				E_CHARGE_TRANSACTION_DETAIL);
		String strAuthorizationId ="";
		if(!YFCCommon.isVoid(eleChargeTransactionDetail))
			 strAuthorizationId = eleChargeTransactionDetail.getAttribute(A_AUTHORIZATION_ID);
		String strOriginalTotalAmount = order.getAttribute(A_ORIGINAL_TOTAL_AMOUNT);
		String strBillToEmailId = personBillToInfo.getAttribute(A_EMAIL_ID);
		
		//EOMS-8616 Changes Start - Additional Fields For APP Orders
		objCrocsPropertyEncrypterImpl = new CrocsPropertyEncrypterImpl();
		
		Element eleCrocsForterFields = SCXmlUtil.getXpathElement(order, "/Order/Extn/CrocsForterFieldsList/CrocsForterFields");
		if(!YFCCommon.isVoid(eleCrocsForterFields)) {
			logger.verbose("CrocsCheckFraudOnOrderUserExitImpl : processOrderFraudCheck : eleCrocsForterFields : " + SCXmlUtil.getString(eleCrocsForterFields));

			// Decrypt common Forter fields stored in the hangoff table.
			if(!YFCCommon.isVoid(eleCrocsForterFields.getAttribute(CrocsXmlConstants.A_CUSTOMER_IP_FORTER))) {
				strCustomerIP  = objCrocsPropertyEncrypterImpl.decrypt(eleCrocsForterFields.getAttribute(A_CUSTOMER_IP_FORTER));
			}
			
			if(!YFCCommon.isVoid(eleCrocsForterFields.getAttribute(CrocsXmlConstants.A_MERCHANT_DOMAIN_FORTER))) {
				strMerchantDomain = objCrocsPropertyEncrypterImpl.decrypt(eleCrocsForterFields.getAttribute(A_MERCHANT_DOMAIN_FORTER));
			}
			
			StringBuilder strCombinedForterTokenCookie = new StringBuilder();
			for(int forterTokenIndex = 1; forterTokenIndex <= 2; forterTokenIndex++) {
				if(!YFCCommon.isVoid(eleCrocsForterFields.getAttribute(CrocsXmlConstants.A_FORTER_TOKEN_COOKIE_FORTER + forterTokenIndex))) {
					strCombinedForterTokenCookie.append(eleCrocsForterFields.getAttribute(A_FORTER_TOKEN_COOKIE_FORTER + forterTokenIndex));
				}				
			}
			strForterTokenCookie = YFCCommon.isVoid(strCombinedForterTokenCookie.toString()) ?  "" : objCrocsPropertyEncrypterImpl.decrypt(strCombinedForterTokenCookie.toString());
			
			StringBuilder strEncryptedCombinedUserAgent = new StringBuilder();
			for(int userAgentIndex = 1; userAgentIndex <= 5; userAgentIndex++) {
				if(!YFCCommon.isVoid(eleCrocsForterFields.getAttribute(CrocsXmlConstants.A_USER_AGENT_FORTER + userAgentIndex))) {
					strEncryptedCombinedUserAgent.append(eleCrocsForterFields.getAttribute(A_USER_AGENT_FORTER + userAgentIndex));
				}				
			}
			strUserAgent = YFCCommon.isVoid(strEncryptedCombinedUserAgent.toString()) ? "" : objCrocsPropertyEncrypterImpl.decrypt(strEncryptedCombinedUserAgent.toString());
			
			// Decrypt APP-specific Forter fields (EOMS-8616).
			
			if(ENTRY_TYPE_APP.equalsIgnoreCase(strEntryType)) {
				if(!YFCCommon.isVoid(eleCrocsForterFields.getAttribute(A_FORTER_MOBILE_UID))) {
					strForterMobileUID = objCrocsPropertyEncrypterImpl.decrypt(eleCrocsForterFields.getAttribute(A_FORTER_MOBILE_UID));
				}
				if(!YFCCommon.isVoid(eleCrocsForterFields.getAttribute(A_MOBILE_APP_VERSION))) {
					strMobileAppVersion = objCrocsPropertyEncrypterImpl.decrypt(eleCrocsForterFields.getAttribute(A_MOBILE_APP_VERSION));
				}
				if(!YFCCommon.isVoid(eleCrocsForterFields.getAttribute(A_MOBILE_DEVICE_BRAND))) {
					strMobileDeviceBrand = objCrocsPropertyEncrypterImpl.decrypt(eleCrocsForterFields.getAttribute(A_MOBILE_DEVICE_BRAND));
				}
				if(!YFCCommon.isVoid(eleCrocsForterFields.getAttribute(A_MOBILE_DEVICE_MODEL))) {
					strMobileDeviceModel = objCrocsPropertyEncrypterImpl.decrypt(eleCrocsForterFields.getAttribute(A_MOBILE_DEVICE_MODEL));
				}
				if(!YFCCommon.isVoid(eleCrocsForterFields.getAttribute(A_MOBILE_OS_TYPE))) {
					strMobileOSType = objCrocsPropertyEncrypterImpl.decrypt(eleCrocsForterFields.getAttribute(A_MOBILE_OS_TYPE));
				}
				if(!YFCCommon.isVoid(eleCrocsForterFields.getAttribute(A_MERCHANT_DEVICE_IDENTIFIER))) {
					strMerchantDeviceIdentifier = objCrocsPropertyEncrypterImpl.decrypt(eleCrocsForterFields.getAttribute(A_MERCHANT_DEVICE_IDENTIFIER));
				}
			}

		}
		// EOMS-8616 Changes End
		
		// Custom Attribute
		if(SCXmlUtil.getChildElement(order, E_CUSTOM_ATTRIBUTES)!=null) {
			Element eleCustomAttributes = SCXmlUtil.getChildElement(order, E_CUSTOM_ATTRIBUTES);
			logger.verbose("CrocsCheckFraudOnOrderUserExitImpl : processOrderFraudCheck : eleCustomAttributes : " + SCXmlUtil.getString(eleCustomAttributes));

				if(!YFCCommon.isVoid(eleCustomAttributes.getAttribute(A_TEXT_2)))
					strCustomerIP = objCrocsPropertyEncrypterImpl.decrypt(eleCustomAttributes.getAttribute(A_TEXT_2));
				if(!YFCCommon.isVoid(eleCustomAttributes.getAttribute(A_TEXT_4)))
					strMerchantDomain = objCrocsPropertyEncrypterImpl.decrypt(eleCustomAttributes.getAttribute(A_TEXT_4));
				if(!YFCCommon.isVoid(eleCustomAttributes.getAttribute(A_TEXT_6)) || !YFCCommon.isVoid(eleCustomAttributes.getAttribute(A_TEXT_7)) 
                        || !YFCCommon.isVoid(eleCustomAttributes.getAttribute(A_TEXT_8)) || !YFCCommon.isVoid(eleCustomAttributes.getAttribute(A_TEXT_9))) {
					String strConcatCookie = eleCustomAttributes.getAttribute(A_TEXT_6) + eleCustomAttributes.getAttribute(A_TEXT_7) + eleCustomAttributes.getAttribute(A_TEXT_8) + eleCustomAttributes.getAttribute(A_TEXT_9);
					strForterTokenCookie = objCrocsPropertyEncrypterImpl.decrypt(strConcatCookie);
				}
				if(!YFCCommon.isVoid(eleCustomAttributes.getAttribute(A_TEXT_11)) || !YFCCommon.isVoid(eleCustomAttributes.getAttribute(A_TEXT_12)) 
                        || !YFCCommon.isVoid(eleCustomAttributes.getAttribute(A_TEXT_13)) || !YFCCommon.isVoid(eleCustomAttributes.getAttribute(A_TEXT_14))
                        || !YFCCommon.isVoid(eleCustomAttributes.getAttribute(A_TEXT_15))) {
					String strConcatUserAgent = eleCustomAttributes.getAttribute(A_TEXT_11) + eleCustomAttributes.getAttribute(A_TEXT_12) + eleCustomAttributes.getAttribute(A_TEXT_13) + eleCustomAttributes.getAttribute(A_TEXT_14)+ eleCustomAttributes.getAttribute(A_TEXT_15);
					strUserAgent = objCrocsPropertyEncrypterImpl.decrypt(strConcatUserAgent);
				}
			}
		
		
		// Create the main JSON object
		JSONObject jsonObject = new JSONObject();

		// cartItems Array
		JSONArray cartItemsArray = new JSONArray();

		// For getting the checkout time and timesentToForter
		long checkoutTime = System.currentTimeMillis() / 1000;
		long timeSentToForter = System.currentTimeMillis();
		Element eleOrderLines = (Element) inDoc.getElementsByTagName(E_ORDER_LINES).item(0);
		NodeList orderLineList = SCXmlUtil.getXpathNodes(eleOrderLines, E_ORDER_LINE);
		for (int i = 0; i < orderLineList.getLength(); i++) {
			Element orderLineElement = (Element) orderLineList.item(i);
			Element item = SCXmlUtil.getChildElement(orderLineElement, E_ITEM);
            Element lineOverallTotalsEle=SCXmlUtil.getChildElement(orderLineElement, A_LINE_OVERALL_TOTALS);
            logger.verbose("CrocsCheckFraudOnOrderUserExitImpl : processOrderFraudCheck :lineOverallTotalsEle" + SCXmlUtil.getString(lineOverallTotalsEle));
			strItemCostCurrency = item.getAttribute(A_COST_CURRENCY);
			String quantity = orderLineElement.getAttribute(A_ORDERED_QTY);
			strDeliveryMethod = orderLineElement.getAttribute(A_CARRIER_SERVICE_CODE);
			JSONObject cartItem = new JSONObject();
			JSONObject basicItemData = new JSONObject();
			basicItemData.put(A_NAME_F, item.getAttribute(A_ITEM_DESC));
			basicItemData.put(A_QUANTITY_F, Double.parseDouble(quantity));
			basicItemData.put(A_PRODUCTID_F, item.getAttribute(A_ITEM_ID));
			basicItemData.put(A_TYPE_F, TANGEABLE);
			basicItemData.put(A_CATEGORY_F, SCXmlUtil.getXpathAttribute(orderLineElement, CrocsConstant.STR_XPATH_EXTN_SAP_MATERIAL_GROUP));
			
            //EOMS-4572: Order item total calculation not correct for Forter
			Double lineTotal=Double.parseDouble(lineOverallTotalsEle.getAttribute(A_LINE_TOTAL));
			logger.verbose("CrocsCheckFraudOnOrderUserExitImpl : processOrderFraudCheck :lineTotal" + lineTotal);
			JSONObject price = new JSONObject();
			price.put(A_CURRENCY_F, item.getAttribute(A_COST_CURRENCY));
            String strAmountLocalCurrency=String.format("%.2f", lineTotal/Double.parseDouble(quantity));
			price.put(A_AMOUNT_LOCAL_CURRENCY,(strAmountLocalCurrency));
			basicItemData.put(A_PRICE_F, price);

			cartItem.put(BASICITEMDATA_F, basicItemData);
			cartItemsArray.put(cartItem);
		}
		jsonObject.put(CARTITEMS_F, cartItemsArray);

		// payment Array
		JSONArray paymentArray = new JSONArray();
		JSONObject payment = new JSONObject();
		JSONObject amount = new JSONObject();

		NodeList paymentMethodsList = SCXmlUtil.getChildElement(order, E_PAYMENT_METHODS)
				.getElementsByTagName(E_PAYMENT_METHOD);
		payment=(JSONObject) preparePaymentDetailsObject(paymentMethodsList, strAuthorizationId, strItemCostCurrency, strOriginalTotalAmount,
				payment, strBillToEmailId,strSuccess, personBillToInfo);

		amount.put(A_CURRENCY_F, strItemCostCurrency);
		amount.put(A_AMOUNT_LOCAL_CURRENCY, strOriginalTotalAmount);
		payment.put(A_AMOUNT, amount);

		// billingDetails
		JSONObject billingDetails = new JSONObject();
		JSONObject address = new JSONObject();
		address.put(A_ADDRESS1_F, personBillToInfo.getAttribute(A_ADDRESS_LINE_1));
		address.put(A_ADDRESS2_F, personBillToInfo.getAttribute(A_ADDRESS_LINE_2));
		address.put(A_ZIP_F, personBillToInfo.getAttribute(A_ZIP_CODE));
		address.put(A_CITY_F, personBillToInfo.getAttribute(A_CITY));
		//EOMS-4594 : Forter Changes : START
		address.put(A_REGION, personBillToInfo.getAttribute(A_STATE));
		//EOMS-4594: Forter Changes : END
		address.put(A_COUNTRY_F, personBillToInfo.getAttribute(A_COUNTRY));
		billingDetails.put(A_ADDRESS_F, address);

		// phone array
		JSONArray phoneArray = new JSONArray();
		JSONObject phone = new JSONObject();
		phone.put(A_PHONE_F, personBillToInfo.getAttribute(A_MOBILE_PHONE));
		phoneArray.put(phone);
		billingDetails.put(A_PHONE_F, phoneArray);

		// personalDetails
		JSONObject personalDetails = new JSONObject();
		personalDetails.put(A_FIRST_NAME_F, personBillToInfo.getAttribute(A_FIRST_NAME));
		personalDetails.put(A_LAST_NAME_F, personBillToInfo.getAttribute(A_LAST_NAME));
		personalDetails.put(A_EMAIL_F, personBillToInfo.getAttribute(A_EMAIL_ID));
		billingDetails.put(A_PERSONALDETAILS_F, personalDetails);
		payment.put(A_BILLING_DETAILS_F, billingDetails);
		paymentArray.put(payment);
		jsonObject.put(PAYMENT_F, paymentArray);

		// orderId
		jsonObject.put(A_ORDER_ID_F, order.getAttribute(A_ORDER_NO));

		if (strEntryType.equals(ORDERTYPE))
			jsonObject.put(A_ORDER_TYPE_F, ORDERTYPE);
		else if (strEntryType.equals(ENTRY_TYPE_CALL_CENTER))
			jsonObject.put(A_ORDER_TYPE_F, PHONE);
		else if(ENTRY_TYPE_APP.equalsIgnoreCase(strEntryType))
			jsonObject.put(A_ORDER_TYPE_F, ENTRY_TYPE_APP);

		// timeSentToForter
		jsonObject.put(A_TIME_SENT_TO_FORTER_F, timeSentToForter);

		// checkoutTime
		jsonObject.put(A_CHECKOUT_TIME_F, checkoutTime);
	
		// connectionInformation
		JSONObject connectionInfo = new JSONObject();
		connectionInfo.put(A_CUSTOMER_IP_F, strCustomerIP);
		connectionInfo.put(A_USER_AGENT_F, strUserAgent);
		connectionInfo.put(A_FORTER_TOKEN_COOKIE, strForterTokenCookie);
		jsonObject.put(A_CONNECTION_INFORMATION_F, connectionInfo);
		
		//EOMS-8616 Changes Start - Additional Fields For APP Orders
		if(ENTRY_TYPE_APP.equalsIgnoreCase(strEntryType)) {
			connectionInfo.put(STR_FORTER_MOBILE_UID, strForterMobileUID);
			connectionInfo.put(STR_MOBILE_APP_VERSION, strMobileAppVersion);
			connectionInfo.put(STR_MOBILE_DEVICE_BRAND, strMobileDeviceBrand);
			connectionInfo.put(STR_MOBILE_DEVICE_MODEL, strMobileDeviceModel);
			connectionInfo.put(STR_MOBILE_OS_TYPE, strMobileOSType);
			connectionInfo.put(STR_MERCHANT_DEVICE_IDENTIFIER, strMerchantDeviceIdentifier);
		}
		//EOMS-8616 Changes End	

		// totalAmount
		JSONObject totalAmount = new JSONObject();
		totalAmount.put(A_CURRENCY_F, strItemCostCurrency);
		totalAmount.put(A_AMOUNT_LOCAL_CURRENCY, strOriginalTotalAmount);
		jsonObject.put(A_TOTAL_AMOUNT_F, totalAmount);

		// accountOwner
		JSONObject accountOwner = new JSONObject();
		accountOwner.put(A_FIRST_NAME_F, personShipToInfo.getAttribute(A_FIRST_NAME));
		accountOwner.put(A_LAST_NAME_F, personShipToInfo.getAttribute(A_LAST_NAME));
		accountOwner.put(A_EMAIL_F, personShipToInfo.getAttribute(A_EMAIL_ID));
		jsonObject.put(A_ACCOUNT_OWNER_F, accountOwner);

		// additionalIdentifiers
		JSONObject additionalIdentifiers = new JSONObject();
		
		if (A_TRUE_STRING.equalsIgnoreCase(strSuccess))
			additionalIdentifiers.put(ORDER_SEGMENT, CROCS_NA);
		else 
			additionalIdentifiers.put(ORDER_SEGMENT, NO_DECISION);

		JSONObject merchant = new JSONObject();
		if(CROCS_US.equalsIgnoreCase(strEnterpriseCode)) {
			merchant.put(MERCHANT_ID, YFSSystem.getProperty(V_FORTER_MERCHANT_ID_CUS));
			merchant.put(MERCHANT_NAME, YFSSystem.getProperty(V_FORTER_MERCHANT_NAME_CUS));
		}else if(CROCS_CA.equalsIgnoreCase(strEnterpriseCode)) {
			merchant.put(MERCHANT_ID, YFSSystem.getProperty(V_FORTER_MERCHANT_ID_CCA));
			merchant.put(MERCHANT_NAME, YFSSystem.getProperty(V_FORTER_MERCHANT_NAME_CCA));
		}else if(HEYDUDE_US.equalsIgnoreCase(strEnterpriseCode)) {
			//EOMS-6092 : HeyDude Post Auth Validation: START
			merchant.put(MERCHANT_ID, YFSSystem.getProperty(V_FORTER_MERCHANT_ID_HDUS));
			merchant.put(MERCHANT_NAME, YFSSystem.getProperty(V_FORTER_MERCHANT_NAME_HDUS));
			//EOMS-6092 : HeyDude Post Auth Validation: END
		}else if(HEYDUDE_CA.equalsIgnoreCase(strEnterpriseCode)) {
			//EOMS-10589 : HeyDude Post Auth Validation: START
			merchant.put(MERCHANT_ID, YFSSystem.getProperty(V_FORTER_MERCHANT_ID_HDCA));
			merchant.put(MERCHANT_NAME, YFSSystem.getProperty(V_FORTER_MERCHANT_NAME_HDCA));
			//EOMS-10589 : HeyDude Post Auth Validation: END
		}
		else if(CROCS_AU.equalsIgnoreCase(strEnterpriseCode)) {
			//EOMS-8174 : CrocsAU Post Auth Validation: START
			merchant.put(MERCHANT_ID, YFSSystem.getProperty(V_FORTER_MERCHANT_ID_CAU));
			merchant.put(MERCHANT_NAME, YFSSystem.getProperty(V_FORTER_MERCHANT_NAME_CAU));
			//EOMS-8174 : Crocs AU Post Auth Validation: END
		}else if(CROCS_SG.equalsIgnoreCase(strEnterpriseCode)) {
			//EOMS-11644 : CrocsSG Post Auth Validation: START
			merchant.put(MERCHANT_ID, YFSSystem.getProperty(V_FORTER_MERCHANT_ID_CSG));
			merchant.put(MERCHANT_NAME, YFSSystem.getProperty(V_FORTER_MERCHANT_NAME_CSG));
			//EOMS-11644 : CrocsSG Post Auth Validation: END
			//EOMS-9722 : EMEA Post Auth Validation: START
		}else if(CROCS_DE.equalsIgnoreCase(strEnterpriseCode)) {
            merchant.put(MERCHANT_ID, YFSSystem.getProperty(V_FORTER_MERCHANT_ID_C_DE));
            merchant.put(MERCHANT_NAME, YFSSystem.getProperty(V_FORTER_MERCHANT_NAME_C_DE));
        }else if(CROCS_EU.equalsIgnoreCase(strEnterpriseCode)) {
            merchant.put(MERCHANT_ID, YFSSystem.getProperty(V_FORTER_MERCHANT_ID_C_EU));
            merchant.put(MERCHANT_NAME, YFSSystem.getProperty(V_FORTER_MERCHANT_NAME_C_EU));
        }else if(CROCS_FI.equalsIgnoreCase(strEnterpriseCode)) {
            merchant.put(MERCHANT_ID, YFSSystem.getProperty(V_FORTER_MERCHANT_ID_C_FI));
            merchant.put(MERCHANT_NAME,YFSSystem.getProperty(V_FORTER_MERCHANT_NAME_C_FI));
        }else if(CROCS_FR.equalsIgnoreCase(strEnterpriseCode)) {
            merchant.put(MERCHANT_ID, YFSSystem.getProperty(V_FORTER_MERCHANT_ID_C_FR));
            merchant.put(MERCHANT_NAME, YFSSystem.getProperty(V_FORTER_MERCHANT_NAME_C_FR));
        }else if(CROCS_GB.equalsIgnoreCase(strEnterpriseCode)) {
            merchant.put(MERCHANT_ID, YFSSystem.getProperty(V_FORTER_MERCHANT_ID_C_GB));
            merchant.put(MERCHANT_NAME, YFSSystem.getProperty(V_FORTER_MERCHANT_NAME_C_GB));
        }else if(CROCS_NL.equalsIgnoreCase(strEnterpriseCode)) {
            merchant.put(MERCHANT_ID, YFSSystem.getProperty(V_FORTER_MERCHANT_ID_C_NL));
            merchant.put(MERCHANT_NAME, YFSSystem.getProperty(V_FORTER_MERCHANT_NAME_C_NL));
        }else if(HEYDUDE_DE.equalsIgnoreCase(strEnterpriseCode)) {
            merchant.put(MERCHANT_ID, YFSSystem.getProperty(V_FORTER_MERCHANT_ID_HD_DE));
            merchant.put(MERCHANT_NAME, YFSSystem.getProperty(V_FORTER_MERCHANT_NAME_HD_DE));
        }else if(HEYDUDE_EU.equalsIgnoreCase(strEnterpriseCode)) {
            merchant.put(MERCHANT_ID, YFSSystem.getProperty(V_FORTER_MERCHANT_ID_HD_EU));
            merchant.put(MERCHANT_NAME, YFSSystem.getProperty(V_FORTER_MERCHANT_NAME_HD_EU));
        }else if(HEYDUDE_FR.equalsIgnoreCase(strEnterpriseCode)) {
            merchant.put(MERCHANT_ID, YFSSystem.getProperty(V_FORTER_MERCHANT_ID_HD_FR));
            merchant.put(MERCHANT_NAME, YFSSystem.getProperty(V_FORTER_MERCHANT_NAME_HD_FR));
        }else if(HEYDUDE_GB.equalsIgnoreCase(strEnterpriseCode)) {
            merchant.put(MERCHANT_ID, YFSSystem.getProperty(V_FORTER_MERCHANT_ID_HD_GB));
            merchant.put(MERCHANT_NAME, YFSSystem.getProperty(V_FORTER_MERCHANT_NAME_HD_GB));
        }
		//EOMS-9722 : EMEA Post Auth Validation: END

		merchant.put(A_MERCHANT_DOMAIN, strMerchantDomain);

		additionalIdentifiers.put(MERCHANT, merchant);
		jsonObject.put(ADDITIONAL_IDENTIFIERS, additionalIdentifiers);

		// primaryDeliveryDetails
		JSONObject primaryDeliveryDetails = new JSONObject();
		if(strDeliveryMethod!=null)
			primaryDeliveryDetails.put(A_DELIVERY_METHOD_F, strDeliveryMethod);
		else
			primaryDeliveryDetails.put(A_DELIVERY_METHOD_F, DELIVERY_METHOD);
		primaryDeliveryDetails.put(A_DELIVERY_TYPE_F, DELIVERYTYPE);
		jsonObject.put(A_PRIMARY_DELIVERY_DETAILS_F, primaryDeliveryDetails);

		// primaryRecipient
		JSONObject primaryRecipient = new JSONObject();
		JSONObject primaryPersonalDetails = new JSONObject();
		primaryPersonalDetails.put(A_FIRST_NAME_F, personShipToInfo.getAttribute(A_FIRST_NAME));
		primaryPersonalDetails.put(A_LAST_NAME_F, personShipToInfo.getAttribute(A_LAST_NAME));
		primaryPersonalDetails.put(A_EMAIL_F, personShipToInfo.getAttribute(A_EMAIL_ID));

		primaryRecipient.put(A_PERSONALDETAILS_F, primaryPersonalDetails);

		JSONObject primaryAddress = new JSONObject();
		primaryAddress.put(A_ADDRESS1_F, personShipToInfo.getAttribute(A_ADDRESS_LINE_1));
		primaryAddress.put(A_ADDRESS2_F, personShipToInfo.getAttribute(A_ADDRESS_LINE_2));
		primaryAddress.put(A_ZIP_F, personShipToInfo.getAttribute(A_ZIP_CODE));
		primaryAddress.put(A_CITY_F, personShipToInfo.getAttribute(A_CITY));
		//EOMS-4594 : Forter Changes : START
		primaryAddress.put(A_REGION, personShipToInfo.getAttribute(A_STATE));
		//EOMS-4594 : Forter Changes : END
		primaryAddress.put(A_COUNTRY_F, personShipToInfo.getAttribute(A_COUNTRY));

		primaryRecipient.put(A_ADDRESS_F, primaryAddress);

		JSONArray recipientPhoneArray = new JSONArray();
		JSONObject recipientPhone = new JSONObject();
		recipientPhone.put(A_PHONE_F, personShipToInfo.getAttribute(A_MOBILE_PHONE));
		recipientPhoneArray.put(recipientPhone);

		primaryRecipient.put(A_PHONE_F, recipientPhoneArray);

		jsonObject.put(A_PRIMARY_RECIPIENT_F, primaryRecipient);

		// authorizationStep
		String strPaymentStatus=order.getAttribute(A_PAYMENT_STATUS);
		if(AUTHORIZATION.equalsIgnoreCase(strPaymentStatus))
			jsonObject.put(A_AUT_TYPE_F, PRE_AUTHERIZATION);
		else
			jsonObject.put(A_AUT_TYPE_F, POST_AUTHERIZATION);

		// Print the final JSON
		logger.verbose("CrocsCheckFraudOnOrderUserExitImpl : processOrderFraudCheck : Forter input request json " + jsonObject.toString());
		logger.info("OMS_Forter_Update : CrocsCheckFraudOnOrderUserExitImpl : processOrderFraudCheck : Forter fraud check request" +  jsonObject.toString());
		// Send the request to Forter API
		HttpURLConnection connection = null;
		
			// Setup URL and connection
			String baseURL =YFCConfigurator.getInstance().getProperty(FORTER_API)+STR_ORDERS_F;
			String fullURL = baseURL + orderNo;

			URI uri = new URI(fullURL);
			URL url = uri.toURL();
			connection = (HttpURLConnection) url.openConnection();
			
			// EOMS-7592 – Updated HeyDude Forter configuration with new API key, site ID, and API version
			String strUserName="";
			String strForterSiteID="";
			String strApiVersion="";
			if(HEYDUDE_US.equalsIgnoreCase(strEnterpriseCode) || HEYDUDE_CA.equalsIgnoreCase(strEnterpriseCode)
				//EOMS-9722 : EMEA HEYDUDE Enterprises
                    || (strEnterpriseCode != null && CROCS_EMEA_ENTERPRISES.contains(strEnterpriseCode)
                    && strEnterpriseCode.startsWith("HEYDUDE_")))
			{
				strUserName=YFCConfigurator.getInstance().getProperty(HD_FORTER_API_KEY);
				strForterSiteID=YFCConfigurator.getInstance().getProperty(HD_FORTER_SITE_ID);
				strApiVersion=YFCConfigurator.getInstance().getProperty(HD_FORTER_API_VERSION);
			}
			else if(CROCS_AU.equalsIgnoreCase(strEnterpriseCode) 
					||CROCS_SG.equalsIgnoreCase(strEnterpriseCode))
			{
				strUserName=YFCConfigurator.getInstance().getProperty(CAU_FORTER_API_KEY);
				strForterSiteID=YFCConfigurator.getInstance().getProperty(CAU_FORTER_SITE_ID);
				strApiVersion=YFCConfigurator.getInstance().getProperty(CAU_FORTER_API_VERSION);
			}
			else
			{
				// Dynamically fetch the API key from SMA for the user name
				strUserName = YFCConfigurator.getInstance().getProperty(FORTER_API_KEY);
				// Dynamically fetch the Forter site ID from SMA
				strForterSiteID = YFCConfigurator.getInstance().getProperty(FORTER_SITE_ID);
				strApiVersion=YFCConfigurator.getInstance().getProperty(FORTER_API_VERSION);
			}
			String encodedAuth = Base64.getEncoder()
					.encodeToString((strUserName + ":").getBytes(StandardCharsets.UTF_8));
			connection.setRequestProperty(A_AUTHORIZATION, BASIC + encodedAuth);
			logger.verbose(" CrocsCheckFraudOnOrderUserExitImpl : processOrderFraudCheck : strUserName: " + strUserName);

			connection.setRequestProperty(FORTER_SITE_ID1, strForterSiteID);
			logger.verbose("CrocsCheckFraudOnOrderUserExitImpl : processOrderFraudCheck : strForterSiteID: " + strForterSiteID);

			connection.setRequestMethod(HTTP_POST_REQUEST);
			String strContentType=YFCConfigurator.getInstance().getProperty(A_CONTENT_TYPE);
			
			connection.setRequestProperty(CONTENT_TYPE, strContentType);
			connection.setRequestProperty(API_VERSION, strApiVersion);
			connection.setDoOutput(true);
			connection.connect();

			if (!YFCCommon.isVoid(jsonObject)) {
				String strPostBody = jsonObject.toString();
				byte[] postData = strPostBody.getBytes(StandardCharsets.UTF_8);
				OutputStream outputStream = connection.getOutputStream();
				outputStream.write(postData);
				outputStream.flush();
				logger.verbose("CrocsCheckFraudOnOrderUserExitImpl : processOrderFraudCheck :connection:outputStream:" + outputStream.toString());
			}

			// Get the response code and handle the response
			int responseCode = connection.getResponseCode();

			logger.verbose("CrocsCheckFraudOnOrderUserExitImpl : processOrderFraudCheck :Connection response: " + connection.getResponseMessage());
			
			// Read the response body
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			logger.verbose("CrocsCheckFraudOnOrderUserExitImpl : processOrderFraudCheck :BufferedReader" + in.toString());
			String inputLine;
			StringBuilder response = new StringBuilder();
			logger.verbose("CrocsCheckFraudOnOrderUserExitImpl : processOrderFraudCheck : StringBuilder :response" + response.toString());
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
				logger.verbose("CrocsCheckFraudOnOrderUserExitImpl : processOrderFraudCheck : inside while block :response" + response.toString());
			}
			// Print the response from the server
			logger.verbose("CrocsCheckFraudOnOrderUserExitImpl : processOrderFraudCheck : Response: " + response.toString());
			logger.info("OMS_Update : CrocsCheckFraudOnOrderUserExitImpl : processOrderFraudCheck : Forter fraud check response" + response.toString());
			if (responseCode == HttpURLConnection.HTTP_OK) {
				outDoc = verifyFraudValidation(response, outDoc);
				logger.verbose("OutPut from verifyFraudValidation :" + XMLUtil.getXMLString(outDoc));
				logger.info("OMS_Update : CrocsCheckFraudOnOrderUserExitImpl : processOrderFraudCheck : Success Response : "+ orderNo);

			} else {
				//EOMS-4261 START
				outDoc.getDocumentElement().setAttribute(A_FRAUD_CHECK_RESPONSE_CODE, V_FAILED);
				outDoc.getDocumentElement().setAttribute(A_IS_VALID_ORDER, FLAG_N);
				outDoc.getDocumentElement().setAttribute(A_RESPONSE, response.toString());
				String refernceValue=response.toString();
				
				// Raising alert
				createAlertOnOrder(env, orderNo,strOrderHeaderKey,strEnterpriseCode,refernceValue);
				logger.info("OMS_Update : CrocsCheckFraudOnOrderUserExitImpl : processOrderFraudCheck : Fail Response : "+ orderNo);
				logger.verbose("Request failed with response code: " + responseCode);
			}
		} catch (Exception e) {
			logger.info("OMS_Update : CrocsCheckFraudOnOrderUserExitImpl : processOrderFraudCheck : Forter Connection Exception: "+ orderNo);
			logger.verbose("Execption connection: " + e.getMessage());
			outDoc.getDocumentElement().setAttribute(A_FRAUD_CHECK_RESPONSE_CODE, V_FAILED);
			outDoc.getDocumentElement().setAttribute(A_IS_EXCEPTION_ORDER, FLAG_Y);
			String refernceValue=e.getMessage();
			// Raising alert
			createAlertOnOrder(env, orderNo,strOrderHeaderKey,strEnterpriseCode,refernceValue);
			
			//EOMS-4843 : Fraud check changes :: START
			throw new YFSException(CrocsErrorConstants.VAL_ERROR_DESCRIPTION_FORTER, CrocsErrorConstants.VAL_ERROR_CODE_EXTN_003,CrocsErrorConstants.VAL_ERROR_DESCRIPTION_EXTN_003);
			//EOMS-4843 : Fraud check changes :; END	
			
			//EOMS-4261 END
		}

		logger.verbose("CrocsCheckFraudOnOrderUserExitImpl : processOrderFraudCheck : Output:" + XMLUtil.getXMLString(inDoc));
		
		return outDoc;
	}

	public void createAlertOnOrder(YFSEnvironment env, String orderNo, String strOrderHeaderKey,
			String strEnterpriseCode, String refernceValue)  {
		if(refernceValue.length()>1000)
			refernceValue=refernceValue.substring(0, 1000);
		Document createExceptionIndoc = SCXmlUtil.createDocument(A_INBOX);
		
		createExceptionIndoc.getDocumentElement().setAttribute(A_EXCEPTION_TYPE, STR_CROCS_FORTER_REQUEST_EXP);
		createExceptionIndoc.getDocumentElement().setAttribute(A_DESCRIPTION, STR_CROCS_FORTER_REQUEST_EXP);
		createExceptionIndoc.getDocumentElement().setAttribute(A_DETAIL_DESCRIPTION, refernceValue);
		createExceptionIndoc.getDocumentElement().setAttribute(OrderHeaderKey, strOrderHeaderKey);
		createExceptionIndoc.getDocumentElement().setAttribute(OrderNo, orderNo);
		createExceptionIndoc.getDocumentElement().setAttribute(A_ENTERPRISE_KEY, strEnterpriseCode);
		Element eleInboxReferencesList = SCXmlUtil.createChild(createExceptionIndoc.getDocumentElement(),
				A_INBOX_REFERENCES_LIST);
		Element eleInboxReferences = SCXmlUtil.createChild(eleInboxReferencesList, A_INBOX_REFERENCES);
		eleInboxReferences.setAttribute(A_NAME, FORTER_EXCEPTION);
		eleInboxReferences.setAttribute(A_VALUE, refernceValue);
		eleInboxReferences.setAttribute(A_REFERNCE_TYPE, STR_TEXT);
		logger.verbose("CrocsCheckFraudOnOrderUserExitImpl : processOrderFraudCheck : Rasing alert for createException Input:- " + SCXmlUtil.getString(createExceptionIndoc));
		try {
			CommonUtil.invokeAPI(env, "", API_CREATE_EXCEPTION, createExceptionIndoc);
		} catch (Exception e) {
			e.printStackTrace();
		}


		
	}

	private Object preparePaymentDetailsObject(NodeList paymentMethodsList, String strAuthorizationId,
			String strItemCostCurrency, String strOriginalTotalAmount, JSONObject payment, String strBillToEmailId, String strSuccess, Element personBillToInfo) {
		for (int j = 0; j < paymentMethodsList.getLength(); j++) {
			Element paymentMethodEle = (Element) paymentMethodsList.item(j);
			String paymentType = paymentMethodEle.getAttribute(A_PAYMENT_TYPE);
			
			//EOMS-4594 : Forter Changes : START
			if(YFCCommon.isVoid(strAuthorizationId) || strAuthorizationId==null)
				strAuthorizationId = paymentMethodEle.getAttribute("PaymentReference5");
			//EOMS-4594 : Forter Changes : END
			
			if (P_CREDIT_CARD.equalsIgnoreCase(paymentType)) {
				// // creditCard details

				JSONObject creditCardDetails = new JSONObject();

				String creditCardNumber = paymentMethodEle.getAttribute(A_CREDIT_CARD_NO);

				// Extract the first 6 digits of the credit card number
				String bin = creditCardNumber.substring(0, 6);

				creditCardDetails.put(A_NAME_ON_CARD_F, paymentMethodEle.getAttribute(A_CREDIT_CARD_NAME));
				creditCardDetails.put(A_LAST_FOUR_DIGITS_F, paymentMethodEle.getAttribute(A_DISPLAY_CREDIT_CARD_NO));
				creditCardDetails.put(A_EXP_MONTH_F,
						paymentMethodEle.getAttribute(A_CREDIT_CARD_EXP_DATE).split("/")[0]);
				creditCardDetails.put(A_EXP_YEAR_F,
						paymentMethodEle.getAttribute(A_CREDIT_CARD_EXP_DATE).split("/")[1]);
				creditCardDetails.put(A_BIN_F, bin);
				creditCardDetails.put(A_CARD_TYPE_F, CARDTYPE);
				payment.put(A_CREDIT_CARD_F, creditCardDetails);

			} else if (STR_AFTER_PAY.equalsIgnoreCase(paymentType)) {

				// AfterPay details
				JSONObject cardDetails = new JSONObject();
				cardDetails.put(F_SERVICE_NAME, AFTER_PAY);
				cardDetails.put(F_PAYMENT_ID, strAuthorizationId);
				
				if (!A_TRUE_STRING.equalsIgnoreCase(strSuccess)) {
					cardDetails.put(A_FIRST_NAME_F, personBillToInfo.getAttribute(A_FIRST_NAME));
					cardDetails.put(A_LAST_NAME_F, personBillToInfo.getAttribute(A_LAST_NAME));
					cardDetails.put(A_SERVICE_RESPONSE_CODE_F, DECLINED);
				}
				
				payment.put(F_INSTALLMENT_SERVICE, cardDetails);

			} else if (P_APPLE_PAY.equalsIgnoreCase(paymentType)) {
				// // creditCard details
				JSONObject applePayDetails = new JSONObject();

				String creditCardNumber = paymentMethodEle.getAttribute(A_CREDIT_CARD_NO);

				// Extract the first 6 digits of the credit card number
				String bin = creditCardNumber.substring(0, 6);

				applePayDetails.put(A_NAME_ON_CARD_F, paymentMethodEle.getAttribute(A_CREDIT_CARD_NAME));
				applePayDetails.put(A_LAST_FOUR_DIGITS_F, paymentMethodEle.getAttribute(A_DISPLAY_CREDIT_CARD_NO));
				applePayDetails.put(A_EXP_MONTH_F, paymentMethodEle.getAttribute(A_CREDIT_CARD_EXP_DATE).split("/")[0]);
				applePayDetails.put(A_EXP_YEAR_F, paymentMethodEle.getAttribute(A_CREDIT_CARD_EXP_DATE).split("/")[1]);
				applePayDetails.put(A_BIN_F, bin);

				payment.put(F_APPLE_PAY, applePayDetails);

			} else if (P_PAYPAL.equalsIgnoreCase(paymentType)) {
				//EOMS-4594 : Forter Changes : START
				Element elePersonInfoBillTo = SCXmlUtil.getChildElement(paymentMethodEle, E_PERSON_INFO_BILL_TO);
				JSONObject paypalDetails = new JSONObject();
				JSONObject paymentGateWayData = new JSONObject();
				paymentGateWayData.put(F_GATEWAY_NAME, F_ADYEN);
				paymentGateWayData.put(F_GATEWAY_TRANSACTION_ID, strAuthorizationId);
				paypalDetails.put(F_PAYMENT_GATEWAY_DATA, paymentGateWayData);
				paypalDetails.put(F_AUTHORIZATION_ID, strAuthorizationId);
				paypalDetails.put(F_PAYMENT_ID, strAuthorizationId);
                paypalDetails.put(F_PAYER_STATUS, F_VERIFIED);
				String strPayerId = paymentMethodEle.getAttribute(PaymentReference7);
				paypalDetails.put(F_PAYER_ID, strPayerId);
				//EOMS-4595 : If Payment Emaild is not present taking Order BillTo EmailId
				if(elePersonInfoBillTo.getAttribute(A_EMAIL_ID)!=null && !YFCCommon.isVoid(elePersonInfoBillTo.getAttribute(A_EMAIL_ID))) {
					paypalDetails.put(F_PAYER_EMAIL, elePersonInfoBillTo.getAttribute(A_EMAIL_ID));
				}else {
				paypalDetails.put(F_PAYER_EMAIL, strBillToEmailId);
                }
				
				if (A_TRUE_STRING.equalsIgnoreCase(strSuccess))
					paypalDetails.put(F_PAYMENT_STATUS, F_AUTHORIZED);
				else
					paypalDetails.put(F_PAYMENT_STATUS, F_FAILED);
 
                
				paypalDetails.put(A_ADYEN_PAYMENT_METHOD, F_PAYPAL_STANDARD);
				paypalDetails.put(F_PAYER_ACCOUNT_COUNTRY, elePersonInfoBillTo.getAttribute(A_COUNTRY));
				payment.put(F_PAYPAL, paypalDetails);
                //EOMS-4594 : Forter Changes : END
			} else if (STR_CASH_APP.equalsIgnoreCase(paymentType)) {

				JSONObject digitalWallet = new JSONObject();
				String strPayerId = paymentMethodEle.getAttribute(A_PAYMENT_REFERENCE1);
				digitalWallet.put(F_DIGITAL_WALLET_NAME, F_DIGITAL_WALLET_NAME_TYPE);
				digitalWallet.put(F_DIGITAL_WALLET_PAYER_ID, strPayerId);
				digitalWallet.put(F_FREE_TEXT_DIGITAL_WALLET_NAME, F_FREE_TEXT_DIGITAL_WALLET_NAME_TYPE);

				JSONObject underlyingPaymentMethod = new JSONObject();
				underlyingPaymentMethod.put(F_UNDERLYING_PAYMENT_METHOD_TYPE, F_UNDERLYING_PAYMENT_METHOD_TYPE_VALUE);

				digitalWallet.put(F_UNDERLYING_PAYMENT_METHOD, underlyingPaymentMethod);

				payment.put(F_DIGITAL_WALLET, digitalWallet);

			} else if (A_GIVEX.equalsIgnoreCase(paymentType)) {
				JSONObject giftCard = new JSONObject();
				String strSvcNo = paymentMethodEle.getAttribute(SvcNo);
				giftCard.put(F_CREDIT_CURRENCY, strItemCostCurrency);
				giftCard.put(F_MERCHANT_PAYMENT_ID, strSvcNo);

				JSONObject value = new JSONObject();
				value.put(A_CURRENCY_F, strItemCostCurrency);
				value.put(A_AMOUNT_LOCAL_CURRENCY, strOriginalTotalAmount);
				payment.put(F_GIFT_CARD, giftCard);
			}else if (A_AMAZON_PAY.equalsIgnoreCase(paymentType)) {
				// AmazonPay details
				JSONObject amazonPay = new JSONObject();
				amazonPay.put(F_DIGITAL_WALLET_NAME, F_AMAZON_PAY);
				amazonPay.put(F_AUTHORIZATION_ID, strAuthorizationId);
				JSONObject underlyingPaymentMethod = new JSONObject();
				underlyingPaymentMethod.put(F_UNDERLYING_PAYMENT_METHOD_TYPE, F_UNDERLYING_PAYMENT_METHOD_TYPE_VALUE);
				amazonPay.put(F_UNDERLYING_PAYMENT_METHOD, underlyingPaymentMethod);
				
				amazonPay.put(STR_FREE_TEXT_DIGITAL_WALLET_NAME, F_AMAZON_PAY);
				
				JSONObject paymentAdyenGateWay = new JSONObject();
				paymentAdyenGateWay.put(F_GATEWAY_NAME, F_ADYEN);
				paymentAdyenGateWay.put(F_GATEWAY_TRANSACTION_ID, strAuthorizationId);
				amazonPay.put(F_PAYMENT_GATEWAY_DATA, paymentAdyenGateWay);
				
				payment.put(F_DIGITAL_WALLET, amazonPay);
				
			}else if (A_PAY_WITH_GOOGLE.equalsIgnoreCase(paymentType)) {
				
				// Google Pay details
				JSONObject googlePay = new JSONObject();
				googlePay.put(A_CARD_TYPE_F, CARDTYPE);
				
				JSONObject paymentGatewayData = new JSONObject();
				paymentGatewayData.put(F_GATEWAY_NAME, F_ADYEN);
				paymentGatewayData.put(F_GATEWAY_TRANSACTION_ID, strAuthorizationId);
				googlePay.put(F_PAYMENT_GATEWAY_DATA, paymentGatewayData);
				
				JSONObject paymentProcessorData = new JSONObject();
				paymentProcessorData.put(F_PROCESSOR_NAME, F_ADYEN);
				paymentProcessorData.put(F_PROCESSOR_TRANSACTION_ID, strAuthorizationId);
				googlePay.put(F_PAYMENT_PROCESSOR_DATA, paymentProcessorData);
				
				payment.put(F_ANDROID_PAY, googlePay);
			
			}
		}
		return payment;

	}

	/*
	 * / This method verifies the fraud validation response after getting the
	 * response from forter
	 */
	private static Document verifyFraudValidation(StringBuilder response, Document outDoc) {

		logger.verbose("CrocsCheckFraudOnOrderUserExitImpl : verifyFraudValidation :Response document in verifyFraudValidation:" + XMLUtil.getXMLString(outDoc));
		Element eleOrder = outDoc.getDocumentElement();

		logger.verbose("\"CrocsCheckFraudOnOrderUserExitImpl : verifyFraudValidation response string:" + response.toString());
		// retrieve values
		JSONObject jsonObject = new JSONObject(response.toString());
		logger.verbose("\"CrocsCheckFraudOnOrderUserExitImpl : verifyFraudValidation jsonObject string:" + jsonObject.toString());

		String status = jsonObject.getString(STATUS);
		logger.verbose("CrocsCheckFraudOnOrderUserExitImpl :verifyFraudValidation: status:" + status);
		String strAction = jsonObject.getString(ACTION);
		logger.verbose("CrocsCheckFraudOnOrderUserExitImpl :verifyFraudValidation: strAction:" + eleOrder);
		String strMessage = jsonObject.getString(MESSAGE);
		logger.verbose("CrocsCheckFraudOnOrderUserExitImpl : verifyFraudValidation: strMessage:" + eleOrder);
		logger.info("CrocsCheckFraudOnOrderUserExitImpl : verifyFraudValidation : status: "+status+" and strAction: "+strAction);
		// create element for fraud message to append the input document
		Element eleFruadCheckResponseMessages = SCXmlUtil.createChild(eleOrder, E_FRAUD_CHECK_RESPONSE_MESSAGES);
		Element eleFruadCheckResponseMessage = SCXmlUtil.createChild(eleFruadCheckResponseMessages,
				E_FRAUD_CHECK_RESPONSE_MESSAGE);
		eleFruadCheckResponseMessage.setAttribute(A_TEXT, strMessage);

		logger.verbose("CrocsCheckFraudOnOrderUserExitImpl : verifyFraudValidation : fruadCheckResponseMessages:" + eleFruadCheckResponseMessage);

		if ((strAction.equals(APPROVE) ||strAction.equals(STR_NOT_REVIEWED_F)  ) && status.equals(SUCCESS)) {
			eleOrder.setAttribute(A_FRAUD_CHECK_RESPONSE_CODE, V_SUCCESS);
		} else {
			eleOrder.setAttribute(A_FRAUD_CHECK_RESPONSE_CODE, V_FAILED);

		}
		logger.verbose("CrocsCheckFraudOnOrderUserExitImpl : verifyFraudValidation : Output : " + XMLUtil.getXMLString(outDoc));

		return outDoc;
	}
}