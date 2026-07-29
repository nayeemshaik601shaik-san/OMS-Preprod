package com.crocs.oms.mp;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
 * EOMS-6046 : Duplication Order Check for MP orders.
 * 
 */
public class CrocsCheckDuplicateOrderForMPOrders implements CrocsConstant {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsCheckDuplicateOrderForMPOrders.class);

	public Document crocsCheckDuplicateOrderForMP(YFSEnvironment env, Document inDoc) throws YFSException {

		logger.beginTimer("CrocsCheckDuplicateOrderForMPOrders.crocsCheckDuplicateOrderForMP(): Begin");
		logger.info("Input for crocsCheckDuplicateOrderForMP: Start :: " + XMLUtil.getXMLString(inDoc));

		String strOrderType = "";
		Element eleOrder = inDoc.getDocumentElement();
		strOrderType = eleOrder.getAttribute(A_ORDER_TYPE);

		// Checking duplicate order for MP orders
		if (!YFCCommon.isVoid(strOrderType) && CrocsConstant.ORDER_TYPE_MP.equalsIgnoreCase(strOrderType)) {

			String strCustomerPONo = "";
			Document docGetOrderListOutput = null;
			if (!YFCCommon.isVoid(eleOrder.getAttribute(CrocsConstant.CustomerPONo))) {

				strCustomerPONo = eleOrder.getAttribute(CrocsConstant.CustomerPONo);

				// Calling getOrderList
				docGetOrderListOutput = getOrderListForCustometPONo(env, strCustomerPONo);

				if (docGetOrderListOutput != null && Double
						.parseDouble(docGetOrderListOutput.getDocumentElement().getAttribute(A_TOTAL_ORDER_LIST)) > 0) {

					throw new YFSException(CrocsErrorConstants.VAL_ERROR_DESCRIPTION_MP,
							CrocsErrorConstants.VAL_ERROR_CODE_EXTN_004,
							CrocsErrorConstants.VAL_ERROR_DESCRIPTION_EXTN_004);

				}
			}
		}
		logger.verbose("CrocsCheckDuplicateOrderForMPOrders : crocsCheckDuplicateOrderForMP: END");
		return inDoc;
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

		logger.verbose("CrocsBeforeCreateSOUEImpl : getOrderListForCustometPONo: START");

		Document getOrderListOut = null;
		try {
			if (!YFCObject.isVoid(strCustomerPONo)) {

				Document getOrderListInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER);
				getOrderListInDoc.getDocumentElement().setAttribute(A_DOCUMENT_TYPE,
						CrocsConstant.A_SALES_ORDER_DOCUMENT_TYPE);
				getOrderListInDoc.getDocumentElement().setAttribute(CrocsConstant.CustomerPONo, strCustomerPONo);
				getOrderListOut = CommonUtil.invokeService(env, CrocsConstant.A_CROCS_GET_ORDER_LIST,
						getOrderListInDoc);
			}
		} catch (Exception e) {
			throw new YFSException("CrocsBeforeCreateSOUEImpl.getOrderListForCustometPONo :Expection" + e.getMessage());
		}
		logger.verbose("CrocsBeforeCreateSOUEImpl : getOrderListForCustometPONo:: END:: "
				+ XMLUtil.getXMLString(getOrderListOut));
		return getOrderListOut;
	}

}
