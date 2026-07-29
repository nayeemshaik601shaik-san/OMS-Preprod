package com.crocs.oms.order.receipt;

import java.rmi.RemoteException;
import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;

/**
 * 
 * @author IBM
 * 
 * This class is getting Called on Success of Return Order Created
 * Only For APTOS return to which OMS has to internally prepare
 * receive return order message so that order is marked received in 
 * system and invoice can be generated
 *
 */
public class CrocsAptosOrderPrepReceiveOrderInput implements CrocsConstant{

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsAptosOrderPrepReceiveOrderInput.class);

	public Document prepareReceiveReturnMsgInOMS(YFSEnvironment env, Document indoc){

		logger.verbose("CrocsAptosOrderPrepReceiveOrderInput::prepareReceiveReturnMsgInOMS:: Starts :"
				+SCXmlUtil.getString(indoc));

		String strEntryType = "";
		String strShipNode = "";
		String strOrderNo = "";
		String strEnterpriseCode = "";
		Document receiveOrderIndoc = null;
		//input is on success of Create Return order
		Element orderEle = indoc.getDocumentElement();
		Element orderLines = SCXmlUtil.getChildElement(orderEle, E_ORDER_LINES);
		ArrayList<Element> returnOrderLineList = SCXmlUtil.getChildren(orderLines, E_ORDER_LINE);
		strEntryType = orderEle.getAttribute(A_ENTRY_TYPE);
		strShipNode = orderEle.getAttribute(A_SHIP_NODE);
		if(YFCCommon.isVoid(strShipNode)){
			//take this from orderLine
			strShipNode = SCXmlUtil.getXpathAttribute(orderEle, XPATH_SHIP_NODE_AT_ORDER_LINE);
		}
		strOrderNo = orderEle.getAttribute(A_ORDER_NO);
		strEnterpriseCode = orderEle.getAttribute(A_ENTERPRISE_CODE);
		logger.verbose("Entry type of order: " + strEntryType);
		if(!YFCCommon.isVoid(strEntryType) && strEntryType.equalsIgnoreCase(A_ENTRY_TYPE_STORE)){
			logger.verbose("APTOS return order has came into OMS");

			//APTOS return lets form API input
			receiveOrderIndoc = SCXmlUtil.createDocument(E_RECEIPT);
			Element receiptEle = receiveOrderIndoc.getDocumentElement();
			receiptEle.setAttribute(A_RECEIVING_NODE, strShipNode);
			//Shipment
			Element shipmentEle = SCXmlUtil.createChild(receiptEle, E_SHIPMENT);
			shipmentEle.setAttribute(A_ORDER_NO, strOrderNo);
			shipmentEle.setAttribute(A_RECEIVING_NODE, strShipNode);
			shipmentEle.setAttribute(A_ENTERPRISE_CODE, strEnterpriseCode);


			Element receiptLinesEle = SCXmlUtil.createChild(receiptEle, E_RECEIPT_LINES);
			//Check No of order lines in Return order in OMS
			for(Element returnOrderLine : returnOrderLineList){
				Element returnItemDtls = SCXmlUtil.getChildElement(returnOrderLine, E_ITEM);
				Element receiptLineEle = SCXmlUtil.createChild(receiptLinesEle, E_RECEIPT_LINE);
				receiptLineEle.setAttribute(A_ORDER_NO, strOrderNo);
				receiptLineEle.setAttribute(A_PRIME_LINE_NO, returnOrderLine.getAttribute(A_PRIME_LINE_NO));
				receiptLineEle.setAttribute(A_SUB_LINE_NO, returnOrderLine.getAttribute(A_SUB_LINE_NO));
				receiptLineEle.setAttribute(A_ORDER_NO, strOrderNo);
				receiptLineEle.setAttribute(A_QUANTITY, returnOrderLine.getAttribute(A_ORDERED_QTY));
				receiptLineEle.setAttribute(A_ITEM_ID, returnItemDtls.getAttribute(A_ITEM_ID));

			}

			logger.verbose("Receive return input document is: "+SCXmlUtil.getString(receiveOrderIndoc));

			try {
				logger.verbose("Dropping this message to OMS Receive return Queue ");
				//Putting message in receive return oms queue
				Document outDocument = CommonUtil.invokeService(env, CROCS_PUT_MSG_TO_RECEIVE_RETURN_Q_SYNC_SERV, receiveOrderIndoc);

				logger.verbose("Receive Return message is successfully dropped in OMS inbound Q "
						+SCXmlUtil.getString(outDocument));
			} catch (RemoteException e) {

				logger.error("Error while putting message in to OMS Receive Return Queue" +e.getLocalizedMessage());
			}
		}
		logger.verbose("Output from CrocsAptosOrderPrepReceiveOrderInput:prepareReceiveReturnMsgInOMS is "
				+SCXmlUtil.getString(receiveOrderIndoc));

		return receiveOrderIndoc;

	}

}
