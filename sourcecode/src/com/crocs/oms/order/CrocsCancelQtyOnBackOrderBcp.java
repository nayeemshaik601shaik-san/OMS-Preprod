package com.crocs.oms.order;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSException;

/**
 * EOMS-9160: Back Order POC, Design, and Implementation for CROCS CA to cancel 
 * the BackOrderedQty due to inventory shortage after one retry
 * 
 */

public class CrocsCancelQtyOnBackOrderBcp implements CrocsXmlConstants {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsCancelQtyOnBackOrderBcp.class);

	/**
	 * Description: Below method used to get the BackOrderd Qty from Order Lines. 
	 * And prepares the ChangeOrder Input and calling change Order API.
	 * @param env
	 * @param inDoc
	 * @return
	 * @throws Exception
	 */
	public Document prepareInputForCancelQtyOnBackOrder(Document inDoc) throws YFSException {

		logger.beginTimer("CrocsCancelQtyOnBackOrder.prepareInputForCancelQtyOnBackOrder(): Begin");
		logger.verbose("Input for prepareInputForCancelQtyOnBackOrder: Start :: " + XMLUtil.getXMLString(inDoc));

		Document docChangeOrderInput;

		try {
			Element eleOrder = inDoc.getDocumentElement();
			Element orderLinesEle = SCXmlUtil.getChildElement(eleOrder, E_ORDER_LINES);
			NodeList orderLineList = orderLinesEle.getElementsByTagName(E_ORDER_LINE);

			// Preparing the Change Order Input
			docChangeOrderInput = SCXmlUtil.createDocument(E_ORDER);
			docChangeOrderInput.getDocumentElement().setAttribute(A_ORDER_HEADER_KEY,
					eleOrder.getAttribute(A_ORDER_HEADER_KEY));
			docChangeOrderInput.getDocumentElement().setAttribute(A_DOCUMENT_TYPE,
					eleOrder.getAttribute(A_DOCUMENT_TYPE));
			docChangeOrderInput.getDocumentElement().setAttribute(A_ENTERPRISE_CODE,
					eleOrder.getAttribute(A_ENTERPRISE_CODE));
			docChangeOrderInput.getDocumentElement().setAttribute(A_ORDER_NO, eleOrder.getAttribute(A_ORDER_NO));

			Element eleChangeOrderLines = docChangeOrderInput.createElement(E_ORDER_LINES);

			for (int i = 0; i < orderLineList.getLength(); i++) {

				Element eleOrderLine = (Element) orderLineList.item(i);
				String strOrderLineKey = eleOrderLine.getAttribute(A_ORDER_LINE_KEY);
				String strOrderedQty = eleOrderLine.getAttribute(A_ORDERED_QTY);
				Double dOrderdQty = Double.parseDouble(strOrderedQty);

				Element eleBackOrderedFrom = XMLUtil.getElementByXPath(inDoc,
						"//Order/OrderLines/OrderLine[@OrderLineKey='" + strOrderLineKey
								+ "']/StatusBreakupForBackOrderedQty/BackOrderedFrom");

				if (!YFCCommon.isVoid(eleBackOrderedFrom)) {

					String strBackOrderedStatus = eleBackOrderedFrom.getAttribute(A_STATUS);
					String strBackOrderedDescription = eleBackOrderedFrom.getAttribute(A_STATUS_DESCRIPTION);
					String strBackOrderedQty = eleBackOrderedFrom.getAttribute(A_BACK_ORDERED_QTY);
					Double dBackOrderedQty = Double.parseDouble(strBackOrderedQty);

					if (CrocsConstant.VAL_STATUS_1300.equalsIgnoreCase(strBackOrderedStatus)
							&& A_BACKORDERED.equalsIgnoreCase(strBackOrderedDescription) && dBackOrderedQty > 0) {

						Element eleChangeOrderLine = docChangeOrderInput.createElement(E_ORDER_LINE);

						// setting the orderLineKey, OrderdedQty, QuantityTocancel
						eleChangeOrderLine.setAttribute(A_ORDER_LINE_KEY, strOrderLineKey);
						eleChangeOrderLine.setAttribute(A_ORDERED_QTY, String.valueOf(dOrderdQty - dBackOrderedQty));
						eleChangeOrderLine.setAttribute(A_QUANTITY_TO_CANCEL, strBackOrderedQty);
						eleChangeOrderLines.appendChild(eleChangeOrderLine);
					}
				}

			}
			docChangeOrderInput.getDocumentElement().appendChild(eleChangeOrderLines);
			if (eleChangeOrderLines.getElementsByTagName(E_ORDER_LINE).getLength() > 0) {
				docChangeOrderInput.getDocumentElement().setAttribute(A_IS_CANCELLED, FLAG_Y);

				// Adding Notes to Order tag
				Element eleChangeOrderNotes = docChangeOrderInput.createElement(E_NOTES);
				Element eleChangeOrderNote = docChangeOrderInput.createElement(E_NOTE);
				eleChangeOrderNote.setAttribute(A_NOTE_TEXT, "cancelling backordered qty  due to Inventory Shortage");
				eleChangeOrderNotes.appendChild(eleChangeOrderNote);
				docChangeOrderInput.getDocumentElement().appendChild(eleChangeOrderNotes);
			} else {
				docChangeOrderInput.getDocumentElement().setAttribute(A_IS_CANCELLED, FLAG_N);
			}

		} catch (Exception e) {
			logger.error("Error in prepareInputForCancelQtyOnBackOrder", e);
			throw new YFSException(e.getMessage());
		}
		logger.verbose("CrocsCancelQtyOnBackOrder : prepareInputForCancelQtyOnBackOrder: END");
		return docChangeOrderInput;
	}

}
