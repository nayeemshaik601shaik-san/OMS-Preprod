package com.crocs.oms.shipment;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsErrorConstants;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.crocs.oms.shipment.emea.CrocsEMEAConfirmShipment;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCException;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * this class is used to process ship confirmation msg from WMS
 */
public class CrocsShipmentPackedToShippedStatusFromWMS extends CrocsCreateShipInputForShipmentUpdatesFromWMS implements CrocsConstant {
    private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsShipmentPackedToShippedStatusFromWMS.class);
    Map<String, String> mapPrimeLineNo = new HashMap<>();

    /**
     * input as received to this service:-
     * <Shipment BackOrderNonShippedQuantity="Y"  WMSCode="1" OrderNo="54929265CUS" ReleaseNo="1" >
     * <Containers>
     * <Container TrackingNo="123TRACKING00003" ContainerNo="00009999990000036003"  SCAC="FEDEX">
     * <ContainerDetails>
     * <ContainerDetail Quantity="6">
     * <ShipmentLine Quantity="3" ShipmentLineNo="1"/>
     * <ShipmentLine Quantity="3" ShipmentLineNo="2"/>
     * </ContainerDetail>
     * </ContainerDetails>
     * </Container>
     * </Containers>
     * <ShipmentLines>
     * <ShipmentLine ItemID="40002-001-M18" OrderNo="54929265CUS" Quantity="3" ReleaseNo="1" ShipmentLineNo="1"  UnitOfMeasure="EACH"/>
     * <ShipmentLine ItemID="40003-001-M17" OrderNo="54929265CUS" Quantity="3" ReleaseNo="1" ShipmentLineNo="2"  UnitOfMeasure="EACH"/>
     * </ShipmentLines>
     * </Shipment>
     * <p>
     * <p>
     * <p>
     * Purpose:- If the attribute of BackOrderNonShippedQuantity="Y" this attribute
     * will be changed to CancelNonShippedQuantity Then the quantity mentioned in
     * line will be shipped and the remaining ones will be cancelled
     * <p>
     * If the attribute of BackOrderNonShippedQuantity="N" Then the quantity
     * mentioned in line will be split and shipped, and the remaining ones will be
     * in packed status waiting for future updated from WMS
     */

    public Document processPackedShipmentToConfirmFromWMS(YFSEnvironment env, Document inDoc) throws Exception {
        logger.verbose("Start of method processPackedShipmentToConfirmFromWMS with input: " + SCXmlUtil.getString(inDoc));
	logger.info("CrocsShipmentPackedToShippedStatusFromWMS : processPackedShipmentToConfirmFromWMS : inDoc: " + SCXmlUtil.getString(inDoc));
        String cancelNonShippedQuantity = "", backOrderNonShippedQuantity = "";

        Element inDocEle = inDoc.getDocumentElement();
        cancelNonShippedQuantity = inDocEle.getAttribute(A_CANCEL_NON_SHIPPED_QUANTITY);
        backOrderNonShippedQuantity = inDocEle.getAttribute(A_BACK_ORDER_NON_SHIPPED_QUANTITY);

        Document outDoc = null;
        
        // EOMS-10448: EMEA Shipment Changes Start
        // Confirm Shipment updates for CrocsUS, CrocsAU, and EMEA are received through the same queue.
        // Validate the enterprise code before proceeding, as EMEA supports mixed cart scenarios.
        
        String strOrderNo = inDocEle.getAttribute(A_ORDER_NO);
        String strReleaseNo = inDocEle.getAttribute(A_RELEASE_NO);
        
        Document docOrderReleaseList = getOrderReleaseList(env, inDoc, strOrderNo, strReleaseNo);
        
        if(!YFCCommon.isVoid(docOrderReleaseList)) {
        	Element eleOrderReleaseList = docOrderReleaseList.getDocumentElement();
        	String strEnterpriseCode = SCXmlUtil.getXpathAttribute(eleOrderReleaseList, XPATH_ORDERRELEASELIST_ORDERRELEASE_ENTERPRISECODE);
        	
        	if(CROCS_EMEA_ENTERPRISES.contains(strEnterpriseCode)) {
                logger.verbose("CrocsShipmentPackedToShippedStatusFromWMS: processPackedShipmentToConfirmFromWMS: EMEA enterprise identified : " + strEnterpriseCode);
                logger.verbose("Invoking CrocsEMEAConfirmShipment.processShipmentUpdate() to handle EMEA related shipment scenarios: ");
        		return new CrocsEMEAConfirmShipment().processShipmentUpdate(env, inDoc, docOrderReleaseList);
        	}
        }

        
        if (null == backOrderNonShippedQuantity || FLAG_N.equals(backOrderNonShippedQuantity))
            outDoc = performAction(env, inDoc, docOrderReleaseList, FLAG_N);

        else if (FLAG_Y.equals(cancelNonShippedQuantity))
            outDoc = performAction(env, inDoc, docOrderReleaseList, FLAG_Y);
        
        // EOMS-10448: EMEA Shipment Changes End
        
        logger.verbose("End of method processPackedShipmentToConfirmFromWMS with output: " + SCXmlUtil.getString(outDoc));
        return outDoc;

    }

    /**
     * Based on the actionItem provided decision will be taken to make the changes for code 1, 2 or 5
     *
     * @param env
     * @param inputDocumentInitial
     * @param orderReleaseListOutDoc 
     * @param actionItem
     * @return
     * @throws Exception
     */
    private Document performAction(YFSEnvironment env, Document inputDocumentInitial, Document orderReleaseListOutDoc, String actionItem) throws Exception {
        logger.verbose("Start of method processShipmentConfirmUpdates with input: " + SCXmlUtil.getString(inputDocumentInitial));
        String wmsCode = "";
        Element inDocEle = inputDocumentInitial.getDocumentElement();
        String orderType = inDocEle.getAttribute(A_ORDER_TYPE);
        Element inputMsgCopy = SCXmlUtil.getCopy(inDocEle);
        String orderNo = inDocEle.getAttribute(A_ORDER_NO);
        String releaseNo = inDocEle.getAttribute(A_RELEASE_NO);
        wmsCode = inDocEle.getAttribute(A_WMS_CODE);
        Document shipmentListOutput = null, inputDocumentFinal = null, outDoc = null;
        
        //EOMS-11897 :: URL Changes as per Locale : START
		Element orderReleaseListOutEle = orderReleaseListOutDoc.getDocumentElement();
		Element orderRelease = SCXmlUtil.getChildElement(orderReleaseListOutEle, E_ORDER_RELEASE);
		Element orderDetails = SCXmlUtil.getChildElement(orderRelease, E_ORDER);
		orderNo = orderDetails.getAttribute(A_ORDER_NO);
		String customerLocale="";
		if(!YFCCommon.isVoid(SCXmlUtil.getChildElement(orderDetails, E_EXTN))) {
			Element eleOrderExtn = SCXmlUtil.getChildElement(orderDetails, E_EXTN);
			customerLocale = eleOrderExtn.getAttribute(EXTN_CUSTOMER_LOCALE);
		}
		//EOMS-11897 :: URL Changes as per Locale : END

        switch (actionItem) {
            case FLAG_Y:    // code 2 or 5
            	// EOMS-10448: EMEA Shipment Changes Start
                // orderReleaseListOutDoc = getOrderReleaseList(env, inputDocumentInitial, orderNo, releaseNo);
                // EOMS-10448: EMEA Shipment Changes End
                shipmentListOutput = getShipmentListForOrder(env, orderReleaseListOutDoc, orderNo, releaseNo,orderType);
		logger.info("CrocsShipmentPackedToShippedStatusFromWMS : performAction Y : after getShipmentListForOrder" +orderNo);

                //this will be executed for WMSCode=3
                if (!YFCCommon.isVoid(wmsCode) && ("3".equals(wmsCode) || "5".equals(wmsCode))){
                    shipmentListOutput = createShipmentAndReturnShipmentDetails(env, inputDocumentInitial, releaseNo, orderReleaseListOutDoc, shipmentListOutput);
                }
                
                // EOMS-5877 start
                if (!YFCCommon.isVoid(wmsCode) && VAL_TWO.equals(wmsCode)) {
                    boolean isCode1UpdateProcessed = checkIfUpdateWithWMSCode1IsProcessed(shipmentListOutput);
            		logger.verbose("CrocsShipmentPackedToShippedStatusFromWMS : performAction Y: isCode1UpdateProcessed " +isCode1UpdateProcessed);

                	// WMSCode = 2 msg came before WMSCode = 1
                	if(!isCode1UpdateProcessed) {
                        String isAsyncProcess = inDocEle.getAttribute(A_IS_ASYNC_PROCESS);
                		logger.verbose("CrocsShipmentPackedToShippedStatusFromWMS : performAction Y: isAsyncProcess " +isAsyncProcess);

                        // Shipment update was triggered by ASYNC_REQ
                		if(!YFCCommon.isVoid(isAsyncProcess) && FLAG_Y.equals(isAsyncProcess)) { 
                			logger.verbose("CrocsShipmentPackedToShippedStatusFromWMS : performAction Y: Throw exception when input is re-triggered by ASYNC_REQ_PROCESSOR: " + env.getProgId());
                			throw new YFSException(CrocsErrorConstants.VAL_ERROR_DESCRIPTION_WMSCODE,CrocsErrorConstants.VAL_ERROR_CODE_EXTN_005,CrocsErrorConstants.VAL_ERROR_DESCRIPTION_EXTN_005);
                		}

                        // Shipment update came by WMS
            			logger.verbose("CrocsShipmentPackedToShippedStatusFromWMS : performAction Y: Message received by WMS for the first time: ");
            			String orderHeaderKey = SCXmlUtil.getXpathAttribute(orderReleaseListOutDoc.getDocumentElement(),XPATH_ORDER_RELEASE_LIST_ORDER_RELEASE_ORDER_HEADER_KEY);
            			logger.verbose("CrocsShipmentPackedToShippedStatusFromWMS : performAction Y: orderHeaderKey: " + orderHeaderKey);
                        inDocEle.setAttribute(A_ORDER_HEADER_KEY, orderHeaderKey);
                        inDocEle.setAttribute(A_IS_ASYNC_PROCESS, FLAG_Y);

                        // prepare input for createAsyncReq api
                        Document asyncRequestDoc = prepareCreateAsyncRequestInput(inDocEle);

                        // invoke createAsyncRequestAPI 
                        invokeCreateAsyncRequestAPI(env,asyncRequestDoc);                        

                        logger.verbose("End of method processShipmentConfirmUpdates without invoking confirmShipmentAPI because the WMSCode = 2 update was received before the WMSCode = 1 update: " + SCXmlUtil.getString(inputDocumentFinal));
                        return inputDocumentInitial;
                	}     		                          	          
                }
                // EOMS-5877 end


                updateShipmentAttributes(inputDocumentInitial, shipmentListOutput);

                addShipmentLineForCancellation(inputDocumentInitial, orderReleaseListOutDoc);
				
                updateContainerDetailsForFlagY(env, inputDocumentInitial,orderType, customerLocale);

                updateTrackingNo(env, orderNo, orderReleaseListOutDoc, inputMsgCopy, inputDocumentInitial,orderType);

                outDoc = invokeConfirmShipmentAPI(env, inputDocumentInitial);
			logger.info("CrocsShipmentPackedToShippedStatusFromWMS : performAction Y : after confirmShipment" +orderNo);

                break;

            case FLAG_N:    // code 1

            	// EOMS-10448: EMEA Shipment Changes Start
                // orderReleaseListOutDoc = getOrderReleaseList(env, inputDocumentInitial, orderNo, releaseNo);
                // EOMS-10448: EMEA Shipment Changes End
            	
                shipmentListOutput = getShipmentListForOrder(env, orderReleaseListOutDoc, orderNo, releaseNo,orderType);
		logger.info("CrocsShipmentPackedToShippedStatusFromWMS : performAction N : after getShipmentListForOrder" +orderNo);
			
                updateShipmentAttributes(inputDocumentInitial, shipmentListOutput);

                String originalShipmentNo = inDocEle.getAttribute(A_SHIPMENT_NO);
                
				//EOMS-6763 - Added condition to skip for retail orders
                if (!ORDER_TYPE_RETAIL.equals(orderType)){
                	inDocEle.setAttribute(A_BOL_NO, originalShipmentNo);
                 }
                
                outDoc = updateInputForCancellation(env,inputDocumentInitial);
                
                Element shipmentLines = SCXmlUtil.getChildElement(inputDocumentInitial.getDocumentElement(), E_SHIPMENT_LINES);
                
                if(shipmentLines.hasChildNodes()) {
                	logger.info("CrocsShipmentPackedToShippedStatusFromWMS : performAction N : before splitShipment" +orderNo);
                    String newShipmentNo = splitShipment(env, inputDocumentInitial);
                    logger.info("CrocsShipmentPackedToShippedStatusFromWMS : performAction N : after splitShipment" +orderNo);
                    inDocEle.setAttribute(A_SHIPMENT_NO, newShipmentNo);

                    updateContainerDetailsForFlagN(env, inputDocumentInitial, customerLocale);
                    
                    logger.info("CrocsShipmentPackedToShippedStatusFromWMS: performAction N: before updateTrackingNo: " + orderNo);
                    updateTrackingNo(env, orderNo, orderReleaseListOutDoc, inputMsgCopy, inputDocumentInitial,orderType);
                    
                    outDoc = invokeConfirmShipmentAPI(env, inputDocumentInitial);
                    logger.info("CrocsShipmentPackedToShippedStatusFromWMS : performAction N : after confirmShipment" +orderNo);
                }
                
                break;
            default:

                throw new YFSException("Invalid Action provided for the operation", "",
                        "actionItem expected [CANCEL_NON_SHIPPED_QUANTITY_Y,BACKORDERED_NON_SHIPPED_QUANTITY_N] provided:"
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
        logger.info("CrocsShipmentPackedToShippedStatusFromWMS: Start of method invokeConfirmShipmentAPI with input: " + SCXmlUtil.getString(inputDoc));

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
        String orderType = inDocEle.getAttribute(A_ORDER_TYPE);
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
            if (ORDER_TYPE_RETAIL.equals(orderType)){
            	 shipmentLineEle.setAttribute(A_DOCUMENT_TYPE, A_TO_DOCUMENT_TYPE);         	
            }else {           
            shipmentLineEle.setAttribute(A_DOCUMENT_TYPE, VAL_DOCUMENT_TYPE_SALES_ORDER); 
            }
        }
        Element targetEle = SCXmlUtil.createChild(splitShipmentInDocEle, E_TARGET);
        Element targetShipmentEle = SCXmlUtil.createChild(targetEle, E_SHIPMENT);

        targetShipmentEle.setAttribute(A_SELLER_ORGANIZATION_CODE, sellerOrganizationCode);
        targetShipmentEle.setAttribute(A_SHIP_NODE, shipNode);
        targetShipmentEle.setAttribute(A_SHIPMENT_NO, "");

        logger.verbose("splitShipment with input: " + SCXmlUtil.getString(splitShipmentInDoc));
        Document splitShipmentOutDoc = null;
        try {
            splitShipmentOutDoc = CommonUtil.invokeAPI(env, "", API_SPLIT_SHIPMENT,
                    splitShipmentInDoc);
        } catch (Exception e) {
            logger.error("Error invoking splitShipment API: " + e.getMessage());
            throw new YFSException("Error invoking splitShipment API: " + e.getMessage());
        }

        if (splitShipmentOutDoc != null) {
        	fetchPrimeLinesNo(splitShipmentOutDoc);
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

    private void fetchPrimeLinesNo(Document splitShipmentOutDoc) {
    	logger.info("CrocsShipmentPackedToShippedStatusFromWMS: fetchPrimeLinesNo: Start");
        Element eleSplitShipment = splitShipmentOutDoc.getDocumentElement();
        Element eleShipmentLines = SCXmlUtil.getXpathElement(eleSplitShipment, "/SplitShipment/Target/Shipment/ShipmentLines");
        
		logger.verbose(SCXmlUtil.getString(eleShipmentLines));
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
		logger.info("CrocsShipmentPackedToShippedStatusFromWMS: fetchPrimeLinesNo: End");
		
	}

	/**
     * It will update the container details for "Y" case so that one container will have only one shipment line in it
     *
     * @param inDoc
     * @return
     */
    private Document updateContainerDetailsForFlagY(YFSEnvironment env, Document inDoc, String orderType, String customerLocale) throws Exception {
        logger.verbose("Start of method updateContainerDetails with input: " + SCXmlUtil.getString(inDoc));

        String strPrimaryUrl = null;
        
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
				//EOMS-12603 :: START
                if (!(A_RETAIL_ORDER_TYPE.equalsIgnoreCase(orderType)
                        && YFCCommon.isVoid(scac))) {
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

                  logger.verbose("Calling getOrganizationList with input: " + SCXmlUtil.getString(getOrganizationListInDoc));
                 Document getOrganizationListOutDoc = CommonUtil.invokeAPI(env, TEMPLATE_GET_ORGANIZATION_LIST,
                          API_GET_ORGANIZATION_LIST, getOrganizationListInDoc);
                  logger.verbose("Output returned from getOrganizationList: " + SCXmlUtil.getString(getOrganizationListOutDoc));

                  strPrimaryUrl = SCXmlUtil.getXpathAttribute(getOrganizationListOutDoc.getDocumentElement(),
                          XPAH_PRIMARY_URL);
                  
                //EOMS-11897 :: URL Changes as per Locale : START
  				if (!YFCCommon.isVoid(customerLocale) && (STR_FR_CA.equalsIgnoreCase(customerLocale)
  						|| STR_EN_CA.equalsIgnoreCase(customerLocale))) {
  					strPrimaryUrl = strPrimaryUrl.replace(STR_EN_US, customerLocale);
  				}
  				//EOMS-11897 :: URL Changes as per Locale : END
                  
            	  
              }else {
            	  logger.verbose("SCAC value is empty for this retail order");
                
              }
				//EOMS-12603 :: END
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
				//EOMS-12603 :: START
                if (!YFCCommon.isVoid(strPrimaryUrl)) {
                	Element extn = SCXmlUtil.createChild(eachContainer, E_EXTN);
                    strPrimaryUrl = strPrimaryUrl.replaceAll(A_TRACKING_NO, trackingNo);
                    extn.setAttribute(A_EXTN_TRACKING_URL, strPrimaryUrl);
                }
                
				//EOMS-12603 :: END
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
     * @return
     */
    private Document updateContainerDetailsForFlagN(YFSEnvironment env, Document inDoc, String customerLocale) throws Exception {
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
                getOrganizationListOutDoc = CommonUtil.invokeAPI(env, TEMPLATE_GET_ORGANIZATION_LIST,
                        API_GET_ORGANIZATION_LIST, getOrganizationListInDoc);
                logger.verbose("Output returned from getOrganizationList: " + SCXmlUtil.getString(getOrganizationListOutDoc));

                String strPrimaryUrl = SCXmlUtil.getXpathAttribute(getOrganizationListOutDoc.getDocumentElement(),
                        XPAH_PRIMARY_URL);
                
                //EOMS-11897 :: URL Changes as per Locale : START
  				if (!YFCCommon.isVoid(customerLocale) && (STR_FR_CA.equalsIgnoreCase(customerLocale)
  						|| STR_EN_CA.equalsIgnoreCase(customerLocale))) {
  					strPrimaryUrl = strPrimaryUrl.replace(STR_EN_US, customerLocale);
  				}
  				//EOMS-11897 :: URL Changes as per Locale : END
                

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
     * This will update the various attributes at the Shipment level tag of the input received
     *
     * @param inDoc
     * @param shipmentInDoc
     */
    private void updateShipmentAttributes(Document inDoc, Document shipmentInDoc) {
        logger.verbose("Start of method updateShipmentAttributes with input: " + SCXmlUtil.getString(inDoc));
        logger.verbose("Start of method updateShipmentAttributes with shipmentInDoc: " + SCXmlUtil.getString(shipmentInDoc));

        Element shipmentElement = SCXmlUtil.getChildElement(shipmentInDoc.getDocumentElement(), E_SHIPMENT);
        Element inDocEle = inDoc.getDocumentElement();
        String orderType = inDocEle.getAttribute(A_ORDER_TYPE);
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
     * @throws Exception
     */
    private Document getOrderReleaseList(YFSEnvironment env, Document indoc, String orderNo, String releaseNo)
            throws Exception {
        logger.verbose("Start of method getOrderReleaseList with input: " + SCXmlUtil.getString(indoc));
        //EOMS - 6710
        Element inDocEle = indoc.getDocumentElement();
        String orderType = inDocEle.getAttribute(A_ORDER_TYPE);
        Document getOrderReleaseListIndoc = SCXmlUtil.createDocument(E_ORDER_RELEASE);
        Element getOrderReleaseListEle = getOrderReleaseListIndoc.getDocumentElement();
        getOrderReleaseListEle.setAttribute(A_RELEASE_NO, releaseNo);

        Element orderEle = SCXmlUtil.createChild(getOrderReleaseListEle, E_ORDER);
        orderEle.setAttribute(A_ORDER_NO, orderNo);
        if (ORDER_TYPE_RETAIL.equals(orderType)){
            orderEle.setAttribute(A_DOCUMENT_TYPE, A_TO_DOCUMENT_TYPE);
          }else {
        	  orderEle.setAttribute(A_DOCUMENT_TYPE, VAL_DOCUMENT_TYPE_SALES_ORDER);
          }
        
       

        logger.info("getOrderReleaseList: " + SCXmlUtil.getString(getOrderReleaseListIndoc));

        //EOMS-4670 Start
        Document orderReleaseListOutDoc = CommonUtil.invokeService(env, SERVICE_GET_ORDER_RELEASE_LIST_US, getOrderReleaseListIndoc);
        //EOMS-4670 End
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
     * @throws Exception
     */
    private Document getShipmentListForOrder(YFSEnvironment env, Document inDoc, String orderNo, String releaseNo,String orderType)
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
            
            if (ORDER_TYPE_RETAIL.equals(orderType)){
            	  getShipmentListEle.setAttribute(A_DOCUMENT_TYPE, A_TO_DOCUMENT_TYPE);
          }else {           
        	  getShipmentListEle.setAttribute(A_DOCUMENT_TYPE, VAL_DOCUMENT_TYPE_SALES_ORDER);
          }
            
          

            logger.debug("getShipmentListForOrder Input: " + SCXmlUtil.getString(getShipmentListIndoc));

            //EOMS-4670 Start
            Document shipmentListDoc = CommonUtil.invokeService(env, SERVICE_GET_SHIPMENT_LIST_FOR_ORDER, getShipmentListIndoc);
            //EOMS-4670 End
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
			inEleCC.setAttribute(CrocsXmlConstants.A_ORGANIZATION_CODE, STR_CROCS);
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
        Document getScacAndServiceOutput = CommonUtil.invokeAPI(env, TEMPLATE_GET_SCAC_AND_SERVICE_LIST, API_GET_SCAC_AND_SERVICE_LIST, getScacAndServiceInput);
        Element scacAndServiceElement = SCXmlUtil.getXpathElement(getScacAndServiceOutput.getDocumentElement(), "/ScacAndServiceList/ScacAndService");
        Element scacAndServiceElementImport = (Element) inDoc.importNode(scacAndServiceElement, A_TRUE);
        logger.verbose("End of method updateSCAC with updated input: " + SCXmlUtil.getString(inDoc));
        return scacAndServiceElementImport;
    }

    /**
     * This method is used to check status of the order by calling getShipmentListForOrder and
     * if shipment doesn't exsist it will create shipment and confirm shipment
     *
     * @param env       env
     * @param inDoc     input Doc
     * @param releaseNo releaseNo
     * @throws Exception Exception
     */
    private Document createShipmentAndReturnShipmentDetails(YFSEnvironment env, Document inDoc, String releaseNo, Document orderReleaseListOutDoc, Document getshipmentListOrderOutput) throws Exception {
        logger.verbose("Start of method createShipment with input: " + SCXmlUtil.getString(inDoc));
        logger.verbose("orderReleaseListOutDoc" + SCXmlUtil.getString(orderReleaseListOutDoc));
        logger.verbose("shipmentListOutput" + SCXmlUtil.getString(getshipmentListOrderOutput));
        Element shipmentListforOrderOutput = getshipmentListOrderOutput.getDocumentElement();
        Element shipment = SCXmlUtil.getChildElement(shipmentListforOrderOutput, E_SHIPMENT);
        boolean isOpenShipmentExists = false;
        if (!YFCCommon.isVoid(shipment)) {
            Element statusEle = SCXmlUtil.getChildElement(shipment, E_STATUS);
            String status = statusEle.getAttribute(A_STATUS_NAME);
            logger.verbose("shipmentStatus" + status);
            if (!YFCCommon.isVoid(status) && (VAL_SHIPMENT_CREATED.equalsIgnoreCase(status) || VAL_SHIPMENT_PACKED.equalsIgnoreCase(status) || VAL_SHIPMENT_SHIPPED.equalsIgnoreCase(status))) {
                isOpenShipmentExists = true;
            }
        }
        if (!isOpenShipmentExists) {
            Document createShipmentIndoc = createShipmentInput(releaseNo, orderReleaseListOutDoc);
            Document createShipmentDoc = CommonUtil.invokeAPI(env, TEMPLATE_CREATE_SHIPMENT,
                    API_CREATE_SHIPMENT, createShipmentIndoc);
            logger.verbose("createShipmentInput" + SCXmlUtil.getString(createShipmentIndoc));

            // Create a new document with root <ShipmentList>
            Document shipmentListDoc = SCXmlUtil.createDocument("ShipmentList");
            Element shipmentListEle = shipmentListDoc.getDocumentElement();

            // Import the existing <Shipment> into the new document
            Element importedShipment = (Element) shipmentListDoc.importNode(createShipmentDoc.getDocumentElement(), true);
            shipmentListEle.appendChild(importedShipment);

            logger.verbose("shipmentList doc after createShipment" + SCXmlUtil.getString(shipmentListDoc));
            return shipmentListDoc;
        } else {
            List<Element> shipmentElements = SCXmlUtil.getChildren(shipmentListforOrderOutput, E_SHIPMENT);

            for (Element shipmentEle : shipmentElements) {
                Element statusEle = SCXmlUtil.getChildElement(shipmentEle, E_STATUS);
                String status = null;
                if (statusEle != null && !YFCCommon.isVoid(statusEle.getAttribute(A_STATUS_NAME))) {
                    status = statusEle.getAttribute(A_STATUS_NAME);
                }
                if (!VAL_SHIPMENT_CREATED.equalsIgnoreCase(status) && !VAL_SHIPMENT_PACKED.equalsIgnoreCase(status)) {
                    shipmentListforOrderOutput.removeChild(shipmentEle); // Removes shipments which are in Shipped or cancelled status
                }
            }

            logger.verbose("Filtered in-place open shipments only: " + SCXmlUtil.getString(getshipmentListOrderOutput));
            return getshipmentListOrderOutput;
        }


    }

    /**
     * This function is used to add cancellation in the input if we don't add other lines with Quantity as 0 confirmShipment API will ship those lines
     * As per our scenario if any of the line is getting short-pick remaining lines will also be getting cancelled
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
        logger.verbose("End of method addShipmentLineForCancellation with updated inDoc:: " + SCXmlUtil.getString(inputDocumentInitial));
    }

    /**
     * This method will update the tracking url at the order line for the lines which are being shipped
     *
     * @param env
     * @param orderNo
     * @param orderReleaseListOutDoc
     * @param inputMsgCopyEle
     * @param inputMsgConfirmShip
     * @throws Exception
     */
    private void updateTrackingNo(YFSEnvironment env, String orderNo, Document orderReleaseListOutDoc,
                                  Element inputMsgCopyEle, Document inputMsgConfirmShip,String orderType) throws Exception {
        logger.info("CrocsShipmentPackedToShippedStatusFromWMS: updateTrackingNo: start: "+ orderNo);
        logger.info("CrocsShipmentPackedToShippedStatusFromWMS: updateTrackingNo: orderReleaseListOutDoc: "+ XMLUtil.getXMLString(orderReleaseListOutDoc));
        logger.info("CrocsShipmentPackedToShippedStatusFromWMS: updateTrackingNo: inputMsgCopyEle: "+ XMLUtil.getElementXMLString(inputMsgCopyEle));
        logger.info("CrocsShipmentPackedToShippedStatusFromWMS: updateTrackingNo: inputMsgConfirmShip: "+ XMLUtil.getXMLString(inputMsgConfirmShip));
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
  
        if (ORDER_TYPE_RETAIL.equals(orderType)){
        	orderEle.setAttribute(A_DOCUMENT_TYPE, A_TO_DOCUMENT_TYPE);     	
       }else {           
    	   orderEle.setAttribute(A_DOCUMENT_TYPE, VAL_DOCUMENT_TYPE_SALES_ORDER);
       }
        // EOMS-5514 changes start
        orderEle.setAttribute(A_OVERRIDE, FLAG_Y);
        // EOMS-5514 changes end
        
        Element orderLineListEle = SCXmlUtil.createChild(orderEle, E_ORDER_LINES);
        Element shipmentLines = SCXmlUtil.getChildElement(inputMsgCopyEle, E_SHIPMENT_LINES);
        List<Element> listOfShipment = SCXmlUtil.getChildrenList(shipmentLines);

        OffsetDateTime currentDate = OffsetDateTime.now(ZoneOffset.UTC);
        //String systemDate = currentDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	//Start : EOMS-4238 : Order look up : Shipdate Bug
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	String systemDate = currentDate.format(formatter);
	//End : EOMS-4238 : Order look up : Shipdate Bug
	
		
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
            logger.info("CrocsShipmentPackedToShippedStatusFromWMS: updateTrackingNo: input to changeOrder: " + XMLUtil.getXMLString(changeOrderInDoc));
            Document changeOrderOutDoc = CommonUtil.invokeAPI(env, "", API_CHANGE_ORDER, changeOrderInDoc);

            logger.info("output from changeOrder: " + XMLUtil.getXMLString(changeOrderOutDoc));
        } catch (Exception e) {
            logger.error("Exception in changeOrder method: " + e.getMessage(), e);
            throw e; // Rethrow exception to propagate to caller
        }
        logger.verbose("End of method updateTrackingNo: ");
    }
    
    /**
     * This function is used to remove the short-picked line from the input for WNSCode = 1 & 
     * an input will be formed for changeShipment API to cancel that short-picked lines
     * @param env
     * @param inDoc
     * @return
     */
    private Document updateInputForCancellation(YFSEnvironment env, Document inDoc) {
        logger.verbose("Start of method updateInputForCancellation with input: " + SCXmlUtil.getString(inDoc));
		Element inDocEle = inDoc.getDocumentElement();
		//creating input for changeShipment
		Document changeShipmentInDoc = SCXmlUtil.createDocument(E_SHIPMENT);
		Element changeShimpmentInDocEle = changeShipmentInDoc.getDocumentElement();
		changeShimpmentInDocEle.setAttribute(A_OVERRIDE_MODIFICATION_RULES, FLAG_Y);
		changeShimpmentInDocEle.setAttribute(A_CANCEL_REMOVED_QUANTITY, FLAG_Y);
		changeShimpmentInDocEle.setAttribute(A_SHIP_NODE, inDocEle.getAttribute(A_SHIP_NODE));
		changeShimpmentInDocEle.setAttribute(A_SHIPMENT_NO, inDocEle.getAttribute(A_SHIPMENT_NO));
		changeShimpmentInDocEle.setAttribute(A_SELLER_ORGANIZATION_CODE, inDocEle.getAttribute(A_SELLER_ORGANIZATION_CODE));
		
		List<Element> inDocShipmenLineList = SCXmlUtil.getChildrenList(SCXmlUtil.getChildElement(inDocEle, E_SHIPMENT_LINES));
		Element shipmentLines = SCXmlUtil.createChild(changeShimpmentInDocEle, E_SHIPMENT_LINES);
	     
		inDocEle.removeChild(SCXmlUtil.getChildElement(inDocEle, E_SHIPMENT_LINES));
	    Element inDocShipmentLines = SCXmlUtil.createChild(inDocEle, E_SHIPMENT_LINES);
		
		for(Element eachShipmentLine : inDocShipmenLineList) {
		    // if a line has zero quantity keep that line in the changeShipment input 
			if(eachShipmentLine.getAttribute(A_QUANTITY).equals(VAL_ZERO)) {
				SCXmlUtil.importElement(shipmentLines, eachShipmentLine);
			}else {
			 // keep that line in the inDoc  
				SCXmlUtil.importElement(inDocShipmentLines, eachShipmentLine);
			}
		}
		
		Document changeShipmentOutDoc = null;
		if(shipmentLines.hasChildNodes()) {
			changeShipmentOutDoc = invokeChangeShipment(env,changeShipmentInDoc);
		}
        logger.verbose("End of method updateInputForCancellation with changeShipmentOutDoc: " + SCXmlUtil.getString(changeShipmentOutDoc));
        logger.verbose("End of method updateInputForCancellation with updated inDoc: " + SCXmlUtil.getString(inDoc));
		return changeShipmentOutDoc;		
	}

    /**
     * This function is used to make an api call to changeShipment
     * @param env
     * @param inputDoc
     * @return
     * @throws YFSException
     */
	private Document invokeChangeShipment(YFSEnvironment env, Document changeShipmentInDoc) {
        logger.verbose("Start of method invokeChangeShipment with input: " + SCXmlUtil.getString(changeShipmentInDoc));
        try {
        	
            Document outDoc = CommonUtil.invokeAPI(env, "", API_CHANGE_SHIPMENT, changeShipmentInDoc);
            logger.verbose("End of method invokeChangeShipment with output: " + SCXmlUtil.getString(outDoc));
            return outDoc;
            
        } catch (Exception e) {
            logger.error("Error invoking changeShipment API: " + e.getMessage());
            throw new YFSException("Error invoking changeShipment API: " + e.getMessage());
        }
	}
	
	/**
     * Prepares a document for creating an asynchronous request with the provided input element.
     * @param inDocEle     The input XML element to be wrapped in the async request document.
     * @return             A Document representing the async request api input.
     */
	private Document prepareCreateAsyncRequestInput(Element inDocEle) {
		logger.verbose("CrocsShipmentPackedToShippedStatusFromWMS: Start of method prepareCreateAsyncRequestInput with inDocEle: " + SCXmlUtil.getString(inDocEle));

		// Create root async request document
		Document asyncReqDoc = SCXmlUtil.createDocument(E_CREATE_ASYNC_REQUEST);
		Element asyncReqEle = asyncReqDoc.getDocumentElement();

		// Add API element
		Element apiElement = SCXmlUtil.createChild(asyncReqEle, E_API);
		apiElement.setAttribute(A_IS_SERVICE, FLAG_Y);
		apiElement.setAttribute(A_NAME, SERVICE_CROCS_CONFIRM_SHIPMENT_SYNC_SERV);

		// Imported inDocEle element to Input
		Element inputElement = SCXmlUtil.createChild(apiElement, E_INPUT);
		SCXmlUtil.importElement(inputElement, inDocEle);

		logger.verbose("CrocsShipmentPackedToShippedStatusFromWMS: End of method prepareCreateAsyncRequestInput with asyncReqDoc: " + SCXmlUtil.getString(asyncReqDoc));
		return asyncReqDoc;		
	}

    /**
     * This function is used to invoke createAsyncRequestAPI
     * @param env
     * @param createAsyncReqDoc The input received to invoked the createAsyncRequestAPI
     * @return	The response returned by createAsyncRequestAPI
     */
    private Document invokeCreateAsyncRequestAPI(YFSEnvironment env, Document createAsyncReqDoc) {
        logger.verbose("CrocsShipmentPackedToShippedStatusFromWMS: Start of method invokeCreateAsyncRequestAPI with input: " + SCXmlUtil.getString(createAsyncReqDoc));
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
    	logger.verbose("CrocsShipmentPackedToShippedStatusFromWMS: Start of method checkIfUpdateWithWMSCode1IsProcessed with shipmentListOutput: " + SCXmlUtil.getString(shipmentListOutput));
    	Element shipmentListEle = shipmentListOutput.getDocumentElement();
    	List<Element> listOfShipment = SCXmlUtil.getChildrenList(shipmentListEle);

    	for(Element eachShipment : listOfShipment) {
    		if(VAL_ONE.equals(eachShipment.getAttribute(A_CODE))) {
    	    	logger.verbose("CrocsShipmentPackedToShippedStatusFromWMS: Code = 1 found in shipment Returning true");
    	    	logger.verbose("CrocsShipmentPackedToShippedStatusFromWMS: End of method checkIfUpdateWithWMSCode1IsProcessed");
    	    	return true;
    		}
    	}
        logger.verbose("No shipment found with Code = 1. Returning false.");
    	logger.verbose("CrocsShipmentPackedToShippedStatusFromWMS: End of method checkIfUpdateWithWMSCode1IsProcessed:");
    	return false;
    }



}