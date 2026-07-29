package com.crocs.oms.condition;

import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.CrocsConstant;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.ycp.japi.YCPDynamicConditionEx;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;

/**
 * Description: Applying Fraud hold on Order Creation When Fraud status not Accept and for Call center orders with $0.00.
 * 
 */
public class CrocsValidateFraudStatus implements YCPDynamicConditionEx {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsValidateFraudStatus.class);

	@Override
	public boolean evaluateCondition(YFSEnvironment env, String s, Map mapData, Document indoc) {
		
		logger.verbose("Input to the evaluateCondition is:" + SCXmlUtil.getString(indoc));
		
		Element orderEle = indoc.getDocumentElement();
		double dTotalAmount=0.00;
		String strTotaAmount = SCXmlUtil.getXpathAttribute(orderEle,"/Order/PriceInfo/@TotalAmount");
		
		if(!YFCCommon.isVoid(strTotaAmount))
			dTotalAmount = Double.parseDouble(strTotaAmount);
		
		if (orderEle.getAttribute(CrocsConstant.A_ENTRY_TYPE) != null
				&& CrocsConstant.ENTRY_TYPE_CALL_CENTER.equalsIgnoreCase(orderEle.getAttribute(CrocsConstant.A_ENTRY_TYPE)) 
				&& dTotalAmount<=0.00) {
			return false;
			//EOMS-5403 :: Hold changes for US and CA marketplace order : START
		}else if (orderEle.getAttribute(CrocsXmlConstants.A_ORDER_TYPE) != null
				&& CrocsConstant.ORDER_TYPE_MP.equalsIgnoreCase(orderEle.getAttribute(CrocsXmlConstants.A_ORDER_TYPE))) {
			return false;
			//EOMS-5403 :: Hold changes for US and CA marketplace order : END
		} else if(orderEle.getAttribute(CrocsXmlConstants.A_ORDER_PURPOSE) != null
				&& CrocsXmlConstants.A_EVENT_CODE_REFUND.equalsIgnoreCase(orderEle.getAttribute(CrocsXmlConstants.A_ORDER_PURPOSE))){
			return false;
		}
		
		/**
		 * Applying Hold only in case of non GlobalE Sales orders for HEYDUDE_CA For
		 * GlobalE Orders Hold will not be applied which is current functionality
		 **/
		else if (!YFCCommon.isVoid(orderEle.getAttribute(CrocsConstant.A_ENTERED_BY))
				&& CrocsXmlConstants.V_GLOBALE.equalsIgnoreCase(orderEle.getAttribute(CrocsConstant.A_ENTERED_BY))) {
			return false;

		}else {
			if (SCXmlUtil.getChildElement(orderEle, CrocsXmlConstants.E_EXTN) != null) {

				Element eleOrderLineExtn = SCXmlUtil.getChildElement(orderEle, CrocsXmlConstants.E_EXTN);
				if (eleOrderLineExtn.hasAttribute(CrocsXmlConstants.A_EXTN_FRAUD_STATUS)
						&& eleOrderLineExtn.getAttribute(CrocsXmlConstants.A_EXTN_FRAUD_STATUS) != null) {

					String strExtnFraudStatus = eleOrderLineExtn.getAttribute(CrocsXmlConstants.A_EXTN_FRAUD_STATUS);
					if (CrocsConstant.A_ACCEPT.equalsIgnoreCase(strExtnFraudStatus))
						return false;
				}
			}
		}
		return true;
	}

	@Override
	public void setProperties(Map arg0) {
	}

}
