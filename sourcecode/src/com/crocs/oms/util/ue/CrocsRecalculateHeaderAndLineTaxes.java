package com.crocs.oms.util.ue;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.crocs.oms.common.util.CrocsConstant;
import com.crocs.oms.common.util.CrocsXmlConstants;
import com.crocs.oms.order.CrocsGetVertexOrderDetails;
import com.crocs.oms.order.CrocsSOAPRequestToVertex;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.interop.japi.YIFApi;
import com.yantra.interop.japi.YIFClientFactory;
import com.yantra.yfc.core.YFCIterable;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.dom.YFCElement;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfc.util.YFCCommon;
import com.yantra.yfc.util.YFCException;
import com.yantra.yfs.japi.YFSEnvironment;

/**
 * EOMS- 3240 Vertex tax total in the SOPA response does not match the order
 * total in Call center
 * 
 * 
 * Sample Input : <?xml version="1.0" encoding="UTF-8"?>
 * <Order DocumentType="0001" EnterpriseCode="CROCS_US" EntryType="WEB"
 * IsNewOrder="N" OrderDate="2025-05-26T09:18:52+00:00" OrderHeaderKey=
 * "20250526091852400866" OrderNo="1001301OCUS" Status="Created"> <OrderLines>
 * <OrderLine HasRepricingQuantityChanged="N" OrderLineKey=
 * "20250526091852400867" OrderedQty="3.00" OriginalOrderedQty="3.00"
 * PrimeLineNo="1" SubLineNo="1">
 * <Item CostCurrency="USD" CountryOfOrigin="" CustomerItem="11016-6MB-M12"
 * CustomerItemDesc="original, classic clog, classic, crocs, jjjjound, baya"
 * ECCNNo="" HarmonizedCode="" ISBN="" ItemDesc="original, classic clog,
 * classic, crocs, jjjjound, baya" ItemID="11016-6MB-M12" ItemShortDesc=
 * "original, classic clog, classic, crocs, jjjjound, baya" ItemWeight="0.00"
 * ItemWeightUOM="LBS" ManufacturerItem="" ManufacturerItemDesc=""
 * ManufacturerName="" NMFCClass="" NMFCCode="" NMFCDescription="" ProductClass=
 * "" ProductLine="" ScheduleBCode="" SupplierItem="" SupplierItemDesc=""
 * TaxProductCode="PC040144" UPCCode="" UnitCost="0.00" UnitOfMeasure="EACH"/>
 * <LinePriceInfo ActualPricingQty="3.00" DiscountPercentage="0.00"
 * DiscountReference="" DiscountType="" InvoicedLineTotal="0.00"
 * InvoicedPricingQty="0.00" IsEligibleForShippingDiscount="Y" IsPriceLocked="Y"
 * LineTotal="145.91" ListPrice="0.00" OrderedPricingQty="3.00"
 * PricingQtyConversionFactor="0.00" PricingQuantityStrategy="IQTY" PricingUOM=
 * "EACH" RepricingQty="3.00" RetailPrice="0.00" SettledAmount="0.00"
 * SettledQuantity="0.00" Tax="10.94" TaxableFlag="N" UnitPrice="54.99"/>
 * <ItemDetails ItemID="11016-6MB-M12">
 * <PrimaryInformation Description="original, classic clog, classic, crocs,
 * jjjjound, baya" ItemType="Footwear"/>
 * <ClassificationCodes Model="10002" TaxProductCode="PC040144"/> </ItemDetails>
 * <LineOverallTotals AdditionalLinePriceTotal="0.00" Charges="0.00" Discount=
 * "30.00" ExtendedPrice="164.97" LineCost="0.00" LineTotal="145.91"
 * LineTotalWithoutTax="134.97" ManualDiscountPercentage="17.05"
 * ManualOverridePercentage="0.00" OptionPrice="0.00" PercentProfitMargin=
 * "100.00" PricingQty="3.00" ShippingBaseCharge="0.00" ShippingCharges="0.00"
 * ShippingDiscount="0.00" ShippingTotal="0.00" Tax="10.94" UnitPrice="54.99"/>
 * <LineCharges>
 * <LineCharge ChargeAmount="10.00" ChargeCategory="PromotionDiscount"
 * ChargeName="PromotionDiscount1" ChargeNameKey="202503121041022298618"
 * ChargePerLine="10.00" ChargePerUnit="0.00" InvoicedChargeAmount="0.00"
 * InvoicedChargePerLine="0.00" InvoicedChargePerUnit="0.00" IsBillable="Y"
 * IsDiscount="Y" Reference="" RemainingChargeAmount="10.00"
 * RemainingChargePerLine="10.00" RemainingChargePerUnit="0.00"> <Extn/>
 * </LineCharge>
 * <LineCharge ChargeAmount="20.00" ChargeCategory="Discount" ChargeName=
 * "PromotionDiscount" ChargeNameKey="202412151837381016357" ChargePerLine=
 * "20.00" ChargePerUnit="0.00" InvoicedChargeAmount="0.00"
 * InvoicedChargePerLine="0.00" InvoicedChargePerUnit="0.00" IsBillable="Y"
 * IsDiscount="Y" Reference="" RemainingChargeAmount="20.00"
 * RemainingChargePerLine="20.00" RemainingChargePerUnit="0.00"> <Extn/>
 * </LineCharge> </LineCharges> <LineTaxes>
 * <LineTax ChargeCategory="" ChargeName="" ChargeNameKey="" InvoicedTax="0.00"
 * Reference1="" Reference2="" Reference3="" RemainingTax="10.94" Tax="10.94"
 * TaxName="SalesTax" TaxPercentage="0.081" TaxableFlag="N"/> </LineTaxes>
 * <ModificationTypes/> </OrderLine>
 * <OrderLine HasRepricingQuantityChanged="N" OrderLineKey=
 * "20250526091852400868" OrderedQty="4.00" OriginalOrderedQty="4.00"
 * PrimeLineNo="2" SubLineNo="1">
 * <Item CostCurrency="USD" CountryOfOrigin="" CustomerItem="11016-6MB-M5W7"
 * CustomerItemDesc="original, classic clog, classic, crocs, jjjjound, baya"
 * ECCNNo="" HarmonizedCode="" ISBN="" ItemDesc="original, classic clog,
 * classic, crocs, jjjjound, baya" ItemID="11016-6MB-M5W7" ItemShortDesc=
 * "original, classic clog, classic, crocs, jjjjound, baya" ItemWeight="0.00"
 * ItemWeightUOM="LBS" ManufacturerItem="" ManufacturerItemDesc=""
 * ManufacturerName="" NMFCClass="" NMFCCode="" NMFCDescription="" ProductClass=
 * "" ProductLine="" ScheduleBCode="" SupplierItem="" SupplierItemDesc=""
 * TaxProductCode="PC040144" UPCCode="" UnitCost="0.00" UnitOfMeasure="EACH"/>
 * <LinePriceInfo ActualPricingQty="4.00" DiscountPercentage="0.00"
 * DiscountReference="" DiscountType="" InvoicedLineTotal="0.00"
 * InvoicedPricingQty="0.00" IsEligibleForShippingDiscount="Y" IsPriceLocked="Y"
 * LineTotal="179.40" ListPrice="0.00" OrderedPricingQty="4.00"
 * PricingQtyConversionFactor="0.00" PricingQuantityStrategy="IQTY" PricingUOM=
 * "EACH" RepricingQty="4.00" RetailPrice="0.00" SettledAmount="0.00"
 * SettledQuantity="0.00" Tax="13.44" TaxableFlag="N" UnitPrice="49.99"/>
 * <ItemDetails ItemID="11016-6MB-M5W7">
 * <PrimaryInformation Description="original, classic clog, classic, crocs,
 * jjjjound, baya" ItemType="Footwear"/>
 * <ClassificationCodes Model="10002" TaxProductCode="PC040144"/> </ItemDetails>
 * <LineOverallTotals AdditionalLinePriceTotal="0.00" Charges="0.00" Discount=
 * "34.00" ExtendedPrice="199.96" LineCost="0.00" LineTotal="179.40"
 * LineTotalWithoutTax="165.96" ManualDiscountPercentage="15.93"
 * ManualOverridePercentage="0.00" OptionPrice="0.00" PercentProfitMargin=
 * "100.00" PricingQty="4.00" ShippingBaseCharge="0.00" ShippingCharges="0.00"
 * ShippingDiscount="0.00" ShippingTotal="0.00" Tax="13.44" UnitPrice="49.99"/>
 * <LineCharges>
 * <LineCharge ChargeAmount="9.00" ChargeCategory="PromotionDiscount" ChargeName
 * ="PromotionDiscount1" ChargeNameKey="202503121041022298618" ChargePerLine=
 * "9.00" ChargePerUnit="0.00" InvoicedChargeAmount="0.00" InvoicedChargePerLine
 * ="0.00" InvoicedChargePerUnit="0.00" IsBillable="Y" IsDiscount="Y" Reference=
 * "" RemainingChargeAmount="9.00" RemainingChargePerLine="9.00"
 * RemainingChargePerUnit="0.00"> <Extn/> </LineCharge>
 * <LineCharge ChargeAmount="0.00" ChargeCategory="PromotionDiscount" ChargeName
 * ="PromotionDiscount2" ChargeNameKey="202503121041122298621" ChargePerLine=
 * "10.00" ChargePerUnit="0.00" InvoicedChargeAmount="0.00"
 * InvoicedChargePerLine="0.00" InvoicedChargePerUnit="0.00" IsBillable="Y"
 * IsDiscount="Y" Reference="10.0" RemainingChargeAmount="0.00"
 * RemainingChargePerLine="10.00" RemainingChargePerUnit="0.00"> <Extn/>
 * </LineCharge>
 * <LineCharge ChargeAmount="25.00" ChargeCategory="Discount" ChargeName=
 * "PromotionDiscount" ChargeNameKey="202412151837381016357" ChargePerLine=
 * "25.00" ChargePerUnit="0.00" InvoicedChargeAmount="0.00"
 * InvoicedChargePerLine="0.00" InvoicedChargePerUnit="0.00" IsBillable="Y"
 * IsDiscount="Y" Reference="" RemainingChargeAmount="25.00"
 * RemainingChargePerLine="25.00" RemainingChargePerUnit="0.00"> <Extn/>
 * </LineCharge> </LineCharges> <LineTaxes>
 * <LineTax ChargeCategory="" ChargeName="" ChargeNameKey="" InvoicedTax="0.00"
 * Reference1="" Reference2="" Reference3="" RemainingTax="13.44" Tax="13.44"
 * TaxName="SalesTax" TaxPercentage="0.081" TaxableFlag="N"/> </LineTaxes>
 * <ModificationTypes>
 * <ModificationType ImpactsPricing="Y" Level="ORDER_LINE" Name="PRICE"/>
 * <ModificationType ImpactsPricing="N" Level="ORDER_LINE" Name="ADD_NOTE"/>
 * </ModificationTypes> </OrderLine> </OrderLines>
 * <PersonInfoShipTo AddressLine1="5000 S Arizona Mills Circle" AddressLine2=""
 * AddressLine3="" AddressLine4="" AddressLine5="" AddressLine6=""
 * AlternateEmailID="" Beeper="" City="Tempe" Company="" Country="US"
 * Createprogid="SterlingHttpTester" Createts="2025-05-07T15:33:26+00:00"
 * Createuserid="admin" DayFaxNo="" DayPhone="(757) 600-5653" Department=""
 * EMailID="NVENNA@CROCS.COM" ErrorTxt="" EveningFaxNo="" EveningPhone=""
 * FirstName="NAGAMANI" HttpUrl="" JobTitle="" LastName="VENNA" Lockid="0"
 * MiddleName="" MobilePhone="" Modifyprogid="SterlingHttpTester" Modifyts=
 * "2025-05-07T15:33:26+00:00" Modifyuserid="admin" OtherPhone="" PersonID=""
 * PersonInfoKey="20250507153326241102" PreferredShipAddress="" ShortZipCode=
 * "85282" State="AZ" Suffix="" Title="" UseCount="0" VerificationStatus=""
 * ZipCode="85282" isHistory="N"/>
 * <PersonInfoBillTo AddressLine1="5000 S Arizona Mills Circle" AddressLine2=""
 * AddressLine3="" AddressLine4="" AddressLine5="" AddressLine6=""
 * AlternateEmailID="" Beeper="" City="Tempe" Company="" Country="US"
 * Createprogid="SterlingHttpTester" Createts="2025-05-07T15:33:26+00:00"
 * Createuserid="admin" DayFaxNo="" DayPhone="(757) 600-5653" Department=""
 * EMailID="NVENNA@CROCS.COM" ErrorTxt="" EveningFaxNo="" EveningPhone=""
 * FirstName="NAGAMANI" HttpUrl="" JobTitle="" LastName="VENNA" Lockid="0"
 * MiddleName="" MobilePhone="" Modifyprogid="SterlingHttpTester" Modifyts=
 * "2025-05-07T15:33:26+00:00" Modifyuserid="admin" OtherPhone="" PersonID=""
 * PersonInfoKey="20250507153326241102" PreferredShipAddress="" ShortZipCode=
 * "85282" State="AZ" Suffix="" Title="" UseCount="0" VerificationStatus=""
 * ZipCode="85282" isHistory="N"/>
 * <OverallTotals AdditionalLinePriceTotal="0.00" GrandCharges="53.00"
 * GrandDiscount="76.00" GrandShippingBaseCharge="40.00" GrandShippingCharges=
 * "0.00" GrandShippingDiscount="0.00" GrandShippingTotal="40.00" GrandTax=
 * "27.70" GrandTotal="369.63" HdrCharges="53.00" HdrDiscount="12.00"
 * HdrShippingBaseCharge="40.00" HdrShippingCharges="0.00" HdrShippingDiscount=
 * "0.00" HdrShippingTotal="40.00" HdrTax="3.32" HdrTotal="44.32" LineSubTotal=
 * "364.93" ManualDiscountPercentage="5.86" PercentProfitMargin="100.00"/>
 * <HeaderCharges>
 * <HeaderCharge ChargeAmount="40.00" ChargeCategory="ShippingCharge" ChargeName
 * ="ShippingCharge" ChargeNameKey="202412151836581016348" InvoicedChargeAmount=
 * "0.00" IsBillable="Y" IsDiscount="N" IsManual="Y" IsShippingCharge="Y"
 * Reference="" RemainingChargeAmount="40.00"> <Extn/> </HeaderCharge>
 * <HeaderCharge ChargeAmount="12.00" ChargeCategory="ShippingDiscount"
 * ChargeName="ShippingDiscount" ChargeNameKey="202504101002032733186"
 * InvoicedChargeAmount="0.00" IsBillable="Y" IsDiscount="Y" IsManual="Y"
 * IsShippingCharge="N" Reference="" RemainingChargeAmount="12.00"> <Extn/>
 * </HeaderCharge>
 * <HeaderCharge ChargeAmount="13.00" ChargeCategory="ExtendShipProtection"
 * ChargeName="ExtendShipProtection" ChargeNameKey="202504111317482751507"
 * InvoicedChargeAmount="0.00" IsBillable="Y" IsDiscount="N" IsManual="Y"
 * IsShippingCharge="N" Reference="" RemainingChargeAmount="13.00"> <Extn/>
 * </HeaderCharge> </HeaderCharges> <HeaderTaxes>
 * <HeaderTax ChargeCategory="ShippingCharge" ChargeName="ShippingCharge"
 * ChargeNameKey="202412151836581016348" InvoicedTax="0.00" Reference1=""
 * Reference2="" Reference3="" RemainingTax="3.24" Tax="3.24" TaxName=
 * "ShippingTax" TaxPercentage="0.081"/>
 * <HeaderTax ChargeCategory="ShippingDiscount" ChargeName="ShippingDiscount"
 * ChargeNameKey="202504101002032733186" InvoicedTax="0.00" Reference1=""
 * Reference2="" Reference3="" RemainingTax="0.97" Tax="0.97" TaxName=
 * "ShippingTax" TaxPercentage="0.081"/>
 * <HeaderTax ChargeCategory="ExtendShipProtection" ChargeName=
 * "ExtendShipProtection" ChargeNameKey="202504111317482751507" InvoicedTax=
 * "0.00" Reference1="" Reference2="" Reference3="" RemainingTax="1.05" Tax=
 * "1.05" TaxName="ShipProtectionTax" TaxPercentage="0.081"/> <TaxSummary>
 * <TaxSummaryDetail InvoicedTax="0.00" OverallTax="2.27" RemainingTax="2.27"
 * TaxName="ShippingTax"/>
 * <TaxSummaryDetail InvoicedTax="0.00" OverallTax="1.05" RemainingTax="1.05"
 * TaxName="ShipProtectionTax"/> </TaxSummary> </HeaderTaxes>
 * <ModificationTypes/> </Order>
 * 
 */
