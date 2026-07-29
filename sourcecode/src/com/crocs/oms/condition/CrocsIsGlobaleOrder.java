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

public class CrocsIsGlobaleOrder implements YCPDynamicConditionEx, CrocsConstant{
	
	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsIsGlobaleOrder.class);


	@Override
	public boolean evaluateCondition(YFSEnvironment env, String arg1, Map arg2, Document inDoc) {
		
	    logger.verbose("CrocsIsGlobaleOrder : evaluateCondition : Input is :" + SCXmlUtil.getString(inDoc));
		
		Element orderEle = inDoc.getDocumentElement();
        String enteredBy = orderEle.getAttribute(A_ENTERED_BY);
        if(YFCCommon.isVoid(enteredBy) || !V_GLOBALE.equalsIgnoreCase(enteredBy)) {
        	return true;
        }
		
		return false;
	}

	@Override
	public void setProperties(Map arg0) {
		
	}

}
