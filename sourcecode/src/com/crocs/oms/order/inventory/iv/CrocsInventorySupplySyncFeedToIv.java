package com.crocs.oms.order.inventory.iv;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsIVAPIConstants;
import com.crocs.oms.util.restapi.CrocsRestConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.rmi.RemoteException;
import java.util.Objects;

/**
 * EOMS- 1481 this class is used for IV setup for ONHAND and Future Inventory
 */
public class CrocsInventorySupplySyncFeedToIv implements CrocsConstant {

    private static YFCLogCategory logger = YFCLogCategory.instance(CrocsInventorySupplySyncFeedToIv.class);


    /**
     * FLOW:- Establish an on-hand supply feed from SAP to OMS and have the values updated in IV.
     * OIC will consume this file and generate XML which will be consumed by OMS
     * <p>
     * INPUT from OIC for ONHAND Inventory Supply:-
     * <?xml version="1.0" encoding="UTF-8" ?>
     * <suppliesList>
     * <supplies>
     * <itemId>10002-0HZ-M3W5</itemId>
     * <unitOfMeasure>EACH</unitOfMeasure>
     * <type>ONHAND</type>
     * <shipNode>OHIO_DC</shipNode>
     * <quantity>100</quantity>
     * </supplies>
     * <enableFulfillmentOption>true</enableFulfillmentOption>
     * </suppliesList>
     * <p>
     * INPUT from OIC for FUTURE Inventory Supply :-
     * <?xml version="1.0" encoding="UTF-8" ?>
     * <suppliesList>
     * <supplies>
     * <itemId>10002-0HZ-M3W5</itemId>
     * <unitOfMeasure>EACH</unitOfMeasure>
     * <type>PLAN</type>
     * <shipNode>OHIO_DC</shipNode>
     * <quantity>100</quantity>
     * <eta>2025-03-01T00:00:00Z</eta>
     * <referenceType>FutureInventory</referenceType>
     * <reference>PO_0001</reference>
     * </supplies>
     * <enableFulfillmentOption>true</enableFulfillmentOption>
     * </suppliesList>
     * <p>
     * OMS Will consume as a xml and then convert into json to call IV syncSupply api.
     * This IV api will be used for
     * Hourly for delta sync and
     * Nightly for full sync
     **/


    public Document updateInventoryFeed(Document indoc) {

        Document returnDoc = null;
        if (logger.isDebugEnabled()) {
            logger.debug("Input to CrocsInventorySupplySyncFeedToIv:businessLogic() : " + SCXmlUtil.getString(indoc));
        }

        try {
            YFCDocument yfcDoc = YFCDocument.getDocumentFor(indoc);
            org.json.JSONObject syncSupplyJsonInput = XML.toJSONObject(yfcDoc.getString());

            org.json.JSONObject suppliesList = (org.json.JSONObject) syncSupplyJsonInput
                    .get(CrocsConstant.VAL_SUPPLIESLIST);

            org.json.JSONObject suppliesJson = (org.json.JSONObject) suppliesList.get(CrocsConstant.VAL_SUPPLIES);
            suppliesJson.put(V_SHIPNODE, suppliesJson.get(V_SHIPNODE).toString());

            Object supplies = suppliesList.get(CrocsConstant.VAL_SUPPLIES);

            if (!(supplies instanceof JSONArray)) {
                JSONArray suppliesArray = new JSONArray();
                suppliesArray.put(supplies);
                suppliesList.put(CrocsConstant.VAL_SUPPLIES, suppliesArray);
            }

            logger.verbose("IV api Sync Supply Input:" + syncSupplyJsonInput.get(CrocsConstant.VAL_SUPPLIESLIST));

            Document docIVInput = CommonUtil.formIVInput(CrocsRestConstants.APPLICATION_JSON,
                    CrocsRestConstants.METHOD_PUT, CrocsIVAPIConstants.IV_UPSERT_SYNC_SUPPLY_API,
                    suppliesList.toString());
            returnDoc = docIVInput;
            logger.verbose("IV api Sync Supply Output : " + SCXmlUtil.getString(docIVInput));

        } catch (YFSException e) {
            throw new YFSException(e.getMessage(), e.getErrorCode(), e.getErrorDescription());
        }
        return returnDoc;
    }

