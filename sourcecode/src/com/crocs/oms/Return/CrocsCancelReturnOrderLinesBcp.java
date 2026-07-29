package com.crocs.oms.Return;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSException;

/**
 * EOMS-4431 : Canceling Return order lines after 90 days when Order line status is created state.
 * 
 */
public class CrocsCancelReturnOrderLinesBcp {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsCancelReturnOrderLinesBcp.class);

	public static Document crocsCancelReturnOrderLines(Document inDoc) {

		logger.verbose("CrocsCancelReturnOrderLines : crocsCancelReturnOrderLines: START:"+ XMLUtil.getXMLString(inDoc));
		Document docChangeOrderInput=null;
		try {
			Element eleRoot = inDoc.getDocumentElement();
			Element eleOrder = SCXmlUtil.getChildElement(eleRoot, CrocsXmlConstants.E_ORDER);
			
			String strOrderHeaderKey = eleOrder.getAttribute(CrocsXmlConstants.A_ORDER_HEADER_KEY);
			
			Element orderStatusesEle = SCXmlUtil.getChildElement(eleOrder, CrocsXmlConstants.E_ORDER_STATUSES);
			NodeList odrerStatusList = orderStatusesEle.getElementsByTagName(CrocsXmlConstants.E_ORDER_STATUS);
			
			docChangeOrderInput = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER);
			docChangeOrderInput.getDocumentElement().setAttribute(CrocsXmlConstants.A_ORDER_HEADER_KEY, strOrderHeaderKey);
			docChangeOrderInput.getDocumentElement().setAttribute(CrocsXmlConstants.A_OVERRIDE, CrocsXmlConstants.FLAG_Y);
			
			Element eleOrderLines = docChangeOrderInput.createElement(CrocsXmlConstants.E_ORDER_LINES);
			for (int i = 0; i < odrerStatusList.getLength(); i++) {
				Element eleOrderLine = docChangeOrderInput.createElement(CrocsXmlConstants.E_ORDER_LINE);
				Element eleOrderStatus = (Element) odrerStatusList.item(i);
				eleOrderLine.setAttribute(CrocsXmlConstants.A_ACTION, CrocsXmlConstants.A_EVENT_CODE_CANCEL);
				eleOrderLine.setAttribute(CrocsXmlConstants.A_ORDER_LINE_KEY, eleOrderStatus.getAttribute(CrocsXmlConstants.A_ORDER_LINE_KEY));
				eleOrderLines.appendChild(eleOrderLine);
			}
			docChangeOrderInput.getDocumentElement().appendChild(eleOrderLines);
			
		} catch (Exception e) {
			throw new YFSException("CrocsCancelReturnOrderLines.crocsCancelReturnOrderLines :Expection" + e.getMessage());
		}
        logger.info("Cancelled ReturnOrderLines from OMS Return Monitor Event");
		logger.verbose("CrocsCancelReturnOrderLines : crocsCancelReturnOrderLines: END" + XMLUtil.getXMLString(docChangeOrderInput));
		return docChangeOrderInput;
	}

}
