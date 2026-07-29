package com.crocs.oms.shipment;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsAPIConstants;
import com.crocs.oms.common.util.CrocsTemplateConstants;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;

/**
 * EOMS-1486 - Publish Update to Aptos - Interface Mapping Complete order
 * details needs to be published from OMS to Aptos whenever the first shipment
 * happens on the order. This will help in creating returns from Aptos.
 **/

public class CrocsSendOrderDetailsToAptos implements CrocsXmlConstants{

	private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsSendOrderDetailsToAptos.class);

	public Document crocsSendOrderDetailsToAptos(YFSEnvironment env, Document indoc) throws Exception {

		// Get the OrderHeaderKey from the first ShipmentLine element
		try {
			Element eleRoot = indoc.getDocumentElement();
			Element shipmentLines = (Element) indoc.getElementsByTagName(CrocsXmlConstants.E_SHIPMENT_LINE).item(0);
			String strOrderHeaderKey = shipmentLines.getAttribute(CrocsXmlConstants.A_ORDER_HEADER_KEY);

			// from the input for to call API getShipmentListforOrder
			Document getShipmentListForOrderInput = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER);
			Element eleOrder = getShipmentListForOrderInput.getDocumentElement();
			eleOrder.setAttribute(CrocsXmlConstants.A_ORDER_HEADER_KEY, strOrderHeaderKey);
			eleOrder.setAttribute(CrocsXmlConstants.A_ENTERPRISE_CODE,
					eleRoot.getAttribute(CrocsXmlConstants.A_ENTERPRISE_CODE));
			eleOrder.setAttribute(CrocsXmlConstants.A_DOCUMENT_TYPE, CrocsConstant.VAL_DOCUMENT_TYPE_SALES_ORDER);

			logger.verbose(
					"crocsSendOrderDetailsToAptos :Input the getShipmentListForOrder" + SCXmlUtil.getString(eleOrder));

			Document getShipmentListOrderDoc = CommonUtil.invokeAPI(env, CrocsTemplateConstants.TEMPLATE_GET_SHIPMENT_LIST_FOR_ORDER,
					CrocsAPIConstants.API_GET_SHIPMENT_LIST_FOR_ORDER, getShipmentListForOrderInput);

			logger.verbose("crocsSendOrderDetailsToAptos :Output the getShipmentListForOrder"
					+ SCXmlUtil.getString(getShipmentListOrderDoc));

			NodeList nlShipment = getShipmentListOrderDoc.getElementsByTagName(CrocsXmlConstants.E_SHIPMENT);
			int count = 0;

			// Loop through each <Shipment> element
			for (int i = 0; i < nlShipment.getLength(); i++) {
				Element shipmentElement = (Element) nlShipment.item(i);

				// print shipmentElement
				logger.verbose("crocsSendOrderDetailsToAptos :shipmentElement" + SCXmlUtil.getString(shipmentElement));

				// Get the <Status> child element of the current <Shipment>
				Element statusElement = (Element) shipmentElement.getElementsByTagName(CrocsXmlConstants.E_STATUS)
						.item(0);

				String status = statusElement.getAttribute(CrocsXmlConstants.A_STATUS);
				double dStatus = Double.parseDouble(status);

				// Check if Status = "1400" and StatusName = "Shipment Shipped"
				if (dStatus>=1400.00 && dStatus!=9000.00) {
					count++;
					if (count > 1) {
						break;
					}
				}
			}

			// send Update To Aptos on first Shipment
			if (count == 1) {

				logger.verbose("crocsSendOrderDetailsToAptos :getOrderList"
						+ SCXmlUtil.getString(getShipmentListForOrderInput));

				Document getOrderList = CommonUtil.invokeAPI(env, CrocsTemplateConstants.TEMPLATE_MODIFY_GET_ORDER_LIST_FOR_APTOS,
						CrocsAPIConstants.API_GET_ORDER_LIST, getShipmentListForOrderInput);

				logger.verbose("crocsSendOrderDetailsToAptos :getOrderList" + SCXmlUtil.getString(getOrderList));

				Element eleOrderList = getOrderList.getDocumentElement();

				Element eleAptosOrder = SCXmlUtil.getChildElement(eleOrderList, E_ORDER);
				
				eleAptosOrder.setAttribute(CrocsConstant.A_XMLNS, CrocsConstant.A_W3_URL);

				Document docAptosOutput = SCXmlUtil.createDocument(E_ORDER);
				
				Element eleOrderNew = docAptosOutput.getDocumentElement();				
				
				// Import the element into the target document before replacing
				Element importedEleAptosOrder = (Element) docAptosOutput.importNode(eleAptosOrder, true);

				docAptosOutput.replaceChild(importedEleAptosOrder, eleOrderNew);
				
				String strOrderNo = importedEleAptosOrder.getAttribute(CrocsXmlConstants.A_ORDER_NO);

				logger.verbose("crocsSendOrderDetailsToAptos :docAptosOutput" + SCXmlUtil.getString(docAptosOutput));

				CommonUtil.invokeService(env, CrocsConstant.SER_CROCS_APTOS_ORDER_DETAILS_UPDTAE, docAptosOutput);
				
				logger.info("OMS_UPDTAE :" + "crocsSendOrderDetailsToAptos.crocsSendOrderDetailsToAptos() " +strOrderNo );

			}

		} catch (Exception e) {

			logger.error("CrocsSendOrderDetailsToAptos crocsSendOrderDetailsToAptos: " + e.getMessage());

		}
		return indoc;

	}

}