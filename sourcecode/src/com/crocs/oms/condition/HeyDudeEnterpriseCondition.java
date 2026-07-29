package com.crocs.oms.condition;

import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CrocsConstant;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.ycp.japi.YCPDynamicConditionEx;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;

/**
 * EOMS-10453 Changes - EMEA Shipment Invoice
 * 
 * Dynamic condition used to identify whether the invoice belongs to a
 * HeyDude enterprise.
 *
 * This condition evaluates the EnterpriseCode from the invoice input XML
 * and returns true when the enterprise code contains "HEYDUDE".
 */
public class HeyDudeEnterpriseCondition implements YCPDynamicConditionEx, CrocsConstant{

	private static YFCLogCategory logger = YFCLogCategory.instance(HeyDudeEnterpriseCondition.class);

	@Override
	public boolean evaluateCondition(YFSEnvironment arg0, String arg1, Map arg2, Document inDoc) {
		
	    logger.verbose("HeyDudeEnterpriseCondition : evaluateCondition : Input is :" + SCXmlUtil.getString(inDoc));

		Element eleInvoiceDetail = inDoc.getDocumentElement();
		
	    // Get the EnterpriseCode from the invoice header.
		String strEnterpriseCode = SCXmlUtil.getXpathAttribute(eleInvoiceDetail,"//InvoiceDetail/InvoiceHeader/@EnterpriseCode");
	    logger.verbose("HeyDudeEnterpriseCondition : evaluateCondition : strEnterpriseCode is :" + strEnterpriseCode);

	    // Return true when the enterprise belongs to HeyDude.
		if(!YFCCommon.isVoid(strEnterpriseCode) && strEnterpriseCode.contains("HEYDUDE")) {
		    logger.verbose("HeyDudeEnterpriseCondition : evaluateCondition : HEYDUDE Enterpirse found returning true :");
			return true;
		}
		
		logger.verbose("HeyDudeEnterpriseCondition : evaluateCondition : CROCS Enterpirse found returning false");
		return false;
	}

	@Override
	public void setProperties(Map props) {
		/**
		 * No properties are required for this condition implementation.
		 * Method provided to satisfy the YCPDynamicConditionEx interface.
		 * 
		 */
	}

}
