package com.crocs.oms.util.ue;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsIVAPIConstants;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.crocs.oms.order.migration.CrocsMigarationUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import com.yantra.yfs.japi.YFSExtnPaymentCollectionInputStruct;
import com.yantra.yfs.japi.YFSExtnPaymentCollectionOutputStruct;
import com.yantra.yfs.japi.YFSUserExitException;
import com.yantra.yfs.japi.ue.YFSCollectionStoredValueCardUE;

import java.rmi.RemoteException;

/**
 * Implemented Stored Value Card UE for capturing the Givex payments
 * 
 */
public class CrocsCollectionStoredValueCardUEImpl extends CrocsCollectionCreditCardWrapper implements CrocsConstant,YFSCollectionStoredValueCardUE {
	
	
	public static final YFCLogCategory logger = YFCLogCategory.instance(CrocsCollectionStoredValueCardUEImpl.class);

	YFSExtnPaymentCollectionOutputStruct outputStruct = new YFSExtnPaymentCollectionOutputStruct();

	@Override
	public YFSExtnPaymentCollectionOutputStruct collectionStoredValueCard(YFSEnvironment env,
			YFSExtnPaymentCollectionInputStruct inputStruct) throws YFSUserExitException {
		
		logger.beginTimer("CrocsCollectionStoredValueCardUEImpl.collectionStoredValueCard() : Begin");
		  logger.verbose("Starting CrocsCollectionStoredValueCardUEImpl.collectionStoredValueCard() method");
		  logger.verbose("InputStruct for collectionStoredValueCard: " + inputStruct);

		try {
			
			if(!CrocsMigarationUtil.isImportOrderEligibleForPayment(env, inputStruct.orderHeaderKey)) {
				
				outputStruct.holdOrderAndRaiseEvent = CrocsConstant.A_TRUE;
				outputStruct.holdReason = CrocsConstant.STR_MIGRATION_PAY_HOLD;
				
				return outputStruct;
			}else if (CrocsConstant.CHARGETYPE_CHARGE.equals(inputStruct.chargeType) && inputStruct.requestAmount > 0) {
				
				String strRequest = CrocsXmlConstants.A_EVENT_CODE_CAPTURE;
				prepareAdyenWebserviceCaptureRequestGivex(inputStruct,strRequest,env);

			}else if(CrocsConstant.AUTHORIZATION.equals(inputStruct.chargeType) && inputStruct.requestAmount > 0) {
				
				suspendPaymentMethod(inputStruct);
			}

		} catch (Exception e) {
			
			logger.verbose("CrocsCollectionStoredValueCardUEImpl.collectionStoredValueCard :Expection:"+ e.getMessage());
			outputStruct.holdOrderAndRaiseEvent = CrocsConstant.A_TRUE;
			outputStruct.holdReason = CrocsConstant.STR_PAYMENT_ERROR_HOLD;
			//EOMS-4472: Payment Alerts : START
			outputStruct.internalReturnMessage = CrocsConstant.STR_PAYMENT_ERROR_HOLD;
			//EOMS-4472: Payment Alerts : END
		}
		logger.verbose("OutputStruct for collectionStoredValueCard: " + outputStruct);
		logger.endTimer("CrocsCollectionStoredValueCardUEImpl.collectionStoredValueCard() : End");
		
		return outputStruct;
	}
	
	
	/**
	 * Below method used to form JSON Request and hit Adyen URL 
	 * 
	 * @param env
	 * @param inputStruct
	 * @return
	 * @throws Exception
	 */
	
