package com.crocs.oms.pricing;

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
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import com.yantra.yfs.japi.YFSUserExitException;
import com.yantra.yfs.japi.ue.YFSOrderRepricingUE;

/**
 * EOMS-1502 : Calculating Refund Total for Call center return orders
 * 
 */

public class CrocsOrderRepricingROUEImpl implements YFSOrderRepricingUE {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsOrderRepricingROUEImpl.class);

	static Map<String, Integer> returnOrder = new HashMap<>();
	static Map<String, Integer> salesOrder = new HashMap<>();

	@Override
	public Document orderReprice(YFSEnvironment env, Document inDoc) throws YFSUserExitException {

		logger.beginTimer("CrocsOrderRepricingROUEImpl.orderReprice(): Begin");
		logger.verbose("Input for orderReprice: Start :: " + XMLUtil.getXMLString(inDoc));

		String strSalesOrderHeaderKey = null;
		Document docGetOrderListOutput = null;
		try {

			Element eleOrder = inDoc.getDocumentElement();
			Element orderLinesEle = SCXmlUtil.getChildElement(eleOrder, CrocsXmlConstants.E_ORDER_LINES);
			NodeList odrerLineList = orderLinesEle.getElementsByTagName(CrocsXmlConstants.E_ORDER_LINE);

			if (odrerLineList.getLength() > 0) {
				Element eleOrderLine = (Element) odrerLineList.item(0);
				strSalesOrderHeaderKey = eleOrderLine.getAttribute(CrocsXmlConstants.A_DERIVED_FROM_ORDER_HEADER_KEY);
			}
	        
			// Calling getOrderList
			docGetOrderListOutput = getOrderListForSaleOrderHeaderKey(env, strSalesOrderHeaderKey);

			if (docGetOrderListOutput != null) {

				// Order Line Level Changes
				appendLineChargesAndTaxesOnEachOrderLine(inDoc, docGetOrderListOutput);

				// checking sales order is fully returned or not.
				boolean appendHeaderChargesAndTaxes = isFullyReturned(inDoc);

				// Appending Header Charges and Header taxes on Last return of the Order.
				if (appendHeaderChargesAndTaxes)
					appendHeaderTaxesOnLastReturn(inDoc, docGetOrderListOutput);
			}

		} catch (Exception e) {
			logger.verbose("CrocsOrderRepricingROUEImpl.orderReprice :Expection" + e.getMessage());
			throw new YFSException("CrocsOrderRepricingROUEImpl.orderReprice :Expection" + e.getMessage());
		}
		logger.verbose("Output for orderReprice: END :: " + XMLUtil.getXMLString(inDoc));
		return inDoc;
	}
	/**
	 * Description: Calling GetOrderList with Sales Order Header Key
	 *
	 * @param env
	 * @param strSalesOrderHeaderKey
	 * @return
	 * @throws Exception
	 */
	public static Document getOrderListForSaleOrderHeaderKey(YFSEnvironment env, String strSalesOrderHeaderKey)
			throws YFSException {

		logger.verbose("CrocsOrderRepricingROUEImpl : getOrderListForSaleOrderHeaderKey: START");

		Document getOrderListOut = null;
		try {
			if (!YFCObject.isVoid(strSalesOrderHeaderKey)) {

				Document getOrderListInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER);
				getOrderListInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_ORDER_HEADER_KEY,
						strSalesOrderHeaderKey);
				getOrderListOut = CommonUtil.invokeService(env, CrocsConstant.STR_CROCS_GET_ORDERLIST_NARVAR,
						getOrderListInDoc);
			}
		} catch (Exception e) {
			throw new YFSException(
					"CrocsOrderRepricingROUEImpl.getOrderListForSaleOrderHeaderKey :Expection" + e.getMessage());
		}
		logger.verbose("CrocsOrderRepricingROUEImpl : getOrderListForSaleOrderHeaderKey:: END:: "
				+ XMLUtil.getXMLString(getOrderListOut));
		return getOrderListOut;
	}
	/**
	 * Description: Appending Line Charges and Line Taxes to each Order line of Return Order
	 * 
	 * @param inDoc
	 * @param docGetOrderList
	 */
	public static void appendLineChargesAndTaxesOnEachOrderLine(Document inDoc, Document docGetOrderList) {

		logger.verbose("CrocsOrderRepricingROUEImpl : appendLineChargesAndTaxesOnEachOrderLine: START");

		try {
			Element eleOrder = inDoc.getDocumentElement();
			Element orderLinesEle = SCXmlUtil.getChildElement(eleOrder, CrocsXmlConstants.E_ORDER_LINES);
			NodeList odrerLineList = orderLinesEle.getElementsByTagName(CrocsXmlConstants.E_ORDER_LINE);

			for (int i = 0; i < odrerLineList.getLength(); i++) {

				
				Element eleOrderLine = (Element) odrerLineList.item(i);
				
				Element eleLineChargesNode = SCXmlUtil.getChildElement(eleOrderLine, CrocsXmlConstants.E_LINE_CHARGES);
				Element eleLineTaxesNode =  SCXmlUtil.getChildElement(eleOrderLine, CrocsXmlConstants.E_LINE_TAXES);
				
				eleOrderLine.removeChild(eleLineChargesNode);
				eleOrderLine.removeChild(eleLineTaxesNode);
				
				double intOrderedQty = Double.parseDouble(eleOrderLine.getAttribute(CrocsXmlConstants.A_ORDERED_QTY));

				String strItemID = SCXmlUtil.getXpathAttribute(eleOrderLine, CrocsConstant.STR_XPATH_ITEM_ID);

				int intValue = (int) intOrderedQty;
				returnOrder.put(strItemID, intValue);

				Element eleSalesOrderLine = XMLUtil.getElementByXPath(docGetOrderList,
						"/OrderList/Order/OrderLines/OrderLine[Item/@ItemID='" + strItemID + "']");

				double intSalesOrderedQty = Double
						.parseDouble(eleSalesOrderLine.getAttribute(CrocsXmlConstants.A_ORDERED_QTY));
				
				//Checking Last Return Qty for the OrderLine
				
				boolean bLastReturnQty=isLastReturnQtyForOrderLine(eleSalesOrderLine);
				
				double dTotalReturnedQty = TotalReturnedOrderQtyForEachLine(eleSalesOrderLine);
				
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

					dChargeAmount = CommonUtil.prorateLineChargesAndLineTaxes(bLastReturnQty, intChargeAmount,
							intSalesOrderedQty, intOrderedQty, dTotalReturnedQty);

					eleLineCharge.setAttribute(CrocsXmlConstants.A_CHARGE_PER_LINE,
							String.format("%.2f", dChargeAmount));
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

					dTaxAmountValue = CommonUtil.prorateLineChargesAndLineTaxes(bLastReturnQty, dTaxAmount,
							intSalesOrderedQty, intOrderedQty, dTotalReturnedQty);

					eleOrderLineTax.setAttribute(CrocsXmlConstants.A_TAX, String.format("%.2f", dTaxAmountValue));
					eleOrderLineTaxes.appendChild(eleOrderLineTax);
				}
				eleOrderLine.appendChild(eleOrderLineCharges);
				eleOrderLine.appendChild(eleOrderLineTaxes);
			}
		} catch (Exception e) {
			throw new YFSException(
					"CrocsOrderRepricingROUEImpl.appendLineChargesAndTaxesOnEachOrderLine :Expection"
							+ e.getMessage());
		}
		logger.verbose("CrocsOrderRepricingROUEImpl : appendLineChargesAndTaxesOnEachOrderLine: END");
	}
	/**
	 * Description: Appending Header Charges and Header Taxes to Last Return of Sales Order.
	 * 
	 * @param inDoc
	 * @param docGetOrderListOutput
	 */
	public static void appendHeaderTaxesOnLastReturn(Document inDoc, Document docGetOrderListOutput) {
		logger.verbose("CrocsOrderRepricingROUEImpl : appendHeaderChargesAndTaxesOnLastReturn: START");
		try {
			// Header Charges for US & CA
			Element eleOrder = inDoc.getDocumentElement();
			
			Element eleHeaderTaxesNode = (Element) eleOrder.getElementsByTagName(CrocsXmlConstants.E_HEADER_TAXES).item(0);
			eleOrder.removeChild(eleHeaderTaxesNode);
			
			Element eleOrderList = docGetOrderListOutput.getDocumentElement();
			Element eleorder = SCXmlUtil.getChildElement(eleOrderList, CrocsXmlConstants.E_ORDER);

			// Header Taxes for US & CA
			Element orderHeaderTaxes = SCXmlUtil.getChildElement(eleorder, CrocsXmlConstants.E_HEADER_TAXES);
			ArrayList<Element> arrHeaderTaxes = SCXmlUtil.getChildren(orderHeaderTaxes, CrocsXmlConstants.E_HEADER_TAX);
			Element eleOrderHeaderTaxes = inDoc.createElement(CrocsXmlConstants.E_HEADER_TAXES);
			for (Element Headertax : arrHeaderTaxes) {
				Element eleOrderHeaderTax = inDoc.createElement(CrocsXmlConstants.E_HEADER_TAX);
				
				if(Headertax.getAttribute(CrocsXmlConstants.A_CHARGE_CATEGORY)!=null 
						&&!CrocsConstant.A_EXTENDED_SHIPPING_PROTECTION.equalsIgnoreCase(Headertax.getAttribute(CrocsXmlConstants.A_CHARGE_CATEGORY))) {
					
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
			eleOrder.appendChild(eleOrderHeaderTaxes);
		} catch (Exception e) {
			throw new YFSException("CrocsOrderRepricingROUEImpl.appendHeaderChargesAndTaxesOnLastReturn :Expection"
					+ e.getMessage());
		}
		logger.verbose("CrocsOrderRepricingROUEImpl : appendHeaderChargesAndTaxesOnLastReturn: END");
	}
	/**
	 * Description: This method compares the sale order's returnable quantity with the return order's quantity, 
	 * and returns 'Y' if both values are the same, indicating the order is fully returned.
	 * 
	 * @param salesOrder
	 * @param returnOrder
	 * @return
	 */
	private static boolean isFullyReturned(Document inDoc) {

		logger.verbose("CrocsOrderRepricingROUEImpl : isFullyReturned: START");
		Element eleOrder = inDoc.getDocumentElement();
		Element eleHeaderChargeNode = (Element) eleOrder.getElementsByTagName(CrocsXmlConstants.E_HEADER_CHARGES).item(0);
		if(eleHeaderChargeNode!=null && eleHeaderChargeNode.hasChildNodes()) {
			Element orderHeaderCharges = SCXmlUtil.getChildElement(eleOrder, CrocsXmlConstants.E_HEADER_CHARGES);
			ArrayList<Element> arrHeaderCharges = SCXmlUtil.getChildren(orderHeaderCharges,CrocsXmlConstants.E_HEADER_CHARGE);
			for (Element Headercharge : arrHeaderCharges) {
				double dChargeAmount = Double.parseDouble(Headercharge.getAttribute(CrocsXmlConstants.A_CHARGE_AMOUNT));
				if(dChargeAmount>=0.00) {
					return true;
				}
				
			}
		}
		logger.verbose("CrocsOrderRepricingROUEImpl : isFullyReturned: END");
		return false; // Fully returned if all items have been returned in full
	}
	/**
	 * Description: This method returns as false when any OrderLine doesn't shipped. 
	 * 
	 * @param eleSalesOrderLine
	 * @return
	 */
	public static boolean isLastReturnQtyForOrderLine(Element eleSalesOrderLine) {

		logger.verbose("CrocsOrderRepricingROUEImpl : isLastReturnQtyForOrderLine: START");
		Element eleOrderStatuses = SCXmlUtil.getChildElement(eleSalesOrderLine, CrocsXmlConstants.E_ORDER_STATUSES);
		NodeList eleorderLineStatusList = eleOrderStatuses.getElementsByTagName(CrocsXmlConstants.E_ORDER_STATUS);
		for(int j=0; j<eleorderLineStatusList.getLength();j++) {
			
			Element eleOrderLineStatus = (Element) eleorderLineStatusList.item(j);
			String statusValue = eleOrderLineStatus.getAttribute(CrocsXmlConstants.A_STATUS);
            double status = Double.parseDouble(statusValue);
            if(status<=3700.00 && status != 9000.00) {
            	return false;
            }
		}
		logger.verbose("CrocsOrderRepricingROUEImpl : isLastReturnQtyForOrderLine: END");
		return true;
	}
	/**
	 * Description: This Method provide the sum of all qty which are returned created.
	 * 
	 * @param eleSalesOrderLine
	 * @return
	 */
	public static double TotalReturnedOrderQtyForEachLine(Element eleSalesOrderLine) {

		logger.verbose("CrocsOrderRepricingROUEImpl : TotalReturnedOrderQtyForOrderLine: START");
		double dTotalReturnedQty = 0;
		Element eleOrderStatuses = SCXmlUtil.getChildElement(eleSalesOrderLine, CrocsXmlConstants.E_ORDER_STATUSES);
		NodeList eleorderLineStatusList = eleOrderStatuses.getElementsByTagName(CrocsXmlConstants.E_ORDER_STATUS);
		for(int k=0; k<eleorderLineStatusList.getLength();k++) {
			
			Element eleOrderLineStatus = (Element) eleorderLineStatusList.item(k);
			String statusValue = eleOrderLineStatus.getAttribute(CrocsXmlConstants.A_STATUS);
            double status = Double.parseDouble(statusValue);
            if(status > 3700.00 && status != 9000.00) {
				String strStatusQty = eleOrderLineStatus.getAttribute(CrocsXmlConstants.A_STAT_QTY);
				double dValue = Double.parseDouble(strStatusQty);
				dTotalReturnedQty = dTotalReturnedQty + dValue;
            }
		}
		logger.verbose("CrocsOrderRepricingROUEImpl : TotalReturnedOrderQtyForOrderLine: END");
		return dTotalReturnedQty; 
	}

}
