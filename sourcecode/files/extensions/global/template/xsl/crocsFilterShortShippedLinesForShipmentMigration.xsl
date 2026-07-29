<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="/Shipment">
		<Shipment>
			<xsl:copy-of select="//Shipment/@*" />
			<!-- Copy all child nodes except ShipmentLines -->
			<xsl:copy-of select="//Shipment/*[not(self:: ShipmentLines)]" />
			<!-- Filtered ShipmentLines -->
			<ShipmentLines>
				<xsl:for-each select="/Shipment/ShipmentLines/ShipmentLine[@Quantity &gt; 0][@ItemID!='EXTEND-SHIPPING-PROTECTION']">
					<ShipmentLine>
						<xsl:copy-of select="./@*" />
					</ShipmentLine>
				</xsl:for-each>
			</ShipmentLines>
		</Shipment>
	</xsl:template>
</xsl:stylesheet>
