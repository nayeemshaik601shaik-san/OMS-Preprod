<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" >
	<xsl:template match="/">
		<ItemList>
			<Item>
				<xsl:copy-of select="//Item/@*"/>
				<!-- Compute ExtnSAPMaterialGroup -->
				<xsl:variable name="varExtnSAPMaterialGroup">
				<xsl:choose>
					<xsl:when test="//Item/Extn/@ExtnSAPMaterialGroup and normalize-space(//Item/Extn/@ExtnSAPMaterialGroup) != ''">
					<!-- Convert to Camel Case -->
					<xsl:value-of select="concat(translate(substring(//Item/Extn/@ExtnSAPMaterialGroup,1,1),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),
					translate(substring(//Item/Extn/@ExtnSAPMaterialGroup,2),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'))"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="'Footwear'"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
				<PrimaryInformation>
				    <xsl:copy-of select="//Item/PrimaryInformation/@*[name() != 'ItemType']"/>
					<!-- CamelCase ItemType -->
					<xsl:attribute name="ItemType">
				<xsl:value-of select="concat(translate(substring(//Item/PrimaryInformation/@ItemType,1,1),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ'),
				translate(substring(//Item/PrimaryInformation/@ItemType,2),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'))"/>
				</xsl:attribute>
				    <xsl:variable name="varExtendedDisplayDescription" select="substring(//Item/PrimaryInformation/@Description,1,480)" />
				    <xsl:attribute name="Description">
				        	<xsl:value-of select="substring(//Item/PrimaryInformation/@Description,1,480)" />
				     </xsl:attribute>
				     <xsl:attribute name="ExtendedDisplayDescription">
				          <xsl:value-of select="$varExtendedDisplayDescription" />
				    </xsl:attribute>
					<xsl:attribute name="ShortDescription">
				            <xsl:value-of select="substring(//Item/PrimaryInformation/@ShortDescription, 1, 180)" />
				          </xsl:attribute>	
				</PrimaryInformation>				
				<InventoryParameters ATPRule="Crocs_NA_ATP_Rule" InventoryMonitorRule="Crocs_NA_RTAM_RULE" NodeLevelInventoryMonitorRule="Crocs_NA_RTAM_RULE" />
				<xsl:copy-of select="//Item/ItemAliasList"></xsl:copy-of>
				<!--
				We copy all attributes on <Extn> like ExtnColor  EXCEPT ExtnSAPMaterialGroup  because we need to
				(re)compute ExtnSAPMaterialGroup as per below business rule above:
				- If Extn/ @ExtnSAPMaterialGroup is present and non-empty, set it to Camel Case
				(first letter uppercase, remaining lowercase).
				- Otherwise, default it to 'Footwear'.
					Therefore, we exclude ExtnSAPMaterialGroup from the copy and write the computed
				value as a new attribute below.
				-->
				<Extn>
				 <xsl:copy-of select="//Item/Extn/@*[name() != 'ExtnSAPMaterialGroup']"/>
					<xsl:attribute name="ExtnSAPMaterialGroup">
				          <xsl:value-of select="$varExtnSAPMaterialGroup" />
				     </xsl:attribute>
				</Extn>				
				<AdditionalAttributeList>
					<xsl:copy-of select="//Item/AdditionalAttributeList/@*"/>
					<xsl:for-each select="//Item/AdditionalAttributeList/AdditionalAttribute[@Value!='']">
						<AdditionalAttribute>
							<xsl:copy-of select="@*"/>
						</AdditionalAttribute>
					</xsl:for-each>
				</AdditionalAttributeList>		
			</Item>		
		</ItemList>
	</xsl:template>
</xsl:stylesheet>