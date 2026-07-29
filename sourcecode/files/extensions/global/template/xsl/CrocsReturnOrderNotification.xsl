<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://www.w3.org" >
	<xsl:output method="xml" indent="yes"/>

	<xsl:template match="/Receipt">
		<Receipt ReceiptDate="{concat(substring(@ReceiptDate,6,2), '/', substring(@ReceiptDate,9,2), '/', substring(@ReceiptDate,1,4))}">
			<!-- Add CurrencySymbol attribute directly at Order (header) level -->
			<xsl:attribute name="CurrencySymbol">
				<xsl:choose>
					<xsl:when test="ReceiptLines/ReceiptLine/OrderLine/Order/PriceInfo/@Currency = 'USD'">$</xsl:when>
					<xsl:when test="ReceiptLines/ReceiptLine/OrderLine/Order/PriceInfo/@Currency = 'CAD'">C$</xsl:when>
					<xsl:when test="ReceiptLines/ReceiptLine/OrderLine/Order/PriceInfo/@Currency = 'AUD'">A$</xsl:when>
					<xsl:when test="ReceiptLines/ReceiptLine/OrderLine/Order/PriceInfo/@Currency = 'EUR'">€</xsl:when>
					<xsl:when test="ReceiptLines/ReceiptLine/OrderLine/Order/PriceInfo/@Currency = 'GBP'">£</xsl:when>
					<xsl:when test="ReceiptLines/ReceiptLine/OrderLine/Order/PriceInfo/@Currency = 'SGD'">S$</xsl:when>
					 <xsl:when test="ReceiptLines/ReceiptLine/OrderLine/Order/PriceInfo/@Currency = 'KRW'">₩</xsl:when>
					<xsl:otherwise>$</xsl:otherwise>
				</xsl:choose>
			</xsl:attribute>
			<xsl:attribute name="EnterpriseCode">
				<xsl:value-of select="ReceiptLines/ReceiptLine/OrderLine/DerivedFromOrder/@EnterpriseCode"/>
			</xsl:attribute>
			<!-- Loop through all receipt lines -->
			<ReceiptLines>
				<xsl:for-each select="ReceiptLines/ReceiptLine">
					<ReceiptLine OrderNo="{@OrderNo}">
						<xsl:attribute name="EnteredBy">
							<xsl:value-of select="OrderLine/Order/@EnteredBy"/>
						</xsl:attribute>
						<xsl:attribute name="CustomerPONo">
							<xsl:value-of select="OrderLine/Order/@CustomerPONo"/>
						</xsl:attribute>
						<OrderLine OrderedQty="{OrderLine/@OrderedQty}">
							<!-- Process OverallTotals -->
							<Order SCAC="{OrderLine/Order/@SCAC}">
								<OverallTotals
										GrandCharges="{OrderLine/Order/OverallTotals/@GrandCharges}"
										GrandDiscount="{OrderLine/Order/OverallTotals/@GrandDiscount}"
										GrandTax="{OrderLine/Order/OverallTotals/@GrandTax}"
										GrandTotal="{OrderLine/Order/OverallTotals/@GrandTotal}"
										LineSubTotal="{OrderLine/Order/OverallTotals/@LineSubTotal}"/>
							</Order>
							<!-- Item Info -->
							<Item ItemDesc="{OrderLine/Item/@ItemDesc}" ItemID="{OrderLine/Item/@ItemID}">
							</Item>
							<!-- Extract locale-->
							<xsl:variable name="locale">
								<xsl:choose>
									<xsl:when test="OrderLine/DerivedFromOrder/Extn/@ExtnCustomerLocale = 'default'">
										<xsl:text>en_US</xsl:text>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="OrderLine/DerivedFromOrder/Extn/@ExtnCustomerLocale"/>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:variable>
							<xsl:variable name="localeString" select="string($locale)"/>
							<!-- Get the root-level EnterpriseCode -->
							<xsl:variable name="enterpriseCode" select="OrderLine/DerivedFromOrder/@EnterpriseCode"/>
							<!-- Try to find SizeCode where OrganizationCode = enterpriseCode -->
							<xsl:variable name="orgSizeCode"
										  select="OrderLine/ItemDetails/Extn/CrocsItemOrgDataList/CrocsItemOrgData[@OrganizationCode = $enterpriseCode][@Locale = $localeString]/@SizeCode"/>
							<!-- Get color from matching locale -->
							<xsl:variable name="orgColorCode"
										  select="OrderLine/ItemDetails/Extn/CrocsItemOrgDataList/CrocsItemOrgData[@OrganizationCode = $enterpriseCode][@Locale = $localeString]/@Color"/>
							<!-- Get ProductUrl from matching locale -->
							<xsl:variable name="orgProductUrl"
										  select="OrderLine/ItemDetails/Extn/CrocsItemOrgDataList/CrocsItemOrgData[@OrganizationCode = $enterpriseCode][@Locale = $localeString]/@ProductUrl"/>

							<ItemDetails>
								<Extn>
									<xsl:attribute name="ExtnImageUrl">
										<xsl:value-of select="OrderLine/ItemDetails/Extn/@ExtnImageUrl"/>
									</xsl:attribute>
									<!-- Assign ExtnProductUrl -->
									<xsl:attribute name="ExtnProductUrl">
										<xsl:choose>
											<xsl:when test="string-length(normalize-space($orgProductUrl)) > 0">
												<xsl:value-of select="$orgProductUrl"/>
											</xsl:when>
											<xsl:otherwise>
												<xsl:value-of select="OrderLine/ItemDetails/Extn/@ExtnProductUrl"/>
											</xsl:otherwise>
										</xsl:choose>
									</xsl:attribute>
								</Extn>
								<PrimaryInformation>
								<!-- Override or fall back SizeCode -->
								<xsl:attribute name="SizeCode">
									<xsl:choose>
										<xsl:when test="string-length(normalize-space($orgSizeCode)) > 0">
											<xsl:value-of select="$orgSizeCode"/>
										</xsl:when>
										<xsl:otherwise>
											<xsl:value-of select="OrderLine/ItemDetails/PrimaryInformation/@SizeCode"/>
										</xsl:otherwise>
									</xsl:choose>
								</xsl:attribute>
								<!-- Assign ColorCode -->
								<xsl:attribute name="ColorCode">
									<xsl:choose>
										<xsl:when test="string-length(normalize-space($orgColorCode)) > 0">
											<xsl:value-of select="$orgColorCode"/>
										</xsl:when>
										<xsl:otherwise>
											<xsl:value-of select="OrderLine/ItemDetails/PrimaryInformation/@ColorCode"/>
										</xsl:otherwise>
									</xsl:choose>
								</xsl:attribute>
							</PrimaryInformation>
							</ItemDetails>

							<LineOverallTotals
									Discount="{OrderLine/LineOverallTotals/@Discount}"
									UnitPrice="{OrderLine/LineOverallTotals/@UnitPrice}"/>

							<CustomAttributes
									Text1="{OrderLine/CustomAttributes/@Text1}"
									Text2="{OrderLine/CustomAttributes/@Text2}"/>

							<!-- Derived Order Info -->
							<xsl:for-each select="OrderLine/DerivedFromOrder">
								<DerivedFromOrder
										OrderNo="{@OrderNo}"
										EnteredBy="{@EnteredBy}"
								        CustomerPONo="{@CustomerPONo}"
										CustomerEMailID="{@CustomerEMailID}"
										OrderDate="{concat(substring(@OrderDate,6,2), '/', substring(@OrderDate,9,2), '/', substring(@OrderDate,1,4))}">

									<Extn ExtnCustomerLocale="{Extn/@ExtnCustomerLocale}"/>

									<PersonInfoShipTo>
										<xsl:copy-of select="PersonInfoShipTo/@*"/>
									</PersonInfoShipTo>

									<PersonInfoBillTo>
										<xsl:copy-of select="PersonInfoBillTo/@*"/>
									</PersonInfoBillTo>

									<PaymentMethods>
										<xsl:for-each select="PaymentMethods/PaymentMethod">
											<PaymentMethod
													PaymentType="{@PaymentType}"
													CreditCardType="{@CreditCardType}"
													CreditCardExpDate="{@CreditCardExpDate}"
													DisplayCreditCardNo="{@DisplayCreditCardNo}"
													MaxChargeLimit="{@MaxChargeLimit}"/>
										</xsl:for-each>
									</PaymentMethods>
								</DerivedFromOrder>
							</xsl:for-each>
						</OrderLine>
					</ReceiptLine>
				</xsl:for-each>
			</ReceiptLines>
		</Receipt>
	</xsl:template>
</xsl:stylesheet>