    /**
     * this method is used to update/insert inventory feeds
     *
     * @param env   env
     * @param indoc incoming document
     * @return document
     */
    public Document inventorySupplyFeed(YFSEnvironment env, Document indoc) {
        boolean enableFulfillmentOption = true;
        Document returnDoc = null;
        if (logger.isDebugEnabled()) {
            logger.debug("Input to CrocsInventorySupplySyncFeedToIv:businessLogic() " + SCXmlUtil.getString(indoc));
        }

        try {
            Element suppliesListEle = indoc.getDocumentElement();
            Element suppliesEle = SCXmlUtil.getChildElement(suppliesListEle, VAL_SUPPLIES);
            Element inventoryTypeEle = SCXmlUtil.getChildElement(suppliesEle, A_TYPE_F);
            String inventoryType = inventoryTypeEle.getTextContent();
            Element enableFulfillmentOptionEle = SCXmlUtil.getChildElement(suppliesListEle, V_ENABLE_FULFILLMENT_OPTION);
            if (enableFulfillmentOptionEle != null) {
                enableFulfillmentOption = Boolean.parseBoolean(enableFulfillmentOptionEle.getTextContent());
            }
            if (!YFCCommon.isVoid(inventoryType) && inventoryType.equalsIgnoreCase(V_PLAN)) {
                Element referenceEle = SCXmlUtil.getChildElement(suppliesEle, V_REFERENCE);
                String reference = referenceEle.getTextContent();
                returnDoc = futureInventory(env, indoc, returnDoc, suppliesEle, enableFulfillmentOption, reference);
            } else {
                returnDoc = updateInventoryFeed(indoc);
                logger.verbose("IV api Sync Supply Output : " + SCXmlUtil.getString(returnDoc));
            }
        } catch (YFSException e) {
            throw new YFSException(e.getMessage(), e.getErrorCode(), e.getErrorDescription());
        }
        return returnDoc;
    }

    /** this method is used to insert/update futureInventory feed
     * @param env env
     * @param indoc indoc
     * @param returnDoc doc
     * @param suppliesEle suppliesEle
     * @param enableFulfillmentOption enableFulfillmentOption
     * @param reference reference
     * @return document
     */
    private Document futureInventory(YFSEnvironment env, Document indoc, Document returnDoc, Element suppliesEle, Boolean enableFulfillmentOption, String reference) {
        Document getSupplies;
        // gets all the IV records corresponding to item passed
        getSupplies = getSupplies(env, suppliesEle);
        logger.verbose("IV get Supply Output:" + SCXmlUtil.getString(getSupplies));
        // Extract the <Output> element
        NodeList outputNodes = getSupplies.getElementsByTagName(V_OUTPUT);
        logger.verbose("response from getSupplies:" + outputNodes);
        if (outputNodes.getLength() > 0) {
            Element outputElement = (Element) outputNodes.item(0);
            String jsonString = outputElement.getTextContent().trim();
            // Step 3: Parse the JSON string into a JSONArray
            JSONArray suppliesArray = new JSONArray(jsonString);
            for (int i = 0; i < suppliesArray.length(); i++) {
                JSONObject jsonObject = suppliesArray.getJSONObject(i);
                if (jsonObject.getString(A_TYPE_F).equals(V_PLAN) && jsonObject.getString(V_REFERENCE).equals(reference)) {
                    Document updatedQuantityDoc = updateQuantity(jsonObject, enableFulfillmentOption);
                    updateExsitingPORecord(env, updatedQuantityDoc);
                    logger.verbose("IV api Sync Supply Output: " + SCXmlUtil.getString(returnDoc));
                    returnDoc = updateInventoryFeed(indoc);

                }
            }
            //this condidtion is used to insert data for new PO where type is PLAN
            returnDoc = updateInventoryFeed( indoc);
            logger.verbose("IV api Sync Supply Output:" + SCXmlUtil.getString(returnDoc));
        } else {
            returnDoc = updateInventoryFeed(indoc);
            logger.verbose("IV api Sync Supply Output:" + SCXmlUtil.getString(returnDoc));
        }
        return returnDoc;
    }

    /** this method is used to prepare input to update exsiting record quantity to 0.
     * @param jsonObject jsonObject
     * @param enableFulfillmentOption enableFulfillmentOption
     * @return Document
     */
    private Document updateQuantity(JSONObject jsonObject, Boolean enableFulfillmentOption) {
        Document updatedQuantityDoc = SCXmlUtil.createDocument(VAL_SUPPLIESLIST);
        Element updatedQuantityEle = updatedQuantityDoc.getDocumentElement();
        Element suppliesEle = SCXmlUtil.createChild(updatedQuantityEle, VAL_SUPPLIES);
        //Create and set the value for <itemId> element
        Element itemIdEle = SCXmlUtil.createChild(suppliesEle, V_ITEM_ID);
        itemIdEle.setTextContent(jsonObject.getString(V_ITEM_ID));
        //Create and set the value for <unitOfMeasure> element
        Element unitOfMeasureEle = SCXmlUtil.createChild(suppliesEle, V_UNIT_OF_MEASURE);
        unitOfMeasureEle.setTextContent(jsonObject.getString(V_UNIT_OF_MEASURE));
        //Create and set the value for <shipNode> element
        Element shipNodeEle = SCXmlUtil.createChild(suppliesEle, V_SHIPNODE);
        shipNodeEle.setTextContent(jsonObject.getString(V_SHIPNODE));
        //Create and set the value for <type> element
        Element typeEle = SCXmlUtil.createChild(suppliesEle, A_TYPE_F);
        typeEle.setTextContent(jsonObject.getString(A_TYPE_F));
        //Create and set the value for <quantity> element to 0
        Element quantityEle = SCXmlUtil.createChild(suppliesEle, A_QUANTITY_F);
        quantityEle.setTextContent("0");
        //Create and set the value for <eta> element
        Element etaEle = SCXmlUtil.createChild(suppliesEle, V_ETA);
        etaEle.setTextContent(jsonObject.getString(V_ETA));
        //Create and set the value for <referenceType> element
        Element referenceTypeEle = SCXmlUtil.createChild(suppliesEle, V_REFERENCE_TYPE);
        if (!YFCCommon.isVoid(jsonObject.getString(V_REFERENCE_TYPE))){
            referenceTypeEle.setTextContent(jsonObject.getString(V_REFERENCE_TYPE));
        }
        //Create and set the value for <reference> element
        Element referenceEle = SCXmlUtil.createChild(suppliesEle, V_REFERENCE);
        referenceEle.setTextContent(jsonObject.getString(V_REFERENCE));
        //Create and set the value for <enableFulfillmentOption> element
        Element enableFulfillmentOptionEle = SCXmlUtil.createChild(updatedQuantityEle, V_ENABLE_FULFILLMENT_OPTION);
            enableFulfillmentOptionEle.setTextContent(String.valueOf(enableFulfillmentOption));
        logger.verbose("update exsisting record quantity to 0:" + SCXmlUtil.getString(updatedQuantityDoc));
        return updatedQuantityDoc;
    }