public class CrocsRecalculateHeaderAndLineTaxes {

	private static final YFCLogCategory logger = YFCLogCategory.instance(CrocsRecalculateHeaderAndLineTaxes.class);
	
	/**
	 * @param env
	 * @param getOrderDetails
	 * @return getOrderDetails
	 */
	public Document crocsRecalculateHeaderAndLineTaxes(YFSEnvironment env, Document getOrderDetails) {

		logger.beginTimer("CrocsRecalculateHeaderAndLineTaxes : crocsRecalculateHeaderAndLineTaxes Start");
		logger.verbose("CrocsRecalculateHeaderAndLineTaxes Start");
	    env.setTxnObject(CrocsConstant.A_IS_NEW_TAX_ALERT_NEEDED,CrocsConstant.A_TRUE );
		try {

			Element eleOrder = getOrderDetails.getDocumentElement();

			// Extract required attributes from Order element
			String strEnterpriseCode = eleOrder.getAttribute(CrocsXmlConstants.A_ENTERPRISE_CODE);
			String strOrderNo = eleOrder.getAttribute(CrocsXmlConstants.A_ORDER_NO);
			String strStatus = eleOrder.getAttribute(CrocsXmlConstants.A_STATUS);
			String strIsNewOrder = eleOrder.getAttribute(CrocsXmlConstants.A_IS_NEW_ORDER);

			// Extract OrderLine, HeaderCharge, and ShipTo address nodes
			Element eleOrderLines = (Element) getOrderDetails.getElementsByTagName(CrocsXmlConstants.E_ORDER_LINE)
					.item(0);
			Element elePersonShipToInfo = (Element) getOrderDetails
					.getElementsByTagName(CrocsXmlConstants.E_PERSON_INFO_SHIP_TO).item(0);

			
			if (!YFCCommon.isVoid(eleOrder)) {

			    String strOrderType = eleOrder.getAttribute(CrocsXmlConstants.A_ORDER_TYPE);
			    String enteredBy = eleOrder.getAttribute(CrocsConstant.A_ENTERED_BY);
			    
			    //EOMS-8182 -  No call to vertex for CROCS_AU orders
			    if (Objects.equals(strEnterpriseCode, CrocsConstant.CROCS_AU)) {
			        logger.info("No call to vertex for CROCS_AU order: " + strOrderNo);
			        return getOrderDetails;    
			    } 
			    //EOMS-10430 - Changes Start - No Vertex Tax Call for EMEA
			    else if (CrocsConstant.CROCS_EMEA_ENTERPRISES.contains(strEnterpriseCode)) {
			        logger.info("No call to vertex for EMEA Order: " + strOrderNo);
			        return getOrderDetails;
			    }
			    //EOMS-10430 - Changes End
			    
			    //EOMS-5534 - No call to vertex for marketplace orders.
			    else if (Objects.equals(strOrderType, CrocsConstant.ORDER_TYPE_MP)) {
			        return getOrderDetails;
			    }
			    /**EOMS-10601 - No call to vertex for HeyDude_CA GLOBALE orders.**/
			    else if (CrocsConstant.HEYDUDE_CA.equalsIgnoreCase(strEnterpriseCode) 
			    		&& CrocsXmlConstants.V_GLOBALE.equals(enteredBy)) {
			    	logger.info("No call to vertex for HeyDude_CA GLOBALE order: " +strOrderNo);
			        return getOrderDetails;
				}
			}
			
			// Do not call Vertex if there are no OrderLines
			if ((strIsNewOrder != null && strIsNewOrder.equals(CrocsConstant.VAL_FLAG_Y))
					|| (CrocsConstant.VAR_PARTIALLY_SHIPPED.equals(strStatus)
							|| CrocsConstant.VAL_SHIPPED.equals(strStatus)) || CrocsConstant.STR_CANCELLED.equals(strStatus)
					|| (eleOrderLines == null || YFCCommon.isVoid(eleOrderLines))
					|| (elePersonShipToInfo == null || YFCCommon.isVoid(elePersonShipToInfo))) {
				return getOrderDetails;
			}

			// migration Order by pass
			if (CrocsConstant.V_MIGRATION.equals(eleOrder.getAttribute(CrocsConstant.A_ENTERED_BY))
					&& !CrocsConstant.ENTRY_TYPE_CALL_CENTER
							.equals(eleOrder.getAttribute(CrocsConstant.A_ENTRY_TYPE))) {
				return getOrderDetails;
			}

			// if Vertex Output is null
			Document docVertexOutput = fetchVetexOuput(env, getOrderDetails);
			if (docVertexOutput == null) {
				return getOrderDetails;
			}

			// Extract tax lines
			YFCIterable<YFCElement> taxLineItemList = CrocsGetVertexOrderDetails.extractLineTaxItem(docVertexOutput);
			if (taxLineItemList == null) {
				logger.verbose("No matching Tax Line Item found for OrderNo: ");
				return getOrderDetails;
			}

			// Recalculate Header Taxes
			processHeaderTaxes(env,getOrderDetails, taxLineItemList, strEnterpriseCode);

			processLineTaxes(env,getOrderDetails, taxLineItemList, strEnterpriseCode);

			logger.endTimer("CrocsRecalculateHeaderAndLineTaxes : crocsRecalculateHeaderAndLineTaxes End");
			logger.verbose("CrocsRecalculateHeaderAndLineTaxes End");

			return getOrderDetails;

		} catch (Exception e) {
			logger.error(
					"CrocsRecalculateHeaderAndLineTaxes : crocsRecalculateHeaderAndLineTaxes : Error fetching Vertex  Ouput details",
					e);
			throw new YFCException(e,
					"CrocsRecalculateHeaderAndLineTaxes : crocsRecalculateHeaderAndLineTaxes : Error fetching Vertex  Ouput details");
		}

	}

