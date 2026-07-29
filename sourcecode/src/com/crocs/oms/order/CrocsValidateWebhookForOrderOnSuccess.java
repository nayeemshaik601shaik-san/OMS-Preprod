package com.crocs.oms.order;

import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsIVAPIConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

/**
 * Description: This below methods helps to process Authorizations 
 * for post Auth Payments when webhook came before order gets created.
 * 
 */
public class CrocsValidateWebhookForOrderOnSuccess implements CrocsConstant{

	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsValidateWebhookForOrderOnSuccess.class);
	
	public void crocsValidateWebhooksForOrderOnSuccess(YFSEnvironment env, Document inDoc) {
		
		logger.beginTimer("crocsValidateWebhooksForOrderOnSuccess.crocsValidateWebhooksForOrderOnSuccess");
		logger.verbose("crocsValidateWebhooksForOrderOnSuccess Input XML: " + SCXmlUtil.getString(inDoc));
		
		try {
			Element orderEle = inDoc.getDocumentElement();
			Element paymentMethodsEle = SCXmlUtil.getChildElement(orderEle, E_PAYMENT_METHODS);
			ArrayList<Element> paymentMethods = SCXmlUtil.getChildren(paymentMethodsEle, E_PAYMENT_METHOD);
			for (Element paymentMethod : paymentMethods) {

				// Get capture Info list
				Document getCaptureInfoList = getAdyenCaptureInfoList(env, paymentMethod);

				if (!YFCCommon.isVoid(getCaptureInfoList) && getCaptureInfoList.getDocumentElement().hasChildNodes()) {

					Element getCaptureInfoListEle = getCaptureInfoList.getDocumentElement();
					Element captureInfoEle = SCXmlUtil.getChildElement(getCaptureInfoListEle, C_CAPTURE_INFO);

					String adyenWebhookKey = captureInfoEle.getAttribute(C_ADYEN_WEBHOOK_KEY);

					logger.verbose("adyenWebhookKey::" + adyenWebhookKey);

					Document docCreateAdyenRequest = createAdyenCaptureInfoList(captureInfoEle);

					/**
					 * If the webhook arrives before the order is created, the POST_AUTH logic will not be executed. 
					 * To handle this, we first remove the existing entry and then re-post it into the queue, 
					 * ensuring that the POST_AUTH logic is executed.
					 *
					 **/
					// deleting record from table.
					deleteAdyenCaptureInfo(env, adyenWebhookKey);

					// creating record in table.
					logger.info("CrocsValidateWebhookForOrderOnSuccess.crocsValidateWebhooksForOrderOnSuccess() :Posting into the queue Adenwehook entry to execute POST_AUTH logic"+XMLUtil.getXMLString(docCreateAdyenRequest));

					CommonUtil.invokeService(env, STR_CROCS_ADYEN_WEBHOOK_SYNC_SERVICE, docCreateAdyenRequest);

				}
			}
			
		}catch(Exception e) {
			logger.verbose("Exception" + e.toString());
			throw new YFSException(e.getMessage());
		}
		
		logger.endTimer("crocsValidateWebhooksForOrderOnSuccess.crocsValidateWebhooksForOrderOnSuccess");
	}
	/**
	 * Description: This method helps to get Capture Info list from table.
	 * 
	 * @param env
	 * @param paymentMethod
	 * @return
	 * @throws YFSException
	 */
	public static Document getAdyenCaptureInfoList(YFSEnvironment env,Element paymentMethod) throws YFSException {
		
		logger.verbose("CrocsProcessAdyenWebhooks : getAdyenCaptureInfoList: START");
		Document getCaptureInfoList=null;
		try {
			
			Document createCaptureIndoc = SCXmlUtil.createDocument(C_CAPTURE_INFO);
			Element createCaptureEle = createCaptureIndoc.getDocumentElement();
			createCaptureEle.setAttribute(C_PSPREFERENCE,paymentMethod.getAttribute(PaymentReference5));
			logger.verbose("getCaptureInforList Indoc" + XMLUtil.getXMLString(createCaptureIndoc));
			 
			getCaptureInfoList = CommonUtil.invokeService(env,CrocsIVAPIConstants.GET_CAPTURE_INFO_LIST_SERVICE,createCaptureIndoc);
			logger.verbose("getCaptureInforList outdoc" + XMLUtil.getXMLString(getCaptureInfoList));
			
		} catch (Exception e) {
			throw new YFSException("CrocsProcessAdyenWebhooks.getAdyenCaptureInfoList :Expection" + e.getMessage());
		}
		logger.verbose("CrocsProcessAdyenWebhooks : getAdyenCaptureInfoList: END");
		return getCaptureInfoList;
	}
	/**
	 * Description: This method helps to delete the record in table.
	 * 
	 * @param env
	 * @param adyenWebhookKey
	 * @throws YFSException
	 */
	public static void deleteAdyenCaptureInfo(YFSEnvironment env,String adyenWebhookKey) throws YFSException {
		
		logger.verbose("CrocsProcessAdyenWebhooks : deleteAdyenCaptureInfo: START");
		try {
			
			Document docDeleteWebhookRequest = SCXmlUtil.createDocument(C_CAPTURE_INFO);
			docDeleteWebhookRequest.getDocumentElement().setAttribute(C_ADYEN_WEBHOOK_KEY, adyenWebhookKey);
			logger.info("CrocsValidateWebhookForOrderOnSuccess.crocsValidateWebhooksForOrderOnSuccess() : deleteing existing Adyenwebhook entry"+XMLUtil.getXMLString(docDeleteWebhookRequest));
			logger.verbose("docDeleteWebhookRequest::"+XMLUtil.getXMLString(docDeleteWebhookRequest));
			
			CommonUtil.invokeService(env, CrocsIVAPIConstants.DELETE_CAPTURE_INFO_SERVICE, docDeleteWebhookRequest);
			
		} catch (Exception e) {
			throw new YFSException("CrocsProcessAdyenWebhooks.deleteAdyenCaptureInfo :Expection" + e.getMessage());
		}
		logger.verbose("CrocsProcessAdyenWebhooks : deleteAdyenCaptureInfo: END");
	}
	/**
	 * Description: Below method helps to create new record in table.
	 * 
	 * @param captureInfoEle
	 * @return
	 * @throws YFSException
	 */
	public static Document createAdyenCaptureInfoList(Element captureInfoEle) throws YFSException {
		
		logger.verbose("CrocsProcessAdyenWebhooks : createAdyenCaptureInfoList: START");
		Document docCreateAdyenRequest=null;
		try {
			
			docCreateAdyenRequest = SCXmlUtil.createDocument(C_CAPTURE_INFO);
			Element eleCreateAdyenRequest = docCreateAdyenRequest.getDocumentElement();
			eleCreateAdyenRequest.setAttribute(C_EVENT_CODE, captureInfoEle.getAttribute(C_EVENT_CODE));
			eleCreateAdyenRequest.setAttribute(C_PSPREFERENCE, captureInfoEle.getAttribute(C_PSPREFERENCE));
			eleCreateAdyenRequest.setAttribute(C_EVENT_DATE, captureInfoEle.getAttribute(C_EVENT_DATE));
			eleCreateAdyenRequest.setAttribute(C_MERCHANT_ACCOUNT_CODE, captureInfoEle.getAttribute(C_MERCHANT_ACCOUNT_CODE));
			eleCreateAdyenRequest.setAttribute(C_ORIGINAL_REFERENCE, captureInfoEle.getAttribute(C_ORIGINAL_REFERENCE));
			eleCreateAdyenRequest.setAttribute(C_REASON, captureInfoEle.getAttribute(C_REASON));
			eleCreateAdyenRequest.setAttribute(C_SUCCESS, captureInfoEle.getAttribute(C_SUCCESS));
			eleCreateAdyenRequest.setAttribute(A_ORDER_NO, captureInfoEle.getAttribute(A_ORDER_NO));
			eleCreateAdyenRequest.setAttribute(A_PAYMENT_METHOD, captureInfoEle.getAttribute(A_PAYMENT_METHOD));
			eleCreateAdyenRequest.setAttribute(A_CURRENCY, captureInfoEle.getAttribute(A_CURRENCY));
			eleCreateAdyenRequest.setAttribute(A_VALUE, captureInfoEle.getAttribute(A_VALUE));
			logger.verbose("docCreateAdyenRequest inDoc" + XMLUtil.getXMLString(docCreateAdyenRequest));
			
		} catch (Exception e) {
			throw new YFSException("CrocsProcessAdyenWebhooks.createAdyenCaptureInfoList :Expection" + e.getMessage());
		}
		logger.verbose("CrocsProcessAdyenWebhooks : createAdyenCaptureInfoList: END");
		return docCreateAdyenRequest;
	}
}
