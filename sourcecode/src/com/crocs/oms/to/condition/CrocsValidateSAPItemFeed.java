package com.crocs.oms.to.condition;

import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.ycp.japi.YCPDynamicConditionEx;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsAPIConstants;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsTemplateConstants;
import com.crocs.oms.common.util.CrocsXmlConstants;
import java.util.Map;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Validates SAP Item Feed and performs conditional logic: - Calls getItemList
 * API to check item existence - If exists → replaces ItemAliasList - If not →
 * calls testService
 */
public class CrocsValidateSAPItemFeed implements YCPDynamicConditionEx {

	private static final YFCLogCategory LOGGER = YFCLogCategory.instance(CrocsValidateSAPItemFeed.class);
	Map<String, Object> properties = null;

	/**
	 * Validates the item and executes the corresponding logic.
	 *
	 * @param env           OMS environment
	 * @param conditionName Condition name (unused)
	 * @param dataMap       Additional data
	 * @param inDoc         Input XML
	 * @return true if the item exists, false otherwise
	 */
	@Override
	public boolean evaluateCondition(YFSEnvironment env, String conditionName, Map dataMap, Document inDoc) {
		if (YFCCommon.isVoid(inDoc)) {
			LOGGER.error("Input document is null or empty.");
			return false;
		}

		try {
			LOGGER.verbose("CrocsValidateSAPItemFeed : evaluateCondition input : " + SCXmlUtil.getString(inDoc));

			Element eleItemList = inDoc.getDocumentElement();
			if (YFCCommon.isVoid(eleItemList)) {
				LOGGER.error("Missing root element in input XML.");
				return false;
			}

			Element eleItem = (Element) eleItemList.getElementsByTagName(CrocsXmlConstants.E_ITEM).item(0);
			if (YFCCommon.isVoid(eleItem)) {
				LOGGER.error("Missing <Item> element in input XML.");
				return false;
			}

			Document getItemListInput = prepareGetItemListInput(eleItem);
			Document getItemListOutput = invokeGetItemList(env, getItemListInput);

			if (YFCCommon.isVoid(getItemListOutput)) {
				LOGGER.verbose("getItemListOutput is null. Returning false.");
				return false;
			}

			Element eleItemListOut = getItemListOutput.getDocumentElement();
			Element eleItemOutput = (Element) getItemListOutput.getElementsByTagName(CrocsXmlConstants.E_ITEM).item(0);

			LOGGER.verbose("CrocsValidateSAPItemFeed : evaluateCondition getItemListOutput : "
					+ SCXmlUtil.getString(getItemListOutput));

			if (!YFCCommon.isVoid(eleItemOutput) && eleItemListOut.hasChildNodes()) {
				env.setTxnObject(CrocsConstant.A_SAP_GET_ITEM_LIST, getItemListOutput);
				return true;
			}

			LOGGER.verbose("Item not found in system.");
			return false;

		} catch (Exception e) {
			LOGGER.error("Exception in CrocsValidateSAPItemFeed.evaluateCondition: ", e);
			return false;
		}
	}

	/**
	 * Prepares input for getItemList API.
	 *
	 * @param eleItem Item element
	 * @return Document ready for getItemList API
	 */
	private Document prepareGetItemListInput(Element eleItem) {
		Document getItemListInput = SCXmlUtil.createDocument(CrocsXmlConstants.E_ITEM);
		Element eleGetItem = getItemListInput.getDocumentElement();

		eleGetItem.setAttribute(CrocsXmlConstants.A_ORGANIZATION_CODE,
				eleItem.getAttribute(CrocsXmlConstants.A_ORGANIZATION_CODE));
		eleGetItem.setAttribute(CrocsXmlConstants.A_ITEM_ID, eleItem.getAttribute(CrocsXmlConstants.A_ITEM_ID));
		eleGetItem.setAttribute(CrocsXmlConstants.A_UNIT_OF_MEASURE,
				eleItem.getAttribute(CrocsXmlConstants.A_UNIT_OF_MEASURE));

		return getItemListInput;
	}

	/**
	 * Calls getItemList API with a template.
	 *
	 * @param env              OMS environment
	 * @param getItemListInput Input document
	 * @return API output document
	 * @throws Exception if API call fails
	 */
	private Document invokeGetItemList(YFSEnvironment env, Document getItemListInput) throws Exception {
		LOGGER.verbose("CrocsValidateSAPItemFeed : invokeGetItemList input : " + SCXmlUtil.getString(getItemListInput));

		return CommonUtil.invokeAPI(env, CrocsTemplateConstants.TEMPLATE_GET_ITEM_LIST_TO,
				CrocsAPIConstants.API_GET_ITEM_LIST, getItemListInput);
	}
	
	@Override
	public void setProperties(Map map) {
		this.properties = map;
	}

}
