package com.crocs.oms.condition;

import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.XMLUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.ycp.japi.YCPDynamicConditionEx;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.japi.YFSEnvironment;

import com.yantra.yfs.japi.YFSException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Map;

/**EOMS-6849
 * This Class checks if Receipt Lines are empty for Refund Notification
 */
public class CrocsReceiptLinesEmptyCondition implements YCPDynamicConditionEx,CrocsConstant {
    private static YFCLogCategory logger = YFCLogCategory.instance(CrocsReceiptLinesEmptyCondition.class);


    @Override
    public boolean evaluateCondition(YFSEnvironment yfsEnvironment, String s, Map map, Document document) {
        boolean isRecepitLinesEmpty = false;
        try {
            logger.info("Input to the evaluateCondition for CrocsReceiptLinesEmptyCondition is:" + XMLUtil.getXMLString(document));
            Element receiptEle = document.getDocumentElement();
            Element receiptLines = SCXmlUtil.getChildElement(receiptEle, E_RECEIPT_LINES);
            ArrayList<Element> receiptLineList = SCXmlUtil.getChildren(receiptLines, E_RECEIPT_LINE);
            if (receiptLineList == null || receiptLineList.isEmpty()) {
                isRecepitLinesEmpty = true; // No ReceiptLine elements
            }
        }catch(YFSException e) {
            logger.verbose("CrocsReceiptLinesEmptyCondition.evaluateCondition :Expection" + e.getMessage()+e.getErrorDescription());
            logger.info("CrocsReceiptLinesEmptyCondition.evaluateCondition :Expection" + e.getMessage()+e.getErrorDescription());
        }
        return isRecepitLinesEmpty;
    }

    @Override
    public void setProperties(Map map) {

    }
}
