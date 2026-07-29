package com.crocs.oms.order.migration.emea;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.shipment.emea.CrocsEMEAShipmentUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.dom.YFCNodeList;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.core.YFSSystem;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class CrocsEMEAMigarationUtil implements CrocsConstant {
    private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsEMEAMigarationUtil.class);

    /**
     * @param importOrderYdocEle - YFCElement of Order
     * @param mandatoryAttributeToValidate - Attribute we want to validate
     * @return Flag if the value is valid otherwise throws error
     */

    public static boolean validateMandatoryAttribute(YFCElement importOrderYdocEle, String mandatoryAttributeToValidate) {

        String errorCode = "", errorMessage = "", errorDescription = "", path = "";
        boolean isValid = false;
        YFCNodeList<YFCElement> orderLines = null;
        String documentTypeVal = "";

        switch (mandatoryAttributeToValidate) {

            case A_DOCUMENT_TYPE:
                String documentType = importOrderYdocEle.getAttribute(A_DOCUMENT_TYPE);
                if (!YFCCommon.isVoid(documentType))
                    isValid = true;
                else {
                    path = "/Order/@" + A_DOCUMENT_TYPE;
                    errorMessage = "Invalid " + A_DOCUMENT_TYPE;
                    errorCode = "";
                    errorDescription = "Provided value for " + A_DOCUMENT_TYPE + " is null or empty at xpath: " + path;
                }
                break;

            case VAL_DOCUMENT_TYPE_SALES_ORDER:
                documentTypeVal = importOrderYdocEle.getAttribute(A_DOCUMENT_TYPE);
                if (VAL_DOCUMENT_TYPE_SALES_ORDER.equalsIgnoreCase(documentTypeVal))
                    isValid = true;
                else {
                    path = "/Order/@" + A_DOCUMENT_TYPE;
                    errorMessage = "Invalid " + A_DOCUMENT_TYPE;
                    errorCode = "";
                    errorDescription = "Expected [0001] but found " + documentTypeVal + " at xpath: " + path;
                }
                break;

            case A_RETURN_ORDER_DOCUMENT_TYPE:
                documentTypeVal = importOrderYdocEle.getAttribute(A_DOCUMENT_TYPE);
                if (A_RETURN_ORDER_DOCUMENT_TYPE.equalsIgnoreCase(documentTypeVal))
                    isValid = true;
                else {
                    path = "/Order/@" + A_DOCUMENT_TYPE;
                    errorMessage = "Invalid " + A_DOCUMENT_TYPE;
                    errorCode = "";
                    errorDescription = "Expected [0003] but found " + documentTypeVal + " at xpath: " + path;
                }
                break;

            case A_ENTERED_BY:
                String enteredBy = importOrderYdocEle.getAttribute(A_ENTERED_BY);
                if (!YFCCommon.isVoid(enteredBy) && enteredBy.equalsIgnoreCase(V_MIGRATION))
                    isValid = true;
                else {
                    path = "/Order/@" + A_ENTERED_BY;
                    errorMessage = "Invalid " + A_ENTERED_BY;
                    errorCode = "";
                    errorDescription = "Expected [Migration] but found " + enteredBy + " at xpath: " + path;
                }
                break;

            case A_ENTERPRISE_CODE:
                String enterpriseCode = importOrderYdocEle.getAttribute(A_ENTERPRISE_CODE);
                if (!YFCCommon.isVoid(enterpriseCode))
                    isValid = true;
                else {
                    path = "/Order/@" + A_ENTERPRISE_CODE;
                    errorMessage = "Invalid " + A_ENTERPRISE_CODE;
                    errorCode = "";
                    errorDescription = "Enterprise code missing. Expected [CROCS_DE, CROCS_EU, CROCS_FI, CROCS_FR, CROCS_GB, CROCS_NL, HEYDUDE_DE, HEYDUDE_EU, HEYDUDE_FR, HEYDUDE_GB] at xpath: " + path;
                }
                break;

            case A_ORDER_NO:
                String orderNo = importOrderYdocEle.getAttribute(A_ORDER_NO);
                if (!YFCCommon.isVoid(orderNo))
                    isValid = true;
                else {
                    path = "/Order/@" + A_ORDER_NO;
                    errorMessage = "Invalid " + A_ORDER_NO;
                    errorCode = "";
                    errorDescription = "Order number missing at xpath: " + path;
                }
                break;

            case A_CONDITION_VARIABLE_1:
                orderLines = importOrderYdocEle.getElementsByTagName(E_ORDER_LINE);
                for (YFCElement line : orderLines) {
                    String conditionVariable1 = line.getAttribute(A_CONDITION_VARIABLE_1);
                    if (!YFCCommon.isVoid(conditionVariable1) && conditionVariable1.equalsIgnoreCase(V_MIGRATION))
                        isValid = true;
                    else {
                        path = "/Order/OrderLines/OrderLine/@" + A_CONDITION_VARIABLE_1;
                        errorMessage = "Invalid " + A_CONDITION_VARIABLE_1;
                        errorCode = "";
                        errorDescription = "Expected [Migration] but found " + conditionVariable1 + " at xpath: " + path;
                        break;
                    }
                }
                break;

            //Dynamic pipeline property based on enterprise code
            default:

                // SALES ORDER PIPELINE
                if (mandatoryAttributeToValidate.endsWith("_SO_MIGRATION_PIPELINE_KEY")) {
                    return validatePipelineForSO(importOrderYdocEle, mandatoryAttributeToValidate);
                }
                //SHIPMENT PIPELINE
                else if (mandatoryAttributeToValidate.endsWith("_SHIPMENT_MIGRATION_PIPELINE_KEY")) {
                    return validatePipelineForShipment(importOrderYdocEle, mandatoryAttributeToValidate);
                }
                // RETURN ORDER PIPELINE
                else if (mandatoryAttributeToValidate.endsWith("_RO_MIGRATION_PIPELINE_KEY")) {
                    return validatePipelineForRO(importOrderYdocEle, mandatoryAttributeToValidate);
                } else {
                    throw new YFSException("Unknown attribute for mandatory validation", "", 
                    		"Attribute [" + mandatoryAttributeToValidate + "] not handled. Please add validation logic");
                }
        }

        if (!isValid)
            throw new YFSException(errorMessage, errorCode, errorDescription);

        return isValid;
    }

    /**
     * Dynamic pipeline validation & stamping
     */
    private static boolean validatePipelineForSO(YFCElement orderElem, String propertyName) {

        String pipelineKey = YFSSystem.getProperty(propertyName);
        logger.verbose("pipelineKey for SO is::"+pipelineKey);

        if (YFCCommon.isVoid(pipelineKey)) {
            throw new YFSException(
                    "Invalid Pipeline",
                    "YFS10460",
                    "Missing SMA value for pipeline property: " + propertyName
            );
        }

        // Stamp SO pipeline on each order line
        YFCNodeList<YFCElement> orderLines =
                orderElem.getElementsByTagName(E_ORDER_LINE);

        for (YFCElement line : orderLines) {
            line.setAttribute(CrocsConstant.A_PIPELINE_KEY, pipelineKey);
        }

        return true;
    }

    private static boolean validatePipelineForShipment(YFCElement shipmentElem, String propertyName) {

        String pipelineKey = YFSSystem.getProperty(propertyName);
        logger.verbose("pipelineKey for Shipment is: "+pipelineKey);

        if (YFCCommon.isVoid(pipelineKey)) {
            throw new YFSException(
                    "Invalid Pipeline",
                    "YFS10460",
                    "Missing SMA value for pipeline property: " + propertyName
            );
        }

        // Stamp Shipment pipeline on the root element
        shipmentElem.setAttribute(CrocsConstant.A_PIPELINE_KEY, pipelineKey);

        return true;
    }

    private static boolean validatePipelineForRO(YFCElement orderElem, String propertyName) {

        String pipelineKey = YFSSystem.getProperty(propertyName);
        logger.verbose("pipelineKey for RO is::"+pipelineKey);

        if (YFCCommon.isVoid(pipelineKey)) {
            throw new YFSException(
                    "Invalid Pipeline",
                    "YFS10460",
                    "Missing SMA value for pipeline property: " + propertyName
            );
        }

        YFCNodeList<YFCElement> orderLines =
                orderElem.getElementsByTagName(E_ORDER_LINE);

        for (YFCElement line : orderLines) {
            line.setAttribute(A_PIPELINE_KEY, pipelineKey);
        }

        return true;
    }

    public static boolean isValidSalesOrder(Document golDoc) {
        boolean isValid = false;

        if (!"0".equals(golDoc.getDocumentElement().getAttribute(A_TOTAL_ORDER_LIST)))
            isValid = true;

        return isValid;
    }

    /**
     * This method will update the carrier service code at the order line level If
     * mapping is not present then it will update with default value Standard 
     *     
     * @param yfsEnvironment
     * @param orderLine
     * @param enterpriseCode
     */
	public static void updateCarrierServiceCode(YFSEnvironment yfsEnvironment, YFCElement orderLine, String enterpriseCode) {
		logger.verbose("Start of method updateCarrierServiceCode with input: " + orderLine + " and enterpriseCode as: " + enterpriseCode);

		orderLine.setAttribute(A_CARRIER_SERVICE_CODE, CrocsConstant.VAL_STANDARD);

		logger.verbose("End of method updateCarrierServiceCode with orderLine as : " + orderLine);

	}


    /**
     * @param currentClass - class for which we want to log current method
     * @return class_name : current_method_name
     **/
    public static String logCurrentMethod(Class<?> currentClass) {
        return currentClass.getName() + " : " + Thread.currentThread().getStackTrace()[2].getMethodName();
    }
    
    /**
	 * Forms the carrier tracking URL based on the provided tracking number
	 * and enterprise configuration.
	 *
	 * @param env             YFS environment used to invoke APIs
	 * @param trackingData    tracking number data from the order/shipment
	 * @param enterpriseCode  enterprise/organization code
	 * @return formatted carrier tracking URL or empty string if unavailable
	 * @throws Exception in case of API or processing errors
	 */
	public static String getTrackingURL(YFSEnvironment env, YFCElement orderDetails) throws Exception {
		logger.verbose("Migration : OMS_UPDATE : getTrackingURL : START ");
		logger.verbose("getTrackingURL : orderDetails: " + orderDetails);
		/**
		 * As we have observed there could be two pattren for trackingNo
		 * pattern 01 : &#xa; 1Z6F857YYW86116475|UPS&#xa;
		 * Pattern 02 : &#xa; 1Z6F857YYW86116475|UPS&#xa; 1Z6F857YYW86143767|UPS&#xa;
		 * Pattern 03 : &#xA; 9200190383434300018479&#xA; 1Z6F857YYN36114041&#xA;
		 * Pattern 04 : &#10; 803356510503233324&#10; 9200190383434300020854&#10;
		 * Pattern 05 : &#xa; 920043015566603 &#xa;
		 * In pattern 02,03,04 data in the column is exceeding the set limit, so it needs to be trimmed. 
		 * 
		 * As we are extracting the tracking no and scac using split by "|" , below line will help delealing with all above pattern 
		 * trackingNo.trim().split("\\s")[0];
		 * 
		 * In all above patern we will consider the first set of data.
		 * In pattern 03,04 and 05,we are receiving tracking no without scac and "|",so this is handled in else loop
		 * 
		 **/
		
		YFCElement eleShipment = orderDetails.getChildElement(E_SHIPMENT);
		
		String trackingData = eleShipment.getAttribute(A_TRACKING_NO);


		String strPrimaryUrl = "";

		if (YFCCommon.isVoid(trackingData)) {
			return "";
		}

		if (trackingData.contains("|")) {
			String[] trackingNoSplits = trackingData.split("\\|");

	        String trackingNumber = trackingNoSplits[0].trim();
	        String scacCode = trackingNoSplits[1].trim();
	        
	        CrocsEMEAShipmentUtil emeaShipmentUtilObj = new CrocsEMEAShipmentUtil();
	        
	        Document docScacDetails = emeaShipmentUtilObj.invokeCommonCodeListForSCACDetail(env, scacCode);
			Element eleScacDetails = docScacDetails.getDocumentElement();

			// Fetch OMS-configured SCAC value and SCAC-Service mapping
			String scacValue = SCXmlUtil.getXpathAttribute(eleScacDetails, XPATH_CODE_SHORT_DESCRIPTION);

			logger.verbose("Resolved OMS SCAC value: " + scacValue);

			// Prepare getOrganizationList input document
			Document getOrganizationListInDoc = SCXmlUtil.createDocument(E_ORGANIZATION);
			getOrganizationListInDoc.getDocumentElement().setAttribute(A_ORGANIZATION_CODE, scacValue);

			Document getOrganizationListOutDoc;

			// Invoke getOrganizationList API
			logger.verbose("Calling getOrganizationList API with input: " + SCXmlUtil.getString(getOrganizationListInDoc));
			getOrganizationListOutDoc = CommonUtil.invokeAPI(env, TEMPLATE_GET_ORGANIZATION_LIST, API_GET_ORGANIZATION_LIST, getOrganizationListInDoc);
			logger.verbose("Output returned from getOrganizationList API: " + SCXmlUtil.getString(getOrganizationListOutDoc));

			// Fetch tracking URL template from organization details
			strPrimaryUrl = SCXmlUtil.getXpathAttribute(getOrganizationListOutDoc.getDocumentElement(), XPAH_PRIMARY_URL);

			Document docOrderReleaseList = SCXmlUtil.createDocument(E_ORDER_RELEASE_LIST);
			Element eleOrderReleaseList = docOrderReleaseList.getDocumentElement();

			Element eleOrderRelease = SCXmlUtil.createChild(eleOrderReleaseList, E_ORDER_RELEASE);
			eleOrderRelease.setAttribute(A_ENTERPRISE_CODE, orderDetails.getAttribute(A_ENTERPRISE_CODE));

			YFCElement extn = orderDetails.getChildElement(E_EXTN);
			if (!YFCCommon.isVoid(extn)) {
			    Element eleOrder = SCXmlUtil.createChild(eleOrderRelease, E_ORDER);
			    Element eleExtn = SCXmlUtil.createChild(eleOrder, E_EXTN);
			    eleExtn.setAttribute(EXTN_CUSTOMER_LOCALE, extn.getAttribute(EXTN_CUSTOMER_LOCALE));
			}

			YFCElement personInfoShipTo = orderDetails.getChildElement(E_PERSON_INFO_SHIP_TO);
			if (!YFCCommon.isVoid(personInfoShipTo)) {
			    Element eleShipToAddress = SCXmlUtil.createChild(eleOrderRelease, E_PERSON_INFO_SHIP_TO);
			    eleShipToAddress.setAttribute(A_ZIP_CODE, personInfoShipTo.getAttribute(A_ZIP_CODE));
			}
						
			if (!YFCCommon.isVoid(strPrimaryUrl)) {
				strPrimaryUrl = new CrocsEMEAShipmentUtil().updateTrackingURLAsPerLocale(strPrimaryUrl, scacValue, docOrderReleaseList);
				strPrimaryUrl = strPrimaryUrl.replaceAll(A_TRACKING_NO, trackingNumber.replaceAll("&", "&amp;"));

				// updating tracking url at shipment so that it can be used later when importing shipment
				eleShipment.setAttribute(A_TRACKING_URL, strPrimaryUrl);
				eleShipment.setAttribute(A_SCAC, scacValue);
			}
		}
		logger.verbose("getTrackingURL : strPrimaryUrl: " + strPrimaryUrl);
		logger.verbose("Migration : OMS_UPDATE : getTrackingURL : END ");
		return strPrimaryUrl;
	}
}
