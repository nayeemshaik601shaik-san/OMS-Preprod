package com.crocs.oms.common.util;

import java.util.Set;

public interface CrocsConstant extends CrocsAPIConstants, CrocsTemplateConstants,
        CrocsXmlConstants, CrocsErrorConstants, CrocsPropertyFileConstants {

    public static final String VAL_ACTION_MODIFY = "MODIFY";
    public static final String VAL_REMORSE_HOLD = "REMORSE_PERIOD";
    public static final String VAL_MULTI_SHIPMENT_HOLD = "MULTI_SHIPMENT_HOLD";
    public static final String VAL_MULTI_SHIPMENT_HOLD_REASON = "NO of Shipment is more than one";
    public static final String VAL_SYS_RELEASE = "Systematic Release";
    public static final String VAL_STATUS_1300 = "1300";
    public static final String VAL_FLAG_Y = "Y";
    public static final String VAL_MODIFY = "MODIFY";
    public static final String VAL_CREDIT_CHECK_HOLD = "Credit Check Hold";
    public static final String VAL_CHECK_PROCESSING = "Check Processing";
    public static final String VAL_AR_CREDIT = "AR CREDIT";
    public static final String VAL_CHECK = "CHECK";
    public static final String VAL_WIRE_TRANSFER = "WIRE-TRANSFER";
    public static final String VAL_CREDIT_CARD = "CREDIT_CARD";
    public static final String VAL_CREATE = "1100";
    public static final String VAL_MARKETPLACE = "Marketplace";
    public static final String VAL_INTERCOMPANY = "Intercompany";
    public static final String VAL_DTC = "DTC";
    public static final String FLAG_N = "N";
    public static final String VAL_WAIT = "WAIT";


    public static final String VAL_BETWEEN = "BETWEEN";
    public static final String VAL_STATUS_1200 = "1200";
    public static final String MSG_NOT_DUPLICATE = "Order is not duplicate";
    public static final String MSG_DUPLICATE = "Order is identified as duplicate";
    public static final String VAL_SUPPLIESLIST = "suppliesList";
    public static final String VAL_SUPPLIES = "supplies";
    public static final String VAL_PAYMENT_GATEWAY_REJECTED_RESPONSE = "RejectedResponse";
    public static final String VAL_PAYMENT_EXCEPTION = "paymentException";
    public static final String VAL_APPLY_HOLD = "ApplyHold";
    public static final String VAL_RELEASE_HOLD = "ReleaseHold";
    public static final String VAL_REJECT_HOLD = "RejectHold";


    public static final String VAL_SYSTEM = "SYSTEM";


    public static final String VAL_AR_STATE_TAX = "AR STATE TAX";
    public static final String VAL_TAX = "TAX";
    public static final String VAL_RESET = "RESET";
    public static final String VAL_TAX_INTEGRATION_STUB = "TAX_INTEGRATION_STUB";
    public static final String VAL_CHANGE_TAX_EXEMPT_FLAG_EX = "Change Tax Exempt Flag.ex";
    public static final String VAL_SHIP_NODE_9620 = "9620";
    public static final String VAL_STATUS_REL = "REL";
    public static final String VAL_STATUS_PCNF = "PCNF";
    public static final String VAL_STATUS_CNF = "CNF";
    public static final String VAL_TRANSACTION_YCD_CHECK_FOR_BACKROOM_PICK = "YCD_CHECK_FOR_BACKROOM_PICK";
    public static final String VAL_TRANSACTION_YCD_BACKROOM_PICK_IN_PROGRESS = "YCD_BACKROOM_PICK_IN_PROGRESS";
    public static final String VAL_TRANSACTION_YCD_BACKROOM_PICK = "YCD_BACKROOM_PICK";
    public static final String VAL_DOCUMENT_TYPE_SALES_ORDER = "0001";


    public static final String T_ADYEN_PAYMENT_API_REQ_TEMPLATE_GLOBAL = "<RestApi AuthStyle=\"Open\" ConvertRequestFormat=\"JSON\" MediaType=\"application/xml\" Method=\"POST\" ReadHostFromProperty=\"Y\" ReadHostFromServiceArgument=\"false\" Resource=\"\"> <Headers> </Headers> <Message> <root> <merchantAccount> </merchantAccount> <reference> </reference> <merchantOrderReference> </merchantOrderReference>  <paymentMethod> <storedPaymentMethodId> </storedPaymentMethodId> <type> </type></paymentMethod> <amount> <currency> </currency> <value> </value> </amount> <shopperInteraction> </shopperInteraction> <recurringProcessingModel> </recurringProcessingModel> <shopperReference> </shopperReference> </root> </Message> </RestApi>";
    public static final String T_ADYEN_PAYMENT_API_REQ_TEMPLATE_CAPTURE = "<RestApi AuthStyle=\"Open\" ConvertRequestFormat=\"JSON\" MediaType=\"application/xml\" Method=\"POST\" ReadHostFromProperty=\"Y\" ReadHostFromServiceArgument=\"false\" Resource=\"\"><Headers/><Message><root><merchantAccount/><reference/><amount><currency/><value/></amount></root></Message></RestApi>";
    public static final String ADYEN_RESOURCE_URL = "ADYEN_AUTHORIZE_RESOURCE";
    public static final String ADYEN_AUTH_EXP = "ADYEN_AUTH_EXP";
    public static final String ADYEN_RESOURCE_MID = "ADYEN_RESOURCE_MID";
    public static final String A_USER_NAME = "Username";
    public static final String A_PASSWORD = "Password";
    public static final String A_RESOURCE = "Resource";
    public static final String PaymentReference5 = "PaymentReference5";
    public static final String PAYMENT_CEDITCARD_NO = "CreditCardNo";
    public static final String PAYMENT_REFERENCE4 = "PaymentReference4";
    public static final String A_REQUEST_AMOUNT = "RequestAmount";
    public static final String A_HEADER_CHARGE = "HeaderCharge";
    public static final String A_CHARGE_AMOUNT = "ChargeAmount";
    public static final String A_LIST_PRICE = "ListPrice";
    public static final String A_RETAIL_PRICE = "RetailPrice";
    public static final String A_CHARGE_TRANSACTION_KEY = "ChargeTransactionKey";
    public static final String A_CURRENCY = "Currency";
    public static final String A_ASYNC_FAILURE = "AsyncFailure";
    public static final String A_ENTERPRISE_CODE = "EnterpriseCode";
    public static final String A_CHARGE_TYPE = "ChargeType";
    public static final String ORDERNO = "OrderNo";
    public static final String A_ADYEN_RETRY = "AdyenRetry";
    public static final String A_BillToEmailId = "BillToEmailId";
    public static final String A_BillToFirstName = "BillToFirstName";
    public static final String A_BillToLastName = "BillToLastName";
    public static final String C_100 = "100";
    public static final String ATTR_HEADERS = "Headers";
    public static final String ATTR_HEADER = "Header";
    public static final String A_ADYEN_API_KEY = "ADYEN_API_KEY";
    public static final String A_ADYEN_API_KEY_1 = "ADYEN_API_KEY1";
    public static final String A_ADYEN_API_KEY_2 = "ADYEN_API_KEY2";
    public static final String YFS = "yfs";
    public static final String ATTR_NAME = "Name";
    public static final String ATTR_VALUE = "Value";
    public static final String NO = "N";
    public static final String YES = "Y";
    public static final String A_VALUE = "Value";
    public static final String reference = "reference";
    public static final String C_merchantOrderReference = "merchantOrderReference";
    public static final String COMMON_CODE_CREDIT_CARD_AUTH_EXP = "CREDIT_CARD_AUTH_EXP";
    public static final String RESULT_CODE_AUTHORISED = "Authorised";
    public static final String RESULT_CODE_REFUSED = "Refused";
    public static final String REFUSAL_REASON = "RefusalReason";
    public static final String REFUSAL_REASON_FRAUD = "FRAUD";
    public static final String FRAUD_SCORE = "FraudScore";
    public static final String AuthCode = "AuthCode";
    public static final String ELE_PAYMENT_DETAILS_LIST = "PaymentDetailsList";
    public static final String RequestId = "RequestId";
    public static final String AUTHORIZATION_EXPIRATIO_NDATE = "AuthorizationExpirationDate";
    public static final String ADYEN_AUTHORIZATION_STATUS = "Adyen Authorization Status";
    public static final String ADYEN_RESULT_CODE_NONAUTHORIZED = "resultCode Non-Authorized";
    public static final String ADYEN_CALL_FAILED = "Adyen Auth Call Unsuccessful";
    public static final String ADYEN_CALL_REFUSED_001 = "ADYEN_CALL_REFUSED_001";
    public static final String ADYEN_CALL_NON_AUTHORIZED_002 = "ADYEN_CALL_NON_AUTHORIZED_002";
    public static final String ADYEN_CALL_FAILURE_003 = "ADYEN_CALL_FAILURE_001";
    public static final String A_CARD_HOLDER_NAME = "holderName";
    public static final String SHOPPER_EMAIL = "shopperEmail";
    public static final String SHOPPER_NAME = "shopperName";
    public static final String SHOPPER_REFERENCE = "shopperReference";
    public static final String A_RECURRING_PROCESS_MODEL_UNSCHEDULED_CARD_ON_FILE = "Subscription";
    public static final String ADYEN_PAYMENT_METHOD = "paymentMethod";
    public static final String A_LATEST = "LATEST";
    public static final String ORIGIN_COUNTRY = "origin_country";
    public static final String POSTMEN_QUANTITY = "quantity";
    public static final String SKU = "sku";
    public static final String POSTMEN_AMOUNT = "amount";
    public static final String POSTMEN_CURRENCY = "currency";
    public static final String VALUE = "value";
    public static final String ROOT = "root";
    public static final String ADYEN_FIRST_NAME = "firstName";
    public static final String ADYEN_LAST_NAME = "lastName";
    public static final String CURRENCY_USD = "USD";
    public static final String STORE_PAYMENT_METHOD_ID_ADYEN = "storedPaymentMethodId";
    public static final String TYPE = "type";
    public static final String SCHEME = "scheme";
    public static final String SHOPPER_INTERACTION = "shopperInteraction";
    public static final String SHOPPER_INTERACTION_AUTH = "ContAuth";
    public static final String ADYEN_RESULT_CODE = "resultCode";
    public static final String ADYEN_PSP_REFERENCE = "pspReference";
    public static final String PAYMENT_STATUS_PAID = "PAID";
    public static final String A_X_API_KEY = "X-Api-Key";
    public static final String A_IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String A_PAYMENT_REFERENCE1 = "PaymentReference1";
    public static final String A_MAX_CHARGE_LIMIT = "MaxChargeLimit";
    public static final String A_ADYEN_AUTHORIZE_PROPERTY = "ADYEN.authorize";
    public static final String A_ADYEN_RECURRING_PROCESS_MODEL = "recurringProcessingModel";
    public static final String A_RECURRING_PROCESS_MODEL_CARD_ON_FILE = "CardOnFile";
    public static final String DATE_FORMAT = "dd/MM/yyyy";
    public static final String Payment = "Payment";
    public static final String PaymentTransactionErrorList = "PaymentTransactionErrorList";
    public static final String AUTHORIZATION = "AUTHORIZATION";
    public static final String AuthorizationId = "AuthorizationId";
    public static final String NULL = "null";
    public static final String BillToAddressLine1 = "BillToAddressLine1";
    public static final String BillToCity = "BillToCity";
    public static final String BillToCountry = "BillToCountry";
    public static final String BillToDayPhone = "BillToDayPhone";
    public static final String BillToEmailId = "BillToEmailId";
    public static final String BillToFirstName = "BillToFirstName";
    public static final String BillToId = "BillToId";
    public static final String BillToKey = "BillToKey";
    public static final String BillToLastName = "BillToLastName";
    public static final String BillToState = "BillToState";
    public static final String BillToZipCode = "BillToZipCode";
    public static final String bPreviouslyInvoked = "bPreviouslyInvoked";
    public static final String ChargeTransactionKey = "ChargeTransactionKey";
    public static final String ChargeType = "ChargeType";
    public static final String CreditCardExpirationDate = "CreditCardExpirationDate";
    public static final String CreditCardName = "CreditCardName";
    public static final String CreditCardNo = "CreditCardNo";
    public static final String CreditCardType = "CreditCardType";
    public static final String Currency = "Currency";
    public static final String CustomerAccountNo = "CustomerAccountNo";
    public static final String CustomerPONo = "CustomerPONo";
    public static final String DocumentType = "DocumentType";
    public static final String debitCardNo = "debitCardNo";
    public static final String EnterpriseCode = "EnterpriseCode";
    public static final String MerchantId = "MerchantId";
    public static final String OrderHeaderKey = "OrderHeaderKey";
    public static final String OrderNo = "OrderNo";
    public static final String PaymentReference1 = "PaymentReference1";
    public static final String PaymentReference2 = "PaymentReference2";
    public static final String PaymentReference3 = "PaymentReference3";
    public static final String PaymentReference4 = "PaymentReference4";
    public static final String PaymentReference6 = "PaymentReference6";
    public static final String PaymentReference7 = "PaymentReference7";
    public static final String PaymentReference8 = "PaymentReference8";
    public static final String PaymentReference9 = "PaymentReference9";
    public static final String bVoidTransaction = "bVoidTransaction";
    public static final String cashBackAmount = "cashBackAmount";
    public static final String chequeNo = "chequeNo";
    public static final String chequeReference = "chequeReference";
    public static final String entryType = "entryType";
    public static final String PaymentType = "PaymentType";
    public static final String RequestAmount = "RequestAmount";
    public static final String ShipToAddressLine1 = "ShipToAddressLine1";
    public static final String ShipToCity = "ShipToCity";
    public static final String ShipToCountry = "ShipToCountry";
    public static final String ShipToDayPhone = "ShipToDayPhone";
    public static final String ShipToEmailId = "ShipToEmailId";
    public static final String ShipToFirstName = "ShipToFirstName";
    public static final String ShipToId = "ShipToId";
    public static final String ShipTokey = "ShipTokey";
    public static final String ShipToLastName = "ShipToLastName";
    public static final String ShipToState = "ShipToState";
    public static final String ShipToZipCode = "ShipToZipCode";
    public static final String SvcNo = "SvcNo";
    public static final String TOTAL_CHARGED = "TotalCharged";
    public static final String TOTAL_AUTHORIZED = "TotalAuthorized";
    public static final String TOTAL_AMOUNT = "TotalAmount";
    public static final String A_INVOICED_AMOUNT = "InvoicedAmount";
    public static final String IS_FINAL_CAPTURE = "IsFinalCapture";
    public static final String PAYPAL = "PAYPAL";
    public static final String CAPTURE = "CHARGE";
    public static final String TRUE = "true";
    public static final String callForAuthorizationStatus = "callForAuthorizationStatus";
    public static final String firstName = "firstName";
    public static final String middleName = "middleName";
    public static final String lastName = "lastName";
    public static final String currentAuthorisationAmount = "currentAuthorisationAmount";
    public static final String currentAuthorizationCreditCardTransactions = "currentAuthorizationCreditCardTransactions";
    public static final String currentAuthorizationExpirationDate = "currentAuthorizationExpirationDate";
    public static final String paymentConfigOrganizationCode = "paymentConfigOrganizationCode";
    public static final String paymentKey = "paymentKey";
    public static final String secureAuthenticationCode = "secureAuthenticationCode";
    public static final String voidTransactionStatus = "voidTransactionStatus";
    public static final String AuthAVS = "AuthAVS";
    public static final String AuthorizationAmount = "AuthorizationAmount";
    public static final String AuthorizationExpirationDate = "AuthorizationExpirationDate";
    public static final String AuthReturnCode = "AuthReturnCode";
    public static final String AuthReturnFlag = "AuthReturnFlag";
    public static final String AuthReturnMessage = "AuthReturnMessage";
    public static final String AuthTime = "AuthTime";
    public static final String BPreviousInvocationSuccessful = "BPreviousInvocationSuccessful";
    public static final String HoldAgainstBook = "HoldAgainstBook";
    public static final String HoldOrderAndRaiseEvent = "HoldOrderAndRaiseEvent";
    public static final String HoldReason = "HoldReason";
    public static final String RequestID = "RequestID";
    public static final String RetryFlag = "RetryFlag";
    public static final String CVVAuthCode = "CVVAuthCode";
    public static final String SuspendPayment = "SuspendPayment";
    public static final String A_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String TranAmount = "TranAmount";
    public static final String TranRequestTime = "TranRequestTime";
    public static final String TranReturnCode = "TranReturnCode";
    public static final String TranReturnFlag = "TranReturnFlag";
    public static final String TranReturnMessage = "TranReturnMessage";
    public static final String TranType = "TranType";
    public static final String RequiresCallForAuthorization = "RequiresCallForAuthorization";
    public static final String HOLD_ORDER_RAISE_EVENT = "HoldOrderAndRaiseEvent";
    public static final String CHARGETYPE_CHARGE = "CHARGE";
    public static final String AsynchRequestProcess = "AsynchRequestProcess";
    public static final String A_EVENT_CODE = "eventCode";
    public static final String NULL_VALUE = "null";
    public static final String E_NOTIFICATION_REQUEST_ITEM = "NotificationRequestItem";
    public static final String ADYEN_MERCHANT_REFERENCE = "merchantReference";
    public static final String API_GET_CHARGE_TRANSACTION_LIST = "getChargeTransactionList";
    public static final String STATUS_CHECKED = "CHECKED";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String A_CREDIT_CARD_TYPE = "CreditCardType";
    public static final String CC_VISA = "VISA";
    public static final String ADYEN_VISA = "visa";
    public static final String CC_MASTER_CARD = "MASTER_CARD";
    public static final String ADYEN_MASTER_CARD = "mc";
    public static final String CC_DISCOVER = "DISCOVER";
    public static final String ADYEN_DISCOVER = "discover";
    public static final String CC_AMEX = "AMEX";
    public static final String ADYEN_AMEX = "amex";
    public static final String XPATH_TRANRETURNCODE = "ChargeTransactionDetails/ChargeTransactionDetail/CreditCardTransactions/CreditCardTransaction[@TranReturnCode='";
    public static final String GET_CHARGE_TRANSACTION_LIST_API_TEMP = "<ChargeTransactionDetails><ChargeTransactionDetail ChargeTransactionKey='' ForAsyncRequestIdentifier='' ChargeType='' OrderHeaderKey='' PaymentKey='' Status='' ><PaymentMethod PaymentMthod='' PaymentReference9=''/><CreditCardTransactions><CreditCardTransaction ChargeTransactionKey='' TranReturnCode=''/></CreditCardTransactions></ChargeTransactionDetail></ChargeTransactionDetails>";
    public static final String E_CHARGE_TRANSACTION_DETAIL = "ChargeTransactionDetail";
    public static final String A_FOR_ASYNC_REQ_IDENTIFIER = "ForAsyncRequestIdentifier";
    public static final String A_EXP = "Exp";
    public static final String A_OPERATOR = "Operator";
    public static final String A_COMPLEX_QUERY = "ComplexQuery";
    public static final String PaymentKey = "PaymentKey";

    public static final String VAL_CHARGE = "Charge";
    public static final String CHRG_CUST_DEBIT = "CUSTOMER_DEBIT";
    public static final String CATEGORY_CUSTOMER_APPEASE = "CUSTOMER_APPEASEMENT";


    public static final String CHECK_PROCESSING_HOLD = "Check Processing";
    public static final String IMAGE_REVIEW_HOLD = "Image Review Hold";
    public static final String PAYMENT_PROCESSING_HOLD = "Payment Processing";
    public static final String QRYTYPE = "QryType";
    public static final String EQ = "EQ";
    public static final String E_INBOX = "Inbox";
    public static final String A_CONSOLIDATE = "Consolidate";
    public static final String A_ACTIVE_FLAG = "ActiveFlag";
    public static final String A_CONSOLIDATION_WINDOW = "ConsolidationWindow";
    public static final String A_EXCEPTION_TYPE = "ExceptionType";
    public static final String E_CONSOLIDATION_TEMPLATE = "ConsolidationTemplate";
    public static final String FOREVER = "FOREVER";
    public static final String COMPLEX_QUERY = "ComplexQuery";
    public static final String LAST_HOLD_TYPE_DATE = "LastHoldTypeDate";
    public static final String DTC = "DTC";
    public static final String DTB = "DTB";


    public static final String VAL_US = "US";

    public static final String VAL_SHIP_TO = "SHIPTO";
    public static final String VAL_SHIPPING_CHARGE = "ShippingCharge";


    public static final String VAL_CARRIER_SERVICE_CODE = "CARRIER_SERVICE_CODE";

    //EDD calculations
    public static final String VAL_EXPEDITED = "Expedited";
    public static final String VAL_GROUND = "Ground";
    public static final String VAL_STANDARD_MAIL = "Standard Mail";
    public static final String VAL_STANDARD = "Standard";
    public static final String VAL_EXPRESS_DELIVERY = "Express Delivery";
	public static final String VAL_AUSPOST = "AUSPOST";
    public static final String VAL_EXPRESS = "Express";
    public static final String VAL_COMMERCIAL = "COMMERCIAL";
    public static final String VAL_RESIDENTIAL = "RESIDENTIAL";


    public static final String A_PARENT_ITEM_ID = "ParentItemID";
    public static final String A_COMPONENT_ITEM_ID = "ComponentItemID";
    public static final String E_COMPONENT = "Component";
    public static final String A_COMPONENT_UNIT_OF_MEASURE = "ComponentUnitOfMeasure";
    public static final String A_COMPONENT_ORGANIZATION_CODE = "ComponentOrganizationCode";
    public static final String A_COMPONENT_ITEM_KEY = "ComponentItemKey";
    public static final String A_TRACK_INVENTORY = "TrackInventory";
    public static final String A_KIT_QUANTITY = "KitQuantity";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String HTTP_METHOD = "HTTPMethod";
    public static final String URL = "URL";
    public static final String A_SUPPLY_TYPE = "SupplyType";


    public static final String A_DESCRIPTION = "Description";
    public static final String A_DETAIL_DESCRIPTION = "DetailDescription";

    //Product Feed Attributes added
    public static final String A_ITEM_GROUP_CODE = "ItemGroupCode";
    public static final String A_GLOBAL_ITEM_ID = "GlobalItemID";
    public static final String A_COST_CURRENCY = "CostCurrency";
    public static final String A_IS_RETURNABLE = "IsReturnable";
    public static final String A_MAX_ORDER_QUANTITY = "MaxOrderQuantity";
    public static final String A_MIN_ORDER_QUANTITY = "MinOrderQuantity";
    public static final String A_RETURN_WINDOW = "ReturnWindow";
    public static final String A_TAXABLE_FLAG = "TaxableFlag";
    public static final String A_IS_DELIVERY_ALLOWED = "IsDeliveryAllowed";
    public static final String A_IS_PICKUP_ALLOWED = "IsPickupAllowed";
    public static final String A_IS_SHIPPING_ALLOWED = "IsShippingAllowed";
    public static final String A_UNIT_COST = "UnitCost";
    public static final String A_UNIT_HEIGHT = "UnitHeight";
    public static final String A_UNIT_LENGTH = "UnitLength";
    public static final String A_UNIT_HEIGHT_UOM = "UnitHeightUOM";
    public static final String A_UNIT_LENGTH_UOM = "UnitLengthUOM";
    public static final String A_UNIT_WEIGHT = "UnitWeight";
    public static final String A_UNIT_WIDTH = "UnitWidth";
    public static final String A_UNIT_WIDTH_UOM = "UnitWidthUOM";
    public static final String A_IMAGE_LOCATION = "ImageLocation";
    public static final String A_LEAD_TIME = "LeadTime";
    public static final String A_TAX_PRODUCT_CODE = "TaxProductCode";
    public static final String E_INVENTORY_PARAMETERS = "InventoryParameters";
    public static final String E_CLASSIFICATION_CODES = "ClassificationCodes";
    public static final String A_ITEM_KEY = "ItemKey";
    public static final String A_RESERVATION_MANDATORY = "ReservationMandatory";

    //Product Feed Attributes End
    //public static final String CONTENT_TYPE="Content-Type";
    public static final String HTTP_POST_REQUEST = "POST";
    public static final String HTTP_HEADER_AUTHORIZATION = "Authorization";
    public static final String HTTP_HEADER_AUTH_BASIC = "Basic ";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String UTF_8_STANDARD = "utf-8";
    public static final String TOKEN_NAME = "TokenName";
    public static final String TOKEN_VALUE = "TokenValue";
    public static final String TOKEN_NAME_AUTH_TOKEN = "AuthToken";
    public static final String HEADER_NAME_BEARER = "Bearer ";
    public static final String APPLICATION_JSON = "application/json";

    public static final String VAL_STATUS_READY_TO_SHIP_STAGED = "1300.002";
    public static final String VAL_TRANSACTION_READY_TO_SHIP_STAGED = "READY_TO_SHIP_STAGED.0001.ex";


    public static final String ProcessCollectionCreditCard = "CrocsProcessCollectionCreditCard";
    public static final String CROCS_PAYMENT_FAILURE_ALERT = "Payment Decline Alert";

    //EOMS-574 : Create Customer Definition
    public static final String XPATH_CUSTOMER_ID = "/CustomerList/Customer/@CustomerID";
    public static final String CROCS_MANAGE_CUST_SYNC_SERV = "CrocsManageCustomerSyncService";
    //EOMS-812 : AVS
    public static final String CROCS_AVS_URL = "CROCS_AVS_URL";
    public static final String CROCS_AVS_KEY = "CROCS_AVS_KEY";
    //EOMS-1056
    public static final String VAL_ACTION_CANCEL = "CANCEL";
    public static final String SFCC_CANCELLATION_SUCCESS_MESSAGE = "Order cancelled successfully";
    public static final String SFCC_CANCELLATION_FAILURE_MESSAGE = "Order cannot be cancelled because remorse period is passed";
    public static final String SFCC_REASON_CODE_VALE = "CustomerRequestedWeb";
    public static final String GET_ORDERLIST_TEMPLATE = "<OrderList >\r\n" + "   "
            + " <Order OrderHeaderKey=\"\">\r\n"
            + "        <OrderHoldTypes>\r\n"
            + "            <OrderHoldType HoldType=\"\"   Status=\"\" >\r\n"
            + "        </OrderHoldType>\r\n" + "        "
            + "</OrderHoldTypes>\r\n" + "    </Order>\r\n"
            + "</OrderList>";
    public static final String NOTE_TEXT = "NoteText";
    public static final String NOTE_TEXT_VALUE = "The entire order was canceled due to reason: ";
    public static final String A_NOTES = "Notes";
    public static final String A_NOTE = "Note";
    public static final String XPATH_ORDER_ELEMENT = "/OrderList/Order";

    //EOMS-1009 : Item sync to IV
    public static final String IV_TENANT_ID = "IV_TENANT_ID";

    // EOMS-555 : Create Store
    public static final String CROCS_MANAGE_ORG_HIERCHY_API = "manageOrganizationHierarchy";
    public static final String CROCS_POST_ORG_MSG_FOR_IV_SYNC_SERV = "CrocsPostOrgMsgForIVToSyncService";
    public static final String CROCS_CHANGE_CALENDAR_API = "changeCalendar";
    public static final String CROCS_CREATE_CALENDAR_API = "createCalendar";
    public static final String CROCS_GET_CALENDAR_LIST_API = "getCalendarList";

    //EOMS-582 : Order detail lookup
    public static final String A_ERROR = "Error";
    public static final String A_ERROR_DESCRIPTION = "ErrorDescription";
    public static final String A_ERROR_CODE = "ErrorCode";

    public static final String V_DESCRIPTION = "Mandatory attributes are missing in input.";
    public static final String V_CODE = "YFS10460";

    //EOMS-926 : Store sync to IV
    public static final String E_ORGANIZATION = "Organization";
    public static final String E_NODE = "Node";
    public static final String E_CORPORATE_PERSON_INFO = "CorporatePersonInfo";

    //EOMS-1070 : Prorate the line charges (Disount)
    public static final String VAL_DISCOUNT = "Discount";

    //EOMS-1313 order history lookup
    public static final String V_READ_FROM_HISTORY = "B";
    public static final String V_DRAFT_ORDER_FLAG = "N";
    public static final String A_STATUS_ORDER_MESSAGE = "The request was processed successfully, but no orders were found matching the specified criteria.";
    public static final String A_STATUS_ORDER_CODE = "NO_ORDERS_FOUND";

    //EOMS-690 order priority calculation
    public static final String VAL_PRIORITY_100 = "100";
    public static final String VAL_PRIORITY_10 = "10";
    public static final String COMMON_CODE_ORDER_PRIORITY_PAYMENTS = "ORDR_PRIOTY_PAYMENT";
    public static final String COMMON_CODE_ORDER_PRIORITY_ORDER_TYPE = "ORDR_PRIOTY_ORDR_TYP";
    public static final String FORTER_SITE_ID1 = "x-forter-siteid";
    public static final String PRE_AUTHERIZATION = "PRE_AUTHORIZATION";
    public static final String DELIVERY_METHOD = "Economy";
    public static final String DELIVERYTYPE = "PHYSICAL";
    public static final String ORDER_SEGMENT = "orderSegment";
    public static final String CROCS_NA = "Crocs NA";
    public static final String MERCHANT_ID = "merchantId";
    public static final String MERCHANT_NAME = "merchantName";
    public static final String MERCHANT = "merchant";
    public static final String ADDITIONAL_IDENTIFIERS = "additionalIdentifiers";
    public static final String ORDERTYPE = "WEB";
    public static final String CARDTYPE = "CREDIT";
    public static final String TANGEABLE = "TANGIBLE";
    public static final String BASICITEMDATA_F = "basicItemData";
    public static final String CARTITEMS_F = "cartItems";
    public static final String PAYMENT_F = "payment";
    public static final String V_US = "US";
    public static final String V_CROCS_US = "crocs_us";
    public static final String V_CA = "CA";
    public static final String V_CROCS_CA = "crocs_ca";


    //EOMS-645 ADYEN

    public static final String ADYEN_CAPTURE_URL = "ADYEN_CAPTURE_URL";
    public static final boolean A_TRUE = true;
    public static final boolean A_FALSE = false;
    public static final String ADYEN_MERCHANT_ACCOUNT = "merchantAccount";
    public static final String ADYEN_STATUS_RECEIVED = "received";
    public static final String RELEASED = "Released";
    public static final String PARTIALLY_RELEASED = "Partially Released";

    //EOMS-1444 Get Item Price From SFCC
    public static final String SFCC_API_URL_TO_FETCH_ITEM_PRICE = "SFCC_ITEM_PRICE_URL";
    public static final String SFCC_CLIENT_ID_TO_FETCH_ITEM_PRICE = "SFCC_ITEM_PRICE_CLIENT_ID";
    public static final String A_STATUS_CODE_SUCCESS = "200";
    public static final String CROCS_GET_ITEM_PRICE_FROM_SFCC_SYNC_SERV = "CrocsGetItemPriceFromSFCCSyncServ";

    //EOMS-672 ,EOMS-645, EOMS-648
    public static final String STR_CASH_APP = "Cash App";
    public static final String STR_AFTER_PAY = "After Pay";
    public static final String STR_BODY = "body";
    public static final String STR_ADYEN_REQUEST = "Request";
    public static final String STR_ADYEN_RESPONSE = "Response";
    public static final String STR_ADEYN_REQUEST_PAYLOAD = "AdyenRequestPayload";
    public static final String STR_ADEYN_RESPONSE_PAYLOAD = "AdyenResponsePayload";
    public static final String STR_URL_CAPTURE = "/captures";
    public static final String STR_DB_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
    public static final String CROCS_US = "CROCS_US";
    public static final String CROCS_CA = "CROCS_CA";
    public static final String CROCS_KR = "CROCS_KR";
    public static final String CROCS_MY = "CROCS_MY";
    public static final String TRANSACTION_ID_US = "Crocs_US_Shipment_Packed.0001.ex";
    public static final String TRANSACTION_ID_CA = "Crocs_CA_Shipment_Packed.0001.ex";
    public static final String BASE_DROP_STATUS = "1100.01";

    //EOMS-587 Fraud validation
    public static final String V2_DESCRIPTION = "Fruad validation failed.";
    public static final String V2_CODE = "YFS10460";
    public static final String DELIVERY_TYPE = "PHYSICAL";
    public static final String CUSTOMER_IP = "127.0.0.1";
    public static final String USERAGENT = "Call Center";
    public static final String PHONE = "PHONE";
    public static final String AUTH_STEP = "PRE_AUTHORIZATION";
    public static final String FORTER_SITE_ID = "FORTER_SITE_ID";
    public static final String FORTER_API_KEY = "FORTER_API_KEY";
    public static final String FORTER_API = "FORTER_API";
    public static final String API_VERSION = "api-version";
    public static final String API_VERSION_V = "2.4";
    public static final String UTF_8 = "UTF-8";
    public static final String A_DELIVEY_CODE = "DeliveryCode";
    public static final String A_AUTHORIZATION = "Authorization";
    public static final String BASIC = "Basic ";
    public static final String AFTER_PAY = "Afterpay";
    public static final String GIFT_CARD = "Gift Card";
    public static final String V_SUCCESS = "SUCCESS";
    public static final String V_FAILED = "FAILED";
    public static final String STATUS = "status";
    public static final String A_STATUS = "Status";
    public static final String ACTION = "action";
    public static final String MESSAGE = "message";
    public static final String APPROVE = "approve";
    public static final String SUCCESS = "success";

    //EOMS-660 Givex
    public static final String STR_GIVEX = "Givex";

    //EOMS-710 related changes
    public static final String STATUS_RELEASE = "3200";
    public static final String STATUS_INCLUDED_IN_SHIPMENT = "3350";
    public static final String STATUS_SCHEDULE = "1500";

    // EOMS-687
    public static final String E_CHARGE_TRANSACTION_DETAILS = "ChargeTransactionDetails";
    public static final String F_CREDIT_CURRENCY = "creditCurrency";
    public static final String F_MERCHANT_PAYMENT_ID = "merchantPaymentId";
    public static final String F_GIFT_CARD = "giftCard";
    public static final String A_GIVEX = "Givex";
    public static final String F_DIGITAL_WALLET = "digitalWallet";
    public static final String F_UNDERLYING_PAYMENT_METHOD = "underlyingPaymentMethod";
    public static final String F_UNDERLYING_PAYMENT_METHOD_TYPE = "underlyingPaymentMethodType";
    public static final String F_UNDERLYING_PAYMENT_METHOD_TYPE_VALUE = "UNKNOWN";
    public static final String F_FREE_TEXT_DIGITAL_WALLET_NAME = "freeTextDigitalWalletName";
    public static final String F_FREE_TEXT_DIGITAL_WALLET_NAME_TYPE = "CASHAPP";
    public static final String F_DIGITAL_WALLET_PAYER_ID = "digitalWalletPayerId";
    public static final String F_DIGITAL_WALLET_NAME = "digitalWalletName";
    public static final String F_DIGITAL_WALLET_NAME_TYPE = "OTHER";
    public static final String F_PAYER_ID = "payerId";
    public static final String F_PAYER_EMAIL = "payerEmail";
    public static final String F_AUTHORIZATION_ID = "authorizationId";
    public static final String F_PAYPAL = "paypal";
    public static final String F_PAYPAL_STANDARD = "Paypal Standard";
    public static final String P_PAYPAL = "Paypal";
    public static final String F_APPLE_PAY = "applePay";
    public static final String P_APPLE_PAY = "Apple Pay";
    public static final String F_SERVICE_NAME = "serviceName";
    public static final String F_PAYMENT_ID = "paymentId";
    public static final String F_INSTALLMENT_SERVICE = "installmentService";
    public static final String P_CREDIT_CARD = "Credit Card";
    public static final String A_IS_FRAUD_ORDER = "IsFraudOrder";
    public static final String A_ENTRY_TYPE = "EntryType";
    public static final String ENTRY_TYPE_CALL_CENTER = "Call Center";
    public static final String A_PAYMENT_STATUS = "PaymentStatus";
    public static final String POST_AUTHERIZATION = "POST_AUTHORIZATION";
    public static final String XPATH_PERSONINFOSHIPTO_ELEMENT = "/Order/OrderLines/OrderLine/PersonInfoShipTo";
    public static final String XPATH_ORDER_PERSONINFOSHIPTO_ELEMENT = "/Order/PersonInfoShipTo";

    //EOMS 1724 - Order detail look up
    public static final String A_CUSTOMER_NO = "ExtnCustId";
    public static final String E_EXTN = "Extn";
    public static final String V_ERROR_DESCRIPTION = "Customer number does not match the order number";
    public static final String V_ERROR_CODE = "INVALID_CUSTOMER_NO";
    public static final String VE_ERROR_DESCRIPTION = "Email does not match the order number";
    public static final String VE_ERROR_CODE = "INVALID_EMAIL";

    //Adyen Properties
    public static final String A_ADYEN_PAYMENT_CAPTURE_URL = "ADYEN_PAYMENT_CAPTURE_URL";
    public static final String A_ADYEN_X_API_KEY1 = "ADYEN_X-API-KEY1";
    public static final String A_ADYEN_X_API_KEY2 = "ADYEN_X-API-KEY2";
    public static final String A_ADYEN_X_API_KEY3 = "ADYEN_X-API-KEY3";
    public static final String A_ADYEN_X_API_KEY4 = "ADYEN_X-API-KEY4";
    //EOMS-751
    public static final String STR_URL_REFUND = "/refunds";

    //EOMS-2025
    public static final String STR_CANCELLED = "Cancelled";
    public static final String STR_STATUS_CANCELLED = "9000";
    public static final String STR_PAYPAL = "Paypal";
    public static final String STR_AUTH_VOID_ON_CANCEL = "AuthVoidOnCancel";
    public static final String STR_VOID_CANCELS = "/cancels";

    //EOMS-698
    public static final String ACTION_CANCEL_NON_SHIPPED_QUANTITY_Y = "CANCEL_NON_SHIPPED_QUANTITY_Y";
    public static final String ACTION_BACKORDERED_NON_SHIPPED_QUANTITY_N = "BACKORDERED_NON_SHIPPED_QUANTITY_N";
    //EOMS 725
    public static final String A_RETURN_ORDER_DOCUMENT_TYPE = "0003";
    public static final String A_SALES_ORDER_DOCUMENT_TYPE = "0001";
    public static final String CUSTOM_US_NO = "OCUS";
    public static final String CUSTOM_CA_NO = "OCCA";
    public static final String SEQ_CALL_CENTER_ORDER_NO = "SEQ_CALL_CENTER_ORDER_NO";
    public static final String SEQ_YFS_ORDER_NO = "SEQ_YFS_ORDER_NO";

    //EOMS-747 Return Receipt implementation
    public static final String A_OHIO_DC_VALUE = "1005";
    public static final String A_UPS_SCS_VALUE = "1032";

    //EOMS-1492
    public static final String STR_AMP = "&amp;";
    public static final String STR_AND = "&";
    public static final String STR_TEXT_1 = "Text1";
    public static final String STR_TEXT_2 = "Text2";
    public static final String STR_TEXT_3 = "Text3";
    public static final String STR_CROCS_GET_ORDERLIST_NARVAR = "CrocsGetOrderListForNarvar";
    public static final String STR_XPATH_ITEM_ID = "Item/@ItemID";
    public static final String STR_XPATH_DERIVED_ORDER_NO = "DerivedFrom/@OrderNo";
    public static final String STR_XPATH_ORDERLIST_PERSON_INFO_BILL_TO = "/OrderList/Order/PersonInfoBillTo";
    public static final String STR_XPATH_ORDERLIST_ENTERPRISE_CODE = "/OrderList/Order/@EnterpriseCode";
    public static final String STR_XPATH_ORDERLIST_CUSTOMER_PHNO = "/OrderList/Order/@CustomerPhoneNo";
    public static final String STR_US_SHIP_NODE = "1005";
    public static final String STR_CA_SHIP_NODE = "1032";

		//Vertex Attributes
		public static final String A_VERTEX_URL = "VERTEX_URL";


	  //Vertex Properties
		public static final String V_INVOICE_REQUEST="InvoiceRequest";
		public static final String V_SOAP_ENV="soapenv";
		public static final String V_SOAP_ENV_URL="http://schemas.xmlsoap.org/soap/envelope/";
		public static final String V_URN_URL="urn:vertexinc:o-series:tps:9:0";
		public static final String V_URN="urn";

		//Vertex Envelope
		public static final String V_VERTEX_ENVELOPE = "VertexEnvelope";
	    public static final String V_LOGIN = "Login";
	    public static final String V_QUOTATION_REQUEST = "QuotationRequest";

	    // Login Elements
	    public static final String V_USERNAME = "UserName";
	    public static final String V_PASSWORD = "Password";
	    public static final String V_TRUSTED_ID = "TrustedId";

	    // QuotationRequest Attributes
	    public static final String V_DOCUMENT_DATE = "documentDate";
	    public static final String V_TRANSACTION_TYPE = "transactionType";
	    public static final String V_TRANSACTION_ID = "transactionId";
	    public static final String V_DOCUMENT_NUMBER = "documentNumber";

	    // QuotationRequest Elements
	    public static final String V_CURRENCY = "currency";
	    public static final String V_ORIGINAL_CURRENCY = "originalCurrency";

	    // Seller Elements
	    public static final String V_SELLER = "Seller";
	    public static final String V_COMPANY = "Company";
	    public static final String V_PHYSICAL_ORIGIN = "PhysicalOrigin";
	    public static final String V_ADMINISTRATIVE_ORIGIN = "AdministrativeOrigin";

	    // Address Elements
	    public static final String V_STREET_ADDRESS1 = "StreetAddress1";
	    public static final String V_STREET_ADDRESS2 = "StreetAddress2";
	    public static final String V_CITY = "City";
	    public static final String V_MAIN_DIVISION = "MainDivision";
	    public static final String V_SUB_DIVISION = "SubDivision";
	    public static final String V_POSTAL_CODE = "PostalCode";
	    public static final String V_COUNTRY = "Country";
	    public static final String V_CURRENCY_CONVERSION = "CurrencyConversion";

	    // Customer Elements
	    public static final String V_CUSTOMER = "Customer";
	    public static final String V_DESTINATION = "Destination";
	    public static final String V_IS_TAX_EXEMPT = "IsTaxExempt";
	    public static final String V_EXEMPTION_REASON_CODE = "ExemptionReasonCode";

	    // LineItem Elements
	    public static final String V_LINE_ITEM = "LineItem";
	    public static final String V_LINE_ITEM_NUMBER = "lineItemNumber";
	    public static final String V_PRODUCT = "Product";
	    public static final String V_PRODUCT_CLASS = "productClass";
	    public static final String V_QUANTITY = "Quantity";
	    public static final String V_UNIT_OF_MEASURE = "unitOfMeasure";
	    public static final String V_EXTENDED_PRICE = "ExtendedPrice";

	  // Vertex credentials
	    public static final String V_USER = "VERTEX_USER";
	    public static final String V_PASSWORD_KEY = "VERTEX_PASSWORD";
	    public static final String V_TRUSTED_KEY = "VERTEX_TRUST_KEY";

	  // Additional LineItem Elements
	    public static final String A_ORDERED_QTY = "OrderedQty";
	    public static final String A_ACTUAL_PRICING_QTY = "ActualPricingQty";
	    public static final String A_ITEM_DETAILS = "ItemDetails";
	    public static final String A_CLASSIFICATION_CODES = "ClassificationCodes";
	    public static final String A_ITEM_ID = "ItemID";
	    public static final String A_LINE_OVERALL_TOTALS = "LineOverallTotals";



	    // Tax Element
	    public static final String A_ORGANIZATION_KEY= "OrganizationKey";
	    public static final String A_GET_ORGANIZATION_LIST= "getOrganizationList";
	    public static final String V_VERTEX_QUOTATION_RESPONSE="VertexQuotationResponse";
	    public static final String V_VERTEX_INVOICE_RESPONSE="VertexInvoiceResponse";
	    public static final String V_TAXES="Taxes";
	    public static final String V_TAX_RESULT="taxResult";
	    public static final String V_TAXABLE="TAXABLE";
	    public static final String V_EFFECTIVE_RATE="EffectiveRate";
	    public static final String V_TOTAL_TAX="TotalTax";

	    //Vertex Response
	    public static final String V_IMPOSITION_GST_HST = "GST/HST";
	    public static final String V_IMPOSITION_PST = "Provincial Sales Tax (PST)";
	    public static final String V_IMPOSITION_CA_SALES_TAX = "";
	    public static final String A_SHIPPING_GST_HST_TAX = "ShippingGstTax";
	    public static final String A_SHIPPING_PST_TAX = "ShippingPstTax";
	    public static final String A_GST_HST_TAX = "CAGstTax";
	    public static final String A_PST_TAX = "CAPstTax";
	    public static final String A_SALES_TAX = "SalesTax";
		public static final String A_TAX_GET_ORDER_LIST = "VERTEXGETORDERLIST";
		public static final String A_SHIPPING_TAX_CODE="FR020800";
		public static final String A_SHIPNODE_US="1005";
		public static final String A_SHIPNODE_CA="1032";
		public static final String V_CROCS="Crocs";
		public static final String V_SALE="SALE";
		public static final String V_FALSE="false";
		public static final String V_SHIPPING="Shipping";
		public static final String V_EA="EA";
		public static final String A_CROCS_GET_ORDER_LIST="CrocsGetOrderList";
		public static final String A_CROCS_QUOTATION_REQUEST_TO_VERTEX="CrocsQuotationRequesttoVertex";
		public static final String A_CROCS_GET_ORGANIZATION_LIST ="CrocsGetOrganizationList";

		public static final String V_SOAP_ENV_BODY="soapenv:Body";
		public static final String V_QUOTATION_RESPONSE="QuotationResponse";
		public static final String V_IMPOSITION="Imposition";
		public static final String V_CALCULATED_TAX="CalculatedTax";
	    public static final String A_SHIPPING_TAX = "ShippingTax";

		//EOMS 2896
		public static final int I_400 = 400;

		//EOMS-2901
		public static final String CHARGE_CATEGORY_SHIPPING_CHARGE = "ShippingCharge";

		//EOMS-1474
  	  	public static final String STR_XPATH_EXTN_COLLAB_SKU = "Extn/@ExtnCollabSKU";
  	  	public static final String STR_CROCS_GET_ITEMLIST_SERVICE = "CrocsGetItemList";

  	  	// EOMS-2561
  	  	public static final String VAL_CROCS_SFCC_CARRIER = "CROCS_SFCC_CARRIER";
		public static final String VAL_ECONOMY = "Economy";
		public static final String XPATH_CODE_SHORT_DESCRIPTION = "/CommonCodeList/CommonCode/@CodeShortDescription";

    public static final String V_SHIPNODE = "shipNode";
    public static final String V_ENABLE_FULFILLMENT_OPTION = "enableFulfillmentOption";
    public static final String V_ITEM_ID = "itemId";
    public static final String V_REFERENCE= "reference";
    public static final String V_REFERENCE_TYPE= "referenceType";
    public static final String V_ETA= "eta";
    public static final String V_PLAN= "PLAN";
    public static final String V_OUTPUT= "Output";

//Sales and Return Order Migration
    public static final String A_ENTERED_BY = "EnteredBy";
    public static final String V_IMPORT_ORDER = "ImportOrder";
    public static final String A_CONDITION_VARIABLE_1 = "ConditionVariable1";
    public static final String V_MIGRATION = "Migration";
    public static final String A_SO_MIGRATION_PIPELINE_PROPERTY = "SALES_ORDER_MIGRATION_PIPELINE_KEY";
    public static final String A_SO_MIGRATION_CA_PIPELINE_PROPERTY = "SALES_ORDER_MIGRATION_CA_PIPELINE_KEY";
	public static final String A_SO_MIGRATION_CROCS_AU_PIPELINE_PROPERTY = "SALES_ORDER_MIGRATION_CROCS_AU_PIPELINE_KEY";
    public static final String A_RO_MIGRATION_PIPELINE_PROPERTY = "RETURN_ORDER_MIGRATION_PIPELINE_KEY";
    public static final String A_RO_MIGRATION_CA_PIPELINE_PROPERTY = "RETURN_ORDER_MIGRATION_CA_PIPELINE_KEY";
	public static final String A_RO_MIGRATION_CROCS_AU_PIPELINE_PROPERTY = "RETURN_ORDER_MIGRATION_CROCS_AU_PIPELINE_KEY";
	
    public static final String A_PIPELINE_KEY = "PipelineKey";
    public static final String A_PIPELINE_ID = "PipelineId";
    public static final String A_PROCESS_PAYMENTS_ON_RETURN_ORDER = "ProcessPaymentOnReturnOrder";
    public static final String STR_MIGRATION_SUFFIX = "MIG";
    public static final String E_TO_ADDRESS = "ToAddress";
    public static final String A_DERIVED_FROM = "DerivedFrom";
    public static final String A_ORDER_STATUSES = "OrderStatuses";
    public static final String A_ORDER_STATUS = "OrderStatus";
    public static final String E_SCHEDULE = "Schedule";
    public static final String A_SO_SHIPMENT_MIGRATION_PIPELINE_PROPERTY = "SALES_ORDER_SHIPMENT_MIGRATION_PIPELINE_KEY";
    public static final String A_SO_SHIPMENT_MIGRATION_CA_PIPELINE_PROPERTY = "SALES_ORDER_SHIPMENT_MIGRATION_CA_PIPELINE_KEY";
	public static final String A_SO_SHIPMENT_MIGRATION_CROCS_AU_PIPELINE_PROPERTY = "SALES_ORDER_SHIPMENT_MIGRATION_CROCS_AU_PIPELINE_KEY";
	
	//EOMS-1485
  	public static final String STATUS_SHIPPED="3700";
  	public static final String STATUS_RETURN_CREATED="3700.01";
  	public static final String F_EVENT_TIME="eventTime";
  	public static final String F_UPDATED_TOTAL_AMOUNT="updatedTotalAmount";
  	public static final String F_UPDATED_STATUS="updatedStatus";
  	public static final String F_UPDATED_MERCHANT_STATUS="updatedMerchantStatus";
  	public static final String FORTER_STATUS_API="https://api.forter-secure.com/v2/status/";
    public static final String XPATH_MAX_ORDER_STATUS="/OrderList/Order/@MaxOrderStatus";
  	public static final String XPATH_MIN_ORDER_STATUS="/OrderList/Order/@MinOrderStatus";
  	public static final String XPATH_MAX_ORDER_STATUS_DESC="MaxOrderStatusDesc";
  	public static final String XPATH_ORDER_NO="/Order/@OrderNo";
  	public static final String XPATH_DERIVED_FROM_ORDER_ORDER_NO ="/Order/OrderLines/OrderLine/DerivedFromOrder/@OrderNo";
  	public static final String XPATH_CONFIRM_SHIPEMNT_ORDER_NO="/Shipment/ShipmentLines/ShipmentLine/@OrderNo";
  	public static final String XPATH_PRICE_INFO_CURRENCY="/OrderList/Order/PriceInfo/@Currency";
  	public static final String XPATH_PRICE_INFO_TOTAL_AMOUNT="/OrderList/Order/PriceInfo/@TotalAmount";
  	public static final String STR_COMPLETED="COMPLETED";
  	public static final String STR_CANCELED_BY_CUSTOMER="CANCELED_BY_CUSTOMER";
  	public static final String STR_RETURNED="RETURNED";
  	public static final String STR_ORDERS_F="orders/";
  	public static final String STR_STATUS_F="status/";
  	public static final String STR_NOT_REVIEWED_F="not reviewed";
    public static final String XPATH_ELE_CHARGETRANSACTIONDETAIL="/ChargeTransactionDetails/ChargeTransactionDetail";
  
    //Vertex
    public static final String A_CROCS_TAX_CALL_TO_VERTEX="CrocsTaxCallToVertex";
    public static final String A_CROCS_INVOICE_REQUEST_FAILURE_ALERT_FOR_VERTEX = "CrocsInvoiceRequestFailureAlertforVertex";
    public static final String A_CROCS_QUOTATION_REQUEST_FAILURE_ALERT_FOR_VERTEX = "CrocsQuotationRequestFailureAlertforVertex";
	
    //EOMS-1495 APTOS Return
    public static final String STR_IN_STORE_PAY = "INSTOREPAY";
    public static final String A_ENTRY_TYPE_STORE = "STORE";
    public static final String CROCS_PUT_MSG_TO_RECEIVE_RETURN_Q_SYNC_SERV= "CrocsPutMsgToReceiveReturnOMSQSyncServ";
    public static final String CROCS_GET_ORDER_LIST_FOR_APTOS_SYNC_SERV="CrocsGetOrderListForAPTOS";
	
	//EOMS- 1488
	public static final String SER_CROCS_APTOS_ORDER_DETAILS_UPDTAE="CrocsSendAptosOrderDetailsUpdateQ";
	public static final String A_EXTENDED_SHIPPING_PROTECTION ="ExtendShipProtection";
	public static final String A_CROCS_PROMOTION_HDR_DISCOUNT ="PromotionHdrDiscount";
	public static final String A_CROCS_PROMOTION_DISCOUNT ="PromotionHdrDiscount";
	
	//EOMS - 2568
	   public static final String A_CROCS_DECRYPT_KEY ="DECRYPT_KEY";
	    public static final String A_CUSTOMER_IP ="customerIP";
	    public static final String A_FORTER_TOKEN_COOKIE = "forterTokenCookie";
	    public static final String A_MERCHANT_DOMAIN ="merchantDomain";
	    public static final String A_USER_AGENT ="userAgent";
	    public static final String A_AES_CBC_PKCSSPADDING = "AES/CBC/PKCS5Padding";
	    public static final String A_AES_CBC_NOPADDING="AES/GCM/NoPadding";
	    public static final String A_EMPLOYEE="Employee";
	    public static final String A_REGULAR="Regular";
	    public static final String A_FORTER_STORAGE="ExtnForterStorage";	
	    public static final String A_PROMOTION_DISCOUNT ="PromotionDiscount";
		public static final String VAL_FORTER_SALT = "FORTER_SALT";
		public static final String FORTER_DECRYPT_CODE = "DECRYPT_CODE";
		public static final String AES = "AES";
		public static final String A_AES_TRANSFORMATION = "AES_TRANSFORMATION";
		public static final String IV_SIZE = "IV_SIZE";
		public static final String AES_KEY_SIZE = "AES_KEY_SIZE";
		public static final String CREATE_ORDER_VIA_SERVICE = "CreateOrderViaIntService";
		public static final String CROCS_AVS_AQI = "CROCS_AVS_AQI";

	//EOMS - 602
	public static final String VAL_SHIPPED = "Shipped";
	public static final String VAL_DELAYED = "Delayed";
	public static final String VAL_PARTIAL_SHIPMENT = "PartialShipment";
	public static final String STR_DATE_FORMAT_MM_DD_YYYY = "MM/dd/yyyy";
	
	 //EOMS-3382
	 public static final String STR_GOOGLE_PAY = "paywithgoogle";
     public static final String STR_AMAZON_PAY = "amazonpay";
     
 	//EOMS-2922
	public static final String ERROR_CODE_RECEIVE_RETURN_ERROR = "RECEIVE_RETURN_ERROR";
	public static final String ERROR_DESC_FOR_RECEIVE_RETURN= "Exception while receiving return Quantity in OMS";
	public static final String SECRET_KEY="E_SECRET_KEY";

	//EOMS-3123
	public static final String A_EVENT_AUTHORISATION= "AUTHORISATION";
	public static final String A_PAYMENT_METHOD = "PaymentMethod";
	public static final String A_AFTER_PAY_TOUCH = "afterpaytouch";
	public static final String A_AMAZON_PAY = "amazonpay";
	public static final String A_PAY_WITH_GOOGLE = "paywithgoogle";
	public static final String A_CROCS_US = "CrocsUS";
	public static final String A_CROCS_CA = "CrocsCA";
	public static final String A_ACCEPT = "ACCEPT";
	public static final String A_RETRY = "RETRY";
	public static final String A_FRAUD_HOLD = "FRAUD_HOLD";
	public static final String F_AMAZON_PAY = "AMAZONPAY";
	public static final String F_GATEWAY_NAME = "gatewayName";
	public static final String F_ADYEN = "ADYEN";
	public static final String F_GATEWAY_TRANSACTION_ID = "gatewayTransactionId";
	public static final String F_PAYMENT_GATEWAY_DATA = "paymentGatewayData";
	public static final String F_PROCESSOR_NAME = "processorName";
	public static final String F_PROCESSOR_TRANSACTION_ID = "processorTransactionId";
	public static final String F_PAYMENT_PROCESSOR_DATA = "paymentProcessorData";
	public static final String F_ANDROID_PAY = "androidPay";
	public static final String FRAUD_RESPONSE_SUCCESS = "Fraud check approved and resolving the hold";
	public static final String CROCS_CHANGE_ORDER_SERV = "CrocsChangeOrderService";
	public static final String CROCS_RECORD_EXTERNAL_CHARGES_SERV = "CrocsRecordExternalChargesServ";
	public static final String CROCS_GET_ORDER_LIST_FOR_FORTER = "CrocsGetOrderListForForter";
	public static final String CROCS_GET_COMMON_CODE_LIST_SERV = "CrocsGetCommonCodeList";
	public static final String STR_CROCS_POST_AUTH_PAYMENTS = "CROCS_P_AUTH_PAYMENT";
	public static final String STR_CROCS_AUTH_EXP_DATE = "CROCS_AUTH_EXP_DATE";
	public static final String STR_CROCS_AUTH_EXP_PAYMENTS = "CROCS_AUTH_PAYMENT";
	public static final String STR_XPATH_MONITOR_ORDER_NO = "/MonitorConsolidation/Order/@OrderNo";
	public static final String STR_XPATH_MONITOR_ORDER_HEADER_KEY = "/MonitorConsolidation/Order/@OrderHeaderKey";
	public static final String STR_AUTHORIZATION_DESC = "Order having authorization amount greater than zero";
	public static final String STR_PAYMENT_ERROR_HOLD = "PAYMENT_ERROR_HOLD";
	public static final String STR_FREE_TEXT_DIGITAL_WALLET_NAME = "freeTextDigitalWalletName";
	
	public static final String STR_PAY_PAL = "Paypal";
	public static final String STR_BRAIN_TREE = "BRAINTREE_PAYPAL";
	public static final String STR_BRAIN_TREE_HOLD="BRAIN_TREE";
	public static final String STR_BRAIN_TREE_DESC="Order created with Braintree payment gateway";
	public static final String STR_F="F";
	
	//EOMS-3134
	public static final String VAL_SUB_LINE_NO_1 ="1";
	
	//EOMS-734
	public static final String N_RMA_NUMBER="rma_number";
	public static final String N_CARRIER="carrier";
	public static final String N_ORDER_NUMBER="order_number";
	public static final String N_LOCALE ="locale";
	public static final String N_RETURN_REASON_CODE="return_reason_code";
	public static final String N_CUSTOMER_COMMENT="customer_comment";
	public static final String N_ORDER_ITEMS="order_items";
	public static final String N_RETURNS="returns/";
	public static final String N_TYPE_VAL="return";
	public static final String CROCS_NARVAR_USERNAME="CROCS_NARVAR_USERNAME";
	public static final String CROCS_NARVAR_PASSWORD="CROCS_NARVAR_PASSWORD";
	public static final String N_TRACKING_NUMBER="tracking_number";
	public static final String N_TRACKING_URL="tracking_url";
	public static final String N_LABEL_URL="label_url";
	public static final String N_RETURN_DETAILS="return_details";
	public static final String STR_LABEL_URL="Label URL";
	public static final String STR_RETURN_REASON="ReturnReason";
	public static final String STR_RETURN_REASON_LONG_DESC="ReturnReasonLongDesc";
	public static final String N_NAME="name";
	public static final String EXTN_CUSTOMER_LOCALE="ExtnCustomerLocale";
	public static final String STR_DEFAULT="default";
	public static final String STR_EN_US="en_US";


  //EOMS-3126
  public static final String HTTP_POST_PUT = "PUT";
  
  
  //EOMS - 2988
   public static final String V_SHIPPING_DISCOUNT = "ShippingDiscount";
	public static final String A_1000 = "1000";
	public static final String A_1030 = "1030";
	public static final String V_FLEXIBLE_FIELDS = "FlexibleFields";
	public static final String V_FLEXIBLE_CODE_FIELD = "FlexibleCodeField";
	public static final String V_FIELD_ID = "fieldId";
	public static final String A_SHIPPING_EXTENDED_TAX_CODE = "SS050200";	
	public static final String A_SHIP_PROTECTION_TAX = "ShipProtectionTax";
	public static final String A_SHIP_PROTECTION_GST_HST_TAX = "ShipProtectionGstTax";
	public static final String A_SHIP_PROTECTION_PST_TAX = "ShipProtectionPstTax";
	public static final String STR_OMS = "OMS";
	public static final String STR_WEB = "WEB";
	public static final String STR_ZERO= "0";
	
	public static final String STATUS_CODE_INVALID_EMAIL_ID = "INVALID_EMAIL_ID";
    public static final String STATUS_CODE_GDPR_INTERNAL_ERROR = "GDPR_INTERNAL_ERROR";
    public static final String A_STATUS_MESSAGE = "No customer or person info data found for email:";
    public static final String A_STATUS_MESSAGE_GDPR = "An internal error occurred while processing GDPR delete for email:";
	
	public static final String VAL_MIG_STATUS_INVOICED = "3950.01";
    public static final String A_LINE_PRICE_INFO = "LinePriceInfo";
	
	public static final String MESSAGE_SKIP_BUSINESS_DATA = "Skipped - Business data exists";
    public static final String MESSAGE_FAILURE = "Failure - Exception occurred";
    public static final String VAL_SUCCESS = "Success";

	//EOMS-3696
    public static final String A_PAYPAL_MERCHANT_ID = "PAYPAL_MERCHANT_ID";
    public static final String A_PAYPAL_PUBLIC_KEY = "PAYPAL_PUBLIC_KEY";
    public static final String A_PAYPAL_PRIVATE_KEY = "PAYPAL_PRIVATE_KEY";
    public static final String A_PAYPAL_BRAINTREE_ENVIRONMENT = "BRAINTREE_PAYPAL_ENVIRONMENT";
    public static final String STR_PAYMENT_REJECT_HOLD = "PAYMENT_REJECT_HOLD";
    public static final String STR_CHANGE_ORDER_HOLD_OPERATION = "changeOrderHoldOperation";
    public static final String STR_NOT_PRESENT = "not present";
    public static final String STR_ENV_PRODUCTION = "PRODUCTION";
    public static final String STR_ENV_SANDBOX = "SANDBOX";
    public static final String STR_CROCS_ADYEN_WEBHOOK_SYNC_SERVICE = "CrocsAdyenWebhookSyncService";
    public static final String STR_PAYMENT_REFERENCE_6_EMPTY = "PaymentReference6 value is empty, Hold is applied to the Order.";
    public static final String STR_FAILURE_RESPONSE = "Failure Response from Brain Tree.";
    public static final String STR_CONNECTION_FAILURE_EXCEPTION = "Exception occured while connecting Brain Tree PayPal.";
	
	//Defect 3240
	public static final String VAR_PARTIALLY_SHIPPED="Partially Shipped";
	public static final String A_CROCS_QUOTATION_REQUEST_TO_VERTEX_FOR_PRICING = "CrocsQuotationRequesttoVertexForRepricing";
	
	public static final String A_XMLNS = "xmlns";
	public static final String A_W3_URL = "http://www.w3.org";
	public static final String A_INVOICE_MODE = "SHIPMENT";

    //EOMS-3126
    public static final String A_VAL_DC = "DC";
    public static final String A_VAL_SHIPPED= "SHIPPED";
    public static final String CURRENCY_CAD = "CAD";
    public static final String A_CROCS_GET_ORDER_LIST_FOR_NARVAR_UPDATE = "CrocsGetOrderListForNarvarUpdate";


    //EOMS-3864
    public static final String A_NODE_TYPE_STORE = "Store";
	public static final String A_RETURN_INVOICE_MODE = "RETURN";
	
	//EOMS-3896
	public static final String STR_CROCS_VERTEX_INVOICE_REQUEST_EXP="CROCS_VERTEX_INVOICE_REQUEST_EXP";
	public static final String STR_CROCS_VERTEX_QUOTATION_REQUEST_EXP="CROCS_VERTEX_QUOTATION_REQUEST_EXP";
	public static final String STR_INVOICE_REQUEST_FAILURE="INVOICE REQUEST FAILURE";
	public static final String STR_QUOTATION_REQUEST_FAILURE="QUOTATION REQUEST FAILURE";
	public static final String STR_VERTEX_REFERNCE_VALUE="Connectivity to Vertex got failed";
	public static final String STR_EXPIRATION_DAYS="ExpirationDays";
	public static final String STR_EXPIRATION_DAYS_VALUE="15";
	public static final String XPATH_ORDERLIST_ORDER_ORDER_NO="/OrderList/Order/@OrderNo";
	public static final String XPATH_ORDERINVOICE_ORDER_ORDER_HEADER_KEY="/OrderInvoice/Order/@OrderHeaderKey";
	public static final String XPATH_ORDERINVOICE_ORDER_ORDER_NO="/OrderInvoice/Order/@OrderNo";
	public static final String XPATH_ORDERINVOICE_ORDER_ENTERPRISE_CODE="/OrderInvoice/Order/@EnterpriseCode";
	public static final String A_ENTERPRISE_KEY="EnterpriseKey";
	public static final String CROCS_VERTEX_CREATE_EXCEPTION_SYNC_SERV="CrocsVertexCreateExceptionSyncServ";
	public static final String A_PRIORITY="Priority";
	public static final String A_PRIORITY_1="1";
	public static final String XPATH_ORDER_ENTERPRISE_CODE="/Order/@EnterpriseCode";
	public static final String XPATH_ORDER_ORDER_HEADER_KEY="/Order/@OrderHeaderKey";

	
	//PMR- TS019321178 
	public static final String A_ZERO_UNIT_PRICE = "0.00";
	
	public static final String VAL_INCLUDE_IN_RET = "INCLUDE_IN_RETURN";
    public static final String VAL_RETURN_CREATED = "3700.01";

  //Reading Narvar properties from SMA
  public static final String CROCS_NARVAR_API_FOR_SALES = "CROCS_NARVAR_API_FOR_SALES";
  public static final String CROCS_NARVAR_API_FOR_RETURN = "CROCS_NARVAR_API_FOR_RETURN";
  
  	//EOMS-4261
	public static final String A_RESPONSE="Response";
	public static final String A_IS_VALID_ORDER="IsValidOrder";
	public static final String A_IS_EXCEPTION_ORDER="IsExpcetionOrder";
	
	 //EOMS-4305
    public static final String V_SOAP_ENV_FAULT = "soapenv:Fault";
	public static final String V_DETAIL = "detail";
	public static final String V_VERTEX_EXCEPTION = "ns2:VertexException";
	public static final String V_EXCEPTION_TYPE = "exceptionType";
	public static final String V_ROOT_CAUSE = "rootCause";
    	//SMA property constants
	public static final String FORTER_API_VERSION="FORTER_API_VERSION";
	public static final String A_CONTENT_TYPE="CONTENT_TYPE";
	public static final String FORTER_EXCEPTION="Forter Exception";
	public static final String STR_CROCS_FORTER_REQUEST_EXP="CROCS_FORTER_REQUEST_EXP";
	public static final String A_ADYEN_HTTP_METHOD="ADYEN_HTTP_METHOD";
	public static final String A_VERTEX_HTTP_METHOD="VERTEX_HTTP_METHOD";
	public static final String A_CONTENT_TYPE_TEXT = "CONTENT_TYPE_TEXT";
	public static final String A_APPLICATION_JSON = "APPLICATION_JSON";
	public static final String A_ADYEN_X_API_KEY = "ADYEN_X_API_KEY";
	
		//EOMS-4265
	public static final String STR_CROCS_GET_ORDER_INVOICE_DETAILS_SERV = "CrocsGetOrderInvoiceDetailsServ";

	
	//EOMS-1511
	public static final String STR_MIGRATION_PAY_HOLD = "MIGRATION_PAY_HOLD";

  //EOMS-4394
	public static final String A_IN_STORE_EXCHANGE_DISCOUNT = "InStoreExchangeDisc";
	public static final String A_IN_STORE_EXCHANGE = "InStoreExchange";
//EOMS-4374
	public static final String STR_CROCS_GET_CHARGE_CATEGORYLIST_NARVAR="CrocsGetChargeCategoryListForNarvar";
	public static final String A_RETRY_COUNT="RetryCount";
	//EOMS-4442
	public static final String STR_CROCS_US_NARVAR_LABEL_POST_TO_Q="CrocsUSNarvarLabelPostToQueueSyncServ";
	public static final String STR_CROCS_CA_NARVAR_LABEL_POST_TO_Q="CrocsCANarvarLabelPostToQueueSyncServ";
	public static final String CROCS_NARVAR_RETRY_COUNT="CROCS_NARVAR_RETRY_COUNT";
	public static final String STR_COUNT_1="1";
	public static final String OMS_LOG_INFO="OMS_Update: ";
	
	//EOMS-643:
	public static final String STR_AMOUNT_UPDATES = "/amountUpdates";
	public static final String A_EVENT_CODE_AMOUNT_UPDATES = "AMOUNTUPDATES";
	public static final String A_INDUSTRY_USAGE = "industryUsage";
	public static final String A_DELAYED_CHARGE = "delayedCharge";
	public static final String STR_CROCS_GET_PAYMENT_LIST = "CrocsGetPaymentList";
	public static final String XPATH_PAYMENT_LIST_TOTAL_AUTHORIZED = "/PaymentList/Payment/@TotalAuthorized";
	public static final String A_OUTPUT_STRUCT_DATE_FORMAT = "yyyyMMddHHmmss";
	//EOMS-3695
	public static final String A_EXTN_IS_ACKNOWLEDGED="ExtnIsAcknowledged";
	
	//EOMS - 4538
	public static final String A_APPLE_PAY = "applepay";
	public static final String STR_APPLE_PAY = "APPLEPAY";
	
	//EOMS-4594 : Forter Changes
	public static final String F_AUTHORIZED = "Authorized";
	public static final String F_PAYER_STATUS = "payerStatus";
	public static final String F_PAYMENT_STATUS = "paymentStatus";
	public static final String F_VERIFIED = "VERIFIED";
	public static final String F_PAYER_ACCOUNT_COUNTRY = "payerAccountCountry";
	//EOMS-4652
	public static final String A_HST_TAX="CAHstTax";
	public static final String A_QST_TAX="CAQstTax";
	public static final String[] ELIGIBLE_PRORATION_TAXES_FOR_US={A_SALES_TAX };
	public static final String[] ELIGIBLE_PRORATION_TAXES_FOR_CA={A_GST_HST_TAX, A_PST_TAX,
			A_HST_TAX, A_QST_TAX };
	 
	//EOMS-736 : CA Narvar create Order
	public static final String XPATH_DERIVED_FROM_ORDER_ORDER_LINE_KEY ="/Order/OrderLines/OrderLine/DerivedFrom/@OrderLineKey";

    public static final String E_GOODS_RECEIPT="GoodsReceipt";
    public static final String E_RECEIPT_LINE="ReceiptLine";
    public static final String E_PURCHASE_ORDER="PurchaseOrder";
    public static final String V_ARGUMENT_NAME="ArgumentName";
    public static final String V_DEFAULT="Default";
    public static final String V_3PL_SHIPMENT_CONFIRMATION="3PLShipmentConfirmation";
    public static final String V_3PL_RETURN_RECEIPT_MESSAGE="3PLReturnReceiptMessage";
    
  //EOMS-4847 :: Forter changes
  	public static final String STR_XPATH_EXTN_SAP_MATERIAL_GROUP = "ItemDetails/Extn/@ExtnSAPMaterialGroup";
	//EOMS-4866
	public static final String A_CANCEL="Cancel";
	
	//EOMS - 5048
	public static final String A_SHIPPING_QST_TAX = "ShippingQstTax";
	public static final String A_SHIP_PROTECTION_QST_TAX = "ShipProtectionQstTax";
	public static final String V_IMPOSITION_QST = "Quebec Sales Tax (VAT)";
	public static final String V_IMPOSITION_RST = "Retail Sales Tax (RST)";
	public static final String STR_INVALID_TAX = "Invalid Tax Exception";
	
	//EOMS-5182 :: UEL Changes as per Locale
	public static final String STR_FR_CA = "fr_CA";
	public static final String STR_EN_CA = "en_CA";
	public static final String STR_CROCS = "CROCS";
	//EOMS-5365 :: Bug changes
	public static final String STR_SCAC_UPSC = "UPS";
	public static final String STR_SCAC_PUROLATOR = "purolator-int";
	public static final String STR_SCAC_FLEET_OPTICS = "fleet-optics";
	
	//EOMS -5253
	public static final String  A_IS_NEW_TAX_ALERT_NEEDED = "isNewTaxAlertNeeded";
    //EOMS-5731
	public static final String STR_CANCELED_BY_MERCHANT="CANCELED_BY_MERCHANT";
	public static final String XPATH_EXTN_FRAUD_STATUS="/OrderList/Order/Extn/@ExtnFraudStatus";

	//EOMS-5404: MarketPlace Return creation and Receipt closed
	public static final String ENTRY_TYPE_MARKET_PLACE = "MARKETPLACE";
	public static final String ORDER_TYPE_MP = "MP";
	public static final String CROCS_GET_ORDER_LIST_FOR_MARKETPLACE_SYNC_SERV = "CrocsGetOrderListForMarketPlace";
	public static final String STR_XPATH_ORDERLIST_CUSTOMER_EMAILID = "/OrderList/Order/@CustomerEMailID";
	public static final String STR_XPATH_ORDERLIST_PAYMENT_RULE_ID = "/OrderList/Order/@PaymentRuleId";
	public static final String STR_XPATH_ORDERLIST_ENTERED_BY = "/OrderList/Order/@EnteredBy";
	public static final String A_PAYMENT_RULE_ID = "PaymentRuleId";
	
	public static final String XPATH_ORDER_HDR_KEY = "/Shipment/ShipmentLines/ShipmentLine/@OrderHeaderKey";

	//EOMS - 5586
	public static final String A_STANDARD_TO = "StandardTO";
	public static final String A_CROCS_TO_DATECAL = "CROCS_TO_DATECAL";
	public static final String STR_CROCS_NA = "CROCS_NA";
	public static final String A_RUSH_TO = "RushTO";
	public static final String A_TO_DOCUMENT_TYPE = "0006";

    //5675
    public static final String V_CROCS_CA_SEND_RELMSG_TO_UPS_ON_SUCCESS_SERV="CrocsCASendRelMsgToUPSOnSuccessServ";
    public static final String V_CROCS_CA_SEND_RELMSG_ON_UPDATE_SERV="CrocsCASendRelMsgOnUpdateServ";
    public static final String V_CROCS_CA_SHIPMENT_UPDATES_CREATE_SYNC_SERV="CrocsCAShipmentUpdatesCreateSyncServ";
    public static final String V_CROCS_CA_SHIPMENT_UPDATES_PACKED_SYNC_SERV="CrocsCAShipmentUpdatesPackedSyncServ";
    public static final String V_CROCS_CA_PUBLISH_SHIP_DTLS_TO_NARVAR_ASYNC_SERV="CrocsCAPublishShipDtlsToNarvarAsyncServ";
    public static final String V_CROCS_CA_ORDER_UPDATE_TO_FORTER_POST_TO_QUEUE="CrocsCAOrderUpdateToForterPostToQueue";
    public static final String V_CROCS_CA_NARVAR_RETURN_LABEL_POST_TO_QUEUE_SYNC_SERV="CrocsCANarvarLabelPostToQueueSyncServ";
    public static final String V_GENERIC_FLOW_FOR_INFO_STMT="GenericFlowforInfoStmt";
    public static final String E_FLOW_DETAILS="FlowDetails";
    public static final String V_FLOW_NAME="FlowName";
    public static final String V_SEARCH_TERM="SearchTerm";

	//EOMS-5703
	public static final String XPATH_ORDER_RELEASE_STATUS="OrderRelease/OrderLine/OrderStatuses/OrderStatus[@Status='3200']";
	
	//EOMS-5779
	public static final String XPATH_CARRIER_SERVICE_CODE = "/ShipmentList/Shipment/@CarrierServiceCode";
    public static final String XPATH_CODE_LONG_DESC = "/CommonCodeList/CommonCode[1]/@CodeLongDescription";
	
	
	//EOMS - 5634
	public static final String TO_TRANSACTION_ID_US = "Crocs_US_Shipment_Packed.0006.ex";
	public static final String TO_TRANSACTION_ID_CA = "Crocs_CA_Shipment_Packed.0006.ex";

  //EOMS-5622 Updating OrderType 03 for Non MP Orders
  public  static  final String VAL_ORDER_TYPE_NON_MP = "03";
  public  static  final String SAP_PROFIT_CENTER = "SAP_PROFIT_CENTER";
  public  static  final String MP_ORDER_TYPE = "MP";
  
  public static final String VAL_CROCS_SHIP_VIA_CODE = "CROCS_WMS_SHIP_VIA";
  public static final String XPATH_CODE_SHORT_DESC = "/CommonCodeList/CommonCode/@CodeShortDescription";
  
  public static final String ORDER_TYPE_RETAIL = "02";

  //EOMS - 6306 for Naravr Exchanges
  public static final String EXTN_SAP_MATERIAL_GROUP = "ExtnSAPMaterialGroup";


  // EOMS -6289 Order Reservation
  public  static  final String VAL_ORDER_PURPOSE = "EXCHANGE";
  
  // EOMS-6314
  public static final String XPATH_MONITOR_CONDOLIDATION_ORDER_HEADER_KEY="/MonitorConsolidation/Order/@OrderHeaderKey";
  public static final String XPATH_ORDER_LINE_RESERVATION="OrderLineReservations/OrderLineReservation";

  // EOMS-5877
  public static final String VAL_ONE = "1";
  public static final String VAL_TWO = "2";
  public static final String XPATH_ORDER_LINES="/Order/OrderLines";
  
  //EOMS-6583
  public static final String A_CROCS_GET_ORDER_LIST_TO = "CrocsTOGetOrderList";

  
  // EOMS
  public static final String HEYDUDE_US = "HEYDUDE_US";
  public static final String HEYDUDE_AU = "HEYDUDE_AU";
  public static final String HEYDUDE_CA = "HEYDUDE_CA";
  public static final String TRANSACTION_ID_HEYDUDE_US = "HeyDude_US_Shipmnt_Packd.0001.ex";
  public static final String TRANSACTION_ID_HEYDUDE_AU = "HeyDude_AU_Shipmnt_Packd.0001.ex";
  public static final String TRANSACTION_ID_HEYDUDE_CA = "HeyDude_CA_Shipmnt_Packd.0001.ex";
  
  //EOMS-6092 : HeyDude US Post Auth Payments
  public static final String V_HEYDUDE_US = "heydude_us";
  public static final String V_HDUS = "HDUS";
  
  //EOMS-6182 : HeyDude US Vertex
  public static final String A_4100 = "4100";
  public static final String A_RADIAL_SHIPNODE="4103";
  
  //EOMS-6089 
  public static final String VAL_EXPRESS_2_DAY = "Express 2-Day";

  //EOMS-6076 : HeyDude Create Order Implementation
  public static final String CUSTOM_HAU_NO = "OHAU";
  public static final String CUSTOM_HUS_NO = "OHUS";
  public static final String CUSTOM_HCA_NO = "OHCA";

  //EOMS-6849
  public static final String E_EXCHANGE="exchange";
  public static final String A_CHARGES="Charges";

  //EOMS - 6851
  public static final String A_CROCS_GET_SHIPMENT_LIST_FOR_ORDER_NARVAR_UPDATE = "CrocsGetShipmentListForOrder";

  /*EOMS -6582*/
  public static final String STR_EXCHANGE="EXCHANGE";
  public static final String XPATH_RECEIPT_ORDER_NO="/Receipt/Shipment/@OrderNo";
  public static final String XPATH_ORDER_LIST_CONDITON_VAIRABLE1="/OrderList/Order/OrderLines/OrderLine[@ConditionVariable1='exchange']";
  public static final String XPATH_RECEIPT_SHIPMENT="/Receipt/Shipment";
  public static final String XPATH_RECEIPT_RECEIPT_LINES="/Receipt/ReceiptLines";
  public static final String A_CONDITON_VARIABLE1="ConditionVariable1";
  public static final String XPATH_ITEM_DETAILS_ITEM_ID="ItemDetails/@ItemID";
  public static final String CREATE_ORDER_INVOICE_0003="CREATE_ORDER_INVOICE.0003";
  public static final String XPATH_RECEIPT_ORDER_HEADER_KEY="/Receipt/ReceiptLines/ReceiptLine/@OrderHeaderKey";
  public static final String A_NARVAR_EXCHANGE_LINE="exchange";
  
  //EOMS-6194 : Adyen Payment Capture
  public static final String A_HEYDUDE_US = "HeyDudeUS";
  
  //EOMS-7161 : GLOBALE Return Order Implementation
  public static final String A_HEYDUDE_GET_ORDER_LIST_FOR_GLOBALE = "HeyDudeGetOrderListForGlobalE";
  public static final String STR_XPATH_ORDERLIST_SHIP_NODE = "/OrderList/Order/OrderStatuses/OrderStatus/@ShipNode";
  
  //EOMS-6250 : HeyDude US Narvar Label Generation
  public static final String STR_CHECKOUT_BRAND = "checkout_brand";
  public static final String STR_HEYDUDE_BRAND = "heydude";
  public static final String STR_CROCS_BRAND = "crocs";
  public static final String STR_ATTRIBUTES = "attributes";
  public static final String HEYDUDE_NARVAR_USERNAME="HEYDUDE_NARVAR_USERNAME";
  public static final String HEYDUDE_NARVAR_PASSWORD="HEYDUDE_NARVAR_PASSWORD";
  
  
  //EOMS- 7338 Update Item Additional Attributes for Existing and New Items
   public static final String A_SAP_GET_ITEM_LIST = "CrocsSAPGetItemList";

   //EOMS-6102 HeyDude Changes for Locale
   public static final String V_US_DEFAULT_LOCALE = "default";
   public static final String V_EN_US_LOCALE = "en_US";
   public static final String A_LOCALE = "Locale";
   public static final String A_COLOR = "Color";
   public static final String A_PRODUCT_URL = "ProductUrl";

   //EOMS-7579 : HeyDude Status Update
   public static final String STR_HEYDUDE_GET_ORDER_LIST_FOR_GLOBALE_SERVICE = "HeyDudeGetOrderListForGlobalEService";
   public static final String A_CANCELLED_QTY = "CancelledQty";
   public static final String A_PUBLISH_STATUS_UPDATE = "PublishStatusUpdate";
   
   //EOMS-7443
   public static final String LOCALE_US_EN = "us-en";
   public static final String LOCALE_CA_EN = "ca-en";
   public static final String LOCALE_CA_FR = "ca-fr";
   public static final String LOCALE_AU_EN = "au-en";

   //EOMS-7592 Properties changes HeyDude forter
   public static final String HD_FORTER_API_KEY="HD_FORTER_API_KEY";
   public static final String HD_FORTER_SITE_ID="HD_FORTER_SITE_ID";
   public static final String HD_FORTER_API_VERSION="HD_FORTER_API_VERSION";
   
   //EOMS-8430
   public static final String CROCS_CA_FULFILLMENT_TYPE_EXPRESS = "CROCS_CA_EXPRESS_SHIPPING_FULFILLMENT";
   
   //EOMS - 8495
   public static final String A_TO_FULFILLMENT_TYPE = "CROCS_TO_FULFILLMENT";
   public static final String A_CROCS_TO_SCH = "CRC_TO_SCH";

   //EOMS- 8487 Custom Order No for Tranfer Orders
   public static final String CUSTOM_TO_US_NO = "TOUS";
    public static final String CUSTOM_TO_CA_NO = "TOCA";
    public static final String CUSTOM_TO_KR_NO = "TOKR";
    public static final String CUSTOM_TO_MY_NO = "TOMY";
    public static final String ORDER_NO_PREFIX = "ORDER_NO_PREFIX";
    public static final String TO_SUFFIX = "TO";
    public static final String ORDER_NO_PREFIX_QA = "QA";
    public static final String ORDER_NO_PREFIX_PREPROD  = "Preprod";

    // EOMs- 8174
    public static final String CROCS_AU = "CROCS_AU";
    public static final String V_FORTER_MERCHANT_ID_CAU = "FORTER_MERCHANT_ID_CAU";
    public static final String V_FORTER_MERCHANT_NAME_CAU = "FORTER_MERCHANT_NAME_CAU";
    public static final String CAU_FORTER_API_KEY="CROCS_AU_FORTER_API_KEY";
    public static final String CAU_FORTER_SITE_ID="CROCS_AU_FORTER_SITE_ID";
    public static final String CAU_FORTER_API_VERSION="CROCS_AU_FORTER_API_VERSION";
    public static final String CUSTOM_CAU_NO = "OCAU";
    public static final String A_ADYEN_MERCHANT_ACCOUNT_CAU = "CrocsAU";
    //EOMS-8750
    public static final String CURRENCY_AUD = "AUD";


    // EOMS - 8725
    
    public static final String XPATH_CODE_VALUE = "/CommonCodeList/CommonCode/@CodeValue";
    public static final String STR_CODE_TYPE ="CROCS_WM_RET_SHP_VIA";
    public static final String STR_SCAC="SCAC";
    public static final String STR_EXTN_CARRIER="ExtnCarrier";

    //EOMS-8868 - EOMS-8400
    static final String VAL_HDUS_GET_ACTIVE_NODE = "HDUS_GET_ACTIVE_NODE";
    static final String VAL_IS_LVDC_ACTIVE = "IsLVDCActive";
          
    //EOMS-6967
    public static final String A_LVDC="4101";

    //EOMS-9159 : copy PersonInfoShipTo to Narvar Create XML 
    public static final String STR_XPATH_ORDERLIST_PERSON_INFO_SHIP_TO = "/OrderList/Order/PersonInfoShipTo";

    //EOMS - 9099 CROCS_AU using new API-key to connect to Adyen
    public static final String A_CAU_ADYEN_X_API_KEY5 = "CAU_ADYEN_X-API-KEY5";
    public static final String A_CAU_ADYEN_CAPTURE_REQUEST_BODY = "body";
    
          
    //EOMS -8818
    public static final String HEYDUDE_MP="HEYDUDE_MP";

    //EOMS-8753 CROCS_AU Return Narvar Label
    public static final String STR_CROCS_AU_NARVAR_LABEL_POST_TO_Q="CrocsAUNarvarLabelPostToQueueSyncServ";
    //EOMS-8754 CROCS_AU ShipNode
    public static final String STR_CAU_SHIP_NODE = "3011";
    
    //EOMS-7833,7834, 7832
    public static final String STR_CROCS_RESHIP_REASONS = "CROCS_RESHIP_REASONS";
    public static final String V_REPLACEMENT = "REPLACEMENT";
    public static final String V_REFUND_UPON_RETURN = "REFUND_UPON_RETURN";
    public static final String V_SHIP_TO_WAREHOUSE = "SHIP_TO_WAREHOUSE";
    public static final String V_CROCS_RETURN_REASONS = "CROCS_RETURN_REASONS";
    public static final String A_MODIFICATION_REASON_CODE = "ModificationReasonCode";
    public static final String A_QUANTITY_TO_RESHIP = "QuantityToReship";
    public static final String V_CALL_CENTER = "CALL_CENTER";
    public static final String V_STORE = "STORE";
    public static final String V_IN_STORE = "IN_STORE";
    public static final String A_ACCOUNT_ID_F = "accountId";
    public static final String V_RESHIP_REASON = "ReshipReason";
    public static final String A_INITIATION_TYPE_F = "initiationType";
    public static final String A_COMPENSATION_TYPE_REQ_F = "compensationTypeRequested";
    public static final String A_INITIATION_TIME_F = "initiationTime";
    public static final String A_REASON_CATEGORY_F = "reasonCategory";
    public static final String A_REQ_RETURN_TYPE_F = "requestedReturnType";
    public static final String A_ITEM_COMPENSATION_DATA_F = "itemCompensationData";
    public static final String A_ITEMS_F = "items";
    public static final String A_TOTAL_REQ_AMOUNT_F = "totalRequestedAmount";
    public static final String A_COMPENSATION_REQ_F = "compensationRequest";
    public static final String A_EVENT_ID_F = "eventId";
    public static final String A_IS_GUEST_ACCOUNT_F = "isGuestAccount";
    public static final String A_ORIGINAL_ORDER_ID_F = "originalOrderId";
    public static final String A_TABLE_KEY = "TableKey";
    public static final String FORTER_COMPENSATION_API = "unified-compensation-request";
    public static final String E_ORDER_INVOICE_DETAIL = "OrderInvoiceDetail";
    public static final String E_INVOICE_HEADER = "InvoiceHeader";
    public static final String E_ORDER_LINE_DETAIL = "OrderLineDetail";
    public static final String A_RESHIP_REASON = "ReshipReason";
    public static final String V_WEB = "WEB";
    public static final String V_CREDIT_MEMO = "CREDIT_MEMO";
    public static final String STR_CROCS_APPEASEMENT_REASONS="CROCS_APPEASE_REASON";
    public static final String A_INVOICE_CREATION_REASON="InvoiceCreationReason";
    public static final String A_RETURN_REASON="ReturnReason";
    public static final String A_COMPENSATION_STATUS="compensationStatus";
    public static final String A_ITEM_STATUS="itemStatus";
    public static final String A_UPDATED_TOTAL_AMOUNT="updatedTotalAmount";
    public static final String A_TOTAL_GRANTED_AMOUNT="totalGrantedAmount";
    public static final String A_COMPENSATION_TYPE_GRANTED="compensationTypeGranted";
    public static final String A_RETURN_METHOD_GRANTED="returnMethodGranted";
    public static final String A_APPEASEMENT_STATUS="SENT";
    public static final String A_APPEASEMENT_STATUS_DATA="statusData";
    public static final String A_ACCEPTED_BY_MERCHANT="ACCEPTED_BY_MERCHANT";
	
    //EOMS-10081,EOMS-10082,EOMS-4438
    public static final String A_DIVISION = "Division";
    public static final String STR_PO_ORDER_TYPE = "03";
    public static final String STR_PO_DIVISION = "10";
    public static final String STR_PO_CHANNEL = "10";
    public static final String A_CHANNEL = "Channel";
    public static final String A_EXTN_REASON_CODE = "ExtnReasonCode";
    public static final String STR_CROCS_ORDER_REASONS = "CROCS_ORDER_REASONS";

    //EOMS-10554 : Checking PO Box
    public static final String STR_CROCS_PO_BOXES_LIST = "CROCS_PO_BOXES_LIST";
    public static final String A_ADDRESS_LINE_3="AddressLine3";
    public static final String STR_CROCS_GET_ORDERLIST_FOR_RESHIP_LINES = "CrocsGetOrderListForReshipLines";
    
    //EOMS-10885: Change Reservation of inventory from 21 days to 30 days
    public static final String STR_CROCS_EXCHANGE_RSV_DAY = "CROCS_EXCH_RSV_DAY";
    public static final String STR_CROCS_EXCHANGE_RESERVATION_DAY = "CROCS_EXCHANGE_RSV_DAY";
	
	//EOMS-10938: CROCS_SG CreateOrder Changes
	public static final String CROCS_SG = "CROCS_SG";
	public static final String CUSTOM_TO_SG_NO = "TOSG";
	
	//EOMS-11176 : CROCS SG Adyen Payment Capture 
	public static final String A_ADYEN_MERCHANT_ACCOUNT_CSG = "CrocsSG";
	
	//EOMS-11644 : Crocs SG Post Auth Validation
	public static final String V_FORTER_MERCHANT_ID_CSG = "FORTER_MERCHANT_ID_CSG";
    public static final String V_FORTER_MERCHANT_NAME_CSG = "FORTER_MERCHANT_NAME_CSG";
	//EOMS-11232 : CROCS_SG Start
	public static final String SG_SO_FULFILLMENT_NODE = "3171";
    public static final String SG_RO_FULFILLMENT_NODE = "3001";
    public static final String CUSTOM_CSG_NO = "OCSG";
	public static final String STR_CROCS_SG_NARVAR_LABEL_POST_TO_Q = "CrocsSGNarvarLabelPostToQueueSyncServ";
	//EOMS-11232 : CROCS_SG End
    
  //EOMS-10803 HEYDUDE_CA ShipNode
    public static final String HDCA_SHIP_NODE = "4101";
    //EOMS-11176 : CROCS SG Adyen Payment Capture 
  	public static final String A_ADYEN_MERCHANT_ACCOUNT_HCA = "HeyDudeCA";

    //EOMS -10444 Crocs Shipment Pack Update
    public static final String CROCS_EU = "CROCS_EU";
    public static final String CROCS_DE = "CROCS_DE";
    public static final String CROCS_FR = "CROCS_FR";
    public static final String CROCS_NL = "CROCS_NL";
    public static final String CROCS_FI = "CROCS_FI";
    public static final String CROCS_GB = "CROCS_GB";
    public static final String HEYDUDE_EU = "HEYDUDE_EU";
    public static final String HEYDUDE_DE = "HEYDUDE_DE";
    public static final String HEYDUDE_FR = "HEYDUDE_FR";
    public static final String HEYDUDE_GB = "HEYDUDE_GB";
    public static final String TRANSACTION_ID_CROCS_EU = "Crocs_EU_Shipment_Packed.0001.ex";
    public static final String TRANSACTION_ID_CROCS_DE = "Crocs_DE_Shipment_Packed.0001.ex";
    public static final String TRANSACTION_ID_CROCS_FR = "Crocs_FR_Shipment_Packed.0001.ex";
    public static final String TRANSACTION_ID_CROCS_NL = "Crocs_NL_Shipment_Packed.0001.ex";
    public static final String TRANSACTION_ID_CROCS_FI = "Crocs_FI_Shipment_Packed.0001.ex";
    public static final String TRANSACTION_ID_CROCS_GB = "Crocs_GB_Shipment_Packed.0001.ex";
    public static final String TRANSACTION_ID_HEYDUDE_EU = "HeyDude_EU_Shipmnt_Packd.0001.ex";
    public static final String TRANSACTION_ID_HEYDUDE_DE = "HeyDude_DE_Shipmnt_Packd.0001.ex";
    public static final String TRANSACTION_ID_HEYDUDE_FR = "HeyDude_FR_Shipmnt_Packd.0001.ex";
    public static final String TRANSACTION_ID_HEYDUDE_GB = "HeyDude_GB_Shipmnt_Packd.0001.ex";

    //START-EOMS-11895
    public static final String GET_LOCALE_LIST_API = "getLocaleList";
    
    //EOMS-10448 & EOMS-10915 WMSCode's
    public static final String VAL_THREE = "3";
    public static final String VAL_FIVE = "5";
    public static final Set<String> CROCS_EMEA_ENTERPRISES = Set.of(
            "CROCS_DE",
            "HEYDUDE_DE",
            "CROCS_EU",
            "HEYDUDE_EU",
            "CROCS_FI",
            "CROCS_FR",
            "HEYDUDE_FR",
            "CROCS_GB",
            "HEYDUDE_GB",
            "CROCS_NL"
    );

    //EOMS-9722 EMEA Forter
    public static final String V_FORTER_MERCHANT_ID_C_DE = "FORTER_MERCHANT_ID_C_DE";
    public static final String V_FORTER_MERCHANT_NAME_C_DE = "FORTER_MERCHANT_NAME_C_DE";
    public static final String V_FORTER_MERCHANT_ID_C_EU = "FORTER_MERCHANT_ID_C_EU";
    public static final String V_FORTER_MERCHANT_NAME_C_EU = "FORTER_MERCHANT_NAME_C_EU";
    public static final String V_FORTER_MERCHANT_ID_C_FI = "FORTER_MERCHANT_ID_C_FI";
    public static final String V_FORTER_MERCHANT_NAME_C_FI = "FORTER_MERCHANT_NAME_C_FI";
    public static final String V_FORTER_MERCHANT_ID_C_FR = "FORTER_MERCHANT_ID_C_FR";
    public static final String V_FORTER_MERCHANT_NAME_C_FR = "FORTER_MERCHANT_NAME_C_FR";
    public static final String V_FORTER_MERCHANT_ID_C_GB = "FORTER_MERCHANT_ID_C_GB";
    public static final String V_FORTER_MERCHANT_NAME_C_GB = "FORTER_MERCHANT_NAME_C_GB";
    public static final String V_FORTER_MERCHANT_ID_C_NL = "FORTER_MERCHANT_ID_C_NL";
    public static final String V_FORTER_MERCHANT_NAME_C_NL = "FORTER_MERCHANT_NAME_C_NL";
    public static final String V_FORTER_MERCHANT_ID_HD_DE = "FORTER_MERCHANT_ID_HD_DE";
    public static final String V_FORTER_MERCHANT_NAME_HD_DE = "FORTER_MERCHANT_NAME_HD_DE";
    public static final String V_FORTER_MERCHANT_ID_HD_EU = "FORTER_MERCHANT_ID_HD_EU";
    public static final String V_FORTER_MERCHANT_NAME_HD_EU = "FORTER_MERCHANT_NAME_HD_EU";
    public static final String V_FORTER_MERCHANT_ID_HD_FR = "FORTER_MERCHANT_ID_HD_FR";
    public static final String V_FORTER_MERCHANT_NAME_HD_FR = "FORTER_MERCHANT_NAME_HD_FR";
    public static final String V_FORTER_MERCHANT_ID_HD_GB = "FORTER_MERCHANT_ID_HD_GB";
    public static final String V_FORTER_MERCHANT_NAME_HD_GB = "FORTER_MERCHANT_NAME_HD_GB";
    //EOMS-9722 EMEA Adyen
    public static final String A_ADYEN_MERCHANT_ACCOUNT_HD_DE = "HeyDudeDE";
    public static final String A_ADYEN_MERCHANT_ACCOUNT_HD_EU = "HeyDudeEU";
    public static final String A_ADYEN_MERCHANT_ACCOUNT_HD_FR = "HeyDudeFR";
    public static final String A_ADYEN_MERCHANT_ACCOUNT_HD_GB = "HeyDudeUK";
    public static final String A_ADYEN_MERCHANT_ACCOUNT_C_DE = "CrocsDE";
    public static final String A_ADYEN_MERCHANT_ACCOUNT_C_EU = "CrocsEUR";
    public static final String A_ADYEN_MERCHANT_ACCOUNT_C_FI = "CrocsFI";
    public static final String A_ADYEN_MERCHANT_ACCOUNT_C_FR = "CrocsFR";
    public static final String A_ADYEN_MERCHANT_ACCOUNT_C_NL = "CrocsNL";
    public static final String A_ADYEN_MERCHANT_ACCOUNT_C_GB = "CrocsUK";
    
    //EOMS-10662 : Send Order Details to Narvar 
    public static final String CURRENCY_SGD = "SGD";

    public static final Set<String> CROCS_EMEA_ADYEN = Set.of(
            "HeyDudeDE",
            "HeyDudeEU",
            "HeyDudeFR",
            "HeyDudeUK",
            "CrocsDE",
            "CrocsEUR",
            "CrocsFI",
            "CrocsFR",
            "CrocsNL",
            "CrocsUK"
    );

    public static final String A_EMEA_ADYEN_X_API_KEY6 = "EMEA_ADYEN_X-API-KEY6";
    public static final String HEYDUDE_CA_LVDC_FULFILLMENT_TYPE = "HEYDUDE_LVDC_FULFILLMENT";
    public static final String A_4120 = "4120";
    public static final String STR_EMEA_SHIP_NODE = "2004";
    
    //EOMS-11549: Create Order for Korea
    public static final String CUSTOM_CKR_NO = "OCKR";
    
  //EOMS-11059 : Crocs SG Migration Changes
  	public static final String A_SO_MIGRATION_CROCS_SG_PIPELINE_PROPERTY = "SALES_ORDER_MIGRATION_CROCS_SG_PIPELINE_KEY";
  	public static final String A_RO_MIGRATION_CROCS_SG_PIPELINE_PROPERTY = "RETURN_ORDER_MIGRATION_CROCS_SG_PIPELINE_KEY";
  	public static final String A_SO_SHIPMENT_MIGRATION_CROCS_SG_PIPELINE_PROPERTY = "SALES_ORDER_SHIPMENT_MIGRATION_CROCS_SG_PIPELINE_KEY";
  	
  	//EOMS-11442 : Crocs KR Send Order Details to Narvar
    public static final String CURRENCY_KRW = "KRW";
  	//EOMS-12672 : Crocs KR Migration Changes
  	public static final String A_SO_MIGRATION_CROCS_KR_PIPELINE_PROPERTY = "SALES_ORDER_MIGRATION_CROCS_KR_PIPELINE_KEY";
  	public static final String A_RO_MIGRATION_CROCS_KR_PIPELINE_PROPERTY = "RETURN_ORDER_MIGRATION_CROCS_KR_PIPELINE_KEY";
  	public static final String A_SO_SHIPMENT_MIGRATION_CROCS_KR_PIPELINE_PROPERTY = "SALES_ORDER_SHIPMENT_MIGRATION_CROCS_KR_PIPELINE_KEY";
	
	//EOMS-12634 - Narvar Return For Crocs_KR Orders 
    public static final String KR_RO_FULFILLMENT_NODE = "3101";
    //EOMS- 11405 - Configure and validation Adyen Refunds -for XAPI Key
    public static final String A_ADYEN_MERCHANT_ACCOUNT_CKR = "CrocsKorea";  

    //EOMS-10589 : HeyDude CA Post Auth Payments
    
    public static final String V_FORTER_MERCHANT_ID_CUS = "FORTER_MERCHANT_ID_CUS";
    public static final String V_FORTER_MERCHANT_NAME_CUS = "FORTER_MERCHANT_NAME_CUS";
    
    public static final String V_FORTER_MERCHANT_ID_CCA = "FORTER_MERCHANT_ID_CCA";
    public static final String V_FORTER_MERCHANT_NAME_CCA = "FORTER_MERCHANT_NAME_CCA";
    
    public static final String V_FORTER_MERCHANT_ID_HDUS = "FORTER_MERCHANT_ID_HDUS";
    public static final String V_FORTER_MERCHANT_NAME_HDUS = "FORTER_MERCHANT_NAME_HDUS";
    
    public static final String V_FORTER_MERCHANT_ID_HDCA = "FORTER_MERCHANT_ID_HDCA";
    public static final String V_FORTER_MERCHANT_NAME_HDCA = "FORTER_MERCHANT_NAME_HDCA";
    
    //EOMS-5161: Data Sanitization
  	public static final String CROCS_SANITIZE_ATTR = "CROCS_SANITIZE_ATTR";
  	

  	//EOMS-4875: PolicyBuilder
  	public static final String V_REFUND = "REFUND";
    public static final String V_NO_COMPENSATION = "NO_COMPENSATION";
    public static final String V_FORTER_RETURN_REASONS = "FORTER_RETURN_REASON";
  	public static final String A_REJECTED_BY_MERCHANT="REJECTED_BY_MERCHANT";
    public static final String A_RETURN_STATUS_DATA="statusData";
    public static final String A_EVENT_ID_RETURN_ORDER=" RETURN ORDER";

    public static final String A_NO_RETURN="NO_RETURN";
    public static final String FORTER_ORDER_STATUS_API = "status";

	//EOMS-12003 :: Crocs EMEA Shipment Tracking URL Changes
    public static final String STR_SCAC_DPD = "DPD";
    public static final String STR_SCAC_GLS = "GLS";
    
	public static final String STR_DE_DE = "de_DE";
	public static final String STR_EN_DE = "en_DE";
	
	public static final String STR_FI_FI = "fi_FI";
	public static final String STR_EN_FI = "en_FI";
	
	public static final String STR_FR_FR = "fr_FR";
	public static final String STR_EN_FR = "en_FR";
	
	public static final String STR_NL_NL = "nl_NL";
	public static final String STR_EN_NL = "en_NL";
	
	public static final String STR_EN_GB = "en_GB";
	
	public static final String STR_EN = "en";
	public static final String STR_DE = "de";
	public static final String STR_FR = "fr";
	public static final String STR_NL = "nl";
	
	public static final String DPD_TRACKING_URL_FI_FI = "https://www.postnord.fi/tyokalut/lahetysten-seuranta/?shipmentId=TrackingNo";
	public static final String DPD_TRACKING_URL_FI_EN = "https://www.postnord.fi/en/our-tools/track-and-trace/?shipmentId=TrackingNo";
	public static final String DPD_TRACKING_URL_DEFAULT = "https://track.dpd.co.uk/search?reference=TrackingNo&postcode=";
	
	public static final String GLS_TRACKING_URL_DE_DE = "https://www.gls-pakete.de/reach-sendungsverfolgung?trackingNumber=TrackingNo&postCode=";
	public static final String GLS_TRACKING_URL_DE_EN = "https://www.gls-pakete.de/en/reach-parcel-tracking?trackingNumber=TrackingNo&postCode=";
	public static final String GLS_TRACKING_URL_FI_FI = "https://gls-group.com/FI/fi/laehetysseuranta/?match=TrackingNo";
	public static final String GLS_TRACKING_URL_FI_EN = "https://gls-group.com/FI/en/parcel-tracking/?match=TrackingNo";
	public static final String GLS_TRACKING_URL_NL = "https://www.gls-info.nl/tracking?parcelNo=TrackingNo&zipCode=";

    /*EOMS -12322 Additional attributes on Release message to WMS for EMEA*/
    public static final String COUNTRY_CD_AUSTRIA = "AT";
    public static final String COUNTRY_AUSTRIA = "Austria";
    public static final String COUNTRY_CD_BELGIUM = "BE";
    public static final String COUNTRY_BELGIUM = "Belgium";
    public static final String COUNTRY_CD_CZECH_REPUBLIC = "CZ";
    public static final String COUNTRY_CZECH_REPUBLIC = "Czech Republic";
    public static final String COUNTRY_CD_DENMARK = "DK";
    public static final String COUNTRY_DENMARK = "Denmark";
    public static final String COUNTRY_CD_FRANCE = "FR";
    public static final String COUNTRY_FRANCE = "France";
    public static final String COUNTRY_CD_GERMANY = "DE";
    public static final String COUNTRY_GERMANY = "Germany";
    public static final String COUNTRY_CD_GREECE = "GR";
    public static final String COUNTRY_GREECE = "Greece";
    public static final String COUNTRY_CD_IRELAND = "IE";
    public static final String COUNTRY_IRELAND = "Ireland";
    public static final String COUNTRY_CD_LUXEMBOURG = "LU";
    public static final String COUNTRY_LUXEMBOURG = "Luxembourg";
    public static final String COUNTRY_CD_MONACO = "MC";
    public static final String COUNTRY_MONACO = "Monaco";
    public static final String COUNTRY_CD_SLOVAKIA = "SK";
    public static final String COUNTRY_SLOVAKIA = "Slovakia";
    public static final String COUNTRY_CD_NETHERLANDS = "NL";
    public static final String COUNTRY_NETHERLANDS = "The Netherlands";
    public static final String COUNTRY_HD_NETHERLANDS = "HD INTERNETSALES - NETHERLANDS";
    public static final String COUNTRY_CD_UK = "GB";
    public static final String COUNTRY_UK = "United Kingdom";
    public static final String COUNTRY_HD_UK = "HEYDUDE UK";
    public static final String COUNTRY_CD_FINLAND = "FI";
    public static final String COUNTRY_FINLAND = "Finland";
    public static final String A_REL_DC_CENTER_NBR = "DcCenterNbr";
    public static final String A_REL_BILL_TO_TITLE = "BillToTitle";
    public static final String A_REL_FEDERATED_STORE_NBR = "FederatedStoreNbr";
    public static final String STR_EMEA_RELEASE_ATTR = "EMEA_RELEASE_ATTR";
    public static final String STR_CROCS_EMEA = "CROCS_EMEA";
    
	//EOMS-12356 Rebound Receipt
	public static final String STR_HTTP_CODE = "httpcode";
	public static final String STR_SUCCESS_RESPONSE_RECEIPT_CLOSE = "Receipt processed successfully";

    //EOMS-12352 Return Order Receipt for EMEA
    public static final String XPATH_REBOUND_RECEIPT_ORDER_NO="/CrocsReboundScanAndReceipt/Receipt/Shipment/@OrderNo";
    public static final String XPATH_REBOUND_RECEIPT_SHIPMENT_ELE ="/CrocsReboundScanAndReceipt/Receipt/Shipment";
    public static final String A_EXTN_IS_SENT_TO_SAP="ExtnIsSentToSAP";
	
	// EOMS - 10220 - Cancellation Reasons - Start
  	public static final String XPATH_CANCELED_FROM="StatusBreakupForCanceledQty/CanceledFrom";
  	public static final String CROCS_CANCEL_REASONS = "CROCS_CANCEL_REASONS";
  	public static final String SCH_REL_FAILURE_REASON = "SCH_REL_FAILURE_REASON";
  	public static final String FRAUD_CHECK_DECLINE_REASON = "FRAUD_CHECK_DECLINE_REASON";
  	public static final String POST_AUTH_FAILURE_REASON = "POST_AUTH_FAILURE_REASON";
  	public static final String SHORT_SHIP_REASON = "SHORT_SHIP_REASON";
  	public static final String WEBSITE_CANCEL_REASON = "WEBSITE_CANCEL_REASON";
  	public static final String CALLCENTER_CANCEL_REASON = "CALLCENTER_CANCEL_REASON";
  	public static final String EXCHANGE_CANCEL_REASON = "EXCHANGE_CANCEL_REASON";
  	public static final String EXCHANGE_RESERV_FAIL_REASON = "EXCHANGE_RESERV_FAIL_REASON";
  	public static final String BACKORDER_CANCEL_REASON = "BACKORDER_CANCEL_REASON";
  	public static final String RETURN_CANCEL_REASON = "RETURN_CANCEL_REASON";
  	public static final String OTHER = "OTHER";
  	public static final String ORDER_CANCEL_NOTE_TEXT = "The entire order was canceled due to reason: CANCELLED_REASON";
  	public static final String ORDER_LINE_CANCEL_NOTE_TEXT = "The order line was canceled with QTY quantity due to reason: CANCELLED_REASON";
    public static final String QTY = "QTY";
    public static final String CANCELLED_REASON = "CANCELLED_REASON";
  	public static final String CROCS = "CROCS";
   	// EOMS - 10220 End
    //EOMS-12375 Start
    public static final String STR_CROCS_EMEA_NARVAR_LABEL_POST_TO_Q =  "CrocsEMEANarvarLabelPostToQueueSyncServ";
    //EOMS-12375 End
    
    //EOMS-13235 - Start
    public static final String F_FAILED = "Failed";
  	public static final String NO_DECISION = "noDecision";
  	public static final String A_SERVICE_RESPONSE_CODE_F = "serviceResponseCode";
  	public static final String DECLINED = "DECLINED";
  	//EOMS-13235 - END
  	
    //EOMS-8616 Additional Forter Fields
    public static final String ENTRY_TYPE_APP = "APP";
    public static final String STR_FORTER_MOBILE_UID = "forterMobileUID";
    public static final String STR_MOBILE_APP_VERSION = "mobileAppVersion";
    public static final String STR_MOBILE_DEVICE_BRAND = "mobileDeviceBrand";
    public static final String STR_MOBILE_DEVICE_MODEL = "mobileDeviceModel"; 
    public static final String STR_MOBILE_OS_TYPE = "mobileOSType"; 
    public static final String STR_MERCHANT_DEVICE_IDENTIFIER = "merchantDeviceIdentifier"; 
  	
}

