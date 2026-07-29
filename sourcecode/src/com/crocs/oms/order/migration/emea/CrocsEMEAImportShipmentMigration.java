package com.crocs.oms.order.migration.emea;

import org.w3c.dom.Document;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsAPIConstants;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsTemplateConstants;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.order.migration.sg.CrocsSGMigarationUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.dom.YFCNodeList;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class CrocsEMEAImportShipmentMigration implements CrocsConstant{
	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsEMEAImportShipmentMigration.class.getName());


	/**
	 * Consumes Shipment migration XML messages for EMEA enterprises.
	 *
	 * <p>This service reads inbound XML messages from:
	 * <ul>
	 *   <li><b>CROCS_IMPORT_SHIPMENT_MIGRATION_QUEUE</b> for Crocs EMEA enterprises.</li>
	 *   <li><b>HEYDUDE_IMPORT_SHIPMENT_MIGRATION_QUEUE</b> for HeyDude EMEA enterprises.</li>
	 * </ul>
	 *
	 * <p>The service validates the inbound Shipment XML and ensures all mandatory
	 * attributes are present before importing the shipment.</p>
	 *
	 * <p><b>Mandatory Attributes:</b></p>
	 * <ul>
	 *   <li>DocumentType</li>
	 *   <li>EnteredBy</li>
	 *   <li>EnterpriseCode</li>
	 *   <li>OrderNo</li>
	 *   <li>PipelineKey</li>
	 *   <li>ItemID</li>
	 *   <li>ToAddress</li>
	 * </ul>
	 *
	 * <p><b>Input:</b> Migration Shipment XML.</p>
	 *
	 * <pre>{@code 
	 * 
	 * <Shipment EnteredBy="Migration" ActualDeliveryDate="0" ActualShipmentDate="" BillToCustomerId="00170001" 
	 * 		CarrierServiceCode="Standard" Currency="EUR" DeliveryTS="0" DocumentType="0001" EnterpriseCode="CROCS_EU" ExpectedDeliveryDate="0" 
	 * 		ExpectedShipmentDate="2026-02-12T02:22:06.000+0000" OrderNo="802123453CEU" SellerOrganizationCode="CROCS_EU" ShipDate="2026-02-12" 
	 * 		ShipNode="2004" ShipToCustomerId="00170001" TrackingNo="802123453CEUDPD|DPD" ShipmentNo="802123453CEU" Status="1400">
	 * 		<FromAddress LastName="ANSARI" FirstName="AHMED" AddressLine1="12 KURFURSTENDAMM" City="BERLIN" State="" Country="DE" ZipCode="10719" DayPhone="491701234567" EMailID="MANSARI@CROCS.COM" AddressLine2=""/>
	 * 		<ToAddress LastName="ANSARI" FirstName="AHMED" AddressLine1="12 KURFURSTENDAMM" City="BERLIN" State="" Country="DE" ZipCode="10719" DayPhone="491701234567" EMailID="MANSARI@CROCS.COM" AddressLine2=""/>
	 * 		<ShipmentLines>
	 * 			<ShipmentLine ItemDesc="Classic Clog" ItemID="10002-002-M20" OrderNo="802123453CEU" Quantity="1"/>
	 * 			<ShipmentLine ItemDesc="Toddler Classic Clog" ItemID="10002-002-M21" OrderNo="802123453CEU" Quantity="1"/>
	 * 			<ShipmentLine ItemDesc="Baseball Team 5 Pack" ItemID="10002-002-M22" OrderNo="802123453CEU" Quantity="1"/>
	 * 		</ShipmentLines>
	 * 	</Shipment>
	 * }</pre>
	 *
	 * @param env the Sterling OMS environment.
	 * @param importShipmentInDoc the input Shipment migration XML document.
	 * @return the output document returned by the Import Shipment API.
	 * @throws Exception if shipment validation or import fails.
	 */
    public Document importShipment(YFSEnvironment env, Document importShipmentInDoc) throws Exception {

        logger.beginTimer(CrocsEMEAMigarationUtil.logCurrentMethod(this.getClass()));
        logger.verbose("Migration : OMS_UPDATE : importShipment : START ");
		logger.info("Input Doc for importShipment: \n" + YFCDocument.getDocumentFor(importShipmentInDoc).toString());
		logger.info("Migration :OMS_UPDATE : importShipment: OrderNo: " + importShipmentInDoc.getDocumentElement().getAttribute(CrocsXmlConstants.A_ORDER_NO) 
				+" ShipmentNo"+importShipmentInDoc.getDocumentElement().getAttribute(CrocsXmlConstants.A_SHIPMENT_NO));
        
		try {
            YFCDocument importShipmentYdoc = YFCDocument.getDocumentFor(importShipmentInDoc);
            YFCElement importShipmentYdocEle = importShipmentYdoc.getDocumentElement();

            String enterpriseCode = importShipmentYdocEle.getAttribute(A_ENTERPRISE_CODE);
            String pipelineProperty =  enterpriseCode + "_SHIPMENT_MIGRATION_PIPELINE_KEY";
            
            logger.verbose("Migration :OMS_UPDATE: importShipment: pipelineProperty: " + pipelineProperty);
            
            String[] importShipmentMandatoryAttr = {
            		A_DOCUMENT_TYPE,
                    VAL_DOCUMENT_TYPE_SALES_ORDER, 
                    A_ENTERED_BY,
                    A_ENTERPRISE_CODE, 
                    A_ORDER_NO, 
                    pipelineProperty 
            };

            boolean isValidForImportOrderProcesingFlag = isValidForImportShipmentProcesing(importShipmentYdocEle, importShipmentMandatoryAttr);

            if (isValidForImportOrderProcesingFlag) {

            	CrocsEMEAMigarationUtil.updateCarrierServiceCode(env,importShipmentYdocEle,importShipmentYdocEle.getAttribute(A_ENTERPRISE_CODE));
            	
                importSOProcesingReadiness(env,importShipmentYdocEle);
            }

            logger.verbose("CrocsEMEAImportShipmentMigration :: importShipment :: Input Doc for importShipment: \n" + importShipmentYdoc.toString());

        }catch(YFSException e) {
			logger.info("Migration :OMS_UPDATE: importShipment Catch Block :" + e.getMessage() + " " + e.getErrorCode() + " " + e.getErrorDescription());
            throw new YFSException(e.getMessage(), e.getErrorCode(), e.getErrorDescription());
        }

        logger.endTimer(CrocsEMEAMigarationUtil.logCurrentMethod(this.getClass()));
		logger.verbose("Migration : OMS_UPDATE : importShipment : END ");
        return importShipmentInDoc;
    }

    private void importSOProcesingReadiness(YFSEnvironment env, YFCElement importShipmentYdocEle) throws Exception {
    	logger.verbose("Migration: OMS_UPDATE :  importSOProcesingReadiness: START: ");
		logger.info("Migration : OMS_UPDATE : Input Doc for importSOProcesingReadiness: Order No: " + importShipmentYdocEle.getAttribute(CrocsXmlConstants.A_ORDER_NO));

		try {
            String strOrderNo = importShipmentYdocEle.getAttribute(CrocsConstant.A_ORDER_NO);
            int shipmentLineNo = 1;
            Document getOrderListOutput = CommonUtil.invokeAPI(
            		env, 
            		CrocsTemplateConstants.TEMPLATE_GET_ORDER_LIST_ORDER_EVENT_UPDATES,
            		CrocsAPIConstants.API_GET_ORDER_LIST,
                    SCXmlUtil.createFromString("<Order OrderNo='" + strOrderNo + "'/>"));
            
            if (!CrocsEMEAMigarationUtil.isValidSalesOrder(getOrderListOutput)) {
                throw new YFSException("Invalid Sales Order [" + strOrderNo + "] Provided for importShipment", 
                		"",
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
                            shipmentLine.setAttribute(A_UNIT_OF_MEASURE, S_EACH);

                            YFCElement containerDetail = containerDetails.createChild(E_CONTAINER_DETAIL);

                            containerDetail.setAttribute(A_QUANTITY,shipmentLine.getAttribute(A_QUANTITY));

                            YFCElement containerShipmentLine = containerDetail.createChild(E_SHIPMENT_LINE);
                            containerShipmentLine.setAttribute(A_QUANTITY, shipmentLine.getAttribute(A_QUANTITY));
                            containerShipmentLine.setAttribute(A_SHIPMENT_LINE_NO, shipmentLine.getAttribute(A_SHIPMENT_LINE_NO));
                        }
                    }
                }
                
                // updating the container details                
                String containerNo = importShipmentYdocEle.getAttribute(CrocsConstant.OrderNo);
				container.setAttribute(CrocsXmlConstants.A_CONTAINER_NO,containerNo);
				
				String trackingData = importShipmentYdocEle.getAttribute(A_TRACKING_NO);
								
				if (!YFCCommon.isVoid(trackingData) && trackingData.contains("|")) {
						String[] trackingNoSplits = trackingData.split("\\|");
				        String trackingNumber = trackingNoSplits[0].trim();

						importShipmentYdocEle.setAttribute(A_TRACKING_NO, trackingNumber);
						container.setAttribute(A_TRACKING_NO, trackingNumber);
						container.setAttribute(A_SCAC, importShipmentYdocEle.getAttribute(A_SCAC));
						
						YFCElement extnContainer = container.createChild(E_EXTN);
						extnContainer.setAttribute(A_EXTN_TRACKING_URL, importShipmentYdocEle.getAttribute(A_TRACKING_URL));
						
						// since tracking url is not needed at Shipment level removing it
						importShipmentYdocEle.removeAttribute(A_TRACKING_URL);
						importShipmentYdocEle.removeAttribute(A_SCAC);
					}				
			}
			logger.verbose("Migration: OMS_UPDATE :  importSOProcesingReadiness: END: ");
			logger.info("Migration: OMS_UPDATE :  importSOProcesingReadiness: : importShipmentYdocEle"+ importShipmentYdocEle);

        } catch (YFSException e) {
			logger.info("Migration :OMS_UPDATE: importSOProcesingReadiness Catch Block :" + e.getMessage() + " " + e.getErrorCode() + " " + e.getErrorDescription());
            throw new YFSException(e.getMessage(), e.getErrorCode(), e.getErrorDescription());
        }
    }

    /**
     * @param importShipmentYdocEle - YFCElement of Order
     */
    private boolean isValidForImportShipmentProcesing(YFCElement importShipmentYdocEle, String[] importShipmentMandatoryAttr) {

        logger.beginTimer(CrocsEMEAMigarationUtil.logCurrentMethod(this.getClass()));
		logger.verbose("Migration : OMS_UPDATE : isValidForImportShipmentProcesing: START: ");

        boolean isValid = false;
        for (String ImportOrderMandatoryAttribute : importShipmentMandatoryAttr) {
        	
            if (CrocsEMEAMigarationUtil.validateMandatoryAttribute(importShipmentYdocEle, ImportOrderMandatoryAttribute))
                isValid = true;
            else
                break;
        }
		logger.verbose("Migration :OMS_UPDATE : isValidForImportShipmentProcesing: END: ");
        logger.endTimer(CrocsEMEAMigarationUtil.logCurrentMethod(this.getClass()));
        return isValid;
    }
}
