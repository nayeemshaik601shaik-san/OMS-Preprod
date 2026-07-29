package com.crocs.oms.util.ue;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.order.CrocsGetVertexOrderDetails;
import com.ibm.icu.math.BigDecimal;
import com.yantra.yfc.core.YFCIterable;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCException;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSExtnHeaderTaxCalculationInputStruct;
import com.yantra.yfs.japi.YFSExtnTaxBreakup;
import com.yantra.yfs.japi.YFSExtnTaxCalculationOutStruct;
import com.yantra.yfs.japi.YFSUserExitException;
import com.yantra.yfs.japi.ue.YFSRecalculateHeaderTaxUE;

/**
 * 
 * * EOMS - 809 Tax Call Implementation Recalculates the sales order line tax
 * using Vertex tax calculations.
 * 
 * Sample Input to UE :
 * com.yantra.yfs.japi.YFSExtnHeaderTaxCalculationInputStruct is
 * &orderHeaderKey=20250318120119114521&bForInvoice=false&bForPacklistPrice=false&bLastInvoice=false&invoiceKey=null&sShipNode=null&shipToId=&shipToCity=New
 * York&shipToState=NY&shipToZipCode=10003&shipToCountry=US&purpose=&enterpriseCode=CROCS_US&documentType=0001&taxpayerId=&taxJurisdiction=&taxExemptionCertificate=&taxExemptFlag=N&colCharge=[&chargeCategory=ShippingCharge&chargeName=ShippingCharge&chargeAmount=10.0&invoicedAmount=0.0&reference=10.00&eleExtendedFields=<?xml
 * version="1.0" encoding="UTF-8"?> <HeaderCharge/>
 * ,&chargeCategory=Discount&chargeName=ShippingDiscount&chargeAmount=5.0&invoicedAmount=0.0&reference=&eleExtendedFields=<?xml
 * version="1.0" encoding="UTF-8"?> <HeaderCharge/>
 * ]&colTax=[&chargeCategory=&chargeName=&taxName=SalesTax&taxableFlag=null&taxPercentage=0.13&tax=0.65&reference1=&reference2=0.07299&reference3=&invoicedTax=0.0&totalInvoicedCharge=0.0&totalCurrentCharge=0.0&eleExtendedFields=<?xml
 * version="1.0" encoding="UTF-8"?> <TaxBreakup/>
 * ]&headerShippingCharges=0.0&headerHandlingCharges=0.0&headerPersonalizeCharges=0.0&discountAmount=0.0&hasPendingChanges=false&tax=0.0&taxPercentage=0.0&invoiceMode=null&eMemo=null&totalOriginalChargeAmount=0.0&totalCurrentChargeAmount=0.0
 * 
 * Sample Output of UE: com.yantra.yfs.japi.ue.YFSRecalculateHeaderTaxUE is
 * :&colTax=[&chargeCategory=null&chargeName=null&taxName=SalesTax&taxableFlag=null&taxPercentage=0.08875&tax=0.45&reference1=null&reference2=0.13&reference3=null&invoicedTax=0.0&totalInvoicedCharge=0.0&totalCurrentCharge=0.0&eleExtendedFields=null]&tax=0.0&taxPercentage=0.0
 */
/**
 * 
 */
public class CrocsRecalculateSOHeaderTaxUE implements YFSRecalculateHeaderTaxUE {

