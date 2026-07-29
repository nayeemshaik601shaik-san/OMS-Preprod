package com.crocs.oms.condition;

import com.crocs.oms.common.util.CrocsConstant;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.ycp.japi.YCPDynamicConditionEx;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.Map;


    public class CrocsValidateCancellationCondition implements YCPDynamicConditionEx, CrocsConstant {

    private static YFCLogCategory logger = YFCLogCategory.instance(CrocsValidateCancellationCondition.class);

    Map properties = null;

        /**
         * EOMS-6007 This condition will validate whether the message
         * needs to be sent to OIC based on the OrderReleaseToStatus and OrderType
         *
         * @param env
         * @param indoc
         * @return
         */

    @Override
    public boolean evaluateCondition (YFSEnvironment env, String s, Map mapData, Document indoc) {

    logger.verbose("CrocsValidateCancellationCondition : evaluateCondition : Input is:" + SCXmlUtil.getString(indoc));


        Element orderEle = indoc.getDocumentElement();
        String orderType = orderEle.getAttribute(A_ORDER_TYPE);
        logger.verbose("orderType is:" + orderType);

        if (!ORDER_TYPE_MP.equalsIgnoreCase(orderType) && !ORDER_TYPE_RETAIL.equalsIgnoreCase(orderType)) {
            return false;
        }

        Element orderLines = SCXmlUtil.getChildElement(orderEle, E_ORDER_LINES);
        NodeList orderLine = SCXmlUtil.getXpathNodes(orderLines, E_ORDER_LINE);

        for (int i = 0; i < orderLine.getLength(); ++i) {
            Element eleOrderLine = (Element) orderLine.item(i);

            String toStatus = getToOrderReleaseStatus(eleOrderLine);
            String fromStatus = getFromOrderReleaseStatus(eleOrderLine);
            logger.verbose("toStatus is:" + toStatus);

            // Check if the toStatus is 9000
            if ("9000".equals(toStatus) && !STATUS_INCLUDED_IN_SHIPMENT.equalsIgnoreCase(fromStatus)) {
                return true;

            }
        }

        // If none of the order lines has status 9000, return false
        return false;
    }

        private String getFromOrderReleaseStatus(Element eleOrderLine) {
            String fromStatus = "";
            Element orderRelFromStatuses = SCXmlUtil.getChildElement(eleOrderLine, E_FROM_ORDER_REL_STATUSES);
            Element orderRelFromStatus = SCXmlUtil.getChildElement(orderRelFromStatuses, E_FROM_ORDER_REL_STATUS);

            if (!YFCCommon.isVoid(orderRelFromStatus)) {
                fromStatus = orderRelFromStatus.getAttribute(A_STATUS);
                logger.verbose("fromStatus is:" + fromStatus);

            }
            return fromStatus;

        }

        private String getToOrderReleaseStatus(Element eleOrderLine) {
            String toStatus = "";
            Element orderRelFromStatuses = SCXmlUtil.getChildElement(eleOrderLine, E_FROM_ORDER_REL_STATUSES);
            Element orderRelFromStatus = SCXmlUtil.getChildElement(orderRelFromStatuses, E_FROM_ORDER_REL_STATUS);
            Element orderRelToStatuses = SCXmlUtil.getChildElement(orderRelFromStatus, E_TO_ORDER_REL_STATUSES);
            Element orderRelToStatus = SCXmlUtil.getChildElement(orderRelToStatuses, E_TO_ORDER_REL_STATUS);

            if (!YFCCommon.isVoid(orderRelToStatus)) {
                toStatus = orderRelToStatus.getAttribute(A_STATUS);
                logger.verbose("toStatus is:" + toStatus);
            }
          return toStatus;
        }

       public void setProperties(Map map) {
        this.properties = map;
        }

}


