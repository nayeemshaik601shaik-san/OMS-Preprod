package com.crocs.oms.order;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

/**
 * EOMS-1492 : Narvar Create order Implementation
 * 
 */
public class CrocsCreateOrderDetailsForNarvar {
	
	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsCreateOrderDetailsForNarvar.class);

	static Map<String, Integer> returnOrder = new HashMap<>();
	static Map<String, Integer> salesOrder = new HashMap<>();
	
	public Document crocsPrepareInputForCreateOrder(YFSEnvironment env, Document inDoc) throws YFSException {

		logger.beginTimer("CrocsCreateOrderDetailsForNarvar.crocsPrepareInputForCreateOrder(): Begin");
		logger.verbose("Input for crocsPrepareInputForCreateOrder: Start :: " + XMLUtil.getXMLString(inDoc));

		String strSalesOrderNo = null;
		Document docGetOrderListOutput = null;
		try {

			Element eleOrder = inDoc.getDocumentElement();
			Element orderLinesEle = SCXmlUtil.getChildElement(eleOrder, CrocsXmlConstants.E_ORDER_LINES);
			NodeList odrerLineList = orderLinesEle.getElementsByTagName(CrocsXmlConstants.E_ORDER_LINE);
			
			if (odrerLineList.getLength() > 0) {
				Element eleOrderLine = (Element) odrerLineList.item(0);
				strSalesOrderNo = SCXmlUtil.getXpathAttribute(eleOrderLine, CrocsConstant.STR_XPATH_DERIVED_ORDER_NO);
			}

			// Calling getOrderList
			docGetOrderListOutput = getOrderListForSaleOrderNo(env, strSalesOrderNo);

			if(docGetOrderListOutput!=null) {
				
				// updating personInfoBillTo to the Narvar Create Order XML
				Element elePersonInfoBillTo = SCXmlUtil.getXpathElement(docGetOrderListOutput.getDocumentElement(),
						CrocsConstant.STR_XPATH_ORDERLIST_PERSON_INFO_BILL_TO);
				Element elePersonInfoImport = (Element) inDoc.importNode(elePersonInfoBillTo, CrocsConstant.A_TRUE);
				inDoc.getDocumentElement().appendChild(elePersonInfoImport);
				
				//EOMS-9159 : Updating PersonShipTo to Narvar XML : START
				Element elePersonInfoShipTo = SCXmlUtil.getXpathElement(docGetOrderListOutput.getDocumentElement(),
						CrocsConstant.STR_XPATH_ORDERLIST_PERSON_INFO_SHIP_TO);
				Element elePersonInfoShipToImport = (Element) inDoc.importNode(elePersonInfoShipTo, CrocsConstant.A_TRUE);
				inDoc.getDocumentElement().appendChild(elePersonInfoShipToImport);
				//EOMS-9159 : Updating PersonShipTo to Narvar XML : END
				
				// update EnterpriseCode at Order level
				String strEnterpriseCode = SCXmlUtil.getXpathAttribute(docGetOrderListOutput.getDocumentElement(),
						CrocsConstant.STR_XPATH_ORDERLIST_ENTERPRISE_CODE);
				eleOrder.setAttribute(CrocsXmlConstants.A_ENTERPRISE_CODE, strEnterpriseCode);
				
				//update ShipNode at Order Level
				// EOMS-7159 Changes Start: Added Ship Node for HEYDUDE_US
				if(strEnterpriseCode.equalsIgnoreCase(CrocsConstant.CROCS_US)) {
					eleOrder.setAttribute(CrocsXmlConstants.A_SHIP_NODE, CrocsConstant.STR_US_SHIP_NODE);
				}else if(strEnterpriseCode.equalsIgnoreCase(CrocsConstant.CROCS_CA)) {
					eleOrder.setAttribute(CrocsXmlConstants.A_SHIP_NODE, CrocsConstant.STR_CA_SHIP_NODE);
                    //EOMS-8754 CROCS_AU
                }else if(strEnterpriseCode.equalsIgnoreCase(CrocsConstant.CROCS_AU)) {
                    eleOrder.setAttribute(CrocsXmlConstants.A_SHIP_NODE, CrocsConstant.STR_CAU_SHIP_NODE);
				}else if(strEnterpriseCode.equalsIgnoreCase(CrocsConstant.HEYDUDE_US)) {
					
					//EOMS-8044
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
					
					logger.info("CrocsCreateOrderDetailsForNarvar : crocsPrepareInputForCreateOrder : NarvarReturn_GetShipNodeforReturn");
					
				    String shipNode = CommonUtil.getShipNodeforReturn(env, strEnterpriseCode);
				    eleOrder.setAttribute(CrocsXmlConstants.A_SHIP_NODE, shipNode);
					//EOMS-8044-END
				 
				}

				/** 
				 * EOMS-10803: Stamp ShipNode as LVDC for HEYDUDE_CA orders, 
				 * since Narvar return is not available for GLOBALE.
				 */
				else if(strEnterpriseCode.equalsIgnoreCase(CrocsConstant.HEYDUDE_CA)) {
                    eleOrder.setAttribute(CrocsXmlConstants.A_SHIP_NODE, CrocsConstant.HDCA_SHIP_NODE);
				}
				//EOMS-12135 Start
				else if(CrocsConstant.CROCS_EMEA_ENTERPRISES.contains(strEnterpriseCode)){
					eleOrder.setAttribute(CrocsXmlConstants.A_SHIP_NODE, CrocsConstant.STR_EMEA_SHIP_NODE);
				}
				//EOMS-12135 End
				// EOMS-11246 - Start 	
				/** 
				 * Stamp ShipNode as 3001 for CROCS_SG orders
				 */	
				else if(strEnterpriseCode.equalsIgnoreCase(CrocsConstant.CROCS_SG)) {
					eleOrder.setAttribute(CrocsXmlConstants.A_SHIP_NODE, CrocsConstant.SG_RO_FULFILLMENT_NODE);
				}
				// EOMS-11246 - End
				// EOMS-12634 - CROCS_KR-Create Return from Narvar[stamp ship node as 3101 for CROCS_KR Orders] START 	
				else if(strEnterpriseCode.equalsIgnoreCase(CrocsConstant.CROCS_KR)) {
					eleOrder.setAttribute(CrocsXmlConstants.A_SHIP_NODE, CrocsConstant.KR_RO_FULFILLMENT_NODE);
				}
				// EOMS-12634 - End

				// EOMS-7159 Changes Start
				
				//update customer phoneNo
				String strCustomerPhoneNo = SCXmlUtil.getXpathAttribute(docGetOrderListOutput.getDocumentElement(), CrocsConstant.STR_XPATH_ORDERLIST_CUSTOMER_PHNO);
				eleOrder.setAttribute(CrocsXmlConstants.A_CUSTOMER_PHONE_NO, strCustomerPhoneNo);
				
				//Order Line Level Changes
				appendLineChargesAndTaxesOnEachOrderLine(inDoc,docGetOrderListOutput,strEnterpriseCode);
				
				// Order Header Level Changes
				addSaleOrderLinesIntoSalesMap(docGetOrderListOutput);
				
				//checking sales order is fully returned or not.
				String appendHeaderChargesAndTaxes = isFullyReturned(salesOrder, returnOrder);
				
				//Appending Header Charges and Header taxes on Last return of the Order.
				if (appendHeaderChargesAndTaxes.equalsIgnoreCase(CrocsConstant.VAL_FLAG_Y))
					appendHeaderChargesAndTaxesOnLastReturn(env,inDoc,docGetOrderListOutput,strEnterpriseCode);
				
				//updating note Text to the Order
				appendLableURLOnNoteTex(inDoc);
				
			}

		} catch (Exception e) {
			logger.verbose(
					"CrocsCreateOrderDetailsForNarvar.crocsPrepareInputForCreateOrder :Expection" + e.getMessage());
			throw new YFSException(
					"CrocsCreateOrderDetailsForNarvar.crocsPrepareInputForCreateOrder :Expection" + e.getMessage());
		}
		logger.verbose("CrocsCreateOrderDetailsForNarvar.crocsPrepareInputForCreateOrder:: END ::"+ XMLUtil.getXMLString(inDoc));
		return inDoc;
	}

	/**
	 * Description: Calling GetOrderList with Sales Order No
	 *
	 * @param env
	 * @param strOrderNo
	 * @return
	 * @throws Exception
	 */
	public static Document getOrderListForSaleOrderNo(YFSEnvironment env, String strOrderNo) throws YFSException {
		
		logger.verbose("CrocsCreateOrderDetailsForNarvar : getOrderListForSaleOrderNo: START");

		Document getOrderListOut = null;
		try {
			if (!YFCObject.isVoid(strOrderNo)) {

				Document getOrderListInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER);
				getOrderListInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_ORDER_NO, strOrderNo);
				getOrderListOut = CommonUtil.invokeService(env, CrocsConstant.STR_CROCS_GET_ORDERLIST_NARVAR,
						getOrderListInDoc);
			}
		} catch (Exception e) {
			throw new YFSException("CrocsCreateOrderDetailsForNarvar.getOrderListForSaleOrderNo :Expection" + e.getMessage());
		}
		logger.verbose("CrocsCreateOrderDetailsForNarvar : getOrderListForSaleOrderNo:: END:: " + XMLUtil.getXMLString(getOrderListOut));
		return getOrderListOut;
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
		
		logger.verbose("CrocsCreateOrderDetailsForNarvar : isFullyReturned: START");
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
        logger.verbose("CrocsCreateOrderDetailsForNarvar : isFullyReturned: END");
        return strValue; // Fully returned if all items have been returned in full
    }
	
	/**
	 * Description: Below method helps the adding Sales Order ItemId and 
	 * Returnable Qty's of each order line into Hash Map
	 * 
	 * @param docGetOrderListOutput
	 */
	public static void addSaleOrderLinesIntoSalesMap(Document docGetOrderListOutput) {
		
		logger.verbose("CrocsCreateOrderDetailsForNarvar : addSaleOrderLinesIntoSalesMap: START");
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
			throw new YFSException("CrocsCreateOrderDetailsForNarvar.addSaleOrderLinesIntoSalesMap :Expection" + e.getMessage());
		}
		logger.verbose("CrocsCreateOrderDetailsForNarvar : addSaleOrderLinesIntoSalesMap: END");
    }
	/**
	 * Description: Appending Header Charges and Header Taxes to Last Return of Sales Order.
	 * @param env 
	 * 
	 * @param inDoc
	 * @param docGetOrderListOutput
	 * @param strEnterpriseCode 
	 */
	public static void appendHeaderChargesAndTaxesOnLastReturn(YFSEnvironment env, Document inDoc, Document docGetOrderListOutput, String strEnterpriseCode) {
		logger.verbose("CrocsCreateOrderDetailsForNarvar : appendHeaderChargesAndTaxesOnLastReturn: START");
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
				//EOMS-4374 -START : do not refund shipping related charges
				Element chargeCategoryDetails = SCXmlUtil.getChildElement(Headercharge, CrocsXmlConstants.E_CHARGE_CATEGORY_DETAILS);
				//checking Charge Category is eligible for refundable or not
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

			// Header Taxes for US & CA
			Element orderHeaderTaxes = SCXmlUtil.getChildElement(eleorder, CrocsXmlConstants.E_HEADER_TAXES);
			ArrayList<Element> arrHeaderTaxes = SCXmlUtil.getChildren(orderHeaderTaxes, CrocsXmlConstants.E_HEADER_TAX);
			Element eleOrderHeaderTaxes = inDoc.createElement(CrocsXmlConstants.E_HEADER_TAXES);
			//EOMS-4374 : do not refund shipping related taxes
			String documentType=eleorder.getAttribute(CrocsXmlConstants.A_DOCUMENT_TYPE);
			logger.verbose("Before header taxes added to Narvar retun order create input:"+SCXmlUtil.getString(inDoc));
			
			if(!arrHeaderTaxes.isEmpty())
			{
				//Listing Refundable charge categories
				List<String> refundableChargeCategories=getChargeCategoryList(env,documentType ,strEnterpriseCode,CrocsXmlConstants.FLAG_Y);

				for (Element Headertax : arrHeaderTaxes) {
					//checking Charge Category is eligible for refundable or not
					if(refundableChargeCategories.contains(Headertax.getAttribute(CrocsXmlConstants.A_CHARGE_CATEGORY)))
					{
					Element eleOrderHeaderTax = inDoc.createElement(CrocsXmlConstants.E_HEADER_TAX);
					eleOrderHeaderTax.setAttribute(CrocsXmlConstants.A_CHARGE_CATEGORY,
							Headertax.getAttribute(CrocsXmlConstants.A_CHARGE_CATEGORY));
					eleOrderHeaderTax.setAttribute(CrocsXmlConstants.A_CHARGE_NAME,
							Headertax.getAttribute(CrocsXmlConstants.A_CHARGE_NAME));
					eleOrderHeaderTax.setAttribute(CrocsXmlConstants.A_TAX_NAME,
							Headertax.getAttribute(CrocsXmlConstants.A_TAX_NAME));
					eleOrderHeaderTax.setAttribute(CrocsXmlConstants.A_TAX,
							Headertax.getAttribute(CrocsXmlConstants.A_TAX));
					eleOrderHeaderTax.setAttribute(CrocsXmlConstants.A_TAX_PER_CENTAGE,
							Headertax.getAttribute(CrocsXmlConstants.A_TAX_PER_CENTAGE));
					eleOrderHeaderTaxes.appendChild(eleOrderHeaderTax);
					}
				}
				eleOrder.appendChild(eleOrderHeaderCharges);
				eleOrder.appendChild(eleOrderHeaderTaxes);
				
			}
			logger.verbose("Before header taxes added to Narvar retun order create input:"+SCXmlUtil.getString(inDoc));
		} catch (Exception e) {
			throw new YFSException("CrocsCreateOrderDetailsForNarvar.appendHeaderChargesAndTaxesOnLastReturn :Expection" + e.getMessage());
		}
		logger.verbose("CrocsCreateOrderDetailsForNarvar : appendHeaderChargesAndTaxesOnLastReturn: END");
	}
	
	/*
	 * Description: Calling GetChargeCategoryList for Charge Category
	 */
	public static List<String>  getChargeCategoryList(YFSEnvironment env,String documentType,String organizationCode, String flagY) throws RemoteException {
		List<String> list = new ArrayList<>();
		Document getChargeCategoryListInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.A_CHARGE_CATEGORY);
		getChargeCategoryListInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_DOCUMENT_TYPE,documentType);
		
		getChargeCategoryListInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_ORGANIZATION_CODE, organizationCode);
		getChargeCategoryListInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_IS_REFUNDABLE, flagY);
		logger.verbose("CrocsCreateOrderDetailsForNarvar : getChargeCategoryList inDoc"+SCXmlUtil.getString(getChargeCategoryListInDoc));

		Document getChargeCategoryList = CommonUtil.invokeService(env, CrocsConstant.STR_CROCS_GET_CHARGE_CATEGORYLIST_NARVAR,
				getChargeCategoryListInDoc);
		logger.verbose("CrocsCreateOrderDetailsForNarvar : getChargeCategoryList outDoc"+SCXmlUtil.getString(getChargeCategoryList));

		ArrayList<Element> arrChargeCategories = SCXmlUtil.getChildren(getChargeCategoryList.getDocumentElement(), CrocsXmlConstants.A_CHARGE_CATEGORY);
		
		for(Element eleChargeCategory:arrChargeCategories)
		{
			list.add(eleChargeCategory.getAttribute(CrocsXmlConstants.A_CHARGE_CATEGORY));
		}
		
		return list;
		
	}
	//EOMS-4374 -END : do not refund shipping related charges and taxes
	/**
	 * Description: Appending Line Charges and Line Taxes to each Order line of Return Order
	 * 
	 * @param inDoc
	 * @param docGetOrderList
	 * @param strEnterpriseCode
	 */
	public static void appendLineChargesAndTaxesOnEachOrderLine(Document inDoc, Document docGetOrderList,String strEnterpriseCode) {
		
		logger.verbose("CrocsCreateOrderDetailsForNarvar : appendLineChargesAndTaxesOnEachOrderLine: START");
		
		try {
			Element eleOrder = inDoc.getDocumentElement();
			Element orderLinesEle = SCXmlUtil.getChildElement(eleOrder, CrocsXmlConstants.E_ORDER_LINES);
			NodeList odrerLineList = orderLinesEle.getElementsByTagName(CrocsXmlConstants.E_ORDER_LINE);
			
			for (int i = 0; i < odrerLineList.getLength(); i++) {
				
				Element eleOrderLine = (Element) odrerLineList.item(i);
				double intOrderedQty = Double.parseDouble(eleOrderLine.getAttribute(CrocsXmlConstants.A_ORDERED_QTY));

				Element eleDerivedFromOrder = SCXmlUtil.getChildElement(eleOrderLine, CrocsXmlConstants.E_DERIVED_FROM);
				Element eleLinePriceInfo = SCXmlUtil.getChildElement(eleOrderLine, CrocsXmlConstants.E_LINE_PRICE_INFO);
				Element eleOrderLineCustomAttributes = SCXmlUtil.getChildElement(eleOrderLine, CrocsXmlConstants.E_CUSTOM_ATTRIBUTES);
				String strItemID = SCXmlUtil.getXpathAttribute(eleOrderLine, CrocsConstant.STR_XPATH_ITEM_ID);
				
				int intValue = (int) intOrderedQty; 
				returnOrder.put(strItemID, intValue);
				
				Element eleSalesOrderLine = XMLUtil.getElementByXPath(docGetOrderList,
						"/OrderList/Order/OrderLines/OrderLine[Item/@ItemID='" + strItemID + "']");
				
				Element eleSalesLinePriceInfo = SCXmlUtil.getChildElement(eleSalesOrderLine, CrocsXmlConstants.E_LINE_PRICE_INFO);
				eleLinePriceInfo.setAttribute(CrocsXmlConstants.A_UNIT_PRICE, eleSalesLinePriceInfo.getAttribute(CrocsXmlConstants.A_UNIT_PRICE));
				
				eleOrderLineCustomAttributes.getAttribute(CrocsConstant.STR_TEXT_1).replace(CrocsConstant.STR_AND,
						CrocsConstant.STR_AMP);
				eleOrderLineCustomAttributes.getAttribute(CrocsConstant.STR_TEXT_2).replace(CrocsConstant.STR_AND,
						CrocsConstant.STR_AMP);
				eleOrderLineCustomAttributes.getAttribute(CrocsConstant.STR_TEXT_3).replace(CrocsConstant.STR_AND,
						CrocsConstant.STR_AMP);

				
				Element eleOrderLineExtn =  inDoc.createElement(CrocsXmlConstants.E_EXTN);
				eleOrderLineExtn.setAttribute(CrocsXmlConstants.EXTN_TRACKING_NO, eleOrderLineCustomAttributes.getAttribute(CrocsConstant.STR_TEXT_1));
				eleOrderLineExtn.setAttribute(CrocsXmlConstants.A_EXTN_TRACKING_URL, eleOrderLineCustomAttributes.getAttribute(CrocsConstant.STR_TEXT_2));
				eleOrderLineExtn.setAttribute(CrocsXmlConstants.EXTN_SHIP_CARRIER, eleOrder.getAttribute(CrocsXmlConstants.A_SCAC));
				
				String strPrimeLineNo = eleSalesOrderLine.getAttribute(CrocsXmlConstants.A_PRIME_LINE_NO);
				eleDerivedFromOrder.setAttribute(CrocsXmlConstants.A_PRIME_LINE_NO, strPrimeLineNo);
				eleDerivedFromOrder.setAttribute(CrocsXmlConstants.A_ENTERPRISE_CODE, strEnterpriseCode);

				double intSalesOrderedQty = Double
						.parseDouble(eleSalesOrderLine.getAttribute(CrocsXmlConstants.A_ORDERED_QTY));

				double intSalesReturnablQty = Double.parseDouble(eleSalesOrderLine.getAttribute(CrocsXmlConstants.A_RETURNABLE_QTY));

				// Line Charges for US & CA
				double dChargeAmount;
				double dTaxAmountValue;
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
				
				// Line Taxes for US & CA
				Element orderLineTax = SCXmlUtil.getChildElement(eleSalesOrderLine, CrocsXmlConstants.E_LINE_TAXES);
				ArrayList<Element> lineTaxes = SCXmlUtil.getChildren(orderLineTax, CrocsXmlConstants.E_LINE_TAX);
				Element eleOrderLineTaxes = inDoc.createElement(CrocsXmlConstants.E_LINE_TAXES);
				for (Element tax : lineTaxes) {
					Element eleOrderLineTax = inDoc.createElement(CrocsXmlConstants.E_LINE_TAX);
					eleOrderLineTax.setAttribute(CrocsXmlConstants.A_TAX_NAME,
							tax.getAttribute(CrocsXmlConstants.A_TAX_NAME));
					eleOrderLineTax.setAttribute(CrocsXmlConstants.A_TAX_PER_CENTAGE,
							tax.getAttribute(CrocsXmlConstants.A_TAX_PER_CENTAGE));
					double dTaxAmount = Double.parseDouble(tax.getAttribute(CrocsXmlConstants.A_TAX));
					
					dTaxAmountValue = prorateLineChargesAndLineTaxes(dTaxAmount, intSalesReturnablQty, intSalesOrderedQty,intOrderedQty);
					
					eleOrderLineTax.setAttribute(CrocsXmlConstants.A_TAX, String.format("%.2f", dTaxAmountValue));
					eleOrderLineTaxes.appendChild(eleOrderLineTax);
				}
				eleOrderLine.appendChild(eleOrderLineCharges);
				eleOrderLine.appendChild(eleOrderLineTaxes);
				eleOrderLine.appendChild(eleOrderLineExtn);
			}
		} catch (Exception e) {
			throw new YFSException(
					"CrocsCreateOrderDetailsForNarvar.appendLineChargesAndTaxesOnEachOrderLine :Expection" + e.getMessage());
		}
		logger.verbose("CrocsCreateOrderDetailsForNarvar : appendLineChargesAndTaxesOnEachOrderLine: END");
	}
	
	public static double prorateLineChargesAndLineTaxes(double intChargeAmount,double intSalesReturnablQty, double intSalesOrderedQty,double intOrderedQty) {
		
		logger.verbose("CrocsCreateOrderDetailsForNarvar : prorateLineChargesAndLineTaxes: START");
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
			throw new YFSException("CrocsCreateOrderDetailsForNarvar.prorateLineChargesAndLineTaxes :Expection" + e.getMessage());
		}
		logger.verbose("CrocsCreateOrderDetailsForNarvar : prorateLineChargesAndLineTaxes: END");
		return dChargeAmount;
    }
	
	public static void appendLableURLOnNoteTex(Document inDoc) {
		logger.verbose("CrocsCreateOrderDetailsForNarvar : appendLableURLOnNoteTex: START");
		try {
			
			Element eleOrder = inDoc.getDocumentElement();
	
			Element orderLinesEle = SCXmlUtil.getChildElement(eleOrder, CrocsXmlConstants.E_ORDER_LINES);
			NodeList odrerLineList = orderLinesEle.getElementsByTagName(CrocsXmlConstants.E_ORDER_LINE);
			
			for (int i = 0; i < odrerLineList.getLength(); i++) {
				
				Element eleOrderLine = (Element) odrerLineList.item(i);
				Element eleOrderLineCustomAttributes = SCXmlUtil.getChildElement(eleOrderLine, CrocsXmlConstants.E_CUSTOM_ATTRIBUTES);
				
				Element eleOrderNotes = inDoc.createElement(CrocsConstant.A_NOTES);
				Element eleOrderNote = inDoc.createElement(CrocsConstant.A_NOTE);
				eleOrderNote.setAttribute(CrocsConstant.NOTE_TEXT,eleOrderLineCustomAttributes.getAttribute(CrocsConstant.STR_TEXT_3) );
				eleOrderNote.setAttribute(CrocsXmlConstants.A_REASON_CODE,CrocsConstant.STR_LABEL_URL);
				eleOrderNotes.appendChild(eleOrderNote);
				eleOrder.appendChild(eleOrderNotes);
				break;
			}
		} catch (Exception e) {
			throw new YFSException("CrocsCreateOrderDetailsForNarvar.appendLableURLOnNoteTex :Expection" + e.getMessage());
		}
		logger.verbose("CrocsCreateOrderDetailsForNarvar : appendLableURLOnNoteTex: END");
	}

}
