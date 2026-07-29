package com.crocs.oms.order;

import java.util.List;
import java.util.Properties;

import com.yantra.yfs.japi.YFSException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsAPIConstants;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.sterlingcommerce.tools.datavalidator.XmlUtils;
import com.yantra.interop.japi.YIFCustomApi;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCConfigurator;
import com.yantra.yfs.japi.YFSEnvironment;

public class CrocsSendInvoiceFeed implements YIFCustomApi  {
	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsSendInvoiceFeed.class);

	/**
	 * this method takes input from SFCC if order have remorse hold, order will be
	 * cancelled.
	 * 
	 * @param env
	 * @param inDoc
	 * @return
	 * @throws Exception
	 */
	public Document crocsInvoiceUpdate(YFSEnvironment env, Document inDoc) throws Exception {
		logger.beginTimer("CrocsSendInvoiceFeed.crocsInvoiceUpdate");
		logger.debug("crocsInvoiceUpdate Input XML: " + inDoc);
		Element eleInvoice = inDoc.getDocumentElement();
		String strCarrierNo = SCXmlUtil.getXpathAttribute(eleInvoice, "//LineDetails/LineDetail[1]/OrderLine/@CarrierServiceCode");
		String strDocumentType =  SCXmlUtil.getXpathAttribute(eleInvoice, "//InvoiceHeader/@DocumentType"); 
		String strEnterpriseCode = SCXmlUtil.getXpathAttribute(eleInvoice, "//InvoiceHeader/@EnterpriseCode"); 
		String strInvoiceType = SCXmlUtil.getXpathAttribute(eleInvoice, "//InvoiceHeader/@InvoiceType"); 
		String strOrderHeaderKey = SCXmlUtil.getXpathAttribute(eleInvoice, "//InvoiceHeader/Order/@OrderHeaderKey"); 
		String strOrderType = SCXmlUtil.getXpathAttribute(eleInvoice, "//InvoiceHeader/Order/@OrderType"); 
		String strEnteredBy = SCXmlUtil.getXpathAttribute(eleInvoice, "//InvoiceHeader/Order/@EnteredBy"); 
		Element eInvoiceHeader = SCXmlUtil.getChildElement(eleInvoice, "InvoiceHeader");
		String strOrderPurpose = SCXmlUtil.getXpathAttribute(eleInvoice, "//InvoiceHeader/Order/@OrderPurpose");
		String strEntryType =  SCXmlUtil.getXpathAttribute(eleInvoice, "//InvoiceHeader/Order/@EntryType");
		String strExtReference1 = SCXmlUtil.getXpathAttribute(eleInvoice,"//InvoiceHeader/Shipment/Containers/Container/@ExternalReference1");
		String strOrderNo  = SCXmlUtil.getXpathAttribute(eleInvoice,"//InvoiceHeader/@OrderNo");


		if (!YFCCommon.isVoid(strDocumentType)  &&  !YFCCommon.isVoid(strEnterpriseCode)) {
				try {

					/**  Changes Starts - EOMS-14166
					 * The SAP carrier codes are fetched from CROCS_WMS_SHIP_VIA, based on the ExternalReference1 value on the Shipment Container level
					 * for the Shipment Invoice Type of the Sales Order
					 *
					 */
					
					if (strDocumentType.equals(CrocsConstant.VAL_DOCUMENT_TYPE_SALES_ORDER) && strInvoiceType.equals("SHIPMENT") ) {
						if (!YFCCommon.isVoid(strExtReference1)) {

							Document getCommonCodeListInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_COMMON_CODE);
							getCommonCodeListInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_CODE_TYPE, "CROCS_WMS_SHIP_VIA");
							getCommonCodeListInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_CODE_VALUE, strExtReference1);
							getCommonCodeListInDoc.getDocumentElement().setAttribute(CrocsConstant.A_ORGANIZATION_CODE, strEnterpriseCode);
							logger.debug("getCommonCodeListInDoc:" + getCommonCodeListInDoc);
							Document getCommonCodeListOut = CommonUtil.invokeAPI(env, "", CrocsAPIConstants.API_GET_COMMON_CODE_LIST, getCommonCodeListInDoc);
							logger.info("getCommonCodeListOutDoc for Order No:"+strOrderNo+" " + getCommonCodeListOut);
							

							if (!YFCCommon.isVoid(getCommonCodeListOut)){
								// Short Description will read the value of the Carrier
							String 	sAPCarrierCode = SCXmlUtil.getXpathAttribute(getCommonCodeListOut.getDocumentElement(), "//CommonCodeList/CommonCode[1]/@CodeShortDescription");
								if (!YFCCommon.isVoid(sAPCarrierCode)) {
									eInvoiceHeader.setAttribute(CrocsXmlConstants.A_CARRIER_SERVICE_CODE, sAPCarrierCode);
								}
							}else{

								String  errorCode ="INVALID VALUE FOUND";
								String  errorDescription ="OrderNo: "+ strOrderNo +  "NO value available in CommonCode CROCS_WMS_SHIP_VIA for the CodeValue : " +
															strExtReference1 +" " + "calling organization" + strEnterpriseCode + "CommonCodeInput::" + SCXmlUtil.getString(getCommonCodeListInDoc)  ;
								String  errorMessage ="ExternalReference1 is null/Empty in expected path at Container level";
								logger.info("errorCode: "+errorCode+"\nerrorDescription: "+errorDescription+"\nerrorMessage: "+errorMessage);
							}
						}
						else {
							   /**
							    * 
							    * If value is not in ExternalReference1 at the Shipment Container level
								* and Common Code CROCS_WMS_SHIP_VIA doesn't give any value, logging the details and calling CROCS_SFCC_CARRIER instead
								* which stores the value based on the CarrierServiceCode value on orderline level
								*All enterprise should have values for ExternalReference1 is being taken care as part of -- EOMS-14497
							    */
								
								Document getCommonCodeListInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_COMMON_CODE);
								getCommonCodeListInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_CODE_TYPE, "CROCS_SFCC_CARRIER");
								getCommonCodeListInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_CODE_SHORT_DESCRIPTION, strCarrierNo);
								/* EOMS-11449 Added the attribute Organization Code instead of EnterpriseCode to get the correct SFCC Carrier */
								getCommonCodeListInDoc.getDocumentElement().setAttribute(CrocsConstant.A_ORGANIZATION_CODE,strEnterpriseCode);
								/* EOMS-11449 Added the attribute Organization Code instead of EnterpriseCode to get the correct SFCC Carrier */
								logger.debug("getCommonCodeListInDoc:" + getCommonCodeListInDoc);

								Document getCommonCodeListOut = CommonUtil.invokeAPI(env, "", CrocsAPIConstants.API_GET_COMMON_CODE_LIST, getCommonCodeListInDoc);
							
								if (!YFCCommon.isVoid(getCommonCodeListOut)){
								// Long Description will be same for Specific Carrier
								String sSAPCarrierCode = SCXmlUtil.getXpathAttribute(getCommonCodeListOut.getDocumentElement(), "//CommonCodeList/CommonCode[1]/@CodeLongDescription");
								if (!YFCCommon.isVoid(sSAPCarrierCode)) {
									eInvoiceHeader.setAttribute(CrocsXmlConstants.A_CARRIER_SERVICE_CODE, sSAPCarrierCode);
								}else {
									String  errorCode ="INVALID VALUE FOUND";
									String  errorDescription ="OrderNo: "+ strOrderNo +  "NO value available in CommonCode CROCS_SFCC_CARRIER for the Value : " +
																strCarrierNo + "CommonCodeInput::" + SCXmlUtil.getString(getCommonCodeListInDoc)  ;
									String  errorMessage ="No expected Value found for the SCAC";
									logger.info("errorCode: "+errorCode+"\nerrorDescription: "+errorDescription+"\nerrorMessage: "+errorMessage);
								}
							}
								
								String path = "//InvoiceHeader/Shipment/Containers/Container/@ExternalReference1";
								String  errorCode ="INVALID OR NO VALUE FOUND in" + path;
								String  errorDescription ="SAP invoice for orderNo"+ " : "+  strOrderNo + "    " +
															"No value found to call CROCS_WMS_SHIP_VIA, calling CROCS_SFCC_CARRIER instead to fetch the value for"+
															strCarrierNo + "  " + "CommonCodeInput::"+ SCXmlUtil.getString(getCommonCodeListInDoc)  ;
								String  errorMessage ="ExternalReference1 is null/Empty in expected xpath"  ;
								logger.info("errorCode: "+errorCode+"\nerrorDescription: "+errorDescription+"\nerrorMessage: "+errorMessage);
						}
					}
					/** Changes End - EOMS-14166		* */

					if (!YFCCommon.isVoid(strOrderType) && (CrocsConstant.MP_ORDER_TYPE).equals(strOrderType) && !YFCCommon.isVoid(strEnteredBy)) {
						String strSAPProfitCenter =  YFCConfigurator.getInstance().getProperty(strEnteredBy+"_"+CrocsConstant.SAP_PROFIT_CENTER);
						if (!YFCCommon.isVoid(strSAPProfitCenter)) {
							eInvoiceHeader.setAttribute(CrocsXmlConstants.A_SAP_PROFIT_CENTER, strSAPProfitCenter);
						}else {
							eInvoiceHeader.setAttribute(CrocsXmlConstants.A_SAP_PROFIT_CENTER, "");
						}
					}
					if (!YFCCommon.isVoid(strOrderHeaderKey)) {
						Document getOrderInvoiceListInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER_INVOICE);
						getOrderInvoiceListInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_ORDER_HEADER_KEY, strOrderHeaderKey);
						getOrderInvoiceListInDoc.getDocumentElement().setAttribute("LatestFirst", "N");
						logger.debug("getOrderInvoiceListInDoc:" + getOrderInvoiceListInDoc);
	
						Document getOrderInvoiceListOut = CommonUtil.invokeAPI(env, "", CrocsAPIConstants.API_GET_ORDER_INVOICE_LIST, getOrderInvoiceListInDoc);
						if (null!=getOrderInvoiceListOut){
							XmlUtils.importElement(eleInvoice, getOrderInvoiceListOut.getDocumentElement());
						}
					}
					//EOMS-4438 & EOMS-13714 : SAP Invoice posting for Promotional order : START
					if(CrocsConstant.A_SALES_ORDER_DOCUMENT_TYPE.equalsIgnoreCase(strDocumentType) 
							&& CrocsConstant.ENTRY_TYPE_CALL_CENTER.equalsIgnoreCase(strEntryType)
							&& !CrocsXmlConstants.A_EVENT_CODE_REFUND.equals(strOrderPurpose)
			    			&& !CrocsConstant.VAL_ORDER_PURPOSE.equals(strOrderPurpose)) {
						
						eInvoiceHeader.setAttribute(CrocsConstant.A_CHANNEL, CrocsConstant.STR_PO_CHANNEL);
						
						Element eorderEle = SCXmlUtil.getChildElement(eInvoiceHeader, CrocsXmlConstants.E_ORDER);
						Element eOrderExtn = SCXmlUtil.getChildElement(eorderEle, CrocsXmlConstants.E_EXTN);
						
						Document getCommonCodeListInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_COMMON_CODE);
						getCommonCodeListInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_CODE_TYPE, CrocsConstant.STR_CROCS_ORDER_REASONS);
						getCommonCodeListInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_CODE_VALUE,eOrderExtn.getAttribute(CrocsConstant.A_EXTN_REASON_CODE));
						getCommonCodeListInDoc.getDocumentElement().setAttribute(CrocsConstant.A_ENTERPRISE_CODE,CrocsConstant.STR_CROCS);
						logger.debug("getCommonCodeListInDoc:" + getCommonCodeListInDoc);
	
						Document getCommonCodeListOut = CommonUtil.invokeAPI(env, "", CrocsAPIConstants.API_GET_COMMON_CODE_LIST, getCommonCodeListInDoc);
						if (!YFCCommon.isVoid(getCommonCodeListOut)){
							String strPOReasonCode = SCXmlUtil.getXpathAttribute(getCommonCodeListOut.getDocumentElement(), "//CommonCodeList/CommonCode[1]/@CodeShortDescription");
							if (!YFCCommon.isVoid(strPOReasonCode)) {
								eOrderExtn.setAttribute(CrocsConstant.A_EXTN_REASON_CODE, strPOReasonCode);
							}
						}
					}
					//EOMS-4438 & EOMS-13714 : SAP Invoice posting for Promotional order : END

				}
				catch (Exception e) {
					logger.debug("Error in getCommonCodeList API call in method CrocsSendInvoiceFeed.crocsInvoiceUpdate: "
							+ e.getLocalizedMessage());
				}

			}
		logger.endTimer("CrocsSendInvoiceFeed.crocsInvoiceUpdate");
		return inDoc;

	}

	/** preparing input for getOrderList to get the original salesOrderno by passing OrderHeaderKey
	 * @param env env
	 * @param transferFromOhKey SalesOrder OrderHeaderKey
	 * @return getOrderListOutput
	 * @throws Exception
	 */
	private static Document getGetOrderListOut(YFSEnvironment env, String salesOrderHeaderKey) throws YFSException {
		Document getOrderListOut = null;
		try {
			Document getOrderListInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER);
			getOrderListInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_ORDER_HEADER_KEY, salesOrderHeaderKey);
			logger.info("CrocsSendInvoiceFeed.getGetOrderListout: Input:" + getOrderListInDoc);
			getOrderListOut = CommonUtil.invokeService(env, CrocsConstant.CROCS_GET_ORDER_LIST_FOR_FORTER,
					getOrderListInDoc);
			logger.info("CrocsSendInvoiceFeed.getGetOrderListout: Output :" + getOrderListOut);

		}
		catch (Exception e) {
			logger.verbose("Error in getOrderList API call in method CrocsSendInvoiceFeed.getGetOrderListOut: "
					+ e.getLocalizedMessage() + e.getMessage());
			logger.info("Error in getOrderList API call in method CrocsSendInvoiceFeed.getGetOrderListOut: "
					+ e.getLocalizedMessage() + e.getMessage());
			throw new YFSException("Error getOrderList API call in method CrocsSendInvoiceFeed.getGetOrderListOut: " + e.getMessage());
		}
		return  getOrderListOut;
	}

	@Override
	public void setProperties(Properties arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
