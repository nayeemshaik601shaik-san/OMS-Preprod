package com.crocs.oms.pricing;

import com.crocs.oms.common.util.CrocsConstant;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSUserExitException;
import com.yantra.yfs.japi.ue.YFSOrderRepricingUE;

import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;

import java.util.ArrayList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class CrocsOrderRepricingUEImpl implements CrocsConstant, YFSOrderRepricingUE{


    private static YFCLogCategory logger = YFCLogCategory.instance(CrocsOrderRepricingUEImpl.class);

    /** this UE will be invoked on quantity cancellation from the CC for lineCharge recalculation
     * changes for EOMS-2026
     * @param yfsEnvironment
     * @param document
     * @return repriceOrderUE
     * @throws Exception
     */

    @Override
    public Document orderReprice(YFSEnvironment yfsEnvironment, Document document) throws YFSUserExitException {

       logger.debug("Input into orderReprice::" + SCXmlUtil.getString(document));
       logger.beginTimer("CrocsOrderRepricingUEImpl::orderReprice: START:" + SCXmlUtil.getString(document));

        try {
            Element input = document.getDocumentElement();
            Element orderLines = SCXmlUtil.getChildElement(input, E_ORDER_LINES);
            NodeList orderLineList = SCXmlUtil.getXpathNodes(orderLines, E_ORDER_LINE);

            for (int j = 0; j < orderLineList.getLength(); ++j) {
                Element orderLine = (Element) orderLineList.item(j);

                double origOrderedQty = Double.parseDouble(orderLine.getAttribute(A_ORIGINAL_ORDERED_QTY));
                double orderedQty = Double.parseDouble(orderLine.getAttribute(A_ORDERED_QTY));
                double cancelledQty = origOrderedQty-orderedQty;

                logger.debug("origOrderedQty is: " + origOrderedQty);
                logger.debug("orderedQty is: " + orderedQty);
                logger.debug("cancelledQty is: " + cancelledQty);


                if (isQuantityUpdated(orderLine)) {
                    updateLineCharges(orderLine, origOrderedQty, orderedQty,cancelledQty);
                }
            }

            logger.debug("document after update is:: " + SCXmlUtil.getString(document));
            logger.beginTimer("CrocsOrderRepricingUEImpl::orderReprice: END:" + SCXmlUtil.getString(document));
            return document;

        } catch (Exception e) {
            throw new YFSUserExitException(e.getMessage());
        }
    }

    private boolean isQuantityUpdated(Element orderLine) {
        Element modTypes = SCXmlUtil.getChildElement(orderLine, E_MODIFICATION_TYPES);
        ArrayList<Element> orderLineModificationType = SCXmlUtil.getChildren(modTypes, E_MODIFICATION_TYPE);

        logger.debug("orderLineModificationType.getLength is:" + orderLineModificationType.size());

        for (Element modificationTypeEle : orderLineModificationType) {
            String modificationName = modificationTypeEle.getAttribute("Name");
			
			//this is applicable only for qty cancellation
            if ("CANCEL".equals(modificationName)) {
                logger.debug("Quantity updated due to modification type: " + modificationName);
                return true;
            }
        }

        logger.debug("No quantity update detected.");
        return false;
    }

    private void updateLineCharges(Element orderLine, double origOrderedQty, double orderedQty,double cancelledQty) {
        Element lineChargesEle = SCXmlUtil.getChildElement(orderLine, E_LINE_CHARGES);
        NodeList lineChargeElement = SCXmlUtil.getXpathNodes(lineChargesEle, E_LINE_CHARGE);

        for (int i = 0; i < lineChargeElement.getLength(); ++i) {
            double origChargePerLine;

            Element lineChargeEle = (Element) lineChargeElement.item(i);
            double chargePerLine = Double.parseDouble(lineChargeEle.getAttribute(A_CHARGE_PER_LINE));

            if(!YFCCommon.isVoid(lineChargeEle.getAttribute(A_REFERENCE))){
                 origChargePerLine = Double.parseDouble(lineChargeEle.getAttribute(A_REFERENCE));
                logger.debug("origChargePerLine in if block is: " + origChargePerLine);

            }else{
				
				//in case where Reference attribute is blank while order creation
				
                lineChargeEle.setAttribute(A_REFERENCE,lineChargeEle.getAttribute(A_CHARGE_PER_LINE));
                logger.debug("reference in else block is: " + lineChargeEle.getAttribute(A_REFERENCE));
                origChargePerLine = Double.parseDouble(lineChargeEle.getAttribute(A_REFERENCE));
                logger.debug("origChargePerLine in else block is: " + origChargePerLine);
            }

            logger.debug("Final origChargePerLine is: " + origChargePerLine);
            logger.debug("reference value is: " + lineChargeEle.getAttribute(A_REFERENCE));
            double initialChargePerUnit = (origChargePerLine / origOrderedQty);
            logger.debug("initialChargePerUnit here is: " + initialChargePerUnit);
            initialChargePerUnit = Math.round(initialChargePerUnit*100.0)/100.0;

            if(orderedQty>0) {

                logger.debug("chargePerLine is: " + chargePerLine);
                logger.debug("origChargePerLine is: " + origChargePerLine);
                logger.debug("orderedQty is: " + orderedQty);
                logger.debug("cancelledQty is: " + cancelledQty);
                logger.debug("initialChargePerUnit is: " + initialChargePerUnit);

                double finalChargePerLine = initialChargePerUnit*cancelledQty;
                logger.debug("finalChargePerLine is: " + finalChargePerLine);
                chargePerLine = origChargePerLine - finalChargePerLine;
                logger.debug("chargePerLine is:: " + chargePerLine);
            }

            lineChargeEle.setAttribute(A_CHARGE_PER_LINE, String.format("%.2f", chargePerLine));
            lineChargeEle.setAttribute(A_CHARGE_AMOUNT, String.format("%.2f", chargePerLine));

            logger.debug("lineChargeEle after update is:: " + SCXmlUtil.getString(lineChargeEle));
        }
    }


}



