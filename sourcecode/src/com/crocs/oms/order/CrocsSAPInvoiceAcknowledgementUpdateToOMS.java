package com.crocs.oms.order;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import java.util.Arrays;

/**
 * EOMS-3695 Will consume message from SAP. From that invoice no will update
 * ExtnAcknowledgeStatus as Y
 */

public class CrocsSAPInvoiceAcknowledgementUpdateToOMS implements CrocsConstant {
	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsSAPInvoiceAcknowledgementUpdateToOMS.class);

	/**
	 *
	 * Description: Check if ExtnAcknowledgeStatus is N then will update
	 * ExtnAcknowledgeStatus as Y for invoice no. Input xml:-
	 * <OrderInvoice InvoiceNo="31"> </OrderInvoice>
	 * 
	 * @param env
	 * @param inDoc
	 * @throws Exception
	 */
	public Document acknowledgementUpdateForInvocie(YFSEnvironment env, Document inDoc) {
		logger.beginTimer("CrocsAcknowledgeStatusUpdateToInvoice.acknowledgeStatusUpdateToInvocie");
		logger.verbose("acknowledgeStatusUpdateToInvocie Input Document : " + XMLUtil.getXMLString(inDoc));
		try {

			String invoiceNo = inDoc.getDocumentElement().getAttribute(A_INVOICE_NO);
			if (!YFCCommon.isVoid(invoiceNo)) {
				Document getOrderInvoiceListInDoc = SCXmlUtil.createDocument(E_ORDER_INVOICE);
				getOrderInvoiceListInDoc.getDocumentElement().setAttribute(A_INVOICE_NO, invoiceNo);

				Document getOrderInvoiceListOutDoc = CommonUtil.invokeAPI(env, TEMPLATE_GET_ORDER_INVOICE_LIST_FOR_SAP,
						API_GET_ORDER_INVOICE_LIST, getOrderInvoiceListInDoc);

				logger.verbose("getOrderInvoiceListOutDoc : " + XMLUtil.getXMLString(getOrderInvoiceListOutDoc));

				Element orderInvoiceEle = SCXmlUtil.getXpathElement(getOrderInvoiceListOutDoc.getDocumentElement(),
						E_ORDER_INVOICE);
				String strOrderInvoiceKey = orderInvoiceEle.getAttribute(A_ORDER_INVOICE_KEY);

				Element orderInvoiceExtnEle = SCXmlUtil.getXpathElement(orderInvoiceEle, E_EXTN);
				String strExtnAcknowledgeStatus = orderInvoiceExtnEle.getAttribute(A_EXTN_IS_ACKNOWLEDGED);

				if (FLAG_N.equals(strExtnAcknowledgeStatus)) {

					/** Updating the flag to Y calling changeOrderInvoice */

					Document changeOrderInvoiceInDoc = SCXmlUtil.createDocument(E_ORDER_INVOICE);
					changeOrderInvoiceInDoc.getDocumentElement().setAttribute(A_ORDER_INVOICE_KEY, strOrderInvoiceKey);
					Element changeOrderInvoiceExtnEle = SCXmlUtil
							.createChild(changeOrderInvoiceInDoc.getDocumentElement(), E_EXTN);
					changeOrderInvoiceExtnEle.setAttribute(A_EXTN_IS_ACKNOWLEDGED, FLAG_Y);

					logger.verbose("changeOrderInvoiceInput : " + XMLUtil.getXMLString(changeOrderInvoiceInDoc));
					Document changeOrderInvoiceOutDoc = CommonUtil.invokeAPI(env, "", API_CHANGE_ORDER_INVOICE,
							changeOrderInvoiceInDoc);

					logger.verbose("changeOrderInvoiceOutput: " + XMLUtil.getXMLString(changeOrderInvoiceOutDoc));
					logger.info(OMS_LOG_INFO
							+ "CrocsSAPInvoiceAcknowledgementUpdateToOMS.acknowledgementUpdateForInvocie() : SAP to OMS Acknowledgement received and updated flag for Invoice No "
							+ invoiceNo);

				}
				logger.endTimer("CrocsSAPInvoiceAcknowledgementUpdateToOMS.acknowledgeStatusUpdateToInvocie");
			}
		} catch (Exception e) {
			logger.error("Error invoking CrocsSAPInvoiceAcknowledgementUpdateToOMS.acknowledgementUpdateForInvocie() "
					+ e.getMessage());
			throw new YFSException(e.getMessage(), e.getCause().toString(), Arrays.toString(e.getStackTrace()));
		}
		return inDoc;

	}
}
