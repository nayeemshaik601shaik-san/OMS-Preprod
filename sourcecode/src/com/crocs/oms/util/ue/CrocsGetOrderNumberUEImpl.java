package com.crocs.oms.util.ue;

import com.crocs.oms.common.util.CrocsConstant;
import com.yantra.interop.util.YFSContextManager;
import com.yantra.shared.ycp.YFSContext;
import com.yantra.shared.ysc.util.YSCMultiColonyHelper;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.util.YFCConfigurator;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import com.yantra.yfs.japi.YFSUserExitException;
import com.yantra.yfs.japi.ue.YFSGetOrderNoUE;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * EOMS- 725 -This class is used to generate custom Order No when order
 * is created from Call center.
 */
public class CrocsGetOrderNumberUEImpl implements CrocsConstant, YFSGetOrderNoUE {
    private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsGetOrderNumberUEImpl.class);
    /**
     * this method returns the customOrderNo
     *
     * @param yfsEnvironment yfsEnvironment
     * @param map            map
     * @return Order No
     * @throws YFSUserExitException Exception
     */
    @Override
    public String getOrderNo(YFSEnvironment yfsEnvironment, Map map) throws YFSUserExitException {
        String customOrderNo = null;
        logger.verbose("values of map:" + map);
        String strEnterpriseCode = (String) (map.get(EnterpriseCode));
        String strDocumentType = (String) (map.get(DocumentType));
        logger.verbose("EntryType:" + "EnterpriseCode" + strEnterpriseCode + "DocumentType" + strDocumentType);

           try{
               YFSContext context = getYfsContext(yfsEnvironment);
               /*EOMS - 8487 Transfer Order No will follow the below format:
               * env + OrderNo + TOUS -> Example (QA/Preprod): S70072146TOUS
               * For Production, there will be no env prefix.Example (Prod): 700721TOUS */
               if (A_TO_DOCUMENT_TYPE.equals(strDocumentType)) {
                   long lOrderNo = YSCMultiColonyHelper.getNextDBSeqNo(
                           context, SEQ_CALL_CENTER_ORDER_NO, strEnterpriseCode);
                   String strEnv = YFCConfigurator.getInstance().getProperty(ORDER_NO_PREFIX);
                   // Resolve environment prefix
                   String envPrefix = "";
                   if (!YFCCommon.isVoid(strEnv)) {
                       if (ORDER_NO_PREFIX_QA.equals(strEnv)) {
                           envPrefix = "Q";
                       } else if (ORDER_NO_PREFIX_PREPROD.equals(strEnv)) {
                           envPrefix = "S";
                       }
                   }
                   //EOMS-13733-START
                   if (!YFCObject.isVoid(strEnterpriseCode) && strEnterpriseCode.contains("_")) {
                    String countryCode = strEnterpriseCode.substring(strEnterpriseCode.lastIndexOf('_') + 1);
                    customOrderNo = envPrefix + lOrderNo + TO_SUFFIX + countryCode;
                    logger.verbose("customOrderNo of Retail " + strEnterpriseCode + " : " + customOrderNo);
                    }
                    //EOMS-13733-END
               }
                else if (CROCS_US.equals(strEnterpriseCode)) {
                    long lOrderNo = YSCMultiColonyHelper.getNextDBSeqNo(context, SEQ_CALL_CENTER_ORDER_NO, strEnterpriseCode);
                    customOrderNo = lOrderNo + CUSTOM_US_NO;
                    logger.verbose("customOrderNo of CROCS_US :" + customOrderNo);
                }
                else if (CROCS_CA.equals(strEnterpriseCode)) {
                    long lOrderNo = YSCMultiColonyHelper.getNextDBSeqNo(context, SEQ_CALL_CENTER_ORDER_NO, strEnterpriseCode);
                    customOrderNo = lOrderNo + CUSTOM_CA_NO;
                    logger.verbose("customOrderNo of CROCS_CA:" + customOrderNo);
                }else if (HEYDUDE_US.equals(strEnterpriseCode)) {
                	//EOMS-6076 : HeyDude Create Order Implementation : START
                    long lOrderNo = YSCMultiColonyHelper.getNextDBSeqNo(context, SEQ_CALL_CENTER_ORDER_NO, strEnterpriseCode);
                    customOrderNo = lOrderNo + CUSTOM_HUS_NO;
                    logger.verbose("customOrderNo of HEYDUDE_US:" + customOrderNo);
                }else if (HEYDUDE_CA.equals(strEnterpriseCode)) {
                    long lOrderNo = YSCMultiColonyHelper.getNextDBSeqNo(context, SEQ_CALL_CENTER_ORDER_NO, strEnterpriseCode);
                    customOrderNo = lOrderNo + CUSTOM_HCA_NO;
                    logger.verbose("customOrderNo of HEYDUDE_CA:" + customOrderNo);
                }else if (HEYDUDE_AU.equals(strEnterpriseCode)) {
                    long lOrderNo = YSCMultiColonyHelper.getNextDBSeqNo(context, SEQ_CALL_CENTER_ORDER_NO, strEnterpriseCode);
                    customOrderNo = lOrderNo + CUSTOM_HAU_NO;
                    logger.verbose("customOrderNo of HEYDUDE_AU:" + customOrderNo);
                    //EOMS-6076 : HeyDude Create Order Implementation : END
                }
               else if (CROCS_AU.equals(strEnterpriseCode)) {
                   long lOrderNo = YSCMultiColonyHelper.getNextDBSeqNo(context, SEQ_CALL_CENTER_ORDER_NO, strEnterpriseCode);
                   customOrderNo = lOrderNo + CUSTOM_CAU_NO;
                   logger.info("customOrderNo of CROCS_AU created for Call center:" + customOrderNo);
               } 
               //EOMS-9713: EMEA Changes Start
               else if (CROCS_EMEA_ENTERPRISES.contains(strEnterpriseCode)){

                   long lOrderNo = YSCMultiColonyHelper.getNextDBSeqNo(context, SEQ_CALL_CENTER_ORDER_NO, strEnterpriseCode);
                   String suffixValue = strEnterpriseCode.startsWith("HEYDUDE") ? "H" : "C";
                   String countryCode = strEnterpriseCode.substring(strEnterpriseCode.lastIndexOf("_") + 1);
                   customOrderNo =lOrderNo +"O"+suffixValue +countryCode;
                   logger.info("customOrderNo of "+strEnterpriseCode +
                           " created for Call Center:"+customOrderNo);
               }
               //EOMS-9713: EMEA Changes End
			   //EOMS-10737 Start 
			   else if (CROCS_SG.equals(strEnterpriseCode)) {
                   long lOrderNo = YSCMultiColonyHelper.getNextDBSeqNo(context, SEQ_CALL_CENTER_ORDER_NO, strEnterpriseCode);
                   customOrderNo = lOrderNo + CUSTOM_CSG_NO;
                   logger.info("customOrderNo of CROCS_SG created for Call center:" + customOrderNo);
               }
			   //EOMS-10737 End
               
              /** EOMS-11734 - Generating OrderNo for CROCS_KR if not provided */
			   else if (CROCS_KR.equals(strEnterpriseCode)) {
                   long lOrderNo = YSCMultiColonyHelper.getNextDBSeqNo(context, SEQ_CALL_CENTER_ORDER_NO, strEnterpriseCode);
                   customOrderNo = lOrderNo + CUSTOM_CKR_NO;
                   logger.info("customOrderNo of CROCS_KR created for Call center:" + customOrderNo);
               }
			   //EOMS-11549 End - For Korea
        } catch (YFSException e) {
               logger.verbose("Error in generating OrderNo:" + e.getErrorDescription());
               throw new YFSUserExitException(" Error in generating getOrderNo :" + e.getErrorDescription());
           }
        return customOrderNo;
    }

    /**
     * This method is use to get YFSContext
     *
     * @param yfsEnvironment yfsEnvironment
     * @return YFSContext
     * @throws YFSUserExitException Exception
     */
    private static YFSContext getYfsContext(YFSEnvironment yfsEnvironment) throws YFSUserExitException {
        YFSContext context = null;
        try {
            context = YFSContextManager.getInstance().getContextFor(yfsEnvironment);
        } catch (Exception e) {
            logger.info("Error in YFSContext in getOrderNo:" + e.getMessage());
            throw new YFSUserExitException(" Error in YFSUserExitException in getOrderNo :" + e.getMessage());
        }
        return context;
    }
}
