package com.crocs.oms.order;

import java.util.ArrayList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

/* EOMS-11007 : Crocs CA orders with PO BOX address are stamped with 1032 node on reship Lines
 * 
 */
public class CrocsValidatePOBoxForReshipLines {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsValidatePOBoxForReshipLines.class);

	/*
	 * Description: This Method helps to call GetOrderList. based on output,
	 * checking whether this Order is POBox Order or Not. if yes, updating 1032
	 * fulfillmentType on OrderLines.
	 * 
	 */
	public Document crocsValidatePOBoxForReshipLines(YFSEnvironment env, Document inDoc) throws YFSException {

		logger.beginTimer("CrocsValidatePOBoxForReshipLines.crocsPrepareInputForCreateOrder(): Begin");
		logger.verbose("Input for crocsValidatePOBoxForReshipLines: Start :: " + XMLUtil.getXMLString(inDoc));

		String strOrderHeaderKey = null;
		Document docGetOrderListOutput = null;
		try {
			Element eleOrder = inDoc.getDocumentElement();
			strOrderHeaderKey = eleOrder.getAttribute(CrocsXmlConstants.A_ORDER_HEADER_KEY);

			// Validate input early
			if (YFCObject.isVoid(strOrderHeaderKey)) {
				logger.verbose("OrderHeaderKey is empty. Skipping processing.");
				return inDoc;
			}

			// Calling getOrderList
			docGetOrderListOutput = getOrderListForReshipLines(env, strOrderHeaderKey);

			if (!YFCCommon.isVoid(docGetOrderListOutput) && docGetOrderListOutput != null) {

				Element orderInfo = SCXmlUtil.getChildElement(docGetOrderListOutput.getDocumentElement(),
						CrocsXmlConstants.E_ORDER);
				Document docOrderDetails = XMLUtil.getDocumentFromElement(orderInfo);

				if (CommonUtil.validatePOBoxAddress(docOrderDetails)) {

					Element orderLines = SCXmlUtil.getChildElement(eleOrder, CrocsXmlConstants.E_ORDER_LINES);
					ArrayList<Element> orderLineList = SCXmlUtil.getChildren(orderLines,
							CrocsXmlConstants.E_ORDER_LINE);

					for (Element orderLine : orderLineList) {
						orderLine.setAttribute(CrocsXmlConstants.A_FULFILLMENT_TYPE,
								CrocsConstant.CROCS_CA_FULFILLMENT_TYPE_EXPRESS);
					}
				}
			}
		} catch (Exception e) {

			logger.verbose(
					"CrocsValidatePOBoxForReshipLines.crocsValidatePOBoxForReshipLines :Expection" + e.getMessage());
			throw new YFSException(
					"CrocsValidatePOBoxForReshipLines.crocsValidatePOBoxForReshipLines :Expection" + e.getMessage());

		}
		return inDoc;
	}

	/**
	 * Description: Calling GetOrderList with OrderHeaderKey
	 *
	 * @param env
	 * @param strOrderHeaderKey
	 * @return
	 * @throws Exception
	 */
	public static Document getOrderListForReshipLines(YFSEnvironment env, String strOrderHeaderKey)
			throws YFSException {

		logger.verbose("CrocsValidatePOBoxForReshipLines : getOrderListForReshipLines: START");

		Document getOrderListOut = null;
		try {
			if (!YFCObject.isVoid(strOrderHeaderKey)) {

				Document getOrderListInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER);
				getOrderListInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_ORDER_HEADER_KEY,
						strOrderHeaderKey);
				getOrderListOut = CommonUtil.invokeService(env, CrocsConstant.STR_CROCS_GET_ORDERLIST_FOR_RESHIP_LINES,
						getOrderListInDoc);
			}
		} catch (Exception e) {
			throw new YFSException(
					"CrocsValidatePOBoxForReshipLines.getOrderListForReshipLines :Expection" + e.getMessage());
		}
		logger.verbose("CrocsValidatePOBoxForReshipLines : getOrderListForReshipLines:: END:: "
				+ XMLUtil.getXMLString(getOrderListOut));
		return getOrderListOut;
	}

}
