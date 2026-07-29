<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://www.w3.org" >
    <xsl:output method="xml" indent="yes"/>
    <!-- Template for matching the root element (Order)-->
    <xsl:template match="/Order">
        <Order>
            <xsl:attribute name="OrderNo">
                <xsl:value-of select="@OrderNo"/>
            </xsl:attribute>
        </Order>
    </xsl:template>
    <!-- Identity template for copying unchanged nodes -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>