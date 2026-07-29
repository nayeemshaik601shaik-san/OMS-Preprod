
package com.crocs.oms.item;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;

public class CrocsUpdateAdditionalAttributeList {

	private static final YFCLogCategory LOGGER = YFCLogCategory.instance(CrocsUpdateAdditionalAttributeList.class);


	/**
     * Retrieves Item Details from Transaction Object and updates AdditionalAttributeKey in indoc.
     *
     * @param env   YFSEnvironment
     * @param indoc Input Document (to be updated)
     * @return Updated input Document
     */

    public Document updateAdditionalAttributeList(YFSEnvironment env, Document indoc) {
      Document getItemListOutput = (Document) env.getTxnObject(CrocsConstant.A_SAP_GET_ITEM_LIST);

         updateAdditionalAttributeKeys(indoc, getItemListOutput);
         LOGGER.verbose("Updated AdditionalAttributeKey count: " + SCXmlUtil.getString(indoc));

        return indoc;
    }


    	/**
        * Updates AdditionalAttributeKey in inputDoc based on sourceDoc for single Item.
        *
        * Logic:
        * 1. Read all <AdditionalAttribute> nodes from sourceDoc and store their keys in a Map.
        *    - Map Key: Composite of AttributeGroupID + "|" + Name
        *    - Map Value: AdditionalAttributeKey from sourceDoc
        * 2. Iterate through <AdditionalAttribute> nodes in inputDoc.
        *    - For each node, build the same composite key and check if it exists in the map.
        *    - If found, update the AdditionalAttributeKey in inputDoc.
        *
        * @param inputDoc  Document to update
        * @param sourceDoc Document containing correct keys
        * @return Number of attributes updated
        */

    private void updateAdditionalAttributeKeys(Document inputDoc, Document sourceDoc) {

        try {
            // Get the single Item from sourceDoc
            Element sourceItem = (Element) sourceDoc.getElementsByTagName(CrocsXmlConstants.E_ITEM).item(0);

            // Build a map of AttributeGroupID+Name -> AdditionalAttributeKey
            java.util.Map<String, String> keyMap = new java.util.HashMap<>();
            NodeList sourceAttrs = sourceItem.getElementsByTagName(CrocsXmlConstants.E_ADDITIONAL_ATTRIBUTE);
            for (int i = 0; i < sourceAttrs.getLength(); i++) {
                Element attr = (Element) sourceAttrs.item(i);
                String groupId = attr.getAttribute(CrocsXmlConstants.A_ADDTRIBUTE_GROUP_ID);
                String name = attr.getAttribute(CrocsXmlConstants.A_NAME);
                String key = attr.getAttribute(CrocsXmlConstants.A_ADDITIONAL_ADDTRIBUTE_KEY);
                if (!key.isEmpty()) {
                	
                	/**
                	 * How entries are stored in the map:
                     * - Composite Key: AttributeGroupID + "|" + Name
                     *   Example: "HSTCode|test_ca"
                     * - Value: AdditionalAttributeKey from sourceDoc
                     *   Example: "2025111311460872694700"
                     *
                     * So the map looks like:
                     * {
                     *   "HSTCode|crocs_ca" -> "2025111311460872694700",
                     *   "StandardCost|crocs_ca" -> "2025111311460872694701"
                     * }
                     **/             	
                    keyMap.put(groupId + "|" + name, key);
                }
            }

            // --- STEP 2: Update inputDoc using the map ---
            Element inputItem = (Element) inputDoc.getElementsByTagName(CrocsXmlConstants.E_ITEM).item(0);
           

            NodeList inputAttrs = inputItem.getElementsByTagName(CrocsXmlConstants.E_ADDITIONAL_ATTRIBUTE);
            List<Element> toRemove = new ArrayList<>();
            for (int i = 0; i < inputAttrs.getLength(); i++) {
                Element attr = (Element) inputAttrs.item(i);
                String groupId = attr.getAttribute(CrocsXmlConstants.A_ADDTRIBUTE_GROUP_ID);
                String name = attr.getAttribute(CrocsXmlConstants.A_NAME);
                String compositeKey = groupId + "|" + name;
                String value = attr.getAttribute("Value");

                // If composite key exists in map, update the AdditionalAttributeKey
                
				LOGGER.verbose(value + " value " + compositeKey + "compositeKey");
				
				if (keyMap.containsKey(compositeKey)) {
					if (!YFCCommon.isVoid(value) && !YFCObject.isNull(value)) {
						// Value is NOT empty/null → update AdditionalAttributeKey
						attr.setAttribute(CrocsXmlConstants.A_ADDITIONAL_ADDTRIBUTE_KEY, keyMap.get(compositeKey));
					} else {
						// Value is empty/null → remove the AdditionalAttribute element
						 toRemove.add(attr);
					}
				}
				/* record not in OMS AND value is null */
				else if (YFCCommon.isVoid(value) && YFCObject.isNull(value)) {
					toRemove.add(attr);
					LOGGER.verbose("Removed AdditionalAttribute for [" + compositeKey + "] due to empty Value.");

				}
			}
            
         // Perform removals after iteration
            for (Element e : toRemove) {
                e.getParentNode().removeChild(e);
            }
            
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
