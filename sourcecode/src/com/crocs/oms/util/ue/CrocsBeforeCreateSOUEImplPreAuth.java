package com.crocs.oms.util.ue;

import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsAPIConstants;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsTemplateConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.ibm.icu.util.Calendar;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCException;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import com.yantra.yfs.japi.YFSUserExitException;
import com.yantra.yfs.japi.ue.YFSBeforeCreateOrderUE;
import com.crocs.oms.order.CrocsDecryption;
import com.crocs.oms.order.CrocsHeaderChargesIterationToLine;

import org.apache.commons.json.JSONException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;


public class CrocsBeforeCreateSOUEImplPreAuth implements CrocsXmlConstants, YFSBeforeCreateOrderUE {
	private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsBeforeCreateSOUEImplPreAuth.class);

	@Override
	public String beforeCreateOrder(YFSEnvironment yfsEnvironment, String s) throws YFSUserExitException {
		return null;
	}

	/** this will be invoked when create order API will be called
	 * changes for EOMS-608 :CancelOrderNotification : updating the charges in Reference
	 * @param yfsEnvironment
	 * @param document
	 * @return document
	 * @throws YFSUserExitException
	 */
	@Override
	public Document beforeCreateOrder(YFSEnvironment yfsEnvironment, Document document) throws YFSUserExitException {
		logger.verbose("Input Document : " + XMLUtil.getXMLString(document));
		 //EOMS -2562 prorate header charges and taxes to line level.
		// Iterating through header charges and applying relevant filters
		CrocsHeaderChargesIterationToLine headerCharges = new CrocsHeaderChargesIterationToLine();
		Document inputDoc = headerCharges.headerChargesIterationToLine(document);
		Document chargesdoc = null;
		// Filtering out unwanted header charges and taxes
		chargesdoc = headerCharges.filterHeaderCharges(inputDoc);
		document = headerCharges.filterHeaderTaxes(chargesdoc);
		
		Element orderEle = document.getDocumentElement();
		Element orderLines = SCXmlUtil.getChildElement(orderEle, E_ORDER_LINES);
		ArrayList<Element> orderLineList = SCXmlUtil.getChildren(orderLines, E_ORDER_LINE);
		String enterpriseCode = orderEle.getAttribute(A_ENTERPRISE_CODE);
		String strOrderPurpose = orderEle.getAttribute(A_ORDER_PURPOSE);

		// updating the AuthorizationExpirtion Date for After Pay and Cash App
		updateAuthExpirationDate(yfsEnvironment,orderEle);
		
		/* Updating HoldAgainstbook as "N" for Post Auth payment Methods, 
		 * After Authorization Webhook, creating Authorization Entry in Charge Transaction Table.
		 */
		updateHoldAgainstBookForPostAuthPayment(yfsEnvironment, orderEle);

		// Process OrderLine
		for (Element orderLine : orderLineList) {
			logger.verbose("OrderLine Element is: " + SCXmlUtil.getString(orderLine));
			// Process Line Charges
			extractLineCharges(orderLine);
			// Process Line Tax
			extractLineTax(orderLine);

			// Update the Carrier Service Code
			updateCarrierServiceCode(yfsEnvironment, orderLine, enterpriseCode);

			// Update ExtnIsCollab for Collab Items
			if(!A_EVENT_CODE_REFUND.equalsIgnoreCase(strOrderPurpose)) {
				updateIsCollabForOrderLine(yfsEnvironment,orderLine);
			}
		}
		// processHeaderCharges
		extractHeaderCharges(orderEle);
		// process header Tax
		extractHeaderTax(orderEle);
		//extract OverallChargeTotals
		extractOverallChargeTotals(orderEle);
		
		//Handling GiftCardDetails 
		handlingGiftCardDetails(orderEle);
		return document;

	}

	private void handlingGiftCardDetails(Element orderEle) {

		Element paymentMethodsEle = SCXmlUtil.getChildElement(orderEle, E_PAYMENT_METHODS);
		ArrayList<Element> paymentMethods = SCXmlUtil.getChildren(paymentMethodsEle, E_PAYMENT_METHOD);
		for (Element paymentMethod : paymentMethods) {
			String paymentType = paymentMethod.getAttribute(A_PAYMENT_TYPE);
			if (paymentType.equalsIgnoreCase(CrocsConstant.STR_GIVEX)) {

				String encodedSvcNo = paymentMethod.getAttribute(CrocsConstant.SvcNo);
				String givexPinEncoded = paymentMethod.getAttribute("PaymentReference3");
				try {
					CrocsDecryption decryption = new CrocsDecryption();
					String decodedSvcNo = decryption.getDecryptedDataForGivex(encodedSvcNo);

					paymentMethod.setAttribute("DisplaySvcNo",
							decodedSvcNo.substring(decodedSvcNo.length() - 4, decodedSvcNo.length()));

					paymentMethod.setAttribute("PaymentReference3",
							decryption.getDecryptedDataForGivex(givexPinEncoded));
				} catch (InvalidKeyException | DOMException | NoSuchAlgorithmException | NoSuchPaddingException
						| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException
						| JSONException e) {
					throw new YFCException(e.getMessage());
				}
			}
		}
	}

	/** this sets the tax value in tax reference
	 * @param orderEle header Taxes
	 */
	private static void extractHeaderTax(Element orderEle) {
		Element orderHeaderTax = SCXmlUtil.getChildElement(orderEle, E_HEADER_TAXES);
		ArrayList<Element> headerTaxes = SCXmlUtil.getChildren(orderHeaderTax, E_HEADER_TAX);
		for (Element orderHeaderTaxes : headerTaxes) {
			logger.verbose("HeaderTax Element is: " + SCXmlUtil.getString(orderHeaderTaxes));
			if (!YFCCommon.isVoid(orderHeaderTaxes.getAttribute(A_TAX))) {
				orderHeaderTaxes.setAttribute(A_TAX_REFERENCE, orderHeaderTaxes.getAttribute(A_TAX));
			}
		}
	}

	/** this sets the shipping value in shipping reference
	 * @param orderEle headerCharges
	 */
	private static void extractHeaderCharges(Element orderEle) {
		Element headerChargesEle = SCXmlUtil.getChildElement(orderEle, E_HEADER_CHARGES);
		ArrayList<Element> headerCharges = SCXmlUtil.getChildren(headerChargesEle, E_HEADER_CHARGE);
		for (Element orderHeaderCharge : headerCharges) {
			logger.verbose("HeaderCharge Element is: " + SCXmlUtil.getString(orderHeaderCharge));
			if (!YFCCommon.isVoid(orderHeaderCharge.getAttribute(A_CHARGE_AMOUNT))) {
				orderHeaderCharge.setAttribute(A_SHIPPING_REFERENCE, orderHeaderCharge.getAttribute(A_CHARGE_AMOUNT));
			}
		}
	}

	/** this sets the line tax value in tax reference
	 * @param orderLine linetaxes
	 */
	private static void extractLineTax(Element orderLine) {
		Element orderLineTax = SCXmlUtil.getChildElement(orderLine, E_LINE_TAXES);
		ArrayList<Element> lineTaxes = SCXmlUtil.getChildren(orderLineTax, E_LINE_TAX);
		for (Element tax : lineTaxes) {
			logger.verbose("LineTax Element is: " + SCXmlUtil.getString(tax));
			if (!YFCCommon.isVoid(tax.getAttribute(A_TAX))) {
				tax.setAttribute(A_TAX_REFERENCE, tax.getAttribute(A_TAX));
			}
		}
	}

	/** this sets the discount value in discount reference
	 * @param orderLine LineCharges
	 */
	private static void extractLineCharges(Element orderLine) {
		Element lineChargesEle = SCXmlUtil.getChildElement(orderLine, E_LINE_CHARGES);
		ArrayList<Element> lineCharges = SCXmlUtil.getChildren(lineChargesEle, E_LINE_CHARGE);
		for (Element lineCharge : lineCharges) {
			logger.verbose("LineCharge Element is: " + SCXmlUtil.getString(lineCharge));
			if (!YFCCommon.isVoid(lineCharge.getAttribute(A_CHARGE_PER_LINE))) {
				lineCharge.setAttribute(A_DISCOUNT_REFERENCE, lineCharge.getAttribute(A_CHARGE_PER_LINE));
			}
		}
	}

	/**
	 * this method sets the AuthExpirtaionDate based on Payment types.
	 *
	 * @param orderEle
	 *
	 */
	private static void updateAuthExpirationDate(YFSEnvironment env, Element orderEle) {
		logger.verbose("CrocsBeforeCreateSOUEImpl: updateAuthExpirationDate:START: "+XMLUtil.getElementXMLString(orderEle));
		try {
			String authExpirationDate= null;
			String strEnterpriseCode = orderEle.getAttribute(A_ENTERPRISE_CODE);
			Element paymentMethodsEle = SCXmlUtil.getChildElement(orderEle, E_PAYMENT_METHODS);
			//EOMS-4267 :Auth Expiration date update changes : START
			ArrayList<Element> paymentMethods = SCXmlUtil.getChildren(paymentMethodsEle, E_PAYMENT_METHOD);
			Document docGetCommonCodeListOutput = CommonUtil.getCommonCodeList(env, strEnterpriseCode, CrocsConstant.STR_CROCS_AUTH_EXP_PAYMENTS, null);			
			for (Element paymentMethod : paymentMethods) {
				String paymentType = paymentMethod.getAttribute(A_PAYMENT_TYPE);
				Element paymentMethodDetails = SCXmlUtil.getChildElement(paymentMethod, E_PAYMENT_DETAILS);
				Calendar cal = Calendar.getInstance();
				SimpleDateFormat formatter = new SimpleDateFormat(CrocsConstant.STR_DB_DATE_FORMAT);
				if ((YFCCommon.isVoid(paymentMethodDetails.getAttribute(A_AUTH_EXPIRATION_DATE))
						|| paymentMethodDetails.getAttribute(A_AUTH_EXPIRATION_DATE) == null) 
						&& !CrocsConstant.P_CREDIT_CARD.equalsIgnoreCase(paymentType)) {
					Element eleCommonCodeList = XMLUtil.getElementByXPath(docGetCommonCodeListOutput,"/CommonCodeList/CommonCode[@CodeValue='"+paymentType+"']");
					if(eleCommonCodeList!=null) {
						int intShortDescription = Integer.parseInt(eleCommonCodeList.getAttribute(A_CODE_SHORT_DESCRIPTION));
						cal.add(Calendar.DATE, intShortDescription);
						authExpirationDate = formatter.format(cal.getTime());
					}else {
						cal.add(Calendar.DATE, 30);
						authExpirationDate = formatter.format(cal.getTime());
					}
					paymentMethodDetails.setAttribute(A_AUTH_EXPIRATION_DATE, authExpirationDate);
				}else {
					//EOMS-643 : Delta Auth Changes : START
					if(CrocsConstant.P_CREDIT_CARD.equalsIgnoreCase(paymentType)) {
						paymentType = paymentMethod.getAttribute(A_CREDIT_CARD_TYPE).toUpperCase();
						Element eleCommonCodeList = XMLUtil.getElementByXPath(docGetCommonCodeListOutput,"/CommonCodeList/CommonCode[@CodeValue='"+paymentType+"']");
						if(eleCommonCodeList!=null) {
							int intShortDescription = Integer.parseInt(eleCommonCodeList.getAttribute(A_CODE_SHORT_DESCRIPTION));
							cal.add(Calendar.DATE, intShortDescription);
							authExpirationDate = formatter.format(cal.getTime());
							paymentMethodDetails.setAttribute(A_AUTH_EXPIRATION_DATE, authExpirationDate);
						}
					}
					//EOMS-643 : Delta Auth changes : END
				}
			}
			//EOMS-4267 : Auth Expiration date update changess : END
		}catch(Exception e) {
			logger.verbose("Execption updateAuthExpirationDate API: " + e.getMessage());
			throw new YFSException("Error invoking updateAuthExpirationDate : " + e.getMessage());
		}
		logger.verbose("CrocsBeforeCreateSOUEImpl: updateAuthExpirationDate:END: ");
	}

	/**
	 * Description: Updating OrderLine.ExtnIsCollab as 'Y' when Item.ExtnCollabSKU as true otherwise 'N'
	 *
	 *
	 * @param env
	 * @param orderLine
	 */
	public static void updateIsCollabForOrderLine(YFSEnvironment env, Element orderLine) {

		logger.verbose("CrocsBeforeCreateSOUEImpl : updateIsCollabForOrderLine: START");
    	Element eleOrderLineExtn = null;
    	String strExtnCollabSKU;
    	String strItemID = SCXmlUtil.getXpathAttribute(orderLine, CrocsConstant.STR_XPATH_ITEM_ID);
    	Document docGetItemList = getItemListForItemId(env,strItemID);
    	if(!YFCObject.isVoid(docGetItemList) && docGetItemList!=null) {
    		
    		Element eleItemList = docGetItemList.getDocumentElement();
    		strExtnCollabSKU = SCXmlUtil.getXpathAttribute(eleItemList, CrocsXmlConstants.XPATH_EXTN_COLLAB_SKU);
    		if(SCXmlUtil.getChildElement(orderLine, E_EXTN)!=null) {
    			eleOrderLineExtn = SCXmlUtil.getChildElement(orderLine, E_EXTN);
    		}else {
    			eleOrderLineExtn = SCXmlUtil.createChild(orderLine, E_EXTN);
    		}
    		if((!YFCCommon.isVoid(strExtnCollabSKU)) && (CrocsConstant.TRUE.equalsIgnoreCase(strExtnCollabSKU) 
    				|| CrocsConstant.YES.equalsIgnoreCase(strExtnCollabSKU) )) {
    			eleOrderLineExtn.setAttribute( A_EXTN_IS_COLLAB, CrocsConstant.YES);
    		}else {
    			eleOrderLineExtn.setAttribute( A_EXTN_IS_COLLAB, CrocsConstant.NO);
    		}	
    	}
    	logger.verbose("CrocsBeforeCreateSOUEImpl : updateIsCollabForOrderLine: END");
	}

	/** Description: Calling getItemList with ItemID
	 * @param env
	 * @param strItemId
	 * @return
	 * @throws YFSException
	 */
	public static Document getItemListForItemId(YFSEnvironment env, String strItemId) throws YFSException {

		logger.verbose("CrocsBeforeCreateSOUEImpl : getItemListForItemId: START");

		Document getItemListOut = null;
		try {
			if (!YFCObject.isVoid(strItemId)) {

				Document getItemListInDoc = SCXmlUtil.createDocument(E_ITEM);
				getItemListInDoc.getDocumentElement().setAttribute(A_ITEM_ID, strItemId);
				getItemListOut = CommonUtil.invokeService(env, CrocsConstant.STR_CROCS_GET_ITEMLIST_SERVICE, getItemListInDoc);
			}
		} catch (Exception e) {
			throw new YFSException("CrocsBeforeCreateSOUEImpl.getItemListForItemId :Expection" + e.getMessage());
		}
		logger.verbose("CrocsBeforeCreateSOUEImpl : getItemListForItemId: END:" + XMLUtil.getXMLString(getItemListOut));
		return getItemListOut;
	}

	/**
	 * This method will update the carrier service code at the order line level If
	 * mapping is not present then it will update with default value Economy for US
	 * and Standard for CA
	 *
	 * @param yfsEnvironment
	 * @param orderLine
	 * @param enterpriseCode
	 */
	private void updateCarrierServiceCode(YFSEnvironment yfsEnvironment, Element orderLine, String enterpriseCode) {
		logger.verbose("Start of method updateCarrierServiceCode with input: " + SCXmlUtil.getString(orderLine) + " and enterpriseCode as: " + enterpriseCode);

		String carrierServiceCode = orderLine.getAttribute(A_CARRIER_SERVICE_CODE);

		// Check if carrierServiceCode is empty or null
		if (carrierServiceCode == null || carrierServiceCode.isEmpty()) {
		    logger.verbose("carrierServiceCode received is empty, updating it with default value");

		    // Set default based on enterpriseCode
		    switch (enterpriseCode) {
		        case CrocsConstant.CROCS_US:
		            orderLine.setAttribute(A_CARRIER_SERVICE_CODE, CrocsConstant.VAL_ECONOMY);
		            break;
		        case CrocsConstant.CROCS_CA:
		            orderLine.setAttribute(A_CARRIER_SERVICE_CODE, CrocsConstant.VAL_STANDARD);
		            break;
		        default:
		            logger.verbose("Unknown enterpriseCode: " + enterpriseCode);
		            break;
		    }
		    logger.verbose("End of method updateCarrierServiceCode with default service code");
		    return;
		}

		// If carrierServiceCode is not empty, fetch common code list
		Element getCommonCodeList = getCommonCodeList(yfsEnvironment, enterpriseCode, carrierServiceCode).getDocumentElement();

		String updatedCarrierServiceCode = SCXmlUtil.getXpathAttribute(getCommonCodeList, CrocsConstant.XPATH_CODE_SHORT_DESCRIPTION);

		// Check if the fetched updateCarrierServiceCode is empty
		if (updatedCarrierServiceCode == null || updatedCarrierServiceCode.isEmpty()) {

		    switch (enterpriseCode) {
		        case CrocsConstant.CROCS_US:
		            orderLine.setAttribute(A_CARRIER_SERVICE_CODE, CrocsConstant.VAL_ECONOMY);
		            break;
		        case CrocsConstant.CROCS_CA:
		            orderLine.setAttribute(A_CARRIER_SERVICE_CODE, CrocsConstant.VAL_STANDARD);
		            break;
		        default:
		            logger.verbose("Unknown enterpriseCode for update: " + enterpriseCode);
		            break;
		    }
		} else
		    orderLine.setAttribute(A_CARRIER_SERVICE_CODE, updatedCarrierServiceCode);

		logger.verbose("End of method updateCarrierServiceCode with updatedCarrierServiceCode: " + updatedCarrierServiceCode);
	}

	/**
	 * This function calls the getCommonCodeList api to get the carrierServiceCode
	 * mapping
	 *
	 * @param yfsEnvironment
	 * @param enterpriseCode
	 * @return
	 * @throws YFSException
	 */
	private static Document getCommonCodeList(YFSEnvironment yfsEnvironment, String enterpriseCode,	String carrierServiceCode) throws YFSException {
		logger.verbose("Start of method getCommonCodeList with carrierServiceCode: " + carrierServiceCode
				+ " and enterpriseCode as: " + enterpriseCode);

		// prepare the input to getCommonCodeList api
		Document inDoc = SCXmlUtil.createDocument(A_COMMON_CODE);
		Element inDocEle = inDoc.getDocumentElement();
		inDocEle.setAttribute(A_CODE_TYPE, CrocsConstant.VAL_CROCS_SFCC_CARRIER);
		inDocEle.setAttribute(A_ORGANIZATION_CODE, enterpriseCode);
		inDocEle.setAttribute(A_CODE_VALUE, carrierServiceCode);

		Document outDoc = null;
		// invoking the api
		try {
			logger.verbose("getCommonCodeList inDoc:: " + SCXmlUtil.getString(inDoc));
			outDoc = CommonUtil.invokeAPI(yfsEnvironment, CrocsTemplateConstants.TEMPLATE_GET_COMMON_CODE_LIST,
					CrocsAPIConstants.API_GET_COMMON_CODE_LIST, inDoc);

		} catch (Exception e) {
			logger.error("Error invoking getCommonCodeList API: " + e.getMessage());
			throw new YFSException("Error invoking getCommonCodeList API: " + e.getMessage());
		}
		logger.verbose("getCommonCodeList outDoc:: " + SCXmlUtil.getString(outDoc));

		return outDoc;
	}
	/**
	 * Description: Below method helps to update HoldAgainstBook as "N" for Post Auth
	 * Payment Methods only. After Authorization Webhook, creating Authorization entry in Charge Transaction Table.
	 *
	 * @param orderEle
	 * @param env
	 */
	private static void updateHoldAgainstBookForPostAuthPayment(YFSEnvironment env, Element orderEle) {
		logger.verbose("CrocsBeforeCreateSOUEImpl: updateHoldAgainstBookForPostAuthPayment:START: "+XMLUtil.getElementXMLString(orderEle));
		try {
			String strEnterpriseCode = orderEle.getAttribute(A_ENTERPRISE_CODE);
			Element paymentMethodsEle = SCXmlUtil.getChildElement(orderEle, E_PAYMENT_METHODS);
			ArrayList<Element> paymentMethods = SCXmlUtil.getChildren(paymentMethodsEle, E_PAYMENT_METHOD);
			for (Element paymentMethod : paymentMethods) {
				String paymentType = paymentMethod.getAttribute(A_PAYMENT_TYPE);
				Document docGetCommonCodeListOutput = CommonUtil.getCommonCodeList(env, strEnterpriseCode, CrocsConstant.STR_CROCS_AUTH_EXP_PAYMENTS, null);
				Element eleCommonCodeList = XMLUtil.getElementByXPath(docGetCommonCodeListOutput,"/CommonCodeList/CommonCode[@CodeValue='"+paymentType+"' and @CodeLongDescription='POST_AUTH']");
				if(eleCommonCodeList!=null) {
					Element paymentMethodDetails = SCXmlUtil.getChildElement(paymentMethod, E_PAYMENT_DETAILS);
					paymentMethod.removeChild(paymentMethodDetails);
				}
			}
		}catch(Exception e) {
			logger.verbose("Execption updateHoldAgainstBookForPostAuthPayment API: " + e.getMessage());
			throw new YFSException("Error invoking updateHoldAgainstBookForPostAuthPayment : " + e.getMessage());
		}
		logger.verbose("CrocsBeforeCreateSOUEImpl: updateHoldAgainstBookForPostAuthPayment:END: ");
	}

	/** this sets the header level shipping charge and discount to reference for OverallChargeTotals
	 * @param orderEle Order Ele
	 */
	private static void extractOverallChargeTotals(Element orderEle) {
		Element overallTotals = SCXmlUtil.getChildElement(orderEle, E_OVERALL_TOTALS);
		Element overallChargeTotals = SCXmlUtil.getChildElement(overallTotals, E_OVERALL_CHARGE_TOTALS);
		ArrayList<Element> overallChargeTotalEle = SCXmlUtil.getChildren(overallChargeTotals, E_OVERALL_CHARGE_TOTAL);
		for (Element overAllChargeTotal : overallChargeTotalEle) {
			logger.verbose("HeaderTax Element is: " + SCXmlUtil.getString(overAllChargeTotal));
			if ((A_SHIPPING_CHARGE.equalsIgnoreCase(overAllChargeTotal.getAttribute(A_CHARGE_NAME))) && !YFCCommon.isVoid(overAllChargeTotal.getAttribute(A_GRAND_CHARGES))) {
				overAllChargeTotal.setAttribute(A_REFERENCE, overAllChargeTotal.getAttribute(A_GRAND_CHARGES));
			}
			if((A_SHIPPING_DISCOUNT.equalsIgnoreCase(overAllChargeTotal.getAttribute(A_CHARGE_NAME))) && !YFCCommon.isVoid(overAllChargeTotal.getAttribute(A_GRAND_DISCOUNT))) {
				overAllChargeTotal.setAttribute(A_REFERENCE, overAllChargeTotal.getAttribute(A_GRAND_DISCOUNT));
			}
		}
	}
}