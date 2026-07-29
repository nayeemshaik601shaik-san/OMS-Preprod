package com.crocs.oms.util.ue;

import com.crocs.oms.common.util.CrocsConstant;
import com.yantra.interop.util.YFSContextManager;
import com.yantra.shared.ycp.YFSContext;
import com.yantra.shared.ysc.util.YSCMultiColonyHelper;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCConfigurator;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import com.yantra.yfs.japi.YFSUserExitException;
import com.yantra.yfs.japi.ue.YFSGetOrderNoUE;

import java.util.HashMap;
import java.util.Map;

/**
 * EOMS-11588: This class generates custom Order Numbers for Call Center orders
 * based on Enterprise, Document Type, and Environment.
 *
 * Refactored to reduce duplication and improve maintainability.
 */
public class CrocsGetOrderNumberUEImplForLowerEnv implements CrocsConstant, YFSGetOrderNoUE {

    private static final YFCLogCategory logger =
            YFCLogCategory.instance(CrocsGetOrderNumberUEImplForLowerEnv.class);

    // Centralized suffix mapping
    private static final Map<String, String> SALES_SUFFIX_MAP = new HashMap<>();
    private static final Map<String, String> TRANSFER_SUFFIX_MAP = new HashMap<>();

    static {
        // Sales Order suffixes
        SALES_SUFFIX_MAP.put(CROCS_US, CUSTOM_US_NO);
        SALES_SUFFIX_MAP.put(CROCS_CA, CUSTOM_CA_NO);
        SALES_SUFFIX_MAP.put(HEYDUDE_US, CUSTOM_HUS_NO);
        SALES_SUFFIX_MAP.put(HEYDUDE_CA, CUSTOM_HCA_NO);
        SALES_SUFFIX_MAP.put(HEYDUDE_AU, CUSTOM_HAU_NO);
        SALES_SUFFIX_MAP.put(CROCS_AU, CUSTOM_CAU_NO);

        // Transfer Order suffixes
        TRANSFER_SUFFIX_MAP.put(CROCS_US, CUSTOM_TO_US_NO);
        TRANSFER_SUFFIX_MAP.put(CROCS_CA, CUSTOM_TO_CA_NO);
    }

    /**
     * Generates a custom Order Number based on enterprise, document type,
     * and environment configuration.
     *
     * @param env YFS Environment object
     * @param map Input map containing EnterpriseCode and DocumentType
     * @return Generated Order Number
     * @throws YFSUserExitException if any error occurs during generation
     */
    @Override
    public String getOrderNo(YFSEnvironment env, Map map) throws YFSUserExitException {

        logger.verbose("Input map: " + map);

        String enterprise = (String) map.get(EnterpriseCode);
        String docType = (String) map.get(DocumentType);

        try {
            YFSContext context = getYfsContext(env);

            String prefix = getEnvPrefix();
            long seqNo = YSCMultiColonyHelper.getNextDBSeqNo(
                    context, SEQ_CALL_CENTER_ORDER_NO, enterprise);

            String suffix = getSuffix(enterprise, docType);

            String orderNo = buildOrderNo(prefix, seqNo, suffix, docType);
            logger.verbose("Generated OrderNo: " + orderNo);

            return orderNo;

        } catch (YFSException e) {
            logger.verbose("Error generating OrderNo: " + e.getErrorDescription());
            throw new YFSUserExitException("Error in getOrderNo: " + e.getErrorDescription());
        }
    }

    /**
     * Retrieves environment-specific prefix for Order Number.
     * Example:
     * QA -> Q
     * PREPROD -> S
     *
     * @return Environment prefix string (or empty if not configured)
     */
    private String getEnvPrefix() {
        String env = YFCConfigurator.getInstance().getProperty(ORDER_NO_PREFIX);

        if (YFCCommon.isVoid(env)) return "";

        if (ORDER_NO_PREFIX_QA.equals(env)) return "Q";
        if (ORDER_NO_PREFIX_PREPROD.equals(env)) return "S";

        return "";
    }

    /**
     * Determines the suffix to be appended to Order Number
     * based on Enterprise and Document Type.
     *
     * @param enterprise Enterprise Code
     * @param docType Document Type
     * @return Suffix string or null if not found
     */
    private String getSuffix(String enterprise, String docType) {

        if (A_TO_DOCUMENT_TYPE.equals(docType)) {
            return TRANSFER_SUFFIX_MAP.get(enterprise);
        }

        if (VAL_DOCUMENT_TYPE_SALES_ORDER.equals(docType)) {
            return SALES_SUFFIX_MAP.get(enterprise);
        }

        // fallback to sales suffix
        return SALES_SUFFIX_MAP.get(enterprise);
    }

    /**
     * Builds the final Order Number using prefix, sequence number, and suffix.
     *
     * @param prefix Environment prefix
     * @param seqNo Generated sequence number
     * @param suffix Enterprise-specific suffix
     * @param docType Document type
     * @return Final formatted Order Number
     */
    private String buildOrderNo(String prefix, long seqNo, String suffix, String docType) {

        // For legacy flows where prefix is not required
        if (!VAL_DOCUMENT_TYPE_SALES_ORDER.equals(docType)
                && !A_TO_DOCUMENT_TYPE.equals(docType)) {
            return seqNo + suffix;
        }

        return prefix + seqNo + suffix;
    }

    /**
     * Fetches YFSContext from YFSEnvironment.
     *
     * @param env YFS Environment object
     * @return YFSContext instance
     * @throws YFSUserExitException if context retrieval fails
     */
    private static YFSContext getYfsContext(YFSEnvironment env) throws YFSUserExitException {
        try {
            return YFSContextManager.getInstance().getContextFor(env);
        } catch (Exception e) {
            throw new YFSUserExitException("Error getting YFSContext: " + e.getMessage());
        }
    }
}
