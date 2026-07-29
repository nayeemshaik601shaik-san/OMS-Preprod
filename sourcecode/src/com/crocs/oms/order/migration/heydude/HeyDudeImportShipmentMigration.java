package com.crocs.oms.order.migration.heydude;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsTemplateConstants;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.dom.YFCNodeList;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import org.w3c.dom.Document;

public class HeyDudeImportShipmentMigration implements CrocsConstant{

    private static YFCLogCategory logger = YFCLogCategory.instance(HeyDudeImportShipmentMigration.class.getName());


    /**
     * <p> Hover over method for more details <p>
     <p><b>Service Name:</b> HeyDudeGetSOShipMigrationAsyncServ</p>
     <p><b>Purpose:</b> This service will consume XML from <code>HEYDUDE_INT_IMPORT_SHIPMENT_QUEUE</code>
     and perform data validation before importing the order shipment.</p>

     <p><b>Mandatory Attributes:</b> DocumentType, EnteredBy, EnterpriseCode, OrderNo,
     PipelineKey, ItemID, ToAddress</p>

     *
     * @param env - YFSEnvironment
     * @param importShipmentInDoc - Document
     * @return - Output of importShipment Api.
     * @throws Exception
     */


    /*
     * Service Name: HeyDudeGetSOShipMigrationAsyncServ </p>
     * Purpose: This service will consume XML from shipment queue and perform data validation before importing the order
     * Mandatory Attributes: DocumentType, EnteredBy, EnterpriseCode, OrderNo, PipelineKey, ItemID, ToAddress
     */


    public Document importShipment(YFSEnvironment env, Document importShipmentInDoc) throws Exception {

        logger.beginTimer(HeyDudeMigarationUtil.logCurrentMethod(this.getClass()));
        try {
            YFCDocument importShipmentYdoc = YFCDocument.getDocumentFor(importShipmentInDoc);
            YFCElement importShipmentYdocEle = importShipmentYdoc.getDocumentElement();

            String enterpriseCode = importShipmentYdocEle.getAttribute(A_ENTERPRISE_CODE);
            String pipelineProperty = enterpriseCode + "_SHIPMENT_MIGRATION_PIPELINE_KEY";
            logger.verbose("pipelineProperty is: "+pipelineProperty);


            String importShipmentMandatoryAttr[] = {A_DOCUMENT_TYPE,
                    VAL_DOCUMENT_TYPE_SALES_ORDER, A_ENTERED_BY,
                    A_ENTERPRISE_CODE, A_ORDER_NO,pipelineProperty };

            boolean isValidForImportOrderProcesingFlag = isValidForImportShipmentProcesing(importShipmentYdocEle,
                    importShipmentMandatoryAttr);

            if (isValidForImportOrderProcesingFlag) {

                HeyDudeMigarationUtil.updateCarrierServiceCode(env,importShipmentYdocEle,importShipmentYdocEle.getAttribute(A_ENTERPRISE_CODE));
                importSOProcesingReadiness(env,importShipmentYdocEle);
            }

            logger.verbose("HeyDudeImportShipmentMigration :: importShipment :: Input Doc for importShipment: \n"
                    + importShipmentYdoc.toString());

        }catch(YFSException e) {
            throw new YFSException(e.getMessage(), e.getErrorCode(), e.getErrorDescription());
        }

        logger.endTimer(HeyDudeMigarationUtil.logCurrentMethod(this.getClass()));
        return importShipmentInDoc;
    }

