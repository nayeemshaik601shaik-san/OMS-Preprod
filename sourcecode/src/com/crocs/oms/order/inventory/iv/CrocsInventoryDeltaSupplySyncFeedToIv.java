package com.crocs.oms.order.inventory.iv;

import org.json.JSONArray;
import org.json.XML;
import org.w3c.dom.Document;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsIVAPIConstants;
import com.crocs.oms.util.restapi.CrocsRestConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

/**
 * EOMS- 4677: Inventory OnHand Supply delta sync feed - Implementation
 */
public class CrocsInventoryDeltaSupplySyncFeedToIv  implements CrocsConstant {
	
	 private static YFCLogCategory logger = YFCLogCategory.instance(CrocsInventoryDeltaSupplySyncFeedToIv.class);
	 
	 /**
	  * Description: Establish an Adjustment supply feed from SAP to OMS and have the values updated in IV.
      * OIC will consume this file and generate XML which will be consumed by OMS
	  * INPUT from OIC for Adjust Inventory Supply:-
	  * <suppliesList>
      * <supplies>
      * <itemId>10002-0HZ-M3W5</itemId>
      * <unitOfMeasure>EACH</unitOfMeasure>
      * <type>ONHAND</type>
      * <shipNode>1005</shipNode>
      * <changedQuantity>100</changedQuantity>
      * <adjustmentReason>ADJUSTMENT</adjustmentReason>
      * </supplies>
      * <enableFulfillmentOption>true</enableFulfillmentOption>
      * </suppliesList>
      * 
      * @param env
	  * @param indoc
	  * @return
	  */
	 public Document updateDeltaInventoryFeed(YFSEnvironment env, Document indoc) {

	        Document returnDoc = null;
	        if (logger.isDebugEnabled()) {
	            logger.debug("Input to CrocsInventoryDeltaSupplySyncFeedToIv:businessLogic() : " + SCXmlUtil.getString(indoc));
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

	            logger.verbose("IV api Delta Supply Input:" + syncSupplyJsonInput.get(CrocsConstant.VAL_SUPPLIESLIST));

	            Document docIVInput = CommonUtil.formIVInput(CrocsRestConstants.APPLICATION_JSON,
	                    CrocsRestConstants.METHOD_POST, CrocsIVAPIConstants.IV_UPSERT_SYNC_SUPPLY_API,
	                    suppliesList.toString());
	            returnDoc = docIVInput;
	            logger.verbose("IV api Delta Supply Output : " + SCXmlUtil.getString(docIVInput));

	        } catch (YFSException e) {
	            throw new YFSException(e.getMessage(), e.getErrorCode(), e.getErrorDescription());
	        }
	        logger.verbose("IV api Delta Supply Output : " + SCXmlUtil.getString(returnDoc));
	        return returnDoc;
	    }

}
