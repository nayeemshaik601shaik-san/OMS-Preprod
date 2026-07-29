package com.crocs.oms.mp;

import com.crocs.oms.common.util.CommonUtil;
import com.yantra.yfc.util.YFCCommon;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CrocsConstant;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CrocsSendMPOrderUpdatesToECW implements CrocsConstant {

    private static YFCLogCategory logger = YFCLogCategory.instance(CrocsSendMPOrderUpdatesToECW.class);

    /**
     * EOMS-5546,EOMS-5547 and EOMS-5779  This method does the below
     * 1. checks if there are any cancelled qty or lines during shipment or before release
     * 2. get the details of cancelled lines from the shipment msg and send it to ECOM Middleware
     *
     * @param env
     * @param indoc
     * @return
     */

    public Document prepareShortShipMsg(YFSEnvironment env, Document indoc) throws Exception {

        Document getOrderListOutDoc = null;
        Document getShipmentListForOrderDoc = null;
        Document finalDocument = null;
        String carrierShortDesc = "";
        String shipViaCode = "";

        try {
            logger.verbose("CrocsSendMPOrderUpdatesToECW : prepareShortShipMsg : Start : " + SCXmlUtil.getString(indoc));

            Element shipmentEle = indoc.getDocumentElement();
            String orderHdrKey = SCXmlUtil.getXpathAttribute(shipmentEle, XPATH_ORDER_HDR_KEY);
			String enterpriseCode = shipmentEle.getAttribute(A_ENTERPRISE_CODE);

            // Prepare input for getOrderList
            Document getOrderListInput = SCXmlUtil.createDocument(E_ORDER);
            Element orderElement = getOrderListInput.getDocumentElement();
            orderElement.setAttribute(A_ORDER_HEADER_KEY, orderHdrKey);

            logger.verbose("CrocsSendMPOrderUpdatesToECW : prepareShortShipMsg : getOrderListInput is: " + SCXmlUtil.getString(getOrderListInput));

            // Call getOrderList
            getOrderListOutDoc = CommonUtil.invokeService(env, SERVICE_CROCS_GET_ORDER_LIST_FOR_MP, getOrderListInput);

            logger.verbose("CrocsSendMPOrderUpdatesToECW : prepareShortShipMsg : getOrderListOutDoc is: " + SCXmlUtil.getString(getOrderListOutDoc));

            Element orderEle = SCXmlUtil.getChildElement(getOrderListOutDoc.getDocumentElement(), E_ORDER);

            // Call getShipmentListForOrder with same input
            getShipmentListForOrderDoc = CommonUtil.invokeService(env, SERVICE_CROCS_GET_SHIPMNT_LIST_FOR_MP, getOrderListInput);

            logger.verbose("CrocsSendMPOrderUpdatesToECW : prepareShortShipMsg : getShipmentListForOrderDoc is: " + SCXmlUtil.getString(getShipmentListForOrderDoc));

            Element shipmentListEle = getShipmentListForOrderDoc.getDocumentElement();
            NodeList shipmentListElement = SCXmlUtil.getXpathNodes(shipmentListEle, E_SHIPMENT);

            //EOMS-6010 changes start
            if (shipmentListElement.getLength() > 0) {
                for (int i = 0; i < shipmentListElement.getLength(); ++i) {
                    Element shipEle = (Element) shipmentListElement.item(i);

                    Element status = SCXmlUtil.getChildElement(shipEle, E_STATUS);
                    String statusCodeStr = status.getAttribute(A_STATUS);

                    // Extract ShipViaCode
                    Element containersEle = SCXmlUtil.getChildElement(shipEle, E_CONTAINERS);

                    if (containersEle.hasChildNodes()) {
                        Element containerEle = SCXmlUtil.getChildElement(containersEle, E_CONTAINER);
                        shipViaCode = containerEle.getAttribute(A_EXTERNAL_REFERENCE_1);
                    }
					
					logger.verbose("shipViaCode is:" + shipViaCode);

                    if (!YFCCommon.isVoid(shipViaCode)) {
                        try {
                            Document getCommonCodeListInDoc = SCXmlUtil.createDocument(E_COMMON_CODE);
                            getCommonCodeListInDoc.getDocumentElement().setAttribute(A_CODE_TYPE, VAL_CROCS_SHIP_VIA_CODE);
                            getCommonCodeListInDoc.getDocumentElement().setAttribute(A_CODE_VALUE, shipViaCode);
							getCommonCodeListInDoc.getDocumentElement().setAttribute(A_ENTERPRISE_CODE, enterpriseCode);

                            logger.verbose("CrocsSendMPOrderUpdatesToECW : prepareShortShipMsg : getCommonCodeListInDoc:" + SCXmlUtil.getString(getCommonCodeListInDoc));

                            Document getCommonCodeListOut = CommonUtil.invokeAPI(env, "", API_GET_COMMON_CODE_LIST, getCommonCodeListInDoc);

                            logger.verbose("CrocsSendMPOrderUpdatesToECW : prepareShortShipMsg : getCommonCodeListOutput:" + SCXmlUtil.getString(getCommonCodeListOut));
                            if (getCommonCodeListOut != null) {
                                carrierShortDesc = SCXmlUtil.getXpathAttribute(getCommonCodeListOut.getDocumentElement(), XPATH_CODE_SHORT_DESC);
                            }
                            logger.verbose("CrocsSendMPOrderUpdatesToECW : prepareShortShipMsg : carrierShortDesc is:" + carrierShortDesc);
                        } catch (Exception e) {
                            logger.verbose("Error in getCommonCodeList API call in method prepareShortShipMsg: " + e.getLocalizedMessage());
                        }
                    }

                    double statusCode = 0.00;
                    try {
                        statusCode = Double.parseDouble(statusCodeStr);
                    } catch (NumberFormatException e) {
                        logger.error("Invalid status code: " + statusCodeStr, e);
                        continue;
                    }

                    // Remove shipment if statusCode < 1400
                    if (statusCode < 1400) {
                        Node parent = shipEle.getParentNode();
                        if (parent != null) {
                            parent.removeChild(shipEle);
                            logger.verbose("Removed shipment with statusCode < 1400: " + statusCodeStr);
                        }
                    } else {
                        shipEle.setAttribute(A_CARRIER_LONG_DESC, carrierShortDesc);
                    }
                }
            }
            //EOMS-6010 changes end

            if (!YFCCommon.isVoid(shipmentListEle) && !YFCCommon.isVoid(orderEle)) {
                finalDocument = prepareMessageForMW(orderEle, shipmentListEle);
            }

        } catch (Exception ex) {
            logger.error("Exception in method prepareShortShipMsg: ", ex);
            throw ex;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("CrocsSendMPOrderUpdatesToECW : prepareShortShipMsg : End : " + SCXmlUtil.getString(finalDocument));
        }

        return finalDocument;
    }

      public Document prepareMessageForMW(Element orderElement, Element shipmentDoc) {

        logger.verbose("CrocsSendMPOrderUpdatesToECW : prepareMessageForMW : order Element is: " + SCXmlUtil.getString(orderElement));
        logger.verbose("CrocsSendMPOrderUpdatesToECW : prepareMessageForMW : shipmentDoc is: " + SCXmlUtil.getString(shipmentDoc));

        // Import ShipmentList into the existing Order element
        SCXmlUtil.importElement(orderElement, shipmentDoc);
        Document finalDoc = SCXmlUtil.createDocument(E_ORDER);
        Element root = finalDoc.getDocumentElement();
        finalDoc.removeChild(root);

        // Import the actual orderElement as root into finalDoc
        Node importedOrder = finalDoc.importNode(orderElement, true);
        finalDoc.appendChild(importedOrder);

        logger.verbose("CrocsSendMPOrderUpdatesToECW : prepareMessageForMW : Final merged document is: " + SCXmlUtil.getString(finalDoc));
        return finalDoc;

    }

    public Document prepareCancellationMsg(YFSEnvironment env, Document indoc) throws Exception {

        Document outDoc = null;
        Document finalDoc = null;

        try {
            logger.verbose("CrocsSendMPOrderUpdatesToECW : prepareCancellationMsg : Input to prepareCancellationMsg: " + SCXmlUtil.getString(indoc));

            Element orderEle = indoc.getDocumentElement();

            String orderHdrKey = orderEle.getAttribute(A_ORDER_HEADER_KEY);

            // Prepare input for getOrderList
            Document getOrderListInput = SCXmlUtil.createDocument(E_ORDER);
            Element orderElement = getOrderListInput.getDocumentElement();
            orderElement.setAttribute(A_ORDER_HEADER_KEY, orderHdrKey);

            logger.verbose("CrocsSendMPOrderUpdatesToECW : prepareCancellationMsg : getOrderListInput is: " + SCXmlUtil.getString(getOrderListInput));

            // Calling getOrderList
            outDoc = CommonUtil.invokeService(env, SERVICE_CROCS_GET_ORDER_LIST_FOR_MP,getOrderListInput);

            logger.verbose("CrocsSendMPOrderUpdatesToECW : prepareCancellationMsg : getOrderListOutDoc is: " + SCXmlUtil.getString(outDoc));

            Element cancelOrderEle = SCXmlUtil.getChildElement(outDoc.getDocumentElement(), E_ORDER);
            finalDoc = SCXmlUtil.createDocument(E_ORDER);
            Element root = finalDoc.getDocumentElement();
            finalDoc.removeChild(root);

            Node importedOrder = finalDoc.importNode(cancelOrderEle, true);
            finalDoc.appendChild(importedOrder);

            logger.verbose("CrocsSendMPOrderUpdatesToECW : prepareCancellationMsg : Final merged document is: " + SCXmlUtil.getString(finalDoc));

        }catch (Exception ex) {
            logger.error("Exception in method prepareCancellationMsg: ", ex);
            throw ex;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("CrocsSendMPOrderUpdatesToECW : prepareCancellationMsg : End of method prepareCancellationMsg():: " + SCXmlUtil.getString(finalDoc));
        }

        return finalDoc;
    }

}
