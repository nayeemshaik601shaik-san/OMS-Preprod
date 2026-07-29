package com.crocs.oms.shipment.ca;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class is used to frame input for createShipment API
 * EOMS-4683  & EOMS -4685:Shipment Updates from UPS
 */
public class CrocsCACreateShipInputForShipmentUpdatesFromUPS implements CrocsConstant {
    private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsCACreateShipInputForShipmentUpdatesFromUPS.class);

    /** 
     * This method process the shipment msg from UPS
     * and frame input for getOrderReleaseList API to get details to create input for createShipment API
     *
     * @param env   env
     * @param indoc indoc
     * @return createShipmentIndoc
     */
    public Document processShipmentCreateUpdates(YFSEnvironment env, Document indoc) throws YFSException {
    	logger.verbose("CrocsCACreateShipInputForShipmentUpdatesFromUPS : processShipmentCreateUpdates : indoc: " + SCXmlUtil.getString(indoc));
    	Element shipment = indoc.getDocumentElement();
        String shipmentNo = "";
        String orderNo = "";
        String releaseNo = "";
        Document createShipmentIndoc = null;
        shipmentNo = shipment.getAttribute(A_SHIPMENT_NO);

        try {
            if (!YFCCommon.isVoid(shipmentNo)) {
                int shipIndex = shipmentNo.indexOf("SHIP");
                if (shipIndex != -1) {
                    // Extract the parts using substring
                    orderNo = shipmentNo.substring(0, shipIndex);  // Everything before "SHIP"
                    releaseNo = shipmentNo.substring(shipIndex + 4);  // Everything after "SHIP"
                    logger.verbose("CrocsCACreateShipInputForShipmentUpdatesFromUPS.CrocsCACreateShipInputForShipmentUpdatesFromUPS() : OrderNo: " + orderNo);
                    logger.verbose("CrocsCACreateShipInputForShipmentUpdatesFromUPS.CrocsCACreateShipInputForShipmentUpdatesFromUPS() : ReleaseNo" + releaseNo);
                    if (!YFCCommon.isVoid(orderNo) && !YFCCommon.isVoid(releaseNo)) {
                        Document getOrderReleaseListIndoc = SCXmlUtil.createDocument(E_ORDER_RELEASE);
                        Element getOrderReleaseDetailsEle = getOrderReleaseListIndoc.getDocumentElement();
                        getOrderReleaseDetailsEle.setAttribute(A_RELEASE_NO, releaseNo);

                        Element orderEle = SCXmlUtil.createChild(getOrderReleaseDetailsEle, E_ORDER);
                        orderEle.setAttribute(A_ORDER_NO, orderNo);
                        logger.verbose("CrocsCACreateShipInputForShipmentUpdatesFromUPS.CrocsCACreateShipInputForShipmentUpdatesFromUPS() :releaseListInput: " + SCXmlUtil.getString(getOrderReleaseListIndoc));

                        //calling getOrderReleaseDetails API
                        Document orderReleaseDetailsDoc = CommonUtil.invokeAPI(env, TEMPLATE_GET_ORDER_RELEASE_LIST,
                                API_GET_ORDER_RELEASE_LIST, getOrderReleaseListIndoc);
                        logger.verbose("CrocsCACreateShipInputForShipmentUpdatesFromUPS.CrocsCACreateShipInputForShipmentUpdatesFromUPS() :releaseListoutput" + SCXmlUtil.getString(orderReleaseDetailsDoc));

                        Element releaseStatusEle = SCXmlUtil.getXpathElement(orderReleaseDetailsDoc.getDocumentElement(),
                        		XPATH_ORDER_RELEASE_STATUS);
                        
                        //EOMS-5703: START 
                        //If the shipment already exists, we are skipping its creation.
                        if(!YFCCommon.isVoid(releaseStatusEle))
                        	createShipmentIndoc = createShipmentInput(releaseNo, orderReleaseDetailsDoc);
                        else
                        {	
							logger.info(
									"CrocsCACreateShipInputForShipmentUpdatesFromUPS.CrocsCACreateShipInputForShipmentUpdatesFromUPS() Skipping the CreateShipment,Non of the line in Status[3200].\n Input: "
											+ indoc +"\n ReleaseListoutput: "+SCXmlUtil.getString(orderReleaseDetailsDoc));
							//EOMS-7581:: Duplicate Update Fix:: Changes Start
							shipment.setAttribute(A_IS_DUPLICATE_SHIPMENT_SKIPPED, FLAG_Y);
							return indoc;
							//EOMS-7581:: Changes End
                        }
                      //EOMS-5703: END

                        
                        logger.info("CrocsCACreateShipInputForShipmentUpdatesFromUPS.CrocsCACreateShipInputForShipmentUpdatesFromUPS() : creating shipment for OrderNo: "+orderNo);

                        	

                    } else {
                        String errorDesc = "orderNo & releaseNo is not provided:" + " " + " expected: [orderNo & releaseNo]";
                        logger.verbose("orderNo or releaseNo. is null");
                        throw new YFSException("orderNo and releaseNo is null","",errorDesc);
                    }
                } else {
                    String errorDesc = "Invalid shipmentNo provided:" + " " + " expected: [ValidShipmentNo]";
                    logger.error("Invalid shipmentNo ");
                    throw new YFSException("Invalid shipmentNo ", "",errorDesc);
                }
            } else {
                String errorDesc = "Empty shipmentNo provided:" + " " + " expected: [shipmentNo]";
                logger.error(" Error : shipmentNo is empty");
                throw new YFSException(" shipmentNo is empty", "", errorDesc);
            }
        } catch (Exception e) {
            logger.verbose("Error in getOrderReleaseDetails API call in method createShipmentInput: "
                    + e.getMessage());
            throw new YFSException(" Error in getOrderReleaseDetails API call in method createShipmentInput :" + e.getMessage());
        }
        return createShipmentIndoc;
    }


    /**
     * This method is used to create input for createShipment API
     *
     * @param releaseNo              releaseNo
     * @param orderReleaseDetailsDoc orderReleaseDetailsDoc
     * @return createShipmentInput
     */
    public Document createShipmentInput(String releaseNo, Document orderReleaseDetailsDoc) {
        //processing getOrderReleaseList
        Element eleReleaseList = orderReleaseDetailsDoc.getDocumentElement();
        Element eleRelease = SCXmlUtil.getChildElement(eleReleaseList, E_ORDER_RELEASE);
        Element eleOrder = SCXmlUtil.getChildElement(eleRelease, E_ORDER);
        //framing createShipment Input
        Document createShipmentIndoc = SCXmlUtil.createDocument(E_SHIPMENT);
        Element getCreateShipmentEle = createShipmentIndoc.getDocumentElement();
        getCreateShipmentEle.setAttribute(A_CARRIER_SERVICE_CODE, eleRelease.getAttribute(A_CARRIER_SERVICE_CODE));
        getCreateShipmentEle.setAttribute(A_ENTERPRISE_CODE, eleRelease.getAttribute(A_ENTERPRISE_CODE));
        getCreateShipmentEle.setAttribute(A_SHIP_NODE, eleRelease.getAttribute(A_SHIP_NODE));
        getCreateShipmentEle.setAttribute(A_DOCUMENT_TYPE, eleRelease.getAttribute(A_DOCUMENT_TYPE));
        Element shipmentLinesEle = SCXmlUtil.createChild(getCreateShipmentEle, E_SHIPMENT_LINES);
        ArrayList<Element> eleOrderLines = SCXmlUtil.getChildren(eleRelease, E_ORDER_LINE);
        //iterating each orderLine
        for (Element orderLine : eleOrderLines) {
            Element eleItem = SCXmlUtil.getChildElement(orderLine, E_ITEM);
            
            Element orderStatusesEle = SCXmlUtil.getChildElement(orderLine, E_ORDER_STATUSES);
            List<Element> listOforderStatus = SCXmlUtil.getChildrenList(orderStatusesEle);
            
            // if a line is cancelled do not include it for shipment
            if(listOforderStatus.size() == 1 && listOforderStatus.get(0).getAttribute(A_STATUS).equals(STR_STATUS_CANCELLED))
            	continue;
            
            Element shipmentLineEle = SCXmlUtil.createChild(shipmentLinesEle, E_SHIPMENT_LINE);
            // take the quantities which are in released status only
            for(Element eachStatus : listOforderStatus) {
            	if(eachStatus.getAttribute(A_STATUS).equals(STATUS_RELEASE)) {
                    shipmentLineEle.setAttribute(A_QUANTITY, eachStatus.getAttribute(A_STAT_QTY));
            	}
            }
            
            shipmentLineEle.setAttribute(A_ITEM_ID, eleItem.getAttribute(A_ITEM_ID));
            shipmentLineEle.setAttribute(A_ORDER_NO, eleOrder.getAttribute(A_ORDER_NO));
            shipmentLineEle.setAttribute(A_PRIME_LINE_NO, orderLine.getAttribute(A_PRIME_LINE_NO));
            shipmentLineEle.setAttribute(A_RELEASE_NO, releaseNo);
            shipmentLineEle.setAttribute(A_SHIPMENT_LINE_NO, orderLine.getAttribute(A_PRIME_LINE_NO));
            shipmentLineEle.setAttribute(A_SUB_LINE_NO, orderLine.getAttribute(A_SUB_LINE_NO));
            shipmentLineEle.setAttribute(A_UNIT_OF_MEASURE, eleItem.getAttribute(A_UNIT_OF_MEASURE));
        }
        logger.verbose("CrocsCACreateShipInputForShipmentUpdatesFromUPS : createShipmentinput : " + SCXmlUtil.getString(createShipmentIndoc));
        return createShipmentIndoc;
    }

    /**
     * This method process the shipment msg from UPS
     * and frame input for getShipmentList API to get details to change shipment status to packed using changeShipmentStatus API
     *
     * @param env   env
     * @param indoc indoc
     * @return changeShipmentStatusDoc
     */
    public Document processShipmentPackedUpdates(YFSEnvironment env, Document indoc) {
    	logger.verbose("CrocsCACreateShipInputForShipmentUpdatesFromUPS : processShipmentPackedUpdates : indoc: " + SCXmlUtil.getString(indoc));
    	Element shipment = indoc.getDocumentElement();
        String shipmentNo = "";
        String orderNo = "";
        String releaseNo = "";
        shipmentNo = shipment.getAttribute(A_SHIPMENT_NO);
        Document changeShipmentStatusDoc = null;
        try {
            if (!YFCCommon.isVoid(shipmentNo)) {
                int shipIndex = shipmentNo.indexOf("SHIP");
                if (shipIndex != -1) {
                    // Extract the parts using substring
                    orderNo = shipmentNo.substring(0, shipIndex);  // Everything before "SHIP"
                    releaseNo = shipmentNo.substring(shipIndex + 4);  // Everything after "SHIP"
                    logger.verbose("OrderNo: " + orderNo);
                    logger.verbose("ReleaseNo: " + releaseNo);
                    if (!YFCCommon.isVoid(orderNo) && !YFCCommon.isVoid(releaseNo)) {
                        //calling getShipmentList API
                        Document getShipmentListIndoc = SCXmlUtil.createDocument(E_SHIPMENT);
                        Element changeShipmentEle = getShipmentListIndoc.getDocumentElement();
                        Element shipmentLinesEle = SCXmlUtil.createChild(changeShipmentEle, E_SHIPMENT_LINES);
                        Element shipmentLineEle = SCXmlUtil.createChild(shipmentLinesEle, E_SHIPMENT_LINE);
                        shipmentLineEle.setAttribute(A_ORDER_NO, orderNo);
                        shipmentLineEle.setAttribute(A_RELEASE_NO, releaseNo);
                        logger.verbose("getShipmentListInput" + SCXmlUtil.getString(getShipmentListIndoc));
                        Document getShipmentListDoc = CommonUtil.invokeAPI(env, TEMPLATE_GET_SHIPMENT_LIST,
                                API_GET_SHIPMENT_LIST, getShipmentListIndoc);
                        logger.verbose("getShipmentList output" + getShipmentListDoc);

                        changeShipmentStatusDoc = createChangeShipmentStatusInput(getShipmentListDoc);
                    } else {
                        String errorDesc = "orderNo & releaseNo is not provided:" + " " + " expected: [orderNo & releaseNo]";
                        logger.verbose("orderNo or releaseNo. is null");
                        throw new YFSException("orderNo and releaseNo is null","",errorDesc);
                    }
                } else {
                    String errorDesc = "Invalid shipmentNo provided:" + " " + " expected: [ValidShipmentNo]";
                    logger.error("Invalid shipmentNo");
                    throw new YFSException("Invalid shipmentNo", "",errorDesc);
                }
            } else {
                String errorDesc = "Empty shipmentNo provided:" + " " + " expected: [shipmentNo]";
                logger.error(" Error : shipmentNo is empty");
                throw new YFSException(" shipmentNo is empty", "", errorDesc);
            }
        } catch (Exception e) {
            logger.verbose("Error in getShipmentList API call in method changeShipmentStatus: "
                    + e.getLocalizedMessage());
            throw new YFSException(" Error in changeShipmentStatus API call in method processShipmentPackedUpdates :" + e.getMessage());
        }
        return changeShipmentStatusDoc;
    }

    /**
     * This method forms the ChangeShipmentStatus input to move Shipment statsu to packed
     *
     * @param getShipmentListDoc getShipmentListDoc
     * @return changeShipmentStatusInDoc
     */
    private Document createChangeShipmentStatusInput(Document getShipmentListDoc) throws Exception {
        //processing getShipmentList
        Element shipmentListDoc = getShipmentListDoc.getDocumentElement();
        Element shipmentEle = SCXmlUtil.getChildElement(shipmentListDoc, E_SHIPMENT);
        String sellerOrganizationCode = shipmentEle.getAttribute(A_SELLER_ORGANIZATION_CODE);
        //framing changeShipmentStatus input
        String documentType = shipmentEle.getAttribute(A_DOCUMENT_TYPE);
        Document changeShipmentStatusInDoc = SCXmlUtil.createDocument(E_SHIPMENT);
        Element changeShipmentEle = changeShipmentStatusInDoc.getDocumentElement();
        changeShipmentEle.setAttribute(A_BASE_DROP_STATUS, BASE_DROP_STATUS);
        changeShipmentEle.setAttribute(A_SELLER_ORGANIZATION_CODE, sellerOrganizationCode);
        changeShipmentEle.setAttribute(A_SHIP_NODE, shipmentEle.getAttribute(A_SHIP_NODE));
        changeShipmentEle.setAttribute(A_SHIPMENT_NO, shipmentEle.getAttribute(A_SHIPMENT_NO));
        if (Objects.equals(sellerOrganizationCode, CROCS_US)) {
            changeShipmentEle.setAttribute(A_TRANSACTION_ID, TRANSACTION_ID_US);
        } else {
            changeShipmentEle.setAttribute(A_TRANSACTION_ID, TRANSACTION_ID_CA);
        }
        if(A_TO_DOCUMENT_TYPE.equalsIgnoreCase(documentType)) {
      	  if (Objects.equals(sellerOrganizationCode, CROCS_US)) {
                changeShipmentEle.setAttribute(A_TRANSACTION_ID, TO_TRANSACTION_ID_US);
            } else {
                changeShipmentEle.setAttribute(A_TRANSACTION_ID, TO_TRANSACTION_ID_CA);
            }
      	
      }
        logger.info("For Shipment No: "+shipmentEle.getAttribute(A_SHIPMENT_NO)+" moving shipment status to packed staus");
        logger.verbose("CrocsCACreateShipInputForShipmentUpdatesFromUPS : changeShipmentStatusInput: " + SCXmlUtil.getString(changeShipmentStatusInDoc));
        return changeShipmentStatusInDoc;
    }
}