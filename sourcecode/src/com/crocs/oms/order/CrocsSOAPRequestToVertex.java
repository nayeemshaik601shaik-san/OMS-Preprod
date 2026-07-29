package com.crocs.oms.order;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.yantra.interop.japi.YIFApi;
import com.yantra.interop.japi.YIFClientCreationException;
import com.yantra.interop.japi.YIFClientFactory;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCException;
import com.yantra.yfs.core.YFSSystem;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsAPIConstants;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsTemplateConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.sterlingcommerce.woodstock.util.io.ByteArrayOutputStream;
import com.crocs.oms.common.util.CrocsXmlConstants;

/**
 * EOMS - 809 Tax Call Implementation
 * 
 * Sample SOAP Request To VERTEX
 * <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
 * xmlns:urn="urn:vertexinc:o-series:tps:9:0"> <soapenv:Header/> <soapenv:Body>
 * <urn:VertexEnvelope> <urn:Login> <urn:UserName>*****</urn:UserName>
 * <urn:Password>*****</urn:Password> <urn:TrustedId>*******</urn:TrustedId>
 * </urn:Login>
 * <urn:QuotationRequest documentDate="2024-01-01" transactionType="SALE"
 * transactionId="" documentNumber=""> <urn:currency></urn:currency>
 * <urn:originalCurrency></urn:originalCurrency> <urn:Seller>
 * <urn:Company>Crocs</urn:Company> <urn:PhysicalOrigin> <urn:StreetAddress1>
 * Ministry of Finance</urn:StreetAddress1> <urn:StreetAddress2>Revenue Division
 * PO Box 200</urn:StreetAddress2> <urn:City>Regina</urn:City>
 * <urn:MainDivision>SK</urn:MainDivision> <urn:SubDivision></urn:SubDivision>
 * <urn:PostalCode>S4P 2Z6</urn:PostalCode> <urn:Country>CA</urn:Country>
 * <urn:CurrencyConversion></urn:CurrencyConversion> </urn:PhysicalOrigin>
 * <urn:AdministrativeOrigin> <urn:StreetAddress1> Ministry of
 * Finance</urn:StreetAddress1> <urn:StreetAddress2>Revenue Division PO Box
 * 200</urn:StreetAddress2> <urn:City>Regina</urn:City>
 * <urn:MainDivision>SK</urn:MainDivision> <urn:SubDivision></urn:SubDivision>
 * <urn:PostalCode>S4P 2Z6</urn:PostalCode> <urn:Country>CA</urn:Country>
 * <urn:CurrencyConversion></urn:CurrencyConversion> </urn:AdministrativeOrigin>
 * </urn:Seller> <urn:Customer> <urn:Destination> <urn:StreetAddress1> Ministry
 * of Finance</urn:StreetAddress1> <urn:StreetAddress2>Revenue Division PO Box
 * 200</urn:StreetAddress2> <urn:City>Regina</urn:City>
 * <urn:MainDivision>SK</urn:MainDivision> <urn:SubDivision></urn:SubDivision>
 * <urn:PostalCode>S4P 2Z6</urn:PostalCode> <urn:Country>CA</urn:Country>
 * <urn:CurrencyConversion></urn:CurrencyConversion> </urn:Destination>
 * <urn:IsTaxExempt>false</urn:IsTaxExempt>
 * <urn:ExemptionReasonCode></urn:ExemptionReasonCode> </urn:Customer>
 * <urn:LineItem lineItemNumber="1" >
 * <urn:Product productClass="PC040144">10001-001-M4W6</urn:Product>
 * <urn:Quantity unitOfMeasure="EA">10</urn:Quantity>
 * <urn:ExtendedPrice>101.50</urn:ExtendedPrice> </urn:LineItem>
 * <urn:LineItem lineItemNumber="2" >
 * <urn:Product productClass="FR020800">Shipping</urn:Product>
 * <urn:Quantity unitOfMeasure="EA">1</urn:Quantity>
 * <urn:ExtendedPrice>11.00</urn:ExtendedPrice> </urn:LineItem>
 * </urn:QuotationRequest> </urn:VertexEnvelope> </soapenv:Body>
 * </soapenv:Envelope>
 **/

