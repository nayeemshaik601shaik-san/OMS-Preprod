package com.crocs.oms.order.migration.heydude;

import com.crocs.oms.common.util.CrocsConstant;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.crocs.oms.common.util.CommonUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.dom.YFCNodeList;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCException;
import com.yantra.yfs.core.YFSSystem;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;


public class HeyDudeMigarationUtil implements CrocsConstant {
    private static final YFCLogCategory logger = YFCLogCategory.instance(HeyDudeMigarationUtil.class);

    /**
     * @param importOrderYdocEle - YFCElement of Order
     * @param mandatoryAttributeToValidate - Attribute we want to validate
     * @return Flag if the value is valid otherwise throws error
     */

    public static boolean validateMandatoryAttribute(YFCElement importOrderYdocEle,
                                                     String mandatoryAttributeToValidate) {

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
                    errorDescription = "Enterprise code missing. Expected [HEYDUDE_US,HEYDUDE_CA,HEYDUDE_AU] at xpath: " + path;
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
                if (mandatoryAttributeToValidate.endsWith("_SO_PIPELINE_KEY")) {
                    return validatePipelineForSO(importOrderYdocEle, mandatoryAttributeToValidate);
                }
                //SHIPMENT PIPELINE
                else if (mandatoryAttributeToValidate.endsWith("_SHIPMENT_MIGRATION_PIPELINE_KEY")) {
                    return validatePipelineForShipment(importOrderYdocEle, mandatoryAttributeToValidate);
                }
                // RETURN ORDER PIPELINE
                else if (mandatoryAttributeToValidate.endsWith("_RO_PIPELINE_KEY")) {
                    return validatePipelineForRO(importOrderYdocEle, mandatoryAttributeToValidate);
                } else {
                    throw new YFSException(
                            "Unknown attribute for mandatory validation",
                            "",
                            "Attribute [" + mandatoryAttributeToValidate + "] not handled. Please add validation logic."
                    );
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
                orderElem.getElementsByTagName(CrocsConstant.E_ORDER_LINE);

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
                case HEYDUDE_US:
                    orderLine.setAttribute(A_CARRIER_SERVICE_CODE, CrocsConstant.VAL_ECONOMY);
                    break;
                case HEYDUDE_CA:
                case HEYDUDE_AU:
                    orderLine.setAttribute(A_CARRIER_SERVICE_CODE, CrocsConstant.VAL_STANDARD);
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
                case HEYDUDE_US:
                    orderLine.setAttribute(A_CARRIER_SERVICE_CODE, CrocsConstant.VAL_ECONOMY);
                    break;
                case HEYDUDE_CA:
                case HEYDUDE_AU:
                    orderLine.setAttribute(A_CARRIER_SERVICE_CODE, CrocsConstant.VAL_STANDARD);
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
        inDocEle.setAttribute(A_CODE_TYPE,VAL_CROCS_SFCC_CARRIER);
        inDocEle.setAttribute(A_ORGANIZATION_CODE, enterpriseCode);
        inDocEle.setAttribute(A_CODE_VALUE, carrierServiceCode);

        Document outDoc = null;

        try {
            logger.verbose("getCommonCodeList inDoc:: " + SCXmlUtil.getString(inDoc));
            outDoc = CommonUtil.invokeAPI(yfsEnvironment,TEMPLATE_GET_COMMON_CODE_LIST,API_GET_COMMON_CODE_LIST, inDoc);

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
	public static String getTrackingURL(YFSEnvironment env, String trackingData, String enterpriseCode, String customerLocale) throws Exception {
		logger.verbose("HeyDudeMigarationUtil: Start of method getTrackingURL ");
		logger.verbose("HeyDudeMigarationUtil: getTrackingURL: trackingData " + trackingData);
		logger.verbose("HeyDudeMigarationUtil: getTrackingURL: enterpriseCode: " + enterpriseCode);
		logger.verbose("HeyDudeMigarationUtil: getTrackingURL: customerLocale: " + customerLocale);
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

		String trackingUrl = "";

		if (YFCCommon.isVoid(trackingData)) {
			return trackingUrl;
		}

		if (trackingData.contains("|")) {
			String[] trackingNoSplits = trackingData.split("\\|");

	        String trackingNumber = trackingNoSplits[0].trim();
	        String scacCode = trackingNoSplits[1].trim();

	        // get the common code for the particular SCAC value mapped
			Document commonCodeDoc = fetchScacCommonCode(env, scacCode, enterpriseCode);
			Element commonCodeEle = commonCodeDoc.getDocumentElement();
			
			// short description will have the organization code to form the URL
			String carrierOrgCode = SCXmlUtil.getXpathAttribute(commonCodeEle, XPATH_CODE_SHORT_DESCRIPTION);
			
			YFCDocument organizationYDoc = YFCDocument.createDocument(E_ORGANIZATION);
			YFCElement organizationYDocEle = organizationYDoc.getDocumentElement();
			
			organizationYDocEle.setAttribute(A_ORGANIZATION_CODE, carrierOrgCode);
			
			logger.verbose("HeyDudeMigarationUtil:: fetchScacCommonCode:: Invoking getOrganizationList with organizationYDoc:: " + organizationYDoc.toString());
			Document getOrganizationListOutDoc = CommonUtil.invokeAPI(env, TEMPLATE_GET_ORGANIZATION_LIST, API_GET_ORGANIZATION_LIST, organizationYDoc.getDocument());
			logger.verbose("HeyDudeMigarationUtil:: fetchScacCommonCode:: Output of getOrganizationList: " + SCXmlUtil.getString(getOrganizationListOutDoc));

			trackingUrl = SCXmlUtil.getXpathAttribute(getOrganizationListOutDoc.getDocumentElement(), XPAH_PRIMARY_URL);

			if (!YFCCommon.isVoid(trackingUrl)) {
				trackingUrl = trackingUrl.replaceAll(A_TRACKING_NO, trackingNumber.replaceAll("&", "&amp;"));
				trackingUrl = updatePrimaryURLAsPerLoacale(trackingUrl, enterpriseCode, customerLocale);

			}
		}
		logger.verbose("HeyDudeMigarationUtil: End of method getTrackingURL with final trackingUrl as: " + trackingUrl);
		return trackingUrl;
	}
	
	/**
	 * Updates the locale of the given tracking URL based on the enterprise code and
	 * the customer's locale.
	 *
	 * <p>
	 * If the tracking URL contains the placeholder locale token ({@code locale}),
	 * this method determines the appropriate locale value using the provided
	 * enterprise code and customer locale, and replaces the placeholder
	 * accordingly.
	 * </p>
	 *
	 * @param trackingURL    the original tracking URL that may contain a locale
	 *                       placeholder
	 * @param enterpriseCode the enterprise identifier used to determine the target
	 *                       locale
	 * @param customerLocale the customer's locale, used primarily for CA locale
	 *                       resolution
	 * @return the updated tracking URL with the correct locale applied, or the
	 *         original URL if no locale placeholder is present
	 */
	private static String updatePrimaryURLAsPerLoacale(String trackingURL, String enterpriseCode, String customerLocale) {
		logger.verbose("HeyDudeMigarationUtil: Start of method updatePrimaryURLAsPerLoacale: ");
		logger.verbose("HeyDudeMigarationUtil: updatePrimaryURLAsPerLoacale: trackingURL: " + trackingURL);
		logger.verbose("HeyDudeMigarationUtil: updatePrimaryURLAsPerLoacale: enterpriseCode: " + enterpriseCode);
		logger.verbose("HeyDudeMigarationUtil: updatePrimaryURLAsPerLoacale: enterpriseCode: " + customerLocale);

		String localeToBeUpdated = "";

		if (trackingURL.contains(N_LOCALE)) {
			switch (enterpriseCode) {
			case HEYDUDE_US:
				localeToBeUpdated = LOCALE_US_EN;
				break;
			case HEYDUDE_CA:
				if (customerLocale.equals(STR_EN_CA))
					localeToBeUpdated = LOCALE_CA_EN;
				else
					localeToBeUpdated = LOCALE_CA_FR;
				break;
			case HEYDUDE_AU:
				localeToBeUpdated = LOCALE_AU_EN;
				break;
			default:
				localeToBeUpdated = LOCALE_US_EN;
				break;
			}
			trackingURL = trackingURL.replace(N_LOCALE, localeToBeUpdated);
		}
		logger.verbose("HeyDudeMigarationUtil: End of method updatePrimaryURLAsPerLoacale with updated trackingURL: " + trackingURL);
		return trackingURL;
	}

	/**
	 * Retrieves the SCAC common code details for the given enterprise by invoking
	 * the getCommonCodeList API.
	 *
	 * @param env            the YFS environment used to invoke APIs
	 * @param scac           the Standard Carrier Alpha Code to be looked up
	 * @param enterpriseCode the enterprise/organization code for which the SCAC
	 *                       details are retrieved
	 * @return the output {@link Document} returned by the getCommonCodeList API
	 *         containing SCAC common code details
	 * @throws Exception if an error occurs while building the request or invoking
	 *                   the API
	 */
	private static Document fetchScacCommonCode(YFSEnvironment env, String scac, String enterpriseCode) throws Exception {
		logger.verbose("HeyDudeMigarationUtil: Start of method fetchScacCommonCode: ");
		logger.verbose("HeyDudeMigarationUtil: fetchScacCommonCode: scac: " + scac);
		logger.verbose("HeyDudeMigarationUtil: fetchScacCommonCode: enterpriseCode: " + enterpriseCode);
		
		try {
	        Document commonCodeInDoc = SCXmlUtil.createDocument(A_COMMON_CODE);
	        Element commonCodeInDocEle = commonCodeInDoc.getDocumentElement();
	        
	        commonCodeInDocEle.setAttribute(A_CODE_TYPE, STR_CROCS_SCAC_NAMES);
	        commonCodeInDocEle.setAttribute(A_CODE_VALUE, scac);
	        commonCodeInDocEle.setAttribute(A_ORGANIZATION_CODE, enterpriseCode);

	        logger.verbose("HeyDudeMigarationUtil:: fetchScacCommonCode:: Invoking getCommonCodeList with:: commonCodeInDoc " + SCXmlUtil.getString(commonCodeInDoc));
	        Document outDoc = CommonUtil.invokeAPI(env, TEMPLATE_GET_COMMON_CODE_LIST, API_GET_COMMON_CODE_LIST, commonCodeInDoc);
	        logger.verbose("HeyDudeMigarationUtil:: fetchScacCommonCode:: Output of getCommonCodeList with:: commonCodeInDoc " + SCXmlUtil.getString(outDoc));

			logger.verbose("HeyDudeMigarationUtil: End of method fetchScacCommonCode with outDoc: " + SCXmlUtil.getString(outDoc));
	        return outDoc;
	        
	    } catch (Exception e) {
	        throw new YFCException("Error in fetchScacCommonCode: " + e.getMessage());
	    }
	}
}
