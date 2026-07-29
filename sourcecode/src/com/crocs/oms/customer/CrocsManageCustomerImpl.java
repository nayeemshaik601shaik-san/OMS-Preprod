package com.crocs.oms.customer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsAPIConstants;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;

/**
 * EOMS-574 & EOMS-575 : 
 * This class is implemented to create the customer with the details present in the order xml and 
 * also pass the CustomerId as the BillToId in the create order message so that 
 * the user will be able to view the details of all the orders placed by the customer in call center screen.
 */
public class CrocsManageCustomerImpl {
	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsManageCustomerImpl.class);
	private static final String GET_CUSTOMER_LIST_INPUT_XML = "<Customer OrganizationCode='%s' >"
				+ "<CustomerContactList>"
					+ "<CustomerContact EmailID='%s' />"
				+ "</CustomerContactList>"
			+ "</Customer>" ;
	private static final String GET_CUSTOMER_LIST_OUTPUT_TEMPLATE_XML = "<CustomerList>"
			+ "<Customer CustomerID='' CustomerKey='' CustomerType='' OrganizationCode='' Status=''>"
				+ "<CustomerContactList>"
					+ "<CustomerContact CustomerContactID='' EmailID=''>"
						+ "<CustomerAdditionalAddressList>"
							+ "<CustomerAdditionalAddress >"
								+ "<PersonInfo />"
							+ "</CustomerAdditionalAddress>"
							+ "<CustomerAdditionalAddress >"
								+ "<PersonInfo />"
							+ "</CustomerAdditionalAddress>"
						+ "</CustomerAdditionalAddressList>"
					+ "</CustomerContact>"
				+ "</CustomerContactList>"
			+ "</Customer>"
		+ "</CustomerList>" ;
	
	/**
	 * This method does the below:
	 * 1. Fetches the CustomerId based on the CustomerEMailID at Order level.
	 * 2. If CustomerId is not present in OMS then create the same in OMS.
	 * 3. Stamp the CustomerId as BillToID at the Order level in the createOrder message.
	 * 
	 * @param env
	 * @param inDoc
	 * @return
	 */
	public Document updateCustomerDetailsOnOrder(YFSEnvironment env, Document inDoc) {
		logger.beginTimer("CrocsManageCustomerImpl::updateCustomerDetailsOnOrder: START:" + SCXmlUtil.getString(inDoc));
		logger.verbose("CrocsManageCustomerImpl : updateCustomerDetailsOnOrder : Start : " + SCXmlUtil.getString(inDoc));
		Element eleOrder = inDoc.getDocumentElement();
		String strOrgCode = eleOrder.getAttribute(CrocsXmlConstants.A_ENTERPRISE_CODE);
		String strEmailId = eleOrder.getAttribute(CrocsXmlConstants.A_CUSTOMER_EMAIL_ID);
		if(strOrgCode != null && !strOrgCode.isEmpty() && strEmailId != null && !strEmailId.isEmpty()) {
			Document docGetCustListOutput = getCustomerList(env,strOrgCode,strEmailId);
			if(docGetCustListOutput != null) {
				String strBillToID = createCustomer(env,inDoc,docGetCustListOutput);
				eleOrder.setAttribute(CrocsXmlConstants.A_BILL_TO_ID, strBillToID);
			}
		}
		logger.verbose("CrocsManageCustomerImpl : updateCustomerDetailsOnOrder : End : " + SCXmlUtil.getString(inDoc));
		logger.endTimer("CrocsManageCustomerImpl::updateCustomerDetailsOnOrder: END:"+SCXmlUtil.getString(inDoc));
		return inDoc;
	}

	/**
	 * This method calls the CrocsManageCustomerSyncService to createCustomer.
	 * 
	 * @param env
	 * @param inDoc
	 * @param docGetCustListOutput
	 * @return
	 */
	private String createCustomer(YFSEnvironment env, Document inDoc, Document docGetCustListOutput) {
		logger.verbose("CrocsManageCustomerImpl : createCustomer : Start");
		String strCustomerId = null ;
		if(!YFCCommon.isVoid(docGetCustListOutput)) {
			Element eleCustomerList = docGetCustListOutput.getDocumentElement();
			strCustomerId = SCXmlUtil.getXpathAttribute(eleCustomerList, CrocsConstant.XPATH_CUSTOMER_ID);
			logger.verbose("strCustomerId :"+strCustomerId);
			if(YFCCommon.isVoid(strCustomerId)) {
				//new customer details.
				Document docManageCustOutput = null;
				try {
					docManageCustOutput = CommonUtil.invokeService(env, CrocsConstant.CROCS_MANAGE_CUST_SYNC_SERV, inDoc);
				} catch (Exception e) {
					logger.verbose("CrocsManageCustomerImpl : createCustomer : Catch block" + e.getMessage());
				}
				logger.verbose("CrocsManageCustomerImpl : createCustomer : docManageCustOutput : " + SCXmlUtil.getString(docManageCustOutput));
				if(docManageCustOutput != null) {
					strCustomerId = docManageCustOutput.getDocumentElement().getAttribute(CrocsXmlConstants.A_CUSTOMER_ID);
				}
			}
			logger.verbose("CrocsManageCustomerImpl : createCustomer : End : CustomerID:"+strCustomerId);
		}
		return strCustomerId;		
	}

	/**
	 * This method fetches the customer details based on CustomerEMailID at the order level.
	 * 
	 * @param env
	 * @param strOrgCode
	 * @param strEmailId
	 * @return
	 */
	private Document getCustomerList(YFSEnvironment env, String strOrgCode, String strEmailId) {
		logger.verbose("CrocsManageCustomerImpl : getCustomerList : Start : " + strOrgCode +"  "+strEmailId);
		String strGetCustListInput = String.format(GET_CUSTOMER_LIST_INPUT_XML, strOrgCode, strEmailId.toUpperCase());
		Document docGetCustListInputXML = YFCDocument.getDocumentFor(strGetCustListInput).getDocument();
		Document docGetCustListOutputTemplate = YFCDocument.getDocumentFor(GET_CUSTOMER_LIST_OUTPUT_TEMPLATE_XML).getDocument();
		Document docGetCustListOutput = null ;
		try {
			logger.verbose("CrocsManageCustomerImpl : getCustomerList : docGetCustListOutputTemplate : " + SCXmlUtil.getString(docGetCustListOutputTemplate));
			logger.verbose("CrocsManageCustomerImpl : getCustomerList : docGetCustListInputXML : " + SCXmlUtil.getString(docGetCustListInputXML));
			docGetCustListOutput = CommonUtil.invokeAPI(env, docGetCustListOutputTemplate, CrocsAPIConstants.GET_CUSTOMER_LIST_API, docGetCustListInputXML);
		} catch (Exception e) {
			logger.verbose("CrocsManageCustomerImpl : getCustomerList : Catch block" + e.getMessage());
		}
		logger.verbose("CrocsManageCustomerImpl : getCustomerList : docGetCustListOutput : " + SCXmlUtil.getString(docGetCustListOutput));
		return docGetCustListOutput;
	}
}