<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="OrderLine/OrderStatuses/OrderStatus">
		<xsl:copy>
			<xsl:apply-templates select="@*[name() != 'StatusQty']"/>
			<xsl:attribute name="StatusQty">
				<xsl:value-of select="../../@OrderedQty"/>
			</xsl:attribute>
			<xsl:apply-templates select="node()"/>
		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>