    private void importSOProcesingReadiness(YFSEnvironment env, YFCElement importShipmentYdocEle) throws Exception {
        logger.verbose("HeyDudeImportShipmentMigration: Start of method importSOProcesingReadiness with importShipmentYdocEle: "+importShipmentYdocEle.toString());
    	try {
            String strOrderNo = importShipmentYdocEle.getAttribute(CrocsConstant.A_ORDER_NO);
            int shipmentLineNo = 1;
            String enterpriseCode = importShipmentYdocEle.getAttribute(A_ENTERPRISE_CODE);

            Document getOrderListOutput = CommonUtil.invokeAPI(env, CrocsTemplateConstants.TEMPLATE_GET_ORDER_LIST_ORDER_EVENT_UPDATES,
                    CrocsConstant.API_GET_ORDER_LIST,
                    SCXmlUtil.createFromString("<Order OrderNo='" + strOrderNo + "'/>"));
            
            String customerLocale = SCXmlUtil.getXpathAttribute(getOrderListOutput.getDocumentElement(), "/OrderList/Order/Extn/@ExtnCustomerLocale");

            if (!HeyDudeMigarationUtil.isValidSalesOrder(getOrderListOutput)) {
                throw new YFSException("Invalid Sales Order [" + strOrderNo + "] Provided for importShipment", "",
                        "Sales Order does not exist \n " + SCXmlUtil.getString(getOrderListOutput));
            } else {
                YFCNodeList<YFCElement> golOrderLines = YFCDocument.getDocumentFor(getOrderListOutput)
                        .getDocumentElement().getElementsByTagName(E_ORDER_LINE);

                YFCNodeList<YFCElement> shipmentLines = importShipmentYdocEle.getChildElement(E_SHIPMENT_LINES)
                        .getElementsByTagName(E_SHIPMENT_LINE);

                YFCElement containers = importShipmentYdocEle.createChild(E_CONTAINERS);
                YFCElement container =  containers.createChild(E_CONTAINER);
                YFCElement containerDetails = container.createChild(E_CONTAINER_DETAILS);

                for (YFCElement shipmentLine : shipmentLines) {
                    String shipmentLineitemId = shipmentLine.getAttribute(CrocsConstant.A_ITEM_ID);

                    for (YFCElement golOrderLine : golOrderLines) {
                        if (shipmentLineitemId.equals(golOrderLine.getChildElement(E_ITEM).getAttribute(CrocsConstant.A_ITEM_ID))) {

                            String strOrderLineKey = golOrderLine.getAttribute(A_ORDER_LINE_KEY);
                            shipmentLine.setAttribute(A_ORDER_LINE_KEY, strOrderLineKey);
                            String strOrderHeaderKey = golOrderLine.getAttribute(A_ORDER_HEADER_KEY);
                            shipmentLine.setAttribute(A_ORDER_HEADER_KEY, strOrderHeaderKey);

                            shipmentLine.setAttribute(A_SHIPMENT_LINE_NO, shipmentLineNo++);
                            shipmentLine.setAttribute(A_ORDER_NO, strOrderNo);
                            shipmentLine.setAttribute(A_UNIT_OF_MEASURE,"EACH");

                            YFCElement containerDetail = containerDetails.createChild(E_CONTAINER_DETAIL);

                            containerDetail.setAttribute(A_QUANTITY,shipmentLine.getAttribute(A_QUANTITY));

                            YFCElement containerShipmentLine = containerDetail.createChild(E_SHIPMENT_LINE);
                            containerShipmentLine.setAttribute(A_QUANTITY, shipmentLine.getAttribute(A_QUANTITY));
                            containerShipmentLine.setAttribute(A_SHIPMENT_LINE_NO, shipmentLine.getAttribute(A_SHIPMENT_LINE_NO));
                        }
                    }
                }
                
                /*
                 * Updating containers details 
                */

				String containerNo = importShipmentYdocEle.getAttribute(CrocsConstant.OrderNo);
				container.setAttribute(A_CONTAINER_NO, containerNo);
	        	// EOMS-8173 - Changes Start
	        	// Refactored the code & moved it to HeyDudeMigarationUtil for preparing the tracking URL
				String trackingData = importShipmentYdocEle.getAttribute(A_TRACKING_NO);

				String scac = "", strPrimaryUrl = "";
				if (!YFCCommon.isVoid(trackingData)) {

					if (trackingData.contains("|")) {
						
						String[] trackingNoSplits = trackingData.split("\\|");
						scac = trackingNoSplits[1].trim();
						String trackingNo = trackingNoSplits[0].trim();

						// get tracking url details 
						strPrimaryUrl = HeyDudeMigarationUtil.getTrackingURL(env, trackingData, enterpriseCode, customerLocale);

						importShipmentYdocEle.setAttribute(A_TRACKING_NO, trackingNo);
						container.setAttribute(A_TRACKING_NO, trackingNo);
						container.setAttribute(A_SCAC, scac);

						YFCElement extnContainer = container.createChild(E_EXTN);
						extnContainer.setAttribute(A_EXTN_TRACKING_URL, strPrimaryUrl);
					}
				}
	        	// EOMS-8173 - Changes End
			}

        } catch (YFSException e) {
            throw new YFSException(e.getMessage(), e.getErrorCode(), e.getErrorDescription());
        }
        logger.verbose("HeyDudeImportShipmentMigration: End of method importSOProcesingReadiness with updated importShipmentYdocEle: "+importShipmentYdocEle.toString());
    }

    /**
     * @param importShipmentYdocEle - YFCElement of Order
     */
    private boolean isValidForImportShipmentProcesing(YFCElement importShipmentYdocEle,
                                                      String[] importShipmentMandatoryAttr) {

        logger.beginTimer(HeyDudeMigarationUtil.logCurrentMethod(this.getClass()));

        boolean isValid = false;
        for (String ImportOrderMandatoryAttribute : importShipmentMandatoryAttr) {

            if (HeyDudeMigarationUtil.validateMandatoryAttribute(importShipmentYdocEle,
                    ImportOrderMandatoryAttribute))
                isValid = true;
            else
                break;
        }
        logger.endTimer(HeyDudeMigarationUtil.logCurrentMethod(this.getClass()));
        return isValid;
    }
}
