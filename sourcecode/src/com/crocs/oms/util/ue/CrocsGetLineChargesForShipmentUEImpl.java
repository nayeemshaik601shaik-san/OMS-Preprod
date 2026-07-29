package com.crocs.oms.util.ue;

import java.util.ArrayList;
import java.util.List;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import com.yantra.yfs.japi.YFSExtnInputLineChargesShipment;
import com.yantra.yfs.japi.YFSExtnLineChargeStruct;
import com.yantra.yfs.japi.YFSExtnOutputLineChargesShipment;
import com.yantra.yfs.japi.YFSUserExitException;
import com.yantra.yfs.japi.ue.YFSGetLineChargesForShipmentUE;

/**
 * EOMS - 3896 - Shipment Invoicing : Line charges proration. Prorate the line
 * charges based on the quantity shipped in the invoices.
 * 
 * com.yantra.yfs.japi.YFSExtnInputLineChargesShipment is
 * &orderHeaderKey=20250516100407263507&orderReleaseNo=0&firstShippableRelease=false&bLastInvoiceForOrderLine=false&orderLineKey=20250516100407263509&orderHeaderSCAC=&orderLineSCAC=&actualSCAC=&personalizeCode=&actualFreightChargeFlag=N&orderLineOrderedQty=5.0&shipmentQty=1.0&previousShippedQty=1.0&pricingQty=1.0&pricingUOM=EACH&actualFreightCharge=0.0&orderLineCharges=[&chargeCategory=Discount&chargeName=PromotionDiscount&chargePerUnit=0.0&chargePerLine=25.0&chargeAmount=25.0&invoicedPerLine=0.0&invoicedExtended=0.0&reference=&eleExtendedFields=<?xml
 * version="1.0" encoding="UTF-8"?> <LineCharge> <Extn/> </LineCharge>
 * ]&proformaLineCharges=null&orderLineShippingPerUnit=0.0&orderLineShippingPerLine=0.0&orderLineTotalShippingAmount=0.0&orderLinePersonalizePerUnit=0.0&orderLinePersonalizePerLine=0.0&orderLineTotalPersonalizeAmount=0.0&orderLineHandlingPerUnit=0.0&orderLineHandlingPerLine=0.0&orderLineTotalHandlingAmount=0.0&previousShippingCharges=0.0&previousPersonalizeCharges=0.0&previousHandlingCharges=0.0&shipmentNo=100000330&shipmentKey=20250516100617263552&shipNode=1005&sellerOrgCode=CROCS_US
 * 
 * output: Output from UserExit
 * com.yantra.yfs.japi.ue.YFSGetLineChargesForShipmentUE is
 * :&newLineCharges=[&chargeCategory=Discount&chargeName=PromotionDiscount&chargePerUnit=0.0&chargePerLine=5.0&chargeAmount=5.0&invoicedPerLine=0.0&invoicedExtended=0.0&reference=&eleExtendedFields=null]&adjustLineDiscount=false&newShippingCharges=0.0&newPersonalizeCharges=0.0&newHandlingCharges=0.0
 * [system]: [86b67785-6c0c-4ab4-b39a-ffb23b18834e]: [ ]: OMPUserExitProx
 */
public class CrocsGetLineChargesForShipmentUEImpl implements YFSGetLineChargesForShipmentUE {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsGetLineChargesForShipmentUEImpl.class);

	@Override
	public YFSExtnOutputLineChargesShipment getLineChargesForShipment(YFSEnvironment arg0,
			YFSExtnInputLineChargesShipment lineChargeShipmentArg) throws YFSUserExitException {

		logger.verbose("CrocsGetLineChargesForShipmentUEImpl Start of the Method :  Start");
		YFSExtnOutputLineChargesShipment outputStruct = new YFSExtnOutputLineChargesShipment();
		ArrayList<YFSExtnLineChargeStruct> newLineChargesList = new ArrayList<>();
		try {
			String strOrderHeaderKey = lineChargeShipmentArg.orderHeaderKey;
			double strShippedQty = lineChargeShipmentArg.shipmentQty;
			double strOrginalQty = lineChargeShipmentArg.orderLineOrderedQty;
			boolean strLastShipment = lineChargeShipmentArg.bLastInvoiceForOrderLine;

			// Adding LineCharges to Output
			if (strOrderHeaderKey != null && !strOrderHeaderKey.equals("")) {
				List<YFSExtnLineChargeStruct> chargeList = lineChargeShipmentArg.orderLineCharges;
				for (YFSExtnLineChargeStruct lineCharge : chargeList) {
					YFSExtnLineChargeStruct newLineCharge = new YFSExtnLineChargeStruct();
					newLineCharge.chargeCategory = lineCharge.chargeCategory;
					newLineCharge.chargeName = lineCharge.chargeName;
					newLineCharge.reference = lineCharge.reference;
					double dChargePerLine = lineCharge.chargePerLine;
					if (strLastShipment) {
						dChargePerLine = dChargePerLine - lineCharge.invoicedPerLine;
					} else {

						double strPerUnit = Math.round((dChargePerLine / strOrginalQty) * 100.0) / 100.0;
						dChargePerLine = strPerUnit * strShippedQty;
					}
					logger.verbose(
							"CrocsGetLineChargesForShipmentUEImpl dChargePerLine , invoicedPerLine , Actual ChargeAmount "
									+ dChargePerLine + " ," + lineCharge.invoicedPerLine + lineCharge.chargeAmount);

					newLineCharge.chargePerLine = dChargePerLine;
					newLineCharge.chargeAmount = dChargePerLine;
					newLineCharge.invoicedPerLine = lineCharge.invoicedPerLine;
					newLineCharge.chargePerUnit = lineCharge.chargePerUnit;
					newLineChargesList.add(newLineCharge);
				}
				outputStruct.newLineCharges = newLineChargesList;
				logger.verbose("CrocsGetLineChargesForShipmentUEImpl End  of the Method ");
			}

		} catch (YFSException e) {
			throw new YFSException(e.getMessage(), e.getErrorCode(), e.getErrorDescription());
		}
		return outputStruct;
	}

}
