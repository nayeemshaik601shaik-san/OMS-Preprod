package com.crocs.oms.to.order;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.shared.ycp.YFSContext;
import com.yantra.shared.ysc.util.YSCMultiColonyHelper;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCException;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

/**
 * This class is responsible for generating custom Order Numbers
 * for Transfer Orders in Sterling OMS based on the enterprise code.
 * 
 * <p>
 * The sequence number is retrieved from the YFS database using
 * {@link YSCMultiColonyHelper#getNextDBSeqNo(YFSContext, String, String)} 
 * and then formatted with enterprise-specific suffixes.
 * </p>
 */
public class CrocsTOGetOrderNo {

    /** Logger instance for this class */
    private static final YFCLogCategory LOGGER = YFCLogCategory.instance(CrocsTOGetOrderNo.class);

    /**
     * Generates a custom Order Number for the incoming order document.
     *
     * @param env   YFSEnvironment provided by Sterling OMS runtime
     * @param inDoc Input XML document representing the order
     * @return Document with the generated Order Number (if applicable)
     */
    public Document generateOrderNo(YFSEnvironment env, Document inDoc) {
        String customOrderNo = null;

        // Log the input XML for debugging purposes
        LOGGER.verbose("crocsTOGetOrderNo : Input XML : " + SCXmlUtil.getString(inDoc));

        // Get the Order element and extract attributes
        Element eleOrder = inDoc.getDocumentElement();
        String strEnterpriseCode = eleOrder.getAttribute(CrocsXmlConstants.A_ENTERPRISE_CODE);
        String strDocumentType = eleOrder.getAttribute(CrocsXmlConstants.A_DOCUMENT_TYPE);

        LOGGER.verbose("EnterpriseCode: " + strEnterpriseCode + " | DocumentType: " + strDocumentType);

        try {
            // Cast the environment to YFSContext to fetch DB sequence
            YFSContext context = (YFSContext) env;

            // Retrieve the next sequence number for order number generation
            long seqNo = YSCMultiColonyHelper.getNextDBSeqNo(
                    context,
                    CrocsConstant.SEQ_CALL_CENTER_ORDER_NO,
                    strEnterpriseCode);

            // Format order number based on enterprise code
            if (CrocsConstant.CROCS_US.equals(strEnterpriseCode)) {
                customOrderNo = seqNo + CrocsConstant.CUSTOM_US_NO;
                LOGGER.verbose("Generated customOrderNo for crocs_US: " + customOrderNo);
            } else if (CrocsConstant.CROCS_CA.equals(strEnterpriseCode)) {
                customOrderNo = seqNo + CrocsConstant.CUSTOM_CA_NO;
                LOGGER.verbose("Generated customOrderNo for crocs_CA: " + customOrderNo);
            }
            
            eleOrder.setAttribute(CrocsXmlConstants.A_ORDER_NO, customOrderNo);      

        } catch (YFSException e) {
            // Log and rethrow exception with additional context
            LOGGER.verbose("Error in generating OrderNo: " + e.getErrorDescription());
            throw new YFCException(e);
        }

        return inDoc;
    }
}
