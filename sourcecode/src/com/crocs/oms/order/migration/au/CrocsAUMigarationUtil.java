
package com.crocs.oms.order.migration.au;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsAPIConstants;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsTemplateConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.dom.YFCNodeList;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.core.YFSSystem;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class CrocsAUMigarationUtil implements CrocsConstant {
	private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsAUMigarationUtil.class);

	/**
     * 
     * @param importOrderYdocEle - YFCElement of Order
     * @param mandatoryAttrtributeToValidate - Attribute we want to validate
     * @return Flag if the value is valid otherwise throws error
     */
	
	public static boolean validateMandatoryAttribute(YFCElement importOrderYdocEle,
			String mandatoryAttrtributeToValidate) {

		String errorCode = "", errorMessage = "", errorDescription = "", path = "";
		String documentTypeVal = "";
		boolean isValid = false;
		YFCNodeList<YFCElement> OrderLines = null;
		
		switch (mandatoryAttrtributeToValidate) {

		case CrocsConstant.A_DOCUMENT_TYPE:

			String documentType = importOrderYdocEle.getAttribute(CrocsConstant.A_DOCUMENT_TYPE);
			if (!YFCCommon.isVoid(documentType))
				isValid = true;
			else {
				path = "/Order/@" + CrocsConstant.A_DOCUMENT_TYPE;
				errorMessage = "Invalid " + CrocsConstant.A_DOCUMENT_TYPE;
				errorCode = "";
				errorDescription = "Provided value for " + CrocsConstant.A_DOCUMENT_TYPE + " is " + documentType
						+ "  Expected is [0001] for sales Order, [0003] for returnOrder at xpath: " + path;
			}
			break;

		case CrocsConstant.VAL_DOCUMENT_TYPE_SALES_ORDER:
			 documentTypeVal = importOrderYdocEle.getAttribute(CrocsConstant.A_DOCUMENT_TYPE);
			if (CrocsConstant.VAL_DOCUMENT_TYPE_SALES_ORDER.equalsIgnoreCase(documentTypeVal))
				isValid = true;
			else 
			{
				path = "Order/@" + CrocsConstant.A_DOCUMENT_TYPE;
				errorMessage = "Invalid " + CrocsConstant.A_DOCUMENT_TYPE;
				errorCode = "";
				errorDescription = "Provided value for " + CrocsConstant.A_DOCUMENT_TYPE + " is " + documentTypeVal
						+ " Expected is [0001] at xpath: " + path;
			}
			break;

			case A_RETURN_ORDER_DOCUMENT_TYPE:
				documentTypeVal = importOrderYdocEle.getAttribute(A_DOCUMENT_TYPE);
				if (A_RETURN_ORDER_DOCUMENT_TYPE.equalsIgnoreCase(documentTypeVal))
					isValid = true;
					else
					{
					path = "Order/@"+A_DOCUMENT_TYPE;
					errorMessage = "Invalid " + A_DOCUMENT_TYPE;
					errorCode = "";
					errorDescription = "Provided value for " +A_DOCUMENT_TYPE+ " is "+ documentTypeVal+ " Expected is [0003] at: " + path;
				}
				break;

		case CrocsConstant.A_ENTERED_BY:

			String enteredBy = importOrderYdocEle.getAttribute(CrocsConstant.A_ENTERED_BY);
			if (!YFCCommon.isVoid(enteredBy) && enteredBy.equalsIgnoreCase(CrocsConstant.V_MIGRATION))
				isValid = true;
			else {
				path = "Order/@" + CrocsConstant.A_ENTERED_BY;
				errorMessage = "Invalid " + CrocsConstant.A_ENTERED_BY;
				errorCode = "";
				errorDescription = "Provided value for " + enteredBy + " is " + enteredBy
						+ " Expected is [Migration] at: " + path;
			}
			break;

		case CrocsConstant.A_ENTERPRISE_CODE:
			String enterpriseCode = importOrderYdocEle.getAttribute(CrocsConstant.A_ENTERPRISE_CODE);
			if (!YFCCommon.isVoid(enterpriseCode))
				isValid = true;
			else {
				path = "Order/@" + CrocsConstant.A_ENTERPRISE_CODE;
				errorMessage = "Invalid " + CrocsConstant.A_ENTERPRISE_CODE;
				errorCode = "";
				errorDescription = "Provided value for " + CrocsConstant.A_ENTERPRISE_CODE + " is " + enterpriseCode
						+ ", Expected is [CROCS_US,CROCS_CA] at xpath: " + path;
			}
			break;

		case CrocsConstant.A_ORDER_NO:
			String orderNo = importOrderYdocEle.getAttribute(CrocsConstant.A_ORDER_NO);
			if (!YFCCommon.isVoid(orderNo))
				isValid = true;
			else {
				path = "Order/@" + CrocsConstant.A_ORDER_NO;
				errorMessage = "Invalid " + CrocsConstant.A_ORDER_NO;
				errorCode = "";
				errorDescription = "Provided value for " + CrocsConstant.A_ORDER_NO + " is " + orderNo + " at xpath: " + path;
			}
			break;

		case CrocsConstant.A_CONDITION_VARIABLE_1:
			
			OrderLines = importOrderYdocEle.getElementsByTagName(CrocsConstant.E_ORDER_LINE);
			
			for (YFCElement OrderLine:OrderLines) 
			{
				isValid = false;

				String conditionVariable1 = OrderLine.getAttribute(CrocsConstant.A_CONDITION_VARIABLE_1);
				if (!YFCCommon.isVoid(conditionVariable1)
						&& conditionVariable1.equalsIgnoreCase(CrocsConstant.V_MIGRATION))
					isValid = true;
				else {
					path = "Order/OrderLines/OrderLine/@" + CrocsConstant.A_CONDITION_VARIABLE_1;
					errorMessage = "Invalid " + CrocsConstant.A_CONDITION_VARIABLE_1;
					errorCode = "";
					errorDescription = "Provided value for " + CrocsConstant.A_CONDITION_VARIABLE_1 + " is "
							+ conditionVariable1 + ", Expected is [Migration] at xpath: " + path;
					break;
				}
			}
			break;
			
		case CrocsConstant.A_SO_MIGRATION_PIPELINE_PROPERTY:
			
			/**
			 *  Pipeline is one of the mandatory attribute, pipeline key must be passed in the SMA, if found blank, 
			 *  it will throw an exception.
			 **/
			
			OrderLines = importOrderYdocEle.getElementsByTagName(CrocsConstant.E_ORDER_LINE);
			for (YFCElement OrderLine : OrderLines) {

				String SOMigrationPipelineKey = YFSSystem.getProperty(CrocsConstant.A_SO_MIGRATION_PIPELINE_PROPERTY);
				if (!YFCCommon.isVoid(SOMigrationPipelineKey)) {
					OrderLine.setAttribute(CrocsConstant.A_PIPELINE_KEY, SOMigrationPipelineKey);
					isValid = true;
				} else {
					path = "/Order/OrderLines/OrderLine/@" + CrocsConstant.A_PIPELINE_KEY;
					errorMessage = "Invalid " + CrocsConstant.A_PIPELINE_KEY;
					errorCode = "YFS10460";
					errorDescription = "Provided value for " + CrocsConstant.A_PIPELINE_KEY + " is "
							+ SOMigrationPipelineKey
							+ ", Expected is valid pipeline provided in SMA with property name "
							+ CrocsConstant.A_SO_MIGRATION_PIPELINE_PROPERTY + " at xpath: " + path;
					break;
				}
			}
				break;

		case CrocsConstant.A_SO_MIGRATION_CA_PIPELINE_PROPERTY:
			
			/**
			 *  Pipeline is one of the mandatory attribute, pipeline key must be passed in the SMA, if found blank, 
			 *  it will throw an exception.
			 **/
			
			OrderLines = importOrderYdocEle.getElementsByTagName(CrocsConstant.E_ORDER_LINE);
			for (YFCElement OrderLine : OrderLines) {

				String SOMigrationPipelineKey = YFSSystem.getProperty(CrocsConstant.A_SO_MIGRATION_CA_PIPELINE_PROPERTY);
				if (!YFCCommon.isVoid(SOMigrationPipelineKey)) {
					OrderLine.setAttribute(CrocsConstant.A_PIPELINE_KEY, SOMigrationPipelineKey);
					isValid = true;
				} else {
					path = "/Order/OrderLines/OrderLine/@" + CrocsConstant.A_PIPELINE_KEY;
					errorMessage = "Invalid " + CrocsConstant.A_PIPELINE_KEY;
					errorCode = "YFS10460";
					errorDescription = "Provided value for " + CrocsConstant.A_PIPELINE_KEY + " is "
							+ SOMigrationPipelineKey
							+ ", Expected is valid pipeline provided in SMA with property name "
							+ CrocsConstant.A_SO_MIGRATION_CA_PIPELINE_PROPERTY + " at xpath: " + path;
					break;
				}
			}
				break;
				
       case CrocsConstant.A_SO_MIGRATION_CROCS_AU_PIPELINE_PROPERTY:
			
			/**
			 *  Pipeline is one of the mandatory attribute, pipeline key must be passed in the SMA, if found blank, 
			 *  it will throw an exception.
			 **/
			
			OrderLines = importOrderYdocEle.getElementsByTagName(CrocsConstant.E_ORDER_LINE);
			for (YFCElement OrderLine : OrderLines) {

				String SOMigrationPipelineKey = YFSSystem.getProperty(CrocsConstant.A_SO_MIGRATION_CROCS_AU_PIPELINE_PROPERTY);
				if (!YFCCommon.isVoid(SOMigrationPipelineKey)) {
					OrderLine.setAttribute(CrocsConstant.A_PIPELINE_KEY, SOMigrationPipelineKey);
					isValid = true;
				} else {
					path = "/Order/OrderLines/OrderLine/@" + CrocsConstant.A_PIPELINE_KEY;
					errorMessage = "Invalid " + CrocsConstant.A_PIPELINE_KEY;
					errorCode = "YFS10460";
					errorDescription = "Provided value for " + CrocsConstant.A_PIPELINE_KEY + " is "
							+ SOMigrationPipelineKey
							+ ", Expected is valid pipeline provided in SMA with property name "
							+ CrocsConstant.A_SO_MIGRATION_CROCS_AU_PIPELINE_PROPERTY + " at xpath: " + path;
					break;
				}
			}
				break;
				
			case A_RO_MIGRATION_PIPELINE_PROPERTY:

				OrderLines = importOrderYdocEle.getElementsByTagName(E_ORDER_LINE);
				for (YFCElement OrderLine : OrderLines) {

					String ROMigrationPipelineKey = YFSSystem.getProperty(A_RO_MIGRATION_PIPELINE_PROPERTY);
					if (!YFCCommon.isVoid(ROMigrationPipelineKey)) {
						OrderLine.setAttribute(A_PIPELINE_KEY, ROMigrationPipelineKey);
						isValid = true;
					} else {
						path = "/Order/OrderLines/OrderLine/@" + A_PIPELINE_KEY;
						errorMessage = "Invalid " + A_PIPELINE_KEY;
						errorCode = "YFS10460";
						errorDescription = "Provided value for " + A_PIPELINE_KEY + " is "
								+ ROMigrationPipelineKey
								+ ", Expected is valid pipeline provided in SMA with property name "
								+ A_RO_MIGRATION_PIPELINE_PROPERTY + " at xpath: " + path;
						break;
					}
				}
				break;
			
			case A_RO_MIGRATION_CA_PIPELINE_PROPERTY:

				OrderLines = importOrderYdocEle.getElementsByTagName(E_ORDER_LINE);
				for (YFCElement OrderLine : OrderLines) {

					String ROMigrationPipelineKey = YFSSystem.getProperty(A_RO_MIGRATION_CA_PIPELINE_PROPERTY);
					if (!YFCCommon.isVoid(ROMigrationPipelineKey)) {
						OrderLine.setAttribute(A_PIPELINE_KEY, ROMigrationPipelineKey);
						isValid = true;
					} else {
						path = "/Order/OrderLines/OrderLine/@" + A_PIPELINE_KEY;
						errorMessage = "Invalid " + A_PIPELINE_KEY;
						errorCode = "YFS10460";
						errorDescription = "Provided value for " + A_PIPELINE_KEY + " is "
								+ ROMigrationPipelineKey
								+ ", Expected is valid pipeline provided in SMA with property name "
								+ A_RO_MIGRATION_CA_PIPELINE_PROPERTY + " at xpath: " + path;
						break;
					}
				}
				break;
				
			case A_RO_MIGRATION_CROCS_AU_PIPELINE_PROPERTY:

				OrderLines = importOrderYdocEle.getElementsByTagName(E_ORDER_LINE);
				for (YFCElement OrderLine : OrderLines) {

					String ROMigrationPipelineKey = YFSSystem.getProperty(A_RO_MIGRATION_CROCS_AU_PIPELINE_PROPERTY);
					if (!YFCCommon.isVoid(ROMigrationPipelineKey)) {
						OrderLine.setAttribute(A_PIPELINE_KEY, ROMigrationPipelineKey);
						isValid = true;
					} else {
						path = "/Order/OrderLines/OrderLine/@" + A_PIPELINE_KEY;
						errorMessage = "Invalid " + A_PIPELINE_KEY;
						errorCode = "YFS10460";
						errorDescription = "Provided value for " + A_PIPELINE_KEY + " is "
								+ ROMigrationPipelineKey
								+ ", Expected is valid pipeline provided in SMA with property name "
								+ A_RO_MIGRATION_CROCS_AU_PIPELINE_PROPERTY + " at xpath: " + path;
						break;
					}
				}
				break;
			
		case CrocsConstant.A_SO_SHIPMENT_MIGRATION_PIPELINE_PROPERTY:

			/**
			 *  Pipeline is one of the mandatory attribute, pipeline key must be passed in the SMA, if found blank, 
			 *  it will throw an exception.
			 **/
			
				String shipmentOMigrationPipelineKey = YFSSystem.getProperty(CrocsConstant.A_SO_SHIPMENT_MIGRATION_PIPELINE_PROPERTY);
				if (!YFCCommon.isVoid(shipmentOMigrationPipelineKey)) {
					importOrderYdocEle.setAttribute(CrocsConstant.A_PIPELINE_KEY, shipmentOMigrationPipelineKey);
					isValid = true;
				} else {
					path = "/Shipment/@" + CrocsConstant.A_PIPELINE_KEY;
					errorMessage = "Invalid " + CrocsConstant.A_PIPELINE_KEY;
					errorCode = "YFS10460";
					errorDescription = "Provided value for " + CrocsConstant.A_PIPELINE_KEY + " is "
							+ shipmentOMigrationPipelineKey
							+ ", Expected is valid pipeline provided in SMA with property name "
							+ CrocsConstant.A_SO_SHIPMENT_MIGRATION_PIPELINE_PROPERTY + " at xpath: " + path;
					break;
				}
			
			break;
			
		case CrocsConstant.A_SO_SHIPMENT_MIGRATION_CA_PIPELINE_PROPERTY:

			/**
			 *  Pipeline is one of the mandatory attribute, pipeline key must be passed in the SMA, if found blank, 
			 *  it will throw an exception.
			 **/
			
				String shipmentOMigrationCAPipelineKey = YFSSystem.getProperty(CrocsConstant.A_SO_SHIPMENT_MIGRATION_CA_PIPELINE_PROPERTY);
				if (!YFCCommon.isVoid(shipmentOMigrationCAPipelineKey)) {
					importOrderYdocEle.setAttribute(CrocsConstant.A_PIPELINE_KEY, shipmentOMigrationCAPipelineKey);
					isValid = true;
				} else {
					path = "/Shipment/@" + CrocsConstant.A_PIPELINE_KEY;
					errorMessage = "Invalid " + CrocsConstant.A_PIPELINE_KEY;
					errorCode = "YFS10460";
					errorDescription = "Provided value for " + CrocsConstant.A_PIPELINE_KEY + " is "
							+ shipmentOMigrationCAPipelineKey
							+ ", Expected is valid pipeline provided in SMA with property name "
							+ CrocsConstant.A_SO_SHIPMENT_MIGRATION_CA_PIPELINE_PROPERTY + " at xpath: " + path;
					break;
				}
			
			break;
			
		case CrocsConstant.A_SO_SHIPMENT_MIGRATION_CROCS_AU_PIPELINE_PROPERTY:

			/**
			 *  Pipeline is one of the mandatory attribute, pipeline key must be passed in the SMA, if found blank, 
			 *  it will throw an exception.
			 **/
			
				String shipmentOMigrationAUPipelineKey = YFSSystem.getProperty(CrocsConstant.A_SO_SHIPMENT_MIGRATION_CROCS_AU_PIPELINE_PROPERTY);
				if (!YFCCommon.isVoid(shipmentOMigrationAUPipelineKey)) {
					importOrderYdocEle.setAttribute(CrocsConstant.A_PIPELINE_KEY, shipmentOMigrationAUPipelineKey);
					isValid = true;
				} else {
					path = "/Shipment/@" + CrocsConstant.A_PIPELINE_KEY;
					errorMessage = "Invalid " + CrocsConstant.A_PIPELINE_KEY;
					errorCode = "YFS10460";
					errorDescription = "Provided value for " + CrocsConstant.A_PIPELINE_KEY + " is "
							+ shipmentOMigrationAUPipelineKey
							+ ", Expected is valid pipeline provided in SMA with property name "
							+ CrocsConstant.A_SO_SHIPMENT_MIGRATION_CROCS_AU_PIPELINE_PROPERTY + " at xpath: " + path;
					break;
				}
			
			break;
			
		default:
			errorMessage = "Unknown attribute for mandatory check validation";
			errorCode = "";
			errorDescription = "Please add validation logic";
			break;
		}
		if (!isValid)
			throw new YFSException(errorMessage, errorCode, errorDescription);
		return isValid;
	}
	
    /**
     * @param golDoc - YFCElement of Order
     */
	public static boolean isValidSalesOrder(Document golDoc) {
		boolean isValid = false;

		if (!"0".equals(golDoc.getDocumentElement().getAttribute("TotalOrderList"))) 
			isValid = true;

		return isValid;
	}
	
    /**
     * @param golDoc - YFCElement of Order
     * @throws Exception 
     */
	public static boolean isImportOrderEligibleForPayment(YFSEnvironment env, String orderHeaderKey) {
		boolean isValid = true;

		/**
		 * Method to bypass the Payment UE Logic, 
		 * For Import SO , and Import RO , Payment logic is bypassed as EnteredBy="Migration"
		 * Post Import SO , if RO is created from CC , Payment is called for refund.
		 * 
		 * ChargeType RETURN will only get create when return will get created from CC.
		 * 
		 **/
		
		try {
			YFCDocument orderYfcDoc = YFCDocument.createDocument("Order");
			YFCElement orderYfcDocEle = orderYfcDoc.getDocumentElement();
			orderYfcDocEle.setAttribute(CrocsConstant.OrderHeaderKey, orderHeaderKey);

			Document getOrderListOutput = CommonUtil.invokeAPI(env,
					CrocsTemplateConstants.TEMPLATE_GET_ORDER_LIST_ORDER_EVENT_UPDATES,
					CrocsConstant.API_GET_ORDER_LIST, orderYfcDoc.getDocument());

			YFCDocument golOutYdoc = YFCDocument.getDocumentFor(getOrderListOutput);
			YFCElement golYdocEle = golOutYdoc.getDocumentElement();

			String enteredBy = golYdocEle.getChildElement("Order").getAttribute("EnteredBy");
			if ("Migration".equals(enteredBy))
				isValid = false;

			YFCNodeList<YFCElement> chargeTransactionDetails = golYdocEle.getChildElement("Order")
					.getElementsByTagName("ChargeTransactionDetail");
			for (YFCElement chargeTransactionDetail : chargeTransactionDetails) {
				if (!"RETURN".equals(chargeTransactionDetail.getAttribute("ChargeType"))
						&& "Migration".equals(enteredBy))
					isValid = false;
				
				if ("RETURN".equals(chargeTransactionDetail.getAttribute("ChargeType"))
						&& "Migration".equals(enteredBy)) {
					isValid = true;
					break;}
			}
		} catch (Exception e) {
			throw new YFSException(e.getMessage(), e.getCause().toString(), e.getStackTrace().toString());
		}

		return isValid;
	}
	
	/**
	 * This method will update the carrier service code at the order line level If
	 * mapping is not present then it will update with default value Economy for US
	 * and Standard for CA
	 *
	 * @param yfsEnvironment
	 * @param orderLine
	 * @param enterpriseCode
	 */
	public static void updateCarrierServiceCode(YFSEnvironment yfsEnvironment, YFCElement orderLine, String enterpriseCode) {
		logger.verbose("Start of method updateCarrierServiceCode with input: " + orderLine + " and enterpriseCode as: " + enterpriseCode);

		String carrierServiceCode = orderLine.getAttribute(A_CARRIER_SERVICE_CODE);

		// Check if carrierServiceCode is empty or null
		if (carrierServiceCode == null || carrierServiceCode.isEmpty()) {
		    logger.verbose("carrierServiceCode received is empty, updating it with default value");

		    // Set default based on enterpriseCode
		    switch (enterpriseCode) {
		        case CrocsConstant.CROCS_US:
		            orderLine.setAttribute(A_CARRIER_SERVICE_CODE, CrocsConstant.VAL_ECONOMY);
		            break;
		        case CrocsConstant.CROCS_CA:
		            orderLine.setAttribute(A_CARRIER_SERVICE_CODE, CrocsConstant.VAL_STANDARD);
		            break;
		        case CrocsConstant.CROCS_AU:
		            orderLine.setAttribute(A_CARRIER_SERVICE_CODE, CrocsConstant.VAL_AUSPOST);
		            break;
		        default:
		            logger.verbose("Unknown enterpriseCode: " + enterpriseCode);
		            break;
		    }
		    logger.verbose("End of method updateCarrierServiceCode with default service code");
		    return;
		}

		// If carrierServiceCode is not empty, fetch common code list
		Element getCommonCodeList = getCommonCodeList(yfsEnvironment, enterpriseCode, carrierServiceCode).getDocumentElement();

		String updatedCarrierServiceCode = SCXmlUtil.getXpathAttribute(getCommonCodeList, CrocsConstant.XPATH_CODE_SHORT_DESCRIPTION);

		// Check if the fetched updateCarrierServiceCode is empty
		if (updatedCarrierServiceCode == null || updatedCarrierServiceCode.isEmpty()) {

		    switch (enterpriseCode) {
		        case CrocsConstant.CROCS_US:
		            orderLine.setAttribute(A_CARRIER_SERVICE_CODE, CrocsConstant.VAL_ECONOMY);
		            break;
		        case CrocsConstant.CROCS_CA:
		            orderLine.setAttribute(A_CARRIER_SERVICE_CODE, CrocsConstant.VAL_STANDARD);
		            break;
		        case CrocsConstant.CROCS_AU:
		            orderLine.setAttribute(A_CARRIER_SERVICE_CODE, CrocsConstant.VAL_AUSPOST);
		            break;
		        default:
		            logger.verbose("Unknown enterpriseCode for update: " + enterpriseCode);
		            break;
		    }
		} else
		    orderLine.setAttribute(A_CARRIER_SERVICE_CODE, updatedCarrierServiceCode);

		logger.verbose("End of method updateCarrierServiceCode with updatedCarrierServiceCode: " + updatedCarrierServiceCode);
	}
	
	
	/**
	 * This function calls the getCommonCodeList api to get the carrierServiceCode
	 * mapping
	 *
	 * @param yfsEnvironment
	 * @param enterpriseCode
	 * @return
	 * @throws YFSException
	 */
	private static Document getCommonCodeList(YFSEnvironment yfsEnvironment, String enterpriseCode,	String carrierServiceCode) throws YFSException {
		logger.verbose("Start of method getCommonCodeList with carrierServiceCode: " + carrierServiceCode
				+ " and enterpriseCode as: " + enterpriseCode);

		// prepare the input to getCommonCodeList api
		Document inDoc = SCXmlUtil.createDocument(A_COMMON_CODE);
		Element inDocEle = inDoc.getDocumentElement();
		inDocEle.setAttribute(A_CODE_TYPE, CrocsConstant.VAL_CROCS_SFCC_CARRIER);
		inDocEle.setAttribute(A_ORGANIZATION_CODE, enterpriseCode);
		inDocEle.setAttribute(A_CODE_VALUE, carrierServiceCode);

		Document outDoc = null;
		// invoking the api
		try {
			logger.verbose("getCommonCodeList inDoc:: " + SCXmlUtil.getString(inDoc));
			outDoc = CommonUtil.invokeAPI(yfsEnvironment, CrocsTemplateConstants.TEMPLATE_GET_COMMON_CODE_LIST,
					CrocsAPIConstants.API_GET_COMMON_CODE_LIST, inDoc);

		} catch (Exception e) {
			logger.error("Error invoking getCommonCodeList API: " + e.getMessage());
			throw new YFSException("Error invoking getCommonCodeList API: " + e.getMessage());
		}
		logger.verbose("getCommonCodeList outDoc:: " + SCXmlUtil.getString(outDoc));

		return outDoc;
	}
	
	
    /**
     * @param currentClass - class for which we want to log current method
     * @return class_name : current_method_name
     **/
    public static String logCurrentMethod(Class<?> currentClass) {
        return currentClass.getName() + " : " + Thread.currentThread().getStackTrace()[2].getMethodName();
    }

}
