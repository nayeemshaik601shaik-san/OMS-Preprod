package com.crocs.oms.payment.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.XMLUtil;
import com.ibm.icu.util.Calendar;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class PaymentUtilsPreAuth {
	private static YFCLogCategory logger = (YFCLogCategory) YFCLogCategory.instance(PaymentUtilsPreAuth.class);
    static Document changeorderOutdoc=null;
	public static Document applyHoldFor(YFSEnvironment env,String paymentExceptionType, String orderHeaderKey, String action) {

		/**
		 * Input:- Method receives the required details and decides the types of paymentException is raised 
		 *         and logic applies the required payment hold.
		 *         
		 *         Method is also flexible on performing Applying hold, Rejecting hold , Releasing hold. it 
		 *         just required to pass the right action for operation.
		 *         
		 *         This can be a generic payment util for performing the operation based on action.
		 * 
		 * 
		 * Output:-
		 * <Order Action="MODIFY" DocumentType="" OrderHeaderKey="20250221082625120294"
		 * OrderNo="" > <OrderHoldTypes>
		 * <OrderHoldType HoldType="PAYMENT_REJECT_HOLD" ReasonText="" ResolverUserId=""
		 * Status="1100"/> </OrderHoldTypes> </Order>
		 **/
		
		try {
			
		YFCDocument changeOrderYDoc = YFCDocument.createDocument(CrocsConstant.E_ORDER);
		YFCElement changeOrderEle =  changeOrderYDoc.getDocumentElement();
		changeOrderEle.setAttribute(CrocsConstant.A_ACTION, CrocsConstant.VAL_ACTION_MODIFY);
		changeOrderEle.setAttribute(CrocsConstant.A_ORDER_HEADER_KEY, orderHeaderKey);
		
		YFCElement orderHoldTypes=  changeOrderEle.createChild(CrocsConstant.E_ORDER_HOLD_TYPES);
		YFCElement orderHoldType =orderHoldTypes.createChild(CrocsConstant.E_ORDER_HOLD_TYPE);
					
		switch (paymentExceptionType) {
		case CrocsConstant.VAL_PAYMENT_GATEWAY_REJECTED_RESPONSE:
			
			orderHoldType.setAttribute(CrocsConstant.A_HOLD_TYPE, "PAYMENT_REJECT_HOLD");
			orderHoldType.setAttribute(CrocsConstant.A_REASON_TEXT, "PAYMENT_GATEWAY_REJECTED_RESPONSE");
			orderHoldType.setAttribute(CrocsConstant.A_STATUS, getOrderHoldTypeStatus(action));
			logger.verbose("paymentExceptionType passed : "+paymentExceptionType);
			
			break;
		case CrocsConstant.VAL_PAYMENT_EXCEPTION:

			orderHoldType.setAttribute(CrocsConstant.A_HOLD_TYPE, "PAYMENT_ERROR_HOLD");
			orderHoldType.setAttribute(CrocsConstant.A_REASON_TEXT, "PAYMENT_EXCEPTION_OCCURED");
			orderHoldType.setAttribute(CrocsConstant.A_STATUS, getOrderHoldTypeStatus(action));
			logger.verbose("paymentExceptionType passed : "+paymentExceptionType);

			break;

		default:
			
			String errorDesc ="paymentExceptionType provided: "+paymentExceptionType+" expetced: [gatewayRejected,paymentException]";
			logger.verbose(errorDesc);
			throw new YFSException("Un-Expected paymentExceptionType passed", "", errorDesc);
			//break;
		}
		
		
			try {
				changeorderOutdoc=CommonUtil.invokeAPI(env,"", CrocsConstant.API_CHANGE_ORDER,changeOrderYDoc.getDocument());
				changeorderOutdoc.getDocumentElement().setAttribute("changeOrderHoldOperation","Success");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				throw new YFSException(e.getMessage());
			}
		} catch (YFSException e) {
			// TODO Auto-generated catch block
			throw new YFSException(e.getMessage(), e.getErrorCode(), e.getErrorDescription());
		}
		
		return changeorderOutdoc;
	}

	private static String getOrderHoldTypeStatus(String action) {
		// TODO Auto-generated method stub
		String OrderHoldTypeStatus ="";
		switch (action) {
		case CrocsConstant.VAL_APPLY_HOLD:
			
			logger.verbose("Action passed getOrderHoldTypeStatus: "+action+" Returning: 1100");
			OrderHoldTypeStatus= "1100";
			break;

		case CrocsConstant.VAL_RELEASE_HOLD:
			logger.verbose("Action passed getOrderHoldTypeStatus: "+action+" Returning: 1300");
			OrderHoldTypeStatus= "1300";
			break;

		case CrocsConstant.VAL_REJECT_HOLD:
			logger.verbose("Action passed getOrderHoldTypeStatus: "+action+" Returning: 1200");
			OrderHoldTypeStatus= "1200";
			break;

		default:

			String errorDesc = "Action provided for getOrderHoldTypeStatus: " + action
					+ " expetced: [ApplyHold,ReleaseHold,RejectHold]";
			logger.verbose(errorDesc);
			logger.verbose("Action passed getOrderHoldTypeStatus: "+action+" Returning: "+OrderHoldTypeStatus);
			throw new YFSException("Un-Expected action passed", "", errorDesc);
		// break;
		}
		
		return OrderHoldTypeStatus;
	}
	//EOMS-643 : Delta auth Changes : START
	/**
	 * Description: Below Method helps to stamp AuthExpirationDate based on PaymentType.
	 * All Payment Methods mentioned in the CommonCode. 
	 * 
	 * @param env
	 * @param PaymentType
	 * @param enterpriseCode
	 * @return
	 */
	public static String getAuthorizationExpirationDate(YFSEnvironment env, String paymentType, String enterpriseCode) {
		logger.verbose("PaymentUtils: getAuthorizationExpirationDate :START: ");
		logger.info("PaymentUtils: getAuthorizationExpirationDate: PaymentType:"+ paymentType);
		String authExpirationDate= null;
		try {
			Document docGetCommonCodeListOutput = CommonUtil.getCommonCodeList(env, enterpriseCode, CrocsConstant.STR_CROCS_AUTH_EXP_PAYMENTS, null);			
			Element eleCommonCodeList = XMLUtil.getElementByXPath(docGetCommonCodeListOutput,"/CommonCodeList/CommonCode[@CodeValue='"+paymentType+"']");
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat formatter = new SimpleDateFormat(CrocsConstant.A_OUTPUT_STRUCT_DATE_FORMAT);
			if(eleCommonCodeList!=null) {
				int intShortDescription = Integer.parseInt(eleCommonCodeList.getAttribute(CrocsXmlConstants.A_CODE_SHORT_DESCRIPTION));
				cal.add(5, intShortDescription);
				authExpirationDate = formatter.format(cal.getTime());
			}
		}catch(Exception e) {
			logger.verbose("Execption getAuthorizationExpirationDate API: " + e.getMessage());
			throw new YFSException("Error invoking getAuthorizationExpirationDate : " + e.getMessage());
		}
		logger.info("PaymentUtils: getAuthorizationExpirationDate:"+authExpirationDate);
		logger.verbose("PaymentUtils: getAuthorizationExpirationDate: END:");
		
		return authExpirationDate;
	}
	/**
	 * Description: This method helps to call GetPaymentList to check Authorized Amount.
	 * 
	 * @param env
	 * @param paymentKey
	 * @return
	 */
	public static Document getPaymentList(YFSEnvironment env, String paymentKey) {
		logger.verbose("PaymentUtils : getPaymentList: START");
		logger.info("PaymentUtils: getPaymentList:paymentKey: "+paymentKey);
		Document getGetPaymentListOut = null;
		try {
			if (!YFCObject.isVoid(paymentKey)) {

				Document getPaymentListInDoc = SCXmlUtil.createDocument(CrocsConstant.Payment);
				getPaymentListInDoc.getDocumentElement().setAttribute(CrocsXmlConstants.A_PAYMENT_KEY, paymentKey);
				getGetPaymentListOut = CommonUtil.invokeService(env, CrocsConstant.STR_CROCS_GET_PAYMENT_LIST,
						getPaymentListInDoc);
			}
		} catch (Exception e) {
			throw new YFSException("PaymentUtils : getPaymentList: :Expection" + e.getMessage());
		}
		logger.info("PaymentUtils: getPaymentList: Output: " + XMLUtil.getXMLString(getGetPaymentListOut));
		logger.verbose("PaymentUtils : getPaymentList :: END");
		return getGetPaymentListOut;
	}
	//EOMS-643 : Delta Auth Changes : END
	
}