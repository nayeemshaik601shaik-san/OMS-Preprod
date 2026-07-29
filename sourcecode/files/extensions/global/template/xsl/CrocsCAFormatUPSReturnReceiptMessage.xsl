<!-- Output format
<Receipt  ReceivingNode="1032" >
    <Shipment  OrderNo="Y100002100" ReceivingNode="1032"/>
        <ReceiptLines>
            <ReceiptLine OrderNo="Y100002100" PrimeLineNo="1" Quantity="1" ItemID="40003-001-M22"/>
 			<ReceiptLine OrderNo="Y100002100" PrimeLineNo="2" Quantity="1" ItemID="40003-001-M22"/>
        </ReceiptLines>
</Receipt> -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" >
	<xsl:template match="/">
		<Receipt ReceivingNode="1032" >
			<xsl:attribute name="ReceivingNode">
				<xsl:value-of select="//GoodsReceipt/Plant"/>
			</xsl:attribute>
			<Shipment>
				<xsl:attribute name="OrderNo">
					<xsl:value-of select="/GoodsReceipt/ReceiptLine[1]/PurchaseOrder"/>
				</xsl:attribute>
				<xsl:attribute name="ReceivingNode">
					<xsl:value-of select="//GoodsReceipt/Plant"/>
				</xsl:attribute>
			</Shipment>
			<ReceiptLines>
				<xsl:for-each select="/GoodsReceipt/ReceiptLine">
					<ReceiptLine>
						<xsl:attribute name="OrderNo">
							<xsl:value-of select="./PurchaseOrder"/>
						</xsl:attribute>
						<xsl:attribute name="PrimeLineNo">
							<xsl:value-of select="./PurchaseOrderLineId"/>
						</xsl:attribute>
						<xsl:attribute name="Quantity">
							<xsl:value-of select="./Quantity/Qty"/>
						</xsl:attribute>
						<xsl:attribute name="ItemID">
							<xsl:value-of select="./ItemName"/>
						</xsl:attribute>
					</ReceiptLine>
				</xsl:for-each>
			</ReceiptLines>
		</Receipt>
	</xsl:template>
</xsl:stylesheet>