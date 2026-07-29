<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="/">
		<Order>
			<xsl:copy-of select="/Order/@*" />
			<xsl:copy-of select="/Order/*[not(self::OrderLines)]" />
			
			<OrderLines>
				<xsl:copy-of select="/Order/OrderLines/@*" />
				<xsl:for-each select="/Order/OrderLines/OrderLine">
					<OrderLine>
						<xsl:copy-of select="./@*" />
						<xsl:copy-of select="./*[not(self::LineCharges)]" />
						<xsl:if test="./LineCharges/LineCharge[@ChargeNameKey!='']">
							<xsl:copy-of select="./LineCharges"/>
						</xsl:if>

					</OrderLine>
				</xsl:for-each>
			</OrderLines>
		</Order>
	</xsl:template>
</xsl:stylesheet>
