package com.crocs.oms.common.util;

public interface CrocsErrorConstants {
	
	public static final String VAL_ERROR_CODE_YFS10460="YFS10460";
	public static final String VAL_ERROR_DESCRIPTION_YFS10460="Missing mandatory attributes in input";
	public static final String VAL_ERROR_CODE_YFS10003="YFS10003";
	public static final String VAL_ERROR_DESCRIPTION_YFS10003="YFS:Invalid Details";
	//EOMS-1056
	public static final String VAL_OOB_ERROR="Unable to find order. Please provide valid order details";
	public static final String VAL_ERROR_CODE_EXTN_001="EXTN_001";
	public static final String VAL_ERROR_CODE_EXTN_002="EXTN_002";
	public static final String VAL_ERROR_DESCRIPTION_EXTN_001="Order cannot be cancelled because remorse period is passed";
	public static final String VAL_ERROR_DESCRIPTION_EXTN_002="Order is not updated, No action or invalid action is passed in the input";
	
	//EOMS-4843 : Fraud check changes
	public static final String VAL_ERROR_CODE_EXTN_003="EXTN_003";
	public static final String VAL_ERROR_DESCRIPTION_EXTN_003="Forter exception occurred while processing fraud check";
	public static final String VAL_ERROR_DESCRIPTION_FORTER ="The Forter fraud check failed, causing an exception during order processing.";
	
	//EOMS-6046 : Duplication Order Check for MP orders
	public static final String VAL_ERROR_CODE_EXTN_004="EXTN_004";
	public static final String VAL_ERROR_DESCRIPTION_EXTN_004="The order already exists. Duplicate orders are not allowed.";
	public static final String VAL_ERROR_DESCRIPTION_MP ="Duplicate order detected. Please use a unique Customer PO Number.";
	
	//EOMS-5877 : WMSCode = 2 changes
	public static final String VAL_ERROR_CODE_EXTN_005="EXTN_005";
	public static final String VAL_ERROR_DESCRIPTION_EXTN_005="Confirm Shipment update with WMSCode = 2 was not processed for this order";
	public static final String VAL_ERROR_DESCRIPTION_WMSCODE ="WMSCode = 2 was received before the WMSCode = 1 update, resulting in a processing delay for the WMSCode = 2 update";
	
	//EOMS-6583 : Duplicate Order Check - AptosOrderNo+Store
	public static final String VAL_ERROR_DESCRIPTION_TO ="Duplicate order detected. Please use a unique Customer PO Number and store Combination.";


}

