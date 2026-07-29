package com.crocs.oms.to.condition;

import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsAPIConstants;
import com.crocs.oms.common.util.CrocsTemplateConstants;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.ycp.japi.YCPDynamicConditionEx;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;

/**
 * This class checks whether Organization exist in OMS.
 */

public class CrocsValidateOrganizationExist implements YCPDynamicConditionEx {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsValidateOrganizationExist.class);

	@Override
	public boolean evaluateCondition(YFSEnvironment env, String arg1, Map arg2, Document inDoc) {

		try {

			logger.verbose("Input to CrocsValidateOrganizationExist: " + XMLUtil.getXMLString(inDoc));

			Element eleInOrganization = inDoc.getDocumentElement();
			String strOrgCode = eleInOrganization.getAttribute(CrocsXmlConstants.A_ORGANIZATION_CODE);

			logger.verbose("OrganizationCode: " + strOrgCode);

			Document docOrgListOutput = invokeGetOrganizationListAPI(env, strOrgCode);
			if (!YFCCommon.isVoid(docOrgListOutput) && docOrgListOutput.getDocumentElement().hasChildNodes()) {
				return true;
			}

		} catch (Exception e) {
			logger.error("Exception in CrocsValidateOrganizationExist", e);
		}

		return false;
	}

	/**
	 * This method invokes getOrganization List API
	 * 
	 * @param env
	 * @param strOrgCode
	 * @return
	 * @throws Exception
	 */

	private Document invokeGetOrganizationListAPI(YFSEnvironment env, String strOrgCode) throws Exception {

		Document docOrgListInput = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORGANIZATION);

		Element eleOrganization = docOrgListInput.getDocumentElement();
		eleOrganization.setAttribute(CrocsXmlConstants.A_ORGANIZATION_CODE, strOrgCode);

		logger.verbose("Calling getOrganizationList API: " + XMLUtil.getXMLString(docOrgListInput));

		Document docOrgListOutput = CommonUtil.invokeAPI(env, CrocsTemplateConstants.TEMPLATE_GET_ORGANIZATION_LIST,
				CrocsAPIConstants.API_GET_ORGANIZATION_LIST, docOrgListInput);

		logger.verbose("getOrganizationList Output: " + XMLUtil.getXMLString(docOrgListOutput));

		return docOrgListOutput;
	}

	@Override
	public void setProperties(Map arg0) {

	}
}