	/**
	 * @param env
	 * @param getOrderList
	 * @return Document fetch the VertexOuput From Transaction Object Or Call
	 *         CrocsQuotationRequesttoVertex Service
	 */
	public static Document fetchVetexOuput(YFSEnvironment env, Document getOrderList) {
		Document docVertexOutput = null;
		try {
			YIFApi api = YIFClientFactory.getInstance().getApi();
			docVertexOutput = api.executeFlow(env, CrocsConstant.A_CROCS_QUOTATION_REQUEST_TO_VERTEX_FOR_PRICING,
					getOrderList);
			logger.verbose("CrocsGetVertexOrderDetails : fetchOrderAndTaxDetails : docVertexOutput "
					+ SCXmlUtil.getString(docVertexOutput));

			return docVertexOutput;
		} catch (Exception e) {
			logger.error("CrocsGetVertexOrderDetails : fetchOrderAndTaxDetails : Error fetching Vertex  Ouput details",
					e);
			throw new YFCException(e,
					"CrocsGetVertexOrderDetails : fetchOrderAndTaxDetails : Error fetching Vertex  Ouput details");
		}

	}

	/**
	 * @param getOrderDetails
	 * @param taxLineItemList processHeaderTaxes - Recalculate Order HeaderTaxes
	 *                        based on Vertex Output
	 */
	private Document processHeaderTaxes(YFSEnvironment env,Document getOrderDetails, YFCIterable<YFCElement> taxLineItemList,
			String strEnterpriseCode) {

		// Filter relevant shipping tax line items
		List<YFCElement> vertexShippingLineItemList = new ArrayList<>();

		for (YFCElement lineItem : taxLineItemList) {
			if (CrocsConstant.A_SHIPPING_TAX_CODE.equalsIgnoreCase(
					lineItem.getChildElement(CrocsConstant.V_PRODUCT).getAttribute(CrocsConstant.V_PRODUCT_CLASS))
					|| CrocsConstant.A_SHIPPING_EXTENDED_TAX_CODE.equalsIgnoreCase(lineItem
							.getChildElement(CrocsConstant.V_PRODUCT).getAttribute(CrocsConstant.V_PRODUCT_CLASS))) {
				logger.verbose("crocsRecalculateHeaderAndLineTaxes: processHeaderTaxes" + lineItem.toString());
				vertexShippingLineItemList.add(lineItem);
			}
		}
		if (CrocsConstant.CROCS_US.equalsIgnoreCase(strEnterpriseCode) || CrocsConstant.HEYDUDE_US.equalsIgnoreCase(strEnterpriseCode)) {
			processUSTax(vertexShippingLineItemList, getOrderDetails);
		} else if (CrocsConstant.CROCS_CA.equalsIgnoreCase(strEnterpriseCode) || CrocsConstant.HEYDUDE_CA.equalsIgnoreCase(strEnterpriseCode)) {
			processCATax(env,vertexShippingLineItemList, getOrderDetails);
		}
		return getOrderDetails;
	}

