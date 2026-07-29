package com.crocs.oms.to.order;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.comergent.api.xml.XMLUtils;
import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsAPIConstants;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.CrocsTemplateConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class CrocsAddSCACToWMReleaseMessage {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsAddSCACToWMReleaseMessage.class);

	/**
	 * https://crocsinc.atlassian.net/browse/EOMS-8725
	 * 
	 * Enriches the incoming OrderRelease doc with SCAC attribute for WM. Always
	 * sets SCAC as empty string if carrier is not defined for an org or scac is not mapped for the carrier.
	 */

	public Document updateSCAC(YFSEnvironment env, Document indoc) throws Exception {

		try {
			logger.verbose("CrocsAddSCACToWMReleaseMessage : updateSCAC : Start : " + SCXmlUtil.getString(indoc));
			String strSCAC = "";
			Element eleOrderRelease = indoc.getDocumentElement();
			String strStore = eleOrderRelease.getAttribute(CrocsXmlConstants.A_RECEIVING_NODE);
			if (!YFCCommon.isVoid(strStore)) {
				// Fetch Organization for the store
				YFCDocument docOrg = YFCDocument.createDocument(CrocsConstant.E_ORGANIZATION);
				docOrg.getDocumentElement().setAttribute(CrocsXmlConstants.A_ORGANIZATION_CODE, strStore);

				logger.verbose("Calling getOrganizationList with input: " + docOrg.toString());

				Document docOrgOut = CommonUtil.invokeAPI(env, CrocsTemplateConstants.TEMPLATE_GET_ORGANIZATION_LIST_TO,
						CrocsAPIConstants.API_GET_ORGANIZATION_LIST, docOrg.getDocument());

				if (!YFCObject.isNull(docOrgOut) && docOrgOut.getDocumentElement().hasChildNodes()) {
					Element eleExtn = XMLUtils.getElementByName(docOrgOut.getDocumentElement(), CrocsConstant.E_EXTN);
					if (eleExtn != null && eleExtn.hasAttribute(CrocsConstant.STR_EXTN_CARRIER)) {
						String strExtnCarrier = eleExtn.getAttribute(CrocsConstant.STR_EXTN_CARRIER);
						strSCAC = getSCACFromCommonCode(env, strExtnCarrier);
						eleOrderRelease.setAttribute(CrocsConstant.STR_SCAC, strSCAC);
					} else {
						logger.error("Error in getOrganizationList API : NO ExtnCarrier FOUND !! for -" + strStore);
						// In this case, will send SCAC with Blank Value -

						eleOrderRelease.setAttribute(CrocsConstant.STR_SCAC, strSCAC);
					}
				} else {
					throw new YFSException("No Organization Details found");
				}
			}
			else {
				throw new YFSException("Receiving Store Not Found at OrderRelease/@ReceivingNode level");
			}
		} catch (Exception e) {
			logger.error("Error in updateSCAC method of class CrocsAddSCACToWMReleaseMessage " + e.getMessage());
			throw new YFSException(e.getMessage());
		}

		return indoc;

	}

	private String getSCACFromCommonCode(YFSEnvironment env, String strExtnCarrier) {
		// The requirement is to send "" to WM , in case carrier isn't found.
		String strCodeValue = "";
		if (!YFCCommon.isVoid(strExtnCarrier)) {
			try {
				Document docInGetComCodeList = SCXmlUtil.createDocument(CrocsXmlConstants.E_COMMON_CODE);
				docInGetComCodeList.getDocumentElement().setAttribute(CrocsXmlConstants.A_CODE_TYPE,
						CrocsConstant.STR_CODE_TYPE);
				docInGetComCodeList.getDocumentElement().setAttribute(CrocsXmlConstants.A_CODE_SHORT_DESCRIPTION,
						strExtnCarrier);
				docInGetComCodeList.getDocumentElement().setAttribute(CrocsXmlConstants.A_ORGANIZATION_CODE,
						CrocsConstant.CROCS_US);

				logger.verbose("CrocsAddSCACToWMReleaseMessage : getSCACFromCommonCode : docInGetComCodeList:"
						+ SCXmlUtil.getString(docInGetComCodeList));

				Document docGetCommonCodeListOut = CommonUtil.invokeAPI(env,
						CrocsTemplateConstants.TEMPLATE_MODIFY_GET_COMMON_CODE_LIST_FOR_TO,
						CrocsAPIConstants.API_GET_COMMON_CODE_LIST, docInGetComCodeList);

				logger.verbose("CrocsAddSCACToWMReleaseMessage : getSCACFromCommonCode : docGetCommonCodeListOut:"
						+ SCXmlUtil.getString(docGetCommonCodeListOut));

				if (!YFCObject.isNull(docGetCommonCodeListOut)) {
					strCodeValue = SCXmlUtil.getXpathAttribute(docGetCommonCodeListOut.getDocumentElement(),
							CrocsConstant.XPATH_CODE_VALUE);
				} else {
					logger.error("Error in getCommonCodeList API : NO SCAC FOUND !! for -" + strExtnCarrier);
				}
				logger.verbose("CrocsAddSCACToWMReleaseMessage : getSCACFromCommonCode : CodeValue is:" + strCodeValue);
			} catch (Exception e) {
				logger.error("Error in getCommonCodeList API call in method getSCACFromCommonCode: " + e.getMessage());
				throw new YFSException(e.getMessage());
			}
		}
		return strCodeValue;
	}
}
