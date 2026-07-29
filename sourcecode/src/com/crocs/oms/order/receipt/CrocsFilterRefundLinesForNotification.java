package com.crocs.oms.order.receipt;

import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;

/** EOMS-6849
 * Caluclate updated refund after EchangeReturn line filter for email notifications:
 * - Calculates totals for return lines.
 * - Updates overall totals in the receipt.
 */
public class CrocsFilterRefundLinesForNotification implements CrocsConstant {

    private static final YFCLogCategory logger =
            YFCLogCategory.instance(CrocsFilterRefundLinesForNotification.class);

    public Document filterRefundLines(YFSEnvironment env, Document inDoc) {
        logger.verbose(" Input for CrocsFilterRefundLinesForNotification: Start "+ XMLUtil.getXMLString(inDoc));
        double grandTotal = 0.0;
        double grandDiscount = 0.0;
        double grandTax = 0.0;
        double lineSubTotal = 0.0;
        double grandCharges = 0.0;
        String orderNo =null;
        try {
        Element receiptEle = inDoc.getDocumentElement();
        Element receiptLines = SCXmlUtil.getChildElement(receiptEle, E_RECEIPT_LINES);
        ArrayList<Element> receiptLineList = SCXmlUtil.getChildren(receiptLines, E_RECEIPT_LINE);

        for (Element receiptLine : receiptLineList) {
            logger.verbose("Iterating Receipt Line: " + SCXmlUtil.getString(receiptLine));

            Element orderLine = SCXmlUtil.getChildElement(receiptLine, E_ORDER_LINE);
            orderNo = receiptLine.getAttribute(A_ORDER_NO);
                    Element lineOverallTotals = SCXmlUtil.getChildElement(orderLine, E_LINE_OVERALL_TOTALS);
                    if (lineOverallTotals != null) {
                        String subTotalStr =lineOverallTotals.getAttribute(A_EXTENDED_PRICE);
                        double subTotal = Double.parseDouble(subTotalStr);
                        lineSubTotal += subTotal;

                        String lineGrandDiscountStr =lineOverallTotals.getAttribute(A_DISCOUNT);
                        double lineGrandDiscount = Double.parseDouble(lineGrandDiscountStr);
                        grandDiscount += lineGrandDiscount;

                        String lineGrandChargesStr =lineOverallTotals.getAttribute(A_CHARGES);
                        double lineGrandCharges = Double.parseDouble(lineGrandChargesStr);
                        grandCharges += lineGrandCharges;

                        String lineGrandTaxStr =lineOverallTotals.getAttribute(A_TAX);
                        double lineGrandTax = Double.parseDouble(lineGrandTaxStr);
                        grandTax += lineGrandTax;

                        String lineGrandTotalStr =lineOverallTotals.getAttribute(A_LINE_TOTAL);
                        double lineGrandTotal = Double.parseDouble(lineGrandTotalStr);
                        grandTotal += lineGrandTotal;
                    }
                }
        if (receiptLineList != null && !receiptLineList.isEmpty()) {
                // Use calculated totals
                setOverallTotals(grandDiscount, grandCharges, grandTax, lineSubTotal, grandTotal,receiptLineList);
                logger.verbose("ExchangeLines found. Set new calculated overall totals"+ "grandDiscount"+grandDiscount+ "grandCharges"+grandCharges+"grandTax"+
                        grandTax+"lineSubTotal"+lineSubTotal+"grandTotal"+grandTotal+"receiptLineUpdated"+"receiptLineUpdated");
            }
        } catch (YFSException e) {
            e.printStackTrace();
            logger.verbose("Exception in CrocsFilterRefundLinesForNotification : filterRefundLines " + e.getErrorDescription()+e.getMessage());
            logger.info("CrocsFilterRefundLinesForNotification : filterRefundLines :OrderNo is " + orderNo);
        }
        logger.info("CrocsFilterRefundLinesForNotification : filterRefundLines :OrderNo is " + orderNo +"final Doc"+ SCXmlUtil.getString(inDoc));
        return inDoc;
    }

    /** Updating OverallTotals for remaining Return lines after filtteration
     * @param grandDiscount grandDiscount
     * @param grandCharges grandCharges
     * @param grandTax grandTax
     * @param lineSubTotal lineSubTotal
     * @param grandTotal grandTotal
     * @param receiptLineList receiptLineList
     */
    private static void setOverallTotals(double grandDiscount, double grandCharges, double grandTax,
                                         double lineSubTotal, double grandTotal, ArrayList<Element> receiptLineList) {

        Element receiptLine = receiptLineList.get(0);
        Element orderLine = SCXmlUtil.getChildElement(receiptLine, E_ORDER_LINE);
        Element order = SCXmlUtil.getChildElement(orderLine, E_ORDER);
        Element overallTotals = SCXmlUtil.getChildElement(order, E_OVERALL_TOTALS);

        if (overallTotals != null) {
            overallTotals.setAttribute(A_GRAND_DISCOUNT, String.format("%.2f", grandDiscount));
            overallTotals.setAttribute(A_GRAND_CHARGES, String.format("%.2f", grandCharges));
            overallTotals.setAttribute(A_GRAND_TAX, String.format("%.2f", grandTax));
            overallTotals.setAttribute(A_LINE_SUB_TOTAL, String.format("%.2f", lineSubTotal));
            overallTotals.setAttribute(A_GRAND_TOTAL, String.format("%.2f", grandTotal));
        }
    }
}
