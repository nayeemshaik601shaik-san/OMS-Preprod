package com.crocs.oms.condition;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.ycp.japi.YCPDynamicConditionEx;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;

/**
 * Description: This below method help to check whether post Auth payment methods having ExtnFraudStatus as RETRY and FRAUD_HOLD
 * 
 */
public class CrocsCheckFraudHoldOnRetry implements YCPDynamicConditionEx {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsCheckFraudHoldOnRetry.class);
	
	@Override
	public boolean evaluateCondition(YFSEnvironment env, String arg1, Map arg2, Document indoc) {

		try {
						
			logger.verbose("Input to the evaluateCondition is:" + XMLUtil.getXMLString(indoc));
			
			Element orderEle = indoc.getDocumentElement();
			Element eleOrderExtn = SCXmlUtil.getChildElement(orderEle, CrocsXmlConstants.E_EXTN);
			String strExtnFraudStatus = eleOrderExtn.getAttribute(CrocsXmlConstants.A_EXTN_FRAUD_STATUS);
			
			if(CrocsConstant.A_RETRY.equalsIgnoreCase(strExtnFraudStatus) && SCXmlUtil.getChildElement(orderEle, CrocsXmlConstants.E_ORDER_HOLD_TYPES)!=null) {
				
				Element eleHoldType = XMLUtil.getElementByXPath(indoc,
						"/Order/OrderHoldTypes/OrderHoldType[@HoldType='FRAUD_HOLD' and @Status='1100']");
				
				//EOMS-4843: Forter Retry changes : START
				String strEnterpriseCode = orderEle.getAttribute(CrocsXmlConstants.A_ENTERPRISE_CODE);
				Document docGetCommonCodeListOutput = CommonUtil.getCommonCodeList(env, strEnterpriseCode, CrocsConstant.STR_CROCS_AUTH_EXP_PAYMENTS, null);
				
				Element paymentMethodsEle = SCXmlUtil.getChildElement(orderEle, CrocsXmlConstants.E_PAYMENT_METHODS);
				ArrayList<Element> paymentMethods = SCXmlUtil.getChildren(paymentMethodsEle, CrocsXmlConstants.E_PAYMENT_METHOD);
				for (Element paymentMethod : paymentMethods) {
					String paymentType = paymentMethod.getAttribute(CrocsXmlConstants.A_PAYMENT_TYPE);
					Element eleCommonCodeList = XMLUtil.getElementByXPath(docGetCommonCodeListOutput,"/CommonCodeList/CommonCode[@CodeValue='"+paymentType+"' and @CodeLongDescription='POST_AUTH']");
					if(eleHoldType!=null && eleCommonCodeList!=null) {
						return true;
					}
				}
				//EOMS-4843: Forter Retry changes : END
			}
		}catch(Exception e) {
			logger.verbose("CrocsCheckFraudHoldOnRetry.evaluateCondition :Expection" + e.getMessage());
		}
		return false;
	}

	@Override
	public void setProperties(Map arg0) {
		// TODO Auto-generated method stub

	}

}
