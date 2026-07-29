<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xalan="http://xml.apache.org/xslt"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xmlns:xsd="http://www.w3.org/2001/XMLSchema"
	xmlns:mediationUtil="xalan://com.ibm.commerce.sample.mediation.util.MediationUtil"
	xmlns:scwc="http://www.sterlingcommerce.com/scwc/"
	xmlns:ValueMaps="xalan://com.yantra.scwc.impl.ValueMapsData"
	extension-element-prefixes="ValueMaps" exclude-result-prefixes="xalan"
	version="1.0">
<xsl:template match="/">
<Order>
<xsl:attribute name="OrderHeaderKey">
        <xsl:value-of select="//@OrderHeaderKey" />
</xsl:attribute>
<OrderHoldTypes>
<OrderHoldType>
<xsl:attribute name="Status">1300</xsl:attribute>
  <xsl:attribute name="HoldType">
  <xsl:text>REMORSE_HOLD</xsl:text>
  </xsl:attribute>       
</OrderHoldType>
</OrderHoldTypes>
</Order>
</xsl:template>
</xsl:stylesheet>