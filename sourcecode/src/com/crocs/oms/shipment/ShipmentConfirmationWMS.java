package com.crocs.oms.shipment;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class ShipmentConfirmationWMS implements CrocsConstant {
	private static final YFCLogCategory logger = YFCLogCategory.instance(ShipmentConfirmationWMS.class);
	/**
	 * input as received to this service:-
	 *<Shipment BackOrderNonShippedQuantity="Y" OrderNo="54929265CUS" ReleaseNo="1" SCAC="FEDX">
			<Containers>
				<Container TrackingNo="123TRACKING00003" ContainerNo="00009999990000036003">
					<ContainerDetails>
						<ContainerDetail Quantity="6">
							<ShipmentLine Quantity="3" ShipmentLineNo="1"/>
							<ShipmentLine Quantity="3" ShipmentLineNo="2"/>
						</ContainerDetail>
					</ContainerDetails>
				</Container>
			</Containers>
			<ShipmentLines>
				<ShipmentLine ItemID="40002-001-M18" OrderNo="54929265CUS" Quantity="3" ReleaseNo="1" ShipmentLineNo="1" UnitOfMeasure="EACH"/>
				<ShipmentLine ItemID="40003-001-M17" OrderNo="54929265CUS" Quantity="3" ReleaseNo="1" ShipmentLineNo="2" UnitOfMeasure="EACH"/>
			</ShipmentLines>
		</Shipment>
		
	 * 
	 * 
	 * Purpose:- If the attribute of BackOrderNonShippedQuantity="Y" this attribute
	 * will be changed to CancelNonShippedQuantity Then the quantity mentioned in
	 * line will be shipped and the remaining ones will be cancelled
	 * 
	 * If the attribute of BackOrderNonShippedQuantity="N" Then the quantity
	 * mentioned in line will be split and shipped, and the remaining ones will be
	 * in packed status waiting for future updated from WMS
	 */

	public Document processPackedShipmentToConfirmFromWMS(YFSEnvironment env, Document inDoc) throws Exception {
		logger.verbose("Start of method processPackedShipmentToConfirmFromWMS with input: " + SCXmlUtil.getString(inDoc));

		String backOrderNonShippedQuantity = "";
		String cancelNonShippedQuantity = "";

		Element inDocEle = inDoc.getDocumentElement();
		backOrderNonShippedQuantity = inDocEle.getAttribute(A_BACK_ORDER_NON_SHIPPED_QUANTITY);
		cancelNonShippedQuantity = inDocEle.getAttribute(A_CANCEL_NON_SHIPPED_QUANTITY);

		Document outDoc = null;
		Document confirmShipmentInDoc = null;
		
		if (FLAG_Y.equals(cancelNonShippedQuantity)) 
			confirmShipmentInDoc  = performAction(env, inDoc, ACTION_CANCEL_NON_SHIPPED_QUANTITY_Y);
		
		 else if (FLAG_N.equals(backOrderNonShippedQuantity)) 
			 confirmShipmentInDoc = performAction(env, inDoc, ACTION_BACKORDERED_NON_SHIPPED_QUANTITY_N);
		
		outDoc = invokeConfirmShipmentAPI(env, confirmShipmentInDoc);
		logger.verbose("End of method processPackedShipmentToConfirmFromWMS with output: " + SCXmlUtil.getString(outDoc));
		return outDoc;

	}

	/**
	 * Based on the actionItem provided decision will be taken to make the changes for code 1, 2 or 5
	 * @param env
	 * @param inputDocumentInitial
	 * @param actionItem
	 * @return
	 * @throws Exception
	 */
	private Document performAction(YFSEnvironment env, Document inputDocumentInitial, String actionItem) throws Exception {
		logger.verbose("Start of method processShipmentConfirmUpdates with input: " + SCXmlUtil.getString(inputDocumentInitial));

		Element inDocEle = inputDocumentInitial.getDocumentElement();
		String orderNo = inDocEle.getAttribute(A_ORDER_NO);
		String releaseNo = inDocEle.getAttribute(A_RELEASE_NO);

		Document orderReleaseListOutDoc = null, shipmentListOutput = null,inputDocumentFinal=null;
		String strCarrierServiceCode="";

		switch (actionItem) {
		case "CANCEL_NON_SHIPPED_QUANTITY_Y":	// code 2 or 5

			
			orderReleaseListOutDoc = getOrderReleaseList(env, inputDocumentInitial, orderNo, releaseNo);

			shipmentListOutput = getShipmentListForOrder(env, orderReleaseListOutDoc, orderNo, releaseNo);
			
			strCarrierServiceCode=SCXmlUtil.getXpathAttribute(orderReleaseListOutDoc.getDocumentElement(), "/OrderReleaseList/OrderRelease/@CarrierServiceCode");
			
			updateShipmentAttributes(inputDocumentInitial, shipmentListOutput);
			
			updateSCAC(env,inputDocumentInitial,strCarrierServiceCode);
			
			updateContainerDetailsForFlagY(env, inputDocumentInitial);
			
			inputDocumentFinal=inputDocumentInitial;			

			break;

		case "BACKORDERED_NON_SHIPPED_QUANTITY_N":	// code 1
			
			orderReleaseListOutDoc = getOrderReleaseList(env, inputDocumentInitial, orderNo, releaseNo);
			shipmentListOutput = getShipmentListForOrder(env, orderReleaseListOutDoc, orderNo, releaseNo);
		    strCarrierServiceCode=SCXmlUtil.getXpathAttribute(orderReleaseListOutDoc.getDocumentElement(), "/OrderReleaseList/OrderRelease/@CarrierServiceCode");

			
			updateShipmentAttributes(inputDocumentInitial, shipmentListOutput);
			
			updateSCAC(env, inputDocumentInitial,strCarrierServiceCode);
			
			
			
			String originalShipmentNo = inDocEle.getAttribute(A_SHIPMENT_NO);
            inDocEle.setAttribute(A_BOL_NO, originalShipmentNo);

			String newShipmentNo = splitShipment(env, inputDocumentInitial);
			inDocEle.setAttribute(A_SHIPMENT_NO, newShipmentNo);
			
			updateContainerDetailsForFlagN(env,inputDocumentInitial);
			
			inputDocumentFinal=inputDocumentInitial;
			break;
		default:

			throw new YFSException("Invalid Action provided for the operation", "",
					"actionItem expected [CANCEL_NON_SHIPPED_QUANTITY_Y,BACKORDERED_NON_SHIPPED_QUANTITY_N] provided:"
							+ actionItem
							+ "\n If BackOrderNonShippedQuantity=Y then CANCEL_NON_SHIPPED_QUANTITY_Y action is called else BACKORDERED_NON_SHIPPED_QUANTITY_N is called");
		}
		logger.verbose("End of method processShipmentConfirmUpdates with output: " + SCXmlUtil.getString(inputDocumentFinal));
		return inputDocumentFinal;
	}
	

	/**
	 * This function is used to make an api call to confirmShipment 
	 * @param env
	 * @param inputDoc
	 * @return
	 * @throws YFSException
	 */
	private Document invokeConfirmShipmentAPI(YFSEnvironment env, Document inputDoc) throws YFSException {
		logger.verbose("Start of method invokeConfirmShipmentAPI with input: " + SCXmlUtil.getString(inputDoc));

		try {
			Document result = CommonUtil.invokeAPI(env, TEMPLATE_CONFIRM_SHIPMENT, API_CONFIRM_SHIPMENT, inputDoc);
			logger.verbose("End of method invokeConfirmShipmentAPI with output: " + SCXmlUtil.getString(result));
			return result;
		} catch (Exception e) {
			logger.error("Error invoking confirmShipment API: " + e.getMessage());
			throw new YFSException("Error invoking confirmShipment API: " + e.getMessage());
		}
	}

	/**
	 * This function is used to make an api call to splitShipment  
	 * @param env
	 * @param inputDoc
	 * @return
	 * @throws YFSException
	 */
	private String splitShipment(YFSEnvironment env, Document inDoc) throws YFSException {
		logger.verbose("Start of method splitShipment with input: " + SCXmlUtil.getString(inDoc));

		Element inDocEle = inDoc.getDocumentElement();
		Element inDocShipmentLinesEle = SCXmlUtil.getChildElement(inDocEle, E_SHIPMENT_LINES);
		List<Element> inDocShipmentLineListEle = SCXmlUtil.getChildrenList(inDocShipmentLinesEle);

		Document splitShipmentInDoc = SCXmlUtil.createDocument(E_SPLIT_SHIPMENT);
		Element splitShipmentInDocEle = splitShipmentInDoc.getDocumentElement();

		Element sourceEle = SCXmlUtil.createChild(splitShipmentInDocEle, E_SOURCE);
		Element shipmentEle = SCXmlUtil.createChild(sourceEle, E_SHIPMENT);

		String sellerOrganizationCode = inDocEle.getAttribute(A_SELLER_ORGANIZATION_CODE);
		String shipNode = inDocEle.getAttribute(A_SHIP_NODE);
		shipmentEle.setAttribute(A_SELLER_ORGANIZATION_CODE, sellerOrganizationCode);
		shipmentEle.setAttribute(A_SHIP_NODE, shipNode);
		shipmentEle.setAttribute(A_SHIPMENT_NO, inDocEle.getAttribute(A_SHIPMENT_NO));

		Element shipmentLinesEle = SCXmlUtil.createChild(shipmentEle, E_SHIPMENT_LINES);
		for (Element eachShipmentLine : inDocShipmentLineListEle) {
			Element shipmentLineEle = SCXmlUtil.createChild(shipmentLinesEle, E_SHIPMENT_LINE);
			shipmentLineEle.setAttribute(A_ITEM_ID, eachShipmentLine.getAttribute(A_ITEM_ID));
			shipmentLineEle.setAttribute(A_ORDER_NO, eachShipmentLine.getAttribute(A_ORDER_NO));
			shipmentLineEle.setAttribute(A_QUANTITY, eachShipmentLine.getAttribute(A_QUANTITY));
			shipmentLineEle.setAttribute(A_RELEASE_NO, eachShipmentLine.getAttribute(A_RELEASE_NO));
			shipmentLineEle.setAttribute(A_SHIPMENT_LINE_NO, eachShipmentLine.getAttribute(A_SHIPMENT_LINE_NO));
			shipmentLineEle.setAttribute(A_UNIT_OF_MEASURE, eachShipmentLine.getAttribute(A_UNIT_OF_MEASURE));
			shipmentLineEle.setAttribute(A_DOCUMENT_TYPE, VAL_DOCUMENT_TYPE_SALES_ORDER);
		}

		Element targetEle = SCXmlUtil.createChild(splitShipmentInDocEle, E_TARGET);
		Element targetShipmentEle = SCXmlUtil.createChild(targetEle, E_SHIPMENT);

		targetShipmentEle.setAttribute(A_SELLER_ORGANIZATION_CODE, sellerOrganizationCode);
		targetShipmentEle.setAttribute(A_SHIP_NODE, shipNode);
		targetShipmentEle.setAttribute(A_SHIPMENT_NO, "");

		logger.verbose("splitShipment with input: " + SCXmlUtil.getString(splitShipmentInDoc));
		System.out.println("splitShipment with input: " + SCXmlUtil.getString(splitShipmentInDoc));
		Document splitShipmentOutDoc = null;
		try {
			splitShipmentOutDoc = CommonUtil.invokeAPI(env, "", API_SPLIT_SHIPMENT,
					splitShipmentInDoc);
		} catch (Exception e) {
			logger.error("Error invoking splitShipment API: " + e.getMessage());
			throw new YFSException("Error invoking splitShipment API: " + e.getMessage());
		}

		if (splitShipmentOutDoc != null) {
			Element splitShipmentOutDocEle = splitShipmentOutDoc.getDocumentElement();
			Element splitShipmentOutDocTargetEle = SCXmlUtil.getChildElement(splitShipmentOutDocEle, E_TARGET);
			Element splitShipmentOutDocShipmentEle = SCXmlUtil.getChildElement(splitShipmentOutDocTargetEle,
					E_SHIPMENT);
			String newShipmentNo = splitShipmentOutDocShipmentEle.getAttribute(A_SHIPMENT_NO);

			logger.verbose("End of method splitShipment with output: " + newShipmentNo);
			return newShipmentNo;
		} else {
			throw new YFSException(
					"NO output received from splitShipmentAPI: " + SCXmlUtil.getString(splitShipmentOutDoc));
		}
	}

	/**
	 * It will update the container details for "Y" case so that one container will have only one shipment line in it
	 * @param inDoc
	 * @return
	 */
	private static Document updateContainerDetailsForFlagY(YFSEnvironment env, Document inDoc) throws Exception {
		logger.verbose("Start of method updateContainerDetails with input: " + SCXmlUtil.getString(inDoc));

		Element inDocEle = inDoc.getDocumentElement();
		Element containersEle = SCXmlUtil.getChildElement(inDocEle, E_CONTAINERS);
		List<Element> containerListEle = SCXmlUtil.getChildrenList(containersEle);
		Document getOrganizationListInDoc = SCXmlUtil.createDocument(E_ORGANIZATION);
		String strSCAC = inDoc.getDocumentElement().getAttribute(A_SCAC);
		getOrganizationListInDoc.getDocumentElement().setAttribute(A_ORGANIZATION_CODE, strSCAC);
		Document getOrganizationListOutDoc;

		getOrganizationListOutDoc = CommonUtil.invokeAPI(env, TEMPLATE_GET_ORGANIZATION_LIST,
				API_GET_ORGANIZATION_LIST, getOrganizationListInDoc);
		
		

		for (Element eachContainer : containerListEle) {
			
			String strPrimaryUrl = SCXmlUtil.getXpathAttribute(getOrganizationListOutDoc.getDocumentElement(),
					XPAH_PRIMARY_URL);
			
			Element containerDetails = SCXmlUtil.getChildElement(eachContainer, E_CONTAINER_DETAILS);
			Element containerDetail = SCXmlUtil.getChildElement(containerDetails, E_CONTAINER_DETAIL);
			List<Element> shipmentList = SCXmlUtil.getChildrenList(containerDetail);
			containerDetails.removeChild(SCXmlUtil.getChildElement(containerDetails, E_CONTAINER_DETAIL));

			for (Element shipmentLine : shipmentList) {
				Element containerDetailEle = SCXmlUtil.createChild(containerDetails, E_CONTAINER_DETAIL);
				containerDetailEle.setAttribute(A_QUANTITY, shipmentLine.getAttribute(A_QUANTITY));

				Element containerShipmentLine = SCXmlUtil.createChild(containerDetailEle, E_SHIPMENT_LINE);
				containerShipmentLine.setAttribute(A_QUANTITY, shipmentLine.getAttribute(A_QUANTITY));
				containerShipmentLine.setAttribute(A_SHIPMENT_LINE_NO, shipmentLine.getAttribute(A_SHIPMENT_LINE_NO));
			}
			String trackingNo=eachContainer.getAttribute(A_TRACKING_NO);
			Element extn=SCXmlUtil.createChild(eachContainer, E_EXTN);
			strPrimaryUrl=strPrimaryUrl.replaceAll(A_TRACKING_NO,trackingNo );
			extn.setAttribute(A_EXTN_TRACKING_URL, strPrimaryUrl);
		}

		logger.verbose("End of method updateContainerDetails with output: " + SCXmlUtil.getString(inDoc));
		return inDoc;
	}
	
	/**
	 * It will update the container details for "N" case so that one container will have only one shipment line in it
	 * and we are also updating the shipment line no according to the new shipment no
	 * @param inDoc
	 * @return
	 */
	private static Document updateContainerDetailsForFlagN(YFSEnvironment env, Document inDoc) throws Exception {
		logger.verbose("Start of method updateContainerDetails with input: " + SCXmlUtil.getString(inDoc));

		Element inDocEle = inDoc.getDocumentElement();
		Element containersEle = SCXmlUtil.getChildElement(inDocEle, E_CONTAINERS);
		List<Element> containerListEle = SCXmlUtil.getChildrenList(containersEle);
		Document getOrganizationListInDoc = SCXmlUtil.createDocument(E_ORGANIZATION);
		String strSCAC = inDoc.getDocumentElement().getAttribute(A_SCAC);
		getOrganizationListInDoc.getDocumentElement().setAttribute(A_ORGANIZATION_CODE, strSCAC);
		Document getOrganizationListOutDoc;

		logger.verbose("Calling getOrganizationList with input: " + SCXmlUtil.getString(getOrganizationListInDoc));
		getOrganizationListOutDoc = CommonUtil.invokeAPI(env, TEMPLATE_GET_ORGANIZATION_LIST,
				API_GET_ORGANIZATION_LIST, getOrganizationListInDoc);
		logger.verbose("Output returned from getOrganizationList: " + SCXmlUtil.getString(getOrganizationListOutDoc));
		
		int shipmentLineNoAtContainer = 1;
		for (Element eachContainer : containerListEle) {
			
			String strPrimaryUrl = SCXmlUtil.getXpathAttribute(getOrganizationListOutDoc.getDocumentElement(),
					XPAH_PRIMARY_URL);
			
			Element containerDetails = SCXmlUtil.getChildElement(eachContainer, E_CONTAINER_DETAILS);
			Element containerDetail = SCXmlUtil.getChildElement(containerDetails, E_CONTAINER_DETAIL);
			List<Element> shipmentList = SCXmlUtil.getChildrenList(containerDetail);
			containerDetails.removeChild(SCXmlUtil.getChildElement(containerDetails, E_CONTAINER_DETAIL));

			for (Element shipmentLine : shipmentList) {
				Element containerDetailEle = SCXmlUtil.createChild(containerDetails, E_CONTAINER_DETAIL);
				containerDetailEle.setAttribute(A_QUANTITY, shipmentLine.getAttribute(A_QUANTITY));

				Element containerShipmentLine = SCXmlUtil.createChild(containerDetailEle, E_SHIPMENT_LINE);
				containerShipmentLine.setAttribute(A_QUANTITY, shipmentLine.getAttribute(A_QUANTITY));
				
				/* This changes are made as per the EOMS-3437
				 * Here we are updating the shipment line no within each container as line no 1,2,3..
				 * Because after split shipment the line no is different when compared to the line no in original line no
				 * */
				containerShipmentLine.setAttribute(A_SHIPMENT_LINE_NO, String.valueOf(shipmentLineNoAtContainer));
				shipmentLineNoAtContainer++;
			}
			String trackingNo=eachContainer.getAttribute(A_TRACKING_NO);
			Element extn=SCXmlUtil.createChild(eachContainer, E_EXTN);
			strPrimaryUrl=strPrimaryUrl.replaceAll(A_TRACKING_NO,trackingNo );
			extn.setAttribute(A_EXTN_TRACKING_URL, strPrimaryUrl);
		}
		
		/* This changes are made as per the EOMS-3437
		 * Here we are updating the shipment line no inside shipment lines tag from the input as line no 1,2,3..
		 * Because after split shipment the line no is different when compared to the line no in original line no
		 * */		
		Element shipmentLines = SCXmlUtil.getChildElement(inDocEle, E_SHIPMENT_LINES);
		List<Element> shipmentLineList = SCXmlUtil.getChildrenList(shipmentLines);
		int shipmentLineNoAtShipmentLines = 1;
		for(Element eachShipmentLine : shipmentLineList) {
			eachShipmentLine.setAttribute(A_SHIPMENT_LINE_NO, String.valueOf(shipmentLineNoAtShipmentLines));
			shipmentLineNoAtShipmentLines++;
		}

		logger.verbose("End of method updateContainerDetails with output: " + SCXmlUtil.getString(inDoc));
		return inDoc;
	}

	/**
	 * This will update the various attributes at the Shipment level tag of the input received
	 * @param inDoc
	 * @param shipmentInDoc
	 */
	private void updateShipmentAttributes(Document inDoc, Document shipmentInDoc) {
		logger.verbose("Start of method updateShipmentAttributes with input: " + SCXmlUtil.getString(inDoc));
		logger.verbose("Start of method updateShipmentAttributes with shipmentInDoc: " + SCXmlUtil.getString(shipmentInDoc));

		Element shipmentElement = SCXmlUtil.getChildElement(shipmentInDoc.getDocumentElement(), E_SHIPMENT);
		Element inDocEle = inDoc.getDocumentElement();
		inDocEle.setAttribute(A_SHIPMENT_NO, shipmentElement.getAttribute(A_SHIPMENT_NO));
		inDocEle.setAttribute(A_OVERRIDE_MODIFICATION_RULES, FLAG_Y);
		inDocEle.setAttribute(A_ENTERPRISE_CODE, shipmentElement.getAttribute(A_ENTERPRISE_CODE));
		inDocEle.setAttribute(A_DOCUMENT_TYPE, shipmentElement.getAttribute(A_DOCUMENT_TYPE));
		inDocEle.setAttribute(A_SHIP_NODE, shipmentElement.getAttribute(A_SHIP_NODE));
		inDocEle.setAttribute(A_SELLER_ORGANIZATION_CODE, shipmentElement.getAttribute(A_SELLER_ORGANIZATION_CODE));

		logger.verbose("End of method updateShipmentAttributes with output: " + SCXmlUtil.getString(inDoc));
	}

	/**
	 * This function is used to make an api call to getOrderReleaseList
	 * @param env
	 * @param indoc
	 * @param orderNo
	 * @param releaseNo
	 * @return
	 * @throws Exception
	 */
	private Document getOrderReleaseList(YFSEnvironment env, Document indoc, String orderNo, String releaseNo)
			throws Exception {
		logger.verbose("Start of method getOrderReleaseList with input: " + SCXmlUtil.getString(indoc));
		Document getOrderReleaseListIndoc = SCXmlUtil.createDocument(E_ORDER_RELEASE);
		Element getOrderReleaseListEle = getOrderReleaseListIndoc.getDocumentElement();
		getOrderReleaseListEle.setAttribute(A_RELEASE_NO, releaseNo);

		Element orderEle = SCXmlUtil.createChild(getOrderReleaseListEle, E_ORDER);
		orderEle.setAttribute(A_ORDER_NO, orderNo);

		logger.debug("releaseListInput: " + SCXmlUtil.getString(getOrderReleaseListIndoc));

		Document orderReleaseListOutDoc = CommonUtil.invokeAPI(env, TEMPLATE_GET_ORDER_RELEASE_LIST,
				API_GET_ORDER_RELEASE_LIST, getOrderReleaseListIndoc);

		logger.debug("releaseListOutput: " + SCXmlUtil.getString(orderReleaseListOutDoc));

		logger.verbose("End of method getOrderReleaseList with output: " + SCXmlUtil.getString(orderReleaseListOutDoc));
		return orderReleaseListOutDoc;
	}

	/**
	 * This function is used to make an api call to getShipmentListForOrder
	 * @param env
	 * @param inDoc
	 * @param orderNo
	 * @param releaseNo
	 * @return
	 * @throws Exception
	 */
	private Document getShipmentListForOrder(YFSEnvironment env, Document inDoc, String orderNo, String releaseNo)
			throws Exception {
		logger.verbose("Start of method getShipmentListForOrder with input: " + SCXmlUtil.getString(inDoc));

		Element orderReleaseListEle = inDoc.getDocumentElement();
		Element orderReleaseEle = SCXmlUtil.getChildElement(orderReleaseListEle, E_ORDER_RELEASE);

		if (orderReleaseEle != null) {
			String enterpriseCode = orderReleaseEle.getAttribute(A_ENTERPRISE_CODE);

			Document getShipmentListIndoc = SCXmlUtil.createDocument(E_ORDER);
			Element getShipmentListEle = getShipmentListIndoc.getDocumentElement();
			getShipmentListEle.setAttribute(A_ORDER_NO, orderNo);
			getShipmentListEle.setAttribute(A_ENTERPRISE_CODE, enterpriseCode);
			getShipmentListEle.setAttribute(A_RELEASE_NO, releaseNo);

			logger.debug("getShipmentListForOrder Input: " + SCXmlUtil.getString(getShipmentListIndoc));

			Document shipmentListDoc = CommonUtil.invokeAPI(env, TEMPLATE_GET_SHIPMENT_LIST_FOR_ORDER,
					API_GET_SHIPMENT_LIST_FOR_ORDER, getShipmentListIndoc);

			logger.debug("Shipment List output: " + SCXmlUtil.getString(shipmentListDoc));

			logger.verbose(
					"End of method getShipmentListForOrder with output: " + SCXmlUtil.getString(shipmentListDoc));
			return shipmentListDoc;
		} else {
			throw new YFSException("No OrderRelease element found in the response.", "",
					SCXmlUtil.getString(orderReleaseListEle));
		}
	}


	/**
	 * This method will update the SCAC details as per OMS
	 * @param inDoc
	 */
	private void updateSCAC(YFSEnvironment env, Document inDoc, String strCarrierServiceCode) throws Exception {
		logger.verbose("Start of method updateSCAC with input: " + SCXmlUtil.getString(inDoc));

		String scac = inDoc.getDocumentElement().getAttribute(A_SCAC);
		Document inDocCC=SCXmlUtil.createDocument(A_COMMON_CODE);
		Element inEleCC=inDocCC.getDocumentElement();
		inEleCC.setAttribute(A_CODE_TYPE, STR_CROCS_SCAC_NAMES);
		inEleCC.setAttribute(A_CODE_VALUE, scac);
		logger.verbose("getCommonCodeList.Indoc_CC:: "+SCXmlUtil.getString(inDocCC));
		Document outDocCC=CommonUtil.invokeAPI(env, TEMPLATE_GET_COMMON_CODE_LIST, API_GET_COMMON_CODE_LIST, inDocCC);
		Element outEleCC=outDocCC.getDocumentElement();
		String strSCAC=SCXmlUtil.getXpathAttribute(outEleCC, XPATH_CODE_SHORT_DESCRIPTION);
		String strSCACandService=SCXmlUtil.getXpathAttribute(outEleCC, XPATH_CODE_LONG_DESCRIPTION);
		
		
		inDoc.getDocumentElement().setAttribute(A_SCAC, strSCAC);
		//calling getscacadndserviceList api. From output will take ScacandService element and append to inDoc.		
		Document getScacAndServiceInput=SCXmlUtil.createDocument(A_SCAC_AND_SERVICE);
		
		getScacAndServiceInput.getDocumentElement().setAttribute(A_SCAC_KEY, strSCAC);	
		getScacAndServiceInput.getDocumentElement().setAttribute(A_SCAC_AND_SERVICE, strSCACandService);
		Document getScacAndServiceOutput=CommonUtil.invokeAPI(env,TEMPLATE_GET_SCAC_AND_SERVICE_LIST, API_GET_SCAC_AND_SERVICE_LIST, getScacAndServiceInput);
		Element scacAndServiceElement=SCXmlUtil.getXpathElement(getScacAndServiceOutput.getDocumentElement(), "/ScacAndServiceList/ScacAndService");
	    Element scacAndServiceElementImport=(Element) inDoc.importNode(scacAndServiceElement, A_TRUE);
	    inDoc.getDocumentElement().appendChild(scacAndServiceElementImport);
		logger.verbose("End of method updateSCAC with updated input: " + SCXmlUtil.getString(inDoc));
	}

}