	/**
	 * @param vertexShippingLineItemList
	 * @param getOrderDetails
	 * @return eleNewHeaderTaxes
	 */
	private Document processUSTax(List<YFCElement> vertexShippingLineItemList, Document getOrderDetails) {

		logger.verbose("CrocsRecalculateHeaderAndLineTaxes : processUSTax - start of the Method");
		

		Element nlHeaderTaxes = (Element) getOrderDetails.getElementsByTagName(CrocsXmlConstants.E_HEADER_TAXES)
				.item(0);
		Document docUpdatedHeaderTaxes = SCXmlUtil.createDocument(CrocsXmlConstants.E_HEADER_TAXES);
		Element eleNewHeaderTaxes = docUpdatedHeaderTaxes.getDocumentElement();
		for (YFCElement vertexShippingtaxLineItem : vertexShippingLineItemList) {
			logger.verbose("crocsRecalculateHeaderAndLineTaxes: processUSTax " + vertexShippingLineItemList.toString());
			double newTaxPercentage = CrocsGetVertexOrderDetails.computeNewTaxPercentage(vertexShippingtaxLineItem);
			Element eleHeaderTax = docUpdatedHeaderTaxes.createElement(CrocsXmlConstants.E_HEADER_TAX);
			String strVProductName = vertexShippingtaxLineItem.getChildElement(CrocsConstant.V_PRODUCT).getNodeValue();
			logger.verbose("crocsRecalculateHeaderAndLineTaxes: processUSTax - strVProductName" + strVProductName);
			if (!YFCObject.isVoid(strVProductName)
					&& (strVProductName.equalsIgnoreCase(CrocsXmlConstants.A_SHIPPING_DISCOUNT)
							|| strVProductName.equalsIgnoreCase(CrocsXmlConstants.A_SHIPPING_CHARGE)
							|| strVProductName.equalsIgnoreCase(CrocsConstant.A_EXTENDED_SHIPPING_PROTECTION))) {

				eleHeaderTax.setAttribute(CrocsXmlConstants.A_CHARGE_CATEGORY, strVProductName);
				eleHeaderTax.setAttribute(CrocsXmlConstants.A_CHARGE_NAME, strVProductName);
			}
			// Extract old tax details
			String[] taxDetails = extractOldTaxDetails(nlHeaderTaxes, strVProductName);
			eleHeaderTax.setAttribute(CrocsXmlConstants.A_TAX_PER_CENTAGE, String.valueOf(newTaxPercentage));
			eleHeaderTax.setAttribute(CrocsXmlConstants.A_TAX_NAME, CrocsConstant.A_SHIPPING_TAX);
			if (!YFCObject.isVoid(strVProductName)
					&& strVProductName.equalsIgnoreCase(CrocsConstant.A_EXTENDED_SHIPPING_PROTECTION)) {
				eleHeaderTax.setAttribute(CrocsXmlConstants.A_TAX_NAME, CrocsConstant.A_SHIP_PROTECTION_TAX);
			}
			double taxValue = Double
					.parseDouble(vertexShippingtaxLineItem.getChildElement(CrocsConstant.V_TOTAL_TAX).getNodeValue());
			eleHeaderTax.setAttribute(CrocsXmlConstants.A_TAX, String.valueOf(Math.abs(taxValue)));
			eleHeaderTax.setAttribute(CrocsXmlConstants.A_REFERENCE1, taxDetails[0]);
			eleHeaderTax.setAttribute(CrocsXmlConstants.A_REFERENCE2, taxDetails[1]);
			eleHeaderTax.setAttribute(CrocsXmlConstants.A_REMAINING_TAX, String.valueOf(Math.abs(taxValue)));
			eleNewHeaderTaxes.appendChild(eleHeaderTax);
			logger.verbose("CrocsRecalculateSOHeaderTaxUE : processUSTax - Print eleHeaderTax  "
					+ SCXmlUtil.getString(eleHeaderTax));
		}

		replaceOldHeaderTaxes(getOrderDetails, nlHeaderTaxes, eleNewHeaderTaxes);

		logger.verbose("CrocsRecalculateHeaderAndLineTaxes : processUSTax - End of the Method");
		return getOrderDetails;
	}

