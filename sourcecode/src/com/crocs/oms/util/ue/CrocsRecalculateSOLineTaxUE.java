package com.crocs.oms.util.ue;

import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.crocs.oms.order.CrocsGetVertexOrderDetails;
import com.yantra.yfc.core.YFCIterable;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCException;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSExtnLineTaxCalculationInputStruct;
import com.yantra.yfs.japi.YFSExtnTaxBreakup;
import com.yantra.yfs.japi.YFSExtnTaxCalculationOutStruct;
import com.yantra.yfs.japi.YFSUserExitException;
import com.yantra.yfs.japi.ue.YFSRecalculateLineTaxUE;

/**
 ** EOMS - 809 Tax Call Implementation Recalculates the sales order line tax
 * Recalculates the sales order line tax using Vertex tax calculations. Sample
 * Input to UE: com.yantra.yfs.japi.YFSExtnLineTaxCalculationInputStruct is
 * &orderHeaderKey=20250318120119114521&orderLineKey=20250318120120114523&bForInvoice=false&hasPendingChanges=false&bLastInvoiceForOrderLine=false&bForPreSettlement=false&bForPacklistPrice=false&sShipNode=null&itemId=11016-6MB-M5W7&unitPrice=210.0&currentUnitPrice=0.0&originalUnitPrice=0.0&invoicedPricingQty=0.0&lineQty=3.0&currentQty=3.0&invoicedQty=0.0&shipToId=&shipToCity=New
 * York&shipToState=NY&shipToZipCode=10003&shipToCountry=US&purpose=&enterpriseCode=CROCS_US&documentType=0001&taxpayerId=&taxJurisdiction=&taxExemptionCertificate=&taxExemptFlag=N&colCharge=[&chargeCategory=Discount&chargeName=PromotionDiscount&chargePerUnit=0.0&chargePerLine=25.0&chargeAmount=25.0&invoicedPerLine=0.0&invoicedExtended=0.0&reference=25.00&eleExtendedFields=<?xml
 * version="1.0" encoding="UTF-8"?> <LineCharge/>
 * ]&colTax=[&chargeCategory=&chargeName=&taxName=SalesTax&taxableFlag=null&taxPercentage=0.13&tax=78.65&reference1=0.13&reference2=0.07299&reference3=&invoicedTax=0.0&totalInvoicedCharge=0.0&totalCurrentCharge=0.0&eleExtendedFields=<?xml
 * version="1.0" encoding="UTF-8"?> <TaxBreakup/>
 * ]&totaloptionprice=0.0&orderedQty=3.0&invoiceKey=null&totalShippingCharges=0.0&totalHandlingCharges=0.0&totalPersonalizeCharges=0.0&tax=0.0&taxPercentage=0.0&invoiceMode=null&eMemo=null&orderHeaderKey=null&orderLineKey=null&bForInvoice=false&bForPreSettlement=false&bForPacklistPrice=false&sShipNode=null&itemId=null&unitPrice=0.0&lineQty=0.0&currentQty=0.0&invoicedQty=0.0&shipToId=null&shipToCity=null&shipToState=null&shipToZipCode=null&shipToCountry=null&purpose=null&enterpriseCode=null&documentType=null&taxpayerId=null&taxJurisdiction=null&taxExemptionCertificate=null&taxExemptFlag=null&colCharge=null&colTax=null&orderedQty=0.0&invoiceKey=null&totalShippingCharges=0.0&totalHandlingCharges=0.0&totalPersonalizeCharges=0.0&tax=0.0&taxPercentage=0.0
 * Sample Output to UE: com.yantra.yfs.japi.ue.YFSRecalculateLineTaxUE is
 * :&colTax=[&chargeCategory=null&chargeName=null&taxName=SalesTax&taxableFlag=null&taxPercentage=0.08875&tax=53.69&reference1=0.08875&reference2=0.13&reference3=null&invoicedTax=0.0&totalInvoicedCharge=0.0&totalCurrentCharge=0.0&eleExtendedFields=null]&tax=0.0&taxPercentage=0.0
 */
public class CrocsRecalculateSOLineTaxUE implements YFSRecalculateLineTaxUE {

