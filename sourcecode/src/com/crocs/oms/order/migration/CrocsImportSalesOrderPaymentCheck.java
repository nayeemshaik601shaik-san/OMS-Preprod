package com.crocs.oms.order.migration;

import org.w3c.dom.Document;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsTemplateConstants;
import com.crocs.oms.payment.util.PaymentUtils;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class CrocsImportSalesOrderPaymentCheck {
	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsImportSalesOrderPaymentCheck.class.getName());


	/**
	 * <p> Hover over method for more details <p>
	  <p><b>Service Name:</b> </p>
      <p><b>Purpose:</b> This Service will validate if this Order eligible for hold so thst further payment processing can be restricted.
      </p>
      
      <p><b>Input:</b> Output of ImportOrder is input for this service XML</p>
      <pre>{@code
      
      <Order DocumentType="0001" EnterpriseCode="CROCS_US" OrderHeaderKey="20250404091037243361" OrderNo="61866065CUSMIG"/>
      
     * }
	 * @param env - YFSEnvironment
	 * @param importOrderOutDoc - Document
	 * @return - Output of getOrderListOutput Api.
	 * @throws Exception 
	 */
	
	public Document SalesOrderPaymentCheck(YFSEnvironment env, Document importOrderOutDoc) throws Exception {
		logger.beginTimer(CrocsMigarationUtil.logCurrentMethod(this.getClass()));

		try {

			Document getOrderListOutput = CommonUtil.invokeAPI(env,
					CrocsTemplateConstants.TEMPLATE_GET_ORDER_LIST_ORDER_EVENT_UPDATES,
					CrocsConstant.API_GET_ORDER_LIST, importOrderOutDoc);

			YFCDocument golOutYdoc = YFCDocument.getDocumentFor(getOrderListOutput);
			YFCElement golYdocEle = golOutYdoc.getDocumentElement();

			if (!isEligibleforPaymentProcessingHold(golYdocEle)) {

				Document changeOrderOutDoc = PaymentUtils.applyHoldFor(env, CrocsConstant.VAL_PAYMENT_EXCEPTION,
						golYdocEle.getChildElement("Order").getAttribute(CrocsConstant.A_ORDER_HEADER_KEY), CrocsConstant.VAL_APPLY_HOLD);
			}

			logger.verbose("CrocsImportSalesOrderPaymentCheck :: getOrderListOutput :: Output Doc for getOrderLine: \n"
					+ golOutYdoc.toString());

		} catch (YFSException e) {
			throw new YFSException(e.getMessage(), e.getErrorCode(), e.getErrorDescription());
		}

		logger.endTimer(CrocsMigarationUtil.logCurrentMethod(this.getClass()));
		return importOrderOutDoc;
	}


	private boolean isEligibleforPaymentProcessingHold(YFCElement importOrderYdocEle) {

		boolean isvalid = false;
		YFCElement order = importOrderYdocEle.getChildElement("Order");
		YFCElement chargeTransactionDetails = order.getChildElement("ChargeTransactionDetails");

		if (!YFCCommon.isVoid(order.getAttribute("OriginalTotalAmount"))
				&& !YFCCommon.isVoid(chargeTransactionDetails.getAttribute("TotalDebits"))) {

			double totalAmount = Double.valueOf(order.getAttribute("OriginalTotalAmount"));
			double invoicedAmount = Double.valueOf(chargeTransactionDetails.getAttribute("TotalDebits"));

			if (totalAmount == invoicedAmount)
				isvalid = true;
		} else {
			throw new YFSException("Invalid TotalAmount OR InvoicedAmount", "",
					"TotalAmount OR InvoicedAmount is either blank or null in path [OrderList/Order/@OriginalTotalAmount, "
					+ "OrderList/Order/ChargeTransactionDetails/@TotalDebits] ");
		}
		return isvalid;
	}
}
