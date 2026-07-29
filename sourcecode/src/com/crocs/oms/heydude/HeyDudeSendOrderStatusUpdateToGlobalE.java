package com.crocs.oms.heydude;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

/**
 * EOMS-7579 : Partial cancellation update to Global-E Implementation.
 * This class handles logic
 * to send an Partial cancellation status update to GLOBALE when the order is fully shipped.
 * And updating the flag as Y to send QUEUE.
 * SAMPLE Output XML:
 * 
 * <Order CustomerPONo="14768536868" EnterpriseCode="HEYDUDE_CA"
 *  MaxOrderStatus="3700.02" EnteredBy="GLOBALE" MinOrderStatus="3700.02"
 *  OrderHeaderKey="2025120909422476885982" OrderNo="1083687OHCA" PublishUpd="Y" IsPartialCancelled="Y">
 *   <OrderLines>
 *       <OrderLine CancelledQty="4.0" OrderLineKey="2025120909422476885983"
 *           OrderedQty="1.00" OriginalOrderedQty="5.00">
 *           <Item ItemID="40003-2BS-M15"/>
 *       </OrderLine>
 *       <OrderLine CancelledQty="2.0"
 *           OrderLineKey="2025120909422476885983" OrderedQty="3.00" OriginalOrderedQty="5.00">
 *           <Item ItemID="40003-2BS-M14"/>
 *       </OrderLine>
 *       <OrderLine CancelledQty="5.0" OrderLineKey="2025120909422476885983"
 *           OrderedQty="5.00" OriginalOrderedQty="5.00">
 *           <Item ItemID="40003-2BS-M12"/>
 *       </OrderLine>
 *   </OrderLines>
 *	</Order>
 * 
 */

public class HeyDudeSendOrderStatusUpdateToGlobalE implements CrocsConstant{
	
	private static YFCLogCategory logger = YFCLogCategory.instance(HeyDudeSendOrderStatusUpdateToGlobalE.class);
	
	/**
	 * This method get details of Sales Order
	 * and update the CancelledQty to send Status Update.
	 * 
	 * @param env
	 * @param indoc
	 * @return
	 * @throws Exception
	 */
	public Document preparePartialCancellationMsg(YFSEnvironment env, Document indoc) throws YFSException {

		logger.verbose("HeyDudeSendOrderStatusUpdateToGlobalE : preparePartialCancellationMsg : Start : " + SCXmlUtil.getString(indoc));
		 
        Document getOrderListOutDoc = null;
        Document finalDoc = null;
        String strPublishUpdate = FLAG_N;
        try {

            Element shipmentEle = indoc.getDocumentElement();
            String orderHdrKey = SCXmlUtil.getXpathAttribute(shipmentEle, XPATH_ORDER_HDR_KEY);

            // Prepare input for getOrderList
            Document getOrderListInput = SCXmlUtil.createDocument(E_ORDER);
            Element orderElement = getOrderListInput.getDocumentElement();
            orderElement.setAttribute(A_ORDER_HEADER_KEY, orderHdrKey);

            logger.verbose("HeyDudeSendOrderStatusUpdateToGlobalE : preparePartialCancellationMsg : getOrderListInput is: " + SCXmlUtil.getString(getOrderListInput));

            // Call getOrderList
            getOrderListOutDoc = CommonUtil.invokeService(env, STR_HEYDUDE_GET_ORDER_LIST_FOR_GLOBALE_SERVICE, getOrderListInput);
    		
            logger.verbose("HeyDudeSendOrderStatusUpdateToGlobalE : preparePartialCancellationMsg : getOrderListOutDoc is: " + SCXmlUtil.getString(getOrderListOutDoc));

            Element eleOrder = SCXmlUtil.getChildElement(getOrderListOutDoc.getDocumentElement(), E_ORDER);

			String strMaxOrderStatus = eleOrder.getAttribute(CrocsXmlConstants.A_MAX_ORDER_STATUS);
			String strMinOrderStatus = eleOrder.getAttribute(CrocsXmlConstants.A_MIN_ORDER_STATUS);

			if (Double.parseDouble(STATUS_SHIPPED) <= Double.parseDouble(strMaxOrderStatus)
					&& Double.parseDouble(STATUS_SHIPPED) <= Double.parseDouble(strMinOrderStatus)) {
				
				Element orderLinesEle = SCXmlUtil.getChildElement(eleOrder, CrocsXmlConstants.E_ORDER_LINES);
			    NodeList orderLineList = orderLinesEle.getElementsByTagName(CrocsXmlConstants.E_ORDER_LINE);

			    for (int i = 0; i < orderLineList.getLength(); i++) {
			    	Element eleOrderLine = (Element) orderLineList.item(i);
			        double dOrderedQty = Double.parseDouble(eleOrderLine.getAttribute(CrocsXmlConstants.A_ORDERED_QTY));
			        double dOriginalOrderedQty = Double.parseDouble(eleOrderLine.getAttribute(CrocsXmlConstants.A_ORIGINAL_ORDERED_QTY));

			        if (dOrderedQty == dOriginalOrderedQty) {
			            orderLinesEle.removeChild(eleOrderLine);
			            i--; 
			            continue;
			        }
			        double dCancelledQty = dOriginalOrderedQty - dOrderedQty;
			        eleOrderLine.setAttribute(A_CANCELLED_QTY, String.valueOf(dCancelledQty));
			    }
			    if(orderLineList.getLength()>0) {
			    	strPublishUpdate = VAL_FLAG_Y;
			    }
			}
			eleOrder.setAttribute(A_PUBLISH_STATUS_UPDATE, strPublishUpdate);
			
		
			finalDoc = SCXmlUtil.createDocument(E_ORDER);
            Element root = finalDoc.getDocumentElement();
            finalDoc.removeChild(root);

            Node importedOrder = finalDoc.importNode(eleOrder, true);
            finalDoc.appendChild(importedOrder);
            

        } catch (Exception e) {
            logger.error("HeyDudeSendOrderStatusUpdateToGlobalE: preparePartialCancellationMsg :Expection: "+ e.getMessage());
        }

       logger.verbose("HeyDudeSendOrderStatusUpdateToGlobalE : preparePartialCancellationMsg : End :" + SCXmlUtil.getString(finalDoc));
        return finalDoc;
    }

}
