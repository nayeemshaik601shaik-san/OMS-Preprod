<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml" indent="yes" encoding="UTF-8"/>
  <!-- Remove root node -->
  <xsl:template match="/*">
    <xsl:apply-templates select="@*|node()"/>
  </xsl:template>
  <!-- Identity template to copy all nodes by default -->
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>
  <!-- Template to modify the Promise element -->
  <xsl:template match="Promise">
    <xsl:copy>
      <!-- Add AllocationRuleID based on OrganizationCode -->
      <xsl:attribute name="AllocationRuleID">
        <xsl:choose>
          <xsl:when test="@OrganizationCode='CROCS_US'">CRS_US_SCH</xsl:when>
          <xsl:when test="@OrganizationCode='CROCS_CA'">CRS_CA_SCH</xsl:when>
          <!--EOMS-10728 : CROCS SG ATP Implementation : START -->
          <xsl:when test="@OrganizationCode='CROCS_SG'">CRS_SG_SCH</xsl:when>
          <!--EOMS-10728 :  CROCS SG ATP Implementation : END -->
		      <!--EOMS-11464 : CROCS KR ATP Implementation : START -->
          <xsl:when test="@OrganizationCode='CROCS_KR'">CRS_KR_SCH</xsl:when>	
          <!--EOMS-8527 : CROCS AU ATP Implementation-->
          <xsl:when test="@OrganizationCode='CROCS_AU'">CRS_AU_SCH</xsl:when>
          <!--EOMS-6071 : HeyDude US,CA and AU ATP Implementations : START -->
	        <xsl:when test="@OrganizationCode='HEYDUDE_US'">HD_US_SCH</xsl:when>
	        <xsl:when test="@OrganizationCode='HEYDUDE_CA'">HD_CA_SCH</xsl:when>
	        <xsl:when test="@OrganizationCode='HEYDUDE_AU'">HD_AU_SCH</xsl:when>
	        <!--EOMS-6071 : HeyDude US,CA and AU ATP Implementations : END -->
          <!--EOMS-12462 : EMEA CROCS,HEYDUDE ATP Implementations : START -->
          <xsl:when test="@OrganizationCode='CROCS_FR'">CRS_FR_SCH</xsl:when>
          <xsl:when test="@OrganizationCode='CROCS_DE'">CRS_DE_SCH</xsl:when>
          <xsl:when test="@OrganizationCode='CROCS_NL'">CRS_NL_SCH</xsl:when>
          <xsl:when test="@OrganizationCode='CROCS_EU'">CRS_EU_SCH</xsl:when>
          <xsl:when test="@OrganizationCode='CROCS_FI'">CRS_FI_SCH</xsl:when>
          <xsl:when test="@OrganizationCode='CROCS_GB'">CRS_GB_SCH</xsl:when>
          <xsl:when test="@OrganizationCode='HEYDUDE_FR'">HD_FR_SCH</xsl:when>
          <xsl:when test="@OrganizationCode='HEYDUDE_DE'">HD_DE_SCH</xsl:when>
          <xsl:when test="@OrganizationCode='HEYDUDE_GB'">HD_GB_SCH</xsl:when>
          <xsl:when test="@OrganizationCode='HEYDUDE_EU'">HD_EU_SCH</xsl:when>
          <!--EOMS-12462 : EMEA CROCS,HEYDUDE ATP Implementations : END -->
        </xsl:choose>
      </xsl:attribute>
	   <!-- Iterate over all PromiseLines -->
      <xsl:for-each select="/PromiseLines/PromiseLine">
        <xsl:copy>
          <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
      </xsl:for-each>
      <!-- Copy other attributes and children -->
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>
  <!-- Template to remove the ShipToAddress element -->
  <xsl:template match="ShipToAddress"/>
</xsl:stylesheet>


