package com.crocs.oms.order;

import java.util.Properties;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCException;
import com.yantra.yfs.japi.YFSEnvironment;

/**
 * EOMS-2596: Processes the decrypted Forter payload from SFCC.
 *
 * EOMS-8616: Enhanced to populate Forter hangoff-table fields, including
 * additional mobile/app attributes required for the new hangoff-table implementation.
 */
public class CrocsOrderForterCheck {

	private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsOrderForterCheck.class);

	private Properties properties;
	
	/**
	 * Entry point for processing Forter encrypted data on an order.
	 *
	 * <p>
	 * If the order contains the ExtnForterStorage attribute, the encrypted
	 * payload is decrypted and the corresponding Forter information is
	 * populated into the order.
	 * </p>
	 *
	 * @param env YFS environment
	 * @param inputDoc Input Order document
	 * @return inputDoc with updated forter attributes in it
	 */
	public Document processCrocsOrder(YFSEnvironment env, Document inputDoc) {
		logger.beginTimer("CrocsOrderForterCheck::processCrocsOrder: START:" + SCXmlUtil.getString(inputDoc));

		Element eleOrder = inputDoc.getDocumentElement();
		Element eleExtnOrder = SCXmlUtil.getXpathElement(eleOrder, CrocsXmlConstants.XPATH_ORDER_EXTN);

		if (!YFCCommon.isVoid(eleExtnOrder)) {
			processForterEncryptedData(eleOrder, eleExtnOrder);
		}
		logger.verbose("CrocsOrderForterCheck : processCrocsOrder" + SCXmlUtil.getString(inputDoc));
		logger.endTimer("CrocsOrderForterCheck::processCrocsOrder: END:" + SCXmlUtil.getString(inputDoc));
		return inputDoc;
	}

	/**
	 * Decrypts the Forter payload and imports the processed data into Hangoff Table
	 *
	 * @param eleOrder Order element
	 * @param eleExtnOrder Order Extn element
	 * 
	 */
	private void processForterEncryptedData(Element eleOrder, Element eleExtnOrder) {
		try {
			String strEncryptedForter = eleExtnOrder.getAttribute(CrocsConstant.A_FORTER_STORAGE);
			if (!YFCCommon.isVoid(strEncryptedForter)) {

				String strUseHangOffTable = !YFCCommon.isVoid(properties)
				        ? properties.getProperty(CrocsXmlConstants.A_USE_HANGOFF_TABLE)
				        : null;

				if (YFCCommon.isVoid(strUseHangOffTable)) {
				    strUseHangOffTable = CrocsXmlConstants.FLAG_Y;
				}
				
				// Decrypting the Forter string
				CrocsDecryption decrypt = new CrocsDecryption();
				Document docForter = decrypt.processEncryptedText(strEncryptedForter);

				Element eleForter = docForter.getDocumentElement();
				eleForter.setAttribute(CrocsConstant.A_ENTRY_TYPE, eleOrder.getAttribute(CrocsConstant.A_ENTRY_TYPE));
				
				eleForter.setAttribute(CrocsXmlConstants.A_USE_HANGOFF_TABLE, strUseHangOffTable);

				// Adding custom attributes from the decrypted Forter document
				CrocsHeaderCustomAttributes cust = new CrocsHeaderCustomAttributes();
				docForter = cust.addCustomAttributes(docForter);

				if (CrocsXmlConstants.FLAG_Y.equals(strUseHangOffTable)) {
					logger.verbose("Importing Forter fields using hangoff-table");
					importHangoffFields(eleForter, eleExtnOrder);
				} else {
					logger.verbose("Importing Forter fields using custom attributes.");
					importCustomAttributes(eleOrder, docForter);
				}

				// Remove the Forter Storage attribute after processing
				eleExtnOrder.removeAttribute(CrocsConstant.A_FORTER_STORAGE);
			}
		} catch (Exception e) {
			logger.verbose("processForterEncryptedData" + e.getMessage());
			throw new YFCException("processForterEncryptedData" + e.getMessage());
		}
	}

	/**
	 * EOMS-8616: Imports the Forter fields list produced on the hangoff-table using eleExtnOrder.
	 */
	private void importHangoffFields(Element eleForter, Element eleExtnOrder) {
		Element eleForterFieldList = SCXmlUtil.getChildElement(eleForter, CrocsXmlConstants.E_CROCS_FORTER_FIELDS_LIST);
		
		if (!YFCCommon.isVoid(eleForterFieldList)) {
			SCXmlUtil.importElement(eleExtnOrder, eleForterFieldList);
		}
	}

	/**
	 * Import the CustomAttributes element into eleOrder.
	 */
	private void importCustomAttributes(Element eleOrder, Document docForter) {
		Element eleCustAtt = SCXmlUtil.getChildElement(docForter.getDocumentElement(), CrocsXmlConstants.E_CUSTOM_ATTRIBUTES);
		
		if (!YFCCommon.isVoid(eleCustAtt)) {
			SCXmlUtil.importElement(eleOrder, eleCustAtt);
		}
	}

	/**
	 * Sets the configuration properties for this API.
	 *
	 * @param properties API configuration properties
	 */
	public void setProperties(Properties properties) {
		this.properties = properties;
	}
}
