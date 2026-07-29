package com.crocs.oms.order;

import com.crocs.oms.common.util.CommonUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CrocsConstant;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import org.w3c.dom.NodeList;

/**
 * @author IBM
 *
 */
public class CrocsFilterChargeCategory implements CrocsConstant  {

    private static YFCLogCategory logger = YFCLogCategory.instance(CrocsFilterChargeCategory.class);

    /**
     * This method filters the charge list (either ChargeCategory or ChargeName) based on the provided filtering criteria.
     * @param env
     * @param indoc
     * @param templateName - the template name to be used in the API call (for ChargeCategory or ChargeName)
     * @param apiName - the API name to be invoked (for ChargeCategory or ChargeName)
     * @param chargeType - the charge type to be used for filtering (ChargeCategory or ChargeName)
     * @param removeCondition - the condition for removal (i.e., matching charge category and name/description)
     * @return updated Document with filtered charge list
     */

    private Document filterChargeList(YFSEnvironment env, Document indoc, String templateName,
                                      String apiName, String chargeType, String removeCondition) throws Exception {

        Document chargeListDoc = null;

        try {
            logger.debug("Input to filterChargeList: " + SCXmlUtil.getString(indoc));

            chargeListDoc = CommonUtil.invokeAPI(env, templateName, apiName, indoc);
            logger.debug("Charge list fetched is::" + SCXmlUtil.getString(chargeListDoc));

            Element chargeListOutEle = chargeListDoc.getDocumentElement();
            NodeList chargeListEle = SCXmlUtil.getXpathNodes(chargeListOutEle, chargeType);
            logger.debug(chargeType + " list length is::" + chargeListEle.getLength());

            if (chargeListEle.getLength() > 0) {
                for (int i = 0; i < chargeListEle.getLength(); ++i) {
                    Element eleCharge = (Element) chargeListEle.item(i);
                    logger.debug(chargeType + " element is:" + SCXmlUtil.getString(eleCharge));

                    String chargeCategory = eleCharge.getAttribute(A_CHARGE_CATEGORY);
                    String chargeNameOrDescription = eleCharge.getAttribute(chargeType.equals(A_CHARGE_CATEGORY) ? A_DESCRIPTION : A_CHARGE_NAME);
                    logger.debug("chargeCategory is:" + chargeCategory);
                    logger.debug(chargeType + " is:" + chargeNameOrDescription);

                    // Check if the condition matches for removal
                    if (removeCondition.equalsIgnoreCase(chargeCategory) && removeCondition.equalsIgnoreCase(chargeNameOrDescription)) {
                        chargeListOutEle.removeChild(eleCharge);
                        logger.debug(chargeType + " list after update is:" + SCXmlUtil.getString(chargeListOutEle));
                    }
                }
            }

        } catch (Exception ex) {
            logger.debug("Exception in method filterChargeList: " + ex.getStackTrace());
        }

        if (logger.isDebugEnabled()) {
            logger.debug("End of method filterChargeList(): " + SCXmlUtil.getString(indoc));
        }
        logger.debug("Updated " + chargeType + " list is:" + SCXmlUtil.getString(chargeListDoc));
        return chargeListDoc;
    }

    /**
     * Filter the charge category list by removing "ShippingCharge" if present.
     * @param env
     * @param indoc
     * @return updated charge category list
     */
    public Document filterChargeCategoryList(YFSEnvironment env, Document indoc) throws Exception {
        return filterChargeList(env, indoc, TEMPLATE_GET_CHARGE_CATEGORY_LIST, API_GET_CHARGE_CATEGORY_LIST, A_CHARGE_CATEGORY, CHARGE_CATEGORY_SHIPPING_CHARGE);
    }

    /**
     * Filter the charge name list by removing "ShippingCharge" if present.
     * @param env
     * @param indoc
     * @return updated charge name list
     */
    public Document filterChargeNameList(YFSEnvironment env, Document indoc) throws Exception {
        return filterChargeList(env, indoc, TEMPLATE_GET_CHARGE_NAME_LIST, API_GET_CHARGE_NAME_LIST, A_CHARGE_NAME, CHARGE_CATEGORY_SHIPPING_CHARGE);
    }
}

