<!-- Output format
<Shipment BackOrderNonShippedQuantity="N" WMSCode="1" OrderNo="54929265CUS" ReleaseNo="1" >
	<Containers>
		<Container TrackingNo="123TRACKING00003" ContainerNo="00009999990000036003" SCAC="FEDEX">
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
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="xml" indent="yes"/>

    <xsl:template match="/Shipment">

        <!-- Original OrderNo -->
        <xsl:variable name="fullOrder" select="@OrderNo"/>

        <!-- Conditional OrderNo -->
        <xsl:variable name="orderNo">
            <xsl:choose>
                <xsl:when test="contains($fullOrder,'SHIP')">
                    <xsl:value-of select="substring-before($fullOrder,'SHIP')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$fullOrder"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <!-- Conditional ReleaseNo -->
        <xsl:variable name="releaseNo">
            <xsl:choose>
                <xsl:when test="contains($fullOrder,'SHIP')">
                    <xsl:value-of select="substring-after($fullOrder,'SHIP')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="@ReleaseNo"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <!-- Determine if all shipment lines have zero quantity -->
        <xsl:variable name="allZero"
            select="count(ShipmentLines/ShipmentLine)
                  = count(ShipmentLines/ShipmentLine[@Quantity='0'])"/>

        <Shipment>
            <!-- Copy all attributes except OrderNo and ReleaseNo -->
            <xsl:copy-of select="@*[name() != 'OrderNo' and name() != 'ReleaseNo']"/>

            <!-- Set updated OrderNo / ReleaseNo -->
            <xsl:attribute name="OrderNo">
                <xsl:value-of select="$orderNo"/>
            </xsl:attribute>
            <xsl:attribute name="ReleaseNo">
                <xsl:value-of select="$releaseNo"/>
            </xsl:attribute>

            <!-- Add Cancelled attribute when all quantities are zero -->
            <xsl:if test="$allZero">
                <xsl:attribute name="Cancelled">Y</xsl:attribute>
            </xsl:if>

            <!-- Containers: empty if allZero -->
            <Containers>
                <xsl:if test="not($allZero)">
                    <xsl:copy-of select="Containers/*"/>
                </xsl:if>
            </Containers>

            <!-- ShipmentLines with updated OrderNo and ReleaseNo -->
            <ShipmentLines>
                <xsl:for-each select="ShipmentLines/ShipmentLine">
                    <ShipmentLine>
                        <xsl:copy-of
                            select="@*[name() != 'OrderNo' and name() != 'ReleaseNo']"/>
                        <xsl:attribute name="OrderNo">
                            <xsl:value-of select="$orderNo"/>
                        </xsl:attribute>
                        <xsl:attribute name="ReleaseNo">
                            <xsl:value-of select="$releaseNo"/>
                        </xsl:attribute>
                    </ShipmentLine>
                </xsl:for-each>
            </ShipmentLines>

        </Shipment>
    </xsl:template>

</xsl:stylesheet>
