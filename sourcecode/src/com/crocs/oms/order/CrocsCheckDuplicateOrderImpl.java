package com.crocs.oms.order;

import com.yantra.pca.ycd.business.YCDCheckDuplicateOrder;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.dom.YFCNode;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import java.util.Iterator;
import org.w3c.dom.Document;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.XMLUtil;

public class CrocsCheckDuplicateOrderImpl implements CrocsConstant {
   
    private static YFCLogCategory logger = YFCLogCategory.instance(CrocsCheckDuplicateOrderImpl.class.getName());

   /*
    * This method takes the input hold type process and check the duplicate order api 
    * get the duplicate order header key
    * get the status
    * Create the document for order hold type
    * fetch the reson text
    * return the out DOC
    * if it's duplicate- Order is identied as duplicate
    * else - order is not duplicate */
    public Document invoke(YFSEnvironment env, Document inDoc) {
    	logger.verbose("Input Doc for CrocsCheckDuplicateOrderImpl class:invoke(): START" + XMLUtil.getXMLString(inDoc));
    	
        YFCDocument orderDoc = YFCDocument.getDocumentFor(inDoc);
        YFCElement holdTypesToProcess = orderDoc.getDocumentElement().getChildElement(E_HOLD_TYPES_TO_PROCESS);
        
        //call the checkDuplicateOrder 
        YCDCheckDuplicateOrder api = new YCDCheckDuplicateOrder();
        YFCDocument checkDuplicateOrderOutput = api.checkDuplicateOrder(env, orderDoc);
        
        //1300 is resolved status
        String status = VAL_STATUS_1300;
        if (!YFCCommon.isVoid(checkDuplicateOrderOutput.getDocumentElement().getAttribute(A_DUP_ORDER_HEADER_KEY))) {
        	// hold status
            status = VAL_STATUS_1200;
        }
        
        // Create a new document 
        YFCDocument outDoc = YFCDocument.createDocument();
        YFCNode root = outDoc.importNode(orderDoc.getDocumentElement(), true);
        
        //append the root document to the outDoc
        outDoc.appendChild(root);
        YFCElement order = outDoc.getDocumentElement();
       
        // Create a child element for process hold type
        YFCElement processedHoldTypes = order.createChild(E_PROCESSED_HOLD_TYPES);
        Iterator<?> itr = holdTypesToProcess.getChildren();

        while(itr.hasNext()) {
        	logger.verbose("while loop iteration: getting list of processed hold types:"+itr);
        	
            YFCElement holdToProcess = (YFCElement)itr.next();
            //Create child element for order hold type
            YFCElement processedHold = processedHoldTypes.createChild(E_ORDER_HOLD_TYPE);
            processedHold.setAttribute(A_HOLD_TYPE, holdToProcess.getAttribute(A_HOLD_TYPE));
            String reasonText = "";
            
            // Check the reson text based status
            if(YFCCommon.isVoid(holdToProcess.getAttribute(A_REASON_TEXT))) {
            	reasonText = getReasonText(status);
            	 logger.verbose("reasonText if it's empty:"+reasonText);
            } else {
            	if(holdToProcess.getAttribute(A_REASON_TEXT) != "") {
            		reasonText = holdToProcess.getAttribute(A_REASON_TEXT);
            		 logger.verbose("reasonText if it's not null:"+reasonText);
            	}
            	else {
            		reasonText = getReasonText(status);
            		 logger.verbose("reasonText in else condition:"+reasonText);
            	}
            }
            processedHold.setAttribute(A_REASON_TEXT, reasonText);
            processedHold.setAttribute(A_STATUS, status);
            logger.verbose("processed Hold element:"+processedHold);
        }

        order.removeChild(order.getChildElement(E_HOLD_TYPES_TO_PROCESS));
        logger.verbose("outDoc Doc CrocsCheckDuplicateOrderImpl class:invoke(): END:"+outDoc.toString());
        return outDoc.getDocument();
    }
    
    //Method checks the based on status returning the duplicate order message
    private String getReasonText(String status) {
    	if(status == VAL_STATUS_1300) {
    		logger.verbose("For not duplicate Order status is 1300:"+status);
			return MSG_NOT_DUPLICATE;
		} else if(status == VAL_STATUS_1200) {
			logger.verbose("For duplicate Order status is 1200:"+status);
			return MSG_DUPLICATE;
		}
    	return "";
    }
}