package com.crocs.oms.order.migration;

import com.crocs.oms.common.util.CommonUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CrocsConstant;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import org.w3c.dom.NodeList;


public class CrocsChangeOrderStatusForSO implements CrocsConstant {

    private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsChangeOrderStatusForSO.class);

        /**
         *This class is to handle the SO status for migrated orders.
         *Once the return order is moved to Return Invoiced,
         * move the SO to Return Created using the changeOrderStatus
         * call
         * @param env
         */

        public Document prepareChangeOrderStatusDoc(YFSEnvironment env, Document indoc) throws Exception {

            logger.debug("Input to prepareChangeOrderStatusDoc: " + SCXmlUtil.getString(indoc));

            Document changeOrderStatusOutDoc = null;

            Element orderEle = indoc.getDocumentElement();
            NodeList orderLineList = orderEle.getElementsByTagName(E_ORDER_LINE);

            logger.debug("Found " + orderLineList.getLength() + " OrderLines");

            Document changeOrderStatusDoc = SCXmlUtil.createDocument(E_ORDER_STATUS_CHANGE);
            Element changeOrderStatusEle = changeOrderStatusDoc.getDocumentElement();

            if (orderLineList.getLength() == 0) {
                return changeOrderStatusDoc;
            }

            Element OrderLineEle = (Element) orderLineList.item(0);
            Element derivedFromOrder = SCXmlUtil.getChildElement(OrderLineEle, E_DERIVED_FROM_ORDER);

            changeOrderStatusEle.setAttribute(A_TRANSACTION_ID, VAL_INCLUDE_IN_RET);
            changeOrderStatusEle.setAttribute(A_SELECT_METHOD, VAL_WAIT);
            changeOrderStatusEle.setAttribute(A_IGNORE_TXN_DEPENDENCIES, FLAG_Y);
            changeOrderStatusEle.setAttribute(A_ORDER_NO, derivedFromOrder.getAttribute(A_ORDER_NO));
            changeOrderStatusEle.setAttribute(A_ORDER_HEADER_KEY, OrderLineEle.getAttribute(A_DERIVED_FROM_ORDER_HEADER_KEY));
            changeOrderStatusEle.setAttribute(A_ENTERPRISE_CODE, derivedFromOrder.getAttribute(A_ENTERPRISE_CODE));
            changeOrderStatusEle.setAttribute(A_DOCUMENT_TYPE, derivedFromOrder.getAttribute(A_DOCUMENT_TYPE));

            Element orderLinesEle = SCXmlUtil.createChild(changeOrderStatusEle, E_ORDER_LINES);

            for (int i = 0; i < orderLineList.getLength(); i++) {
                Element orderLine = (Element) orderLineList.item(i);

                Element changeOrderStatusLine = SCXmlUtil.createChild(orderLinesEle, E_ORDER_LINE);
                changeOrderStatusLine.setAttribute(A_BASE_DROP_STATUS, VAL_RETURN_CREATED);
                changeOrderStatusLine.setAttribute(A_ORDER_LINE_KEY, orderLine.getAttribute(A_DERIVED_FROM_ORDER_LINE_KEY));
                changeOrderStatusLine.setAttribute(A_PRIME_LINE_NO, orderLine.getAttribute(A_PRIME_LINE_NO));
                changeOrderStatusLine.setAttribute(A_QUANTITY, orderLine.getAttribute(A_ORDERED_QTY));
                changeOrderStatusLine.setAttribute(A_SUB_LINE_NO, orderLine.getAttribute(A_SUB_LINE_NO));

                logger.debug("Prepared OrderLine: " + SCXmlUtil.getString(changeOrderStatusLine));
            }


            logger.debug("Final changeOrderStatus doc: " + SCXmlUtil.getString(changeOrderStatusDoc));

            changeOrderStatusOutDoc = CommonUtil.invokeAPI(env, TEMPLATE_CHANGE_ORDER_STATUS,
                    API_CHANGE_ORDER_STATUS, changeOrderStatusDoc);
            return changeOrderStatusOutDoc;
        }

        }


