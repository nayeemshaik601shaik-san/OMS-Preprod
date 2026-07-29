package com.crocs.oms.ivsync;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsIVAPIConstants;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.util.restapi.CrocsRestConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;

/**
 * EOMS-1009 : Item sync to IV
 * 
 * This class creates the input the the IV upsert items api.
 * 
 */
public class CrocsPublishItemDetailsToIV{
	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsPublishItemDetailsToIV.class);
	
	public Document formCreateItemIVInput(Document docInput) {
		logger.verbose("CrocsPublishItemDetailsToIV : formCreateItemIVInput : Start : " + SCXmlUtil.getString(docInput));
		Element eleInput = docInput.getDocumentElement();
        
        // Directly build the IV JSON input content as a string
		String strJsonInput = "{\"items\":[{\"itemId\":\"" + eleInput.getAttribute(CrocsXmlConstants.A_ITEM_ID)
				+ "\",\"unitOfMeasure\":\"" + eleInput.getAttribute(CrocsXmlConstants.A_UNIT_OF_MEASURE)
				+ "\",\"subCatalogOrganizationCode\":\"" + eleInput.getAttribute(CrocsXmlConstants.A_ORGANIZATION_CODE)
				+ "\"}]}";
        
     	// Generate IV input.
		Document docIVInput = CommonUtil.formIVInput(CrocsRestConstants.APPLICATION_JSON,
				CrocsRestConstants.METHOD_POST, CrocsIVAPIConstants.IV_UPSERT_ITEMS_API, strJsonInput);
        
		logger.verbose("CrocsPublishItemDetailsToIV : formCreateItemIVInput : docIVInput : End : " + SCXmlUtil.getString(docIVInput));
        return docIVInput;
	}
}