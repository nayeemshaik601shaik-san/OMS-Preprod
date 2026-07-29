package com.crocs.oms.to.receipt;

import java.util.ArrayList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCException;
import com.yantra.yfs.japi.YFSEnvironment;

/**
 * Handles Return Receipt flow from WMS for Transfer Orders
 */
public class CrocsTOReceiveReturnReceiptFromWMS implements CrocsConstant {

	private static final YFCLogCategory LOGGER = YFCLogCategory.instance(CrocsTOReceiveReturnReceiptFromWMS.class);

	public void receiveReturnReceiptInOMS(YFSEnvironment env, Document indoc) {

		Document getShipmentListOutDoc = null;
		Document receiptDoc = null;
		Document receiveOrderOutDoc = null;

		try {
			Element shipmentEle = indoc.getDocumentElement();
			String shipmentKey = shipmentEle.getAttribute(A_SHIPMENT_KEY);
			String shipmentNo = shipmentEle.getAttribute(A_SHIPMENT_NO);

			// Call getShipmentList if ShipmentKey and ShipmentNo are not null
			if (!YFCObject.isNull(shipmentNo) && !YFCObject.isNull(shipmentKey)) {
				LOGGER.verbose(
						"Calling getShipmentList API for ShipmentKey: " + shipmentKey + ", ShipmentNo: " + shipmentNo);
				getShipmentListOutDoc = CommonUtil.invokeAPI(env, TEMPLATE_GET_SHIPMENT_LIST_TO, API_GET_SHIPMENT_LIST,
						indoc);
			}

			// Build Receipt XML from getShipmentList output
			if (!YFCObject.isNull(getShipmentListOutDoc)) {
				receiptDoc = prepareReceiptInput(getShipmentListOutDoc);
			}

			// If valid receipt with lines and Quantity > 0, call receiveOrder
			if (receiptDoc != null && prepareInputForReceiveOrderAPI(receiptDoc)) {
				LOGGER.verbose("Calling receiveOrder API with input: " + SCXmlUtil.getString(receiptDoc));
				receiveOrderOutDoc = CommonUtil.invokeAPI(env, TEMPLATE_RECEIVE_ORDER_FOR_RETURN, API_RECEIVE_ORDER,
						receiptDoc);

				// Call closeReceipt API
				closeReceiptInOMS(env, receiveOrderOutDoc);

			} else {
				LOGGER.verbose("No valid ReceiptLines found with Quantity > 0. Skipping receiveOrder API call.");
			}

		} catch (Exception e) {
			LOGGER.error("Error in receiveReturnReceiptInOMS", e);
			throw new YFCException(e,
					"CrocsTOReceiveReturnReceiptFromWMS : receiveReturnReceiptInOMS ");
		}
	}

