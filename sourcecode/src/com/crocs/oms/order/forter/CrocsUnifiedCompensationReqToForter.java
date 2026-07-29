package com.crocs.oms.order.forter;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import com.yantra.yfc.util.YFCDoubleUtils;

import com.crocs.oms.common.util.CrocsXmlConstants;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsAPIConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCException;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class CrocsUnifiedCompensationReqToForter implements CrocsConstant {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsUnifiedCompensationReqToForter.class);

	public enum CompensationType {
		RESHIP, RETURN, APPEASEMENT, NOT_APPLICABLE
	}

	Double capturedAmount = 0.0;

	/**
	 * @param env
	 * @param inDoc
	 * @throws Exception
	 */
	public void processCompensationUpdateToForter(YFSEnvironment env, Document inDoc) throws Exception {

		logger.beginTimer("CrocsUnifiedCompensationReqToForter : processCompensationUpdateToForter");
		Element inDocEle = inDoc.getDocumentElement();

		CompensationType compType = determineCompensationType(inDocEle);
		logger.info("processCompensationUpdateToForter :" + SCXmlUtil.getString(inDocEle) + "CompensationType:" + compType);
		publishCompensationRequest(env, inDoc, compType);
		logger.endTimer("CrocsUnifiedCompensationReqToForter : processCompensationUpdateToForter");
	}
	
	/**
	 * @param inDocEle
	 * @return
	 */
	private CompensationType determineCompensationType(Element inDocEle) {
		logger.beginTimer("CrocsUnifiedCompensationReqToForter : determineCompensationType");
		/**
		 * CompensationType is decided on below factor
		 * Appeasement :-  OrderInvoice will always be available when customer gets appeased.
		 * Return :- DocumentType is 0003 and there should be return reason.
		 * ReShip :-
		 * 
		 **/

		String rootName = inDocEle.getNodeName();



		if ("OrderInvoice".equals(rootName)) {
			logger.info("CompensationType"+CompensationType.APPEASEMENT +"applicble for Order_no: "+inDocEle.getAttribute(A_ORDER_NO));
			return CompensationType.APPEASEMENT;

		}

		
		else if ("OrderRelease".equals(rootName)) {
			logger.info("CompensationType"+CompensationType.RESHIP +"applicble for Order_no: "+inDocEle.getAttribute(A_ORDER_NO));
			return CompensationType.RESHIP;
		}

		/** validate from chainedFromOrderHeaderKey	, documentType, */
		else if ("Order".equals(rootName)) {
			Element tempOrderLineEle = SCXmlUtil.getXpathElement(inDocEle, "//OrderLine");
			if(tempOrderLineEle != null) {
				String tempDocumentType = inDocEle.getAttribute("DocumentType");
				String tempReturnReason = tempOrderLineEle.getAttribute("ReturnReason");
				if (("0003".equals(tempDocumentType)) && (!YFCCommon.isVoid(tempReturnReason))) {
					logger.info("CompensationType"+CompensationType.RETURN +"applicble for Order_no: "+inDocEle.getAttribute(A_ORDER_NO));
					return CompensationType.RETURN;
				}
			}
		}
		logger.info("CompensationType"+CompensationType.NOT_APPLICABLE +"applicble for Order_no: "+inDocEle.getAttribute(A_ORDER_NO));
			return CompensationType.NOT_APPLICABLE;

	}
	
	public Document publishCompensationRequest(YFSEnvironment env, Document inDoc, CompensationType compType)
			throws Exception {
		logger.beginTimer("CrocsUnifiedCompensationReqToForter : publishCompensationRequest");
		try {
			Element inputDoc = inDoc.getDocumentElement();
			switch(compType){
				case APPEASEMENT:
					prepareForterAppeasementInput(env,inputDoc,compType);
					break;

				case RESHIP:
					prepareForterReshipAndReturnInput(env,inDoc,compType);
					break;

				case RETURN:
					prepareForterReshipAndReturnInput(env,inDoc,compType);
					break;

				default:
					logger.info("compensation type: "+compType+" didn't match as this call is for regulat Order "+inputDoc.getAttribute("OrderNo"));

			}
			//CrocsForterUtil.invokeForter(env, orderNo, orderHdrKey, enterpriseCode, payload, "compensation");
		} catch (YFCException e) {
			logger.error("Failed to publish UCR", "" , e.getStackTrace());
			throw new YFSException("CrocsUnifiedCompensationReqToForter", "UCR_FAILURE",
					"Publish Compensation Request failed" + e.getMessage());
		}

		logger.endTimer("CrocsUnifiedCompensationReqToForter : publishCompensationRequest");
		return inDoc;
	}

	/**
	 * @param env
	 * @param inDoc
	 * @param compType
	 * @return
	 * @throws Exception
	 */
	public Document prepareForterReshipAndReturnInput(YFSEnvironment env, Document inDoc, CompensationType compType)
			throws Exception {

		logger.beginTimer("CrocsUnifiedCompensationReqToForter : publishCompensationRequest");

		Element orderEle = inDoc.getDocumentElement();
		String orderNo = orderEle.getAttribute(A_ORDER_NO);
		String orderHdrKey = orderEle.getAttribute(A_ORDER_HEADER_KEY);
		String enterpriseCode = orderEle.getAttribute(A_ENTERPRISE_CODE);

		logger.info("Publishing UCR to Forter. CompensationType=" + compType);// give the order No

		try {
			Element listOrderEle = CrocsForterUtil.fetchOrderList(env, orderHdrKey);

			JSONObject payload = prepareInputToForter(env, orderEle, listOrderEle, compType);
			logger.info("Payload for Forter UCR : " + payload);

			CrocsForterUtil.invokeForter(env, orderNo, orderHdrKey, enterpriseCode, payload, "compensation");

		} catch (Exception e) {
			logger.error("Failed to publish UCR", e);
			throw new YFSException("CrocsUnifiedCompensationReqToForter", "UCR_FAILURE",
					"Publish Compensation Request failed");
		}

		logger.endTimer("CrocsUnifiedCompensationReqToForter : publishCompensationRequest");
		return inDoc;
	}

	private void prepareForterAppeasementInput(YFSEnvironment env ,Element orderInvoiceEle,CompensationType compType) {
		Document docGetOrderInvoiceOutput = null;
		try {
			String strOrderInvoiceKey = orderInvoiceEle.getAttribute(CrocsXmlConstants.A_ORDER_INVOICE_KEY);
			// preparing input for GetOrderInvoiceDetails
			Document getOrderInvoiceDetailsInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_GET_ORDER_INVOICE_DETAILS);
			getOrderInvoiceDetailsInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_INVOICE_KEY, strOrderInvoiceKey);

			docGetOrderInvoiceOutput = CommonUtil.invokeService(env, CrocsConstant.STR_CROCS_GET_ORDER_INVOICE_DETAILS_SERV,
					getOrderInvoiceDetailsInDoc);

			logger.info("CrocsCompensationUpdateToForter : publishAppeasementToForter : getOrderListOutDoc is: "
					+ SCXmlUtil.getString(docGetOrderInvoiceOutput));

			Element orderInvoiceDetailsEle = docGetOrderInvoiceOutput.getDocumentElement();
			Element orderEle = SCXmlUtil.getXpathElement(orderInvoiceDetailsEle, "/InvoiceDetail/InvoiceHeader/Order");
			String orderNo = orderEle.getAttribute(A_ORDER_NO);
			String strEnterpriseCode = orderEle.getAttribute(CrocsConstant.A_ENTERPRISE_CODE);
			String orderHdrKey = orderEle.getAttribute(A_ORDER_HEADER_KEY);

			JSONObject appesementDetails = prepareInputToForterForAppeasement(env, orderInvoiceDetailsEle,compType);
			logger.verbose("Final UCR request to forter: CrocsCompensationUpdateToForter.getBuildForterAppeasementInput" +appesementDetails.toString());
			CrocsForterUtil.invokeForter(env, orderNo, orderHdrKey, strEnterpriseCode, appesementDetails, "compensation");
		}
		catch(Exception e) {
			logger.info("CrocsCompensationUpdateToForter.getBuildForterAppeasementInput :Expection" + SCXmlUtil.getString(docGetOrderInvoiceOutput));
			logger.verbose("CrocsCompensationUpdateToForter.getBuildForterAppeasementInput :Expection" + e.getMessage());
			throw new YFSException("Error invoking getCommonCodeList API: " + e.getMessage());
		}
	}

	/* ------------------- PAYLOAD BUILDER --------------------------- */

	/**
	 * @param env
	 * @param orderEle
	 * @param listOrderEle
	 * @param compType
	 * @return
	 * @throws Exception
	 */
	private JSONObject prepareInputToForter(YFSEnvironment env, Element orderEle, Element listOrderEle,
											CompensationType compType) throws Exception {
		logger.beginTimer("CrocsUnifiedCompensationReqToForter : prepareInputToForter");

		Double appeasedAmount = 0.0;

		if(compType == CompensationType.RETURN) {

			String salesOrderNo = SCXmlUtil.getXpathAttribute(orderEle, XPATH_DERIVED_ORDER_NO);
			
			if (!YFCCommon.isVoid(salesOrderNo)) {
				Document getOrderInvoiceListInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER_INVOICE);
				getOrderInvoiceListInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_ORDER_NO, salesOrderNo);
				getOrderInvoiceListInDoc.getDocumentElement().setAttribute("LatestFirst", "N");
				logger.info("Input for GetOrderInvoiceList in CrocsUCRStatusUpdateToForter.prepareForterAppeasementStatusUpdateInput :" + getOrderInvoiceListInDoc);

				Document getOrderInvoiceListOut = CommonUtil.invokeAPI(env,"",CrocsAPIConstants.API_GET_ORDER_INVOICE_LIST,getOrderInvoiceListInDoc);
				logger.info("Output of GetOrderInvoiceList in CrocsUCRStatusUpdateToForter.prepareForterAppeasementStatusUpdateInput :" + getOrderInvoiceListOut);

				Element orderInvoiceEle = getOrderInvoiceListOut.getDocumentElement();

				if (getOrderInvoiceListOut != null) {
					NodeList invoiceList = SCXmlUtil.getXpathNodes(orderInvoiceEle,
							"//OrderInvoice[@InvoiceType='CREDIT_MEMO']");

					for (int i = 0; i < invoiceList.getLength(); i++) {
						Element invoiceEle = (Element) invoiceList.item(i);
						String totalAmountStr = invoiceEle.getAttribute("TotalAmount");

						if (!YFCCommon.isVoid(totalAmountStr)) {
							appeasedAmount += Math.abs(Double.parseDouble(totalAmountStr));
						}
					}
				}
			}
		}
		
		// Attach sample

		long eventTime = System.currentTimeMillis();
		String currency = SCXmlUtil.getXpathAttribute(orderEle,"//PriceInfo/@Currency");

		JSONObject root = buildEventDetails(orderEle, listOrderEle, eventTime,compType);

		/* ---------------- Compensation Request ------------- */

		JSONObject compReq = new JSONObject();
		compReq.put(A_INITIATION_TYPE_F, getInitiationType(SCXmlUtil.getXpathAttribute(orderEle,"@EntryType")));

		JSONArray items = new JSONArray();
		NodeList ListOrderLines = SCXmlUtil.getXpathNodes(orderEle,"OrderLines/OrderLine");

		for (int i = 0; i < ListOrderLines.getLength(); i++) {

			Element listOrdLine = (Element) ListOrderLines.item(i);
			String primeLineNo = listOrdLine.getAttribute(A_PRIME_LINE_NO);

			Element orderLineEle = SCXmlUtil.getXpathElement(orderEle,
					"//OrderLine[@PrimeLineNo='" + primeLineNo + "']");

			if(!YFCCommon.isVoid(orderLineEle)) {

				Map<String, String> compDetails = prepareCompensationDetailsJObj(env, compType, orderEle, orderLineEle);

				JSONObject basicItem = buildBasicItemData(listOrdLine, listOrdLine, currency);
				JSONObject itemComp = buildItemCompensationData(env, compDetails, eventTime,V_SHIP_TO_WAREHOUSE);

				JSONObject item = new JSONObject();
				item.put(BASICITEMDATA_F, basicItem);
				item.put(A_ITEM_COMPENSATION_DATA_F, itemComp);

				items.put(item);
			}
		}

		compReq.put(A_ITEMS_F, items);

		JSONObject totalAmount = new JSONObject();
		Double totalAmountNumber = Double.parseDouble(SCXmlUtil.getXpathAttribute(orderEle,"//PriceInfo/@TotalAmount"));

		if(compType.equals(CompensationType.RETURN)) {
			if(!YFCCommon.isVoid(appeasedAmount)) {
				totalAmountNumber = YFCDoubleUtils.roundOff((totalAmountNumber - appeasedAmount), 2);
			}

		}
		totalAmount.put(A_AMOUNT_LOCAL_CURRENCY, String.valueOf(totalAmountNumber));
		totalAmount.put(A_CURRENCY_F, currency);

		compReq.put(A_TOTAL_REQ_AMOUNT_F, totalAmount);
		root.put(A_COMPENSATION_REQ_F, compReq);

		Map<String, String> security = CrocsForterUtil
				.getCustomerSecurityDetails(SCXmlUtil.getChildElement(listOrderEle, E_CUSTOM_ATTRIBUTES));

		JSONObject conn = new JSONObject();
		conn.put(A_CUSTOMER_IP_F, security.get("CUSTOMER_IP"));
		conn.put(A_USER_AGENT_F, security.get("USER_AGENT"));

		root.put(A_CONNECTION_INFORMATION_F, conn);

		logger.info("Final Forter UCR Payload:\n" + root.toString(2));
		logger.endTimer("CrocsUnifiedCompensationReqToForter : prepareInputToForter");
		return root;
	}

	/* ============== PAYLOAD BUILDER for Appeasement===================== */
	public JSONObject prepareInputToForterForAppeasement(YFSEnvironment env, Element orderInvoiceDetailsEle,CompensationType compType) throws Exception {

		logger.beginTimer("CrocsCompensationUpdateToForter.prepareInputToForter(): Begin");
		logger.info("prepareInputToForter: Begin :" + XMLUtil.getElementXMLString(orderInvoiceDetailsEle));

		Element invoiceHeaderEle = SCXmlUtil.getChildElement(orderInvoiceDetailsEle,E_INVOICE_HEADER);
		Element lineDetailsEle= SCXmlUtil.getXpathElement(orderInvoiceDetailsEle,"/InvoiceDetail/InvoiceHeader/LineDetails");
		int totalLines = Integer.parseInt(lineDetailsEle.getAttribute("TotalLines"));
		Element orderEle = SCXmlUtil.getXpathElement(orderInvoiceDetailsEle,"/InvoiceDetail/InvoiceHeader/Order");

		long eventTime = System.currentTimeMillis();
		String currency = SCXmlUtil.getXpathAttribute(orderEle, "./PriceInfo/@Currency");
		String appeasedAmount =SCXmlUtil.getXpathAttribute(orderInvoiceDetailsEle,"/InvoiceDetail/InvoiceHeader/@TotalDiscount");

		/*-----------Event details---------*/
		JSONObject appesementDetails = buildEventDetails(orderEle,orderEle, eventTime,compType);


		/* ================= Connection Info ===================== */
		Map<String, String> security = CrocsForterUtil
				.getCustomerSecurityDetails(SCXmlUtil.getChildElement(orderEle, E_CUSTOM_ATTRIBUTES));

		JSONObject conn = new JSONObject();
		conn.put(A_CUSTOMER_IP_F, security.get("CUSTOMER_IP"));
		conn.put(A_USER_AGENT_F, security.get("USER_AGENT"));

		appesementDetails.put(A_CONNECTION_INFORMATION_F, conn);


		/* ---------------- Compensation Request ------------- */

		JSONObject compensationRequest = new JSONObject();
		compensationRequest.put(A_INITIATION_TYPE_F, getInitiationType(orderEle.getAttribute(A_ENTRY_TYPE)));

		/*-----------------totalReqAmount----------------------*/
		JSONObject totalReqAmount = new JSONObject();
		totalReqAmount.put(A_AMOUNT_LOCAL_CURRENCY, appeasedAmount);
		totalReqAmount.put(A_CURRENCY_F,currency);
		compensationRequest.put(A_TOTAL_REQ_AMOUNT_F,totalReqAmount);
		appesementDetails.put(A_COMPENSATION_REQ_F, compensationRequest);

		JSONArray items = new JSONArray();
		if(!YFCCommon.isVoid(totalLines) && totalLines == 0 ) {
			NodeList orderListLines = orderEle.getElementsByTagName(E_ORDER_LINE);
			for (int i = 0; i < orderListLines.getLength(); i++) {
				Element eleOrderLine = (Element) orderListLines.item(i);
				String quantity = eleOrderLine.getAttribute(A_ORDERED_QTY);
				Element eleItem = SCXmlUtil.getChildElement(eleOrderLine, E_ITEM);
				Element lineOverallTotalsEle = SCXmlUtil.getChildElement(eleOrderLine, A_LINE_OVERALL_TOTALS);
				String lineTotal = lineOverallTotalsEle.getAttribute(A_LINE_TOTAL);

				JSONObject item = new JSONObject();

				// basicItemData
				JSONObject basicItemData = new JSONObject();
				basicItemData.put(A_PRODUCTID_F, eleItem.getAttribute(A_ITEM_ID));
				basicItemData.put(A_NAME_F, eleItem.getAttribute(A_ITEM_DESC));
				basicItemData.put(A_QUANTITY_F, quantity);
				basicItemData.put(A_TYPE_F, TANGEABLE);

				JSONObject price = new JSONObject();
				price.put(A_AMOUNT_LOCAL_CURRENCY, lineTotal);
				price.put(A_CURRENCY_F, currency);
				basicItemData.put(A_PRICE_F, price);
				item.put(BASICITEMDATA_F, basicItemData);

				Map<String, String> compDetails = prepareCompensationDetailsJObj(env,compType, invoiceHeaderEle, eleOrderLine);
				JSONObject itemComp = buildItemCompensationData(env, compDetails, eventTime,A_NO_RETURN);
				item.put(A_ITEM_COMPENSATION_DATA_F, itemComp);

				items.put(item);
			}
		}
		compensationRequest.put(A_ITEMS_F, items);

		logger.info("Final UCR request to forter: CrocsCompensationUpdateToForter.getBuildForterAppeasementInput" +appesementDetails.toString());
		logger.endTimer("CrocsCompensationUpdateToForter.prepareInputToForter(): End");
		return appesementDetails;
	}

	/* --------------------- BASIC EVENT DETAILS -------------------- */

	/**
	 * @param orderEle
	 * @param orderListEle
	 * @param eventTime
	 * @return
	 */
	private JSONObject buildEventDetails(Element orderEle, Element orderListEle, long eventTime,CompensationType compType) {
		String eventId = "";
		logger.beginTimer("CrocsUnifiedCompensationReqToForter : buildEventDetails");

		String orderNo = orderEle.getAttribute(A_ORDER_NO);

		String extnCustId = SCXmlUtil.getXpathAttribute(orderListEle, "./Extn/@ExtnCustId");

		boolean isGuest = YFCCommon.isVoid(extnCustId);
		String accountId = isGuest ? orderNo + "_GUEST" : orderNo + "_" + extnCustId;

		if(CompensationType.APPEASEMENT.equals(compType)) {
			String invoiceNo = SCXmlUtil.getXpathAttribute(orderEle,
					"/InvoiceDetail/InvoiceHeader/@InvoiceNo");
			eventId = orderNo +"_"+ invoiceNo;
		}else if(CompensationType.RESHIP.equals(compType)) {
			eventId = orderNo + "_" + orderEle.getAttribute(A_RELEASE_NO);
		}
		else{
			eventId = orderNo;
		}
		JSONObject root = new JSONObject();
		root.put(A_ACCOUNT_ID_F, accountId);
		root.put(A_EVENT_ID_F, eventId);
		root.put(F_EVENT_TIME, eventTime);
		root.put(A_IS_GUEST_ACCOUNT_F, isGuest);
		root.put(A_ORIGINAL_ORDER_ID_F, orderNo);

		logger.endTimer("CrocsUnifiedCompensationReqToForter : buildEventDetails");
		return root;
	}


	/*------------- COMPENSATION DETAILS ------------- */

	/**
	 * @param env
	 * @param type
	 * @param orderEle
	 * @param orderLineEle
	 * @return
	 * @throws RemoteException
	 */
	private Map<String, String> prepareCompensationDetailsJObj(YFSEnvironment env, CompensationType type, Element orderEle,
														   Element orderLineEle) throws RemoteException {

		logger.beginTimer("CrocsUnifiedCompensationReqToForter : prepareCompensationDetailsJObj");

		Map<String, String> map = new HashMap<>();
		String reshipReason = "";

		switch (type) {

			case RETURN:
				map.put("COMP_TYPE", V_REFUND_UPON_RETURN);
				map.put("COMMON_CODE_TYPE", V_FORTER_RETURN_REASONS);
				map.put("REASON_CODE", SCXmlUtil.getXpathAttribute(orderEle, "./OrderLines/OrderLine/@ReturnReason"));
				break;

			case APPEASEMENT:
				map.put("COMP_TYPE", V_REFUND_UPON_RETURN);
				map.put("COMMON_CODE_TYPE", STR_CROCS_APPEASEMENT_REASONS);
				map.put("REASON_CODE", orderEle.getAttribute(A_INVOICE_CREATION_REASON));
				break;

			case RESHIP:
				map.put("COMP_TYPE", V_REPLACEMENT);
				map.put("COMMON_CODE_TYPE", STR_CROCS_RESHIP_REASONS);
				reshipReason =  getReshipReason(env, orderLineEle);
				map.put("REASON_CODE", reshipReason);
				break;
		}

		logger.endTimer("CrocsUnifiedCompensationReqToForter : prepareCompensationDetailsJObj");
		return map;
	}

	/**
	 * @param env
	 * @param orderLineEle
	 * @return
	 * @throws RemoteException
	 */
	private String getReshipReason(YFSEnvironment env, Element orderLineEle) throws RemoteException {
		logger.beginTimer("CrocsForterUtil::getReshipReason");

		Document getComOrdLineDtlsOutDoc = null;
		// Prepare input for getOrderList
		Document getComOrdLineDtlsInput = SCXmlUtil.createDocument(E_ORDER_LINE_DETAIL);
		Element orderDtlEle = getComOrdLineDtlsInput.getDocumentElement();
		orderDtlEle.setAttribute(A_ORDER_LINE_KEY, orderLineEle.getAttribute(A_ORDER_LINE_KEY));


		logger.info("CrocsForterUtil : getReshipReason : getCompleteOrderLineDetails Input is: " + SCXmlUtil.getString(getComOrdLineDtlsInput));

		// Call getCompleteOrderLineDetails
		getComOrdLineDtlsOutDoc = CommonUtil.invokeService(env, CrocsConstant.CROCS_GET_COMPLETE_ORD_LINE_DETAILS_FOR_FORTER,
				getComOrdLineDtlsInput);

		logger.info("CrocsForterUtil : fetchOrderList : getCompleteOrderLineDetails output is: " + SCXmlUtil.getString(getComOrdLineDtlsOutDoc));

		Element orderListEle = SCXmlUtil.getChildElement(getComOrdLineDtlsOutDoc.getDocumentElement(), E_ORDER);
		//TODO fetch the reason for the orderLine and return it
		logger.endTimer("CrocsForterUtil::getReshipReason");
		return null;
	}

	/* --------------- ITEM JSON HELPERS --------------------- */

	/**
	 * @param orderLineEle
	 * @param listOrdLine
	 * @param currency
	 * @return
	 */
	private JSONObject buildBasicItemData(Element orderLineEle, Element listOrdLine, String currency) {

		logger.beginTimer("CrocsUnifiedCompensationReqToForter : buildBasicItemData");

		Element itemEle = SCXmlUtil.getChildElement(listOrdLine, E_ITEM);

		JSONObject basicItem = new JSONObject();

		JSONObject price = new JSONObject();
		price.put(A_AMOUNT_LOCAL_CURRENCY, SCXmlUtil.getXpathAttribute(listOrdLine, "./LineOverallTotals/@LineTotal"));
		price.put(A_CURRENCY_F, currency);

		basicItem.put(A_NAME_F, itemEle.getAttribute(A_ITEM_ID));
		basicItem.put(A_QUANTITY_F, Double.parseDouble(listOrdLine.getAttribute(A_ORDERED_QTY)));
		basicItem.put(A_PRICE_F, price);
		basicItem.put(A_TYPE_F, TANGEABLE);

		logger.endTimer("CrocsUnifiedCompensationReqToForter : buildBasicItemData");
		return basicItem;
	}

	/**
	 * @param env
	 * @param compDetails
	 * @param eventTime
	 * @return
	 * @throws Exception
	 */
	private JSONObject buildItemCompensationData(YFSEnvironment env, Map<String, String> compDetails, long eventTime, String returnType)
			throws Exception {

		logger.beginTimer("CrocsUnifiedCompensationReqToForter : buildItemCompensationData");

		String reasonCategory = CrocsForterUtil.getCommonCodeDesc(env, compDetails.get("COMMON_CODE_TYPE"),
				compDetails.get("REASON_CODE"));

		JSONObject obj = new JSONObject();
		obj.put(A_COMPENSATION_TYPE_REQ_F, compDetails.get("COMP_TYPE"));
		obj.put(A_REASON_CATEGORY_F, reasonCategory);
		obj.put(A_REQ_RETURN_TYPE_F, returnType);
		obj.put(A_INITIATION_TIME_F, eventTime);

		logger.endTimer("CrocsUnifiedCompensationReqToForter : buildItemCompensationData");
		return obj;
	}

	/* --------------------- InitiationType ----------------------- */

	/**
	 * @param entryType
	 * @return
	 */
	private String getInitiationType(String entryType) {

		logger.beginTimer("CrocsUnifiedCompensationReqToForter::getInitiationType");

		switch (entryType) {
			case ENTRY_TYPE_CALL_CENTER:
			case V_WEB:
				return V_CALL_CENTER;
			case V_STORE:
				return V_IN_STORE;
			default:
				logger.info("getInitiationType: No matching case for entryType:"+ entryType +" returning empty value" );
				return "";
		}
	}

	/**
	 * Updates the notes at the orderline level
	 * @param env
	 * @param inDoc
	 * @throws Exception
	 */
	public void callChangeOrderUpdateNotes(YFSEnvironment env, Document inDoc) throws Exception {

		logger.beginTimer("CrocsUnifiedCompensationReqToForter::callChangeOrderUpdateNotes");
		Element orderEle = inDoc.getDocumentElement();
		Element orderLineEle = SCXmlUtil.getXpathElement(orderEle,"//OrderLines/OrderLine");
		String ModificationReasonCode = orderEle.getAttribute("ModificationReasonCode");
		try {
			Document changeOrderInDoc = SCXmlUtil.createDocument(E_ORDER);
			Element changeOrderInEle = changeOrderInDoc.getDocumentElement();
			changeOrderInEle.setAttribute(A_ORDER_HEADER_KEY, orderEle.getAttribute(A_ORDER_HEADER_KEY));
			Element changeOrderLinesInEle = SCXmlUtil.createChild(changeOrderInEle,E_ORDER_LINES);
			Element changeOrderLineInEle = SCXmlUtil.createChild(changeOrderLinesInEle,E_ORDER_LINE);
			changeOrderLineInEle.setAttribute(A_ACTION, VAL_MODIFY);
			changeOrderLineInEle.setAttribute(A_ORDER_LINE_KEY,orderLineEle.getAttribute(A_ORDER_LINE_KEY));
			Element notesEle = SCXmlUtil.createChild(changeOrderLineInEle,E_NOTES);
			Element noteEle = SCXmlUtil.createChild(notesEle,E_NOTE);
			noteEle.setAttribute(A_REASON_CODE,V_RESHIP_REASON);
			noteEle.setAttribute(A_NOTE_TEXT, ModificationReasonCode);

			logger.info("changeOrderInDoc: " + XMLUtil.getXMLString(changeOrderInDoc));
			CommonUtil.invokeAPI(env,"",API_CHANGE_ORDER, changeOrderInDoc);

		} catch (Exception ex) {
			logger.verbose("callChangeOrderUpdateNotes: error: "+ex.getMessage()+"ex.toString() "+ex.toString());
			throw new Exception("Exception occured Updateing Notes");
		}
		logger.endTimer("CrocsUnifiedCompensationReqToForter::callChangeOrderUpdateNotes");
	}
}