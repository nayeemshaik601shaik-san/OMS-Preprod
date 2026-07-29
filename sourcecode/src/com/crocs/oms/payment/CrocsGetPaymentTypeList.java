package com.crocs.oms.payment;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.crocs.oms.common.util.CrocsXmlConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSException;

/**
 * Description: EOMS-3115 : This class filter out all the payment Methods in the call center UI 
 * 
 */
public class CrocsGetPaymentTypeList {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsGetPaymentTypeList.class);
	
	
	public Document removePaymentTypes(Document inDoc) {
		
		logger.verbose("CrocsGetPaymentTypeList :removePaymentTypes Input XML: " + SCXmlUtil.getString(inDoc));
		
		try {
			if(inDoc!=null && inDoc.hasChildNodes()) {
				
				NodeList paymentTypeLists = inDoc.getDocumentElement().getElementsByTagName(CrocsXmlConstants.A_PAYMENT_TYPE);
				
				while (paymentTypeLists.getLength() > 0) {
					paymentTypeLists.item(0).getParentNode().removeChild(paymentTypeLists.item(0));
		        }
			}
		}catch(Exception e) {
			throw new YFSException("CrocsGetPaymentTypeList :removePaymentTypes: " + e.getMessage());
		}
		logger.verbose("CrocsGetPaymentTypeList :removePaymentTypes: END ");
		return inDoc;
	}
}
