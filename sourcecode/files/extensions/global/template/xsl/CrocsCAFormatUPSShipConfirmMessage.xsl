<!-- Output format
<Shipment BackOrderNonShippedQuantity="Y"  WMSCode="1" OrderNo="54929265CUS" ReleaseNo="1" >
	<Containers>
		<Container TrackingNo="123TRACKING00003" ContainerNo="00009999990000036003"  SCAC="FEDEX">
			<ContainerDetails>
				<ContainerDetail Quantity="6">
					<ShipmentLine Quantity="3" ShipmentLineNo="1"/> 
					<ShipmentLine Quantity="3" ShipmentLineNo="2"/> 
				</ContainerDetail>
			</ContainerDetails>
		</Container>
	</Containers>
	<ShipmentLines>
		<ShipmentLine ItemID="40002-001-M18" OrderNo="54929265CUS" Quantity="3" ReleaseNo="1" ShipmentLineNo="1"  UnitOfMeasure="EACH"/>
		<ShipmentLine ItemID="40003-001-M17" OrderNo="54929265CUS" Quantity="3" ReleaseNo="1" ShipmentLineNo="2"  UnitOfMeasure="EACH"/>
	</ShipmentLines>
</Shipment> -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" >
	<xsl:template match="/">
		<Shipment BackOrderNonShippedQuantity="Y" WMSCode="5" >
			<xsl:attribute name="OrderNo">
				<xsl:value-of select="substring-before(//ASN/ASNID,'SHIP')"/>
			</xsl:attribute>
			<xsl:attribute name="ReleaseNo">
				<xsl:value-of select="substring-after(//ASN/ASNID,'SHIP')"/>
			</xsl:attribute>
			<xsl:attribute name="BolNo">
				<xsl:value-of select="//ASN/BillOfLadingNumber"/>
			</xsl:attribute>
			<xsl:if test="normalize-space(//ASN/OrderType) != ''">
				<xsl:attribute name="OrderType">
				<xsl:value-of select="//ASN/OrderType"/>
				</xsl:attribute>
			</xsl:if>
			<xsl:choose>
				<xsl:when test="//ASN/IsCancelled='Y'">
					<xsl:attribute name="Cancelled">
						<xsl:value-of select="'Y'"/>
					</xsl:attribute>
					<xsl:attribute name="Action">
						<xsl:value-of select="'Cancel'"/>
					</xsl:attribute>
					<xsl:attribute name="OverrideModificationRules">
						<xsl:value-of select="'Y'"/>
					</xsl:attribute>
					<xsl:attribute name="CancelRemovedQuantity">
						<xsl:value-of select="'Y'"/>
					</xsl:attribute>
				</xsl:when>
				<xsl:otherwise>
					<Containers>
						<xsl:for-each select="ASN/LPN">
						<xsl:if test="sum(./LPNDetail/LPNDetailQuantity/Quantity) &gt; 0">
							<Container>
								<xsl:attribute name="TrackingNo">
									<xsl:value-of select="./TrackingNbr"/>
								</xsl:attribute>
								<xsl:attribute name="ContainerNo">
									<xsl:value-of select="./LPNID"/>
								</xsl:attribute>
								<xsl:attribute name="SCAC">
									<xsl:value-of select="//ASN/ShipVia"/>
								</xsl:attribute>
								<ContainerDetails>
									<ContainerDetail>
										<xsl:attribute name="Quantity">
											<xsl:value-of select="sum(./LPNDetail/LPNDetailQuantity/Quantity)"/>
										</xsl:attribute>
										<xsl:for-each select="./LPNDetail">
										<xsl:if test="./LPNDetailQuantity/Quantity &gt; 0">
											<ShipmentLine>
												<xsl:attribute name="Quantity">
													<xsl:value-of select="./LPNDetailQuantity/Quantity"/>
												</xsl:attribute>
												<xsl:attribute name="ShipmentLineNo">
													<xsl:value-of select="./ItemSequenceNbr"/>
												</xsl:attribute>
											</ShipmentLine>
										</xsl:if>
										</xsl:for-each>
									</ContainerDetail>
								</ContainerDetails>
							</Container>
						</xsl:if>
						</xsl:for-each>
					</Containers>
					<ShipmentLines>
						<xsl:for-each select="/ASN/ASNDetail">
							<xsl:if test="./ASNDetailQuantity/ShippedQuantity &gt; 0">
								<ShipmentLine UnitOfMeasure="EACH">
									<xsl:attribute name="ItemID">
										<xsl:value-of select="./ItemName"/>
									</xsl:attribute>
									<xsl:attribute name="OrderNo">
										<xsl:value-of select="substring-before(//ASN/ASNID,'SHIP')"/>
									</xsl:attribute>
									<xsl:attribute name="Quantity">
										<xsl:value-of select="./ASNDetailQuantity/ShippedQuantity"/>
									</xsl:attribute>
									<xsl:attribute name="ShipmentLineNo">
										<xsl:value-of select="./PurchaseOrderLineItemID"/>
									</xsl:attribute>
									<xsl:attribute name="ReleaseNo">
										<xsl:value-of select="substring-after(//ASN/ASNID,'SHIP')"/><!-- Map this -->
									</xsl:attribute>
								</ShipmentLine>
							</xsl:if>
						</xsl:for-each>
					</ShipmentLines>
				</xsl:otherwise>
			</xsl:choose>
		</Shipment>
	</xsl:template>
</xsl:stylesheet>