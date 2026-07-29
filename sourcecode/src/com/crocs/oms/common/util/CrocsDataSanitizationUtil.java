package com.crocs.oms.common.util;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class CrocsDataSanitizationUtil {

	private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsDataSanitizationUtil.class);
	
	/**
	 * Purpose:-
	 	This class sanitizes the configured order Attribute value where emoji characters, special symbols, and other non-standard value exists
	 * Flow:-
	 	Fetches configured element xpath from from CommonCode for the operation.
	 	If no Xpath present, santization logic skips
	 	Else, logic validate if requires sanitization and santizates the attribute value.
	 	
	 * Jira:- EOMS-5161 : Data Sanitization - Clean Emoji Characters from Order Data
	 */
	
	/*  */
	private static final Pattern SAFE_PATTERN = Pattern.compile("[^A-Za-z0-9 .,'#/-]");
	
	/*  */
	private static final Pattern EMOJI_TEXT_PATTERN = Pattern.compile("(:[a-zA-Z0-9_+-]+:)+");
	
	/*  */
	private static final Pattern HEX_PATTERN = Pattern.compile("&#x([0-9A-Fa-f]+);");
	
	/*  */
	private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	
	/*  */
	private static final Pattern NEEDS_SANITIZATION = Pattern.compile("[^\\p{ASCII}]|(:[a-zA-Z0-9_+-]+:)|&#x[0-9A-Fa-f]+;|[’‘`]");

	private static final Map<Integer, Character> ENCLOSED_MAP = buildMap();

	private static final String LOG_ORDER_NO_PREFIX = "OrderNo: ";
	private static final String LOG_SANITIZING_XPATH = " Sanitizing XPath: ";
	private static final String LOG_ORIGINAL_VALUE = " Original value: '";
	private static final String LOG_SANITIZED_VALUE = " Sanitized value: '";
	private static final String LOG_SKIPPING_XPATH = " Skipping sanitization for XPath: ";
	private static final String LOG_NO_NODES_XPATH = " No nodes found for XPath: ";

	public Document dataSanitization(YFSEnvironment yfsEnvironment, Document document) throws Exception {
		logger.verbose("Input Document : " + XMLUtil.getXMLString(document));
		logger.beginTimer("CrocsDataSanitizationUtil::dataSanitization: START:" + SCXmlUtil.getString(document));

		if (!YFCCommon.isVoid(document)) {
			Document getCommonCodeList = CommonUtil.getCommonCodeList(yfsEnvironment, CrocsConstant.STR_CROCS,
					CrocsConstant.CROCS_SANITIZE_ATTR, null);

			if (!YFCCommon.isVoid(getCommonCodeList)) {
				getXPathForSanitization(document, getCommonCodeList);
			} else 
				logger.info("CommonCode CROCS_SANITIZE_ATTR not found. Skipping sanitization logic.");
			
		} else
			logger.info("CrocsDataSanitizationUtil::dataSanitization Input Document is null");
		

		logger.verbose("CrocsDataSanitizationUtil::dataSanitization Final Output: " + SCXmlUtil.getString(document));
		logger.endTimer("CrocsDataSanitizationUtil::dataSanitization: END");
		return document;
	}

	public static void getXPathForSanitization(Document invoiceDoc, Document commonCodeDoc) {
		try {
			String orderNo = invoiceDoc.getDocumentElement().getAttribute(CrocsXmlConstants.A_ORDER_NO);
			XPath xpath = XPathFactory.newInstance().newXPath();
			NodeList commonCodes = commonCodeDoc.getElementsByTagName(CrocsXmlConstants.E_COMMON_CODE);
			if (YFCCommon.isVoid(commonCodes) || commonCodes.getLength() == 0) {
				logger.info("No CommonCode List found for CodeType CROCS_SANITIZE_ATTR. Skipping sanitization.");
				return;
			}
			for (int k = 0; k < commonCodes.getLength(); k++) {
				Element commonCode = (Element) commonCodes.item(k);
				NodeList attrList = commonCode.getElementsByTagName(CrocsXmlConstants.E_COMMON_CODE_ATTRIBUTE);
				if (attrList.getLength() > 0) {
					for (int i = 0; i < attrList.getLength(); i++) {
						String fullXPath = ((Element) attrList.item(i)).getAttribute("Value");
						if (!YFCCommon.isVoid(fullXPath)) {
							try {
								NodeList nodes = (NodeList) xpath.compile(fullXPath).evaluate(invoiceDoc, XPathConstants.NODESET);
								if (nodes.getLength() > 0) {
									for (int j = 0; j < nodes.getLength(); j++) {
										Node node = nodes.item(j);
										String value = node.getNodeValue();

										if (!YFCCommon.isVoid(value) && isSanitizationRequired(value)) {
											logger.info(LOG_ORDER_NO_PREFIX + orderNo + LOG_SANITIZING_XPATH + fullXPath + LOG_ORIGINAL_VALUE + value + "'");											
											String sanitized = sanitize(value);
											node.setNodeValue(sanitized);
											logger.info(LOG_ORDER_NO_PREFIX + orderNo + " Sanitized XPath: " + fullXPath + LOG_SANITIZED_VALUE + sanitized + "'");
										} else {
											logger.info(LOG_ORDER_NO_PREFIX + orderNo + LOG_SKIPPING_XPATH + fullXPath);										}
									}
								} else {
									logger.info(LOG_ORDER_NO_PREFIX + orderNo + LOG_NO_NODES_XPATH + fullXPath);
								}
							} catch (XPathExpressionException e) {
								logger.info("Invalid XPath Provided for santization in commoncode: " + fullXPath+"\n"+ e.getMessage());
							}
						}
					}
				} else {
					logger.info("No CommonCodeAttributes List found. Skipping sanitization.");
				}
			} 
		} catch (YFSException e) {
			logger.info("ErrorCode:" + e.getErrorCode() + " Message:" + e.getMessage() + " Description:" + e.getErrorDescription());
			throw new YFSException(e.getMessage(), e.getErrorCode(), e.getErrorDescription());
		}
	}

	private static Map<Integer, Character> buildMap() {
		Map<Integer, Character> map = new HashMap<>();
		String enclosed = "🅐🅑🅒🅓🅔🅕🅖🅗🅘🅙🅚🅛🅜🅝🅞🅟🅠🅡🅢🅣🅤🅥🅦🅧🅨🅩";
		String ascii = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		int[] cps = enclosed.codePoints().toArray();
		for (int i = 0; i < cps.length; i++) { 
			map.put(cps[i], ascii.charAt(i)); 
		}
		return map;
	}

	public static String sanitize(String input) {

		if (YFCCommon.isVoid(input)) return input;

		String value = input.trim();

		if (!isSanitizationRequired(value)) {
			return value;
		}

		logger.info("Sanitization started with value: '" + value + "'");

		// Pattern sequence must remain unchanged
		// HEX Pattern
		if (HEX_PATTERN.matcher(value).find()) {
			String before = value;
			value = decodeHex(value);
			if (!before.equals(value)) {
				logging("Applied Pattern 1: HEX", before, value);
				if (!isSanitizationRequired(value)) return value;
			}
		}

		// Enclosed
		String before = value;
		value = replaceEnclosed(value);
		if (!before.equals(value)) {
			logging("Applied Pattern 2: ENCLOSED normalized", before, value);
			if (!isSanitizationRequired(value)) return value;
		}

		// Font 
		before = value;
		value = Normalizer.normalize(value, Normalizer.Form.NFKC);
		if (!before.equals(value)) {
			logging("Applied Pattern 3: FONT normalized", before, value);
			if (!isSanitizationRequired(value)) return value;
		}

		// Accent
		before = value;
		value = Normalizer.normalize(value, Normalizer.Form.NFD);
		value = DIACRITICS.matcher(value).replaceAll("");
		if (!before.equals(value)) {
			logging("Applied Pattern 4: ACCENT removed", before, value);
			if (!isSanitizationRequired(value)) return value;
		}

		// Apostrophe
		before = value;
		value = value.replaceAll("[’‘`]", "'");
		if (!before.equals(value)) {
			logging("Applied Pattern 5: APOSTROPHE normalized", before, value);
			if (!isSanitizationRequired(value)) return value;
		}

		// Emoji text
		if (EMOJI_TEXT_PATTERN.matcher(value).find()) {
			String beforeEmoji = value;
			value = EMOJI_TEXT_PATTERN.matcher(value).replaceAll("");
			if (!beforeEmoji.equals(value)) {
				logging("Applied Pattern 6: EMOJI removed", beforeEmoji, value);
				if (!isSanitizationRequired(value)) return value;
			}
		}

		// Final cleanup
		String beforeClean = value;
		value = SAFE_PATTERN.matcher(value).replaceAll("");
		if (!beforeClean.equals(value)) {
			logging("SYMBOL cleaned", beforeClean, value);
		}

		logger.info("Sanitization Completed with value: '" + value.trim() + "'");
		return value.trim();
	}
	public static boolean isSanitizationRequired(String input) {
		return input != null && NEEDS_SANITIZATION.matcher(input).find();
	}

	private static String decodeHex(String input) {
		Matcher m = HEX_PATTERN.matcher(input);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			try {
				int cp = Integer.parseInt(m.group(1), 16);
				m.appendReplacement(sb, Matcher.quoteReplacement(new String(Character.toChars(cp))));
			} catch (Exception e) {
				m.appendReplacement(sb, "");
			}
		}
		m.appendTail(sb);
		return sb.toString();
	}

	private static String replaceEnclosed(String input) {
		StringBuilder sb = new StringBuilder(input.length());
		input.codePoints().forEach(cp -> {
			Character mapped = ENCLOSED_MAP.get(cp);
			if (mapped != null) sb.append(mapped); else sb.appendCodePoint(cp);
		});
		return sb.toString();
	}

	private static void logging(String step, String before, String after) {
		if (!before.equals(after)) {
			logger.info(step + " before: '" + before + "' after: '" + after + "'");
		}
	}
}