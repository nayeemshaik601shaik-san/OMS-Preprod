package com.crocs.oms.order;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.CrocsPropertyEncrypterImpl;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;

/**
 * EOMS-2596: Encrypts Forter attributes received from SFCC before persisting
 * them in OMS.
 *
 * EOMS-8616: Enhanced to populate encrypted Forter fields for the hangoff-table
 * implementation, including additional mobile/app attributes for APP orders.
 */
public class CrocsHeaderCustomAttributes {
 
	private static final int TEXT_SIZE = 200;
	private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsHeaderCustomAttributes.class);

	/**
	 * Encrypts Forter attributes and prepares the output document using 
	 * which the encrypted values are
	 * populated in Forter hangoff-table fields.
	 * </p>
	 *
	 * @param docForter Decrypted Forter document
	 * @return Updated Forter document
	 */
	public Document addCustomAttributes(Document docForter) {
		logger.beginTimer("CrocsHeaderCustomAttributes:: Start of method addCustomAttributes with docForter: ");

		try {
			Element eleForter = docForter.getDocumentElement();
			CrocsPropertyEncrypterImpl prop = new CrocsPropertyEncrypterImpl();

			String strUseHangOffTable = eleForter.getAttribute(CrocsXmlConstants.A_USE_HANGOFF_TABLE);

			if (CrocsXmlConstants.FLAG_Y.equals(strUseHangOffTable)) {
				saveToHangoffTable(eleForter, prop);
			} else {
				saveToCustomAttributes(eleForter, prop);
			}
			
		} catch (Exception e) {
			logger.error("CrocsHeaderCustomAttributes : addCustomAttributes " + e.getMessage());
		}
		logger.verbose("CrocsHeaderCustomAttributes:: End of method addCustomAttributes with docForter: " + SCXmlUtil.getString(docForter));
		return docForter;
	}

	/**
	 * Encrypts the required Forter attributes and populates the
	 * CrocsForterFieldsList element used by the hangoff-table implementation.
	 *
	 * <p>
	 * Additional mobile/app fields are populated only for APP orders.
	 * </p>
	 *
	 * @param eleForter Root Forter element
	 * @param prop Encryption utility
	 * @throws Exception if encryption fails
	 */
	private void saveToHangoffTable(Element eleForter, CrocsPropertyEncrypterImpl prop) throws Exception {
		logger.verbose("CrocsHeaderCustomAttributes:: Start of method saveToHangoffTable with eleForter: ");
		
		String strCustomerIP = encryptIfPresent(prop, eleForter.getAttribute(CrocsConstant.A_CUSTOMER_IP));
		String strMerchantDomain = encryptIfPresent(prop, eleForter.getAttribute(CrocsConstant.A_MERCHANT_DOMAIN));
		String strUserAgent = encryptIfPresent(prop, eleForter.getAttribute(CrocsConstant.A_USER_AGENT));
		String strForterTokenCookie = encryptIfPresent(prop, eleForter.getAttribute(CrocsConstant.A_FORTER_TOKEN_COOKIE));

		Element eleForterFieldList = eleForter.getOwnerDocument().createElement(CrocsXmlConstants.E_CROCS_FORTER_FIELDS_LIST);
		Element eleForterFields = SCXmlUtil.createChild(eleForterFieldList, CrocsXmlConstants.E_CROCS_FORTER_FIELDS);

		eleForterFields.setAttribute(CrocsXmlConstants.A_CUSTOMER_IP_FORTER, strCustomerIP);
		eleForterFields.setAttribute(CrocsXmlConstants.A_MERCHANT_DOMAIN_FORTER, strMerchantDomain);

		processAndSetAttributes(eleForterFields, strUserAgent, 1, 5, CrocsXmlConstants.A_USER_AGENT_FORTER);
		processAndSetAttributes(eleForterFields, strForterTokenCookie, 1, 2, CrocsXmlConstants.A_FORTER_TOKEN_COOKIE_FORTER);

		String entryType = eleForter.getAttribute(CrocsConstant.A_ENTRY_TYPE);
		if (CrocsConstant.ENTRY_TYPE_APP.equalsIgnoreCase(entryType)) {
			setAppOrderFields(eleForter, eleForterFields, prop);
		}

		eleForter.appendChild(eleForterFieldList);
		
		logger.verbose("CrocsHeaderCustomAttributes:: End of method saveToHangoffTable with eleForter: ");
	}

	/**
	 * Encrypts and populates APP-specific Forter attributes required for
	 * the hangoff-table implementation.
	 *
	 * @param eleForter Source Forter element
	 * @param eleForterFields Target Forter fields element
	 * @param prop Encryption utility
	 * @throws Exception if encryption fails
	 */
	private void setAppOrderFields(Element eleForter, Element eleForterFields, CrocsPropertyEncrypterImpl prop) throws Exception {
		logger.verbose("CrocsHeaderCustomAttributes:: Start of method setAppOrderFields with eleForter: ");
		logger.verbose("eleForterFields: ");		

		String strForterMobileUID = encryptIfPresent(prop, eleForter.getAttribute(CrocsConstant.STR_FORTER_MOBILE_UID));
		String strMobileAppVersion = encryptIfPresent(prop, eleForter.getAttribute(CrocsConstant.STR_MOBILE_APP_VERSION));
		String strMobileDeviceBrand = encryptIfPresent(prop, eleForter.getAttribute(CrocsConstant.STR_MOBILE_DEVICE_BRAND));
		String strMobileDeviceModel = encryptIfPresent(prop, eleForter.getAttribute(CrocsConstant.STR_MOBILE_DEVICE_MODEL));
		String strMobileOSType = encryptIfPresent(prop, eleForter.getAttribute(CrocsConstant.STR_MOBILE_OS_TYPE));
		String strMerchantDeviceIdentifier = encryptIfPresent(prop, eleForter.getAttribute(CrocsConstant.STR_MERCHANT_DEVICE_IDENTIFIER));

		eleForterFields.setAttribute(CrocsXmlConstants.A_FORTER_MOBILE_UID, strForterMobileUID);
		eleForterFields.setAttribute(CrocsXmlConstants.A_MOBILE_APP_VERSION, strMobileAppVersion);
		eleForterFields.setAttribute(CrocsXmlConstants.A_MOBILE_DEVICE_BRAND, strMobileDeviceBrand);
		eleForterFields.setAttribute(CrocsXmlConstants.A_MOBILE_DEVICE_MODEL, strMobileDeviceModel);
		eleForterFields.setAttribute(CrocsXmlConstants.A_MOBILE_OS_TYPE, strMobileOSType);
		eleForterFields.setAttribute(CrocsXmlConstants.A_MERCHANT_DEVICE_IDENTIFIER, strMerchantDeviceIdentifier);
		
		logger.verbose("CrocsHeaderCustomAttributes:: End of method setAppOrderFields with updated eleForterFields: " + SCXmlUtil.getString(eleForterFields));
	}

	/**
	 * Populates the CustomAttributes element with encrypted Forter values.
	 * 
	 * @param eleForter Source Forter element
	 * @param prop Encryption utility
	 * @throws Exception if encryption fails
	 */
	private void saveToCustomAttributes(Element eleForter, CrocsPropertyEncrypterImpl prop) throws Exception {
		logger.verbose("CrocsHeaderCustomAttributes:: Start of method saveToCustomAttributes with eleForter: ");
		
		String strCustomerIP = encryptIfPresent(prop, eleForter.getAttribute(CrocsConstant.A_CUSTOMER_IP));
		String strMerchantDomain = encryptIfPresent(prop, eleForter.getAttribute(CrocsConstant.A_MERCHANT_DOMAIN));
		String strUserAgent = encryptIfPresent(prop, eleForter.getAttribute(CrocsConstant.A_USER_AGENT));
		String strForterTokenCookie = encryptIfPresent(prop, eleForter.getAttribute(CrocsConstant.A_FORTER_TOKEN_COOKIE));

		Element eleCustAtt = eleForter.getOwnerDocument().createElement(CrocsXmlConstants.A_CUSTOM_ATTRIBUTES);

		eleCustAtt.setAttribute(CrocsXmlConstants.A_TEXT_1, CrocsConstant.A_CUSTOMER_IP);
		eleCustAtt.setAttribute(CrocsXmlConstants.A_TEXT_2, strCustomerIP);
		eleCustAtt.setAttribute(CrocsXmlConstants.A_TEXT_3, CrocsConstant.A_MERCHANT_DOMAIN);
		eleCustAtt.setAttribute(CrocsXmlConstants.A_TEXT_4, strMerchantDomain);
		eleCustAtt.setAttribute(CrocsXmlConstants.A_TEXT_5, CrocsConstant.A_FORTER_TOKEN_COOKIE);
		eleCustAtt.setAttribute(CrocsXmlConstants.A_TEXT_10, CrocsConstant.A_USER_AGENT);

		processAndSetAttributes(eleCustAtt, strForterTokenCookie, 6, 9, CrocsXmlConstants.A_TEXT);
		processAndSetAttributes(eleCustAtt, strUserAgent, 11, 15, CrocsXmlConstants.A_TEXT);

		eleForter.appendChild(eleCustAtt);
		logger.verbose("CrocsHeaderCustomAttributes:: End of method saveToCustomAttributes with eleForter: " + SCXmlUtil.getString(eleForter));
	}

	/**
	 * Encrypts the supplied value when it is not null or blank.
	 *
	 * @param prop Encryption utility
	 * @param value Value to encrypt
	 * @return Encrypted value or an empty string when the input is blank
	 * @throws Exception if encryption fails
	 */
	private String encryptIfPresent(CrocsPropertyEncrypterImpl prop, String value) throws Exception {
		return YFCCommon.isVoid(value) ? "" : prop.encrypt(value.trim());
	}

	/**
	 * Splits the supplied value into fixed-size chunks and stores each chunk
	 * as sequentially numbered attributes on the specified XML element.
	 *
	 * @param element XML element on which the chunked attributes are populated
	 * @param value Value to be split into fixed-size chunks
	 * @param startIndex Starting index to use while naming the attributes
	 * @param maxIndex Maximum attribute index that can be populated
	 * @param strAttribute Attribute name prefix used to construct the final
	 *                     attribute names (for example, Text or UserAgent)
	 */
	private static void processAndSetAttributes(Element element, String value, int startIndex, int maxIndex, String strAttribute) {
		logger.verbose("CrocsHeaderCustomAttributes:: Start of method processAndSetAttributes for attribute: " + strAttribute);
		logger.verbose("startIndex: " + startIndex);
		logger.verbose("maxIndex: " + maxIndex);				
		for (int n = 0, q = startIndex; n < value.length() && q <= maxIndex; n += TEXT_SIZE, q++) {
			String chunk = (n + TEXT_SIZE <= value.length()) ? value.substring(n, n + TEXT_SIZE) : value.substring(n);
			element.setAttribute(strAttribute + q, chunk);
		}
		
		logger.verbose("CrocsHeaderCustomAttributes:: End of method processAndSetAttributes for attribute: " + strAttribute);
	}
}