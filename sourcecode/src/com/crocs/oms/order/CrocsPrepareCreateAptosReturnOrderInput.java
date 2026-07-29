package com.crocs.oms.order;

import java.util.ArrayList;
import java.math.BigDecimal;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;

/**
 * This class handles logic to update OMS mandatory attributes
 * on APTOS create order input before calling CreateOrder API
 * in Sterling.
 * Update ShipNode
 * Copy ExtnAttributes from SO
 * Copy payment methods from SO in specific scenarios
 * Update ProcessOrderOnReturnOrder flag conditionally
 * 
 * 
 * @author IBM
 *
 */
public class CrocsPrepareCreateAptosReturnOrderInput implements CrocsConstant {

	private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsPrepareCreateAptosReturnOrderInput.class);

	/**
	 * This method get details of Sales Order
	 * and copy fields on Return order
	 * Conditionally update ProcessPaymentOnReturnOrder attribute
	 * 
	 * @param env
	 * @param indoc is Return Order XML
	 * @return
	 */
	public Document prepareAPTOSReturnInput(YFSEnvironment env, Document indoc){
		logger.verbose("Starts of method CrocsBeforeCreateOrderROUEImpl:beforeCreateOrder: with input: "
				+SCXmlUtil.getString(indoc));

		String entryType = "";
		Element returnOrderEle = indoc.getDocumentElement();
		entryType = returnOrderEle.getAttribute(A_ENTRY_TYPE);
		logger.verbose("Return order having entry type: "+entryType);
		//APTOS Return STORE
		if(!YFCCommon.isVoid(entryType) && entryType.equalsIgnoreCase(A_ENTRY_TYPE_STORE)){

			logger.verbose("Adding details for APTOS return: ");

			//copying all required attributes on RO create order xml
			copyExtnAndOtherAttributesFromSO(env, returnOrderEle);
		}
		logger.verbose("Final create order xml is  "+SCXmlUtil.getString(indoc));

		return indoc;

	}

	/**
	 * This method is doing below logic
	 * Copy ExtnAttributes from SO
	 * Copy payment methods from SO in specific scenarios
	 * Update ProcessOrderOnReturnOrder flag conditionall
	 * @param env
	 * @param order
	 */
	private void copyExtnAndOtherAttributesFromSO(YFSEnvironment env, Element returnOrderEle){

		logger.verbose("Starts of method copyExtnattributesFromSO with input: "+SCXmlUtil.getString(returnOrderEle));
		Document salesOrderDetailsDoc = null;
		//DeriveFromOrder Element
		Element deriveFromOrderEle = SCXmlUtil.getXpathElement(returnOrderEle, 
				XPAH_DERIVED_FROM);
		//Preparing getOrderList Input Document
		Document getOrderListIndoc = SCXmlUtil.createDocument(E_ORDER);
		getOrderListIndoc.getDocumentElement().setAttribute(A_ORDER_NO, deriveFromOrderEle.getAttribute(A_ORDER_NO));
		getOrderListIndoc.getDocumentElement().setAttribute(A_DOCUMENT_TYPE, deriveFromOrderEle.getAttribute(A_DOCUMENT_TYPE));
		getOrderListIndoc.getDocumentElement().setAttribute(A_ENTERPRISE_CODE, deriveFromOrderEle.getAttribute(A_ENTERPRISE_CODE));

		logger.verbose("Input of getOrderList API: "+SCXmlUtil.getString(getOrderListIndoc));

		try {
			salesOrderDetailsDoc = CommonUtil.invokeService(env, CROCS_GET_ORDER_LIST_FOR_APTOS_SYNC_SERV, getOrderListIndoc);
			logger.verbose("GetOrderList output document is "+SCXmlUtil.getString(salesOrderDetailsDoc));

			Element salesOrderEle = SCXmlUtil.getChildElement(salesOrderDetailsDoc.getDocumentElement(), E_ORDER);
			String extnRefundAmount = "";
			Element returnOrderExn = SCXmlUtil.getChildElement(returnOrderEle, E_EXTN);
			extnRefundAmount = returnOrderExn.getAttribute(A_EXTN_REFUND_AMOUNT);

			//Calculating TotalAmount for Return order
			Double dTotalAmountOnRO = calculateTotalAmountInOMS(env, returnOrderEle);

			if(!YFCCommon.isVoid(extnRefundAmount) && !YFCCommon.isVoid(dTotalAmountOnRO)){
				Double dRefundAmount = Double.parseDouble(extnRefundAmount);
				Double dStoreSettledAmount = dTotalAmountOnRO- dRefundAmount;

				/**
				 * EOMS-4394 Changes Start
				 */
				//Refund is processed at Sales order
				returnOrderEle.setAttribute(A_PROCESS_PAYMENTS_ON_RETURN_ORDER, FLAG_N);

				//1.RefundAmount != TotalAmount 
				if(dTotalAmountOnRO.compareTo(dRefundAmount)!=0){
					logger.verbose("Refund Amount and Total Amount are Not same::");
					//First check if headerCharges element is present EOMS-4445
					Element headerChargesReturnEle = SCXmlUtil.getChildElement(returnOrderEle, E_HEADER_CHARGES);
					if(!YFCCommon.isVoid(headerChargesReturnEle)){
						logger.verbose("Return order already has headerCharges element::");
						Element dummyHeaderChargeEle = SCXmlUtil.createChild(headerChargesReturnEle, E_HEADER_CHARGE);
						dummyHeaderChargeEle.setAttribute(A_CHARGE_AMOUNT, dStoreSettledAmount.toString());
						dummyHeaderChargeEle.setAttribute(A_CHARGE_CATEGORY, A_IN_STORE_EXCHANGE_DISCOUNT);
						dummyHeaderChargeEle.setAttribute(A_CHARGE_NAME, A_IN_STORE_EXCHANGE);
						logger.verbose("Added HeaderCharges Element is:: "+SCXmlUtil.getString(dummyHeaderChargeEle));

					}else{
						logger.verbose("HeaderCharges Element is not present on return order, so first creating element");
						Element dummyHeaderChargesEle = SCXmlUtil.createChild(returnOrderEle, E_HEADER_CHARGES);
						Element dummyHeaderChargeEle = SCXmlUtil.createChild(dummyHeaderChargesEle, E_HEADER_CHARGE);
						dummyHeaderChargeEle.setAttribute(A_CHARGE_AMOUNT, dStoreSettledAmount.toString());
						dummyHeaderChargeEle.setAttribute(A_CHARGE_CATEGORY, A_IN_STORE_EXCHANGE_DISCOUNT);
						dummyHeaderChargeEle.setAttribute(A_CHARGE_NAME, A_IN_STORE_EXCHANGE);
						logger.verbose("InStoreExchange Header Discount element details are: "+SCXmlUtil.getString(dummyHeaderChargeEle));
					}
				}
				/**
				 * EOMS-4394 Changes End
				 */

			}			

			/**
			 * Copy PersonInfoShipTo & PersonInfoBillTo
			 * From Sales Order
			 */

			Element elePersonInfoBillTo = SCXmlUtil.getXpathElement(salesOrderEle,
					CrocsConstant.STR_XPATH_ORDERLIST_PERSON_INFO_BILL_TO);
			Element elePersonInfoShipTo = SCXmlUtil.getXpathElement(salesOrderEle, STR_XPATH_ORDERLIST_PERSON_INFO_SHIP_TO);

			SCXmlUtil.importElement(returnOrderEle, elePersonInfoBillTo);
			SCXmlUtil.importElement(returnOrderEle, elePersonInfoShipTo);

			/**
			 * Copying Order/Extn from Sales to return Order
			 */

			Element salesOrderLinesEle = SCXmlUtil.getChildElement(salesOrderEle, E_ORDER_LINES);
			Element salesOrderExtnEle = SCXmlUtil.getChildElement(salesOrderEle, E_EXTN);
			if(!YFCCommon.isVoid(salesOrderExtnEle)){
				//As return order element is already present so adding SO Extn attributes to RO
				NamedNodeMap extnAllAttributes = salesOrderExtnEle.getAttributes();
				int size = extnAllAttributes.getLength();
				Element extnReturnOrder = SCXmlUtil.getChildElement(returnOrderEle, E_EXTN);
				if(!YFCCommon.isVoid(extnReturnOrder)){
					for(int i=0; i<size; i++){
						Node attribute = extnAllAttributes.item(i);
						extnReturnOrder.setAttribute(attribute.getNodeName(), attribute.getNodeValue());
					}

				}else{
					//do import
					SCXmlUtil.importElement(returnOrderEle, salesOrderExtnEle);
				}
			}
			/**
			 * Copying OrderLine/Extn from Sales to Return Order
			 */
			Element returnOrderLines = SCXmlUtil.getChildElement(returnOrderEle, E_ORDER_LINES);
			ArrayList<Element> returnOrderLineList = SCXmlUtil.getChildren(returnOrderLines, E_ORDER_LINE);
			for (Element returnOrderLine : returnOrderLineList) {
				//Updating ShipNode
				returnOrderLine.setAttribute(A_RECEIVING_NODE, returnOrderEle.getAttribute(A_SELLER_ORGANIZATION_CODE));

				// For each return line fetch associated sales order line
				/**
				 * As per discussion with Saurabh,
				 * need to fetch associate Sales order
				 * based upon ItemID in Return Order XML
				 */
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

				Element salesOrderLineExtn = SCXmlUtil.getChildElement(orderLineSOEle, E_EXTN);
				if (!YFCCommon.isVoid(salesOrderLineExtn)) {
					SCXmlUtil.importElement(returnOrderLine, salesOrderLineExtn);
				}

				/**
				 * Copying LineCharge/Extn for Each line
				 * As per current assumption chargeName coming on return
				 * order line are same as Sales order line charges
				 *
				 * Will be checking with Saurabh for Unique Identifier to fetch
				 * matching line charge
				 */

				Element lineChargesReturn = SCXmlUtil.getChildElement(returnOrderLine, E_LINE_CHARGES);
				ArrayList<Element> lineChargeReturnList = SCXmlUtil.getChildren(lineChargesReturn, E_LINE_CHARGE);
				for(Element lineChargeReturn : lineChargeReturnList){
					//need to pro-rate this before create order as APTOS is sending per quantity charge to OMS
					logger.verbose("Line Charge Element is: "+SCXmlUtil.getString(lineChargeReturn));
					String orderedQty = returnOrderLine.getAttribute(A_ORDERED_QTY);
					double dOrderedQty = Double.parseDouble(orderedQty);

					String chargeValue = lineChargeReturn.getAttribute(A_CHARGE_PER_LINE);
					//Need to multiply Quantity with Charge as Discount EOMS-3540 defect fix
					double dLineChargeAmount = Double.parseDouble(chargeValue) * dOrderedQty ;
					//updating chargePerLine value
					lineChargeReturn.setAttribute(A_CHARGE_PER_LINE, Double.toString(dLineChargeAmount));

					String chargeNameOnRo = lineChargeReturn.getAttribute(A_CHARGE_NAME);
					Element lineChargeEleSO = SCXmlUtil.getXpathElement(orderLineSOEle,
							"//LineCharges/LineCharge[@ChargeName='"+chargeNameOnRo+"']");
					Element lineChargeExtnSO = SCXmlUtil.getChildElement(lineChargeEleSO, E_EXTN);
					if(!YFCCommon.isVoid(lineChargeExtnSO)){
						SCXmlUtil.importElement(lineChargeReturn, lineChargeExtnSO);
					}

				}
			}

			//Orderheader attributes
			returnOrderEle.setAttribute(A_SHIP_NODE, returnOrderEle.getAttribute(A_SELLER_ORGANIZATION_CODE));

			logger.verbose("Updated Return order create order input XML: "+SCXmlUtil.getString(returnOrderEle));

		} catch (Exception e) {
			logger.error("Error during getOrderList API call In CrocsPrepareCreateAptosReturnOrderInput "+e.getLocalizedMessage());
		}

	}

	/**
	 * This method is calculating TotalAmount value
	 * on a Return order using formula
	 * (Qty * Price) - Charges + Taxes for each line
	 * which will be later used for identification if 
	 * refund should be done on Sales Order or Return order
	 * 
	 * @param env
	 * @param returnOrderEle
	 * @return
	 */
	public Double calculateTotalAmountInOMS(YFSEnvironment env, Element returnOrderEle){

		logger.verbose("Starts of method CrocsPrepareCreateAptosReturnOrderInput::calculateTotalAmountInOMS:: with input "
				+SCXmlUtil.getString(returnOrderEle));
			
		Element returnOrderLines = SCXmlUtil.getChildElement(returnOrderEle, E_ORDER_LINES);
		ArrayList<Element> returnOrderLineList = SCXmlUtil.getChildren(returnOrderLines, E_ORDER_LINE);
		double orderLinesTotalAmount = 0.0d;
		for (Element orderLine : returnOrderLineList) {
			logger.verbose("Iterating order line: "+SCXmlUtil.getString(orderLine));
			//UnitPrice * OrderedQty
			double dUnitPrice = 0.00d;
			double dOrderedQty = 0.00d;
			double dLineChargeAmount = 0.00d;
			double dTaxValue = 0.00d;
			Element linePriceInfo = SCXmlUtil.getChildElement(orderLine, E_LINE_PRICE_INFO);
			String unitPrice = linePriceInfo.getAttribute(A_UNIT_PRICE);
			dUnitPrice = Double.parseDouble(unitPrice);
			String orderedQty = orderLine.getAttribute(A_ORDERED_QTY);
			dOrderedQty = Double.parseDouble(orderedQty);

			//LineCharges
			/**
			 * Fix for defect-4121 when multiple line charges are
			 * present in Return order XML
			 */
			Element lineCharges = SCXmlUtil.getChildElement(orderLine, E_LINE_CHARGES);
			if (!YFCCommon.isVoid(lineCharges)) {
				ArrayList<Element> lineChargeList = SCXmlUtil.getChildren(lineCharges, E_LINE_CHARGE);
				for (Element lineCharge : lineChargeList) {
					logger.verbose("Line charge Element is: "+SCXmlUtil.getString(lineCharge));
					String chargeValue = lineCharge.getAttribute(A_CHARGE_PER_LINE);
					if (!YFCCommon.isVoid(chargeValue)) {
						dLineChargeAmount += Double.parseDouble(chargeValue) * dOrderedQty;
					}
				}
			}

			//LineTaxes
			Element lineTaxEle = SCXmlUtil.getXpathElement(orderLine, XPATH_LINE_TAX);
			if(!YFCCommon.isVoid(lineTaxEle)){
				logger.verbose("Line tax Element is: "+SCXmlUtil.getString(lineTaxEle));
				String taxValue = lineTaxEle.getAttribute(A_TAX);
				dTaxValue = Double.parseDouble(taxValue);
			}
			

			// Line total = (Qty * Price) - Charges + Taxes
			double lineTotal = (dOrderedQty * dUnitPrice) - dLineChargeAmount + dTaxValue;
			orderLinesTotalAmount += lineTotal;
		}
		
		 /**
		  * Need to include HeaderCharges and HeaderTaxes as well
		  * coming from APTOS
		  * EOMS-4262 Fix starts
		  */
		
		double dShippingCharge = 0.00d;
		double dTaxValue = 0.00d;
		Element headerChargesEle = SCXmlUtil.getChildElement(returnOrderEle, E_HEADER_CHARGES);
		//Adding Null Check
		if(!YFCCommon.isVoid(headerChargesEle) && headerChargesEle.hasChildNodes()){
			//for now lets consider one charge (not discount) HeaderCharge
			String shippingCharge  = "";
			Element headerChargeEle = SCXmlUtil.getChildElement(headerChargesEle, E_HEADER_CHARGE);
			shippingCharge = headerChargeEle.getAttribute(A_CHARGE_AMOUNT);
			dShippingCharge = Double.parseDouble(shippingCharge);
			logger.verbose("Shipping Charge value is "+dShippingCharge);
		}
		
		Element headerTaxesEle = SCXmlUtil.getChildElement(returnOrderEle, E_HEADER_TAXES);
		//Adding Null Check
		if(!YFCCommon.isVoid(headerTaxesEle)  && headerTaxesEle.hasChildNodes()){
			String taxValue = "";
			Element headerTaxEle = SCXmlUtil.getChildElement(headerTaxesEle, E_HEADER_TAX);
			taxValue = headerTaxEle.getAttribute(A_TAX);
			dTaxValue = Double.parseDouble(taxValue);
			logger.verbose("Tax value is "+dTaxValue);
		}
		
		double dReturnOrderTotal = 0.00d;
		dReturnOrderTotal = orderLinesTotalAmount + dShippingCharge + dTaxValue;
		logger.verbose("Return order total is "+dReturnOrderTotal);
		
		/**
		 * EOMS-4262 Fix Ends
		 */

		// Round totalAmount to 2 decimal places
		BigDecimal bdTotalAmount = new BigDecimal(dReturnOrderTotal).setScale(2, BigDecimal.ROUND_HALF_UP);
		logger.verbose("Total Amount value is: " + bdTotalAmount.doubleValue());
		return bdTotalAmount.doubleValue();
	}

}
