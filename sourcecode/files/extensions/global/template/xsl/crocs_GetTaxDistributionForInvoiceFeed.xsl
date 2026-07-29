<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" >
	<xsl:template match="/">
		<InvoiceDetail xmlns="http://www.w3.org">
			<InvoiceHeader>
				<xsl:copy-of select="//InvoiceHeader/@*"/>
				<xsl:copy-of select="/InvoiceDetail/InvoiceHeader/*[not(self:: LineDetails)]"/>
				<LineDetails>
					<xsl:copy-of select="/InvoiceDetail/InvoiceHeader/LineDetails/@*"/>	
					<xsl:for-each select="/InvoiceDetail/InvoiceHeader/LineDetails/LineDetail">
						<LineDetail>
							<xsl:copy-of select="./@*"/>
							<xsl:variable name="vTotalTaxOnDiscount">
								<xsl:value-of select="sum(./LineCharges/LineCharge/@Tax)"/>
							</xsl:variable>
							<xsl:attribute name="TotalTaxOnDiscount">
								<xsl:value-of select="sum(./LineCharges/LineCharge/@Tax)"/>
							</xsl:attribute>
							<xsl:variable name="vTotalTaxDifference">
								<xsl:value-of select="format-number((./@Tax - $vTotalTaxOnDiscount), '#.##')"/>
							</xsl:variable>
							<xsl:variable name="vTaxDifference">
								<xsl:choose>
									<xsl:when test="(./@Tax - $vTotalTaxOnDiscount)=(./LineTaxes/LineTax[@TaxName='SalesTax']/@Tax)">
										<xsl:value-of select="0.00"></xsl:value-of>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="format-number((./LineTaxes/LineTax[@TaxName='SalesTax']/@Tax) - ($vTotalTaxDifference), '#.##')"></xsl:value-of>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:variable>
							<xsl:copy-of select="./*[not(self:: LineCharges)]"/>
							<LineCharges>
								<xsl:for-each select="./LineCharges/LineCharge[@IsDiscount='Y' and @ChargeAmount!='0.00']">									
									<xsl:variable name="vNodePostion">
										<xsl:value-of select="position()"></xsl:value-of>
									</xsl:variable>
									<xsl:variable name="vNodeCount">
										<xsl:value-of select="count(../LineCharge[@IsDiscount='Y' and @ChargeAmount!='0.00'])"></xsl:value-of>
									</xsl:variable>
									<LineCharge>
										<xsl:copy-of select="./@*"/>
										<xsl:attribute name="Tax">
											<xsl:choose>
												<xsl:when test="$vTaxDifference='0.00' or ($vNodePostion != $vNodeCount)">
													<xsl:value-of select="./@Tax"></xsl:value-of>
												</xsl:when>
												<xsl:otherwise>
													<xsl:if test="$vNodePostion = $vNodeCount">
														<xsl:choose>
															<xsl:when test="../../LineTaxes/LineTax[@TaxName='SalesTax']/@TaxPercentage &gt; 0">
																<xsl:value-of select="format-number(./@Tax - ($vTaxDifference), '#.##')"></xsl:value-of>
															</xsl:when>
															<xsl:otherwise>
																<xsl:value-of select="'0.00'"></xsl:value-of>
															</xsl:otherwise>
														</xsl:choose>
													</xsl:if>
												</xsl:otherwise>
											</xsl:choose>
										</xsl:attribute>
										<xsl:copy-of select="./*"/>
									</LineCharge>
								</xsl:for-each>
								<xsl:for-each select="./LineCharges/LineCharge[@IsDiscount!='Y' and @ChargeAmount!='0.00']">
									<LineCharge>
										<xsl:copy-of select="./@*"/>
										<xsl:copy-of select="./*"/>
									</LineCharge>
								</xsl:for-each>		
							</LineCharges>
						</LineDetail>
					</xsl:for-each>
				</LineDetails>	
			</InvoiceHeader>		
		</InvoiceDetail>
	</xsl:template>
</xsl:stylesheet>