	/**
	 * @param nlHeaderTaxes
	 * @param strChargeName return new String[] { oldTax, oldTaxPercentage }
	 */
	private String[] extractOldTaxDetails(Element eleHeaderTaxes, String strChargeName) {

		logger.verbose("CrocsRecalculateHeaderAndLineTaxes : extractOldTaxDetails - start" + strChargeName);

		String oldTax = "0.00";
		String oldTaxPercentage = "0.00";
		NodeList nlHeaderTaxes = eleHeaderTaxes.getElementsByTagName(CrocsXmlConstants.E_HEADER_TAX);

		for (int i = 0; i < nlHeaderTaxes.getLength(); i++) {
			Element inputTaxLine = (Element) nlHeaderTaxes.item(i);
			String chargeName = inputTaxLine.getAttribute(CrocsXmlConstants.A_CHARGE_NAME);
			if (strChargeName.equalsIgnoreCase(chargeName)) {
				logger.verbose("CrocsRecalculateHeaderAndLineTaxes : extractOldTaxDetails - if condition" + chargeName);
				oldTax = inputTaxLine.getAttribute(CrocsXmlConstants.A_TAX);
				oldTaxPercentage = inputTaxLine.getAttribute(CrocsXmlConstants.A_TAX_PER_CENTAGE);
				break; // Exit early after finding the match
			}
		}

		logger.verbose(
				"CrocsRecalculateHeaderAndLineTaxes : extractOldTaxDetails - End" + oldTax + ", " + oldTaxPercentage);
		return new String[] { oldTax, oldTaxPercentage };
	}

	/**
	 * @param getOrderDetails 
	 * @param vertexShippingLineItemList 
	 * @param vertexShippingLineItemList
	 * @param getOrderDetails
	 * @return
	 */
	private Document processCATax(YFSEnvironment env, List<YFCElement> vertexShippingLineItemList, Document getOrderDetails) {
		logger.verbose("CrocsRecalculateHeaderAndLineTaxes : processCATax - start of the Method");

		Element nlHeaderTaxes = (Element) getOrderDetails.getElementsByTagName(CrocsXmlConstants.E_HEADER_TAXES)
				.item(0);
		Document docUpdatedHeaderTaxes = SCXmlUtil.createDocument(CrocsXmlConstants.E_HEADER_TAXES);
		Element eleNewHeaderTaxes = docUpdatedHeaderTaxes.getDocumentElement();

		for (YFCElement lineItem : vertexShippingLineItemList) {
			String strVProductName = lineItem.getChildElement(CrocsConstant.V_PRODUCT).getNodeValue();
			logger.verbose("CrocsRecalculateHeaderAndLineTaxes: processCATax - strVProductName: " + strVProductName);

			lineItem.getChildren(CrocsConstant.V_TAXES).forEach(tax -> {
				if (!CrocsConstant.V_TAXABLE.equals(tax.getAttribute(CrocsConstant.V_TAX_RESULT))) {
					return;
				}
                    Element eleHeaderTax = createHeaderTaxElement(env, docUpdatedHeaderTaxes, tax, strVProductName, nlHeaderTaxes);
                    if (eleHeaderTax != null) {
                        eleNewHeaderTaxes.appendChild(eleHeaderTax);
                        logger.verbose("CrocsRecalculateHeaderAndLineTaxes: processCATax - HeaderTax Element: " + SCXmlUtil.getString(eleHeaderTax));
                    }                
			});
		}

		replaceOldHeaderTaxes(getOrderDetails, nlHeaderTaxes, eleNewHeaderTaxes);

		logger.verbose("RecalculateHeaderAndLineTaxes : processCATax - End of the Method");
		return getOrderDetails;
	}

