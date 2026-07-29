package com.crocs.oms.order.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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
 * EOMS-7161: Return Creation and Return Receipt closed This class handles logic
 * to update OMS mandatory attributes on GLOBALE create order input before
 * calling CreateOrder API in Sterling. Update ShipNode Update
 * ProcessOrderOnReturnOrder flag conditionally
 * 
 * <Order Action="CREATE" DocumentType="0003" EntryType="GLOBALE" EnteredBy=
 * "GLOBALE" SCAC="DHL" CustomerPONo="10800503H01"> 
 * <PriceInfo Currency="AUD"/>
 * <OrderLines>
 * <OrderLine OrderedQty="1" PrimeLineNo="1" SubLineNo="1" ReturnReason="Arrived
 * too late"> 
 * <CustomAttributes Text1="857854854778" Text2=
 * "www.dhl.com/Tracking?TrackingNumber=857854854778"/>
 * <Item ItemID="10001-001-M4W6" Desc="Blue jacket"/> 
 * <DerivedFrom OrderNo="10800503HAU" DocumentType="0001" SubLineNo="1"/>
 * </OrderLine>
 * </OrderLines> 
 * </Order>
 * 
 * @author IBM
 *
 */

public class CrocsPrepareReturnOrderInputUtil implements CrocsConstant {
	
