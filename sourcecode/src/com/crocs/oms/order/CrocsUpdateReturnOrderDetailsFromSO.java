package com.crocs.oms.order;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsAPIConstants;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsTemplateConstants;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

/**
 * Description: Coping Call Center Return order details from SO
 * 
 */
public class CrocsUpdateReturnOrderDetailsFromSO {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsUpdateReturnOrderDetailsFromSO.class);

	/**
	 * EOMS-736 : Updating ExtnCustomerLocale from SO to RO
	 * 
	 * @param inDoc
	 * @return 
	 */
	public static Document updateLocaleFromSOToRO(YFSEnvironment env, Document inDoc) {
		logger.verbose("CrocsUpdateReturnOrderDetailsFromSO : updateLocaleFromSOToRO: START");
		try {
			String extnCustomerLocale = "";
			Element eleOrderExtn = null;

			Element eleOrder = inDoc.getDocumentElement();

			String strOrderLineKey = SCXmlUtil.getXpathAttribute(inDoc.getDocumentElement(),
					CrocsConstant.XPATH_DERIVED_FROM_ORDER_ORDER_LINE_KEY);

			Document getOrderListInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER_LINE);
			getOrderListInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_ORDER_LINE_KEY, strOrderLineKey);
			logger.verbose("getOrderLineListInput:" + XMLUtil.getXMLString(getOrderListInDoc));
			
			Document getOrderLineListout = CommonUtil.invokeAPI(env,
					CrocsTemplateConstants.TEMPLATE_GET_ORDER_LINE_LIST_FOR_EXTN_CUSTOMER_LOCALE,
					CrocsAPIConstants.API_GET_ORDER_LINE_LIST, getOrderListInDoc);
			
		
			logger.verbose("getOrderLineListout:" + XMLUtil.getXMLString(getOrderLineListout));

			if(getOrderLineListout!=null && getOrderLineListout.getDocumentElement().hasChildNodes()) {
				
				Element orderLineList = getOrderLineListout.getDocumentElement();
				Element eleorderLineDetails = SCXmlUtil.getChildElement(orderLineList, CrocsXmlConstants.E_ORDER_LINE);
				Element orderDetails = SCXmlUtil.getChildElement(eleorderLineDetails, CrocsXmlConstants.E_ORDER);
				
				if (SCXmlUtil.getChildElement(orderDetails, CrocsXmlConstants.E_EXTN) != null) {
					Element eleOrderDetailsExtn = SCXmlUtil.getChildElement(orderDetails, CrocsXmlConstants.E_EXTN);
					extnCustomerLocale = eleOrderDetailsExtn.getAttribute(CrocsConstant.EXTN_CUSTOMER_LOCALE);
					if(extnCustomerLocale!=null) {
						if (SCXmlUtil.getChildElement(eleOrder, CrocsXmlConstants.E_EXTN) != null) {
							eleOrderExtn = SCXmlUtil.getChildElement(eleOrder, CrocsXmlConstants.E_EXTN);
						} else {
							eleOrderExtn = SCXmlUtil.createChild(eleOrder, CrocsXmlConstants.E_EXTN);
						}
						eleOrderExtn.setAttribute(CrocsConstant.EXTN_CUSTOMER_LOCALE, extnCustomerLocale);
					}
				}
			}

		} catch (Exception e) {
			throw new YFSException(
					"CrocsUpdateReturnOrderDetailsFromSO.updateLocaleFromSOToRO :Expection" + e.getMessage());
		}
		logger.verbose("CrocsUpdateReturnOrderDetailsFromSO : updateLocaleFromSOToRO: END: "+ XMLUtil.getXMLString(inDoc));
		return inDoc;
	}

}
