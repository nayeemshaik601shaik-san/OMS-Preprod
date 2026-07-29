package com.crocs.oms.util.ue;

import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.XMLUtil;
import com.ibm.icu.util.Calendar;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSUserExitException;
import com.yantra.yfs.japi.ue.YFSBeforeCreateOrderUE;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Objects;

public class BeforeCreateSOUEImpl implements CrocsXmlConstants, YFSBeforeCreateOrderUE {
    private static final YFCLogCategory logger = YFCLogCategory.instance(BeforeCreateSOUEImpl.class);

    @Override
    public String beforeCreateOrder(YFSEnvironment yfsEnvironment, String s) throws YFSUserExitException {
        return null;
    }

    /** this will be invoked when create order API will be called
     * changes for EOMS-608 :CancelOrderNotification : updating the charges in Reference
     * @param yfsEnvironment
     * @param document
     * @return document
     * @throws YFSUserExitException
     */
    @Override
    public Document beforeCreateOrder(YFSEnvironment yfsEnvironment, Document document) throws YFSUserExitException {
        logger.debug("Input Document : " + XMLUtil.getXMLString(document));
        Element orderEle = document.getDocumentElement();
        Element orderLines = SCXmlUtil.getChildElement(orderEle, E_ORDER_LINES);
        ArrayList<Element> orderLineList = SCXmlUtil.getChildren(orderLines, E_ORDER_LINE);
        
        //updating the AuthorizationExpirtion Date for After Pay and Cash App
        updateAuthExpirationDate(orderEle);
        
        //Process OrderLine
        for (Element orderLine : orderLineList) {
            logger.debug("OrderLine Element is: " + SCXmlUtil.getString(orderLine));
            // Process Line Charges
            extractLineCharges(orderLine);
            // Process Line Tax
            extractLineTax(orderLine);
        }
        //processHeaderCharges
        extractHeaderCharges(orderEle);
        //process header Tax
        extractHeaderTax(orderEle);
        return document;

    }

    /** this sets the tax value in tax reference
     * @param orderEle header Taxes
     */
    private static void extractHeaderTax(Element orderEle) {
        Element orderHeaderTax = SCXmlUtil.getChildElement(orderEle, E_HEADER_TAXES);
        ArrayList<Element> headerTaxes = SCXmlUtil.getChildren(orderHeaderTax, E_HEADER_TAX);
        for (Element orderHeaderTaxes : headerTaxes) {
            logger.debug("HeaderTax Element is: " + SCXmlUtil.getString(orderHeaderTaxes));
            if (!orderHeaderTaxes.getAttribute(A_TAX).isEmpty()) {
                orderHeaderTaxes.setAttribute(A_TAX_REFERENCE, orderHeaderTaxes.getAttribute(A_TAX));
            }
        }
    }

    /** this sets the shipping value in shipping reference
     * @param orderEle  headerCharges
     */
    private static void extractHeaderCharges(Element orderEle) {
        Element headerChargesEle = SCXmlUtil.getChildElement(orderEle, E_HEADER_CHARGES);
        ArrayList<Element> headerCharges = SCXmlUtil.getChildren(headerChargesEle, E_HEADER_CHARGE);
        for (Element orderHeaderCharge : headerCharges) {
            logger.debug("HeaderCharge Element is: " + SCXmlUtil.getString(orderHeaderCharge));
            if (!orderHeaderCharge.getAttribute(A_CHARGE_AMOUNT).isEmpty() && Objects.equals(orderHeaderCharge.getAttribute(A_CHARGE_CATEGORY), A_SHIPPING_CHARGE)) {
                orderHeaderCharge.setAttribute(A_SHIPPING_REFERENCE, orderHeaderCharge.getAttribute(A_CHARGE_AMOUNT));
            }
        }
    }

    /** this sets the line tax value in tax reference
     * @param orderLine linetaxes
     */
    private static void extractLineTax(Element orderLine) {
        Element orderLineTax = SCXmlUtil.getChildElement(orderLine, E_LINE_TAXES);
        ArrayList<Element> lineTaxes = SCXmlUtil.getChildren(orderLineTax, E_LINE_TAX);
        for (Element tax : lineTaxes) {
            logger.debug("LineTax Element is: " + SCXmlUtil.getString(tax));
            if (!tax.getAttribute(A_TAX).isEmpty()) {
                tax.setAttribute(A_TAX_REFERENCE, tax.getAttribute(A_TAX));
            }
        }
    }

    /** this sets the discount  value in discount reference
     * @param orderLine LineCharges
     */
    private static void extractLineCharges(Element orderLine) {
        Element lineChargesEle = SCXmlUtil.getChildElement(orderLine, E_LINE_CHARGES);
        ArrayList<Element> lineCharges = SCXmlUtil.getChildren(lineChargesEle, E_LINE_CHARGE);
        for (Element lineCharge : lineCharges) {
            logger.debug("LineCharge Element is: " + SCXmlUtil.getString(lineCharge));
            String chargeCategory = lineCharge.getAttribute(A_CHARGE_CATEGORY);
            if (!lineCharge.getAttribute(A_CHARGE_PER_UNIT).isEmpty()&& Objects.equals(chargeCategory, A_DISCOUNT)) {
                lineCharge.setAttribute(A_DISCOUNT_REFERENCE, lineCharge.getAttribute(A_CHARGE_PER_UNIT));
            }
        }
    }
    
    /**
     * this sets the AuthExpirtaionDate to 30 days 
     * 
     * @param orderEle
     * 
     */
    private static void updateAuthExpirationDate(Element orderEle) {
        Element paymentMethodsEle = SCXmlUtil.getChildElement(orderEle, E_PAYMENT_METHODS);
        ArrayList<Element> paymentMethods = SCXmlUtil.getChildren(paymentMethodsEle, E_PAYMENT_METHOD);
        for (Element paymentMethod : paymentMethods) {
            String paymentType = paymentMethod.getAttribute(A_PAYMENT_TYPE);
            if (paymentType.equalsIgnoreCase(CrocsConstant.STR_CASH_APP) 
            		|| paymentType.equalsIgnoreCase(CrocsConstant.STR_AFTER_PAY)
            		|| paymentType.equalsIgnoreCase(CrocsConstant.STR_GIVEX)) {
            	Element paymentMethodDetails = SCXmlUtil.getChildElement(paymentMethod, E_PAYMENT_DETAILS);
            	 Calendar cal = Calendar.getInstance();
                 cal.add(5, 30);
                 SimpleDateFormat formatter = new SimpleDateFormat(CrocsConstant.STR_DB_DATE_FORMAT);
                 String AuthExpiryDate = formatter.format(cal.getTime());
                 if(YFCCommon.isVoid(paymentMethodDetails.getAttribute(A_AUTH_EXPIRATION_DATE)) 
                		 || paymentMethodDetails.getAttribute(A_AUTH_EXPIRATION_DATE)==null) {
                	 paymentMethodDetails.setAttribute(A_AUTH_EXPIRATION_DATE, AuthExpiryDate);
                 }
            	
            }
        }
    }
}