	/**
	 * @param doc
	 * @param tax
	 * @param productName
	 * @param existingTaxes
	 * @return
	 * @throws RemoteException 
	 * @throws Exception 
	 */
	private Element createHeaderTaxElement(YFSEnvironment env,Document doc, YFCElement tax, String productName, Element existingTaxes)   {
		Element eleHeaderTax = doc.createElement(CrocsXmlConstants.E_HEADER_TAX);

		if (!YFCObject.isVoid(productName) && (productName.equalsIgnoreCase(CrocsXmlConstants.A_SHIPPING_DISCOUNT)
				|| productName.equalsIgnoreCase(CrocsXmlConstants.A_SHIPPING_CHARGE)
				|| productName.equalsIgnoreCase(CrocsConstant.A_EXTENDED_SHIPPING_PROTECTION))) {
			eleHeaderTax.setAttribute(CrocsXmlConstants.A_CHARGE_CATEGORY, productName);
			eleHeaderTax.setAttribute(CrocsXmlConstants.A_CHARGE_NAME, productName);
		}

		String taxName = getCATaxName(tax.getChildElement(CrocsConstant.V_IMPOSITION).getNodeValue());
		if (CrocsConstant.A_EXTENDED_SHIPPING_PROTECTION.equalsIgnoreCase(productName)) {
			taxName = getCATaxNameForExtendedShipProtection(
					tax.getChildElement(CrocsConstant.V_IMPOSITION).getNodeValue());
		}
		if (YFCObject.isVoid(taxName)) {
			handleInvalidTaxImposition(env,doc,tax.getChildElement(CrocsConstant.V_IMPOSITION).getNodeValue() );				
			return null;
		}
		eleHeaderTax.setAttribute(CrocsXmlConstants.A_TAX_NAME, taxName);
		eleHeaderTax.setAttribute(CrocsXmlConstants.A_TAX_PER_CENTAGE,
				tax.getChildElement(CrocsConstant.V_EFFECTIVE_RATE).getNodeValue());

		double taxValue = Double.parseDouble(tax.getChildElement(CrocsConstant.V_CALCULATED_TAX).getNodeValue());
		eleHeaderTax.setAttribute(CrocsXmlConstants.A_TAX, String.valueOf(Math.abs(taxValue)));

		String[] taxDetails = getCAOldTaxPercentageForHeader(existingTaxes, taxName, productName);
		eleHeaderTax.setAttribute(CrocsXmlConstants.A_REFERENCE1, taxDetails[0]);
		eleHeaderTax.setAttribute(CrocsXmlConstants.A_REFERENCE2, taxDetails[1]);
		eleHeaderTax.setAttribute(CrocsXmlConstants.A_REMAINING_TAX, String.valueOf(Math.abs(taxValue)));

		return eleHeaderTax;
	}
	/**
	 * @param doc
	 * @param tax
	 * @throws RemoteException 
	 */
	public void handleInvalidTaxImposition(YFSEnvironment env, Document inputdoc, String impositionId) {
		// Create alert for invalid tax
		logger.info("CrocsRecalculateHeaderAndLineTaxes: handleInvalidTaxImposition - Invalid Tax Exception " + impositionId);
		CrocsSOAPRequestToVertex crocsSoapReq = new CrocsSOAPRequestToVertex();
		boolean isAlertNeeded = (boolean) env.getTxnObject(CrocsConstant.A_IS_NEW_TAX_ALERT_NEEDED);
		try {
			if (isAlertNeeded) {
				crocsSoapReq.createAlertOnOrder(env, inputdoc, CrocsConstant.STR_INVALID_TAX, impositionId, null);
				env.setTxnObject(CrocsConstant.A_IS_NEW_TAX_ALERT_NEEDED, CrocsConstant.A_FALSE);
				logger.verbose("CrocsRecalculateHeaderAndLineTaxes: processLineCATax - isTaxCallAlertNeeded"
						+ env.getTxnObject(CrocsConstant.A_IS_NEW_TAX_ALERT_NEEDED));
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param orderDoc
	 * @param oldTaxes
	 * @param newHeaderTaxes
	 */
	private void replaceOldHeaderTaxes(Document orderDoc, Element oldTaxes, Element newHeaderTaxes) {
		if (newHeaderTaxes == null || !newHeaderTaxes.hasChildNodes())
			return;

		Element eleOrder = orderDoc.getDocumentElement();
		// Remove old header tax elements
		oldTaxes.getParentNode().removeChild(oldTaxes);
		eleOrder.appendChild(orderDoc.importNode(newHeaderTaxes, true));
	}

	/**
	 * @param imposition
	 * @return String fetches CA Header Tax Names
	 */
	private String getCATaxName(String imposition) {

		switch (imposition) {
		case CrocsConstant.V_IMPOSITION_GST_HST:
			return CrocsConstant.A_SHIPPING_GST_HST_TAX;
		case CrocsConstant.V_IMPOSITION_PST:
			return CrocsConstant.A_SHIPPING_PST_TAX;
		case CrocsConstant.V_IMPOSITION_QST:
			return CrocsConstant.A_SHIPPING_QST_TAX;
		case CrocsConstant.V_IMPOSITION_RST:
			return CrocsConstant.A_SHIPPING_PST_TAX;
		default: 
			logger.info(
					"Invalid Vertex tax imposition. Expected one of [GST/HST, Provincial Sales Tax (PST), Quebec Sales Tax (VAT), Retail Sales Tax (RST)], but received:"
							+ imposition);
			return null;
		}
	}

	/**
	 * @param imposition
	 * @return String fetches CA Extended Ship Protection Header Tax Names
	 */
	private String getCATaxNameForExtendedShipProtection(String imposition) {
		logger.info("CrocsRecalculateHeaderAndLineTaxes: getCATaxNameForExtendedShipProtection  " + imposition);
		switch (imposition) {
		case CrocsConstant.V_IMPOSITION_GST_HST:
			return CrocsConstant.A_SHIP_PROTECTION_GST_HST_TAX;
		case CrocsConstant.V_IMPOSITION_PST:
			return CrocsConstant.A_SHIP_PROTECTION_PST_TAX;
		case CrocsConstant.V_IMPOSITION_QST:
			return CrocsConstant.A_SHIP_PROTECTION_QST_TAX;
		case CrocsConstant.V_IMPOSITION_RST:
			return CrocsConstant.A_SHIP_PROTECTION_PST_TAX;
		default: 
			logger.info(
					"Invalid Vertex tax imposition. Expected one of [GST/HST, Provincial Sales Tax (PST), Quebec Sales Tax (VAT), Retail Sales Tax (RST)], but received:"
							+ imposition);
			return null;
		}
	}

	/**
	 * @param headerTaxInputList
	 * @param taxName
	 * @param strChargeName
	 * @return double[] { tax, taxPercentage }
	 */
	public static String[] getCAOldTaxPercentageForHeader(Element eleHeaderTaxes, String taxName,
			String strChargeName) {
		String taxPercentage = "0.00";
		String tax = "0.00";
		NodeList nlHeaderTaxes = eleHeaderTaxes.getElementsByTagName(CrocsXmlConstants.E_HEADER_TAX);

		for (int i = 0; i < nlHeaderTaxes.getLength(); i++) {
			Element inputTax = (Element) nlHeaderTaxes.item(i);
			String strTaxName = inputTax.getAttribute(CrocsXmlConstants.A_TAX_NAME);
			String strCharge = inputTax.getAttribute(CrocsXmlConstants.A_CHARGE_NAME);
			if (strTaxName.equalsIgnoreCase(taxName) && strCharge.equalsIgnoreCase(strChargeName)) {
				taxPercentage = inputTax.getAttribute(CrocsXmlConstants.A_TAX_PER_CENTAGE);
				tax = inputTax.getAttribute(CrocsXmlConstants.A_TAX);
			}
		}

		logger.verbose(
				"CrocsRecalculateHeaderAndLineTaxes : getCAOldTaxPercentageForHeader :  " + taxPercentage + " " + tax);
		return new String[] { tax, taxPercentage };
	}

	/**
	 * @param getOrderDetails
	 * @param taxLineItemList
	 * @param strEnterpriseCode
	 * @param strStatus
	 * @return
	 */
	private Document processLineTaxes(YFSEnvironment env,Document getOrderDetails, YFCIterable<YFCElement> taxLineItemList,
			String strEnterpriseCode) {
		NodeList nleleOrderLine = getOrderDetails.getElementsByTagName(CrocsXmlConstants.E_ORDER_LINE);
		for (int i = 0; i < nleleOrderLine.getLength(); i++) {
			Element eleOrderLine = (Element) nleleOrderLine.item(i);
			YFCElement taxLineItem = fetchLineTaxDetails(taxLineItemList, eleOrderLine);
			if (!YFCCommon.isVoid(taxLineItem)) {
				if (CrocsConstant.CROCS_US.equalsIgnoreCase(strEnterpriseCode) || CrocsConstant.HEYDUDE_US.equalsIgnoreCase(strEnterpriseCode) ) {
					processLineUSTax(taxLineItem, eleOrderLine);
				} else if (CrocsConstant.CROCS_CA.equalsIgnoreCase(strEnterpriseCode) || CrocsConstant.HEYDUDE_CA.equalsIgnoreCase(strEnterpriseCode)) {
					processLineCATax(env,getOrderDetails,taxLineItem, eleOrderLine);

				}
			}

		}
		return getOrderDetails;
	}

	/**
	 * @param taxLineItemList
	 * @param eleOrderLine
	 * @return YFCElement Calculates US and CA Soap LineItem Elements
	 */
	private YFCElement fetchLineTaxDetails(YFCIterable<YFCElement> taxLineItemList, Element eleOrderLine) {

		Element eleItemDet = (Element) eleOrderLine.getElementsByTagName(CrocsXmlConstants.E_ITEM_DETAILS).item(0);
		Element eleClassification = (Element) eleItemDet.getElementsByTagName(CrocsXmlConstants.E_CLASSIFICATION_CODES)
				.item(0);
		String strItemId = eleItemDet.getAttribute(CrocsXmlConstants.A_ITEM_ID);
		String strTaxCode = eleClassification.getAttribute(CrocsXmlConstants.A_TAX_PRODUCT_CODE);
		String strPrimeLineNo = eleOrderLine.getAttribute(CrocsXmlConstants.A_PRIME_LINE_NO);
		YFCElement taxLineItem = null;

		logger.verbose("CrocsRecalculateSOLineTaxUE :fetchLineTaxDetails" + strItemId + "," + strTaxCode + ","
				+ strPrimeLineNo);

		for (YFCElement lineItem : taxLineItemList) {
			if (strPrimeLineNo.equalsIgnoreCase(lineItem.getAttribute(CrocsConstant.V_LINE_ITEM_NUMBER))
					&& strItemId.equalsIgnoreCase(lineItem.getChildElement(CrocsConstant.V_PRODUCT).getNodeValue())
					&& strTaxCode.equalsIgnoreCase(lineItem.getChildElement(CrocsConstant.V_PRODUCT)
							.getAttribute(CrocsConstant.V_PRODUCT_CLASS))) {
				taxLineItem = lineItem;
				logger.verbose("CrocsRecalculateSOLineTaxUE :fetchLineTaxDetails-lineItem" + lineItem.toString());
				break;
			}
		}
		if (taxLineItem != null) {
			logger.verbose("CrocsRecalculateSOLineTaxUE :fetchLineTaxDetails-taxLineItem" + taxLineItem.toString());
		}

		return taxLineItem;
	}

	/**
	 * @param lineTaxes
	 * @param lineTaxInputList
	 * @return List<YFSExtnTaxBreakup> Calculates CA Line LeveL Taxes
	 */
	private void processLineCATax(YFSEnvironment env,Document getOrderDetails,YFCElement lineTaxes, Element eleOrderLine) {

		if (lineTaxes == null || eleOrderLine == null)
			return;

		logger.verbose("CrocsRecalculateHeaderAndLineTaxes :processLineCATax - start of the Method");
		Element eleOldLineTaxes = (Element) eleOrderLine.getElementsByTagName(CrocsXmlConstants.E_LINE_TAXES).item(0);
		Document docUpdatedLineTaxes = SCXmlUtil.createDocument(CrocsXmlConstants.E_LINE_TAXES);
		Element eleNewLineTaxes = docUpdatedLineTaxes.getDocumentElement();

		lineTaxes.getChildren(CrocsConstant.V_TAXES).forEach(tax -> {
			if (CrocsConstant.V_TAXABLE.equals(tax.getAttribute(CrocsConstant.V_TAX_RESULT))) {

				String taxName = getCALineTaxName(tax.getChildElement(CrocsConstant.V_IMPOSITION).getNodeValue());
				if (YFCObject.isVoid(taxName)) {
					handleInvalidTaxImposition(env, getOrderDetails,
							tax.getChildElement(CrocsConstant.V_IMPOSITION).getNodeValue());			 
				} else {
					Element eleLineTax = docUpdatedLineTaxes.createElement(CrocsXmlConstants.E_LINE_TAX);
					eleLineTax.setAttribute(CrocsXmlConstants.A_TAX_NAME, taxName);
					eleLineTax.setAttribute(CrocsXmlConstants.A_TAX_PER_CENTAGE,
							tax.getChildElement(CrocsConstant.V_EFFECTIVE_RATE).getNodeValue());
                                        
					eleLineTax.setAttribute(CrocsXmlConstants.A_TAX,
							tax.getChildElement(CrocsConstant.V_CALCULATED_TAX).getNodeValue());
					String[] taxDetails = getCAOldTaxPercentage(eleOldLineTaxes, taxName);
					eleLineTax.setAttribute(CrocsXmlConstants.A_REFERENCE1, taxDetails[0]);
					eleLineTax.setAttribute(CrocsXmlConstants.A_REFERENCE2, taxDetails[1]);
					eleLineTax.setAttribute(CrocsXmlConstants.A_REMAINING_TAX,
							tax.getChildElement(CrocsConstant.V_CALCULATED_TAX).getNodeValue());
					eleNewLineTaxes.appendChild(eleLineTax);
				}
			}
		});
		replaceOldLineTaxes(eleOrderLine, eleOldLineTaxes, eleNewLineTaxes);
		logger.verbose("CrocsRecalculateHeaderAndLineTaxes :processLineCATax");

	}

	/**
	 * @param lineTaxes
	 * @param lineTaxInputList
	 * @return
	 */
	private void processLineUSTax(YFCElement lineTaxes, Element eleOrderLine) {

		logger.verbose("CrocsRecalculateHeaderAndLineTaxes :processLineUSTax - start of the Method");
		Element eleOldLineTaxes = (Element) eleOrderLine.getElementsByTagName(CrocsXmlConstants.E_LINE_TAXES).item(0);
		Document docUpdatedLineTaxes = SCXmlUtil.createDocument(CrocsXmlConstants.E_LINE_TAXES);
		Element eleNewLineTaxes = docUpdatedLineTaxes.getDocumentElement();
		Element eleLineTax = docUpdatedLineTaxes.createElement(CrocsXmlConstants.E_LINE_TAX);
		// Extract old tax details
		String[] taxDetails = extractLineOldTaxDetails(eleOrderLine);

		if (lineTaxes != null) {
			double newTaxPercentage = CrocsGetVertexOrderDetails.computeNewTaxPercentage(lineTaxes);
			eleLineTax.setAttribute(CrocsXmlConstants.A_TAX_PER_CENTAGE, String.valueOf(newTaxPercentage));
			eleLineTax.setAttribute(CrocsXmlConstants.A_TAX_NAME, CrocsConstant.A_SALES_TAX);
			eleLineTax.setAttribute(CrocsXmlConstants.A_TAX,
					lineTaxes.getChildElement(CrocsConstant.V_TOTAL_TAX).getNodeValue());
			eleLineTax.setAttribute(CrocsXmlConstants.A_REFERENCE1, taxDetails[0]);
			eleLineTax.setAttribute(CrocsXmlConstants.A_REFERENCE2, taxDetails[1]);
			eleLineTax.setAttribute(CrocsXmlConstants.A_REMAINING_TAX,
					lineTaxes.getChildElement(CrocsConstant.V_TOTAL_TAX).getNodeValue());
			eleNewLineTaxes.appendChild(eleLineTax);
		}

		replaceOldLineTaxes(eleOrderLine, eleOldLineTaxes, eleNewLineTaxes);
		logger.verbose("CrocsRecalculateHeaderAndLineTaxes :processLineUSTax" + SCXmlUtil.getString(eleLineTax));

	}

	/**
	 * @param headerTaxInputList
	 * @param taxName
	 * @return String - taxPercentage
	 */
	public String[] getCAOldTaxPercentage(Element eleOldLineTaxes, String taxName) {
		String taxPercentage = "0.00";
		String tax = "0.00";
		NodeList nlOldLineTaxes = eleOldLineTaxes.getElementsByTagName(CrocsXmlConstants.E_LINE_TAX);
		for (int i = nlOldLineTaxes.getLength() - 1; i >= 0; i--) {
			Element eleLineTax = (Element) nlOldLineTaxes.item(i);
			String strTaxName = eleLineTax.getAttribute(CrocsXmlConstants.A_TAX_NAME);

			if (strTaxName.equalsIgnoreCase(taxName)) {
				taxPercentage = eleLineTax.getAttribute(CrocsXmlConstants.A_TAX_PER_CENTAGE);
				tax = eleLineTax.getAttribute(CrocsXmlConstants.A_TAX);

			}
		}
		logger.verbose("CrocsGetVertexOrderDetails : getCAOldTaxPercentage :  " + taxPercentage);
		return new String[] { tax, taxPercentage };
	}

	/**
	 * @param imposition
	 * @return String fetches CA Line Level Tax Names
	 */
	private String getCALineTaxName(String imposition) {
		logger.info("CrocsRecalculateHeaderAndLineTaxes: getCATaxNameForExtendedShipProtection  " + imposition);
		switch (imposition) {
		case CrocsConstant.V_IMPOSITION_GST_HST:
			return CrocsConstant.A_GST_HST_TAX;
		case CrocsConstant.V_IMPOSITION_PST:
			return CrocsConstant.A_PST_TAX;
		case CrocsConstant.V_IMPOSITION_QST:
			return CrocsConstant.A_QST_TAX;
		case CrocsConstant.V_IMPOSITION_RST:
			return CrocsConstant.A_PST_TAX;
		default:
			logger.info(
					"Invalid Vertex tax imposition. Expected one of [GST/HST, Provincial Sales Tax (PST), Quebec Sales Tax (VAT), Retail Sales Tax (RST)], but received:"
							+ imposition);
			return null;
		}
	}

	/**
	 * @param lineTaxInputList
	 * @return String[] { CrocsConstant.A_SALES_TAX, "0" } Get US oldTaxpercentage
	 */
	private String[] extractLineOldTaxDetails(Element eleOrderLine) {

		Element eleLineTax = (Element) eleOrderLine.getElementsByTagName(CrocsXmlConstants.E_LINE_TAX).item(0);

		if (eleLineTax != null && !YFCObject.isVoid(eleLineTax)) {

			String oldtaxPercentage = eleLineTax.getAttribute(CrocsXmlConstants.A_TAX_PER_CENTAGE);
			String oldtax = eleLineTax.getAttribute(CrocsXmlConstants.A_TAX);
			logger.verbose("CrocsRecalculateSOLineTaxUE :extractLineOldTaxDetails" + oldtaxPercentage + "," + oldtax);
			return new String[] { oldtax, oldtaxPercentage };
		}

		logger.verbose("CrocsRecalculateSOLineTaxUE :extractLineOldTaxDetails" + CrocsConstant.A_SALES_TAX + "," + 0);
		return new String[] { "0.00", "0.00" };

	}

	/**
	 * @param orderDoc
	 * @param oldTaxes
	 * @param newHeaderTaxes
	 */
	private void replaceOldLineTaxes(Element orderLineElement, Element oldTaxes, Element newLineTaxes) {
		if (newLineTaxes == null || !newLineTaxes.hasChildNodes())
			return;
		// Remove old tax elements
		oldTaxes.getParentNode().removeChild(oldTaxes);

		// Append new tax elements
		Document ownerDoc = orderLineElement.getOwnerDocument();
		Element importedTaxes = (Element) ownerDoc.importNode(newLineTaxes, true);
		orderLineElement.appendChild(importedTaxes);
	}

}
