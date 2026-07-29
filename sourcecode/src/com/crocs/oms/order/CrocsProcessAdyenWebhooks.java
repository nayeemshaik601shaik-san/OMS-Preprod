package com.crocs.oms.order;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.crocs.oms.common.util.CrocsIVAPIConstants;
import com.yantra.yfc.util.YFCCommon;
import org.apache.commons.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.util.ue.CrocsCheckFraudOnOrderUserExitImpl;
import com.crocs.oms.common.util.XMLUtil;
import com.crocs.oms.order.util.CrocsOrderCancellationNotesUtil;
import com.ibm.icu.util.Calendar;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

/**
 * EOMS-656 Capture Webhooks Async
 * This class will create alert on order if adyen capture webhook response is failed.
 */
public class CrocsProcessAdyenWebhooks implements CrocsConstant {
	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsOrderUpdate.class);

	/**
	 * if Event Code is "CAPTURE" or "REFUND" or "CANCEL_OR_REFUND" or
	 * "CANCELLATION" and Success is "false" will create alert on order if Event
	 * Code is "REFUND_FAILED" and Success is "true" will create alert on order
	 *
	 * @param env
	 * @param inDoc
	 * @return
	 * @throws Exception
	 */

	public void crocsProcessAdyenWebhooks(YFSEnvironment env, Document inDoc) throws Exception {
		logger.beginTimer("CrocsProcessAdyenWebhooks.crocsProcessAdyenWebhooks");
		logger.verbose("crocsProcessAdyenWebhooks Input XML: " + SCXmlUtil.getString(inDoc));
		try {

			Element eleCapture = inDoc.getDocumentElement();
			String strOrderNo = eleCapture.getAttribute(A_ORDER_NO);
			String strEventCode = eleCapture.getAttribute(C_EVENT_CODE);
			String strEventDate = eleCapture.getAttribute(C_EVENT_DATE);
			String strPSPReference = eleCapture.getAttribute(C_PSPREFERENCE);
			String strOriginalReference = eleCapture.getAttribute(C_ORIGINAL_REFERENCE);
			String strMerchantAccountCode = eleCapture.getAttribute(C_MERCHANT_ACCOUNT_CODE);
			String strReason = eleCapture.getAttribute(C_REASON);
			String strSuccess = eleCapture.getAttribute(C_SUCCESS);
			String strValue = eleCapture.getAttribute(A_VALUE);
			String strCurrency = eleCapture.getAttribute(A_CURRENCY);
			logger.verbose("ReasonCode received from Webhook" + strReason);
			//EOMS - 4254 Trim ReasonCode to a maximum of 100 characters
			if (strReason != null && strReason.length() > 95) {
				String reason = strReason.substring(0, 94);
				eleCapture.setAttribute(C_REASON, reason);
				logger.verbose("trimmedReasonCode" + reason);
			}

			// Define sets for event codes
			Set<String> falseEventCodes = new HashSet<>(Arrays.asList(A_EVENT_CODE_CAPTURE, A_EVENT_CODE_REFUND,
					A_EVENT_CODE_CANCEL_OR_REFUND, A_EVENT_CODE_CANCELLATION));

			if ((strSuccess.equalsIgnoreCase(A_FALSE_STRING) && falseEventCodes.contains(strEventCode))
					|| (strSuccess.equalsIgnoreCase(TRUE)
					&& A_EVENT_CODE_REFUND_FAILED.equalsIgnoreCase(strEventCode) || A_EVENT_CODE_CAPTURE_FAILED.equalsIgnoreCase(strEventCode))) {

				logger.verbose("EventCode is :" + strEventCode + "Success :" + strSuccess);

				JSONObject jNotificationRequestItem = new JSONObject();
				jNotificationRequestItem.put(A_EVENT_CODE, strEventCode);
				jNotificationRequestItem.put(A_EVENT_DATE, strEventDate);
				jNotificationRequestItem.put(A_PSPREFERENCE, strPSPReference);
				jNotificationRequestItem.put(A_ORIGINAL_REFERENCE, strOriginalReference);
				jNotificationRequestItem.put(A_MERCHANT_ACCOUNT_CODE, strMerchantAccountCode);
				jNotificationRequestItem.put(A_REASON, strReason);
				jNotificationRequestItem.put(A_SUCCESS, strSuccess);
				jNotificationRequestItem.put(A_MERCHANT_REFERENCE, strOrderNo);

				JSONObject jAmount = new JSONObject();
				jAmount.put(VALUE, strValue);
				jAmount.put(A_CURRENCY_F, strCurrency);

				jNotificationRequestItem.put(POSTMEN_AMOUNT, jAmount);

				logger.verbose("Adyen webhook response is " + jNotificationRequestItem.toString());

				//Fetch the OrderHeaderKey from the getChargeTransactionList API output.
				Document docGetOrderListOutput = getOrderListForSaleOrderNo(env, strOrderNo);
				String strOrderHeaderKey = SCXmlUtil.getXpathAttribute(docGetOrderListOutput.getDocumentElement(), XPATH_ORDERLIST_ORDER_ORDER_HEADER_KEY);

				// Raising alert
				Document createExceptionIndoc = SCXmlUtil.createDocument(A_INBOX);
				createExceptionIndoc.getDocumentElement().setAttribute(A_INBOX_TYPE, A_INBOX_TYPE_VALUE);
				createExceptionIndoc.getDocumentElement().setAttribute(A_EXCEPTION_TYPE, A_EXCEPTION_VALUE_WEBHOOKS_EXP);
				createExceptionIndoc.getDocumentElement().setAttribute(OrderHeaderKey, strOrderHeaderKey);
				createExceptionIndoc.getDocumentElement().setAttribute(OrderNo, strOrderNo);
				Element eleInboxReferencesList = SCXmlUtil.createChild(createExceptionIndoc.getDocumentElement(),
						A_INBOX_REFERENCES_LIST);
				Element eleInboxReferences = SCXmlUtil.createChild(eleInboxReferencesList, A_INBOX_REFERENCES);
				eleInboxReferences.setAttribute(A_NAME, strEventCode);
				eleInboxReferences.setAttribute(A_REFERNCE_TYPE, strEventCode);
				eleInboxReferences.setAttribute(A_VALUE, jNotificationRequestItem.toString());
				logger.verbose("Rasing alert for createException Input:- " + SCXmlUtil.getString(createExceptionIndoc));
				CommonUtil.invokeAPI(env, "", API_CREATE_EXCEPTION, createExceptionIndoc);

			}

			/**
			 * Description: This below logic helps to check the Authorization Webhook. If webhook came as Success, persisted the data
			 * if failure, cancelling the order. This is only post Auth Payment Methods
			 *
			 */
			if (A_EVENT_AUTHORISATION.equalsIgnoreCase(strEventCode)) {

				logger.info("CrocsProcessAdyenWebhooks.crocsProcessAdyenWebhooks() :getOrderListForSaleOrderNo for Order No: "+strOrderNo);
				// Calling getOrderList
				Document docGetOrderListOutput = getOrderListForSaleOrderNo(env, strOrderNo);
				/**
				 *If the webhook entry CREATETS and the order CREATETS are the same, a race condition will occur,
				 * and the POST_AUTH logic will not be executed.
				 */
				if (docGetOrderListOutput != null && Double
						.parseDouble(docGetOrderListOutput.getDocumentElement().getAttribute(A_TOTAL_ORDER_LIST)) > 0) {

					Element elePaymentMethod = XMLUtil.getElementByXPath(docGetOrderListOutput,
							"/OrderList/Order/PaymentMethods/PaymentMethod[@PaymentReference5='" + strPSPReference + "']");

					String strOrderHeaderKey = SCXmlUtil.getXpathAttribute(docGetOrderListOutput.getDocumentElement(), XPATH_ORDERLIST_ORDER_ORDER_HEADER_KEY);
					String strChargeTransctionKey = SCXmlUtil.getXpathAttribute(elePaymentMethod, "ChargeTransactionDetails/ChargeTransactionDetail[@ChargeType='AUTHORIZATION']/@ChargeTransactionKey");

					//Preparing input for GetCaptureInfo Call
					Document createCaptureIndoc = SCXmlUtil.createDocument(C_CAPTURE_INFO);
					Element createCaptureEle = createCaptureIndoc.getDocumentElement();
					createCaptureEle.setAttribute(C_PSPREFERENCE, eleCapture.getAttribute(C_PSPREFERENCE));
					logger.verbose("getCaptureInforList Indoc" + createCaptureIndoc);
					Document getCaptureInfoList = CommonUtil.invokeService(env, CrocsIVAPIConstants.GET_CAPTURE_INFO_LIST_SERVICE, createCaptureIndoc);
					logger.verbose("getCaptureInforList outdoc" + getCaptureInfoList);

					//if getCaptureList is empty than insert the new record
					if (YFCCommon.isVoid(getCaptureInfoList) || !getCaptureInfoList.getDocumentElement().hasChildNodes()) {
						inDoc.getDocumentElement().setAttribute(A_DOCUMENT_TYPE, VAL_DOCUMENT_TYPE_SALES_ORDER);
						inDoc.getDocumentElement().setAttribute(A_ORDER_HEADER_KEY, strOrderHeaderKey);
						inDoc.getDocumentElement().setAttribute(ChargeTransactionKey, strChargeTransctionKey);
						Document createCaptureInfo = CommonUtil.invokeService(env, CrocsIVAPIConstants.CREATE_CAPTURE_INFO_SERVICE, inDoc);
						logger.verbose("createCaptureInfo outdoc" + createCaptureInfo);

						String strPaymentType = elePaymentMethod.getAttribute(A_PAYMENT_TYPE);

						String strEnterpriseCode = SCXmlUtil.getXpathAttribute(docGetOrderListOutput.getDocumentElement(),
								CrocsConstant.STR_XPATH_ORDERLIST_ENTERPRISE_CODE);

						Document docGetCommonCodeListOutput = CommonUtil.getCommonCodeList(env, strEnterpriseCode,
								CrocsConstant.STR_CROCS_AUTH_EXP_PAYMENTS, null);

						Element eleCommonCodeList = XMLUtil.getElementByXPath(docGetCommonCodeListOutput,
								"/CommonCodeList/CommonCode[@CodeValue='" + strPaymentType + "' and @CodeLongDescription='POST_AUTH']");
						if (eleCommonCodeList != null) {
						    logger.info("CrocsProcessAdyenWebhooks.crocsProcessAdyenWebhooks() :Post auth logic is executing for Order No: "+strOrderNo);
							updateAuthorizationIdForPostPayments(env, eleCapture, docGetOrderListOutput, elePaymentMethod, eleCommonCodeList);
						}
					}
				}
			}

			//update the details in CROCS_ADYEN_WEBHOOKS_RES table
			updateAdyenWebhookResponse(env, inDoc);

		} catch (YFSException e) {
			logger.verbose("Exception" + e.toString());
			throw new YFSException(e.getMessage(), e.getErrorCode(), e.getErrorDescription());

		}

		logger.endTimer("CrocsProcessAdyenWebhooks.crocsProcessAdyenWebhooks");
	}

	/**
	 * Description: Calling GetOrderList with Sales Order No
	 *
	 * @param env
	 * @param strOrderNo
	 * @return
	 * @throws Exception
	 */
	public static Document getOrderListForSaleOrderNo(YFSEnvironment env, String strOrderNo) throws YFSException {

		logger.verbose("CrocsProcessAdyenWebhooks : getOrderListForSaleOrderNo: START");

		Document getOrderListOut = null;
		try {
			if (!YFCObject.isVoid(strOrderNo)) {

				Document getOrderListInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER);
				getOrderListInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_ORDER_NO, strOrderNo);
				getOrderListOut = CommonUtil.invokeService(env, CrocsConstant.CROCS_GET_ORDER_LIST_FOR_FORTER,
						getOrderListInDoc);
			}
		} catch (Exception e) {
			throw new YFSException("CrocsProcessAdyenWebhooks.getOrderListForSaleOrderNo :Expection" + e.getMessage());
		}
		logger.verbose("CrocsProcessAdyenWebhooks : getOrderListForSaleOrderNo:: END:: " + XMLUtil.getXMLString(getOrderListOut));
		return getOrderListOut;
	}

	/**
	 * Description: Calling ChangeOrder with Sales Order No to cancel the order on
	 * Auth webhook fails and Forter Fails
	 *
	 * @return
	 * @throws Exception
	 */
	public static Document cancelOrderOnAuthFailure(YFSEnvironment env,String strOrderHeaderKey, Document docChangeOrderInput) throws YFSException {

		logger.verbose("CrocsProcessAdyenWebhooks : cancelOrderOnAuthFailureAndForterFailure: START");
		try {
			if (!YFCObject.isVoid(strOrderHeaderKey)) {

				docChangeOrderInput = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER);
				Element changeOrderEle = docChangeOrderInput.getDocumentElement();
				changeOrderEle.setAttribute(CrocsXmlConstants.A_ORDER_HEADER_KEY, strOrderHeaderKey);
				changeOrderEle.setAttribute(CrocsXmlConstants.A_ACTION, VAL_ACTION_CANCEL);
				changeOrderEle.setAttribute(CrocsXmlConstants.A_OVERRIDE, CrocsXmlConstants.FLAG_Y);
				docChangeOrderInput = addNotesForCancellation(env,docChangeOrderInput,"_AUTH");
			}
		} catch (Exception e) {
			throw new YFSException("CrocsProcessAdyenWebhooks.cancelOrderOnAuthFailureAndForterFailure :Expection" + e.getMessage());
		}
		logger.verbose("CrocsProcessAdyenWebhooks : cancelOrderOnAuthFailureAndForterFailure: END");
		return docChangeOrderInput;
	}

	/**
	 * Description: Appending Action="CANCEL" when Forter gets Declined.
	 *
	 * @param docChangeOrderInput
	 * @return
	 * @throws YFSException
	 */
	public static Document cancelOrderOnForterFailure(YFSEnvironment env, Document docChangeOrderInput) throws YFSException {

		logger.verbose("CrocsProcessAdyenWebhooks : cancelOrderOnForterFailure: START");
		try {
			Element changeOrderEle = docChangeOrderInput.getDocumentElement();
			changeOrderEle.setAttribute(CrocsXmlConstants.A_ACTION, VAL_ACTION_CANCEL);
			docChangeOrderInput = addNotesForCancellation(env,docChangeOrderInput,"_FORTER");
		} catch (Exception e) {
			throw new YFSException("CrocsProcessAdyenWebhooks.cancelOrderOnForterFailure :Expection" + e.getMessage());
		}
		logger.verbose("CrocsProcessAdyenWebhooks : cancelOrderOnForterFailure: END");
		return docChangeOrderInput;
	}

	/**
	 * EOMS-10220
	 * Description: Adding Notes with Reason Code and Text as part of cancellation due to auth failure / Forter decline .
	 *
	 * @param  env
	 * @param  docChangeOrderInput
	 * @return
	 * @throws YFSException
	 */
	public static Document addNotesForCancellation(YFSEnvironment env, Document docChangeOrderInput, String strFailureCause) {
		try {

			logger.verbose("CrocsProcessAdyenWebhooks : addNotesForCancellation input Xml" + SCXmlUtil.getString(docChangeOrderInput));

			CrocsOrderCancellationNotesUtil crocsOrderCancellationNotesUtil = new CrocsOrderCancellationNotesUtil();
			String cancelReason =  crocsOrderCancellationNotesUtil.getCancellationReason(env,CrocsProcessAdyenWebhooks.class.getSimpleName()+strFailureCause);
		
			Element changeOrderEle = docChangeOrderInput.getDocumentElement();
			String noteText = ORDER_CANCEL_NOTE_TEXT.replace(CrocsConstant.CANCELLED_REASON, cancelReason);
			logger.verbose("cancel Reason : " + cancelReason);
			logger.verbose("cancellation notes : " + noteText);
			docChangeOrderInput = crocsOrderCancellationNotesUtil.getNotesTag(changeOrderEle, docChangeOrderInput, noteText, cancelReason);
						
		} catch (Exception e) {
			throw new YFSException("CrocsProcessAdyenWebhooks.addNotesForCancellation :Expection" + e.getMessage());
		}
		logger.verbose("CrocsProcessAdyenWebhooks : addNotesForCancellation Output Xml" + SCXmlUtil.getString(docChangeOrderInput));

		return docChangeOrderInput;
	}

	/**
	 * Description: This below method helps to append the payment Details tag when
	 * Webhook response success.
	 *
	 * @param env
	 * @param strOrderHeaderKey
	 * @param strPaymentKey
	 * @param strPSPReference
	 * @param strValue
	 * @param strEventDate
	 * @return
	 * @throws YFSException
	 */
	public static Document appendPaymentDetailsForPostAuthPayments(YFSEnvironment env, String strOrderHeaderKey, String strPaymentKey, String strPSPReference, String strValue, String strEventDate, Element eleCommonCodeList) throws YFSException {

		logger.verbose("CrocsProcessAdyenWebhooks : appendPaymentDetailsForPostAuthPayments: START");

		Document recordExternalChargesInDoc = null;
		try {

			if (!YFCObject.isVoid(strOrderHeaderKey) && !YFCObject.isVoid(strPaymentKey)) {

				String strAuthExpirationDate = updateAuthorizationExpirationDate(eleCommonCodeList);

				recordExternalChargesInDoc = SCXmlUtil.createDocument(A_RECORD_EXTENRAL_CHARGES);
				Element recordExternalChargesEle = recordExternalChargesInDoc.getDocumentElement();
				recordExternalChargesEle.setAttribute(CrocsXmlConstants.A_ORDER_HEADER_KEY, strOrderHeaderKey);

				Element elePaymentMethod = recordExternalChargesInDoc.createElement(E_PAYMENT_METHOD);
				elePaymentMethod.setAttribute(A_PAYMENT_KEY, strPaymentKey);
				Element elePaymentDeatilsList = recordExternalChargesInDoc.createElement(ELE_PAYMENT_DETAILS_LIST);
				Element elePaymentDeatils = recordExternalChargesInDoc.createElement(E_PAYMENT_DETAILS);
				elePaymentDeatils.setAttribute(A_TRAN_TYPE, A_ONLINE_ECOMM);
				elePaymentDeatils.setAttribute(A_PAYMENT_KEY, strPaymentKey);
				elePaymentDeatils.setAttribute(A_AUTHORIZATION_ID, strPSPReference);
				elePaymentDeatils.setAttribute(A_CHARGE_TYPE, AUTHORIZATION);
				elePaymentDeatils.setAttribute(A_REQUEST_AMOUNT, strValue);
				elePaymentDeatils.setAttribute(A_PROCESSED_AMOUNT, strValue);
				elePaymentDeatils.setAttribute(HoldAgainstBook, VAL_FLAG_Y);
				elePaymentDeatils.setAttribute(TranRequestTime, strEventDate);
				elePaymentDeatils.setAttribute(A_AUTH_EXPIRATION_DATE, strAuthExpirationDate);
				elePaymentDeatilsList.appendChild(elePaymentDeatils);
				elePaymentMethod.appendChild(elePaymentDeatilsList);
				recordExternalChargesEle.appendChild(elePaymentMethod);

				// RecordExternalCharges Call 
				logger.verbose("appendPaymentDetailsForPostAuthPayments :Record External Charges Input" + XMLUtil.getXMLString(recordExternalChargesInDoc));
				callRecordExternalChargesService(env, recordExternalChargesInDoc);

			}
		} catch (Exception e) {
			throw new YFSException("CrocsProcessAdyenWebhooks.appendPaymentDetailsForPostAuthPayments :Expection" + e.getMessage());
		}
		logger.verbose("CrocsProcessAdyenWebhooks : appendPaymentDetailsForPostAuthPayments: END");
		return recordExternalChargesInDoc;
	}

	/**
	 * Description: updating Authorization Expiration Date for Post Auth Payment Methods.
	 *
	 * @return
	 * @throws YFSException
	 */
	public static String updateAuthorizationExpirationDate(Element eleCommonCodeList) throws YFSException {

		logger.verbose("CrocsProcessAdyenWebhooks : updateAuthorizationExpirationDate: START");
		String strAuthExpirationDate;
		try {
			Calendar cal = Calendar.getInstance();
			int intShortDescription = Integer.parseInt(eleCommonCodeList.getAttribute(A_CODE_SHORT_DESCRIPTION));
			cal.add(5, intShortDescription);
			SimpleDateFormat formatter = new SimpleDateFormat(CrocsConstant.STR_DB_DATE_FORMAT);
			strAuthExpirationDate = formatter.format(cal.getTime());
		} catch (Exception e) {
			throw new YFSException("CrocsProcessAdyenWebhooks.updateAuthorizationExpirationDate :Expection" + e.getMessage());
		}
		logger.verbose("CrocsProcessAdyenWebhooks : updateAuthorizationExpirationDate: END");
		return strAuthExpirationDate;
	}

	/**
	 * Description: Below Method helps to resolve the hold and updating ExtnFraudStatus as Accept
	 *
	 * @param docChangeOrderInput
	 * @param docGetOrderListOutput
	 * @return
	 * @throws YFSException
	 */
	public static Document resolveFraudHoldOnOrder(Document docChangeOrderInput, Document docGetOrderListOutput) throws YFSException {

		logger.verbose("CrocsProcessAdyenWebhooks : ResolveFrudHoldOnOrder: START");
		try {

			Element eleOrderExtn = docChangeOrderInput.createElement(E_EXTN);
			eleOrderExtn.setAttribute(A_EXTN_FRAUD_STATUS, A_ACCEPT);
			docChangeOrderInput.getDocumentElement().appendChild(eleOrderExtn);

			Element eleHoldType = XMLUtil.getElementByXPath(docGetOrderListOutput,
					"/OrderList/Order/OrderHoldTypes/OrderHoldType[@HoldType='FRAUD_HOLD' and @Status='1100']");
			if (eleHoldType != null) {
				Element eleOrderHoldTypes = docChangeOrderInput.createElement(E_ORDER_HOLD_TYPES);
				Element eleOrderHoldType = docChangeOrderInput.createElement(E_ORDER_HOLD_TYPE);
				eleOrderHoldType.setAttribute(A_HOLD_TYPE, A_FRAUD_HOLD);
				eleOrderHoldType.setAttribute(A_STATUS, VAL_STATUS_1300);
				eleOrderHoldType.setAttribute(A_REASON_TEXT, FRAUD_RESPONSE_SUCCESS);
				eleOrderHoldTypes.appendChild(eleOrderHoldType);
				docChangeOrderInput.getDocumentElement().appendChild(eleOrderHoldTypes);
			}

		} catch (Exception e) {
			throw new YFSException("CrocsProcessAdyenWebhooks.ResolveFrudHoldOnOrder :Expection" + e.getMessage());
		}
		logger.verbose("CrocsProcessAdyenWebhooks : ResolveFrudHoldOnOrder: END");
		return docChangeOrderInput;
	}

	/**
	 * Description: Calling Change Order using input.
	 *
	 * @param env
	 * @param docChangeOrderInput
	 * @throws YFSException
	 */
	public static void callChangeOrderService(YFSEnvironment env, Document docChangeOrderInput) throws YFSException {

		logger.verbose("CrocsProcessAdyenWebhooks : callChangeOrderService: START");
		try {

			CommonUtil.invokeService(env, CROCS_CHANGE_ORDER_SERV, docChangeOrderInput);

		} catch (Exception e) {
			throw new YFSException("CrocsProcessAdyenWebhooks.callChangeOrderService :Expection" + e.getMessage());
		}
		logger.verbose("CrocsProcessAdyenWebhooks : callChangeOrderService: END");
	}

	/**
	 * Description: Calling Record External Charges using input.
	 *
	 * @param env
	 * @param docChangeOrderInput
	 * @throws YFSException
	 */
	public static void callRecordExternalChargesService(YFSEnvironment env, Document docChangeOrderInput) throws YFSException {

		logger.verbose("CrocsProcessAdyenWebhooks : callRecordExternalChargesService: START");
		try {

			CommonUtil.invokeService(env, CROCS_RECORD_EXTERNAL_CHARGES_SERV, docChangeOrderInput);

		} catch (Exception e) {
			throw new YFSException("CrocsProcessAdyenWebhooks.callRecordExternalChargesService :Expection" + e.getMessage());
		}
		logger.verbose("CrocsProcessAdyenWebhooks : callRecordExternalChargesService: END");
	}

	/**
	 * Description: Based on Forter Message, if Success resolving hold,
	 * otherwise cancelling order
	 *
	 * @param env
	 * @param docChangeOrderInput
	 * @throws YFSException
	 */
	public static Document validateFraudCheckForPostAuthPayments(YFSEnvironment env, Document docGetOrderListOutput, Document docChangeOrderInput) throws YFSException {

		logger.verbose("CrocsProcessAdyenWebhooks : ValidateFraudCheckForPostAuthPayments: START");
		CrocsCheckFraudOnOrderUserExitImpl objFraudCheck = new CrocsCheckFraudOnOrderUserExitImpl();
		try {
			if (docGetOrderListOutput != null
					&& Double.parseDouble(docGetOrderListOutput.getDocumentElement().getAttribute(A_TOTAL_ORDER_LIST)) > 0) {

				Element eleOrder = SCXmlUtil.getChildElement(docGetOrderListOutput.getDocumentElement(), E_ORDER);
				Document docForterInput = XMLUtil.getDocumentFromElement(eleOrder);
				String strOrderHeaderKey = eleOrder.getAttribute(A_ORDER_HEADER_KEY);

				docChangeOrderInput = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER);
				Element changeOrderEle = docChangeOrderInput.getDocumentElement();
				changeOrderEle.setAttribute(CrocsXmlConstants.A_ORDER_HEADER_KEY, strOrderHeaderKey);
				changeOrderEle.setAttribute(CrocsXmlConstants.A_ACTION, CrocsConstant.VAL_MODIFY);
				changeOrderEle.setAttribute(CrocsXmlConstants.A_OVERRIDE, CrocsXmlConstants.FLAG_Y);

				//Calling Fraud Check Java class for Validation
				logger.info("CrocsProcessAdyenWebhooks.validateFraudCheckForPostAuthPayments() :Fraud check for OrderHeaderKey: "+strOrderHeaderKey);
				Document docForterOutput = objFraudCheck.processOrderFraudCheck(env, docForterInput);
				logger.info("CrocsProcessAdyenWebhooks.validateFraudCheckForPostAuthPayments() :Forter Response: "+SCXmlUtil.getString(docForterOutput));
				/*
				 * if Response is SUCCESS will resolve FRAUD_HOLD on order else will cancel the order
				 */
				String strForterMessage = docForterOutput.getDocumentElement().getAttribute(A_FRAUD_CHECK_RESPONSE_CODE);
				if (V_SUCCESS.equalsIgnoreCase(strForterMessage)) {
					docChangeOrderInput = resolveFraudHoldOnOrder(docChangeOrderInput, docGetOrderListOutput);
				} else if (V_FAILED.equalsIgnoreCase(strForterMessage)) {
					docChangeOrderInput = cancelOrderOnForterFailure(env,docChangeOrderInput);
				}
			}

		} catch (Exception e) {

			docChangeOrderInput = updateFraudStatusOnException(docChangeOrderInput);
			logger.verbose("CrocsProcessAdyenWebhooks.ValidateFraudCheckForPostAuthPayments :Expection" + e.getMessage());
		}
		logger.verbose("CrocsProcessAdyenWebhooks : ValidateFraudCheckForPostAuthPayments: END");
		return docChangeOrderInput;
	}

	/**
	 * Description: Updating ExtnFraudStatus as RETRY, When Forter Exception Caught and Again forter call will
	 * happen using monitor rule.
	 *
	 * @param docChangeOrderInput
	 * @return
	 * @throws YFSException
	 */
	public static Document updateFraudStatusOnException(Document docChangeOrderInput) throws YFSException {

		logger.verbose("CrocsProcessAdyenWebhooks : updateFraudStatusOnException: START");
		try {

			Element eleOrderExtn = docChangeOrderInput.createElement(E_EXTN);
			eleOrderExtn.setAttribute(A_EXTN_FRAUD_STATUS, A_RETRY);
			docChangeOrderInput.getDocumentElement().appendChild(eleOrderExtn);

		} catch (Exception e) {
			throw new YFSException("CrocsProcessAdyenWebhooks.updateFraudStatusOnException :Expection" + e.getMessage());
		}
		logger.verbose("CrocsProcessAdyenWebhooks : updateFraudStatusOnException: END");
		return docChangeOrderInput;
	}

	/**
	 * Description: Below method checks the Authorization webhook response,
	 * Based on Success flag, updating order
	 *
	 * @param env
	 * @param eleCapture
	 * @throws YFSException
	 */
	public static void updateAuthorizationIdForPostPayments(YFSEnvironment env, Element eleCapture,
															Document docGetOrderListOutput, Element elePaymentMethod, Element eleCommonCodeList) throws YFSException {

		logger.verbose("CrocsProcessAdyenWebhooks : updateAuthorizationIdForPostPayments: START");
		CrocsCheckFraudOnOrderUserExitImpl objFraudCheck = null;
		Document docChangeOrderInput = null;
		try {
			String strEventDate = eleCapture.getAttribute(C_EVENT_DATE);
			String strPSPReference = eleCapture.getAttribute(C_PSPREFERENCE);
			String strValue = eleCapture.getAttribute(A_VALUE);
			String strSuccess = eleCapture.getAttribute(C_SUCCESS);

			Element eleOrderList = docGetOrderListOutput.getDocumentElement();
			Element eleOrder = SCXmlUtil.getChildElement(eleOrderList, E_ORDER);
			String strOrderHeaderKey = eleOrder.getAttribute(A_ORDER_HEADER_KEY);
			eleOrder.setAttribute(C_SUCCESS, strSuccess);

			String strPaymentKey = elePaymentMethod.getAttribute(A_PAYMENT_KEY);
			double dValue = Double.parseDouble(strValue);
			dValue = dValue * Math.pow(10, -2);
			strValue = String.valueOf(dValue);
			if (A_TRUE_STRING.equalsIgnoreCase(strSuccess)) {

				// Appending Payment Details Tag
				appendPaymentDetailsForPostAuthPayments(env, strOrderHeaderKey, strPaymentKey,
						strPSPReference, strValue, strEventDate, eleCommonCodeList);

				// Forter Call
				docChangeOrderInput = validateFraudCheckForPostAuthPayments(env, docGetOrderListOutput, docChangeOrderInput);

			} else {
				/**
				 * Calling ChangeOrder to cancel order
				 * On failure, sends an update to Forter for post-auth payments.
				 */
				docChangeOrderInput = cancelOrderOnAuthFailure(env,strOrderHeaderKey, docChangeOrderInput);
				objFraudCheck = new CrocsCheckFraudOnOrderUserExitImpl();
				Document docForterInput = XMLUtil.getDocumentFromElement(eleOrder);
				objFraudCheck.processOrderFraudCheck(env, docForterInput);
			}

		} catch (Exception e) {
			logger.verbose(
					"CrocsProcessAdyenWebhooks.updateAuthorizationIdForPostPayments :Expection" + e.getMessage());
			throw new YFSException(
					"CrocsProcessAdyenWebhooks.updateAuthorizationIdForPostPayments :Expection" + e.getMessage());
		} finally {
			// changeOrdercall
			logger.verbose("CrocsProcessAdyenWebhooks: Change Order Input" + XMLUtil.getXMLString(docChangeOrderInput));
			callChangeOrderService(env, docChangeOrderInput);
		}
		logger.verbose("CrocsProcessAdyenWebhooks : updateAuthorizationIdForPostPayments: END");
	}

	/**
	 * EOMS 3698 ->
	 * this method update the records in CROCS_ADYEN_WEBHOOKS_RES table
	 *If record is not present will insert into the table
	 * @param inDoc input Doc
	 */
	private static void updateAdyenWebhookResponse(YFSEnvironment env, Document inDoc) throws RemoteException {

		try {
			Element eleCapture = inDoc.getDocumentElement();
			Document createCaptureIndoc = SCXmlUtil.createDocument(C_CAPTURE_INFO);
			Element createCaptureEle = createCaptureIndoc.getDocumentElement();
			createCaptureEle.setAttribute(C_PSPREFERENCE, eleCapture.getAttribute(C_PSPREFERENCE));
			logger.verbose("getCaptureInforList Indoc" + createCaptureIndoc);
			Document getCaptureInfoList = CommonUtil.invokeService(env, CrocsIVAPIConstants.GET_CAPTURE_INFO_LIST_SERVICE, createCaptureIndoc);
			logger.verbose("getCaptureInforList outdoc" + getCaptureInfoList);

			if (!YFCCommon.isVoid(getCaptureInfoList) && getCaptureInfoList.getDocumentElement().hasChildNodes()) {
				updateCaptureInfo(env, eleCapture, getCaptureInfoList);
			} else {
				Document createCaptureInfo = CommonUtil.invokeService(env, CrocsIVAPIConstants.CREATE_CAPTURE_INFO_SERVICE, inDoc);
				logger.verbose("createCaptureInfo outdoc" + createCaptureInfo);
			}
		} catch (Exception e) {
			logger.verbose("Error in updateAdyenWebhookResponse : "
					+ e.getLocalizedMessage());
			throw new YFSException(" Error in updateAdyenWebhookResponse  in CrocsProcessAdyenWebhooks class :" + e.getMessage());
		}
	}

	private static void updateCaptureInfo(YFSEnvironment env, Element eleCapture, Document getCaptureInfoList) throws RemoteException {
		try {
			Element getCaptureInfoListEle = getCaptureInfoList.getDocumentElement();
			Element captureInfoEle = SCXmlUtil.getChildElement(getCaptureInfoListEle, C_CAPTURE_INFO);
			String adyenWebhookKey = captureInfoEle.getAttribute(C_ADYEN_WEBHOOK_KEY);
			//Update the record
			Document updateAdyenResponseIndoc = SCXmlUtil.createDocument(C_CAPTURE_INFO);
			Element updateAdyenResponseEle = updateAdyenResponseIndoc.getDocumentElement();
			updateAdyenResponseEle.setAttribute(C_ADYEN_WEBHOOK_KEY, adyenWebhookKey);
			updateAdyenResponseEle.setAttribute(C_EVENT_CODE, eleCapture.getAttribute(C_EVENT_CODE));
			updateAdyenResponseEle.setAttribute(C_PSPREFERENCE, eleCapture.getAttribute(C_PSPREFERENCE));
			updateAdyenResponseEle.setAttribute(C_EVENT_DATE, eleCapture.getAttribute(C_EVENT_DATE));
			updateAdyenResponseEle.setAttribute(C_MERCHANT_ACCOUNT_CODE, eleCapture.getAttribute(C_MERCHANT_ACCOUNT_CODE));
			updateAdyenResponseEle.setAttribute(C_ORIGINAL_REFERENCE, eleCapture.getAttribute(C_ORIGINAL_REFERENCE));
			updateAdyenResponseEle.setAttribute(C_REASON, eleCapture.getAttribute(C_REASON));
			updateAdyenResponseEle.setAttribute(C_SUCCESS, eleCapture.getAttribute(C_SUCCESS));
			updateAdyenResponseEle.setAttribute(A_ORDER_NO, eleCapture.getAttribute(A_ORDER_NO));
			updateAdyenResponseEle.setAttribute(A_PAYMENT_METHOD, eleCapture.getAttribute(A_PAYMENT_METHOD));
			updateAdyenResponseEle.setAttribute(A_CURRENCY, eleCapture.getAttribute(A_CURRENCY));
			updateAdyenResponseEle.setAttribute(A_VALUE, eleCapture.getAttribute(A_VALUE));
			logger.verbose("updateCaptureInfo inDoc" + updateAdyenResponseIndoc);
				
			Document updateCaptureInfo = CommonUtil.invokeService(env, CrocsIVAPIConstants.CHANGE_CAPTURE_INFO_SERVICE, updateAdyenResponseIndoc);
			logger.verbose("updateCaptureInfo outdoc" + updateCaptureInfo);
		} catch (Exception e) {
			logger.verbose("Error in updateCaptureInfo : "
					+ e.getLocalizedMessage());
			throw new YFSException(" Error in updateCaptureInfo  in CrocsProcessAdyenWebhooks class :" + e.getMessage());

		}
	}
}
