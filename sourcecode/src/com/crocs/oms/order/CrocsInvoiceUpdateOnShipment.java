package com.crocs.oms.order;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;

/**
 *EOMS-4625 ::  Description: Publishing Invoice to SAP on create Shipment Invoice
 * 
 */
public class CrocsInvoiceUpdateOnShipment {

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsSendInvoiceFeed.class);

	/**
	 * Description: Publishing Invoice details to SAP
	 * 
	 * @param env
	 * @param inDoc
	 * @return
	 * @throws Exception
	 */
	public Document crocsInvoiceUpdate(YFSEnvironment env, Document inDoc) {

		Document docGetOrderInvoiceOutput = null;
		try {

			logger.beginTimer("CrocsInvoiceUpdateOnShipment.crocsInvoiceUpdate");
			logger.debug("crocsInvoiceUpdate Input XML: " + inDoc);
			Element eleOrderInvoice = inDoc.getDocumentElement();

			if (eleOrderInvoice.hasAttribute(CrocsXmlConstants.A_ORDER_INVOICE_KEY)) {
				String strOrderInvoiceKey = eleOrderInvoice.getAttribute(CrocsXmlConstants.A_ORDER_INVOICE_KEY);

				// preparing input for GetOrderInvoiceDetails
				Document getOrderInvoiceDetailsInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_GET_ORDER_INVOICE_DETAILS);
				getOrderInvoiceDetailsInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_INVOICE_KEY, strOrderInvoiceKey);

				docGetOrderInvoiceOutput = CommonUtil.invokeService(env, CrocsConstant.STR_CROCS_GET_ORDER_INVOICE_DETAILS_SERV,
						getOrderInvoiceDetailsInDoc);
			}

		} catch (Exception e) {

			logger.verbose("CrocsInvoiceUpdateOnShipment.crocsInvoiceUpdate :Expection" + e.getMessage());
		}
		return docGetOrderInvoiceOutput;
	}

}
