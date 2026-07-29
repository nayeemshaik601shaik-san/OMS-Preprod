package com.crocs.oms.order.migration.heydude;

import com.crocs.oms.common.util.CrocsConstant;
import java.rmi.RemoteException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.w3c.dom.Document;
import com.crocs.oms.common.util.CommonUtil;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.dom.YFCNode;
import com.yantra.yfc.dom.YFCNodeList;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class HeyDudeImportSOAndShipmentFromIntegServer implements CrocsConstant {
    private static YFCLogCategory logger = YFCLogCategory.instance(HeyDudeImportSOAndShipmentFromIntegServer.class.getName());


    /**
     * <p> EOMS-6121 Service: HeyDudeGetImportSOFromQSyncService <p>
     *<p> Purpose: This service will consume XML from queue HEYDUDE_IN_ORDER_MIGRATION_QUEUE</code>
     * and perform data validation before importing the order and shipment.</p>

     <p> Mandatory Attributes: DocumentType, EnteredBy, EnterpriseCode, OrderNo,
     PipelineKey, ItemID, ToAddress</p>

     <p><b>Input:</b> Migration Sales Order Shipment sample XML</p>
     *
     * @param env - YFSEnvironment
     * @param importSOAndShipmentInDoc - Document
     * @return - Output of importShipment Api.
     */

    public Document importSOAndShipment(YFSEnvironment env, Document importSOAndShipmentInDoc) {

        logger.beginTimer(HeyDudeMigarationUtil.logCurrentMethod(this.getClass()));
        try {
            boolean isEligibleForShipmentProcessing = false;

            YFCDocument orderYdoc = YFCDocument.createDocument(CrocsConstant.E_ORDER);
            YFCElement importShipmentOrderYdocEle = orderYdoc.getDocumentElement();

            YFCDocument importSalesOrderYdoc = YFCDocument.getDocumentFor(importSOAndShipmentInDoc);
            YFCElement importSalesOrderYdocEle = importSalesOrderYdoc.getDocumentElement();

            if (STR_STATUS_CANCELLED.equals(
                    importSalesOrderYdocEle.getElementsByTagName(A_ORDER_STATUS).item(0)
                            .getAttribute(A_STATUS))) {
                Document crocsCreateSOMigrationSyncServOutDoc = CommonUtil.invokeService(env,SERVICE_HEYDUDE_CREATE_SO_MIGRATION, importSalesOrderYdoc.getDocument());

            } else {
                /**
                 * Handline Extn Tag for below 4 fields to be handled in importOrder call itself
                 * rather calling changeOrder later to store these attribute value on yfs_order_line
                 *
                 * ExtnTrackingUrl="",ExtnShipmentDate="",ExtnTrackingNo="", ExtnShipCarrier=""
                 */

                updateTrackingNoOnLine(env,importSalesOrderYdocEle);
                YFCNodeList<YFCNode> nodes = importSalesOrderYdocEle.getChildNodes();

                for (YFCNode node : nodes) {

                    if (E_SHIPMENT.equals(node.getNodeName())) {

                        YFCNode ShipmentNode = orderYdoc.importNode(node, true);
                        orderYdoc.getFirstChild().appendChild(ShipmentNode);
                        importSalesOrderYdocEle.removeChild(node);

                        if (E_ORDER.equals(importSalesOrderYdocEle.getTagName())) {

                            Document crocsCreateSOMigrationSyncServOutDoc = CommonUtil.invokeService(env,
                                    SERVICE_HEYDUDE_CREATE_SO_MIGRATION, importSalesOrderYdoc.getDocument());

                            if (importSalesOrderYdocEle.getAttribute(CrocsConstant.OrderNo)
                                    .equals(crocsCreateSOMigrationSyncServOutDoc.getDocumentElement()
                                            .getAttribute(CrocsConstant.OrderNo)))
                                isEligibleForShipmentProcessing = true;
                        }
                    }
                }

                if (isEligibleForShipmentProcessing) {

                    Document crocsPostSOShipMessageSyncServerOutDoc = CommonUtil.invokeService(env,
                            SERVICE_HEYDUDE_POST_SO_SHIP_MESSAGE_SYNC,
                            YFCDocument.getDocumentFor(
                                            importShipmentOrderYdocEle.getChildElement(E_SHIPMENT).toString())
                                    .getDocument());
                    importSOAndShipmentInDoc = crocsPostSOShipMessageSyncServerOutDoc;
                }
            }

            logger.verbose("CrocsImportShipmentMigration :: importShipment :: Input Doc for importShipment: \n"
                    + importSalesOrderYdoc.toString());

        } catch (YFSException e) {
            throw new YFSException(e.getMessage(), e.getErrorCode(), e.getErrorDescription());
        } catch (RemoteException e) {
            throw new YFSException(e.getMessage());
        }

        logger.endTimer(HeyDudeMigarationUtil.logCurrentMethod(this.getClass()));
        return importSOAndShipmentInDoc;
    }


    private void updateTrackingNoOnLine(YFSEnvironment env, YFCElement importSalesOrderYdocEle) {

        logger.verbose("updateTrackingNoOnLine is getting started "+importSalesOrderYdocEle);
        try {
        	// EOMS-8173 - Changes Start
        	// Refactored the code & moved it to HeyDudeMigarationUtil for preparing the tracking URL
			String customerLocale = "", scac = "", strPrimaryUrl = "", trackingNo = "";
			YFCElement orderExtnEle = importSalesOrderYdocEle.getChildElement(E_EXTN);
			if (orderExtnEle != null) {
				customerLocale = orderExtnEle.getAttribute(EXTN_CUSTOMER_LOCALE);
			}
			String enterpriseCode = importSalesOrderYdocEle.getAttribute(A_ENTERPRISE_CODE);

			String trackingData = importSalesOrderYdocEle.getChildElement(E_SHIPMENT).getAttribute(A_TRACKING_NO);
			
			if (!YFCCommon.isVoid(trackingData)) {
				if (trackingData.contains("|")) {

					String[] trackingNoSplits = trackingData.split("\\|");
					scac = trackingNoSplits[1].trim();
					trackingNo = trackingNoSplits[0].trim();

					// get tracking url details
					strPrimaryUrl = HeyDudeMigarationUtil.getTrackingURL(env, trackingData, enterpriseCode, customerLocale);
				}
			}
        	//EOMS-8173 - Changes End

            OffsetDateTime currentDate = OffsetDateTime.now(ZoneOffset.UTC);
            String systemDate = currentDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            
          //EOMS-7977-START
			String expectedShipmentDate=importSalesOrderYdocEle.getChildElement("Shipment").getAttribute("ExpectedShipmentDate");
			String orderDate=importSalesOrderYdocEle.getAttribute("OrderDate");
			//EOMS-7977-END

            YFCNodeList<YFCElement> orderLines = importSalesOrderYdocEle.getElementsByTagName(E_ORDER_LINE);
            YFCNodeList<YFCElement> shipmentLines = importSalesOrderYdocEle.getChildElement(E_SHIPMENT).getElementsByTagName(E_SHIPMENT_LINE);

            for (YFCElement shipmentLine : shipmentLines) {

                String shipmentLineitemId = shipmentLine.getAttribute(A_ITEM_ID);

                for (YFCElement orderLine : orderLines) {
                    if (shipmentLineitemId.equals(orderLine.getChildElement(E_ITEM).getAttribute(A_ITEM_ID))) {
                        /** if Extn is not in orderLine tag , it will create it else it will get and add in existing tag **/

                        YFCElement orderLineExtn = null;
                        if (!YFCCommon.isVoid(orderLine.getChildElement(E_EXTN)))
                            orderLineExtn = orderLine.getChildElement(E_EXTN);
                        else
                            orderLineExtn = orderLine.createChild(E_EXTN);
                        
                      //EOMS-7977- START
						if(YFCCommon.isVoid(expectedShipmentDate)){
							String carrierServiceCode=orderLine.getAttribute(A_CARRIER_SERVICE_CODE);
							OffsetDateTime odt = OffsetDateTime.parse(orderDate);

					        // Common formatter
					        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

							OffsetDateTime newExpectedShipmentDate=null;
							if("expedited".equalsIgnoreCase(carrierServiceCode))
								newExpectedShipmentDate = odt.plusDays(2);
							else
								 newExpectedShipmentDate = odt.plusDays(4);
							
							expectedShipmentDate=newExpectedShipmentDate.format(formatter);
						}
						//EOMS-7977- END
						String shipmentDate=CommonUtil.convertInputStringDateIntoSFCCFormat(expectedShipmentDate);

                        orderLineExtn.setAttribute(A_EXTN_TRACKING_URL, strPrimaryUrl);
                        orderLineExtn.setAttribute(A_EXTN_SHIPMENT_DATE, shipmentDate);
                        orderLineExtn.setAttribute(EXTN_TRACKING_NO, trackingNo);
                        orderLineExtn.setAttribute(EXTN_SHIP_CARRIER, scac);
                    }
                    logger.verbose("updateTrackingNoOnLine is getting updated: " +importSalesOrderYdocEle);
                }
            }

        } catch (Exception e) {
            throw new YFSException(e.getMessage());
        }
    }
}
