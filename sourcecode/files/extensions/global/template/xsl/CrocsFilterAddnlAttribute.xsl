<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    
    <xsl:template match="/OrderRelease">
        <xsl:copy>
            <xsl:copy-of select="@*"/>

            <!-- Collect all ExtnSAPMaterialGroup values -->
            <xsl:variable name="materialGroupValues" select="OrderLine/ItemDetails/Extn/@ExtnSAPMaterialGroup"/>

            <!-- Check if there are any missing or empty values -->
            <xsl:variable name="missingValues" select="OrderLine/ItemDetails/Extn[not(normalize-space(@ExtnSAPMaterialGroup))]"/>

            <!-- Check for conditions -->
            <xsl:choose>
                <!-- Case 1: If any value is missing or empty, set to "MIXED" -->
                <xsl:when test="count($missingValues) > 0">
                    <xsl:attribute name="InternationalGoodsDescription">MIXED</xsl:attribute>
                </xsl:when>

                <!-- Case 2: If all values are the same, assign the corresponding value -->
                <xsl:when test="count($materialGroupValues) = count($materialGroupValues[. = $materialGroupValues[1]])">
                    <xsl:choose>
                        <xsl:when test="$materialGroupValues[1] = 'Footwear'">
                            <xsl:attribute name="InternationalGoodsDescription">FOOTWEAR</xsl:attribute>
                        </xsl:when>
                        <xsl:when test="$materialGroupValues[1] = 'Charms'">
                            <xsl:attribute name="InternationalGoodsDescription">CHARMS</xsl:attribute>
                        </xsl:when>
                        <xsl:when test="$materialGroupValues[1] = 'Apparel'">
                            <xsl:attribute name="InternationalGoodsDescription">APPAREL</xsl:attribute>
                        </xsl:when>
                        <xsl:when test="$materialGroupValues[1] = 'Accessories'">
                            <xsl:attribute name="InternationalGoodsDescription">ACCESSORY</xsl:attribute>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:attribute name="InternationalGoodsDescription">MIXED</xsl:attribute>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>

                <!-- Case 3: Otherwise, set to "MIXED" -->
                <xsl:otherwise>
                    <xsl:attribute name="InternationalGoodsDescription">MIXED</xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>

            <!-- Apply templates to other child elements -->
            <xsl:apply-templates select="*"/>
        </xsl:copy>
    </xsl:template>

    <!-- Template to copy all elements and attributes -->
    <xsl:template match="*">
        <xsl:copy>
            <xsl:copy-of select="@*"/>
            <xsl:apply-templates select="*"/>
        </xsl:copy>
    </xsl:template>
	
	 <!-- Skip OrderLine with OrderedQty = 0 -->
    <xsl:template match="OrderLine[number(@OrderedQty) = 0]"/>
	
	 <!-- Template to filter AdditionalAttribute elements based on EnterpriseCode -->
    <xsl:template match="AdditionalAttribute">
        <xsl:variable name="enterpriseCode" select="/OrderRelease/@EnterpriseCode" />

        <!-- EOMS-14215-START -->
        <xsl:variable name="documentType" select="/OrderRelease/Order/@DocumentType"/>

        <!-- Use attributeName CROCS_DE for EMEA enterprises and DocumentType 0006-->
    <xsl:variable name="attributeName">
        <xsl:choose>
            <xsl:when test="$documentType='0006' and ($enterpriseCode='CROCS_GB' or $enterpriseCode='CROCS_IE' or $enterpriseCode='CROCS_NL' or $enterpriseCode='CROCS_FR' or $enterpriseCode='CROCS_AT' or $enterpriseCode='CROCS_DE')">CROCS_DE</xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$enterpriseCode"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
        
        <!-- Only copy AdditionalAttribute if its Name matches the EnterpriseCode -->
        <xsl:if test="translate(@Name, 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ') = translate(normalize-space($attributeName), 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')">
          <!-- EOMS-14215-END -->
            <xsl:copy>
                <!-- Copy all attributes of AdditionalAttribute -->
                <xsl:copy-of select="@*"/>
            </xsl:copy>
        </xsl:if>
    </xsl:template>
    <xsl:template match="PaymentMethods"/>

</xsl:stylesheet>
