package com.crocs.oms.order;

import com.crocs.oms.common.util.CommonUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CrocsConstant;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * EOMS-710 This class is created to send unscheduled
 * and qty which is cancelled during release details to WMS
 *
 * 
 * @author IBM
 *
 */
public class CrocsCheckUpdatedLinesDuringRelease implements CrocsConstant  {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsCheckUpdatedLinesDuringRelease.class);

	/**
	 * This method does the below
	 * 1. checks if there are any cancelled qty or BO lines
	 * 2. get the details of cancelled or backordered lines from the release msg and send it to WMS
	 *
	 * @param env
	 * @param indoc
	 * @return
	 */

	public Document prepareOrderReleaseMsg(YFSEnvironment env, Document indoc) throws Exception {

        Document getOrderRelListOutDoc = null;
        Document finalReleaseDocument = null;
        try {

            logger.debug("Input to prepareOrderReleaseMsg: " + SCXmlUtil.getString(indoc));

            Element orderEle = indoc.getDocumentElement();
            Element orderLinesEle = SCXmlUtil.getChildElement(orderEle, E_ORDER_LINES);
            NodeList orderLineEle = SCXmlUtil.getXpathNodes(orderLinesEle, E_ORDER_LINE);
            logger.debug("orderLineEle length is:" + orderLineEle.getLength());

            String orderReleaseKey = null;

            if (orderLineEle.getLength() > 0) {

                Element eleOrderLine = (Element) orderLineEle.item(0);
                logger.debug("eleOrderLine is::" + SCXmlUtil.getString(eleOrderLine));

                Element orderRelFromStatuses = SCXmlUtil.getChildElement(eleOrderLine, E_FROM_ORDER_REL_STATUSES);
                Element orderRelFromStatus = SCXmlUtil.getChildElement(orderRelFromStatuses, E_FROM_ORDER_REL_STATUS);

                orderReleaseKey = orderRelFromStatus.getAttribute(A_ORDER_RELEASE_KEY);
                logger.debug("orderRelFromStatus is:" + SCXmlUtil.getString(orderRelFromStatus));
                logger.debug("orderReleaseKey is::" + orderReleaseKey);

                Document getOrderRelListInp = SCXmlUtil.createDocument(E_ORDER_RELEASE);
                Element orderReleaseEle = getOrderRelListInp.getDocumentElement();
                orderReleaseEle.setAttribute(A_ORDER_RELEASE_KEY, orderReleaseKey);
                logger.debug("getOrderRelListInp is:" + SCXmlUtil.getString(getOrderRelListInp));

                getOrderRelListOutDoc = CommonUtil.invokeAPI(env, TEMPLATE_GET_ORDER_RELEASE_LIST_ON_CHANGE,
                        API_GET_ORDER_RELEASE_LIST, getOrderRelListInp);

                logger.debug("getOrderRelListOutDoc is::" + SCXmlUtil.getString(getOrderRelListOutDoc));

                Element orderReleaseElement = SCXmlUtil.getChildElement(getOrderRelListOutDoc.getDocumentElement(), E_ORDER_RELEASE);

                finalReleaseDocument = prepareReleaseMessageForWMS(orderReleaseElement);

                logger.debug("finalReleaseDocument is::" + SCXmlUtil.getString(finalReleaseDocument));

            }

        } catch (Exception ex) {
            logger.debug("Exception in method prepareOrderReleaseMsg: " + ex.getStackTrace());
        }

        if (logger.isDebugEnabled()) {
            logger.debug("End of method prepareOrderReleaseMsg() with order details:: " + SCXmlUtil.getString(indoc));
        }

        return finalReleaseDocument;
    }

    private Document prepareReleaseMessageForWMS(Element orderReleaseElement) throws ParserConfigurationException {

        logger.debug("orderReleaseElement is::"+SCXmlUtil.getString(orderReleaseElement));

        NodeList orderLineEle = SCXmlUtil.getXpathNodes(orderReleaseElement,E_ORDER_LINE);
        logger.debug("orderLineEle length is:" + orderLineEle.getLength());

        for(int k=0; k<orderLineEle.getLength(); ++k){
            Element eleOrderLine = (Element)orderLineEle.item(k);
            logger.debug("eleOrderLine is:" + SCXmlUtil.getString(eleOrderLine));

            String statusQty = eleOrderLine.getAttribute(A_STATUS_QTY);
            logger.debug("statusQty is:" +statusQty);

            eleOrderLine.setAttribute(A_ORDERED_QTY,statusQty);
            logger.debug("eleOrderLine after update is:" + SCXmlUtil.getString(eleOrderLine));

        }
        logger.debug("orderReleaseElement after updating qty is:"+SCXmlUtil.getString(orderReleaseElement));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document updatedDocument = builder.newDocument();
        Element orderReleaseElementUpdated = (Element) updatedDocument.importNode(orderReleaseElement, true);
        updatedDocument.appendChild(orderReleaseElementUpdated);

        logger.debug("updatedDocument is::"+SCXmlUtil.getString(updatedDocument));

        return updatedDocument;
    }
}
