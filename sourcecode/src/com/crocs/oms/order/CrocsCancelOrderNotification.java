package com.crocs.oms.order;

import com.crocs.oms.common.util.CrocsXmlConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CrocsCancelOrderNotification implements CrocsXmlConstants {
    private double lineDiscountTotal = 0.0;
    private double lineDiscount = 0.0;
    private double headerDiscount = 0.0;
    private double headerTax = 0.0;
    private double lineTax = 0.0;
    private double lineSubTotal = 0.0;
    private double headerShippingCharge = 0.0;

    private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsCancelOrderNotification.class);

    /** It calculates the overalltotals and line charges
     * changes for EOMS-608 :CancelOrderNotification : updating the values in OverallTotals and LineOverallTotals
     * @param inDoc
     * @return document
     */
    public Document crocsCancelOrderCalculation(Document inDoc){
        logger.verbose("Input XML for class crocsCancelOrderNotification:-" + SCXmlUtil.getString(inDoc));
        double grandDiscount;
        double grandTax;
        double subTotal;
        double grandTotal;
        double linePrice = 0.0;
        try {

            Element orderEle = inDoc.getDocumentElement();
            Element orderLines = SCXmlUtil.getChildElement(orderEle, E_ORDER_LINES);
            ArrayList<Element> orderLineList = SCXmlUtil.getChildren(orderLines, E_ORDER_LINE);
            Element overallTotals = SCXmlUtil.getChildElement(orderEle, E_OVERALL_TOTALS);
            //iterating each orderLine
            for (Element orderLine : orderLineList) {
                logger.verbose("orderLine Element is: " + SCXmlUtil.getString(orderLine));
                logger.verbose("orderNo is: " + orderEle.getAttribute(CrocsXmlConstants.A_ORDER_NO));
                double originalOrderedQty = Double.parseDouble(orderLine.getAttribute(A_ORIGINAL_ORDERED_QTY));
                Element linePriceInfo = SCXmlUtil.getChildElement(orderLine, E_LINE_PRICE_INFO);
                double unitPrice = Double.parseDouble(linePriceInfo.getAttribute(A_UNIT_PRICE));
                Element lineOverallTotals = SCXmlUtil.getChildElement(orderLine, E_LINE_OVERALL_TOTALS);
                //calculate lineSubtotal
                linePrice = unitPrice * originalOrderedQty;
                lineSubTotal += linePrice;
                logger.verbose("Total Line price  " + linePrice);
                logger.verbose("Total Line price  " + lineSubTotal);

                // Process Line Charges
                processLineCharges(orderLine, lineOverallTotals);
                // Process Line Tax
                processLineTax(orderLine);
            }
            //process header charges
            processHeaderCharges(orderEle);
            //process header Tax
            processHeaderTax(orderEle);
            //process overallChargeTotals
            extractOverallChargeTotals(orderEle);
            // Calculate overall totals
            grandDiscount = lineDiscountTotal + headerDiscount;
            grandTax = headerTax + lineTax;
            subTotal = lineSubTotal - grandDiscount;
            double subTotals = Double.parseDouble(String.format("%.2f", subTotal));
            grandTotal = subTotals + grandTax + headerShippingCharge;
            String grandTotals = String.format("%.2f", grandTotal);
            //set overallTotals
            setOverallTotals(grandDiscount, grandTax, grandTotal, overallTotals);
            logger.verbose("grandDiscount" + grandDiscount + "grandCharges" + headerShippingCharge + "grandTax" + grandTax + "lineSubTotal" + lineSubTotal + "grandTotal" + grandTotals);
        } catch (YFSException e) {
            e.printStackTrace();
            logger.verbose("Exception message during order Cancellation " + e.getErrorDescription());
        }
        logger.verbose("Output of CancelOrder:cancelOrderCalculation" + SCXmlUtil.getString(inDoc));
        return inDoc;
    }

    /**Calculating discount
     * @param orderLine orderLine
     * @param lineOverallTotals lineOverallTotals
     */
    private void processLineCharges(Element orderLine, Element lineOverallTotals) {
        Element lineChargesEle = SCXmlUtil.getChildElement(orderLine, E_LINE_CHARGES);
        ArrayList<Element> lineCharges = SCXmlUtil.getChildren(lineChargesEle, E_LINE_CHARGE);
        lineDiscount=0.0;
        for (Element lineCharge : lineCharges) {
            logger.verbose("lineCharge Element is: " + SCXmlUtil.getString(lineCharge));
            if (!YFCCommon.isVoid(lineCharge.getAttribute(A_DISCOUNT_REFERENCE))) {
                double discount = Double.parseDouble(lineCharge.getAttribute(A_DISCOUNT_REFERENCE));
                lineDiscount += discount;
            }
        }
        lineDiscountTotal+=lineDiscount;
        lineDiscount= Double.parseDouble(String.format("%.2f",lineDiscount));
        lineDiscountTotal=Double.parseDouble(String.format("%.2f",lineDiscountTotal));
        lineOverallTotals.setAttribute(A_DISCOUNT, String.valueOf(lineDiscount));

    }

    /** Calculating line tax
     * @param orderLine  OrderLines
     */
    private void processLineTax(Element orderLine) {
        Element orderLineTax = SCXmlUtil.getChildElement(orderLine, E_LINE_TAXES);
        ArrayList<Element> lineTaxes = SCXmlUtil.getChildren(orderLineTax, E_LINE_TAX);
        for (Element tax : lineTaxes) {
            logger.verbose("lineTax Element is: " + SCXmlUtil.getString(tax));
            if (!YFCCommon.isVoid(tax.getAttribute(A_TAX_REFERENCE_1))) {
                lineTax += Double.parseDouble(tax.getAttribute(A_TAX_REFERENCE_1));
            }
        }
    }

    /** setting calculates values in OverallTotals
     * @param grandDiscount grandDiscount
     * @param grandTax grandTax
     * @param grandTotal grandTotal
     * @param overallTotals overallTotals
     */
    private void setOverallTotals(double grandDiscount, double grandTax, double grandTotal, Element overallTotals) {
        overallTotals.setAttribute(A_GRAND_DISCOUNT, String.format("%.2f", grandDiscount));
        overallTotals.setAttribute(A_GRAND_CHARGES, String.format("%.2f", headerShippingCharge));
        overallTotals.setAttribute(A_GRAND_TAX, String.format("%.2f", grandTax));
        overallTotals.setAttribute(A_LINE_SUB_TOTAL, String.format("%.2f", lineSubTotal));
        overallTotals.setAttribute(A_GRAND_TOTAL, String.format("%.2f", grandTotal));
    }

    /** Calculating Header Tax
     * @param orderEle  HeaderTaxes
     */
    private void processHeaderTax(Element orderEle) {
        Element orderHeaderTax = SCXmlUtil.getChildElement(orderEle, E_HEADER_TAXES);
        ArrayList<Element> headerTaxes = SCXmlUtil.getChildren(orderHeaderTax, E_HEADER_TAX);
        for (Element orderHeaderTaxes : headerTaxes) {
            logger.verbose("headerTax Element is: " + SCXmlUtil.getString(orderHeaderTaxes));

            String taxRef = orderHeaderTaxes.getAttribute(A_TAX_REFERENCE_1);
            String chargeCategory = orderHeaderTaxes.getAttribute(A_CHARGE_CATEGORY);

            if (!YFCCommon.isVoid(taxRef)) {
                    double taxAmount = Double.parseDouble(taxRef);

                    if (A_SHIPPING_CHARGE.equalsIgnoreCase(chargeCategory)) {
                        headerTax += taxAmount;
                    } else if (A_SHIPPING_DISCOUNT.equalsIgnoreCase(chargeCategory)) {
                        headerTax -= taxAmount;
                    } else {
                        headerTax += taxAmount;
                    }
            }
        }

    }

    /** Calculating header charges
     * @param orderEle  headercharges
     */
    private void processHeaderCharges(Element orderEle) {
        Element headerChargesEle = SCXmlUtil.getChildElement(orderEle, E_HEADER_CHARGES);
        List<Element> headerCharges = SCXmlUtil.getChildren(headerChargesEle, E_HEADER_CHARGE);

        for (Element orderHeaderCharge : headerCharges) {
            logger.verbose("HeaderCharge Element: " + SCXmlUtil.getString(orderHeaderCharge));

            String shippingRef = orderHeaderCharge.getAttribute(A_SHIPPING_REFERENCE);
            String chargeCategory = orderHeaderCharge.getAttribute(A_CHARGE_CATEGORY);

            if (!YFCCommon.isVoid(shippingRef)) {
                double amount = Double.parseDouble(shippingRef);

                if (A_SHIPPING_CHARGE.equalsIgnoreCase(chargeCategory) ||A_EXTENDSHIP_PROTECTION.equalsIgnoreCase(chargeCategory)) {
                    headerShippingCharge += amount;
                } else if (A_DISCOUNT.equalsIgnoreCase(chargeCategory) ||
                                A_SHIPPING_DISCOUNT.equalsIgnoreCase(chargeCategory) || A_PROMOTION_DISCOUNT.equalsIgnoreCase(chargeCategory)) {
                    headerDiscount += amount;
                }
            }
        }

    }
    /** this process the header level shipping charge and discount to set OverallChargeTotals
     * @param orderEle OrderEle
     */
    private static void extractOverallChargeTotals(Element orderEle) {
        Element overallTotals = SCXmlUtil.getChildElement(orderEle, E_OVERALL_TOTALS);
        Element overallChargeTotals = SCXmlUtil.getChildElement(overallTotals, E_OVERALL_CHARGE_TOTALS);
        ArrayList<Element> overallChargeTotalEle = SCXmlUtil.getChildren(overallChargeTotals, E_OVERALL_CHARGE_TOTAL);
        for (Element overAllChargeTotal : overallChargeTotalEle) {
            logger.verbose("HeaderTax Element is: " + SCXmlUtil.getString(overAllChargeTotal));
            if ((A_SHIPPING_CHARGE.equalsIgnoreCase(overAllChargeTotal.getAttribute(A_CHARGE_NAME))) && !YFCCommon.isVoid(overAllChargeTotal.getAttribute(A_REFERENCE))) {
                overAllChargeTotal.setAttribute(A_GRAND_CHARGES, overAllChargeTotal.getAttribute(A_REFERENCE));
            }
            if ((A_SHIPPING_DISCOUNT.equalsIgnoreCase(overAllChargeTotal.getAttribute(A_CHARGE_NAME))) && !YFCCommon.isVoid(overAllChargeTotal.getAttribute(A_REFERENCE))) {
                overAllChargeTotal.setAttribute(A_GRAND_DISCOUNT, overAllChargeTotal.getAttribute(A_REFERENCE));
            }
        }
    }
}
