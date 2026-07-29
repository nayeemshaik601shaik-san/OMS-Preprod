package com.crocs.oms.order.migration.au;
 
import java.rmi.RemoteException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.crocs.oms.common.util.CommonUtil;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.shipment.CrocsShipmentPackedToShippedStatusFromWMS;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.dom.YFCNode;
import com.yantra.yfc.dom.YFCNodeList;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class CrocsAUImportSOAndShipmentFromIntegServer implements CrocsConstant {
	private static YFCLogCategory logger = YFCLogCategory.instance(CrocsAUImportSOAndShipmentFromIntegServer.class.getName());

	
	/** Sample Xml for import SO and Shipment */
	/**
	 * <p> Hover over method for more details <p>
	  <p><b>Service Name:</b> CrocsImportShipmentAsync</p>
      <p><b>Purpose:</b> This service will consume XML from <code>CROCS_IMPORT_SHIPMENT_MIGRATION_QUEUE</code> 
      and perform data validation before importing the order shipment.</p>
      
      <p><b>Mandatory Attributes:</b> DocumentType, EnteredBy, EnterpriseCode, OrderNo, 
       PipelineKey, ItemID, ToAddress</p>
      
      <p><b>Input:</b> Migration Sales Order Shipment sample XML</p>
      <pre>{@code
	    
	    <?xml version="1.0" encoding="UTF-8"?>
		<Order DocumentType="0001" EnteredBy="Migration" EnterpriseCode="CROCS_AU" OrderDate="2025-04-03T07:01:02.000Z" OrderNo="AULOC000015" EntryType="WEB" CustomerEMailID="PBAGLA@CROCS.COM" CustomerFirstName="PRIYANKA" CustomerLastName="BAGLA" CustomerPhoneNo="(965)221-0880" PaymentStatus="" PaymentRuleId="CROCS_AU_PAY_RULE" SellerOrganizationCode="CROCS_AU">
	<PriceInfo Currency="AUD" EnterpriseCurrency="AUD"  TotalAmount="69.13" InvoicedAmount="69.13"/>
	<Extn ExtnSourceCodeGrpId="CrocsClub" ExtnIsCrocsClub="N" ExtnCustId="cust123" ExtnFraudStatus="Y"/>
	<PersonInfoShipTo LastName="BAGLA" FirstName="PRIYANKA" AddressLine1="123 S COLLINGWOOD ST" City="PRETTY PRAIRIE" State="KS" Country="AU" ZipCode="2260" DayPhone="(333) 333-3333" EMailID="PBAGLA@CROCS.COM"/>
	<PersonInfoBillTo LastName="BAGLA" FirstName="PRIYANKA" AddressLine1="123 S COLLINGWOOD ST" City="PRETTY PRAIRIE" State="KS" Country="AU" ZipCode="2260" DayPhone="(333) 333-3333" EMailID="PBAGLA@CROCS.COM"/>
	<HeaderCharges>
		<HeaderCharge ChargeCategory="ShippingCharge" ChargeName="ShippingCharge" ChargeAmount="6.99"/>
	</HeaderCharges>
	<OrderLines>
		<OrderLine ConditionVariable1="Migration" DeliveryMethod="SHP" FulfillmentType="CROCS_DC_FULFILLMENT" PrimeLineNo="1" SubLineNo="1" OrderedQty="2.00">
			<Item CostCurrency="AUD" ItemID="10006-001-M20" ItemDesc="original, classic clog, classic, crocs, jjjjound, baya" ItemShortDesc="original, classic clog, classic, crocs, jjjjound, baya" UnitCost="4.41" UnitOfMeasure="EACH" TaxProductCode=""/>
			<LinePriceInfo IsPriceLocked="Y" UnitPrice="21.0" ListPrice="21.0" RetailPrice="21.0" LineTotal="37.48" ActualPricingQty="2" OrderedPricingQty="2" Tax="0.48" TaxableFlag="Y"/>
			<LineCharges>
				<LineCharge ChargeCategory="PromotionDiscount" ChargeName="PromotionDiscount1" ChargePerLine="5.0" ChargeAmount="5.0">
					<Extn ExtnPromotionId="2017-01-01-thru-2019-01-31_5off_TestProductAmountClass_PR99_PATEST" ExtnDWPromotionId="c6b336051bbe7c21e85d0154a5" ExtnPromotionText="$5 off Footwear Product"/>
				</LineCharge>
			</LineCharges>
			<OrderStatuses>
				<OrderStatus Status="3700" StatusQty="2.0">
					<Schedule ShipNode="3011"/>
				</OrderStatus>
			</OrderStatuses>
		</OrderLine>
		<OrderLine ConditionVariable1="Migration" DeliveryMethod="SHP" FulfillmentType="CROCS_DC_FULFILLMENT" PrimeLineNo="2" SubLineNo="1" OrderedQty="1.00">
			<Item CostCurrency="AUD" ItemID="10006-001-M44" ItemDesc="original, classic clog, classic, crocs, jjjjound, baya" ItemShortDesc="original, classic clog, classic, crocs, jjjjound, baya" UnitCost="4.41" UnitOfMeasure="EACH" TaxProductCode=""/>
			<LinePriceInfo IsPriceLocked="Y" UnitPrice="25.0" ListPrice="25.0" RetailPrice="25.0" LineTotal="24.07" ActualPricingQty="1" OrderedPricingQty="1" Tax="3.57" TaxableFlag="Y"/>
			<LineCharges>
				<LineCharge ChargeCategory="PromotionDiscount" ChargeName="PromotionDiscount2" ChargePerLine="4.5" ChargeAmount="4.5">
					<Extn ExtnPromotionId="2017-01-01-thru-2019-01-31_10peroff_TestProductPercentClass_PR99_PPTEST" ExtnDWPromotionId="06b9028097c1edb79456adef70" ExtnPromotionText="10% off any product"/>
				</LineCharge>
			</LineCharges>
			<OrderStatuses>
				<OrderStatus Status="3700" StatusQty="1.0">
					<Schedule ShipNode="3011"/>
				</OrderStatus>
			</OrderStatuses>
		</OrderLine>
	</OrderLines>
	<PaymentMethods>
		<PaymentMethod CreditCardExpDate="10/2029" CreditCardName="CREDIT_CARD" CreditCardNo="8946123" CreditCardType="Master" PaymentType="Credit Card" DisplayCreditCardNo="8946" FirstName="RAJ123" LastName="ALEX123" MaxChargeLimit="69.13" DisplaySvcNo="123" SvcNo="678" PaymentReference1="Ref123" PaymentReference2="CrocsAU" PaymentReference4="ADYEN" UnlimitedCharges="N">
			<PaymentDetailsList>
				<PaymentDetails AuthorizationExpirationDate="2025-05-30T12:46:12+00:00" AuthorizationID="23123" AuditTransactionId="txn123" TranType="ONLINE-ECOMM" ChargeType="CHARGE" RequestAmount="69.13" RequestProcessed="Y" ProcessedAmount="69.13" TranRequestTime="2025-03-03T12:46:12+00:00" HoldAgainstBook="Y"/>
			</PaymentDetailsList>
			<PersonInfoBillTo LastName="BAGLA" FirstName="PRIYANKA" AddressLine1="123 S COLLINGWOOD ST" City="PRETTY PRAIRIE" State="KS" Country="AU" ZipCode="2260" DayPhone="(333) 333-3333" EMailID="PBAGLA@CROCS.COM"/>
		</PaymentMethod>
	</PaymentMethods>
	<Shipment EnteredBy="Migration" ActualDeliveryDate="" ActualShipmentDate="2025-04-05T08:15:47.000+0000" BillToCustomerId="" BuyerOrganizationCode="" CarrierServiceCode="AUSPOST" Currency="USD" DeliveryTS="" DocumentType="0001" EnterpriseCode="CROCS_AU" ExpectedDeliveryDate="" ExpectedShipmentDate="2025-04-05T08:15:47.000+0000" History="N" OrderNo="AULOC000015" ShipDate="2025-04-05" ShipNode="3011" ShipToCustomerId="" PipelineKey="202504031215582621376" ReceivingNode="" ReleaseNo="" SellerOrganizationCode="CROCS_AU" ShipmentKey="" ShipmentNo="AULOC000015_1" Status="1400" TrackingNo="1Z6F857YYW260869975|STP">
		<FromAddress AddressLine1="484 NW 53rd Ave" AddressLine2="" City="Miami" Country="US" EMailID="nannasolano@gmail.com" FirstName="PRIYANKA" LastName="BAGLA" MobilePhone="(178) 626-2445" State="FL" ZipCode="33126-5040"/>
		<ToAddress AddressLine1="123 S COLLINGWOOD ST" AddressLine2="" City="PRETTY PRAIRIE" Country="US" EMailID="PRIYANKA.BAGLA@IBM.COM" FirstName="PRIYANKA" LastName="BAGLA" OtherPhone="(333) 333-3333" PersonID="" State="KS" ZipCode="67570-8923"/>
		<ShipmentLines>
			<ShipmentLine ItemID="10006-001-M20" ItemDesc="original, classic clog, classic, crocs, jjjjound, baya" NetWeight="50" NetWeightUom="LB" OrderHeaderKey="" OrderLineKey="" OrderNo="AULOC000015" ShipmentLineNo="1" Quantity="2" UnitOfMeasure="EACH"> 
</ShipmentLine>
			<ShipmentLine ItemID="10006-001-M44" ItemDesc="original, classic clog, classic, crocs, jjjjound, baya" NetWeight="50" NetWeightUom="LB" OrderHeaderKey="" OrderLineKey="" OrderNo="AULOC000015" ShipmentLineNo="2" Quantity="1" UnitOfMeasure="EACH"> 
</ShipmentLine>
		</ShipmentLines>
	</Shipment>
</Order>

     * }
	 * @param env - YFSEnvironment
	 * @param importShipmentInDoc - Document
	 * @return - Output of importShipment Api.
	 * @throws Exception 
	 */
	
	
    /*
     * Service Name: CrocsSalesOrderImportAsync</p>
     * Purpose: This service will consume XML from CROCS_IMPORT_SHIPMENT_MIGRATION_QUEUE and perform data validation before importing the order
     * Mandatory Attributes: DocumentType, EnteredBy, EnterpriseCode, OrderNo, PipelineKey, ItemID, ToAddress
     */
       
	
	public Document importSOAndShipment(YFSEnvironment env, Document importSOAndShipmentInDoc) {

		logger.beginTimer(CrocsAUMigarationUtil.logCurrentMethod(this.getClass()));
		try {
			boolean isEligibleForShipmentProcessing = false;
			
			YFCDocument orderYdoc = YFCDocument.createDocument(CrocsConstant.E_ORDER);
			YFCElement importShipmentOrderYdocEle = orderYdoc.getDocumentElement();
			
			YFCDocument importSalesOrderYdoc = YFCDocument.getDocumentFor(importSOAndShipmentInDoc);
			YFCElement importSalesOrderYdocEle = importSalesOrderYdoc.getDocumentElement();
			
			if (CrocsConstant.STR_STATUS_CANCELLED.equals(
					importSalesOrderYdocEle.getElementsByTagName(CrocsConstant.A_ORDER_STATUS).item(0)
							.getAttribute(CrocsConstant.A_STATUS))) {
				Document crocsCreateSOMigrationSyncServOutDoc = CommonUtil.invokeService(env,"CrocsAUCreateSOMigrationSyncServ", importSalesOrderYdoc.getDocument());
				
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

					if (CrocsConstant.E_SHIPMENT.equals(node.getNodeName())) {

						YFCNode ShipmentNode = orderYdoc.importNode(node, true);
						orderYdoc.getFirstChild().appendChild(ShipmentNode);
						importSalesOrderYdocEle.removeChild(node);

						if (CrocsConstant.E_ORDER.equals(importSalesOrderYdocEle.getTagName())) {

							Document crocsCreateAUSOMigrationSyncServOutDoc = CommonUtil.invokeService(env,
									"CrocsAUCreateSOMigrationSyncServ", importSalesOrderYdoc.getDocument());

							if (importSalesOrderYdocEle.getAttribute(CrocsConstant.OrderNo)
									.equals(crocsCreateAUSOMigrationSyncServOutDoc.getDocumentElement()
											.getAttribute(CrocsConstant.OrderNo)))
								isEligibleForShipmentProcessing = true;
						}
					}
				}

				if (isEligibleForShipmentProcessing) {

					Document crocsPostSOShipMessageSyncServerOutDoc = CommonUtil.invokeService(env,
							"CrocsAUPostSOShipMessageSyncServer",
							YFCDocument.getDocumentFor(
									importShipmentOrderYdocEle.getChildElement(CrocsConstant.E_SHIPMENT).toString())
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

		logger.endTimer(CrocsAUMigarationUtil.logCurrentMethod(this.getClass()));
		return importSOAndShipmentInDoc;
	}


	private void updateTrackingNoOnLine(YFSEnvironment env, YFCElement importSalesOrderYdocEle) {
		
		logger.verbose("updateTrackingNoOnLine is getting started "+importSalesOrderYdocEle);
		try {
			/**Input to call getOrganization List	*/
			YFCDocument organizationYDoc = YFCDocument.createDocument(E_ORGANIZATION);
			YFCElement organizationYDocEle = organizationYDoc.getDocumentElement();

			String trackingNo = importSalesOrderYdocEle.getChildElement("Shipment").getAttribute("TrackingNo");
			String scac = "", strPrimaryUrl = "", customerLocale = "", orderNo="";
				//EOMS-5166-START
			String expectedShipmentDate=importSalesOrderYdocEle.getChildElement("Shipment").getAttribute("ExpectedShipmentDate");
			String orderDate=importSalesOrderYdocEle.getAttribute("OrderDate");
			//EOMS-5166-END
			
			//EOMS-5182 :: URL Changes : START
			if(importSalesOrderYdocEle.getChildElement(E_EXTN)!=null) {
				orderNo = importSalesOrderYdocEle.getAttribute(CrocsConstant.OrderNo);
				YFCElement eleOrderExtn = importSalesOrderYdocEle.getChildElement(E_EXTN);
				customerLocale = eleOrderExtn.getAttribute(EXTN_CUSTOMER_LOCALE);
			}
			//EOMS-5182 :: URL Changes : END
			
			OffsetDateTime currentDate = OffsetDateTime.now(ZoneOffset.UTC);
			String systemDate = currentDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
			
			YFCNodeList<YFCElement> orderLines = importSalesOrderYdocEle.getElementsByTagName(CrocsConstant.E_ORDER_LINE);
			YFCNodeList<YFCElement> shipmentLines = importSalesOrderYdocEle.getChildElement(CrocsConstant.E_SHIPMENT).getElementsByTagName(CrocsConstant.E_SHIPMENT_LINE);
			
			for (YFCElement shipmentLine : shipmentLines) {

				String shipmentLineitemId = shipmentLine.getAttribute(CrocsConstant.A_ITEM_ID);
				
				for (YFCElement orderLine : orderLines) {
					if (shipmentLineitemId.equals(orderLine.getChildElement(CrocsConstant.E_ITEM).getAttribute(CrocsConstant.A_ITEM_ID))) {
						
						if (!YFCCommon.isVoid(trackingNo)) {
							
							/** As we have observed there could be two pattren for trackingNo
							 * pattern 01 : &#xa; 1Z6F857YYW86116475|UPS&#xa;
							 * Pattern 02 : &#xa; 1Z6F857YYW86116475|UPS&#xa; 1Z6F857YYW86143767|UPS&#xa;
							 
							 * Pattern 03 : &#xA; 9200190383434300018479&#xA; 1Z6F857YYN36114041&#xA;
							 * Pattern 04 : &#10; 803356510503233324&#10; 9200190383434300020854&#10;
							 * Pattern 05 : &#xa; 920043015566603 &#xa;
							 
							 * In pattern 02,03,04 , data in the column is exceeding the set limit, so it needs to be trimmed. 
							 * 
							 * As we are extracting the tracking no and scac using split by "|" , below line will help delealing with all above pattern 
							 * trackingNo.trim().split("\\s")[0];
							 * 
							 * In all above pattern we will consider the first set of data.
							 * In pattern 03,04 and 05,we are receiving tracking_no without scac and "|",so this is handled in else loop
							 * 
							 **/

							if (trackingNo.contains("|")) {
								String[] trackingNoSplits = trackingNo.split("\\|");
								
								if (trackingNoSplits.length > 2)
									scac = trackingNoSplits[1].split("\\s")[0];
								else 
								scac = trackingNoSplits[1].trim();
								
								trackingNo = trackingNoSplits[0].trim();
								CrocsShipmentPackedToShippedStatusFromWMS obj = new CrocsShipmentPackedToShippedStatusFromWMS();
								Document OrgCode = obj.updateSCAC(env, SCXmlUtil.createDocument(A_COMMON_CODE), scac);
								Element outEleCC = OrgCode.getDocumentElement();
								
								String scacValue = SCXmlUtil.getXpathAttribute(outEleCC, XPATH_CODE_SHORT_DESCRIPTION);
								String strSCACandService = SCXmlUtil.getXpathAttribute(outEleCC, XPATH_CODE_LONG_DESCRIPTION);

								organizationYDocEle.setAttribute(A_ORGANIZATION_CODE, scacValue);
								logger.verbose("Calling getOrganizationList with input: " + organizationYDoc.toString());

								Document getOrganizationListOutDoc = CommonUtil.invokeAPI(env, TEMPLATE_GET_ORGANIZATION_LIST,
										API_GET_ORGANIZATION_LIST, organizationYDoc.getDocument());

								logger.verbose("Output returned from getOrganizationList: "
										+ SCXmlUtil.getString(getOrganizationListOutDoc));

								strPrimaryUrl = SCXmlUtil.getXpathAttribute(getOrganizationListOutDoc.getDocumentElement(),
										XPAH_PRIMARY_URL);
								
								//EOMS-5182 :: URL Changes as per Locale : START
//								if (!YFCCommon.isVoid(customerLocale) && STR_FR_CA.equalsIgnoreCase(customerLocale)) {
//									strPrimaryUrl = CommonUtil.updatePrimaryURLAsPerLocale(strPrimaryUrl,customerLocale,scacValue,orderNo);
//								}
								//EOMS-5182 :: URL Changes as per Locale : END
								
								if (!YFCCommon.isVoid(strPrimaryUrl))
								strPrimaryUrl = strPrimaryUrl.replaceAll(A_TRACKING_NO,trackingNoSplits[0].trim().replaceAll("&", "&amp;"));
							}else{
								trackingNo = trackingNo.trim().split("\\s")[0];
							}
							
							logger.verbose("Final updated trackingNo: " +trackingNo);
							
							/** if Extn is not in orderLine tag , it will create it else it will get and add in existing tag 
							 **/
							YFCElement orderLineExtn = null;
							if (!YFCCommon.isVoid(orderLine.getChildElement(E_EXTN)))
								orderLineExtn = orderLine.getChildElement(E_EXTN);
							else 
								orderLineExtn = orderLine.createChild(E_EXTN);
							//EOMS-5166- START
							if(YFCCommon.isVoid(expectedShipmentDate))
							{
								String carrierServiceCode=orderLine.getAttribute(A_CARRIER_SERVICE_CODE);
								 OffsetDateTime odt = OffsetDateTime.parse(orderDate);

						           // Common formatter
						           DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

								 OffsetDateTime newExpectedShipmentDate=null;
								if(VAL_AUSPOST.equals(carrierServiceCode))
									newExpectedShipmentDate = odt.plusDays(5);
								else
									 newExpectedShipmentDate = odt.plusDays(2);
								
								 expectedShipmentDate=newExpectedShipmentDate.format(formatter);
							}
							//EOMS-5166- END
							String shipmentDate=CommonUtil.convertInputStringDateIntoSFCCFormat(expectedShipmentDate);
							orderLineExtn.setAttribute(A_EXTN_TRACKING_URL, strPrimaryUrl);
							orderLineExtn.setAttribute(A_EXTN_SHIPMENT_DATE, shipmentDate);
							orderLineExtn.setAttribute(EXTN_TRACKING_NO, trackingNo);
							orderLineExtn.setAttribute(EXTN_SHIP_CARRIER, scac);
						}
					}
					
					logger.verbose("updateTrackingNoOnLine is getting updated: "+importSalesOrderYdocEle);
				}
			}	

		} catch (Exception e) {
			throw new YFSException(e.getMessage());
		}
	}	
}
