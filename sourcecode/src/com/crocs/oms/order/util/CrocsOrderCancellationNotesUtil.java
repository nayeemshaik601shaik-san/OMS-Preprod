package com.crocs.oms.order.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

/**
 * EOMS-10220 Cancellation Reasons and Export to Snowflake
 * 
 * This is a utility class for mapping cancellation reason code.
 * This class constructs Notes Document and returns it
 * 
 * @author Kiran Thallapally
 * @version 1.0
 */

public class CrocsOrderCancellationNotesUtil {

    private static YFCLogCategory logger = YFCLogCategory.instance(CrocsOrderCancellationNotesUtil.class);

    public String getCancellationReason(YFSEnvironment env, String cancellationInitiatedFrom) {

        logger.verbose("Cancellation Initiated from the class : " + cancellationInitiatedFrom);
		
        String codeValue;
        String cancellationReason;
        try {
            switch (cancellationInitiatedFrom) {
                case "CrocsOrderUpdate":
                    codeValue = CrocsConstant.WEBSITE_CANCEL_REASON;
                    break;
                case "CrocsCancelQtyOnBackOrder":
                    codeValue = CrocsConstant.BACKORDER_CANCEL_REASON;
                    break;
                case "CrocsOrderCancelNotes_SHORT_SHIP":
                    codeValue = CrocsConstant.SHORT_SHIP_REASON;
                    break;
                case "CrocsModifyExchangeOrder_Line":
                    codeValue = CrocsConstant.EXCHANGE_RESERV_FAIL_REASON;
                    break;
                case "CrocsModifyExchangeOrder":
                    codeValue = CrocsConstant.EXCHANGE_CANCEL_REASON;
                    break;
                case "CrocsCancelReturnOrderLines":
                    codeValue = CrocsConstant.RETURN_CANCEL_REASON;
                    break;
                case "CrocsOrderCancelNotes_SCH_REL":
                    codeValue = CrocsConstant.SCH_REL_FAILURE_REASON;
                    break;
                case "CrocsProcessAdyenWebhooks_AUTH":
                    codeValue = CrocsConstant.POST_AUTH_FAILURE_REASON;
                    break;
                case "CrocsProcessAdyenWebhooks_FORTER":
                    codeValue = CrocsConstant.FRAUD_CHECK_DECLINE_REASON;
                    break;
                default:
                    codeValue = CrocsConstant.OTHER;
                    logger.info("there is no identifier for cancellationInitiatedFrom  : " + cancellationInitiatedFrom
                            + " , hence returning default reason");
                    break;
            }

            logger.verbose("CrocsOrderCancellationNotesUtil : getCancellationReason ");

            Document docGetCommonCodeListOutput = CommonUtil.getCommonCodeList(env, CrocsConstant.CROCS,
                    CrocsConstant.CROCS_CANCEL_REASONS, codeValue);
            logger.verbose("CrocsOrderCancellationNotesUtil : getCancellationReason : getCommonCodeList Output XML: "
                    + SCXmlUtil.getString(docGetCommonCodeListOutput));
            cancellationReason = SCXmlUtil.getXpathAttribute(docGetCommonCodeListOutput.getDocumentElement(),
                    "/CommonCodeList/CommonCode[@CodeValue='" + codeValue + "']/@CodeShortDescription");

            if (YFCCommon.isVoid(cancellationReason)) {
                logger.info("Missing common code short description for the code value " + codeValue
                        + " , hence returning default reason");
                cancellationReason = "Other";
            }
        } catch (YFSException e) {
            logger.error("Error in getCancellationReason : " + e.getMessage());
            throw new YFSException(
                    "Error occured in CrocsOrderCancellationNotesUtil.getCancellationReason while getting cancellation reason for the source where cancellation is intiated from ",
                    "EXTN_006", "Error occured in getting cancellation reason");
        }
        logger.verbose ("CrocsOrderCancellationNotesUtil : Cancellation Reason  : " + cancellationReason);
        return cancellationReason;
    }

    public Document getNotesTag(Element eleParent, Document inDoc, String noteText, String reasonCode) {

        logger.verbose("CrocsOrderCancellationNotesUtil : getNotesTag : Input Xml : " + SCXmlUtil.getString(inDoc));
        logger.verbose(
                "CrocsOrderCancellationNotesUtil : getNotesTag : Input Element : " + SCXmlUtil.getString(eleParent));
        logger.verbose("CrocsOrderCancellationNotesUtil : getNotesTag : NoteText : " + noteText);
        try {
            Document docNotes = SCXmlUtil.createDocument(CrocsXmlConstants.E_NOTES);
            Element eleNotes = docNotes.getDocumentElement();
            Element eleNote = SCXmlUtil.createChild(eleNotes, CrocsXmlConstants.E_NOTE);
            eleNote.setAttribute(CrocsXmlConstants.A_REASON_CODE,reasonCode);
            eleNote.setAttribute(CrocsXmlConstants.A_NOTE_TEXT, noteText);
            eleParent.appendChild(inDoc.importNode(docNotes.getDocumentElement(), CrocsConstant.A_TRUE));
        }
        catch (Exception e) {
            logger.error("Error in getCancellationReason : " + e.getMessage());
            throw new YFSException(
                    "Error occured in CrocsOrderCancellationNotesUtil.getNotesTag while creating Notes Tag for adding cancellation notes ",
                    "EXTN_008", "Error occured in creating notes tag");
        }
		logger.verbose("CrocsOrderCancellationNotesUtil : getNotesTag : Output Xml : " + SCXmlUtil.getString(inDoc));
        return inDoc;
    }

}