<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://www.w3.org">
    <xsl:output method="xml" indent="yes"/>
    <!-- Key to identify unique promotions -->
    <xsl:key name="unique-promos" match="OrderLine/LineCharges/LineCharge/Extn"
             use="@ExtnDWPromotionId"/>
    <!-- Template for matching the root element (Order)-->
    <xsl:template match="/Order">
        <Order>
            <xsl:attribute name="OrderNo">
                <xsl:value-of select="@OrderNo"/>
            </xsl:attribute>
            <xsl:attribute name="EnteredBy">
                <xsl:value-of select="@EnteredBy"/>
            </xsl:attribute>
            <xsl:attribute name="CustomerPONo">
                <xsl:value-of select="@CustomerPONo"/>
            </xsl:attribute>
            <xsl:attribute name="CustomerEMailID">
                <xsl:value-of select="@CustomerEMailID"/>
            </xsl:attribute>
            <xsl:attribute name="OrderDate">
                <!-- Extract the date part (before the 'T') -->
                <xsl:variable name="date-part" select="substring(@OrderDate, 1, 10)"/>

                <!-- Output the date in the desired format (MM/DD/YYYY) -->
                <xsl:value-of
                        select="concat(substring($date-part, 6, 2), '/', substring($date-part, 9, 2), '/', substring($date-part, 1, 4))"/>
            </xsl:attribute>

            <xsl:attribute name="EnterpriseCode">
                <xsl:value-of select="@EnterpriseCode"/>
            </xsl:attribute>

            <!-- Add CurrencySymbol attribute directly at Order (header) level -->
            <xsl:attribute name="CurrencySymbol">
                <xsl:choose>
                    <xsl:when test="PriceInfo/@Currency = 'USD'">$</xsl:when>
                    <xsl:when test="PriceInfo/@Currency = 'CAD'">C$</xsl:when>
                    <xsl:when test="PriceInfo/@Currency = 'AUD'">A$</xsl:when>
                    <xsl:when test="PriceInfo/@Currency = 'GBP'">£</xsl:when>
                    <xsl:when test="PriceInfo/@Currency = 'EUR'">€</xsl:when>
                    <xsl:when test="PriceInfo/@Currency = 'SGD'">S$</xsl:when>
                    <xsl:when test="PriceInfo/@Currency = 'KRW'">₩</xsl:when>
                    <xsl:otherwise>$</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>

            <Extn>
                <xsl:attribute name="ExtnCustomerLocale">
                    <xsl:value-of select="Extn/@ExtnCustomerLocale"/>
                </xsl:attribute>
            </Extn>
            <!-- Process PersonInfoShipTo -->
            <PersonInfoShipTo>
                <xsl:copy-of select="PersonInfoShipTo/@*"/>
            </PersonInfoShipTo>
            <!-- Process PersonInfoBillTo -->
            <PersonInfoBillTo>
                <xsl:copy-of select="PersonInfoBillTo/@*"/>
            </PersonInfoBillTo>
            <!-- Process OrderLines -->
            <OrderLines>
                <xsl:for-each select="OrderLines/OrderLine">
                    <OrderLine OrderedQty="{@OrderedQty}">
                        <xsl:variable name="locale">
                            <xsl:choose>
                                <xsl:when test="/Order/Extn/@ExtnCustomerLocale = 'default'">
                                    <xsl:text>en_US</xsl:text>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="/Order/Extn/@ExtnCustomerLocale"/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:variable>
                        <xsl:variable name="localeString" select="string($locale)"/>
                        <!-- Get the root-level EnterpriseCode -->
                        <xsl:variable name="enterpriseCode" select="/Order/@EnterpriseCode"/>
                        <!-- Try to find SizeCode where OrganizationCode = enterpriseCode -->
                        <xsl:variable name="orgSizeCode"
                                      select="ItemDetails/Extn/CrocsItemOrgDataList/CrocsItemOrgData[@OrganizationCode = $enterpriseCode][@Locale = $localeString]/@SizeCode"/>
                        <!-- Get color from matching locale -->
                        <xsl:variable name="orgColorCode"
                                      select="ItemDetails/Extn/CrocsItemOrgDataList/CrocsItemOrgData[@OrganizationCode = $enterpriseCode][@Locale = $localeString]/@Color"/>
                        <!-- Get ProductUrl from matching locale -->
                        <xsl:variable name="orgProductUrl"
                                      select="ItemDetails/Extn/CrocsItemOrgDataList/CrocsItemOrgData[@OrganizationCode = $enterpriseCode][@Locale = $localeString]/@ProductUrl"/>

                        <ItemDetails>
                            <PrimaryInformation>
                                <xsl:attribute name="ColorCode">
                                    <xsl:choose>
                                        <xsl:when test="string-length(normalize-space($orgColorCode)) > 0">
                                            <xsl:value-of select="$orgColorCode"/>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:value-of select="ItemDetails/PrimaryInformation/@ColorCode"/>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </xsl:attribute>
                                <xsl:attribute name="SizeCode">
                                    <xsl:choose>
                                        <xsl:when test="string-length(normalize-space($orgSizeCode)) > 0">
                                            <xsl:value-of select="$orgSizeCode"/>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:value-of select="ItemDetails/PrimaryInformation/@SizeCode"/>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </xsl:attribute>
                            </PrimaryInformation>
                        </ItemDetails>
                        <Item>
                            <xsl:attribute name="ItemDesc">
                                <xsl:value-of select="Item/@ItemDesc"/>
                            </xsl:attribute>
                            <xsl:attribute name="ItemID">
                                <xsl:value-of select="Item/@ItemID"/>
                            </xsl:attribute>
                            <Extn>
                                <xsl:attribute name="ExtnImageUrl">
                                    <xsl:value-of select="ItemDetails/Extn/@ExtnImageUrl"/>
                                </xsl:attribute>
                                <xsl:attribute name="ExtnProductUrl">
                                    <xsl:choose>
                                        <xsl:when test="string-length(normalize-space($orgProductUrl)) > 0">
                                            <xsl:value-of select="$orgProductUrl"/>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:value-of select="ItemDetails/Extn/@ExtnProductUrl"/>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </xsl:attribute>
                            </Extn>
                        </Item>
                        <!-- Process LineCharges -->
                        <LineCharges>
                            <!-- Only for the first OrderLine -->
                            <xsl:if test="position() = 1">
                                <!-- Loop over all unique promotions -->
                                <xsl:for-each select="/Order/OrderLines/OrderLine/LineCharges/LineCharge/Extn[
                                    generate-id() = generate-id(key('unique-promos', @ExtnDWPromotionId)[1])
                                ]">
                                    <LineCharge>
                                        <Extn>
                                            <xsl:attribute name="ExtnDWPromotionId">
                                                <xsl:value-of select="@ExtnDWPromotionId"/>
                                            </xsl:attribute>
                                            <xsl:attribute name="ExtnPromotionText">
                                                <xsl:value-of select="@ExtnPromotionText"/>
                                            </xsl:attribute>
                                        </Extn>
                                    </LineCharge>
                                </xsl:for-each>
                            </xsl:if>
                        </LineCharges>
                        <!-- Process LinePriceInfo -->
                        <LinePriceInfo>
                            <xsl:attribute name="UnitPrice">
                                <xsl:value-of select="LinePriceInfo/@UnitPrice"/>
                            </xsl:attribute>
                        </LinePriceInfo>
                        <!-- Process LineOverallTotals -->
                        <LineOverallTotals>
                            <xsl:attribute name="Discount">
                                <xsl:value-of select="LineOverallTotals/@Discount"/>
                            </xsl:attribute>
                        </LineOverallTotals>
                    </OrderLine>
                </xsl:for-each>
            </OrderLines>
            <PaymentMethods>
                <xsl:for-each select="PaymentMethods/PaymentMethod">
                    <PaymentMethod>
                        <xsl:attribute name="MaxChargeLimit">
                            <xsl:value-of select="@MaxChargeLimit"/>
                        </xsl:attribute>
                        <xsl:attribute name="PaymentType">
                            <xsl:value-of select="@PaymentType"/>
                        </xsl:attribute>
                        <xsl:attribute name="CreditCardExpDate">
                            <xsl:value-of select="@CreditCardExpDate"/>
                        </xsl:attribute>
                        <xsl:attribute name="CreditCardType">
                            <xsl:value-of select="@CreditCardType"/>
                        </xsl:attribute>
                        <xsl:attribute name="DisplayCreditCardNo">
                            <xsl:value-of select="@DisplayCreditCardNo"/>
                        </xsl:attribute>
                    </PaymentMethod>
                </xsl:for-each>
            </PaymentMethods>
            <!-- Calculate the total discount and shipping charge -->
            <xsl:variable name="shippingCharge"
                          select="sum(/Order/OverallTotals/OverallChargeTotals/OverallChargeTotal[@ChargeName='ShippingCharge']/@GrandCharges)"/>
            <xsl:variable name="shippingDiscount"
                          select="sum(/Order/OverallTotals/OverallChargeTotals/OverallChargeTotal[@ChargeName='ShippingDiscount']/@GrandDiscount)"/>

            <!-- Calculate the final GrandCharges -->
            <xsl:variable name="finalShippingCharges" select="$shippingCharge - $shippingDiscount"/>

            <!-- Calculate the lineOverallDiscount discount and shipping charge -->
            <xsl:variable name="LineOverAllDiscount"
                          select="sum(/Order/OrderLines/OrderLine/LineOverallTotals/@Discount)"/>

            <!-- Process OverallTotals -->
            <OverallTotals>
                <xsl:attribute name="GrandCharges">
                    <xsl:value-of select="format-number($finalShippingCharges, '0.00')"/>
                </xsl:attribute>
                <xsl:attribute name="GrandDiscount">
                    <xsl:value-of select="format-number($LineOverAllDiscount, '0.00')"/>
                </xsl:attribute>
                <xsl:attribute name="GrandTax">
                    <xsl:value-of select="OverallTotals/@GrandTax"/>
                </xsl:attribute>
                <xsl:attribute name="GrandTotal">
                    <xsl:value-of select="OverallTotals/@GrandTotal"/>
                </xsl:attribute>
                <xsl:attribute name="LineSubTotal">
                    <xsl:value-of select="OverallTotals/@LineSubTotal"/>
                </xsl:attribute>
            </OverallTotals>
        </Order>
    </xsl:template>
    <!-- Identity template for copying unchanged nodes -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
