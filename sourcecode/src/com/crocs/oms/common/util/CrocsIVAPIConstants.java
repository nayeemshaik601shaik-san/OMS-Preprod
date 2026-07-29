package com.crocs.oms.common.util;

/**
 * This will include all the IV api which will be used inside OMS.
 * 
 */
public interface CrocsIVAPIConstants {
	//EOMS-1009 : Item sync to IV
	public static final String IV_UPSERT_ITEMS_API = "https://api.watsoncommerce.ibm.com/catalog/{tenantId}/v1/items";
	
	//EOMS-926 : store feed sync to IV
	public static final String IV_UPSERT_NODE_API ="https://api.watsoncommerce.ibm.com/configuration/{tenantId}/v1/nodes/{nodeId}";
	
	//EOMS-560 : Inventory OnHand Supply full sync to IV
	public static final String IV_UPSERT_SYNC_SUPPLY_API ="https://api.watsoncommerce.ibm.com/inventory/{tenantId}/v1/supplies?forcePublish=true&recordTransaction=true&newBatch=true&endOfBatch=true";
	public static final String IV_GET_SYNC_SUPPLY_API ="https://api.watsoncommerce.ibm.com/inventory/{tenantId}/v1/supplies?itemId={itemId}&unitOfMeasure={unitOfMeasure}&shipNode={shipNode}";

	public static final String CUSTOM_IV_INVOKE_REST_API="CustomIVInvokeRestAPI";
	public static final String CREATE_CAPTURE_INFO_SERVICE="createCaptureInfo";
	public static final String GET_CAPTURE_INFO_LIST_SERVICE="getCaptureInfoList";
	public static final String CHANGE_CAPTURE_INFO_SERVICE="changeCaptureInfo";
	public static final String DELETE_CAPTURE_INFO_SERVICE = "deleteCaptureInfo";
}
