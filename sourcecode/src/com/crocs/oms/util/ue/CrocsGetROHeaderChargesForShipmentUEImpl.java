package com.crocs.oms.util.ue;

import java.util.ArrayList;
import java.util.List;

import com.crocs.oms.common.util.CrocsConstant;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSExtnHeaderChargeStruct;
import com.yantra.yfs.japi.YFSExtnInputHeaderChargesShipment;
import com.yantra.yfs.japi.YFSExtnOutputHeaderChargesShipment;
import com.yantra.yfs.japi.YFSUserExitException;
import com.yantra.yfs.japi.ue.YFSGetHeaderChargesForShipmentUE;

/**
 * EOMS - 3896 Return header charges will be invoiced in the final invoice of
 * the return order.
 * 
 * Input: com.yantra.yfs.japi.YFSExtnInputHeaderChargesShipment is
 * &orderHeaderKey=20250520043143388958&orderReleaseNo=0&firstShippableRelease=false&bLastInvoice=true&completeOrderFlag=false&otherShipments=false&orderHeaderSCAC=&actualSCAC=null&actualFreightCharge=0.0&personalizeCode=&previousTotalInvoiced=138.34&totalOrderAmount=355.58&thisInvoiceLineTotal=209.96&totalRemainingLineAmount=-1.4210854715202004E-14&actualFreightChargeFlag=N&orderHeaderCharges=[&chargeCategory=ShippingCharge&chargeName=ShippingCharge&chargeAmount=40.0&invoicedAmount=0.0&reference=&eleExtendedFields=<?xml
 * version="1.0" encoding="UTF-8"?> <HeaderCharge> <Extn/> </HeaderCharge>
 * ,&chargeCategory=ShippingDiscount&chargeName=ShippingDiscount&chargeAmount=12.0&invoicedAmount=0.0&reference=&eleExtendedFields=<?xml
 * version="1.0" encoding="UTF-8"?> <HeaderCharge> <Extn/> </HeaderCharge>
 * ]&proformaHeaderCharges=null&shipmentKey=null&orderHeaderShippingCharges=0.0&orderHeaderHandlingCharges=0.0&orderHeaderPersonalizeCharges=0.0&orderHeaderDiscountAmount=0.0&previousHeaderShippingCharges=0.0&previousHeaderPersonalizeCharges=0.0&previousHeaderHandlingCharges=0.0&previousHeaderDiscount=0.0
 * 
 * 
 * Output for first Invoice:
 * &newHeaderCharges=[]&adjustHeaderDiscount=false&newShippingCharges=0.0&newPersonalizeCharges=0.0&newHandlingCharges=0.0&newDiscount=0.0
 * [system]:
 * 
 * Output for last Invoice:
 * &newHeaderCharges=[&chargeCategory=ShippingCharge&chargeName=ShippingCharge&chargeAmount=40.0&invoicedAmount=0.0&reference=&eleExtendedFields=<?xml
 * version="1.0" encoding="UTF-8"?> <HeaderCharge> <Extn/> </HeaderCharge>
 * ,&chargeCategory=ShippingDiscount&chargeName=ShippingDiscount&chargeAmount=12.0&invoicedAmount=0.0&reference=&eleExtendedFields=<?xml
 * version="1.0" encoding="UTF-8"?> <HeaderCharge> <Extn/> </HeaderCharge>
 * ]&adjustHeaderDiscount=false&newShippingCharges=0.0&newPersonalizeCharges=0.0&newHandlingCharges=0.0&newDiscount=0.0
 * 
 * 
 */
public class CrocsGetROHeaderChargesForShipmentUEImpl implements YFSGetHeaderChargesForShipmentUE {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsGetROHeaderChargesForShipmentUEImpl.class);

	@Override
	public YFSExtnOutputHeaderChargesShipment getHeaderChargesForShipment(YFSEnvironment arg0,
			YFSExtnInputHeaderChargesShipment inStruct) throws YFSUserExitException {
		logger.verbose("CrocsGetROHeaderChargesForShipmentUEImpl : Start");
		YFSExtnOutputHeaderChargesShipment outputStruct = new YFSExtnOutputHeaderChargesShipment();
		ArrayList<YFSExtnHeaderChargeStruct> newHeaderChargesList;
		boolean strlastInvoice = inStruct.bLastInvoice;
		List<YFSExtnHeaderChargeStruct> chargeList = inStruct.orderHeaderCharges;

		if (!strlastInvoice) {
			outputStruct.adjustHeaderDiscount = Boolean.parseBoolean(CrocsConstant.V_FALSE);
			outputStruct.newDiscount = Double.parseDouble("0.0");
			outputStruct.newHandlingCharges = Double.parseDouble("0.0");
			outputStruct.newPersonalizeCharges = Double.parseDouble("0.0");
			outputStruct.newShippingCharges = Double.parseDouble("0.0");
			newHeaderChargesList = new ArrayList<>();
			outputStruct.newHeaderCharges = newHeaderChargesList;
			logger.verbose("getHeaderChargesOutput : if end : ");
		} else {
			logger.verbose("CrocsGetROHeaderChargesForShipmentUEImpl.getHeaderChargesForShipment() : chargeList");
			outputStruct.newHeaderCharges = chargeList;
		}

		logger.verbose("CrocsGetROHeaderChargesForShipmentUEImpl : End");
		return outputStruct;
	}

}
