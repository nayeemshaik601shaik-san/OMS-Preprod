<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" >
	<xsl:template match="/">
		<ItemList>
			<Item ItemGroupCode="PROD">
				<xsl:copy-of select="//Item/@*"/>
				<xsl:variable name="varExtnSAPMaterialGroup">
					<xsl:choose>
						<xsl:when test="//Item/Extn/@ExtnSAPMaterialGroup and //Item/Extn/@ExtnSAPMaterialGroup!=''">
							<xsl:value-of select="//Item/Extn/@ExtnSAPMaterialGroup" />
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="'Footwear'" />
						</xsl:otherwise>
					</xsl:choose>
				 </xsl:variable>
				<PrimaryInformation>
					<xsl:copy-of select="//Item/PrimaryInformation/@*"/>
					<xsl:attribute name="IsAirShippingAllowed">Y</xsl:attribute>
				    <xsl:attribute name="IsDeliveryAllowed">Y</xsl:attribute>
				    <xsl:attribute name="IsEligibleForShippingDiscount">Y</xsl:attribute>
				    <xsl:attribute name="IsForwardingAllowed">N</xsl:attribute>
				    <xsl:attribute name="IsParcelShippingAllowed">Y</xsl:attribute>
				    <xsl:attribute name="IsPickupAllowed">N</xsl:attribute>
				    <xsl:attribute name="IsReturnable">Y</xsl:attribute>
				    <xsl:attribute name="IsShippingAllowed">Y</xsl:attribute>
				    <xsl:variable name="varImageURL">
				    	<xsl:value-of select="concat(//Item/PrimaryInformation/@ImageLocation, //Item/PrimaryInformation/@ImageID)"></xsl:value-of>
				    </xsl:variable>
			    	<xsl:choose>
			    		<xsl:when test="string-length($varImageURL)&gt; '255' or string-length(//PrimaryInformation/@ImageID) &gt; '100'">
			    			<xsl:attribute name="ImageLocation">
			    				<xsl:value-of select="substring($varImageURL,1,255)"/>
			    			</xsl:attribute>
			    			<xsl:attribute name="ImageID">
			    				<xsl:value-of select="substring($varImageURL,256,string-length($varImageURL))"/>
			    			</xsl:attribute>
			    		</xsl:when>
			    	</xsl:choose>
				    <xsl:variable name="varExtendedDisplayDescription" select="substring(//Item/PrimaryInformation/@ExtendedDisplayDescription,1,480)" />
				    <xsl:attribute name="Description">
				      <xsl:choose>
				        <xsl:when test="//Item/PrimaryInformation/@Description = '' or not(//Item/PrimaryInformation/@Description)">
				            <xsl:value-of select="substring($varExtendedDisplayDescription,1,480)" />
				        </xsl:when>
				        <xsl:otherwise>
				        	<xsl:value-of select="substring(//Item/PrimaryInformation/@Description,1,480)" />
				        </xsl:otherwise>
				      </xsl:choose>
				     </xsl:attribute>
				     <xsl:attribute name="ExtendedDescription">
				      <xsl:choose>
				        <xsl:when test="//Item/PrimaryInformation/@ExtendedDescription = '' or not(//Item/PrimaryInformation/@ExtendedDescription)">
				            <xsl:value-of select="$varExtendedDisplayDescription" />
				        </xsl:when>
				        <xsl:otherwise>
				        	<xsl:value-of select="substring(//Item/PrimaryInformation/@ExtendedDescription,1,1980)" />
				        </xsl:otherwise>
				      </xsl:choose>
				     </xsl:attribute>
				     <xsl:attribute name="ExtendedDisplayDescription">
				          <xsl:value-of select="$varExtendedDisplayDescription" />
				     </xsl:attribute>
				      <xsl:choose>
				        <xsl:when test="//Item/PrimaryInformation/@ShortDescription = '' or not(//Item/PrimaryInformation/@ShortDescription)">
				          <xsl:attribute name="ShortDescription">
				            <xsl:value-of select="substring($varExtendedDisplayDescription, 1, 180)" />
				          </xsl:attribute>
				        </xsl:when>
				        <xsl:when test="//Item/PrimaryInformation/@ShortDescription != '' ">
				          <xsl:attribute name="ShortDescription">
				            <xsl:value-of select="substring(//Item/PrimaryInformation/@ShortDescription, 1, 180)" />
				          </xsl:attribute>
				        </xsl:when>
				      </xsl:choose>
				      <xsl:attribute name="ProductLine">
				      	<xsl:value-of select="$varExtnSAPMaterialGroup" />
				      </xsl:attribute>
				      <xsl:attribute name="Department">
				      	<xsl:value-of select="//PrimaryInformation/@ProductLine" />
				      </xsl:attribute>
				</PrimaryInformation>				
				<InventoryParameters ATPRule="Crocs_NA_ATP_Rule" InventoryMonitorRule="Crocs_NA_RTAM_RULE" NodeLevelInventoryMonitorRule="Crocs_NA_RTAM_RULE" />
				<xsl:copy-of select="//Item/ClassificationCodes"></xsl:copy-of>
				<xsl:copy-of select="//Item/ItemAliasList"></xsl:copy-of>
				<Extn>
					<xsl:copy-of select="//Item/Extn/@*[not(local-name() = 'ExtnImageURL')][not(local-name() = 'ExtnProductURL')]"></xsl:copy-of>
					<xsl:attribute name="ExtnImageUrl">
				          <xsl:value-of select="//Item/Extn/@ExtnImageURL" />
				     </xsl:attribute>
				     <xsl:attribute name="ExtnProductUrl">
				          <xsl:value-of select="//Item/Extn/@ExtnProductURL" />
				     </xsl:attribute>
				<!-- Start : EOMS-6151 : HeyDude : Items feed modifications -->     
				<!-- <CrocsItemOrgDataList>
					<xsl:for-each select="//Item/Extn/CrocsItemOrgDataList/CrocsItemOrgData">
					<CrocsItemOrgData>
					<xsl:copy-of select="@*"/>
					<xsl:attribute name="ItemID">
				          <xsl:value-of select="//Item/@ItemID" />
				     </xsl:attribute>
					</CrocsItemOrgData>
					</xsl:for-each>
				</CrocsItemOrgDataList> -->			
								
				<CrocsItemOrgDataList Reset="Y">
				    <xsl:variable name="itemId" select="//Item/@ItemID"/>				
				    <xsl:for-each select="//Item/LanguageDescriptionList/LanguageDescription">				
				        <!-- Determine OrganizationCode based on ExtnProductUrl -->
				        <xsl:variable name="orgCode">
				            <xsl:choose>
								<!--IMPORTANT: Order matters here.
                                  'www.crocs.com.au' contains 'www.crocs.com', so the AU condition
                                   must be evaluated BEFORE the US condition.Otherwise , AU URLs
                                    will incorrectly resolve to crocs_us.-->
								<xsl:when test="contains(Extn/@ExtnProductUrl,'www.crocs.com.au')">crocs_au</xsl:when>
				                <xsl:when test="contains(Extn/@ExtnProductUrl,'www.crocs.com')">crocs_us</xsl:when>
				                <xsl:when test="contains(Extn/@ExtnProductUrl,'www.crocs.ca')">crocs_ca</xsl:when>
				                <xsl:when test="contains(Extn/@ExtnProductUrl,'www.heydude.com')">heydude_us</xsl:when>
				                <xsl:when test="contains(Extn/@ExtnProductUrl,'www.heydude.ca')">heydude_ca</xsl:when>
				                <xsl:when test="contains(Extn/@ExtnProductUrl,'heydev.heydude.com.au')">heydude_au</xsl:when>
				                <xsl:otherwise>crocs_us</xsl:otherwise>
				            </xsl:choose>
				        </xsl:variable>				
				        <!-- Find original org record -->
				        <xsl:variable name="origOrg"
				                      select="//Item/Extn/CrocsItemOrgDataList/CrocsItemOrgData[@OrganizationCode=$orgCode]"/>
				
				        <!-- Output merged record -->
				        <CrocsItemOrgData
				            MaxOrderQuantity="{$origOrg/@MaxOrderQuantity}"
				            ReturnWindow="{$origOrg/@ReturnWindow}"
				            SizeCode="{$origOrg/@SizeCode}"
				            ItemID="{$itemId}"
				            OrganizationCode="{$orgCode}"
				            Locale="{@LocaleCode}"
				            Color="{Extn/@ExtnColorCode}"
				            ProductUrl="{Extn/@ExtnProductUrl}"
				            ExtendedDisplayDescription="{@ExtendedDisplayDescription}"
				        />				
				    </xsl:for-each>
				</CrocsItemOrgDataList>
				<!-- End : EOMS-6151 : HeyDude : Items feed modifications -->				
				</Extn>
				<LanguageDescriptionList Reset="Y">
				 	
					<xsl:for-each select="//Item/LanguageDescriptionList/LanguageDescription[@LocaleCode!='en_US']">
						<!-- EOMS-6151 : HeyDude : Items feed modifications -->
                        			<!-- <xsl:if test="@ExtendedDisplayDescription !=''"> -->
						<xsl:if test="@ExtendedDisplayDescription !='' and not(contains(Extn/@ExtnProductUrl, 'heydude'))">
							<LanguageDescription>
								<xsl:copy-of select="@*"/>
								<xsl:variable name="varProductURL">
							    	<xsl:value-of select="concat(./@ImageLocation, ./@ImageID)"/>
							    </xsl:variable>
						    	<xsl:choose>
						    		<xsl:when test="string-length($varProductURL)&gt; '255' or string-length(./@ImageID) &gt; '100'">
						    			<xsl:attribute name="ImageLocation">
						    				<xsl:value-of select="substring($varProductURL,1,255)"/>
						    			</xsl:attribute>
						    			<xsl:attribute name="ImageID">
						    				<xsl:value-of select="substring($varProductURL,256,string-length($varProductURL))"/>
						    			</xsl:attribute>
						    		</xsl:when>
						    	</xsl:choose>
						        <xsl:variable name="varLangExtendedDisplayDescription" select="@ExtendedDisplayDescription" />
							      <xsl:choose>
							        <xsl:when test="@Description = '' or not(@Description)">
							          <xsl:attribute name="Description">
							            <xsl:value-of select="$varLangExtendedDisplayDescription" />
							          </xsl:attribute>
							        </xsl:when>
							        <xsl:otherwise>
							          	<xsl:attribute name="Description">
							            	<xsl:value-of select="substring(@Description, 1, 480)" />
							          	</xsl:attribute>
							          </xsl:otherwise>
							      </xsl:choose>
							      <xsl:choose>
							        <xsl:when test="@ExtendedDescription = '' or not(@ExtendedDescription)">
							          <xsl:attribute name="ExtendedDescription">
							            <xsl:value-of select="$varLangExtendedDisplayDescription" />
							          </xsl:attribute>
							        </xsl:when>
							        <xsl:otherwise>
							          	<xsl:attribute name="ExtendedDescription">
							            	<xsl:value-of select="substring(@ExtendedDescription, 1, 480)" />
							          	</xsl:attribute>
							          </xsl:otherwise>
							      </xsl:choose>
							      <xsl:choose>
							        <xsl:when test="@ShortDescription = '' or not(@ShortDescription)">
							          <xsl:attribute name="ShortDescription">
							            <xsl:value-of select="substring($varLangExtendedDisplayDescription,1,80)" />
							          </xsl:attribute>
							        </xsl:when>
							         <xsl:when test="@ShortDescription != ''">
							          <xsl:attribute name="ShortDescription">
							            <xsl:value-of select="substring(@ShortDescription,1,80)" />
							          </xsl:attribute>
							        </xsl:when>
							      </xsl:choose>
							</LanguageDescription>
						</xsl:if>
					</xsl:for-each>
				</LanguageDescriptionList>
				<ItemLocaleList Reset="Y">
                       <!-- Iterate over each LanguageDescription element in LanguageDescriptionList -->
                   
                    <xsl:for-each select="//LanguageDescriptionList/LanguageDescription[@LocaleCode!='en_US']">
                        <!-- EOMS-6151 : HeyDude : Items feed modifications -->
                        <!-- <xsl:if test="@ExtendedDisplayDescription !=''"> -->
                        <xsl:if test="@ExtendedDisplayDescription !='' and not(contains(Extn/@ExtnProductUrl, 'heydude'))">
                         	<xsl:variable name="locale" select="@LocaleCode"/>
                         	<xsl:variable name="varLocaleExtendedDisplayDescription" select="@ExtendedDisplayDescription" />
	                        <ItemLocale Country="{substring-after($locale, '_')}" Language="{substring-before($locale, '_')}">
		                         <PrimaryInformation>
		                         	<xsl:copy-of select="@*"/>
		                            <xsl:variable name="varItemURL">
								    	<xsl:value-of select="concat(./@ImageLocation, ./@ImageID)"/>
								    </xsl:variable>
							    	<xsl:choose>
							    		<xsl:when test="string-length($varItemURL)&gt; '255' or string-length(./@ImageID) &gt; '100'">
							    			<xsl:attribute name="ImageLocation">
							    				<xsl:value-of select="substring($varItemURL,1,255)"/>
							    			</xsl:attribute>
							    			<xsl:attribute name="ImageID">
							    				<xsl:value-of select="substring($varItemURL,256,string-length($varItemURL))"/>
							    			</xsl:attribute>
							    		</xsl:when>
							    		<xsl:otherwise>
							    			<xsl:attribute name="ImageLocation">
							    				<xsl:value-of select="concat(./@ImageLocation, '/')"/>
							    			</xsl:attribute>
							    			<xsl:attribute name="ImageID">
							    				<xsl:value-of select="./@ImageID"/>
							    			</xsl:attribute>
							    		</xsl:otherwise>
							    	</xsl:choose>
	                                <xsl:choose>
								        <xsl:when test="@Description = '' or not(@Description)">
								          <xsl:attribute name="Description">
								            <xsl:value-of select="$varLocaleExtendedDisplayDescription" />
								          </xsl:attribute>
								        </xsl:when>
								        <xsl:otherwise>
								          	<xsl:attribute name="Description">
								            	<xsl:value-of select="substring(@Description, 1, 480)" />
								          	</xsl:attribute>
								        </xsl:otherwise>
								     </xsl:choose>
	                                 <xsl:choose>
								        <xsl:when test="@ExtendedDescription = '' or not(@ExtendedDescription)">
								          <xsl:attribute name="ExtendedDescription">
								            <xsl:value-of select="$varLocaleExtendedDisplayDescription" />
								          </xsl:attribute>
								        </xsl:when>
								        <xsl:otherwise>
								          	<xsl:attribute name="ExtendedDescription">
								            	<xsl:value-of select="substring(@ExtendedDescription, 1, 480)" />
								          	</xsl:attribute>
								        </xsl:otherwise>
								    </xsl:choose>
	                                 <xsl:copy-of select="@ExtendedDisplayDescription"/>
	                                 <xsl:choose>
								        <xsl:when test="@ShortDescription = '' or not(@ShortDescription)">
								          <xsl:attribute name="ShortDescription">
								            <xsl:value-of select="substring($varLocaleExtendedDisplayDescription,1,80)" />
								          </xsl:attribute>
								        </xsl:when>
								         <xsl:otherwise test="@ShortDescription != ''">
								          <xsl:attribute name="ShortDescription">
								            <xsl:value-of select="substring(@ShortDescription,1,80)" />
								          </xsl:attribute>
								        </xsl:otherwise>
								    </xsl:choose>
	                             </PrimaryInformation>
	                             <Extn>
                                 	<xsl:attribute name="ExtnColorCode">
				           			 	<xsl:value-of select="./Extn/@ExtnColorCode" />
				          			</xsl:attribute>
				           			<xsl:attribute name="ExtnProductUrl">
				           				 <xsl:value-of select="./Extn/@ExtnProductUrl" />
				         			</xsl:attribute>
                                </Extn>
	                         </ItemLocale>
                        </xsl:if>
                    </xsl:for-each>
                </ItemLocaleList>
				<xsl:if test="//ClassificationCodes/@Model = '' or //PrimaryInformation/@IsModelItem='Y' or //ClassificationCodes/@Model=//Item/@ItemID">
					<!-- CategoryList with a default value for CategoryID -->
				      <CategoryList>
				        <Category>
				          <xsl:attribute name="CategoryID">
				            <xsl:value-of select="$varExtnSAPMaterialGroup" />
				          </xsl:attribute>
				          <xsl:attribute name="CategoryPath">
				            <xsl:value-of select="concat('/CROCS_NAMasterCatalog/', $varExtnSAPMaterialGroup)" />
				          </xsl:attribute>
				        </Category>
				      </CategoryList>     
				      <CategoryAssociations IgnoreCategoryAssociationException="Y" >
				        <Category>
				          <xsl:attribute name="CategoryPath">
				            <xsl:value-of select="concat('/CROCS_NAMasterCatalog/', $varExtnSAPMaterialGroup)" />
				          </xsl:attribute>
				        </Category>
				       </CategoryAssociations>
			    </xsl:if>
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
