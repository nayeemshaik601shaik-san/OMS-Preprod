package com.crocs.oms.condition;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.ycp.japi.YCPDynamicConditionEx;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.util.Map;


public class HeyDudeCheckCancellationCondition implements YCPDynamicConditionEx, CrocsConstant {

    private static YFCLogCategory logger = YFCLogCategory.instance(HeyDudeCheckCancellationCondition.class);

    Map properties = null;


    /**
     * EOMS-6193 This condition will validate whether the message
     * needs to be sent to GlobalE based on the Order Status
     *
     * @param env
     * @param indoc
     * @return
     */

    @Override
    public boolean evaluateCondition (YFSEnvironment env, String s, Map mapData, Document indoc) {

    logger.verbose("HeyDudeCheckCancellationCondition : evaluateCondition : Input is :" + SCXmlUtil.getString(indoc));


        try {
            // Extract OrderHeaderKey from input document
            Element orderEle = indoc.getDocumentElement();
            String orderHeaderKey = orderEle.getAttribute(A_ORDER_HEADER_KEY);

            logger.verbose("OrderHeaderKey is:: " + orderHeaderKey);

            Document getOrderListInput = SCXmlUtil.createDocument(E_ORDER);
            Element orderElement = getOrderListInput.getDocumentElement();
            orderElement.setAttribute(A_ORDER_HEADER_KEY, orderHeaderKey);

            logger.verbose("HeyDudeCheckCancellationCondition : evaluateCondition : getOrderListInput is: " + SCXmlUtil.getString(getOrderListInput));

            Document getOrderListOutDoc = CommonUtil.invokeAPI(env, TEMPLATE_GET_ORDER_LIST_FORTER_UPDATE, API_GET_ORDER_LIST, getOrderListInput);

            logger.verbose("HeyDudeCheckCancellationCondition : evaluateCondition : getOrderListOutDoc is: " + SCXmlUtil.getString(getOrderListOutDoc));

            Element orderOutEle = SCXmlUtil.getChildElement(getOrderListOutDoc.getDocumentElement(), E_ORDER);

            if (orderOutEle == null) {
                logger.verbose("No order element found in getOrderList output");
                return false;
            }

            String maxStatus = orderOutEle.getAttribute(A_MAX_ORDER_STATUS);
            String minStatus = orderOutEle.getAttribute(A_MIN_ORDER_STATUS);

            logger.verbose("MaxStatus: " + maxStatus + ", MinStatus: " + minStatus);

            // Return true only if both statuses are 9000 (cancelled)
            if ("9000".equalsIgnoreCase(maxStatus) && "9000".equalsIgnoreCase(minStatus)) {
                return true;
            }

            return false;

        } catch (Exception e) {
            logger.error("HeyDudeCheckCancellationCondition: evaluateCondition : Exception occurred", e);
            throw new YFSException("HeyDudeCheckCancellationCondition: evaluateCondition : exception " + e.getMessage());
        }
    }

    public void setProperties(Map map) {
        this.properties = map;
    }

}


