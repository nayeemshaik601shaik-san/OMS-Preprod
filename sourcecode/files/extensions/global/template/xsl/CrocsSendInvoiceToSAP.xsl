<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://www.w3.org">
    <xsl:output method="xml" indent="yes"/>

    <!-- Template to match the root element -->
    <xsl:template match="/InvoiceDetail">
        <InvoiceDetail>
            <xsl:apply-templates select="InvoiceHeader"/>
        </InvoiceDetail>
    </xsl:template>

    <!-- Template to match the InvoiceHeader element -->
    <xsl:template match="InvoiceHeader">
        <InvoiceHeader>
            <xsl:attribute name="AmountCollected">
                <xsl:value-of select="@AmountCollected"/>
            </xsl:attribute>
            <xsl:attribute name="Currency">
                <xsl:value-of select="@Currency"/>
            </xsl:attribute>
            <xsl:attribute name="DateInvoiced">
                <xsl:value-of select="@DateInvoiced"/>
            </xsl:attribute>
            <xsl:attribute name="DocumentType">
                <xsl:value-of select="@DocumentType"/>
            </xsl:attribute>
            <xsl:attribute name="EnterpriseCode">
                <xsl:value-of select="@EnterpriseCode"/>
            </xsl:attribute>
            <xsl:attribute name="HeaderCharges">
                <xsl:value-of select="@HeaderCharges"/>
            </xsl:attribute>
            <xsl:attribute name="HeaderDiscount">
                <xsl:value-of select="@HeaderDiscount"/>
            </xsl:attribute>
            <xsl:attribute name="HeaderTax">
                <xsl:value-of select="@HeaderTax"/>
            </xsl:attribute>
            <xsl:attribute name="InvoiceCreationReason">
                <xsl:value-of select="@InvoiceCreationReason"/>
            </xsl:attribute>
            <xsl:attribute name="InvoiceNo">
                <xsl:value-of select="@InvoiceNo"/>
            </xsl:attribute>
            <xsl:attribute name="InvoiceType">
                <xsl:value-of select="@InvoiceType"/>
            </xsl:attribute>
            <xsl:attribute name="LineSubTotal">
                <xsl:value-of select="@LineSubTotal"/>
            </xsl:attribute>
            <xsl:attribute name="OrderInvoiceKey">
                <xsl:value-of select="@OrderInvoiceKey"/>
            </xsl:attribute>
            <xsl:attribute name="OrderNo">
                <xsl:value-of select="@OrderNo"/>
            </xsl:attribute>
            <xsl:attribute name="Reference1">
                <xsl:value-of select="@Reference1"/>
            </xsl:attribute>
            <xsl:attribute name="TotalAmount">
                <xsl:value-of select="@TotalAmount"/>
            </xsl:attribute>
            <xsl:attribute name="TotalCharges">
                <xsl:value-of select="@TotalCharges"/>
            </xsl:attribute>
            <xsl:attribute name="TotalDiscount">
                <xsl:value-of select="@TotalDiscount"/>
            </xsl:attribute>
            <xsl:attribute name="TotalHeaderCharges">
                <xsl:value-of select="@TotalHeaderCharges"/>
            </xsl:attribute>
            <xsl:attribute name="TotalTax">
                <xsl:value-of select="@TotalTax"/>
            </xsl:attribute>

            <xsl:apply-templates select="Order"/>
            <xsl:apply-templates select="Shipment"/>
            <xsl:apply-templates select="LineDetails"/>
            <xsl:apply-templates select="TotalSummary"/>
            <xsl:apply-templates select="HeaderCharges"/>
            <xsl:apply-templates select="HeaderTaxes"/>
        </InvoiceHeader>
    </xsl:template>

    <!-- Template to match the Order element -->
    <xsl:template match="Order">
        <Order>
            <xsl:attribute name="DocumentType">
                <xsl:value-of select="@DocumentType"/>
            </xsl:attribute>
            <xsl:attribute name="EnterpriseCode">
                <xsl:value-of select="@EnterpriseCode"/>
            </xsl:attribute>
            <xsl:attribute name="EntryType">
                <xsl:value-of select="@EntryType"/>
            </xsl:attribute>
            <xsl:attribute name="OrderDate">
                <xsl:value-of select="@OrderDate"/>
            </xsl:attribute>
            <xsl:attribute name="OrderHeaderKey">
                <xsl:value-of select="@OrderHeaderKey"/>
            </xsl:attribute>
            <xsl:attribute name="OrderNo">
                <xsl:value-of select="@OrderNo"/>
            </xsl:attribute>
            <xsl:attribute name="SellerOrganizationCode">
                <xsl:value-of select="@SellerOrganizationCode"/>
            </xsl:attribute>
            <xsl:attribute name="Status">
                <xsl:value-of select="@Status"/>
            </xsl:attribute>

            <xsl:apply-templates select="PriceInfo"/>
            <xsl:apply-templates select="PersonInfoShipTo"/>
            <xsl:apply-templates select="PersonInfoBillTo"/>
        </Order>
    </xsl:template>

    <!-- Template to match the PriceInfo element -->
    <xsl:template match="PriceInfo">
        <PriceInfo>
            <xsl:attribute name="Currency">
                <xsl:value-of select="@Currency"/>
            </xsl:attribute>
        </PriceInfo>
    </xsl:template>

    <!-- Template to match the PersonInfoShipTo element -->
    <xsl:template match="PersonInfoShipTo">
        <PersonInfoShipTo>
            <xsl:attribute name="AddressLine1">
                <xsl:value-of select="@AddressLine1"/>
            </xsl:attribute>
            <xsl:attribute name="AddressLine2">
                <xsl:value-of select="@AddressLine2"/>
            </xsl:attribute>
            <xsl:attribute name="AddressLine3">
                <xsl:value-of select="@AddressLine3"/>
            </xsl:attribute>
            <xsl:attribute name="City">
                <xsl:value-of select="@City"/>
            </xsl:attribute>
            <xsl:attribute name="Company">
                <xsl:value-of select="@Company"/>
            </xsl:attribute>
            <xsl:attribute name="Country">
                <xsl:value-of select="@Country"/>
            </xsl:attribute>
            <xsl:attribute name="DayPhone">
                <xsl:value-of select="@DayPhone"/>
            </xsl:attribute>
            <xsl:attribute name="EMailID">
                <xsl:value-of select="@EMailID"/>
            </xsl:attribute>
            <xsl:attribute name="FirstName">
                <xsl:value-of select="@FirstName"/>
            </xsl:attribute>
            <xsl:attribute name="LastName">
                <xsl:value-of select="@LastName"/>
            </xsl:attribute>
            <xsl:attribute name="MiddleName">
                <xsl:value-of select="@MiddleName"/>
            </xsl:attribute>
            <xsl:attribute name="State">
                <xsl:value-of select="@State"/>
            </xsl:attribute>
            <xsl:attribute name="ZipCode">
                <xsl:value-of select="@ZipCode"/>
            </xsl:attribute>
        </PersonInfoShipTo>
    </xsl:template>

    <!-- Template to match the PersonInfoBillTo element -->
    <xsl:template match="PersonInfoBillTo">
        <PersonInfoBillTo>
            <xsl:attribute name="AddressLine1">
                <xsl:value-of select="@AddressLine1"/>
            </xsl:attribute>
            <xsl:attribute name="AddressLine2">
                <xsl:value-of select="@AddressLine2"/>
            </xsl:attribute>
            <xsl:attribute name="AddressLine3">
                <xsl:value-of select="@AddressLine3"/>
            </xsl:attribute>
            <xsl:attribute name="City">
                <xsl:value-of select="@City"/>
            </xsl:attribute>
            <xsl:attribute name="Company">
                <xsl:value-of select="@Company"/>
            </xsl:attribute>
            <xsl:attribute name="Country">
                <xsl:value-of select="@Country"/>
            </xsl:attribute>
            <xsl:attribute name="DayPhone">
                <xsl:value-of select="@DayPhone"/>
            </xsl:attribute>
            <xsl:attribute name="EMailID">
                <xsl:value-of select="@EMailID"/>
            </xsl:attribute>
            <xsl:attribute name="FirstName">
                <xsl:value-of select="@FirstName"/>
            </xsl:attribute>
            <xsl:attribute name="LastName">
                <xsl:value-of select="@LastName"/>
            </xsl:attribute>
            <xsl:attribute name="MiddleName">
                <xsl:value-of select="@MiddleName"/>
            </xsl:attribute>
            <xsl:attribute name="State">
                <xsl:value-of select="@State"/>
            </xsl:attribute>
            <xsl:attribute name="ZipCode">
                <xsl:value-of select="@ZipCode"/>
            </xsl:attribute>
        </PersonInfoBillTo>
    </xsl:template>

    <!-- Template to match the Shipment element -->
    <xsl:template match="Shipment">
        <Shipment>
            <xsl:attribute name="CarrierServiceCode">
                <xsl:value-of select="@CarrierServiceCode"/>
            </xsl:attribute>
            <xsl:attribute name="SCAC">
                <xsl:value-of select="@SCAC"/>
            </xsl:attribute>
            <xsl:attribute name="ShipDate">
                <xsl:value-of select="@ShipDate"/>
            </xsl:attribute>
            <xsl:attribute name="ShipNode">
                <xsl:value-of select="@ShipNode"/>
            </xsl:attribute>
            <xsl:attribute name="ShipmentKey">
                <xsl:value-of select="@ShipmentKey"/>
            </xsl:attribute>
            <xsl:attribute name="ShipmentNo">
                <xsl:value-of select="@ShipmentNo"/>
            </xsl:attribute>

            <xsl:apply-templates select="ToAddress"/>
            <xsl:apply-templates select="ShipNode"/>
        </Shipment>
    </xsl:template>

    <!-- Template to match the ToAddress element -->
    <xsl:template match="ToAddress">
        <ToAddress>
            <xsl:attribute name="AddressLine1">
                <xsl:value-of select="@AddressLine1"/>
            </xsl:attribute>
            <xsl:attribute name="AddressLine2">
                <xsl:value-of select="@AddressLine2"/>
            </xsl:attribute>
            <xsl:attribute name="AddressLine3">
                <xsl:value-of select="@AddressLine3"/>
            </xsl:attribute>
            <xsl:attribute name="City">
                <xsl:value-of select="@City"/>
            </xsl:attribute>
            <xsl:attribute name="Company">
                <xsl:value-of select="@Company"/>
            </xsl:attribute>
            <xsl:attribute name="Country">
                <xsl:value-of select="@Country"/>
            </xsl:attribute>
            <xsl:attribute name="DayPhone">
                <xsl:value-of select="@DayPhone"/>
            </xsl:attribute>
            <xsl:attribute name="EMailID">
                <xsl:value-of select="@EMailID"/>
            </xsl:attribute>
            <xsl:attribute name="FirstName">
                <xsl:value-of select="@FirstName"/>
            </xsl:attribute>
            <xsl:attribute name="LastName">
                <xsl:value-of select="@LastName"/>
            </xsl:attribute>
            <xsl:attribute name="MiddleName">
                <xsl:value-of select="@MiddleName"/>
            </xsl:attribute>
            <xsl:attribute name="State">
                <xsl:value-of select="@State"/>
            </xsl:attribute>
            <xsl:attribute name="ZipCode">
                <xsl:value-of select="@ZipCode"/>
            </xsl:attribute>
        </ToAddress>
    </xsl:template>
    <!-- Template to match the shipNode element -->
    <xsl:template match="ShipNode">
        <ShipNode>
            <xsl:attribute name="IdentifiedByParentAs">
                <xsl:value-of select="@IdentifiedByParentAs"/>
            </xsl:attribute>
            <xsl:attribute name="NodeOrgCode">
                <xsl:value-of select="@NodeOrgCode"/>
            </xsl:attribute>
            <xsl:attribute name="NodeType">
                <xsl:value-of select="@NodeType"/>
            </xsl:attribute>
            <xsl:attribute name="OwnerKey">
                <xsl:value-of select="@OwnerKey"/>
            </xsl:attribute>
            <xsl:attribute name="ShipnodeKey">
                <xsl:value-of select="@ShipnodeKey"/>
            </xsl:attribute>
        </ShipNode>
    </xsl:template>

    <!-- Template to match the LineDetails element -->
    <xsl:template match="LineDetails">
        <LineDetails>
            <xsl:attribute name="TotalLines">
                <xsl:value-of select="@TotalLines"/>
            </xsl:attribute>
            <xsl:apply-templates select="LineDetail"/>
        </LineDetails>
    </xsl:template>

    <!-- Template to match the LineDetail element -->
    <xsl:template match="LineDetail">
        <LineDetail>
            <xsl:attribute name="Charges">
                <xsl:value-of select="@Charges"/>
            </xsl:attribute>
            <xsl:attribute name="ExtendedPrice">
                <xsl:value-of select="@ExtendedPrice"/>
            </xsl:attribute>
            <xsl:attribute name="ItemID">
                <xsl:value-of select="@ItemID"/>
            </xsl:attribute>
            <xsl:attribute name="LineTotal">
                <xsl:value-of select="@LineTotal"/>
            </xsl:attribute>
            <xsl:attribute name="OrderInvoiceDetailKey">
                <xsl:value-of select="@OrderInvoiceDetailKey"/>
            </xsl:attribute>
            <xsl:attribute name="OrderLineKey">
                <xsl:value-of select="@OrderLineKey"/>
            </xsl:attribute>
            <xsl:attribute name="PrimeLineNo">
                <xsl:value-of select="@PrimeLineNo"/>
            </xsl:attribute>
            <xsl:attribute name="Quantity">
                <xsl:value-of select="@Quantity"/>
            </xsl:attribute>
            <xsl:attribute name="Tax">
                <xsl:value-of select="@Tax"/>
            </xsl:attribute>
            <xsl:attribute name="UnitOfMeasure">
                <xsl:value-of select="@UnitOfMeasure"/>
            </xsl:attribute>
            <xsl:attribute name="UnitPrice">
                <xsl:value-of select="@UnitPrice"/>
            </xsl:attribute>

            <xsl:apply-templates select="OrderLine"/>
            <xsl:apply-templates select="LineCharges"/>
            <xsl:apply-templates select="LineTaxes"/>
        </LineDetail>
    </xsl:template>

    <!-- Template to match the OrderLine element -->
    <xsl:template match="OrderLine">
        <OrderLine>
            <xsl:attribute name="CarrierServiceCode">
                <xsl:value-of select="@CarrierServiceCode"/>
            </xsl:attribute>
            <xsl:attribute name="DeliveryMethod">
                <xsl:value-of select="@DeliveryMethod"/>
            </xsl:attribute>
            <xsl:attribute name="OrderLineKey">
                <xsl:value-of select="@OrderLineKey"/>
            </xsl:attribute>
            <xsl:attribute name="OrderedQty">
                <xsl:value-of select="@OrderedQty"/>
            </xsl:attribute>
            <xsl:attribute name="PrimeLineNo">
                <xsl:value-of select="@PrimeLineNo"/>
            </xsl:attribute>
            <xsl:attribute name="ReturnReason">
                <xsl:value-of select="@ReturnReason"/>
            </xsl:attribute>
            <xsl:attribute name="Status">
                <xsl:value-of select="@Status"/>
            </xsl:attribute>

            <xsl:apply-templates select="Item"/>
            <xsl:apply-templates select="LinePriceInfo"/>
        </OrderLine>
    </xsl:template>

    <!-- Template to match the Item element -->
    <xsl:template match="Item">
        <Item>
            <xsl:attribute name="ItemID">
                <xsl:value-of select="@ItemID"/>
            </xsl:attribute>
            <xsl:attribute name="ItemShortDesc">
                <xsl:value-of select="@ItemShortDesc"/>
            </xsl:attribute>
            <xsl:attribute name="UPCCode">
                <xsl:value-of select="@UPCCode"/>
            </xsl:attribute>
        </Item>
    </xsl:template>

    <!-- Template to match the LinePriceInfo element -->
    <xsl:template match="LinePriceInfo">
        <LinePriceInfo>
            <xsl:attribute name="DiscountPercentage">
                <xsl:value-of select="@DiscountPercentage"/>
            </xsl:attribute>
            <xsl:attribute name="DiscountReference">
                <xsl:value-of select="@DiscountReference"/>
            </xsl:attribute>
            <xsl:attribute name="DiscountType">
                <xsl:value-of select="@DiscountType"/>
            </xsl:attribute>
            <xsl:attribute name="InvoicedPricingQty">
                <xsl:value-of select="@InvoicedPricingQty"/>
            </xsl:attribute>
            <xsl:attribute name="ListPrice">
                <xsl:value-of select="@ListPrice"/>
            </xsl:attribute>
            <xsl:attribute name="OrderedPricingQty">
                <xsl:value-of select="@OrderedPricingQty"/>
            </xsl:attribute>
            <xsl:attribute name="UnitPrice">
                <xsl:value-of select="@UnitPrice"/>
            </xsl:attribute>
        </LinePriceInfo>
    </xsl:template>

    <!-- Template to match the LineCharges element -->
    <xsl:template match="LineCharges">
        <LineCharges>
            <xsl:apply-templates select="LineCharge"/>
        </LineCharges>
    </xsl:template>

    <!-- Template to match the LineCharge element -->
    <xsl:template match="LineCharge">
        <LineCharge>
            <xsl:attribute name="AmountFromAddnlLinePrices">
                <xsl:value-of select="@AmountFromAddnlLinePrices"/>
            </xsl:attribute>
            <xsl:attribute name="ChargeAmount">
                <xsl:value-of select="@ChargeAmount"/>
            </xsl:attribute>
            <xsl:attribute name="ChargeCategory">
                <xsl:value-of select="@ChargeCategory"/>
            </xsl:attribute>
            <xsl:attribute name="ChargeName">
                <xsl:value-of select="@ChargeName"/>
            </xsl:attribute>
            <xsl:attribute name="ChargeNameKey">
                <xsl:value-of select="@ChargeNameKey"/>
            </xsl:attribute>
            <xsl:attribute name="ChargePerLine">
                <xsl:value-of select="@ChargePerLine"/>
            </xsl:attribute>
            <xsl:attribute name="ChargePerUnit">
                <xsl:value-of select="@ChargePerUnit"/>
            </xsl:attribute>
            <xsl:attribute name="IsBillable">
                <xsl:value-of select="@IsBillable"/>
            </xsl:attribute>
            <xsl:attribute name="IsDiscount">
                <xsl:value-of select="@IsDiscount"/>
            </xsl:attribute>
            <xsl:attribute name="IsManual">
                <xsl:value-of select="@IsManual"/>
            </xsl:attribute>
            <xsl:attribute name="IsShippingCharge">
                <xsl:value-of select="@IsShippingCharge"/>
            </xsl:attribute>
            <xsl:attribute name="OriginalChargePerLine">
                <xsl:value-of select="@OriginalChargePerLine"/>
            </xsl:attribute>
            <xsl:attribute name="OriginalChargePerUnit">
                <xsl:value-of select="@OriginalChargePerUnit"/>
            </xsl:attribute>
            <xsl:attribute name="Reference">
                <xsl:value-of select="@Reference"/>
            </xsl:attribute>
            <xsl:attribute name="Tax">
                <xsl:value-of select="@Tax"/>
            </xsl:attribute>

            <xsl:apply-templates select="Extn"/>
        </LineCharge>
    </xsl:template>

    <!-- Template to match the Extn element -->
    <xsl:template match="Extn">
        <Extn>
            <xsl:attribute name="ExtnDWPromotionId">
                <xsl:value-of select="@ExtnDWPromotionId"/>
            </xsl:attribute>
            <xsl:attribute name="ExtnPromotionId">
                <xsl:value-of select="@ExtnPromotionId"/>
            </xsl:attribute>
            <xsl:attribute name="ExtnPromotionText">
                <xsl:value-of select="@ExtnPromotionText"/>
            </xsl:attribute>
        </Extn>
    </xsl:template>

    <!-- Template to match the LineTaxes element -->
    <xsl:template match="LineTaxes">
        <LineTaxes>
            <xsl:apply-templates select="LineTax"/>
            <xsl:apply-templates select="TaxSummary"/>
        </LineTaxes>
    </xsl:template>

    <!-- Template to match the LineTax element -->
    <xsl:template match="LineTax">
        <LineTax>
            <xsl:attribute name="ChargeCategory">
                <xsl:value-of select="@ChargeCategory"/>
            </xsl:attribute>
            <xsl:attribute name="ChargeName">
                <xsl:value-of select="@ChargeName"/>
            </xsl:attribute>
            <xsl:attribute name="ChargeNameKey">
                <xsl:value-of select="@ChargeNameKey"/>
            </xsl:attribute>
            <xsl:attribute name="Reference_1">
                <xsl:value-of select="@Reference_1"/>
            </xsl:attribute>
            <xsl:attribute name="Reference_2">
                <xsl:value-of select="@Reference_2"/>
            </xsl:attribute>
            <xsl:attribute name="Reference_3">
                <xsl:value-of select="@Reference_3"/>
            </xsl:attribute>
            <xsl:attribute name="Tax">
                <xsl:value-of select="@Tax"/>
            </xsl:attribute>
            <xsl:attribute name="TaxName">
                <xsl:value-of select="@TaxName"/>
            </xsl:attribute>
            <xsl:attribute name="TaxPercentage">
                <xsl:value-of select="@TaxPercentage"/>
            </xsl:attribute>
        </LineTax>
    </xsl:template>

    <!-- Template to match the TaxSummary element -->
    <xsl:template match="TaxSummary">
        <TaxSummary>
            <xsl:apply-templates select="TaxSummaryDetail"/>
        </TaxSummary>
    </xsl:template>

    <!-- Template to match the TaxSummaryDetail element -->
    <xsl:template match="TaxSummaryDetail">
        <TaxSummaryDetail>
            <xsl:attribute name="Tax">
                <xsl:value-of select="@Tax"/>
            </xsl:attribute>
            <xsl:attribute name="TaxName">
                <xsl:value-of select="@TaxName"/>
            </xsl:attribute>
        </TaxSummaryDetail>
    </xsl:template>

    <!-- Template to match the TotalSummary element -->
    <xsl:template match="TotalSummary">
        <TotalSummary>
            <xsl:apply-templates select="ChargeSummary"/>
            <xsl:apply-templates select="TaxSummary"/>
        </TotalSummary>
    </xsl:template>

    <!-- Template to match the ChargeSummary element -->
    <xsl:template match="ChargeSummary">
        <ChargeSummary>
            <xsl:apply-templates select="ChargeSummaryDetail"/>
        </ChargeSummary>
    </xsl:template>

    <!-- Template to match the ChargeSummaryDetail element -->
    <xsl:template match="ChargeSummaryDetail">
        <ChargeSummaryDetail>
            <xsl:attribute name="ChargeAmount">
                <xsl:value-of select="@ChargeAmount"/>
            </xsl:attribute>
            <xsl:attribute name="ChargeCategory">
                <xsl:value-of select="@ChargeCategory"/>
            </xsl:attribute>
            <xsl:attribute name="ChargeName">
                <xsl:value-of select="@ChargeName"/>
            </xsl:attribute>
            <xsl:attribute name="ChargeNameKey">
                <xsl:value-of select="@ChargeNameKey"/>
            </xsl:attribute>
            <xsl:attribute name="IsBillable">
                <xsl:value-of select="@IsBillable"/>
            </xsl:attribute>
            <xsl:attribute name="IsDiscount">
                <xsl:value-of select="@IsDiscount"/>
            </xsl:attribute>
            <xsl:attribute name="IsShippingCharge">
                <xsl:value-of select="@IsShippingCharge"/>
            </xsl:attribute>
            <xsl:attribute name="OriginalChargeAmount">
                <xsl:value-of select="@OriginalChargeAmount"/>
            </xsl:attribute>
            <xsl:attribute name="Reference">
                <xsl:value-of select="@Reference"/>
            </xsl:attribute>

            <xsl:apply-templates select="Extn"/>
        </ChargeSummaryDetail>
    </xsl:template>

    <!-- Template to match the HeaderCharges element -->
    <xsl:template match="HeaderCharges">
        <HeaderCharges>
            <xsl:apply-templates select="HeaderCharge"/>
        </HeaderCharges>
    </xsl:template>

    <!-- Template to match the HeaderCharge element -->
    <xsl:template match="HeaderCharge">
        <HeaderCharge>
            <xsl:attribute name="ChargeAmount">
                <xsl:value-of select="@ChargeAmount"/>
            </xsl:attribute>
            <xsl:attribute name="ChargeCategory">
                <xsl:value-of select="@ChargeCategory"/>
            </xsl:attribute>
            <xsl:attribute name="ChargeName">
                <xsl:value-of select="@ChargeName"/>
            </xsl:attribute>
            <xsl:attribute name="ChargeNameKey">
                <xsl:value-of select="@ChargeNameKey"/>
            </xsl:attribute>
            <xsl:attribute name="IsBillable">
                <xsl:value-of select="@IsBillable"/>
            </xsl:attribute>
            <xsl:attribute name="IsDiscount">
                <xsl:value-of select="@IsDiscount"/>
            </xsl:attribute>
            <xsl:attribute name="IsShippingCharge">
                <xsl:value-of select="@IsShippingCharge"/>
            </xsl:attribute>
            <xsl:attribute name="OriginalChargeAmount">
                <xsl:value-of select="@OriginalChargeAmount"/>
            </xsl:attribute>
            <xsl:attribute name="Reference">
                <xsl:value-of select="@Reference"/>
            </xsl:attribute>

            <xsl:apply-templates select="Extn"/>
        </HeaderCharge>
    </xsl:template>

    <!-- Template to match the HeaderTaxes element -->
    <xsl:template match="HeaderTaxes">
        <HeaderTaxes>
            <xsl:apply-templates select="HeaderTax"/>
            <xsl:apply-templates select="TaxSummary"/>
        </HeaderTaxes>
    </xsl:template>

    <!-- Template to match the HeaderTax element -->
    <xsl:template match="HeaderTax">
        <HeaderTax>
            <xsl:attribute name="ChargeCategory">
                <xsl:value-of select="@ChargeCategory"/>
            </xsl:attribute>
            <xsl:attribute name="ChargeName">
                <xsl:value-of select="@ChargeName"/>
            </xsl:attribute>
            <xsl:attribute name="ChargeNameKey">
                <xsl:value-of select="@ChargeNameKey"/>
            </xsl:attribute>
            <xsl:attribute name="Reference_1">
                <xsl:value-of select="@Reference_1"/>
            </xsl:attribute>
            <xsl:attribute name="Reference_2">
                <xsl:value-of select="@Reference_2"/>
            </xsl:attribute>
            <xsl:attribute name="Reference_3">
                <xsl:value-of select="@Reference_3"/>
            </xsl:attribute>
            <xsl:attribute name="Tax">
                <xsl:value-of select="@Tax"/>
            </xsl:attribute>
            <xsl:attribute name="TaxName">
                <xsl:value-of select="@TaxName"/>
            </xsl:attribute>
            <xsl:attribute name="TaxPercentage">
                <xsl:value-of select="@TaxPercentage"/>
            </xsl:attribute>
        </HeaderTax>
    </xsl:template>
</xsl:stylesheet>
