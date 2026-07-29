package com.crocs.oms.util.ue;

import java.util.ArrayList;
import java.util.List;

import com.crocs.oms.common.util.CrocsConstant;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSExtnLineTaxCalculationInputStruct;
import com.yantra.yfs.japi.YFSExtnTaxBreakup;
import com.yantra.yfs.japi.YFSExtnTaxCalculationOutStruct;
import com.yantra.yfs.japi.YFSUserExitException;
import com.yantra.yfs.japi.ue.YFSRecalculateLineTaxUE;

public class CrocsRecalculateROLineTaxUE implements YFSRecalculateLineTaxUE {

	/**
	 * EOMS - 809 Tax Call Implementation //Java code executes for return orders,
	 * transferring taxes from the input to the output structure
	 * 
	 * * Input: com.yantra.yfs.japi.YFSExtnLineTaxCalculationInputStruct is
	 * &orderHeaderKey=20250520043143388958&orderLineKey=20250520043155388959&bForInvoice=true&hasPendingChanges=false&bLastInvoiceForOrderLine=true&bForPreSettlement=false&bForPacklistPrice=false&sShipNode=1005&itemId=11016-6MB-M12&unitPrice=54.99&currentUnitPrice=54.99&originalUnitPrice=0.0&invoicedPricingQty=1.0&lineQty=3.0&currentQty=2.0&invoicedQty=0.0&shipToId=&shipToCity=Tempe&shipToState=AZ&shipToZipCode=85282&shipToCountry=US&purpose=&enterpriseCode=CROCS_US&documentType=0003&taxpayerId=&taxJurisdiction=&taxExemptionCertificate=&taxExemptFlag=N&colCharge=[&chargeCategory=PromotionDiscount&chargeName=PromotionDiscount1&chargePerUnit=0.0&chargePerLine=6.67&chargeAmount=6.67&invoicedPerLine=0.0&invoicedExtended=0.0&reference=null&eleExtendedFields=<?xml
	 * version="1.0" encoding="UTF-8"?> <LineCharge> <Extn/> </LineCharge>
	 * ,&chargeCategory=Discount&chargeName=PromotionDiscount&chargePerUnit=0.0&chargePerLine=13.33&chargeAmount=13.33&invoicedPerLine=0.0&invoicedExtended=0.0&reference=null&eleExtendedFields=<?xml
	 * version="1.0" encoding="UTF-8"?> <LineCharge> <Extn/> </LineCharge>
	 * ]&colTax=[&chargeCategory=&chargeName=&taxName=SalesTax&taxableFlag=null&taxPercentage=0.081&tax=10.94&reference1=&reference2=&reference3=&invoicedTax=3.65&totalInvoicedCharge=0.0&totalCurrentCharge=0.0&eleExtendedFields=<?xml
	 * version="1.0" encoding="UTF-8"?> <TaxBreakup/>
	 * ]&totaloptionprice=0.0&orderedQty=2.0&invoiceKey=null&totalShippingCharges=0.0&totalHandlingCharges=0.0&totalPersonalizeCharges=0.0&tax=10.94&taxPercentage=0.0&invoiceMode=RETURN&eMemo=null&orderHeaderKey=null&orderLineKey=null&bForInvoice=false&bForPreSettlement=false&bForPacklistPrice=false&sShipNode=null&itemId=null&unitPrice=0.0&lineQty=0.0&currentQty=0.0&invoicedQty=0.0&shipToId=null&shipToCity=null&shipToState=null&shipToZipCode=null&shipToCountry=null&purpose=null&enterpriseCode=null&documentType=null&taxpayerId=null&taxJurisdiction=null&taxExemptionCertificate=null&taxExemptFlag=null&colCharge=null&colTax=null&orderedQty=0.0&invoiceKey=null&totalShippingCharges=0.0&totalHandlingCharges=0.0&totalPersonalizeCharges=0.0&tax=0.0&taxPercentage=0.0
	 * 
	 * 
	 * Output :
	 * &colTax=[&chargeCategory=&chargeName=&taxName=SalesTax&taxableFlag=null&taxPercentage=0.081&tax=7.289999999999999&reference1=&reference2=&reference3=&invoicedTax=3.65&totalInvoicedCharge=0.0&totalCurrentCharge=0.0&eleExtendedFields=<?xml
	 * version="1.0" encoding="UTF-8"?> <TaxBreakup/>
	 * ]&tax=7.289999999999999&taxPercentage=0.0
	 * 
	 * 
	 **/