    /**
     * this method is used to call the getSupplies API to get all the IV records for item that is passed in the input
     * @param env         env
     * @param suppliesEle supplies
     * @return list of supplies
     */
    private Document getSupplies(YFSEnvironment env, Element suppliesEle) {
        Document getSupplies = null;
        try {
            Element item = SCXmlUtil.getChildElement(suppliesEle, V_ITEM_ID);
            String itemId = item.getTextContent();
            Element unitofMeasureEle = SCXmlUtil.getChildElement(suppliesEle, V_UNIT_OF_MEASURE);
            String unitOfMeasure = unitofMeasureEle.getTextContent();
            Element shipNodeEle = SCXmlUtil.getChildElement(suppliesEle, V_SHIPNODE);
            String shipNode = shipNodeEle.getTextContent();
            logger.verbose("get supplies input:" + itemId +unitOfMeasure + shipNode);
            Document docGetIV = CommonUtil.formGetIVInput(CrocsRestConstants.APPLICATION_JSON,
                    CrocsRestConstants.METHOD_GET, CrocsIVAPIConstants.IV_GET_SYNC_SUPPLY_API,
                    itemId, unitOfMeasure, shipNode);
            getSupplies = CommonUtil.invokeService(env,  CrocsIVAPIConstants.CUSTOM_IV_INVOKE_REST_API, docGetIV);
            logger.verbose("Get supplies output:" + SCXmlUtil.getString(getSupplies));
        } catch (YFSException | RemoteException e) {
            throw new YFSException("Exception in getSupplies API: " + e.getMessage());
        }
        logger.verbose("IV get Supply Output:" + SCXmlUtil.getString(getSupplies));
        return getSupplies;
    }

    /** This method is used to update quantity to 0 for exisisting reference record
     * @param env env
     * @param indoc indoc
     */
    public void updateExsitingPORecord(YFSEnvironment env, Document indoc) {
        Document retrunDoc = null;
        if (logger.isDebugEnabled()) {
            logger.debug("Input to CrocsInventorySupplySyncFeedToIv:businessLogic() " + SCXmlUtil.getString(indoc));
        }
        try {
            YFCDocument yfcDoc = YFCDocument.getDocumentFor(indoc);
            org.json.JSONObject syncSupplyJsonInput = XML.toJSONObject(yfcDoc.getString());

            org.json.JSONObject suppliesList = (org.json.JSONObject) syncSupplyJsonInput
                    .get(CrocsConstant.VAL_SUPPLIESLIST);
            org.json.JSONObject suppliesJson = (org.json.JSONObject) suppliesList.get(CrocsConstant.VAL_SUPPLIES);
            suppliesJson.put(V_SHIPNODE, suppliesJson.get(V_SHIPNODE).toString());
            Object supplies = suppliesList.get(CrocsConstant.VAL_SUPPLIES);
            if (!(supplies instanceof JSONArray)) {
                JSONArray suppliesArray = new JSONArray();
                suppliesArray.put(supplies);
                suppliesList.put(CrocsConstant.VAL_SUPPLIES, suppliesArray);
            }
            logger.verbose("IV api Sync Supply Input:" + syncSupplyJsonInput.get(CrocsConstant.VAL_SUPPLIESLIST));

            Document docIVInput = CommonUtil.formIVInput(CrocsRestConstants.APPLICATION_JSON,
                    CrocsRestConstants.METHOD_PUT, CrocsIVAPIConstants.IV_UPSERT_SYNC_SUPPLY_API,
                    suppliesList.toString());
            retrunDoc = CommonUtil.invokeService(env, CrocsIVAPIConstants.CUSTOM_IV_INVOKE_REST_API, docIVInput);
            logger.verbose("IV api sync Supply Output: " + SCXmlUtil.getString(retrunDoc));

        } catch (YFSException | RemoteException e) {
            throw new YFSException(e.getMessage());
        }
    }
}
