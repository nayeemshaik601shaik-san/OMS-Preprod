<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

    <!-- Identity template to copy elements by default -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- Transform the root element -->
    <xsl:template match="ItemList">
        <xsl:copy>
            <xsl:apply-templates select="Item"/>
        </xsl:copy>
    </xsl:template>
    
    <!-- Update AdditionalAttribute element -->
  <xsl:template match="AdditionalAttribute">
  <xsl:choose>
      <xsl:when test="@Value != ''">
	    <xsl:copy>
	    	 <xsl:apply-templates select="@*" />
	    </xsl:copy>
	    </xsl:when>
	   </xsl:choose>
  </xsl:template>


  <!-- Transform Item element -->
  <xsl:template match="Item">
    <xsl:copy>
      <xsl:variable name="varExtnSAPMaterialGroup" select="Extn/@ExtnSAPMaterialGroup" />
      
      <!-- Determine the value for CategoryID and CategoryPath -->
      <xsl:variable name="categoryValue">
        <xsl:choose>
          <xsl:when test="string($varExtnSAPMaterialGroup)!=''">
            <xsl:value-of select="$varExtnSAPMaterialGroup"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="'Footwear'"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>

      <xsl:attribute name="Action">manage</xsl:attribute>
      <xsl:attribute name="ItemGroupCode">PROD</xsl:attribute>
      <xsl:attribute name="ItemID">
        <xsl:value-of select="@ItemID" />
      </xsl:attribute>
      <xsl:attribute name="OrganizationCode">
        <xsl:value-of select="@OrganizationCode" />
      </xsl:attribute>
      <xsl:attribute name="UnitOfMeasure">
        <xsl:value-of select="@UnitOfMeasure" />
      </xsl:attribute>
      <xsl:apply-templates select="@*[name()!='OrganizationCode' and name()!='UnitOfMeasure']|node()" />  

	<xsl:if test="ClassificationCodes/@Model = '' or PrimaryInformation/@IsModelItem='Y'">
					<!-- CategoryList with a default value for CategoryID -->
      <xsl:element name="CategoryList">
        <xsl:element name="Category">
          <xsl:attribute name="CategoryID">
            <xsl:value-of select="$categoryValue" />
          </xsl:attribute>
          <xsl:attribute name="CategoryPath">
            <xsl:value-of select="concat('/CROCS_NAMasterCatalog/', $categoryValue)" />
          </xsl:attribute>
        </xsl:element>
      </xsl:element>     
      <xsl:element name="CategoryAssociations">
        <xsl:element name="Category">
          <xsl:attribute name="CategoryPath">
            <xsl:value-of select="concat('/CROCS_NAMasterCatalog/', $categoryValue)" />
          </xsl:attribute>
        </xsl:element>
      </xsl:element>
    </xsl:if>

	 
      <!-- Create ItemLocaleList element -->
                    <ItemLocaleList>
                        <!-- Iterate over each LanguageDescription element in LanguageDescriptionList -->
                        <xsl:for-each select="LanguageDescriptionList/LanguageDescription">
                            <xsl:variable name="locale" select="@LocaleCode"/>
                            <ItemLocale Country="{substring-after($locale, '_')}" Language="{substring-before($locale, '_')}">
                                <PrimaryInformation>
                                    <xsl:copy-of select="@Description"/>
                                    <xsl:copy-of select="@ExtendedDescription"/>
                                    <xsl:copy-of select="@ExtendedDisplayDescription"/>
                                    <xsl:copy-of select="@ShortDescription"/>
                                </PrimaryInformation>
                            </ItemLocale>
                        </xsl:for-each>
                    </ItemLocaleList>
    </xsl:copy>
  </xsl:template>

  <!-- Update PrimaryInformation element -->
  <xsl:template match="PrimaryInformation">
    <xsl:copy>
      <xsl:apply-templates select="@*" />
      <xsl:attribute name="IsAirShippingAllowed">Y</xsl:attribute>
      <xsl:attribute name="IsDeliveryAllowed">Y</xsl:attribute>
      <xsl:attribute name="IsEligibleForShippingDiscount">Y</xsl:attribute>
      <xsl:attribute name="IsForwardingAllowed">N</xsl:attribute>
      <xsl:attribute name="IsParcelShippingAllowed">Y</xsl:attribute>
      <xsl:attribute name="IsPickupAllowed">Y</xsl:attribute>
      <xsl:attribute name="IsReturnable">Y</xsl:attribute>
      <xsl:attribute name="IsShippingAllowed">Y</xsl:attribute>
      <xsl:variable name="varExtendedDisplayDescription" select="@ExtendedDisplayDescription" />
      <xsl:choose>
        <xsl:when test="@Description = '' or not(@Description)">
          <xsl:attribute name="Description">
            <xsl:value-of select="$varExtendedDisplayDescription" />
          </xsl:attribute>
        </xsl:when>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="@ExtendedDescription = '' or not(@ExtendedDescription)">
          <xsl:attribute name="ExtendedDescription">
            <xsl:value-of select="$varExtendedDisplayDescription" />
          </xsl:attribute>
        </xsl:when>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="@ShortDescription = '' or not(@ShortDescription)">
          <xsl:attribute name="ShortDescription">
            <xsl:value-of select="$varExtendedDisplayDescription" />
          </xsl:attribute>
        </xsl:when>
      </xsl:choose>
    </xsl:copy>
    <xsl:choose>
      <xsl:when test="ancestor::Item/@OrganizationCode = 'CROCS_NA'">
        <InventoryParameters ATPRule="Crocs_NA_ATP_Rule"
          InventoryMonitorRule="Crocs_NA_RTAM_RULE"
          NodeLevelInventoryMonitorRule="Crocs_NA_RTAM_RULE" />
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <!-- Add InventoryParameters and OperationalConfigurationComplete conditionally -->
  <xsl:template match="ClassificationCodes">
    <xsl:copy>
      <xsl:apply-templates select="@*" />
      <xsl:attribute name="PostingClassification" />
      <!-- Add OperationalConfigurationComplete if IsModelItem is not 'Y' -->
      <xsl:choose>
        <xsl:when test="ancestor::Item/PrimaryInformation/@IsModelItem != 'Y'">
          <xsl:attribute name="OperationalConfigurationComplete">N</xsl:attribute>
        </xsl:when>
      </xsl:choose>
      <xsl:apply-templates select="node()" />
    </xsl:copy>
  </xsl:template>

    <!-- Set Reset attribute for LanguageDescriptionList -->
    <xsl:template match="LanguageDescriptionList">
        <xsl:copy>
            <xsl:attribute name="Reset">Y</xsl:attribute>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

  <xsl:template match="LanguageDescription">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*" />
      <xsl:variable name="varExtendedDisplayDescription" select="@ExtendedDisplayDescription" />
      <xsl:choose>
        <xsl:when test="@Description = '' or not(@Description)">
          <xsl:attribute name="Description">
            <xsl:value-of select="$varExtendedDisplayDescription" />
          </xsl:attribute>
        </xsl:when>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="@ExtendedDescription = '' or not(@ExtendedDescription)">
          <xsl:attribute name="ExtendedDescription">
            <xsl:value-of select="$varExtendedDisplayDescription" />
          </xsl:attribute>
        </xsl:when>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="@ShortDescription = '' or not(@ShortDescription)">
          <xsl:attribute name="ShortDescription">
            <xsl:value-of select="$varExtendedDisplayDescription" />
          </xsl:attribute>
        </xsl:when>
      </xsl:choose>
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>
