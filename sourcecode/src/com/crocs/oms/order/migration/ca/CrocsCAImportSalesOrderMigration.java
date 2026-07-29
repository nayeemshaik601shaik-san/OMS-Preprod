package com.crocs.oms.order.migration.ca;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.json.JSONException;
import org.w3c.dom.Document;

import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.order.CrocsDecryption;
import com.crocs.oms.order.CrocsHeaderChargesIterationToLine;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.dom.YFCNode;
import com.yantra.yfc.dom.YFCNodeList;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCException;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class CrocsCAImportSalesOrderMigration {
    
    private static YFCLogCategory logger = YFCLogCategory.instance(CrocsCAImportSalesOrderMigration.class.getName()); 
    boolean isHeaderToLineChargeProrationRequired = false;
    
    /**
     * <p> Hover over method for more details <p>
     * <p><b>Service Name:</b> CrocsSalesOrderImportAsync</p>
     * <p><b>Purpose:</b> This service will consume XML from <code>CROCS_IN_SALES_ORDER_MIGRATION_QUEUE</code> 
     * and perform data validation before importing the order.</p>
     * 
     * <p><b>Mandatory Attributes:</b> DocumentType, EnteredBy, EnterpriseCode, OrderNo, 
     * ConditionVariable1, PipelineKey</p>
     * 
     * <p><b>Input:</b> Migration Sales Order sample XML</p>
     * <pre>{@code
      <Order DocumentType="0001" EnteredBy="ImportOrder" EnterpriseCode="CROCS_US" OrderDate="2025-01-19T07:01:02.000Z" OrderNo="61866059CUS" EntryType="WEB" CustomerEMailID="Hemant.Kumar23@ibm.com" CustomerFirstName="RAJ123" CustomerLastName="ALEX123" CustomerPhoneNo="(965)221-0880" PaymentStatus="" PaymentRuleId="CROCS_US_PAY_RULE" SellerOrganizationCode="CROCS_US" ReqDeliveryDate="">
	  	<PriceInfo Currency="USD" EnterpriseCurrency="USD" HeaderTax="0.59" TotalAmount="51.50"/>
	  	<Extn ExtnSourceCodeGrpId="CrocsClub" ExtnIsCrocsClub="N" ExtnCustId="cust123" ExtnFraudStatus="Y"/>
	  	<PersonInfoBillTo AddressLine1="5050 Factory Shops Blvd" AddressLine2="Suite 505" City="Castle Rock" Country="US" DayPhone="(965)221-0880" EMailID="Hemant.Kumar23@ibm.com" FirstName="RAJ123" LastName="ALEX123" State="CO" ZipCode="80108"/>
	  	<PersonInfoShipTo AddressLine1="5050 Factory Shops Blvd" AddressLine2="Suite 505" City="Castle Rock" Country="US" DayPhone="(123)456-7890" EMailID="Hemant.Kumar23@ibm.com" FirstName="RAJ123" LastName="ALEX123" State="CO" ZipCode="80108"/>
	  	<HeaderCharges>
	  		<HeaderCharge ChargeCategory="ShippingCharge" ChargeName="ShippingCharge" ChargeAmount="6.99">
	  			<Extn ExtnPromotionId="" ExtnPromotionText="" ExtnDWPromotionId=""/>
	  		</HeaderCharge>
	  	</HeaderCharges>
	  	<HeaderTaxes>
	  		<HeaderTax TaxName="ShippingTax" Tax="0.59" TaxPercentage="0.08440629470672388"/>
	  	</HeaderTaxes>
	  	<OrderLines>
	  		<OrderLine ConditionVariable1="Migration" CarrierServiceCode="" DeliveryMethod="SHP" FulfillmentType="CROCS_DC_FULFILLMENT" PipelineKey="2025032409131197462" PrimeLineNo="1" SubLineNo="1" OrderedQty="1.00">
	  			<Extn ExtnHSTCode="" ExtnMSRP=""/>
	  			<Item CostCurrency="USD" ItemID="40002-001-M17" ItemDesc="original, classic clog, classic, crocs, jjjjound, baya" ItemShortDesc="original, classic clog, classic, crocs, jjjjound, baya" UnitCost="4.41" UnitOfMeasure="EACH" TaxProductCode=""/>
	  			<LinePriceInfo IsPriceLocked="Y" UnitPrice="49.99" ListPrice="49.99" RetailPrice="49.99" LineTotal="43.92" ActualPricingQty="1" OrderedPricingQty="1" Tax="3.43" TaxableFlag="Y"/>
	  			<LineCharges>
	  				<LineCharge ChargeCategory="PromotionDiscount" ChargeName="PromotionDiscount1" ChargePerLine="5" ChargeAmount="5.00">
	  					<Extn ExtnPromotionId="2017-01-01-thru-2019-01-31_5off_TestProductAmountClass_PR99_PATEST" ExtnDWPromotionId="c6b336051bbe7c21e85d0154a5" ExtnPromotionText="$5 off Footwear Product"/>
	  				</LineCharge>
	  				<LineCharge ChargeCategory="PromotionDiscount" ChargeName="PromotionDiscount2" ChargePerLine="4.5" ChargeAmount="4.5">
	  					<Extn ExtnPromotionId="2017-01-01-thru-2019-01-31_10peroff_TestProductPercentClass_PR99_PPTEST" ExtnDWPromotionId="06b9028097c1edb79456adef70" ExtnPromotionText="10% off any product"/>
	  				</LineCharge>
	  			</LineCharges>
	  			<LineTaxes>
	  				<LineTax TaxName="SalesTax" TaxPercentage="0.08471227463571251" Tax="3.43"/>
	  			</LineTaxes>
	  			<OrderStatuses>
	  				<OrderStatus Status="3700" StatusQty="1.0">
	  					<Schedule ShipNode="OHIO_DC"/>
	  				</OrderStatus>
	  			</OrderStatuses>
	  		</OrderLine>
	  	</OrderLines>
	  	<PaymentMethods>
	  		<PaymentMethod CreditCardExpDate="10/2029" CreditCardName="CREDIT_CARD" CreditCardNo="8946123" DisplayCreditCardNo="8946" CreditCardType="Master" PaymentType="Credit Card" FirstName="RAJ123" LastName="ALEX123" MaxChargeLimit="5000">
	  			<PersonInfoBillTo AddressLine1="5050 Factory Shops Blvd" City="Castle Rock" Country="US" DayPhone="(965)221-0880" EMailID="Hemant.Kumar23@ibm.com" FirstName="RAJ123" LastName="ALEX123" State="CO" ZipCode="80108"/>
	  			<PaymentDetails RequestAmount="430.73" ChargeType="AUTHORIZATION" ProcessedAmount="430.73" AuthorizationID="23123" AuditTransactionId="txn123" AuthorizationExpirationDate="2025-05-30T12:46:12+00:00" HoldAgainstBook="Y" TranType="ONLINE-ECOMM" TranRequestTime="2025-03-03T12:46:12+00:00"/>
	  		</PaymentMethod>
	  	</PaymentMethods>
	  </Order>
     * }</pre>
     * 
     * @param env - YFSEnvironment
     * @param inDoc - Input XML
     * @return Final validated and updated XML
     **/
    
    /*
     * Service Name: CrocsImportShipmentAsync</p>
     * Purpose: This service will consume XML from CROCS_IN_SALES_ORDER_MIGRATION_QUEUE and perform data validation before importing the order
     * Mandatory Attributes: DocumentType, EnteredBy, EnterpriseCode, OrderNo, ConditionVariable1, PipelineKey
     */
       
    
    
	public Document ImportSOMigrationValidation(YFSEnvironment env, Document inDoc) {
		logger.beginTimer(CrocsCAMigarationUtil.logCurrentMethod(this.getClass()));
		logger.verbose("Input Doc for CrocsSalesOrderMigration: \n" + YFCDocument.getDocumentFor(inDoc).toString());

		try {
			CrocsHeaderChargesIterationToLine headerChargesIterationToLine = null;
			Document headerChargesIterationToLineDoc = null;
			
			YFCDocument importOrderInYdoc = YFCDocument.getDocumentFor(inDoc);
			YFCElement importOrderYdocEle = importOrderInYdoc.getDocumentElement();

			/** There is a chnage in pipe line , update in SMA properties 	*/
			String importSOMandatoryAttr[] = { CrocsConstant.A_DOCUMENT_TYPE,
					CrocsConstant.VAL_DOCUMENT_TYPE_SALES_ORDER, CrocsConstant.A_ENTERED_BY,
					CrocsConstant.A_ENTERPRISE_CODE, CrocsConstant.A_ORDER_NO, CrocsConstant.A_CONDITION_VARIABLE_1,
					CrocsConstant.A_SO_MIGRATION_CA_PIPELINE_PROPERTY };
			
			logger.verbose("ImportSOMigrationValidation validated in mandatory fields "+importSOMandatoryAttr);
			
			boolean isValidForImportOrderProcesingFlag = isValidForImportOrderProcesing(importOrderYdocEle,
					importSOMandatoryAttr);
			logger.verbose("ImportSOMigrationValidation : is validation passed "+isValidForImportOrderProcesingFlag);
			
			if (isValidForImportOrderProcesingFlag) {
				
				validateOrderQtyForCancellation(importOrderInYdoc);
				
				importOrderInYdoc = validateSortShip(importOrderInYdoc);
				
				isHeaderToLineChargeProrationRequired = IsHeaderToLineChargeProrationRequired(importOrderYdocEle);
				logger.verbose("ImportSOMigrationValidation : is isHeaderToLineChargeProrationRequired "+isHeaderToLineChargeProrationRequired);
				
				if(isHeaderToLineChargeProrationRequired) {
					headerChargesIterationToLine = new CrocsHeaderChargesIterationToLine();
					headerChargesIterationToLineDoc = headerChargesIterationToLine
							.headerChargesIterationToLine(importOrderInYdoc.getDocument());
					
					headerChargesIterationToLine.filterHeaderCharges(importOrderInYdoc.getDocument());
					headerChargesIterationToLine.filterHeaderTaxes(importOrderInYdoc.getDocument());
					
					logger.verbose("ImportSOMigrationValidation : Document post proration "+importOrderInYdoc);	
				}
				else
					headerChargesIterationToLineDoc = importOrderInYdoc.getDocument();
				
				importSOProcesingReadiness(env,YFCDocument.getDocumentFor(headerChargesIterationToLineDoc).getDocumentElement());
			}
			logger.endTimer(CrocsCAMigarationUtil.logCurrentMethod(this.getClass()));
			return importOrderInYdoc.getDocument();

		} catch (YFSException e) {
			throw new YFSException(e.getMessage(), e.getErrorCode(), e.getErrorDescription());
		}
	}
    
	private void validateOrderQtyForCancellation(YFCDocument importOrderInYdoc) {
		
		/**
		 * This is to handle the scenario where line is cancelled so shippedQty is 0.As per mapping OrderedQty is mapped to ShippedQty.
		 * and hence status is 9000 , this is cancelled scenarion, which was conflicting with shortShip Scenario.
		 * 
		 **/

		YFCNodeList<YFCElement> orderLines = importOrderInYdoc.getElementsByTagName(CrocsConstant.E_ORDER_LINE);
		for (YFCElement orderLine : orderLines) {
			if (CrocsConstant.STR_STATUS_CANCELLED
					.equals(orderLine.getElementsByTagName(CrocsConstant.A_ORDER_STATUS).item(0).getAttribute(CrocsConstant.A_STATUS))) {

				String strOrderedQty = orderLine.getAttribute(CrocsConstant.A_ORDERED_QTY);
				if(YFCCommon.isVoid(strOrderedQty))
					orderLine.setAttribute(CrocsConstant.A_ORDERED_QTY,"0");
					
				double orderedQty = Double.valueOf(orderLine.getAttribute(CrocsConstant.A_ORDERED_QTY));
				if (orderedQty < 1) {
					orderLine.setAttribute(CrocsConstant.A_ORDERED_QTY,
							orderLine.getChildElement(CrocsConstant.A_LINE_PRICE_INFO).getAttribute(CrocsConstant.A_ACTUAL_PRICING_QTY));
				}
			}
		}
	}

	private YFCDocument validateSortShip(YFCDocument importOrderInYdoc) {
		
		
		/** This method only for SortShip Scenario where order has 4 lines each with 4 Qty and for one of the lines, few of the lines are not shipped 
		 *  logic is to :- 
		 *  
		 *  OrderQty will hold "No of ShipmentQty" for a particular line.
		 *  In sortship Scenario, lets say item_id: 1234 , qty : 4 
		 *  	 item_id: 1234 , OrderedQty="0"(ShipmentQty) then actual shipped Qty is: 3
		 *       so we store OriginalOrderedQty = 4 (Actually shipped + actual Non Shipped Qty). 
		 **/

		boolean isSortShipScenario = false;
		YFCDocument  importOrderNonSortShipInYdoc = YFCDocument.createDocument();
		importOrderNonSortShipInYdoc = importOrderInYdoc.getCopy();
		
		YFCDocument onlyActualShipOrderLines = YFCDocument.createDocument(CrocsConstant.E_ORDER_LINES);
		YFCElement onlyActualShipOrderLine = onlyActualShipOrderLines.getDocumentElement();

		YFCNodeList<YFCElement> sortShipOrderLines = importOrderInYdoc.getElementsByTagName(CrocsConstant.E_ORDER_LINE);
		for (YFCElement sortShipOrderLine : sortShipOrderLines) {

			double OrderedQty = Double.valueOf(sortShipOrderLine.getAttribute(CrocsConstant.A_ORDERED_QTY));
			if (OrderedQty < 1) {

				isSortShipScenario = true;
				String shortShipItem = sortShipOrderLine.getChildElement(CrocsConstant.E_ITEM).getAttribute(CrocsConstant.A_ITEM_ID);
				double shortShipQty = Double.valueOf(sortShipOrderLine.getChildElement(CrocsConstant.A_LINE_PRICE_INFO).getAttribute("ActualPricingQty"));

				YFCNodeList<YFCElement> actualShipOrderLines = importOrderInYdoc.getElementsByTagName(CrocsConstant.E_ORDER_LINE);
				for (YFCElement actualShipOrderLine : actualShipOrderLines) {

					if (shortShipItem.equals(actualShipOrderLine.getChildElement(CrocsConstant.E_ITEM).getAttribute(CrocsConstant.A_ITEM_ID))) {
						double actualShipOrderedQty = Double.valueOf(actualShipOrderLine.getAttribute(CrocsConstant.A_ORDERED_QTY));
						if (actualShipOrderedQty > 0) {
							double originalOrderedQty = actualShipOrderedQty + shortShipQty;
							actualShipOrderLine.setAttribute(CrocsConstant.A_ORIGINAL_ORDERED_QTY, originalOrderedQty);

							YFCElement actualShiporderLine = onlyActualShipOrderLines.importNode(actualShipOrderLine,
									true);
							onlyActualShipOrderLines.getFirstChild().appendChild((YFCNode) actualShiporderLine);
						}
					}
				}
			} else {
				
				/** if OrderedQty is non 0 , then logic is to prepare a document and keep apending the real qty Shipped lines and then 
				 * go for proration line total and grand total 	**/
				boolean isItemAppended = false;
				YFCNodeList<YFCElement> OrderLines = onlyActualShipOrderLine.getElementsByTagName(CrocsConstant.E_ORDER_LINE);
				for (YFCElement OrderLine : OrderLines) {
					if (OrderLine.getChildElement(CrocsConstant.E_ITEM).getAttribute(CrocsConstant.A_ITEM_ID)
							.equals(sortShipOrderLine.getChildElement(CrocsConstant.E_ITEM).getAttribute(CrocsConstant.A_ITEM_ID))) {
						isItemAppended = true;
						break;
					} else
						isItemAppended = false;
				}
				if (!isItemAppended) {
					YFCElement sortShipOrderLinee = onlyActualShipOrderLines.importNode(sortShipOrderLine, true);
					onlyActualShipOrderLines.getFirstChild().appendChild((YFCNode) sortShipOrderLinee);
				}
			}
		}

		importOrderInYdoc.getDocumentElement()
				.removeChild((YFCNode) importOrderInYdoc.getDocumentElement().getChildElement(CrocsConstant.E_ORDER_LINES));

		YFCElement onlyActualShipOrderLine1 = importOrderInYdoc.importNode(onlyActualShipOrderLine, true);
		importOrderInYdoc.getFirstChild().appendChild((YFCNode) onlyActualShipOrderLine1);

		if (isSortShipScenario)
			return importOrderInYdoc;
		else
			return importOrderNonSortShipInYdoc;
	}

	private boolean IsHeaderToLineChargeProrationRequired(YFCElement importOrderYdocEle) {
		boolean IsProrationRequired = false;

		if (!CrocsConstant.STR_STATUS_CANCELLED.equals(importOrderYdocEle
				.getElementsByTagName(CrocsConstant.A_ORDER_STATUS).item(0).getAttribute(CrocsConstant.A_STATUS))) {

			YFCNodeList<YFCElement> headerCharges = importOrderYdocEle.getElementsByTagName(CrocsConstant.E_HEADER_CHARGE);
			for (YFCElement headerCharge : headerCharges) {

				/**
				 * Marking the flag as true for proration in case of chargeCategory other then
				 * ShippingCharges even once.
				 */

				String chargeCategory = headerCharge.getAttribute(CrocsConstant.A_CHARGE_CATEGORY);

				if (!CrocsConstant.CHARGE_CATEGORY_SHIPPING_CHARGE.equals(chargeCategory)
						&& !CrocsXmlConstants.A_SHIPPING_DISCOUNT.equals(chargeCategory)
						&& !CrocsConstant.A_EXTENDED_SHIPPING_PROTECTION.equals(chargeCategory))
					IsProrationRequired = true;
			}
		}

		return IsProrationRequired;
	}

	private void importSOProcesingReadiness(YFSEnvironment env, YFCElement importOrderYdocEle) {
		logger.beginTimer(CrocsCAMigarationUtil.logCurrentMethod(this.getClass()));
		

		if (!CrocsConstant.STR_STATUS_CANCELLED.equals(importOrderYdocEle
				.getElementsByTagName(CrocsConstant.A_ORDER_STATUS).item(0).getAttribute(CrocsConstant.A_STATUS))) {
			
			setLineTotal(importOrderYdocEle);
			logger.verbose("ImportSOMigrationValidation : setLineTotal(importOrderYdocEle) " + importOrderYdocEle);
			
			handlingGiftCardDetails(importOrderYdocEle);
			logger.verbose(
					"ImportSOMigrationValidation : handlingGiftCardDetails(importOrderYdocEle) " + importOrderYdocEle);
		}else {
			
			handlingGiftCardDetails(importOrderYdocEle);
			logger.verbose(
					"ImportSOMigrationValidation : handlingGiftCardDetails(importOrderYdocEle) " + importOrderYdocEle);
			handlingFullCancelledImportSO(importOrderYdocEle);
			
		}
		
		if (isHeaderToLineChargeProrationRequired)
			setGrandTotal(importOrderYdocEle);
		
		logger.verbose("ImportSOMigrationValidation : setGrandTotal(importOrderYdocEle) " + importOrderYdocEle);
		logger.endTimer(CrocsCAMigarationUtil.logCurrentMethod(this.getClass()));
	}
    
	private void handlingFullCancelledImportSO(YFCElement importOrderYdocEle) {
		
		
		importOrderYdocEle.getChildElement(CrocsConstant.E_PRICE_INFO).setAttribute(CrocsConstant.A_INVOICED_AMOUNT, "0.0");
		importOrderYdocEle.getChildElement(CrocsConstant.E_PRICE_INFO).setAttribute(CrocsConstant.TOTAL_AMOUNT, "0.0");
		
		YFCNodeList<YFCElement> headerCharges = importOrderYdocEle.getElementsByTagName(CrocsConstant.A_HEADER_CHARGE);
		for(YFCElement headerCharge:headerCharges)
			headerCharge.setAttribute( CrocsConstant.A_CHARGE_AMOUNT, "0.0");
		
		
		YFCNodeList<YFCElement> orderLines = importOrderYdocEle.getElementsByTagName(CrocsConstant.E_ORDER_LINE);
		for (YFCElement orderLine : orderLines) {

			orderLine.setAttribute( CrocsConstant.A_ORIGINAL_ORDERED_QTY, orderLine.getAttribute(CrocsConstant.A_ORDERED_QTY));
			orderLine.setAttribute(CrocsConstant.A_ORDERED_QTY, "0.0");

			YFCNodeList<YFCElement> lineTaxs = orderLine.getElementsByTagName(CrocsConstant.E_LINE_TAX);
			for (YFCElement lineTax : lineTaxs) {
				
				/** will let all kinds of tax to be 0	*/
				//if (lineTax.getAttribute(CrocsConstant.A_TAX_NAME).equals(CrocsConstant.A_SALES_TAX)) 
				{
					lineTax.setAttribute(CrocsConstant.A_TAX, "0.0");
				}
			}
			
			YFCElement linesPriceInfo = orderLine.getChildElement(CrocsConstant.E_LINE_PRICE_INFO);
			linesPriceInfo.setAttribute(CrocsConstant.A_LINE_TOTAL, "0.0");
			linesPriceInfo.setAttribute(CrocsConstant.A_LIST_PRICE, "0.0");
			linesPriceInfo.setAttribute(CrocsConstant.A_RETAIL_PRICE, "0.0");
			linesPriceInfo.setAttribute(CrocsConstant.A_TAX, "0.0");
			linesPriceInfo.setAttribute(CrocsConstant.A_UNIT_PRICE, "0.0");
		}
	}

	private void handlingGiftCardDetails(YFCElement importOrderYdocEle) {

		/**
		 * SVC and pin for giftcard will be received as plain text or as Encrypted,
		 * 
		 * if received as plain text then 
		 * 		before persisting it to db we will encrypt it.
		 *
		 * if received as Encrypted text then
		 * 		Store SVC as received
		 *  	Decrypt SVC and get last 4 digit and store in DisplaySvcNo
		 *		Decrypt SVC pin and store it in PaymentReference3
		 */
		
		try {
			CrocsDecryption decryption = null;
			
			YFCNodeList<YFCElement> paymentMethods = importOrderYdocEle
					.getElementsByTagName(CrocsConstant.A_PAYMENT_METHOD);

			for (YFCElement paymentMethod : paymentMethods) {

				if (CrocsConstant.STR_GIVEX.equals(paymentMethod.getAttribute(CrocsConstant.PaymentType))) {

					decryption = new CrocsDecryption();
					
					String SvcNoPlainText = paymentMethod.getAttribute(CrocsConstant.SvcNo);
					String givexPinPlainText = paymentMethod.getAttribute(CrocsConstant.PaymentReference3);

						String svcNoEncoded = paymentMethod.getAttribute("SvcNoEncoded");
						String svcPinEncoded = paymentMethod.getAttribute("SvcPinEncoded");	

					if (!YFCCommon.isVoid(svcNoEncoded) && !YFCCommon.isVoid(svcPinEncoded)) {
						
						logger.verbose("extnSvcNoEncoded and extnSvcPinEncoded has value extnSvcNoEncoded: "+svcNoEncoded+" extnSvcPinEncoded: "+svcPinEncoded);
						
						/* Decrypting SVC */
						paymentMethod.setAttribute(CrocsConstant.SvcNo, svcNoEncoded);

						String extnSvcNoDecoded = decryption.getDecryptedDataForGivex(svcNoEncoded);
						String extnSvcPinDecoded = decryption.getDecryptedDataForGivex(svcPinEncoded);

						paymentMethod.setAttribute("DisplaySvcNo",
								extnSvcNoDecoded.substring(extnSvcNoDecoded.length() - 4, extnSvcNoDecoded.length()));
						paymentMethod.setAttribute(CrocsConstant.PaymentReference3, extnSvcPinDecoded);
						
						logger.verbose("Givex PaymentMethod post Decryption: "+paymentMethod);

					} else if (!YFCCommon.isVoid(SvcNoPlainText) && !YFCCommon.isVoid(givexPinPlainText)) {
						
						logger.verbose("SvcNoPlainText and givexPinPlainText has value SvcNoPlainText: "+SvcNoPlainText+" extnSvcPinEncoded: "+givexPinPlainText);
						
						/* Encrypting SVC */
						paymentMethod.setAttribute(CrocsConstant.SvcNo,
								decryption.getEncryptedDataForGivex(SvcNoPlainText));

						paymentMethod.setAttribute("DisplaySvcNo",
								SvcNoPlainText.substring(SvcNoPlainText.length() - 4, SvcNoPlainText.length()));
						paymentMethod.setAttribute(CrocsConstant.PaymentReference3, givexPinPlainText);
						
						logger.verbose("Givex PaymentMethod post Encryption: "+paymentMethod);
					}
				}
			}
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException
				| JSONException e) {
			throw new YFCException(e.getMessage());
		}

	}

	private void setGrandTotal(YFCElement importOrderYdocEle) {
		double  grandLineTotal = 0.0, shippingCharge = 0.0, shippingTaxForShippingCharge = 0.0, shippingTaxForShippingDiscount=0.0,
				shipProtectionTax = 0.0, extendShipProtection = 0.0, shippingDiscount=0.0,grandTotal = 0.0;

		YFCNodeList<YFCElement> headerCharges = importOrderYdocEle.getElementsByTagName(CrocsConstant.E_HEADER_CHARGE);
		for (YFCElement headerCharge : headerCharges) {
			if (CrocsConstant.CHARGE_CATEGORY_SHIPPING_CHARGE.equals(headerCharge.getAttribute(CrocsConstant.A_CHARGE_CATEGORY)))
				shippingCharge += Double.valueOf(headerCharge.getAttribute(CrocsConstant.A_CHARGE_AMOUNT));
			
			if (CrocsConstant.A_EXTENDED_SHIPPING_PROTECTION.equals(headerCharge.getAttribute(CrocsConstant.A_CHARGE_CATEGORY)))
				extendShipProtection += Double.valueOf(headerCharge.getAttribute(CrocsConstant.A_CHARGE_AMOUNT));
			
			if (CrocsXmlConstants.A_SHIPPING_DISCOUNT.equals(headerCharge.getAttribute(CrocsConstant.A_CHARGE_CATEGORY)))
				shippingDiscount += Double.valueOf(headerCharge.getAttribute(CrocsConstant.A_CHARGE_AMOUNT));
		}

		YFCNodeList<YFCElement> headerTaxes = importOrderYdocEle.getElementsByTagName(CrocsConstant.E_HEADER_TAX);
		for (YFCElement headerTax : headerTaxes) {
			//if (CrocsConstant.A_SHIPPING_TAX.equals(headerTax.getAttribute(CrocsConstant.A_TAX_NAME))) 
			{

				if (CrocsConstant.CHARGE_CATEGORY_SHIPPING_CHARGE
						.equals(headerTax.getAttribute(CrocsConstant.A_CHARGE_CATEGORY)))
					shippingTaxForShippingCharge += Double.valueOf(headerTax.getAttribute(CrocsConstant.A_TAX));

				if (CrocsXmlConstants.A_SHIPPING_DISCOUNT
						.equals(headerTax.getAttribute(CrocsConstant.A_CHARGE_CATEGORY)))
					shippingTaxForShippingDiscount += Double.valueOf(headerTax.getAttribute(CrocsConstant.A_TAX));
			}

			if (CrocsConstant.A_SHIP_PROTECTION_TAX.equals(headerTax.getAttribute(CrocsConstant.A_TAX_NAME)))
				shipProtectionTax += Double.valueOf(headerTax.getAttribute(CrocsConstant.A_TAX));
		}

		logger.verbose("ImportSOMigrationValidation : setGrandTotal(importOrderYdocEle) shippingCharge" + shippingCharge
				+ " shippingTax" + shippingTaxForShippingCharge);	
		
		YFCNodeList<YFCElement> orderLines = importOrderYdocEle.getElementsByTagName(CrocsConstant.E_ORDER_LINE);
		for (YFCElement orderLine : orderLines) {
			double salesTax = 0.0, chargeAmount = 0.0;
			double itemQty = Double.valueOf(orderLine.getAttribute(CrocsConstant.A_ORDERED_QTY));
			double unitPrice = Double.valueOf(orderLine.getChildElement(CrocsConstant.E_LINE_PRICE_INFO).getAttribute(CrocsConstant.A_UNIT_PRICE));

			logger.verbose("ImportSOMigrationValidation : setGrandTotal(importOrderYdocEle) itemQty" + itemQty
					+ " unitPrice" + unitPrice);
			
			YFCNodeList<YFCElement> lineTaxes = orderLine.getElementsByTagName(CrocsConstant.E_LINE_TAX);
			for (YFCElement lineTax : lineTaxes) {
				//if (lineTax.getAttribute(CrocsConstant.A_TAX_NAME).equals(CrocsConstant.A_SALES_TAX)) 
				{
					salesTax += Double.valueOf(lineTax.getAttribute(CrocsConstant.A_TAX));
				}
			}

			YFCNodeList<YFCElement> lineCharges = orderLine.getElementsByTagName(CrocsConstant.E_LINE_CHARGE);
			for (YFCElement lineCharge : lineCharges) {

				String chargeCategory = lineCharge.getAttribute(CrocsConstant.A_CHARGE_CATEGORY);
				if (chargeCategory.equals(CrocsConstant.A_PROMOTION_DISCOUNT))
					chargeAmount += Double.valueOf(lineCharge.getAttribute(CrocsConstant.A_CHARGE_AMOUNT));

				else if (chargeCategory.equals(CrocsConstant.A_CROCS_PROMOTION_HDR_DISCOUNT)) {
					chargeAmount += Double.valueOf(lineCharge.getAttribute(CrocsConstant.A_CHARGE_PER_LINE));
				}
			}
			logger.verbose("ImportSOMigrationValidation : setGrandTotal(importOrderYdocEle) salesTax" + salesTax
					+ " chargeAmount" + chargeAmount);
			
			grandLineTotal += (itemQty * unitPrice) + salesTax - chargeAmount;
			
			logger.verbose("ImportSOMigrationValidation : setGrandTotal(importOrderYdocEle) "
					+ "grandLineTotal += (itemQty * unitPrice) + salesTax - chargeAmount"
					+ " grandLineTotal" + grandLineTotal);

		}

		grandTotal = grandLineTotal + (shippingCharge + extendShipProtection - shippingDiscount)
				     + shippingTaxForShippingCharge - shippingTaxForShippingDiscount;
		
		logger.verbose("ImportSOMigrationValidation : setGrandTotal(importOrderYdocEle) "
				+ "grandTotal = grandLineTotal + shippingCharge + shippingTax"
				+ " grandTotal" + grandTotal);
		
		importOrderYdocEle.getChildElement(CrocsConstant.E_PRICE_INFO).setAttribute(CrocsConstant.E_HEADER_TAX, (shippingTaxForShippingCharge + shipProtectionTax - shippingTaxForShippingDiscount));
		importOrderYdocEle.getChildElement(CrocsConstant.E_PRICE_INFO).setAttribute(CrocsConstant.TOTAL_AMOUNT, grandTotal);
		
		/**
		 * We will calculate the grandTotal and verify it with the sum of payment
		 * details received in xpath /Order/PaymentMethods/PaymentMethod/PaymentDetailsList/PaymentDetails/@RequestAmount
		 * 
		 * For both single and multi payment tags,
		 * 		has matched we will not touch the received value. 
		 * 		has difference <= $0.10 then difference will be adjusted in Givex PaymentType.
		 * 		has difference > $0.10 then we will not touch the received value.
		 */
		handleGrandTotalForPayments(importOrderYdocEle);
		
	}

	private void handleGrandTotalForPayments(YFCElement importOrderYdocEle) {

		double processedAmountReceived = 0.0, processedAmountAdjusted = 0.0;
		double grandTotalCalculated = Double.valueOf(importOrderYdocEle.getChildElement(CrocsConstant.E_PRICE_INFO)
				.getAttribute(CrocsConstant.TOTAL_AMOUNT));

		YFCNodeList<YFCElement> PaymentMethods = importOrderYdocEle.getElementsByTagName(CrocsConstant.A_PAYMENT_METHOD);

		for (YFCElement PaymentMethod : PaymentMethods)
			processedAmountReceived += Double.valueOf(PaymentMethod.getElementsByTagName(CrocsConstant.E_PAYMENT_DETAILS).item(0).getAttribute(CrocsConstant.A_PROCESSED_AMOUNT));
		
		/** Rounding off difference to two decimal places */
		double difference = Math.round((grandTotalCalculated - processedAmountReceived) * 100.0) / 100.0;
		if (difference <= 0.10) {

			for (YFCElement PaymentMethod : PaymentMethods) {
				if (CrocsConstant.A_GIVEX.equals(PaymentMethod.getAttribute(CrocsConstant.PaymentType))) {

					YFCElement paymentDetails = PaymentMethod.getElementsByTagName(CrocsConstant.E_PAYMENT_DETAILS).item(0);

					double processedAmount = Double.valueOf(paymentDetails.getAttribute(CrocsConstant.A_PROCESSED_AMOUNT));
					processedAmountAdjusted = processedAmount + difference;
					
					PaymentMethod.setAttribute(CrocsConstant.A_MAX_CHARGE_LIMIT, processedAmountAdjusted);
					paymentDetails.setAttribute(CrocsConstant.A_PROCESSED_AMOUNT, processedAmountAdjusted);
					paymentDetails.setAttribute(CrocsConstant.RequestAmount, processedAmountAdjusted);
				}
			}
		}
	}

	private void setLineTotal(YFCElement importOrderYdocEle) {
		/**
		 * How the lineTOtal has been calculated 
		 * Item unitPrice * LineQty + Line Taxes + (-Discount) + (-Discount Taxes);
		 
		 * in below case SalesTax= Line Taxes + (-Discount Taxes);
		 * 
		 **/

		YFCNodeList<YFCElement> orderLines = importOrderYdocEle.getElementsByTagName(CrocsConstant.E_ORDER_LINE);
		for (YFCElement orderLine : orderLines) {
			double salesTax = 0.0, chargeAmount = 0.0, lineTotal = 0.0, grandTotal = 0.0;

			/** Updating /Order/OrderLines/OrderLine/OrderStatuses/OrderStatus/@StatusQty  with 
			 * /Order/OrderLines/OrderLine/@OrderedQty
			 * just to make sure shipped qty is mapped to the StatusQty with same Qty.
			 **/
			String lineOrderedQty = orderLine.getAttribute(CrocsConstant.A_ORDERED_QTY);
			YFCElement orderLineStatus = orderLine.getElementsByTagName(CrocsConstant.A_ORDER_STATUS).item(0);
			if ("3700".equals(orderLineStatus.getAttribute(CrocsConstant.A_STATUS))) {
				orderLineStatus.setAttribute("StatusQty",lineOrderedQty);
			}
			/** END	*/
			
			double itemQty = Double.valueOf(orderLine.getAttribute(CrocsConstant.A_ORDERED_QTY));
			double unitPrice = Double.valueOf(orderLine.getChildElement(CrocsConstant.E_LINE_PRICE_INFO).getAttribute(CrocsConstant.A_UNIT_PRICE));
			orderLine.setAttribute(CrocsConstant.A_SHIP_NODE,orderLine.getElementsByTagName("Schedule").item(0).getAttribute(CrocsConstant.A_SHIP_NODE));
			
			YFCNodeList<YFCElement> lineTaxes = orderLine.getElementsByTagName(CrocsConstant.E_LINE_TAX);
			for (YFCElement lineTax : lineTaxes) {
				
				/* Disabling to get sum of all the tax for CA 	*/			
				//if (lineTax.getAttribute(CrocsConstant.A_TAX_NAME).equals(CrocsConstant.A_SALES_TAX)) 
				{
					salesTax += Double.valueOf(lineTax.getAttribute(CrocsConstant.A_TAX));
				}
			}
			
			
			YFCNodeList<YFCElement> lineCharges = orderLine.getElementsByTagName(CrocsConstant.E_LINE_CHARGE);
			for (YFCElement lineCharge : lineCharges) {

				String chargeCategory = lineCharge.getAttribute(CrocsConstant.A_CHARGE_CATEGORY);
				if (chargeCategory.equals(CrocsConstant.A_PROMOTION_DISCOUNT)) {
					lineCharge.setAttribute(CrocsConstant.A_CHARGE_AMOUNT, lineCharge.getAttribute(CrocsConstant.A_CHARGE_PER_LINE));
					chargeAmount += Double.valueOf(lineCharge.getAttribute(CrocsConstant.A_CHARGE_PER_LINE));
				}
				/** getting the charges PromotionHdrDiscount to calculate total lineChargeAmount
				 *  and setting the ChargeAmount at LineCharge to reflect right discount on UI**/
				
				else if (chargeCategory.equals(CrocsConstant.A_CROCS_PROMOTION_HDR_DISCOUNT)) {
					lineCharge.setAttribute(CrocsConstant.A_CHARGE_AMOUNT, lineCharge.getAttribute(CrocsConstant.A_CHARGE_PER_LINE));
					chargeAmount += Double.valueOf(lineCharge.getAttribute(CrocsConstant.A_CHARGE_PER_LINE));	
				}
			}

			if (isHeaderToLineChargeProrationRequired) {

				/**
				 * Setting the tax at LinePriceInfo label so that header tax shown right post
				 * tax proration
				 */
				orderLine.getChildElement(CrocsConstant.E_LINE_PRICE_INFO).setAttribute(CrocsConstant.A_TAX, salesTax);
			}
			
			lineTotal = ((itemQty * unitPrice) + salesTax) - chargeAmount; 
			orderLine.getChildElement(CrocsConstant.E_LINE_PRICE_INFO).setAttribute(CrocsConstant.A_LINE_TOTAL, lineTotal);
		}
		//importOrderYdocEle.getElementsByTagName(CrocsConstant.A_PAYMENT_METHOD).item(0).getElementsByTagName(CrocsConstant.E_PAYMENT_DETAILS).item(0).setAttribute(CrocsConstant.ChargeType,CrocsConstant.CHARGETYPE_CHARGE);
	}

	/**
     * @param importOrderYdocEle - YFCElement of Order
     */
	private boolean isValidForImportOrderProcesing(YFCElement importOrderYdocEle,
			String[] ImportSalesOrderMandatoryAttrtributes) {
		logger.beginTimer(CrocsCAMigarationUtil.logCurrentMethod(this.getClass()));

		boolean isValid = false;
		for (String ImportOrderMandatoryAttrtribute : ImportSalesOrderMandatoryAttrtributes) {

			if (CrocsCAMigarationUtil.validateMandatoryAttribute(importOrderYdocEle,
					ImportOrderMandatoryAttrtribute))
				isValid = true;
			else
				break;
		}
		logger.endTimer(CrocsCAMigarationUtil.logCurrentMethod(this.getClass()));
		return isValid;
	}
}