	private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsRecalculateSOLineTaxUE.class);

	@Override

	public YFSExtnTaxCalculationOutStruct recalculateLineTax(YFSEnvironment env,
			YFSExtnLineTaxCalculationInputStruct lineTaxInputArgs) throws YFSUserExitException {

		logger.verbose("CrocsRecalculateSOLineTaxUE : recalculateLineTax Start");

		YFSExtnTaxCalculationOutStruct outStruct = new YFSExtnTaxCalculationOutStruct();
		outStruct.colTax = lineTaxInputArgs.colTax;
		String strOrderHeaderKey = lineTaxInputArgs.orderHeaderKey;

		try {
			if (YFCCommon.isVoid(strOrderHeaderKey))
				return outStruct;

			Document orderDetails = CrocsGetVertexOrderDetails.fetchOrderAndTaxDetails(env, strOrderHeaderKey);
			if (!isValidOrderDetails(orderDetails))
				return outStruct;

			Element eleOrder = (Element) orderDetails.getDocumentElement()
					.getElementsByTagName(CrocsXmlConstants.E_ORDER).item(0);
			if (isMigrationOrder(eleOrder))
				return outStruct;

			if (isEnterpriseTaxApplicable(lineTaxInputArgs) && !lineTaxInputArgs.bForInvoice) {
				return handleVertexTaxCalculation(env, orderDetails, lineTaxInputArgs);
			}

			if (lineTaxInputArgs.bForInvoice
					&& CrocsConstant.A_INVOICE_MODE.equalsIgnoreCase(lineTaxInputArgs.invoiceMode)) {
				return handleInvoiceProratedTax(lineTaxInputArgs, outStruct);
			}

			return outStruct;
		} catch (Exception e) {
			logger.error("Exception during tax recalculation", e);
			throw new YFCException(e, "Exception during tax recalculation");
		} finally {
			logger.verbose("CrocsRecalculateSOLineTaxUE: recalculateLineTax End");
		}
	}

	private boolean isValidOrderDetails(Document doc) {
		if (doc == null || YFCCommon.isVoid(doc))
			return false;
		Element root = doc.getDocumentElement();
		String orderCount = root.getAttribute(CrocsXmlConstants.A_TOTAL_ORDER_LIST);
		if (CrocsConstant.STR_ZERO.equals(orderCount))
			return false;

		return doc.getElementsByTagName(CrocsXmlConstants.E_ORDER_LINE).item(0) != null
				&& doc.getElementsByTagName(CrocsXmlConstants.E_PERSON_INFO_SHIP_TO).item(0) != null;
	}

	private boolean isMigrationOrder(Element eleOrder) {
		
		/* By-passing vertex call only in case of migration order, and EntryType is call center as when EntryType is call center 
		 * it means there is return created from call center for imported SO.
		 * */
		logger.verbose("isMigrationOrder being true will bypass the tax call, value of boolean is isMigrationOrder :- " + (eleOrder != null
				&& CrocsConstant.V_MIGRATION.equals(eleOrder.getAttribute(CrocsConstant.A_ENTERED_BY))
				&& !CrocsConstant.ENTRY_TYPE_CALL_CENTER.equals(eleOrder.getAttribute(CrocsConstant.A_ENTRY_TYPE))));

		return eleOrder != null && CrocsConstant.V_MIGRATION.equals(eleOrder.getAttribute(CrocsConstant.A_ENTERED_BY))
				&& !CrocsConstant.ENTRY_TYPE_CALL_CENTER.equals(eleOrder.getAttribute(CrocsConstant.A_ENTRY_TYPE));
	}

	private boolean isEnterpriseTaxApplicable(YFSExtnLineTaxCalculationInputStruct input) {
		return CrocsConstant.CROCS_US.equalsIgnoreCase(input.enterpriseCode)
				|| CrocsConstant.CROCS_CA.equalsIgnoreCase(input.enterpriseCode);
	}

	private YFSExtnTaxCalculationOutStruct handleVertexTaxCalculation(YFSEnvironment env, Document orderDetails,
			YFSExtnLineTaxCalculationInputStruct inputArgs) throws TransformerException {

		YFSExtnTaxCalculationOutStruct outStruct = new YFSExtnTaxCalculationOutStruct();
		outStruct.colTax = inputArgs.colTax;

		Document vertexOutput = CrocsGetVertexOrderDetails.fetchVetexOuput(env, orderDetails);
		if (vertexOutput == null || YFCCommon.isVoid(vertexOutput))
			return outStruct;

		YFCIterable<YFCElement> taxItems = CrocsGetVertexOrderDetails.extractLineTaxItem(vertexOutput);
		Element lineElement = XMLUtil.getElementByXPath(orderDetails,
				"/OrderList/Order/OrderLines/OrderLine[@OrderLineKey='" + inputArgs.orderLineKey + "']");

		YFCElement taxDetails = fetchLineTaxDetails(taxItems, lineElement);
		if (CrocsConstant.CROCS_US.equalsIgnoreCase(inputArgs.enterpriseCode) && !YFCCommon.isVoid(taxDetails)) {
			outStruct.colTax = processLineUSTax(taxDetails, inputArgs.colTax);
		}
		if (CrocsConstant.CROCS_CA.equalsIgnoreCase(inputArgs.enterpriseCode) && !YFCCommon.isVoid(taxDetails)) {
			outStruct.colTax = processLineCATax(taxDetails, inputArgs.colTax);
		}

		return outStruct;
	}

	private YFSExtnTaxCalculationOutStruct handleInvoiceProratedTax(YFSExtnLineTaxCalculationInputStruct input,
			YFSExtnTaxCalculationOutStruct outStruct) {

		double totalTax = 0.0;
		List<YFSExtnTaxBreakup> proratedTaxes = new ArrayList<>();

		List<YFSExtnTaxBreakup> orderLineTaxes = input.colTax;

		if (!YFCCommon.isVoid(orderLineTaxes)) {
			for (YFSExtnTaxBreakup tax : orderLineTaxes) {
				double calculatedTax = calculateProratedTax(tax, input);
				tax.tax = calculatedTax;
				totalTax += calculatedTax;
				proratedTaxes.add(tax);
			}
		}

		outStruct.colTax = proratedTaxes;
		outStruct.tax = totalTax;
		return outStruct;
	}

	private double calculateProratedTax(YFSExtnTaxBreakup tax, YFSExtnLineTaxCalculationInputStruct input) {
		if (input.bLastInvoiceForOrderLine) {
			return tax.tax - tax.invoicedTax;
		}
		double unitTax = Math.round((tax.tax / input.lineQty) * 100.0) / 100.0;
		return input.currentQty * unitTax;
	}

	/**
	 * @param lineTaxes
	 * @param lineTaxInputList
	 * @return List<YFSExtnTaxBreakup> Calculates CA Line LeveL Taxes
	 */
	private List<YFSExtnTaxBreakup> processLineCATax(YFCElement lineTaxes, List<YFSExtnTaxBreakup> lineTaxInputList) {

		List<YFSExtnTaxBreakup> taxOutputList = new ArrayList<>();
		if (lineTaxes == null || lineTaxInputList == null) {
			logger.warn("processLineCATax: lineTaxes or lineTaxInputList is null.");
			return taxOutputList;
		}
		
		lineTaxes.getChildren(CrocsConstant.V_TAXES).forEach(tax -> {
			if (CrocsConstant.V_TAXABLE.equals(tax.getAttribute(CrocsConstant.V_TAX_RESULT))) {
				YFSExtnTaxBreakup taxBreakup = new YFSExtnTaxBreakup();
				taxBreakup.taxName = getCALineTaxName(tax.getChildElement(CrocsConstant.V_IMPOSITION).getNodeValue());
				taxBreakup.taxPercentage = Double
						.parseDouble(tax.getChildElement(CrocsConstant.V_EFFECTIVE_RATE).getNodeValue());
				taxBreakup.tax = Double.parseDouble(tax.getChildElement(CrocsConstant.V_CALCULATED_TAX).getNodeValue());
				taxBreakup.reference1 = tax.getChildElement(CrocsConstant.V_EFFECTIVE_RATE).getNodeValue();
				taxBreakup.reference2 = CrocsGetVertexOrderDetails.getCAOldTaxPercentage(lineTaxInputList,
						taxBreakup.taxName);

				taxOutputList.add(taxBreakup);
			}
		});
		logger.verbose("CrocsRecalculateSOLineTaxUE :processLineCATax" + taxOutputList.toString());
		return taxOutputList;

	}

	/**
	 * @param imposition
	 * @return String fetches CA Line Level Tax Names
	 */
	private String getCALineTaxName(String imposition) {
		switch (imposition) {
		case CrocsConstant.V_IMPOSITION_GST_HST:
			return CrocsConstant.A_GST_HST_TAX;
		case CrocsConstant.V_IMPOSITION_PST:
			return CrocsConstant.A_PST_TAX;
		default:
			return CrocsConstant.A_SALES_TAX;
		}
	}

	/**
	 * @param lineTaxes
	 * @param lineTaxInputList
	 * @return List<YFSExtnTaxBreakup> Calculates US Line LeveL Taxes
	 */
	private List<YFSExtnTaxBreakup> processLineUSTax(YFCElement lineTaxes, List<YFSExtnTaxBreakup> lineTaxInputList) {

		List<YFSExtnTaxBreakup> taxOutputList = new ArrayList<>();

		// Extract old tax details
		String[] taxDetails = extractLineOldTaxDetails(lineTaxInputList);
		String oldTaxName = taxDetails[0];
		String oldTaxPercentage = taxDetails[1];

		if (lineTaxes != null) {
			double newTaxPercentage = CrocsGetVertexOrderDetails.computeNewTaxPercentage(lineTaxes);
			YFSExtnTaxBreakup tax = new YFSExtnTaxBreakup();
			tax.taxPercentage = newTaxPercentage;
			tax.taxName = oldTaxName;
			tax.tax = Double.parseDouble(lineTaxes.getChildElement(CrocsConstant.V_TOTAL_TAX).getNodeValue());
			tax.reference2 = oldTaxPercentage;
			taxOutputList.add(tax);
		}

		logger.verbose("CrocsRecalculateSOLineTaxUE :processLineUSTax" + taxOutputList.toString());
		return taxOutputList;

	}

	/**
	 * @param taxLineItemList
	 * @param eleOrderLine
	 * @return YFCElement Calculates US and CA Soap LineItem Elements
	 */
	private YFCElement fetchLineTaxDetails(YFCIterable<YFCElement> taxLineItemList, Element eleOrderLine) {

		Element eleItemDet = (Element) eleOrderLine.getElementsByTagName(CrocsXmlConstants.E_ITEM_DETAILS).item(0);
		Element eleClassification = (Element) eleItemDet.getElementsByTagName(CrocsXmlConstants.E_CLASSIFICATION_CODES)
				.item(0);
		String strItemId = eleItemDet.getAttribute(CrocsXmlConstants.A_ITEM_ID);
		String strTaxCode = eleClassification.getAttribute(CrocsXmlConstants.A_TAX_PRODUCT_CODE);
		String strPrimeLineNo = eleOrderLine.getAttribute(CrocsXmlConstants.A_PRIME_LINE_NO);
		YFCElement taxLineItem = null;

		logger.verbose("CrocsRecalculateSOLineTaxUE :fetchLineTaxDetails" + strItemId + "," + strTaxCode + ","
				+ strPrimeLineNo);

		for (YFCElement lineItem : taxLineItemList) {
			if (strPrimeLineNo.equalsIgnoreCase(lineItem.getAttribute(CrocsConstant.V_LINE_ITEM_NUMBER))
					&& strItemId.equalsIgnoreCase(lineItem.getChildElement(CrocsConstant.V_PRODUCT).getNodeValue())
					&& strTaxCode.equalsIgnoreCase(lineItem.getChildElement(CrocsConstant.V_PRODUCT)
							.getAttribute(CrocsConstant.V_PRODUCT_CLASS))) {
				taxLineItem = lineItem;
				logger.verbose("CrocsRecalculateSOLineTaxUE :fetchLineTaxDetails-lineItem" + lineItem.toString());
				break;
			}
		}
		if (taxLineItem != null) {
			logger.verbose("CrocsRecalculateSOLineTaxUE :fetchLineTaxDetails-taxLineItem" + taxLineItem.toString());
		}

		return taxLineItem;
	}

	/**
	 * @param lineTaxInputList
	 * @return String[] { CrocsConstant.A_SALES_TAX, "0" } Get US oldTaxpercentage
	 */
	private String[] extractLineOldTaxDetails(List<YFSExtnTaxBreakup> lineTaxInputList) {

		if (lineTaxInputList != null && !lineTaxInputList.isEmpty()) {
			YFSExtnTaxBreakup inputTax = lineTaxInputList.get(0);

			String oldtaxPercentage = String.valueOf(inputTax.taxPercentage);
			logger.verbose("CrocsRecalculateSOLineTaxUE :extractLineOldTaxDetails" + inputTax.taxName + ","
					+ oldtaxPercentage);
			return new String[] { inputTax.taxName, oldtaxPercentage };
		}

		logger.verbose("CrocsRecalculateSOLineTaxUE :extractLineOldTaxDetails" + CrocsConstant.A_SALES_TAX + "," + 0);
		return new String[] { CrocsConstant.A_SALES_TAX, "0" };

	}

}
