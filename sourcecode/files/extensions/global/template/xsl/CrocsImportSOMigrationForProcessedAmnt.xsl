<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" indent="yes"/>

	<xsl:template match="/Order">
		<Order>
			<xsl:copy-of select="@*"/>
			<xsl:copy-of select="*[not(self::PaymentMethods)]"/>

			<PaymentMethods>
				<xsl:for-each select="PaymentMethods/PaymentMethod">
					<xsl:variable name="processedAmnt" select="normalize-space(PaymentDetailsList/PaymentDetails/@ProcessedAmount)"/>
					<xsl:variable name="requestAmnt" select="normalize-space(PaymentDetailsList/PaymentDetails/@RequestAmount)"/>

					<!-- If ProcessedAmount is empty, use RequestAmount -->
					<xsl:variable name="finalAmt">
						<xsl:choose>
							<xsl:when test="$processedAmnt != ''">
								<xsl:value-of select="$processedAmnt"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="$requestAmnt"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:variable>

					<PaymentMethod>
						<xsl:copy-of select="@*"/>
						<xsl:attribute name="MaxChargeLimit">
							<xsl:value-of select="$finalAmt"/>
						</xsl:attribute>
						<xsl:copy-of select="PersonInfoBillTo"/>
						<PaymentDetailsList>
							<PaymentDetails>
								<xsl:copy-of select="PaymentDetailsList/PaymentDetails/@*"/>
								<xsl:attribute name="RequestAmount">
									<xsl:value-of select="$finalAmt"/>
								</xsl:attribute>
								<xsl:attribute name="ProcessedAmount">
									<xsl:value-of select="$finalAmt"/>
								</xsl:attribute>
							</PaymentDetails>
						</PaymentDetailsList>
					</PaymentMethod>
				</xsl:for-each>
			</PaymentMethods>
		</Order>
	</xsl:template>
</xsl:stylesheet>
