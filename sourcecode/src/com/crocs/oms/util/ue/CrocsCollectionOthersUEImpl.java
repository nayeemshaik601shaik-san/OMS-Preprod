package com.crocs.oms.util.ue;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.Environment;
import com.braintreegateway.Result;
import com.braintreegateway.Transaction;
import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsIVAPIConstants;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.crocs.oms.order.migration.CrocsMigarationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.core.YFSSystem;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import com.yantra.yfs.japi.YFSExtnPaymentCollectionInputStruct;
import com.yantra.yfs.japi.YFSExtnPaymentCollectionOutputStruct;
import com.yantra.yfs.japi.YFSUserExitException;
import com.yantra.yfs.japi.ue.YFSCollectionOthersUE;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.math.BigDecimal;
import java.rmi.RemoteException;

/**
 * Implemented Others UE for capturing the After Pay and Cash App payments
 * 
 */
public class CrocsCollectionOthersUEImpl implements CrocsConstant,YFSCollectionOthersUE {

	public static final YFCLogCategory logger = YFCLogCategory.instance(CrocsCollectionOthersUEImpl.class);

	YFSExtnPaymentCollectionOutputStruct outputStruct = new YFSExtnPaymentCollectionOutputStruct();

	@Override
	public YFSExtnPaymentCollectionOutputStruct collectionOthers(YFSEnvironment env,
			YFSExtnPaymentCollectionInputStruct inputStruct) throws YFSUserExitException {

		logger.beginTimer("CrocsCollectionOthersUEImpl.collectionOthers() : Begin");
		logger.verbose("Starting CrocsCollectionOthersUEImpl.collectionOthers() method");
		logger.verbose("InputStruct for collectionOthers: " + inputStruct);

		try {

			String strPaymentReference4 = inputStruct.paymentReference4;

			if (!CrocsMigarationUtil.isImportOrderEligibleForPayment(env, inputStruct.orderHeaderKey)) {

				outputStruct.holdOrderAndRaiseEvent = CrocsConstant.A_TRUE;
				outputStruct.holdReason = CrocsConstant.STR_MIGRATION_PAY_HOLD;
				
				return outputStruct;
			}else if (CrocsConstant.CHARGETYPE_CHARGE.equals(inputStruct.chargeType) && inputStruct.requestAmount > 0) {

				// EOMS - 676 Paypal capture Implementation - If the order was placed using the
				// PayPal Braintree payment gateway, apply a hold on the order.
				if (!YFCObject.isVoid(strPaymentReference4)
						&& CrocsConstant.STR_BRAIN_TREE.equals(strPaymentReference4)) {
					logger.verbose(
							"CrocsCollectionOthersUEImpl : collectionOthers - Applying Hold if the Order is BrainTree Order");
					processBrainTreePaymentGateWayOrd(inputStruct);
					return outputStruct;
				}

				// method helps to form Json request and hit Adyen
				String strRequest = CrocsXmlConstants.A_EVENT_CODE_CAPTURE;
				prepareAdyenWebserviceCaptureRequest(inputStruct, strRequest,env);
				
			} else if (CrocsConstant.CHARGETYPE_CHARGE.equals(inputStruct.chargeType) && inputStruct.requestAmount < 0) {
				// refactored conditions to build logic for SO & RO depend on requestAmount
				if (CrocsConstant.STR_IN_STORE_PAY.equalsIgnoreCase(inputStruct.paymentType)) {
					// EOMS-1495
					logger.verbose("In Store Payment processing:");
					// method helps to bypass Adyen in case of Store returns EOMS-1495
					handleDummyPaymentForStoreReturns(inputStruct);
				} else {

					// EOMS - 676 Paypal capture Implementation - If the order was placed using the
					// PayPal Braintree payment gateway, apply a hold on the order.
					if (!YFCObject.isVoid(strPaymentReference4)
							&& CrocsConstant.STR_BRAIN_TREE.equals(strPaymentReference4)) {
						
						processRefundsForPayPalBrainTree(inputStruct);
						return outputStruct;
					}

					// method helps to form Json request and hit Adyen
					String strRequest = CrocsXmlConstants.A_EVENT_CODE_REFUND;
					prepareAdyenWebserviceCaptureRequest(inputStruct, strRequest,env);

				}
			}else if(CrocsConstant.AUTHORIZATION.equals(inputStruct.chargeType) && inputStruct.requestAmount > 0) {
				
				suspendPaymentMethod(inputStruct);
			}

		} catch (Exception e) {

			logger.verbose("CrocsCollectionOthersUEImpl.collectionOthers :Expection" + e.getMessage());
			outputStruct.holdOrderAndRaiseEvent = CrocsConstant.A_TRUE;
			outputStruct.holdReason = CrocsConstant.STR_PAYMENT_ERROR_HOLD;
			//EOMS-4472: Payment Alerts : START
			outputStruct.internalReturnMessage = CrocsConstant.STR_PAYMENT_ERROR_HOLD;
			//EOMS-4472: Payment Alerts : END

		}
		logger.verbose("OutputStruct for collectionOthers: " + outputStruct);
		logger.endTimer("CrocsCollectionOthersUEImpl.collectionOthers() : End");

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
	private Document prepareAdyenWebserviceCaptureRequest(YFSExtnPaymentCollectionInputStruct inputStruct, String strRequest,YFSEnvironment env) {

		logger.beginTimer("CrocsCollectionOthersUEImpl.prepareAdyenWebserviceCaptureRequest() : Begin");
		logger.verbose("Starting CrocsCollectionOthersUEImpl.prepareAdyenWebserviceCaptureRequest() method");
		logger.verbose("InputStruct for prepareAdyenWebserviceCaptureRequest: " + inputStruct);

		String strJSONPayload = null;
		Document adyenInputDoc = null;
		Document adyenResponseOutDoc = null;
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
			/** EOMS-11405 -Configure and validation Adyen Refunds - For Korea
			 *  Need to skip the decimal conversion part of amount for Korea
			 *  For EX: ₩124,000 after convert will became ₩124,0 so skip the call kor Korea.
			**/
			String strEnterpriseCode = inputStruct.enterpriseCode;
			if(!CROCS_KR.equalsIgnoreCase(strEnterpriseCode)) {
				dValue = dValue * Math.pow(10, 2);
			}
			//EOMS-11405 -Configure and validation Adyen Refunds - For Korea - END
			int iValue = (int) Math.round(dValue);

			JSONObject jsonPayloadObj = new JSONObject();
			JSONObject jsonAmountObj = new JSONObject();
			JSONObject jsonOrderObj = new JSONObject();
			String reqPayload = null;
			try {
				jsonAmountObj.put(CrocsConstant.POSTMEN_CURRENCY, sCurrency);
				jsonAmountObj.put(CrocsConstant.VALUE, iValue);
				jsonOrderObj.put(CrocsConstant.POSTMEN_AMOUNT, jsonAmountObj);
				jsonOrderObj.put(CrocsConstant.ADYEN_MERCHANT_ACCOUNT, sMerchantAcc);
				//EOMS -3698 pass Order No to Adyen in MerchantReference instead of ChargeTransactionKey
				jsonOrderObj.put(CrocsConstant.reference, sReference);
				jsonPayloadObj.put(CrocsConstant.STR_BODY, jsonOrderObj);
				jsonPayloadObj.put(CrocsConstant.ADYEN_PSP_REFERENCE, pspreference);
				reqPayload = jsonPayloadObj.toString();

				// Creating new Request Document for Adyen
				adyenInputDoc = SCXmlUtil.createDocument(CrocsConstant.STR_ADYEN_REQUEST);
				Element adyenInputdocEle = adyenInputDoc.getDocumentElement();
				adyenInputdocEle.setAttribute(CrocsConstant.STR_ADEYN_REQUEST_PAYLOAD, reqPayload);
				adyenInputdocEle.setAttribute(CrocsConstant.STR_ADYEN_REQUEST, strRequest);

			} catch (JSONException e) {
				logger.verbose("Json Request formation Failed: " + e);
				throw new YFSException(
						"CrocsCollectionOthersUEImpl.prepareAdyenWebserviceCaptureRequest :Expection" + e.getMessage());
			}

			/**
			 * Printing the adeyen input, then colling the adyen endpoint
			 */

			logger.verbose("Input for Adyen Request: " + XMLUtil.getXMLString(adyenInputDoc));
			AdyenCaptureObj = new CrocsAdyenCaptureRequest();
			strJSONPayload = AdyenCaptureObj.crocsAdyenCaptureRequest(adyenInputDoc);

			adyenResponseOutDoc = SCXmlUtil.createDocument(CrocsConstant.STR_ADYEN_RESPONSE);
			Element adyenOutputdocEle = adyenResponseOutDoc.getDocumentElement();
			adyenOutputdocEle.setAttribute(CrocsConstant.STR_ADEYN_RESPONSE_PAYLOAD, strJSONPayload);
			logger.verbose("Output from Adyen " + XMLUtil.getXMLString(adyenResponseOutDoc));

			/**
			 * if the response is received , we will prepare the outputstruct object to
			 * update chargeTransactionTable
			 **/
			setAdyenOutputStruct(strJSONPayload, strRequest,inputStruct,env);

		} catch (Exception e) {
			logger.verbose("Exception in CrocsCollectionOthersUEImpl.prepareAdyenWebserviceCaptureRequest" + e);
			throw new YFSException(
					"CrocsCollectionOthersUEImpl.prepareAdyenWebserviceCaptureRequest :Expection" + e.getMessage());
		}

		logger.endTimer("CrocsCollectionOthersUEImpl.prepareAdyenWebserviceCaptureRequest() : End");
		return adyenResponseOutDoc;

	}

