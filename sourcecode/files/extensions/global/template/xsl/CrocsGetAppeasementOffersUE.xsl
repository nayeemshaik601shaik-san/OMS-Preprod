<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" >
	<xsl:template match="/">
		<AppeasementOffers ChargeCategory="CUSTOMER_APPEASEMENT" ChargeName="CUSTOMER_APPEASEMENT" >
			<xsl:attribute name="OrderHeaderKey">
				<xsl:value-of select="//Order/@OrderHeaderKey"/>
			</xsl:attribute>
			<xsl:attribute name="ReasonCode">
				<xsl:value-of select="//AppeasementReason/@ReasonCode"/>
			</xsl:attribute>

			<AppeasementOffer OfferAmount="0.00">
				<xsl:attribute name="OfferType">
					<xsl:value-of select="'VARIABLE_AMOUNT_ORDER'"/>
				</xsl:attribute>
				<xsl:attribute name="Preferred">
					<xsl:value-of select="'Y'"/>
				</xsl:attribute>
				<Order ChargeCategory="CUSTOMER_APPEASEMENT" ChargeName="CUSTOMER_APPEASEMENT" HeaderOfferAmount="0.00"/>
				<OrderLines>
					<xsl:for-each select="//AppeasementOffers/Order/OrderLines/OrderLine">
						<OrderLine ChargeCategory="CUSTOMER_APPEASEMENT" ChargeName="CUSTOMER_APPEASEMENT" LineOfferAmount="0.00">
							<xsl:attribute name="OrderLineKey">
								<xsl:value-of select="./@OrderLineKey"/>
							</xsl:attribute>
							<xsl:attribute name="OrderedQty">
								<xsl:value-of select="./@OrderedQty"/>
							</xsl:attribute>
						</OrderLine>
					</xsl:for-each>
				</OrderLines>
			</AppeasementOffer>
		</AppeasementOffers>
	</xsl:template>
</xsl:stylesheet>