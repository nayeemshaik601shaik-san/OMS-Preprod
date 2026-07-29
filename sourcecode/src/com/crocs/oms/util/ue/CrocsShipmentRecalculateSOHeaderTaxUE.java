package com.crocs.oms.util.ue;

import java.util.ArrayList;
import java.util.List;
import com.crocs.oms.common.util.CrocsConstant;
import com.ibm.icu.math.BigDecimal;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSExtnHeaderTaxCalculationInputStruct;
import com.yantra.yfs.japi.YFSExtnTaxBreakup;
import com.yantra.yfs.japi.YFSExtnTaxCalculationOutStruct;
import com.yantra.yfs.japi.YFSUserExitException;
import com.yantra.yfs.japi.ue.YFSRecalculateHeaderTaxUE;

/**
 * This User Exit implementation is used for testing tax calculation.
 * It does not invoke external tax services like Vertex.
 */
public class CrocsShipmentRecalculateSOHeaderTaxUE implements YFSRecalculateHeaderTaxUE {

    private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsShipmentRecalculateSOHeaderTaxUE.class);

    /**
     * Recalculates the header-level tax based on input arguments.
     * If the calculation is for shipment invoice mode, it calculates the prorated remaining tax.
     * Otherwise, it simply copies the tax fields from the input.
     *
     * @param env YFS environment
     * @param headerTaxInputArgs input structure containing tax calculation parameters
     * @return output structure containing recalculated tax data
     * @throws YFSUserExitException in case of any error
     */
    @Override
    public YFSExtnTaxCalculationOutStruct recalculateHeaderTax(YFSEnvironment env,
                                                                YFSExtnHeaderTaxCalculationInputStruct headerTaxInputArgs)
            throws YFSUserExitException {

        YFSExtnTaxCalculationOutStruct outStruct = new YFSExtnTaxCalculationOutStruct();

        // Default assignment of tax data
        outStruct.colTax = headerTaxInputArgs.colTax;
        outStruct.tax = headerTaxInputArgs.tax;
        outStruct.taxPercentage = headerTaxInputArgs.taxPercentage;

        // Special processing for Shipment Invoice mode
        if (headerTaxInputArgs.bForInvoice && CrocsConstant.A_INVOICE_MODE.equalsIgnoreCase(headerTaxInputArgs.invoiceMode)) {

            logger.verbose("Processing SHIPMENT INVOICE: bForInvoice = " + headerTaxInputArgs.bForInvoice
                    + ", invoiceMode = " + headerTaxInputArgs.invoiceMode);

            BigDecimal totalProratedHeaderTax = new BigDecimal("0.0");
            List<YFSExtnTaxBreakup> invoiceTaxes = new ArrayList<>();
            List<YFSExtnTaxBreakup> orderHeaderTaxes = headerTaxInputArgs.colTax;

            if (headerTaxInputArgs.colTax != null) {
                for (YFSExtnTaxBreakup extnTaxBreakup : orderHeaderTaxes) {

                    BigDecimal invoicedTax = BigDecimal.valueOf(extnTaxBreakup.invoicedTax);
                    BigDecimal totalTax = BigDecimal.valueOf(extnTaxBreakup.tax);
                    BigDecimal remainingProratedTax = totalTax.subtract(invoicedTax);

                    // Update total header-level prorated tax
                    totalProratedHeaderTax = totalProratedHeaderTax.add(remainingProratedTax);

                    // Update individual tax entry
                    extnTaxBreakup.tax = remainingProratedTax.doubleValue();
                    invoiceTaxes.add(extnTaxBreakup);
                }
            }

            outStruct.colTax = invoiceTaxes;
            outStruct.tax = totalProratedHeaderTax.doubleValue();
        }

        return outStruct;
    }
}
