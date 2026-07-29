<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
      xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <!-- ***************************************
         Template Name: CopyPrimaryInfoWithoutDescription
         Purpose: Copy entire XML document except
                  the Description attribute in PrimaryInformation and
				  the ExtnColor attribute from Extn			  
       **************************************** -->
    <xsl:template match="@*|node()">

        <xsl:copy>
            <!-- Copy all attributes except Description on PrimaryInformation -->
            <xsl:choose>
                <xsl:when test="self::PrimaryInformation">
                    <xsl:apply-templates select="@*[name() != 'Description']"/>
                </xsl:when>
				<xsl:when test="self::Extn">
				<xsl:apply-templates select="@*[name() != 'ExtnColor']"/>
				</xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates select="@*"/>
                </xsl:otherwise>				
            </xsl:choose>
            <!-- Copy child nodes -->
            <xsl:apply-templates select="node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
