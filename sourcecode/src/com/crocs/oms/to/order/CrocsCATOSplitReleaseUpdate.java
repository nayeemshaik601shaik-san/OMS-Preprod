package com.crocs.oms.to.order;

import com.crocs.oms.common.util.CommonUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import org.w3c.dom.NodeList;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * This class is created to send unscheduled and qty which is cancelled during
 * release details to WMS for Split Releases
 *
 * 
 * @author IBM
 *
 */
public class CrocsCATOSplitReleaseUpdate implements CrocsConstant {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsCATOSplitReleaseUpdate.class);

	/**
	 * This method does the below 1. checks if there are any cancelled qty or BO
	 * lines 2. get the details of cancelled or backordered lines from the release
	 * msg and send it to WMS
	 *
	 * @param env
	 * @param indoc
	 * @return
	 */

	public Document prepareOrderReleaseMsg(YFSEnvironment env, Document indoc) throws Exception {

		Document getOrderRelListOutDoc = null;
		Document finalReleaseDocument = null;
		try {

			logger.verbose("Input to prepareOrderReleaseMsg: " + SCXmlUtil.getString(indoc));

			Element orderEle = indoc.getDocumentElement();
			Element orderLinesEle = SCXmlUtil.getChildElement(orderEle, E_ORDER_LINES);
			NodeList orderLineEle = SCXmlUtil.getXpathNodes(orderLinesEle, E_ORDER_LINE);
			logger.verbose("orderLineEle length is:" + orderLineEle.getLength());

			// Will hold unique keys and map them to the final WMS message document
			// (optional)
			Set<String> uniqueReleaseKeys = new LinkedHashSet<>();

			String orderReleaseKey = null;

			for (int i = 0; i < orderLineEle.getLength(); i++) {

				Element eleOrderLine = (Element) orderLineEle.item(i);

				if (eleOrderLine == null) {
					logger.warn("Encountered null OrderLine at index: " + i);
					continue;
				}

				logger.verbose("eleOrderLine is::" + SCXmlUtil.getString(eleOrderLine));

				Element orderRelFromStatuses = SCXmlUtil.getChildElement(eleOrderLine, E_FROM_ORDER_REL_STATUSES);
				Element orderRelFromStatus = SCXmlUtil.getChildElement(orderRelFromStatuses, E_FROM_ORDER_REL_STATUS);

				orderReleaseKey = orderRelFromStatus.getAttribute(A_ORDER_RELEASE_KEY);
				logger.verbose("orderReleaseKey is::" + orderReleaseKey);

				// Add to set to ensure uniqueness
				boolean isNew = uniqueReleaseKeys.add(orderReleaseKey);
				if (isNew) {
					logger.verbose("Queued unique orderReleaseKey: " + orderReleaseKey);
				} else {
					logger.verbose("Duplicate orderReleaseKey skipped: " + orderReleaseKey);
				}

			}

			/*  Invoke API once for each unique orderReleaseKey and prepare the WMS
			 message */
			logger.verbose("Unique Release Key Count = " + uniqueReleaseKeys.size());

			/* Build ComplexQuery input for API_GET_ORDER_RELEASE_LIST using
			 OrderReleaseStatusKey(s) */

			Document getOrderRelListInp = SCXmlUtil.createDocument(E_ORDER_RELEASE);
			Element orderReleaseEle = getOrderRelListInp.getDocumentElement();

			/* <ComplexQuery> */
			Element complexQueryEle = getOrderRelListInp.createElement(CrocsXmlConstants.E_COMPLEX_QUERY);
			orderReleaseEle.appendChild(complexQueryEle);

			/* <And> */
			Element andEle = getOrderRelListInp.createElement(CrocsXmlConstants.E_AND);
			complexQueryEle.appendChild(andEle);

			/* <Or> */
			Element orEle = getOrderRelListInp.createElement(CrocsXmlConstants.E_OR);
			andEle.appendChild(orEle);

			/* Build <Exp Name="OrderReleaseStatusKey" QryType="EQ" Value="..."/> for each
			 status key */
			for (String releaseKey : uniqueReleaseKeys) {
				Element expEle = getOrderRelListInp.createElement(CrocsXmlConstants.E_EXP);
				expEle.setAttribute(CrocsXmlConstants.A_NAME, CrocsXmlConstants.A_ORDER_RELEASE_KEY);
				expEle.setAttribute(CrocsXmlConstants.A_QRY_TYPE, CrocsXmlConstants.S_EQUAL);
				expEle.setAttribute(CrocsXmlConstants.S_VALUE, releaseKey);
				orEle.appendChild(expEle);
			}

			logger.verbose("getOrderRelListInp is:" + SCXmlUtil.getString(getOrderRelListInp));

			getOrderRelListOutDoc = CommonUtil.invokeAPI(env, TEMPLATE_GET_ORDER_RELEASE_LIST_ON_CHANGE,
					API_GET_ORDER_RELEASE_LIST, getOrderRelListInp);

			logger.verbose("getOrderRelListOutDoc is::" + SCXmlUtil.getString(getOrderRelListOutDoc));

			/* Iterate the <OrderReleaseList>/<OrderRelease> elements in the response */
			Element orderReleaseListEle = getOrderRelListOutDoc.getDocumentElement();

			NodeList orderReleaseNodes = SCXmlUtil.getXpathNodes(orderReleaseListEle, E_ORDER_RELEASE);
			
			logger.verbose("orderReleaseNodes Length is::" + orderReleaseNodes.getLength());

			for (int i = 0; i < orderReleaseNodes.getLength(); i++) {

				Element orderReleaseElement = (Element) orderReleaseNodes.item(i);

				String strOrderReleaseKey = orderReleaseElement.getAttribute(CrocsXmlConstants.A_ORDER_RELEASE_KEY);
				String strshipNode = orderReleaseElement.getAttribute(CrocsXmlConstants.A_SHIP_NODE);
				logger.verbose("Processing OrderReleaseKey={}"+strOrderReleaseKey +""+strshipNode);
				
				finalReleaseDocument = prepareReleaseMessageForWMS(orderReleaseElement);
				
				logger.verbose("Processing  ShipNode" +""+strshipNode);
				if (strshipNode.equalsIgnoreCase(CrocsConstant.A_OHIO_DC_VALUE)) {

					CommonUtil.invokeService(env, CrocsXmlConstants.CROCS_US_ORDER_UPDATE_SERVICE,
							finalReleaseDocument);

					logger.verbose("routeRelease US : {} invocation completed." + strOrderReleaseKey);

				} else if (strshipNode.equalsIgnoreCase(CrocsConstant.A_UPS_SCS_VALUE)) {

					CommonUtil.invokeService(env, CrocsXmlConstants.CROCS_CA_ORDER_UPDATE_SERVICE,
							finalReleaseDocument);

					logger.verbose("routeRelease CA : {} invocation completed." + strOrderReleaseKey);

				}
				
			}

		} catch (Exception ex) {
			logger.debug("Exception in method prepareOrderReleaseMsg: " + ex.getStackTrace());
		}

		if (logger.isDebugEnabled()) {
			logger.debug("End of method prepareOrderReleaseMsg() with order details:: " + SCXmlUtil.getString(indoc));
		}

		return indoc;
	}

	private Document prepareReleaseMessageForWMS(Element orderReleaseElement) throws ParserConfigurationException {

		logger.verbose("orderReleaseElement is::" + SCXmlUtil.getString(orderReleaseElement));

		NodeList orderLineEle = SCXmlUtil.getXpathNodes(orderReleaseElement, E_ORDER_LINE);
		logger.verbose("orderLineEle length is:" + orderLineEle.getLength());

		for (int k = 0; k < orderLineEle.getLength(); ++k) {
			Element eleOrderLine = (Element) orderLineEle.item(k);
			logger.verbose("eleOrderLine is:" + SCXmlUtil.getString(eleOrderLine));

			String statusQty = eleOrderLine.getAttribute(A_STATUS_QTY);
			logger.verbose("statusQty is:" + statusQty);

			eleOrderLine.setAttribute(A_ORDERED_QTY, statusQty);
			logger.verbose("eleOrderLine after update is:" + SCXmlUtil.getString(eleOrderLine));

		}
		logger.verbose("orderReleaseElement after updating qty is:" + SCXmlUtil.getString(orderReleaseElement));

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document updatedDocument = builder.newDocument();
		Element orderReleaseElementUpdated = (Element) updatedDocument.importNode(orderReleaseElement, true);
		updatedDocument.appendChild(orderReleaseElementUpdated);

		logger.verbose("updatedDocument is::" + SCXmlUtil.getString(updatedDocument));

		return updatedDocument;
	}
}
