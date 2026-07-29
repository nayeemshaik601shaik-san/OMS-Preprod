package com.crocs.oms.order;

import java.rmi.RemoteException;
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

public class CrocsUpdateReshipLines {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsUpdateReshipLines.class);

	/**
	 * Method to update FulfillmentType to ReshipLine 
	 * @param env
	 * @param inDoc
	 * @return
	 * @throws YFSException
	 * @throws RemoteException
	 */
	public Document updateFulfillmentTypeToReshipLine(YFSEnvironment env, Document inDoc) throws YFSException, RemoteException {

		logger.beginTimer("CrocsUpdateReshipLines : updateFulfillmentTypeToReshipLine(): Begin");
		logger.verbose("Input for updateFulfillmentTypeToReshipLine: Start :: " + XMLUtil.getXMLString(inDoc));

		Document docGetOrderListOutput = null;
		try {
			Element eleOrder = inDoc.getDocumentElement();
			String orderHeaderKey = eleOrder.getAttribute(CrocsXmlConstants.A_ORDER_HEADER_KEY);

			if (!YFCObject.isVoid(orderHeaderKey)) {
				
				/**Calling getOrderList**/
				docGetOrderListOutput = getOrderListForReshipLines(env, orderHeaderKey);
				Element reshipGetOrderListEle = docGetOrderListOutput.getDocumentElement();

				if (!YFCCommon.isVoid(reshipGetOrderListEle)) {

					Element resihpOrderEle = SCXmlUtil.getChildElement(reshipGetOrderListEle, CrocsXmlConstants.E_ORDER);
					if (CrocsConstant.HEYDUDE_CA
							.equalsIgnoreCase(resihpOrderEle.getAttribute(CrocsConstant.A_ENTERPRISE_CODE))) {

						Element orderLines = SCXmlUtil.getChildElement(eleOrder, CrocsXmlConstants.E_ORDER_LINES);
						ArrayList<Element> orderLineList = SCXmlUtil.getChildren(orderLines,
								CrocsXmlConstants.E_ORDER_LINE);

						for (Element orderLine : orderLineList) {
							orderLine.setAttribute(CrocsXmlConstants.A_FULFILLMENT_TYPE,
									CrocsConstant.HEYDUDE_CA_LVDC_FULFILLMENT_TYPE);
							logger.info("FulfillmentType Stammped: "+ orderLine.getAttribute(CrocsXmlConstants.A_FULFILLMENT_TYPE));
						}
					}
				}
			}
			
		} catch (YFSException e) {

			logger.info("CrocsUpdateReshipLines:updateFulfillmentTypeToReshipLine: Failed to update FulfillmentType for reship line."+ inDoc);
			throw new YFSException(
					"CrocsUpdateReshipLines:updateFulfillmentTypeToReshipLine :Expection" + e.getMessage());
		}
		return inDoc;
	}

	/**
	 * Calls getOrderList service using OrderHeaderKey.
	 *
	 * @param env
	 * @param orderHeaderKey
	 * @return Document
	 * @throws YFSException
	 * @throws RemoteException 
	 */
	public static Document getOrderListForReshipLines(YFSEnvironment env, String orderHeaderKey)
			throws YFSException, RemoteException {

		logger.verbose("CrocsUpdateReshipLines : getOrderListForReshipLines: START");

		Document getOrderListOut = null;
		try {
			
			Document getOrderListInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER);
			getOrderListInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_ORDER_HEADER_KEY,
					orderHeaderKey);
			getOrderListOut = CommonUtil.invokeService(env, CrocsConstant.STR_CROCS_GET_ORDERLIST_FOR_RESHIP_LINES,
					getOrderListInDoc);
			
		} catch (YFSException e) {
			logger.info("Exception occurred during getOrderList call for reship lines: " + orderHeaderKey);
			throw new YFSException(e.getMessage(), e.getErrorCode(), e.getErrorDescription());
		}
		logger.verbose(
				"CrocsUpdateReshipLines : getOrderListForReshipLines:: END:: " + XMLUtil.getXMLString(getOrderListOut));
		return getOrderListOut;
	}

}
