package com.crocs.oms.util.ue;

import com.crocs.oms.order.CrocsProcessAdyenWebhooks;
import com.yantra.yfs.japi.YFSException;
import org.apache.commons.json.JSONException;
import org.apache.commons.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.crocs.oms.common.util.*;
import com.crocs.oms.order.migration.CrocsMigarationUtil;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSExtnPaymentCollectionInputStruct;
import com.yantra.yfs.japi.YFSExtnPaymentCollectionOutputStruct;
import com.yantra.yfs.japi.YFSUserExitException;
import com.yantra.yfs.japi.ue.YFSCollectionCreditCardUE;
import com.sterlingcommerce.baseutil.SCXmlUtil;

import java.rmi.RemoteException;

/*
 * Implemented Credit Card UE for capturing the Credit Card payments
 * 
 */
public class CrocsCollectionCreditCardWrapper extends CrocsProcessAdyenWebhooks implements CrocsConstant, YFSCollectionCreditCardUE {


	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsCollectionCreditCardWrapper.class);
	YFSExtnPaymentCollectionOutputStruct outStruct = new YFSExtnPaymentCollectionOutputStruct();

	public YFSExtnPaymentCollectionOutputStruct collectionCreditCard(YFSEnvironment env,YFSExtnPaymentCollectionInputStruct inStruct) throws YFSUserExitException {
		logger.beginTimer("CollectionCreditCardWrapper.collectionCreditCard() : Begin");
		logger.verbose("Starting CollectionCreditCardWrapper.collectionCreditCard() method");

		String strMerchantAccount = null;
		double dValue = Math.abs(inStruct.requestAmount);

		// coverting into decimal value expected by Adyen input for Adyen Tenders
		dValue = dValue * Math.pow(10, 2);
		
		int iValue = (int) Math.round(dValue);

		if (inStruct.enterpriseCode.equals(CrocsConstant.CROCS_US)) {
			strMerchantAccount = CrocsConstant.A_CROCS_US;
		}
		if (inStruct.enterpriseCode.equals(CrocsConstant.CROCS_CA)) {
			strMerchantAccount = CrocsConstant.A_CROCS_CA;
		}
		//EOMS-6194 : Adyen Payment Capture : START
		if (CrocsConstant.HEYDUDE_US.equals(inStruct.enterpriseCode)) {
			strMerchantAccount = CrocsConstant.A_HEYDUDE_US;
		}
		//EOMS-6194 : Adyen Payment Capture : END

		if (inStruct.enterpriseCode.equals(CrocsConstant.CROCS_AU)) {
			strMerchantAccount = CrocsConstant.A_ADYEN_MERCHANT_ACCOUNT_CAU;
		}
		
		//EOMS-11176 : CROCS SG Adyen Payment Capture : START
		if (CrocsConstant.CROCS_SG.equalsIgnoreCase(inStruct.enterpriseCode)) {
			strMerchantAccount = CrocsConstant.A_ADYEN_MERCHANT_ACCOUNT_CSG;
		}
		//EOMS-11176 : CROCS SG Adyen Payment Capture : END
		
		//EOMS-10595 : HeyDude CA Adyen Payment Capture : START
		if (CrocsConstant.HEYDUDE_CA.equalsIgnoreCase(inStruct.enterpriseCode)) {
			strMerchantAccount = CrocsConstant.A_ADYEN_MERCHANT_ACCOUNT_HCA;
		}
		//EOMS-10595 : HeyDude CA Adyen Payment Capture : END

		//EOMS-9722 : EMEA Adyen Payment Capture : START
        if (CrocsConstant.HEYDUDE_DE.equalsIgnoreCase(inStruct.enterpriseCode)) {
            strMerchantAccount = CrocsConstant.A_ADYEN_MERCHANT_ACCOUNT_HD_DE;
        }
        if (CrocsConstant.HEYDUDE_EU.equalsIgnoreCase(inStruct.enterpriseCode)) {
            strMerchantAccount = CrocsConstant.A_ADYEN_MERCHANT_ACCOUNT_HD_EU;
        }
        if (CrocsConstant.HEYDUDE_FR.equalsIgnoreCase(inStruct.enterpriseCode)) {
            strMerchantAccount = CrocsConstant.A_ADYEN_MERCHANT_ACCOUNT_HD_FR;
        }
        if (CrocsConstant.HEYDUDE_GB.equalsIgnoreCase(inStruct.enterpriseCode)) {
            strMerchantAccount = CrocsConstant.A_ADYEN_MERCHANT_ACCOUNT_HD_GB;
        }
        if (CrocsConstant.CROCS_DE.equalsIgnoreCase(inStruct.enterpriseCode)) {
            strMerchantAccount = CrocsConstant.A_ADYEN_MERCHANT_ACCOUNT_C_DE;
        }
        if (CrocsConstant.CROCS_EU.equalsIgnoreCase(inStruct.enterpriseCode)) {
            strMerchantAccount = CrocsConstant.A_ADYEN_MERCHANT_ACCOUNT_C_EU;
        }
        if (CrocsConstant.CROCS_FI.equalsIgnoreCase(inStruct.enterpriseCode)) {
            strMerchantAccount = CrocsConstant.A_ADYEN_MERCHANT_ACCOUNT_C_FI;
        }
        if (CrocsConstant.CROCS_FR.equalsIgnoreCase(inStruct.enterpriseCode)) {
            strMerchantAccount = CrocsConstant.A_ADYEN_MERCHANT_ACCOUNT_C_FR;
        }
        if (CrocsConstant.CROCS_NL.equalsIgnoreCase(inStruct.enterpriseCode)) {
            strMerchantAccount = CrocsConstant.A_ADYEN_MERCHANT_ACCOUNT_C_NL;
        }
        if (CrocsConstant.CROCS_GB.equalsIgnoreCase(inStruct.enterpriseCode)) {
            strMerchantAccount = CrocsConstant.A_ADYEN_MERCHANT_ACCOUNT_C_GB;
        }
		//EOMS-9722 : EMEA Adyen Payment Capture : END

		@SuppressWarnings("null")
		JSONObject jsonPayloadObj = new JSONObject();
		JSONObject jsonAmountObj = new JSONObject();
		JSONObject jsonOrderObj = new JSONObject();
		String reqPayload = null;
		String strAdyenRequest = "";

		
		if (!CrocsMigarationUtil.isImportOrderEligibleForPayment(env, inStruct.orderHeaderKey)) {

			/* Restricting migration Order for payment agent to pick */
			outStruct.holdOrderAndRaiseEvent = CrocsConstant.A_TRUE;
			outStruct.holdReason = CrocsConstant.STR_MIGRATION_PAY_HOLD;

			return outStruct;
		} else if ((CrocsConstant.CHARGETYPE_CHARGE.equals(inStruct.chargeType) && inStruct.requestAmount > 0)) {
			
			strAdyenRequest = CrocsXmlConstants.A_EVENT_CODE_CAPTURE;
		
		} else if ((CrocsConstant.CHARGETYPE_CHARGE.equals(inStruct.chargeType) && inStruct.requestAmount < 0)) {
		
			strAdyenRequest = CrocsXmlConstants.A_EVENT_CODE_REFUND;
		
		} else if (CrocsConstant.AUTHORIZATION.equals(inStruct.chargeType) && inStruct.requestAmount > 0) {

			suspendPaymentMethod(inStruct);
			return outStruct;
		}

		logger.verbose("Creating input to call Adyen from instruct:: for CHARGE");
		String strResponse = null;
		try {
			String pspreference = inStruct.authorizationId;
			if (YFCCommon.isVoid(pspreference))
				pspreference = inStruct.paymentReference5;

			jsonAmountObj.put(CrocsConstant.POSTMEN_CURRENCY, inStruct.currency);
			jsonAmountObj.put(CrocsConstant.VALUE, iValue);
			jsonOrderObj.put(CrocsConstant.POSTMEN_AMOUNT, jsonAmountObj);
			jsonOrderObj.put(CrocsConstant.ADYEN_MERCHANT_ACCOUNT, strMerchantAccount);
			//EOMS -3698 pass Order No to Adyen in MerchantReference instead of ChargeTransactionKey
			jsonOrderObj.put(CrocsConstant.reference, inStruct.orderNo);
			jsonPayloadObj.put(CrocsConstant.STR_BODY, jsonOrderObj);
			jsonPayloadObj.put(CrocsConstant.ADYEN_PSP_REFERENCE, pspreference);
			reqPayload = jsonPayloadObj.toString();
			logger.verbose("Request Json Payload for adyen capture call:" + reqPayload);

		} catch (JSONException e) {

			outStruct.holdOrderAndRaiseEvent = CrocsConstant.A_TRUE;
			outStruct.holdReason = CrocsConstant.STR_PAYMENT_ERROR_HOLD;
			//EOMS-4472: Payment Alerts : START
			outStruct.internalReturnMessage = CrocsConstant.STR_PAYMENT_ERROR_HOLD;
			//EOMS-4472: Payment Alerts : END
			logger.verbose("Json Error Message" + e.getMessage());
		}

		Document docAdyenInput = SCXmlUtil.createDocument("Request");
		Element inDocEle = docAdyenInput.getDocumentElement();
		if (!YFCObject.isVoid(reqPayload)) {
			inDocEle.setAttribute(CrocsConstant.STR_ADEYN_REQUEST_PAYLOAD, reqPayload);
			inDocEle.setAttribute(CrocsConstant.STR_ADYEN_REQUEST, strAdyenRequest);
			logger.verbose("Request Document Payload for adyen capture call:" + XMLUtil.getXMLString(docAdyenInput));

		}

		CrocsAdyenCaptureRequest adyenCaptureObj = new CrocsAdyenCaptureRequest();
		try {
			strResponse = adyenCaptureObj.crocsAdyenCaptureRequest(docAdyenInput);
		} catch (Exception e) {
			
			/**
			 * Applying the Payment hold as payment gateway will rejected the request
			 **/
			outStruct.holdOrderAndRaiseEvent = CrocsConstant.A_TRUE;
			outStruct.holdReason = CrocsConstant.STR_PAYMENT_ERROR_HOLD;
			//EOMS-4472: Payment Alerts : START
			outStruct.internalReturnMessage = CrocsConstant.STR_PAYMENT_ERROR_HOLD;
			//EOMS-4472: Payment Alerts : END
			logger.verbose("CollectionCreditCardWrapper.collectionCreditCard :Expection" + e.getMessage());
		}

		try {
			JSONObject jsonObject = null;
			String status = null;
			
			if(strResponse != null && strResponse.contains(CrocsConstant.STATUS)) {
				jsonObject = new JSONObject(strResponse);
				status = jsonObject.getString(CrocsConstant.STATUS);
				
				if (status != null && status.equalsIgnoreCase(CrocsConstant.ADYEN_STATUS_RECEIVED)) {
					logger.verbose("ADYEN PAYMENT RESPONSE WAS SUCCESSFUL");
					double value = jsonObject.getJSONObject(CrocsXmlConstants.A_AMOUNT).getInt(CrocsConstant.VALUE);
					value = value * Math.pow(10, -2);
					logger.debug("amount collected from adyen" + value);

					outStruct.authorizationAmount = value;
					outStruct.tranAmount = value;
					outStruct.retryFlag = CrocsXmlConstants.FLAG_N;
					outStruct.tranType = CrocsConstant.CHARGETYPE_CHARGE;
					outStruct.holdOrderAndRaiseEvent = CrocsConstant.A_FALSE;
					if (CrocsXmlConstants.A_EVENT_CODE_REFUND.equalsIgnoreCase(strAdyenRequest))
						outStruct.authorizationAmount = -value;
					//insert capture details in CROCS_ADYEN_WEBHOOKS_RES
					createCaptureInfo(env, inStruct, jsonObject);
				}
				/** If there is any issue with the adyen URL */
				else if (strResponse!=null && strResponse.contains(STR_NOT_PRESENT)) {
					/* */
					outStruct.holdOrderAndRaiseEvent = CrocsConstant.A_TRUE;
					outStruct.holdReason = CrocsConstant.STR_PAYMENT_ERROR_HOLD;
					//EOMS-4472: Payment Alerts : START
					outStruct.internalReturnMessage = CrocsConstant.STR_PAYMENT_ERROR_HOLD;
					//EOMS-4472: Payment Alerts : END
				}
				
				else {
					
					/**
					 * Applying the Payment hold as payment gateway is rejected the request Below
					 **/
					outStruct.holdOrderAndRaiseEvent = CrocsConstant.A_TRUE;
					outStruct.holdReason = CrocsConstant.STR_PAYMENT_REJECT_HOLD;
					//EOMS-4472: Payment Alerts : START
					outStruct.internalReturnMessage = CrocsConstant.STR_PAYMENT_REJECT_HOLD;
					//EOMS-4472: Payment Alerts : END
					//insert capture details in CROCS_ADYEN_WEBHOOKS_RES
					createCaptureInfo(env, inStruct, jsonObject);
					logger.verbose(
							"CollectionCreditCardWrapper.collectionCreditCard: payment_gateway_rejected_response_Expection ");				
				}
			}
			

		} catch (Exception e) {
			logger.verbose("CollectionCreditCardWrapper.collectionCreditCard() Error :" + e.getMessage());

		}

		logger.beginTimer("CollectionCreditCardWrapper.collectionCreditCard() : End");
		logger.verbose("outStruct for  CollectionCreditCardWrapper.collectionCreditCard() :" + outStruct);
		return outStruct;
	}

	/** EOMS 3698 -> this method is used to insert capture Info details in CROCS_ADYEN_WEBHOOKS_RES table
	 * @param env env
	 * @param inStruct inStruct
	 * @param jsonObject jsonObject
	 * @throws JSONException Exception
	 * @throws RemoteException Exception
	 */
	private static void createCaptureInfo(YFSEnvironment env, YFSExtnPaymentCollectionInputStruct inStruct, JSONObject jsonObject){
		try {
			if (jsonObject.get(CrocsConstant.ADYEN_PSP_REFERENCE).toString() != null) {
				String orderNo= inStruct.orderNo;
				Document createCaptureIndoc = SCXmlUtil.createDocument(C_CAPTURE_INFO);
				Element createCaptureEle = createCaptureIndoc.getDocumentElement();
				createCaptureEle.setAttribute(ChargeTransactionKey, inStruct.chargeTransactionKey);
				createCaptureEle.setAttribute(OrderHeaderKey, inStruct.orderHeaderKey);
				createCaptureEle.setAttribute(DocumentType, inStruct.documentType);
				createCaptureEle.setAttribute(C_PSPREFERENCE, jsonObject.getString(CrocsConstant.ADYEN_PSP_REFERENCE));
				if(A_RETURN_ORDER_DOCUMENT_TYPE.equalsIgnoreCase(inStruct.documentType)){
					String strOrderNo = getSalesOrderNo(env, orderNo);
					createCaptureEle.setAttribute(ORDERNO,strOrderNo);
				}
				else{
					createCaptureEle.setAttribute(ORDERNO,orderNo);
				}
				logger.verbose("createCaptureIndoc" + createCaptureIndoc);
				Document createCaptureInfo = CommonUtil.invokeService(env, CrocsIVAPIConstants.CREATE_CAPTURE_INFO_SERVICE, createCaptureIndoc);
				logger.verbose("createCaptureInfo outdoc" + createCaptureInfo);
			}
		} catch(Exception e) {
			logger.verbose("Error in createCaptureInfo outdoc: "
					+ e.getLocalizedMessage());
			throw new YFSException(" Error in createCaptureInfo outdoc in CrocsCollectionCreditCardWrapper class :" + e.getMessage());
		}
	}

	/** EOMS - 3698 this method is used to getSalesOrder no from getOrderList API in case of Refund Call
	 * @param env env
	 * @param orderNo orderNo
	 * @return String
	 * @throws Exception Exception
	 */
	public static String getSalesOrderNo(YFSEnvironment env, String orderNo) throws Exception {
		try {
			Document getOrderListInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER);
			getOrderListInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_ORDER_NO, orderNo);
			Document getOrderListout = CommonUtil.invokeAPI(env, TEMPLATE_CROCS_ADYEN_GETORDERLIST, API_GET_ORDER_LIST, getOrderListInDoc);
			String strOrderNo = SCXmlUtil.getXpathAttribute(getOrderListout.getDocumentElement(),
					"/OrderList/Order/OrderLines/OrderLine/DerivedFromOrder/@OrderNo");
			logger.verbose("SalesOrderNo is "+ strOrderNo);
			return strOrderNo;
		}catch (Exception e){
			logger.verbose("Error in getOrderListAPI during Capture call outdoc: "
					+ e.getLocalizedMessage());
			throw new YFSException(" Error in getOrderListAPI during Capture call CrocsCollectionCreditCardWrapper class :" + e.getMessage());
		}
	}

	/**
	 * Description: Applying note text to the order
	 * 
	 * @param strErrorResponse
	 * @return
	 * @throws Exception
	 */
	private Document preparePaymentTransactionErrorDocument(String strErrorResponse) {

		logger.verbose("####### START Method: preparePaymentTransactionErrorDocument #########");

		Document docPaymentTransError = SCXmlUtil.createDocument(CrocsXmlConstants.E_PAYMNT_TRANS_ERROR_LIST);

		Element elePaymentTransactionErrorList = docPaymentTransError.getDocumentElement();
		Element elePaymentTransactionError = docPaymentTransError.createElement(CrocsXmlConstants.E_PAYMNT_TRANS_ERROR);

		if (strErrorResponse.length() > 100) {
			strErrorResponse = strErrorResponse.substring(0, 99);
		}
		elePaymentTransactionError.setAttribute(CrocsXmlConstants.A_Message, strErrorResponse);
		elePaymentTransactionError.setAttribute(CrocsXmlConstants.A_MSG_TYPE, CrocsXmlConstants.STR_TEXT);

		elePaymentTransactionErrorList.appendChild(elePaymentTransactionError);

		logger.verbose("The Payment Transaction Error document prepared by the method is: \n"
				+ XMLUtil.getXMLString(docPaymentTransError));

		logger.verbose("####### END Method: preparePaymentTransactionErrorDocument #########");

		return docPaymentTransError;
	}
	
	/**
	 * Description: Suspend payment method and applying hold to the Order
	 * 
	 * @param inputStruct
	 */
	private void suspendPaymentMethod(YFSExtnPaymentCollectionInputStruct inputStruct) {
		
		logger.beginTimer("CrocsCollectionOthersUEImpl.handleDummyPaymentForStoreReturns() : Begin");
		logger.verbose("Starts of method handleDummyPaymentForStoreReturns::");

		outStruct.suspendPayment = CrocsXmlConstants.FLAG_Y;
		
		outStruct.holdReason = CrocsConstant.STR_PAYMENT_ERROR_HOLD;
		outStruct.retryFlag = CrocsXmlConstants.FLAG_N;
		outStruct.authAVS = "-1";
		outStruct.asynchRequestProcess = CrocsConstant.A_FALSE;
		outStruct.authorizationAmount = 0.00;
		outStruct.authReturnFlag = CrocsConstant.STR_F;
		outStruct.authReturnMessage = CrocsConstant.STR_AUTHORIZATION_DESC;
		outStruct.tranAmount = inputStruct.requestAmount;
		outStruct.internalReturnFlag = CrocsConstant.STR_F;
		outStruct.tranReturnFlag = CrocsConstant.STR_F;
		outStruct.tranType = inputStruct.chargeType;
		outStruct.holdOrderAndRaiseEvent = CrocsConstant.A_TRUE;

		try {
			outStruct.PaymentTransactionError = preparePaymentTransactionErrorDocument(CrocsConstant.STR_AUTHORIZATION_DESC);
		} catch (Exception e) {
			logger.verbose("CollectionCreditCardWrapper.collectionCreditCard() Error :" + e.getMessage());
		}

		logger.endTimer("CrocsCollectionOthersUEImpl.handleDummyPaymentForStoreReturns() : End");

	}

}
