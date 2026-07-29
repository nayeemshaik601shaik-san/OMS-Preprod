<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="Shipment">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            
            <xsl:attribute name="OrderNo">
                <xsl:value-of select="ShipmentLines/ShipmentLine[1]/@OrderNo"/>
            </xsl:attribute>
            
            <xsl:attribute name="OrderHeaderKey">
                <xsl:value-of select="ShipmentLines/ShipmentLine[1]/@OrderHeaderKey"/>
            </xsl:attribute>
            
            <xsl:apply-templates select="node()"/>
        </xsl:copy>
    </xsl:template>
    
</xsl:stylesheet>