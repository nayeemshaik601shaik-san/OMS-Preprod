package com.crocs.oms.order.forter;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsAPIConstants;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCDoubleUtils;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class CrocsUCRStatusUpdateToForter implements CrocsConstant {

    private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsUCRStatusUpdateToForter.class);

    double capturedAmount = 0.0;
    double cumulativeRefundAmount = 0.0;
    double capturedReturnAmount = 0.0;
    double appeasedAmountForReturn = 0.0;
    double grantedReturnAmount = 0.0;

    public enum CompensationType {
        RESHIP, RETURN, APPEASEMENT
    }

    /**
     * Processes the UCR status update and publishes the update to Forter
     * based on the determined compensation type.
     * @param env
     * @param inDoc
     * @throws Exception
     */
    public void processUCRStatusUpdateToForter(YFSEnvironment env, Document inDoc) {

        logger.beginTimer("CrocsUCRStatusUpdateToForter : processCompensationUpdateToForter");
        logger.info("CrocsUCRStatusUpdateToForter : processCompensationUpdateToForter" + inDoc.toString());
        Element rootEle = inDoc.getDocumentElement();

        CompensationType compType = determineCompensationType(rootEle);
        publishUCRStatusUpdateToForter(env, inDoc, compType);
        logger.endTimer("CrocsUCRStatusUpdateToForter : processCompensationUpdateToForter");
    }

    /**
     * @param rootEle
     * @return
     */
    private CompensationType determineCompensationType(Element rootEle) {

        logger.beginTimer("CrocsUCRStatusUpdateToForter : determineCompensationType");

        String rootName = rootEle.getNodeName();

        if ("OrderInvoice".equalsIgnoreCase(rootName)) {
            String invoiceType = SCXmlUtil.getXpathAttribute(rootEle, "@InvoiceType");
            if("RETURN".equalsIgnoreCase(invoiceType)) {
                return CompensationType.RETURN;
            }
            return CompensationType.APPEASEMENT;
        }

        if ("OrderRelease".equalsIgnoreCase(rootName)) {
            return CompensationType.RESHIP;
        }

        if ("Order".equalsIgnoreCase(rootName)) {
            if (SCXmlUtil.getXpathElement(rootEle, "//OrderLine[@ReturnReason!='']") != null) {
                return CompensationType.RETURN;
            } else if (SCXmlUtil.getXpathElement(rootEle, "@Status ='Cancelled'") != null) {
                return CompensationType.APPEASEMENT;
            }
        }

        logger.endTimer("CrocsUCRStatusUpdateToForter : determineCompensationType");
        throw new IllegalStateException("Unable to determine CompensationType");
    }

    private void publishUCRStatusUpdateToForter(YFSEnvironment env, Document inDoc, CompensationType compType) {

        logger.beginTimer("CrocsUCRStatusUpdateToForter : publishUCRStatusUpdateToForter");
        try {
            switch (compType) {
                case APPEASEMENT:
                    prepareForterAppeasementStatusUpdateInput(env, compType, inDoc);
                    break;

                case RESHIP:

                    break;

                case RETURN:
                    prepareForterReturnStatusUpdateInput(env, compType, inDoc);
                    break;

                default:
                    logger.info("compensation type didn't match: " + compType);

            }
        } catch (Exception e) {
            logger.error("Failed to send status Update for UCR", e);
            throw new YFSException("CrocsUCRStatusUpdateToForter", "ORDER_STATUS_FAILURE",
                    "Publish order Status failed");
        }

        logger.endTimer("CrocsUCRStatusUpdateToForter : publishUCRStatusUpdateToForter");
    }

    private void prepareForterAppeasementStatusUpdateInput(YFSEnvironment env, CompensationType compType, Document inDoc) throws Exception {
        logger.info("prepareForterAppeasementStatusUpdateInput Input XML: " + inDoc);

        try {

            Element inputEle = inDoc.getDocumentElement();
            String orderNo = SCXmlUtil.getXpathAttribute(inputEle, "Order/@OrderNo");

            // preparing input for GetOrderInvoiceList
            if (!YFCCommon.isVoid(orderNo)) {
                Document getOrderInvoiceListInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER_INVOICE);
                getOrderInvoiceListInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_ORDER_NO, orderNo);
                getOrderInvoiceListInDoc.getDocumentElement().setAttribute("LatestFirst", "N");
                logger.info("Input for GetOrderInvoiceList in CrocsUCRStatusUpdateToForter.prepareForterAppeasementStatusUpdateInput :" + getOrderInvoiceListInDoc);

                Document getOrderInvoiceListOut = CommonUtil.invokeAPI(env,"",CrocsAPIConstants.API_GET_ORDER_INVOICE_LIST,getOrderInvoiceListInDoc);
                logger.info("Output of GetOrderInvoiceList in CrocsUCRStatusUpdateToForter.prepareForterAppeasementStatusUpdateInput :" + getOrderInvoiceListOut);

                Element orderInvoiceEle = getOrderInvoiceListOut.getDocumentElement();
                // At least one InvoiceHeader with InvoiceType = Credit Memo exists
                if (getOrderInvoiceListOut != null &&
                        SCXmlUtil.getXpathElement(orderInvoiceEle,
                                "OrderInvoice[@InvoiceType='CREDIT_MEMO']") != null) {

                    /*-------------To Calculate Granted amount------------------------*/
                    capturedAmount = Double.parseDouble(SCXmlUtil.getXpathAttribute(
                            orderInvoiceEle,
                            "sum(/OrderInvoiceList/OrderInvoice[@InvoiceType='SHIPMENT']/@TotalAmount)"));

                    NodeList invoiceList = SCXmlUtil.getXpathNodes(orderInvoiceEle, "OrderInvoice");
                    for (int i = 0; i < invoiceList.getLength(); i++) {
                        Element invoiceEle = (Element) invoiceList.item(i);
                        String invoiceType = invoiceEle.getAttribute(A_INVOICE_TYPE);
                        if (!YFCCommon.isVoid(invoiceType) && V_CREDIT_MEMO.equalsIgnoreCase(invoiceType)) {
                            processUCRStatusUpdates(env, compType, orderNo, invoiceEle);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error in orderInvoiceList API call in method CrocsUCRStatusUpdateToForter.prepareForterAppeasementStatusUpdateInput: "
                    + e.getLocalizedMessage());
        }
    }

    private void prepareForterReturnStatusUpdateInput(YFSEnvironment env, CompensationType compType, Document inDoc) throws Exception {
        logger.info("prepareForterReturnStatusUpdateInput Input XML: " + inDoc);

        try {

            Element inputEle = inDoc.getDocumentElement();
            JSONObject returnPayload = new JSONObject();
            String returnOrderNo = SCXmlUtil.getXpathAttribute(inputEle, "@OrderNo");
            String salesOrderNo = SCXmlUtil.getXpathAttribute(inputEle, "//OrderLines/OrderLine/DerivedFromOrder/@OrderNo");
            String compensationTypeGranted = "";
            String returnMethodGranted = "";
            String updatedStatus = "";

            // preparing input for GetOrderInvoiceList
            if (!YFCCommon.isVoid(salesOrderNo)) {
                Document getOrderInvoiceListInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER_INVOICE);
                getOrderInvoiceListInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_ORDER_NO, salesOrderNo);
                getOrderInvoiceListInDoc.getDocumentElement().setAttribute("LatestFirst", "N");
                logger.info("Input for GetOrderInvoiceList in CrocsUCRStatusUpdateToForter.prepareForterAppeasementStatusUpdateInput :" + getOrderInvoiceListInDoc);

                Document getOrderInvoiceListOut = CommonUtil.invokeAPI(env,"",CrocsAPIConstants.API_GET_ORDER_INVOICE_LIST,getOrderInvoiceListInDoc);
                logger.info("Output of GetOrderInvoiceList in CrocsUCRStatusUpdateToForter.prepareForterAppeasementStatusUpdateInput :" + getOrderInvoiceListOut);

                Element orderInvoiceEle = getOrderInvoiceListOut.getDocumentElement();
                // At least one InvoiceHeader with InvoiceType = Credit Memo exists
               /* if (getOrderInvoiceListOut != null &&
                        SCXmlUtil.getXpathElement(orderInvoiceEle,
                                "OrderInvoice[@InvoiceType='CREDIT_MEMO']") != null) {

                    /*-------------To Calculate Granted amount------------------------*/
                  /*  capturedAmount = Double.parseDouble(SCXmlUtil.getXpathAttribute(
                            orderInvoiceEle,
                            "sum(/OrderInvoiceList/OrderInvoice[@InvoiceType='SHIPMENT']/@TotalAmount)"));

                    NodeList invoiceList = SCXmlUtil.getXpathNodes(orderInvoiceEle, "OrderInvoice");
                    for (int i = 0; i < invoiceList.getLength(); i++) {
                        Element invoiceEle = (Element) invoiceList.item(i);
                        String invoiceType = invoiceEle.getAttribute(A_INVOICE_TYPE);
                        if (!YFCCommon.isVoid(invoiceType) && V_CREDIT_MEMO.equalsIgnoreCase(invoiceType)) {
                            processUCRStatusUpdates(env, compType, orderNo, invoiceEle);
                        }
                    }
                } */

                if (getOrderInvoiceListOut != null) {

                    /*-------------To Calculate Granted amount------------------------*/
                    capturedReturnAmount = Double.parseDouble(SCXmlUtil.getXpathAttribute(inputEle,"//PriceInfo/@TotalAmount"));

                   // NodeList invoiceList = SCXmlUtil.getXpathNodes(orderInvoiceEle, "OrderInvoice");
                   NodeList invoiceList = SCXmlUtil.getXpathNodes(orderInvoiceEle,
                            "//OrderInvoice[@InvoiceType='CREDIT_MEMO']");
                    for (int i = 0; i < invoiceList.getLength(); i++) {
                        Element invoiceEle = (Element) invoiceList.item(i);
                      //  String invoiceType = invoiceEle.getAttribute(A_INVOICE_TYPE);
                       // if (!YFCCommon.isVoid(invoiceType) && V_CREDIT_MEMO.equalsIgnoreCase(invoiceType)) {
                       //     processUCRStatusUpdates(env, compType, orderNo, invoiceEle);
                        // }
                        String totalAmountStr = invoiceEle.getAttribute("TotalAmount");

                        if (!YFCCommon.isVoid(totalAmountStr)) {
                            appeasedAmountForReturn += Math.abs(Double.parseDouble(totalAmountStr));
                        }
                    }
                }
                grantedReturnAmount = YFCDoubleUtils.roundOff((capturedReturnAmount - appeasedAmountForReturn), 2);
                updatedStatus = A_ACCEPTED_BY_MERCHANT;
                compensationTypeGranted = V_REFUND_UPON_RETURN;
                returnMethodGranted = V_SHIP_TO_WAREHOUSE;
                long eventTime = System.currentTimeMillis();
                //String currency = orderEle.getAttribute(A_CURRENCY);
                String currency = SCXmlUtil.getXpathAttribute(inputEle,"@Currency");
                JSONObject compReq = new JSONObject();

                JSONArray items = new JSONArray();
                NodeList ListOrderLines = SCXmlUtil.getXpathNodes(inputEle,"//OrderLines/OrderLine");
                for (int i = 0; i < ListOrderLines.getLength(); i++) {

                    Element listOrdLine = (Element) ListOrderLines.item(i);
                    String primeLineNo = listOrdLine.getAttribute("PrimeLineNo");

                    Element orderLineEle = SCXmlUtil.getXpathElement(inputEle,
                            "//OrderLines/OrderLine[@PrimeLineNo='" + primeLineNo + "']");

                    if(!YFCCommon.isVoid(listOrdLine)) {
                        JSONObject item = new JSONObject();
                        JSONObject basicItemData = new JSONObject();
                        JSONObject priceData = new JSONObject();
                        priceData.put(A_AMOUNT_LOCAL_CURRENCY, SCXmlUtil.getXpathAttribute(orderLineEle, "//LinePriceInfo/@LineTotal"));
                        priceData.put(A_CURRENCY_F, currency);
                        basicItemData.put(A_NAME_F, SCXmlUtil.getXpathAttribute(orderLineEle, "Item/@ItemDesc"));
                        basicItemData.put(A_PRICE_F, priceData);
                        basicItemData.put(A_PRODUCTID_F, SCXmlUtil.getXpathAttribute(orderLineEle, "Item/@ItemID"));
                        basicItemData.put(A_QUANTITY_F, Double.parseDouble(SCXmlUtil.getXpathAttribute(orderLineEle, "@OrderedQty")));
                        basicItemData.put(A_TYPE_F, TANGEABLE);

                        JSONObject itemStatusData = new JSONObject();
                        String reasonCategory = CrocsForterUtil.getCommonCodeDesc(env, V_FORTER_RETURN_REASONS,
                                SCXmlUtil.getXpathAttribute(orderLineEle, "@ReturnReason"));

                        itemStatusData.put(A_COMPENSATION_TYPE_GRANTED,compensationTypeGranted);
                        itemStatusData.put(A_REASON_CATEGORY_F,reasonCategory);
                        itemStatusData.put(A_RETURN_METHOD_GRANTED,returnMethodGranted);
                        itemStatusData.put(F_UPDATED_STATUS,updatedStatus);

                        item.put(BASICITEMDATA_F, basicItemData);
                        item.put(A_RETURN_STATUS_DATA, itemStatusData);

                        items.put(item);
                    }
                }

                compReq.put(A_ITEM_STATUS,items);
                JSONObject totalGrantedAmountObj = new JSONObject();
                totalGrantedAmountObj.put(A_AMOUNT_LOCAL_CURRENCY,Double.toString(grantedReturnAmount));
                totalGrantedAmountObj.put(A_CURRENCY_F,currency);
                compReq.put(A_TOTAL_GRANTED_AMOUNT,totalGrantedAmountObj);

                returnPayload.put(A_COMPENSATION_STATUS,compReq);
                returnPayload.put(A_EVENT_ID_F,returnOrderNo);
                returnPayload.put(F_EVENT_TIME,eventTime);
                returnPayload.put(A_ORDER_ID_F,returnOrderNo);
                returnPayload.put(F_UPDATED_STATUS,STR_RETURNED);

                JSONObject updatedTotalAmountObj = new JSONObject();
                updatedTotalAmountObj.put(A_AMOUNT_LOCAL_CURRENCY,Double.toString(grantedReturnAmount));
                updatedTotalAmountObj.put(A_CURRENCY_F,currency);
                returnPayload.put(A_UPDATED_TOTAL_AMOUNT,updatedTotalAmountObj);

                String strEnterpriseCode = SCXmlUtil.getXpathAttribute(inputEle, "@EnterpriseCode");
                String orderHdrKey = SCXmlUtil.getXpathAttribute(inputEle, "//Order/@OrderHeaderKey");

                logger.info("Forter Status Update Payload: " + returnPayload);

                CrocsForterUtil.invokeForter(env, returnOrderNo, orderHdrKey, strEnterpriseCode, returnPayload,"orderStatus");


            }
        } catch (Exception e) {
            logger.debug("Error in orderInvoiceList API call in method CrocsUCRStatusUpdateToForter.prepareForterAppeasementStatusUpdateInput: "
                    + e.getLocalizedMessage());
        }
    }

    /**
     * This method we prepare Order Status Update for each UCR request
     *
     * @param env
     * @param compType
     * @param orderNo
     * @param orderInvoiceEle
     * @throws RemoteException
     */
    public void processUCRStatusUpdates(YFSEnvironment env, CompensationType compType, String orderNo, Element orderInvoiceEle) throws Exception {
        String strOrderInvoiceKey = orderInvoiceEle.getAttribute(CrocsXmlConstants.A_ORDER_INVOICE_KEY);
        // preparing input for GetOrderInvoiceDetails
        Document getOrderInvoiceDetailsInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_GET_ORDER_INVOICE_DETAILS);
        getOrderInvoiceDetailsInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_INVOICE_KEY, strOrderInvoiceKey);

        logger.info(" Input for GetOrderInvoiceDetails in CrocsUCRStatusUpdateToForter : processUcrStatusUpdates : "
                + SCXmlUtil.getString(getOrderInvoiceDetailsInDoc));
        Document docGetOrderInvoiceOutput = CommonUtil.invokeService(env, CrocsConstant.STR_CROCS_GET_ORDER_INVOICE_DETAILS_SERV,
                getOrderInvoiceDetailsInDoc);
        logger.info("Output of GetOrderInvoiceDetails in CrocsUCRStatusUpdateToForter : processUcrStatusUpdates: "
                + SCXmlUtil.getString(docGetOrderInvoiceOutput));

        Element orderInvoiceDetailsEle = docGetOrderInvoiceOutput.getDocumentElement();
        Element orderEle = SCXmlUtil.getXpathElement(orderInvoiceDetailsEle, "/InvoiceDetail/InvoiceHeader/Order");
        String strEnterpriseCode = orderEle.getAttribute(CrocsConstant.A_ENTERPRISE_CODE);
        String orderHdrKey = orderEle.getAttribute(A_ORDER_HEADER_KEY);

        JSONObject appesementDetails = prepareForterUCRStatusAppeasementRequest(env, compType, orderInvoiceDetailsEle, orderEle);
        logger.verbose("Final UCR Status Update to Forter: CrocsUCRStatusUpdateToForter : prepareForterAppeasementStatusUpdateInput"
                + appesementDetails.toString());
        CrocsForterUtil.invokeForter(env, orderNo, orderHdrKey, strEnterpriseCode, appesementDetails, "orderStatus");
    }


    /**
     * @param env
     * @param compType
     * @param orderInvoiceDetailsEle
     * @return
     */
    /* ============== PAYLOAD BUILDER for Appeasement===================== */
    public JSONObject prepareForterUCRStatusAppeasementRequest(YFSEnvironment env, CompensationType compType, Element orderInvoiceDetailsEle, Element orderEle) throws Exception {
        logger.info("prepareOrderStatusInputToForter: Begin :" + XMLUtil.getElementXMLString(orderEle) + "compType" + compType);
        long eventTime = System.currentTimeMillis();
        String currency = SCXmlUtil.getXpathAttribute(orderEle, "./PriceInfo/@Currency");

        /*-----------Event details---------*/
        JSONObject statusUpdate = buildEventDetails(orderEle, eventTime, compType, A_APPEASEMENT_STATUS);

        /*---------------Updated Total Details-----------*/
        JSONObject updatedTotalAmount = new JSONObject();
        updatedTotalAmount.put(A_AMOUNT_LOCAL_CURRENCY, capturedAmount);
        updatedTotalAmount.put(Currency, currency);
        statusUpdate.put(A_UPDATED_TOTAL_AMOUNT, updatedTotalAmount);

        /*-----------Compensation details---------*/
        JSONObject compensationStatus = buildCompensationStatus(env, compType, orderInvoiceDetailsEle);
        statusUpdate.put(A_COMPENSATION_STATUS, compensationStatus);

        logger.info("Final UCR Status Update to Forter: CrocsUCRStatusUpdateToForter : prepareForterAppeasementStatusUpdateInput"
                + statusUpdate.toString());
        return statusUpdate;
    }

    /**
     * @param orderInvoiceDetailsEle
     * @return
     */
    public JSONObject buildCompensationStatus(YFSEnvironment env, CompensationType compType, Element orderInvoiceDetailsEle) throws Exception {
        JSONObject compensationStatus = new JSONObject();
        Element invoiceHeaderEle = SCXmlUtil.getChildElement(orderInvoiceDetailsEle,E_INVOICE_HEADER);
        Element orderEle = SCXmlUtil.getChildElement(invoiceHeaderEle,E_ORDER);
        String currency = SCXmlUtil.getXpathAttribute(orderEle, "./PriceInfo/@Currency");
       /* String appeasedAmountStr = SCXmlUtil.getXpathAttribute(orderInvoiceDetailsEle, "/InvoiceDetail/InvoiceHeader/@TotalAmount");*/
        String appeasedAmountStr = invoiceHeaderEle.getAttribute(TOTAL_AMOUNT);
        double appeasedAmount = 0.0;
        double grantedAmount = 0.0;
        if (!YFCCommon.isVoid(appeasedAmountStr)) {
            appeasedAmount = Math.abs(Double.parseDouble(appeasedAmountStr));
        }

        /*----------------CalculateGrantedAmount---------------------*/

        cumulativeRefundAmount = cumulativeRefundAmount + appeasedAmount;
        if (appeasedAmount < capturedAmount) {
            grantedAmount = appeasedAmount;
            logger.info("GrantedAmount in CrocsUCRStatusUpdateToForter.buildCompensationStatus is" + grantedAmount);
        } else {
            grantedAmount = capturedAmount - (cumulativeRefundAmount - appeasedAmount);
            logger.info("GrantedAmount in CrocsUCRStatusUpdateToForter.buildCompensationStatus is" + grantedAmount);
        }

        /*----------------totalGrantedAmount---------------------*/
        JSONObject totalGrantedAmount = new JSONObject();
        totalGrantedAmount.put(A_AMOUNT_LOCAL_CURRENCY, grantedAmount);
        totalGrantedAmount.put(A_CURRENCY_F, currency);
        compensationStatus.put(A_TOTAL_GRANTED_AMOUNT, totalGrantedAmount);

        /*-----------------item Status Details---------------*/
        JSONArray itemStatus = new JSONArray();
        NodeList orderListLines = orderEle.getElementsByTagName(E_ORDER_LINE);
        for (int i = 0; i < orderListLines.getLength(); i++) {
            Element eleOrderLine = (Element) orderListLines.item(i);
            String quantity = eleOrderLine.getAttribute(A_ORDERED_QTY);
            Element eleItem = SCXmlUtil.getChildElement(eleOrderLine, E_ITEM);
            Element lineOverallTotalsEle = SCXmlUtil.getChildElement(eleOrderLine, A_LINE_OVERALL_TOTALS);
            String lineTotal = lineOverallTotalsEle.getAttribute(A_LINE_TOTAL);

            JSONObject item = new JSONObject();

            JSONObject basicItemData = getBasicItemData(lineTotal, currency, quantity, eleItem);
            item.put(BASICITEMDATA_F, basicItemData);

            /*-----------statusData Details------------------*/
            Map<String, String> compDetails = resolveCompensationDetails(compType,invoiceHeaderEle);
            JSONObject statusData = buildStatusData(env, compDetails, A_ACCEPTED_BY_MERCHANT);
            item.put(A_APPEASEMENT_STATUS_DATA, statusData);
            itemStatus.put(item);
        }
        compensationStatus.put(A_ITEM_STATUS, itemStatus);
        logger.info("CompensationStatus created for CompensaCrocsUCRStatusUpdateToForter.buildCompensationStatus " + compensationStatus.toString());
        return compensationStatus;
    }

    /* --------------- ITEM JSON HELPERS --------------------- */

    private static JSONObject getBasicItemData(String strAmountLocalCurrency, String currency, String quantity, Element eleItem) {
        // basicItemData
        JSONObject basicItemData = new JSONObject();
        basicItemData.put(A_PRODUCTID_F, eleItem.getAttribute(A_ITEM_ID));
        basicItemData.put(A_NAME_F, eleItem.getAttribute(A_ITEM_DESC));
        basicItemData.put(A_QUANTITY_F, quantity);
        basicItemData.put(A_TYPE_F, TANGEABLE);

        JSONObject price = new JSONObject();
        price.put(A_AMOUNT_LOCAL_CURRENCY, strAmountLocalCurrency);
        price.put(A_CURRENCY_F, currency);
        basicItemData.put(A_PRICE_F, price);
        logger.info("basicItem data json is " + basicItemData.toString());
        return basicItemData;
    }


    /* --------------------- BASIC EVENT DETAILS -------------------- */

    /**
     * @param compType
     * @param eventTime
     * @return
     */
    private JSONObject buildEventDetails(Element orderEle, long eventTime, CompensationType compType, String updatedStatus) {
        logger.info("CrocsUCRStatusUpdateToForter : buildEventDetails" + orderEle);
        String eventId = "";
        String orderNo = orderEle.getAttribute(A_ORDER_NO);

        if (CompensationType.APPEASEMENT.equals(compType)) {
            String invoiceNo = SCXmlUtil.getXpathAttribute(orderEle,
                    "/InvoiceDetail/InvoiceHeader/@InvoiceNo");
            eventId = orderNo + "_" + invoiceNo;
        } else {
            eventId = orderNo + "_" + orderEle.getAttribute(A_RELEASE_NO);
        }
        JSONObject root = new JSONObject();
        root.put(A_EVENT_ID_F, eventId);
        root.put(F_EVENT_TIME, eventTime);
        root.put(A_ORDER_ID_F, orderNo);
        root.put(F_UPDATED_STATUS, updatedStatus);
        logger.info("CrocsUCRStatusUpdateToForter : buildEventDetails " + root.toString());
        return root;
    }

    /**
     * @param env
     * @param compDetails
     * @param updatedStatus
     * @return
     * @throws Exception
     */
    private JSONObject buildStatusData(YFSEnvironment env, Map<String, String> compDetails, String updatedStatus)
            throws Exception {

        logger.beginTimer("CrocsUCRStatusUpdateToForter : buildStatusData");

        String reasonCategory = CrocsForterUtil.getCommonCodeDesc(env, compDetails.get("COMMON_CODE_TYPE"),
                compDetails.get("REASON_CODE"));

        JSONObject statusData = new JSONObject();
        statusData.put(A_COMPENSATION_TYPE_GRANTED, compDetails.get("COMP_TYPE"));
        statusData.put(A_REASON_CATEGORY_F, reasonCategory);
        statusData.put(A_RETURN_METHOD_GRANTED,A_NO_RETURN);
        statusData.put(F_UPDATED_STATUS, updatedStatus);

        logger.endTimer("CrocsUCRStatusUpdateToForter : buildStatusData");
        logger.info("Status Data in CrocsUCRStatusUpdateToForter : buildStatusData" + statusData.toString());
        return statusData;
    }

    /**
     * @param type
     * @param orderEle
     * @return
     * @throws RemoteException
     */
    private Map<String, String> resolveCompensationDetails(CompensationType type, Element orderEle) throws RemoteException {

        logger.beginTimer("CrocsUCRStatusUpdateToForter : resolveCompensationDetails");

        Map<String, String> map = new HashMap<>();
        switch (type) {

            case RETURN:
                break;

            case APPEASEMENT:
                map.put("COMP_TYPE",A_EVENT_CODE_REFUND);
                map.put("COMMON_CODE_TYPE", STR_CROCS_APPEASEMENT_REASONS);
                map.put("REASON_CODE", orderEle.getAttribute(A_INVOICE_CREATION_REASON));
                break;

            case RESHIP:
                break;
        }

        logger.endTimer("CrocsUCRStatusUpdateToForter : resolveCompensationDetails");
        return map;
    }

}
