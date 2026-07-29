package com.crocs.oms.order;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

/**
 * Description: Checking Fraud Validation only for Post Auth Payment Methods when ExtnFraudStatus as "RETRY"
 * 
 */
public class CrocsCheckFraudCheckOnRetry {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsOrderUpdate.class);
	
	public static Document validateFraudCheckOnRetryStatus(YFSEnvironment env, Document inDoc) throws YFSException {
		
		logger.verbose("CrocsCheckFraudCheckOnRetry : validateFraudCheckOnRetryStatus: START" + XMLUtil.getXMLString(inDoc));
		Document docChangeOrderInput = null;
		try {
			
			String strOrderNo = SCXmlUtil.getXpathAttribute(inDoc.getDocumentElement(),CrocsConstant.STR_XPATH_MONITOR_ORDER_NO);
			Document docGetOrderListOutput = getOrderListForSaleOrderNo(env, strOrderNo);
			
			docChangeOrderInput = CrocsProcessAdyenWebhooks.validateFraudCheckForPostAuthPayments(env, docGetOrderListOutput,docChangeOrderInput);
			
		}catch(Exception e) {
			
			logger.verbose("CrocsCheckFraudCheckOnRetry.validateFraudCheckOnRetryStatus :Expection" + e.getMessage());
			
		}
		logger.verbose("CrocsCheckFraudCheckOnRetry : validateFraudCheckOnRetryStatus:: END:: " );
		return docChangeOrderInput;
	}
	
	/**
	 * Description: Calling GetOrderList with Sales Order No
	 *
	 * @param env
	 * @param strOrderNo
	 * @return
	 * @throws Exception
	 */
	public static Document getOrderListForSaleOrderNo(YFSEnvironment env, String strOrderNo) throws YFSException {
		
		logger.verbose("CrocsProcessAdyenWebhooks : getOrderListForSaleOrderNo: START");

		Document getOrderListOut = null;
		try {
			if (!YFCObject.isVoid(strOrderNo)) {

				Document getOrderListInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER);
				getOrderListInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_ORDER_NO, strOrderNo);
				getOrderListOut = CommonUtil.invokeService(env, CrocsConstant.CROCS_GET_ORDER_LIST_FOR_FORTER,
						getOrderListInDoc);
			}
		} catch (Exception e) {
			throw new YFSException("CrocsProcessAdyenWebhooks.getOrderListForSaleOrderNo :Expection" + e.getMessage());
		}
		logger.verbose("CrocsProcessAdyenWebhooks : getOrderListForSaleOrderNo:: END:: " + XMLUtil.getXMLString(getOrderListOut));
		return getOrderListOut;
	}
}
