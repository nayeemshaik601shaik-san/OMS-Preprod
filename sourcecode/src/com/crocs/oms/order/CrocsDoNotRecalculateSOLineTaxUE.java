package com.crocs.oms.order;

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
 * This class is created for testing purpose.
 * This class will not make any call to vertex for tax recalculation.
 */
public class CrocsDoNotRecalculateSOLineTaxUE implements YFSRecalculateLineTaxUE{
	
	private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsDoNotRecalculateSOLineTaxUE.class);


	@Override
	public YFSExtnTaxCalculationOutStruct recalculateLineTax(YFSEnvironment arg0,
			YFSExtnLineTaxCalculationInputStruct lineTaxInputArgs) throws YFSUserExitException {
		YFSExtnTaxCalculationOutStruct outStruct = new YFSExtnTaxCalculationOutStruct();
		outStruct.colTax = lineTaxInputArgs.colTax;
		outStruct.tax = lineTaxInputArgs.tax;
		outStruct.taxPercentage = lineTaxInputArgs.taxPercentage;
		
		 if (lineTaxInputArgs.bForInvoice && CrocsConstant.A_INVOICE_MODE.equalsIgnoreCase(lineTaxInputArgs.invoiceMode)) {
				logger.verbose("Line Invoice" + lineTaxInputArgs.bForInvoice + "::::::" + lineTaxInputArgs.invoiceMode);
				Double totalProratedLineTax = 0.0;
				ArrayList<YFSExtnTaxBreakup> invoiceTaxes = new ArrayList<>();
				if (lineTaxInputArgs.colTax != null) {
					List<YFSExtnTaxBreakup> orderLineTaxes = lineTaxInputArgs.colTax;
				logger.verbose("lineTaxInputArgs.bLastInvoiceForOrderLine : "+lineTaxInputArgs.bLastInvoiceForOrderLine);
					if (lineTaxInputArgs.bLastInvoiceForOrderLine) {
						for (YFSExtnTaxBreakup extnTaxBreakup : orderLineTaxes) {
							Double invoicedTax = extnTaxBreakup.invoicedTax;
							Double remainingProratedTax = extnTaxBreakup.tax - invoicedTax;
							totalProratedLineTax = totalProratedLineTax + remainingProratedTax;
							extnTaxBreakup.tax = remainingProratedTax.doubleValue();
							invoiceTaxes.add(extnTaxBreakup);
						}
					} else {
						for (YFSExtnTaxBreakup extnTaxBreakup : orderLineTaxes) {
							Double currentQty = lineTaxInputArgs.currentQty;
							Double tax = extnTaxBreakup.tax;
							Double lineQty = lineTaxInputArgs.lineQty;
							Double unitTax = Math.round((tax / lineQty) * 100.0) / 100.0;
							Double lineTax = currentQty * unitTax;
							totalProratedLineTax = totalProratedLineTax + lineTax;
							extnTaxBreakup.tax = lineTax.doubleValue();
							invoiceTaxes.add(extnTaxBreakup);
						}
					}
				}
				outStruct.colTax = invoiceTaxes;
				outStruct.tax = totalProratedLineTax.doubleValue();
				return outStruct;
			}
		return outStruct;
	}
}
