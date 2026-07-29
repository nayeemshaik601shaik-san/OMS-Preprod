package com.crocs.oms.to.order;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsErrorConstants;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

/**
 * EOMS-6583 :  Duplicate Order Check - AptosOrderNo+Store
 * 
 */
public class CrocsCheckDuplicateOrderForTOOrders implements CrocsConstant {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsCheckDuplicateOrderForTOOrders.class);

	public Document crocsCheckDuplicateOrderForTO(YFSEnvironment env, Document inDoc) throws YFSException {

		logger.beginTimer("CrocsCheckDuplicateOrderForTOOrders.crocsCheckDuplicateOrderForTO(): Begin");
		logger.info("Input for crocsCheckDuplicateOrderForTO: Start :: " + XMLUtil.getXMLString(inDoc));

		Element eleOrder = inDoc.getDocumentElement();
		// Checking duplicate order for TO orders
	

			String strCustomerPONo = "";
			String strStoreNo="";
			Document docGetOrderListOutput = null;
			if (!YFCCommon.isVoid(eleOrder.getAttribute(CrocsConstant.CustomerPONo))) {

				strCustomerPONo = eleOrder.getAttribute(CrocsConstant.CustomerPONo);
				Element eleOrderLine = (Element) eleOrder.getElementsByTagName(CrocsXmlConstants.E_ORDER_LINE).item(0);
				strStoreNo = eleOrderLine.getAttribute(CrocsXmlConstants.A_RECEIVING_NODE);
				

				// Calling getOrderList
				docGetOrderListOutput = getOrderListForCustometPONo(env, strCustomerPONo);

				if (docGetOrderListOutput != null && Double
						.parseDouble(docGetOrderListOutput.getDocumentElement().getAttribute(A_TOTAL_ORDER_LIST)) > 0) {

					validateDuplicateOrders(docGetOrderListOutput.getDocumentElement(), strStoreNo, strCustomerPONo);
				}
			}
		
		logger.verbose("CrocsCheckDuplicateOrderForTOOrders : crocsCheckDuplicateOrderForTO: END");
		return inDoc;
	}
	
	/**
	 * Validates if any existing orders in the OrderList match the given ReceivingNodes.
	 * Throws YFSException if a duplicate CustomerPONo + ReceivingNode combination is found.
	 */
	private void validateDuplicateOrders(Element eleOrderList, String storeNode, String strCustomerPONo)
			throws YFSException {
		NodeList nlOrders = eleOrderList.getElementsByTagName(CrocsXmlConstants.E_ORDER);

		for (int i = 0; i < nlOrders.getLength(); i++) {
			Element eleExistingOrder = (Element) nlOrders.item(i);
			Element eleExistingOrderLine = (Element) eleExistingOrder
					.getElementsByTagName(CrocsXmlConstants.E_ORDER_LINE).item(0);
			String existingReceivingNode = eleExistingOrderLine.getAttribute(CrocsXmlConstants.A_RECEIVING_NODE);

			if (storeNode.equals(existingReceivingNode)) {
				logger.error(String.format("Duplicate Order detected — CustomerPONo: %s, ReceivingNode: %s",
						strCustomerPONo, existingReceivingNode));

				throw new YFSException(CrocsErrorConstants.VAL_ERROR_DESCRIPTION_TO,
						CrocsErrorConstants.VAL_ERROR_CODE_EXTN_004,
						CrocsErrorConstants.VAL_ERROR_DESCRIPTION_EXTN_004);
			}
		}
	}
	/**
	 * Description: Calling GetOrderList with Customer PO Number.
	 *
	 * @param env
	 * @param strOrderNo
	 * @return
	 * @throws Exception
	 */
	public static Document getOrderListForCustometPONo(YFSEnvironment env, String strCustomerPONo) throws YFSException {

		logger.verbose("CrocsCheckDuplicateOrderForTOOrders : getOrderListForCustometPONo: START");

		Document getOrderListOut = null;
		try {
			if (!YFCObject.isVoid(strCustomerPONo)) {

				Document getOrderListInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER);
				getOrderListInDoc.getDocumentElement().setAttribute(A_DOCUMENT_TYPE,
						CrocsConstant.A_TO_DOCUMENT_TYPE);
				getOrderListInDoc.getDocumentElement().setAttribute(CrocsConstant.CustomerPONo, strCustomerPONo);
				getOrderListOut = CommonUtil.invokeService(env, CrocsConstant.A_CROCS_GET_ORDER_LIST_TO,
						getOrderListInDoc);
			}
		} catch (Exception e) {
			throw new YFSException("CrocsCheckDuplicateOrderForTOOrders.getOrderListForCustometPONo :Expection" + e.getMessage());
		}
		logger.verbose("CrocsCheckDuplicateOrderForTOOrders : getOrderListForCustometPONo:: END:: "
				+ XMLUtil.getXMLString(getOrderListOut));
		return getOrderListOut;
	}

}
