package com.crocs.oms.payment;

import java.rmi.RemoteException;
import java.util.ArrayList;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsIVAPIConstants;
import com.crocs.oms.util.ue.CrocsCollectionCreditCardWrapper;
import com.yantra.yfs.japi.YFSEnvironment;
import org.apache.commons.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.crocs.oms.order.CrocsOrderUpdate;
import com.crocs.oms.util.ue.CrocsAdyenCaptureRequest;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSException;

/*
 * EOMS-2025 : Implemented logic to void the Authorization on full order cancel 
 * 
 */
public class CrocsAuthVoidOnFullCancel implements CrocsConstant{
	
	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsOrderUpdate.class);

	public Document crocsAuthVoidOnFullCancel(YFSEnvironment env, Document inDoc ) throws Exception {
		
		logger.beginTimer("CrocsAuthReversalOnFullCancel.crocsAuthVoidOnFullCancel() : Begin");
		logger.verbose("Starting CrocsAuthReversalOnFullCancel.crocsAuthVoidOnFullCancel() method");
		logger.verbose("Input for crocsAuthVoidOnFullCancel: " + XMLUtil.getXMLString(inDoc));
		
		try {
			
			Element eleOrder = inDoc.getDocumentElement();
			String strOrderStatus = eleOrder.getAttribute(CrocsXmlConstants.A_STATUS);
			String strMaxOrderStatus = eleOrder.getAttribute(CrocsXmlConstants.A_MAX_ORDER_STATUS);
			String strMinOrderStatus = eleOrder.getAttribute(CrocsXmlConstants.A_MIN_ORDER_STATUS);

			if (strOrderStatus.equalsIgnoreCase(CrocsConstant.STR_CANCELLED)
					&& strMaxOrderStatus.equalsIgnoreCase(CrocsConstant.STR_STATUS_CANCELLED)
					&& strMinOrderStatus.equalsIgnoreCase(CrocsConstant.STR_STATUS_CANCELLED)) {

				Element paymentMethodsEle = SCXmlUtil.getChildElement(eleOrder, CrocsXmlConstants.E_PAYMENT_METHODS);
				ArrayList<Element> paymentMethods = SCXmlUtil.getChildren(paymentMethodsEle, CrocsXmlConstants.E_PAYMENT_METHOD);
				for (Element paymentMethod : paymentMethods) {
					//EOMS- 676 Call Adyen for Paypal Orders
					String strPaymentReference4 = paymentMethod.getAttribute(CrocsXmlConstants.A_PAYMENT_REFERENCE4);
					if (!YFCObject.isVoid(strPaymentReference4) && CrocsConstant.STR_BRAIN_TREE.equals(strPaymentReference4)) {
						//Call braintree void

					} else {
						//Prepare Input for Adyen to void Authorization
						prepareAdyenAuthVoidOnCancel(env, paymentMethod, eleOrder);
					}
				}

			}

		} catch (Exception e) {

			logger.verbose("CrocsAuthVoidOnFullCancel.crocsAuthVoidOnFullCancel :Expection" + e.getMessage());
			throw new YFSException("CrocsAuthVoidOnFullCancel.crocsAuthVoidOnFullCancel :Expection" + e.getMessage());
		}

		logger.verbose("OutputStruct for crocsAuthVoidOnFullCancel: " + inDoc);
		logger.endTimer("CrocsAuthVoidOnFullCancel.crocsAuthVoidOnFullCancel() : End");

		return inDoc;
	}

	/**
	 * Below method used to form JSON Request and hit Adyen URL
	 *
	 * @param env
	 * @param paymentMethod
	 * @return
	 * @throws Exception
	 */
	private Document prepareAdyenAuthVoidOnCancel(YFSEnvironment env, Element paymentMethod, Element eleOrder) throws Exception {

		logger.beginTimer("CrocsAuthReversalOnFullCancel.prepareAdyenAuthVoidOnCancel() : Begin");
		logger.verbose("Starting CrocsAuthReversalOnFullCancel.prepareAdyenAuthVoidOnCancel() method");

		String strJSONPayload = null;
		Document adyenInputDoc = null;
		Document adyenResponseOutDoc = null;
		CrocsAdyenCaptureRequest adyenCaptureObj = null;

		try {
			String sMerchantAcc = paymentMethod.getAttribute(CrocsConstant.PaymentReference2);
			String pspreference = paymentMethod.getAttribute(CrocsConstant.PaymentReference5);
			String sReference = SCXmlUtil.getXpathAttribute(paymentMethod, "ChargeTransactionDetails/ChargeTransactionDetail[@ChargeType='AUTHORIZATION']/@ChargeTransactionKey");
			String orderHeaderKey = eleOrder.getAttribute(CrocsXmlConstants.A_ORDER_HEADER_KEY);
			String documentType = eleOrder.getAttribute(CrocsXmlConstants.A_DOCUMENT_TYPE);
			String orderNo = eleOrder.getAttribute(CrocsXmlConstants.A_ORDER_NO);

			JSONObject jsonPayloadObj = new JSONObject();
			JSONObject jsonOrderObj = new JSONObject();
			String reqPayload = null;

			jsonOrderObj.put(CrocsConstant.ADYEN_MERCHANT_ACCOUNT, sMerchantAcc);
			//EOMS -3698 pass Order No to Adyen in MerchantReference instead of ChargeTransactionKey
			jsonOrderObj.put(CrocsConstant.reference, orderNo);
			jsonPayloadObj.put(CrocsConstant.STR_BODY, jsonOrderObj);
			jsonPayloadObj.put(CrocsConstant.ADYEN_PSP_REFERENCE, pspreference);
			reqPayload = jsonPayloadObj.toString();

			//Creating new Request Document for Adyen 
			adyenInputDoc = SCXmlUtil.createDocument(CrocsConstant.STR_ADYEN_REQUEST);
			Element adyenInputdocEle = adyenInputDoc.getDocumentElement();
			adyenInputdocEle.setAttribute(CrocsConstant.STR_ADEYN_REQUEST_PAYLOAD, reqPayload);
			adyenInputdocEle.setAttribute(CrocsConstant.STR_ADYEN_REQUEST, CrocsXmlConstants.A_EVENT_CODE_CANCEL);


			/**
			 * Printing the adyen input, then calling the adyen end point
			 * */
			logger.verbose("Input for crocsAuthVoidOnFullCancel: " + XMLUtil.getXMLString(adyenInputDoc));
			adyenCaptureObj = new CrocsAdyenCaptureRequest();
			strJSONPayload = adyenCaptureObj.crocsAdyenCaptureRequest(adyenInputDoc);
			//create Capture Info in CROCS_ADYEN_WEBHOOKS_RES
			createCaptureInfo(env, sReference, orderHeaderKey, documentType, orderNo,strJSONPayload);

			adyenResponseOutDoc = SCXmlUtil.createDocument(CrocsConstant.STR_ADYEN_RESPONSE);
			Element adyenOutputdocEle = adyenResponseOutDoc.getDocumentElement();
			adyenOutputdocEle.setAttribute(CrocsConstant.STR_ADEYN_RESPONSE_PAYLOAD, strJSONPayload);
			logger.verbose("Output from Adyen " + XMLUtil.getXMLString(adyenResponseOutDoc));

		} catch (Exception e) {
			logger.verbose("Exception in crocsAuthVoidOnFullCancel.prepareAdyenAuthVoidOnCancel" + e);
			throw new YFSException("crocsAuthVoidOnFullCancel.prepareAdyenAuthVoidOnCancel :Expection" + e.getMessage());
		}

		logger.endTimer("crocsAuthVoidOnFullCancel.prepareAdyenAuthVoidOnCancel() : End");
		return adyenResponseOutDoc;

	}

	/**EOMS 3698 ->
	 * this method is used to insert capture Info details in CROCS_ADYEN_WEBHOOKS_RES table
	 *
	 * @param env            env
	 * @param sReference     sReference
	 * @param orderHeaderKey orderHeaderKey
	 * @param documentType   documentType
	 * @param
	 * @throws RemoteException Exception
	 * @throws JSONException   Exception
	 */
	private static void createCaptureInfo(YFSEnvironment env, String sReference, String orderHeaderKey, String documentType,String orderNo, String strJSONPayload) throws RemoteException, JSONException {
		try {
			JSONObject jsonPayload = new JSONObject(strJSONPayload);
			if (jsonPayload.get(CrocsConstant.ADYEN_PSP_REFERENCE).toString() != null) {
				Document createCaptureIndoc = SCXmlUtil.createDocument(C_CAPTURE_INFO);
				Element createCaptureEle = createCaptureIndoc.getDocumentElement();
				createCaptureEle.setAttribute(ChargeTransactionKey, sReference);
				createCaptureEle.setAttribute(OrderHeaderKey, orderHeaderKey);
				createCaptureEle.setAttribute(DocumentType, documentType);
				createCaptureEle.setAttribute(C_PSPREFERENCE, jsonPayload.get(CrocsConstant.ADYEN_PSP_REFERENCE).toString());
				if(A_RETURN_ORDER_DOCUMENT_TYPE.equalsIgnoreCase(documentType)){
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
			throw new YFSException(" Error in createCaptureInfo outdoc in CrocsAuthVoidOnFullCancel class :" + e.getMessage());
		}
	}
}