	/**
	 * Below method used to capture the records in Charge Transaction table.
	 * 
	 * @param env
	 * @param inputStruct
	 * @param strJSONPayload
	 * @param strRequest
	 * @throws Exception
	 */
	private void setAdyenOutputStruct(String strJSONPayload, String strRequest,YFSExtnPaymentCollectionInputStruct inputStruct,YFSEnvironment env) throws RemoteException {

		logger.beginTimer("CrocsCollectionOthersUEImpl.setAdyenOutputStruct() : Begin");
		logger.verbose("Starting CrocsCollectionOthersUEImpl.setAdyenOutputStruct() method" + strJSONPayload);

		JSONObject jsonPayload = new JSONObject();
		String status = null;

		if (strJSONPayload != null && strJSONPayload.contains(CrocsConstant.STATUS)) {
			jsonPayload = new JSONObject(strJSONPayload);
			status = jsonPayload.get(CrocsConstant.STATUS).toString();
		}

		if (status != null && status.equalsIgnoreCase(CrocsConstant.ADYEN_STATUS_RECEIVED)) {

			double dValue = jsonPayload.getJSONObject(CrocsXmlConstants.A_AMOUNT).getInt(CrocsConstant.VALUE);
			// coverting into decimal value expected by Adyen input for Adyen Tenders
			/** EOMS-11405 -Configure and validation Adyen Refunds - For Korea
			 *  Need to skip the decimal conversion part of amount for Korea
			 *  For EX: ₩124,000 after convert will became ₩124,0 so skip the call kor Korea.
			**/
			String strEnterpriseCode = inputStruct.enterpriseCode;
			if(!CROCS_KR.equalsIgnoreCase(strEnterpriseCode)) {
				dValue = dValue * Math.pow(10, -2);
			}
			//EOMS-11405 -Configure and validation Adyen Refunds - For Korea - END				
			outputStruct.authorizationAmount = dValue;
			outputStruct.tranAmount = dValue;
			outputStruct.retryFlag = CrocsConstant.FLAG_N;
			outputStruct.tranType = CrocsConstant.CHARGETYPE_CHARGE;
			outputStruct.holdOrderAndRaiseEvent = CrocsConstant.A_FALSE;
			if (CrocsXmlConstants.A_EVENT_CODE_REFUND.equalsIgnoreCase(strRequest))
				outputStruct.authorizationAmount = -dValue;

			//insert capture details in CROCS_ADYEN_WEBHOOKS_RES
			createCaptureInfo(env, inputStruct, jsonPayload);


		} else {
			outputStruct.holdOrderAndRaiseEvent = CrocsConstant.A_TRUE;
			logger.verbose(
					"CrocsCollectionOthersUEImpl.collectionOthers: payment_gateway_rejected_response_Expection ");
			/**
			 * Applying the Payment hold as payment gateway is rejected the request Below
			 * method will be called with appropriate flag and hold will be applied
			 **/
			outputStruct.holdReason = CrocsConstant.STR_PAYMENT_REJECT_HOLD;
			//EOMS-4472: Payment Alerts : START
			outputStruct.internalReturnMessage = CrocsConstant.STR_PAYMENT_REJECT_HOLD;
			//EOMS-4472: Payment Alerts : END
			//insert capture details in CROCS_ADYEN_WEBHOOKS_RES
			createCaptureInfo(env, inputStruct, jsonPayload);
			logger.verbose(
					"CollectionCreditCardWrapper.collectionCreditCard: payment_gateway_rejected_response_Expection ");	
		}

		logger.endTimer("CrocsCollectionOthersUEImpl.setAdyenOutputStruct() : End");
	}