	// Logger instance for this class
	private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsRecalculateROLineTaxUE.class);

	/**
	 * This method recalculates line-level taxes for a return invoice scenario.
	 * It first checks whether tax needs to be recalculated and then delegates to helper methods accordingly.
	 */
	@Override
	public YFSExtnTaxCalculationOutStruct recalculateLineTax(YFSEnvironment env,
	        YFSExtnLineTaxCalculationInputStruct lineTaxInputArgs) throws YFSUserExitException {

	    logger.verbose("CrocsRecalculateROLineTaxUE - recalculateLineTax - Start");

	    YFSExtnTaxCalculationOutStruct outStruct = new YFSExtnTaxCalculationOutStruct();
	    outStruct.colTax = lineTaxInputArgs.colTax;

	    // Check if this is a return invoice and needs recalculation
	    if (shouldRecalculate(lineTaxInputArgs)) {
	        logger.verbose(String.format("Line Invoice: %s :::::: Mode: %s", 
	                lineTaxInputArgs.bForInvoice, lineTaxInputArgs.invoiceMode));

	        // Calculate taxes for return invoice
	        outStruct = calculateReturnInvoiceTax(lineTaxInputArgs);
	    }

	    logger.verbose("CrocsRecalculateROLineTaxUE - recalculateLineTax - End");
	    return outStruct;
	}

	/**
	 * Determines whether tax recalculation should happen for this invoice.
	 * Only recalculates if it's for an invoice and the invoice mode is RETURN.
	 *
	 * @param input The input tax calculation structure
	 * @return true if recalculation should be done, false otherwise
	 */
	private boolean shouldRecalculate(YFSExtnLineTaxCalculationInputStruct input) {
	    return input.bForInvoice && CrocsConstant.A_RETURN_INVOICE_MODE.equalsIgnoreCase(input.invoiceMode);
	}

	/**
	 * Handles return invoice tax calculation logic based on whether it is the last invoice
	 * for the order line or not. Uses two different paths to handle prorated vs. remaining tax.
	 *
	 * @param input Tax input structure
	 * @return The output structure with calculated taxes
	 */
	private YFSExtnTaxCalculationOutStruct calculateReturnInvoiceTax(YFSExtnLineTaxCalculationInputStruct input) {
	    YFSExtnTaxCalculationOutStruct outStruct = new YFSExtnTaxCalculationOutStruct();
	    List<YFSExtnTaxBreakup> invoiceTaxes = new ArrayList<>();
	    double totalProratedLineTax = 0.0;

	    List<YFSExtnTaxBreakup> orderLineTaxes = input.colTax;

	    if (orderLineTaxes != null && !orderLineTaxes.isEmpty()) {
	        logger.verbose("lineTaxInputArgs.bLastInvoiceForOrderLine : " + input.bLastInvoiceForOrderLine);
	        
	        // Final invoice — use remaining unapplied tax
	        if (input.bLastInvoiceForOrderLine) {
	            totalProratedLineTax = processLastInvoiceTaxes(orderLineTaxes, invoiceTaxes);
	        } 
	        // Partial invoice — calculate prorated tax based on current quantity
	        else {
	            totalProratedLineTax = processProratedTaxes(orderLineTaxes, invoiceTaxes, input.currentQty, input.lineQty);
	        }
	    }

	    outStruct.colTax = invoiceTaxes;
	    outStruct.tax = totalProratedLineTax;
	    return outStruct;
	}

	/**
	 * Calculates taxes for the final invoice of the order line.
	 * Applies the remaining tax (total - invoiced) to the invoice.
	 *
	 * @param orderLineTaxes Existing tax details
	 * @param invoiceTaxes Collection to hold updated taxes
	 * @return Total remaining tax amount to be invoiced
	 */
	private double processLastInvoiceTaxes(List<YFSExtnTaxBreakup> orderLineTaxes, List<YFSExtnTaxBreakup> invoiceTaxes) {
	    double total = 0.0;
	    for (YFSExtnTaxBreakup taxBreakup : orderLineTaxes) {
	        double remainingTax = taxBreakup.tax - taxBreakup.invoicedTax;
	        taxBreakup.tax = remainingTax;
	        total += remainingTax;
	        invoiceTaxes.add(taxBreakup);
	    }
	    return total;
	}

	/**
	 * Calculates prorated tax for partial quantity being invoiced.
	 * Uses the formula: (tax / lineQty) * currentQty.
	 *
	 * @param orderLineTaxes Tax details for the full line
	 * @param invoiceTaxes Output list of recalculated tax breakups
	 * @param currentQty Quantity being invoiced in current call
	 * @param lineQty Total line quantity
	 * @return Total prorated tax amount
	 */
	private double processProratedTaxes(List<YFSExtnTaxBreakup> orderLineTaxes, List<YFSExtnTaxBreakup> invoiceTaxes,
	                                    double currentQty, double lineQty) {
	    double total = 0.0;

	    if (lineQty == 0) {
	        logger.error("Line quantity is zero. Skipping prorated tax calculation.");
	        return total;
	    }

	    for (YFSExtnTaxBreakup taxBreakup : orderLineTaxes) {
	        double unitTax = Math.round((taxBreakup.tax / lineQty) * 100.0) / 100.0;
	        double proratedTax = currentQty * unitTax;
	        taxBreakup.tax = proratedTax;
	        total += proratedTax;
	        invoiceTaxes.add(taxBreakup);
	    }
	    return total;
	}



}
