package com.crocs.oms.order;

import com.crocs.oms.common.util.CrocsConstant;
import com.yantra.yfc.util.YFCException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.crocs.oms.common.util.XMLUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSUserExitException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CrocsDuplicateOrderHoldCriteria implements CrocsConstant {
    private static YFCLogCategory logger = YFCLogCategory.instance(CrocsDuplicateOrderHoldCriteria.class);
   
    /**
     * Method accepts a input document and returns criteria document to invoke getOrderList API
     * @param env
     * @param inXML
     * @return criteriaDoc
     * @throws YFSUserExitException
     */
    public Document invoke(YFSEnvironment env, Document inXML) throws YFSUserExitException {

		String strOrderNo = null;
		try {
			logger.verbose("Input Doc from UE: CrocsDuplicateOrderHoldCriteria: Start " + XMLUtil.getXMLString(inXML));
			Document criteriaDoc = SCXmlUtil.createDocument(E_ORDER);
			Element strCriteriaRoot = criteriaDoc.getDocumentElement();

			Element eleRoot = inXML.getDocumentElement();

			//BillToID
			String strBillToID = eleRoot.getAttribute(A_BILL_TO_ID);
			strCriteriaRoot.setAttribute(A_BILL_TO_ID, strBillToID);
			logger.verbose("strBillToID:" + strBillToID);

			//ShipTo
			Element elepersonInfoShipTo = SCXmlUtil.getChildElement(eleRoot, E_PERSON_INFO_SHIP_TO);
			String strShipToKey = elepersonInfoShipTo.getAttribute(A_PERSON_INFO_KEY);
			strCriteriaRoot.setAttribute(A_SHIP_TO_KEY, strShipToKey);
			logger.verbose("strShipToKey:" + strShipToKey);

			//EnterpriseCode
			String strEnterpriseCode = eleRoot.getAttribute(A_ENTERPRISE_CODE);
			strCriteriaRoot.setAttribute(A_ENTERPRISE_CODE, strEnterpriseCode);
			logger.verbose("strEnterpriseCode:" + strEnterpriseCode);

			//OrderDate
			String strOrderDate = eleRoot.getAttribute(A_ORDER_DATE);
			strOrderNo = eleRoot.getAttribute(A_ORDER_NO);
			if (strOrderDate != null) {

				String strToOrderDate = strOrderDate;
				//Remove date to required format
				String strTrimmedDate = strToOrderDate.substring(0, 19);
				logger.verbose("strTrimmedDate:" + strTrimmedDate);

				LocalDateTime lDate = LocalDateTime.parse(strTrimmedDate);
				//Subtract 5 minutes
				LocalDateTime lfiveMinutesBefore = lDate.minusMinutes(5);
				logger.verbose("lfiveMinutesBefore:" + lfiveMinutesBefore);
				//Prepare formatter that keeps seconds
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern(V_DATE_FORMAT);

				String strFiveMinutesBefore = lfiveMinutesBefore.format(formatter);
				logger.verbose("strFiveMinutesBefore:" + strFiveMinutesBefore);

				//Setting OrderDate criteria
				strCriteriaRoot.setAttribute(A_FROM_ORDER_DATE, strFiveMinutesBefore);
				strCriteriaRoot.setAttribute(A_TO_ORDER_DATE, strTrimmedDate);
				strCriteriaRoot.setAttribute(A_ORDER_DATE_QRY_TYPE, VAL_BETWEEN);
			}

			//TotalAmount
			Element elePriceInfo = SCXmlUtil.getChildElement(eleRoot, E_PRICE_INFO);
			String strTotalAmount = elePriceInfo.getAttribute(A_TOTAL_AMOUNT);
			logger.verbose("strTotalAmount:" + strTotalAmount);
			if (strTotalAmount != null) {
				float fTotalAmount = Float.parseFloat(strTotalAmount);
				float fFromTotalAmount = fTotalAmount - 5;
				float fToTotalAmount = fTotalAmount + 5;
				Element elePriceInfor = SCXmlUtil.createChild(strCriteriaRoot, E_PRICE_INFO);
				elePriceInfor.setAttribute(A_FROM_TOTAL_AMOUNT, String.valueOf(fFromTotalAmount));
				elePriceInfor.setAttribute(A_TO_TOTAL_AMOUNT, String.valueOf(fToTotalAmount));
				elePriceInfor.setAttribute(A_TOTAL_AMOUNT_QRY_TYPE, VAL_BETWEEN);
				logger.verbose("elePriceInfor:" + elePriceInfor);
			}

			logger.verbose("Input to getOrderList with duplicate order criteria: END" + XMLUtil.getXMLString(criteriaDoc));
			return criteriaDoc;
		} catch (Exception e) {
			logger.info("OMS_Update : CrocsDuplicateOrderHoldCriteria : invoke : in catch block : " + strOrderNo);
			logger.verbose(" error in duplicateOrderHoldCriteria" + e.getMessage());
			throw new YFCException("duplicateOrderHoldCriteria" + e.getMessage());
		}
	}
}