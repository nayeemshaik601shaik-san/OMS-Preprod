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

/**
 *  EOMS- 3240 Vertex tax total in the SOPA response does not match the order total in Call center
 * This User Exit class recalculates line-level taxes for invoice scenarios.
 * It avoids external tax engine calls and instead handles prorated tax logic internally.
 */
public class CrocsShipmentRecalculateSOLineTaxUE implements YFSRecalculateLineTaxUE {

    private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsShipmentRecalculateSOLineTaxUE.class);

    @Override
    public YFSExtnTaxCalculationOutStruct recalculateLineTax(YFSEnvironment env,
            YFSExtnLineTaxCalculationInputStruct lineTaxInputArgs) throws YFSUserExitException {

        YFSExtnTaxCalculationOutStruct outStruct = initializeOutStruct(lineTaxInputArgs);

        if (shouldRecalculateForInvoice(lineTaxInputArgs)) {
            logger.verbose("Line Invoice Mode: " + lineTaxInputArgs.bForInvoice + " :::: " + lineTaxInputArgs.invoiceMode);

            List<YFSExtnTaxBreakup> invoiceTaxes = new ArrayList<>();
            double totalProratedLineTax = calculateProratedTaxes(lineTaxInputArgs, invoiceTaxes);

            outStruct.colTax = invoiceTaxes;
            outStruct.tax = totalProratedLineTax;
        }

        return outStruct;
    }

    /**
     * Initializes the output structure with default values.
     */
    private YFSExtnTaxCalculationOutStruct initializeOutStruct(YFSExtnLineTaxCalculationInputStruct input) {
        YFSExtnTaxCalculationOutStruct outStruct = new YFSExtnTaxCalculationOutStruct();
        outStruct.colTax = input.colTax;
        outStruct.tax = input.tax;
        outStruct.taxPercentage = input.taxPercentage;
        return outStruct;
    }

    /**
     * Checks if the input qualifies for invoice-based recalculation.
     */
    private boolean shouldRecalculateForInvoice(YFSExtnLineTaxCalculationInputStruct input) {
        return input.bForInvoice && CrocsConstant.A_INVOICE_MODE.equalsIgnoreCase(input.invoiceMode);
    }

    /**
     * Calculates prorated tax values either for the last invoice or intermediate ones.
     */
    private double calculateProratedTaxes(YFSExtnLineTaxCalculationInputStruct input, List<YFSExtnTaxBreakup> invoiceTaxes) {
        double totalTax = 0.0;
        
        List<YFSExtnTaxBreakup> orderLineTaxes = input.colTax;

        if (orderLineTaxes == null) return totalTax;

        logger.verbose("bLastInvoiceForOrderLine: " + input.bLastInvoiceForOrderLine);

        for (YFSExtnTaxBreakup taxBreakup : orderLineTaxes) {
            double proratedTax = input.bLastInvoiceForOrderLine
                    ? calculateRemainingTax(taxBreakup)
                    : calculateTaxForCurrentQty(taxBreakup, input.currentQty, input.lineQty);

            taxBreakup.tax = proratedTax;
            totalTax += proratedTax;
            invoiceTaxes.add(taxBreakup);
        }

        return totalTax;
    }

    /**
     * Calculates the remaining tax for the last invoice.
     */
    private double calculateRemainingTax(YFSExtnTaxBreakup taxBreakup) {
        return taxBreakup.tax - taxBreakup.invoicedTax;
    }

    /**
     * Prorates the tax for the current quantity (used in non-final invoices).
     */
    private double calculateTaxForCurrentQty(YFSExtnTaxBreakup taxBreakup, double currentQty, double totalQty) {
        double unitTax = Math.round((taxBreakup.tax / totalQty) * 100.0) / 100.0;
        return currentQty * unitTax;
    }
}
