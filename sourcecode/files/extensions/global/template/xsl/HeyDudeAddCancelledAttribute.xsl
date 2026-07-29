<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="@* | node()">
		<xsl:copy>
			<xsl:apply-templates select="@* | node()"/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="Shipment">
		<xsl:copy>
			<xsl:apply-templates select="@*"/>
			<xsl:if test="not(ShipmentLines/ShipmentLine[not(@Quantity = '0')])">
				<xsl:attribute name="Cancelled">Y</xsl:attribute>
			</xsl:if>
			<xsl:apply-templates select="node()"/>
		</xsl:copy>
	</xsl:template>
</xsl:stylesheet>