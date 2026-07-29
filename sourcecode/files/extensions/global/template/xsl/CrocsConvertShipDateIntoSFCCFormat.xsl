<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:util="xalan://com.crocs.oms.common.util.CommonUtil" exclude-result-prefixes="util">
	<xsl:output method="xml" indent="yes"/>
	<!-- Identity template to copy everything -->
	<xsl:template match="@* | node()">
		<xsl:copy>
			<xsl:apply-templates select="@* | node()"/>
		</xsl:copy>
	</xsl:template>
	<!-- Override Extn/@ExtnShipmentDate under OrderLine only -->
	<xsl:template match="OrderLine/Extn/@ExtnShipmentDate">
		<xsl:attribute name="ExtnShipmentDate">
			<xsl:value-of select="util:convertInputStringDateIntoSFCCFormat(.)"/>
		</xsl:attribute>
	</xsl:template>
</xsl:stylesheet>