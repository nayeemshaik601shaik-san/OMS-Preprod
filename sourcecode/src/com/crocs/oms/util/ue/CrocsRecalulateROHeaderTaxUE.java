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
 * EOMS- 809 Tax Call Implementation Java code executes for return orders,
 * transferring taxes from the input to the output structure
 * 
 * Input: com.yantra.yfs.japi.YFSExtnHeaderTaxCalculationInputStruct is
 * &orderHeaderKey=20250520043143388958&bForInvoice=true&bForPacklistPrice=false&bLastInvoice=true&invoiceKey=null&sShipNode=1005&shipToId=&shipToCity=Tempe&shipToState=AZ&shipToZipCode=85282&shipToCountry=US&purpose=&enterpriseCode=CROCS_US&documentType=0003&taxpayerId=&taxJurisdiction=&taxExemptionCertificate=&taxExemptFlag=N&colCharge=[&chargeCategory=ShippingCharge&chargeName=ShippingCharge&chargeAmount=40.0&invoicedAmount=0.0&reference=null&eleExtendedFields=<?xml
 * version="1.0" encoding="UTF-8"?> <HeaderCharge> <Extn/> </HeaderCharge>
 * ,&chargeCategory=ShippingDiscount&chargeName=ShippingDiscount&chargeAmount=12.0&invoicedAmount=0.0&reference=null&eleExtendedFields=<?xml
 * version="1.0" encoding="UTF-8"?> <HeaderCharge> <Extn/> </HeaderCharge>
 * ]&colTax=[&chargeCategory=ShippingCharge&chargeName=ShippingCharge&taxName=ShippingTax&taxableFlag=null&taxPercentage=0.081&tax=3.24&reference1=&reference2=&reference3=&invoicedTax=0.0&totalInvoicedCharge=0.0&totalCurrentCharge=40.0&eleExtendedFields=<?xml
 * version="1.0" encoding="UTF-8"?> <TaxBreakup/>
 * ,&chargeCategory=ShippingDiscount&chargeName=ShippingDiscount&taxName=ShippingTax&taxableFlag=null&taxPercentage=0.081&tax=0.97&reference1=&reference2=&reference3=&invoicedTax=0.0&totalInvoicedCharge=0.0&totalCurrentCharge=12.0&eleExtendedFields=<?xml
 * version="1.0" encoding="UTF-8"?> <TaxBreakup/>
 * ]&headerShippingCharges=0.0&headerHandlingCharges=0.0&headerPersonalizeCharges=0.0&discountAmount=0.0&hasPendingChanges=false&tax=0.0&taxPercentage=0.0&invoiceMode=RETURN&eMemo=<?xml
 * version="1.0" encoding="UTF-8"?> <YFSRecalculateHeaderTaxUEInput> <eMemo/>
 * </YFSRecalculateHeaderTaxUEInput>
 * &totalOriginalChargeAmount=0.0&totalCurrentChargeAmount=0.0
 * 
 * 
 * Output:
 * &colTax=[&chargeCategory=ShippingCharge&chargeName=ShippingCharge&taxName=ShippingTax&taxableFlag=null&taxPercentage=0.081&tax=3.24&reference1=&reference2=&reference3=&invoicedTax=0.0&totalInvoicedCharge=0.0&totalCurrentCharge=40.0&eleExtendedFields=<?xml
 * version="1.0" encoding="UTF-8"?> <TaxBreakup/>
 * ,&chargeCategory=ShippingDiscount&chargeName=ShippingDiscount&taxName=ShippingTax&taxableFlag=null&taxPercentage=0.081&tax=0.97&reference1=&reference2=&reference3=&invoicedTax=0.0&totalInvoicedCharge=0.0&totalCurrentCharge=12.0&eleExtendedFields=<?xml
 * version="1.0" encoding="UTF-8"?> <TaxBreakup/> ]&tax=4.21&taxPercentage=0.0
 * 
 */
public class CrocsRecalulateROHeaderTaxUE implements YFSRecalculateHeaderTaxUE {

	private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsRecalulateROHeaderTaxUE.class);

	@Override
	public YFSExtnTaxCalculationOutStruct recalculateHeaderTax(YFSEnvironment env,
			YFSExtnHeaderTaxCalculationInputStruct headerTaxInputArgs) throws YFSUserExitException {
		logger.verbose("CrocsRecalulateROHeaderTaxUE : recalculateHeaderTax Start");
		YFSExtnTaxCalculationOutStruct outStruct = new YFSExtnTaxCalculationOutStruct();
		outStruct.colTax = headerTaxInputArgs.colTax;

		 //for the Return Invoice the Header Taxes should go in Last Invoice
		logger.verbose("RETURN INVOICE " + headerTaxInputArgs.bForInvoice + "  " + headerTaxInputArgs.invoiceMode + headerTaxInputArgs.bLastInvoice );
		if (headerTaxInputArgs.bForInvoice
				&& CrocsConstant.A_RETURN_INVOICE_MODE.equalsIgnoreCase(headerTaxInputArgs.invoiceMode)) {			
			BigDecimal totalProratedHeaderTax = new BigDecimal("0.0");
			List<YFSExtnTaxBreakup> invoiceTaxes = new ArrayList<>();
			if (headerTaxInputArgs.colTax != null && headerTaxInputArgs.bLastInvoice
					&& headerTaxInputArgs.documentType.equals(CrocsConstant.A_RETURN_ORDER_DOCUMENT_TYPE)) {
				List<YFSExtnTaxBreakup> orderHeaderTaxes = headerTaxInputArgs.colTax;
				for (YFSExtnTaxBreakup extnTaxBreakup : orderHeaderTaxes) {
					BigDecimal invoicedTax = BigDecimal.valueOf(extnTaxBreakup.invoicedTax);
					BigDecimal remainingProratedTax = BigDecimal.valueOf(extnTaxBreakup.tax).subtract(invoicedTax);
					totalProratedHeaderTax = totalProratedHeaderTax.add(remainingProratedTax);
					extnTaxBreakup.tax = remainingProratedTax.doubleValue();
					invoiceTaxes.add(extnTaxBreakup);
				}
			}

			outStruct.colTax = invoiceTaxes;
			outStruct.tax = totalProratedHeaderTax.doubleValue();
			return outStruct;
		}
		logger.verbose("CrocsRecalulateROHeaderTaxUE : recalculateHeaderTax End");
		return outStruct;
	}
}
