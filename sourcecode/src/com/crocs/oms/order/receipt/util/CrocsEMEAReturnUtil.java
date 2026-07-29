package com.crocs.oms.order.receipt.util;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CrocsEMEAReturnUtil implements CrocsConstant {
    private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsEMEAReturnUtil.class);


    /** EOMS-12352
     *  Create an Item-Dept Map based on Crocs or HeyDude values by calling a getItemList
     *
     * @param env   OMS environment context
     * @param inDoc Input Receipt XML
     *
     *
     *    Returns a map with Item and Department values
     */
    public Map<String, String> createMapForItemDepartment(YFSEnvironment env, Document inDoc) {
        Document docGetItemListInput = prepareGetItemListInput(inDoc);
        Document docGetItemListOutput = invokeGetItemListAPI(env, docGetItemListInput);
        Element itemOutEle = docGetItemListOutput.getDocumentElement();
        // Map Creation for Item Dept
        Map<String, String> deptMap = new HashMap<>();
        NodeList itemNL = itemOutEle.getElementsByTagName(E_ITEM);
        if (!YFCCommon.isVoid(itemNL)) {
            for (int i = 0; i < itemNL.getLength(); i++) {
                Element eleItem = (Element) itemNL.item(i);
                String strItemID = eleItem.getAttribute(A_ITEM_ID);
                Element elePrimaryInfo = SCXmlUtil.getChildElement(eleItem, E_PRIMARY_INFORMATION);
                String strDepartment = elePrimaryInfo.getAttribute(A_DEPARTMENT);
                if (!YFCCommon.isVoid(strItemID) && !YFCCommon.isVoid(strDepartment)) {
                    deptMap.put(strItemID, strDepartment.toLowerCase());
                }

            }
        }
        return deptMap;
    }

    /** EOMS-12352
     *  Forms input of complex query to invoke GetItemList
     *
     * @param inDoc Input Receipt XML
     *
     *
     *   Return Get Item List Input
     */
    private Document prepareGetItemListInput(Document inDoc) {
         logger.verbose("CrocsEMEAReturnUtil: Start of method prepareGetItemListInput: with input inDoc: " + SCXmlUtil.getString(inDoc));
        if (YFCCommon.isVoid(inDoc)) {
            logger.verbose("Input inDoc is null or void. Returning original document.");
            return inDoc;
        }
        Element ReceiptEle = inDoc.getDocumentElement();

        // Create the root getItemList input element for the Item entity
        Document docGetItemListInput = SCXmlUtil.createDocument(E_ITEM);
        Element getItemListInputEle = docGetItemListInput.getDocumentElement();
        getItemListInputEle.setAttribute(A_ORGANIZATION_CODE, STR_CROCS_NA);
        // Build the ComplexQuery
        Element complexQueryEle = SCXmlUtil.createChild(getItemListInputEle, E_COMPLEX_QUERY);
        complexQueryEle.setAttribute(A_OPERATOR, S_AND);
        Element andEle = SCXmlUtil.createChild(complexQueryEle, E_AND);
        Element orEle = SCXmlUtil.createChild(andEle, E_OR);
        Element receiptLinesEle = SCXmlUtil.getChildElement(ReceiptEle, E_RECEIPT_LINES);
        if (!YFCCommon.isVoid(receiptLinesEle)) {
            List<Element> receiptLinesList = SCXmlUtil.getChildrenList(receiptLinesEle);
            logger.verbose("Building ComplexQuery for " + (receiptLinesList != null ? receiptLinesList.size() : 0) + " Receipt lines.");
            if (receiptLinesList != null) {
                for (Element receiptLine : receiptLinesList) {
                    String itemIdAtReceiptLine = receiptLine.getAttribute(A_ITEM_ID);
                    if (!YFCCommon.isVoid(itemIdAtReceiptLine)) {
                        Element expEle = SCXmlUtil.createChild(orEle, A_EXP);
                        expEle.setAttribute(A_NAME, A_ITEM_ID);
                        expEle.setAttribute(A_VALUE, itemIdAtReceiptLine);
                        expEle.setAttribute(A_QRY_TYPE, EQ);
                    }
                }
            }
        }

        logger.verbose("CrocsEMEAReturnUtil: End of method prepareGetItemListInput: with output docGetItemListInput: " + SCXmlUtil.getString(docGetItemListInput));
        return docGetItemListInput;
    }


    /** EOMS-12352
     *  Calls getItemList Service
     * @param env   OMS environment context
     * @param  getItemListInput  Item List Input XML
     *
     *
     *   Return Get Item List Output
     */

    private Document invokeGetItemListAPI(YFSEnvironment env, Document getItemListInput) {
        logger.verbose("CrocsEMEAReturnUtil: Start of method invokeGetItemListAPI: with input getItemListInput: " + SCXmlUtil.getString(getItemListInput));
        Document getItemListOutput = null;
        try {
            if (!YFCObject.isVoid(getItemListInput)) {
                getItemListOutput = CommonUtil.invokeService(env, SERVICE_CROCS_EMEA_GET_ITEM_LIST_SHIPMENT, getItemListInput);
            }
        } catch (Exception e) {
            logger.verbose("CrocsEMEAReturnUtil: Error encountered while invoking getItemList Service: " + e.getMessage());
            throw new YFSException("Error invoking getItemList Service: " + e.getMessage());
        }
        logger.verbose("CrocsEMEAReturnUtil: End of method invokeGetItemListAPI: successfully retrieved getItemListOutput: " + SCXmlUtil.getString(getItemListOutput));
        return getItemListOutput;
    }



    /** EOMS-12352
     * Group the Receipt Elements based on the Departmemt, based on which the documents will formed separately
     *
     * @param  mapForItemDepartment  Department Item map
     *
     *
     *   Return Get Item List Output
     */
    public  Map<String, List<Element>> groupReceiptLinesByDepartment(Document inDoc, Map<String, String> mapForItemDepartment) {
        logger.verbose("CrocsEMEAReturnUtil.groupReceiptLinesByDepartment: Start");

        Map<String, List<Element>> deptEleMap = new HashMap<>();
        NodeList nlReceiptLine = inDoc.getElementsByTagName(E_RECEIPT_LINE);
        if (!YFCCommon.isVoid(nlReceiptLine)) {
            for (int i = 0; i < nlReceiptLine.getLength(); i++) {
                Element eleReceiptLine = (Element) nlReceiptLine.item(i);
                String strItemID =
                        eleReceiptLine.getAttribute(A_ITEM_ID);

                String strDepartment = mapForItemDepartment.get(strItemID);
                List<Element> receiptList = deptEleMap.computeIfAbsent(strDepartment, k -> new ArrayList<Element>());
                receiptList.add(eleReceiptLine);

            }
        }
        return deptEleMap;

    }

    /** EOMS-12352
     * Group the Receipt Elements based on the Departmemt
     * @param groupedLines
     * @param  inDoc
     *
     *
     *   Return Get Item List Output
     */
    public List<Document> prepareSplitDocumentForReceipts(Document inDoc, Map<String, List<Element>> groupedLines) {
        logger.verbose("CrocsEMEAReturnUtil.prepareSplitDocumentForReceipts: Start");
        List<Document> receiptDocs = new ArrayList<>();
        for (String strDept : groupedLines.keySet()) {
            List<Element> lsReceiptList = groupedLines.get(strDept);

            //Form separate receipt Document by department
            Document docSplitReceipt = SCXmlUtil.createDocument(E_RECEIPT);

            Element eleRoot = docSplitReceipt.getDocumentElement();
            String receivingNode = SCXmlUtil.getAttribute(inDoc.getDocumentElement(), A_RECEIVING_NODE);
            docSplitReceipt.getDocumentElement().setAttribute(A_RECEIVING_NODE, receivingNode);
            Element eleShipment = docSplitReceipt.createElement(E_SHIPMENT);
            Element inputShipmentElement = SCXmlUtil.getXpathElement(inDoc.getDocumentElement(),
                    XPATH_RECEIPT_SHIPMENT);
            String shipmentReceivingNode = inputShipmentElement.getAttribute(A_RECEIVING_NODE);
            String shipmentOrderNo = inputShipmentElement.getAttribute(A_ORDER_NO);
            String shipmentEnterpriseCode = inputShipmentElement.getAttribute(A_ENTERPRISE_CODE);
            eleShipment.setAttribute(A_RECEIVING_NODE, shipmentReceivingNode);
            eleShipment.setAttribute(A_ORDER_NO, shipmentOrderNo);
            eleShipment.setAttribute(A_ENTERPRISE_CODE, shipmentEnterpriseCode);
            Element eleReceiptLines = docSplitReceipt.createElement(E_RECEIPT_LINES);
            eleRoot.appendChild(eleShipment);
            eleRoot.appendChild(eleReceiptLines);
            for (Element eleReceiptLine : lsReceiptList) {
                Element cloneReceiptLine =
                        (Element) docSplitReceipt.importNode(eleReceiptLine, true);
                eleReceiptLines.appendChild(cloneReceiptLine);

            }

            receiptDocs.add(docSplitReceipt);
           logger.verbose("Returning " + receiptDocs.size() + " receipt lines element for grouping with department");
        }
        return receiptDocs;
    }


    /** EOMS-12352
     * Updates a custom attribute flag if invoices are posted once to SAP once WMS Receipt is received for GB to avoid sending
     * duplicate invoices
     * @param docInvoiceDetails
     * @param  env
     *
     *
     */
    public void updateFlagForDuplicateInvoice(YFSEnvironment env, Document docInvoiceDetails) throws Exception {
        logger.verbose("CrocsEMEAReturnUtil.updateFlagForDuplicateInvoice: Start");

        Element orderInvoiceEle = docInvoiceDetails.getDocumentElement();
        String strOrderInvoiceKey = orderInvoiceEle.getAttribute(A_ORDER_INVOICE_KEY);
        Document changeOrderInvoiceInDoc = SCXmlUtil.createDocument(E_ORDER_INVOICE);
        changeOrderInvoiceInDoc.getDocumentElement().setAttribute(A_ORDER_INVOICE_KEY, strOrderInvoiceKey);
        Element changeOrderInvoiceExtnEle = SCXmlUtil
                .createChild(changeOrderInvoiceInDoc.getDocumentElement(), E_EXTN);
        changeOrderInvoiceExtnEle.setAttribute(A_EXTN_IS_SENT_TO_SAP, FLAG_Y);
        logger.verbose("changeOrderInvoiceInput : " + XMLUtil.getXMLString(changeOrderInvoiceInDoc));
        Document changeOrderInvoiceOutDoc = CommonUtil.invokeAPI(env, "", API_CHANGE_ORDER_INVOICE,
                changeOrderInvoiceInDoc);
        logger.verbose("changeOrderInvoiceOutput: " + XMLUtil.getXMLString(changeOrderInvoiceOutDoc));
        logger.info(OMS_LOG_INFO
                + "Flag updated for GB SAP posting Invoice Key"
                + strOrderInvoiceKey);
        logger.verbose("CrocsEMEAReturnUtil.updateFlagForDuplicateInvoice: End");

    }
}


