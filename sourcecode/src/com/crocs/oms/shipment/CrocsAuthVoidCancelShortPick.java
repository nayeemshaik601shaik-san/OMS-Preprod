package com.crocs.oms.shipment;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class CrocsAuthVoidCancelShortPick implements CrocsConstant{
	
    private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsAuthVoidCancelShortPick.class);
    
    public Document prepareMsgForAuthVoidCancel(YFSEnvironment env, Document inDoc) {
		logger.verbose("Start of method prepareMsgForAuthVoidCancel with input: " + SCXmlUtil.getString(inDoc));

    	Document outDoc = null;
    	
    	Element inDocEle = inDoc.getDocumentElement();
    	Element orderEle = SCXmlUtil.getChildElement(inDocEle, E_ORDER);
    	
    	Document orderDetailsDoc = SCXmlUtil.createDocument(E_ORDER);
    	Element orderDetailsEle = orderDetailsDoc.getDocumentElement();
    	
    	orderDetailsEle.setAttribute(A_ORDER_NO, orderEle.getAttribute(A_ORDER_NO));
    	
        try {
            outDoc = CommonUtil.invokeAPI(env, TEMPLATE_GET_ORDER_LIST_SHORT_PICK, API_GET_ORDER_LIST, orderDetailsDoc);
            logger.verbose("End of method getOrderList with output: " + SCXmlUtil.getString(outDoc));
        } catch (Exception e) {
            logger.error("Error invoking getOrderList API: " + e.getMessage());
            throw new YFSException("Error invoking getOrderList API: " + e.getMessage());
        }
        
        Element eleOrderList = outDoc.getDocumentElement();
    	Element orderElement = SCXmlUtil.getChildElement(eleOrderList, E_ORDER);
    	
    	Document  updatedOutDoc = SCXmlUtil.createDocument(E_ORDER);
    	
    	Element eleOrderNew = updatedOutDoc.getDocumentElement();
    	// Import the element into the target document before replacing
    	Element importedEleAptosOrder = (Element) updatedOutDoc.importNode(orderElement, true);
    	updatedOutDoc.replaceChild(importedEleAptosOrder, eleOrderNew);

		logger.verbose("End of method prepareMsgForAuthVoidCancel with output: " + SCXmlUtil.getString(outDoc));
    	return updatedOutDoc;
    }
    

}
