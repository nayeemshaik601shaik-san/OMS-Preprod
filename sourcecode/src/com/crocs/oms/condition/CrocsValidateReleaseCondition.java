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


public class CrocsValidateReleaseCondition implements YCPDynamicConditionEx, CrocsConstant {
    private static YFCLogCategory logger = YFCLogCategory.instance(CrocsValidateReleaseCondition.class);
    Map properties = null;

    @Override
    public boolean evaluateCondition (YFSEnvironment env, String s, Map mapData, Document indoc){
        logger.debug("Input to the evaluateCondition is:" + SCXmlUtil.getString(indoc));

        boolean isReleased = false;

        Element orderEle = indoc.getDocumentElement();
        Element orderLines = SCXmlUtil.getChildElement(orderEle,E_ORDER_LINES);
        NodeList orderLine = SCXmlUtil.getXpathNodes(orderLines,E_ORDER_LINE);

        if(orderLine.getLength() > 0) {

            Element eleOrderLine = (Element) orderLine.item(0);
            logger.debug("eleOrderLine is::" + SCXmlUtil.getString(eleOrderLine));

            Element orderRelFromStatuses = SCXmlUtil.getChildElement(eleOrderLine, E_FROM_ORDER_REL_STATUSES);
            Element orderRelFromStatus = SCXmlUtil.getChildElement(orderRelFromStatuses, E_FROM_ORDER_REL_STATUS);

            Element orderRelToStatuses = SCXmlUtil.getChildElement(orderRelFromStatus, E_TO_ORDER_REL_STATUSES);
            Element orderRelToStatus = SCXmlUtil.getChildElement(orderRelToStatuses, E_TO_ORDER_REL_STATUS);
            logger.debug("orderRelToStatus is:" + SCXmlUtil.getString(orderRelToStatus));
            logger.debug("orderRelFromStatus is:" + SCXmlUtil.getString(orderRelFromStatus));


            String fromStatus = "";
            String toStatus = "";
            if (!YFCCommon.isVoid(orderRelFromStatus)) {
                fromStatus = orderRelFromStatus.getAttribute(A_STATUS);
            }

            if (!YFCCommon.isVoid(orderRelToStatus)) {
                toStatus = orderRelToStatus.getAttribute(A_STATUS);
            }

            logger.debug("fromStatus is::" + fromStatus);
            logger.debug("toStatus is::" + toStatus);

            if (!YFCCommon.isVoid(fromStatus) && STATUS_RELEASE.equalsIgnoreCase(fromStatus)) {

                if (!YFCCommon.isVoid(toStatus) && !STATUS_INCLUDED_IN_SHIPMENT.equalsIgnoreCase(toStatus)) {
                    isReleased = true;
                }
            }
        }
        logger.debug("isReleased is::"+isReleased);
        return isReleased;
    }

    public void setProperties(Map map) {
        this.properties = map;
    }

}


