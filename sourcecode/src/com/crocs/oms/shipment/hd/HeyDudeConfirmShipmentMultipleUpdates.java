package com.crocs.oms.shipment.hd;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsErrorConstants;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.crocs.oms.shipment.CrocsCreateShipInputForShipmentUpdatesFromWMS;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCException;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class HeyDudeConfirmShipmentMultipleUpdates extends CrocsCreateShipInputForShipmentUpdatesFromWMS implements CrocsConstant {
	private static final YFCLogCategory logger = YFCLogCategory.instance(HeyDudeConfirmShipmentMultipleUpdates.class);

	Map<String, String> mapPrimeLineNo = new HashMap<>();

    /**
     * input as received to this service:-
     * <Shipment BackOrderNonShippedQuantity="Y" WMSCode="5" OrderNo="54929265CUS" ReleaseNo="1">
			<Containers>
				<Container TrackingNo="123TRACKING00003" ContainerNo="00009999990000036003" SCAC="UE20">
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
     * Purpose:- If the attribute of BackOrderNonShippedQuantity="Y" this attribute
     * will be changed to CancelNonShippedQuantity Then the quantity mentioned in
     * line will be shipped and the remaining ones will be cancelled
     * <p>
     * If the attribute of BackOrderNonShippedQuantity="N" Then the quantity
     * mentioned in line will be split and shipped, and the remaining ones will be
     * in packed status waiting for future updated from WMS
     */

    public Document processShipmentUpdate(YFSEnvironment env, Document inDoc) throws Exception {
        logger.verbose("Start of method processShipmentUpdate with input: " + SCXmlUtil.getString(inDoc));
        logger.info("HeyDudeConfirmShipmentMultipleUpdates : Start of method processShipmentUpdate : inDoc: " + SCXmlUtil.getString(inDoc));

        Element inDocEle = inDoc.getDocumentElement();
        String cancelNonShippedQuantity = inDocEle.getAttribute(A_CANCEL_NON_SHIPPED_QUANTITY);
        String backOrderNonShippedQuantity = inDocEle.getAttribute(A_BACK_ORDER_NON_SHIPPED_QUANTITY);

        Document outDoc = null;
        if (!YFCCommon.isVoid(backOrderNonShippedQuantity) && FLAG_N.equals(backOrderNonShippedQuantity))
            outDoc = performAction(env, inDoc, FLAG_N);

        else if (!YFCCommon.isVoid(cancelNonShippedQuantity) && FLAG_Y.equals(cancelNonShippedQuantity))
            outDoc = performAction(env, inDoc, FLAG_Y);
        
        logger.verbose("End of method processShipmentUpdate with output: " + SCXmlUtil.getString(outDoc));
        return outDoc;
    }

    /**
     * Based on the actionItem provided decision will be taken to make the changes for code 1, 2, 3 or 5
     *
     * @param env
     * @param inputDocumentInitial
     * @param actionItem
     * @return
     * @throws Exception
     */
    private Document performAction(YFSEnvironment env, Document inputDocumentInitial, String actionItem) throws Exception {
        logger.verbose("Start of method performAction with input: " + SCXmlUtil.getString(inputDocumentInitial));
        Element inDocEle = inputDocumentInitial.getDocumentElement();
        Element inputMsgCopy = SCXmlUtil.getCopy(inDocEle);
        String orderNo = inDocEle.getAttribute(A_ORDER_NO);
        String releaseNo = inDocEle.getAttribute(A_RELEASE_NO);
        String wmsCode = inDocEle.getAttribute(A_WMS_CODE);
        Document orderReleaseListOutDoc = null, shipmentListOutput = null, inputDocumentFinal = null, outDoc = null;
        String currentOrderStatus = "";
        
        switch (actionItem) {
        	// code 2, 3 or 5
            case FLAG_Y:    
            	
            	// invoke getOrderReleaseList 
                orderReleaseListOutDoc = getOrderReleaseList(env, orderNo, releaseNo);
                
                currentOrderStatus = SCXmlUtil.getXpathAttribute(orderReleaseListOutDoc.getDocumentElement(),"/OrderReleaseList/OrderRelease/@Status");

                // if order is in released status then create a shipment for it
                if(!YFCCommon.isVoid(currentOrderStatus) && currentOrderStatus.equals(RELEASED)) 
                    shipmentListOutput = createShipmentAndReturnShipmentDetails(env, inputDocumentInitial, releaseNo, orderReleaseListOutDoc, shipmentListOutput);
                // otherwise fetch the shipment details
                else
                	shipmentListOutput = getShipmentListForOrder(env, orderReleaseListOutDoc, orderNo, releaseNo);
                
                // To handle scenarios where shipment update with WMSCode = 2 is received prior to shipment update with WMSCode = 1
                if (!YFCCommon.isVoid(wmsCode) && VAL_TWO.equals(wmsCode)) {
                    boolean isCode1UpdateProcessed = checkIfUpdateWithWMSCode1IsProcessed(shipmentListOutput);
            		logger.verbose("HeyDudeConfirmShipmentMultipleUpdates : performAction Y: isCode1UpdateProcessed " +isCode1UpdateProcessed);

                	// WMSCode = 2 msg came before WMSCode = 1
                	if(!isCode1UpdateProcessed) {
                        String isAsyncProcess = inDocEle.getAttribute(A_IS_ASYNC_PROCESS);
                		logger.verbose("HeyDudeConfirmShipmentMultipleUpdates : performAction Y: isAsyncProcess " +isAsyncProcess);

                        // Shipment update was triggered by ASYNC_REQ
                		if(!YFCCommon.isVoid(isAsyncProcess) && FLAG_Y.equals(isAsyncProcess)) { 
                			logger.verbose("HeyDudeConfirmShipmentMultipleUpdates : performAction Y: Throw exception when input is re-triggered by ASYNC_REQ_PROCESSOR: " + env.getProgId());
                			throw new YFSException(CrocsErrorConstants.VAL_ERROR_DESCRIPTION_WMSCODE,CrocsErrorConstants.VAL_ERROR_CODE_EXTN_005,CrocsErrorConstants.VAL_ERROR_DESCRIPTION_EXTN_005);
                		}

                        // Shipment update came by WMS
            			logger.verbose("HeyDudeConfirmShipmentMultipleUpdates : performAction Y: Message received by WMS for the first time: ");
            			String orderHeaderKey = SCXmlUtil.getXpathAttribute(orderReleaseListOutDoc.getDocumentElement(),XPATH_ORDER_RELEASE_LIST_ORDER_RELEASE_ORDER_HEADER_KEY);
            			logger.verbose("HeyDudeConfirmShipmentMultipleUpdates : performAction Y: orderHeaderKey: " + orderHeaderKey);
                        inDocEle.setAttribute(A_ORDER_HEADER_KEY, orderHeaderKey);
                        inDocEle.setAttribute(A_IS_ASYNC_PROCESS, FLAG_Y);
                        
//                        String formattedOrderNo = orderNo + "SHIP" + releaseNo;
//                        inDocEle.setAttribute(A_ORDER_NO, formattedOrderNo);
                        
//                        logger.info("inDocEle after updating order no : " + formattedOrderNo +" " + SCXmlUtil.getString(inDocEle));

                        // prepare input for createAsyncReq api
                        Document asyncRequestDoc = prepareCreateAsyncRequestInput(inDocEle);

                        // invoke createAsyncRequestAPI 
                        invokeCreateAsyncRequestAPI(env,asyncRequestDoc);                        

                        logger.verbose("End of method performAction without invoking confirmShipmentAPI because the WMSCode = 2 update was received before the WMSCode = 1 update: " + SCXmlUtil.getString(inputDocumentFinal));
                        return inputDocumentInitial;
                	}     		                          	          
                }

                // update shipment related attributes
                updateShipmentAttributes(inputDocumentInitial, shipmentListOutput);

                addShipmentLineForCancellation(inputDocumentInitial, orderReleaseListOutDoc);

                updateContainerDetailsForFlagY(env, inputDocumentInitial,orderReleaseListOutDoc);

                updateTrackingNo(env, orderNo, orderReleaseListOutDoc, inputMsgCopy, inputDocumentInitial);

                outDoc = invokeConfirmShipmentAPI(env, inputDocumentInitial);
                logger.info("HeyDudeConfirmShipmentMultipleUpdates : performAction Y : after confirmShipment " +orderNo);

                break;

             // code 1
            case FLAG_N:    
            	// invoke getOrderReleaseList 
                orderReleaseListOutDoc = getOrderReleaseList(env, orderNo, releaseNo);
                
                currentOrderStatus = SCXmlUtil.getXpathAttribute(orderReleaseListOutDoc.getDocumentElement(),"/OrderReleaseList/OrderRelease/@Status");

                // if order is in released status then create a shipment for it
                if(!YFCCommon.isVoid(currentOrderStatus) && currentOrderStatus.equals(RELEASED)) 
                    shipmentListOutput = createShipmentAndReturnShipmentDetails(env, inputDocumentInitial, releaseNo, orderReleaseListOutDoc, shipmentListOutput);
                // otherwise fetch the shipment details
                else
                	shipmentListOutput = getShipmentListForOrder(env, orderReleaseListOutDoc, orderNo, releaseNo);
                			
                updateShipmentAttributes(inputDocumentInitial, shipmentListOutput);

                String originalShipmentNo = inDocEle.getAttribute(A_SHIPMENT_NO);
                
                inDocEle.setAttribute(A_BOL_NO, originalShipmentNo);                
                
                String newShipmentNo = splitShipment(env, inputDocumentInitial);
                inDocEle.setAttribute(A_SHIPMENT_NO, newShipmentNo);

                updateContainerDetailsForFlagN(env, inputDocumentInitial, orderReleaseListOutDoc);

                updateTrackingNo(env, orderNo, orderReleaseListOutDoc, inputMsgCopy, inputDocumentInitial);
                    
                outDoc = invokeConfirmShipmentAPI(env, inputDocumentInitial);
                
                logger.info("HeyDudeConfirmShipmentMultipleUpdates : performAction N : after confirmShipment" +orderNo);
                
                break;
            default:

                throw new YFSException("Invalid Action provided for the operation", "", "actionItem expected [CANCEL_NON_SHIPPED_QUANTITY_Y,BACKORDERED_NON_SHIPPED_QUANTITY_N] provided:"
                                + actionItem
                                + "\n If BackOrderNonShippedQuantity=Y then CANCEL_NON_SHIPPED_QUANTITY_Y action is called else BACKORDERED_NON_SHIPPED_QUANTITY_N is called");
        }
        logger.verbose("End of method processShipmentConfirmUpdates with output: " + SCXmlUtil.getString(inputDocumentFinal));
        return outDoc;
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
        logger.info("HeyDudeConfirmShipmentMultipleUpdates: Start of method invokeConfirmShipmentAPI with input: " + SCXmlUtil.getString(inputDoc));

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
     *
     * @param env env
     * @param
     * @return String
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
        Document splitShipmentOutDoc = null;
        try {
            splitShipmentOutDoc = CommonUtil.invokeAPI(env, "", API_SPLIT_SHIPMENT, splitShipmentInDoc);
        } catch (Exception e) {
            logger.error("Error invoking splitShipment API: " + e.getMessage());
            throw new YFSException("Error invoking splitShipment API: " + e.getMessage());
        }

        if (splitShipmentOutDoc != null) {
        	fetchPrimeLinesNo(splitShipmentOutDoc);
            Element splitShipmentOutDocEle = splitShipmentOutDoc.getDocumentElement();
            Element splitShipmentOutDocTargetEle = SCXmlUtil.getChildElement(splitShipmentOutDocEle, E_TARGET);
            Element splitShipmentOutDocShipmentEle = SCXmlUtil.getChildElement(splitShipmentOutDocTargetEle, E_SHIPMENT);
            String newShipmentNo = splitShipmentOutDocShipmentEle.getAttribute(A_SHIPMENT_NO);

            logger.verbose("End of method splitShipment with output: " + newShipmentNo);
            return newShipmentNo;
        } else {
            throw new YFSException("No output received from splitShipmentAPI: " + SCXmlUtil.getString(splitShipmentOutDoc));
        }
    }

    private void fetchPrimeLinesNo(Document splitShipmentOutDoc) {
    	logger.info("HeyDudeConfirmShipmentMultipleUpdates: fetchPrimeLinesNo: Start");
        Element eleSplitShipment = splitShipmentOutDoc.getDocumentElement();
        Element eleShipmentLines = SCXmlUtil.getXpathElement(eleSplitShipment, "/SplitShipment/Target/Shipment/ShipmentLines");
        
		System.out.println(SCXmlUtil.getString(eleShipmentLines));
		if(eleShipmentLines != null) {
            NodeList nlShipmentlines = eleShipmentLines.getElementsByTagName("ShipmentLine");
            if(nlShipmentlines != null) {
                for (int k = 0; k < nlShipmentlines.getLength(); k++) {
                    Element eleShipmentLine = (Element) nlShipmentlines.item(k);
                    String primeLineNo = eleShipmentLine.getAttribute("PrimeLineNo");
                    String shipmentLineNo = eleShipmentLine.getAttribute("ShipmentLineNo");
                    if (!primeLineNo.isEmpty() && !shipmentLineNo.isEmpty()) {
                    	mapPrimeLineNo.put(primeLineNo, shipmentLineNo);
                    }
                }
            }

		}
		if(mapPrimeLineNo != null) {
			mapPrimeLineNo.forEach((prime, shipLine) -> logger.info("PrimeLineNo: " + prime + " => ShipmentLineNo: " + shipLine));
		}
		logger.info("HeyDudeConfirmShipmentMultipleUpdates: fetchPrimeLinesNo: End");		
	}

	/**
     * It will update the container details for "Y" case so that one container will have only one shipment line in it
     *
     * @param inDoc
	 * @param orderReleaseListOutDoc 
     * @return
     */
    private Document updateContainerDetailsForFlagY(YFSEnvironment env, Document inDoc, Document orderReleaseListOutDoc) throws Exception {
        logger.verbose("Start of method updateContainerDetails with input: " + SCXmlUtil.getString(inDoc));

        Element inDocEle = inDoc.getDocumentElement();
        Element containersEle = SCXmlUtil.getChildElement(inDocEle, E_CONTAINERS);
        
        /** if <Containers /> tag is empty means it is a short pick scenario we can directly return the inDoc no update at container level is required
         * <Shipment OrderNo="1001700OCUS" ReleaseNo="1" ShipmentNo="" WMSCode="5" BackOrderNonShippedQuantity="Y">
				<Containers/>
				<ShipmentLines>
					<ShipmentLine ItemID="10002-002-M17" OrderNo="1001700OCUS" Quantity="0" ReleaseNo="1" ShipmentLineNo="1" UnitOfMeasure="EACH"/>
				</ShipmentLines>
			</Shipment>
         */ 
        if(containersEle.hasChildNodes()) {
            List<Element> containerListEle = SCXmlUtil.getChildrenList(containersEle);
            Document getOrganizationListInDoc = SCXmlUtil.createDocument(E_ORGANIZATION);

            for (Element eachContainer : containerListEle) {
                //SCAC value received from WMS
                String scac = eachContainer.getAttribute(A_SCAC);
                //EOMS-6010 changes start
                eachContainer.setAttribute(A_EXTERNAL_REFERENCE_1,scac);
                logger.verbose("ExternalRef1 is: " + eachContainer.getAttribute(A_EXTERNAL_REFERENCE_1));
                //EOMS-6010 changes end
                Document updateSCACOutDoc = updateSCAC(env, inDoc, scac);
                Element outEleCC = updateSCACOutDoc.getDocumentElement();
                String scacValue = SCXmlUtil.getXpathAttribute(outEleCC, XPATH_CODE_SHORT_DESCRIPTION);
                String strSCACandService = SCXmlUtil.getXpathAttribute(outEleCC, XPATH_CODE_LONG_DESCRIPTION);
                //Update SCAC at container level
                eachContainer.setAttribute(A_SCAC, scacValue);
                Element scacAndServiceElementImport = getScacAndServiceList(env, inDoc, scacValue, strSCACandService);
                eachContainer.appendChild(scacAndServiceElementImport);
                //this SCAC will have value based on OMS confirguration
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
                    containerShipmentLine.setAttribute(A_SHIPMENT_LINE_NO, shipmentLine.getAttribute(A_SHIPMENT_LINE_NO));
                }
                String trackingNo = eachContainer.getAttribute(A_TRACKING_NO);
                Element extn = SCXmlUtil.createChild(eachContainer, E_EXTN);
                
                strPrimaryUrl = strPrimaryUrl.replaceAll(A_TRACKING_NO, trackingNo);
                strPrimaryUrl = updatePrimaryURLAsPerLoacale(strPrimaryUrl,orderReleaseListOutDoc);
                extn.setAttribute(A_EXTN_TRACKING_URL, strPrimaryUrl);
            }
        }

        logger.verbose("End of method updateContainerDetails with output: " + SCXmlUtil.getString(inDoc));
        return inDoc;
    }

	/**
     * It will update the container details for "N" case so that one container will have only one shipment line in it
     * and we are also updating the shipment line no according to the new shipment no
     *
     * @param inDoc
     * @param orderReleaseListOutDoc 
     * @return
     */
    private Document updateContainerDetailsForFlagN(YFSEnvironment env, Document inDoc, Document orderReleaseListOutDoc) throws Exception {
        logger.verbose("Start of method updateContainerDetails with input: " + SCXmlUtil.getString(inDoc));

        Element inDocEle = inDoc.getDocumentElement();
        Element containersEle = SCXmlUtil.getChildElement(inDocEle, E_CONTAINERS);
        
        /** if <Containers /> tag is empty means it is a short pick scenario we can directly return the inDoc no update at container level is required
         * <Shipment OrderNo="1001700OCUS" ReleaseNo="1" ShipmentNo="" WMSCode="1" BackOrderNonShippedQuantity="N">
				<Containers/>
				<ShipmentLines>
					<ShipmentLine ItemID="10002-002-M17" OrderNo="1001700OCUS" Quantity="0" ReleaseNo="1" ShipmentLineNo="1" UnitOfMeasure="EACH"/>
				</ShipmentLines>
			</Shipment>
         */        
        if(containersEle.hasChildNodes()) {
            List<Element> containerListEle = SCXmlUtil.getChildrenList(containersEle);
            Document getOrganizationListInDoc = SCXmlUtil.createDocument(E_ORGANIZATION);


            for (Element eachContainer : containerListEle) {
                //SCAC value received from WMS
                String scac = eachContainer.getAttribute(A_SCAC);
                //EOMS-6010 changes start
                eachContainer.setAttribute(A_EXTERNAL_REFERENCE_1,scac);
                //EOMS-6010 changes end
                Document updateSCACOutDoc = updateSCAC(env, inDoc, scac);
                logger.verbose("Output updateSCAC: " + SCXmlUtil.getString(updateSCACOutDoc));
                Element outEleCC = updateSCACOutDoc.getDocumentElement();
                String scacValue = SCXmlUtil.getXpathAttribute(outEleCC, XPATH_CODE_SHORT_DESCRIPTION);
                String strSCACandService = SCXmlUtil.getXpathAttribute(outEleCC, XPATH_CODE_LONG_DESCRIPTION);
                //Update SCAC at container level
                eachContainer.setAttribute(A_SCAC, scacValue);
                Element scacAndServiceElementImport = getScacAndServiceList(env, inDoc, scacValue, strSCACandService);
                eachContainer.appendChild(scacAndServiceElementImport);
                logger.verbose("Output scacAndServiceList: " + SCXmlUtil.getString(scacAndServiceElementImport));
                //this SCAC will have value based on OMS confirguration
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

                    /* This changes are made as per the EOMS-3437
                     * Here we are updating the shipment line no within each container as line no 1,2,3..
                     * Because after split shipment the line no is different when compared to the line no in original line no
                     * */
                    String mappedValue = mapPrimeLineNo.get(shipmentLine.getAttribute(A_SHIPMENT_LINE_NO));
                    if(mappedValue != null) {
                    	containerShipmentLine.setAttribute(A_SHIPMENT_LINE_NO, mappedValue);
                    }                    
                 
                }
                String trackingNo = eachContainer.getAttribute(A_TRACKING_NO);
                Element extn = SCXmlUtil.createChild(eachContainer, E_EXTN);
                strPrimaryUrl = strPrimaryUrl.replaceAll(A_TRACKING_NO, trackingNo);
                strPrimaryUrl = updatePrimaryURLAsPerLoacale(strPrimaryUrl,orderReleaseListOutDoc);
                extn.setAttribute(A_EXTN_TRACKING_URL, strPrimaryUrl);
            }

            /* This changes are made as per the EOMS-3437
             * Here we are updating the shipment line no inside shipment lines tag from the input as line no 1,2,3..
             * Because after split shipment the line no is different when compared to the line no in original line no
             * */
            Element shipmentLines = SCXmlUtil.getChildElement(inDocEle, E_SHIPMENT_LINES);
            List<Element> shipmentLineList = SCXmlUtil.getChildrenList(shipmentLines);
            
            for (Element eachShipmentLine : shipmentLineList) {
            	logger.info(containersEle);
                String mappedValue = mapPrimeLineNo.get(eachShipmentLine.getAttribute(A_SHIPMENT_LINE_NO));
                if(mappedValue != null) {
                	eachShipmentLine.setAttribute(A_SHIPMENT_LINE_NO, mappedValue);
                }              
            }
        }
        
        logger.info("End of method updateContainerDetails with output: " + SCXmlUtil.getString(inDoc));
        return inDoc;
    }

    /**
     * Updates shipment-related attributes on the input document using details from a created shipment.
     * The method identifies the shipment in status {@code 1100} from the provided
     * ShipmentList document and copies key attributes such as ShipmentNo, EnterpriseCode, DocumentType, ShipNode, and SellerOrganizationCode
     * to the input document. 
     * Modification rules are overridden as part of the update.
     * 
     * @param inDoc           Input document whose root element is updated with shipment attributes
     * @param shipmentInDoc   ShipmentList document containing shipment details
     */

    private void updateShipmentAttributes(Document inDoc, Document shipmentInDoc) {
        logger.verbose("HeyDudeConfirmShipmentMultipleUpdates : Start of method updateShipmentAttributes with inDoc: " + SCXmlUtil.getString(inDoc));
        logger.verbose("shipmentInDoc: " + SCXmlUtil.getString(shipmentInDoc));

        Element shipmentListEle = shipmentInDoc.getDocumentElement();
        // take shipment which is in created status
        Element shipmentElement = SCXmlUtil.getXpathElement(shipmentListEle, "/ShipmentList/Shipment[Status[@Status='1100']]");
        logger.verbose("HeyDudeConfirmShipmentMultipleUpdates : shipmentElement : " + SCXmlUtil.getString(shipmentElement));
        
        Element inDocEle = inDoc.getDocumentElement();
        inDocEle.setAttribute(A_SHIPMENT_NO, shipmentElement.getAttribute(A_SHIPMENT_NO));
        inDocEle.setAttribute(A_OVERRIDE_MODIFICATION_RULES, FLAG_Y);
        inDocEle.setAttribute(A_ENTERPRISE_CODE, shipmentElement.getAttribute(A_ENTERPRISE_CODE));
        inDocEle.setAttribute(A_DOCUMENT_TYPE, shipmentElement.getAttribute(A_DOCUMENT_TYPE));
        inDocEle.setAttribute(A_SHIP_NODE, shipmentElement.getAttribute(A_SHIP_NODE));
        inDocEle.setAttribute(A_SELLER_ORGANIZATION_CODE, shipmentElement.getAttribute(A_SELLER_ORGANIZATION_CODE));

        logger.verbose("HeyDudeConfirmShipmentMultipleUpdates : End of method updateShipmentAttributes with updated inDoc: " + SCXmlUtil.getString(inDoc));
    }

    /**
     * Invokes the HeyDudeGetOrderReleaseList service to retrieve order release
     * details for the given order and release number.
     *
     * @param env       The YFS environment used to invoke the service
     * @param orderNo   The order number for which the release details are requested
     * @param releaseNo The release number associated with the order
     * @return          XML Document containing the order release list response
     * @throws Exception If an error occurs while creating the request or invoking the service
     */
    private Document getOrderReleaseList(YFSEnvironment env, String orderNo, String releaseNo) throws Exception {
		logger.verbose("HeyDudeConfirmShipmentMultipleUpdates : Start of method getOrderReleaseList with orderNo: " + orderNo);
		logger.verbose("HeyDudeConfirmShipmentMultipleUpdates : Start of method getOrderReleaseList with releaseNo: " + releaseNo);
        Document getOrderReleaseListIndoc = SCXmlUtil.createDocument(E_ORDER_RELEASE);
        Element getOrderReleaseListEle = getOrderReleaseListIndoc.getDocumentElement();
        getOrderReleaseListEle.setAttribute(A_RELEASE_NO, releaseNo);

        Element orderEle = SCXmlUtil.createChild(getOrderReleaseListEle, E_ORDER);
        orderEle.setAttribute(A_ORDER_NO, orderNo);
        orderEle.setAttribute(A_DOCUMENT_TYPE, VAL_DOCUMENT_TYPE_SALES_ORDER);

        logger.verbose("HeyDudeConfirmShipmentMultipleUpdates : getOrderReleaseList : Input to HeyDudeGetOrderReleaseList: " + SCXmlUtil.getString(getOrderReleaseListIndoc));
        Document orderReleaseListOutDoc = CommonUtil.invokeService(env, SERVICE_HEY_DUDE_GET_ORDER_RELEASE_LIST, getOrderReleaseListIndoc);
        logger.verbose("HeyDudeConfirmShipmentMultipleUpdates : getOrderReleaseList : Output from HeyDudeGetOrderReleaseList: " + SCXmlUtil.getString(orderReleaseListOutDoc));

        logger.verbose("HeyDudeConfirmShipmentMultipleUpdates : End of method getOrderReleaseList with orderReleaseListOutDoc:" + SCXmlUtil.getString(orderReleaseListOutDoc));
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
     * @throws Exception
     */
    private Document getShipmentListForOrder(YFSEnvironment env, Document inDoc, String orderNo, String releaseNo) throws Exception {
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

          //EOMS-4670 Start
          Document shipmentListDoc = CommonUtil.invokeService(env, SERVICE_HEY_DUDE_GET_SHIPMENT_LIST_FOR_ORDER, getShipmentListIndoc);
          //EOMS-4670 End
          logger.debug("Shipment List output: " + SCXmlUtil.getString(shipmentListDoc));

          logger.verbose("End of method getShipmentListForOrder with output: " + SCXmlUtil.getString(shipmentListDoc));
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
	public  Document updateSCAC(YFSEnvironment env, Document inDoc, String scac) throws Exception {
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
			throw new YFCException("getMessage:"+e.getMessage(),e.toString());
		}

		return outDocCC;
	}

    private Element getScacAndServiceList(YFSEnvironment env, Document inDoc, String strSCAC, String strSCACandService) throws Exception {
        //calling getscacadndserviceList api. From output will take ScacandService element and append to inDoc.
        Document getScacAndServiceInput = SCXmlUtil.createDocument(A_SCAC_AND_SERVICE);

        getScacAndServiceInput.getDocumentElement().setAttribute(A_SCAC_KEY, strSCAC);
        getScacAndServiceInput.getDocumentElement().setAttribute(A_SCAC_AND_SERVICE, strSCACandService);
        // invoke getScacAndServiceList api
        Document getScacAndServiceOutput = CommonUtil.invokeAPI(env, TEMPLATE_GET_SCAC_AND_SERVICE_LIST, API_GET_SCAC_AND_SERVICE_LIST, getScacAndServiceInput);
        Element scacAndServiceElement = SCXmlUtil.getXpathElement(getScacAndServiceOutput.getDocumentElement(), "/ScacAndServiceList/ScacAndService");
        Element scacAndServiceElementImport = (Element) inDoc.importNode(scacAndServiceElement, A_TRUE);
        logger.verbose("End of method updateSCAC with updated input: " + SCXmlUtil.getString(inDoc));
        return scacAndServiceElementImport;
    }

    /**
     * Creates a shipment for the specified release and returns shipment details as a ShipmentList document.
     *
     * @param env                        YFS environment
     * @param inDoc                      Input document
     * @param releaseNo                  Release number
     * @param orderReleaseListOutDoc     Order release list output
     * @param getshipmentListOrderOutput Shipment list output for the order
     * @return                           ShipmentList document with created shipment
     * @throws Exception                 On API invocation or XML errors
     */
    private Document createShipmentAndReturnShipmentDetails(YFSEnvironment env, Document inDoc, String releaseNo, Document orderReleaseListOutDoc, Document getshipmentListOrderOutput) throws Exception {
		logger.verbose("HeyDudeConfirmShipmentMultipleUpdates : Start of method getOrderReleaseList with inDoc: " + SCXmlUtil.getString(inDoc));
		logger.verbose("releaseNo: " + releaseNo);
        logger.verbose("orderReleaseListOutDoc" + SCXmlUtil.getString(orderReleaseListOutDoc));
        logger.verbose("shipmentListOutput" + SCXmlUtil.getString(getshipmentListOrderOutput));

        // form input to invoke create shipment
        Document createShipmentIndoc = createShipmentInput(releaseNo, orderReleaseListOutDoc);
        logger.verbose("HeyDudeConfirmShipmentMultipleUpdates : createShipmentAndReturnShipmentDetails : Input to createShipment: " + SCXmlUtil.getString(createShipmentIndoc));
        Document createShipmentDoc = CommonUtil.invokeAPI(env, TEMPLATE_CREATE_SHIPMENT, API_CREATE_SHIPMENT, createShipmentIndoc);
        logger.verbose("HeyDudeConfirmShipmentMultipleUpdates : createShipmentAndReturnShipmentDetails : Output from createShipment: " + SCXmlUtil.getString(createShipmentDoc));

        // Create a new document with root <ShipmentList>
        Document shipmentListDoc = SCXmlUtil.createDocument("ShipmentList");
        Element shipmentListEle = shipmentListDoc.getDocumentElement();

        // Import the existing <Shipment> into the new document
        Element importedShipment = (Element) shipmentListDoc.importNode(createShipmentDoc.getDocumentElement(), true);
        shipmentListEle.appendChild(importedShipment);
        
        logger.verbose("HeyDudeConfirmShipmentMultipleUpdates : createShipmentAndReturnShipmentDetails : End of method createShipmentAndReturnShipmentDetails with shipmentListEle: " + SCXmlUtil.getString(shipmentListEle));
        return shipmentListDoc;
    }

    /**
     * Adds zero-quantity shipment lines to the input document to explicitly cancel remaining order lines during confirmShipment.
     * 
     * If shipment lines for all order lines are not present, the confirmShipment API may ship the missing lines by default. To prevent this, the method identifies
     * order lines that are not already included in the shipment request and adds corresponding ShipmentLine elements with quantity set to 0.
     *
     * @param inputDocumentInitial   Input document for ConfirmShipment API, updated in-place
     * @param orderReleaseListOutDoc Output document from getOrderReleaseList API containing order lines
     */

    private void addShipmentLineForCancellation(Document inputDocumentInitial, Document orderReleaseListOutDoc) {
		logger.verbose("HeyDudeConfirmShipmentMultipleUpdates : Start of method getOrderReleaseList with inputDocumentInitial: " + SCXmlUtil.getString(inputDocumentInitial));
		logger.verbose("orderReleaseListOutDoc: " + SCXmlUtil.getString(orderReleaseListOutDoc));

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
            if(listOforderStatus.size() == 1 && listOforderStatus.get(0).getAttribute(A_STATUS).equals(STR_STATUS_CANCELLED))
            	continue;

            Element shipmentLineToCancel = SCXmlUtil.createChild(shipmentLines, E_SHIPMENT_LINE);

            shipmentLineToCancel.setAttribute(A_ITEM_ID, SCXmlUtil.getXpathAttribute(remainingOrderLine, STR_XPATH_ITEM_ID));
            shipmentLineToCancel.setAttribute(A_ORDER_NO, orderNo);
            shipmentLineToCancel.setAttribute(A_QUANTITY, VAL_ZERO);
            shipmentLineToCancel.setAttribute(A_SHIPMENT_LINE_NO, remainingOrderLine.getAttribute(A_PRIME_LINE_NO));
            shipmentLineToCancel.setAttribute(A_RELEASE_NO, releaseNo);
            shipmentLineToCancel.setAttribute(A_UNIT_OF_MEASURE, SCXmlUtil.getXpathAttribute(remainingOrderLine, XPATH_ITEM_UNIT_OF_MEASURE));
        }
		logger.verbose("HeyDudeConfirmShipmentMultipleUpdates : Start of method getOrderReleaseList with updated inputDocumentInitial: " + SCXmlUtil.getString(inputDocumentInitial));
    }

    /**
     * Updates tracking details on order lines that are shipped.
     * <p>
     * The method derives tracking information from container details in the confirmShipment input and updates corresponding order lines using
     * the changeOrder API. 
     * No update is performed in short-pick scenarios where no containers are present.
     *
     * @param env                    YFS environment for API invocation
     * @param orderNo                Order number to be updated
     * @param orderReleaseListOutDoc Order release list output
     * @param inputMsgCopyEle        Input document containing shipment lines
     * @param inputMsgConfirmShip    confirmShipment input with container details
     * @throws Exception             If changeOrder API invocation fails
     */

    private void updateTrackingNo(YFSEnvironment env, String orderNo, Document orderReleaseListOutDoc, Element inputMsgCopyEle, Document inputMsgConfirmShip) throws Exception {
        logger.verbose("HeyDudeConfirmShipmentMultipleUpdates: Start of method updateTrackingNo : " + orderNo);
        logger.verbose("orderReleaseListOutDoc: " + XMLUtil.getXMLString(orderReleaseListOutDoc));
        logger.verbose("inputMsgCopyEle: " + XMLUtil.getElementXMLString(inputMsgCopyEle));
        logger.verbose("inputMsgConfirmShip: " + XMLUtil.getXMLString(inputMsgConfirmShip));

        Element orderReleaseListEle = orderReleaseListOutDoc.getDocumentElement();

        List<Element> listOfOrderRelease = SCXmlUtil.getChildrenList(orderReleaseListEle);
        String orderHeaderKey = "";
        String enterpriseCode = "";
        Element inputMsgConfirmShipEle = inputMsgConfirmShip.getDocumentElement();
        Element containersEle = SCXmlUtil.getChildElement(inputMsgConfirmShipEle, E_CONTAINERS);
        List<Element> listOfContainerEle = SCXmlUtil.getChildrenList(containersEle);
        
        // short pick scenario
        if(listOfContainerEle.isEmpty()) {
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

        //Preparing input for changeOrder
        Document changeOrderInDoc = SCXmlUtil.createDocument(E_ORDER);
        Element orderEle = changeOrderInDoc.getDocumentElement();

        orderEle.setAttribute(A_ACTION, VAL_MODIFY);
        orderEle.setAttribute(A_ORDER_HEADER_KEY, orderHeaderKey);
        orderEle.setAttribute(A_ORDER_NO, orderNo);
        orderEle.setAttribute(A_ENTERPRISE_CODE, enterpriseCode);    
    	orderEle.setAttribute(A_DOCUMENT_TYPE, VAL_DOCUMENT_TYPE_SALES_ORDER);
        orderEle.setAttribute(A_OVERRIDE, FLAG_Y);
        
        Element orderLineListEle = SCXmlUtil.createChild(orderEle, E_ORDER_LINES);
        Element shipmentLines = SCXmlUtil.getChildElement(inputMsgCopyEle, E_SHIPMENT_LINES);
        List<Element> listOfShipment = SCXmlUtil.getChildrenList(shipmentLines);

        OffsetDateTime currentDate = OffsetDateTime.now(ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String systemDate = currentDate.format(formatter);	
		
		int noOfShipmentLinesAvailable = listOfShipment.size();
		int linesNoToStart = 0;
		for(Element eachContainer : listOfContainerEle) {
			
			String trackingUrlAtContainer = SCXmlUtil.getXpathAttribute(eachContainer, "Extn/@ExtnTrackingUrl");
	        String trackingNoAtContainer = eachContainer.getAttribute(A_TRACKING_NO);
	        String scacAtContainer = eachContainer.getAttribute(A_SCAC);
	        
	        Element containerDetailsEle = SCXmlUtil.getChildElement(eachContainer, E_CONTAINER_DETAILS);
	        List<Element> listOfContainerDetailEle = SCXmlUtil.getChildrenList(containerDetailsEle);
	        
	        int numberOfShipmentLinesAtContainer = listOfContainerDetailEle.size();
	        
	        
	        while(numberOfShipmentLinesAtContainer > 0) {
	        	// updating the tracking details at each order line
	        	if(linesNoToStart < noOfShipmentLinesAvailable) {
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
            logger.verbose("HeyDudeConfirmShipmentMultipleUpdates: updateTrackingNo: input to changeOrder: " + XMLUtil.getXMLString(changeOrderInDoc));
            Document changeOrderOutDoc = CommonUtil.invokeAPI(env, "", API_CHANGE_ORDER, changeOrderInDoc);
            logger.verbose("HeyDudeConfirmShipmentMultipleUpdates: updateTrackingNo: output from changeOrder: " + XMLUtil.getXMLString(changeOrderOutDoc));

        } catch (Exception e) {
            logger.error("Exception in changeOrder method: " + e.getMessage(), e);
            throw e; // Rethrow exception to propagate to caller
        }
        logger.verbose("HeyDudeConfirmShipmentMultipleUpdates: End of method updateTrackingNo : ");
    }
    
    /**
     * Prepares a document for creating an asynchronous request with the provided input element.
     * @param inDocEle     The input XML element to be wrapped in the async request document.
     * @return             A Document representing the async request api input.
     */
	private Document prepareCreateAsyncRequestInput(Element inDocEle) {
		logger.verbose("HeyDudeConfirmShipmentMultipleUpdates: Start of method prepareCreateAsyncRequestInput with inDocEle: " + SCXmlUtil.getString(inDocEle));

		// Create root async request document
		Document asyncReqDoc = SCXmlUtil.createDocument(E_CREATE_ASYNC_REQUEST);
		Element asyncReqEle = asyncReqDoc.getDocumentElement();

		// Add API element
		Element apiElement = SCXmlUtil.createChild(asyncReqEle, E_API);
		apiElement.setAttribute(A_IS_SERVICE, FLAG_Y);
		apiElement.setAttribute(A_NAME, SERVICE_HEYDUDE_CONFIRM_SHIPMENT_SYNC_SERV);

		// Imported inDocEle element to Input
		Element inputElement = SCXmlUtil.createChild(apiElement, E_INPUT);
		SCXmlUtil.importElement(inputElement, inDocEle);

		logger.verbose("HeyDudeConfirmShipmentMultipleUpdates: End of method prepareCreateAsyncRequestInput with asyncReqDoc: " + SCXmlUtil.getString(asyncReqDoc));
		return asyncReqDoc;		
	}

    /**
     * This function is used to invoke createAsyncRequestAPI
     * @param env
     * @param createAsyncReqDoc The input received to invoked the createAsyncRequestAPI
     * @return	The response returned by createAsyncRequestAPI
     */
    private Document invokeCreateAsyncRequestAPI(YFSEnvironment env, Document createAsyncReqDoc) {
        logger.verbose("HeyDudeConfirmShipmentMultipleUpdates: Start of method invokeCreateAsyncRequestAPI with input: " + SCXmlUtil.getString(createAsyncReqDoc));
    	try {
            Document result = CommonUtil.invokeAPI(env, "", API_CREATE_ASYNC_REQUEST, createAsyncReqDoc);
            logger.verbose("End of method invokeCreateAsyncRequestAPI with output: " + SCXmlUtil.getString(result));
            return result;
    	}catch (Exception e) {
    		logger.error("Error invoking createAsyncRequestAPI API: " + e.getMessage());
            throw new YFSException("Error invoking createAsyncRequestAPI API: " + e.getMessage());
		}
    }

	/**
     * If Update with WMSCode = 1 is processed then at the Shipment/@Code would be 1
	 * @param shipmentListOutput     The output Document from the getShipmentList API, containing shipment details.
     * @return						 true if any shipment has Code = 1; false otherwise.
     */
    private boolean checkIfUpdateWithWMSCode1IsProcessed(Document shipmentListOutput) {
    	logger.verbose("HeyDudeConfirmShipmentMultipleUpdates: Start of method checkIfUpdateWithWMSCode1IsProcessed with shipmentListOutput: " + SCXmlUtil.getString(shipmentListOutput));
    	Element shipmentListEle = shipmentListOutput.getDocumentElement();
    	List<Element> listOfShipment = SCXmlUtil.getChildrenList(shipmentListEle);

    	for(Element eachShipment : listOfShipment) {
    		if(VAL_ONE.equals(eachShipment.getAttribute(A_CODE))) {
    	    	logger.verbose("HeyDudeConfirmShipmentMultipleUpdates: Code = 1 found in shipment Returning true");
    	    	logger.verbose("HeyDudeConfirmShipmentMultipleUpdates: End of method checkIfUpdateWithWMSCode1IsProcessed");
    	    	return true;
    		}
    	}
        logger.verbose("No shipment found with Code = 1. Returning false.");
    	logger.verbose("HeyDudeConfirmShipmentMultipleUpdates: End of method checkIfUpdateWithWMSCode1IsProcessed:");
    	return false;
    }
    
    /**
     * This method updates the provided tracking URL with the appropriate locale 
     * based on the enterprise code and customer locale.
     * 
     * @param trackingURL The original tracking URL that needs to be updated.
     * @param orderReleaseListOutDoc The XML document containing order release details.
     * @return The updated tracking URL with the appropriate locale.
     */
    private String updatePrimaryURLAsPerLoacale(String trackingURL, Document orderReleaseListOutDoc) {
    	logger.verbose("HeyDudeConfirmShipmentMultipleUpdates: Start of method updatePrimaryURLAsPerLoacale with orderReleaseListOutDoc: " + SCXmlUtil.getString(orderReleaseListOutDoc));
    	logger.verbose("trackingURL: " + trackingURL);

    	Element orderReleaseListEle = orderReleaseListOutDoc.getDocumentElement();
    	
    	String enterpriseCode = SCXmlUtil.getXpathAttribute(orderReleaseListEle, XPATH_ORDERRELEASELIST_ORDERRELEASE_ENTERPRISECODE);
    	String extnCustomerLocale = SCXmlUtil.getXpathAttribute(orderReleaseListEle, XPATH_ORDERRELEASELIST_ORDERRELEASE_ORDER_EXTNCUSTOMERLOCALE);
    	
    	String localeToBeUpdated = "";
    	
    	if(trackingURL.contains(N_LOCALE)) {
    		switch (enterpriseCode) {
			case HEYDUDE_US:
				localeToBeUpdated = LOCALE_US_EN;
				break;
			case HEYDUDE_CA:
				if(extnCustomerLocale.equals(STR_EN_CA))
					localeToBeUpdated = LOCALE_CA_EN;
				else
					localeToBeUpdated = LOCALE_CA_FR;
				break;
			case HEYDUDE_AU:
				localeToBeUpdated = LOCALE_AU_EN;
				break;
			default:
				localeToBeUpdated = LOCALE_US_EN;
				break;
			}
    		trackingURL = trackingURL.replace(N_LOCALE, localeToBeUpdated);
    	}
    	logger.verbose("HeyDudeConfirmShipmentMultipleUpdates: End of method updatePrimaryURLAsPerLoacale with updated trackingURL: " + trackingURL);
    	return trackingURL;
	}
}