	private Document prepareAdyenWebserviceCaptureRequestGivex(YFSExtnPaymentCollectionInputStruct inputStruct,String strRequest,YFSEnvironment env) {
		
		logger.beginTimer("CrocsCollectionStoredValueCardUEImpl.prepareAdyenWebserviceCaptureRequestGivex() : Begin");
		logger.verbose("Starting CrocsCollectionStoredValueCardUEImpl.prepareAdyenWebserviceCaptureRequestGivex() method");
		logger.verbose("InputStruct for prepareAdyenWebserviceCaptureRequestGivex: " + inputStruct);
		
		String strJSONPayload=null;
		Document adyenInputDoc=null;
		Document adyenResponseOutDoc=null;
		CrocsAdyenCaptureRequest AdyenCaptureObj = null;
		
		try {
			String sMerchantAcc = inputStruct.paymentReference2;
			String pspreference = inputStruct.authorizationId;
			if (YFCCommon.isVoid(pspreference))
				pspreference = inputStruct.paymentReference5;
			String sCurrency = inputStruct.currency;
			String sReference = inputStruct.orderNo;
			double dValue = Math.abs(inputStruct.requestAmount);

			// coverting into decimal value expected by Adyen input for Adyen Tenders
			dValue = dValue * Math.pow(10, 2);
			int iValue = (int) Math.round(dValue);

			JSONObject jsonPayloadObj = new JSONObject();
			JSONObject jsonAmountObj = new JSONObject();
			JSONObject jsonOrderObj = new JSONObject();
			String reqPayload = null;
			
			jsonAmountObj.put(CrocsConstant.POSTMEN_CURRENCY, sCurrency);
			jsonAmountObj.put(CrocsConstant.VALUE, iValue);
			jsonOrderObj.put(CrocsConstant.POSTMEN_AMOUNT, jsonAmountObj);
			jsonOrderObj.put(CrocsConstant.ADYEN_MERCHANT_ACCOUNT, sMerchantAcc);
			//EOMS -3698 pass Order No to Adyen in MerchantReference instead of ChargeTransactionKey
			jsonOrderObj.put(CrocsConstant.reference, sReference);
			jsonPayloadObj.put(CrocsConstant.STR_BODY, jsonOrderObj);
			jsonPayloadObj.put(CrocsConstant.ADYEN_PSP_REFERENCE, pspreference);
			reqPayload = jsonPayloadObj.toString();
				
			//Creating new Request Document for Adyen 
			adyenInputDoc = SCXmlUtil.createDocument(CrocsConstant.STR_ADYEN_REQUEST);
			Element adyenInputdocEle = adyenInputDoc.getDocumentElement();
			adyenInputdocEle.setAttribute(CrocsConstant.STR_ADEYN_REQUEST_PAYLOAD, reqPayload);
			adyenInputdocEle.setAttribute(CrocsConstant.STR_ADYEN_REQUEST, strRequest);
			
			
			/**	
			 * Printing the Adyen input, then calling the Adyen endpoint
			 * 
			 * */
			logger.info("Input for Adyen Request: " + XMLUtil.getXMLString(adyenInputDoc));
			logger.verbose("Input for Adyen Request: " + XMLUtil.getXMLString(adyenInputDoc));
			AdyenCaptureObj = new CrocsAdyenCaptureRequest();
	    	strJSONPayload = AdyenCaptureObj.crocsAdyenCaptureRequest(adyenInputDoc);
	    	
	    	
	    	
			adyenResponseOutDoc = SCXmlUtil.createDocument(CrocsConstant.STR_ADYEN_RESPONSE);
			Element adyenOutputdocEle = adyenResponseOutDoc.getDocumentElement();
			adyenOutputdocEle.setAttribute(CrocsConstant.STR_ADEYN_RESPONSE_PAYLOAD, strJSONPayload);
	    	logger.verbose("Output from Adyen " + XMLUtil.getXMLString(adyenResponseOutDoc));
			

			/** if the response is received , we will prepare the outputstruct  object to update chargeTransactionTable **/
			setAdyenOutputStruct(strJSONPayload,inputStruct,env);
		
		} catch (Exception e) {
			logger.verbose("Exception in CrocsCollectionStoredValueCardUEImpl.prepareAdyenWebserviceCaptureRequestGivex" + e);		
			throw new YFSException("CrocsCollectionStoredValueCardUEImpl.prepareAdyenWebserviceCaptureRequestGivex :Expection" + e.getMessage()); 
		}
		
		logger.endTimer("CrocsCollectionStoredValueCardUEImpl.prepareAdyenWebserviceCaptureRequestGivex() : End");
		return adyenResponseOutDoc;
	}
	
