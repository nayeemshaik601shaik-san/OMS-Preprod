<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="xml" indent="yes"/>
    <xsl:template match="/Order">
        <Customer CustomerType="02" OrganizationCode="{translate(@EnterpriseCode, 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')}" Status="10">
            <CustomerContactList>
                <CustomerContact 
                    EmailID="{translate(@CustomerEMailID, 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')}" 
                    FirstName="{translate(@CustomerFirstName, 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')}" 
                    LastName="{translate(@CustomerLastName, 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')}"
					DayPhone="{PersonInfoBillTo/@DayPhone}">
                    <CustomerAdditionalAddressList>
                        <CustomerAdditionalAddress IsShipTo="Y" IsDefaultShipTo="Y">
                            <PersonInfo>
                                <xsl:apply-templates select="PersonInfoShipTo"/>
                            </PersonInfo>
                        </CustomerAdditionalAddress>
                        <CustomerAdditionalAddress IsBillTo="Y" IsDefaultBillTo="Y">
                            <PersonInfo>
                                <xsl:apply-templates select="PersonInfoBillTo"/>
                            </PersonInfo>
                        </CustomerAdditionalAddress>
                    </CustomerAdditionalAddressList>
                </CustomerContact>
            </CustomerContactList>
        </Customer>
    </xsl:template>
	
	<xsl:template match="PersonInfoShipTo | PersonInfoBillTo">
        <xsl:for-each select="@*">
            <xsl:attribute name="{name()}">
                <xsl:value-of select="translate(., 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')"/>
            </xsl:attribute>
        </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>