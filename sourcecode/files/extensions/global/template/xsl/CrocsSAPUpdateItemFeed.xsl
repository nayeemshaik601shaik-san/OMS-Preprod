<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
      xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output method="xml" indent="yes" encoding="UTF-8"/>

  <!-- Root template -->
  <xsl:template match="/">
    <ItemList>
      <xsl:for-each select="ItemList/Item">
        <Item>
          <!-- Copy all Item-level attributes -->
          <xsl:copy-of select="@*"/>
          <PrimaryInformation>
		   <xsl:attribute name="Description">
			<xsl:value-of select="//Item/PrimaryInformation/@Description" />
		  </xsl:attribute>
		  </PrimaryInformation>
		   <!-- Copy ExtnColorCode attributes -->
		  <!-- EOMS-9492 : Update ExtnSAPMaterial for new and existing items -->
					<Extn>
					<xsl:if test="Extn/@ExtnColor and normalize-space(Extn/@ExtnColor) != ''">
					<xsl:attribute name="ExtnColor">
					<xsl:value-of select="Extn/@ExtnColor"/>
					</xsl:attribute>
					</xsl:if>
					<xsl:if test="Extn/@ExtnSAPMaterial and normalize-space(Extn/@ExtnSAPMaterial) != ''">
					<xsl:attribute name="ExtnSAPMaterial">
					<xsl:value-of select="Extn/@ExtnSAPMaterial"/>
					</xsl:attribute>
					</xsl:if>
					</Extn>	  
		  <!-- Copy AdditionalAttributeList -->
          <xsl:copy-of select="AdditionalAttributeList"/>
          <!-- Copy ItemAliasList as-is -->     
          <xsl:copy-of select="ItemAliasList"/>
        </Item>
      </xsl:for-each>
    </ItemList>
  </xsl:template>

</xsl:stylesheet>
