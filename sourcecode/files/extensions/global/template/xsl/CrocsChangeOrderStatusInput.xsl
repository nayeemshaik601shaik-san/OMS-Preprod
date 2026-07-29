<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="/">
<OrderStatusChange>
	<xsl:attribute name="TransactionId">INCLUDE_IN_RETURN</xsl:attribute>
	<xsl:attribute name="SelectMethod">WAIT</xsl:attribute>
	<xsl:attribute name="IgnoreTransactionDependencies">Y</xsl:attribute>
	<xsl:attribute name="BaseDropStatus">3700.01</xsl:attribute>
	<xsl:attribute name="OrderNo">
	<xsl:value-of select="/Order/OrderLines/OrderLine/DerivedFromOrder/@OrderNo"/>
	</xsl:attribute>
	<xsl:attribute name="OrderHeaderKey">
	<xsl:value-of select="/Order/OrderLines/OrderLine/DerivedFromOrder/@OrderHeaderKey"/>
	</xsl:attribute>
	<xsl:attribute name="EnterpriseCode">
	<xsl:value-of select="/Order/OrderLines/OrderLine/DerivedFromOrder/@EnterpriseCode"/>
	</xsl:attribute>
	<xsl:attribute name="DocumentType">
	<xsl:value-of select="/Order/OrderLines/OrderLine/DerivedFromOrder/@DocumentType"/>
	</xsl:attribute>
</OrderStatusChange>	
</xsl:template>
</xsl:stylesheet>