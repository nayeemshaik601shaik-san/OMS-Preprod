package com.crocs.oms.ivsync;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsIVAPIConstants;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.util.restapi.CrocsRestConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;

/**
 * EOMS-926 : store feed sync to IV
 * 
 * This class creates the input to IV define node api.
 * 
 */
public class CrocsCreateStoreFeedDetailsToIV{
	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsCreateStoreFeedDetailsToIV.class);
	
	public Document formCreateNodeIVInput(YFSEnvironment env, Document inDoc) throws Exception {
		
		logger.verbose("CrocsCreateStoreFeedDetailsToIV : formCreateNodeIVInput : Start : " + SCXmlUtil.getString(inDoc));
		Element eleOrgnaizationCode = inDoc.getDocumentElement();
		Element eleNode = SCXmlUtil.getChildElement(eleOrgnaizationCode, CrocsConstant.E_NODE);
	    Element eleContactInformation = SCXmlUtil.getChildElement(eleOrgnaizationCode, CrocsConstant.E_CORPORATE_PERSON_INFO);
	    String strCountry = eleContactInformation.getAttribute(CrocsConstant.A_COUNTRY); 
	    String timezone =null;
	    
	    //Convert upper case node type as STORE or DC dynamically
	    String strNodetype=eleNode.getAttribute(CrocsXmlConstants.A_NODE_TYPE); 
	    eleNode.setAttribute(CrocsXmlConstants.A_NODE_TYPE, strNodetype.toUpperCase()); 
	    
	    // To fetch the timezone based on country with  
	    /*if(strCountry.equals(CrocsXmlConstants.A_CA))
	    {
	    	  timezone =CrocsXmlConstants.A_TIMEZONE_CA;
	    }
	    else if(strCountry.equals(CrocsXmlConstants.A_US) || strCountry.equals(CrocsXmlConstants.A_PR))
	    {
	    	timezone =CrocsXmlConstants.A_TIMEZONE_US;
	    }
	    //START-EOMS-11008
	    else  if(strCountry.equals(CrocsXmlConstants.A_KR))
	    {
	    	  timezone =CrocsXmlConstants.A_TIMEZONE_KR;
	    }else  if(strCountry.equals(CrocsXmlConstants.A_SG))
	    {
	    	  timezone =CrocsXmlConstants.A_TIMEZONE_SG;
	    }*/
	    //END-EOMS-11008
	    
	    //START-EOMS-11895
	    String localeCode = eleOrgnaizationCode.getAttribute(CrocsXmlConstants.A_LOCALE_CODE);
	    if (!YFCCommon.isVoid(localeCode)) {
	    	if (CrocsXmlConstants.VAL_LOCALE_MST.equals(localeCode)) {
	    		timezone = CrocsXmlConstants.A_TIMEZONE_US;
	    	} else {
	    		timezone = setTimezone(env,localeCode);
	    	}
	    }
	    if (YFCCommon.isVoid(timezone)) {
	    	timezone = CrocsXmlConstants.A_TIMEZONE_US;
	    }
		//END-EOMS-11895
	    
	    String nodeId = eleOrgnaizationCode.getAttribute("OrganizationCode");
	    String latitude = eleContactInformation.getAttribute(CrocsXmlConstants.A_LATITUDE);
	    String longitude = eleContactInformation.getAttribute(CrocsXmlConstants.A_LONGITUDE);
        
        //IV JSON input content as a string
	    String jsonString = "{" + "\"node\": \""+eleOrgnaizationCode.getAttribute(CrocsXmlConstants.A_ORGANIZATION_CODE)
		                 + "\",\"nodeName\": \""+ eleOrgnaizationCode.getAttribute(CrocsXmlConstants.A_ORGANIZATION_NAME) 
		                 + "\",\"fulfillmentNodeType\": \"" + eleNode.getAttribute(CrocsXmlConstants.A_NODE_TYPE) 
		                 + "\",\"rateNodeType\":\"" + eleNode.getAttribute(CrocsXmlConstants.A_NODE_TYPE) 
	                     + "\",\"timeZone\":  \""+timezone+"\","
	                     + (!YFCCommon.isVoid(latitude) && !YFCCommon.isVoid(longitude)
		         	                ? "\"geoCode\": {\"latitude\":" + latitude + ",\"longitude\":" + longitude + "},"
		         	                : "")
		                 + "\"address\": {"
		                 + "\"addressLine1\":\""+ eleContactInformation.getAttribute(CrocsXmlConstants.A_ADDRESSLINE)  
		                 + "\",\"city\": \""+ eleContactInformation.getAttribute(CrocsXmlConstants.A_CITY)  
		                 + "\",\"state\": \"" + eleContactInformation.getAttribute(CrocsXmlConstants.A_STATE)  
		                 + "\",\"postalCode\": \"" + eleContactInformation.getAttribute(CrocsXmlConstants.A_ZIP_CODE)  
		                 + "\",\"country\": \"" + eleContactInformation.getAttribute(CrocsXmlConstants.A_COUNTRY)  
		                 + "\"}"
		                 + "}";
		
		logger.verbose("CrocsCreateStoreFeedDetailsToIV: jsonString output :"+ jsonString);

     	// Generate IV input.
		Document docIVInput = CommonUtil.formIVInputForNode(CrocsRestConstants.APPLICATION_JSON,
				CrocsRestConstants.METHOD_PUT, CrocsIVAPIConstants.IV_UPSERT_NODE_API, jsonString, nodeId);
        
		logger.verbose("CrocsCreateStoreFeedDetailsToIV : formCreateNodeIVInput : docIVInput : End : " + SCXmlUtil.getString(docIVInput));
        return docIVInput;
	}

	//START-EOMS-11895
	private String setTimezone(YFSEnvironment env, String localeCode) throws Exception {
		String timezone = null;
		Document getLocaleListInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_LOCALE);
		Element getLocaleListEle = getLocaleListInDoc.getDocumentElement();
		getLocaleListEle.setAttribute(CrocsXmlConstants.A_LOCALECODE, localeCode);
		
		Document localeListTemplate = SCXmlUtil.createDocument(CrocsXmlConstants.E_LOCALE_LIST);
		Element localeEle = localeListTemplate.createElement(CrocsXmlConstants.E_LOCALE);
		localeEle.setAttribute(CrocsXmlConstants.A_LOCALECODE, "");
		localeEle.setAttribute(CrocsXmlConstants.A_TIMEZONE, "");
		localeListTemplate.getDocumentElement().appendChild(localeEle);

		Document getLocaleListOut = null;
		try {
			getLocaleListOut = CommonUtil.invokeAPI(env, localeListTemplate, CrocsConstant.GET_LOCALE_LIST_API, getLocaleListInDoc);
		} catch (Exception e) {
			logger.verbose("Exception while setting Timezone in store" +e.getMessage());
			throw e;
		}

		if (getLocaleListOut != null && getLocaleListOut.getDocumentElement() != null) {
			Element eleLocale = SCXmlUtil.getChildElement(getLocaleListOut.getDocumentElement(),CrocsXmlConstants.E_LOCALE);
			if (!YFCObject.isVoid(eleLocale)) {
				timezone = eleLocale.getAttribute(CrocsXmlConstants.A_TIMEZONE);
			}
		}
		return timezone;
	}
	//END-EOMS-11895

}