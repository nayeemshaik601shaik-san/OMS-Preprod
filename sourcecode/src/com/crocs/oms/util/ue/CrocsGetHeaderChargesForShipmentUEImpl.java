package com.crocs.oms.util.ue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.crocs.oms.common.util.CrocsConstant;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;

import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import com.yantra.yfs.japi.YFSExtnHeaderChargeStruct;
import com.yantra.yfs.japi.YFSExtnInputHeaderChargesShipment;
import com.yantra.yfs.japi.YFSExtnOutputHeaderChargesShipment;
import com.yantra.yfs.japi.YFSUserExitException;
import com.yantra.yfs.japi.ue.YFSGetHeaderChargesForShipmentUE;


public class CrocsGetHeaderChargesForShipmentUEImpl implements YFSGetHeaderChargesForShipmentUE,CrocsConstant {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsGetHeaderChargesForShipmentUEImpl.class);

	YFSExtnOutputHeaderChargesShipment outputStruct = new YFSExtnOutputHeaderChargesShipment();
	
	@Override
	public YFSExtnOutputHeaderChargesShipment getHeaderChargesForShipment(YFSEnvironment env, YFSExtnInputHeaderChargesShipment inStruct) throws YFSUserExitException {

		logger.verbose("CrocsGetHeaderChargesForShipmentUEImpl :  Start");
		YFSExtnOutputHeaderChargesShipment outputStruct = new YFSExtnOutputHeaderChargesShipment();
		Document getHeaderChargesInput = null;
		Element inputRootElement = null;
		Element orderHeaderChargesElement = null;
		List<YFSExtnHeaderChargeStruct> orderHeaderChargesList = null;
		YFSExtnHeaderChargeStruct orderHeaderCharge = null;
		Element YFSExtnHeaderChargeStructElement = null;
		org.w3c.dom.Node eleExtendedFieldsNode = null;
		List<?> proformaHeaderChargesList = null;
		YFSExtnHeaderChargeStruct proformaHeaderCharge = null;
		Element proformaHeaderChargesElement = null;
		Element outputRootElement = null;
		ArrayList<YFSExtnHeaderChargeStruct> newHeaderChargesList = null;
		List<?> YFSExtnHeaderChargeStructList = null;
		YFSExtnHeaderChargeStruct outputYFSExtnHeaderChargeStruct = null;
		Element outputYFSExtnHeaderChargeStructElement = null;
		Element extnHeaderChargesElement = null;
		Element newHeaderChargesElement = null;
		boolean isPartOfOrderAlreadyShipped = false;
		try {
			getHeaderChargesInput = SCXmlUtil.createDocument(E_GET_HEADER_CHRGES_FOR_SHIPMENT);
		} catch (YFSException e) {
			 throw new YFSException(e.getMessage(), e.getErrorCode(), e.getErrorDescription());
		}
		inputRootElement = getHeaderChargesInput.getDocumentElement();
		inputRootElement.setAttribute(A_ACTUAL_FREIGHT_CHRG, String.valueOf(inStruct.actualFreightCharge));
		inputRootElement.setAttribute(A_ACTUAL_FREIGHT_CHRG_FLAG, inStruct.actualFreightChargeFlag);
		inputRootElement.setAttribute(A_ACTUAL_SCAC,inStruct.actualSCAC);
		inputRootElement.setAttribute(A_BLAST_INV, String.valueOf(inStruct.bLastInvoice));
		inputRootElement.setAttribute(A_COMP_ORDER_FLAG, String.valueOf(inStruct.completeOrderFlag));
		inputRootElement.setAttribute(A_FRST_SHIPPABLE_REL, String.valueOf(inStruct.firstShippableRelease));
		inputRootElement.setAttribute(A_ORDER_HDR_DISC_AMNT, String.valueOf(inStruct.orderHeaderDiscountAmount));
		inputRootElement.setAttribute(A_ORDER_HDR_HANDLING_CHARGS, String.valueOf(inStruct.orderHeaderHandlingCharges));
		inputRootElement.setAttribute(A_ORDER_HDR_KEY,inStruct.orderHeaderKey);
		inputRootElement.setAttribute(A_ORDER_HDR_PERSONALISED_CHARGS, String.valueOf(inStruct.orderHeaderPersonalizeCharges));
		inputRootElement.setAttribute(A_ORDER_HDR_SCAC, inStruct.orderHeaderSCAC);
		inputRootElement.setAttribute(A_ORDER_HDR_SHP_CHARGS, String.valueOf(inStruct.orderHeaderShippingCharges));
		inputRootElement.setAttribute(A_ORDER_REL_NO, String.valueOf(inStruct.orderReleaseNo));
		inputRootElement.setAttribute(A_OTHR_SHIPMENTS, String.valueOf(inStruct.otherShipments));
		inputRootElement.setAttribute(A_PERSONALIZE_CODE,inStruct.personalizeCode);
		inputRootElement.setAttribute(A_PREV_HDR_DISCOUNT, String.valueOf(inStruct.previousHeaderDiscount));
		inputRootElement.setAttribute(A_PREV_HDR_HANDLING_CHARGS, String.valueOf(inStruct.previousHeaderHandlingCharges));
		inputRootElement.setAttribute(A_PREV_HDR_PERSONALISED_CHARGS, String.valueOf(inStruct.previousHeaderPersonalizeCharges));
		inputRootElement.setAttribute(A_PREV_HDR_SHP_CHARGS, String.valueOf(inStruct.previousHeaderShippingCharges));
		inputRootElement.setAttribute(A_PREV_TOTAL_INVOICED, String.valueOf(inStruct.previousTotalInvoiced));
		inputRootElement.setAttribute(A_THIS_INVOICE_LINE_TOTAL, String.valueOf(inStruct.thisInvoiceLineTotal));
		inputRootElement.setAttribute(A_TOTAL_ORDER_AMOUNT, String.valueOf(inStruct.totalOrderAmount));
		inputRootElement.setAttribute(A_TOTAL_REM_LINE_AMOUNT, String.valueOf(inStruct.totalRemainingLineAmount));
		
		logger.verbose("getHeaderChargesInput Document : " + XMLUtil.getXMLString(getHeaderChargesInput));
		logger.verbose("getHeaderChargesInput inStruct : " + inStruct);
		
		orderHeaderChargesList = inStruct.orderHeaderCharges;
		isPartOfOrderAlreadyShipped = inStruct.otherShipments;
		
		if (orderHeaderChargesList != null) {
			orderHeaderChargesElement = getHeaderChargesInput.createElement(E_GET_HEADER_CHRGES_FOR_SHIPMENT);
			inputRootElement.appendChild(orderHeaderChargesElement);
			orderHeaderCharge = new YFSExtnHeaderChargeStruct();
			logger.verbose("orderHeaderChargesElement Document : " + SCXmlUtil.getString(orderHeaderChargesElement));
			for (Iterator<YFSExtnHeaderChargeStruct> chargeIter = orderHeaderChargesList.iterator(); chargeIter.hasNext();) {
				logger.verbose("*******************");
				orderHeaderCharge = chargeIter.next();
				YFSExtnHeaderChargeStructElement = getHeaderChargesInput.createElement(E_YFS_EXTN_HDR_CHRG_STRUCT);
				orderHeaderChargesElement.appendChild(YFSExtnHeaderChargeStructElement);
				YFSExtnHeaderChargeStructElement.setAttribute(A_CHRG_AMNT_UE, String.valueOf(orderHeaderCharge.chargeAmount));
				YFSExtnHeaderChargeStructElement.setAttribute(A_CHRG_CATGRY_UE, orderHeaderCharge.chargeCategory);
				YFSExtnHeaderChargeStructElement.setAttribute(A_CHRG_NAME_UE, orderHeaderCharge.chargeName);
				YFSExtnHeaderChargeStructElement.setAttribute(A_INV_AMNT_UE, String.valueOf(orderHeaderCharge.invoicedAmount));
				YFSExtnHeaderChargeStructElement.setAttribute(A_REFERENCE_UE, orderHeaderCharge.reference);
				if (orderHeaderCharge.eleExtendedFields != null) {
					eleExtendedFieldsNode = getHeaderChargesInput.importNode(orderHeaderCharge.eleExtendedFields.getDocumentElement(), true);
					YFSExtnHeaderChargeStructElement.appendChild(eleExtendedFieldsNode);
				}
			}

		}
		proformaHeaderChargesList = inStruct.proformaHeaderCharges;
		if (proformaHeaderChargesList != null) {
			proformaHeaderChargesElement = getHeaderChargesInput.createElement(E_PROFORMA_HDR_CHARGES);
			inputRootElement.appendChild(proformaHeaderChargesElement);
			proformaHeaderCharge = new YFSExtnHeaderChargeStruct();
			for (Iterator<?> proformaChargeIter = proformaHeaderChargesList.iterator(); proformaChargeIter.hasNext();) {
				proformaHeaderCharge = (YFSExtnHeaderChargeStruct) proformaChargeIter.next();
				YFSExtnHeaderChargeStructElement = getHeaderChargesInput.createElement(E_YFS_EXTN_HDR_CHRG_STRUCT);
				proformaHeaderChargesElement.appendChild(YFSExtnHeaderChargeStructElement);
				YFSExtnHeaderChargeStructElement.setAttribute(A_CHRG_AMNT_UE, String.valueOf(proformaHeaderCharge.chargeAmount));
				YFSExtnHeaderChargeStructElement.setAttribute(A_CHRG_CATGRY_UE, proformaHeaderCharge.chargeCategory);
				YFSExtnHeaderChargeStructElement.setAttribute(A_CHRG_NAME_UE, proformaHeaderCharge.chargeName);
				YFSExtnHeaderChargeStructElement.setAttribute(A_INV_AMNT_UE, String.valueOf(proformaHeaderCharge.invoicedAmount));
				YFSExtnHeaderChargeStructElement.setAttribute(A_REFERENCE_UE, proformaHeaderCharge.reference);
				if (proformaHeaderCharge.eleExtendedFields != null) {
					eleExtendedFieldsNode = getHeaderChargesInput.importNode(proformaHeaderCharge.eleExtendedFields.getDocumentElement(), true);
					YFSExtnHeaderChargeStructElement.appendChild(eleExtendedFieldsNode);
				}
			}
		}
		try {
			logger.verbose("Input to CrocsGetOrderListForHeaderCharges is: \n" + XMLUtil.getXMLString(getHeaderChargesInput));
			if(isPartOfOrderAlreadyShipped){
				outputStruct.adjustHeaderDiscount = Boolean.parseBoolean(V_FALSE);
				outputStruct.newDiscount = Double.parseDouble("0.0");
				outputStruct.newHandlingCharges = Double.parseDouble("0.0");
				outputStruct.newPersonalizeCharges = Double.parseDouble("0.0");
				outputStruct.newShippingCharges = Double.parseDouble("0.0");
				newHeaderChargesList = new ArrayList<YFSExtnHeaderChargeStruct>();
				outputStruct.newHeaderCharges = newHeaderChargesList;
				logger.verbose("getHeaderChargesOutput : if end : ");
			}
			else{
				logger.verbose("CrocsGetHeaderChargesForShipmentUE.getHeaderChargesForShipment() : 1234");
				outputStruct.newHeaderCharges = orderHeaderChargesList;
			}
			logger.verbose("CrocsGetHeaderChargesForShipmentUESvc invoked successfully");
		} catch (Exception e) {
			 throw new YFSException("CrocsGetOrderListForHeaderCharges : Catch " + e.getMessage() +"%%%%%%");
		}
		logger.verbose("CrocsGetHeaderChargesForShipmentUE.getHeaderChargesForShipment() : End");
		return outputStruct;
	}
}