	private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsPrepareReturnOrderInputUtil.class);
	
	static Map<String, Integer> returnOrder = new HashMap<>();
	static Map<String, Integer> salesOrder = new HashMap<>();
	
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
		
		logger.beginTimer("CrocsPrepareReturnOrderInputUtil.crocsPrepareInputForCreateOrder(): Begin" );
		logger.info("OMS_Update : CrocsPrepareReturnOrderInputUtil : Input for crocsPrepareInputForCreateOrder: Start :: " + XMLUtil.getXMLString(inDoc));
		
		Document docGetOrderListOutput = null;

		try {
			
			Element returnOrderEle = inDoc.getDocumentElement();
			
			//DeriveFromOrder Element
			Element deriveFromOrderEle = SCXmlUtil.getXpathElement(returnOrderEle, XPAH_DERIVED_FROM);
			
			//Preparing getOrderList Input Document
			Document getOrderListIndoc = SCXmlUtil.createDocument(E_ORDER);
			getOrderListIndoc.getDocumentElement().setAttribute(A_ORDER_NO, deriveFromOrderEle.getAttribute(A_ORDER_NO));
			getOrderListIndoc.getDocumentElement().setAttribute(A_DOCUMENT_TYPE, deriveFromOrderEle.getAttribute(A_DOCUMENT_TYPE));

			logger.verbose("Input of getOrderList API: "+SCXmlUtil.getString(getOrderListIndoc));
			
			docGetOrderListOutput = CommonUtil.invokeService(env, A_HEYDUDE_GET_ORDER_LIST_FOR_GLOBALE, getOrderListIndoc);
			logger.verbose("GetOrderList output document is "+SCXmlUtil.getString(docGetOrderListOutput));
			
			
			if(docGetOrderListOutput!=null && Double
					.parseDouble(docGetOrderListOutput.getDocumentElement().getAttribute(A_TOTAL_ORDER_LIST)) > 0) {
				
				Element salesOrderEle = SCXmlUtil.getChildElement(docGetOrderListOutput.getDocumentElement(), E_ORDER);
				
				//ShipNode
				String strShipNode = SCXmlUtil.getXpathAttribute(docGetOrderListOutput.getDocumentElement(),
						CrocsConstant.STR_XPATH_ORDERLIST_SHIP_NODE);
				if(YFCCommon.isVoid(strShipNode)){
					strShipNode = CrocsConstant.A_RADIAL_SHIPNODE;
				}
				
				// updating personInfoBillTo and PersonInfoShipTo to the MarketPlace Create Order XML
				Element elePersonInfoBillTo = SCXmlUtil.getXpathElement(salesOrderEle,
						CrocsConstant.STR_XPATH_ORDERLIST_PERSON_INFO_BILL_TO);
				Element elePersonInfoShipTo = SCXmlUtil.getXpathElement(salesOrderEle, STR_XPATH_ORDERLIST_PERSON_INFO_SHIP_TO);

				SCXmlUtil.importElement(returnOrderEle, elePersonInfoBillTo);
				SCXmlUtil.importElement(returnOrderEle, elePersonInfoShipTo);
				
				//updating Override Flag
				returnOrderEle.setAttribute(A_OVERRIDE, VAL_FLAG_Y);
				
				//Refund is processed at Sales order
				returnOrderEle.setAttribute(A_PROCESS_PAYMENTS_ON_RETURN_ORDER, FLAG_N);
				
				//updating Payment Rule Id
				String strPaymentRuleId = SCXmlUtil.getXpathAttribute(docGetOrderListOutput.getDocumentElement(),
						CrocsConstant.STR_XPATH_ORDERLIST_PAYMENT_RULE_ID);
				returnOrderEle.setAttribute(A_PAYMENT_RULE_ID, strPaymentRuleId);
				
				//update ShipNode at Order Level
				String strEnterpriseCode = SCXmlUtil.getXpathAttribute(docGetOrderListOutput.getDocumentElement(),
						CrocsConstant.STR_XPATH_ORDERLIST_ENTERPRISE_CODE);
				
				returnOrderEle.setAttribute(A_ENTERPRISE_CODE, strEnterpriseCode);
				returnOrderEle.setAttribute(CrocsXmlConstants.A_SHIP_NODE, strShipNode);
				returnOrderEle.setAttribute(CrocsXmlConstants.A_RECEIVING_NODE, strShipNode);

				
				//update customer Email ID
				String strCustomerEmailId = SCXmlUtil.getXpathAttribute(docGetOrderListOutput.getDocumentElement(), CrocsConstant.STR_XPATH_ORDERLIST_CUSTOMER_EMAILID);
				returnOrderEle.setAttribute(CrocsXmlConstants.A_CUSTOMER_EMAIL_ID, strCustomerEmailId);
				
				//update customer phoneNo
				String strCustomerPhoneNo = SCXmlUtil.getXpathAttribute(docGetOrderListOutput.getDocumentElement(), CrocsConstant.STR_XPATH_ORDERLIST_CUSTOMER_PHNO);
				returnOrderEle.setAttribute(CrocsXmlConstants.A_CUSTOMER_PHONE_NO, strCustomerPhoneNo);
				
				//Order Line Level Changes
				appendLineChargesOnEachOrderLine(inDoc,docGetOrderListOutput,strEnterpriseCode,strShipNode);
				
				// Order Header Level Changes
				addSaleOrderLinesIntoSalesMap(docGetOrderListOutput);
				
				//checking sales order is fully returned or not.
				String appendHeaderCharges = isFullyReturned(salesOrder, returnOrder);
				
				//Appending Header Charges and Header taxes on Last return of the Order.
				if (appendHeaderCharges.equalsIgnoreCase(CrocsConstant.VAL_FLAG_Y))
					appendHeaderChargesOnLastReturn(inDoc,docGetOrderListOutput);
			}
			
		}catch(Exception e) {
			logger.verbose(
					"CrocsPrepareReturnOrderInputUtil.crocsPrepareInputForCreateOrder :Expection" + e.getMessage());
			throw new YFSException(
					"CrocsPrepareReturnOrderInputUtil.crocsPrepareInputForCreateOrder :Expection" + e.getMessage());
		}
		
		logger.info("OMS_Update : CrocsPrepareReturnOrderInputUtil : crocsPrepareInputForCreateOrder : Final Output:"+SCXmlUtil.getString(inDoc));
		
		return inDoc;
	}
	
	/**
	 * Description: Appending Line Charges to each Order line of Return Order
	 * 
	 * @param inDoc
	 * @param docGetOrderList
	 * @param strEnterpriseCode
	 */
	public static void appendLineChargesOnEachOrderLine(Document inDoc, Document docGetOrderList,String strEnterpriseCode,String strShipNode) {
		
		logger.verbose("CrocsPrepareReturnOrderInputUtil : appendLineChargesOnEachOrderLine: START");
		
		try {
			Element eleOrder = inDoc.getDocumentElement();
			Element orderLinesEle = SCXmlUtil.getChildElement(eleOrder, CrocsXmlConstants.E_ORDER_LINES);
			NodeList odrerLineList = orderLinesEle.getElementsByTagName(CrocsXmlConstants.E_ORDER_LINE);
			
			for (int i = 0; i < odrerLineList.getLength(); i++) {
				
				Element eleOrderLine = (Element) odrerLineList.item(i);
				double intOrderedQty = Double.parseDouble(eleOrderLine.getAttribute(CrocsXmlConstants.A_ORDERED_QTY));

				Element eleDerivedFromOrder = SCXmlUtil.getChildElement(eleOrderLine, CrocsXmlConstants.E_DERIVED_FROM);
				Element eleLinePriceInfo = SCXmlUtil.getChildElement(eleOrderLine, CrocsXmlConstants.E_LINE_PRICE_INFO);
				if(YFCCommon.isVoid(eleLinePriceInfo)) {
					eleLinePriceInfo = SCXmlUtil.createChild(eleOrderLine, CrocsXmlConstants.E_LINE_PRICE_INFO);
				}
				String strItemID = SCXmlUtil.getXpathAttribute(eleOrderLine, CrocsConstant.STR_XPATH_ITEM_ID);
				Element eleOrderLineCustomAttributes = SCXmlUtil.getChildElement(eleOrderLine, CrocsXmlConstants.E_CUSTOM_ATTRIBUTES);
				eleOrderLineCustomAttributes.getAttribute(CrocsConstant.STR_TEXT_1).replace(CrocsConstant.STR_AND,
						CrocsConstant.STR_AMP);
				eleOrderLineCustomAttributes.getAttribute(CrocsConstant.STR_TEXT_2).replace(CrocsConstant.STR_AND,
						CrocsConstant.STR_AMP);
				
				Element eleOrderLineExtn =  inDoc.createElement(CrocsXmlConstants.E_EXTN);
				eleOrderLineExtn.setAttribute(CrocsXmlConstants.EXTN_TRACKING_NO, eleOrderLineCustomAttributes.getAttribute(CrocsConstant.STR_TEXT_1));
				eleOrderLineExtn.setAttribute(CrocsXmlConstants.A_EXTN_TRACKING_URL, eleOrderLineCustomAttributes.getAttribute(CrocsConstant.STR_TEXT_2));
				eleOrderLineExtn.setAttribute(CrocsXmlConstants.EXTN_SHIP_CARRIER, eleOrder.getAttribute(CrocsXmlConstants.A_SCAC));
				
				
				int intValue = (int) intOrderedQty; 
				returnOrder.put(strItemID, intValue);
				
				Element eleSalesOrderLine = XMLUtil.getElementByXPath(docGetOrderList,
						"/OrderList/Order/OrderLines/OrderLine[Item/@ItemID='" + strItemID + "']");
				
				Element eleSalesLinePriceInfo = SCXmlUtil.getChildElement(eleSalesOrderLine, CrocsXmlConstants.E_LINE_PRICE_INFO);
				eleLinePriceInfo.setAttribute(CrocsXmlConstants.A_UNIT_PRICE, eleSalesLinePriceInfo.getAttribute(CrocsXmlConstants.A_UNIT_PRICE));
				
				String strPrimeLineNo = eleSalesOrderLine.getAttribute(CrocsXmlConstants.A_PRIME_LINE_NO);
				eleDerivedFromOrder.setAttribute(CrocsXmlConstants.A_PRIME_LINE_NO, strPrimeLineNo);
				eleDerivedFromOrder.setAttribute(CrocsXmlConstants.A_ENTERPRISE_CODE, strEnterpriseCode);
				
				//Updating ShipNode
				eleOrderLine.setAttribute(CrocsXmlConstants.A_RECEIVING_NODE, strShipNode);
				
				double intSalesOrderedQty = Double
						.parseDouble(eleSalesOrderLine.getAttribute(CrocsXmlConstants.A_ORDERED_QTY));

				double intSalesReturnablQty = Double.parseDouble(eleSalesOrderLine.getAttribute(CrocsXmlConstants.A_RETURNABLE_QTY));

				// Line Charges
				double dChargeAmount;
				Element eleorderLineCharges = SCXmlUtil.getChildElement(eleSalesOrderLine,
						CrocsXmlConstants.E_LINE_CHARGES);
				ArrayList<Element> lineCharges = SCXmlUtil.getChildren(eleorderLineCharges,
						CrocsXmlConstants.E_LINE_CHARGE);
				Element eleOrderLineCharges = inDoc.createElement(CrocsXmlConstants.E_LINE_CHARGES);
				for (Element linecharge : lineCharges) {
					Element eleLineCharge = inDoc.createElement(CrocsXmlConstants.E_LINE_CHARGE);
					eleLineCharge.setAttribute(CrocsXmlConstants.A_CHARGE_CATEGORY,
							linecharge.getAttribute(CrocsXmlConstants.A_CHARGE_CATEGORY));
					eleLineCharge.setAttribute(CrocsXmlConstants.A_CHARGE_NAME,
							linecharge.getAttribute(CrocsXmlConstants.A_CHARGE_NAME));
					double intChargeAmount = Double
							.parseDouble(linecharge.getAttribute(CrocsXmlConstants.A_CHARGE_PER_LINE));
					
					dChargeAmount = prorateLineChargesAndLineTaxes(intChargeAmount, intSalesReturnablQty, intSalesOrderedQty,intOrderedQty);
					
					eleLineCharge.setAttribute(CrocsXmlConstants.A_CHARGE_PER_LINE, String.format("%.2f", dChargeAmount));
					eleOrderLineCharges.appendChild(eleLineCharge);
				}
				
				eleOrderLine.appendChild(eleOrderLineCharges);
				eleOrderLine.appendChild(eleOrderLineExtn);
			}
		} catch (Exception e) {
			throw new YFSException(
					"CrocsPrepareReturnOrderInputUtil.appendLineChargesOnEachOrderLine :Expection" + e.getMessage());
		}
		logger.verbose("CrocsPrepareReturnOrderInputUtil : appendLineChargesOnEachOrderLine: END");
	}
	/**
	 * 
	 * 
	 * @param intChargeAmount
	 * @param intSalesReturnablQty
	 * @param intSalesOrderedQty
	 * @param intOrderedQty
	 * @return
	 */
	public static double prorateLineChargesAndLineTaxes(double intChargeAmount,double intSalesReturnablQty, double intSalesOrderedQty,double intOrderedQty) {
		
		logger.verbose("CrocsPrepareReturnOrderInputUtil : prorateLineChargesAndLineTaxes: START");
		double dChargeAmount;
		try {
			if (intSalesReturnablQty == intOrderedQty) {
				dChargeAmount = intChargeAmount / intSalesOrderedQty;
				dChargeAmount = Math.round(dChargeAmount * 100.0) / 100.0;
				dChargeAmount = dChargeAmount * (intSalesOrderedQty - intSalesReturnablQty);
				dChargeAmount = intChargeAmount - dChargeAmount;  
			} else {
				dChargeAmount = intChargeAmount / intSalesOrderedQty;
				dChargeAmount = Math.round(dChargeAmount * 100.0) / 100.0;
				dChargeAmount = dChargeAmount * intOrderedQty;
			}
		}catch(Exception e) {
			throw new YFSException("CrocsPrepareReturnOrderInputUtil.prorateLineChargesAndLineTaxes :Expection" + e.getMessage());
		}
		logger.verbose("CrocsPrepareReturnOrderInputUtil : prorateLineChargesAndLineTaxes: END");
		return dChargeAmount;
    }
	
	/**
	 * Description: Below method helps the adding Sales Order ItemId and 
	 * Returnable Qty's of each order line into Hash Map
	 * 
	 * @param docGetOrderListOutput
	 */
	public static void addSaleOrderLinesIntoSalesMap(Document docGetOrderListOutput) {
		
		logger.verbose("CrocsPrepareReturnOrderInputUtil : addSaleOrderLinesIntoSalesMap: START");
		try {
		Element eleOrderList = docGetOrderListOutput.getDocumentElement();
		Element eleorder = SCXmlUtil.getChildElement(eleOrderList, CrocsXmlConstants.E_ORDER);
		Element eleSalesorderLines = SCXmlUtil.getChildElement(eleorder, CrocsXmlConstants.E_ORDER_LINES);
		NodeList eleSalesorderLine = eleSalesorderLines.getElementsByTagName(CrocsXmlConstants.E_ORDER_LINE);
		for(int i=0;i<eleSalesorderLine.getLength();i++) {
			Element eleOrderLine = (Element) eleSalesorderLine.item(i);
			String strItemID = SCXmlUtil.getXpathAttribute(eleOrderLine, CrocsConstant.STR_XPATH_ITEM_ID);
			double intOrderedQty = Double.parseDouble(eleOrderLine.getAttribute(CrocsXmlConstants.A_RETURNABLE_QTY));
			int intValue = (int) intOrderedQty; 
			salesOrder.put(strItemID, intValue);
		}
		}catch(Exception e) {
			throw new YFSException("CrocsPrepareReturnOrderInputUtil.addSaleOrderLinesIntoSalesMap :Expection" + e.getMessage());
		}
		logger.verbose("CrocsPrepareReturnOrderInputUtil : addSaleOrderLinesIntoSalesMap: END");
    }
	
	/**
	 * Description: This method compares the sale order's returnable quantity with the return order's quantity, 
	 * and returns 'Y' if both values are the same, indicating the order is fully returned.
	 * 
	 * @param salesOrder
	 * @param returnOrder
	 * @return
	 */
	private static String isFullyReturned(Map<String, Integer> salesOrder, Map<String, Integer> returnOrder) {
		
		logger.verbose("CrocsPrepareReturnOrderInputUtil : isFullyReturned: START");
		String strValue=CrocsConstant.VAL_FLAG_Y;
        for (Map.Entry<String, Integer> entry : salesOrder.entrySet()) {
            String itemId = entry.getKey();
            int returnableQty = entry.getValue();
            int returnedQty = returnOrder.getOrDefault(itemId, 0);
            if (returnedQty < returnableQty) {
            	strValue=CrocsConstant.FLAG_N;
                return strValue; // Not fully returned if any item has remaining returnable quantity
            }
        }
        logger.verbose("CrocsPrepareReturnOrderInputUtil : isFullyReturned: END");
        return strValue; // Fully returned if all items have been returned in full
    }
	
	/**
	 * Description: Appending Header Charges to Last Return of Sales Order.
	 * @param env 
	 * 
	 * @param inDoc
	 * @param docGetOrderListOutput
	 * @param strEnterpriseCode 
	 */
	public static void appendHeaderChargesOnLastReturn(Document inDoc, Document docGetOrderListOutput) {
		logger.verbose("CrocsPrepareReturnOrderInputUtil : appendHeaderChargesAndTaxesOnLastReturn: START");
		try {
			// Header Charges for US & CA
			Element eleOrder = inDoc.getDocumentElement();
			Element eleOrderList = docGetOrderListOutput.getDocumentElement();
			Element eleorder = SCXmlUtil.getChildElement(eleOrderList, CrocsXmlConstants.E_ORDER);
			Element orderHeaderCharges = SCXmlUtil.getChildElement(eleorder, CrocsXmlConstants.E_HEADER_CHARGES);
			ArrayList<Element> arrHeaderCharges = SCXmlUtil.getChildren(orderHeaderCharges,
					CrocsXmlConstants.E_HEADER_CHARGE);
			Element eleOrderHeaderCharges = inDoc.createElement(CrocsXmlConstants.E_HEADER_CHARGES);
			for (Element Headercharge : arrHeaderCharges) {
				Element chargeCategoryDetails = SCXmlUtil.getChildElement(Headercharge, CrocsXmlConstants.E_CHARGE_CATEGORY_DETAILS);
				String isRefundable=chargeCategoryDetails.getAttribute(CrocsXmlConstants.A_IS_REFUNDABLE);
				if(CrocsXmlConstants.FLAG_Y.equals(isRefundable))
				{
				Element eleOrderHeaderCharge = inDoc.createElement(CrocsXmlConstants.E_HEADER_CHARGE);
				eleOrderHeaderCharge.setAttribute(CrocsXmlConstants.A_CHARGE_CATEGORY,
						Headercharge.getAttribute(CrocsXmlConstants.A_CHARGE_CATEGORY));
				eleOrderHeaderCharge.setAttribute(CrocsXmlConstants.A_CHARGE_NAME,
						Headercharge.getAttribute(CrocsXmlConstants.A_CHARGE_NAME));
				eleOrderHeaderCharge.setAttribute(CrocsXmlConstants.A_CHARGE_AMOUNT,
						Headercharge.getAttribute(CrocsXmlConstants.A_CHARGE_AMOUNT));
				eleOrderHeaderCharges.appendChild(eleOrderHeaderCharge);
				}
			}
			eleOrder.appendChild(eleOrderHeaderCharges);
	
		} catch (Exception e) {
			throw new YFSException("CrocsPrepareReturnOrderInputUtil.appendHeaderChargesOnLastReturn :Expection" + e.getMessage());
		}
		logger.verbose("CrocsPrepareReturnOrderInputUtil : appendHeaderChargesOnLastReturn: END");
	}
}
