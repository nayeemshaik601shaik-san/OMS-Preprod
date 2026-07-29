package com.crocs.oms.util.ue;

import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.ibm.icu.util.Calendar;
import com.crocs.oms.common.util.*;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.core.YFSSystem;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import com.yantra.yfs.japi.YFSExtnPaymentCollectionInputStruct;
import com.yantra.yfs.japi.YFSExtnPaymentCollectionOutputStruct;
import com.yantra.yfs.japi.YFSUserExitException;
import com.yantra.yfs.japi.ue.YFSCollectionCreditCardUE;
import com.sterlingcommerce.baseutil.SCXmlUtil;



public class CrocsCollectionCreditCardWrapper_Backup implements CrocsConstant,YFSCollectionCreditCardUE {
	private static YFCLogCategory logger;

	static {
		
		logger = YFCLogCategory.instance(CrocsCollectionCreditCardWrapper_Backup.class);
	}

	public CrocsCollectionCreditCardWrapper_Backup() throws Exception {
	}

	public YFSExtnPaymentCollectionOutputStruct collectionCreditCard(YFSEnvironment env, YFSExtnPaymentCollectionInputStruct inStruct)
    throws YFSUserExitException
  {
	  logger.beginTimer("CrocsCollectionCreditCardWrapper_Backup.collectionCreditCard() : Begin");
    logger.debug("Starting CrocsCollectionCreditCardWrapper_Backup.collectionCreditCard() method");

	YFSExtnPaymentCollectionOutputStruct outStruct = new YFSExtnPaymentCollectionOutputStruct();

	/*
	 * try { if (YFCCommon.equals(inStruct.chargeType,
	 * CROCSConstant.CHARGETYPE_CHARGE) &&inStruct.requestAmount < 0.00D) {
	 * 
	 * logger.debug("Process Refund");
	 * 
	 * //call refund service .processRefund(env, inStruct, outStruct, null);
	 * 
	 * } else if
	 * (YFCCommon.equals(inStruct.chargeType,CROCSConstant.CHARGETYPE_CHARGE) &&
	 * inStruct.requestAmount >= 0.00D) { logger.debug("Processing  Capture"); //
	 * ProcessCaptureCall.processCapture(env, inStruct, outStruct, null); } }
	 * 
	 * else { YFSException ex = new
	 * YFSException("This function for Adyen is NOT implemented!!!!");
	 * ex.fillInStackTrace(); throw ex; } } catch (Exception e) { if (e instanceof
	 * YFSException) { throw (YFSException) e; }
	 */
	 // return outStruct; 
	 
    Document docCollectionCreditCardInput = null;
    Document docCollectionCreditCardOutput = null; 
    Document docPaymentTransactionError = null;

    Element elePaymentInput = null;
    Element elePaymentOutput = null;
    String message = "";
    String messageType = "";
    docCollectionCreditCardInput = SCXmlUtil.createDocument(CrocsConstant.Payment);
    docPaymentTransactionError = SCXmlUtil.createDocument(CrocsConstant.PaymentTransactionErrorList);
    Element elePaymentTransError = XMLUtil.getRootElement(docPaymentTransactionError);
    elePaymentInput = docCollectionCreditCardInput.getDocumentElement();
    
	if (!(CrocsConstant.AUTHORIZATION.equals(inStruct.chargeType) && inStruct.requestAmount > 0)) {
		elePaymentInput.setAttribute(CrocsConstant.AuthorizationId, String.valueOf(inStruct.authorizationId));	
	}
	
	//Converting inputstruct to xml /payment 
	logger.debug("Creating input from instruct::");
    elePaymentInput.setAttribute(CrocsConstant.BillToAddressLine1, String.valueOf(inStruct.bPreviouslyInvoked));
    elePaymentInput.setAttribute(CrocsConstant.BillToCity, String.valueOf(inStruct.billToCity));
    elePaymentInput.setAttribute(CrocsConstant.BillToCountry, String.valueOf(inStruct.billToCountry));
    elePaymentInput.setAttribute(CrocsConstant.BillToDayPhone, String.valueOf(inStruct.billToDayPhone));
    elePaymentInput.setAttribute(CrocsConstant.BillToEmailId, String.valueOf(inStruct.billToEmailId));
    elePaymentInput.setAttribute(CrocsConstant.BillToFirstName, String.valueOf(inStruct.billToFirstName));
    elePaymentInput.setAttribute(CrocsConstant.BillToId, String.valueOf(inStruct.billToId));
    if (elePaymentInput.getAttribute(CrocsConstant.BillToId).equalsIgnoreCase(CrocsConstant.NULL)) {
      elePaymentInput.setAttribute(CrocsConstant.BillToId, "");
    }
    elePaymentInput.setAttribute(CrocsConstant.BillToKey, String.valueOf(inStruct.billTokey));
    elePaymentInput.setAttribute(CrocsConstant.BillToLastName, String.valueOf(inStruct.billToLastName));
    elePaymentInput.setAttribute(CrocsConstant.BillToState, String.valueOf(inStruct.billToState));
    elePaymentInput.setAttribute(CrocsConstant.BillToZipCode, String.valueOf(inStruct.billToZipCode));
    elePaymentInput.setAttribute(CrocsConstant.bPreviouslyInvoked, String.valueOf(inStruct.bPreviouslyInvoked));
    elePaymentInput.setAttribute(CrocsConstant.ChargeTransactionKey, String.valueOf(inStruct.chargeTransactionKey));
    elePaymentInput.setAttribute(CrocsConstant.ChargeType, String.valueOf(inStruct.chargeType));
    elePaymentInput.setAttribute(CrocsConstant.CreditCardExpirationDate, String.valueOf(inStruct.creditCardExpirationDate));
    elePaymentInput.setAttribute(CrocsConstant.CreditCardName, String.valueOf(inStruct.creditCardName));
    elePaymentInput.setAttribute(CrocsConstant.CreditCardNo, String.valueOf(inStruct.creditCardNo));
    elePaymentInput.setAttribute(CrocsConstant.CreditCardType, String.valueOf(inStruct.creditCardType));
    elePaymentInput.setAttribute(CrocsConstant.Currency, String.valueOf(inStruct.currency));
    elePaymentInput.setAttribute(CrocsConstant.CustomerAccountNo, String.valueOf(inStruct.customerAccountNo));
    if (elePaymentInput.getAttribute(CrocsConstant.CustomerAccountNo).equalsIgnoreCase("null")) {
      elePaymentInput.setAttribute(CrocsConstant.CustomerAccountNo, "");
    }

    elePaymentInput.setAttribute(CrocsConstant.CustomerPONo, String.valueOf(inStruct.customerPONo));
    elePaymentInput.setAttribute(CrocsConstant.DocumentType, String.valueOf(inStruct.documentType));
    elePaymentInput.setAttribute(CrocsConstant.debitCardNo, String.valueOf(inStruct.debitCardNo));
    elePaymentInput.setAttribute(CrocsConstant.EnterpriseCode, String.valueOf(inStruct.enterpriseCode));
    elePaymentInput.setAttribute(CrocsConstant.MerchantId, String.valueOf(inStruct.merchantId));
    elePaymentInput.setAttribute(CrocsConstant.OrderHeaderKey, String.valueOf(inStruct.orderHeaderKey));
    elePaymentInput.setAttribute(CrocsConstant.OrderNo, String.valueOf(inStruct.orderNo));

    elePaymentInput.setAttribute(CrocsConstant.PaymentReference1, String.valueOf(inStruct.paymentReference1));
    if (elePaymentInput.getAttribute(CrocsConstant.PaymentReference1).equalsIgnoreCase("null")) {
      elePaymentInput.setAttribute(CrocsConstant.PaymentReference1, "");
    }

    elePaymentInput.setAttribute(CrocsConstant.PaymentReference2, String.valueOf(inStruct.paymentReference2));
    elePaymentInput.setAttribute(CrocsConstant.PaymentReference3, String.valueOf(inStruct.paymentReference3));
    elePaymentInput.setAttribute(CrocsConstant.PaymentReference4, String.valueOf(inStruct.paymentReference4));
    elePaymentInput.setAttribute(CrocsConstant.PaymentReference5, String.valueOf(inStruct.paymentReference5));
    elePaymentInput.setAttribute(CrocsConstant.PaymentReference6, String.valueOf(inStruct.paymentReference6));
    elePaymentInput.setAttribute(CrocsConstant.PaymentReference7, String.valueOf(inStruct.paymentReference7));
    elePaymentInput.setAttribute(CrocsConstant.PaymentReference8, String.valueOf(inStruct.paymentReference8));
    elePaymentInput.setAttribute(CrocsConstant.PaymentReference9, String.valueOf(inStruct.paymentReference9));
    elePaymentInput.setAttribute(CrocsConstant.bVoidTransaction, String.valueOf(inStruct.bVoidTransaction));
    elePaymentInput.setAttribute(CrocsConstant.cashBackAmount, String.valueOf(inStruct.cashBackAmount));
    elePaymentInput.setAttribute(CrocsConstant.chequeNo, String.valueOf(inStruct.chequeNo));
    elePaymentInput.setAttribute(CrocsConstant.chequeReference, String.valueOf(inStruct.chequeReference));
    elePaymentInput.setAttribute(CrocsConstant.entryType, String.valueOf(inStruct.entryType));
    elePaymentInput.setAttribute(CrocsConstant.PaymentType, String.valueOf(inStruct.paymentType));
    elePaymentInput.setAttribute(CrocsConstant.RequestAmount, String.valueOf(inStruct.requestAmount));
    elePaymentInput.setAttribute(CrocsConstant.ShipToAddressLine1, String.valueOf(inStruct.shipToAddressLine1));
    elePaymentInput.setAttribute(CrocsConstant.ShipToCity, String.valueOf(inStruct.shipToCity));
    elePaymentInput.setAttribute(CrocsConstant.ShipToCountry, String.valueOf(inStruct.shipToCountry));
    elePaymentInput.setAttribute(CrocsConstant.ShipToDayPhone, String.valueOf(inStruct.shipToDayPhone));
    elePaymentInput.setAttribute(CrocsConstant.ShipToEmailId, String.valueOf(inStruct.shipToEmailId));
    elePaymentInput.setAttribute(CrocsConstant.ShipToFirstName, String.valueOf(inStruct.shipToFirstName));
    elePaymentInput.setAttribute(CrocsConstant.ShipToId, String.valueOf(inStruct.shipToId));
    if (elePaymentInput.getAttribute(CrocsConstant.ShipToId).equalsIgnoreCase(CrocsConstant.NULL)) {
      elePaymentInput.setAttribute(CrocsConstant.ShipToId, "");
    }

    elePaymentInput.setAttribute(CrocsConstant.ShipTokey, String.valueOf(inStruct.shipTokey));
    elePaymentInput.setAttribute(CrocsConstant.ShipToLastName, String.valueOf(inStruct.shipToLastName));
    elePaymentInput.setAttribute(CrocsConstant.ShipToState, String.valueOf(inStruct.shipToState));
    elePaymentInput.setAttribute(CrocsConstant.ShipToZipCode, String.valueOf(inStruct.shipToZipCode));
    elePaymentInput.setAttribute(CrocsConstant.SvcNo, String.valueOf(inStruct.svcNo));
    if (elePaymentInput.getAttribute(CrocsConstant.SvcNo).equalsIgnoreCase("null")) {
      elePaymentInput.setAttribute(CrocsConstant.SvcNo, "");
    }

    elePaymentTransError.setAttribute(CrocsConstant.PaymentReference1, String.valueOf(inStruct.paymentReference1));
    if (elePaymentInput.getAttribute(CrocsConstant.PaymentReference1).equalsIgnoreCase(CrocsConstant.NULL)) {
      elePaymentInput.setAttribute(CrocsConstant.PaymentReference1, "");
    }

    elePaymentInput.setAttribute(CrocsConstant.callForAuthorizationStatus, String.valueOf(inStruct.callForAuthorizationStatus));
    elePaymentInput.setAttribute(CrocsConstant.firstName, String.valueOf(inStruct.firstName));
    elePaymentInput.setAttribute(CrocsConstant.middleName, String.valueOf(inStruct.middleName));
    elePaymentInput.setAttribute(CrocsConstant.lastName, String.valueOf(inStruct.lastName));
    elePaymentInput.setAttribute(CrocsConstant.currentAuthorisationAmount, String.valueOf(inStruct.currentAuthorizationAmount));
    elePaymentInput.setAttribute(CrocsConstant.currentAuthorizationCreditCardTransactions, String.valueOf(inStruct.currentAuthorizationCreditCardTransactions));
    elePaymentInput.setAttribute(CrocsConstant.currentAuthorizationExpirationDate, String.valueOf(inStruct.currentAuthorizationExpirationDate));
    elePaymentInput.setAttribute(CrocsConstant.paymentConfigOrganizationCode, String.valueOf(inStruct.paymentConfigOrganizationCode));
    elePaymentInput.setAttribute(CrocsConstant.paymentKey, String.valueOf(inStruct.paymentKey));
    elePaymentInput.setAttribute(CrocsConstant.secureAuthenticationCode, String.valueOf(inStruct.secureAuthenticationCode));
    elePaymentInput.setAttribute(CrocsConstant.voidTransactionStatus, String.valueOf(inStruct.voidTransactionStatus));
    elePaymentTransError.setAttribute(CrocsConstant.PaymentReference2, String.valueOf(inStruct.paymentReference2));
    elePaymentTransError.setAttribute( CrocsConstant.PaymentReference3, String.valueOf(inStruct.paymentReference3));
   
      logger.debug("The input to the ProcessCollectionCreditCard service is: " + XMLUtil.getXMLString(docCollectionCreditCardInput));
      //System.out.println("The input to the ProcessCollectionCreditCard service is:"+ XMLUtil.getXMLString(docCollectionCreditCardInput));

		
		  try {
		  //System.out.println("invoketing ProcessCollectionCreditCard service:: ");
		  logger.debug("invoketing ProcessCollectionCreditCard service:: ");
		  docCollectionCreditCardOutput= CommonUtil.invokeService(env,CrocsConstant.ProcessCollectionCreditCard, docCollectionCreditCardInput); }
		  catch (RemoteException e1)
		  { // TODO Auto-generated catch block
		 // e1.printStackTrace(); 
			  logger.error("Error Message is:"+ e1.getMessage());
		  }
     // System.out.println("ProcessCollectionCreditCard output::"+XMLUtil.getXMLString(docCollectionCreditCardOutput));
      logger.debug("The output of the ProcessCollectionCreditCard service is: " + XMLUtil.getXMLString(docCollectionCreditCardOutput));
 //Setting outStruct from Adyen response
    elePaymentOutput = docCollectionCreditCardOutput.getDocumentElement();
    outStruct.asynchRequestProcess = Boolean.parseBoolean(elePaymentOutput.getAttribute(CrocsConstant.AsynchRequestProcess));
    logger.debug("nAsynchRequestProcess" + elePaymentOutput.getAttribute(CrocsConstant.AsynchRequestProcess));
    //System.out.println("AsynchRequestProcess::" +elePaymentOutput.getAttribute(CROCSConstant.AsynchRequestProcess));
    if (!elePaymentOutput.equals(null))
    {
      outStruct.authAVS = elePaymentOutput.getAttribute(CrocsConstant.AuthAVS);

      outStruct.authCode = elePaymentOutput.getAttribute(CrocsConstant.AuthCode);

      if (!elePaymentOutput.getAttribute(CrocsConstant.AuthorizationAmount).isEmpty())
      {
      outStruct.authorizationAmount = Double.parseDouble(elePaymentOutput.getAttribute(CrocsConstant.AuthorizationAmount));
      }

      //FraudHold check  here  
      String strOrderHeaderKey=elePaymentOutput.getAttribute(CrocsConstant.OrderHeaderKey);
      String strFraudScore=elePaymentOutput.getAttribute(CrocsConstant.FRAUD_SCORE);
      if (strFraudScore != null && !strFraudScore.isEmpty()) {
    	  int fraudScore = Integer.parseInt(strFraudScore);
          if (fraudScore > 80) {
          // Create the output XML document
    	  Document outputDoc = SCXmlUtil.createDocument("Order");
          Element orderElement = outputDoc.getDocumentElement();
          orderElement.setAttribute("Action", "MODIFY");
          orderElement.setAttribute("OrderHeaderKey", strOrderHeaderKey);
          Element orderHoldTypes = SCXmlUtil.createChild(orderElement, CrocsConstant.E_ORDER_HOLD_TYPES);
          Element orderHoldType = SCXmlUtil.createChild(orderHoldTypes, CrocsConstant.E_ORDER_HOLD_TYPE);
          orderHoldType.setAttribute(CrocsConstant.A_HOLD_TYPE, "FRAUD_REVIEW");
          orderHoldType.setAttribute(CrocsConstant.A_REASON_TEXT, "Farud Score is less than 80");
          orderHoldType.setAttribute("ResolverUserId", "");
          orderHoldType.setAttribute(CrocsConstant.A_STATUS, "1100");
          try {
			Document docFraudHoldInputToQueue= CommonUtil.invokeService(env, "CrocsFraudHoldInputToQueue", outputDoc);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			logger.error("Error Message is:"+ e.getMessage());
		}
          // Log the output XML
          }
      }
      //FraudHold check End
      outStruct.authorizationId = elePaymentOutput.getAttribute(CrocsConstant.AuthorizationId);
      outStruct.authReturnCode = elePaymentOutput.getAttribute(CrocsConstant.AuthReturnCode);
      outStruct.authReturnFlag = elePaymentOutput.getAttribute(CrocsConstant.AuthReturnFlag);
      outStruct.authReturnMessage = elePaymentOutput.getAttribute(CrocsConstant.AuthReturnMessage);
      String strAuthmsg=elePaymentOutput.getAttribute(CrocsConstant.AuthReturnMessage);
		// if (strAuthmsg.equalsIgnoreCase(CROCSConstant.RESULT_CODE_AUTHORISED)) {
    	String strAuthorizationExpirationDay = YFSSystem.getProperty(CrocsConstant.ADYEN_AUTH_EXP);
      String format= "yyyyMMddHHmmss";
    		  //CROCSConstant.A_DATE_FORMAT;
      int day = Integer.parseInt(strAuthorizationExpirationDay);
	try {
		String 	strAuthExp = getFutureDate(format,day);
		// System.out.println("Date After conversion:: "+ strAuthExp);
		logger.debug("Date After conversion:: "+ strAuthExp);
		 
		 elePaymentOutput.setAttribute(CrocsConstant.AuthorizationExpirationDate, strAuthExp);       
           logger.debug("Date After conversion:: "+ elePaymentOutput.getAttribute(CrocsConstant.AuthorizationExpirationDate));
	      outStruct.authorizationExpirationDate =elePaymentOutput.getAttribute(CrocsConstant.AuthorizationExpirationDate);
	      
	} catch (Exception e) {
		// TODO Auto-generated catch block
		//e.printStackTrace();
		logger.error("Error Message is:"+ e.getMessage());
	}
     
     // }
      //Useful incase of payment Exceptions
      outStruct.bPreviousInvocationSuccessful = Boolean.parseBoolean(elePaymentOutput.getAttribute(CrocsConstant.BPreviousInvocationSuccessful));
		/*
		 * try { outStruct.executionDate = new Date(); logger.debug("\nToday = " + new
		 * Date()); } catch (Exception e1) { throw e1; }
		 */

      outStruct.HoldAgainstBook = elePaymentOutput.getAttribute(CrocsConstant.HoldAgainstBook);

      outStruct.holdOrderAndRaiseEvent = Boolean.parseBoolean(elePaymentOutput.getAttribute(CrocsConstant.HoldOrderAndRaiseEvent));
    String strHoldAndRaiseEvent=elePaymentOutput.getAttribute(CrocsConstant.HoldOrderAndRaiseEvent);
      outStruct.holdReason = elePaymentOutput.getAttribute(CrocsConstant.HoldReason);
     // outStruct.suspendPayment = elePaymentOutput.getAttribute("SuspendPayment");
      outStruct.requestID = elePaymentOutput.getAttribute(CrocsConstant.RequestID);

      outStruct.retryFlag = elePaymentOutput.getAttribute(CrocsConstant.RetryFlag);

      outStruct.sCVVAuthCode = elePaymentOutput.getAttribute(CrocsConstant.CVVAuthCode);

      outStruct.suspendPayment = elePaymentOutput.getAttribute(CrocsConstant.SuspendPayment);
      Element paymentTransactionErrorList = SCXmlUtil.getChildElement(elePaymentOutput,E_PAYMNT_TRANS_ERROR_LIST);
      Element paymentTransactionError = SCXmlUtil.getChildElement(paymentTransactionErrorList,E_PAYMNT_TRANS_ERROR);

      if(!YFCCommon.isVoid(paymentTransactionError)){
           message = paymentTransactionError.getAttribute(A_Message);
           messageType = paymentTransactionError.getAttribute(A_MSG_TYPE);
          
      }


      if (strHoldAndRaiseEvent != null && !strHoldAndRaiseEvent.isEmpty() && strHoldAndRaiseEvent.equalsIgnoreCase(CrocsConstant.TRUE) ) {
    	  //Create Exception if HoldOrderAndRaiseEvent= true
    	  
    	  Document inboxDoc = SCXmlUtil.createDocument(CrocsConstant.E_INBOX);
          Element inboxElem = inboxDoc.getDocumentElement();
          String strOHK= elePaymentInput.getAttribute(CrocsConstant.OrderHeaderKey);
          String strOrderNumber= elePaymentInput.getAttribute(CrocsConstant.OrderNo);
          inboxElem.setAttribute(CrocsConstant.A_CONSOLIDATE, CrocsConstant.FLAG_Y);
          inboxElem.setAttribute(CrocsConstant.A_ACTIVE_FLAG, CrocsConstant.FLAG_Y);
          inboxElem.setAttribute(CrocsConstant.OrderHeaderKey, strOHK);
          inboxElem.setAttribute(CrocsConstant.OrderNo, strOrderNumber);
          inboxElem.setAttribute(CrocsConstant.A_CONSOLIDATION_WINDOW, CrocsConstant.FOREVER);
          inboxElem.setAttribute(CrocsConstant.A_EXCEPTION_TYPE, CrocsConstant.CROCS_PAYMENT_FAILURE_ALERT);
          inboxElem.setAttribute(A_DESCRIPTION, message);
          inboxElem.setAttribute(A_DETAIL_DESCRIPTION, message);

          // Set the Exception Type
          // Create the ConsolidationTemplate element
          Element consolidationTemplate = inboxDoc.createElement(CrocsConstant.E_CONSOLIDATION_TEMPLATE);
          inboxElem.appendChild(consolidationTemplate);
          // Create the inner Inbox element within the ConsolidationTemplate
          Element innerInboxElem = inboxDoc.createElement(CrocsConstant.E_INBOX);
          innerInboxElem.setAttribute(CrocsConstant.A_ACTIVE_FLAG, CrocsConstant.FLAG_Y);
          innerInboxElem.setAttribute(CrocsConstant.A_EXCEPTION_TYPE,  CrocsConstant.CROCS_PAYMENT_FAILURE_ALERT);  // Set the Exception Type
          innerInboxElem.setAttribute(CrocsConstant.OrderHeaderKey, strOHK);
          innerInboxElem.setAttribute(CrocsConstant.OrderNo, strOrderNumber);
          innerInboxElem.setAttribute(A_DESCRIPTION, message);
          innerInboxElem.setAttribute(A_DETAIL_DESCRIPTION, message);
          consolidationTemplate.appendChild(innerInboxElem);
          
          // Invoke the createException API
          try {
			CommonUtil.invokeAPI(env, "", CrocsAPIConstants.API_CREATE_EXCEPTION, inboxDoc);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			
			logger.error("Error Message is:"+ e.getMessage());
			
			//e.printStackTrace();
		}
         
          }
      
     if (!elePaymentOutput.getAttribute(CrocsConstant.TranAmount).isEmpty())
      {
        outStruct.tranAmount = Double.parseDouble(elePaymentOutput.getAttribute(CrocsConstant.TranAmount));
        logger.debug("Transamoutn" + XMLUtil.getElementXMLString(elePaymentOutput));
      }

      outStruct.tranRequestTime = elePaymentOutput.getAttribute(CrocsConstant.TranRequestTime);

      outStruct.tranReturnCode = elePaymentOutput.getAttribute(CrocsConstant.TranReturnCode);

      outStruct.tranReturnFlag = elePaymentOutput.getAttribute(CrocsConstant.TranReturnFlag);

      outStruct.tranReturnMessage = elePaymentOutput.getAttribute(CrocsConstant.TranReturnMessage);

      outStruct.tranType = elePaymentOutput.getAttribute(CrocsConstant.TranType);
        logger.debug("DocumentCollectionCreditCard::" + XMLUtil.getXMLString(docCollectionCreditCardOutput));

		/*
		 * Element elePaymentTransactionError =
		 * XMLUtil.getElementByXPath(docCollectionCreditCardOutput,
		 * "Payment/PaymentTransactionErrorList");
		 * 
		 * if (elePaymentTransactionError != null) { outStruct.PaymentTransactionError =
		 * XMLUtil.getDocumentFromElement(elePaymentTransactionError); } }
		 */
		/*
		 * outStruct.PaymentReference1
		 * =elePaymentOutput.getAttribute(CROCSConstant.PaymentReference1);
		 * 
		 * outStruct.PaymentReference2
		 * =elePaymentOutput.getAttribute(CROCSConstant.PaymentReference2);
		 * 
		 * outStruct.PaymentReference3
		 * =elePaymentOutput.getAttribute(CROCSConstant.PaymentReference3);
		 */
			/*
			 * outStruct.PaymentReference4 =
			 * elePaymentOutput.getAttribute(CROCSConstant.PaymentReference4);
			 * 
			 * outStruct.PaymentReference5 =
			 * elePaymentOutput.getAttribute(CROCSConstant.PaymentReference5);
			 * outStruct.PaymentReference8 =
			 * elePaymentOutput.getAttribute(CROCSConstant.PaymentReference8);
			 * 
			 * outStruct.PaymentReference9 =
			 * elePaymentOutput.getAttribute(CROCSConstant.PaymentReference9);
			 */
		 
      
      //outStruct.RequiresCallForAuthorization = Boolean.parseBoolean(elePaymentOutput.getAttribute(CROCSConstant.RequiresCallForAuthorization));
      logger.debug("DocumentCollectionCreditCard::" + XMLUtil.getXMLString(docCollectionCreditCardOutput));
    
    }
    return outStruct;
  }
	//return outStruct;

	 public static String addToCurrentDate(String format,int day,int month,int year){
	       
         DateFormat dateFormat = new SimpleDateFormat(format);
  
         Calendar calendarInstance = Calendar.getInstance();
         calendarInstance.add(Calendar.YEAR, year);
         calendarInstance.add(Calendar.MONTH, month);
        // System.out.println("Current date: "+calendarInstance.getTime());
         calendarInstance.add(Calendar.DATE, day);
         return dateFormat.format(calendarInstance.getTime());
 
 }	
	 public static String getFutureDate(String sOutputFormat, int iFutureNoOfDays) throws Exception {
         Calendar cal = Calendar.getInstance();
         cal.add(5, iFutureNoOfDays);
         return formatDate(sOutputFormat, cal.getTime());
       }

      public static String formatDate(String sOutFormat, Date date) {
         SimpleDateFormat formatter = new SimpleDateFormat(sOutFormat);
         return formatter.format(date);
       }
}
