package com.crocs.oms.order.receipt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.crocs.oms.common.util.XMLUtil;
import com.crocs.oms.order.receipt.util.CrocsEMEAReturnUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

/**
 * 
 * @author IBM This class covers logic to update receipt's quantity in OMS once
 *         received from WMS and close the receipt in OMS
 *
 */
public class CrocsProcessSplitReturnReceiptFromWMS implements CrocsConstant {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsProcessSplitReturnReceiptFromWMS.class);

	//EOMS-12352 Start
	private final CrocsEMEAReturnUtil returnUtil = new CrocsEMEAReturnUtil();
	//EOMS-12352 End

	public void receiveReturnReceiptInOMS(YFSEnvironment env, Document indoc) {

		/**
		 * Sample input XML coming from WMS via OIC
		 * 
		 * 
		 * <Receipt ReceivingNode="1032" >
		 * <Shipment OrderNo="Y100002100" ReceivingNode="1032"/> <ReceiptLines>
		 * <ReceiptLine OrderNo="Y100002100" PrimeLineNo="1" Quantity="1" ItemID=
		 * "40003-001-M22"/> </ReceiptLines> </Receipt>
		 * 
		 */

		/**
		 * 
		 * Expected indoc in OMS
		 * 
		 * 
		 * <Receipt DocumentType="0003" ReceivingNode="1032" >
		 * <Shipment DocumentType="0003" EnterpriseCode="CROCS_CA" OrderNo="Y100002100"
		 * ReceivingNode="1032"/> <ReceiptLines>
		 * <ReceiptLine OrderNo="Y100002100" PrimeLineNo="1" SubLineNo="1" Quantity="1"
		 * ItemID="40003-001-M22"/> </ReceiptLines> </Receipt>
		 * 
		 */
		try {
			/**
			 * EOMS-6582 For Narvar mix cart returns: If both normal return and exchange
			 * return lines are present, separate receipts should be created for normal and
			 * exchange lines. The normal receipt should be moved to Receipt Closed, while
			 * the exchange lines should be invoiced using the createOrderInvoice API.
			 */
			handleReceiptProcessing(env, indoc);
		} catch (Exception e) {
			logger.verbose(
					"CrocsReceiveReturnReceiptFromWMS.receiveReturnReceiptInOMS: Exception while calling handleReceiptProcessing method "
							+ e.getMessage());
			logger.info(
					"CrocsReceiveReturnReceiptFromWMS.receiveReturnReceiptInOMS: Exception while calling handleReceiptProcessing method "
							+ e.getMessage());
			throw new YFSException(e.getMessage());
		}

	}

	/**
	 * 
	 * @param env
	 * @param indoc
	 * @return
	 */
	private boolean prepareInputForReceiveOrderAPI(Document indoc) {

		logger.verbose("Starts of method prepareInputForReceiveOrderAPI with input: " +SCXmlUtil.getString(indoc));

		// to skip API calls if there are no lines
		boolean hasValidLine = false;
		String receivingNode = "";
		String enterpriseCode = "";
		Element receiptEle = indoc.getDocumentElement();
		receiptEle.setAttribute(A_DOCUMENT_TYPE, A_RETURN_ORDER_DOCUMENT_TYPE);
		receivingNode = receiptEle.getAttribute(A_RECEIVING_NODE);

		Element shipmentEle = SCXmlUtil.getChildElement(receiptEle, E_SHIPMENT);
		shipmentEle.setAttribute(A_DOCUMENT_TYPE, A_RETURN_ORDER_DOCUMENT_TYPE);
		enterpriseCode = shipmentEle.getAttribute(A_ENTERPRISE_CODE);

		// Safety check as for APTOS returns while preparing message we will stamp
		// EnterpriseCode
		// EOMS-6996:: Changes Start
		if (YFCCommon.isVoid(enterpriseCode) && !YFCCommon.isVoid(receivingNode)) {
			// setting enterprise code
		    String enterpriseCodeToUpdate = "";

		    switch (receivingNode) {
		        case A_OHIO_DC_VALUE:
		            enterpriseCodeToUpdate = CROCS_US;
		            break;
		        case A_UPS_SCS_VALUE:
		            enterpriseCodeToUpdate = CROCS_CA;
		            break;
		        case A_RADIAL_SHIPNODE:
		            enterpriseCodeToUpdate = HEYDUDE_US;
		            break;
		        case A_LVDC:
		            enterpriseCodeToUpdate = HEYDUDE_US;
		            break;
                case STR_CAU_SHIP_NODE:
                    enterpriseCodeToUpdate = CROCS_AU;
                    break;
				// EOMS-11254:: Changes Start
				case SG_RO_FULFILLMENT_NODE:
					enterpriseCodeToUpdate = CROCS_SG;
                    break;
				// EOMS-11254:: Changes End	
                //EOMS -12642 - Return Reciept Changes For Korea    
				case KR_RO_FULFILLMENT_NODE:
					enterpriseCodeToUpdate = CROCS_KR;
                    break;
		        default:
		        	logger.info("ERROR : CrocsProcessSplitReturnReceiptFromWMS : prepareInputForReceiveOrderAPI");
		        	throw new YFSException("Expected EnterpriseCode for "+receivingNode+ " is Not Found", "", "Expected ReceivingNode is [1005,1032,4103,4101] validate Return Receipt input : "+SCXmlUtil.getString(indoc));
		    }
		    shipmentEle.setAttribute(A_ENTERPRISE_CODE, enterpriseCodeToUpdate);
		}
		// EOMS-6996:: Changes End

		/**
		 * EOMS-4393 changes Filter out Receipt Lines with Quantity as zero and set flag
		 * to skip api call processing if no lines
		 */
		Element receiptLines = SCXmlUtil.getChildElement(receiptEle, E_RECEIPT_LINES);
		ArrayList<Element> receiptLineList = SCXmlUtil.getChildren(receiptLines, E_RECEIPT_LINE);
		for (Element receiptLine : receiptLineList) {
			logger.verbose("Iterating Receipt Line : " + SCXmlUtil.getString(receiptLine));
			String quantity = receiptLine.getAttribute(A_QUANTITY);
			if (quantity.equalsIgnoreCase(VAL_ZERO)) {
				logger.verbose("Receipt Line Quantity is zero, hence removing this line");
				SCXmlUtil.removeNode(receiptLine);
			} else {
				// if there is at least one receipt line, below flag is set as true
				hasValidLine = true;
			}

		}

		if (!hasValidLine) {

			logger.info(
					"CrocsReceiveReturnReceiptFromWMS.prepareInputForReceiveOrderAPI: Value of hasValidLine flag is : "
							+ hasValidLine);
			logger.info(
					"CrocsReceiveReturnReceiptFromWMS.prepareInputForReceiveOrderAPI: Ends of method prepareInputForReceiveOrderAPI with updated input: "
							+ SCXmlUtil.getString(indoc));
		}
		logger.verbose(
				"CrocsReceiveReturnReceiptFromWMS.prepareInputForReceiveOrderAPI: Ends of method prepareInputForReceiveOrderAPI with updated input: "
						+ SCXmlUtil.getString(indoc));
		logger.verbose(
				"CrocsReceiveReturnReceiptFromWMS.prepareInputForReceiveOrderAPI: Value of hasValidLine flag is : "
						+ hasValidLine);

		return hasValidLine;

	}

	/**
	 * 
	 * @param env
	 * @param receiveOrderOutDoc
	 */
	private void closeReceiptInOMS(YFSEnvironment env, Document receiveOrderOutDoc) {

		logger.verbose(
				"CrocsReceiveReturnReceiptFromWMS.closeReceiptInOMS: Starts of method closeReceiptInOMS with input: "
						+ SCXmlUtil.getString(receiveOrderOutDoc));

		Element receiceOrderOutEle = receiveOrderOutDoc.getDocumentElement();
		String receiptHeaderKey = receiceOrderOutEle.getAttribute(A_RECEIPT_HEADER_KEY);

		Document closeReceiptIndoc = SCXmlUtil.createDocument(E_RECEIPT);
		Element closeReceiptEle = closeReceiptIndoc.getDocumentElement();
		closeReceiptEle.setAttribute(A_DOCUMENT_TYPE, A_RETURN_ORDER_DOCUMENT_TYPE);
		closeReceiptEle.setAttribute(A_RECEIPT_HEADER_KEY, receiptHeaderKey);

		boolean isMultiReceiptForEMEA =
				"Y".equals(env.getTxnObject("MultipleReceiptEMEAFlag"));

		try {
			Document closeReceiptOutDoc = CommonUtil.invokeAPI(env, "", API_CLOSE_RECEIPT, closeReceiptIndoc);

			logger.verbose(
					"CrocsReceiveReturnReceiptFromWMS.closeReceiptInOMS: Close receipt API call is successfully completed "
							+ SCXmlUtil.getString(closeReceiptOutDoc));
			String orderPurpose = receiceOrderOutEle.getAttribute(A_ORDER_PURPOSE);
			if (STR_EXCHANGE.equalsIgnoreCase(orderPurpose) || isMultiReceiptForEMEA) {
				Document orderInvoiceIndoc = SCXmlUtil.createDocument(E_ORDER);
				Element orderLines = SCXmlUtil.createChild(orderInvoiceIndoc.getDocumentElement(), E_ORDER_LINES);
				Element orderLine = SCXmlUtil.createChild(orderLines, E_ORDER_LINE);

				String orderHeaderKey = SCXmlUtil.getXpathAttribute(receiceOrderOutEle, XPATH_RECEIPT_ORDER_HEADER_KEY);

				orderInvoiceIndoc.getDocumentElement().setAttribute(A_ORDER_HEADER_KEY, orderHeaderKey);
				orderInvoiceIndoc.getDocumentElement().setAttribute(A_TRANSACTION_ID, CREATE_ORDER_INVOICE_0003);
				Element receiptLines = SCXmlUtil.getChildElement(receiveOrderOutDoc.getDocumentElement(),
						E_RECEIPT_LINES);
				ArrayList<Element> receiptLineList = SCXmlUtil.getChildren(receiptLines, E_RECEIPT_LINE);
				for (Element receiptLine : receiptLineList) {
					String orderLineKey = receiptLine.getAttribute(A_ORDER_LINE_KEY);
					orderLine.setAttribute(A_ORDER_LINE_KEY, orderLineKey);

				}

				Document orderInvoiceOutDoc = CommonUtil.invokeAPI(env, "", API_CREATE_ORDER_INVOICE,
						orderInvoiceIndoc);

				logger.verbose(
						"CrocsReceiveReturnReceiptFromWMS: create Order Invoice API call is successfully completed "
								+ SCXmlUtil.getString(orderInvoiceOutDoc));

			}

		} catch (Exception e) {
			logger.verbose(
					"CrocsReceiveReturnReceiptFromWMS.closeReceiptInOMS: Error in catch block of close receipt API: "
							+ e.getLocalizedMessage());
			logger.info("CrocsReceiveReturnReceiptFromWMS.closeReceiptInOMS: Exception while calling CloseReceiptApi "
					+ e.getMessage());
			throw new YFSException(e.getMessage());

		}

	}

	/**
	 * Processes the Receive Order flow.
	 * <p>
	 * Prepares input for the Receive Order API, invokes it if valid receipt lines
	 * exist, and then triggers the Close Receipt API.
	 *
	 * @param env   The YFSEnvironment instance.
	 * @param inDoc The input XML document for the Receive Order.
	 * @return The output document from the Receive Order API, or null if skipped.
	 * @throws Exception if any API call fails.
	 */
	private Document processReceiveOrder(YFSEnvironment env, Document inDoc) {
		Document receiveOrderOutDoc = null;
		/**
		 * EOMS-4393 changes preparing receiveOrder API Input and checking if there are
		 * receipt lines to process which has quantity greater then zero
		 */

		if (prepareInputForReceiveOrderAPI(inDoc)) {

			try {
				logger.verbose("calling receiveOrder API with input: " + SCXmlUtil.getString(inDoc));
				receiveOrderOutDoc = CommonUtil.invokeAPI(env, TEMPLATE_RECEIVE_ORDER_FOR_RETURN, API_RECEIVE_ORDER,
						inDoc);
				String orderPurpose = inDoc.getDocumentElement().getAttribute(A_ORDER_PURPOSE);
				if (STR_EXCHANGE.equalsIgnoreCase(orderPurpose)) {
					receiveOrderOutDoc.getDocumentElement().setAttribute(A_ORDER_PURPOSE, orderPurpose);
				}
				// calling closeReceipt API
				closeReceiptInOMS(env, receiveOrderOutDoc);

			} catch (Exception e) {
				logger.verbose(
						"CrocsReceiveReturnReceiptFromWMS.processReceiveOrder: Exception while calling receiveOrderAPI "
								+ e.getMessage());
				logger.info(
						"CrocsReceiveReturnReceiptFromWMS.processReceiveOrder: Exception while calling receiveOrderAPI "
								+ e.getMessage());
				CommonUtil.throwError(ERROR_CODE_RECEIVE_RETURN_ERROR, ERROR_DESC_FOR_RECEIVE_RETURN);
				throw new YFSException(e.getMessage());

			}
		} else {
			logger.verbose(
					"CrocsReceiveReturnReceiptFromWMS.receiveReturnReceiptInOMS: There are no receipt lines having Quantity as non zero so skipping receive order API call");
			logger.info(
					"CrocsReceiveReturnReceiptFromWMS.receiveReturnReceiptInOMS: There are no receipt lines having Quantity as non zero so skipping receive order API call");
		}

		return receiveOrderOutDoc;
	}

	/**
	 * Handles receipt processing for return and exchange orders.
	 * <p>
	 * - Retrieves the order details using the provided OrderNo. - Checks for
	 * exchange order lines (ConditionVariable1 = 'exchange'). - If found, separates
	 * those receipt lines into a new Receipt document for exchange. - Otherwise,
	 * proceeds with the standard Receive Order flow.
	 *
	 * @param env   OMS environment context
	 * @param inDoc Input Receipt XML
	 * @throws Exception in case of XML or API errors
	 */
	public void handleReceiptProcessing(YFSEnvironment env, Document inDoc) {

		try {

			// Extract OrderNo from incoming Receipt document
			String orderNo = SCXmlUtil.getXpathAttribute(inDoc.getDocumentElement(), XPATH_RECEIPT_ORDER_NO);
			if (orderNo.isEmpty() && "CrocsReboundScanAndReceipt".equals(inDoc.getDocumentElement().getNodeName())){
				orderNo = SCXmlUtil.getXpathAttribute(inDoc.getDocumentElement(), XPATH_REBOUND_RECEIPT_ORDER_NO);
				logger.info("Processing Rebound Receive Order for OrderNo: " + orderNo);
			}
			logger.verbose("Processing Receipt for OrderNo: " + orderNo);
			logger.info("Processing ReceiveOrder for OrderNo: " + orderNo);
			// Prepare input for getOrderList service
			Document getOrderListInDoc = SCXmlUtil.createDocument(E_ORDER);
			getOrderListInDoc.getDocumentElement().setAttribute(A_ORDER_NO, orderNo);
			getOrderListInDoc.getDocumentElement().setAttribute(A_DOCUMENT_TYPE, A_RETURN_ORDER_DOCUMENT_TYPE);

			Document getOrderListOutDoc = CommonUtil.invokeService(env, SERVICE_GET_ORDER_LIST_FOR_RECEIPT,
					getOrderListInDoc);
			
			//stamping order EnterpriseCode in inDoc
			String enterpriseCode= SCXmlUtil.getXpathAttribute(getOrderListOutDoc.getDocumentElement(),
					"/OrderList/Order/@EnterpriseCode");
			Element shipment = SCXmlUtil.getXpathElement(inDoc.getDocumentElement(), "/Receipt/Shipment");
			if ((YFCCommon.isVoid(shipment)) && "CrocsReboundScanAndReceipt".equals(inDoc.getDocumentElement().getNodeName())) {
				shipment = SCXmlUtil.getXpathElement(inDoc.getDocumentElement(), XPATH_REBOUND_RECEIPT_SHIPMENT_ELE);
			}
			shipment.setAttribute(A_ENTERPRISE_CODE, enterpriseCode);

			// Check if any order line has ConditionVariable1='exchange'
			Element exchangeOrderLine = SCXmlUtil.getXpathElement(getOrderListOutDoc.getDocumentElement(),
					XPATH_ORDER_LIST_CONDITON_VAIRABLE1);

			// If no exchange order lines, process receipt normally
			if ((YFCCommon.isVoid(exchangeOrderLine))  && (!CROCS_EMEA_ENTERPRISES.contains(enterpriseCode))) {
				logger.verbose("No exchange lines found. Proceeding with normal receive order flow.");
				
				processReceiveOrder(env, inDoc);
				return;
			}
			//EOMS-12352 Starts - Split Receipt handling for EMEA
			//The condition below checks the EMEA enterprises, if Received from Rebound for GB, or regular receipt messages from WMS
			else if ( (YFCCommon.isVoid(exchangeOrderLine)) && CROCS_EMEA_ENTERPRISES.contains(enterpriseCode) ) {
				if ((enterpriseCode.equals(HEYDUDE_GB) || (enterpriseCode.equals(CROCS_GB))) &&
						"CrocsReboundScanAndReceipt".equals(inDoc.getDocumentElement().getNodeName())) {
					//Create new Doc by removing the element CrocsReboundScanAndReceipt
					Element newReceiptEle = (Element) inDoc.getDocumentElement()
							.getElementsByTagName(E_RECEIPT)
							.item(0);
					if (!YFCCommon.isVoid(newReceiptEle)) {
						Document newDocR = SCXmlUtil.createDocument(E_RECEIPT);
						Node importedOrder = newDocR.importNode(newReceiptEle, true);
						newDocR.removeChild(newDocR.getDocumentElement());
						newDocR.appendChild(importedOrder);
						inDoc = newDocR;
					}
				}
				else if ((YFCCommon.isVoid(exchangeOrderLine)) && (enterpriseCode.equals(HEYDUDE_GB) ||
						(enterpriseCode.equals(CROCS_GB))) &&
						!"CrocsReboundScanAndReceipt".equals(inDoc.getDocumentElement().getNodeName())){
							//call GetOrderInvoiceList for GB when received again from WMS
						publishInvoiceToSAPForGB(env, orderNo);
						return;
				}
						logger.verbose("EMEA No exchange lines found. Proceeding with normal receive order flow.");
						logger.verbose("Document to invoke split method :::: " + XMLUtil.getXMLString(inDoc));
						formSplitReceiptInputForEMEA(env,inDoc);
						return;
			}
			//EOMS-12352 Ends----

			logger.verbose("Exchange lines found. Splitting receipt for exchange processing...");

			// Get all OrderLines from order list output
			Element orderLinesElement = (Element) getOrderListOutDoc.getElementsByTagName(E_ORDER_LINES).item(0);
			NodeList orderLineList = SCXmlUtil.getXpathNodes(orderLinesElement, E_ORDER_LINE);

			// Create a new Receipt document for exchange lines
			Document exchangeReceiptDoc = SCXmlUtil.createDocument(E_RECEIPT);

			String receivingNode = SCXmlUtil.getAttribute(inDoc.getDocumentElement(), A_RECEIVING_NODE);
			exchangeReceiptDoc.getDocumentElement().setAttribute(A_RECEIVING_NODE, receivingNode);
			exchangeReceiptDoc.getDocumentElement().setAttribute(A_ORDER_PURPOSE, STR_EXCHANGE);
			// Import Shipment element from the input doc

			Element shipmentElement = SCXmlUtil.createChild(exchangeReceiptDoc.getDocumentElement(), E_SHIPMENT);
			Element inputShipmentElement = SCXmlUtil.getXpathElement(inDoc.getDocumentElement(),
					XPATH_RECEIPT_SHIPMENT);
			String shipmentReceivingNode = inputShipmentElement.getAttribute(A_RECEIVING_NODE);
			String shipmentOrderNo = inputShipmentElement.getAttribute(A_ORDER_NO);
			String shipmentEnterpriseCode = inputShipmentElement.getAttribute(A_ENTERPRISE_CODE);
			shipmentElement.setAttribute(A_RECEIVING_NODE, shipmentReceivingNode);
			shipmentElement.setAttribute(A_ORDER_NO, shipmentOrderNo);
			shipmentElement.setAttribute(A_ENTERPRISE_CODE, shipmentEnterpriseCode);

			// Create ReceiptLines node in new exchange document
			Element exchangeReceiptLines = SCXmlUtil.createChild(exchangeReceiptDoc.getDocumentElement(),
					E_RECEIPT_LINES);
			Element inputReceiptLines = SCXmlUtil.getXpathElement(inDoc.getDocumentElement(),
					XPATH_RECEIPT_RECEIPT_LINES);

			logger.verbose("Exchange lines found. before exchangeReceiptDoc" + SCXmlUtil.getString(exchangeReceiptDoc));
			// Iterate through all order lines and move exchange lines
			for (int i = 0; i < orderLineList.getLength(); i++) {
				Element orderLineElement = (Element) orderLineList.item(i);
				String conditionVariable1 = orderLineElement.getAttribute(A_CONDITON_VARIABLE1);
				String primeLineNo = orderLineElement.getAttribute(A_PRIME_LINE_NO);

				if (A_NARVAR_EXCHANGE_LINE.equalsIgnoreCase(conditionVariable1)) {
					String itemID = SCXmlUtil.getXpathAttribute(orderLineElement, XPATH_ITEM_DETAILS_ITEM_ID);

					// Find matching ReceiptLine from input document
					Element inputReceiptLine = SCXmlUtil.getXpathElement(inDoc.getDocumentElement(),
							"/Receipt/ReceiptLines/ReceiptLine[@ItemID='" + itemID + "' and @PrimeLineNo='"
									+ primeLineNo + "']");

					if (inputReceiptLine != null) {

						// Import and add to exchange receipt
						Element importedReceiptLine = (Element) exchangeReceiptDoc.importNode(inputReceiptLine, true);
						exchangeReceiptLines.appendChild(importedReceiptLine);

						// Remove from original receipt
						inputReceiptLines.removeChild(inputReceiptLine);
						logger.verbose("Moved exchange line for ItemID: " + itemID);
					}
				}
			}

			/**
			 * At this point: - inDoc has only normal return lines- exchangeReceiptDoc has
			 * only exchange lines now call processReceiveOrder separately
			 */

			logger.verbose("Receipt indoc for Normal Return lines: " + SCXmlUtil.getString(inDoc));
			logger.verbose("Receipt indoc for Exchange Return lines: " + SCXmlUtil.getString(exchangeReceiptDoc));
			if (exchangeReceiptLines.hasChildNodes())
				processReceiveOrder(env, exchangeReceiptDoc);
			if (inputReceiptLines.hasChildNodes())
				processReceiveOrder(env, inDoc);

			logger.verbose("Receipt splitting complete for OrderNo: " + orderNo);
		} catch (Exception e) {
			logger.verbose(
					"CrocsReceiveReturnReceiptFromWMS.handleReceiptProcessing: Error in catch block of close receipt API: "
							+ e.getLocalizedMessage());
			logger.info(
					"CrocsReceiveReturnReceiptFromWMS.handleReceiptProcessing: Exception while calling handleReceiptProcessing method "
							+ e.getMessage());
			throw new YFSException(e.getMessage(), "",
					"Error in CrocsReceiveReturnReceiptFromWMS.handleReceiptProcessing() class");
		}

	}
	
	/**
	 * A wrapper method for processing a return receipt received from Rebound.
	 * 
	 * <p/>
	 * This method delegates the actual receipt processing to
	 * {@code receiveReturnReceiptInOMS()} and returns a success response
	 * once the receipt has been successfully received and closed in OMS.
	 * 
	 * <p/>
	 * Any exception encountered during processing will be propagated as a
	 * {@link YFSException}, allowing the REST framework to handle and return
	 * the appropriate error response.
	 *
	 * @param env   OMS environment context
	 * @param inDoc Input receipt XML received from Rebound
	 * @return Success response document containing status details
	 * @throws YFSException if receipt processing fails
	 */
	public Document processReboundReturnReceipt(YFSEnvironment env, Document inDoc) {
		logger.verbose("CrocsProcessSplitReturnReceiptFromWMS.processReboundReturnReceipt: Started processing Rebound return receipt. Input: " + SCXmlUtil.getString(inDoc));
	    
	    // Delegates processing to the existing receipt processing flow.
		receiveReturnReceiptInOMS(env, inDoc);

	    Document docResponse = SCXmlUtil.createDocument(A_RESPONSE);
	    Element eleResponse = docResponse.getDocumentElement();

	    eleResponse.setAttribute(STR_HTTP_CODE, A_STATUS_CODE_SUCCESS);
	    eleResponse.setAttribute(A_DESCRIPTION, STR_SUCCESS_RESPONSE_RECEIPT_CLOSE);
	    eleResponse.setAttribute(A_STATUS, V_SUCCESS);

	    logger.verbose("CrocsProcessSplitReturnReceiptFromWMS.processReboundReturnReceipt: Returning response: " + SCXmlUtil.getString(docResponse));
	    return docResponse;
	}

	/** EOMS-12352
	 * Calls the return Util Class For EMEA and handles the splitting of Recipts based on the Item and Department
	 * Combination.
	 * <p>
	 * .
	 *
	 * @param env   OMS environment context
	 * @param inDoc Input Receipt XML
	 *
	 */
	private void formSplitReceiptInputForEMEA(YFSEnvironment env, Document inDoc) {
		Map<String, String> mapForItemDepartment = returnUtil.createMapForItemDepartment(env, inDoc);
		// Group the Receipt Elements based on the Departmemt
		Map<String, List<Element>> groupedLines = returnUtil.groupReceiptLinesByDepartment (inDoc,mapForItemDepartment);
		List<Document> processReceiptInput = returnUtil.prepareSplitDocumentForReceipts (inDoc, groupedLines);
		for (Document doc : processReceiptInput) {
			logger.verbose("Document to invoke EMEA::::  " + XMLUtil.getXMLString(doc));
			if (processReceiptInput.size() > 1) {
				env.setTxnObject("MultipleReceiptEMEAFlag", "Y");
			}else{
				env.setTxnObject("MultipleReceiptEMEAFlag", "N");
			}
			processReceiveOrder(env,doc);
		}

	}

	/** EOMS-12352
	 *
	 * The method is for GB enterprise returns which calls only when the receipt message is from WMS
	 * Calls getOrderInvoiceList to get the list of invoices for that RO
	 * then extract each OrderInvoice and post to SAP internal Q service and calls Util class to update a
	 * custom flag to avoid multiple postings.

	 *
	 * @param env   OMS environment context
	 * @param orderNo
	 *
	 */
	private void publishInvoiceToSAPForGB(YFSEnvironment env, String  orderNo) throws YFSException {
		logger.verbose("CrocsReceiveReturnReceiptFromWMS.publishInvoicetoSAPForGB: Start");
		try {
			Document getOrderInvoiceListInDoc = SCXmlUtil.createDocument(E_ORDER_INVOICE);
			getOrderInvoiceListInDoc.getDocumentElement().setAttribute(A_ORDER_NO, orderNo);
			logger.verbose("Invoice Input GB ::::: " + XMLUtil.getXMLString(getOrderInvoiceListInDoc));
			Document getOrderInvoiceListOutDoc = null;
			getOrderInvoiceListOutDoc = CommonUtil.invokeAPI(env,TEMPLATE_GET_ORDER_INVOICE_LIST_FOR_SAP,
					API_GET_ORDER_INVOICE_LIST, getOrderInvoiceListInDoc);
			NodeList invoiceList = getOrderInvoiceListOutDoc.getDocumentElement()
					.getElementsByTagName(E_ORDER_INVOICE);
			for (int i = 0; i < invoiceList.getLength(); i++) {
				Element orderInvoice = (Element) invoiceList.item(i);
				Element orderInvoiceExtnEle = SCXmlUtil.getXpathElement(orderInvoice, E_EXTN);
				String strIsSentToSAP = orderInvoiceExtnEle.getAttribute(A_EXTN_IS_SENT_TO_SAP);
				if (strIsSentToSAP.trim().isEmpty() || !"Y".equalsIgnoreCase(strIsSentToSAP)) {
					Document docInvoiceDetails = SCXmlUtil.createDocument();
					Element root = docInvoiceDetails.getDocumentElement();
					Node importedInvoice = (Element) docInvoiceDetails.importNode(orderInvoice, true);
					docInvoiceDetails.appendChild(importedInvoice);
					logger.verbose("Document to invoke Service SAP :::: " + XMLUtil.getXMLString(docInvoiceDetails));
					CommonUtil.invokeService(env, SERVICE_POST_RETURN_INVOICES_TO_Q,
							docInvoiceDetails);
					returnUtil.updateFlagForDuplicateInvoice(env, docInvoiceDetails);
				}
			}
			logger.verbose("CrocsReceiveReturnReceiptFromWMS.publishInvoicetoSAPForGB: End");
		} catch (Exception ex) {
			throw new  YFSException("Error in publishInvoicetoSAPForGB: " + ex.getMessage());
		}
	}

}