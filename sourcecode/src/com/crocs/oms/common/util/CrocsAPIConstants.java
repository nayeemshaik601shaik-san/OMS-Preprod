package com.crocs.oms.common.util;
 
public interface CrocsAPIConstants {
	
	    public static final String API_GET_ORDER_LIST="getOrderList";
		public static final String API_CHANGE_ORDER="changeOrder";
	    public static final String API_FIND_INVENTORY="findInventory";
	    public static final String API_MANAGE_ITEM ="manageItem";
	    public static final String API_GET_COMMON_CODE_LIST ="getCommonCodeList";
		public static final String API_GET_SHIPMENT_LINE_LIST="getShipmentLineList";
		public static final String API_CHANGE_SHIPMENT_STATUS="changeShipmentStatus";
		public static final String API_CHANGE_SHIPMENT="changeShipment";
	    public static final String API_RECORD_SHORTAGE_FOR_PACK="recordShortageForPack";
		public static final String API_GET_COMPLETE_ORDER_LINE_LIST="getCompleteOrderLineList";
		public static final String API_CANCEL_ORDER="cancelOrder";
		public static final String API_CREATE_EXCEPTION="createException";
		public static final String API_CONFIRM_SHIPMENT="confirmShipment";
		public static final String API_MULTI_API="multiApi";
		public static final String API_GET_ORDER_DETAILS="getOrderDetails";
		public static final String API_GET_ORDER_INVOICE_DETAILS="getOrderInvoiceDetails";
		public static final String API_GET_ORDER_INVOICE_LIST="getOrderInvoiceList";
		public static final String API_CHANGE_ORDER_INVOICE="changeOrderInvoice";
		public static final String API_GET_SHIPMENT_LIST="getShipmentList";
		public static final String API_GET_SHIPMENT_LIST_FOR_ORDER="getShipmentListForOrder";
		//EOMS-574 : Create Customer Definition
		public static final String GET_CUSTOMER_LIST_API = "getCustomerList";
		public static final String API_GET_ORDER_RELEASE_LIST ="getOrderReleaseList";
		//EOMS-687
		public static final String API_MODIFY_FULFILLMENT_OPTIONS="modifyFulfillmentOptions";
		// EOMS-698
		public static final String API_SPLIT_SHIPMENT = "splitShipment";
		
		//EOMS-747
		public static final String API_RECEIVE_ORDER = "receiveOrder";
		public static final String API_CLOSE_RECEIPT = "closeReceipt";
		//EOMS-2301
		public static final String API_GET_ORGANIZATION_LIST = "getOrganizationList";
		public static final String API_GET_SCAC_AND_SERVICE_LIST = "getScacAndServiceList";
		public static final String API_GET_SHIPMENT_CONTAINER_LIST="getShipmentContainerList";
		
		//EOMS-2901
		public static final String API_GET_CHARGE_CATEGORY_LIST="getChargeCategoryList";
		public static final String API_GET_CHARGE_NAME_LIST="getChargeNameList";
		
		public static final String API_IMPORT_SHIPMENT="importShipment";
		public static final String API_IMPORT_ORDER="importOrder";
		public static final String API_GET_CHARGE_TRANSACTION_LIST="getChargeTransactionList";

		public static final String API_GET_ORDER_LINE_LIST="getOrderLineList";
		public static final String SERVICE_GET_ORDER_LIST = "CrocsGetOrderListService";
		public static final String SERVICE_GET_ORDER_LINE_LIST = "CrocsGetOrderLineListService";

		public static final String API_GET_PERSON_INFO_LIST="getPersonInfoList";
	    public static final String API_CREATE_SHIPMENT ="createShipment";
		
		public static final String SERVICE_GDPR_DELETE_DATA = "GDPR_Delete_Data";
		
		public static final String API_CHANGE_ORDER_STATUS = "changeOrderStatus";
		
	    //EOMS-4670
	    public static final String SERVICE_GET_ORDER_RELEASE_LIST_US  = "CrocsGetOrderReleaseListForUS";
	    public static final String SERVICE_GET_SHIPMENT_LIST_FOR_ORDER  = "CrocsGetShipmentListForOrderService";
	    //EOMS-4866
	    public static final String SERVICE_GET_ORDER_RELEASE_LIST_CA  = "CrocsGetOrderReleaseListForCA";
	    public static final String SERVICE_GET_SHIPMENT_LIST_FOR_ORDER_CA  = "CrocsGetShipmentListForOrderForCA";
		
		//EOMS -5234
		public static final String SERVICE_UPDATE_ITEM_ORG_DATA_LIST =  "UpdateCrocsItemOrgData";
		public static final String SERVICE_CREATE_ITEM_ORG_DATA_LIST =  "CreateCrocsItemOrgData";
		public static final String API_GET_ITEM_LIST = "getItemList";
		
		//EOMS-5547
		public static final String SERVICE_CROCS_GET_SHIPMNT_LIST_FOR_MP = "CrocsGetShipmentListForMP";
		public static final String SERVICE_CROCS_GET_ORDER_LIST_FOR_MP = "CrocsGetOrderListForMP";
		
		//EOMS-5587
		public static final String SERVICE_CROCS_CONFIRM_SHIPMENT_SYNC_SERV = "CrocsConfirmShipmentSyncServ";
		public static final String API_CREATE_ASYNC_REQUEST = "createAsyncRequest";
		/*EOMS-6582*/
		public static final String SERVICE_GET_ORDER_LIST_FOR_RECEIPT="CrocsGetOrderListForReceiptServ";
	    public static final String API_CREATE_ORDER_INVOICE="createOrderInvoice";
	    
	    //EOMS-6162
	    public static final String SERVICE_HEY_DUDE_GET_ORDER_RELEASE_LIST = "HeyDudeGetOrderReleaseList";
	    public static final String SERVICE_HEY_DUDE_GET_SHIPMENT_LIST_FOR_ORDER = "HeyDudeGetShipmentListForOrder";
		
		public static final String SERVICE_HEYDUDE_CREATE_SO_MIGRATION = "HeyDudeCreateSOMigrationSyncServ";
	    public static final String SERVICE_HEYDUDE_POST_SO_SHIP_MESSAGE_SYNC = "HeyDudePostSOShipMessageSyncServer";

	    //EOMS-7443																
		public static final String SERVICE_HEYDUDE_CONFIRM_SHIPMENT_SYNC_SERV = "HeyDudeConfirmShipmentSyncServMultipleUpdates";
		
		//EOMS-7833
		public static final String API_GET_NOTE_LIST = "getNoteList";
	    public static final String CROCS_GET_ORDER_INVOICE_DETAILS_FORTER = "CrocsGetOrderInvoiceDetailsForForter";
	    public static final String CROCS_GET_COMPLETE_ORD_LINE_DETAILS_FOR_FORTER = "CrocsGetCompleteOrderLineDetailsForForter";

		//EOMS-10219
		public static final String API_CHANGE_RELEASE="changeRelease";
		
		//EOMS-10448 & EOMS-10915
		public static final String SERVICE_CROCS_EMEA_GET_ITEM_LIST_SHIPMENT = "CrocsEMEAGetItemListForShipment";

		//EOMS-12352
		public static final String SERVICE_POST_RETURN_INVOICES_TO_Q="CrocsEMEAPublishInvoiceDetailsToSAP";

}
