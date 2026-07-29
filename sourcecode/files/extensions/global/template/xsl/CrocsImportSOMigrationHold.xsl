<?xml version="1.0"?>
<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:output method="xml" indent="yes"/>

	<xsl:template match="/">
		<Order>
			<xsl:variable name="varProcessedAmount" select="format-number(sum(/Order/PaymentMethods/PaymentMethod/PaymentDetailsList/PaymentDetails/@ProcessedAmount), '#.00')"/>
			<xsl:variable name="varOrderTotalAmount" select="format-number(/Order/PriceInfo/@TotalAmount, '#.00')"/>

			<xsl:copy-of select="Order/@*"/>
			<xsl:copy-of select="Order/*[not(self::PriceInfo or self::PaymentMethods)]"/>

			<PriceInfo>
				<xsl:copy-of select="Order/PriceInfo/@*"/>
				<xsl:attribute name="InvoicedAmount">
					<xsl:value-of select="/Order/PriceInfo/@TotalAmount"/>
				</xsl:attribute>
			</PriceInfo>
			<PaymentMethods>
				<xsl:for-each select="/Order/PaymentMethods/PaymentMethod">
					<PaymentMethod>
						<xsl:copy-of select="./@*"/>
						<xsl:attribute name="MaxChargeLimit">
							<xsl:value-of select="./PaymentDetailsList/PaymentDetails/@ProcessedAmount"/>
						</xsl:attribute>
						<xsl:copy-of select="./PersonInfoBillTo"/>
						<PaymentDetailsList>
							<PaymentDetails>
								<xsl:copy-of select="./PaymentDetailsList/PaymentDetails/@*"/>
								<xsl:attribute name="RequestAmount">
									<xsl:value-of select="./PaymentDetailsList/PaymentDetails/@ProcessedAmount"/>
								</xsl:attribute>
							</PaymentDetails>
						</PaymentDetailsList>
					</PaymentMethod>
				</xsl:for-each>
			</PaymentMethods>
            <xsl:if test="$varOrderTotalAmount!=$varProcessedAmount">
				<OrderHoldTypes>
					<OrderHoldType>
						<xsl:attribute name="HoldType">
							<xsl:value-of select="'MIGRATION_PAY_HOLD'"/>
						</xsl:attribute>
						<xsl:attribute name="HoldReasonCode">
							<xsl:value-of select="'Payment Processing hold'"/>
						</xsl:attribute>
						<xsl:attribute name="ReasonText">
							<xsl:value-of select="'Migration Payment Mismatch'"/>
						</xsl:attribute>
						<xsl:attribute name="Status">
							<xsl:value-of select="'1100'"/>
						</xsl:attribute>
					</OrderHoldType>
				</OrderHoldTypes>
			</xsl:if>
		</Order>
	</xsl:template>

</xsl:stylesheet>
