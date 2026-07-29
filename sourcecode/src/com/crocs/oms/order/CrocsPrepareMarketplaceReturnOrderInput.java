package com.crocs.oms.order;

import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

/**
 * EOMS-5404: Return Creation and Return Receipt closed
 * This class handles logic to update 
 *     ** OMS mandatory attributes on MarketPlace create order input before calling CreateOrder API in Sterling.
 *     ** Updates ShipNode
 *     ** Update ProcessOrderOnReturnOrder flag conditionally
 * 
 * @author IBM
 *
 */
public class CrocsPrepareMarketplaceReturnOrderInput implements CrocsConstant {
	
	private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsPrepareMarketplaceReturnOrderInput.class);
	
	/**
	 * This method get details of Sales Order
	 * and copy fields on Return order
	 * Conditionally update ProcessPaymentOnReturnOrder attribute
	 * 
	 * @param env
	 * @param inDoc
	 * @return
	 * @throws YFSException
	 */
	public Document crocsPrepareInputForCreateOrder(YFSEnvironment env, Document inDoc) throws YFSException {
		
		logger.beginTimer("CrocsPrepareMarketplaceReturnOrderInput.crocsPrepareInputForCreateOrder(): Begin" );
		logger.info("OMS_Update : CrocsPrepareMarketplaceReturnOrderInput : crocsPrepareInputForCreateOrder: Input for crocsPrepareInputForCreateOrder: Start :: " + XMLUtil.getXMLString(inDoc));
		
		String entryType = "";
		String strOrderType = "";
		Element returnOrderEle = inDoc.getDocumentElement();
		entryType = returnOrderEle.getAttribute(A_ENTRY_TYPE);
		strOrderType = returnOrderEle.getAttribute(A_ORDER_TYPE);
		logger.verbose("Return order having entry type: "+entryType+" Order Type: "+strOrderType);
		logger.info("Return order having entry type: "+entryType+" Order Type: "+strOrderType);

		//DeriveFromOrder Element
		Element deriveFromOrderEle = SCXmlUtil.getXpathElement(returnOrderEle, 
				XPAH_DERIVED_FROM);
		logger.info("Received a return order from the marketplace, the marketplace sales order CustCustPo:"+ deriveFromOrderEle.getAttribute(A_CUST_CUST_PO_NO));
		
		if(!YFCCommon.isVoid(strOrderType) && ORDER_TYPE_MP.equalsIgnoreCase(strOrderType)){

			logger.verbose("Adding details for MarketPlace return from Sales Order as OrderType="+ORDER_TYPE_MP);
			logger.info("Adding details for MarketPlace return from Sales Order as OrderType="+ORDER_TYPE_MP);

			//copying all required attributes on RO create order xml
			copyExtnAndOtherAttributesFromSO(env, returnOrderEle);
		}
		logger.info("OMS_Update : CrocsPrepareMarketplaceReturnOrderInput : crocsPrepareInputForCreateOrder : Final Output:"+SCXmlUtil.getString(inDoc));
		
		return inDoc;
	}
	
	/**
	 * 
	 * @param env
	 * @param returnOrderEle
	 */
	private void copyExtnAndOtherAttributesFromSO(YFSEnvironment env, Element returnOrderEle){
		


		logger.verbose("Starts of method copyExtnattributesFromSO with input: "+SCXmlUtil.getString(returnOrderEle));
		Document docGetOrderListOutput = null;
		
		try {
			
			//DeriveFromOrder Element
			Element deriveFromOrderEle = SCXmlUtil.getXpathElement(returnOrderEle, 
					XPAH_DERIVED_FROM);
			//Preparing getOrderList Input Document
			Document getOrderListIndoc = SCXmlUtil.createDocument(E_ORDER);
			getOrderListIndoc.getDocumentElement().setAttribute(A_CUST_CUST_PO_NO, deriveFromOrderEle.getAttribute(A_CUST_CUST_PO_NO));
			getOrderListIndoc.getDocumentElement().setAttribute(A_DOCUMENT_TYPE, deriveFromOrderEle.getAttribute(A_DOCUMENT_TYPE));

			logger.info("CrocsPrepareMarketplaceReturnOrderInput : copyExtnAndOtherAttributesFromSO GetOrderList input document: "+SCXmlUtil.getString(getOrderListIndoc));
			
			docGetOrderListOutput = CommonUtil.invokeService(env, CROCS_GET_ORDER_LIST_FOR_MARKETPLACE_SYNC_SERV, getOrderListIndoc);
			logger.verbose("CrocsPrepareMarketplaceReturnOrderInput : copyExtnAndOtherAttributesFromSO GetOrderList output document "+SCXmlUtil.getString(docGetOrderListOutput));
			
			
			if(docGetOrderListOutput!=null && Double
					.parseDouble(docGetOrderListOutput.getDocumentElement().getAttribute(A_TOTAL_ORDER_LIST)) > 0) {
				
				Element salesOrderEle = SCXmlUtil.getChildElement(docGetOrderListOutput.getDocumentElement(), E_ORDER);
				
				String strOrderNo= salesOrderEle.getAttribute(A_ORDER_NO);
				logger.info("Preparing the return order XML for this marketplace sales orderNo:"+salesOrderEle);
				
				// updating personInfoBillTo and PersonInfoShipTo to the MarketPlace Create Order XML
				Element elePersonInfoBillTo = SCXmlUtil.getXpathElement(salesOrderEle,
						CrocsConstant.STR_XPATH_ORDERLIST_PERSON_INFO_BILL_TO);
				Element elePersonInfoShipTo = SCXmlUtil.getXpathElement(salesOrderEle, STR_XPATH_ORDERLIST_PERSON_INFO_SHIP_TO);

				SCXmlUtil.importElement(returnOrderEle, elePersonInfoBillTo);
				SCXmlUtil.importElement(returnOrderEle, elePersonInfoShipTo);
				
				//Refund is processed at Sales order
				returnOrderEle.setAttribute(A_PROCESS_PAYMENTS_ON_RETURN_ORDER, FLAG_N);
				
				//updating Payment Rule Id
				String strPaymentRuleId = SCXmlUtil.getXpathAttribute(docGetOrderListOutput.getDocumentElement(),
						CrocsConstant.STR_XPATH_ORDERLIST_PAYMENT_RULE_ID);
				returnOrderEle.setAttribute(A_PAYMENT_RULE_ID, strPaymentRuleId);
				
				//updating enteredBy Field
				String strEnteredBy = SCXmlUtil.getXpathAttribute(docGetOrderListOutput.getDocumentElement(),
						CrocsConstant.STR_XPATH_ORDERLIST_ENTERED_BY);
				returnOrderEle.setAttribute(A_ENTERED_BY, strEnteredBy);
				
				//update ShipNode at Order Level
				String strEnterpriseCode = SCXmlUtil.getXpathAttribute(docGetOrderListOutput.getDocumentElement(),
						CrocsConstant.STR_XPATH_ORDERLIST_ENTERPRISE_CODE);
				returnOrderEle.setAttribute(A_ENTERPRISE_CODE, strEnterpriseCode);
				
				//EOMS-7033- START
				/**As Part of Jira, we fetch ShipNode and ReceivingNode form SO and map it to RO, 
				 * Removed the harcoded value. 
				 **/
				String shipNode=SCXmlUtil.getXpathAttribute(docGetOrderListOutput.getDocumentElement(), STR_XPATH_ORDERLIST_SHIP_NODE);
				
				//EOMS-8044 - START
				if (CrocsConstant.HEYDUDE_US.equalsIgnoreCase(strEnterpriseCode)) {
					/**
					 Purpose:- This Condition is to stamp the shipNode and take below decisions.
					 				if LVDCnode is active, LVDC ShipNode will be stamped..
					 				else Radial ShipNode will be stamped
					 				For HEYDUDE_US and HEYDUDE_US_MP, returns are expected to be redirected to LVDC when active.
					 			
					 			Another one for switch:-
					 			if LVDC is de-activatd, Return shipnode will always be radial, it will be accomplished just modifying the value in CommonCode.
					 
					 Operation:- If LVDCnode is active, fetch mapped ShipNode from
					              CommonCode configuration and route to LVDC.
					 **/
					
					logger.info("CrocsPrepareMarketplaceReturnOrderInput : copyExtnAndOtherAttributesFromSO : MarketPlaceReturn_GetShipNodeforReturn");
				    String redirectedShipNode = CommonUtil.getShipNodeforReturn(env, strEnterpriseCode);
				    shipNode = redirectedShipNode;
			    	
				}	
				//EOMS-8044 - END
				
				returnOrderEle.setAttribute(CrocsXmlConstants.A_SHIP_NODE, shipNode);
				returnOrderEle.setAttribute(CrocsXmlConstants.A_RECEIVING_NODE, shipNode);
				//EOMS-7033- END

				
				//update customer Email ID
				String strCustomerEmailId = SCXmlUtil.getXpathAttribute(docGetOrderListOutput.getDocumentElement(), CrocsConstant.STR_XPATH_ORDERLIST_CUSTOMER_EMAILID);
				returnOrderEle.setAttribute(CrocsXmlConstants.A_CUSTOMER_EMAIL_ID, strCustomerEmailId);
				
				//update customer phoneNo
				String strCustomerPhoneNo = SCXmlUtil.getXpathAttribute(docGetOrderListOutput.getDocumentElement(), CrocsConstant.STR_XPATH_ORDERLIST_CUSTOMER_PHNO);
				returnOrderEle.setAttribute(CrocsXmlConstants.A_CUSTOMER_PHONE_NO, strCustomerPhoneNo);
				
				Element salesOrderLinesEle = SCXmlUtil.getChildElement(salesOrderEle, E_ORDER_LINES);
				
				/**
				 * Copying OrderLine/Extn from Sales to Return Order
				 */
				Element returnOrderLines = SCXmlUtil.getChildElement(returnOrderEle, E_ORDER_LINES);
				ArrayList<Element> returnOrderLineList = SCXmlUtil.getChildren(returnOrderLines, E_ORDER_LINE);
				for (Element returnOrderLine : returnOrderLineList) {
					//Updating ShipNode
					returnOrderLine.setAttribute(CrocsXmlConstants.A_RECEIVING_NODE, shipNode);
					Element derivedFromEle = SCXmlUtil.getChildElement(returnOrderLine, E_DERIVED_FROM);
					Element returnItemEle = SCXmlUtil.getChildElement(returnOrderLine, E_ITEM);
					String itemID = returnItemEle.getAttribute(A_ITEM_ID);
					Element orderLineSOEle = SCXmlUtil.getXpathElement(salesOrderLinesEle,
							"/OrderList/Order/OrderLines/OrderLine[Item/@ItemID='"+itemID+"']");
					logger.verbose("SO line is "+SCXmlUtil.getString(orderLineSOEle));

					//Copying PrimeLineNo & SubLineNo From SO order Line and appending
					//at RO Order/OrderLines/OrderLine/DerivedFromOrder Element
					derivedFromEle.setAttribute(A_PRIME_LINE_NO, orderLineSOEle.getAttribute(A_PRIME_LINE_NO));
					derivedFromEle.setAttribute(A_SUB_LINE_NO, orderLineSOEle.getAttribute(A_SUB_LINE_NO));
					derivedFromEle.setAttribute(A_ENTERPRISE_CODE, strEnterpriseCode);
					derivedFromEle.setAttribute(A_ORDER_NO, strOrderNo);

				}
					
			}			
			
		}catch(Exception e) {
			logger.verbose(
					"CrocsPrepareMarketplaceReturnOrderInput.copyExtnAndOtherAttributesFromSO :Expection" + e.getMessage());
			throw new YFSException(
					"CrocsPrepareMarketplaceReturnOrderInput.copyExtnAndOtherAttributesFromSO :Expection" + e.getMessage());
		}
		
	}
}