	/**
	 * Below method used to capture the records in Charge Transaction table.
	 * 
	 * @param env
	 * @param inputStruct
	 * @param strJSONPayload
	 * @throws Exception
	 */
	private void setAdyenOutputStruct(String strJSONPayload,YFSExtnPaymentCollectionInputStruct inputStruct,YFSEnvironment env) throws RemoteException {
		
		 logger.beginTimer("CrocsCollectionStoredValueCardUEImpl.setAdyenOutputStruct() : Begin");
		 logger.verbose("Starting CrocsCollectionStoredValueCardUEImpl.setAdyenOutputStruct() method" + strJSONPayload);

		JSONObject jsonPayload = new JSONObject(strJSONPayload);
		
		String status = jsonPayload.get(CrocsConstant.STATUS).toString();

		if ( status!=null && status.equalsIgnoreCase(CrocsConstant.ADYEN_STATUS_RECEIVED)) {
			
			double dValue = jsonPayload.getJSONObject(CrocsXmlConstants.A_AMOUNT).getInt(CrocsConstant.VALUE);
			dValue = dValue * Math.pow(10, -2);
			outputStruct.authorizationAmount = dValue;  
			outputStruct.tranAmount = dValue;
			outputStruct.retryFlag = CrocsConstant.FLAG_N;
			outputStruct.tranType = CrocsConstant.CHARGETYPE_CHARGE;
			outputStruct.holdOrderAndRaiseEvent = CrocsConstant.A_FALSE;

			//insert capture details in CROCS_ADYEN_WEBHOOKS_RES
			createCaptureInfo(env, inputStruct, jsonPayload);

		} else {
			
			/**
			 * Applying the Payment hold as payment gateway is rejected the request  
			 **/
			outputStruct.holdOrderAndRaiseEvent = CrocsConstant.A_TRUE;
			outputStruct.holdReason = CrocsConstant.STR_PAYMENT_REJECT_HOLD;
			//EOMS-4472: Payment Alerts : START
			outputStruct.internalReturnMessage = CrocsConstant.STR_PAYMENT_REJECT_HOLD;
			//EOMS-4472: Payment Alerts : END

			//insert capture details in CROCS_ADYEN_WEBHOOKS_RES
			createCaptureInfo(env, inputStruct, jsonPayload);
		}
		
		logger.endTimer("CrocsCollectionStoredValueCardUEImpl.setAdyenOutputStruct() : End");
	}
	/**
	 * Description: Suspend payment method and applying hold to the Order
	 * 
	 * @param inputStruct
	 */
	private void suspendPaymentMethod(YFSExtnPaymentCollectionInputStruct inputStruct) {
		
		logger.beginTimer("CrocsCollectionOthersUEImpl.handleDummyPaymentForStoreReturns() : Begin");
		logger.verbose("Starts of method handleDummyPaymentForStoreReturns::");

		outputStruct.suspendPayment = CrocsXmlConstants.FLAG_Y;
		
		outputStruct.holdReason = CrocsConstant.STR_PAYMENT_ERROR_HOLD;
		outputStruct.retryFlag = CrocsXmlConstants.FLAG_N;
		outputStruct.authAVS = "-1";
		outputStruct.asynchRequestProcess = CrocsConstant.A_FALSE;
		outputStruct.authorizationAmount = 0.00;
		outputStruct.authReturnFlag = CrocsConstant.STR_F;
		outputStruct.authReturnMessage = CrocsConstant.STR_AUTHORIZATION_DESC;
		outputStruct.tranAmount = inputStruct.requestAmount;
		outputStruct.internalReturnFlag = CrocsConstant.STR_F;
		outputStruct.tranReturnFlag = CrocsConstant.STR_F;
		outputStruct.tranType = inputStruct.chargeType;
		outputStruct.holdOrderAndRaiseEvent = CrocsConstant.A_TRUE;

		try {
			outputStruct.PaymentTransactionError = preparePaymentTransactionErrorDocument(CrocsConstant.STR_AUTHORIZATION_DESC);
		} catch (Exception e) {
			logger.verbose("CollectionCreditCardWrapper.collectionCreditCard() Error :" + e.getMessage());
		}

		logger.endTimer("CrocsCollectionOthersUEImpl.handleDummyPaymentForStoreReturns() : End");

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

	/** EOMS 3698 ->this method is used to insert capture Info details in CROCS_ADYEN_WEBHOOKS_RES table
	 * @param env  env
	 * @param inStruct inStruct
	 * @param jsonObject jsonObject
	 * @throws RemoteException
	 */
	private static void createCaptureInfo(YFSEnvironment env, YFSExtnPaymentCollectionInputStruct inStruct, JSONObject jsonObject) throws RemoteException {
		try {
			if (jsonObject.get(CrocsConstant.ADYEN_PSP_REFERENCE).toString() != null) {
				String orderNo =inStruct.orderNo;
				Document createCaptureIndoc = SCXmlUtil.createDocument(C_CAPTURE_INFO);
				Element createCaptureEle = createCaptureIndoc.getDocumentElement();
				createCaptureEle.setAttribute(ChargeTransactionKey, inStruct.chargeTransactionKey);
				createCaptureEle.setAttribute(OrderHeaderKey, inStruct.orderHeaderKey);
				createCaptureEle.setAttribute(DocumentType, inStruct.documentType);
				createCaptureEle.setAttribute(C_PSPREFERENCE, jsonObject.getString(CrocsConstant.ADYEN_PSP_REFERENCE));
				if(A_RETURN_ORDER_DOCUMENT_TYPE.equalsIgnoreCase(inStruct.documentType)){
					String strOrderNo = CrocsCollectionCreditCardWrapper.getSalesOrderNo(env, orderNo);
					createCaptureEle.setAttribute(ORDERNO,strOrderNo);
				}
				else{
					createCaptureEle.setAttribute(ORDERNO,orderNo);
				}
				logger.verbose("createCaptureIndoc" + createCaptureIndoc);
				Document createCaptureInfo = CommonUtil.invokeService(env, CrocsIVAPIConstants.CREATE_CAPTURE_INFO_SERVICE, createCaptureIndoc);
				logger.verbose("createCaptureInfo outdoc" + createCaptureInfo);
			}
		} catch (Exception e) {
			logger.verbose("Error in createCaptureInfo outdoc: "
					+ e.getLocalizedMessage());
			throw new YFSException(" Error in createCaptureInfo outdoc in CrocsCollectionStoredValueCardUE class :" + e.getMessage());
		}
	}
}