	/**
	 * Prepare Receipt XML from getShipmentList output
	 */
	private Document prepareReceiptInput(Document getShipmentListOutDoc) {
		LOGGER.verbose(
				"Building Receipt XML from getShipmentList output: " + SCXmlUtil.getString(getShipmentListOutDoc));
		Document receiptDoc = null;
		try {
			Element shipmentEle = SCXmlUtil.getChildElement(getShipmentListOutDoc.getDocumentElement(), E_SHIPMENT);
			if (YFCObject.isNull(shipmentEle)) {
				LOGGER.verbose("No Shipment found in getShipmentList output.");
				return null;
			}

			receiptDoc = SCXmlUtil.createDocument(E_RECEIPT);
			Element receiptEle = receiptDoc.getDocumentElement();
			receiptEle.setAttribute(A_DOCUMENT_TYPE, shipmentEle.getAttribute(A_DOCUMENT_TYPE));
			receiptEle.setAttribute(A_RECEIVING_NODE, shipmentEle.getAttribute(A_RECEIVING_NODE));

			Element shipmntele = receiptDoc.createElement(E_SHIPMENT);
			shipmntele.setAttribute(A_DOCUMENT_TYPE, shipmentEle.getAttribute(A_DOCUMENT_TYPE));
			shipmntele.setAttribute(A_SHIPMENT_KEY, shipmentEle.getAttribute(A_SHIPMENT_KEY));
			shipmntele.setAttribute(A_SHIPMENT_NO, shipmentEle.getAttribute(A_SHIPMENT_NO));
			shipmntele.setAttribute(A_ENTERPRISE_CODE, shipmentEle.getAttribute(A_ENTERPRISE_CODE));

			// Pick OrderNo from first ShipmentLine
			Element firstShipmentLine = SCXmlUtil
					.getChildElement(SCXmlUtil.getChildElement(shipmentEle, E_SHIPMENT_LINES), E_SHIPMENT_LINE);
			if (firstShipmentLine != null) {
				shipmntele.setAttribute(A_ORDER_NO, firstShipmentLine.getAttribute(A_ORDER_NO));
			}
			shipmntele.setAttribute(A_RECEIVING_NODE, shipmentEle.getAttribute(A_RECEIVING_NODE));
			receiptEle.appendChild(shipmntele);

			// ReceiptLines
			Element receiptLinesEle = receiptDoc.createElement(E_RECEIPT_LINES);
			receiptEle.appendChild(receiptLinesEle);

			ArrayList<Element> shipmentLineList = SCXmlUtil
					.getChildren(SCXmlUtil.getChildElement(shipmentEle, E_SHIPMENT_LINES), E_SHIPMENT_LINE);
			for (Element shipmentLine : shipmentLineList) {
				String qty = shipmentLine.getAttribute(A_QUANTITY);
				double value = Double.parseDouble(qty);
				if (!YFCObject.isNull(qty) && value >0) {
					Element receiptLine = receiptDoc.createElement(E_RECEIPT_LINE);
					receiptLine.setAttribute(A_ORDER_NO, shipmentLine.getAttribute(A_ORDER_NO));
					receiptLine.setAttribute(A_PRIME_LINE_NO, shipmentLine.getAttribute(A_PRIME_LINE_NO));
					receiptLine.setAttribute(A_SUB_LINE_NO, shipmentLine.getAttribute(A_SUB_LINE_NO));
					receiptLine.setAttribute(A_QUANTITY, qty);
					receiptLine.setAttribute(A_ITEM_ID, shipmentLine.getAttribute(A_ITEM_ID));
					receiptLinesEle.appendChild(receiptLine);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error in prepareReceiptInput", e);
		}
		LOGGER.verbose("Final constructed Receipt XML: " + SCXmlUtil.getString(receiptDoc));
		return receiptDoc;
	}

	/**
	 * Validates and removes zero-qty receipt lines
	 */
	private boolean prepareInputForReceiveOrderAPI(Document indoc) {
		LOGGER.verbose("Starts prepareInputForReceiveOrderAPI with input: " + SCXmlUtil.getString(indoc));

		boolean hasValidLine = false;
		Element receiptLines = SCXmlUtil.getChildElement(indoc.getDocumentElement(), E_RECEIPT_LINES);
		ArrayList<Element> receiptLineList = SCXmlUtil.getChildren(receiptLines, E_RECEIPT_LINE);

		for (Element receiptLine : receiptLineList) {
			String quantity = receiptLine.getAttribute(A_QUANTITY);
			double value = Double.parseDouble(quantity);
			if (value==0.0) {
				LOGGER.verbose("Removing ReceiptLine with zero Quantity: " + SCXmlUtil.getString(receiptLine));
				SCXmlUtil.removeNode(receiptLine);
			} else {
				hasValidLine = true;
			}
		}

		LOGGER.verbose("Ends prepareInputForReceiveOrderAPI with updated input: " + SCXmlUtil.getString(indoc));
		return hasValidLine;
	}

	/**
	 * Calls closeReceipt API after receiveOrder
	 */
	private void closeReceiptInOMS(YFSEnvironment env, Document receiveOrderOutDoc) {
		LOGGER.verbose("Starts closeReceiptInOMS with input: " + SCXmlUtil.getString(receiveOrderOutDoc));

		Element receiveOrderOutEle = receiveOrderOutDoc.getDocumentElement();
		String receiptHeaderKey = receiveOrderOutEle.getAttribute(A_RECEIPT_HEADER_KEY);

		Document closeReceiptIndoc = SCXmlUtil.createDocument(E_RECEIPT);
		Element closeReceiptEle = closeReceiptIndoc.getDocumentElement();
		closeReceiptEle.setAttribute(A_DOCUMENT_TYPE, A_TO_DOCUMENT_TYPE);
		closeReceiptEle.setAttribute(A_RECEIPT_HEADER_KEY, receiptHeaderKey);

		try {
			Document closeReceiptOutDoc = CommonUtil.invokeAPI(env, "", API_CLOSE_RECEIPT, closeReceiptIndoc);
			LOGGER.verbose("Close receipt API completed successfully: " + SCXmlUtil.getString(closeReceiptOutDoc));
		} catch (Exception e) {
			LOGGER.error("Error in closeReceipt API", e);
		}
	}
}