	// Logger instance for debugging and logging information
	private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsRecalculateSOHeaderTaxUE.class);

	

	/**
	 * Recalculates header-level tax for sales orders based on enterprise code, invoice mode, 
	 * and order details retrieved from Vertex and order system.
	 *
	 * @param env - environment object
	 * @param headerTaxInputArgs - input struct containing header tax data and context
	 * @return tax calculation output struct with recalculated taxes
	 * @throws YFSUserExitException in case of tax recalculation failure
	 */
	@Override
	public YFSExtnTaxCalculationOutStruct recalculateHeaderTax(YFSEnvironment env,
	        YFSExtnHeaderTaxCalculationInputStruct headerTaxInputArgs) throws YFSUserExitException {

	    logger.verbose("CrocsRecalculateSOHeaderTaxUE : recalculateHeaderTax Start");

	    YFSExtnTaxCalculationOutStruct outStruct = new YFSExtnTaxCalculationOutStruct();
	    List<YFSExtnTaxBreakup> headerTaxInputList = headerTaxInputArgs.colTax;
	    outStruct.colTax = headerTaxInputList;

	    try {
	        Document orderDetailsDoc = fetchOrderDetails(env, headerTaxInputArgs.orderHeaderKey);

	        if (!isValidOrderDetails(orderDetailsDoc)) {
	            return outStruct; // Early exit for invalid order conditions
	        }

	        Element eleOrder = (Element) orderDetailsDoc.getDocumentElement()
	                .getElementsByTagName(CrocsXmlConstants.E_ORDER).item(0);

	        // Skip migration orders except call center entry types
	        if (isMigrationOrder(eleOrder)) {
	            return outStruct;
	        }

	        // Handle tax recalculation based on enterprise and invoice conditions
	        if (shouldProcessVertexTax(headerTaxInputArgs)) {
	            return processVertexTax(env, headerTaxInputArgs, orderDetailsDoc, headerTaxInputList);
	        } else if (isInvoiceMode(headerTaxInputArgs)) {
	            return processShipmentInvoiceTax(headerTaxInputArgs);
	        }

	        logger.verbose("CrocsRecalculateSOHeaderTaxUE: recalculateHeaderTax End");
	        return outStruct;

	    } catch (Exception e) {
	        logger.error("Exception during tax recalculation: ", e);
	        throw new YFCException(e, "Exception during tax recalculation");
	    }
	}

	/**
	 * Fetch order details document using orderHeaderKey.
	 */
	private Document fetchOrderDetails(YFSEnvironment env, String orderHeaderKey)  {
	    if (!YFCObject.isVoid(orderHeaderKey)) {
	        return CrocsGetVertexOrderDetails.fetchOrderAndTaxDetails(env, orderHeaderKey);
	    }
	    return null;
	}

	/**
	 * Validates if order details document and required elements are present and non-empty.
	 */
	private boolean isValidOrderDetails(Document orderDetailsDoc) {
	    if (orderDetailsDoc == null || YFCCommon.isVoid(orderDetailsDoc)) {
	        return false;
	    }

	    Element eleOrderList = orderDetailsDoc.getDocumentElement();
	    String strOrderCount = eleOrderList.getAttribute(CrocsXmlConstants.A_TOTAL_ORDER_LIST);

	    Element eleOrderLines = (Element) orderDetailsDoc.getElementsByTagName(CrocsXmlConstants.E_ORDER_LINE).item(0);
	    Element elePersonShipToInfo = (Element) orderDetailsDoc.getElementsByTagName(CrocsXmlConstants.E_PERSON_INFO_SHIP_TO)
	            .item(0);

	    return !(CrocsConstant.STR_ZERO.equals(strOrderCount) || eleOrderLines == null || YFCCommon.isVoid(eleOrderLines)
	            || elePersonShipToInfo == null || YFCCommon.isVoid(elePersonShipToInfo));
	}

	/**
	 * Checks if the order is a migration order that should be skipped.
	 */

	private boolean isMigrationOrder(Element eleOrder) {
	
		/*By-passing vertex call only in case of migration order, and EntryType is call center as when EntryType is call center
		 *it means there is return created from call center for imported SO. 
		 **/

		logger.verbose("isMigrationOrder being true will bypass the tax call, value of boolean is isMigrationOrder :- " +
				(eleOrder != null && CrocsConstant.V_MIGRATION.equals(eleOrder.getAttribute(CrocsConstant.A_ENTERED_BY))
				&& !CrocsConstant.ENTRY_TYPE_CALL_CENTER.equals(eleOrder.getAttribute(CrocsConstant.A_ENTRY_TYPE))));
		
	    return CrocsConstant.V_MIGRATION.equals(eleOrder.getAttribute(CrocsConstant.A_ENTERED_BY))
	            && !CrocsConstant.ENTRY_TYPE_CALL_CENTER.equals(eleOrder.getAttribute(CrocsConstant.A_ENTRY_TYPE));
	}

	/**
	 * Determines if Vertex tax processing should be done based on enterprise code and invoice flag.
	 */
	private boolean shouldProcessVertexTax(YFSExtnHeaderTaxCalculationInputStruct input) {
	    return (CrocsConstant.CROCS_US.equalsIgnoreCase(input.enterpriseCode)
	            || CrocsConstant.CROCS_CA.equalsIgnoreCase(input.enterpriseCode)) && !input.bForInvoice;
	}

	/**
	 * Checks if input is for invoice return mode.
	 */
	private boolean isInvoiceMode(YFSExtnHeaderTaxCalculationInputStruct input) {
	    return input.bForInvoice && CrocsConstant.A_INVOICE_MODE.equalsIgnoreCase(input.invoiceMode);
	}

	/**
	 * Processes Vertex tax for US or CA enterprises.
	 */
	private YFSExtnTaxCalculationOutStruct processVertexTax(YFSEnvironment env,
	        YFSExtnHeaderTaxCalculationInputStruct input, Document orderDetailsDoc,
	        List<YFSExtnTaxBreakup> headerTaxInputList)   {

	    YFSExtnTaxCalculationOutStruct outStruct = new YFSExtnTaxCalculationOutStruct();

	    Document vertexOutput = CrocsGetVertexOrderDetails.fetchVetexOuput(env, orderDetailsDoc);
	    if (vertexOutput == null) {
	        return outStruct;
	    }

	    YFCIterable<YFCElement> taxLineItems = CrocsGetVertexOrderDetails.extractLineTaxItem(vertexOutput);
	    if (taxLineItems == null) {
	        logger.warn("No matching Tax Line Item found for OrderHeaderKey: " + input.orderHeaderKey);
	        return outStruct;
	    }

	    List<YFCElement> shippingTaxItems = filterShippingTaxItems(taxLineItems);

	    if (CrocsConstant.CROCS_US.equalsIgnoreCase(input.enterpriseCode)) {
	        outStruct.colTax = processUSTax(shippingTaxItems, headerTaxInputList);
	    } else if (CrocsConstant.CROCS_CA.equalsIgnoreCase(input.enterpriseCode)) {
	        outStruct.colTax = processCATax(shippingTaxItems, headerTaxInputList);
	    }
	    return outStruct;
	}

	/**
	 * Filters shipping related tax items from the Vertex tax line items.
	 */
	private List<YFCElement> filterShippingTaxItems(YFCIterable<YFCElement> taxLineItems) {
	    List<YFCElement> shippingItems = new ArrayList<>();
	    for (YFCElement item : taxLineItems) {
	        String productClass = item.getChildElement(CrocsConstant.V_PRODUCT).getAttribute(CrocsConstant.V_PRODUCT_CLASS);
	        if (CrocsConstant.A_SHIPPING_TAX_CODE.equalsIgnoreCase(productClass)
	                || CrocsConstant.A_SHIPPING_EXTENDED_TAX_CODE.equalsIgnoreCase(productClass)) {
	            logger.verbose("Shipping Tax Line Item: " + item.toString());
	            shippingItems.add(item);
	        }
	    }
	    return shippingItems;
	}

	/**
	 * Processes tax recalculation for invoice return mode by prorating the remaining tax.
	 */
	private YFSExtnTaxCalculationOutStruct processShipmentInvoiceTax(YFSExtnHeaderTaxCalculationInputStruct input) {
	    YFSExtnTaxCalculationOutStruct outStruct = new YFSExtnTaxCalculationOutStruct();
	    BigDecimal totalProratedHeaderTax = BigDecimal.ZERO;
	    List<YFSExtnTaxBreakup> invoiceTaxes = new ArrayList<>();
	    List<YFSExtnTaxBreakup> orderHeaderTaxes = input.colTax;

	    if (input.colTax != null) {
	        for (YFSExtnTaxBreakup extnTaxBreakup : orderHeaderTaxes) {
	            BigDecimal invoicedTax = BigDecimal.valueOf(extnTaxBreakup.invoicedTax);
	            BigDecimal remainingTax = BigDecimal.valueOf(extnTaxBreakup.tax).subtract(invoicedTax);
	            extnTaxBreakup.tax = remainingTax.doubleValue();
	            totalProratedHeaderTax = totalProratedHeaderTax.add(remainingTax);
	            invoiceTaxes.add(extnTaxBreakup);
	        }
	    }

	    outStruct.colTax = invoiceTaxes;
	    outStruct.tax = totalProratedHeaderTax.doubleValue();
	    return outStruct;
	}


	/**
	 * @param vertexShippingLineItemList
	 * @param headerTaxInputList
	 * @return List<YFSExtnTaxBreakup> Calculates U.S. header taxes for Shipping
	 *         Charge, Shipping Discount, and Extended Shipping Protection charges.
	 */
	private List<YFSExtnTaxBreakup> processUSTax(List<YFCElement> vertexShippingLineItemList,
			List<YFSExtnTaxBreakup> headerTaxInputList) {

		logger.verbose("CrocsRecalculateSOHeaderTaxUE: processUSTax Start of the Method");

		List<YFSExtnTaxBreakup> taxOutputList = new ArrayList<>();

		for (YFCElement vertexShippingtaxLineItem : vertexShippingLineItemList) {

			logger.verbose("CrocsRecalculateSOHeaderTaxUE: processUSTax " + vertexShippingLineItemList.toString());

			double newTaxPercentage = CrocsGetVertexOrderDetails.computeNewTaxPercentage(vertexShippingtaxLineItem);

			YFSExtnTaxBreakup tax = new YFSExtnTaxBreakup();
			String strVProductName = vertexShippingtaxLineItem.getChildElement(CrocsConstant.V_PRODUCT).getNodeValue();
			logger.verbose("CrocsRecalculateSOHeaderTaxUE: processUSTax - strVProductName" + strVProductName);
			if (!YFCObject.isVoid(strVProductName)
					&& (strVProductName.equalsIgnoreCase(CrocsXmlConstants.A_SHIPPING_DISCOUNT)
							|| strVProductName.equalsIgnoreCase(CrocsXmlConstants.A_SHIPPING_CHARGE)
							|| strVProductName.equalsIgnoreCase(CrocsConstant.A_EXTENDED_SHIPPING_PROTECTION))) {

				tax.chargeCategory = strVProductName;
				tax.chargeName = strVProductName;
			}

			// Extract old tax details
			double[] taxDetails = extractOldTaxDetails(headerTaxInputList, strVProductName);
			tax.taxPercentage = newTaxPercentage;
			tax.taxName = CrocsConstant.A_SHIPPING_TAX;
			if (!YFCObject.isVoid(strVProductName)
					&& strVProductName.equalsIgnoreCase(CrocsConstant.A_EXTENDED_SHIPPING_PROTECTION)) {
				tax.taxName = CrocsConstant.A_SHIP_PROTECTION_TAX;
			}
			double taxValue = Double
					.parseDouble(vertexShippingtaxLineItem.getChildElement(CrocsConstant.V_TOTAL_TAX).getNodeValue());
			tax.tax = Math.abs(taxValue);
			tax.reference1 = String.valueOf(taxDetails[0]);
			tax.reference2 = String.valueOf(taxDetails[1]);
			taxOutputList.add(tax);
			logger.verbose("CrocsRecalculateSOHeaderTaxUE : processUSTax" + tax.chargeCategory + ", " + tax.chargeName
					+ tax.taxPercentage + ", " + tax.taxName + tax.reference1 + ", " + tax.reference2);
		}

		return taxOutputList;
	}

	/**
	 * @param headerTaxInputList
	 * @param strChargeName
	 * @return new double[] { oldTax, oldTaxPercentage } Extract old tax details
	 *         from the header tax list
	 */
	private double[] extractOldTaxDetails(List<YFSExtnTaxBreakup> headerTaxInputList, String strChargeName) {
		double oldTax = 0.00;
		double oldTaxPercentage = 0.00;
		logger.verbose("CrocsRecalculateSOHeaderTaxUE : extractOldTaxDetails - start" + strChargeName);

		if (!YFCObject.isVoid(headerTaxInputList) && !headerTaxInputList.isEmpty()
				&& !YFCObject.isVoid(strChargeName)) {
			for (YFSExtnTaxBreakup inputTaxLine : headerTaxInputList) {
				String chargeName = inputTaxLine.chargeName;
				if (strChargeName.equalsIgnoreCase(chargeName)) {
					logger.verbose("CrocsRecalculateSOHeaderTaxUE : extractOldTaxDetails - if condition" + chargeName);
					oldTax = inputTaxLine.tax;
					oldTaxPercentage = inputTaxLine.taxPercentage;
					break; // Exit early after finding the match
				}
			}
		}
		logger.verbose("CrocsRecalculateSOHeaderTaxUE : extractOldTaxDetails - End" + oldTax + ", " + oldTaxPercentage);
		return new double[] { oldTax, oldTaxPercentage };
	}

	/**
	 * @param vertexShippingLineItemList
	 * @param headerTaxInputList
	 * @return List<YFSExtnTaxBreakup> Calculates CA Header Tax Output
	 */
	private List<YFSExtnTaxBreakup> processCATax(List<YFCElement> vertexShippingLineItemList,
			List<YFSExtnTaxBreakup> headerTaxInputList) {

		logger.verbose("CrocsRecalculateSOHeaderTaxUE: processCATax Start of the Method");
		List<YFSExtnTaxBreakup> taxOutputList = new ArrayList<>();

		for (YFCElement lineItem : vertexShippingLineItemList) {
			String strVProductName = lineItem.getChildElement(CrocsConstant.V_PRODUCT).getNodeValue();
			logger.verbose("CrocsRecalculateSOHeaderTaxUE: processCATax - strVProductName" + strVProductName);
			lineItem.getChildren(CrocsConstant.V_TAXES).forEach(tax -> {
				YFSExtnTaxBreakup taxBreakup = new YFSExtnTaxBreakup();
				if (!YFCObject.isVoid(strVProductName)
						&& (strVProductName.equalsIgnoreCase(CrocsXmlConstants.A_SHIPPING_DISCOUNT)
								|| strVProductName.equalsIgnoreCase(CrocsXmlConstants.A_SHIPPING_CHARGE)
								|| strVProductName.equalsIgnoreCase(CrocsConstant.A_EXTENDED_SHIPPING_PROTECTION))) {
					taxBreakup.chargeCategory = strVProductName;
					taxBreakup.chargeName = strVProductName;
				}
				if (CrocsConstant.V_TAXABLE.equals(tax.getAttribute(CrocsConstant.V_TAX_RESULT))) {
					taxBreakup.taxName = getCATaxName(tax.getChildElement(CrocsConstant.V_IMPOSITION).getNodeValue());
					if (!YFCObject.isVoid(strVProductName)
							&& strVProductName.equalsIgnoreCase(CrocsConstant.A_EXTENDED_SHIPPING_PROTECTION)) {
						taxBreakup.taxName = getCATaxNameForExtendedShipProtection(
								tax.getChildElement(CrocsConstant.V_IMPOSITION).getNodeValue());
					}
					taxBreakup.taxPercentage = Double
							.parseDouble(tax.getChildElement(CrocsConstant.V_EFFECTIVE_RATE).getNodeValue());
					double taxValue = Double
							.parseDouble(tax.getChildElement(CrocsConstant.V_CALCULATED_TAX).getNodeValue());
					taxBreakup.tax = Math.abs(taxValue);
					double[] taxDetails = CrocsGetVertexOrderDetails.getCAOldTaxPercentageForHeader(headerTaxInputList,
							taxBreakup.taxName, strVProductName);
					taxBreakup.reference1 = String.valueOf(taxDetails[0]);
					taxBreakup.reference2 = String.valueOf(taxDetails[1]);
					taxOutputList.add(taxBreakup);
					logger.verbose("CrocsRecalculateSOHeaderTaxUE : processCATax" + taxBreakup.chargeCategory + ", "
							+ taxBreakup.chargeName + taxBreakup.taxPercentage + ", " + taxBreakup.taxName
							+ taxBreakup.reference1 + ", " + taxBreakup.reference2);
				}
			});
		}
		return taxOutputList;
	}

	/**
	 * @param imposition
	 * @return String fetches CA Header Tax Names
	 */
	private String getCATaxName(String imposition) {
		switch (imposition) {
		case CrocsConstant.V_IMPOSITION_GST_HST:
			return CrocsConstant.A_SHIPPING_GST_HST_TAX;
		case CrocsConstant.V_IMPOSITION_PST:
			return CrocsConstant.A_SHIPPING_PST_TAX;
		default:
			return CrocsConstant.A_SHIPPING_TAX;
		}
	}

	/**
	 * @param imposition
	 * @return String fetches CA Extended Ship Protection Header Tax Names
	 */
	private String getCATaxNameForExtendedShipProtection(String imposition) {
		switch (imposition) {
		case CrocsConstant.V_IMPOSITION_GST_HST:
			return CrocsConstant.A_SHIP_PROTECTION_GST_HST_TAX;
		case CrocsConstant.V_IMPOSITION_PST:
			return CrocsConstant.A_SHIP_PROTECTION_PST_TAX;
		default:
			return CrocsConstant.A_SHIP_PROTECTION_TAX;
		}
	}

}