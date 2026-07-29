package com.crocs.oms.order;

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
 * This class is created for testing purpose.
 * This class will not make any call to vertex for tax recalculation.
 */
public class CrocsDoNotRecalculateSOHeaderTaxUE implements YFSRecalculateHeaderTaxUE{
	
	private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsDoNotRecalculateSOHeaderTaxUE.class);

	@Override
	public YFSExtnTaxCalculationOutStruct recalculateHeaderTax(YFSEnvironment arg0,
			YFSExtnHeaderTaxCalculationInputStruct headerTaxInputArgs) throws YFSUserExitException {
		YFSExtnTaxCalculationOutStruct outStruct = new YFSExtnTaxCalculationOutStruct();
		outStruct.colTax = headerTaxInputArgs.colTax;
		outStruct.tax=headerTaxInputArgs.tax;
		outStruct.taxPercentage=headerTaxInputArgs.taxPercentage;	
		if(headerTaxInputArgs.bForInvoice&& CrocsConstant.A_INVOICE_MODE.equalsIgnoreCase(headerTaxInputArgs.invoiceMode)) {
			logger.verbose("SHIPMENT INVOICE" + headerTaxInputArgs.bForInvoice + "  "+ headerTaxInputArgs.invoiceMode);
			BigDecimal totalProratedHeaderTax = new BigDecimal("0.0");
			List<YFSExtnTaxBreakup> invoiceTaxes = new ArrayList<>();
			if (headerTaxInputArgs.colTax != null) {
				List<YFSExtnTaxBreakup> orderHeaderTaxes = headerTaxInputArgs.colTax;
				for (YFSExtnTaxBreakup extnTaxBreakup : orderHeaderTaxes) {
					BigDecimal invoicedTax = BigDecimal.valueOf(extnTaxBreakup.invoicedTax);
					BigDecimal remainingProratedTax = BigDecimal.valueOf(extnTaxBreakup.tax).subtract(invoicedTax);
					totalProratedHeaderTax = totalProratedHeaderTax.add(remainingProratedTax);
					extnTaxBreakup.tax = remainingProratedTax.doubleValue();
					invoiceTaxes.add(extnTaxBreakup);
				}
			}
			outStruct.colTax=invoiceTaxes;
			outStruct.tax = totalProratedHeaderTax.doubleValue();
			return outStruct;
		}

		return outStruct;
	}
}
