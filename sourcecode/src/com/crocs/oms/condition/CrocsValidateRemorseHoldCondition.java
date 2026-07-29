package com.crocs.oms.condition;

import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.yantra.ycp.japi.YCPDynamicConditionEx;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;

/**
 * Based on the boolean value returned by method evaluateCondition it will be decided whether to apply hold or not
 * RemorseHold & DuplicateOrderHold should be applied to sales order's but it shouldn't applied for RFO Order's
 */
public class CrocsValidateRemorseHoldCondition implements YCPDynamicConditionEx, CrocsConstant{

	@Override
	public boolean evaluateCondition(YFSEnvironment arg0, String arg1, Map arg2, Document inDoc) {
				
		Element inDocEle = inDoc.getDocumentElement();
		String orderPurpose = inDocEle.getAttribute(A_ORDER_PURPOSE);
		
		//EOMS-5403 :: Hold changes for US and CA marketplace order : START
		if (!YFCCommon.isVoid(inDocEle.getAttribute(CrocsXmlConstants.A_ORDER_TYPE))
				&& CrocsConstant.ORDER_TYPE_MP.equalsIgnoreCase(inDocEle.getAttribute(CrocsXmlConstants.A_ORDER_TYPE)) 
				|| CrocsXmlConstants.V_GLOBALE.equalsIgnoreCase(inDocEle.getAttribute(CrocsConstant.A_ENTERED_BY))) {
			return false;
		}
		//EOMS-5403 :: Hold changes for US and CA marketplace order : END
		
		// if it's normal sales order then it would be null & for RFO order's it is 'REFUND'
		return orderPurpose == null || !orderPurpose.equals(A_EVENT_CODE_REFUND);
	}

	@Override
	public void setProperties(Map arg0) {
	}
}