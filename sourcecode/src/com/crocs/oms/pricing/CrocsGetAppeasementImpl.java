package com.crocs.oms.pricing;

import com.crocs.oms.common.util.CrocsConstant;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.pca.ycd.demo.YCDGetAppeasementOffersUEImpl;
import com.yantra.pca.ycd.japi.ue.YCDGetAppeasementOffersUE;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCDoubleUtils;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSUserExitException;
import java.io.IOException;

import java.util.Iterator;
import java.util.Properties;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class CrocsGetAppeasementImpl implements CrocsConstant,YCDGetAppeasementOffersUE {

    private Properties props;
    private static YFCLogCategory logger = YFCLogCategory.instance(CrocsGetAppeasementImpl.class.getName());
    private String preferredType = "VARIABLE_AMOUNT_ORDER";
    private boolean isPreferredTypeSet = false;


    public Document getAppeasementOffers(YFSEnvironment env, Document inDoc) throws YFSUserExitException {
        YFCDocument dIn = YFCDocument.getDocumentFor(inDoc);
        Element input = inDoc.getDocumentElement();
        logger.debug("input is:" +SCXmlUtil.getString(input));

        if (dIn == null) {
            return null;
        } else {
            YFCDocument dOut = YFCDocument.createDocument(E_APPEASEMENT_OFFERS);
            YFCElement eOut = dOut.getDocumentElement();
            YFCElement eIn = dIn.getDocumentElement();
            YFCElement order = eIn.getChildElement(E_ORDER);
            this.setAppeasementOffersForOrder(eOut, eIn);

            return dOut.getDocument();
        }
    }

    private YFCElement setAppeasementOffersForOrder(YFCElement eOut, YFCElement eIn) {
        YFCElement eOrder = eIn.getChildElement(E_ORDER);
        YFCElement eSelectedReason = eOrder.getChildElement(E_APPEASEMENT_REASON);
        String sAppeasementReasonCode = eSelectedReason.getAttribute(A_REASON_CODE);
        YFCElement eOrderIn = eIn.getChildElement(E_ORDER);
        String sOrderHeaderKey = eOrderIn.getAttribute(A_ORDER_HEADER_KEY);
        this.setChargeCataegoryAndNameForAppeasementOffers(eOut);
        eOut.setAttribute(A_ORDER_HEADER_KEY, sOrderHeaderKey);
        eOut.setAttribute(A_REASON_CODE, sAppeasementReasonCode);
        YFCElement eAppeasementOffers = this.getAppeasementOffersList(eIn);

        if (eAppeasementOffers != null) {
            Iterator iteratorAppeasementOffers = eAppeasementOffers.getChildren();
            if (iteratorAppeasementOffers != null) {
                while(iteratorAppeasementOffers.hasNext()) {
                    YFCElement eAppeasementOffer = (YFCElement)iteratorAppeasementOffers.next();
                    eOut = this.setAppeasementOffer(eOrderIn, eOut, eAppeasementOffer);
                }
            }
        }

        eOut = this.setPreferredOffer(eOut);
        return eOut;
    }

    private YFCElement setAppeasementOffer(YFCElement eOrderIn, YFCElement eOut, YFCElement eAppeasementOffer) {
        String offerType = eAppeasementOffer.getAttribute(A_OFFER_TYPE);

        YFCElement eOrderAppeasementOffer = eAppeasementOffer.createChild(E_ORDER);
        YFCElement eOrderLinesAppeasementOffer = eAppeasementOffer.createChild(E_ORDER_LINES);
        eOrderAppeasementOffer.setAttribute(A_HEADER_OFFER_AMOUNT, "");
        this.setChargeCataegoryAndNameForAppeasementOffers(eOrderAppeasementOffer,offerType);
        double discountPercent = 0.0;
        double offerAmount = 0.0;
        String strDiscountPercent = eAppeasementOffer.getAttribute(A_DISCOUNT_PERCENT);
        String strOfferAmount = eAppeasementOffer.getAttribute(A_OFFER_AMOUNT);

        if (strDiscountPercent.trim() != null && !strDiscountPercent.equalsIgnoreCase("")) {
            discountPercent = Double.parseDouble(strDiscountPercent);
        }

        if (strOfferAmount.trim() != null && !strOfferAmount.equalsIgnoreCase("")) {
            offerAmount = YFCDoubleUtils.roundOff(Double.parseDouble(strOfferAmount), 2);
        }

        double totalLineOfferAmounts = 0.0;
        YFCElement eOrderLines = eOrderIn.getChildElement(E_ORDER_LINES);
        Iterator iteratorOrderLines = eOrderLines.getChildren();
        YFCElement eOrderLineAppeasementOffer;
        if (iteratorOrderLines != null) {
            for(; iteratorOrderLines.hasNext(); this.setChargeCataegoryAndNameForAppeasementOffers(eOrderLineAppeasementOffer,offerType)) {
                YFCElement eOrderLine = (YFCElement)iteratorOrderLines.next();
                eOrderLineAppeasementOffer = eOrderLinesAppeasementOffer.createChild(E_ORDER_LINE);
                String sOrderLineKey = eOrderLine.getAttribute(A_ORDER_LINE_KEY);
                eOrderLineAppeasementOffer.setAttribute(A_ORDER_LINE_KEY, sOrderLineKey);
                String sOrderedQty = eOrderLine.getAttribute(A_ORDERED_QTY);
                eOrderLineAppeasementOffer.setAttribute(A_ORDERED_QTY, sOrderedQty);
                if (discountPercent != 0.0) {
                    YFCElement eLineOverallTotals = eOrderLine.getChildElement(E_LINE_OVERALL_TOTALS);
                    double lineGrandTotal = Double.parseDouble(eLineOverallTotals.getAttribute(A_LINE_TOTAL));
                    double lineOfferAmount = lineGrandTotal * 0.01 * discountPercent;
                    eOrderLineAppeasementOffer.setAttribute(A_LINE_OFFER_AMOUNT, YFCDoubleUtils.roundOff(lineOfferAmount, 2));
                    totalLineOfferAmounts += lineOfferAmount;
                } else {
                    eOrderLineAppeasementOffer.setAttribute(A_LINE_OFFER_AMOUNT, "0.0");
                }
            }
        }

        if (offerType.equalsIgnoreCase("VARIABLE_AMOUNT_ORDER")) {
            eOrderAppeasementOffer.setAttribute(A_HEADER_OFFER_AMOUNT, YFCDoubleUtils.roundOff(offerAmount - totalLineOfferAmounts, 2));
        }

        if (offerType.equalsIgnoreCase("VARIABLE_AMOUNT_ORDER_DEBIT")) {
            eOrderAppeasementOffer.setAttribute(A_HEADER_OFFER_AMOUNT, YFCDoubleUtils.roundOff(offerAmount + totalLineOfferAmounts, 2));
        }

        eOut.importNode(eAppeasementOffer);
        logger.debug("eOut after import in setAppeasementOffer is::"+eOut);
        return eOut;

    }



    private YFCElement setPreferredOffer(YFCElement eOut) {
        return eOut;
    }

    private YFCElement getAppeasementOffersList(YFCElement eIn) {

        YFCElement eAppeasementOffers = YFCDocument.createDocument(E_APPEASEMENT_OFFERS).getDocumentElement();

        YFCElement eOrderIn = eIn.getChildElement(E_ORDER);

        YFCElement eOverallTotalIn = eOrderIn.getChildElement(E_OVERALL_TOTALS);
        double orderGrandTotal = Double.parseDouble(eOverallTotalIn.getAttribute(A_GRAND_TOTAL));
        Properties propertyMap = new Properties();

        try {
            propertyMap.load(CrocsGetAppeasementImpl.class.getResourceAsStream("/resources/ycd_appeasement_variable.properties"));
        } catch (IOException var20) {
            IOException e = var20;
            logger.debug("IO Exception", e);
        }



        String showVariableOffer = propertyMap.getProperty("VARIABLE_AMOUNT_ORDER");
        String showVariableFutureOffer = propertyMap.getProperty("VARIABLE_FUTURE_AMOUNT_ORDER");

        if ("Y".equalsIgnoreCase(showVariableOffer)) {
            eAppeasementOffers = this.createAppeasementOffer(eAppeasementOffers, "VARIABLE_AMOUNT_ORDER", 0.0, orderGrandTotal);
        }

        if ("Y".equalsIgnoreCase(showVariableFutureOffer)) {
            eAppeasementOffers = this.createAppeasementOffer(eAppeasementOffers, "VARIABLE_AMOUNT_ORDER_DEBIT", 0.0, orderGrandTotal);
        }

        return eAppeasementOffers;

    }

    private YFCElement createAppeasementOffer(YFCElement eAppeasementOffers, String sOfferType, double iDiscountPercent, double orderGrandTotal) {

        YFCElement eAppeasementOffer = YFCDocument.createDocument(E_APPEASEMENT_OFFER).getDocumentElement();


        eAppeasementOffer.setAttribute(A_OFFER_TYPE, sOfferType);
        if (iDiscountPercent != 0.0) {
            eAppeasementOffer.setAttribute(A_DISCOUNT_PERCENT, iDiscountPercent);
            double offerAmount = 0.0;
            offerAmount = iDiscountPercent * 0.01 * orderGrandTotal;
            eAppeasementOffer.setAttribute(A_OFFER_AMOUNT, YFCDoubleUtils.roundOff(offerAmount, 2));
        } else {
            eAppeasementOffer.setAttribute(A_DISCOUNT_PERCENT, "");
            eAppeasementOffer.setAttribute(A_CHARGE_PERCENT, "");

            eAppeasementOffer.setAttribute(A_OFFER_AMOUNT, "");

        }

        if (YFCCommon.equalsIgnoreCase(sOfferType, this.preferredType)) {
            eAppeasementOffer.setAttribute(A_PREFERRED, FLAG_Y);
            this.isPreferredTypeSet = true;
        } else {
            eAppeasementOffer.setAttribute(A_PREFERRED, FLAG_N);
        }

        eAppeasementOffers.importNode(eAppeasementOffer);

        return eAppeasementOffers;
    }

    private void setChargeCataegoryAndNameForAppeasementOffers(YFCElement eOut) {

        eOut.setAttribute(A_CHARGE_CATEGORY, CATEGORY_CUSTOMER_APPEASE);
        eOut.setAttribute(A_CHARGE_NAME, CATEGORY_CUSTOMER_APPEASE);
    }

    private void setChargeCataegoryAndNameForAppeasementOffers(YFCElement eOut,String offerType) {
       logger.debug("offerType in setChargeCataegoryAndNameForAppeasementOffers is: "+offerType);

        if(YFCCommon.equalsIgnoreCase(offerType,"VARIABLE_AMOUNT_ORDER_DEBIT")){
            eOut.setAttribute(A_CHARGE_CATEGORY, VAL_CHARGE);
            eOut.setAttribute(A_CHARGE_NAME, CHRG_CUST_DEBIT);
            logger.debug("eOut in if block is: "+eOut);

        }else{
            eOut.setAttribute(A_CHARGE_CATEGORY, CATEGORY_CUSTOMER_APPEASE);
            eOut.setAttribute(A_CHARGE_NAME, CATEGORY_CUSTOMER_APPEASE);

        }
        logger.debug("eOut in setChargeCataegoryAndNameForAppeasementOffers after adding charges is: "+eOut);
    }
}
