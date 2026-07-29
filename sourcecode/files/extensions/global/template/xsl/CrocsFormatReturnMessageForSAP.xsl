<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" >
	<xsl:template match="/">
		<InvoiceDetail xmlns="http://www.w3.org">
			<InvoiceHeader AmountCollected="0.00" Currency="USD" DocumentType="0003" InvoiceType="RETURN" Event="OrderCreate" SAPCarrierCode="" SAPShippingCondition="01">
				<xsl:attribute name="DateInvoiced">
					<xsl:value-of select="/Order/@OrderDate"/>
				</xsl:attribute>
				<xsl:attribute name="OrderNo">
					<xsl:value-of select="/Order/@OrderNo"/>
				</xsl:attribute>
				<xsl:attribute name="InvoiceNo">
					<xsl:value-of select="/Order/@OrderNo"/>
				</xsl:attribute>
				<xsl:attribute name="EnterpriseCode">
					<xsl:value-of select="/Order/@EnterpriseCode"/>
				</xsl:attribute>
				<xsl:attribute name="SAPInvoiceType">		
					<xsl:value-of select="''"></xsl:value-of>
				</xsl:attribute>
				<xsl:variable name="vEnterpriseCode" select="/Order/@EnterpriseCode" />
				<xsl:variable name="vRoundingRate">
					<xsl:choose>
						<xsl:when test="(//Order/OrderLines/OrderLine[1]/DerivedFromOrder/Extn/@ExtnRoundingRate !='0.00') and (//Order/OrderLines/OrderLine[1]/DerivedFromOrder/Extn/@ExtnRoundingRate &gt; 0)">
							<xsl:value-of select="//Order/OrderLines/OrderLine[1]/DerivedFromOrder/Extn/@ExtnRoundingRate"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="'1'"></xsl:value-of>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:variable>				
				<!-- <xsl:attribute name="SAPCarrierCode">
					<xsl:choose>
						<xsl:when test="//InvoiceHeader/@InvoiceType='SHIPMENT'">
							<xsl:value-of select="substring-before(//InvoiceHeader/@CarrierServiceCode,'-')"/>
						</xsl:when>
					</xsl:choose>
				</xsl:attribute> 
				<xsl:attribute name="SAPShippingCondition">
					<xsl:choose>
						<xsl:when test="//InvoiceHeader/@InvoiceType='SHIPMENT'">
							<xsl:value-of select="substring-after(//InvoiceHeader/@CarrierServiceCode,'-')"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="'01'"/>
					</xsl:otherwise>
					</xsl:choose>
				</xsl:attribute>-->
				<Order>
					<xsl:copy-of select="/Order/@*"/>
					<xsl:copy-of select="/Order/PersonInfoShipTo"/>
					<xsl:copy-of select="/Order/PersonInfoBillTo"/>
					<Extn>
						<xsl:copy-of select="/Order/Extn/@*"/>
					</Extn>
				</Order>
				<LineDetails>
					<xsl:copy-of select="/InvoiceDetail/InvoiceHeader/LineDetails/@*"/>	
					<xsl:for-each select="/Order/OrderLines/OrderLine">
						<LineDetail UnitOfMeasure="EACH" Tax="0.00" >
							<xsl:attribute name="ItemID">
								<xsl:value-of select="./Item/@ItemID"/>
							</xsl:attribute>
							<xsl:attribute name="PrimeLineNo">
								<xsl:value-of select="./@PrimeLineNo"/>
							</xsl:attribute>
							<xsl:attribute name="UnitPrice">
								<xsl:value-of select="format-number(((-1)*(./LinePriceInfo/@UnitPrice))* $vRoundingRate, '#.##')"/>
							</xsl:attribute>
							<xsl:attribute name="Quantity">
								<xsl:value-of select="./@OrderedQty"/>
							</xsl:attribute>
							<xsl:attribute name="LineTotal">
								<xsl:value-of select="format-number(((-1)*(./LineOverallTotals/@LineTotal))* $vRoundingRate, '#.##')"/>
							</xsl:attribute>
							<xsl:attribute name="ExtendedPrice">
								<xsl:value-of select="format-number(((-1)*(./LineOverallTotals/@LineTotal))* $vRoundingRate, '#.##')"/>
							</xsl:attribute>
							<LineTaxes />
							<LineCharges />
							<OrderLine>
								<xsl:copy-of select="./@*"/>
								<ItemDetails>
									<PrimaryInformation>
										<xsl:variable name="vItemId">
											<xsl:choose>
												<xsl:when test="contains(./Item/@ItemID,'-')">
													<xsl:value-of select="substring-after(./Item/@ItemID,'-')"></xsl:value-of>
												</xsl:when>
											</xsl:choose>
										</xsl:variable>	
										<xsl:attribute name="SizeCode">
											<xsl:choose>
												<xsl:when test="contains($vItemId,'-')">
													<xsl:value-of select="substring-after($vItemId,'-')"></xsl:value-of>
												</xsl:when>
												<xsl:otherwise>
													<xsl:value-of select="''"></xsl:value-of>
												</xsl:otherwise>
											</xsl:choose>
										</xsl:attribute>
									</PrimaryInformation>
								</ItemDetails>
								<DerivedFromOrderLine>
									<Order>
										<xsl:copy-of select="./DerivedFromOrder/@CustomerPONo"/>
										<xsl:copy-of select="./DerivedFromOrder/@CustCustPONo"/>
										<xsl:attribute name="OrderNo">
											<xsl:value-of select="concat(./DerivedFromOrder/@OrderNo,'R')"/>
										</xsl:attribute>
									</Order>
								</DerivedFromOrderLine>
								<xsl:copy-of select="./*[not(self:: ItemDetails) and not(self:: ItemLocaleList) and not(self:: ItemAliasList) and not(self:: OrderStatuses) and not(self:: DerivedFromOrder)] "/>
							</OrderLine>
						</LineDetail>
					</xsl:for-each>
				</LineDetails>	
			</InvoiceHeader>		
		</InvoiceDetail>
	</xsl:template>
</xsl:stylesheet>
