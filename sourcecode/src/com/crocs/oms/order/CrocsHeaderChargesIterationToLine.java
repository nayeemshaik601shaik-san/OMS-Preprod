package com.crocs.oms.order;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//EOMS - 2596 
//Iterator HeaderCharges and HeaderTaxes to LineCharges and Line Taxes
// Create Order Input is the Input to the Java code

public class CrocsHeaderChargesIterationToLine {

	private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsHeaderChargesIterationToLine.class);
	String enterpriseCode= "";
	boolean isKoreaOrder = false;
	public Document headerChargesIterationToLine(Document inputDoc) {
		try {
			Element orderElement = inputDoc.getDocumentElement();
			enterpriseCode = orderElement.getAttribute(CrocsConstant.A_ENTERPRISE_CODE);
			if(CrocsConstant.CROCS_KR.equals(enterpriseCode)) {
				isKoreaOrder = true;
			}
			Double totalWeightedValue = getTotalWeightedValue(orderElement);
			applyChargesToOrderLines(inputDoc, totalWeightedValue);
			//EOMS-4652 START
			// Defined eligible taxes for US and CA
			String[] eligibleProrationTaxesForUS = CrocsConstant.ELIGIBLE_PRORATION_TAXES_FOR_US;
			String[] eligibleProrationTaxesForCA =CrocsConstant.ELIGIBLE_PRORATION_TAXES_FOR_CA;
			//String strEnterpriseCode = orderElement.getAttribute(CrocsConstant.A_ENTERPRISE_CODE);
			// key: taxName and value: totalWeighted value for tax
			Map<String, Double> totalWeightedValueTaxMap = new HashMap<>();
			//key :taxeName and Value:orderline list
			Map<String,List<Element>> orderLineListTaxMap=new HashMap<>();
			if (CrocsConstant.CROCS_US.equalsIgnoreCase(enterpriseCode) || CrocsConstant.HEYDUDE_US.equals(enterpriseCode)) {
				for (String taxName : eligibleProrationTaxesForUS) {
					getTotalWeightedValueForTax(orderElement, taxName, totalWeightedValueTaxMap,orderLineListTaxMap);

				}
				applyTaxesToOrderLines(orderElement, totalWeightedValueTaxMap, eligibleProrationTaxesForUS,orderLineListTaxMap);

			} else if(CrocsConstant.CROCS_CA.equalsIgnoreCase(enterpriseCode) || CrocsConstant.HEYDUDE_CA.equals(enterpriseCode)){
				for (String taxName : eligibleProrationTaxesForCA) {
					getTotalWeightedValueForTax(orderElement, taxName, totalWeightedValueTaxMap,orderLineListTaxMap);

				}
				applyTaxesToOrderLines(orderElement, totalWeightedValueTaxMap, eligibleProrationTaxesForCA,orderLineListTaxMap);

			}
			//EOMS-4652 END
			logger.info("Updated Order: " + SCXmlUtil.getString(orderElement));

		} catch (Exception e) {

			logger.error("CrocsHeaderChargesIterationToLine : Error processing header charges", e);
		}

		return inputDoc;
	}

	// calculate the Charge per line based on the OrderedQty and unit price for all
	// The
	// OrderLines
	private void applyChargesToOrderLines(Document inputDoc, Double totalWeightedValue) {
		NodeList headerChargesNodes = inputDoc.getElementsByTagName(CrocsXmlConstants.E_HEADER_CHARGE);
		NodeList orderLineNodeList = inputDoc.getElementsByTagName(CrocsXmlConstants.E_ORDER_LINE);
		HashMap<Integer, Double> discountMap = new HashMap<>();
		HashMap<Integer, Double> chargeOverflowMap = new HashMap<>();

		int numLines = orderLineNodeList.getLength();

		for (int j = 0; j < numLines; j++) {
			Element orderLineEle = (Element) orderLineNodeList.item(j);
			double lineValue = getLineWeightedValue(orderLineEle);
			int chargeIndex = 0;

			for (int i = 0; i < headerChargesNodes.getLength(); i++) {
				Element headerChargeEle = (Element) headerChargesNodes.item(i);
				String chargeName = headerChargeEle.getAttribute(CrocsXmlConstants.A_CHARGE_NAME);

				if (shouldSkipCharge(chargeName))
					continue;

				
				String chargeAmountHdr = headerChargeEle.getAttribute(CrocsXmlConstants.A_CHARGE_AMOUNT);
				if(isKoreaOrder) {
					chargeAmountHdr = getSubString(chargeAmountHdr);
				}
				double chargeAmount = Double.parseDouble(chargeAmountHdr);
				Element headerExtn = (Element) headerChargeEle.getElementsByTagName(CrocsXmlConstants.E_EXTN).item(0);

				double chargePerLine = 0;

				if (j < numLines - 1) {
					chargePerLine = calculateProportionalCharge(lineValue, totalWeightedValue, chargeAmount);
					discountMap.merge(chargeIndex, chargePerLine,
							(oldVal, newVal) -> roundToTwoDecimals(oldVal + newVal));
				} else {
					// Last line: ensure rounding adjustment
					chargePerLine = roundToTwoDecimals(chargeAmount - discountMap.getOrDefault(chargeIndex, 0.0));
				}

				// EOMS-7885 YFS10419 YFS: Charge cannot be negative
				// Update to 0 and record overflow for redistribution after the loop
				if (chargePerLine < 0) {
					logger.info(" chargePerLine is negative " + chargePerLine);
					chargeOverflowMap.merge(chargeIndex, roundToTwoDecimals(Math.abs(chargePerLine)),
							(oldVal, newVal) -> roundToTwoDecimals(oldVal + newVal));
					chargePerLine = 0.0;
				}

				chargeIndex++;
				Element eleLineCharge = addOrUpdateLineCharge(orderLineEle, chargePerLine, chargeIndex, headerExtn);
				applyChargesToOrderLines(inputDoc, orderLineEle, chargePerLine, eleLineCharge, chargeAmount, headerExtn,
						chargeIndex);

				logger.verbose(String.format(
						"CrocsHeaderChargesIterationToLine: applyChargesToOrderLines %s, %s, %.2f, %.2f, %d",
						headerChargeEle.getAttribute(CrocsXmlConstants.A_CHARGE_CATEGORY), chargeName, chargeAmount,
						chargePerLine, chargeIndex));

				
			}
		}

		// EOMS-7885 YFS10419 YFS: Charge cannot be negative
		// redistribute accumulated charge overflow across lines, one line at a time
		for (Map.Entry<Integer, Double> entry : chargeOverflowMap.entrySet()) {
			int idx = entry.getKey();
			double overflow = entry.getValue();
			
			while (overflow > 0.0) {
				Element maxChargeLine = getLineWithMaxLineCharge(orderLineNodeList, idx + 1);
				if (maxChargeLine == null) {
					logger.info("YFS10419 YFS: Charge overflow " + overflow + " could not be fully redistributed for chargeIndex " + (idx + 1));
					break;
				}
				String chargeName = CrocsConstant.A_CROCS_PROMOTION_HDR_DISCOUNT + (idx + 1);
				Element lineCharge = SCXmlUtil.getXpathElement(maxChargeLine,
						"LineCharges/LineCharge[@ChargeName='" + chargeName + "']");
				if(lineCharge == null){
				logger.info("YFS10419 YFS: Charge node not found for chargeIndex " + (idx + 1));
                break;
				}
				double existing = Double.parseDouble(lineCharge.getAttribute(CrocsXmlConstants.A_CHARGE_PER_LINE));
				double reduction = roundToTwoDecimals(Math.min(existing, overflow));
				lineCharge.setAttribute(CrocsXmlConstants.A_CHARGE_PER_LINE,
						String.valueOf(roundToTwoDecimals(existing - reduction)));
				overflow = roundToTwoDecimals(overflow - reduction);
				
			}
		}
	}

	private double getLineWeightedValue(Element orderLineEle) {
		int qty = (int) Double.parseDouble(SCXmlUtil.getAttribute(orderLineEle, CrocsXmlConstants.A_ORDERED_QTY));
		Element linePriceInfo = SCXmlUtil.getChildElement(orderLineEle, CrocsConstant.A_LINE_PRICE_INFO);
		String strUnitPrice = linePriceInfo.getAttribute(CrocsConstant.A_UNIT_PRICE);
		if(isKoreaOrder) {
			strUnitPrice = getSubString(strUnitPrice);
		}
		double unitPrice = Double.parseDouble(strUnitPrice);
		return qty * unitPrice;
	}

	private double calculateProportionalCharge(double lineValue, double totalWeightedValue, double chargeAmount) {
		return lineValue > 0 ? roundToTwoDecimals((lineValue / totalWeightedValue) * chargeAmount) : 0;
	}

	private boolean shouldSkipCharge(String chargeName) {
		return chargeName.equals(CrocsXmlConstants.A_SHIPPING_CHARGE)
				|| chargeName.equals(CrocsXmlConstants.A_SHIPPING_DISCOUNT)
				|| chargeName.equalsIgnoreCase(CrocsConstant.A_EXTENDED_SHIPPING_PROTECTION);
	}

	// Consider all the Taxes Except Shippingtaxes and for each Tax update On
	// eachOrderLine
	private void applyChargesToOrderLines(Document inputDoc, Element orderLineEle, double chargePerLine,
			Element eleLineCharge, double chargeAmount, Element headerExtn, int count) {

		chargePerLine = roundToTwoDecimals(chargePerLine);

		Element lineChargesExtnElement = eleLineCharge.getOwnerDocument().createElement(CrocsXmlConstants.E_EXTN);
		if (!YFCCommon.isVoid(headerExtn)) {
			lineChargesExtnElement = copyAttributes(headerExtn, lineChargesExtnElement);
		}
		Node importedNode = inputDoc.importNode(lineChargesExtnElement, true);
		eleLineCharge.appendChild(importedNode);

		NodeList lineChargesNodes = orderLineEle.getElementsByTagName(CrocsXmlConstants.E_LINE_CHARGES);
		if (lineChargesNodes.getLength() > 0) {
			(lineChargesNodes.item(0)).appendChild(eleLineCharge);
		} else {
			Element lineChargesElement = orderLineEle.getOwnerDocument()
					.createElement(CrocsXmlConstants.E_LINE_CHARGES);
			lineChargesElement.appendChild(eleLineCharge);
			orderLineEle.appendChild(lineChargesElement);
		}

		logger.verbose("CrocsHeaderChargesIterationToLine:  applyChargesToOrderLines " + chargePerLine + " , "
				+ chargeAmount + " , " + SCXmlUtil.getString(orderLineEle) + " , " + " , " + count + " , "
				+ SCXmlUtil.getString(headerExtn));

	}

	// Apply Taxes on the Each OrderLine
	// EOMS-7885 YFS10420	YFS: Tax cannot be negative
	private double applyTaxToOrderLines(Element orderElement, String taxName, double taxPerLine, double taxAmount) {
    logger.verbose("CrocsHeaderChargesIterationToLine:  applyTaxToOrderLines " + taxName + " , " + taxPerLine
				+ " , " + SCXmlUtil.getString(orderElement) + " , " + taxAmount + " , ");
		 return addOrUpdateLineTax(orderElement, taxName, taxPerLine);

	}

	// Add LineCharges to the OrderLine if LineCharge isn't present
	private Element addOrUpdateLineCharge(Element orderLineElement, double chargePerLine, int count,
			Element headerExtn) {
		Element lineChargeElement = createChargeElement(orderLineElement, chargePerLine, count, headerExtn);

		logger.verbose(
				"CrocsHeaderChargesIterationToLine:  addOrUpdateLineCharge " + SCXmlUtil.getString(orderLineElement));
		return lineChargeElement;

	}

	
	/**
	 * Applies proportional taxes to individual order lines for all eligible tax types,
	 * excluding shipping-related taxes.
	 *
	 * <p>This method iterates over the provided list of eligible tax names and calculates
	 * each order line's share of the header-level tax based on its weighted value. It then
	 * applies the calculated tax to the corresponding order line. The last line receives
	 * any remaining tax amount to handle rounding differences.</p>
	 *
	 * @param orderElement              The XML element representing the order.
	 * @param totalWeightedValueTaxMap A map containing total weighted values per tax type.
	 * @param eligibleTaxes             An array of tax names eligible for proportional distribution.
	 * @param orderLineListTaxMap       A map containing lists of order lines for each eligible tax.
	 */

	public void applyTaxesToOrderLines(Element orderElement, Map<String, Double> totalWeightedValueTaxMap,
			String[] eligibleTaxes, Map<String, List<Element>> orderLineListTaxMap) {
		//loop through eligible taxes
		for (String taxName : eligibleTaxes) {

			double totalAddedTaxes = 0.0;
			double totalOverflow = 0.0; 
			//EOMS-4652
			// fetch list of lines which are eligible for proportional tax
			List<Element> orderLineList = orderLineListTaxMap.get(taxName);
			Element headerTaxElment = SCXmlUtil.getXpathElement(orderElement,
					"HeaderTaxes/HeaderTax[@TaxName='" + taxName + "' and @Tax!=0.0 ]");
			Double totalWeightedValue = totalWeightedValueTaxMap.get(taxName);
			if (headerTaxElment != null && totalWeightedValue != null) {
				double taxAmount = Double.parseDouble(headerTaxElment.getAttribute(CrocsXmlConstants.A_TAX));

				for (int j = 0; j < orderLineList.size(); j++) {
					Element orderLine = orderLineList.get(j);

					double lineWeightedValue = getLineWeightedValue(orderLine);
					double taxPerLine;

					if (j == orderLineList.size() - 1) {
						// Adjust last line for rounding accuracy
						taxPerLine = taxAmount - totalAddedTaxes;
					} else {
						taxPerLine = calculateProportionalTax(lineWeightedValue, totalWeightedValue, taxAmount);
					}

					taxPerLine = roundToTwoDecimals(taxPerLine);
					totalAddedTaxes += taxPerLine;
					totalOverflow += applyTaxToOrderLines(orderLine, taxName, taxPerLine, taxAmount);
				}

				// YFS10420	YFS: Tax cannot be negative
				// after ALL lines are processed — redistribute overflow across multiple lines
				
				while (totalOverflow > 0.0) {
					Element maxTaxLine = getLineWithMaxLineTax(orderLineList, taxName);
					if (maxTaxLine == null) {
						logger.info("YFS10420 YFS: Tax overflow " + totalOverflow
								+ " could not be fully redistributed for taxName " + taxName);
						break;
					}
					totalOverflow = addOrUpdateLineTax(maxTaxLine, taxName,
							roundToThreeDecimals(totalOverflow));
					
				}
			}
		}

	}

	//Get Line with Maximum Tax 
	private Element getLineWithMaxLineTax(List<Element> orderLineList, String taxName) {
	 Element maxLine = null;
	 double maxTax = 0.0;
	 for (Element orderLine : orderLineList) {
	 	Element lineTaxEle = SCXmlUtil.getXpathElement(orderLine,
	 			"LineTaxes/LineTax[@TaxName='" + taxName + "']");
	 	if (lineTaxEle != null) {
	 		double tax = Double.parseDouble(lineTaxEle.getAttribute(CrocsXmlConstants.A_TAX));
	 		if (tax > maxTax) { maxTax = tax; maxLine = orderLine; }
	 	}
	 }
	 return maxLine;
	}

	//Get Line with Maximum Charge
	private Element getLineWithMaxLineCharge(NodeList orderLineNodeList, int chargeIndex) {
		Element maxLine = null;
		double maxCharge = 0.0;
		String chargeName = CrocsConstant.A_CROCS_PROMOTION_HDR_DISCOUNT + chargeIndex;
		for (int j = 0; j < orderLineNodeList.getLength(); j++) {
			Element orderLine = (Element) orderLineNodeList.item(j);
			Element lineChargeEle = SCXmlUtil.getXpathElement(orderLine,
					"LineCharges/LineCharge[@ChargeName='" + chargeName + "']");
			if (lineChargeEle != null) {
				double charge = Double.parseDouble(lineChargeEle.getAttribute(CrocsXmlConstants.A_CHARGE_PER_LINE));
				if (charge > maxCharge) { maxCharge = charge; maxLine = orderLine; }
			}
		}
		return maxLine;
	}

	private double calculateProportionalTax(double lineValue, double totalWeightedValue, double taxAmount) {
		return lineValue > 0 ? (lineValue / totalWeightedValue) * taxAmount : 0;
	}

	// Add Taxes on the LineTaxes
	private double addOrUpdateLineTax(Element orderLineElement, String taxName, double taxPerLine) {
		double overflow =0.0;
		NodeList lineTaxesNodes = orderLineElement.getElementsByTagName(CrocsXmlConstants.E_LINE_TAXES);

		if (lineTaxesNodes.getLength() > 0) {
			Element lineTaxesElement = (Element) lineTaxesNodes.item(0);
			NodeList lineTaxElements = lineTaxesElement.getElementsByTagName(CrocsXmlConstants.E_LINE_TAX);

			for (int j = 0; j < lineTaxElements.getLength(); j++) {
				Element lineTaxElement = (Element) lineTaxElements.item(j);
				if (taxName.equals(lineTaxElement.getAttribute(CrocsXmlConstants.A_TAX_NAME))) {
					double existingTax = Double.parseDouble(lineTaxElement.getAttribute(CrocsXmlConstants.A_TAX))
							- taxPerLine;
                             // YFS10420	YFS: Tax cannot be negative
							 if(existingTax < 0){
							 	logger.info("YFS10420 YFS: Tax cannot be negative " + existingTax);
								overflow = roundToThreeDecimals(Math.abs(existingTax));
							 	existingTax=0.0;
							}

					existingTax = roundToThreeDecimals(existingTax);
					lineTaxElement.setAttribute(CrocsXmlConstants.A_TAX, String.valueOf(existingTax));

					break;
				}
			}

		}

		logger.verbose("CrocsHeaderChargesIterationToLine:  addOrUpdateLineTax " + taxName + " , " + taxPerLine + " , "
				+ SCXmlUtil.getString(orderLineElement) + " , " + taxPerLine);

				return overflow;
    }

	// Create Line Charge Element
	private static Element createChargeElement(Element orderLineElement, double chargePerLine, int count,
			Element headerExtn) {
		Element lineChargeElement = orderLineElement.getOwnerDocument().createElement(CrocsXmlConstants.E_LINE_CHARGE);
		lineChargeElement.setAttribute(CrocsXmlConstants.A_CHARGE_CATEGORY, CrocsConstant.A_CROCS_PROMOTION_DISCOUNT);
		if (count > 0) {
			lineChargeElement.setAttribute(CrocsXmlConstants.A_CHARGE_NAME,
					CrocsConstant.A_CROCS_PROMOTION_HDR_DISCOUNT + count);
		}
		lineChargeElement.setAttribute(CrocsXmlConstants.A_CHARGE_PER_LINE, String.valueOf(chargePerLine));

		logger.verbose("CrocsHeaderChargesIterationToLine:  createChargeElement " + chargePerLine + " , " + count
				+ " , " + SCXmlUtil.getString(orderLineElement) + " , " + SCXmlUtil.getString(headerExtn));

		return lineChargeElement;
	}

	// totalWeightedValue for header
	private Double getTotalWeightedValue(Element orderElement) {
		Double totalWeightedValue = 0.0;
		NodeList orderLineNodes = orderElement.getElementsByTagName(CrocsXmlConstants.E_ORDER_LINE);

		for (int i = 0; i < orderLineNodes.getLength(); i++) {
			Element orderLineElement = (Element) orderLineNodes.item(i);
			if (orderLineElement != null) {
				String orderedQtyStr = orderLineElement.getAttribute(CrocsXmlConstants.A_ORDERED_QTY);
				int orderQty = (int) Double.parseDouble(orderedQtyStr);

				// Check if the orderedQtyStr is not empty and is a valid integer
				if (orderedQtyStr != null && !orderedQtyStr.isEmpty()) {

					Element linePrineInfoEle = SCXmlUtil.getChildElement(orderLineElement, CrocsConstant.A_LINE_PRICE_INFO);
					String strUnitPrice = linePrineInfoEle.getAttribute(CrocsConstant.A_UNIT_PRICE);
					if(isKoreaOrder) {
						strUnitPrice = getSubString(strUnitPrice);
					}				
					Double doubleUnitPrice = Double.parseDouble(strUnitPrice);
					totalWeightedValue += orderQty * doubleUnitPrice;

				}
			}
		}
		logger.verbose("CrocsHeaderChargesIterationToLine:  totalWeightedValue " + totalWeightedValue);

		return totalWeightedValue;
	}

	/**
	 * Calculates the total weighted value for a specific tax across all order lines in the given order.
	 * 
	 * <p>This method iterates through each order line in the provided {@code orderElement}, checks if the
	 * line contains the specified tax (by {@code taxName}), and if so, calculates a weighted value by 
	 * multiplying the ordered quantity with the unit price. The result is stored in 
	 * {@code totalWeightedValueTaxMap}, and the relevant order lines are collected in 
	 * {@code orderLineListTaxMap}.</p>
	 *
	 * @param orderElement               The XML element representing the order.
	 * @param taxName                    The name of the tax to filter and calculate weighted value for.
	 * @param totalWeightedValueTaxMap  A map to store the total weighted value for the given tax name.
	 * @param orderLineListTaxMap       A map to store order lines associated with the given tax name.
	 */

	private void getTotalWeightedValueForTax(Element orderElement, String taxName,
			Map<String, Double> totalWeightedValueTaxMap, Map<String, List<Element>> orderLineListTaxMap) {
		Double totalWeightedValue = 0.0;
		NodeList orderLineNodes = orderElement.getElementsByTagName(CrocsXmlConstants.E_ORDER_LINE);
		List<Element> orderLineNodesWithSalesTax = new ArrayList<>();
		for (int i = 0; i < orderLineNodes.getLength(); i++) {
			Element orderLineElement = (Element) orderLineNodes.item(i);
			if (orderLineElement != null) {
				String orderedQtyStr = orderLineElement.getAttribute(CrocsXmlConstants.A_ORDERED_QTY);
				int orderQty = (int) Double.parseDouble(orderedQtyStr);

				// Check if the orderedQtyStr is not empty and is a valid integer
				if (orderedQtyStr != null && !orderedQtyStr.isEmpty()) {

					Element lineTaxesEle = SCXmlUtil.getXpathElement(orderLineElement,
							"LineTaxes/LineTax[@TaxName='" + taxName + "' and @Tax!=0.0]");
					if (!YFCCommon.isVoid(lineTaxesEle)) {
						Element linePrineInfoEle = SCXmlUtil.getChildElement(orderLineElement,
								CrocsConstant.A_LINE_PRICE_INFO);
						String strUnitPrice = linePrineInfoEle.getAttribute(CrocsConstant.A_UNIT_PRICE);
						if(isKoreaOrder) {
							strUnitPrice = getSubString(strUnitPrice);
						}
						Double doubleUnitPrice = Double.parseDouble(strUnitPrice);
						totalWeightedValue += orderQty * doubleUnitPrice;
						orderLineNodesWithSalesTax.add(orderLineElement);
					}

				}
			}
		}
		logger.verbose("CrocsHeaderChargesIterationToLine:  totalWeightedValue " + totalWeightedValue);
		totalWeightedValueTaxMap.put(taxName, totalWeightedValue);
		orderLineListTaxMap.put(taxName, orderLineNodesWithSalesTax);

	}

	// round to two Decimals for charges
	private double roundToTwoDecimals(double value) {
		return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}

	// round to three Decimals for charges
	private double roundToThreeDecimals(double value) {
		return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).doubleValue();
	}

	public Element copyAttributes(Element sourceElement, Element targetElement) {
		// Get all attributes of the source element
		NamedNodeMap attributes = sourceElement.getAttributes();

		// Loop through each attribute of the source element
		for (int i = 0; i < attributes.getLength(); i++) {
			Node attribute = attributes.item(i);

			// Copy the attribute to the target element
			targetElement.setAttribute(attribute.getNodeName(), attribute.getNodeValue());

		}
		logger.verbose("CrocsHeaderChargesIterationToLine:  copyAttributes " + SCXmlUtil.getString(targetElement));
		return targetElement;
	}

	/**
	 * Filters the HeaderCharge elements based on ChargeCategory.
	 */
	public Document filterHeaderCharges(Document document) {
		NodeList headerChargeNodes = document.getElementsByTagName(CrocsXmlConstants.E_HEADER_CHARGE);

		// Iterate and remove unwanted HeaderCharges
		for (int i = 0; i < headerChargeNodes.getLength(); i++) {

			Element headerChargeElement = (Element) headerChargeNodes.item(i);

			if (!YFCCommon.isVoid(headerChargeElement)) {
				String strChargeName = headerChargeElement.getAttribute(CrocsXmlConstants.A_CHARGE_NAME);

				if (!isValidCharge(strChargeName)) {
					logger.verbose("Skipping the charges: " + strChargeName);

				} else {
					removeNode(headerChargeElement);
					i--; // Adjust index due to node removal

				}
			}
		}

		return document;
	}

	// to check if charge name is valid
	public boolean isValidCharge(String strChargeName) {
		return (!strChargeName.equals(CrocsXmlConstants.A_SHIPPING_CHARGE)
				&& !strChargeName.equals(CrocsXmlConstants.A_SHIPPING_DISCOUNT)
				&& !strChargeName.equalsIgnoreCase(CrocsConstant.A_EXTENDED_SHIPPING_PROTECTION));
	}

	// Helper method to remove the node from its parent
	public void removeNode(Element node) {
		node.getParentNode().removeChild(node);
	}

	/**
	 * Filters the HeaderTax elements based on TaxName starting with "Shipping".
	 */
	public Document filterHeaderTaxes(Document document) {
		NodeList headerTaxNodes = document.getElementsByTagName(CrocsXmlConstants.E_HEADER_TAX);

		// Iterate and remove unwanted HeaderTaxes
		for (int i = 0; i < headerTaxNodes.getLength(); i++) {
			Element headerTaxElement = (Element) headerTaxNodes.item(i);
			if (!YFCCommon.isVoid(headerTaxElement)) {
				String taxName = headerTaxElement.getAttribute(CrocsXmlConstants.A_TAX_NAME);

				// Remove unwanted elements where TaxName does not start with "Shipping"
				if (shouldRemoveTax(taxName)) {
					headerTaxElement.getParentNode().removeChild(headerTaxElement);
					i--; // Adjust the index as we've removed a node
				} else {
					logger.verbose("Skipping the taxes" + taxName);
				}

			}

		}

		return document;
	}

	public boolean shouldRemoveTax(String taxName) {
		return taxName == null || !taxName.startsWith("Ship");
	}
	// Replace the comma in the UnitPrice for Korea 
	private String getSubString(String unitPrice) {
		unitPrice = unitPrice.replaceAll(",", "");
		return unitPrice;
	}
}