public class CrocsSOAPRequestToVertex implements CrocsConstant {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsSOAPRequestToVertex.class);
	/**
	 * @param env
	 * @param inputDoc
	 * @return Document
	 *  * EOMS - 809 Tax Call implementation This is called in
	 * CrocsRecalculateSOHeaderTaxUE.java and CrocsRecalculateSOLineTaxUE.java
	 */
	public Document quotationRequestToVertex(YFSEnvironment env, Document inputDoc) {
		Document docSoapOutput = null;
		Element eleOrd = (Element) inputDoc.getDocumentElement().getElementsByTagName("Order").item(0);
		String strOrdNo = eleOrd.getAttribute(CrocsXmlConstants.A_ORDER_NO);
		
		
		try {
			logger.verbose("CrocsSOAPRequestToVertex : quotationRequestToVertex Begin" + SCXmlUtil.getString(inputDoc));

			// EOMS-4538 Vertex Changes for Apple Pay - start
			
	        String strPaymentType = null;
			Element elePaymentMethods = (Element) eleOrd.getElementsByTagName(CrocsXmlConstants.E_PAYMENT_METHODS).item(0);
			Element elePaymentMethod = (Element) elePaymentMethods.getElementsByTagName(CrocsXmlConstants.E_PAYMENT_METHOD).item(0);
			if (!YFCObject.isVoid(elePaymentMethod)) {
				strPaymentType = elePaymentMethod.getAttribute(CrocsXmlConstants.A_PAYMENT_TYPE);
			}

			// EOMS-4538 Vertex Changes for Apple Pay - End
			
			String strOrderKey = eleOrd.getAttribute(CrocsXmlConstants.A_ORDER_HEADER_KEY);
			String strEnterpriseCode = eleOrd.getAttribute(CrocsXmlConstants.A_ENTERPRISE_CODE);
			String strOrdDate = eleOrd.getAttribute(CrocsXmlConstants.A_ORDER_DATE);
			Element elePersonInfoShipTo = (Element) eleOrd.getElementsByTagName(CrocsXmlConstants.E_PERSON_INFO_SHIP_TO)
					.item(0);
			Element eleHeaderChargeSC = XMLUtil.getElementByXPath(inputDoc, CrocsXmlConstants.XPATH_SHIPPING_CHARGE);
			Element eleHeaderChargeDS = XMLUtil.getElementByXPath(inputDoc, CrocsXmlConstants.XPATH_SHIPPING_DISCOUNT);
			Element eleHeaderChargeESP = XMLUtil.getElementByXPath(inputDoc,
					CrocsXmlConstants.XPATH_SHIPPING_EXTENDED_SHIP_PROTECTION);
			Element eleSelleradd = addOrgDetails(env, strEnterpriseCode, inputDoc);		

			// Add Line Items
			Element orderLines = SCXmlUtil.getChildElement(eleOrd, CrocsXmlConstants.E_ORDER_LINES);
			ArrayList<Element> orderLineList = SCXmlUtil.getChildren(orderLines, CrocsXmlConstants.E_ORDER_LINE);

			// Generate SOAP Message
			SOAPMessage soapMessage = createSOAPMessage();
			SOAPEnvelope soapEnvelope = soapMessage.getSOAPPart().getEnvelope();
			SOAPElement vertexEnvelope = soapEnvelope.getBody().addChildElement(CrocsConstant.V_VERTEX_ENVELOPE,
					CrocsConstant.V_URN);

			// Add Login Details
			addLogin(vertexEnvelope);

			// Add Quotation Request details
			SOAPElement quotationRequest = addRequest(vertexEnvelope, CrocsConstant.V_QUOTATION_REQUEST, strOrdDate,
					strOrderKey, strOrdNo);

			// Add Seller details
			addSellerDetails(quotationRequest, eleSelleradd, strEnterpriseCode);

			// Add Customer details
			addCustomerDetails(quotationRequest, elePersonInfoShipTo);

			// Add Line Item details
			addLineItems(quotationRequest, orderLineList,strPaymentType,strEnterpriseCode);

			// Add Shipping Charge
			addShippingCharge(eleHeaderChargeSC, quotationRequest, orderLineList.size() + 1,strPaymentType,strEnterpriseCode);

			// Add Shipping Discount
			addShippingCharge(eleHeaderChargeDS, quotationRequest, orderLineList.size() + 2,strPaymentType,strEnterpriseCode);

			// Add Extended Ship Protection
			addShippingCharge(eleHeaderChargeESP, quotationRequest, orderLineList.size() + 3,strPaymentType,strEnterpriseCode);			

			// Convert SoapMessage to Document
			Document docSoapInput = soapMsgtoDocument(soapMessage);
			

			logger.verbose("CrocsSOAPRequestToVertex :quotationRequestToVertex Soap Input XML"
					+ SCXmlUtil.getString(docSoapInput));

			docSoapOutput = callvertexforQuotationRequest(env, docSoapInput, inputDoc);

			return docSoapOutput;

		} catch (Exception e) {
			e.printStackTrace();
			throw new YFCException(
					"CrocsSOAPRequestToVertex: Error the method quotationRequestToVertex : OrderNo" + strOrdNo);
		}

	}
	/**
	 *  EOMS- 3240 Vertex tax total in the SOPA response does not match the order total in Call center
	 * @param env
	 * @param inputDoc
	 * @return
	 */
	public Document quotationRequestToRecalculateTaxes(YFSEnvironment env, Document inputDoc) {
		Document docSoapOutput = null;
		Element eleOrd =  inputDoc.getDocumentElement();
		String strOrdNo = eleOrd.getAttribute(CrocsXmlConstants.A_ORDER_NO);
		
         
		try {
			logger.verbose("CrocsSOAPRequestToVertex : quotationRequestToRecalculateTaxes Begin" + SCXmlUtil.getString(inputDoc));

			// EOMS-4538 Vertex Changes for Apple Pay - start

			String strPaymentType = null;
			Element elePaymentMethods = (Element) eleOrd.getElementsByTagName(CrocsXmlConstants.E_PAYMENT_METHODS)
					.item(0);
			Element elePaymentMethod = (Element) elePaymentMethods
					.getElementsByTagName(CrocsXmlConstants.E_PAYMENT_METHOD).item(0);
			if (!YFCObject.isVoid(elePaymentMethod)) {
				strPaymentType = elePaymentMethod.getAttribute(CrocsXmlConstants.A_PAYMENT_TYPE);
			}

			// EOMS-4538 Vertex Changes for Apple Pay - End
	         
			String strOrderKey = eleOrd.getAttribute(CrocsXmlConstants.A_ORDER_HEADER_KEY);
			String strEnterpriseCode = eleOrd.getAttribute(CrocsXmlConstants.A_ENTERPRISE_CODE);
			String strOrdDate = eleOrd.getAttribute(CrocsXmlConstants.A_ORDER_DATE);
			String strOrderStatus=eleOrd.getAttribute(CrocsXmlConstants.A_STATUS);
			
			logger.info("OMS_UPDTAE" + "CrocsSOAPRequestToVertex.quotationRequestToRecalculateTaxes() : start of the Method : SO:" +strOrdNo);
			
			Element elePersonInfoShipTo = XMLUtil.getElementByXPath(inputDoc, CrocsXmlConstants.STR_XPATH_ORDER_PERSON_INFO_SHIP_TO);
			Element eleHeaderChargeSC = XMLUtil.getElementByXPath(inputDoc, CrocsXmlConstants.XPATH_SHIPPING_CHARGE_ORDER);
			Element eleHeaderChargeDS = XMLUtil.getElementByXPath(inputDoc, CrocsXmlConstants.XPATH_SHIPPING_DISCOUNT_ORDER);
			Element eleHeaderChargeESP = XMLUtil.getElementByXPath(inputDoc,
					CrocsXmlConstants.XPATH_SHIPPING_EXTENDED_SHIP_PROTECTION_ORDER);
			Element eleSelleradd = addOrgDetails(env, strEnterpriseCode, inputDoc);

			// Add Line Items
			Element orderLines = SCXmlUtil.getChildElement(eleOrd, CrocsXmlConstants.E_ORDER_LINES);
			ArrayList<Element> orderLineList = SCXmlUtil.getChildren(orderLines, CrocsXmlConstants.E_ORDER_LINE);

			// Generate SOAP Message
			SOAPMessage soapMessage = createSOAPMessage();
			SOAPEnvelope soapEnvelope = soapMessage.getSOAPPart().getEnvelope();
			SOAPElement vertexEnvelope = soapEnvelope.getBody().addChildElement(CrocsConstant.V_VERTEX_ENVELOPE,
					CrocsConstant.V_URN);

			// Add Login Details
			addLogin(vertexEnvelope);

			// Add Quotation Request details
			SOAPElement quotationRequest = addRequest(vertexEnvelope, CrocsConstant.V_QUOTATION_REQUEST, strOrdDate,
					strOrderKey, strOrdNo);

			// Add Seller details
			addSellerDetails(quotationRequest, eleSelleradd, strEnterpriseCode);

			// Add Customer details
			addCustomerDetails(quotationRequest, elePersonInfoShipTo);

			// Add Line Item details
			addRepricingLineItems(quotationRequest, orderLineList,strOrderStatus,strPaymentType,strEnterpriseCode);

			// Add Shipping Charge
			addShippingCharge(eleHeaderChargeSC, quotationRequest, orderLineList.size() + 1,strPaymentType,strEnterpriseCode);
			// Add Shipping Discount
			addShippingCharge(eleHeaderChargeDS, quotationRequest, orderLineList.size() + 2,strPaymentType,strEnterpriseCode);

			// Add Extended Ship Protection
			addShippingCharge(eleHeaderChargeESP, quotationRequest, orderLineList.size() + 3,strPaymentType,strEnterpriseCode);

			// Convert SoapMessage to Document
			Document docSoapInput = soapMsgtoDocument(soapMessage);

			logger.verbose("CrocsSOAPRequestToVertex :quotationRequestToRecalculateTaxes Soap Input XML"
					+ SCXmlUtil.getString(docSoapInput));

			docSoapOutput = callvertexforQuotationRequest(env, docSoapInput, inputDoc);
			
			logger.info("OMS_UPDTAE" + "CrocsSOAPRequestToVertex.quotationRequestToRecalculateTaxes() : End of the Method : SO:" +strOrdNo);

			return docSoapOutput;

		} catch (Exception e) {
			e.printStackTrace();
			throw new YFCException(
					"CrocsSOAPRequestToVertex: Error the method quotationRequestToRecalculateTaxes : OrderNo" + strOrdNo);
		}

	}
	/**
	 * @param env
	 * @param inputDoc
	 * @return Document
	 *  * This is called in CreateShipmentInvoice ON Invoice Creation Event and Rturn
	 * Order Invoice Creation Event
	 */
	public Document invoiceRequestToVertex(YFSEnvironment env, Document inputDoc) {

		logger.verbose("CrocsSOAPRequestToVertex : invoiceRequestToVertex Begin" + SCXmlUtil.getString(inputDoc));

		Document docSoapOutput = null;
		Element eleRoot = inputDoc.getDocumentElement();
		String strInvNo = eleRoot.getAttribute(CrocsXmlConstants.A_INVOICE_NO);
		String strOrderNo = eleRoot.getAttribute(CrocsXmlConstants.A_ORDER_NO);
		String strInvKey = eleRoot.getAttribute(CrocsXmlConstants.A_ORDER_INVOICE_KEY);
		String strInvoiceType = eleRoot.getAttribute(CrocsXmlConstants.A_INVOICE_TYPE);
		strInvNo = getDocumentNumber(inputDoc, strInvoiceType, strInvNo, strOrderNo);
		
		logger.info("OMS_UPDTAE" + "CrocsSOAPRequestToVertex.invoiceRequestToVertex() : Start of the Method :" +strInvNo+ " , " + strOrderNo);
		

		try {
			// EOMS-4538 Short Ship Scenario changes - Start
			boolean isVertexCallNeeded = isValidXMLforVertexCall(eleRoot);
			if (isVertexCallNeeded) {
				return inputDoc;
			}
			// EOMS-4538 Short Ship Scenario changes - Start

			Element eleOrder = (Element) eleRoot.getElementsByTagName(CrocsXmlConstants.E_ORDER).item(0);
             
			// EOMS-4538 Vertex Changes for Apple Pay - start
			
			String strPaymentType = null;
			Element elePaymentMethods = (Element) eleOrder.getElementsByTagName(CrocsXmlConstants.E_PAYMENT_METHODS)
					.item(0);
			Element elePaymentMethod = (Element) elePaymentMethods
					.getElementsByTagName(CrocsXmlConstants.E_PAYMENT_METHOD).item(0);
			if (!YFCObject.isVoid(elePaymentMethod)) {
				strPaymentType = elePaymentMethod.getAttribute(CrocsXmlConstants.A_PAYMENT_TYPE);
			}
			
			// EOMS-4538 Vertex Changes for Apple Pay - End
			
			String strEnterpriseCode = eleOrder.getAttribute(CrocsXmlConstants.A_ENTERPRISE_CODE);
			String strOrdDate = eleOrder.getAttribute(CrocsXmlConstants.A_ORDER_DATE);
			Element eleSelleradd = addOrgDetails(env, strEnterpriseCode, inputDoc);
			Element elePersonInfoShipTo = (Element) eleOrder
					.getElementsByTagName(CrocsXmlConstants.E_PERSON_INFO_SHIP_TO).item(0);
			Element eleHeaderChargeSC = XMLUtil.getElementByXPath(inputDoc,
					CrocsXmlConstants.XPATH_INVOICE_SHIPPING_CHARGE);
			Element eleHeaderChargeDS = XMLUtil.getElementByXPath(inputDoc,
					CrocsXmlConstants.XPATH_INVOICE_SHIPPING_DISCOUNT);
			Element eleHeaderChargeESP = XMLUtil.getElementByXPath(inputDoc,
					CrocsXmlConstants.XPATH_INVOICE_SHIPPING_EXTENDED_SHIP_PROTECTION);
			Element orderLines = SCXmlUtil.getChildElement(eleRoot, CrocsXmlConstants.A_LINE_DETAILS);

			// SOAP Message
			SOAPMessage soapMessage = createSOAPMessage();
			SOAPEnvelope soapEnvelope = soapMessage.getSOAPPart().getEnvelope();
			SOAPElement vertexEnvelope = soapEnvelope.getBody().addChildElement(CrocsConstant.V_VERTEX_ENVELOPE,
					CrocsConstant.V_URN);
			//EOMS - 4337 - start mapping Sales Order Ship To address to return Invoice 
			if (CrocsXmlConstants.A_INVOICE_RETURN.equalsIgnoreCase(strInvoiceType)) {
				Element eleSalesOrderEle =getSalesOrderShippingAddressForReturn(env, inputDoc, strInvoiceType,
						strEnterpriseCode);				
				if (!YFCCommon.isVoid(eleSalesOrderEle) || !YFCObject.isNull(eleSalesOrderEle)) {
					Element eleReturnPaymentMethod = (Element) eleSalesOrderEle.getElementsByTagName(CrocsXmlConstants.E_PAYMENT_METHOD).item(0);
					if(!YFCObject.isVoid(eleReturnPaymentMethod)) {
						strPaymentType = eleReturnPaymentMethod.getAttribute(CrocsXmlConstants.A_PAYMENT_TYPE);
					}
					Element eleSOPersonInfoShipTo = (Element) eleSalesOrderEle.getElementsByTagName(CrocsXmlConstants.E_PERSON_INFO_SHIP_TO).item(0);
					if (!YFCCommon.isVoid(eleSOPersonInfoShipTo)) {
						elePersonInfoShipTo = eleSOPersonInfoShipTo;
					}
				}
			}
			//EOMS - 4337 End

			// Add Login Details
			addLogin(vertexEnvelope);

			// SOAP InvoiveRequest
			SOAPElement invoiceRequest = addRequest(vertexEnvelope, CrocsConstant.V_INVOICE_REQUEST, strOrdDate,
					strInvKey, strInvNo);

			// Add Seller Details
			addSellerDetails(invoiceRequest, eleSelleradd, strEnterpriseCode);

			// Add Customer Details
			addCustomerDetails(invoiceRequest, elePersonInfoShipTo);

			// Add Line Items
			ArrayList<Element> orderLineList = SCXmlUtil.getChildren(orderLines, CrocsXmlConstants.A_LINE_DETAIL);

			// Add Invoice Details
			addInvoiceLineItems(invoiceRequest, orderLineList,strPaymentType,strEnterpriseCode);

			// Add Shipping Charge
			addShippingCharge(eleHeaderChargeSC, invoiceRequest, orderLineList.size() + 1,strPaymentType,strEnterpriseCode);

			// Add Shipping Discount
			addShippingCharge(eleHeaderChargeDS, invoiceRequest, orderLineList.size() + 2,strPaymentType,strEnterpriseCode);

			// Add Extended Ship Protection
			addShippingCharge(eleHeaderChargeESP, invoiceRequest, orderLineList.size() + 3,strPaymentType,strEnterpriseCode);

			logger.verbose("CrocsSOAPRequestToVertex : invoiceRequestToVertex Vertex Input" + soapMessage.toString());

			Document docSoapInput = soapMsgtoDocument(soapMessage);

			logger.verbose("CrocsSOAPRequestToVertex :invoiceRequestToVertex Soap Input XML"
					+ SCXmlUtil.getString(docSoapInput));

			docSoapOutput = callvertexforInvoiceRequest(env, docSoapInput, inputDoc);
			
			logger.info("OMS_UPDTAE" + "CrocsSOAPRequestToVertex.invoiceRequestToVertex() : End of the Method :" +strInvNo+ " , " + strOrderNo);

		} catch (Exception e) {
			e.printStackTrace();
			throw new YFCException(
					"CrocsSOAPRequestToVertex:  Error in the method invoiceRequestToVertex : InvoiceKey" + strInvKey);
		}
		logger.verbose("CrocsSOAPRequestToVertex :invoiceRequestToVertex Soap output XML"
				+ SCXmlUtil.getString(docSoapOutput));

		return docSoapOutput;

	}
    
	/**
	 * @param eleOrderInvoice
	 * @return EOMS-4538 In the case of a full cancellation due to short shipment
	 *         from WMS, the Vertex call should be skipped.
	 */
	public boolean isValidXMLforVertexCall(Element eleOrderInvoice) {
		Element eleLineDetails = (Element) eleOrderInvoice.getElementsByTagName(CrocsXmlConstants.E_LINE_DETAILS)
				.item(0);
		if (YFCObject.isVoid(eleLineDetails)) {
			return true;
		}
		NodeList nlLineDetail = eleLineDetails.getElementsByTagName(CrocsXmlConstants.E_LINE_DETAIL);
		for (int i = 0; i < nlLineDetail.getLength(); i++) {
			Element eleLineDetail = (Element) nlLineDetail.item(i);
			String strShippedQty = eleLineDetail.getAttribute("ShippedQty");
			double dShippedQty = Double.parseDouble(strShippedQty);
			if (dShippedQty != 0.00) {
				return false; // At least one line has shipped quantity
			}
		}
		return true;

	}
	/**
	 * @param inputDoc
	 * @param strInvoiceType
	 * @param strInvNo
	 * @param strOrderNo
	 * @return
	 * this methods returns documentNo has salesOrderNo_InvoiceNo
	 */
	private String getDocumentNumber(Document inputDoc, String strInvoiceType, String strInvNo, String strOrderNo) {
	    if (CrocsXmlConstants.A_INVOICE_RETURN.equalsIgnoreCase(strInvoiceType)) {
	        Element eleDerivedFromOrder = (Element) inputDoc.getElementsByTagName(CrocsXmlConstants.E_DERIVED_FROM_ORDER).item(0);
	        if (eleDerivedFromOrder != null) {
	            String strSalesOrderNo = eleDerivedFromOrder.getAttribute(CrocsXmlConstants.A_ORDER_NO);
	            if (strSalesOrderNo != null && !strSalesOrderNo.isEmpty()) {
	                return strSalesOrderNo + "_" + strInvNo;
	            }
	        }
	    }
	    return strOrderNo + "_" + strInvNo;
	}
	/**
	 * @param env
	 * @param inputDoc
	 * @param invoiceType
	 * @param enterpriseCode
	 * @return
	 * @throws Exception
	 * EOMS - 4337 This method is used map the Shipping Address of Sales order to Return invoice
	 */
	private Element getSalesOrderShippingAddressForReturn(
	        YFSEnvironment env, Document inputDoc, String invoiceType,String enterpriseCode) throws Exception {

	    if (!CrocsXmlConstants.A_INVOICE_RETURN.equalsIgnoreCase(invoiceType)) return null;

	    Element derivedOrder = (Element) inputDoc.getElementsByTagName(CrocsXmlConstants.E_DERIVED_FROM_ORDER).item(0);
	    if (YFCCommon.isVoid(derivedOrder)) return null;

	    String salesOrderNo = derivedOrder.getAttribute(CrocsXmlConstants.A_ORDER_NO);
	    if (YFCCommon.isVoid(salesOrderNo) || salesOrderNo.isEmpty()) return null;

	    Document getOrderDetailsDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER);
	    Element eleOrder = getOrderDetailsDoc.getDocumentElement();
	    eleOrder.setAttribute(CrocsXmlConstants.A_ORDER_NO, salesOrderNo);
	    eleOrder.setAttribute(CrocsXmlConstants.A_DOCUMENT_TYPE, CrocsConstant.A_SALES_ORDER_DOCUMENT_TYPE);
	    eleOrder.setAttribute(CrocsXmlConstants.A_ENTERPRISE_CODE, enterpriseCode);

	    Document docGetOrderList = CommonUtil.invokeAPI(
	        env,
	        CrocsTemplateConstants.TEMPLATE_MODIFY_GET_ORDER_LIST_FOR_RETURN_INVOICE,
	        CrocsAPIConstants.API_GET_ORDER_LIST,
	        getOrderDetailsDoc
	    );
	  
	    return (Element) docGetOrderList.getElementsByTagName(CrocsXmlConstants.E_ORDER).item(0);
	}	
	/**
	 * @param strEnterpriseCode
	 * @return String
	 * getCompanyCode for US and CA Enterprise
	 */
	private String getCompanyCode(String strEnterpriseCode) {

		String strCode = null;
		if (CrocsConstant.CROCS_US.equals(strEnterpriseCode)) {
			strCode = CrocsConstant.A_1000;
		} else if (CrocsConstant.CROCS_CA.equals(strEnterpriseCode)) {
			strCode = CrocsConstant.A_1030;
		} else if (CrocsConstant.HEYDUDE_US.equals(strEnterpriseCode)) {
			strCode = CrocsConstant.A_4100;
		} else if (CrocsConstant.HEYDUDE_CA.equals(strEnterpriseCode)) {
			strCode = CrocsConstant.A_4120;
		}

		return strCode;

	} 
	/**
	 * @return SOAPMessage
	 * @throws SOAPException
	 * Create SOAPMessage
	 */
	private SOAPMessage createSOAPMessage() throws SOAPException {
		MessageFactory messageFactory = MessageFactory.newInstance();
		SOAPMessage soapMessage = messageFactory.createMessage();
		SOAPEnvelope soapEnvelope = soapMessage.getSOAPPart().getEnvelope();
		soapEnvelope.addNamespaceDeclaration(CrocsConstant.V_SOAP_ENV, CrocsConstant.V_SOAP_ENV_URL);
		soapEnvelope.addNamespaceDeclaration(CrocsConstant.V_URN, CrocsConstant.V_URN_URL);

		logger.verbose("CrocsSOAPRequestToVertex : createSOAPMessage " + soapMessage.toString());

		return soapMessage;
	}
	/**
	 * @param parentElement
	 * @throws SOAPException
	 * add Login Details 
	 */
	private void addLogin(SOAPElement parentElement) throws SOAPException {
		SOAPElement login = parentElement.addChildElement(CrocsConstant.V_LOGIN, CrocsConstant.V_URN);
		login.addChildElement(CrocsConstant.V_USERNAME, CrocsConstant.V_URN)
				.addTextNode(YFSSystem.getProperty(CrocsConstant.V_USER));
		login.addChildElement(CrocsConstant.V_PASSWORD, CrocsConstant.V_URN)
				.addTextNode(YFSSystem.getProperty(CrocsConstant.V_PASSWORD_KEY));
		login.addChildElement(CrocsConstant.V_TRUSTED_ID, CrocsConstant.V_URN)
				.addTextNode(YFSSystem.getProperty(CrocsConstant.V_TRUSTED_KEY));

		logger.verbose("CrocsSOAPRequestToVertex : addLogin Method ");
	}
	/**
	 * @param parentElement
	 * @param requestType
	 * @param strOrdDate
	 * @param transactionId
	 * @param documentNumber
	 * @return parentElement
	 * @throws SOAPException
	 * add Login Quotation/Invoice Request
	 */
	private SOAPElement addRequest(SOAPElement parentElement, String requestType, String strOrdDate,
			String transactionId, String documentNumber) throws SOAPException {
		SOAPElement request = parentElement.addChildElement(requestType, CrocsConstant.V_URN);
		request.setAttribute(CrocsConstant.V_DOCUMENT_DATE, strOrdDate);
		request.setAttribute(CrocsConstant.V_TRANSACTION_TYPE, CrocsConstant.V_SALE);
		request.setAttribute(CrocsConstant.V_TRANSACTION_ID, transactionId);
		request.setAttribute(CrocsConstant.V_DOCUMENT_NUMBER, documentNumber);

		logger.verbose("CrocsSOAPRequestToVertex : addRequest Method " + request.toString());
		return request;
	}
	/**
	 * @param request
	 * @param eleAddress
	 * @param strEnterpriseCode
	 * @throws SOAPException
	 * add Login Seller Details
	 */
	private void addSellerDetails(SOAPElement request, Element eleAddress, String strEnterpriseCode)
			throws SOAPException {
		SOAPElement seller = request.addChildElement(CrocsConstant.V_SELLER, CrocsConstant.V_URN);
		String strCode = getCompanyCode(strEnterpriseCode);
		seller.addChildElement(CrocsConstant.V_COMPANY, CrocsConstant.V_URN).addTextNode(strCode);

		SOAPElement physicalOrigin = seller.addChildElement(CrocsConstant.V_PHYSICAL_ORIGIN, CrocsConstant.V_URN);
		addAddressElements(physicalOrigin, eleAddress);

		SOAPElement administrativeOrigin = seller.addChildElement(CrocsConstant.V_ADMINISTRATIVE_ORIGIN,
				CrocsConstant.V_URN);
		addAddressElements(administrativeOrigin, eleAddress);

		logger.verbose("CrocsSOAPRequestToVertex : addSellerDetails Method ");

	}
	/**
	 * @param request
	 * @param eleAddress
	 * @throws SOAPException
	 * add Customer Details
	 */
	private void addCustomerDetails(SOAPElement request, Element eleAddress) throws SOAPException {
		SOAPElement customer = request.addChildElement(CrocsConstant.V_CUSTOMER, CrocsConstant.V_URN);
		SOAPElement destination = customer.addChildElement(CrocsConstant.V_DESTINATION, CrocsConstant.V_URN);
		addAddressElements(destination, eleAddress);
		customer.addChildElement(CrocsConstant.V_IS_TAX_EXEMPT, CrocsConstant.V_URN).addTextNode(CrocsConstant.V_FALSE);
		customer.addChildElement(CrocsConstant.V_EXEMPTION_REASON_CODE, CrocsConstant.V_URN).addTextNode("");

		logger.verbose("CrocsSOAPRequestToVertex : addCustomerDetails Method ");
	}
	/**
	 * @param request
	 * @param orderLineList
	 * @throws SOAPException
	 * add Line Details
	 */
	private void addLineItems(SOAPElement request, ArrayList<Element> orderLineList,String strPaymentType, String strEnterpriseCode)  throws SOAPException {		
		for (Element orderLine : orderLineList) {
			 Map<String, String> lineItemMap = new HashMap<>();

		        String strPrimeLineNo = orderLine.getAttribute(CrocsXmlConstants.A_PRIME_LINE_NO);
		        String strOrderedQty = orderLine.getAttribute(CrocsXmlConstants.A_ORDERED_QTY);

		        Element eItem = (Element) orderLine.getElementsByTagName(CrocsXmlConstants.E_ITEM_DETAILS).item(0);
		        Element eClassificationCodes = (Element) eItem.getElementsByTagName(CrocsXmlConstants.E_CLASSIFICATION_CODES).item(0);
		        Element elePersonShipTo = (Element) orderLine.getElementsByTagName(CrocsXmlConstants.E_PERSON_INFO_SHIP_TO).item(0);
		        Element eleLineTotals = (Element) orderLine.getElementsByTagName(CrocsXmlConstants.E_LINE_OVERALL_TOTALS).item(0);

		        String strProductClass = eClassificationCodes.getAttribute(CrocsXmlConstants.A_TAX_PRODUCT_CODE);
		        String strProduct = eItem.getAttribute(CrocsXmlConstants.A_ITEM_ID);
		        String strLineTotal = eleLineTotals.getAttribute(CrocsXmlConstants.A_LINE_TOTAL_WITHOUT_TAX);
                 
				/**
				 * As per sonar lint guidelines, a method should not have more than 7 arguments.
				 * Hence, we are consolidating all string parameters into a map and passing it
				 * to the addLineItem method.
				 */

				lineItemMap.put(CrocsXmlConstants.A_PRIME_LINE_NO, strPrimeLineNo);
				lineItemMap.put(CrocsXmlConstants.A_ORDERED_QTY, strOrderedQty);
				lineItemMap.put(CrocsXmlConstants.A_TAX_PRODUCT_CODE, strProductClass);
				lineItemMap.put(CrocsXmlConstants.A_ITEM_ID, strProduct);
				lineItemMap.put(CrocsXmlConstants.A_LINE_TOTAL_WITHOUT_TAX, strLineTotal);
				lineItemMap.put(CrocsXmlConstants.A_PAYMENT_TYPE, strPaymentType);
				lineItemMap.put(CrocsXmlConstants.A_ENTERPRISE_CODE, strEnterpriseCode);

				addLineItem(request, lineItemMap, elePersonShipTo);

			logger.verbose("CrocsSOAPRequestToVertex : addLineItems Method ");
		}
	}
	/**
	 * @param request
	 * @param orderLineList
	 * @throws SOAPException
	 * add Line Details
	 */
	private void addRepricingLineItems(SOAPElement request, ArrayList<Element> orderLineList,String strOrderStatus,String strPaymentType,String strEnterpriseCode)  throws SOAPException {	
		for (Element orderLine : orderLineList) {
			String strStatus = orderLine.getAttribute(CrocsXmlConstants.A_STATUS);
			logger.verbose("CrocsSOAPRequestToVertex : addLineItems Method : Status of the OrderLine" + strStatus);
			/*
			 * This condition is added to handle order creation from the call center,
			 * where the RepricingUE input may not include the 'status' attribute at the
			 * OrderLine level when the line is first added to the order. 
			 * In such cases, if the overall OrderStatus is "Draft Order Created", 
			 * we use it as the line status.
			 */		
			if ((!YFCCommon.isVoid(strStatus) && !CrocsConstant.STR_CANCELLED.equalsIgnoreCase(strStatus))||(YFCCommon.isVoid(strStatus)
					&& strOrderStatus.equalsIgnoreCase(CrocsXmlConstants.STR_DRAFT_ORDER_CREATED))) {
				String strPrimeLineNo = orderLine.getAttribute(CrocsXmlConstants.A_PRIME_LINE_NO);
			String strOrderedQty = orderLine.getAttribute(CrocsXmlConstants.A_ORDERED_QTY);
			Element eItem = (Element) orderLine.getElementsByTagName(CrocsXmlConstants.E_ITEM_DETAILS).item(0);
			Element eClassificationCodes = (Element) eItem
					.getElementsByTagName(CrocsXmlConstants.E_CLASSIFICATION_CODES).item(0);
			Element elePersonShipTo = (Element) orderLine.getElementsByTagName(CrocsXmlConstants.E_PERSON_INFO_SHIP_TO)
					.item(0);
			String strProductClass = eClassificationCodes.getAttribute(CrocsXmlConstants.A_TAX_PRODUCT_CODE);
			String strProduct = eItem.getAttribute(CrocsXmlConstants.A_ITEM_ID);
			String strLineTotal = ((Element) orderLine.getElementsByTagName(CrocsXmlConstants.E_LINE_OVERALL_TOTALS)
					.item(0)).getAttribute(CrocsXmlConstants.A_EXTENDED_PRICE);
			NodeList nlLineCharges = orderLine.getElementsByTagName(CrocsXmlConstants.E_LINE_CHARGE);
			double lineTotal = Double.parseDouble(strLineTotal);			
			double ddiscount = 0.0;
			double dCharge =0.0;
			for (int i = 0; i < nlLineCharges.getLength(); i++) {
			    Element eleLineCharge = (Element) nlLineCharges.item(i); // Use 'i' instead of '0' to iterate correctly
			    String strChargeCategory = eleLineCharge.getAttribute(CrocsXmlConstants.A_CHARGE_CATEGORY);
			    String strChargeAmount = eleLineCharge.getAttribute(CrocsXmlConstants.A_CHARGE_PER_LINE);
			    Double strAmount = Double.valueOf(strChargeAmount);
			    if (strChargeCategory.contains("Discount")) {
			        ddiscount = ddiscount + strAmount;
			    } else {
			        dCharge = dCharge + strAmount;
			    }
			}
			
			lineTotal = lineTotal-ddiscount+dCharge;
			strLineTotal = String.valueOf(lineTotal);
			
			/**
			 * As per sonar lint guidelines, a method should not have more than 7 arguments.
			 * Hence, we are consolidating all string parameters into a map and passing it
			 * to the addLineItem method.
			 */

			Map<String, String> lineItemMap = new HashMap<>();
			lineItemMap.put(CrocsXmlConstants.A_PRIME_LINE_NO, strPrimeLineNo);
			lineItemMap.put(CrocsXmlConstants.A_ORDERED_QTY, strOrderedQty);
			lineItemMap.put(CrocsXmlConstants.A_TAX_PRODUCT_CODE, strProductClass);
			lineItemMap.put(CrocsXmlConstants.A_ITEM_ID, strProduct);
			lineItemMap.put(CrocsXmlConstants.A_LINE_TOTAL_WITHOUT_TAX, strLineTotal);
			lineItemMap.put(CrocsXmlConstants.A_PAYMENT_TYPE, strPaymentType);
			lineItemMap.put(CrocsXmlConstants.A_ENTERPRISE_CODE, strEnterpriseCode);
			

			addLineItem(request, lineItemMap, elePersonShipTo);

			logger.verbose("CrocsSOAPRequestToVertex : addLineItems Method ");
			}
		}
	}
	/**
	 * @param request
	 * @param orderLineList
	 * @throws SOAPException
	 * add Invoice Line Details
	 */
	private void addInvoiceLineItems(SOAPElement request, ArrayList<Element> orderLineList,String strPaymentType,String strEnterpriseCode)  throws SOAPException {	
		int count = 0;
		for (Element elelineDetails : orderLineList) {
			count++;
			String strshippedQty = elelineDetails.getAttribute(CrocsXmlConstants.A_SHIPPED_QTY);
			double dShippedQty = Double.parseDouble(strshippedQty);
			if(dShippedQty!=0.00) {
			Element eleItem = (Element) elelineDetails.getElementsByTagName(CrocsXmlConstants.E_ITEM_DETAILS).item(0);
			Element elePersonInfoShipTo = (Element) elelineDetails
					.getElementsByTagName(CrocsXmlConstants.E_PERSON_INFO_SHIP_TO).item(0);
			String strProduct = eleItem.getAttribute(CrocsXmlConstants.A_ITEM_ID);
			String strProductClass = ((Element) eleItem.getElementsByTagName(CrocsXmlConstants.E_CLASSIFICATION_CODES)
					.item(0)).getAttribute(CrocsXmlConstants.A_TAX_PRODUCT_CODE);
			float exPrice = Float.parseFloat(elelineDetails.getAttribute(CrocsXmlConstants.A_LINE_TOTAL))
					- Float.parseFloat(elelineDetails.getAttribute(CrocsXmlConstants.A_TAX));
			
			/**
			 * As per sonar lint guidelines, a method should not have more than 7 arguments.
			 * Hence, we are consolidating all string parameters into a map and passing it
			 * to the addLineItem method.
			 */

			Map<String, String> lineItemMap = new HashMap<>();
			lineItemMap.put(CrocsXmlConstants.A_PRIME_LINE_NO, String.valueOf(count));
			lineItemMap.put(CrocsXmlConstants.A_TAX_PRODUCT_CODE, strProductClass);
			lineItemMap.put(CrocsXmlConstants.A_ITEM_ID, strProduct);
			lineItemMap.put(CrocsXmlConstants.A_ORDERED_QTY, strshippedQty);
			lineItemMap.put(CrocsXmlConstants.A_LINE_TOTAL_WITHOUT_TAX, String.valueOf(exPrice));
			lineItemMap.put(CrocsXmlConstants.A_PAYMENT_TYPE, strPaymentType);
			lineItemMap.put(CrocsXmlConstants.A_ENTERPRISE_CODE, strEnterpriseCode);

			addLineItem(request, lineItemMap, elePersonInfoShipTo);
			}
		}
		logger.verbose("CrocsSOAPRequestToVertex : addLineItem Method ");
	} 
	/**
	 * @param eleHeaderChargeSC
	 * @param request
	 * @param lineNo
	 * @throws SOAPException
	 * add Shipping Charge Details
	 */
	private void addShippingCharge(Element eleHeaderChargeSC, SOAPElement request, int lineNo,String strPaymentType, String strEnterpriseCode)  throws SOAPException {
		float fChargeAmount = 0.0f;
		Element elePersonInfoShipTo = null;
		String strShippingCode = null;
		try {
			if(!YFCObject.isVoid(eleHeaderChargeSC)&& eleHeaderChargeSC!=null) {
			String strChargeCategory = eleHeaderChargeSC.getAttribute(CrocsXmlConstants.A_CHARGE_CATEGORY);
			if (strChargeCategory.equalsIgnoreCase(CrocsConstant.A_EXTENDED_SHIPPING_PROTECTION)) {
				strShippingCode = CrocsConstant.A_SHIPPING_EXTENDED_TAX_CODE;
			} else {
				strShippingCode = CrocsConstant.A_SHIPPING_TAX_CODE;
			}
			if (!YFCObject.isVoid(eleHeaderChargeSC)) {
				String strChargeAmount = eleHeaderChargeSC.getAttribute(CrocsXmlConstants.A_CHARGE_AMOUNT);
				if (strChargeAmount != null && !strChargeAmount.isEmpty()) {
					fChargeAmount = Float.parseFloat(strChargeAmount);
				}
			}
			if (strChargeCategory.equalsIgnoreCase(CrocsXmlConstants.A_SHIPPING_DISCOUNT)) {
				fChargeAmount=-fChargeAmount;
			}
			
			/**
			 * As per sonar lint guidelines, a method should not have more than 7 arguments.
			 * Hence, we are consolidating all string parameters into a map and passing it
			 * to the addLineItem method.
			 */

			Map<String, String> shippingLineMap = new HashMap<>();
			shippingLineMap.put(CrocsXmlConstants.A_PRIME_LINE_NO, String.valueOf(lineNo));
			shippingLineMap.put(CrocsXmlConstants.A_TAX_PRODUCT_CODE, strShippingCode);
			shippingLineMap.put(CrocsXmlConstants.A_ITEM_ID,
					eleHeaderChargeSC.getAttribute(CrocsXmlConstants.A_CHARGE_NAME));
			shippingLineMap.put(CrocsXmlConstants.A_ORDERED_QTY, "1");
			shippingLineMap.put(CrocsXmlConstants.A_LINE_TOTAL_WITHOUT_TAX, String.valueOf(fChargeAmount));
			shippingLineMap.put(CrocsXmlConstants.A_PAYMENT_TYPE, strPaymentType);
			shippingLineMap.put(CrocsXmlConstants.A_ENTERPRISE_CODE, strEnterpriseCode);
			addLineItem(request, shippingLineMap, elePersonInfoShipTo);
            
			}
		} catch (NumberFormatException e) {

			logger.error(e.getMessage());

		}
	}
	/**
	 * @param element
	 * @param eleaddress
	 * @throws SOAPException
	 * add Address Details
	 */
	private void addAddressElements(SOAPElement element, Element eleaddress) throws SOAPException {
		if(!YFCObject.isVoid(eleaddress)&&eleaddress!=null ) {
		element.addChildElement(CrocsConstant.V_STREET_ADDRESS1, CrocsConstant.V_URN)
				.addTextNode(eleaddress.getAttribute(CrocsXmlConstants.A_ADDRESS_LINE_1));
		element.addChildElement(CrocsConstant.V_STREET_ADDRESS2, CrocsConstant.V_URN)
				.addTextNode(eleaddress.getAttribute(CrocsXmlConstants.A_ADDRESS_LINE_2));
		element.addChildElement(CrocsConstant.V_CITY, CrocsConstant.V_URN)
				.addTextNode(eleaddress.getAttribute(CrocsXmlConstants.A_CITY));
		element.addChildElement(CrocsConstant.V_MAIN_DIVISION, CrocsConstant.V_URN)
				.addTextNode(eleaddress.getAttribute(CrocsXmlConstants.A_STATE));
		element.addChildElement(CrocsConstant.V_POSTAL_CODE, CrocsConstant.V_URN)
				.addTextNode(eleaddress.getAttribute(CrocsXmlConstants.A_ZIP_CODE));
		element.addChildElement(CrocsConstant.V_COUNTRY, CrocsConstant.V_URN)
				.addTextNode(eleaddress.getAttribute(CrocsXmlConstants.A_COUNTRY));
		logger.verbose("CrocsSOAPRequestToVertex : addAddressElements Method ");
		}
	}
	/**
	 * @param parent
	 * @param lineItemNumber
	 * @param productClass
	 * @param product
	 * @param quantity
	 * @param extendedPrice
	 * @param elePersonInfoShipTo
	 * @throws SOAPException
	 * add QuotaTion Line Details
	 */
	private void addLineItem(SOAPElement parent,Map<String, String> lineItemMap, Element elePersonInfoShipTo) 
			throws SOAPException {
		
		String strPaymentType=lineItemMap.get(CrocsXmlConstants.A_PAYMENT_TYPE);
		String strUnitOfMeasure = CrocsConstant.V_EA;
		SOAPElement lineItem = parent.addChildElement(CrocsConstant.V_LINE_ITEM, CrocsConstant.V_URN);
		lineItem.setAttribute(CrocsConstant.V_LINE_ITEM_NUMBER, lineItemMap.get(CrocsXmlConstants.A_PRIME_LINE_NO));

		SOAPElement productElement = lineItem.addChildElement(CrocsConstant.V_PRODUCT, CrocsConstant.V_URN);
		productElement.setAttribute(CrocsConstant.V_PRODUCT_CLASS, lineItemMap.get(CrocsXmlConstants.A_TAX_PRODUCT_CODE));
		productElement.addTextNode(lineItemMap.get(CrocsXmlConstants.A_ITEM_ID));

		SOAPElement quantityElement = lineItem.addChildElement(CrocsConstant.V_QUANTITY, CrocsConstant.V_URN);
		quantityElement.setAttribute(CrocsConstant.V_UNIT_OF_MEASURE, strUnitOfMeasure);
		quantityElement.addTextNode(lineItemMap.get(CrocsXmlConstants.A_ORDERED_QTY));

		SOAPElement flexibleFields = lineItem.addChildElement(CrocsConstant.V_FLEXIBLE_FIELDS, CrocsConstant.V_URN);
		SOAPElement flexibleCodeField1 = flexibleFields.addChildElement(CrocsConstant.V_FLEXIBLE_CODE_FIELD,
				CrocsConstant.V_URN);
		flexibleCodeField1.setAttribute(CrocsConstant.V_FIELD_ID, "2");
		flexibleCodeField1.addTextNode(CrocsConstant.STR_OMS);
		SOAPElement flexibleCodeField2 = flexibleFields.addChildElement(CrocsConstant.V_FLEXIBLE_CODE_FIELD,
				CrocsConstant.V_URN);
		flexibleCodeField2.setAttribute(CrocsConstant.V_FIELD_ID, "19");
		flexibleCodeField2.addTextNode(CrocsConstant.STR_WEB);
		
		/**
		 * EOMS-4538 When the paymentType is 'Apple Pay', include the following field in
		 * the request:
		 * <urn:FlexibleCodeField fieldId="7">APPLEPAY</urn:FlexibleCodeField>
		 **/

		if (!YFCObject.isNull(strPaymentType) && strPaymentType.equalsIgnoreCase(CrocsConstant.A_APPLE_PAY)) {
			SOAPElement flexibleCodeField3 = flexibleFields.addChildElement(CrocsConstant.V_FLEXIBLE_CODE_FIELD,
					CrocsConstant.V_URN);
			flexibleCodeField3.setAttribute(CrocsConstant.V_FIELD_ID, "7");
			flexibleCodeField3.addTextNode(CrocsConstant.STR_APPLE_PAY);
		}

		//EOMS-6182:HeyDude US Tax Implementation --END
		String enterpriseCode = lineItemMap.get(CrocsXmlConstants.A_ENTERPRISE_CODE);
		if (!YFCObject.isVoid(enterpriseCode)) {
		    String value = null;
		    if (CrocsConstant.CROCS_US.equalsIgnoreCase(enterpriseCode) || CrocsConstant.CROCS_CA.equalsIgnoreCase(enterpriseCode)) {
		        value = "CROCS";
		    } else if (CrocsConstant.HEYDUDE_US.equalsIgnoreCase(enterpriseCode) || CrocsConstant.HEYDUDE_CA.equalsIgnoreCase(enterpriseCode)) {
		        value = "HEYDUDE";
		    }
		    if (value != null) {
		        SOAPElement flexibleCodeField = flexibleFields.addChildElement(
		                CrocsConstant.V_FLEXIBLE_CODE_FIELD,
		                CrocsConstant.V_URN
		        );
		        flexibleCodeField.setAttribute(CrocsConstant.V_FIELD_ID, "13");
		        flexibleCodeField.addTextNode(value);
		    }
		}
		//EOMS-6182:HeyDude US Tax Implementation --END
		
		lineItem.addChildElement(CrocsConstant.V_EXTENDED_PRICE, CrocsConstant.V_URN).addTextNode(lineItemMap.get(CrocsXmlConstants.A_LINE_TOTAL_WITHOUT_TAX));

		if (!YFCObject.isVoid(elePersonInfoShipTo)) {

			addCustomerDetails(lineItem, elePersonInfoShipTo);

		}

		logger.verbose("CrocsSOAPRequestToVertex : addLineItem Method ");
	}
	/**
	 * @param env
	 * @param strEnterpriseCode
	 * @return Element - Organization Corporate Address
	 * @throws Exception 
	 */
	private Element addOrgDetails(YFSEnvironment env, String strEnterpriseCode, Document inputDoc)
			throws Exception {
		
		logger.info("CrocsSOAPRequestToVertex : addOrgDetails : Input" + SCXmlUtil.getString(inputDoc));
		
		// EOMS-8453 : Crocs CA Cross Border Ship changes - START
		String strShipNode = "";

		if (!YFCCommon.isVoid(inputDoc)
		        && inputDoc.getDocumentElement() != null
		        && inputDoc.getDocumentElement().hasChildNodes()
		        && CrocsXmlConstants.E_ORDER_INVOICE
		                .equalsIgnoreCase(inputDoc.getDocumentElement().getTagName())) {
			
		    Element eleRoot = inputDoc.getDocumentElement();
		    String strDocumentType = eleRoot.getAttribute(CrocsXmlConstants.A_DOCUMENT_TYPE);

		    if (CrocsConstant.CROCS_CA.equalsIgnoreCase(strEnterpriseCode) || CrocsConstant.HEYDUDE_US.equalsIgnoreCase(strEnterpriseCode)
		    		|| CrocsConstant.HEYDUDE_CA.equalsIgnoreCase(strEnterpriseCode) && CrocsConstant.VAL_DOCUMENT_TYPE_SALES_ORDER
		                    .equalsIgnoreCase(strDocumentType)) {
		    
		        strShipNode = eleRoot.getAttribute(CrocsXmlConstants.A_SHIP_NODE);
		    }
		}
		if(YFCCommon.isVoid(strShipNode)) {
			
			if(CrocsConstant.HEYDUDE_US.equalsIgnoreCase(strEnterpriseCode))
				strShipNode = CommonUtil.getShipNodeforReturn(env, strEnterpriseCode);
			else if(CrocsConstant.HEYDUDE_CA.equalsIgnoreCase(strEnterpriseCode))
				strShipNode = CrocsConstant.HDCA_SHIP_NODE;
			else
				strShipNode = CrocsConstant.A_SHIPNODE_CA;
		}
		// EOMS-8453 : Crocs CA Cross Border Ship changes - END
		
		Map<String, String> orgMap = new HashMap<>();
		orgMap.put(CrocsConstant.CROCS_US, CrocsConstant.A_SHIPNODE_US);
		orgMap.put(CrocsConstant.CROCS_CA, strShipNode);
		orgMap.put(CrocsConstant.HEYDUDE_US, strShipNode);
		orgMap.put(CrocsConstant.HEYDUDE_CA, strShipNode);
		
		logger.info("CrocsSOAPRequestToVertex : ShipNode " + orgMap);
		
		String orgCode = orgMap.get(strEnterpriseCode);
		if (orgCode == null) {
			throw new YFSException("Invalid Enterprise Code: " + strEnterpriseCode);
		}

		// Creating Organization document
		YFCDocument getOrgListDoc = YFCDocument.createDocument(CrocsConstant.E_ORGANIZATION);
		YFCElement getOrgListEle = getOrgListDoc.getDocumentElement();
		getOrgListEle.setAttribute(CrocsXmlConstants.A_ORGANIZATION_CODE, orgCode);
		getOrgListEle.setAttribute(CrocsConstant.A_ORGANIZATION_KEY, orgCode);

		// Executing API call
		YIFApi api = YIFClientFactory.getInstance().getLocalApi();

		Document getOrgList = api.executeFlow(env, CrocsConstant.A_CROCS_GET_ORGANIZATION_LIST,
				getOrgListDoc.getDocument());

		if (getOrgList == null) {
			throw new YFSException("No response received from getOrganizationList API");
		}

		Element eleOrgList = getOrgList.getDocumentElement();
		Element eleOrg = (Element) eleOrgList.getElementsByTagName(CrocsConstant.E_ORGANIZATION).item(0);

		if (eleOrg == null) {
			throw new YFSException("No Organization element found in response");
		}
		logger.verbose("CrocsSOAPRequestToVertex : addOrgDetails Method ");
		return (Element) eleOrg.getElementsByTagName(CrocsConstant.E_CORPORATE_PERSON_INFO).item(0);

	}
	/**
	 * @param soapMessage
	 * @return
	 * @throws SOAPException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 *  Convert SoapMessage to Document
	 */
	public Document soapMsgtoDocument(SOAPMessage soapMessage)
			throws SOAPException, IOException, ParserConfigurationException, SAXException {
		if (soapMessage == null) {
			throw new IllegalArgumentException("SOAPMessage cannot be null");
		}

		// Convert SOAPMessage to String
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		soapMessage.writeTo(outputStream);
		String soapMessageString = outputStream.toString();

		// Parse String to Document
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setXIncludeAware(false);

		// Disable DTDs and external entity resolution
		// Disallow DOCTYPE declaration
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		// Disable external general entities
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		// Disable external parameter entities
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

		return builder
				.parse(new InputSource(new ByteArrayInputStream(soapMessageString.getBytes(StandardCharsets.UTF_8))));
	}
	/**
	 * @param env
	 * @param docSoapInput
	 * @param inputDoc
	 * @return
	 * @throws YFSException
	 * @throws RemoteException
	 * @throws YIFClientCreationException
	 * For QuotationResponse Call vertex
	 */
	public Document callvertexforQuotationRequest(YFSEnvironment env, Document docSoapInput, Document inputDoc)
			throws YFSException, RemoteException, YIFClientCreationException {

		Document docSoapOutput = null;
		YIFApi api = YIFClientFactory.getInstance().getApi();

		try {

			docSoapOutput = api.executeFlow(env, CrocsConstant.A_CROCS_TAX_CALL_TO_VERTEX, docSoapInput);
			//EOMS-4305: create alert on order if vertex response contains VertexvalidAddressException
			Element faultEle=extractFaultLineItem(docSoapOutput);
			
			if(!YFCCommon.isVoid(faultEle)) {
				createAlertOnOrder(env,inputDoc,STR_CROCS_VERTEX_QUOTATION_REQUEST_EXP,"",faultEle);
				logger.error("CrocsSOAPRequestToVertex: A fault exception was received in the response from Vertex"
						+ faultEle.toString());
				throw new YFCException(
						"CrocsSOAPRequestToVertex: A fault exception was received in the response from Vertex." +faultEle.toString());
			}
			
			//EOMS-4305
		} catch (Exception e) {
			logger.error("CrocsSOAPRequestToVertex: callvertexforQuotationRequest : Connectivity with Vertex got failed"
					+ e.getMessage());
			//EOMS-3896 Create exception when Connectivity to Vertex got failed
			createAlertOnOrder(env,inputDoc,STR_CROCS_VERTEX_QUOTATION_REQUEST_EXP,STR_QUOTATION_REQUEST_FAILURE, null);

			throw new YFCException(
					"CrocsSOAPRequestToVertex: callvertexforQuotationRequest : Connectivity with Vertex got failed",e.getMessage() );

		}
		return docSoapOutput;
	}
	/**
	 * @param env
	 * @param docSoapInput
	 * @param inputDoc
	 * @return
	 * @throws YFSException
	 * @throws RemoteException
	 * @throws YIFClientCreationException
	 * For InvoiceResponse Call vertex
	 */
	public Document callvertexforInvoiceRequest(YFSEnvironment env, Document docSoapInput, Document inputDoc)
			throws YFSException, RemoteException, YIFClientCreationException {

		logger.info("callvertexforInvoiceRequest Input" +SCXmlUtil.getString(docSoapInput));
		Document docSoapOutput = null;
		YIFApi api = YIFClientFactory.getInstance().getApi();

		try {

			docSoapOutput = api.executeFlow(env, CrocsConstant.A_CROCS_TAX_CALL_TO_VERTEX, docSoapInput);
			//EOMS-4305 -create alert on order if vertex response contains VertexvalidAddressException
			Element faultEle=extractFaultLineItem(docSoapOutput);
		
			if(!YFCCommon.isVoid(faultEle)) {
				createAlertOnOrder(env,inputDoc,STR_CROCS_VERTEX_INVOICE_REQUEST_EXP,"",faultEle);
				logger.error("CrocsSOAPRequestToVertex: A fault exception was received in the response from Vertex"
						+ faultEle.toString());
				throw new YFCException(
						"CrocsSOAPRequestToVertex: A fault exception was received in the response from Vertex." +faultEle.toString());
			}
			//EOMS-4305
		} catch (Exception e) {
			logger.error("CrocsSOAPRequestToVertex: callvertexforInvoiceRequest : Connectivity with Vertex got failed"
					+ e.getMessage());
			//EOMS-4214 Create exception when Connectivity to Vertex got failed
			createAlertOnOrder(env,inputDoc,STR_CROCS_VERTEX_INVOICE_REQUEST_EXP,STR_INVOICE_REQUEST_FAILURE,null);

			throw new YFCException(
					"CrocsSOAPRequestToVertex: callvertexforInvoiceRequest : Connectivity with Vertex got failed",e.getMessage());

		}
		return docSoapOutput;

	}
	//EOMS-4305: create alert on order if vertex response contains VertexvalidAddressException
	public  Element extractFaultLineItem(Document docVertexOutput){
		
		Element eleVertexException=null;
		Element eleSoapBody=SCXmlUtil.getChildElement(docVertexOutput.getDocumentElement(), V_SOAP_ENV_BODY);
		if(eleSoapBody!=null)
			{
			Element eleSoapEnv=SCXmlUtil.getChildElement(eleSoapBody, V_SOAP_ENV_FAULT);
			if(eleSoapEnv!=null)
			{
				Element eleDetail=SCXmlUtil.getChildElement(eleSoapEnv, V_DETAIL);
				if(eleDetail!=null)
					eleVertexException=SCXmlUtil.getChildElement(eleDetail, V_VERTEX_EXCEPTION);
			}
			}
		return  eleVertexException;
	}
	//EOMS-4305
	public void createAlertOnOrder(YFSEnvironment env,Document inputDoc,String exceptionType, String referenceName, Element faultEle) throws RemoteException
	{
		/*
		 * EOMS - Canada (CA) Tax Validation:
		 * If the Imposition ID associated with a tax line is not present in the configured CA tax bucket list,
		 * raise an alert or exception to indicate an invalid or unrecognized tax configuration.
		 * This ensures only valid Imposition IDs (e.g., GST/HST, PST, QST) are processed for Canadian orders.
		 */
		
		String strValue = null;
		if (exceptionType.equalsIgnoreCase(CrocsConstant.STR_INVALID_TAX)) {
			exceptionType = STR_CROCS_VERTEX_QUOTATION_REQUEST_EXP;
			strValue = STR_INVALID_TAX;
		} else {
			strValue = STR_VERTEX_REFERNCE_VALUE;
		}
		
		/*
		 *  XPaths stored in arrays for maintainability
		 */
		String[] orderHeaderKeyXpaths = {
			    XPATH_ORDERINVOICE_ORDER_ORDER_HEADER_KEY,
			    XPATH_ORDERLIST_ORDER_ORDER_HEADER_KEY,
			    XPATH_ORDER_ORDER_HEADER_KEY
			};
			 String[] orderNoXpaths = {
			    XPATH_ORDERINVOICE_ORDER_ORDER_NO,
			    XPATH_ORDERLIST_ORDER_ORDER_NO,
			    XPATH_ORDER_NO
			};
			String[] enterpriseCodeXpaths = {
			    XPATH_ORDERINVOICE_ORDER_ENTERPRISE_CODE,
			    XPATH_ORDERLIST_ORDER_ORDER_ENTERPRISE_CODE,
			    XPATH_ORDER_ENTERPRISE_CODE
			};
		Element rootElement = inputDoc.getDocumentElement();
		String strOrderHeaderKey = getFirstMatchingXPathValue(rootElement, orderHeaderKeyXpaths);
		String strOrderNo = getFirstMatchingXPathValue(rootElement, orderNoXpaths);
		String strEnterpriseKey = getFirstMatchingXPathValue(rootElement, enterpriseCodeXpaths);
		
		// Raising alert
		Document createExceptionIndoc = SCXmlUtil.createDocument(A_INBOX);
		createExceptionIndoc.getDocumentElement().setAttribute(A_EXCEPTION_TYPE, exceptionType);
		createExceptionIndoc.getDocumentElement().setAttribute(A_DESCRIPTION, exceptionType);
		createExceptionIndoc.getDocumentElement().setAttribute(OrderHeaderKey, strOrderHeaderKey);
		createExceptionIndoc.getDocumentElement().setAttribute(OrderNo, strOrderNo);
		createExceptionIndoc.getDocumentElement().setAttribute(A_ENTERPRISE_KEY, strEnterpriseKey);
		createExceptionIndoc.getDocumentElement().setAttribute(STR_EXPIRATION_DAYS, STR_EXPIRATION_DAYS_VALUE);
		createExceptionIndoc.getDocumentElement().setAttribute(A_PRIORITY, A_PRIORITY_1);
		Element eleInboxReferencesList = SCXmlUtil.createChild(createExceptionIndoc.getDocumentElement(),
				A_INBOX_REFERENCES_LIST);
		Element eleInboxReferences = SCXmlUtil.createChild(eleInboxReferencesList, A_INBOX_REFERENCES);
	
		if(!YFCCommon.isVoid(faultEle))
		{
			Element eleExceptionType=SCXmlUtil.getChildElement(faultEle, V_EXCEPTION_TYPE);
			String faultExceptionType = eleExceptionType.getTextContent();
			Element eleRootCause=SCXmlUtil.getChildElement(faultEle, V_ROOT_CAUSE);
			
			String rootcause = eleRootCause.getTextContent();
			eleInboxReferences.setAttribute(A_NAME, faultExceptionType);
			eleInboxReferences.setAttribute(A_VALUE, rootcause);
		}
		else {
			eleInboxReferences.setAttribute(A_NAME, referenceName);
			eleInboxReferences.setAttribute(A_VALUE, strValue);
		}
		
		eleInboxReferences.setAttribute(A_REFERNCE_TYPE, STR_TEXT);
	
		logger.verbose("Rasing alert for createException Input:- " + SCXmlUtil.getString(createExceptionIndoc));
	  CommonUtil.invokeService(env, CROCS_VERTEX_CREATE_EXCEPTION_SYNC_SERV, createExceptionIndoc);
	}
	/**
	 * Utility method to evaluate multiple XPath expressions and return the first non-empty result.
	 *
	 * @param rootElement XML root element
	 * @param xpaths Array of XPath expressions
	 * @return First non-empty result or null
	 */
	private static String getFirstMatchingXPathValue(Element rootElement, String[] xpaths) {
	    for (String xpath : xpaths) {
	        String value = SCXmlUtil.getXpathAttribute(rootElement, xpath);
	        if (!YFCCommon.isVoid(value)) {
	            return value;
	        }
	    }
	    return null;
	}

}