	/**
	 *
	 * @param inputStruct
	 */
	private void handleDummyPaymentForStoreReturns(YFSExtnPaymentCollectionInputStruct inputStruct) {
		logger.beginTimer("CrocsCollectionOthersUEImpl.handleDummyPaymentForStoreReturns() : Begin");
		logger.verbose("Starts of method handleDummyPaymentForStoreReturns::");

		outputStruct.authorizationAmount = inputStruct.requestAmount;
		outputStruct.tranAmount = inputStruct.requestAmount;
		outputStruct.retryFlag = CrocsConstant.FLAG_N;
		outputStruct.tranType = CrocsConstant.CHARGETYPE_CHARGE;
		outputStruct.holdOrderAndRaiseEvent = CrocsConstant.A_FALSE;

		logger.endTimer("CrocsCollectionOthersUEImpl.handleDummyPaymentForStoreReturns() : End");

	}

	/**
	 * @param inputStruct EOMS - Paypal capture Implementation - If the order was
	 *                    placed using the PayPal Braintree payment gateway, apply a
	 *                    hold on the order.
	 *  @param inputStruct
	 */
	private void processBrainTreePaymentGateWayOrd(YFSExtnPaymentCollectionInputStruct inputStruct) {

		logger.verbose("CrocsCollectionOthersUEImpl : braintreeOrder - start of the method");

		outputStruct.holdReason = CrocsConstant.STR_BRAIN_TREE_HOLD;
		outputStruct.retryFlag = CrocsXmlConstants.FLAG_N;
		outputStruct.authAVS = "-1";
		outputStruct.asynchRequestProcess = CrocsConstant.A_FALSE;
		outputStruct.authorizationAmount = 0.00;
		outputStruct.authReturnFlag = CrocsConstant.STR_F;
		outputStruct.authReturnMessage = CrocsConstant.STR_BRAIN_TREE_DESC;
		outputStruct.tranAmount = inputStruct.requestAmount;
		outputStruct.internalReturnFlag = CrocsConstant.STR_F;
		outputStruct.tranReturnFlag = CrocsConstant.STR_F;
		outputStruct.tranType = inputStruct.chargeType;
		outputStruct.holdOrderAndRaiseEvent = CrocsConstant.A_TRUE;

		logger.verbose("CrocsCollectionOthersUEImpl : braintreeOrder - End of the  method");

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
	 * Description: Below method helps to make Status as ERROR in Charge Transaction Table.
	 * 
	 * @param strErrorResponse
	 * @return
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
	 * Description: Below method helps to call Brain tree gateway for processing refunds. 
	 * 
	 * @param inputStruct
	 */
	
	private void processRefundsForPayPalBrainTree(YFSExtnPaymentCollectionInputStruct inputStruct) {

		logger.verbose("CrocsCollectionOthersUEImpl : processBrainTreeRefund - start of the method");
		
		try {
			Environment strEnv=null;
			String strPayPalMerchantId = YFSSystem.getProperty(CrocsConstant.A_PAYPAL_MERCHANT_ID);
			String strPayPalPublicKey = YFSSystem.getProperty(CrocsConstant.A_PAYPAL_PUBLIC_KEY);
			String strPayPalPrivateKey = YFSSystem.getProperty(CrocsConstant.A_PAYPAL_PRIVATE_KEY);
			String strPayPalEnvironment = YFSSystem.getProperty(CrocsConstant.A_PAYPAL_BRAINTREE_ENVIRONMENT);
			
			//Code Push
			if(CrocsConstant.STR_ENV_PRODUCTION.equalsIgnoreCase(strPayPalEnvironment))
				strEnv = Environment.PRODUCTION;
			else if(CrocsConstant.STR_ENV_SANDBOX.equalsIgnoreCase(strPayPalEnvironment))
				strEnv = Environment.SANDBOX;
			
			if (!YFCObject.isVoid(inputStruct.paymentReference6)) {
				
				BraintreeGateway gateway = new BraintreeGateway(strEnv, strPayPalMerchantId, strPayPalPublicKey,
						strPayPalPrivateKey);
	
				double dValue = Math.abs(inputStruct.requestAmount);

				Result<Transaction> result = gateway.transaction().refund(inputStruct.paymentReference6,
						BigDecimal.valueOf(dValue));

				 ObjectMapper mapper = new ObjectMapper(); 
				 String json = mapper.writeValueAsString(result);				 
				 logger.verbose("JSON:::"+ json);
				   
				if (result.isSuccess()) {
					Transaction settledTransaction = result.getTarget();
					logger.verbose("Transaction submitted for settlement successfully. ID: " + settledTransaction.getId());
					logger.verbose("Amount:::::" + settledTransaction.getAmount());

					String strValue = settledTransaction.getAmount().toString();
					
					double dPayPalValue = Double.parseDouble(strValue);
					
					outputStruct.authorizationAmount = -dPayPalValue;
					outputStruct.tranAmount = -dPayPalValue;
					outputStruct.retryFlag = CrocsConstant.FLAG_N;
					outputStruct.tranType = CrocsConstant.CHARGETYPE_CHARGE;
					outputStruct.holdOrderAndRaiseEvent = CrocsConstant.A_FALSE;

				} else {
					outputStruct.holdReason = CrocsConstant.STR_BRAIN_TREE_HOLD;
					outputStruct.holdOrderAndRaiseEvent = CrocsConstant.A_TRUE;
					outputStruct.PaymentTransactionError = preparePaymentTransactionErrorDocument(CrocsConstant.STR_FAILURE_RESPONSE);
					logger.verbose("Errors:: " + result.getErrors());
				}
			}else {
				outputStruct.holdReason = CrocsConstant.STR_BRAIN_TREE_HOLD;
				outputStruct.holdOrderAndRaiseEvent = CrocsConstant.A_TRUE;
				outputStruct.PaymentTransactionError = preparePaymentTransactionErrorDocument(CrocsConstant.STR_PAYMENT_REFERENCE_6_EMPTY);
			}
		}catch(Exception e) {
			logger.verbose("CollectionCreditCardWrapper.processRefundsForPayPalBrainTree() Error :" + e.getMessage());
			outputStruct.holdReason = CrocsConstant.STR_BRAIN_TREE_HOLD;
			outputStruct.holdOrderAndRaiseEvent = CrocsConstant.A_TRUE;
			outputStruct.PaymentTransactionError = preparePaymentTransactionErrorDocument(CrocsConstant.STR_CONNECTION_FAILURE_EXCEPTION);
		}
		logger.verbose("CrocsCollectionOthersUEImpl : processBrainTreeRefund - End of the  method");
	}

	/** EOMS 3698 -> this method is used to insert capture Info details in CROCS_ADYEN_WEBHOOKS_RES table
	 * @param env env
	 * @param inStruct inStruct
	 * @param jsonObject jsonObject
	 * @throws RemoteException
	 */
	private static void createCaptureInfo(YFSEnvironment env, YFSExtnPaymentCollectionInputStruct inStruct, JSONObject jsonObject) throws RemoteException {
		try{
			if( jsonObject.get(CrocsConstant.ADYEN_PSP_REFERENCE).toString() != null) {
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
		}catch(Exception e) {
			logger.verbose("Error in createCaptureInfo outdoc: "
					+ e.getLocalizedMessage());
			throw new YFSException(" Error in createCaptureInfo outdoc in CrocsCollectionOthersUE class :" + e.getMessage());
		}
	}
}
