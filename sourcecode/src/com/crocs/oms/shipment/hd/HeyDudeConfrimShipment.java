package com.crocs.oms.shipment.hd;

import java.rmi.RemoteException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.crocs.oms.shipment.CrocsCreateShipInputForShipmentUpdatesFromWMS;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCException;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class HeyDudeConfrimShipment extends CrocsCreateShipInputForShipmentUpdatesFromWMS implements CrocsConstant{
	private static final YFCLogCategory logger = YFCLogCategory.instance(HeyDudeConfrimShipment.class);
	
	/**
	 * input as received to this service:-
	 * <Shipment BackOrderNonShippedQuantity="Y" WMSCode="5" OrderNo="54929265CUS" ReleaseNo="1">
			<Containers>
				<Container TrackingNo="123TRACKING00003" ContainerNo="00009999990000036003" SCAC="FEDEX">
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
	 * Purpose:- If the attribute of BackOrderNonShippedQuantity="Y" this attribute
	 * will be changed to CancelNonShippedQuantity Then the quantity mentioned in
	 * line will be shipped and the remaining ones will be cancelled
	 */

	public Document processShipmentUpdateFrom3PL(YFSEnvironment env, Document inDoc) throws Exception {
		logger.verbose("Start of method processShipmentUpdateFrom3PL with input: " + SCXmlUtil.getString(inDoc));
		logger.info("HeyDudeConfrimShipmentForm3PL : processShipmentUpdateFrom3PL : inDoc: " + SCXmlUtil.getString(inDoc));

		Document outDoc = null;
		
		outDoc = performAction(env, inDoc);

		logger.verbose("End of method processShipmentUpdateFrom3PL with output: " + SCXmlUtil.getString(outDoc));
		return outDoc;
	}

	/**
	 * Based on the actionItem provided decision will be taken to make the changes
	 * for code 5
	 *
	 * @param env
	 * @param inputDocumentInitial
	 * @param actionItem
	 * @param isCancelled
	 * @return
	 * @throws Exception
	 */
	private Document performAction(YFSEnvironment env, Document inputDocumentInitial) throws Exception {
		logger.verbose("Start of method performAction with input: " + SCXmlUtil.getString(inputDocumentInitial));

		Element inDocEle = inputDocumentInitial.getDocumentElement();

		String orderNo = inDocEle.getAttribute(A_ORDER_NO);
		String releaseNo = inDocEle.getAttribute(A_RELEASE_NO);
		String isCancelled = inDocEle.getAttribute(STR_CANCELLED);

		Element inputMsgCopy = SCXmlUtil.getCopy(inDocEle);

		Document orderReleaseListOutDoc = null;
		Document shipmentListOutput = null;
		Document inputDocumentFinal = null;
		Document outDoc = null;

		orderReleaseListOutDoc = getOrderReleaseList(env, inputDocumentInitial, orderNo, releaseNo);
		
		shipmentListOutput = getShipmentListForOrder(env, orderReleaseListOutDoc, orderNo, releaseNo);
		
		logger.info("HeyDudeConfrimShipmentForm3PL : performAction Y : after getShipmentListForOrder" + orderNo);

		// cancel order if isCancelled flag is Y
		if (FLAG_Y.equals(isCancelled)) {
			return handleCancelOrder(env, orderReleaseListOutDoc, inputDocumentFinal);
		}
		
		// otherwise proceed with normal flow 
		shipmentListOutput = createShipmentAndReturnShipmentDetails(env, inputDocumentInitial, releaseNo, orderReleaseListOutDoc, shipmentListOutput);
		
		updateShipmentAttributes(inputDocumentInitial, shipmentListOutput);

		addShipmentLineForCancellation(inputDocumentInitial, orderReleaseListOutDoc);

		updateContainerDetailsForFlagY(env, inputDocumentInitial);

		updateTrackingNo(env, orderNo, orderReleaseListOutDoc, inputMsgCopy, inputDocumentInitial);

		outDoc = invokeConfirmShipmentAPI(env, inputDocumentInitial);

		logger.info("HeyDudeConfrimShipmentForm3PL : performAction Y : after confirmShipment" + orderNo);

		logger.verbose("End of method processShipmentConfirmUpdates with output: " + SCXmlUtil.getString(inputDocumentFinal));
		
		return outDoc;
	}

	/**
	 * Handles cancellation of an order or its associated shipment based on the
	 * provided shipment list.
	 * <p>
	 * - If no shipment exists for the order, the order itself is cancelled using
	 * the ChangeOrder API. - If a shipment exists, the specific shipment is
	 * cancelled using the ChangeShipment API.
	 * </p>
	 *
	 * @param env                    The YFSEnvironment object representing the
	 *                               current execution context.
	 * @param shipmentListOutput     The output Document from the getShipmentList
	 *                               API, containing shipment details.
	 * @param orderReleaseListOutDoc The output Document from the
	 *                               getOrderReleaseList API, containing order
	 *                               release details.
	 * @param inputDocumentInitial   The original input Document passed to this
	 *                               method.
	 * @return The original input document (unchanged).
	 * @throws Exception If an error occurs while preparing or invoking the
	 *                   ChangeOrder or ChangeShipment API.
	 */

	private Document handleCancelOrder(YFSEnvironment env, Document orderReleaseListOutDoc, Document inputDocumentInitial) throws Exception {

			String strOrderHeaderKey = SCXmlUtil.getXpathAttribute(orderReleaseListOutDoc.getDocumentElement(), "OrderRelease/@OrderHeaderKey");
			Document changeOrderInDoc = createChangeOrderDoc(strOrderHeaderKey);
			logger.verbose("HeyDudeConfrimShipmentForm3PL : changeOrderInDoc" + SCXmlUtil.getString(changeOrderInDoc));

			CommonUtil.invokeAPI(env, "", API_CHANGE_ORDER, changeOrderInDoc);

		return inputDocumentInitial;
	}

	/**
	 * Creating change order input xml to cancel order
	 * 
	 * @param orderHeaderKey
	 * @return
	 */
	private Document createChangeOrderDoc(String orderHeaderKey) {
		Document doc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER);
		Element ele = doc.getDocumentElement();
		ele.setAttribute(OrderHeaderKey, orderHeaderKey);
		ele.setAttribute(A_ACTION, VAL_ACTION_CANCEL);
		ele.setAttribute(A_OVERRIDE, CrocsXmlConstants.FLAG_Y);
		logger.verbose("HeyDudeConfrimShipmentForm3PL: createChangeOrderDoc: " + SCXmlUtil.getString(doc));

		return doc;
	}

	/**
	 * This function is used to make an api call to confirmShipment
	 *
	 * @param env
	 * @param inputDoc
	 * @return
	 * @throws YFSException
	 */
	private Document invokeConfirmShipmentAPI(YFSEnvironment env, Document inputDoc) throws YFSException {
		logger.info("HeyDudeConfrimShipmentForm3PL: Start of method invokeConfirmShipmentAPI with input: "+ SCXmlUtil.getString(inputDoc));

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
	 * It will update the container details for "Y" case so that one container will
	 * have only one shipment line in it
	 *
	 * @param inDoc
	 * @return
	 */
	private Document updateContainerDetailsForFlagY(YFSEnvironment env, Document inDoc) throws Exception {
		logger.verbose("Start of method updateContainerDetails with input: " + SCXmlUtil.getString(inDoc));

		Element inDocEle = inDoc.getDocumentElement();
		Element containersEle = SCXmlUtil.getChildElement(inDocEle, E_CONTAINERS);
		/**
		 * if <Containers/> tag is empty means it is a short pick scenario we can
		 * directly return the inDoc no update at container level is required
		 * <Shipment OrderNo="1001700OCUS" ReleaseNo="1" ShipmentNo="" WMSCode="5"
		 * BackOrderNonShippedQuantity="Y"> <Containers/> <ShipmentLines>
		 * <ShipmentLine ItemID="10002-002-M17" OrderNo="1001700OCUS" Quantity="0"
		 * ReleaseNo="1" ShipmentLineNo="1" UnitOfMeasure="EACH"/> </ShipmentLines>
		 * </Shipment>
		 */
		if (containersEle.hasChildNodes()) {
			List<Element> containerListEle = SCXmlUtil.getChildrenList(containersEle);
			Document getOrganizationListInDoc = SCXmlUtil.createDocument(E_ORGANIZATION);

			for (Element eachContainer : containerListEle) {
				// SCAC value received from WMS
				String scac = eachContainer.getAttribute(A_SCAC);
				Document updateSCACOutDoc = updateSCAC(env, inDoc, scac);
				Element outEleCC = updateSCACOutDoc.getDocumentElement();
				String scacValue = SCXmlUtil.getXpathAttribute(outEleCC, XPATH_CODE_SHORT_DESCRIPTION);
				String strSCACandService = SCXmlUtil.getXpathAttribute(outEleCC, XPATH_CODE_LONG_DESCRIPTION);
				// Update SCAC at container level
				eachContainer.setAttribute(A_SCAC, scacValue);
				Element scacAndServiceElementImport = getScacAndServiceList(env, inDoc, scacValue, strSCACandService);
				eachContainer.appendChild(scacAndServiceElementImport);
				// this SCAC will have value based on OMS configuration
				String strSCAC = eachContainer.getAttribute(A_SCAC);
				getOrganizationListInDoc.getDocumentElement().setAttribute(A_ORGANIZATION_CODE, strSCAC);
				Document getOrganizationListOutDoc;

				logger.verbose("Calling getOrganizationList with input: " + SCXmlUtil.getString(getOrganizationListInDoc));
				getOrganizationListOutDoc = CommonUtil.invokeAPI(env, TEMPLATE_GET_ORGANIZATION_LIST, API_GET_ORGANIZATION_LIST, getOrganizationListInDoc);
				logger.verbose("Output returned from getOrganizationList: " + SCXmlUtil.getString(getOrganizationListOutDoc));

				String strPrimaryUrl = SCXmlUtil.getXpathAttribute(getOrganizationListOutDoc.getDocumentElement(), XPAH_PRIMARY_URL);

				Element containerDetails = SCXmlUtil.getChildElement(eachContainer, E_CONTAINER_DETAILS);
				Element containerDetail = SCXmlUtil.getChildElement(containerDetails, E_CONTAINER_DETAIL);
				List<Element> shipmentList = SCXmlUtil.getChildrenList(containerDetail);
				containerDetails.removeChild(SCXmlUtil.getChildElement(containerDetails, E_CONTAINER_DETAIL));

				for (Element shipmentLine : shipmentList) {
					Element containerDetailEle = SCXmlUtil.createChild(containerDetails, E_CONTAINER_DETAIL);
					containerDetailEle.setAttribute(A_QUANTITY, shipmentLine.getAttribute(A_QUANTITY));

					Element containerShipmentLine = SCXmlUtil.createChild(containerDetailEle, E_SHIPMENT_LINE);
					containerShipmentLine.setAttribute(A_QUANTITY, shipmentLine.getAttribute(A_QUANTITY));
					containerShipmentLine.setAttribute(A_SHIPMENT_LINE_NO,
							shipmentLine.getAttribute(A_SHIPMENT_LINE_NO));
				}
				String trackingNo = eachContainer.getAttribute(A_TRACKING_NO);
				Element extn = SCXmlUtil.createChild(eachContainer, E_EXTN);
				strPrimaryUrl = strPrimaryUrl.replaceAll(A_TRACKING_NO, trackingNo);
				extn.setAttribute(A_EXTN_TRACKING_URL, strPrimaryUrl);
			}
		}

		logger.verbose("End of method updateContainerDetails with output: " + SCXmlUtil.getString(inDoc));
		return inDoc;
	}

	/**
	 * This will update the various attributes at the Shipment level tag of the
	 * input received
	 *
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
	 *
	 * @param env
	 * @param indoc
	 * @param orderNo
	 * @param releaseNo
	 * @return
	 * @throws RemoteException
	 * @throws Exception
	 */
	private Document getOrderReleaseList(YFSEnvironment env, Document indoc, String orderNo, String releaseNo)
			throws RemoteException {
		logger.verbose("Start of method getOrderReleaseList with input: " + SCXmlUtil.getString(indoc));
		Document getOrderReleaseListIndoc = SCXmlUtil.createDocument(E_ORDER_RELEASE);
		Element getOrderReleaseListEle = getOrderReleaseListIndoc.getDocumentElement();
		getOrderReleaseListEle.setAttribute(A_RELEASE_NO, releaseNo);

		Element orderEle = SCXmlUtil.createChild(getOrderReleaseListEle, E_ORDER);
		orderEle.setAttribute(A_ORDER_NO, orderNo);
		orderEle.setAttribute(A_DOCUMENT_TYPE, VAL_DOCUMENT_TYPE_SALES_ORDER);

		logger.info("getOrderReleaseList: " + SCXmlUtil.getString(getOrderReleaseListIndoc));

		// EOMS-4670 Start
		Document orderReleaseListOutDoc = CommonUtil.invokeService(env, SERVICE_HEY_DUDE_GET_ORDER_RELEASE_LIST, getOrderReleaseListIndoc);
		// EOMS-4670 End
		logger.debug("releaseListOutput: " + SCXmlUtil.getString(orderReleaseListOutDoc));

		logger.verbose("End of method getOrderReleaseList with output: " + SCXmlUtil.getString(orderReleaseListOutDoc));
		return orderReleaseListOutDoc;
	}

	/**
	 * This function is used to make an api call to getShipmentListForOrder
	 *
	 * @param env       env
	 * @param inDoc     inDoc
	 * @param orderNo   orderNo
	 * @param releaseNo releaseNo
	 * @return getShipmentListForOrder
	 * @throws RemoteException
	 * @throws Exception
	 */
	private Document getShipmentListForOrder(YFSEnvironment env, Document inDoc, String orderNo, String releaseNo) throws RemoteException {
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
			getShipmentListEle.setAttribute(A_DOCUMENT_TYPE, VAL_DOCUMENT_TYPE_SALES_ORDER);

			logger.debug("getShipmentListForOrder Input: " + SCXmlUtil.getString(getShipmentListIndoc));

			// EOMS-4670 Start
			Document shipmentListDoc = CommonUtil.invokeService(env, SERVICE_HEY_DUDE_GET_SHIPMENT_LIST_FOR_ORDER, getShipmentListIndoc);
			// EOMS-4670 End
			logger.debug("Shipment List output: " + SCXmlUtil.getString(shipmentListDoc));

			logger.verbose(
					"End of method getShipmentListForOrder with output: " + SCXmlUtil.getString(shipmentListDoc));
			return shipmentListDoc;
		} else {
			throw new YFSException("No OrderRelease element found in the response.", "", SCXmlUtil.getString(orderReleaseListEle));
		}
	}

	/**
	 * This method will update the SCAC details as per OMS
	 *
	 * @param inDoc
	 */
	public Document updateSCAC(YFSEnvironment env, Document inDoc, String scac) throws Exception {
		logger.verbose("Start of method updateSCAC with input: " + SCXmlUtil.getString(inDoc));
		Document outDocCC = null;
		try {
			Document inDocCC = SCXmlUtil.createDocument(A_COMMON_CODE);
			Element inEleCC = inDocCC.getDocumentElement();
			inEleCC.setAttribute(A_CODE_TYPE, STR_CROCS_SCAC_NAMES);
			inEleCC.setAttribute(A_CODE_VALUE, scac);
			// update the enterprise code from inDoc
			
			String enterpriseCode = inDoc.getDocumentElement().getAttribute(A_ENTERPRISE_CODE);
			inEleCC.setAttribute(CrocsXmlConstants.A_ORGANIZATION_CODE, enterpriseCode);
			
			logger.verbose("getCommonCodeList.Indoc_CC:: " + SCXmlUtil.getString(inDocCC));
			outDocCC = CommonUtil.invokeAPI(env, TEMPLATE_GET_COMMON_CODE_LIST, API_GET_COMMON_CODE_LIST, inDocCC);
			logger.verbose("getCommonCodeList.outdoc:: " + SCXmlUtil.getString(outDocCC));
		} catch (Exception e) {
			throw new YFCException("getMessage:" + e.getMessage(), e.toString());
		}

		return outDocCC;
	}

	private Element getScacAndServiceList(YFSEnvironment env, Document inDoc, String strSCAC, String strSCACandService)
			throws Exception {
		// calling getscacadndserviceList api. From output will take ScacandService
		// element and append to inDoc.
		Document getScacAndServiceInput = SCXmlUtil.createDocument(A_SCAC_AND_SERVICE);

		getScacAndServiceInput.getDocumentElement().setAttribute(A_SCAC_KEY, strSCAC);
		getScacAndServiceInput.getDocumentElement().setAttribute(A_SCAC_AND_SERVICE, strSCACandService);
		Document getScacAndServiceOutput = CommonUtil.invokeAPI(env, TEMPLATE_GET_SCAC_AND_SERVICE_LIST,
				API_GET_SCAC_AND_SERVICE_LIST, getScacAndServiceInput);
		Element scacAndServiceElement = SCXmlUtil.getXpathElement(getScacAndServiceOutput.getDocumentElement(), "/ScacAndServiceList/ScacAndService");
		Element scacAndServiceElementImport = (Element) inDoc.importNode(scacAndServiceElement, A_TRUE);
		logger.verbose("End of method updateSCAC with updated input: " + SCXmlUtil.getString(inDoc));
		return scacAndServiceElementImport;
	}

	/**
	 * This method is used to check status of the order by calling
	 * getShipmentListForOrder and if shipment doesn't exsist it will create
	 * shipment and confirm shipment
	 *
	 * @param env       env
	 * @param inDoc     input Doc
	 * @param releaseNo releaseNo
	 * @throws Exception Exception
	 */
	private Document createShipmentAndReturnShipmentDetails(YFSEnvironment env, Document inDoc, String releaseNo, Document orderReleaseListOutDoc, Document shipmentListForOrderDoc) throws Exception {

		logger.verbose("Start of createShipmentAndReturnShipmentDetails with input: " + SCXmlUtil.getString(inDoc));
		logger.verbose("Order Release List Output: " + SCXmlUtil.getString(orderReleaseListOutDoc));
		logger.verbose("Shipment List for Order: " + SCXmlUtil.getString(shipmentListForOrderDoc));

		if (!isOpenShipmentPresent(shipmentListForOrderDoc)) {
			return createNewShipment(env, releaseNo, orderReleaseListOutDoc);
		}

		filterClosedShipments(shipmentListForOrderDoc);
		logger.verbose("Filtered open shipments: " + SCXmlUtil.getString(shipmentListForOrderDoc));
		return shipmentListForOrderDoc;
	}

	/**
	 * Checks if there is any open shipment in the given shipment list document.
	 */
	private boolean isOpenShipmentPresent(Document shipmentListDoc) {
		Element shipmentElement = SCXmlUtil.getChildElement(shipmentListDoc.getDocumentElement(), E_SHIPMENT);
		if (shipmentElement == null) {
			return false;
		}

		Element statusElement = SCXmlUtil.getChildElement(shipmentElement, E_STATUS);
		String status = (statusElement != null) ? statusElement.getAttribute(A_STATUS_NAME) : null;

		logger.verbose("Shipment Status: " + status);

		return !YFCCommon.isVoid(status) && (VAL_SHIPMENT_CREATED.equalsIgnoreCase(status) 
				|| VAL_SHIPMENT_PACKED.equalsIgnoreCase(status) || VAL_SHIPMENT_SHIPPED.equalsIgnoreCase(status));
	}

	/**
	 * Creates a new shipment and wraps it into a ShipmentList document.
	 */
	private Document createNewShipment(YFSEnvironment env, String releaseNo, Document orderReleaseListOutDoc) throws Exception {
		Document createShipmentInputDoc = createShipmentInput(releaseNo, orderReleaseListOutDoc);
		logger.verbose("Create Shipment Input: " + SCXmlUtil.getString(createShipmentInputDoc));

		Document createdShipmentDoc = CommonUtil.invokeAPI(env, TEMPLATE_CREATE_SHIPMENT, API_CREATE_SHIPMENT, createShipmentInputDoc);

		// Wrap into <ShipmentList>
		Document shipmentListDoc = SCXmlUtil.createDocument("ShipmentList");
		shipmentListDoc.getDocumentElement().appendChild(shipmentListDoc.importNode(createdShipmentDoc.getDocumentElement(), true));

		logger.verbose("Shipment List after creation: " + SCXmlUtil.getString(shipmentListDoc));
		return shipmentListDoc;
	}

	/**
	 * Removes shipments from the list that are not in CREATED or PACKED status.
	 */
	private void filterClosedShipments(Document shipmentListDoc) {
		List<Element> shipmentElements = SCXmlUtil.getChildren(shipmentListDoc.getDocumentElement(), E_SHIPMENT);

		for (Element shipmentEle : shipmentElements) {
			Element statusEle = SCXmlUtil.getChildElement(shipmentEle, E_STATUS);
			String status = (statusEle != null) ? statusEle.getAttribute(A_STATUS_NAME) : null;

			if (!VAL_SHIPMENT_CREATED.equalsIgnoreCase(status) && !VAL_SHIPMENT_PACKED.equalsIgnoreCase(status)) {
				shipmentListDoc.getDocumentElement().removeChild(shipmentEle);
			}
		}
	}

	/**
	 * This function is used to add cancellation in the input if we don't add other
	 * lines with Quantity as 0 confirmShipment API will ship those lines As per our
	 * scenario if any of the line is getting short-pick remaining lines will also
	 * be getting cancelled
	 * 
	 * @param inputDocumentInitial
	 * @param orderReleaseListOutDoc
	 */
	private void addShipmentLineForCancellation(Document inputDocumentInitial, Document orderReleaseListOutDoc) {
		logger.verbose("Start of method addShipmentLineForCancellation with inputDocumentInitial: " + SCXmlUtil.getString(inputDocumentInitial));
		logger.verbose("Start of method addShipmentLineForCancellation with orderReleaseListOutDoc: " + SCXmlUtil.getString(orderReleaseListOutDoc));

		Element inDocEle = inputDocumentInitial.getDocumentElement();
		Element orderReleaseListOutEle = orderReleaseListOutDoc.getDocumentElement();

		String orderNo = inDocEle.getAttribute(A_ORDER_NO);
		String releaseNo = inDocEle.getAttribute(A_RELEASE_NO);

		Element shipmentLines = SCXmlUtil.getChildElement(inDocEle, E_SHIPMENT_LINES);
		List<Element> listOfShipmentLine = SCXmlUtil.getChildrenList(shipmentLines);

		Element orderRelease = SCXmlUtil.getChildElement(orderReleaseListOutEle, E_ORDER_RELEASE);
		List<Element> listOfOrderLine = SCXmlUtil.getChildren(orderRelease, E_ORDER_LINE);

		Set<String> shipmentLineSet = new HashSet<>();

		for (Element eachShipmentLine : listOfShipmentLine) {
			String shipmentLineNo = eachShipmentLine.getAttribute(A_SHIPMENT_LINE_NO);
			shipmentLineSet.add(shipmentLineNo);
		}

		Iterator<Element> orderLineIterator = listOfOrderLine.iterator();
		while (orderLineIterator.hasNext()) {
			Element orderLine = orderLineIterator.next();
			String primeLineNo = orderLine.getAttribute(A_PRIME_LINE_NO);

			if (shipmentLineSet.contains(primeLineNo)) {
				orderLineIterator.remove();
			}
		}

		for (Element remainingOrderLine : listOfOrderLine) {

			Element orderStatusesEle = SCXmlUtil.getChildElement(remainingOrderLine, E_ORDER_STATUSES);
			List<Element> listOforderStatus = SCXmlUtil.getChildrenList(orderStatusesEle);

			// if a line is cancelled do not include it for shipment
			if (listOforderStatus.size() == 1
					&& listOforderStatus.get(0).getAttribute(A_STATUS).equals(STR_STATUS_CANCELLED))
				continue;

			Element shipmentLineToCancel = SCXmlUtil.createChild(shipmentLines, E_SHIPMENT_LINE);

			shipmentLineToCancel.setAttribute(A_ITEM_ID, SCXmlUtil.getXpathAttribute(remainingOrderLine, STR_XPATH_ITEM_ID));
			shipmentLineToCancel.setAttribute(A_ORDER_NO, orderNo);
			shipmentLineToCancel.setAttribute(A_QUANTITY, VAL_ZERO);
			shipmentLineToCancel.setAttribute(A_SHIPMENT_LINE_NO, remainingOrderLine.getAttribute(A_PRIME_LINE_NO));
			shipmentLineToCancel.setAttribute(A_RELEASE_NO, releaseNo);
			shipmentLineToCancel.setAttribute(A_UNIT_OF_MEASURE, SCXmlUtil.getXpathAttribute(remainingOrderLine, XPATH_ITEM_UNIT_OF_MEASURE));
		}
		logger.verbose("End of method addShipmentLineForCancellation with updated inDoc:: " + SCXmlUtil.getString(inputDocumentInitial));
	}

	/**
	 * This method will update the tracking url at the order line for the lines
	 * which are being shipped
	 *
	 * @param env
	 * @param orderNo
	 * @param orderReleaseListOutDoc
	 * @param inputMsgCopyEle
	 * @param inputMsgConfirmShip
	 * @throws Exception
	 */
	private void updateTrackingNo(YFSEnvironment env, String orderNo, Document orderReleaseListOutDoc, Element inputMsgCopyEle, Document inputMsgConfirmShip) throws Exception {
		logger.info("HeyDudeConfrimShipmentForm3PL: updateTrackingNo: start: " + orderNo);
		logger.verbose("HeyDudeConfrimShipmentForm3PL: updateTrackingNo: orderReleaseListOutDoc: " + XMLUtil.getXMLString(orderReleaseListOutDoc));
		logger.verbose("orderNo: " + orderNo);
		logger.verbose("orderReleaseListOutDoc: " + SCXmlUtil.getString(orderReleaseListOutDoc));
		logger.verbose("inputMsgCopyEle: " + SCXmlUtil.getString(inputMsgCopyEle));
		logger.verbose("inputMsgConfirmShip: " + SCXmlUtil.getString(inputMsgConfirmShip));

		Element orderReleaseListEle = orderReleaseListOutDoc.getDocumentElement();

		List<Element> listOfOrderRelease = SCXmlUtil.getChildrenList(orderReleaseListEle);
		String orderHeaderKey = "";
		String enterpriseCode = "";
		Element inputMsgConfirmShipEle = inputMsgConfirmShip.getDocumentElement();
		Element containersEle = SCXmlUtil.getChildElement(inputMsgConfirmShipEle, E_CONTAINERS);
		List<Element> listOfContainerEle = SCXmlUtil.getChildrenList(containersEle);

		// short pick scenario
		if (listOfContainerEle.isEmpty()) {
			return;
		}

		for (Element eachRelease : listOfOrderRelease) {
			Element order = SCXmlUtil.getChildElement(eachRelease, E_ORDER);
			String orderNoInReleaseList = order.getAttribute(A_ORDER_NO);

			if (orderNo.equals(orderNoInReleaseList)) {
				orderHeaderKey = SCXmlUtil.getAttribute(eachRelease, A_ORDER_HEADER_KEY);
				enterpriseCode = SCXmlUtil.getAttribute(eachRelease, A_ENTERPRISE_CODE);
			}
		}

		// Preparing input for changeOrder
		Document changeOrderInDoc = SCXmlUtil.createDocument(E_ORDER);
		Element orderEle = changeOrderInDoc.getDocumentElement();

		orderEle.setAttribute(A_ACTION, VAL_MODIFY);
		orderEle.setAttribute(A_ORDER_HEADER_KEY, orderHeaderKey);
		orderEle.setAttribute(A_ORDER_NO, orderNo);
		orderEle.setAttribute(A_ENTERPRISE_CODE, enterpriseCode);
		orderEle.setAttribute(A_DOCUMENT_TYPE, VAL_DOCUMENT_TYPE_SALES_ORDER);
		// EOMS-6567 changes start
		orderEle.setAttribute(A_OVERRIDE, FLAG_Y);
		// EOMS-6567 changes end

		Element orderLineListEle = SCXmlUtil.createChild(orderEle, E_ORDER_LINES);
		Element shipmentLines = SCXmlUtil.getChildElement(inputMsgCopyEle, E_SHIPMENT_LINES);
		List<Element> listOfShipment = SCXmlUtil.getChildrenList(shipmentLines);

		OffsetDateTime currentDate = OffsetDateTime.now(ZoneOffset.UTC);

		// Start : EOMS-4238 : Order look up : Shipdate Bug
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		String systemDate = currentDate.format(formatter);
		// End : EOMS-4238 : Order look up : Shipdate Bug

		int noOfShipmentLinesAvailable = listOfShipment.size();
		int linesNoToStart = 0;
		for (Element eachContainer : listOfContainerEle) {

			String trackingUrlAtContainer = SCXmlUtil.getXpathAttribute(eachContainer, "Extn/@ExtnTrackingUrl");
			String trackingNoAtContainer = eachContainer.getAttribute(A_TRACKING_NO);
			String scacAtContainer = eachContainer.getAttribute(A_SCAC);

			Element containerDetailsEle = SCXmlUtil.getChildElement(eachContainer, E_CONTAINER_DETAILS);
			List<Element> listOfContainerDetailEle = SCXmlUtil.getChildrenList(containerDetailsEle);

			int numberOfShipmentLinesAtContainer = listOfContainerDetailEle.size();

			while (numberOfShipmentLinesAtContainer > 0) {
				// updating the tracking details at each order line
				if (linesNoToStart < noOfShipmentLinesAvailable) {
					Element eachShipment = listOfShipment.get(linesNoToStart);
					Element eachOrderLine = SCXmlUtil.createChild(orderLineListEle, E_ORDER_LINE);
					eachOrderLine.setAttribute(A_ACTION, VAL_MODIFY);
					eachOrderLine.setAttribute(A_PRIME_LINE_NO, eachShipment.getAttribute(A_SHIPMENT_LINE_NO));
					eachOrderLine.setAttribute(A_SUB_LINE_NO, VAL_SUB_LINE_NO_1);

					Element extnEle = SCXmlUtil.createChild(eachOrderLine, E_EXTN);
					extnEle.setAttribute(A_EXTN_TRACKING_URL, trackingUrlAtContainer);
					extnEle.setAttribute(A_EXTN_SHIPMENT_DATE, systemDate);
					extnEle.setAttribute(EXTN_TRACKING_NO, trackingNoAtContainer);
					extnEle.setAttribute(EXTN_SHIP_CARRIER, scacAtContainer);
				}
				++linesNoToStart;
				--numberOfShipmentLinesAtContainer;
			}
		}

		try {
			logger.verbose("HeyDudeConfrimShipmentForm3PL: updateTrackingNo: input to changeOrder: " + XMLUtil.getXMLString(changeOrderInDoc));
			Document changeOrderOutDoc = CommonUtil.invokeAPI(env, "", API_CHANGE_ORDER, changeOrderInDoc);

			logger.verbose("output from changeOrder: " + XMLUtil.getXMLString(changeOrderOutDoc));
		} catch (Exception e) {
			logger.error("Exception in changeOrder method: " + e.getMessage(), e);
			throw e; // Rethrow exception to propagate to caller
		}
		logger.verbose("End of method updateTrackingNo: ");
	}

}