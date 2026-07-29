package com.crocs.oms.order.migration.heydude;

import org.w3c.dom.Document;

import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.order.CrocsHeaderChargesIterationToLine;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.dom.YFCNode;
import com.yantra.yfc.dom.YFCNodeList;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class HeyDudeImportSalesOrderMigration implements CrocsConstant {

    private static YFCLogCategory logger = YFCLogCategory.instance(HeyDudeImportSalesOrderMigration.class.getName());
    boolean isHeaderToLineChargeProrationRequired = false;

    /**
     * @param env - YFSEnvironment
     * @param inDoc - Input XML
     * @return Final validated and updated XML
     **/

    /**
     * EOMS-6121 Service Name: HeyDudeGetImportSOFromQAsyncService</p>
     * Purpose: This service will consume XML from HEYDUDE_IN_ORDER_MIGRATION_QUEUE and perform data validation before importing the order
     * Mandatory Attributes: DocumentType, EnteredBy, EnterpriseCode, OrderNo, ConditionVariable1, PipelineKey
     */



    public Document importSOMigrationValidation(YFSEnvironment env, Document inDoc){
        logger.beginTimer(HeyDudeMigarationUtil.logCurrentMethod(this.getClass()));
        logger.verbose("Input Doc for CrocsSalesOrderMigration: \n" + YFCDocument.getDocumentFor(inDoc).toString());

        try {
            CrocsHeaderChargesIterationToLine headerChargesIterationToLine = null;
            Document headerChargesIterationToLineDoc = null;

            YFCDocument importOrderInYdoc = YFCDocument.getDocumentFor(inDoc);
            YFCElement importOrderYdocEle = importOrderInYdoc.getDocumentElement();
            String enterpriseCode = importOrderYdocEle.getAttribute(A_ENTERPRISE_CODE);
            String pipelineProperty =  enterpriseCode + "_SO_PIPELINE_KEY";
            logger.verbose("pipelineProperty is: "+pipelineProperty);


            String importSOMandatoryAttr[] = {A_DOCUMENT_TYPE,
                    VAL_DOCUMENT_TYPE_SALES_ORDER, A_ENTERED_BY,
                    A_ENTERPRISE_CODE, A_ORDER_NO, A_CONDITION_VARIABLE_1,
                    pipelineProperty };

            logger.verbose("importSOMigrationValidation validated in mandatory fields "+importSOMandatoryAttr);

            boolean isValidForImportOrderProcesingFlag = isValidForImportOrderProcesing(importOrderYdocEle,
                    importSOMandatoryAttr);
            logger.verbose("importSOMigrationValidation : is validation passed "+isValidForImportOrderProcesingFlag);

            if (isValidForImportOrderProcesingFlag) {

                validateOrderQtyForCancellation(importOrderInYdoc);

                importOrderInYdoc = validateSortShip(importOrderInYdoc);

                isHeaderToLineChargeProrationRequired = IsHeaderToLineChargeProrationRequired(importOrderYdocEle);
                logger.verbose("importSOMigrationValidation : is isHeaderToLineChargeProrationRequired "+isHeaderToLineChargeProrationRequired);

                if(isHeaderToLineChargeProrationRequired) {
                    headerChargesIterationToLine = new CrocsHeaderChargesIterationToLine();
                    headerChargesIterationToLineDoc = headerChargesIterationToLine
                            .headerChargesIterationToLine(importOrderInYdoc.getDocument());

                    headerChargesIterationToLine.filterHeaderCharges(importOrderInYdoc.getDocument());
                    headerChargesIterationToLine.filterHeaderTaxes(importOrderInYdoc.getDocument());

                    logger.verbose("importSOMigrationValidation : Document post proration " +importOrderInYdoc);
                }
                else
                    headerChargesIterationToLineDoc = importOrderInYdoc.getDocument();

                importSOProcesingReadiness(env,YFCDocument.getDocumentFor(headerChargesIterationToLineDoc).getDocumentElement());
            }
            logger.endTimer(HeyDudeMigarationUtil.logCurrentMethod(this.getClass()));
            return importOrderInYdoc.getDocument();

        } catch (YFSException e) {
            throw new YFSException(e.getMessage(), e.getErrorCode(), e.getErrorDescription());
        }
    }

    private void validateOrderQtyForCancellation(YFCDocument importOrderInYdoc) {

        /**
         * This is to handle the scenario where line is cancelled so shippedQty is 0.As per mapping OrderedQty is mapped to ShippedQty.
         * and hence status is 9000 , this is cancelled scenario, which was conflicting with shortShip Scenario.
         *
         **/

        YFCNodeList<YFCElement> orderLines = importOrderInYdoc.getElementsByTagName(E_ORDER_LINE);
        for (YFCElement orderLine : orderLines) {
            if (STR_STATUS_CANCELLED
                    .equals(orderLine.getElementsByTagName(A_ORDER_STATUS).item(0).getAttribute(A_STATUS))) {

                String strOrderedQty = orderLine.getAttribute(CrocsConstant.A_ORDERED_QTY);
                if(YFCCommon.isVoid(strOrderedQty))
                    orderLine.setAttribute(A_ORDERED_QTY,"0");

                double orderedQty = Double.parseDouble(orderLine.getAttribute(A_ORDERED_QTY));
                if (orderedQty < 1) {
                    orderLine.setAttribute(A_ORDERED_QTY,
                            orderLine.getChildElement(A_LINE_PRICE_INFO).getAttribute(A_ACTUAL_PRICING_QTY));
                }
            }
        }
    }

    private YFCDocument validateSortShip(YFCDocument importOrderInYdoc) {


        /** This method only for SortShip Scenario where order has 4 lines each with 4 Qty and for one of the lines, few of the lines are not shipped
         *  logic is to :-
         *
         *  OrderQty will hold "No of ShipmentQty" for a particular line.
         *  In sortship Scenario, lets say item_id: 1234 , qty : 4
         *  item_id: 1234 , OrderedQty="0"(ShipmentQty) then actual shipped Qty is: 3
         *  so we store OriginalOrderedQty = 4 (Actually shipped + actual Non Shipped Qty).
         **/

        boolean isSortShipScenario = false;
        YFCDocument  importOrderNonSortShipInYdoc = YFCDocument.createDocument();
        importOrderNonSortShipInYdoc = importOrderInYdoc.getCopy();

        YFCDocument onlyActualShipOrderLines = YFCDocument.createDocument(E_ORDER_LINES);
        YFCElement onlyActualShipOrderLine = onlyActualShipOrderLines.getDocumentElement();

        YFCNodeList<YFCElement> sortShipOrderLines = importOrderInYdoc.getElementsByTagName(E_ORDER_LINE);
        for (YFCElement sortShipOrderLine : sortShipOrderLines) {

            double OrderedQty = Double.parseDouble(sortShipOrderLine.getAttribute(A_ORDERED_QTY));
            if (OrderedQty < 1) {

                isSortShipScenario = true;
                String shortShipItem = sortShipOrderLine.getChildElement(E_ITEM).getAttribute(A_ITEM_ID);
                double shortShipQty = Double.parseDouble(sortShipOrderLine.getChildElement(A_LINE_PRICE_INFO).getAttribute(A_ACTUAL_PRICING_QTY));

                YFCNodeList<YFCElement> actualShipOrderLines = importOrderInYdoc.getElementsByTagName(E_ORDER_LINE);
                for (YFCElement actualShipOrderLine : actualShipOrderLines) {

                    if (shortShipItem.equals(actualShipOrderLine.getChildElement(E_ITEM).getAttribute(A_ITEM_ID))) {
                        double actualShipOrderedQty = Double.parseDouble(actualShipOrderLine.getAttribute(A_ORDERED_QTY));
                        if (actualShipOrderedQty > 0) {
                            double originalOrderedQty = actualShipOrderedQty + shortShipQty;
                            actualShipOrderLine.setAttribute(A_ORIGINAL_ORDERED_QTY, originalOrderedQty);

                            YFCElement actualShiporderLine = onlyActualShipOrderLines.importNode(actualShipOrderLine,
                                    true);
                            onlyActualShipOrderLines.getFirstChild().appendChild((YFCNode) actualShiporderLine);
                        }
                    }
                }
            } else {

                /** if OrderedQty is non 0 , then logic is to prepare a document and keep apending the real qty Shipped lines and then
                 * go for proration line total and grand total 	**/

                boolean isItemAppended = false;
                YFCNodeList<YFCElement> OrderLines = onlyActualShipOrderLine.getElementsByTagName(E_ORDER_LINE);
                for (YFCElement OrderLine : OrderLines) {
                    if (OrderLine.getChildElement(E_ITEM).getAttribute(A_ITEM_ID)
                            .equals(sortShipOrderLine.getChildElement(E_ITEM).getAttribute(A_ITEM_ID))) {
                        isItemAppended = true;
                        break;
                    } else
                        isItemAppended = false;
                }
                if (!isItemAppended) {
                    YFCElement sortShipOrderLinee = onlyActualShipOrderLines.importNode(sortShipOrderLine, true);
                    onlyActualShipOrderLines.getFirstChild().appendChild((YFCNode) sortShipOrderLinee);
                }
            }
        }

        importOrderInYdoc.getDocumentElement()
                .removeChild((YFCNode) importOrderInYdoc.getDocumentElement().getChildElement(E_ORDER_LINES));

        YFCElement onlyActualShipOrderLine1 = importOrderInYdoc.importNode(onlyActualShipOrderLine, true);
        importOrderInYdoc.getFirstChild().appendChild((YFCNode) onlyActualShipOrderLine1);

        if (isSortShipScenario)
            return importOrderInYdoc;
        else
            return importOrderNonSortShipInYdoc;
    }

    private boolean IsHeaderToLineChargeProrationRequired(YFCElement importOrderYdocEle) {
        boolean IsProrationRequired = false;

        if (!CrocsConstant.STR_STATUS_CANCELLED.equals(importOrderYdocEle
                .getElementsByTagName(CrocsConstant.A_ORDER_STATUS).item(0).getAttribute(CrocsConstant.A_STATUS))) {

            YFCNodeList<YFCElement> headerCharges = importOrderYdocEle.getElementsByTagName(E_HEADER_CHARGE);
            for (YFCElement headerCharge : headerCharges) {

                /**
                 * Marking the flag as true for proration in case of chargeCategory other than
                 * ShippingCharges even once.
                 */

                String chargeCategory = headerCharge.getAttribute(A_CHARGE_CATEGORY);

                if (!CHARGE_CATEGORY_SHIPPING_CHARGE.equals(chargeCategory)
                        && !A_SHIPPING_DISCOUNT.equals(chargeCategory)
                        && !A_EXTENDED_SHIPPING_PROTECTION.equals(chargeCategory))
                    IsProrationRequired = true;
            }
        }

        return IsProrationRequired;
    }

    private void importSOProcesingReadiness(YFSEnvironment env, YFCElement importOrderYdocEle) {
        logger.beginTimer(HeyDudeMigarationUtil.logCurrentMethod(this.getClass()));


        if (!STR_STATUS_CANCELLED.equals(importOrderYdocEle
                .getElementsByTagName(A_ORDER_STATUS).item(0).getAttribute(A_STATUS))) {

            setLineTotal(importOrderYdocEle);
            logger.verbose("importSOMigrationValidation : setLineTotal(importOrderYdocEle) " + importOrderYdocEle);

        }else {
            handlingFullCancelledImportSO(importOrderYdocEle);

        }

        if (isHeaderToLineChargeProrationRequired)
            setGrandTotal(importOrderYdocEle);

        logger.verbose("importSOMigrationValidation : setGrandTotal(importOrderYdocEle) " + importOrderYdocEle);
        logger.endTimer(HeyDudeMigarationUtil.logCurrentMethod(this.getClass()));
    }

    private void handlingFullCancelledImportSO(YFCElement importOrderYdocEle) {


        importOrderYdocEle.getChildElement(E_PRICE_INFO).setAttribute(A_INVOICED_AMOUNT, "0.0");
        importOrderYdocEle.getChildElement(E_PRICE_INFO).setAttribute(TOTAL_AMOUNT, "0.0");

        YFCNodeList<YFCElement> headerCharges = importOrderYdocEle.getElementsByTagName(A_HEADER_CHARGE);
        for(YFCElement headerCharge:headerCharges)
            headerCharge.setAttribute(A_CHARGE_AMOUNT, "0.0");


        YFCNodeList<YFCElement> orderLines = importOrderYdocEle.getElementsByTagName(E_ORDER_LINE);
        for (YFCElement orderLine : orderLines) {

            orderLine.setAttribute(A_ORIGINAL_ORDERED_QTY, orderLine.getAttribute(A_ORDERED_QTY));
            orderLine.setAttribute(A_ORDERED_QTY, "0.0");

            YFCNodeList<YFCElement> lineTaxs = orderLine.getElementsByTagName(E_LINE_TAX);
            for (YFCElement lineTax : lineTaxs) {
                    lineTax.setAttribute(A_TAX, "0.0");
            }

            YFCElement linesPriceInfo = orderLine.getChildElement(E_LINE_PRICE_INFO);
            linesPriceInfo.setAttribute(A_LINE_TOTAL, "0.0");
            linesPriceInfo.setAttribute(A_LIST_PRICE, "0.0");
            linesPriceInfo.setAttribute(A_RETAIL_PRICE, "0.0");
            linesPriceInfo.setAttribute(A_TAX, "0.0");
            linesPriceInfo.setAttribute(A_UNIT_PRICE, "0.0");
        }
    }

    private void setGrandTotal(YFCElement importOrderYdocEle) {
        double  grandLineTotal = 0.0, shippingCharge = 0.0, shippingTaxForShippingCharge = 0.0, shippingTaxForShippingDiscount=0.0,
                shipProtectionTax = 0.0, extendShipProtection = 0.0, shippingDiscount=0.0,grandTotal = 0.0;

        YFCNodeList<YFCElement> headerCharges = importOrderYdocEle.getElementsByTagName(E_HEADER_CHARGE);
        for (YFCElement headerCharge : headerCharges) {
            if (CHARGE_CATEGORY_SHIPPING_CHARGE.equals(headerCharge.getAttribute(A_CHARGE_CATEGORY)))
                shippingCharge += Double.parseDouble(headerCharge.getAttribute(A_CHARGE_AMOUNT));

            if (CrocsConstant.A_EXTENDED_SHIPPING_PROTECTION.equals(headerCharge.getAttribute(A_CHARGE_CATEGORY)))
                extendShipProtection += Double.parseDouble(headerCharge.getAttribute(A_CHARGE_AMOUNT));

            if (CrocsXmlConstants.A_SHIPPING_DISCOUNT.equals(headerCharge.getAttribute(A_CHARGE_CATEGORY)))
                shippingDiscount += Double.parseDouble(headerCharge.getAttribute(A_CHARGE_AMOUNT));
        }

        YFCNodeList<YFCElement> headerTaxes = importOrderYdocEle.getElementsByTagName(E_HEADER_TAX);
        for (YFCElement headerTax : headerTaxes) {
            if (A_SHIPPING_TAX.equals(headerTax.getAttribute(A_TAX_NAME))) {

                if (CHARGE_CATEGORY_SHIPPING_CHARGE
                        .equals(headerTax.getAttribute(A_CHARGE_CATEGORY)))
                    shippingTaxForShippingCharge += Double.parseDouble(headerTax.getAttribute(A_TAX));

                if (CrocsXmlConstants.A_SHIPPING_DISCOUNT
                        .equals(headerTax.getAttribute(A_CHARGE_CATEGORY)))
                    shippingTaxForShippingDiscount += Double.parseDouble(headerTax.getAttribute(A_TAX));
            }

            if (A_SHIP_PROTECTION_TAX.equals(headerTax.getAttribute(A_TAX_NAME)))
                shipProtectionTax += Double.parseDouble(headerTax.getAttribute(A_TAX));
        }

        logger.verbose("importSOMigrationValidation : setGrandTotal(importOrderYdocEle) shippingCharge" + shippingCharge
                + " shippingTax" + shippingTaxForShippingCharge);

        YFCNodeList<YFCElement> orderLines = importOrderYdocEle.getElementsByTagName(E_ORDER_LINE);
        for (YFCElement orderLine : orderLines) {
            double salesTax = 0.0;
            double chargeAmount = 0.0;
            double itemQty = Double.parseDouble(orderLine.getAttribute(A_ORDERED_QTY));
            double unitPrice = Double.parseDouble(orderLine.getChildElement(E_LINE_PRICE_INFO).getAttribute(A_UNIT_PRICE));

            logger.verbose("importSOMigrationValidation : setGrandTotal(importOrderYdocEle) itemQty" + itemQty
                    + " unitPrice" + unitPrice);

            YFCNodeList<YFCElement> lineTaxes = orderLine.getElementsByTagName(E_LINE_TAX);
            for (YFCElement lineTax : lineTaxes) {
                    salesTax += Double.parseDouble(lineTax.getAttribute(A_TAX));

            }

            YFCNodeList<YFCElement> lineCharges = orderLine.getElementsByTagName(E_LINE_CHARGE);
            for (YFCElement lineCharge : lineCharges) {

                String chargeCategory = lineCharge.getAttribute(A_CHARGE_CATEGORY);
                if (chargeCategory.equals(A_PROMOTION_DISCOUNT))
                    chargeAmount += Double.parseDouble(lineCharge.getAttribute(A_CHARGE_AMOUNT));

                else if (chargeCategory.equals(A_CROCS_PROMOTION_HDR_DISCOUNT)) {
                    chargeAmount += Double.parseDouble(lineCharge.getAttribute(A_CHARGE_PER_LINE));
                }
            }
            logger.verbose("importSOMigrationValidation : setGrandTotal(importOrderYdocEle) salesTax" + salesTax
                    + " chargeAmount" + chargeAmount);

            grandLineTotal += (itemQty * unitPrice) + salesTax - chargeAmount;

            logger.verbose("importSOMigrationValidation : setGrandTotal(importOrderYdocEle) "
                    + "grandLineTotal += (itemQty * unitPrice) + salesTax - chargeAmount"
                    + " grandLineTotal" + grandLineTotal);

        }

        grandTotal = grandLineTotal + (shippingCharge + extendShipProtection - shippingDiscount)
                + shippingTaxForShippingCharge - shippingTaxForShippingDiscount;

        logger.verbose("importSOMigrationValidation : setGrandTotal(importOrderYdocEle) "
                + "grandTotal = grandLineTotal + shippingCharge + shippingTax"
                + " grandTotal" + grandTotal);

        importOrderYdocEle.getChildElement(E_PRICE_INFO).setAttribute(E_HEADER_TAX, (shippingTaxForShippingCharge + shipProtectionTax - shippingTaxForShippingDiscount));
        importOrderYdocEle.getChildElement(E_PRICE_INFO).setAttribute(TOTAL_AMOUNT, grandTotal);

    }

    private void setLineTotal(YFCElement importOrderYdocEle) {
        /**
         * How the lineTOtal has been calculated
         * Item unitPrice * LineQty + Line Taxes + (-Discount) + (-Discount Taxes);

         * in below case SalesTax= Line Taxes + (-Discount Taxes);
         *
         **/

        YFCNodeList<YFCElement> orderLines = importOrderYdocEle.getElementsByTagName(E_ORDER_LINE);
        for (YFCElement orderLine : orderLines) {
            double salesTax = 0.0, chargeAmount = 0.0, lineTotal = 0.0, grandTotal = 0.0;

            /** Updating /Order/OrderLines/OrderLine/OrderStatuses/OrderStatus/@StatusQty  with
             * /Order/OrderLines/OrderLine/@OrderedQty
             * just to make sure shipped qty is mapped to the StatusQty with same Qty.
             **/
            String lineOrderedQty = orderLine.getAttribute(A_ORDERED_QTY);
            YFCElement orderLineStatus = orderLine.getElementsByTagName(A_ORDER_STATUS).item(0);
            if ("3700".equals(orderLineStatus.getAttribute(A_STATUS))) {
                orderLineStatus.setAttribute(A_STAT_QTY,lineOrderedQty);
            }
            /* END	*/

            double itemQty = Double.parseDouble(orderLine.getAttribute(A_ORDERED_QTY));
            double unitPrice = Double.parseDouble(orderLine.getChildElement(E_LINE_PRICE_INFO).getAttribute(A_UNIT_PRICE));
            orderLine.setAttribute(A_SHIP_NODE,orderLine.getElementsByTagName(E_SCHEDULE).item(0).getAttribute(A_SHIP_NODE));

            YFCNodeList<YFCElement> lineTaxes = orderLine.getElementsByTagName(E_LINE_TAX);
            for (YFCElement lineTax : lineTaxes) {

                    salesTax += Double.parseDouble(lineTax.getAttribute(A_TAX));

            }


            YFCNodeList<YFCElement> lineCharges = orderLine.getElementsByTagName(E_LINE_CHARGE);
            for (YFCElement lineCharge : lineCharges) {
                String chargeCategory = lineCharge.getAttribute(A_CHARGE_CATEGORY);
                if (chargeCategory.equals(A_PROMOTION_DISCOUNT)) {
                    lineCharge.setAttribute(A_CHARGE_AMOUNT, lineCharge.getAttribute(A_CHARGE_PER_LINE));
                    chargeAmount += Double.parseDouble(lineCharge.getAttribute(A_CHARGE_PER_LINE));
                }
                /** getting the charges PromotionHdrDiscount to calculate total lineChargeAmount
                 *  and setting the ChargeAmount at LineCharge to reflect right discount on UI
                 *  **/

                else if (chargeCategory.equals(A_CROCS_PROMOTION_HDR_DISCOUNT)) {
                    lineCharge.setAttribute(A_CHARGE_AMOUNT, lineCharge.getAttribute(A_CHARGE_PER_LINE));
                    chargeAmount += Double.parseDouble(lineCharge.getAttribute(CrocsConstant.A_CHARGE_PER_LINE));
                }
            }

            if (isHeaderToLineChargeProrationRequired) {

                /**
                 * Setting the tax at LinePriceInfo label so that header tax shown right post
                 * tax proration
                 */
                orderLine.getChildElement(E_LINE_PRICE_INFO).setAttribute(A_TAX, salesTax);
            }

            lineTotal = ((itemQty * unitPrice) + salesTax) - chargeAmount;
            orderLine.getChildElement(E_LINE_PRICE_INFO).setAttribute(A_LINE_TOTAL, lineTotal);
        }
    }

    /**
     * @param importOrderYdocEle - YFCElement of Order
     */
    private boolean isValidForImportOrderProcesing(YFCElement importOrderYdocEle,
                                                   String[] ImportSalesOrderMandatoryAttrtributes) {
        logger.beginTimer(HeyDudeMigarationUtil.logCurrentMethod(this.getClass()));

        boolean isValid = false;
        for (String ImportOrderMandatoryAttrtribute : ImportSalesOrderMandatoryAttrtributes) {

            if (HeyDudeMigarationUtil.validateMandatoryAttribute(importOrderYdocEle,
                    ImportOrderMandatoryAttrtribute))
                isValid = true;
            else
                break;
        }
        logger.endTimer(HeyDudeMigarationUtil.logCurrentMethod(this.getClass()));
        return isValid;
    }
}
