package com.crocs.oms.order;

import java.rmi.RemoteException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsErrorConstants;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.order.util.CrocsOrderCancellationNotesUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;

/**
 * EOMS-1056 Full order cancellation during remorse period from SFCC
 */
public class CrocsOrderUpdate implements CrocsConstant {
	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsOrderUpdate.class);

	/**
	 * this method takes input from SFCC if order have remorse hold, order will be
	 * cancelled.
	 * 
	 * @param env
	 * @param inDoc
	 * @return
	 * @throws Exception
	 */
	public Document crocsOrderUpdate(YFSEnvironment env, Document inDoc) throws Exception {
		logger.beginTimer("CrocsOrderUpdate.crocsOrderUpdate");
		logger.debug("crocsOrderUpdate Input XML: " + inDoc);					
		Element eleOrder = inDoc.getDocumentElement();
		String strOrderNo = eleOrder.getAttribute(CrocsXmlConstants.A_ORDER_NO);
		String strDocumentType = eleOrder.getAttribute(CrocsConstant.DocumentType);
		String strEnterpriseCode = eleOrder.getAttribute(CrocsConstant.A_ENTERPRISE_CODE);
		String strAction = eleOrder.getAttribute(CrocsXmlConstants.A_ACTION);
		Document outDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER);

		outDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_ORDER_NO, strOrderNo);
		Document errorOutDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ERROR);
		if (!YFCCommon.isVoid(strAction) &&  VAL_ACTION_CANCEL.equalsIgnoreCase(strAction)) {
			if (YFCCommon.isVoid(strEnterpriseCode) || YFCCommon.isVoid(strDocumentType)
					|| YFCCommon.isVoid(strOrderNo)) {

				errorOutDoc.getDocumentElement().setAttribute(CrocsXmlConstants.E_ERROR_DESCRIPTION,
						CrocsErrorConstants.VAL_ERROR_DESCRIPTION_YFS10460);
				errorOutDoc.getDocumentElement().setAttribute(CrocsXmlConstants.E_ERROR_CODE,
						CrocsErrorConstants.VAL_ERROR_CODE_YFS10460);
				return errorOutDoc;
			} else {

				// prepare GetOrderList input Xml
				try {
					Document getOrderListInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER);
					getOrderListInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_ORDER_NO, strOrderNo);
					getOrderListInDoc.getDocumentElement().setAttribute(CrocsConstant.DocumentType, strDocumentType);
					getOrderListInDoc.getDocumentElement().setAttribute(CrocsConstant.A_ENTERPRISE_CODE,
							strEnterpriseCode);
					logger.debug("getOrderListInDoc:" + getOrderListInDoc);

					Document getOrderListout = getOrderList(env, getOrderListInDoc);
					logger.debug("getOrderListout:" + getOrderListout);

					Element eleOrderListOut = getOrderListout.getDocumentElement();
					Element eleOrderOut = SCXmlUtil.getXpathElement(eleOrderListOut, XPATH_ORDER_ELEMENT);
					String strOrderHeaderKey = eleOrderOut.getAttribute(CrocsConstant.OrderHeaderKey);
					outDoc.getDocumentElement().setAttribute(CrocsConstant.OrderHeaderKey, strOrderHeaderKey);
					Element eleOrderHoldType = SCXmlUtil.getXpathElement(eleOrderListOut,
							"Order/OrderHoldTypes/OrderHoldType[@HoldType='REMORSE_HOLD' and @Status='1100']");
					if (!YFCCommon.isVoid(eleOrderHoldType)) {
						Document changeOrderInDoc = SCXmlUtil.createDocument(CrocsXmlConstants.E_ORDER);
						Element changeOrderEle = changeOrderInDoc.getDocumentElement();
						changeOrderEle.setAttribute(CrocsConstant.OrderHeaderKey, strOrderHeaderKey);
						changeOrderEle.setAttribute(CrocsXmlConstants.A_ACTION, strAction.toUpperCase());
						changeOrderEle.setAttribute(CrocsXmlConstants.A_OVERRIDE, CrocsXmlConstants.FLAG_Y);
						// EOMS-10220 Start
						CrocsOrderCancellationNotesUtil crocsOrderCancellationNotesUtil = new CrocsOrderCancellationNotesUtil();
						String cancelReason = crocsOrderCancellationNotesUtil.getCancellationReason(env,this.getClass().getSimpleName());	
						String noteText = ORDER_CANCEL_NOTE_TEXT.replace(CrocsConstant.CANCELLED_REASON, cancelReason);
						
						changeOrderInDoc = crocsOrderCancellationNotesUtil.getNotesTag(changeOrderEle,changeOrderInDoc,noteText,cancelReason);
						// EOMS-10220 End

						logger.debug("changeOrderInput:  " + changeOrderInDoc);
						CommonUtil.invokeAPI(env, "", API_CHANGE_ORDER, changeOrderInDoc);

						outDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_Message,
								CrocsConstant.SFCC_CANCELLATION_SUCCESS_MESSAGE);
					} else {
						outDoc.getDocumentElement().setAttribute(CrocsXmlConstants.E_ERROR_DESCRIPTION,
								CrocsErrorConstants.VAL_ERROR_DESCRIPTION_EXTN_001);
						outDoc.getDocumentElement().setAttribute(CrocsXmlConstants.E_ERROR_CODE,
								CrocsErrorConstants.VAL_ERROR_CODE_EXTN_001);

					}
				}

				catch (Exception e) {
					logger.debug("Exception" + e.toString());
					errorOutDoc.getDocumentElement().setAttribute(CrocsXmlConstants.E_ERROR_DESCRIPTION,
							CrocsErrorConstants.VAL_OOB_ERROR);
					errorOutDoc.getDocumentElement().setAttribute(CrocsXmlConstants.E_ERROR_CODE,
							CrocsErrorConstants.VAL_ERROR_CODE_YFS10003);

					return errorOutDoc;
				}

			}
		} else {
			outDoc.getDocumentElement().setAttribute(CrocsXmlConstants.E_ERROR_DESCRIPTION,
					CrocsErrorConstants.VAL_ERROR_DESCRIPTION_EXTN_002);
			outDoc.getDocumentElement().setAttribute(CrocsXmlConstants.E_ERROR_CODE,
					CrocsErrorConstants.VAL_ERROR_CODE_EXTN_002);

		}
		logger.endTimer("CrocsOrderUpdate.crocsOrderUpdate");
		return outDoc;

	}

	/**
	 * This method returns getOrderListOut
	 * 
	 * @param env
	 * @param getOrderListInDoc
	 * @return
	 * @throws RemoteException
	 */
	private Document getOrderList(YFSEnvironment env, Document getOrderListInDoc) throws RemoteException {
		logger.beginTimer("CrocsOrderUpdate.getOrderList");
		Document getOrderListTemplateDoc = getOrderListTemplate();
		Document getOrderListOut = CommonUtil.invokeAPI(env, getOrderListTemplateDoc, API_GET_ORDER_LIST,
				getOrderListInDoc);
		logger.debug("getOrderListOut" + getOrderListOut.toString());
		logger.endTimer("CrocsOrderUpdate.getOrderList");
		return getOrderListOut;
	}

	/**
	 * This method returns getOrderListAPI Template
	 * 
	 * @return
	 */
	private Document getOrderListTemplate() {
		logger.beginTimer("CrocsOrderUpdate.getOrderListTemplate");

		String strTemplateGetOrderList = CrocsConstant.GET_ORDERLIST_TEMPLATE;
		Document getOrdrListTemplate = SCXmlUtil.createFromString(strTemplateGetOrderList);
		logger.endTimer("CrocsOrderUpdate.getOrderListTemplate");
		return getOrdrListTemplate;
	}
}
