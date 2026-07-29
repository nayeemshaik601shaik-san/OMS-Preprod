package com.crocs.oms.order;

import java.util.List;
import org.w3c.dom.Document;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.interop.japi.YIFApi;
import com.yantra.interop.japi.YIFClientFactory;
import com.yantra.yfc.core.YFCIterable;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCException;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSExtnTaxBreakup;

public class CrocsGetVertexOrderDetails {

	CrocsGetVertexOrderDetails() {}
	private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsGetVertexOrderDetails.class);

	/**
	 * @param env
	 * @param strOrderHeaderKey
	 * @return Document fetch the GetOrderList From Transaction Object Or Call
	 *         CrocsGetOrderList Service
	 */
	public static Document fetchOrderAndTaxDetails(YFSEnvironment env, String strOrderHeaderKey) {
		Document getOrderDetailsOut = null;
		try {
			getOrderDetailsOut = (Document) env.getTxnObject(CrocsConstant.A_TAX_GET_ORDER_LIST);
			if (getOrderDetailsOut != null)
				return getOrderDetailsOut;

			YIFApi api = YIFClientFactory.getInstance().getApi();
			YFCDocument getOrderDetailsDoc = YFCDocument.createDocument(CrocsXmlConstants.E_ORDER);
			getOrderDetailsDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_ORDER_HEADER_KEY,
					strOrderHeaderKey);

			getOrderDetailsOut = api.executeFlow(env, CrocsConstant.A_CROCS_GET_ORDER_LIST,
					getOrderDetailsDoc.getDocument());

			logger.verbose("CrocsGetVertexOrderDetails : fetchOrderAndTaxDetails : getOrderDetailsOut "
					+ SCXmlUtil.getString(getOrderDetailsOut));

			env.setTxnObject(CrocsConstant.A_TAX_GET_ORDER_LIST, getOrderDetailsOut);

			return getOrderDetailsOut;

		} catch (Exception e) {
			logger.error("CrocsGetVertexOrderDetails : fetchOrderAndTaxDetails : Error fetching order and tax details",
					e);
		}
		return getOrderDetailsOut;
	}
	/**
	 * @param env
	 * @param getOrderList
	 * @return Document
	 * fetch the VertexOuput From Transaction Object Or Call CrocsQuotationRequesttoVertex Service
	 */
	public static Document fetchVetexOuput(YFSEnvironment env, Document getOrderList) {
		Document docVertexOutput = null;
		try {
			YIFApi api = YIFClientFactory.getInstance().getApi();
			docVertexOutput = (Document) env.getTxnObject(CrocsConstant.V_VERTEX_QUOTATION_RESPONSE);
			if (docVertexOutput == null) {
				docVertexOutput = api.executeFlow(env, CrocsConstant.A_CROCS_QUOTATION_REQUEST_TO_VERTEX, getOrderList);

				logger.verbose("CrocsGetVertexOrderDetails : fetchOrderAndTaxDetails : docVertexOutput "
						+ SCXmlUtil.getString(docVertexOutput));

				env.setTxnObject(CrocsConstant.V_VERTEX_QUOTATION_RESPONSE, docVertexOutput);
			}
			return docVertexOutput;
		} catch (Exception e) {
			logger.error("CrocsGetVertexOrderDetails : fetchOrderAndTaxDetails : Error fetching Vertex  Ouput details",
					e);
			throw new YFCException(e,
					"CrocsGetVertexOrderDetails : fetchOrderAndTaxDetails : Error fetching Vertex  Ouput details");
		}

	}
	/**
	 * @param docVertexOutput
	 * @return  YFCIterable<YFCElement>
	 * fetch the VertexOuput From Transaction Object Or Call CrocsQuotationRequesttoVertex Service
	 */
	public static YFCIterable<YFCElement> extractLineTaxItem(Document docVertexOutput) {

		YFCIterable<YFCElement> lineItems = YFCDocument.getDocumentFor(docVertexOutput).getDocumentElement()
				.getChildElement(CrocsConstant.V_SOAP_ENV_BODY).getChildElement(CrocsConstant.V_VERTEX_ENVELOPE)
				.getChildElement(CrocsConstant.V_QUOTATION_RESPONSE).getChildren(CrocsConstant.V_LINE_ITEM);

		logger.verbose("CrocsGetVertexOrderDetails : extractLineTaxItem :  " + lineItems.toString() + ","
				+ lineItems.getTotalCount());

		return lineItems;
	} 
	/**
	 * @param taxLineItem
	 * @return double newTaxPercentage
	 * Compute the new tax percentage from taxLineItem
	 */
	public static double computeNewTaxPercentage(YFCElement taxLineItem) {
		YFCIterable<YFCElement> taxesItr = taxLineItem.getChildren(CrocsConstant.V_TAXES);
		double newTaxPercentage = 0;

		while (taxesItr.hasNext()) {
			YFCElement taxes = taxesItr.next();
			if (CrocsConstant.V_TAXABLE.equals(taxes.getAttribute(CrocsConstant.V_TAX_RESULT))) {
				String taxPercentage = taxes.getChildElement(CrocsConstant.V_EFFECTIVE_RATE).getNodeValue();
				newTaxPercentage += Double.parseDouble(taxPercentage);
			}
		}
		logger.verbose("CrocsGetVertexOrderDetails : computeNewTaxPercentage :  " + newTaxPercentage);
		return newTaxPercentage;
	}

	/**
	 * @param headerTaxInputList
	 * @param taxName
	 * @return String - taxPercentage
	 */
	public static String getCAOldTaxPercentage(List<YFSExtnTaxBreakup> headerTaxInputList, String taxName) {
		String taxPercentage = "0";
		if (headerTaxInputList != null && !headerTaxInputList.isEmpty()) {
			for (YFSExtnTaxBreakup inputTax : headerTaxInputList) {
				if (inputTax.taxName.equalsIgnoreCase(taxName)) {
					taxPercentage = String.valueOf(inputTax.taxPercentage);
				}
			}
		}
		logger.verbose("CrocsGetVertexOrderDetails : getCAOldTaxPercentage :  " + taxPercentage);
		return taxPercentage;

	}
	/**
	 * @param headerTaxInputList
	 * @param taxName
	 * @param strChargeName
	 * @return  double[] { tax, taxPercentage }
	 */
	public static double[] getCAOldTaxPercentageForHeader(List<YFSExtnTaxBreakup> headerTaxInputList, String taxName,
			String strChargeName) {
		double taxPercentage = 0.00;
		double tax = 0.00;
		if (!YFCObject.isVoid(headerTaxInputList) && !headerTaxInputList.isEmpty()) {
			for (YFSExtnTaxBreakup inputTax : headerTaxInputList) {
				if (inputTax.taxName.equalsIgnoreCase(taxName) && inputTax.chargeName.equalsIgnoreCase(strChargeName)) {
					taxPercentage = inputTax.taxPercentage;
					tax = inputTax.tax;
				}
			}
		}
		logger.verbose("CrocsGetVertexOrderDetails : getCAOldTaxPercentageForHeader :  " + taxPercentage + " " + tax);
		return new double[] { tax, taxPercentage };
	}

}
