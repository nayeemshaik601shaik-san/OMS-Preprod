<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <!-- Match the root element -->
    <xsl:template match="/">
        <OrderStatusChange 
            BaseDropStatus="3700" 
            ChangeForAllAvailableQty="Y" 
            IgnoreTransactionDependencies="Y"
            TransactionId="SHIP_ORDER">
            <!-- Copy the OrderHeaderKey attribute from the input -->
            <xsl:attribute name="OrderHeaderKey">
                <xsl:value-of select="/OrderHoldType/Order/@OrderHeaderKey"/>
            </xsl:attribute>
        </OrderStatusChange>
    </xsl:template>
</xsl:stylesheet>