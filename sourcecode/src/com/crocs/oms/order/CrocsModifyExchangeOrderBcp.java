package com.crocs.oms.order;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class CrocsModifyExchangeOrderBcp implements CrocsConstant {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsModifyExchangeOrderBcp.class);

	/**
	 * EOMS-6314: This will be triggered from the monitor agent. If an exchange
	 * order has remained in “Created” status for more than 21 days, the system will
	 * cancel the exchange order
	 * 
	 * @param env
	 * @param inDoc
	 * @return
	 */
	public Document crocsCancelExchangeOrder(YFSEnvironment env, Document inDoc) {
		logger.beginTimer("CrocsCancelExchangeorder.crocsCancelExchangeOrder");
		logger.verbose("CrocsCancelExchangeorder.crocsCancelExchangeOrder input XML:" + SCXmlUtil.getString(inDoc));

		try {
			String exchangeOrderHeaderKey = SCXmlUtil.getXpathAttribute(inDoc.getDocumentElement(),
					XPATH_MONITOR_CONDOLIDATION_ORDER_HEADER_KEY);

			Document changeOrderInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER);
			Element changeOrderEle = changeOrderInDoc.getDocumentElement();
			changeOrderEle.setAttribute(CrocsConstant.OrderHeaderKey, exchangeOrderHeaderKey);
			changeOrderEle.setAttribute(CrocsXmlConstants.A_ACTION, VAL_ACTION_CANCEL);
			changeOrderEle.setAttribute(CrocsXmlConstants.A_OVERRIDE, CrocsXmlConstants.FLAG_Y);
			Element changeOrderNotesEle = SCXmlUtil.createChild(changeOrderEle, A_NOTES);
			Element changeOrderNoteEle = SCXmlUtil.createChild(changeOrderNotesEle, A_NOTE);
			changeOrderNoteEle.setAttribute(A_REASON_CODE, "Order is older than 21 days");
			changeOrderNoteEle.setAttribute(NOTE_TEXT,
					"Order cancelled by the Monitor Agent as the exchange order has remained in Created status for the past 21 days.");
			logger.debug("changeOrderInput:- " + changeOrderInDoc);

			logger.verbose("CrocsCancelExchangeorder.crocsCancelExchangeOrder changeOrderInDoc:"
					+ SCXmlUtil.getString(changeOrderInDoc));

			CommonUtil.invokeAPI(env, "", API_CHANGE_ORDER, changeOrderInDoc);

		} catch (Exception e) {
			logger.info("OMS_Update : CrocsCancelExchangeorder : crocsCancelExchangeOrder : in catch block  : " + "\n"
					+ e.getMessage());
			throw new YFSException(e.getMessage(), "", "Error in CrocsCancelExchangeorder.crocsCancelExchangeOrder() class");
		}
		logger.endTimer("CrocsCancelExchangeorder.crocsCancelExchangeorder");
		return inDoc;

	}

	/**
	 * EOMS-6452: This will be triggered by the monitor agent. If an exchange order
	 * line reservation fails, the exchange order line will be cancelled.
	 * if we are not able to secure reservation at line level, the exchange order line will be cancelled post order create.
	 * @param env
	 * @param inDoc
	 * @return
	 */
	public Document cancelExchangeOrderLine(YFSEnvironment env, Document inDoc) {
		try {
			String exchangeOrderHeaderKey = SCXmlUtil.getXpathAttribute(inDoc.getDocumentElement(),
					XPATH_MONITOR_CONDOLIDATION_ORDER_HEADER_KEY);

			Document changeOrderInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER);
			Element changeOrderEle = changeOrderInDoc.getDocumentElement();
			changeOrderEle.setAttribute(CrocsConstant.OrderHeaderKey, exchangeOrderHeaderKey);

			Element changeOrderLinesEle = SCXmlUtil.createChild(changeOrderEle, E_ORDER_LINES);

			Element eleOrderLines = (Element) inDoc.getElementsByTagName(E_ORDER_LINES).item(0);
			NodeList orderLineList = SCXmlUtil.getXpathNodes(eleOrderLines, E_ORDER_LINE);
			for (int i = 0; i < orderLineList.getLength(); i++) {
				Element orderLineElement = (Element) orderLineList.item(i);
				Element orderLineReservation=SCXmlUtil.getXpathElement(orderLineElement, XPATH_ORDER_LINE_RESERVATION);
				if (YFCCommon.isVoid(orderLineReservation)) {
					String orderLineKey = SCXmlUtil.getAttribute(orderLineElement, A_ORDER_LINE_KEY);
					Element changeOrderLineEle = SCXmlUtil.createChild(changeOrderLinesEle, E_ORDER_LINE);
					changeOrderLineEle.setAttribute(CrocsXmlConstants.A_ACTION, VAL_ACTION_CANCEL);
					changeOrderLineEle.setAttribute(CrocsXmlConstants.A_OVERRIDE, CrocsXmlConstants.FLAG_Y);
					changeOrderLineEle.setAttribute(A_ORDER_LINE_KEY, orderLineKey);
					Element changeOrderNotesEle = SCXmlUtil.createChild(changeOrderLineEle, A_NOTES);
					Element changeOrderNoteEle = SCXmlUtil.createChild(changeOrderNotesEle, A_NOTE);
					changeOrderNoteEle.setAttribute(A_REASON_CODE, "No Inventory Reservation");
					changeOrderNoteEle.setAttribute(NOTE_TEXT,
							"Reservation could not be secured due to insufficient inventory. The line was automatically cancelled by the monitoring agent. ");

				}
			}
			
			logger.verbose("CrocsCancelExchangeorder.crocsCancelExchangeOrder changeOrderInDoc:"
					+ SCXmlUtil.getString(changeOrderInDoc));
			Element elechangeOrderLines = SCXmlUtil.getXpathElement(changeOrderInDoc.getDocumentElement(),
					XPATH_ORDER_LINES);
			if (elechangeOrderLines.hasChildNodes()) {
				CommonUtil.invokeAPI(env, "", API_CHANGE_ORDER, changeOrderInDoc);
			}

		} catch (Exception e) {
			logger.info("OMS_Update : CrocsCancelExchangeorder : cancelExchangeOrderLine : in catch block  : " + "\n"
					+ e.getMessage());
			throw new YFSException(e.getMessage(), "", "Error in CrocsCancelExchangeorder.cancelExchangeOrderLine() class");
	
		}
		logger.endTimer("CrocsCancelExchangeorder.crocsCancelExchangeorder");
		return inDoc;

	}

}
