package com.crocs.oms.order;

import java.util.ArrayList;
import java.util.Map;

import com.crocs.oms.common.util.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;

/**
 * EOMS-690 This class is created to first calculate order priority
 * based upon business rules such as PaymentMethod, OrderType, Reship(in futureScope)
 * and accordingly update this value in OMS and update same in message which is
 * sent to WMS
 *
 * @author IBM
 *
 */
public class CrocsCalculateOrderPriorityAndSendToWMS implements CrocsConstant  {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsCalculateOrderPriorityAndSendToWMS.class);

	/**
	 * This method does the below
	 * 1. CalculatePriorityNo based upon business conditions/Rules
	 * 2. Send same field with appropriate value to WMS in release on success message
	 * @param env
	 * @param indoc
	 * @return
	 */
	public Document sendReleaseDetailsToWMS(YFSEnvironment env, Document indoc){

		if(logger.isDebugEnabled()){
			logger.debug("Input to CrocsSendReleaseDtlsToWMS:sendReleaseDetailsToWMS() " +SCXmlUtil.getString(indoc));
		}


		try{

			Element orderRelease = indoc.getDocumentElement();
			Element order = SCXmlUtil.getChildElement(orderRelease,E_ORDER);
            String enterpriseCode = order.getAttribute(A_ENTERPRISE_CODE);
			
			logger.debug("orderRelease is:: " +SCXmlUtil.getString(orderRelease));
			logger.debug("orderEle is:: " +SCXmlUtil.getString(order));

            /* EOMS-7289 changes start : adding new attribute "IsShortShipFlag" to stop sending Release Update msg to UPS for ShortShip*/
            if (CROCS_CA.equals(enterpriseCode)) {
                String shortShipped = (String) env.getTxnObject("IsCancelled");

                if ("Y".equals(shortShipped)) {
                    indoc.getDocumentElement().setAttribute("IsShortShipFlag", FLAG_Y);
                    return indoc;
                }
            }
            /*EOMS-7289 changes end*/

            /* EOMS -12322 start : Add Additional attributes on Release message to WMS for EMEA */
            if (CROCS_EMEA_ENTERPRISES.contains(enterpriseCode)) {
                addAdditionalAttributesToReleaseMsg(env, orderRelease, enterpriseCode);
            }
            /*EOMS -12322 end*/

			//evaluating order priority based upon Payment methods
			boolean isPriorityPaymentTrue = calculateOrderPriorityByPaymentMethods(env, order);
			boolean isPrioirtyOrderTypeTrue = calculateOrderPrioritybyOrderType(env, order);

			logger.debug("isPriorityPaymentTrue "+isPriorityPaymentTrue);
			logger.debug("isPrioirtyOrderTypeTrue "+isPrioirtyOrderTypeTrue );


			//Order is having Either CashApp or AfterPay as paymentMethod
			if(isPriorityPaymentTrue){
				//updating order priority in OMS
				updateReleaseMsgWithPriorityNo(env, order, VAL_PRIORITY_10);
				logger.debug("UpdatePriorityOnOrder completes with priority as 10 " );
				
			}else if(isPrioirtyOrderTypeTrue){
				//EOMS-5545 related changes
				updateReleaseMsgWithPriorityNo(env, order, VAL_PRIORITY_10);
				logger.debug("UpdatePriorityOnOrder completes with priority as 10 " );
				
			}else{
				//Non priority order,updating order priority as 100 in out message
				updateReleaseMsgWithPriorityNo(env, order, VAL_PRIORITY_100);
				logger.debug("UpdatePriorityOnOrder completes with priority as 100" );
				
			}

		}catch(Exception e){
			logger.debug("Exception in method sendReleaseDetailsToWMS "+e.getLocalizedMessage());
		}

		if(logger.isDebugEnabled()){
			logger.debug("Ends of method sendReleaseDetailsToWMS() with order details: " +SCXmlUtil.getString(indoc));
		}

		return indoc;
	}

    /**EOMS -12322 start Additional attributes on Release message to WMS for EMEA*
     * this method fetches the billToTitle, FederatedStoreNbr,DcCenterNbr from CommonCode and set
     * in Release msg to Send to WMS for EMEA enterprises
     * In case of no Country matched values will be default to 9999.
     * @param env
     * @param orderRelease
     * @param enterpriseCode
     */
    private static void addAdditionalAttributesToReleaseMsg(YFSEnvironment env, Element orderRelease, String enterpriseCode) {
        Element personInfoShipToEle = SCXmlUtil.getChildElement(orderRelease, E_PERSON_INFO_SHIP_TO);
        String country = personInfoShipToEle.getAttribute(A_COUNTRY);

        final Map<String, String> COUNTRY_NAMES =
                Map.ofEntries(
                        Map.entry(COUNTRY_CD_AUSTRIA, COUNTRY_AUSTRIA),
                        Map.entry(COUNTRY_CD_BELGIUM, COUNTRY_BELGIUM),
                        Map.entry(COUNTRY_CD_CZECH_REPUBLIC, COUNTRY_CZECH_REPUBLIC),
                        Map.entry(COUNTRY_CD_DENMARK, COUNTRY_DENMARK),
                        Map.entry(COUNTRY_CD_FRANCE, COUNTRY_FRANCE),
                        Map.entry(COUNTRY_CD_GERMANY, COUNTRY_GERMANY),
                        Map.entry(COUNTRY_CD_GREECE, COUNTRY_GREECE),
                        Map.entry(COUNTRY_CD_IRELAND, COUNTRY_IRELAND),
                        Map.entry(COUNTRY_CD_LUXEMBOURG, COUNTRY_LUXEMBOURG),
                        Map.entry(COUNTRY_CD_MONACO, COUNTRY_MONACO),
                        Map.entry(COUNTRY_CD_SLOVAKIA, COUNTRY_SLOVAKIA),
                        Map.entry(COUNTRY_CD_NETHERLANDS, COUNTRY_NETHERLANDS),
                        Map.entry(COUNTRY_CD_UK, COUNTRY_UK),
                        Map.entry(COUNTRY_CD_FINLAND, COUNTRY_FINLAND)
                );

        String countryName = COUNTRY_NAMES.get(country);
        String emeaReleaseAttr = enterpriseCode.contains("CROCS")
                ? "crocs.com"
                : "heydude.com";

        if (!YFCCommon.isVoid(countryName)) {
            emeaReleaseAttr = enterpriseCode.contains("CROCS")
                    ? "Internetsales - " + countryName
                    : "HD INTERNETSALES - " + countryName.toUpperCase();

            if (enterpriseCode.contains("HEYDUDE")
                    && (COUNTRY_CD_UK.equals(country)
                    || COUNTRY_CD_NETHERLANDS.equals(country))) {
                emeaReleaseAttr = country.contains(COUNTRY_CD_UK)
                        ? COUNTRY_HD_UK : COUNTRY_HD_NETHERLANDS ;
            }
        }

        Document docGetCommonCodeList = CommonUtil.getCommonCodeList(env,STR_CROCS_EMEA,STR_EMEA_RELEASE_ATTR, emeaReleaseAttr);
        logger.verbose("CrocsCalculateOrderPriorityAndSendToWMS : sendReleaseDetailsToWMS : getCommonCodeListOutput:" + SCXmlUtil.getString(docGetCommonCodeList));
        if (!YFCCommon.isVoid(docGetCommonCodeList)) {
            String FederatedStoreNbr = SCXmlUtil.getXpathAttribute(docGetCommonCodeList.getDocumentElement(), "//CommonCodeList/CommonCode[1]/@CodeShortDescription");
            String DcCenterNbr = SCXmlUtil.getXpathAttribute(docGetCommonCodeList.getDocumentElement(), "//CommonCodeList/CommonCode[1]/@CodeLongDescription");
            orderRelease.setAttribute(A_REL_BILL_TO_TITLE, emeaReleaseAttr);
            orderRelease.setAttribute(A_REL_FEDERATED_STORE_NBR, FederatedStoreNbr);
            orderRelease.setAttribute(A_REL_DC_CENTER_NBR, DcCenterNbr);
        }
        logger.verbose("Additional Attributes added in Release Msg : CrocsCalculateOrderPriorityAndSendToWMS: addAdditionalAttributesToReleaseMsg"+ XMLUtil.getElementXMLString(orderRelease));
    }
    /**
	 * This methods checks for order priority based upon
	 * payment methods, as of now CashApp and Afterpay are
	 * considered
	 * @param env
	 * @param order
	 * @return
	 */
	private boolean calculateOrderPriorityByPaymentMethods(YFSEnvironment env, Element order){

		if(logger.isDebugEnabled()){
			logger.debug("Starts of method calculateOrderPriorityByPaymentMethods() with order details: "
					+SCXmlUtil.getString(order));
		}

		boolean isPriorityPaymentMethod = false;
		String strOrganizationCode = "";

		//EOMS-6468 & EOMS-6469 - START
        //Get enterpriseCode instead of sellerOrganizationCode to derive common codes
		strOrganizationCode = order.getAttribute(A_ENTERPRISE_CODE);
		//EOMS-6468 & EOMS-6469 - END

		Element paymentMethods = SCXmlUtil.getChildElement(order, E_PAYMENT_METHODS);
		ArrayList<Element> paymentMethodList =	SCXmlUtil.getChildren(paymentMethods, E_PAYMENT_METHOD);
		if(!paymentMethodList.isEmpty()){

			//getting list of configured payment methods for order priority calculation
			Document paymentTypesForPriority = getPaymentMethodsForOrderPriority(env, strOrganizationCode, false, true);

			if(paymentTypesForPriority != null){

				logger.debug("We have these list of payment methods which are configured for order priority calculation"
						+SCXmlUtil.getString(paymentTypesForPriority));

				Element commonCodeListEle = paymentTypesForPriority.getDocumentElement();

				for(Element paymentMethod : paymentMethodList){
					String paymentType = "";
					paymentType = paymentMethod.getAttribute(A_PAYMENT_TYPE);

					//just check if this payment type value is present in common code or not to priritize
					if(!YFCCommon.isVoid(paymentType) && !YFCCommon.isVoid(SCXmlUtil.getXpathElement(commonCodeListEle,
							"/CommonCodeList/CommonCode[@CodeValue='"+paymentType+"']"))){

						isPriorityPaymentMethod = true;
						logger.debug("Order is placed with payment method present in common code list:: ");


					}else{

						logger.debug("This payment type is not configured in common code for order priority ");
						
					}

				}
			}
		}else{
			logger.debug("No Payment methods present on the order ");
			
		}


		if(logger.isDebugEnabled()){
			logger.debug("Ends of method calculateOrderPriorityByPaymentMethods() isPriorityPaymentMethod as : "
					+ isPriorityPaymentMethod);
		}

		return isPriorityPaymentMethod;

	}

	/**
	 *
	 * @param env
	 * @param organizationCode
	 * @param isCallForOrderType
	 * @param isCallForPaymentMethods
	 * @return
	 */
	private Document getPaymentMethodsForOrderPriority(YFSEnvironment env, String organizationCode,
													   boolean isCallForOrderType,  boolean isCallForPaymentMethods){

		if(logger.isDebugEnabled()){
			logger.debug("Start of method getPaymentMethodsForOrderPriority with isCallForOrderType: " + isCallForOrderType);
			logger.debug("Start of method getPaymentMethodsForOrderPriority with isCallForPaymentMethods: "
					+ isCallForPaymentMethods);
		}

		Document paymentMethodListsDoc = null;
		try {
			Document getCommonCodeIndoc = SCXmlUtil.createDocument(E_COMMON_CODE);
			Element getCommonCodeEle = getCommonCodeIndoc.getDocumentElement();
			getCommonCodeEle.setAttribute(A_ORGANIZATION_CODE, organizationCode);

			if(isCallForOrderType){
				getCommonCodeEle.setAttribute(A_CODE_TYPE, COMMON_CODE_ORDER_PRIORITY_ORDER_TYPE);

			}else if (isCallForPaymentMethods){
				getCommonCodeEle.setAttribute(A_CODE_TYPE, COMMON_CODE_ORDER_PRIORITY_PAYMENTS);
			}

			//calling getCommonCodeList API
			paymentMethodListsDoc = CommonUtil.invokeAPI(env, TEMPLATE_GET_COMMON_CODE_LIST,
					API_GET_COMMON_CODE_LIST, getCommonCodeIndoc) ;

		} catch (Exception e) {

			logger.debug("Error in getCommonCodeList API call in method getPaymentMethodsForOrderPriority: "
					+ e.getLocalizedMessage());
		}

		if(logger.isDebugEnabled()){
			logger.debug("Ends of method getPaymentMethodsForOrderPriority() with "
					+SCXmlUtil.getString(paymentMethodListsDoc));
		}
		return paymentMethodListsDoc;

	}

	/**
	 * This method update the indoc with priorityNo
	 * before sending this to WMS
	 * @param env
	 * @param order OnSuccess Release message
	 * @param priorityNo
	 */
	private void updateReleaseMsgWithPriorityNo(YFSEnvironment env, Element order, String priorityNo){

		if(logger.isDebugEnabled()){
			logger.debug("Start of method updateReleaseMsgWithPriorityNo() with order element: "+ SCXmlUtil.getString(order));
			logger.debug("The value of Priority is " +priorityNo);
		}

		order.setAttribute(A_PRIORITY_NUMBER, priorityNo);

		if(logger.isDebugEnabled()){
			logger.debug("End of method updateReleaseMsgWithPriorityNo() with updated order element: "
					+ SCXmlUtil.getString(order));
		}
	}

	/**
	 *
	 * @param env
	 * @param order
	 * @return
	 */
	private boolean calculateOrderPrioritybyOrderType(YFSEnvironment env, Element order){

		boolean isOrderTypeofPriority = false;

		if(logger.isDebugEnabled()){
			logger.debug("Starts of method calculateOrderPrioritybyOrderType() with order details: "
					+SCXmlUtil.getString(order));
		}
		String strOrderType = "";
		String strOrganizationCode = "";

		strOrderType = order.getAttribute(A_ORDER_TYPE);
		strOrganizationCode = order.getAttribute(A_SELLER_ORGANIZATION_CODE);

		//getting list of configured payment methods for order priority calculation
		Document orderTypesForPriority = getPaymentMethodsForOrderPriority(env, strOrganizationCode, true, false);

		if(orderTypesForPriority != null){

			logger.debug("We have these list of order types which are configured for order priority calculation"
					+SCXmlUtil.getString(orderTypesForPriority));

			Element commonCodeListEle = orderTypesForPriority.getDocumentElement();

			//just check if this payment type value is present in common code or not to priritize
			if(!YFCCommon.isVoid(strOrderType) && !YFCCommon.isVoid(SCXmlUtil.getXpathElement(commonCodeListEle,
					"/CommonCodeList/CommonCode[@CodeValue='"+strOrderType+"']"))){

				isOrderTypeofPriority = true;
				logger.debug("Order is placed with OrderType present in common code list for priority:: ");

			}else{
				logger.debug("This order type is not present in common code list for order priority ");

			}

		}
		logger.debug("Value of flag isOrderTypeofPriority: "+isOrderTypeofPriority);
		return isOrderTypeofPriority;

	}
}
