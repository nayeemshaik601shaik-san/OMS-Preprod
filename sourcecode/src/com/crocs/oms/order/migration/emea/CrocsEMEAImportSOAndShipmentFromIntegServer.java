package com.crocs.oms.order.migration.emea;

import java.rmi.RemoteException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.w3c.dom.Document;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.dom.YFCNode;
import com.yantra.yfc.dom.YFCNodeList;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class CrocsEMEAImportSOAndShipmentFromIntegServer implements CrocsConstant {
    private static YFCLogCategory logger = YFCLogCategory.instance(CrocsEMEAImportSOAndShipmentFromIntegServer.class.getName());


	/**
	 * Consumes and processes migrated Sales Order and Shipment XML messages for EMEA enterprises.
	 *
	 * <p>This service reads inbound XML messages from:
	 * <ul>
	 *   <li><b>CROCS_IN_IMPORT_SALES_ORDER_QUEUE</b> for Crocs EMEA enterprises.</li>
	 *   <li><b>HEYDUDE_IN_ORDER_MIGRATION_QUEUE</b> for HeyDude EMEA enterprises.</li>
	 * </ul>
	 *
	 * <p>Before importing the order and shipment, the service validates the inbound XML
	 * and ensures all mandatory attributes are present.
	 *
	 * <p><b>Mandatory Attributes:</b>
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
	 * <p><b>Input:</b> Migration Sales Order and Shipment XML.</p>
	 *
	 * <pre>{@code
	 * 	<?xml version="1.0" encoding="UTF-8" ?>
		<Order DocumentType="0001" EnterpriseCode="CROCS_EU" OrderDate="2026-06-30T16:09:53+00:00" OrderNo="802123453CEU" CustomerEMailID="MANSARI@CROCS.COM" CustomerFirstName="AHMED" CustomerLastName="ANSARI" CustomerPhoneNo="6591234567" SellerOrganizationCode="CROCS_EU" EnteredBy="Migration" PaymentRuleId="CROCS_EU_PAY_RULE" EntryType="WEB">
			<Extn ExtnSourceCodeGrpId="" ExtnCustId="00170001" ExtnFraudStatus="ACCEPT" ExtnForterStorage="" ExtnCustomerLocale="en_ES" ExtnIsCrocsClub="N"/>
			<PersonInfoShipTo LastName="ANSARI" FirstName="AHMED" AddressLine1="12 KURFURSTENDAMM" City="BERLIN" State="" Country="DE" ZipCode="10719" DayPhone="491701234567" EMailID="MANSARI@CROCS.COM" AddressLine2=""/>
			<PersonInfoBillTo LastName="ANSARI" FirstName="AHMED" AddressLine1="12 KURFURSTENDAMM" City="BERLIN" State="" Country="DE" ZipCode="10719" DayPhone="491701234567" EMailID="MANSARI@CROCS.COM" AddressLine2=""/>
			<OrderLines>
				<OrderLine DeliveryMethod="SHP" PrimeLineNo="1" SubLineNo="1" OrderedQty="1" CarrierServiceCode="IE-shipping-standard" FulfillmentType="CROCS_DC_FULFILLMENT" ConditionVariable1="Migration">
					<Extn ExtnHSTCode="64029990" ExtnMSRP="74.95" ExtnSAPGender="Unisex Adult" ExtnColorRMA="Black" ExtnSizeRMA="M5W7" ExtnSizeLabel=""/>
					<Item ItemID="10002-002-M20" ItemDesc="Classic Clog" ItemShortDesc="Classic Clog" UnitCost="4.86" UnitOfMeasure="EACH" TaxProductCode="64029990" CostCurrency="EUR"/>
					<LineCharges>
						<LineCharge ChargeCategory="PromotionDiscount" ChargeName="PromotionDiscount1" ChargePerLine="37.47">
							<Extn ExtnPromotionId="2021-11-05-thru-continuous_50pcoffFTWmax5_CrocsSGemployee_PR07" ExtnPromotionText="Employees 50% Off Retail Price* - Up to 10 FTW per order" ExtnDWPromotionId="012d1a262786f9e3b2a91054b8"/>
						</LineCharge>
					</LineCharges>
					<LinePriceInfo IsPriceLocked="Y" UnitPrice="74.95" ListPrice="74.95" RetailPrice="74.95" LineTotal="91.91" ActualPricingQty="1.0" OrderedPricingQty="1.0" TaxableFlag="Y"/>
					<OrderStatuses>
						<OrderStatus Status="3700" StatusQty="1.0">
							<Schedule ShipNode="2004"/>
						</OrderStatus>
					</OrderStatuses>
				</OrderLine>
				<OrderLine DeliveryMethod="SHP" PrimeLineNo="2" SubLineNo="1" OrderedQty="1" CarrierServiceCode="IE-shipping-standard" FulfillmentType="CROCS_DC_FULFILLMENT" ConditionVariable1="Migration">
					<Extn ExtnHSTCode="64029990" ExtnMSRP="54.95" ExtnSAPGender="Unisex Kids" ExtnColorRMA="Black" ExtnSizeRMA="C4" ExtnSizeLabel=""/>
					<Item ItemID="10002-002-M21" ItemDesc="Toddler Classic Clog" ItemShortDesc="Toddler Classic Clog" UnitCost="3.80" UnitOfMeasure="EACH" TaxProductCode="64029990" CostCurrency="EUR"/>
					<LineCharges>
						<LineCharge ChargeCategory="PromotionDiscount" ChargeName="PromotionDiscount1" ChargePerLine="27.47">
							<Extn ExtnPromotionId="2021-11-05-thru-continuous_50pcoffFTWmax5_CrocsSGemployee_PR07" ExtnPromotionText="Employees 50% Off Retail Price* - Up to 10 FTW per order" ExtnDWPromotionId="012d1a262786f9e3b2a91054b8"/>
						</LineCharge>
					</LineCharges>
					<LinePriceInfo IsPriceLocked="Y" UnitPrice="54.95" ListPrice="54.95" RetailPrice="54.95" LineTotal="91.91" ActualPricingQty="1.0" OrderedPricingQty="1.0" TaxableFlag="Y"/>
					<OrderStatuses>
						<OrderStatus Status="3700" StatusQty="1.0">
							<Schedule ShipNode="2004"/>
						</OrderStatus>
					</OrderStatuses>
				</OrderLine>
				<OrderLine DeliveryMethod="SHP" PrimeLineNo="3" SubLineNo="1" OrderedQty="1" CarrierServiceCode="IE-shipping-standard" FulfillmentType="CROCS_DC_FULFILLMENT" ConditionVariable1="Migration">
					<Extn ExtnHSTCode="71179020" ExtnMSRP="26.95" ExtnSAPGender="Unisex Adult" ExtnColorRMA="" ExtnSizeRMA="" ExtnSizeLabel=""/>
					<Item ItemID="10002-002-M22" ItemDesc="Baseball Team 5 Pack" ItemShortDesc="Baseball Team 5 Pack" UnitCost="1.14" UnitOfMeasure="EACH" TaxProductCode="71179020" CostCurrency="EUR"/>
					<LineCharges/>
					<LinePriceInfo IsPriceLocked="Y" UnitPrice="26.95" ListPrice="26.95" RetailPrice="26.95" LineTotal="91.91" ActualPricingQty="1.0" OrderedPricingQty="1.0" TaxableFlag="Y"/>
					<OrderStatuses>
						<OrderStatus Status="3700" StatusQty="1.0">
							<Schedule ShipNode="2004"/>
						</OrderStatus>
					</OrderStatuses>
				</OrderLine>
			</OrderLines>
			<PriceInfo Currency="EUR" EnterpriseCurrency="EUR" HeaderTax="0.00" TotalAmount="91.91" InvoicedAmount="91.91"/>
			<HeaderCharges>
				<HeaderCharge ChargeCategory="ShippingCharge" ChargeName="ShippingCharge" ChargeAmount="0.00"/>
			</HeaderCharges>
			<PaymentMethods>
				<PaymentMethod CreditCardName="AHMED ANSARI" UnlimitedCharges="N" MaxChargeLimit="91.91" PaymentReference2="CrocsEUR" PaymentReference5="R7L79FZMXZCCZ8W5" CreditCardNo="1111" CreditCardExpDate="3/2030" CreditCardType="VISA" PaymentType="Credit Card" DisplayCreditCardNo="1111" FirstName="" LastName="" PaymentReference1="" PaymentReference4="ADYEN">
					<PersonInfoBillTo LastName="ANSARI" FirstName="AHMED" AddressLine1="12 KURFURSTENDAMM" City="BERLIN" State="" Country="DE" ZipCode="10719" DayPhone="491701234567" EMailID="MANSARI@CROCS.COM" AddressLine2=""/>
					<PaymentDetailsList>
						<PaymentDetails TranType="ONLINE-ECOMM" RequestAmount="91.91" RequestProcessed="Y" HoldAgainstBook="Y" AuthorizationID="R7L79FZMXZCCZ8W5" AuthorizationExpirationDate="2026-07-05T15:00:10Z" ProcessedAmount="91.91" ChargeType="AUTHORIZATION" TranRequestTime="2026-06-30T16:09:55.000+0000"/>
					</PaymentDetailsList>
				</PaymentMethod>
			</PaymentMethods>
			<Shipment EnteredBy="Migration" ActualDeliveryDate="0" ActualShipmentDate="" BillToCustomerId="00170001" CarrierServiceCode="IE-shipping-standard" Currency="EUR" DeliveryTS="0" DocumentType="0001" EnterpriseCode="CROCS_EU" ExpectedDeliveryDate="0" ExpectedShipmentDate="2026-02-12T02:22:06.000+0000" OrderNo="802123453CEU" SellerOrganizationCode="CROCS_EU" ShipDate="2026-02-12" ShipNode="2004" ShipToCustomerId="00170001" TrackingNo="802123453CEUDPD|DPD" ShipmentNo="802123453CEU" Status="1400">
				<FromAddress LastName="ANSARI" FirstName="AHMED" AddressLine1="12 KURFURSTENDAMM" City="BERLIN" State="" Country="DE" ZipCode="10719" DayPhone="491701234567" EMailID="MANSARI@CROCS.COM" AddressLine2=""/>
				<ToAddress LastName="ANSARI" FirstName="AHMED" AddressLine1="12 KURFURSTENDAMM" City="BERLIN" State="" Country="DE" ZipCode="10719" DayPhone="491701234567" EMailID="MANSARI@CROCS.COM" AddressLine2=""/>
				<ShipmentLines>
					<ShipmentLine ItemDesc="Classic Clog" ItemID="10002-002-M20" OrderNo="802123453CEU" Quantity="1"/>
					<ShipmentLine ItemDesc="Toddler Classic Clog" ItemID="10002-002-M21" OrderNo="802123453CEU" Quantity="1"/>
					<ShipmentLine ItemDesc="Baseball Team 5 Pack" ItemID="10002-002-M22" OrderNo="802123453CEU" Quantity="1"/>
				</ShipmentLines>
			</Shipment>
		</Order>
	 * }</pre>
	 *
	 * @param env the Sterling OMS environment.
	 * @param importSOAndShipmentInDoc the input XML document containing the migrated
	 *        Sales Order and Shipment details.
	 * @return the output document returned by the Import Shipment API.
	 */
    public Document importSOAndShipment(YFSEnvironment env, Document importSOAndShipmentInDoc) {
		logger.verbose("Migration : OMS_UPDATE : importSOAndShipment : START ");
		logger.info("importSOAndShipment : importSOAndShipmentInDoc:  " + importSOAndShipmentInDoc);

        logger.beginTimer(CrocsEMEAMigarationUtil.logCurrentMethod(this.getClass()));
        try {
        	// purposefully updated it to true
            boolean isEligibleForShipmentProcessing = true; 

            YFCDocument orderYdoc = YFCDocument.createDocument(E_ORDER);
            YFCElement importShipmentOrderYdocEle = orderYdoc.getDocumentElement();

            YFCDocument importSalesOrderYdoc = YFCDocument.getDocumentFor(importSOAndShipmentInDoc);
            YFCElement importSalesOrderYdocEle = importSalesOrderYdoc.getDocumentElement();

            if (STR_STATUS_CANCELLED.equals(importSalesOrderYdocEle.getElementsByTagName(A_ORDER_STATUS).item(0).getAttribute(A_STATUS))) {
                Document crocsCreateSOMigrationSyncServOutDoc = CommonUtil.invokeService(env,"CrocsEMEACreateSOMigrationSyncServ", importSalesOrderYdoc.getDocument());

            } else {
                /**
                 * Handled Extn Tag for below 4 fields to be handled in importOrder call itself
                 * rather calling changeOrder later to store these attribute value on yfs_order_line
                 *
                 * ExtnTrackingUrl="",ExtnShipmentDate="",ExtnTrackingNo="", ExtnShipCarrier=""
                 */
                updateTrackingNoOnLine(env,importSalesOrderYdocEle);
        		logger.verbose("importSOAndShipment : importSalesOrderYdocEle:  " + importSalesOrderYdocEle);
                
                YFCNodeList<YFCNode> nodes = importSalesOrderYdocEle.getChildNodes();

                for (YFCNode node : nodes) {

                    if (E_SHIPMENT.equals(node.getNodeName())) {

                        YFCNode shipmentNode = orderYdoc.importNode(node, true);
                        orderYdoc.getFirstChild().appendChild(shipmentNode);
                        importSalesOrderYdocEle.removeChild(node);

                        if (E_ORDER.equals(importSalesOrderYdocEle.getTagName())) {

                            Document crocsCreateSOMigrationSyncServOutDoc = CommonUtil.invokeService(env, "CrocsEMEACreateSOMigrationSyncServ", importSalesOrderYdoc.getDocument());

                            if (importSalesOrderYdocEle.getAttribute(CrocsConstant.OrderNo).equals(crocsCreateSOMigrationSyncServOutDoc.getDocumentElement().getAttribute(CrocsConstant.OrderNo)))
                                isEligibleForShipmentProcessing = true;
                        }
                    }
                }

                if (isEligibleForShipmentProcessing) {

                    Document crocsPostSOShipMessageSyncServerOutDoc = CommonUtil.invokeService(
                    		env, 
                    		"CrocsEMEAPostSOShipMessageSyncServer",
                            YFCDocument.getDocumentFor(importShipmentOrderYdocEle.getChildElement(E_SHIPMENT).toString()).getDocument());
            		logger.verbose("importSOAndShipment : crocsPostSOShipMessageSyncServerOutDoc:  " + crocsPostSOShipMessageSyncServerOutDoc);
                    importSOAndShipmentInDoc = crocsPostSOShipMessageSyncServerOutDoc;
                }
            }

        } catch (YFSException e) {
			logger.info("Migration :OMS_UPDATE : importSOAndShipment Catch Block :" + e.getMessage());
            throw new YFSException(e.getMessage(), e.getErrorCode(), e.getErrorDescription());
        } catch (RemoteException e) {
			logger.info("Migration :OMS_UPDATE : importSOAndShipment Catch Block :" + e.getMessage());
            throw new YFSException(e.getMessage());
        }

		logger.info("importSOAndShipment : importSOAndShipmentInDoc:  " + importSOAndShipmentInDoc);
		logger.verbose("Migration : OMS_UPDATE : importSOAndShipment : END ");
        logger.endTimer(CrocsEMEAMigarationUtil.logCurrentMethod(this.getClass()));
        return importSOAndShipmentInDoc;
    }


    private void updateTrackingNoOnLine(YFSEnvironment env, YFCElement importSalesOrderYdocEle) {
		logger.verbose("Migration : OMS_UPDATE : updateTrackingNoOnLine : START ");
		logger.verbose("updateTrackingNoOnLine : importSalesOrderYdocEle:  " + importSalesOrderYdocEle);
        try {
        	
        	YFCElement eleShipment = importSalesOrderYdocEle.getChildElement(E_SHIPMENT);
        	
    		String trackingData = eleShipment.getAttribute(A_TRACKING_NO);
    		
    		String trackingNumber = "";
    		String scacCode = "";
    		String strPrimaryUrl = "";
    		
    		if(!YFCCommon.isVoid(trackingData) && trackingData.contains("|")) {
    			String[] trackingNoSplits = trackingData.split("\\|");

    	        trackingNumber = trackingNoSplits[0].trim();
    	        scacCode = trackingNoSplits[1].trim();
    	        
            	strPrimaryUrl = CrocsEMEAMigarationUtil.getTrackingURL(env, importSalesOrderYdocEle);
    		}

    		String expectedShipmentDate = eleShipment.getAttribute("ExpectedShipmentDate");
			YFCNodeList<YFCElement> orderLines = importSalesOrderYdocEle.getElementsByTagName(E_ORDER_LINE);
            YFCNodeList<YFCElement> shipmentLines = eleShipment.getElementsByTagName(E_SHIPMENT_LINE);

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
                        
						String shipmentDate = CommonUtil.convertInputStringDateIntoSFCCFormat(expectedShipmentDate);

                        orderLineExtn.setAttribute(A_EXTN_TRACKING_URL, strPrimaryUrl);
                        orderLineExtn.setAttribute(A_EXTN_SHIPMENT_DATE, shipmentDate);
                        orderLineExtn.setAttribute(EXTN_TRACKING_NO, trackingNumber);
                        orderLineExtn.setAttribute(EXTN_SHIP_CARRIER, scacCode);
                    }
                }
            }

        } catch (Exception e) {
            throw new YFSException(e.getMessage());
        }
		logger.verbose("updateTrackingNoOnLine : importSalesOrderYdocEle:  " + importSalesOrderYdocEle);
		logger.verbose("Migration : OMS_UPDATE : updateTrackingNoOnLine : END ");
    }
}