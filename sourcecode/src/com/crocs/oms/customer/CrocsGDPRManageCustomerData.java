package com.crocs.oms.customer;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class CrocsGDPRManageCustomerData implements CrocsConstant{


    private static YFCLogCategory logger = YFCLogCategory.instance(CrocsGDPRManageCustomerData.class);

    /**
     * This method does the below
     * 1. checks if there are any customer contacts associated with the email id by calling getCustomerList
     * 2. prepares input for GDPR_Delete_Data api call
     *
     * @param env
     * @param indoc
     * @return
     */

    public Document prepareGDPRDeleteServiceCall(YFSEnvironment env, Document indoc) throws Exception {
        String emailID = null;
        boolean dataFound = false;

        try {
            logger.debug("Input to prepareGDPRDeleteServiceCall: " + SCXmlUtil.getString(indoc));
            Element rootEle = indoc.getDocumentElement();
            emailID = rootEle.getAttribute(A_EMAILID);

            logger.debug("rootEle for GDPR call is: " + SCXmlUtil.getString(rootEle));
            logger.debug("emailID for GDPR call is: " + emailID);

            Document summaryDoc = SCXmlUtil.createDocument(E_GDPR_DELETE_RESULTS);
            Element summaryRoot = summaryDoc.getDocumentElement();

            // calling getCustomerList
            Document getCustomerListInput = SCXmlUtil.createDocument(E_CUSTOMER);
            Element customerEle = getCustomerListInput.getDocumentElement();
            Element custContactList = SCXmlUtil.createChild(customerEle, E_CUSTOMER_CONTACT_LIST);
            Element customerContact = SCXmlUtil.createChild(custContactList, E_CUSTOMER_CONTACT);
            customerContact.setAttribute(A_EMAILID, emailID);

            logger.debug("getCustomerListInput for GDPR call is:" + SCXmlUtil.getString(getCustomerListInput));

            Document customerListOutput = CommonUtil.invokeAPI(env, TEMPLATE_GET_CUSTOMER_LIST_GDPR,
                    GET_CUSTOMER_LIST_API, getCustomerListInput);
            logger.debug("getCustomerListOutputDoc for GDPR call is:" + SCXmlUtil.getString(customerListOutput));
            NodeList customerNodes = customerListOutput.getDocumentElement().getElementsByTagName(E_CUSTOMER);

            if (customerNodes != null && customerNodes.getLength() > 0) {
                dataFound = true;
                for (int i = 0; i < customerNodes.getLength(); i++) {
                    Element customerElement = (Element) customerNodes.item(i);
                    String customerId = customerElement.getAttribute(A_CUSTOMER_ID);
                    Document deleteInputDoc = prepareDeleteInputForCustomer(customerElement);

                    try {
                        Document deleteResponse = CommonUtil.invokeService(env, SERVICE_GDPR_DELETE_DATA, deleteInputDoc);
                        logger.debug("respElement for GDPR call of customer is: " + SCXmlUtil.getString(deleteResponse));
                        String statusCode = deleteResponse.getDocumentElement().getAttribute(A_IS_DELETION_SUCCESS);

                        Element result = SCXmlUtil.createChild(summaryRoot, E_RESULT);
                        result.setAttribute(A_KEY, customerId);

                        if (FLAG_N.equals(statusCode)) {
                            result.setAttribute(A_STATUS, MESSAGE_SKIP_BUSINESS_DATA);
                            logger.warn("Customer delete skipped (business data exists): " + customerId);
                        } else {
                            result.setAttribute(A_STATUS, VAL_SUCCESS);
                            logger.info("Customer deleted:: " + customerId);
                        }

                    } catch (Exception e) {
                        logger.error("Failed to delete customer with ID: " + customerId, e);
                        Element result = SCXmlUtil.createChild(summaryRoot, E_RESULT);
                        result.setAttribute(A_KEY, customerId);
                        result.setAttribute(A_STATUS, MESSAGE_FAILURE);
                    }
                }
            }

            // Call getPersonInfoList Processing
            Document getPersonInfoListInput = SCXmlUtil.createDocument(E_PERSON_INFO);
            Element personInfoEle = getPersonInfoListInput.getDocumentElement();
            personInfoEle.setAttribute(A_EMAIL_ID, emailID);

            Document personInfoListOutput = CommonUtil.invokeAPI(env, TEMPLATE_GET_PERSON_INFO_LIST_GDPR,
                    API_GET_PERSON_INFO_LIST, getPersonInfoListInput);

            logger.debug("personInfoListOutput for GDPR call is:" + SCXmlUtil.getString(personInfoListOutput));

            NodeList personInfoNodes = personInfoListOutput.getDocumentElement().getElementsByTagName(E_PERSON_INFO);

            if (personInfoNodes != null && personInfoNodes.getLength() > 0) {
                dataFound = true;
                for (int i = 0; i < personInfoNodes.getLength(); i++) {
                    Element personInfoElement = (Element) personInfoNodes.item(i);
                    String personInfoKey = personInfoElement.getAttribute(A_PERSON_INFO_KEY);
                    Document deleteInputDoc = prepareDeleteInputWithPersonInfo(personInfoElement);

                    try {
                        Document deleteResponse = CommonUtil.invokeService(env, SERVICE_GDPR_DELETE_DATA, deleteInputDoc);
                        logger.debug("deleteResponse for GDPR call for personInfo is: " + SCXmlUtil.getString(deleteResponse));
                        String statusCode = deleteResponse.getDocumentElement().getAttribute(A_IS_DELETION_SUCCESS);

                        Element result = SCXmlUtil.createChild(summaryRoot, E_RESULT);
                        result.setAttribute(A_KEY, personInfoKey);

                        if (FLAG_N.equalsIgnoreCase(statusCode)) {
                            result.setAttribute(A_STATUS, MESSAGE_SKIP_BUSINESS_DATA);
                            logger.warn("PersonInfo delete skipped (business data exists): " + personInfoKey);
                        } else {
                            result.setAttribute(A_STATUS, VAL_SUCCESS);
                            logger.info("PersonInfo deleted: " + personInfoKey);
                        }

                    } catch (Exception e) {
                        logger.error("Failed to delete PersonInfo with key: " + personInfoKey, e);
                        Element result = SCXmlUtil.createChild(summaryRoot, E_RESULT);
                        result.setAttribute(A_KEY, personInfoKey);
                        result.setAttribute(A_STATUS, MESSAGE_FAILURE);
                    }
                }
            }

            // Final Outcome
            if (!dataFound) {
                logger.error("GDPR_DELETE_ERROR: No customer or person info data found for email: " + emailID);
                Document errorDoc = SCXmlUtil.createDocument(A_ERROR);
                errorDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_STATUS_CODE, STATUS_CODE_INVALID_EMAIL_ID);
                errorDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_STATUS_MESSAGE, A_STATUS_MESSAGE + emailID);
                return errorDoc;
            }

            return summaryDoc;

        } catch (YFSException yex) {
            logger.error("User exception in prepareGDPRDeleteServiceCall: ", yex);
            throw yex;
        } catch (Exception ex) {
            logger.error("Unexpected exception in prepareGDPRDeleteServiceCall: ", ex);
            Document errorDoc = SCXmlUtil.createDocument(A_ERROR);
            errorDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_STATUS_CODE, STATUS_CODE_GDPR_INTERNAL_ERROR);
            errorDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_STATUS_MESSAGE, A_STATUS_MESSAGE_GDPR + emailID);
            return errorDoc;
        }
    }

    private Document prepareDeleteInputWithPersonInfo(Element personInfoElement) {

        String personInfoKey = personInfoElement.getAttribute(A_PERSON_INFO_KEY);
        Document prepareDeleteDocForGDPR = SCXmlUtil.createDocument(E_GDPR_DATA);
        Element gdprEle = prepareDeleteDocForGDPR.getDocumentElement();
        gdprEle.setAttribute(A_PERSON_INFO_KEY,personInfoKey);

        logger.debug("prepareDeleteDocForGDPR with PersonInfo data is:" + SCXmlUtil.getString(prepareDeleteDocForGDPR));

        return prepareDeleteDocForGDPR;

    }

    private Document prepareDeleteInputForCustomer(Element customerElement) {
        logger.debug("customerElement is:: " + SCXmlUtil.getString(customerElement));

        String customerId = customerElement.getAttribute(A_CUSTOMER_ID);
        String orgCode = customerElement.getAttribute(A_ORGANIZATION_CODE);
        logger.debug("customerId is: "+customerId);

        Document prepareDeleteDocForGDPR = SCXmlUtil.createDocument(E_GDPR_DATA);
        Element gdprEle = prepareDeleteDocForGDPR.getDocumentElement();
        gdprEle.setAttribute(A_CUSTOMER_ID,customerId);
        gdprEle.setAttribute(A_ORGANIZATION_CODE,orgCode);

        logger.debug("prepareDeleteDocForGDPR with Customer data is: " + SCXmlUtil.getString(prepareDeleteDocForGDPR));
        return prepareDeleteDocForGDPR;
    }

}