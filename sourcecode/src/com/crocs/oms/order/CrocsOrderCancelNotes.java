package com.crocs.oms.order;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsAPIConstants;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.order.util.CrocsOrderCancellationNotesUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class CrocsOrderCancelNotes {

    private static YFCLogCategory logger = YFCLogCategory.instance(CrocsOrderCancelNotes.class);


    /**
     * This method is used to create changeOrder Input to add notes when an order
     * line is cancelled during schedule or release
     * Events : ScheduleOrder.OnCancel and ReleaseOrder.OnCancel
     * 
     * @param env
     * @param inDoc
     * @return
     */
    public void addNotesOnCancel(YFSEnvironment env, Document inDoc) {
        logger.verbose("Input XML for addNotesOnCancel : " + SCXmlUtil.getString(inDoc));
        
         try {
            CrocsOrderCancellationNotesUtil crocsOrderCancellationNotesUtil = new CrocsOrderCancellationNotesUtil();
            String cancelReason = crocsOrderCancellationNotesUtil.getCancellationReason(env, this.getClass().getSimpleName()+"_SCH_REL");
            
            NodeList nlOrderLine = inDoc.getElementsByTagName(CrocsXmlConstants.E_ORDER_LINE); 
          for (int i = 0; i < nlOrderLine.getLength(); i++) {
                Element eleOrderLine = (Element) nlOrderLine.item(i);
                Element eleCanceledFrom = SCXmlUtil.getXpathElement(eleOrderLine,
                        CrocsConstant.XPATH_CANCELED_FROM);
                if (!YFCCommon.isVoid(eleCanceledFrom)) {
                    String strCanceledQty = eleCanceledFrom.getAttribute(CrocsXmlConstants.A_QUANTITY);
                    String noteText = CrocsConstant.ORDER_LINE_CANCEL_NOTE_TEXT.replace(CrocsConstant.QTY, strCanceledQty).replace(CrocsConstant.CANCELLED_REASON, cancelReason);  
                    logger.verbose("Cancellation Reason Code :  " + cancelReason);
                    logger.verbose("Cancellation Notes :  " + noteText); 
                    eleOrderLine.removeChild(SCXmlUtil.getXpathElement(eleOrderLine,"StatusBreakupForCanceledQty"));
                    inDoc = crocsOrderCancellationNotesUtil.getNotesTag(eleOrderLine,inDoc,noteText,cancelReason);
                  }
            }
            logger.verbose("Input XML for changeOrder API: " + SCXmlUtil.getString(inDoc));

            Document docChangeOrderOut = CommonUtil.invokeAPI(env, inDoc, CrocsAPIConstants.API_CHANGE_ORDER, inDoc);
            logger.verbose("addNotesOnCancel : changeOrder Output XML : " + SCXmlUtil.getString(docChangeOrderOut));
        
        } catch (Exception e) {
            logger.error("Error in addNotesOnCancel : " + e.getMessage());
            throw new YFSException(e.getMessage(),"EXTN_007","Error in adding cancellation notes during Schedule/Release Failure");
        
        }
    
    }

    /**
     * This method is used to create changeOrder Input to add notes when an order line is cancelled as part of short ship
     * Event : RemoveShipment.OnCancel
     * 
     * @param env
     * @param inDoc
     * @return
     */

    public void addNotesOnShortShip(YFSEnvironment env, Document inDoc) {

    
        logger.verbose("CrocsOrderCancellationNotesUtil : addNotesOnShortShip Input Xml  : " + SCXmlUtil.getString(inDoc));

        Element orderEle = inDoc.getDocumentElement();
        Document docChangeOrder  = CommonUtil
                .getChangeOrderDocInput(SCXmlUtil.getAttribute(orderEle, CrocsXmlConstants.A_ENTERPRISE_CODE),CrocsConstant.VAL_DOCUMENT_TYPE_SALES_ORDER,SCXmlUtil.getAttribute(orderEle, CrocsXmlConstants.A_ORDER_NO));   
        try {
            CrocsOrderCancellationNotesUtil crocsOrderCancellationNotesUtil = new CrocsOrderCancellationNotesUtil();
	        String cancelReason =  crocsOrderCancellationNotesUtil.getCancellationReason(env, this.getClass().getSimpleName()+"_SHORT_SHIP");
            Element eleOrder = docChangeOrder.getDocumentElement();
            Element eleOrderLines = SCXmlUtil.createChild(eleOrder, CrocsXmlConstants.E_ORDER_LINES);
            NodeList nlOrderLine = inDoc.getElementsByTagName(CrocsXmlConstants.E_ORDER_LINE);
            for (int i = 0; i < nlOrderLine.getLength(); i++) {
                Element orderLineElement = (Element) nlOrderLine.item(i);
                Element eleOrderStatus = SCXmlUtil.getXpathElement(orderLineElement, "OrderStatuses/OrderStatus[@Status='9000']");
                String strCanceledQty = eleOrderStatus.getAttribute(CrocsXmlConstants.A_STAT_QTY);
                String orderLineKey = SCXmlUtil.getAttribute(orderLineElement, CrocsXmlConstants.A_ORDER_LINE_KEY);
                Element changeOrderLineEle = SCXmlUtil.createChild(eleOrderLines, CrocsXmlConstants.E_ORDER_LINE);
                changeOrderLineEle.setAttribute(CrocsXmlConstants.A_OVERRIDE, CrocsXmlConstants.FLAG_Y);
                changeOrderLineEle.setAttribute(CrocsXmlConstants.A_ORDER_LINE_KEY, orderLineKey);
               	String noteText = CrocsConstant.ORDER_LINE_CANCEL_NOTE_TEXT.replace(CrocsConstant.QTY, strCanceledQty).replace(
                        CrocsConstant.CANCELLED_REASON, cancelReason);
                logger.verbose("Cancellation Reason Code :  " + cancelReason);
                logger.verbose("Cancellation Notes :  " + noteText);
               	docChangeOrder = crocsOrderCancellationNotesUtil.getNotesTag(changeOrderLineEle,docChangeOrder,noteText,cancelReason);
            }

            Document docChangeOrderOut = CommonUtil.invokeAPI(env, docChangeOrder, CrocsAPIConstants.API_CHANGE_ORDER, docChangeOrder);
            logger.verbose("addNotesOnShortShip : changeOrder Output XML : " + SCXmlUtil.getString(docChangeOrderOut));
        }
        catch (Exception e) {
            logger.error("Error in addNotesOnShortShip : " + e.getMessage());
            throw new YFSException(e.getMessage(),"EXTN_007","Error in adding cancellation notes during short ship");
        }   
        logger.verbose("CrocsOrderCancellationNotesUtil : addNotesOnShortShip Output Xml  : " + SCXmlUtil.getString(docChangeOrder));
   
    }

}
