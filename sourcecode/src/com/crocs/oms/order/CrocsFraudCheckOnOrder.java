package com.crocs.oms.order;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.util.ue.CrocsCheckFraudOnOrderUserExitImpl;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;

/*
 * EOMS-687 Fraud check during shipping address modification
 */

public class CrocsFraudCheckOnOrder implements CrocsConstant {
	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsFraudCheckOnOrder.class);

	CrocsCheckFraudOnOrderUserExitImpl crocsCheckFraudOnOrderUserExitImpl=new CrocsCheckFraudOnOrderUserExitImpl();
	
	/*
	 * 
	 * This method take input from Call Center UI .
	 * will prepare forter api request and get response . 
	 * if FraudCheckResponseCode is success address will be saved on order
	 * else error will be thrown in Call Center UI
	 */
	public Document fraudCheckOnShippingAddress(YFSEnvironment env, Document inDoc) throws Exception {
		
		Element orderEle=inDoc.getDocumentElement();
		 
		Element elePersonInfoShipTo=SCXmlUtil.getXpathElement(inDoc.getDocumentElement(), XPATH_PERSONINFOSHIPTO_ELEMENT);
		

		String strOrderHeaderKey=orderEle.getAttribute(A_ORDER_HEADER_KEY);
		Document getOrderListInDoc=SCXmlUtil.createDocument(E_ORDER);
		getOrderListInDoc.getDocumentElement().setAttribute(A_ORDER_HEADER_KEY, strOrderHeaderKey);
		
		Document getOrderListOutDoc=CommonUtil.invokeService(env, CrocsConstant.CROCS_GET_ORDER_LIST_FOR_FORTER,getOrderListInDoc);
		logger.verbose("getOrderList Output"+SCXmlUtil.getString(getOrderListOutDoc));
		Element eleOrder=SCXmlUtil.getChildElement(getOrderListOutDoc.getDocumentElement(), E_ORDER);
		String strOrder=SCXmlUtil.getString(eleOrder);
		Document inputDoc=SCXmlUtil.createFromString(strOrder);
		logger.verbose("inputDoc"+SCXmlUtil.getString(inputDoc));
		Element eleInputPersonInfoShipTo=SCXmlUtil.getXpathElement(inputDoc.getDocumentElement(), XPATH_ORDER_PERSONINFOSHIPTO_ELEMENT);

		inputDoc.getDocumentElement().removeChild(eleInputPersonInfoShipTo);
		Element elePersonInfoImport=(Element) inputDoc.importNode(elePersonInfoShipTo, A_TRUE);
		
		inputDoc.getDocumentElement().appendChild(elePersonInfoImport);
		System.out.println("inputDoc"+SCXmlUtil.getString(inputDoc));
		
		Document outDoc=crocsCheckFraudOnOrderUserExitImpl.processOrderFraudCheck(env, inputDoc);
		
		String responseCode=outDoc.getDocumentElement().getAttribute(E_FRAUD_CHECK_RESPONSECODE);
		String isValidOrder=outDoc.getDocumentElement().getAttribute(A_IS_VALID_ORDER);
		String isExpOrder=outDoc.getDocumentElement().getAttribute(A_IS_EXCEPTION_ORDER);
		if(FLAG_N.equals(isValidOrder))
		{
			String response=outDoc.getDocumentElement().getAttribute(A_RESPONSE);
			inDoc.getDocumentElement().setAttribute(A_IS_VALID_ORDER, FLAG_N);
			inDoc.getDocumentElement().setAttribute(A_RESPONSE, response);
		}
		else if(FLAG_Y.equals(isExpOrder))
		{
			inDoc.getDocumentElement().setAttribute(A_IS_EXCEPTION_ORDER, FLAG_Y);
		}
		else if(V_SUCCESS.equalsIgnoreCase(responseCode))
		{
			Document modifyFulfillmentOptionsOutDoc=CommonUtil.invokeAPI(env, TEMPLATE_MODIFY_FULFILLMENT_OPTIONS, API_MODIFY_FULFILLMENT_OPTIONS, inDoc);
			modifyFulfillmentOptionsOutDoc.getDocumentElement().setAttribute(A_IS_FRAUD_ORDER, FLAG_N);
			
			return modifyFulfillmentOptionsOutDoc;
		}
		else
			inDoc.getDocumentElement().setAttribute(A_IS_FRAUD_ORDER, VAL_FLAG_Y);	
		return inDoc;
		
	}
	}



