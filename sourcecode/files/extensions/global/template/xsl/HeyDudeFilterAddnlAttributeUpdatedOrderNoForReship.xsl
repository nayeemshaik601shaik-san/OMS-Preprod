<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="/OrderRelease">
		<xsl:copy>
			<xsl:copy-of select="@*"/>
			<!-- Get OrderNo from child Order element -->
			<xsl:variable name="orderNo" select="Order/@OrderNo"/>
			<xsl:variable name="releaseNo" select="@ReleaseNo"/>
			<!-- Collect all ExtnSAPMaterialGroup values -->
			<xsl:variable name="materialGroupValues" select="OrderLine/ItemDetails/Extn/@ExtnSAPMaterialGroup"/>
			<!-- Check if there are any missing or empty values -->
			<xsl:variable name="missingValues" select="OrderLine/ItemDetails/Extn[not(normalize-space(@ExtnSAPMaterialGroup))]"/>
			<!-- Check for conditions -->
			<xsl:choose>
				<xsl:when test="count($missingValues) > 0">
					<xsl:attribute name="InternationalGoodsDescription">MIXED</xsl:attribute>
				</xsl:when>
				<xsl:when test="count($materialGroupValues) = count($materialGroupValues[. = $materialGroupValues[1]])">
					<xsl:choose>
						<xsl:when test="$materialGroupValues[1] = 'Footwear'">
							<xsl:attribute name="InternationalGoodsDescription">FOOTWEAR</xsl:attribute>
						</xsl:when>
						<xsl:when test="$materialGroupValues[1] = 'Charms'">
							<xsl:attribute name="InternationalGoodsDescription">CHARMS</xsl:attribute>
						</xsl:when>
						<xsl:when test="$materialGroupValues[1] = 'Apparel'">
							<xsl:attribute name="InternationalGoodsDescription">APPAREL</xsl:attribute>
						</xsl:when>
						<xsl:when test="$materialGroupValues[1] = 'Accessories'">
							<xsl:attribute name="InternationalGoodsDescription">ACCESSORY</xsl:attribute>
						</xsl:when>
						<xsl:otherwise>
							<xsl:attribute name="InternationalGoodsDescription">MIXED</xsl:attribute>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:when>
				<xsl:otherwise>
					<xsl:attribute name="InternationalGoodsDescription">MIXED</xsl:attribute>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:apply-templates select="*"/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="Order">
		<xsl:variable name="releaseNo" select="/OrderRelease/@ReleaseNo"/>
		<xsl:variable name="enterpriseCode" select="/OrderRelease/@EnterpriseCode"/>
		<xsl:variable name="originalOrderNo" select="@OrderNo"/>
		<xsl:variable name="CustomerPONo" select="@CustomerPONo"/>
		<xsl:copy>
			<!-- Copy all attributes except OrderNo -->
			<xsl:for-each select="@*">
				<xsl:if test="name() != 'OrderNo'">
					<xsl:attribute name="{name()}">
						<xsl:value-of select="."/>
					</xsl:attribute>
				</xsl:if>
			</xsl:for-each>
			<!-- Determine final OrderNo -->
			<xsl:variable name="finalOrderNo">
				<xsl:choose>
					<xsl:when test="normalize-space(/OrderRelease/OrderLine/@ReshipParentLineKey) != ''">
						<xsl:value-of select="concat($originalOrderNo, 'SHIP', $releaseNo)"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="$originalOrderNo"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:variable>
			<xsl:attribute name="OrderNo">
				<xsl:value-of select="$finalOrderNo"/>
			</xsl:attribute>
			<xsl:apply-templates select="*"/>
		</xsl:copy>
	</xsl:template>
	<!-- Template to copy all elements and attributes -->
	<xsl:template match="*">
		<xsl:copy>
			<xsl:copy-of select="@*"/>
			<xsl:apply-templates select="*"/>
		</xsl:copy>
	</xsl:template>
	<!-- Skip OrderLine with OrderedQty = 0 -->
	<xsl:template match="OrderLine[number(@OrderedQty) = 0]"/>
	<!-- Template to filter AdditionalAttribute elements based on EnterpriseCode -->
	<xsl:template match="AdditionalAttribute">
		<xsl:variable name="enterpriseCode" select="/OrderRelease/@EnterpriseCode"/>
		<xsl:if test="translate(@Name, 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ') = translate($enterpriseCode, 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')">
			<xsl:copy>
				<xsl:copy-of select="@*"/>
			</xsl:copy>
		</xsl:if>
	</xsl:template>
	<xsl:template match="PaymentMethods"/>
</xsl:stylesheet>
